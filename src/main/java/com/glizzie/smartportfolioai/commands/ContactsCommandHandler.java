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
    private final LocalizationManager localizationManager;

    @Value("${bot.contacts.github}")
    private String githubUrl;

    @Value("${bot.contacts.telegram}")
    private String telegramUrl;

    @Value("${bot.contacts.email_ru}")
    private String emailAddressRu;

    @Value("${bot.contacts.email_en}")
    private String emailAddressEn;

    @Value("${bot.contacts.email_de}")
    private String emailAddressDe;

    public ContactsCommandHandler(UserService userService, LocalizationManager localizationManager) {
        this.userService = userService;
        this.localizationManager = localizationManager;
    }

    @Override
    public String getCommandName() {
        return "/contacts";
    }

    @Override
    public void handle(Message message, TelegramClient telegramClient) {
        Long chatId = message.getChatId();

        String langCode = userService.getUserLanguage(chatId).orElse("EN");
        LocalizationService lang = localizationManager.get(langCode);

        String targetEmail;
        if (lang.getLanguageCode().equals("RU")) {
            targetEmail = emailAddressRu;
        } else if (lang.getLanguageCode().equals("DE")) {
            targetEmail = emailAddressDe;
        } else {
            targetEmail = emailAddressEn;
        }

        String text = lang.getContactsMessage(targetEmail);

        InlineKeyboardButton githubBtn = InlineKeyboardButton.builder()
                .text(lang.getGithubBtnText())
                .url(githubUrl)
                .build();

        InlineKeyboardButton telegramBtn = InlineKeyboardButton.builder()
                .text(lang.getTelegramBtnText())
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