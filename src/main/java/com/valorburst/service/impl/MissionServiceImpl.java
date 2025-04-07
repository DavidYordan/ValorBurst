package com.valorburst.service.impl;

import com.valorburst.config.AppProperties;
import com.valorburst.dto.JdbcDto;
import com.valorburst.model.local.Mission;
import com.valorburst.model.local.MissionArchive;
import com.valorburst.model.local.MissionDetails;
import com.valorburst.model.local.MissionDetailsArchive;
import com.valorburst.model.local.MissionDetailsCompensation;
import com.valorburst.model.local.User;
import com.valorburst.repository.local.LocalCourseDetailsRepository;
import com.valorburst.repository.local.LocalCourseRepository;
import com.valorburst.repository.local.LocalVipDetailsRepository;
import com.valorburst.repository.local.MissionArchiveRepository;
import com.valorburst.repository.local.MissionDetailsArchiveRepository;
import com.valorburst.repository.local.MissionDetailsCompensationRepository;
import com.valorburst.repository.local.MissionDetailsRepository;
import com.valorburst.repository.local.MissionRepository;
import com.valorburst.repository.local.SettingRepository;
import com.valorburst.repository.local.UserRepository;
import com.valorburst.service.MissionService;
import com.valorburst.service.TelegramBotService;
import com.valorburst.util.DedupingExecutor;
import com.valorburst.util.JdbcHelper;
import com.valorburst.util.MissionDetailsBuilder;
import com.valorburst.util.TelegramMessageBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionServiceImpl implements MissionService {

    private final LocalCourseRepository localCourseRepository;
    private final LocalCourseDetailsRepository localCourseDetailsRepository;
    private final LocalVipDetailsRepository localVipDetailsRepository;
    private final MissionArchiveRepository missionArchiveRepository;
    private final MissionDetailsRepository missionDetailsRepository;
    private final MissionDetailsArchiveRepository missionDetailsArchiveRepository;
    private final MissionDetailsCompensationRepository compensationRepository;
    private final MissionRepository missionRepository;
    private final SettingRepository settingRepository;
    private final UserRepository userRepository;
    
    private final AppProperties appProperties;
    private final DedupingExecutor dedupingExecutor;
    private final JdbcHelper jdbcHelper;
    private final MissionDetailsBuilder missionDetailsBuilder;
    private final TelegramBotService telegramBotService;

    Set<Integer> executingSet = new HashSet<>();

    /**
     * 检查超时任务
     */
    public void checkTimeoutMissions() {
        dedupingExecutor.execute(() -> {
            String timeoutStr = settingRepository.findValueByKey("execution_timeout");
            // 超时时间为当前时间减去设置的超时时间
            Instant timeout = Instant.now().minusSeconds(Long.parseLong(timeoutStr));
            List<MissionDetails> timeoutDetails = missionDetailsRepository.findAllTimeoutExecute(timeout);
            if (timeoutDetails.isEmpty()) {
                log.info("没有超时任务");
                return;
            }
            String message = TelegramMessageBuilder.buildTimeoutDetails(timeoutDetails);
            telegramBotService.sendMessage(appProperties.getTelegram().getChatId(), message, "timeout");
        }, "check-timeout-missions");
    }

    /**
     * 初始化任务
     */
    public void generateInitMissions() {
        List<Mission> missions = missionRepository.findAllNeedInit();
        for (Mission mission : missions) {
            Integer type = mission.getType();
            if (type == 0) {
                buildEmptyMissionDetails(mission);
            } else if (type >= 1 && type <= 8) {
                dedupingExecutor.execute(() -> buildVipMissionDetails(mission), "build-vip-mission-" + mission.getMissionId());
            } else if (type == 11 || type == 12) {
                dedupingExecutor.execute(() -> buildSingleMissionDetails(mission), "build-single-mission-" + mission.getMissionId());
            } else if (type == 21 || type == 22) {
                dedupingExecutor.execute(() -> buildAllMissionDetails(mission), "build-all-mission-" + mission.getMissionId());
            } else {
                log.info("跳过非vip任务，missionId={}，任务类型={}", mission.getMissionId(), type);
            }
        }
    }

    /**
     * 扫描到期任务并执行
     */
    public void executeDueMissions() {
        List<MissionDetails> dueDetails = missionDetailsRepository.findAllNeedExecute();
        for (MissionDetails details : dueDetails) {
            Integer missionDetailsId = details.getMissionDetailsId();

            synchronized (executingSet) {
                if (!executingSet.add(missionDetailsId)) {
                    log.debug("跳过重复任务: {}", missionDetailsId);
                    continue;
                }
            }

            dedupingExecutor.execute(() -> {
                try {
                    // 额外双重校验（数据库中executing字段为false）
                    Integer locked = missionDetailsRepository.tryMarkExecuting(missionDetailsId);
                    if (locked == 0) {
                        log.warn("数据库执行状态冲突，跳过执行: {}", missionDetailsId);
                        return;
                    }

                    doExecuteMissionDetails(details);
                } catch (Exception e) {
                    log.error("任务执行异常: {}", missionDetailsId, e);
                } finally {
                    // 执行结束后移除记录
                    synchronized (executingSet) {
                        executingSet.remove(missionDetailsId);
                    }
                }
            });
        }
    }

    /**
     * 构建空任务详情
     */
    private void buildEmptyMissionDetails(Mission mission) {
        Integer missionId = mission.getMissionId();
        log.info("开始构建missionDetails, missionId = {}, type = 0", missionId);

        BigDecimal expectMoney = mission.getExpectMoney();
        Integer count = missionDetailsArchiveRepository.countMoneyZeroByMissionId(missionId);
        // if (count >= expectMoney) {
        if (count >= expectMoney.intValue()) {
            archiveMission(mission);
            return;
        }

        Integer remainingExecutions = (int) Math.ceil((expectMoney.subtract(BigDecimal.valueOf(count))).doubleValue());
        Instant exeCuteTime = mission.getExecuteTime();
        Instant nextExecuteTime;
        Instant endTime = mission.getEndTime();
        if (exeCuteTime == null) {
            exeCuteTime = mission.getStartTime();
        }
        if (exeCuteTime.isAfter(endTime)) {
            nextExecuteTime = Instant.now();
        } else {
            nextExecuteTime = getNextExecuteTime(exeCuteTime, endTime, remainingExecutions);
        }

        BigDecimal bigZero = BigDecimal.ZERO;
        MissionDetails details = MissionDetails.builder()
                .missionId(missionId)
                .userId(mission.getUserId())
                .type(mission.getType())
                .cost(bigZero)
                .rate(bigZero)
                .money(bigZero)
                .executeTime(nextExecuteTime)
                .languageType(mission.getLanguageType())
                .build();
        missionDetailsRepository.save(details);

        mission.setExecuteTime(nextExecuteTime);
        missionRepository.save(mission);
    }

    /**
     * 构建VIP任务详情
     */
    private void buildVipMissionDetails(Mission mission) {
        Integer vipType;
        Integer inviterLevel;
        Integer type = mission.getType();
        // String languageType = mission.getLanguageType();
        String languageType = "en";
        switch (type) {
            case 1 -> { vipType = 3; inviterLevel = 1; }
            case 2 -> { vipType = 0; inviterLevel = 1; }
            case 3 -> { vipType = 1; inviterLevel = 1; }
            case 4 -> { vipType = 2; inviterLevel = 1; }
            case 5 -> { vipType = 3; inviterLevel = 2; }
            case 6 -> { vipType = 0; inviterLevel = 2; }
            case 7 -> { vipType = 1; inviterLevel = 2; }
            case 8 -> { vipType = 2; inviterLevel = 2; }
            default -> {
                log.error("未知的vip任务类型: {}", type);
                return;
            }
        }

        log.info("开始构建missionDetails, missionId = {}, type = {}, languageType = {}", mission.getMissionId(), type, languageType);
        BigDecimal vipMoney = Optional.ofNullable(localVipDetailsRepository.findMoney(vipType, languageType))
                .orElse(null);
        if (vipMoney == null) {
            log.error("未配置vip价格, missionId = {}, type = {}, languageType = {}", mission.getMissionId(), type, languageType);
            return;
        }

        User user = userRepository.findById(mission.getUserId())
                .orElse(null);
        if (user == null) {
            log.error("用户不存在, userId = {}", mission.getUserId());
            return;
        }

        BigDecimal userRate = inviterLevel == 1 ? user.getRate() : user.getTwoRate();
        if (userRate == null || userRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("missionId {} 用户费率不正确", mission.getMissionId());
            return;
        }

        // vipMoney * userRate = 每次执行的金额
        BigDecimal perExecutionMoney = vipMoney.multiply(userRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal money = mission.getMoney() == null ? BigDecimal.ZERO : mission.getMoney();
        BigDecimal expectMoney = mission.getExpectMoney();
        BigDecimal needMoney = expectMoney.subtract(money);
        BigDecimal overflowFactor = BigDecimal.ONE.add(mission.getOverflow().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal overflowLimit = expectMoney.multiply(overflowFactor);
        
        if (needMoney.compareTo(BigDecimal.ZERO) <= 0 || money.add(perExecutionMoney).compareTo(overflowLimit) > 0) {
            archiveMission(mission);
            return;
        }

        // Integer remainingExecutions = (int) Math.ceil(needMoney / perExecutionMoney);
        Integer remainingExecutions = (int) Math.ceil(needMoney.divide(perExecutionMoney, 2, RoundingMode.HALF_UP).doubleValue());

        Instant exeCuteTime = mission.getExecuteTime();
        Instant nextExecuteTime;
        Instant endTime = mission.getEndTime();
        if (exeCuteTime == null) {
            exeCuteTime = mission.getStartTime();
        }
        if (exeCuteTime.isAfter(endTime)) {
            nextExecuteTime = Instant.now();
        } else {
            nextExecuteTime = getNextExecuteTime(exeCuteTime, endTime, remainingExecutions);
        }

        MissionDetails details = MissionDetails.builder()
                .missionId(mission.getMissionId())
                .userId(mission.getUserId())
                .type(mission.getType())
                .cost(vipMoney)
                .rate(userRate)
                .money(perExecutionMoney)
                .executeTime(nextExecuteTime)
                .languageType(mission.getLanguageType())
                .build();
        missionDetailsRepository.save(details);

        mission.setExecuteTime(details.getExecuteTime());
        missionRepository.save(mission);
    }

    /**
     * 生成下一次任务（如果业务需求允许）
     */
    private void generateNextMissions(Mission mission) {
        Integer type = mission.getType();
        if (type == 0) {
            buildEmptyMissionDetails(mission);
        } else if (type >= 1 && type <= 8) {
            dedupingExecutor.execute(() -> buildVipMissionDetails(mission), "build-vip-mission-" + mission.getMissionId());
        } else if (type == 11 || type == 12) {
            dedupingExecutor.execute(() -> buildSingleMissionDetails(mission), "build-single-mission-" + mission.getMissionId());
        } else if (type == 21 || type == 22) {
            dedupingExecutor.execute(() -> buildAllMissionDetails(mission), "build-all-mission-" + mission.getMissionId());
        } else {
            log.error("未知的任务类型: {}", type);
        }
    }

    private Instant getNextExecuteTime(Instant startTime, Instant endTime, Integer remainingExecutions) {
        Long durationSec = Duration.between(startTime, endTime).getSeconds();
        Double u = Math.random();
        Double min = 1 - Math.pow(1 - u, 1.0 / remainingExecutions);
        return startTime.plusSeconds((long) (min * durationSec));
    }

    /**
     * 真正执行任务的函数：
     * 1. 先执行远程数据库操作（批量高效提交）
     * 2. 再在本地事务中更新数据（更新mission金额、归档mission_details、生成下一任务）
     * 如果本地更新失败，则记录补偿信息，由后续补偿流程处理
     */
    public void doExecuteMissionDetails(MissionDetails details) {
        // 远程数据库操作
        JdbcDto jdbcDto = missionDetailsBuilder.buildJdbc(details);
        String sql = jdbcDto.getSql();
        log.info("构建sql: missionDetailsId={}", details.getMissionDetailsId());
        if (sql == null || sql.trim().isEmpty()) {
            log.error("构建sql失败, missionDetailsId={}", details.getMissionDetailsId());
            return;
        }
        jdbcHelper.executeInTransaction(sql);

        details.setInviteeId(jdbcDto.getUserId());
        details.setInviteeName(jdbcDto.getUsername());

        // 本地数据库操作（采用本地事务处理）
        try {
            updateLocalData(details);
        } catch (Exception e) {
            log.error("本地数据库更新失败, missionDetailsId={}, scheduling compensation", details.getMissionDetailsId(), e);
            recordCompensation(details);
            throw new RuntimeException("本地数据库更新失败，已记录补偿信息", e);
        }

        log.info("MissionDetails {} 执行完毕, money = {}", details.getMissionDetailsId(), details.getMoney());
    }

    /**
     * 本地数据更新：在一个事务中更新mission金额、归档mission_details、生成下一任务
     */
    @Transactional
    public void updateLocalData(MissionDetails details) {
        Mission mission = missionRepository.findById(details.getMissionId())
                .orElseThrow(() -> new RuntimeException("任务不存在, missionId=" + details.getMissionId()));
        BigDecimal totalMoney = missionDetailsArchiveRepository.sumMoneyByMissionId(mission.getMissionId());
        BigDecimal todayMoney = missionDetailsArchiveRepository.sumMoneyByMissionIdAndToday(mission.getMissionId());
        mission.setMoney(totalMoney == null ? BigDecimal.ZERO : totalMoney);
        mission.setTodayMoney(todayMoney == null ? BigDecimal.ZERO : todayMoney);
        missionRepository.save(mission);

        archiveMissionDetails(details);
        generateNextMissions(mission);
    }

    /**
     * 记录补偿信息，用于后续补偿处理
     * 实际场景中可将补偿信息存入单独的表，由定时任务或消息队列处理补偿逻辑
     */
    private void recordCompensation(MissionDetails details) {
        log.warn("记录补偿信息: missionDetailsId={}, missionId={}, money={}",
                details.getMissionDetailsId(), details.getMissionId(), details.getMoney());

        // TODO: 可在此将补偿信息写入补偿表
        compensationRepository.save(MissionDetailsCompensation.builder()
                .missionDetailsId(details.getMissionDetailsId())
                .missionId(details.getMissionId())
                .userId(details.getUserId())
                .type(details.getType())
                .cost(details.getCost())
                .rate(details.getRate())
                .money(details.getMoney())
                .inviteeId(details.getInviteeId())
                .inviteeName(details.getInviteeName())
                .continuous(details.getContinuous())
                .executeTime(details.getExecuteTime())
                .languageType(details.getLanguageType())
                .build());
    }

    private void archiveMission(Mission mission) {
        missionArchiveRepository.save(MissionArchive.builder()
                .missionId(mission.getMissionId())
                .userId(mission.getUserId())
                .expectMoney(mission.getExpectMoney())
                .startTime(mission.getStartTime())
                .endTime(mission.getEndTime())
                .money(mission.getMoney())
                .type(mission.getType())
                .archiveTime(Instant.now())
                .languageType(mission.getLanguageType())
                .build());
        missionRepository.deleteById(mission.getMissionId());
        log.info("Mission {} 归档完毕", mission.getMissionId());
    }

    private void archiveMissionDetails(MissionDetails details) {
        Integer type = details.getType();
        Integer continous = details.getContinuous();
        BigDecimal cost = details.getCost();
        BigDecimal money = details.getMoney();
        if (type == 11 || type == 12) {
            cost = cost.multiply(BigDecimal.valueOf(continous)).setScale(2, RoundingMode.HALF_UP);
            money = money.multiply(BigDecimal.valueOf(continous)).setScale(2, RoundingMode.HALF_UP);
        }
        missionDetailsArchiveRepository.save(MissionDetailsArchive.builder()
                .missionDetailsId(details.getMissionDetailsId())
                .missionId(details.getMissionId())
                .userId(details.getUserId())
                .type(type)
                .cost(cost)
                .rate(details.getRate())
                .money(money)
                .inviteeId(details.getInviteeId())
                .inviteeName(details.getInviteeName())
                .continuous(continous)
                .executeTime(details.getExecuteTime())
                .archiveTime(Instant.now())
                .languageType(details.getLanguageType())
                .build());
        missionDetailsRepository.deleteById(details.getMissionDetailsId());
        log.info("MissionDetails {} 归档完毕", details.getMissionDetailsId());
    }

    // 其他构建任务的方法，可根据业务需要实现
    private void buildSingleMissionDetails(Mission mission) {
        Integer inviterLevel;
        Integer type = mission.getType();
        // String languageType = mission.getLanguageType();
        String languageType = "en";
        if (type == 21) {
            inviterLevel = 1;
        } else {
            inviterLevel = 2;
        }

        log.info("开始构建missionDetails, missionId = {}, type = {}, languageType = {}", mission.getMissionId(), type, languageType);

        User user = userRepository.findById(mission.getUserId())
                .orElse(null);

        if (user == null) {
            log.error("用户不存在, userId = {}", mission.getUserId());
            return;
        }

        BigDecimal userRate = inviterLevel == 1 ? user.getRate() : user.getTwoRate();
        if (userRate == null || userRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("missionId {} 用户费率不正确", mission.getMissionId());
            return;
        }

        BigDecimal money = mission.getMoney() == null ? BigDecimal.ZERO : mission.getMoney();
        BigDecimal expectMoney = mission.getExpectMoney();
        BigDecimal needMoney = expectMoney.subtract(money);
        BigDecimal overflowFactor = BigDecimal.ONE.add(mission.getOverflow().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal overflowLimit = expectMoney.multiply(overflowFactor);

        List<BigDecimal> prices = localCourseDetailsRepository.findDistinctValidPrices();

        if (prices.isEmpty()) {
            log.error("未找到有效价格, missionId = {}, type = {}, languageType = {}", mission.getMissionId(), type, languageType);
            return;
        }

        Collections.shuffle(prices);
        BigDecimal selectedPrice = null;
        BigDecimal perExecutionMoney = null;
        Integer continous = 1;
        for (BigDecimal price : prices) {
            if (Math.random() < 0.8) {
                continous = 1;
            } else {
                continous = ThreadLocalRandom.current().nextInt(2, 21);
            }

            perExecutionMoney = price
                    .multiply(BigDecimal.valueOf(continous))
                    .multiply(userRate)
                    .setScale(2, RoundingMode.HALF_UP);

            if (needMoney.compareTo(BigDecimal.ZERO) > 0 &&
                money.add(perExecutionMoney).compareTo(overflowLimit) <= 0) {
                selectedPrice = price;
                break;
            }
        }

        if (selectedPrice == null || perExecutionMoney == null) {
            log.error("未找到合适的价格, missionId = {}, type = {}, languageType = {}", mission.getMissionId(), type, languageType);
            archiveMission(mission);
            return;
        }

        Integer remainingExecutions = (int) Math.ceil(needMoney.divide(perExecutionMoney, 2, RoundingMode.HALF_UP).doubleValue());
        Instant exeCuteTime = mission.getExecuteTime();
        Instant nextExecuteTime;
        Instant endTime = mission.getEndTime();
        if (exeCuteTime == null) {
            exeCuteTime = mission.getStartTime();
        }
        if (exeCuteTime.isAfter(endTime)) {
            nextExecuteTime = Instant.now();
        } else {
            nextExecuteTime = getNextExecuteTime(exeCuteTime, endTime, remainingExecutions);
        } 

        MissionDetails details = MissionDetails.builder()
                .missionId(mission.getMissionId())
                .userId(mission.getUserId())
                .type(mission.getType())
                .cost(selectedPrice)
                .rate(userRate)
                .money(perExecutionMoney)
                .continuous(continous)
                .executeTime(nextExecuteTime)
                .languageType(languageType)
                .continuous(continous)
                .build();
        missionDetailsRepository.save(details);

        mission.setExecuteTime(details.getExecuteTime());
        missionRepository.save(mission);
    }

    private void buildAllMissionDetails(Mission mission) {
        Integer inviterLevel;
        Integer type = mission.getType();
        // String languageType = mission.getLanguageType();
        String languageType = "en";
        if (type == 21) {
            inviterLevel = 1;
        } else {
            inviterLevel = 2;
        }

        log.info("开始构建missionDetails, missionId = {}, type = {}, languageType = {}", mission.getMissionId(), type, languageType);

        User user = userRepository.findById(mission.getUserId())
                .orElse(null);
        if (user == null) {
            log.error("用户不存在, userId = {}", mission.getUserId());
            return;
        }

        BigDecimal userRate = inviterLevel == 1 ? user.getRate() : user.getTwoRate();
        if (userRate == null || userRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("missionId {} 用户费率不正确", mission.getMissionId());
            return;
        }

        BigDecimal money = mission.getMoney() == null ? BigDecimal.ZERO : mission.getMoney();
        BigDecimal expectMoney = mission.getExpectMoney();
        BigDecimal needMoney = expectMoney.subtract(money);
        BigDecimal overflowFactor = BigDecimal.ONE.add(mission.getOverflow().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal overflowLimit = expectMoney.multiply(overflowFactor);

        List<BigDecimal> prices = localCourseRepository.findDistinctValidPrices();
        if (prices.isEmpty()) {
            log.error("未找到有效价格, missionId = {}, type = {}, languageType = {}", mission.getMissionId(), type, languageType);
            return;
        }
        Collections.shuffle(prices);

        BigDecimal selectedPrice = null;
        BigDecimal perExecutionMoney = null;

        for (BigDecimal price : prices) {
            perExecutionMoney = price.multiply(userRate).setScale(2, RoundingMode.HALF_UP);
        
            if (needMoney.compareTo(BigDecimal.ZERO) > 0 &&
                money.add(perExecutionMoney).compareTo(overflowLimit) <= 0) {
                selectedPrice = price;
                break;
            }
        }

        if (selectedPrice == null || perExecutionMoney == null) {
            log.error("未找到合适的价格, missionId = {}, type = {}, languageType = {}", mission.getMissionId(), type, languageType);
            archiveMission(mission);
            return;
        }

        Integer remainingExecutions = (int) Math.ceil(needMoney.divide(perExecutionMoney, 2, RoundingMode.HALF_UP).doubleValue());

        Instant exeCuteTime = mission.getExecuteTime();
        Instant nextExecuteTime;
        Instant endTime = mission.getEndTime();
        if (exeCuteTime == null) {
            exeCuteTime = mission.getStartTime();
        }
        if (exeCuteTime.isAfter(endTime)) {
            nextExecuteTime = Instant.now();
        } else {
            nextExecuteTime = getNextExecuteTime(exeCuteTime, endTime, remainingExecutions);
        }

        MissionDetails details = MissionDetails.builder()
                .missionId(mission.getMissionId())
                .userId(mission.getUserId())
                .type(mission.getType())
                .cost(selectedPrice)
                .rate(userRate)
                .money(perExecutionMoney)
                .executeTime(nextExecuteTime)
                .languageType(languageType)
                .build();
        missionDetailsRepository.save(details);

        mission.setExecuteTime(details.getExecuteTime());
        missionRepository.save(mission);
    }
}
