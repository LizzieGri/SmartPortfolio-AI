package com.glizzie.smartportfolioai;

import com.glizzie.smartportfolioai.commands.CommandHandler;
import com.glizzie.smartportfolioai.config.BotConfig;
import com.glizzie.smartportfolioai.service.ResumeService;
import com.glizzie.smartportfolioai.service.UserService;
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

    public ResumeTelegramBot(BotConfig config,
                             UserService userService,
                             ResumeService resumeService,
                             List<CommandHandler> commandList) {
        this.config = config;
        this.userService = userService;
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
            handleTextMessage(update);
        }
        else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        if (data.startsWith("lang_")) {
            String selectedLang = data.split("_")[1];
            userService.setUserLanguage(chatId, selectedLang);

            // 1. Формируем текст подтверждения
            String confirmation = selectedLang.equals("RU")
                    ? "Язык изменен на русский! 🇷🇺"
                    : "Language changed to English! 🇬🇧";

            // 2. Убираем "часики" на кнопке
            try {
                telegramClient.execute(new AnswerCallbackQuery(callbackQuery.getId()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            // 3. САМЫЙ ВАЖНЫЙ МОМЕНТ:
            // Вместо простого sendText, мы отправляем сообщение С ОБНОВЛЕННЫМ МЕНЮ
            String welcomeText = selectedLang.equals("RU")
                    ? confirmation + "\nЧем я могу вам помочь?"
                    : confirmation + "\nHow can I help you?";

            // Вызываем метод, который соберет кнопки на правильном языке и отправит их
            sendMainMenuWithCustomText(chatId, welcomeText);
        }
    }

    private void sendMainMenuWithCustomText(long chatId, String text) {
        String lang = userService.getUserLanguage(chatId).orElse("EN");

        // Формируем кнопки (уже на новом языке, так как мы только что сохранили его в БД)
        String aiBtn = lang.equals("RU") ? "🤖 Спросить AI" : "🤖 Ask AI";
        String langBtn = lang.equals("RU") ? "🌐 Сменить язык" : "🌐 Change Language";
        String stackBtn = lang.equals("RU") ? "💻 Стек и Опыт" : "💻 Stack & Exp";
        String contactBtn = lang.equals("RU") ? "📞 Контакты" : "📞 Contacts";
        String pdfBtn = lang.equals("RU") ? "📄 Скачать PDF" : "📄 Download PDF";

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(aiBtn))))
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(stackBtn), new KeyboardButton(contactBtn))))
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(langBtn), new KeyboardButton(pdfBtn))))
                .resizeKeyboard(true)
                .build();

        executeMessage(SendMessage.builder()
                .chatId(chatId)
                .text(text) // Используем переданный текст
                .replyMarkup(keyboardMarkup)
                .build());
    }

    private void handleTextMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        String lang = userService.getUserLanguage(chatId).orElse("EN");

        if (!userService.exists(chatId)) {
            sendLanguageSelection(chatId);
            return;
        }

        if (text.equals("/language") || text.equals("🌐 Сменить язык") || text.equals("🌐 Change Language")) {
            sendLanguageSelection(chatId);
            return;
        }

        if (text.equals("💻 Стек и Опыт") || text.equals("💻 Stack & Exp")) {
            sendStackInfo(chatId, lang);
            return;
        }

        if (text.equals("🤖 Спросить AI") || text.equals("🤖 Ask AI")) {
            sendAiHint(chatId, lang);
            return;
        }

        if (text.equals("📞 Контакты") || text.equals("📞 Contacts")) {
            if (commands.containsKey("/contacts")) {
                commands.get("/contacts").handle(update.getMessage(), telegramClient);
            }
            return;
        }

        if (text.equals("📄 Скачать PDF") || text.equals("📄 Download PDF")) {
            sendResumePdf(chatId, lang);
            return;
        }

        if (text.equals("/start")) {
            sendMainMenu(chatId);
            return;
        }

        handleAIQuery(chatId, text);
    }

    private void sendLanguageSelection(long chatId) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("🇬🇧 English").callbackData("lang_EN").build(),
                        InlineKeyboardButton.builder().text("🇷🇺 Русский").callbackData("lang_RU").build()
                ))
                .build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Please select your language / Пожалуйста, выберите язык:")
                .replyMarkup(markup)
                .build();

        executeMessage(message);
    }

    private void sendStackInfo(long chatId, String lang) {
        String info = lang.equals("RU")
                ? """
          *Мой Стек:*
          • Java 11/17/21, Spring Boot 3, JPA
          • Kafka, RabbitMQ, PostgreSQL
          • AI: Spring AI, LangChain4j, OpenAI API
          
          *Опыт:*
          • 4 года в Крок (микросервисы, интеграции).
          • Разработка RAG-систем и AI-ассистентов.
          """
                : """
          *Technical Stack:*
          • Java 11/17/21, Spring Boot 3, JPA
          • Kafka, RabbitMQ, PostgreSQL
          • AI: Spring AI, LangChain4j, OpenAI API
          
          *Experience:*
          • 4 years at CROC (microservices, integrations).
          • Building RAG systems & AI assistants.
          """;

        sendText(chatId, info);
    }

    private void sendAiHint(long chatId, String lang) {
        String hint = lang.equals("RU")
                ? "Отправьте мне любой вопрос! Например: 'Расскажи про опыт с Kafka' или 'Какие проекты на Spring AI ты делала?'"
                : "Send me any question! For example: 'Tell me about your Kafka experience' or 'What Spring AI projects have you built?'";

        sendText(chatId, "🤖 " + hint);
    }

    private void sendResumePdf(long chatId, String lang) {
        String fileName = lang.equals("RU") ? "resume_ru.pdf" : "resume_en.pdf";
        String caption = lang.equals("RU")
                ? "Вот моё резюме в формате PDF 📄"
                : "Here is my resume in PDF format 📄";

        try {
            // Читаем файл из ресурсов
            InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
            if (is == null) throw new IOException("File not found");

            InputFile inputFile = new InputFile(is, fileName);

            SendDocument sendDocument = SendDocument.builder()
                    .chatId(chatId)
                    .document(inputFile)
                    .caption(caption)
                    .build();

            telegramClient.execute(sendDocument);
        } catch (Exception e) {
            String error = lang.equals("RU") ? "Ошибка при отправке файла." : "Error sending file.";
            sendText(chatId, error);
            e.printStackTrace();
        }
    }

    private void sendMainMenu(long chatId) {
        String lang = userService.getUserLanguage(chatId).orElse("EN");
        String welcome = lang.equals("RU") ? "Выберите раздел меню:" : "Please select a menu item:";
        sendMainMenuWithCustomText(chatId, welcome);
    }

    private void handleAIQuery(long chatId, String query) {
        try {
            String lang = userService.getUserLanguage(chatId).orElse("EN");

            String thinkingText = lang.equals("RU") ? "Думаю над ответом... 🧠" : "Thinking... 🧠";
            sendText(chatId, thinkingText);

            String aiResponse = resumeService.askAboutMe(query, lang);
            sendText(chatId, aiResponse);

        } catch (Exception e) {
            // Ошибка тоже должна быть понятна пользователю
            String lang = userService.getUserLanguage(chatId).orElse("EN");
            String errorMsg = lang.equals("RU")
                    ? "Произошла ошибка. Напишите @LizzieGri_INC"
                    : "An error occurred. Please contact @LizzieGri_INC";
            sendText(chatId, errorMsg);
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
