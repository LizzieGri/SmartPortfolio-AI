package com.glizzie.smartportfolioai;

import com.glizzie.smartportfolioai.commands.CommandHandler;
import com.glizzie.smartportfolioai.config.BotConfig;
import com.glizzie.smartportfolioai.service.ResumeService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ResumeTelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final Map<String, CommandHandler> commands;
    private final TelegramClient telegramClient;
    private final BotConfig config;
    private final ResumeService resumeService;

    public ResumeTelegramBot(BotConfig config, ResumeService resumeService, List<CommandHandler> commandList) {
        this.config = config;
        this.resumeService = resumeService;
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
        this.commands = commandList.stream()
                .collect(Collectors.toMap(CommandHandler::getCommandName, handler -> handler));
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();

            if (commands.containsKey(text)) {
                commands.get(text).handle(update.getMessage(), telegramClient);
            } else {
                handleAIQuery(update.getMessage().getChatId(), text);
            }
        }
    }

    private void handleAIQuery(long chatId, String query) {
        try {
            sendText(chatId, "Думаю над ответом... 🧠");

            String aiResponse = resumeService.askAboutMe(query);
            sendText(chatId, aiResponse);
        } catch (Exception e) {
            sendText(chatId, "Извините, произошла ошибка при обращении к боту. Попробуйте позже или напишите @LizzieGri_INC");
        }
    }

    private void sendText(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
