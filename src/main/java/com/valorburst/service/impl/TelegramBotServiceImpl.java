package com.valorburst.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.valorburst.config.AppProperties;
import com.valorburst.dto.DailyStatsSummaryDto;
import com.valorburst.dto.InlineKeyboardButtonDto;
import com.valorburst.dto.InlineKeyboardMarkupDto;
import com.valorburst.dto.RecordStatsDto;
import com.valorburst.model.local.TelegramMessage;
import com.valorburst.repository.local.TelegramMessageRepository;
import com.valorburst.service.TelegramBotService;
import com.valorburst.service.ExcelExportService;
import com.valorburst.util.TelegramMessageBuilder;
import com.valorburst.util.TelegramMultipartBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.io.File;

/**
 * Telegram 机器人核心实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotServiceImpl implements TelegramBotService {

    private final AppProperties appProperties;
    private final ExcelExportService excelExportService;
    private final TelegramMessageRepository messageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理来自用户/群的指令
     */
    @Override
    public void handleCommand(String command, String chatId) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate startDate = LocalDate.of(2021, 1, 1);
            LocalDate endDate = today;
            String[] parts;

            if ("/status".equals(command)) {
                sendStatusReport();
                return;
            } else if (command.equals("/cls")) {
                clearAllMessages();
                return;
            } else if (command.startsWith("/details")) {
                parts = command.trim().split("\\s+");
                if (parts.length == 2 && parts[1].equals("-m")) {
                    startDate = today.withDayOfMonth(1);
                } else if (parts.length == 2 && parts[1].matches("\\d{8}")) {
                    startDate = LocalDate.parse(parts[1], DateTimeFormatter.ofPattern("yyyyMMdd"));
                    endDate = startDate;
                } else if (parts.length == 3 && parts[1].matches("\\d{8}") && parts[2].matches("\\d{8}")) {
                    startDate = LocalDate.parse(parts[1], DateTimeFormatter.ofPattern("yyyyMMdd"));
                    endDate = LocalDate.parse(parts[2], DateTimeFormatter.ofPattern("yyyyMMdd"));
                }

                RecordStatsDto stats = RecordStatsDto.builder()
                        .build();
                String message = TelegramMessageBuilder.buildDetails(startDate, endDate, stats);

                sendMessage(chatId, message, "details");
                return;

            } else if (command.startsWith("/export")) {
                parts = command.trim().split("\\s+");
                if (parts.length == 2 && parts[1].equals("-m")) {
                    startDate = today.withDayOfMonth(1);
                } else if (parts.length == 2 && parts[1].matches("\\d{8}")) {
                    startDate = LocalDate.parse(parts[1], DateTimeFormatter.ofPattern("yyyyMMdd"));
                    endDate = startDate;
                } else if (parts.length == 3 && parts[1].matches("\\d{8}") && parts[2].matches("\\d{8}")) {
                    startDate = LocalDate.parse(parts[1], DateTimeFormatter.ofPattern("yyyyMMdd"));
                    endDate = LocalDate.parse(parts[2], DateTimeFormatter.ofPattern("yyyyMMdd"));
                }

                try {
                    File file = excelExportService.exportRecords(startDate, endDate);
                    sendDocument(chatId, file, file.getName());
                } catch (Exception e) {
                    log.error("导出 Excel 失败", e);
                    sendMessage(chatId, "⚠️ 导出失败：" + e.getMessage(), "error");
                }
                return;
            }

            sendMessageWithMenu(
                chatId,
                """
                ✅ 支持的指令：
                /status                    - 查看当前状态
                /details                   - 查看所有收益数据
                /details -m                - 查看本月收益数据
                /details yyyymmdd          - 查看指定日期的收益数据
                /details yyyymmdd yyyymmdd - 查看指定日期范围的收益数据
                /export                    - 导出所有收益数据
                /export -m                 - 导出本月收益数据
                /export yyyymmdd           - 导出指定日期的收益数据
                /export yyyymmdd yyyymmdd  - 导出指定日期范围的收益数据
                """,
                buildMainMenu()
            );

        } catch (Exception e) {
            log.error("处理指令失败: {}", command, e);
        }
    }

    private void clearAllMessages() {
        try {
            var allMessages = messageRepository.findAll();
            for (TelegramMessage msg : allMessages) {
                Long messageId = msg.getMessageId();
                try {
                    deleteMessage(msg.getChatId(), messageId);
                    messageRepository.deleteById(messageId);
                    log.info("🗑️ 已删除消息 type={}, id={}", msg.getType(), messageId);
                } catch (Exception e) {
                    log.warn("⚠️ 删除消息失败 type={}, id={}: {}", msg.getType(), messageId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("❌ 执行 clearAllMessages 失败", e);
        }
    }

    /**
     * 发送文档
     */
    private void sendDocument(String chatId, File file, String filename) {
        try {
            String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
            String botToken = appProperties.getTelegram().getToken();

            String url = String.format("https://api.telegram.org/bot%s/sendDocument", botToken);

            var body = TelegramMultipartBuilder.buildMultipartFileUpload(chatId, file, filename, boundary);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(body)
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            Long messageId = root.path("result").path("message_id").asLong();

            saveMessage(messageId, chatId, filename, "document");
            log.info("发送 Excel 文件成功：{}", response.body());
        } catch (Exception e) {
            log.error("发送 Excel 文件失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 发消息到 Telegram
     */
    @Override
    public Long sendMessage(String chatId, String text, String type) {
        try {
            String botToken = appProperties.getTelegram().getToken();
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);

            String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                    botToken, chatId, encodedText
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            Long messageId = root.path("result").path("message_id").asLong();

            saveMessage(messageId, chatId, text, type);

            return messageId;
        } catch (Exception e) {
            log.error("发送消息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 发送消息到 Telegram，带有菜单
     */
    private int sendMessageWithMenu(String chatId, String text, InlineKeyboardMarkupDto keyboardMarkup) {
        try {
            String botToken = appProperties.getTelegram().getToken();
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

            String payload = objectMapper.writeValueAsString(
                Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "reply_markup", keyboardMarkup
                )
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            Long messageId = root.path("result").path("message_id").asLong();

            saveMessage(messageId, chatId, text, "menu");

            return root.path("result").path("message_id").asInt();
        } catch (Exception e) {
            log.error("发送消息失败: {}", e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 删除指定聊天中的某条消息
     */
    @Override
    public void deleteMessage(String chatId, Long messageId) {
        try {
            String botToken = appProperties.getTelegram().getToken();
            String url = String.format(
                    "https://api.telegram.org/bot%s/deleteMessage?chat_id=%s&message_id=%d",
                    botToken, chatId, messageId
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            log.info("🗑️ 删除消息成功: {}", response.body());

        } catch (Exception e) {
            log.error("删除消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 构建主菜单
     */
    private InlineKeyboardMarkupDto buildMainMenu() {
        return InlineKeyboardMarkupDto.builder()
            .inline_keyboard(List.of(
                List.of(
                    InlineKeyboardButtonDto.builder().text("📈 状态").callback_data("/status").build()
                ),
                List.of(
                    InlineKeyboardButtonDto.builder().text("📋 明细").callback_data("/details").build(),
                    InlineKeyboardButtonDto.builder().text("📋 本月").callback_data("/details -m").build()
                ),
                List.of(
                    InlineKeyboardButtonDto.builder().text("📤 导出").callback_data("/export").build(),
                    InlineKeyboardButtonDto.builder().text("📤 本月").callback_data("/export -m").build()
                )
            ))
            .build();
    }

    /**
     * 保存消息到数据库
     */
    @Override
    public void saveMessage(Long messageId, String chatId, String text, String type) {
        messageRepository.save(TelegramMessage.builder()
                .messageId(messageId)
                .chatId(chatId)
                // .messageText(text)
                .type(type)
                .build());
    }

    /**
     * 概况汇总
     */
    @Override
    public void sendStatusReport() {
        DailyStatsSummaryDto stats = DailyStatsSummaryDto.builder()
                .build();
        
        String msg = TelegramMessageBuilder.buildStatusReport(stats);

        List<TelegramMessage> oldMessages = messageRepository.findByType("daily");
        for (TelegramMessage m : oldMessages) {
            try {
                deleteMessage(m.getChatId(), m.getMessageId());
                messageRepository.deleteById(m.getMessageId());
                log.info("🗑️ 已删除旧的 status 消息");
            } catch (Exception e) {
                log.warn("⚠️ 删除旧的 status 消息失败", e);
            }
        }

        try {
            sendMessage(appProperties.getTelegram().getChatId(), msg, "daily");
            log.info("✅ 已发送status到 Telegram");
        } catch (Exception e) {
            log.error("发送 status 失败", e);
        }
    }
}
