package com.valorburst.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PircesResponseDto {
    BigDecimal type1;
    BigDecimal type2;
    BigDecimal type3;
    BigDecimal type4;
    BigDecimal type5;
    BigDecimal type6;
    BigDecimal type7;
    BigDecimal type8;
    List<BigDecimal> type11;
    List<BigDecimal> type12;
    List<BigDecimal> type21;
    List<BigDecimal> type22;
}
