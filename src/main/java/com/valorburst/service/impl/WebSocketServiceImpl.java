package com.valorburst.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.valorburst.dto.MissionResponseDto;
import com.valorburst.dto.PircesResponseDto;
import com.valorburst.dto.UserResponseDto;
import com.valorburst.model.local.User;
import com.valorburst.service.MissionService;
import com.valorburst.service.PircesService;
import com.valorburst.service.UserService;
import com.valorburst.service.WebSocketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final MissionService missionService;
    private final PircesService pircesService;
    private final UserService userService;

    @Override
    public List<UserResponseDto> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return users.stream()
                .map(this::mapUserToDto)
                .toList();
    }

    @Override
    public List<MissionResponseDto> getAllMissions() {
        return missionService.getAllMissionDtos();
    }

    @Override
    public List<MissionResponseDto> getMissions(Integer userId) {
        return missionService.getMissionDtos(userId);
    }

    @Override
    public PircesResponseDto getPrices() {
        return pircesService.getPrices();
    }

    @Override
    public UserResponseDto updateUserByInput(String input) {
        Optional<User> user = userService.updateUserByInput(input);
        if (user.isPresent()) {
            return mapUserToDto(user.get());
        } else {
            return null;
        }
    }
    
    private UserResponseDto mapUserToDto(User user) {
        return UserResponseDto.builder()
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
                .moneyWallet(user.getMoneyWallet())
                .build();
    }
}
