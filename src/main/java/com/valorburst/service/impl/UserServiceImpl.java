package com.valorburst.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.valorburst.dto.DashboardDto;
import com.valorburst.dto.MainDataDto;
import com.valorburst.model.local.Mission;
import com.valorburst.model.local.MissionArchive;
import com.valorburst.model.local.MissionDetailsCompensation;
import com.valorburst.model.local.User;
import com.valorburst.model.remote.projection.UserRemoteProjection;
import com.valorburst.repository.local.MissionArchiveRepository;
import com.valorburst.repository.local.MissionDetailsCompensationRepository;
import com.valorburst.repository.local.MissionRepository;
import com.valorburst.repository.local.UserRepository;
import com.valorburst.repository.remote.TbUserRepository;
import com.valorburst.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private static final int BATCHSIZE = 100;
    private static final BigDecimal THRESHOLD = new BigDecimal(1000);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final MissionRepository missionRepository;
    private final MissionArchiveRepository missionArchiveRepository;
    private final MissionDetailsCompensationRepository missionDetailsCompensationRepository;
    private final UserRepository userRepository;
    private final TbUserRepository tbUserRepository;

    @Override
    public void syncAllUsers() {
        List<Integer> userIds = userRepository.fetchAllUserIds();
        for (int i = 0; i < userIds.size(); i += BATCHSIZE) {
            int end = Math.min(i + BATCHSIZE, userIds.size());
            List<Integer> batchUserIds = userIds.subList(i, end);
            List<User> users = tbUserRepository.findProjectedByUserIds(batchUserIds)
                    .stream()
                    .map(this::mapToLocal)
                    .toList();
            userRepository.saveAll(users);
        }
    }

    @Override
    public Map<String, DashboardDto> getDashboardByRegion() {
        LocalDateTime now = LocalDateTime.now();

        List<User> allUsers = userRepository.findAll();
        Map<String, List<Integer>> usersByRegion = allUsers.stream()
            .collect(Collectors.groupingBy(
                User::getRegion,
                Collectors.mapping(User::getUserId, Collectors.toList())
            ));

        List<Mission> allMissions = missionRepository.findAll();
        List<MissionArchive> allArchives = missionArchiveRepository.findAll();
        List<MissionDetailsCompensation> allCompensations = missionDetailsCompensationRepository.findAll();

        Map<String, DashboardDto> result = new HashMap<>();
        for (var entry : usersByRegion.entrySet()) {
            String region = entry.getKey();
            Set<Integer> uids = new HashSet<>(entry.getValue());

            // 3. 筛出本 region 的三类记录
            List<Mission> regionMissions = allMissions.stream()
                .filter(m -> uids.contains(m.getUserId()))
                .toList();
            List<MissionArchive> regionArchives = allArchives.stream()
                .filter(a -> uids.contains(a.getUserId()))
                .toList();
            List<MissionDetailsCompensation> regionComps = allCompensations.stream()
                .filter(c -> uids.contains(c.getUserId()))
                .toList();

            // 4. 用 Accumulator 一次遍历算出所有指标
            StatsAccumulator acc = new StatsAccumulator(now, THRESHOLD);
            regionMissions.forEach(acc::acceptMission);

            // 5. 填充 DTO
            DashboardDto dto = DashboardDto.builder()
                // 用户统计
                .totalUserCount(uids.size())
                .activeUserCount(acc.activeUserCount())
                .inactiveUserCount(uids.size() - acc.activeUserCount())

                // 任务汇总
                .archiveMissionCount(regionArchives.size())
                .totalMissionCount(regionArchives.size() + regionMissions.size())
                .activeMissionCount(acc.activeMissionCount())
                .pauseMissionCount(acc.pauseMissionCount())

                // 收益
                .totalMoney(acc.getTotalMoney())
                .totalTodayMoney(acc.getTotalTodayMoney())

                // 任务分布
                .missionsByType(acc.getMissionsByType())

                // 补偿
                .compensationCount(regionComps.size())

                // 预警
                .overdueMissionCount(acc.getOverdueMissionCount())
                .usersOverTodayThreshold(acc.getUsersOverTodayThreshold())
                .usersOverExpectDailyThreshold(acc.getUsersOverExpectDailyThreshold())
                .build();

            result.put(region, dto);
        }

        return result;
    }

    private static class StatsAccumulator {
        private final LocalDateTime now;
        private final BigDecimal todayThreshold;
        private final BigDecimal expectDailyThreshold;

        // 任务层面
        private BigDecimal totalMoney      = ZERO;
        private BigDecimal totalTodayMoney = ZERO;
        private long activeMissionCount    = 0;
        private long pauseMissionCount     = 0;
        private long overdueMissionCount   = 0;

        private final Map<Integer, Long> missionsByType = new HashMap<>();

        // 用户层面 (用于阈值统计)
        private final Map<Integer, BigDecimal> dailyExpectByUser = new HashMap<>();
        private final Set<Integer> usersOverTodayThreshold     = new HashSet<>();

        public StatsAccumulator(LocalDateTime now, BigDecimal THRESHOLD) {
            this.now = now;
            this.todayThreshold = THRESHOLD;
            this.expectDailyThreshold = THRESHOLD;
        }

        /** 单条任务来的时候调用 **/
        public void acceptMission(Mission m) {
            // —— 1. 任务状态 & 统计
            if (m.getStatus()) activeMissionCount++;
            else                pauseMissionCount++;

            // —— 2. 金额 & 过期
            totalMoney      = totalMoney.add(m.getMoney());
            totalTodayMoney = totalTodayMoney.add(m.getTodayMoney());
            if (m.getExecuteTime().isBefore(now)) overdueMissionCount++;

            // —— 3. 单用户今日阈值，只要有一条超就算这个用户
            if (m.getTodayMoney().compareTo(todayThreshold) > 0) {
                usersOverTodayThreshold.add(m.getUserId());
            }

            // —— 4. missionsByType
            missionsByType.merge(m.getType(), 1L, Long::sum);

            // —— 5. 日均预期 (<expectMoney> ÷ 天数)，先累加到每个用户
            long days = Duration.between(m.getStartTime(), m.getEndTime()).toDays() + 1;
            BigDecimal daily = m.getExpectMoney()
                                 .divide(new BigDecimal(days),  2, RoundingMode.HALF_UP);
            dailyExpectByUser.merge(m.getUserId(), daily, BigDecimal::add);
        }

        // —— 下面是取值方法 —— //
        public long activeUserCount() {
            // 用户数需在外部根据 region 用户集合来算，所以这里不重算
            // 这里只提供 activeMissionCount 之类
            throw new UnsupportedOperationException();
        }
        public long activeMissionCount() { return activeMissionCount; }
        public long pauseMissionCount()  { return pauseMissionCount; }
        public BigDecimal getTotalMoney()      { return totalMoney; }
        public BigDecimal getTotalTodayMoney() { return totalTodayMoney; }
        public long getOverdueMissionCount()   { return overdueMissionCount; }
        public Map<Integer, Long> getMissionsByType() {
            return missionsByType;
        }
        public long getUsersOverTodayThreshold() {
            return usersOverTodayThreshold.size();
        }
        public long getUsersOverExpectDailyThreshold() {
            return dailyExpectByUser.values().stream()
                    .filter(sum -> sum.compareTo(expectDailyThreshold) > 0)
                    .count();
        }
    }

    // 获取所有用户
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> updateUserByInput(String input) {
        Optional<UserRemoteProjection> userOpt = Optional.empty();
        if (input.startsWith("+")) {
            userOpt = tbUserRepository.findProjectedByPhone(input);
        } else if (input.contains("@")) {
            userOpt = tbUserRepository.findProjectedByEmail(input);
        } else if (input.length() == 6) {
            userOpt = tbUserRepository.findProjectedByInvitationCode(input);
        }

        if (userOpt.isPresent()) {
            UserRemoteProjection user = userOpt.get();
            User localUser = mapToLocal(user);
            userRepository.save(localUser);
            return Optional.of(localUser);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public List<MainDataDto> getAllMainData() {
        return userRepository.findAllMainData();
    }

    private User mapToLocal(UserRemoteProjection p) {
        BigDecimal bigZero = BigDecimal.ZERO;
        return User.builder()
                .userId(p.getUserId())
                .userName(p.getUserName())
                .phone(p.getPhone())
                .emailName(p.getEmailName())
                .platform(p.getPlatform())
                .invitationCode(p.getInvitationCode())
                .inviterCode(p.getInviterCode())
                .rate(p.getRate() != null ? p.getRate() : bigZero)
                .twoRate(p.getTwoRate() != null ? p.getTwoRate() : bigZero)
                .moneySum(p.getMoneySum() != null ? p.getMoneySum() : bigZero)
                .money(p.getMoney() != null ? p.getMoney() : bigZero)
                .cashOut(p.getCashOut() != null ? p.getCashOut() : bigZero)
                .cashOutStay(p.getCashOutStay() != null ? p.getCashOutStay() : bigZero)
                .moneyWallet(p.getMoneyWallet() != null ? p.getMoneyWallet() : bigZero)
                .build();
    }
}
