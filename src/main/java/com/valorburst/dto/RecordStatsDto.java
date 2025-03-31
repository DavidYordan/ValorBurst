package com.valorburst.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordStatsDto {
    private LocalDate fabuDate;
    private int totalVideos;
    private int totalAgents;
    private int validVideos;
    private int validAgents;
    private int candidatedVideos;
    private int candidatedAgents;
    private int unCandidateVideos;
    private int unCandidateAgents;
    private double candidatedReward;
    private double unCandidateReward;
    private List<AgentDetailDto> agentDetails;
}
