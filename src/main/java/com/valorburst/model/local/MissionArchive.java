package com.valorburst.model.local;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

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

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "money")
    private BigDecimal money;

    @Column(name = "type")
    private Integer type;

    @Column(name = "archive_time")
    private Instant archiveTime;

    @Column(name = "language_type")
    private String languageType;
}
