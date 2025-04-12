package com.valorburst.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionResponseDto {

    private Integer missionId;
    private Integer userId;
    private BigDecimal expectMoney;
    private BigDecimal overflow;
    private BigDecimal decreasing;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean status;
    private BigDecimal money;
    private BigDecimal todayMoney;
    private Integer type;
    private LocalDateTime executeTime;
    private LocalDateTime archiveTime;
    private String languageType;
}
