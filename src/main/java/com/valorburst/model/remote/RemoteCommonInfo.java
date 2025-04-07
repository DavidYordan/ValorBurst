package com.valorburst.model.remote;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "common_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoteCommonInfo {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "condition_from")
    private String conditionFrom;

    @Column(name = "type")
    private Integer type;

    @Column(name = "min")
    private String min;

    @Column(name = "value")
    private String value;

    @Column(name = "create_at")
    private String createAt;

    @Column(name = "max")
    private String max;
}
