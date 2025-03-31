package com.valorburst.model.remote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbUser {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email_name")
    private String emailName;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "sex")
    private Integer sex;

    @Column(name = "open_id")
    private String openId;

    @Column(name = "wx_open_id")
    private String wxOpenId;

    @Column(name = "password")
    private String password;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "apple_id")
    private String appleId;

    @Column(name = "sys_phone")
    private Integer sysPhone;

    @Column(name = "status")
    private Integer status;

    @Column(name = "platform")
    private String platform;

    @Column(name = "jifen")
    private Integer jifen;

    @Column(name = "invitation_code")
    private String invitationCode;

    @Column(name = "inviter_code")
    private String inviterCode;

    @Column(name = "clientid")
    private String clientId;

    @Column(name = "zhi_fu_bao_name")
    private String zhiFuBaoName;

    @Column(name = "zhi_fu_bao")
    private String zhiFuBao;

    @Column(name = "wx_id")
    private String wxId;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "two_rate")
    private BigDecimal twoRate;

    @Column(name = "on_line_time")
    private LocalDateTime onLineTime;

    @Column(name = "recipient")
    private String recipient;

    @Column(name = "bank_number")
    private String bankNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_address")
    private String bankAddress;

    @Column(name = "qd_code")
    private String qdCode;

    @Column(name = "google_open_id")
    private String googleOpenId;

    @Column(name = "request_number")
    private String requestNumber;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "is_qd_rate")
    private Integer isQdRate;

    @Column(name = "ban_invitations")
    private Boolean banInvitations;

    @Column(name = "fake")
    private Boolean fake;
}
