package com.valorburst.model.remote;

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
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "money_sum")
    private Double moneySum;

    @Column(name = "money")
    private Double money;

    @Column(name = "cash_out")
    private Double cashOut;
}