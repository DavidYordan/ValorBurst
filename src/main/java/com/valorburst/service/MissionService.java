package com.valorburst.service;

import java.util.List;

import com.valorburst.dto.AddMissionRequestDto;
import com.valorburst.dto.MissionResponseDto;

public interface MissionService {

    void addMissions(AddMissionRequestDto addMissionRequestDto);

    void checkTimeoutMissions();

    void checkMissions();
    
    void executeDueMissions();

    List<MissionResponseDto> getAllMissionDtos(Integer userId);
}
