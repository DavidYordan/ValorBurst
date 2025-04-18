package com.valorburst.model.local;

import java.math.BigDecimal;

import com.valorburst.model.remote.RemoteVipDetails;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vip_details") // 对应表名
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalVipDetails {
    
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "vip_name_type")
    private Integer vipNameType;

    @Column(name = "money")
    private BigDecimal money;

    @Column(name = "language_type")
    private String languageType;

    public static LocalVipDetails fromRemote(RemoteVipDetails remote) {
        return LocalVipDetails.builder()
                .id(remote.getId())
                .vipNameType(remote.getVipNameType())
                .money(remote.getMoney())
                .languageType(remote.getLanguageType())
                .build();
    }
}