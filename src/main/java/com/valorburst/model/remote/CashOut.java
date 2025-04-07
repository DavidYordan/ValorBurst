package com.valorburst.model.remote;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cash_out")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashOut {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "create_at")
    private String createAt;

    @Column(name = "is_out")
    private Boolean isOut;

    @Column(name = "money")
    private String money;

    @Column(name = "out_at")
    private String outAt;

    @Column(name = "relation_id")
    private String relationId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "zhifubao")
    private String zhifubao;

    @Column(name = "zhifubao_name")
    private String zhifubaoName;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "state")
    private Integer state;

    @Column(name = "refund")
    private String refund;

    @Column(name = "classify")
    private Integer classify;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "recipient")
    private String recipient;

    @Column(name = "bank_number")
    private String bankNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_address")
    private String bankAddress;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "type")
    private Integer type;
}
