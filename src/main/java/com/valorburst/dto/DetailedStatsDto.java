package com.valorburst.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailedStatsDto {
    private int totalVideos;
    private int totalAgents;
    private int validVideos;
    private int validAgents;
    private double totalReward;
    private List<AgentDetailDto> agentDetails;
}
