package com.valorburst.service.impl;

import com.valorburst.config.AppProperties;
import com.valorburst.dto.AddMissionRequestDto;
import com.valorburst.dto.JdbcDto;
import com.valorburst.dto.MissionResponseDto;
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

import org.apache.commons.math3.distribution.BetaDistribution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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
    Set<Integer> checkMissionSet = new HashSet<>();
    private static final BigDecimal BIGZERO = BigDecimal.ZERO;

    public void addMissions(AddMissionRequestDto missionDto) {
        Integer userId = missionDto.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在, userId=" + userId));
        String team = missionDto.getTeam();
        String region = missionDto.getRegion();
        if (team != null && !team.isEmpty()) {
            user.setTeam(team);
        }
        user.setRegion(region);
        userRepository.save(user);

        String languageType = missionDto.getLanguageType();
        LocalDateTime startTime = missionDto.getStartTime();
        LocalDateTime endTime = missionDto.getEndTime();

        List<AddMissionRequestDto.TypeDto> typeDtos = missionDto.getTypeDtos();

        List<Mission> missions = typeDtos.stream()
                .map((AddMissionRequestDto.TypeDto typeDto) -> {
                    Integer type = typeDto.getType();
                    BigDecimal expectMoney = typeDto.getExpectMoney();
                    BigDecimal overflow = typeDto.getOverflow();
                    BigDecimal decreasing = typeDto.getDecreasing();

                    return Mission.builder()
                            .userId(userId)
                            .expectMoney(expectMoney)
                            .overflow(overflow)
                            .decreasing(decreasing)
                            .startTime(startTime)
                            .endTime(endTime)
                            .type(type)
                            .languageType(languageType)
                            .build();
                })
                .collect(Collectors.toList());

        missionRepository.saveAll(missions);
    }

    /**
     * 检查超时任务
     */
    public void checkTimeoutMissions() {
        dedupingExecutor.execute(() -> {
            String timeoutStr = settingRepository.findValueByKey("execution_timeout");
            // 超时时间为当前时间减去设置的超时时间
            LocalDateTime timeout = LocalDateTime.now().minusSeconds(Long.parseLong(timeoutStr));
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
     * 检查主任务
     */
    public void checkMissions() {
        List<Mission> missions = missionRepository.findMissions();
        for (Mission mission : missions) {
            checkMission(mission);
        }
    }

    private void checkMission(Mission mission) {
        Integer missionId = mission.getMissionId();
        synchronized (checkMissionSet) {
            if (!checkMissionSet.add(missionId)) {
                log.info("任务 {} 正在检查中，跳过本次检查", missionId);
                return;
            }
        }

        BigDecimal expectMoney = mission.getExpectMoney();
        BigDecimal money = mission.getMoney();
        if (money != null && money.compareTo(expectMoney) >= 0) {
            dedupingExecutor.execute(() -> {
                try {
                    archiveMission(mission);
                } catch (Exception e) {
                    log.error("归档任务异常: {}", missionId, e);
                } finally {
                    synchronized (checkMissionSet) {
                        checkMissionSet.remove(missionId);
                    }
                }
            });
            return;
        }

        try {
            Integer type = mission.getType();
            Integer count = 0;
            if (type == 11 || type == 12) {
                count = missionDetailsRepository.countSignalByMissionId(missionId);
            } else {
                count = missionDetailsRepository.countByMissionId(missionId);
            }

            if (count > 0) {
                synchronized (checkMissionSet) {
                    checkMissionSet.remove(missionId);
                }
                return;
            }
        } catch (Exception e) {
            log.error("查询任务详情异常: {}", missionId, e);
            synchronized (checkMissionSet) {
                checkMissionSet.remove(missionId);
            }
            return;
        }

        dedupingExecutor.execute(() -> {
            try {
                generateNextMissions(mission);
            } catch (Exception e) {
                log.error("生成下一任务异常: {}", missionId, e);
            } finally {
                synchronized (checkMissionSet) {
                    checkMissionSet.remove(missionId);
                }
            }
        });
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
                    continue;
                }
            }

            dedupingExecutor.execute(() -> {
                try {
                    doExecuteMissionDetails(details);
                } catch (Exception e) {
                    log.error("任务执行异常: {}", missionDetailsId, e);
                } finally {
                    synchronized (executingSet) {
                        executingSet.remove(missionDetailsId);
                    }
                }
            });
        }
    }

    @Override
    public List<MissionResponseDto> getAllMissionDtos(Integer userId) {
        List<Mission> missions = missionRepository.findAllByUserId(userId);
        List<MissionArchive> archives = missionArchiveRepository.findAllByUserId(userId);
        List<MissionResponseDto> missionDtos = missions.stream()
                .map(mission -> MissionResponseDto.builder()
                        .missionId(mission.getMissionId())
                        .userId(mission.getUserId())
                        .expectMoney(mission.getExpectMoney())
                        .overflow(mission.getOverflow())
                        .decreasing(mission.getDecreasing())
                        .startTime(mission.getStartTime())
                        .endTime(mission.getEndTime())
                        .status(mission.getStatus())
                        .money(mission.getMoney())
                        .todayMoney(mission.getTodayMoney())
                        .type(mission.getType())
                        .executeTime(mission.getExecuteTime())
                        .languageType(mission.getLanguageType())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
        missionDtos.addAll(archives.stream()
            .map(archive -> MissionResponseDto.builder()
                    .missionId(archive.getMissionId())
                    .userId(archive.getUserId())
                    .expectMoney(archive.getExpectMoney())
                    .overflow(archive.getOverflow())
                    .decreasing(archive.getDecreasing())
                    .startTime(archive.getStartTime())
                    .endTime(archive.getEndTime())
                    .money(archive.getMoney())
                    .type(archive.getType())
                    .archiveTime(archive.getArchiveTime())
                    .languageType(archive.getLanguageType())
                    .build())
            .toList());
        return missionDtos;
    }

    /**
     * 构建空任务详情
     */
    private void buildEmptyMissionDetails(Mission mission) {
        Integer missionId = mission.getMissionId();
        log.info("开始构建missionDetails, missionId = {}, type = 0", missionId);

        BigDecimal expectMoney = mission.getExpectMoney();
        Integer count = missionDetailsArchiveRepository.countTypeZeroByMissionId(missionId);

        if (count >= expectMoney.intValue()) {
            archiveMission(mission);
            return;
        }

        LocalDateTime nextExecuteTime = getNextExecuteTime(mission, BigDecimal.valueOf(count));

        MissionDetails details = MissionDetails.builder()
                .missionId(missionId)
                .userId(mission.getUserId())
                .type(mission.getType())
                .cost(BIGZERO)
                .rate(BIGZERO)
                .money(BIGZERO)
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
        Integer missionId = mission.getMissionId();
        Integer vipType;
        Integer inviterLevel;
        Integer type = mission.getType();
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

        log.info("开始构建missionDetails, missionId = {}, type = {}", missionId, type);
        BigDecimal vipMoney = localVipDetailsRepository.findMoney(vipType, "en");
        // BigDecimal vipMoney = localVipDetailsRepository.findMoney(vipType, languageType);
        if (vipMoney == null || vipMoney.compareTo(BIGZERO) <= 0) {
            log.error("未找到有效的VIP金额, missionId = {}, type = {}", missionId, type);
            return;
        }

        User user = userRepository.findById(mission.getUserId())
                .orElse(null);
        if (user == null) {
            log.error("用户不存在, userId = {}", mission.getUserId());
            return;
        }

        BigDecimal userRate = inviterLevel == 1 ? user.getRate() : user.getTwoRate();
        if (userRate == null || userRate.compareTo(BIGZERO) <= 0) {
            log.error("missionId {} 用户费率不正确", missionId);
            return;
        }

        BigDecimal perExecutionMoney = vipMoney.multiply(userRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal money = Optional.ofNullable(mission.getMoney())
                .orElse(BIGZERO);
        BigDecimal expectMoney = mission.getExpectMoney();
        BigDecimal needMoney = expectMoney.subtract(money);
        BigDecimal overflowFactor = BigDecimal.ONE.add(mission.getOverflow().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal overflowLimit = expectMoney.multiply(overflowFactor);
        
        if (needMoney.compareTo(BIGZERO) <= 0 || money.add(perExecutionMoney).compareTo(overflowLimit) > 0) {
            archiveMission(mission);
            return;
        }

        LocalDateTime nextExecuteTime = getNextExecuteTime(mission, perExecutionMoney);

        MissionDetails details = MissionDetails.builder()
                .missionId(missionId)
                .userId(mission.getUserId())
                .type(mission.getType())
                .cost(vipMoney)
                .rate(userRate)
                .money(perExecutionMoney)
                .executeTime(nextExecuteTime)
                .languageType(mission.getLanguageType())
                .build();
        missionDetailsRepository.save(details);

        mission.setExecuteTime(nextExecuteTime);
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
            buildVipMissionDetails(mission);
        } else if (type == 11 || type == 12) {
            buildSingleMissionDetails(mission);
        } else if (type == 21 || type == 22) {
            buildAllMissionDetails(mission);
        } else {
            log.error("未知的任务类型: {}", type);
        }
    }

    public LocalDateTime getNextExecuteTime(Mission mission, BigDecimal thisExecutionAmount) {

        Integer missionId = mission.getMissionId();
        LocalDateTime startTime = mission.getStartTime();
        LocalDateTime lastExecutionTime = Optional.ofNullable(mission.getExecuteTime())
                .or(() -> Optional.ofNullable(missionDetailsArchiveRepository.findMaxExecuteTimeByMissionId(missionId)))
                .orElse(mission.getStartTime());
        LocalDateTime endTime = mission.getEndTime();
        BigDecimal completedAmount = mission.getMoney() == null ? BIGZERO : mission.getMoney();
        BigDecimal totalAmount = mission.getExpectMoney();
        BigDecimal decreasing = mission.getDecreasing();

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须有效且 totalAmount > 0");
        }

        if (decreasing == null || decreasing.compareTo(BigDecimal.ZERO) <= 0 || decreasing.compareTo(BigDecimal.ONE) >= 0) {
            decreasing = BigDecimal.valueOf(0.6); // 默认前50%时间完成60%
        }

        // 计算两次执行后的进度
        BigDecimal futureCompleted = completedAmount.add(thisExecutionAmount.multiply(BigDecimal.valueOf(2)));
        BigDecimal progress = futureCompleted.divide(totalAmount, 10, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE).max(BigDecimal.ZERO);

        // Beta 分布参数
        double alpha = decreasing.doubleValue() * 2;
        double beta = (1.0 - decreasing.doubleValue()) * 2;
        BetaDistribution betaDistribution = new BetaDistribution(beta, alpha);

        // 基于未来进度计算的时间占比
        double timeRatio = betaDistribution.inverseCumulativeProbability(progress.doubleValue());

        long totalSeconds = Duration.between(startTime, endTime).getSeconds();
        long baseSeconds = (long) (timeRatio * totalSeconds);
        LocalDateTime futureTargetTime = startTime.plusSeconds(baseSeconds);

        // 在 [lastExecutionTime, futureTargetTime] 之间取随机时间
        long secondsRange = Duration.between(lastExecutionTime, futureTargetTime).getSeconds();

        if (secondsRange < 0) {
            // 如果时间倒退或相同，随机加 1～10 秒补偿
            return lastExecutionTime.plusSeconds(ThreadLocalRandom.current().nextInt(0, 11));
        }

        long randomOffset = ThreadLocalRandom.current().nextLong(0, secondsRange + 1); // 包含边界
        LocalDateTime nextExecuteTime = lastExecutionTime.plusSeconds(randomOffset);

        // 边界检查
        if (nextExecuteTime.isAfter(endTime)) {
            return endTime;
        }

        return nextExecuteTime;
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

        // 本地数据库操作（采用本地事务处理）
        try {
            updateLocalData(details, jdbcDto);
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
    public void updateLocalData(MissionDetails details, JdbcDto jdbcDto) {
        details.setInviteeId(jdbcDto.getUserId());
        details.setInviteeName(jdbcDto.getUsername());

        List<MissionDetails> detailsList = jdbcDto.getMissionDetailsList();
        if (detailsList != null) {
            missionDetailsRepository.saveAll(detailsList);
        }

        archiveMissionDetails(details);

        Integer missionId = details.getMissionId();
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("任务不存在, missionId=" + missionId));
        BigDecimal totalMoney = missionDetailsArchiveRepository.sumMoneyByMissionId(missionId);
        BigDecimal todayMoney = missionDetailsArchiveRepository.sumMoneyByMissionIdAndToday(missionId);
        mission.setMoney(totalMoney == null ? BIGZERO : totalMoney);
        mission.setTodayMoney(todayMoney == null ? BIGZERO : todayMoney);
        missionRepository.save(mission);

        checkMission(mission);
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
        Integer missionId = mission.getMissionId();
        missionArchiveRepository.save(MissionArchive.builder()
                .missionId(missionId)
                .userId(mission.getUserId())
                .expectMoney(mission.getExpectMoney())
                .overflow(mission.getOverflow())
                .decreasing(mission.getDecreasing())
                .startTime(mission.getStartTime())
                .endTime(mission.getEndTime())
                .money(mission.getMoney())
                .type(mission.getType())
                .archiveTime(LocalDateTime.now())
                .languageType(mission.getLanguageType())
                .build());
        missionRepository.deleteById(missionId);
        log.info("Mission {} 归档完毕", missionId);
    }

    private void archiveMissionDetails(MissionDetails details) {
        Integer missionDetailsId = details.getMissionDetailsId();
        missionDetailsArchiveRepository.save(MissionDetailsArchive.builder()
                .missionDetailsId(missionDetailsId)
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
                .archiveTime(LocalDateTime.now())
                .languageType(details.getLanguageType())
                .build());
        missionDetailsRepository.deleteById(missionDetailsId);
        log.info("MissionDetails {} 归档完毕", missionDetailsId);
    }

    private void buildSingleMissionDetails(Mission mission) {
        Integer missionId = mission.getMissionId();
        Integer inviterLevel;
        Integer type = mission.getType();
        if (type == 11) {
            inviterLevel = 1;
        } else {
            inviterLevel = 2;
        }

        log.info("开始构建missionDetails, missionId = {}, type = {}", missionId, type);

        User user = userRepository.findById(mission.getUserId())
                .orElse(null);

        if (user == null) {
            log.error("用户不存在, userId = {}", mission.getUserId());
            return;
        }

        BigDecimal userRate = inviterLevel == 1 ? user.getRate() : user.getTwoRate();
        if (userRate == null || userRate.compareTo(BIGZERO) <= 0) {
            log.error("missionId {} 用户费率不正确", missionId);
            return;
        }

        BigDecimal futureMoney = Optional.ofNullable(missionDetailsArchiveRepository.sumMoneyByMissionId(missionId))
                .orElse(BIGZERO);
        BigDecimal pastMoney = Optional.ofNullable(mission.getMoney())
                .orElse(BIGZERO);
        BigDecimal money = futureMoney.add(pastMoney);
        BigDecimal expectMoney = mission.getExpectMoney();
        BigDecimal needMoney = expectMoney.subtract(money);

        if (needMoney.compareTo(BIGZERO) <= 0) {
            archiveMission(mission);
            return;
        }

        BigDecimal overflowFactor = BigDecimal.ONE.add(mission.getOverflow().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal overflowLimit = expectMoney.multiply(overflowFactor);

        List<BigDecimal> prices = localCourseDetailsRepository.findDistinctValidPrices();

        if (prices.isEmpty()) {
            log.error("未找到有效价格, missionId = {}, type = {}", missionId, type);
            return;
        }

        Collections.shuffle(prices);
        BigDecimal selectedPrice = null;
        BigDecimal perExecutionMoney = null;
        Integer continuous = 1;
        for (BigDecimal price : prices) {
            if (Math.random() > 0.5) {
                continuous = ThreadLocalRandom.current().nextInt(2, 21);
            }

            perExecutionMoney = price.multiply(userRate).setScale(2, RoundingMode.HALF_UP);

            if (perExecutionMoney.compareTo(BIGZERO) <= 0) {
                continue;
            }

            for (int i = continuous; i >= 1; i--) {
                BigDecimal tempMoney = perExecutionMoney.multiply(BigDecimal.valueOf(i));
                if (money.add(tempMoney).compareTo(overflowLimit) <= 0) {
                    selectedPrice = price;
                    continuous = i;
                    break;
                }
            }
        }

        if (selectedPrice == null || perExecutionMoney == null || perExecutionMoney.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("未找到合适的价格, missionId = {}, type = {}", mission.getMissionId(), type);
            archiveMission(mission);
            return;
        }

        LocalDateTime nextExecuteTime = getNextExecuteTime(mission, perExecutionMoney.multiply(BigDecimal.valueOf(continuous)));

        MissionDetails details = MissionDetails.builder()
                .missionId(mission.getMissionId())
                .userId(mission.getUserId())
                .type(mission.getType())
                .cost(selectedPrice)
                .rate(userRate)
                .money(perExecutionMoney)
                .continuous(continuous)
                .executeTime(nextExecuteTime)
                .languageType(mission.getLanguageType())
                .build();
        missionDetailsRepository.save(details);

        mission.setExecuteTime(nextExecuteTime);
        missionRepository.save(mission);
    }

    private void buildAllMissionDetails(Mission mission) {
        Integer missionId = mission.getMissionId();
        Integer inviterLevel;
        Integer type = mission.getType();
        if (type == 21) {
            inviterLevel = 1;
        } else {
            inviterLevel = 2;
        }

        log.info("开始构建missionDetails, missionId = {}, type = {}", missionId, type);

        User user = userRepository.findById(mission.getUserId())
                .orElse(null);
        if (user == null) {
            log.error("用户不存在, userId = {}", mission.getUserId());
            return;
        }

        BigDecimal userRate = inviterLevel == 1 ? user.getRate() : user.getTwoRate();
        if (userRate == null || userRate.compareTo(BIGZERO) <= 0) {
            log.error("missionId {} 用户费率不正确", missionId);
            return;
        }

        BigDecimal money = mission.getMoney() == null ? BIGZERO : mission.getMoney();
        BigDecimal expectMoney = mission.getExpectMoney();
        BigDecimal needMoney = expectMoney.subtract(money);
        BigDecimal overflowFactor = BigDecimal.ONE.add(mission.getOverflow().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal overflowLimit = expectMoney.multiply(overflowFactor);

        List<BigDecimal> prices = localCourseRepository.findDistinctValidPrices();
        if (prices.isEmpty()) {
            log.error("未找到有效价格, missionId = {}, type = {}", missionId, type);
            return;
        }
        Collections.shuffle(prices);

        BigDecimal selectedPrice = null;
        BigDecimal perExecutionMoney = null;

        for (BigDecimal price : prices) {
            perExecutionMoney = price.multiply(userRate).setScale(2, RoundingMode.HALF_UP);

            if (perExecutionMoney.compareTo(BIGZERO) <= 0) {
                continue;
            }
        
            if (needMoney.compareTo(BIGZERO) > 0 &&
                money.add(perExecutionMoney).compareTo(overflowLimit) <= 0) {
                selectedPrice = price;
                break;
            }
        }

        if (selectedPrice == null || perExecutionMoney == null) {
            log.error("未找到合适的价格, missionId = {}, type = {}", missionId, type);
            archiveMission(mission);
            return;
        }

        LocalDateTime nextExecuteTime = getNextExecuteTime(mission, perExecutionMoney);

        MissionDetails details = MissionDetails.builder()
                .missionId(missionId)
                .userId(mission.getUserId())
                .type(mission.getType())
                .cost(selectedPrice)
                .rate(userRate)
                .money(perExecutionMoney)
                .executeTime(nextExecuteTime)
                .languageType(mission.getLanguageType())
                .build();
        missionDetailsRepository.save(details);

        mission.setExecuteTime(nextExecuteTime);
        missionRepository.save(mission);
    }
}
