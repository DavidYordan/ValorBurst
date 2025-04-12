package com.valorburst.model.local;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mission")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_id")
    private Integer missionId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "expect_money") // 预期收益
    private BigDecimal expectMoney;

    @Column(name = "overflow") // 允许溢出
    private BigDecimal overflow;

    @Column(name = "decreasing") // 递减值
    private BigDecimal decreasing;

    @Column(name = "start_time")
    private LocalDateTime startTime; // 开始时间

    @Column(name = "end_time") // 结束时间
    private LocalDateTime endTime;

    @Column(name = "status") // 0:暂停中 1:进行中
    private Boolean status;

    @Column(name = "money") // 已获得收益
    private BigDecimal money;

    @Column(name = "today_money") // 今日收益
    private BigDecimal todayMoney;

    // 1: vip折扣1, 2: vip月1, 3: vip季1, 4: vip年1,
    // 5: vip折扣2, 6: vip月2, 7: vip季2, 8: vip年2,
    // 11: 单集1, 12: 单集2, 21: 全集1, 22: 全集2
    @Column(name = "type") 
    private Integer type;

    @Column(name = "execute_time") // 下次执行时间
    private LocalDateTime executeTime;

    @Column(name = "language_type")
    private String languageType;
}
