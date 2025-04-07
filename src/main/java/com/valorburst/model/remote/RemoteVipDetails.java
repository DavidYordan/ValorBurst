package com.valorburst.model.remote;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vip_details") // 对应表名
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoteVipDetails {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "vip_name_type")
    private Integer vipNameType;

    @Column(name = "money")
    private BigDecimal money;

    @Column(name = "language_type")
    private String languageType;
}