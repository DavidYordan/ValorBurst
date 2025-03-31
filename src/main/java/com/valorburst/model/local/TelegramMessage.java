package com.valorburst.model.local;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "telegram_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramMessage {

    @Id
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "chat_id")
    private String chatId;

    @Column(name = "type")
    private String type;

    @Column(name = "message_text")
    private String messageText;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}