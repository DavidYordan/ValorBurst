package com.valorburst.scheduler;

import com.valorburst.config.AppProperties;
import com.valorburst.model.local.LocalTelegramMessage;
import com.valorburst.repository.local.LocalTelegramMessageRepository;
import com.valorburst.service.TelegramBotService;
import com.valorburst.util.DedupingExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramScheduler {

    private final AppProperties appProperties;
    private final LocalTelegramMessageRepository messageRepository;
    private final TelegramBotService telegramBotService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DedupingExecutor dedupingExecutor;

    private long lastUpdateId = 0;

    @Scheduled(fixedDelay = 5 * 1000)
    public void pollTelegramUpdates() {
        try {
            String botToken = appProperties.getTelegram().getToken();
            String url = "https://api.telegram.org/bot" + botToken + "/getUpdates"
                    + "?timeout=5&offset=" + (lastUpdateId + 1);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode updates = objectMapper.readTree(response.body()).path("result");

            for (JsonNode update : updates) {
                long updateId = update.path("update_id").asLong();
                lastUpdateId = updateId;

                dedupingExecutor.execute(() -> handleTelegramUpdate(update), "tg-update-" + updateId);
            }
        } catch (Exception e) {
            log.error("❌ Telegram 轮询失败", e);
        }
    }

    @Scheduled(fixedDelay = 60 * 1000)
    public void cleanupOldMessagesIfNeeded() {
        try {
            long total = messageRepository.count();
            if (total > 15) {
                List<LocalTelegramMessage> oldestMessages = messageRepository
                        .findAll()
                        .stream()
                        .sorted(Comparator.comparing(LocalTelegramMessage::getCreateTime))
                        .limit(total - 15)
                        .toList();

                for (LocalTelegramMessage message : oldestMessages) {
                    Long messageId = message.getMessageId(); 
                    dedupingExecutor.execute(() -> {
                        try {
                            telegramBotService.deleteMessage(message.getChatId(), messageId);
                            messageRepository.delete(message);
                        } catch (Exception e) {
                            log.warn("⚠️ 删除消息失败: {}", e.getMessage());
                        }
                    }, "tg-delete-" + messageId);
                }
                log.info("🧹 自动清理{}条消息", total - 15);
            }
        } catch (Exception e) {
            log.warn("⚠️ 清理消息失败: {}", e.getMessage());
        }
    }

    private void acknowledgeCallbackQuery(String callbackQueryId) {
        try {
            String url = "https://api.telegram.org/bot" + appProperties.getTelegram().getToken()
                    + "/answerCallbackQuery?callback_query_id=" + callbackQueryId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("❌ 回调确认失败", e);
        }
    }

    private void handleTelegramUpdate(JsonNode update) {
        try {
            // 👉 处理普通消息
            JsonNode message = update.path("message");
            if (!message.isMissingNode()) {
                String chatId = message.path("chat").path("id").asText();
                Long messageId = message.path("message_id").asLong();
    
                telegramBotService.saveMessage(messageId, chatId, "", "user_input");
    
                if (message.has("text")) {
                    String text = message.path("text").asText();
                    if (text.startsWith("/")) {
                        log.info("📥 收到命令: [{}] from chatId: {}", text, chatId);
                        dedupingExecutor.execute(() -> telegramBotService.handleCommand(text, chatId), "tg-callback-" + text);
                    }
                }
            }
    
            // 👉 处理按钮回调
            JsonNode callbackQuery = update.path("callback_query");
            if (!callbackQuery.isMissingNode()) {
                String chatId = callbackQuery.path("message").path("chat").path("id").asText();
                String callbackData = callbackQuery.path("data").asText();
    
                log.info("🔘 收到按钮回调: [{}] from chatId: {}", callbackData, chatId);
    
                dedupingExecutor.execute(() -> telegramBotService.handleCommand(callbackData, chatId), "tg-callback-" + callbackData);
    
                // 回调响应确认，避免转圈
                acknowledgeCallbackQuery(callbackQuery.path("id").asText());
            }
        } catch (Exception e) {
            log.error("❌ 处理 Telegram 更新失败", e);
        }
    }
}
