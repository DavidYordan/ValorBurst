package com.valorburst.repository.local;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.valorburst.dto.MainDataDto;
import com.valorburst.model.local.Mission;
import com.valorburst.model.local.User;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    @Override
    public List<MainDataDto> findAllMainData() {
        
        List<User> users = userRepository.findAll();

        List<Integer> userIds = users.stream()
                .map(User::getUserId)
                .toList();

        List<Mission> missions = missionRepository.findAllActiveByUserIds(userIds);
        return buildMainDataDtoList(users, missions);
    }

    private List<MainDataDto> buildMainDataDtoList(List<User> users, List<Mission> allMissions) {
        // 1. 将Mission按userId分组
        Map<Integer, List<Mission>> missionMap = allMissions.stream()
                .collect(Collectors.groupingBy(Mission::getUserId));

        // 2. 遍历每个用户，构建MainDataDto
        return users.stream().map(user -> {
            List<Mission> missions = missionMap.getOrDefault(user.getUserId(), Collections.emptyList());
    
            BigDecimal remainMoney = BigDecimal.ZERO;
            LocalDateTime remainTime = null;
    
            Mission nextMission = null;
            LocalDateTime earliestExecuteTime = null;
    
            for (Mission mission : missions) {
                // remainMoney = ∑(expectMoney - money)
                BigDecimal expect = mission.getExpectMoney() != null ? mission.getExpectMoney() : BigDecimal.ZERO;
                BigDecimal actual = mission.getMoney() != null ? mission.getMoney() : BigDecimal.ZERO;
                remainMoney = remainMoney.add(expect.subtract(actual));
    
                // remainTime = max(endTime)
                LocalDateTime endTime = mission.getEndTime();
                if (endTime != null && (remainTime == null || endTime.isAfter(remainTime))) {
                    remainTime = endTime;
                }
    
                // nextMission = mission with earliest executeTime
                LocalDateTime executeTime = mission.getExecuteTime();
                if (executeTime != null && (earliestExecuteTime == null || executeTime.isBefore(earliestExecuteTime))) {
                    earliestExecuteTime = executeTime;
                    nextMission = mission;
                }
            }

            return MainDataDtoBuilder(user)
                    .nextMoney(nextMission != null ? nextMission.getMoney() : null)
                    .nextTime(nextMission != null ? nextMission.getExecuteTime() : null)
                    .remainMoney(remainMoney)
                    .remainTime(remainTime)
                    .build();
        }).collect(Collectors.toList());
    }

    private MainDataDto.MainDataDtoBuilder MainDataDtoBuilder(User user) {
        return MainDataDto.builder()
                .userId(user.getUserId())
                .team(user.getTeam())
                .region(user.getRegion())
                .userName(user.getUserName())
                .phone(user.getPhone())
                .emailName(user.getEmailName())
                .platform(user.getPlatform())
                .invitationCode(user.getInvitationCode())
                .inviterCode(user.getInviterCode())
                .rate(user.getRate())
                .twoRate(user.getTwoRate())
                .moneySum(user.getMoneySum())
                .money(user.getMoney())
                .cashOut(user.getCashOut())
                .cashOutStay(user.getCashOutStay())
                .moneyWallet(user.getMoneyWallet());
    }
}