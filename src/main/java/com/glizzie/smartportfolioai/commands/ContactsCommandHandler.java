package com.glizzie.smartportfolioai.commands;

import com.glizzie.smartportfolioai.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class ContactsCommandHandler implements CommandHandler {

    private final UserService userService;

    @Value("${bot.contacts.github}")
    private String githubUrl;

    @Value("${bot.contacts.telegram}")
    private String telegramUrl;

    @Value("${bot.contacts.email_ru}")
    private String emailAddressRu;

    @Value("${bot.contacts.email_en}")
    private String emailAddressEn;

    // Внедряем UserService через конструктор
    public ContactsCommandHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public String getCommandName() {
        return "/contacts";
    }

    @Override
    public void handle(Message message, TelegramClient telegramClient) {
        Long chatId = message.getChatId();
        String lang = userService.getUserLanguage(chatId).orElse("EN");

        String text = lang.equals("RU")
                ? "🤝 <b>Давайте оставаться на связи!</b>\n\n" +
                "💻 <b>Вы можете изучить мой код на GitHub или написать мне напрямую.</b>\n" +
                "💬 <b>Я всегда открыта для интересных предложений и обсуждения Java/AI технологий.</b>\n\n" +
                "📧 <b>Email (нажмите для копирования):</b>\n" +
                "<code>" + emailAddressRu + "</code>"
                : "🤝 <b>Let's stay in touch!</b>\n\n" +
                "💻 <b>You can check out my code on GitHub or contact me directly.</b>\n" +
                "💬 <b>I'm always open to interesting offers and discussions about Java/AI technologies.</b>\n\n" +
                "📧 <b>Email (click to copy):</b>\n" +
                "<code>" + emailAddressEn + "</code>";

        // 2. Определяем названия кнопок
        String githubBtnText = lang.equals("RU") ? "Открыть GitHub" : "Open GitHub";
        String telegramBtnText = lang.equals("RU") ? "Написать в ЛС" : "Send a Message";

        InlineKeyboardButton githubBtn = InlineKeyboardButton.builder()
                .text(githubBtnText)
                .url(githubUrl)
                .build();

        InlineKeyboardButton telegramBtn = InlineKeyboardButton.builder()
                .text(telegramBtnText)
                .url(telegramUrl)
                .build();

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(githubBtn))
                .keyboardRow(new InlineKeyboardRow(telegramBtn))
                .build();

        SendMessage sm = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(keyboard)
                .build();

        try {
            telegramClient.execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}