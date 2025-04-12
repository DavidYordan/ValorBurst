package com.valorburst.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Integer userId;
    private String team;
    private String region;
    private String userName;
    private String phone;
    private String emailName;
    private String platform;
    private String invitationCode;
    private String inviterCode;
    private BigDecimal rate;
    private BigDecimal twoRate;
    private BigDecimal moneySum;
    private BigDecimal money;
    private BigDecimal cashOut;
    private BigDecimal cashOutStay;
    private BigDecimal moneyWallet;
}
