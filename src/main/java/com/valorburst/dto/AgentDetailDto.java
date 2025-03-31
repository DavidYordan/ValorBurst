package com.valorburst.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentDetailDto {
    private int agentId;
    private String agentName;
    private String myBusinessId;
    private int totalVideoCount;
    private int validVideoCount;
    private int candidatedVideoCount;
    private double candidatedReward;
    private int unCandidateVideoCount;
    private double unCandidateReward;

    public double getMixCandidateReward() {
        return (candidatedReward == 0.0 ? 0.0 : candidatedReward) + 
               (unCandidateReward == 0.0 ? 0.0 : unCandidateReward);
    }
}
