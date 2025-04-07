package com.valorburst.model.remote;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_money")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMoney {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "money")
    private BigDecimal money;

    @Column(name = "user_id")
    private Integer userId;
}
