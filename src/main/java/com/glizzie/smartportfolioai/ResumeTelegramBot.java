package com.glizzie.smartportfolioai;

import com.glizzie.smartportfolioai.commands.CommandHandler;
import com.glizzie.smartportfolioai.commands.LocalizationManager;
import com.glizzie.smartportfolioai.commands.LocalizationService;
import com.glizzie.smartportfolioai.config.BotConfig;
import com.glizzie.smartportfolioai.config.Feedback;
import com.glizzie.smartportfolioai.repository.FeedbackRepository;
import com.glizzie.smartportfolioai.service.ResumeService;
import com.glizzie.smartportfolioai.service.UserService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ResumeTelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final Map<String, CommandHandler> commands;
    private final TelegramClient telegramClient;
    private final BotConfig config;
    private final ResumeService resumeService;
    private final UserService userService;
    private final ChatClient chatClient;
    private final FeedbackRepository feedbackRepository;
    private final LocalizationManager localizationManager;

    @Value(value = "${bot.my_chat}")
    private long myChatId;

    public ResumeTelegramBot(BotConfig config,
                             UserService userService,
                             FeedbackRepository feedbackRepository,
                             ResumeService resumeService,
                             List<CommandHandler> commandList,
                             ChatClient.Builder chatClientBuilder,
                             LocalizationManager localizationManager) {
        this.config = config;
        this.userService = userService;
        this.feedbackRepository = feedbackRepository;
        this.resumeService = resumeService;
        this.localizationManager = localizationManager;
        this.chatClient = chatClientBuilder.build();
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
            handleTextMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();
        LocalizationService currentLang = localizationManager.get(userService.getUserLanguage(chatId).orElse("EN"));

        try {
            telegramClient.execute(new AnswerCallbackQuery(callbackQuery.getId()));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        if (data.startsWith("lang_")) {
            String selectedLangStr = data.split("_")[1];
            userService.setUserLanguage(chatId, selectedLangStr);

            LocalizationService newLang = localizationManager.get(selectedLangStr);

            String confirmation = selectedLangStr.equals("RU")
                    ? "Язык изменен на русский! 🇷🇺"
                    : "Language changed to English! 🇬🇧";

            sendMainMenuWithCustomText(chatId, confirmation + "\n" + newLang.getWelcomeMessage());
        }

        if (data.startsWith("fb_")) {
            boolean anon = data.equals("fb_anon");
            userService.setUserState(chatId, anon ? "WAITING_FB_ANON" : "WAITING_FB_PUBLIC");
            sendOnlyCancelButton(chatId, currentLang.getFeedbackPromptText(), currentLang);
        }
    }

    private void sendMainMenuWithCustomText(long chatId, String text) {
        LocalizationService lang = localizationManager.get(userService.getUserLanguage(chatId).orElse("EN"));

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(lang.getAiAnalysisBtnText()), new KeyboardButton(lang.getAiBtnText()))))
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(lang.getPdfBtnText()), new KeyboardButton(lang.getContactBtnText()))))
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(lang.getLangBtnText()))))
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(lang.getFeedbackBtnText()))))
                .resizeKeyboard(true)
                .build();

        executeMessage(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboardMarkup)
                .build());
    }

    private void handleTextMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        LocalizationService lang = localizationManager.get(userService.getUserLanguage(chatId).orElse("EN"));
        String state = userService.getUserState(chatId);

        if (!userService.exists(chatId)) {
            sendLanguageSelection(chatId);
            return;
        }

        if (text.contains(localizationManager.get("RU").getCancelBtnText()) ||
                text.contains(localizationManager.get("EN").getCancelBtnText())) {
            userService.setUserState(chatId, "DEFAULT");
            sendText(chatId, lang.getCancelMessage());
            sendMainMenu(chatId);
            return;
        }

        if (state != null && state.startsWith("WAITING_FB_")) {
            saveAndProcessFeedback(chatId, update.getMessage(), state, lang);
            return;
        }

        if ("WAITING_FOR_SPEC".equals(state)) {
            analyzeVacancy(chatId, text, lang);
            return;
        }

        if (text.equals(lang.getFeedbackBtnText())) {
            InlineKeyboardMarkup inlineMarkup = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                            InlineKeyboardButton.builder().text("👤 Открыто").callbackData("fb_public").build(),
                            InlineKeyboardButton.builder().text("👻 Анонимно").callbackData("fb_anon").build()
                    ))
                    .build();

            executeMessage(SendMessage.builder()
                    .chatId(chatId)
                    .text(lang.getFeedbackSelectionPrompt())
                    .replyMarkup(inlineMarkup)
                    .build());
            return;
        }

        if (text.equals(lang.getAiAnalysisBtnText())) {
            userService.setUserState(chatId, "WAITING_FOR_SPEC");
            sendOnlyCancelButton(chatId, lang.getVacancyAnalysisPrompt(), lang);
            return;
        }

        if (text.equals("/language") || text.equals(lang.getLangBtnText())) {
            sendLanguageSelection(chatId);
            return;
        }

        if (text.equals(lang.getAiBtnText())) {
            sendAiHint(chatId, lang);
            return;
        }

        if (text.equals(lang.getContactBtnText())) {
            if (commands.containsKey("/contacts")) {
                commands.get("/contacts").handle(update.getMessage(), telegramClient);
            }
            return;
        }

        if (text.equals(lang.getPdfBtnText())) {
            sendResumePdf(chatId, lang);
            return;
        }

        if (text.equals("/start")) {
            sendMainMenu(chatId);
            return;
        }

        handleAIQuery(chatId, text, lang);
    }

    private void saveAndProcessFeedback(long chatId, Message message, String state, LocalizationService lang) {
        boolean isAnon = state.equals("WAITING_FB_ANON");
        String text = message.getText();

        String senderInfo = lang.getAnonymousName();

        if (!isAnon) {
            String firstName = message.getFrom().getFirstName();
            String lastName = message.getFrom().getLastName();
            String username = message.getFrom().getUserName();
            senderInfo = (firstName != null ? firstName : "") +
                    (lastName != null ? " " + lastName : "") +
                    (username != null ? " (@" + username + ")" : "");
        }

        Feedback feedback = new Feedback(chatId, senderInfo, text, isAnon);
        feedbackRepository.save(feedback);

        String adminMsg = String.format("📩 *Новый отзыв (%s):*\n👤 От: %s\n📝 Текст:\n%s",
                isAnon ? "Анонимно" : "Открыто",
                senderInfo,
                text);
        sendText(myChatId, adminMsg);

        userService.setUserState(chatId, "DEFAULT");
        sendText(chatId, lang.getFeedbackSuccessMessage());
        sendMainMenu(chatId);
    }

    private void sendLanguageSelection(long chatId) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("🇬🇧 English").callbackData("lang_EN").build(),
                        InlineKeyboardButton.builder().text("🇷🇺 Русский").callbackData("lang_RU").build(),
                        InlineKeyboardButton.builder().text("🇩🇪 Deutsch").callbackData("lang_DE").build()
                ))
                .build();

        executeMessage(SendMessage.builder()
                .chatId(chatId)
                .text("Please select your language / Пожалуйста, выберите язык / Bitte wählen Sie Ihre Sprache:")
                .replyMarkup(markup)
                .build());
    }

    private void sendOnlyCancelButton(long chatId, String text, LocalizationService lang) {
        ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(lang.getCancelBtnText()))))
                .resizeKeyboard(true)
                .build();

        executeMessage(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build());
    }

    private void analyzeVacancy(long chatId, String vacancyText, LocalizationService lang) {

        sendText(chatId, lang.getVacancyWaitingMessage());

        String myResume = loadResumeText(lang.getResumeMarkdownFileName());

        String systemPrompt = lang.getVacancyAnalysisSystemPrompt(myResume);

        try {
            String aiResponse = chatClient.prompt()
                    .system(systemPrompt)
                    .user("Вот текст вакансии для анализа: \n" + vacancyText)
                    .call()
                    .content();

            executeMessage(SendMessage.builder()
                    .chatId(chatId)
                    .text(aiResponse)
                    .parseMode("Markdown")
                    .build());

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, lang.getErrorMessage());
        } finally {
            userService.setUserState(chatId, "DEFAULT");
            sendMainMenu(chatId);
        }
    }

    private String loadResumeText(String fileName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                return "Данные резюме временно недоступны.";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Данные резюме временно недоступны.";
        }
    }

    private void sendAiHint(long chatId, LocalizationService lang) {
        sendText(chatId, "🤖 " + lang.getAiHintMessage());
    }

    private void sendResumePdf(long chatId, LocalizationService lang) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(lang.getResumePdfFileName())) {
            if (is == null) throw new IOException("File not found");

            InputFile inputFile = new InputFile(is, lang.getResumePdfFileName());
            telegramClient.execute(SendDocument.builder()
                    .chatId(chatId)
                    .document(inputFile)
                    .caption(lang.getResumePdfCaption())
                    .build());
        } catch (Exception e) {
            sendText(chatId, lang.getErrorMessage());
            e.printStackTrace();
        }
    }

    private void sendMainMenu(long chatId) {
        LocalizationService lang = localizationManager.get(userService.getUserLanguage(chatId).orElse("EN"));
        sendMainMenuWithCustomText(chatId, lang.getWelcomeMessage());
    }

    private void handleAIQuery(long chatId, String query, LocalizationService lang) {
        try {
            sendText(chatId, lang.getThinkingMessage());
            String aiResponse = resumeService.askAboutMe(query, lang.getLanguageCode());
            sendText(chatId, aiResponse);
        } catch (Exception e) {
            sendText(chatId, lang.getErrorMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendText(long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
