package com.valorburst.model.local;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mission_archive")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionArchive {

    @Id
    @Column(name = "mission_id")
    private Integer missionId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "expect_money")
    private BigDecimal expectMoney;

    @Column(name = "overflow")
    private BigDecimal overflow;

    @Column(name = "decreasing")
    private BigDecimal decreasing;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "money")
    private BigDecimal money;

    @Column(name = "type")
    private Integer type;

    @Column(name = "archive_time")
    private LocalDateTime archiveTime;

    @Column(name = "language_type")
    private String languageType;
}
