package com.valorburst.dto;

import java.util.List;

import com.valorburst.model.local.MissionDetails;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JdbcDto {
    private String sql;
    private Integer userId;
    private String username;
    List<MissionDetails> missionDetailsList;
}
