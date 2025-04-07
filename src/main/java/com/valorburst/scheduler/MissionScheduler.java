package com.valorburst.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.valorburst.service.MissionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionScheduler {

    private final MissionService missionService;

    @Scheduled(fixedDelay = 60 * 1000)
    public void schedulerGenerate() {
        missionService.generateInitMissions();
    }

    @Scheduled(fixedDelay = 1 * 1000)
    public void schedulerExecute() {
        missionService.executeDueMissions();
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void schedulerCheckTimeout() {
        missionService.checkTimeoutMissions();
    }
}
