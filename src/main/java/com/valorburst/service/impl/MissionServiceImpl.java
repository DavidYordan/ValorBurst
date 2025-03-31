package com.valorburst.service.impl;

import com.valorburst.config.AppProperties;
import com.valorburst.model.local.Mission;
import com.valorburst.model.local.MissionDetails;
import com.valorburst.model.local.MissionDetailsArchive;
import com.valorburst.model.local.User;
import com.valorburst.repository.local.LocalVipDetailsRepository;
import com.valorburst.repository.local.MissionDetailsArchiveRepository;
import com.valorburst.repository.local.MissionDetailsRepository;
import com.valorburst.repository.local.MissionRepository;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionServiceImpl implements MissionService {

    private final LocalVipDetailsRepository localVipDetailsRepository;
    private final MissionRepository missionRepository;
    private final MissionDetailsRepository missionDetailsRepository;
    private final MissionDetailsArchiveRepository missionDetailsArchiveRepository;
    private final UserRepository userRepository;

    private final DedupingExecutor dedupingExecutor;
    private final JdbcHelper jdbcHelper;

    private final AppProperties appProperties;
    private final TelegramBotService telegramBotService;

    public void checkTimeoutMissions() {
        dedupingExecutor.execute(() -> {
            List<MissionDetails> timeoutDetails = missionDetailsRepository.findByExecuteTimeLessThan(LocalDateTime.now().minusMinutes(30));
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
            if (mission.getType() >= 1 && mission.getType() <= 8) {
                dedupingExecutor.execute(() -> buildVipMissionDetails(mission), "build-vip-mission-" + mission.getMissionId());
            } else {
                log.info("跳过非vip任务，missionId={}，任务类型={}", mission.getMissionId(), mission.getType());
            }
        }
    }

    /**
     * 扫描当前时间大于等于 mission_details.executeTime 的任务，执行之
     * 为了并发执行，这里将每个任务丢到线程池
     */
    public void executeDueMissions() {
        LocalDateTime now = LocalDateTime.now();
        // 1. 查询所有 mission_details.executeTime <= now 的记录
        List<MissionDetails> dueDetails = missionDetailsRepository.findByExecuteTimeLessThanEqual(now);

        // 2. 逐条分发到线程池
        for (MissionDetails details : dueDetails) {
            Integer missionId = details.getMissionId();
            dedupingExecutor.execute(() -> {
                try {
                    doExecuteMissionDetails(details);
                } catch (Exception e) {
                    log.error("executeDueMissionDetails failed, missionDetailsId={}", missionId, e);
                }
            }, "execute-due-mission-" + missionId);
        }
    }

    private void buildVipMissionDetails(Mission mission) {
        Integer vipType;
        Integer inviterLevel;
        Integer type = mission.getType();
        String languageType = mission.getLanguageType();
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
        Double vipMoney = Optional.ofNullable(localVipDetailsRepository.findMoney(vipType, languageType))
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

        Double userRate = inviterLevel == 1 ? user.getRate() : user.getTwoRate();
        if (userRate == null || userRate <= 0) {
            log.error("missionId {} 用户费率不正确", mission.getMissionId());
            return;
        }

        Double perExecutionMoney = vipMoney * userRate;
        Double needMoney = mission.getExpectMoney() - mission.getMoney();
        if (needMoney <= 0 || perExecutionMoney > needMoney * 1.05) {
            archiveMission(mission);
            return;
        }

        Integer remainingExecutions = (int) Math.ceil(needMoney / perExecutionMoney);
        if (remainingExecutions <= 0) {
            archiveMission(mission);
            return;
        }

        LocalDateTime exeCuteTime = mission.getExecuteTime();
        LocalDateTime nextExecuteTime;
        if (exeCuteTime == null) {
            exeCuteTime = mission.getStartTime();
        }
        if (exeCuteTime.isAfter(mission.getEndTime())) {
            nextExecuteTime = LocalDateTime.now();
        } else {
            nextExecuteTime = getNextExecuteTime(exeCuteTime, mission.getEndTime(), remainingExecutions);
        }

        MissionDetails details = MissionDetails.builder()
                .missionId(mission.getMissionId())
                .userId(mission.getUserId())
                .type(mission.getType())
                .cost(vipMoney)
                .rate(userRate)
                .money(perExecutionMoney)
                .executeTime(nextExecuteTime)
                .languageType(languageType)
                .build();
        missionDetailsRepository.save(details);

        mission.setExecuteTime(details.getExecuteTime());
        missionRepository.save(mission);
    }

    private void generateNextMissions(Integer missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElse(null);
        if (mission == null) {
            log.error("missionId {} 不存在", missionId);
            return;
        }

        if (mission.getType() >= 1 && mission.getType() <= 8) {
            dedupingExecutor.execute(() -> buildVipMissionDetails(mission), "build-vip-mission-" + mission.getMissionId());
        } else {
            log.info("跳过非vip任务，missionId={}，任务类型={}", mission.getMissionId(), mission.getType());
        }
    }

    private LocalDateTime getNextExecuteTime(LocalDateTime startTime, LocalDateTime endTime, Integer remainingExecutions) {
        Long durationSec = Duration.between(startTime, endTime).getSeconds();
        Double u = Math.random();
        Double min = 1 - Math.pow(1 - u, 1.0 / remainingExecutions);
        return startTime.plusSeconds((long) (min * durationSec));
    }

    /**
     * 真正执行任务的函数，假设要对远程数据库进行读写
     */
    private void doExecuteMissionDetails(MissionDetails details) {
        // 1. 进行远程数据库读写 (伪代码)
        String sql = MissionDetailsBuilder.buildJdbc(details.getType(), details);
        jdbcHelper.executeInTransaction(sql);

        // 2. 把 mission_details 归档到 mission_details_archive（可交给一个归档Service处理）
        archiveMissionDetails(details);

        // 3. 计算并生成下一次 mission_details（如果有业务需求的话）
        generateNextMissions(details.getMissionId());

        log.info("MissionDetails {} 执行完毕", details.getMissionDetailsId());
    }

    private void archiveMission(Mission mission) {
        // 归档到 mission_archive
        // 1. 构造 MissionArchive

        // 2. 保存到归档表
        // 3. 删除或做标识
    }

    private void archiveMissionDetails(MissionDetails details) {
        MissionDetailsArchive archive = MissionDetailsArchive.builder()
                .missionDetailsId(details.getMissionDetailsId())
                .missionId(details.getMissionId())
                .userId(details.getUserId())
                .type(details.getType())
                .cost(details.getCost())
                .rate(details.getRate())
                .money(details.getMoney())
                .archiveTime(LocalDateTime.now())
                .languageType(details.getLanguageType())
                .build();

        missionDetailsArchiveRepository.save(archive);

        missionDetailsRepository.deleteById(details.getMissionDetailsId());
    }

    private List<MissionDetails> buildSingleMissionDetails(Mission mission) {
        // 1. 根据 type 和 languageType 查询单集价格
        // 2. 计算收益
        // 3. 构造 MissionDetails
        // 4. 保存到 mission_details
        return List.of();
    }

    private List<MissionDetails> buildAllMissionDetails(Mission mission) {
        // 1. 根据 type 和 languageType 查询全集价格
        // 2. 计算收益
        // 3. 构造 MissionDetails
        // 4. 保存到 mission_details
        return List.of();
    }
}