package com.glizzie.smartportfolioai.config;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bot_users")
@Data
@NoArgsConstructor
@Getter
@Setter
public class BotUsers {

    @Id
    private Long chatId;

    private String languageCode;

    public BotUsers(Long chatId, String languageCode) {
        this.chatId = chatId;
        this.languageCode = languageCode;
    }

}
