package com.valorburst.model.local;

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
    private Double rate;

    @Column(name = "two_rate")
    private Double twoRate;

    @Column(name = "money_sum")
    private Double moneySum;

    @Column(name = "money")
    private Double money;

    @Column(name = "cash_out")
    private Double cashOut;

    @Column(name = "cash_out_stay")
    private Double cashOutStay;

    @Column(name = "money_wallet")
    private Double moneyWallet;
}
