package com.valorburst.model.local;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "mission_details_compensation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionDetailsCompensation {

    @Id
    @Column(name = "mission_details_id")
    private Integer missionDetailsId;

    @Column(name = "mission_id")
    private Integer missionId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "type")
    private Integer type;

    @Column(name = "cost")
    private BigDecimal cost;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "money")
    private BigDecimal money;

    @Column(name = "invitee_id")
    private Integer inviteeId;

    @Column(name = "invitee_name")
    private String inviteeName;

    @Column(name = "continuous")
    private Integer continuous;

    @Column(name = "course_details_id")
    private Integer courseDetailsId;

    @Column(name = "execute_time")
    private Instant executeTime;

    @Column(name = "language_type")
    private String languageType;
}
