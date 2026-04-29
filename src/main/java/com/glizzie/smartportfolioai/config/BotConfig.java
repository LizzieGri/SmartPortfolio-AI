package com.glizzie.smartportfolioai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BotConfig {

    @Value(value = "${bot.name}")
    private String botName;

    @Value(value = "${bot.token}")
    private String botToken;

    public String getBotName() {
        return botName;
    }

    public String getBotToken() {
        return botToken;
    }
}
