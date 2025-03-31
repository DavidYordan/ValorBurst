package com.valorburst.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Telegram telegram;

    @Data
    public static class Telegram {
        private String token;
        private String chatId;
    }
}
