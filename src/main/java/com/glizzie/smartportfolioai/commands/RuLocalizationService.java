package com.glizzie.smartportfolioai.commands;

public class RuLocalizationService implements LocalizationService {

    @Override
    public String getLanguageCode() {
        return "RU";
    }

    @Override
    public String getWelcomeMessage() {
        return "Выберите раздел меню или просто задайте вопрос:";
    }

    @Override
    public String getCancelMessage() {
        return "Анализ прерван.";
    }

    @Override
    public String getFeedbackSuccessMessage() {
        return "✨ Спасибо за фидбек!";
    }

    @Override
    public String getFeedbackPromptText() {
        return "Принято! Теперь напишите ваш отзыв одним сообщением (или нажмите Отмена):";
    }

    @Override
    public String getVacancyAnalysisPrompt() {
        return "Пришлите текст вакансии или файл (PDF/DOCX). Я проанализирую, насколько мой стек подходит под ваши требования! 👇";
    }

    @Override
    public String getVacancyWaitingMessage() {
        return "🔄 Анализирую вакансию... Пожалуйста, подождите.";
    }

    @Override
    public String getAiHintMessage() {
        return "Отправьте мне любой вопрос! Например: 'Расскажи про опыт с Kafka' или 'Какие проекты на Spring AI ты делала?'";
    }

    @Override
    public String getThinkingMessage() {
        return "Думаю над ответом... 🧠";
    }

    @Override
    public String getErrorMessage() {
        return "Произошла ошибка. Напишите @LizzieGri_INC";
    }

    @Override
    public String getFeedbackSelectionPrompt() {
        return "Как отправить ваш отзыв?";
    }

    @Override
    public String getResumePdfCaption() {
        return "Вот моё резюме в формате PDF 📄";
    }

    @Override
    public String getAnonymousName() {
        return "Аноним";
    }

    @Override
    public String getResumePdfFileName() {
        return "resume_ru.pdf";
    }

    @Override
    public String getResumeMarkdownFileName() {
        return "resume_ru.md";
    }

    @Override
    public String getAiBtnText() {
        return "🤖 Спросить AI";
    }

    @Override
    public String getLangBtnText() {
        return "🌐 Сменить язык";
    }

    @Override
    public String getContactBtnText() {
        return "📞 Контакты";
    }

    @Override
    public String getPdfBtnText() {
        return "📄 Скачать PDF";
    }

    @Override
    public String getAiAnalysisBtnText() {
        return "✨ Анализ вакансии";
    }

    @Override
    public String getFeedbackBtnText() {
        return "✍️ Оставить отзыв";
    }

    @Override
    public String getCancelBtnText() {
        return "❌ Отмена";
    }

    @Override
    public String getVacancyAnalysisSystemPrompt(String resumeText) {
        return """
                Ты — профессиональный технический IT-рекрутер и HR-архитектор. 
                Твоя задача — объективно проанализировать, насколько кандидат Елизавета подходит под требования вакансии.
                
                ВОТ ПОЛНОЕ РЕЗЮМЕ ЕЛИЗАВЕТЫ:
                %s
                
                ИНСТРУКЦИЯ ДЛЯ АНАЛИЗА:
                1. Будь строгим и объективным. Если в вакансии требуется навык, которого нет в резюме, обязательно отмечь это.
                2. Выдели "Match" (основные совпадения по стеку, фреймворкам и коммерческому опыту).
                3. Выдели "Gaps" (каких навыков не хватает или какие темы кандидату стоит повторить перед собеседованием).
                4. Дай краткую рекомендацию рекрутеру, стоит ли звать Елизавету на техническое интервью и какие 2-3 точечных вопроса ей стоит задать.
                
                Форматируй ответ красиво с помощью Markdown (используй списки, жирный шрифт для акцентов).
                Отвечай строго на РУССКОМ языке.
                """.formatted(resumeText);
    }

    @Override
    public String getContactsMessage(String emailAddress) {
        return "🤝 <b>Давайте оставаться на связи!</b>\n\n"
               + "💻 <b>Вы можете изучить мой код на GitHub или написать мне напрямую.</b>\n"
               + "💬 <b>Я всегда открыта для интересных предложений и обсуждения Java/AI технологий.</b>\n\n"
               + "📧 <b>Email (нажмите для копирования):</b>\n"
               + "<code>" + emailAddress + "</code>";
    }

    @Override
    public String getGithubBtnText() {
        return "Открыть GitHub";
    }

    @Override
    public String getTelegramBtnText() {
        return "Написать в ЛС";
    }
}