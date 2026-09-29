package com.aicareer.jobradar.module.notification.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;

    public TelegramBot(
            @Value("${app.telegram.bot-token}") String botToken,
            @Value("${app.telegram.bot-username}") String botUsername) {
        super(botToken);
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text   = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();

        if ("/start".equals(text)) {
            sendMessage(chatId, "Welcome to AI Job Radar! 🚀\n\nYour chat ID is: " + chatId +
                    "\n\nSave this ID in your profile to receive job alerts.");
        }
    }

    public void sendMessage(String chatId, String text) {
        try {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setText(text);
            msg.enableMarkdown(false);
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
        }
    }
}
