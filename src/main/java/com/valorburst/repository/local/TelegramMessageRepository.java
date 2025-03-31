package com.valorburst.repository.local;

import com.valorburst.model.local.TelegramMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelegramMessageRepository extends JpaRepository<TelegramMessage, Long> {
    
    List<TelegramMessage> findByType(String type);
}
