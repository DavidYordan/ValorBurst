package com.valorburst.service;

public interface TelegramBotService {

    /**
     * Delete message from telegram
     * @param chatId
     * @param messageId
     */
    void deleteMessage(String chatId, Long messageId);

    /**
     * Handle command
     * @param command
     */
    void handleCommand(String command, String chatId);

    /**
     * Save message to database
     * @param messageId
     * @param chatId
     * @param text
     * @param type
     */
    void saveMessage(Long messageId, String chatId, String text, String type);

    /**
     * Send text message to telegram
     * @param text
     */
    Long sendMessage(String chatId, String text, String type);

    /**
     * Send daily report
     */
    void sendStatusReport();
}
