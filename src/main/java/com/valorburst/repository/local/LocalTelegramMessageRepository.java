package com.valorburst.repository.local;

import com.valorburst.model.local.LocalTelegramMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocalTelegramMessageRepository extends JpaRepository<LocalTelegramMessage, Long> {
    
    List<LocalTelegramMessage> findByType(String type);
}
