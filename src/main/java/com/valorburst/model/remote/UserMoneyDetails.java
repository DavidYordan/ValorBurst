package com.valorburst.model.remote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_money_details") // 对应表名
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMoneyDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "by_user_id")
    private Integer byUserId;

    @Column(name = "title")
    private String title;

    @Column(name = "classify")
    private Integer classify;

    @Column(name = "type")
    private Integer type;

    @Column(name = "state")
    private Integer state;

    @Column(name = "state_delete")
    private Integer stateDelete;

    @Column(name = "money")
    private BigDecimal money;

    @Column(name = "content")
    private String content;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "language_type")
    private String languageType;

    @Column(name = "buy_type")
    private Integer buyType;
}
