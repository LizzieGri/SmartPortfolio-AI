package com.glizzie.smartportfolioai.commands;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public interface CommandHandler {
    String getCommandName();
    void handle(Message message, TelegramClient telegramClient);
}
