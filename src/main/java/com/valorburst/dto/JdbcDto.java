package com.valorburst.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JdbcDto {
    private String sql;
    private Integer userId;
    private String username;
}
