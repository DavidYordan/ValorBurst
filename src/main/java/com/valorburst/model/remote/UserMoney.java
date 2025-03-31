package com.valorburst.model.remote;

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
    @Column(name = "id")
    private Integer id;

    @Column(name = "money")
    private Double money;

    @Column(name = "user_id")
    private Integer userId;
}
