package com.valorburst.model.local;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "setting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Setting {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "`key`")
    private String key;

    @Column(name = "value")
    private String value;
}
