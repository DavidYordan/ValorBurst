package com.valorburst.model.local;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mission_details_archive")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionDetailsArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_details_id")
    private Integer missionDetailsId;

    @Column(name = "mission_id")
    private Integer missionId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "type")
    private Integer type;

    @Column(name = "cost")
    private Double cost;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "money")
    private Double money;

    @Column(name = "archive_time")
    private LocalDateTime archiveTime;

    @Column(name = "language_type")
    private String languageType;
}
