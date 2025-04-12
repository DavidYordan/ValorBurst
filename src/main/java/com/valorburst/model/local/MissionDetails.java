package com.valorburst.model.local;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mission_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionDetails {

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

    @Column(name = "cost") // 花费
    private BigDecimal cost;

    @Column(name = "rate") // 比率
    private BigDecimal rate;

    @Column(name = "money") // 收益
    private BigDecimal money;

    @Column(name = "invitee_id")
    private Integer inviteeId;

    @Column(name = "invitee_name")
    private String inviteeName;

    @Column(name = "continuous")
    private Integer continuous;

    @Column(name = "execute_time")
    private LocalDateTime executeTime;

    @Column(name = "language_type")
    private String languageType;
}
