package com.valorburst.model.remote;

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
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "invitee_user_id")
    private Integer inviteeUserId;

    @Column(name = "state")
    private Integer state;

    @Column(name = "money")
    private Double money;

    @Column(name = "create_time")
    private String createTime;

    @Column(name = "user_type")
    private Integer userType;
}
