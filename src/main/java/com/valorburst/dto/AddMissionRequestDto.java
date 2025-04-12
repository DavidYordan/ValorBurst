package com.valorburst.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMissionRequestDto {

    private Integer userId;
    private String team;
    private String region;
    private String languageType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 0: 无消费
    // 1: vip折扣1, 2: vip月1, 3: vip季1, 4: vip年1,
    // 5: vip折扣2, 6: vip月2, 7: vip季2, 8: vip年2,
    // 11: 单集1, 12: 单集2, 21: 全集1, 22: 全集2
    private List<TypeDto> typeDtos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypeDto {
        private Integer type;
        private BigDecimal overflow;
        private BigDecimal decreasing;
        private BigDecimal expectMoney;
    }
}
