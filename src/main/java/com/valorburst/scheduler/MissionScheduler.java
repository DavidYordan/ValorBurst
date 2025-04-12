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
        try {
            missionService.checkMissions();
        } catch (Exception e) {
            log.error("Error in schedulerGenerate: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 1 * 1000)
    public void schedulerExecute() {
        try {
            missionService.executeDueMissions();
        } catch (Exception e) {
            log.error("Error in schedulerExecute: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void schedulerCheckTimeout() {
        try {
            missionService.checkTimeoutMissions();
        } catch (Exception e) {
            log.error("Error in schedulerCheckTimeout: {}", e.getMessage(), e);
        }
    }
}
