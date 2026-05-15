package com.glizzie.smartportfolioai;

import com.glizzie.smartportfolioai.commands.CommandHandler;
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
import java.math.BigInteger;
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

    @Value(value = "${bot.my_chat}")
    private long myChatId;

    public ResumeTelegramBot(BotConfig config,
                             UserService userService,
                             FeedbackRepository feedbackRepository,
                             ResumeService resumeService,
                             List<CommandHandler> commandList,
                             ChatClient.Builder chatClientBuilder) {
        this.config = config;
        this.userService = userService;
        this.feedbackRepository = feedbackRepository;
        this.resumeService = resumeService;
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
        }
        else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();
        // Получаем текущий язык пользователя из базы на случай, если кнопка fb_ не содержит язык
        String currentLang = userService.getUserLanguage(chatId).orElse("EN");

        // 1. Убираем "часики" сразу для всех кнопок
        try {
            telegramClient.execute(new AnswerCallbackQuery(callbackQuery.getId()));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Логика смены языка
        if (data.startsWith("lang_")) {
            String selectedLang = data.split("_")[1];
            userService.setUserLanguage(chatId, selectedLang);

            String confirmation = selectedLang.equals("RU")
                    ? "Язык изменен на русский! 🇷🇺"
                    : "Language changed to English! 🇬🇧";

            String welcomeText = selectedLang.equals("RU")
                    ? confirmation + "\nЧем я могу вам помочь?"
                    : confirmation + "\nHow can I help you?";

            sendMainMenuWithCustomText(chatId, welcomeText);
        }

        // Логика выбора анонимности фидбека
        if (data.startsWith("fb_")) {
            boolean anon = data.equals("fb_anon");
            // Задаем точечное состояние!
            userService.setUserState(chatId, anon ? "WAITING_FB_ANON" : "WAITING_FB_PUBLIC");

            String promptMsg = currentLang.equals("RU")
                    ? "Принято! Теперь напишите ваш отзыв одним сообщением (или нажмите Отмена):"
                    : "Got it! Now please write your feedback in one message (or press Cancel):";

            sendOnlyCancelButton(chatId, promptMsg, currentLang);
        }
    }

    private void sendMainMenuWithCustomText(long chatId, String text) {
        String lang = userService.getUserLanguage(chatId).orElse("EN");

        // Формируем кнопки (уже на новом языке, так как мы только что сохранили его в БД)
        String aiBtn = lang.equals("RU") ? "🤖 Спросить AI" : "🤖 Ask AI";
        String langBtn = lang.equals("RU") ? "🌐 Сменить язык" : "🌐 Change Language";
        String contactBtn = lang.equals("RU") ? "📞 Контакты" : "📞 Contacts";
        String pdfBtn = lang.equals("RU") ? "📄 Скачать PDF" : "📄 Download PDF";
        String aiAnalysisBtn = lang.equals("RU") ? "✨ Анализ вакансии" : "✨ Job Analysis";
        String feedbackBtn = lang.equals("RU") ? "✍️ Оставить отзыв" : "✍️ Leave Feedback";

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(aiAnalysisBtn), new KeyboardButton(aiBtn))))
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(pdfBtn), new KeyboardButton(contactBtn))))
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(langBtn))))
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(feedbackBtn))))
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
        String state = userService.getUserState(chatId);

        if (!userService.exists(chatId)) {
            sendLanguageSelection(chatId);
            return;
        }

        if (text.contains("Отмена") || text.contains("Cancel")) {
            userService.setUserState(chatId, "DEFAULT");
            sendText(chatId, lang.equals("RU") ? "Анализ прерван." : "Analysis cancelled.");
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

        if (text.equals("✍️ Оставить отзыв") || text.equals("✍️ Leave Feedback")) {
            InlineKeyboardMarkup inlineMarkup = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                            InlineKeyboardButton.builder().text("👤 Открыто").callbackData("fb_public").build(),
                            InlineKeyboardButton.builder().text("👻 Анонимно").callbackData("fb_anon").build()
                    ))
                    .build();

            executeMessage(SendMessage.builder()
                    .chatId(chatId)
                    .text(lang.equals("RU") ? "Как отправить ваш отзыв?" : "How would you like to send feedback?")
                    .replyMarkup(inlineMarkup)
                    .build());
            return;
        }


        if (text.equals("✨ Анализ вакансии") || text.equals("✨ Job Analysis")) {
            userService.setUserState(chatId, "WAITING_FOR_SPEC");

            String msg = lang.equals("RU")
                    ? "Пришлите текст вакансии или файл (PDF/DOCX). Я проанализирую, насколько мой стек подходит под ваши требования! 👇"
                    : "Please send the job description text or a file (PDF/DOCX). I will analyze how my stack matches your requirements! 👇";

            sendOnlyCancelButton(chatId, msg, lang);
            return;
        }

        if (text.equals("/language") || text.equals("🌐 Сменить язык") || text.equals("🌐 Change Language")) {
            sendLanguageSelection(chatId);
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

    private void saveAndProcessFeedback(long chatId, Message message, String state, String lang) {
        boolean isAnon = state.equals("WAITING_FB_ANON");
        String text = message.getText();

        // Если НЕ анонимно, пытаемся взять имя
        String senderInfo = "Аноним";
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

        String adminMsg = String.format("📩 *Новый отзыв (%s):*\n%s",
                isAnon ? "Анонимно" : "Открыто", text);
        sendText(myChatId, adminMsg);

        // Завершаем
        userService.setUserState(chatId, "DEFAULT");
        sendText(chatId, lang.equals("RU") ? "✨ Спасибо за фидбек!" : "✨ Thanks for your feedback!");
        sendMainMenu(chatId);
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

    private void sendOnlyCancelButton(long chatId, String text, String lang) {
        String cancelText = lang.equals("RU") ? "❌ Отмена" : "❌ Cancel";

        ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(cancelText))))
                .resizeKeyboard(true)
                .build();

        executeMessage(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build());
    }

    private void analyzeVacancy(long chatId, String vacancyText, String lang) {
        // 1. Сообщаем пользователю, что начали работу (анализ может занять 3-5 секунд)
        String waitingMsg = lang.equals("RU")
                ? "🔄 Анализирую вакансию... Пожалуйста, подождите."
                : "🔄 Analyzing the job description... Please wait.";
        sendText(chatId, waitingMsg);

        String myResume = loadResumeText();

        // 2. Формируем запрос к AI
        // Мы берем твоё резюме (оно должно быть загружено в контекст или передано строкой)
        String systemPrompt = """
            Ты — профессиональный IT-рекрутер. Твоя задача — проанализировать, насколько Елизавета подходит на вакансию.
            
            ВОТ РЕЗЮМЕ ЕЛИЗАВЕТЫ:
            %s
            
            При анализе вакансии:
            1. Будь объективен. Если в вакансии требуется навык, которого нет в резюме (например, специфический фреймворк), отметь это.
            2. Выдели "Match" (совпадения по стеку и опыту).
            3. Выдели "Gaps" (чего не хватает или что нужно уточнить).
            4. Дай краткий совет рекрутеру: на какие темы стоит поспрашивать Елизавету на интервью.
            
            Тон ответов: профессиональный, дружелюбный, лаконичный.
            Отвечай на языке: %s. Использовать Markdown (жирный шрифт, списки).
        """.formatted(myResume, lang.equals("RU") ? "Русский" : "English");

        try {
            String aiResponse = chatClient.prompt()
                    .system(systemPrompt)
                    .user("Вот текст вакансии для анализа: " + vacancyText)
                    .call()
                    .content();

            executeMessage(SendMessage.builder()
                    .chatId(chatId)
                    .text(aiResponse)
                    .parseMode("Markdown")
                    .build());

            userService.setUserState(chatId, "DEFAULT");
            sendMainMenu(chatId);

        } catch (Exception e) {
            sendText(chatId, "Error analyzing vacancy. Please try again later.");
            userService.setUserState(chatId, "DEFAULT");
            sendMainMenu(chatId);
        }
    }

    private String loadResumeText() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("resume_ru.md");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Данные резюме временно недоступны.";
        }
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
        String welcome = lang.equals("RU") ? "Выберите раздел меню или просто задайте вопрос:" : "Please select a menu item or just ask a question:";
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
