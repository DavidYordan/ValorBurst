package com.valorburst.model.remote;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "invite_money") // 对应表名
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteMoney {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "money_sum")
    private BigDecimal moneySum;

    @Column(name = "money")
    private BigDecimal money;

    @Column(name = "cash_out")
    private BigDecimal cashOut;
}