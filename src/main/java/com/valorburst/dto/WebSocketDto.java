package com.valorburst.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketDto<T> {
    private String type;
    private String requestId;
    private String message;
    private T data;
}
