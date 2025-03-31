package com.valorburst.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyStatsSummaryDto {
    private int totalVideos;
    private int totalValidVideos;
    private int totalAgents;
    private int totalValidAgents;
    private int totalCandidatedVideos;
    private int totalCandidatedAgents;
    private int totalUnCandidateVideos;
    private int totalUnCandidateAgents;
    private double totalCandidatedReward;
    private double totalUnCandidateReward;
    private List<RecordStatsDto> dailyStats;
}
