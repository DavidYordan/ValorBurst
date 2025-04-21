package com.valorburst.service;

import java.util.List;

import com.valorburst.dto.MissionResponseDto;
import com.valorburst.dto.PircesResponseDto;
import com.valorburst.dto.UserResponseDto;

public interface WebSocketService {
    List<UserResponseDto> getAllUsers();
    List<MissionResponseDto> getAllMissions();
    List<MissionResponseDto> getMissions(Integer userId);
    PircesResponseDto getPrices();
    UserResponseDto updateUserByInput(String input);
}
