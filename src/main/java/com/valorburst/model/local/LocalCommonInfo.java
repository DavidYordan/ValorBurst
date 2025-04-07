package com.valorburst.model.local;

import com.valorburst.model.remote.RemoteCommonInfo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "common_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalCommonInfo {

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

    public static LocalCommonInfo fromRemote(RemoteCommonInfo remote) {
        return LocalCommonInfo.builder()
                .id(remote.getId())
                .conditionFrom(remote.getConditionFrom())
                .type(remote.getType())
                .min(remote.getMin())
                .value(remote.getValue())
                .createAt(remote.getCreateAt())
                .max(remote.getMax())
                .build();
    }
}
