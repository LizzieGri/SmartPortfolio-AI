package com.glizzie.smartportfolioai.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class ResumeService {

    private final ChatModel chatModel;

    @Value("classpath:resume.txt")
    private Resource resumeResource;

    public ResumeService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String askAboutMe(String userQuestion) throws IOException {
        String resumeContent = resumeResource.getContentAsString(StandardCharsets.UTF_8);

        String systemText = """
                Ты — AI-ассистент Java-разработчика. 
                Используй следующее резюме, чтобы отвечать на вопросы:
                {resume}
                """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
        var systemMessage = systemPromptTemplate.createMessage(Map.of("resume", resumeContent));

        return chatModel.call(new Prompt(java.util.List.of(systemMessage,
                        new org.springframework.ai.chat.messages.UserMessage(userQuestion))))
                .getResult().getOutput().getContent();
    }
}

