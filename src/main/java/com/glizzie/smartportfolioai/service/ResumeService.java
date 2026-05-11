package com.glizzie.smartportfolioai.service;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class ResumeService {

    private final ChatModel chatModel;

    @Value("classpath:resume_ru.md")
    private Resource resumeRuResource;

    @Value("classpath:resume_en.md")
    private Resource resumeEnResource;

    public ResumeService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String askAboutMe(String userQuestion, String lang) {
        try {
            // Выбираем нужный файл в зависимости от языка
            Resource selectedResource = lang.equalsIgnoreCase("RU")
                    ? resumeRuResource
                    : resumeEnResource;

            String resumeContent = selectedResource.getContentAsString(StandardCharsets.UTF_8);

            // Формируем четкую инструкцию для системного промпта
            String languageInstruction = lang.equalsIgnoreCase("RU")
                    ? "Отвечай строго на РУССКОМ языке."
                    : "Respond strictly in ENGLISH.";

            String systemText = """
                    SYSTEM: {languageInstruction}
                    
                    Ты — персональный AI-ассистент Java-разработчика Елизаветы. Твоя задача — профессионально отвечать на вопросы рекрутеров, используя предоставленный контекст резюме.
                    
                    ПРАВИЛА ОТВЕТА:
                    - Будь лаконичным и технически грамотным.
                    - Если в резюме нет ответа на конкретный вопрос, вежливо направь рекрутера к Елизавете напрямую (@LizzieGri_INC).
                    - Не выдумывай технологии или опыт, которых нет в тексте резюме.
                    
                    КОНТЕКСТ (РЕЗЮМЕ):
                    {resume}
                    """;

            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);

            var systemMessage = systemPromptTemplate.createMessage(Map.of(
                    "resume", resumeContent,
                    "languageInstruction", languageInstruction
            ));

            return chatModel.call(new Prompt(List.of(systemMessage, new UserMessage(userQuestion))))
                    .getResult().getOutput().getContent();

        } catch (IOException e) {
            return lang.equalsIgnoreCase("RU")
                    ? "Ошибка при чтении файла резюме. Свяжитесь с @LizzieGri_INC"
                    : "Error reading resume file. Please contact @LizzieGri_INC";
        }
    }
}

