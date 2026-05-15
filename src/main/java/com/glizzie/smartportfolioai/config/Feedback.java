package com.glizzie.smartportfolioai.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "feedback")
@Data
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatId;
    private String username;

    @Column(columnDefinition = "TEXT")
    private String text;

    private boolean isAnonymous = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Feedback(Long chatId, String username, String text, boolean isAnonymous) {
        this.chatId = chatId;
        this.username = username;
        this.text = text;
        this.isAnonymous = isAnonymous;
        this.createdAt = LocalDateTime.now();
    }
}
