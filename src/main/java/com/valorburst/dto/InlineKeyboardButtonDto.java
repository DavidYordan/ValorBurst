package com.valorburst.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InlineKeyboardButtonDto {
    private String text;
    private String callback_data;
    private String url;
    private String switch_inline_query;
    private String switch_inline_query_current_chat;
    private Boolean pay;
}
