package com.valorburst.model.local;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "team")
    private String team;

    @Column(name = "region")
    private String region;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email_name")
    private String emailName;

    @Column(name = "platform")
    private String platform;

    @Column(name = "invitation_code")
    private String invitationCode;

    @Column(name = "inviter_code")
    private String inviterCode;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "two_rate")
    private BigDecimal twoRate;

    @Column(name = "money_sum")
    private BigDecimal moneySum;

    @Column(name = "money")
    private BigDecimal money;

    @Column(name = "cash_out")
    private BigDecimal cashOut;

    @Column(name = "cash_out_stay")
    private BigDecimal cashOutStay;

    @Column(name = "money_wallet")
    private BigDecimal moneyWallet;
}
