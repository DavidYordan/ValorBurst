package com.valorburst.model.remote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "invite") // 对应表名
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "invitee_user_id")
    private Integer inviteeUserId;

    @Column(name = "state")
    private Integer state;

    @Column(name = "money")
    private BigDecimal money;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "user_type")
    private Integer userType;
}
