package com.valorburst.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InlineKeyboardMarkupDto {
    private List<List<InlineKeyboardButtonDto>> inline_keyboard;
}
