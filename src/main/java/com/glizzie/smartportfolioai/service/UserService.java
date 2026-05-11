package com.glizzie.smartportfolioai.service;

import com.glizzie.smartportfolioai.config.BotUsers;
import com.glizzie.smartportfolioai.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setUserLanguage(Long chatId, String lang) {
        BotUsers user = userRepository.findById(chatId)
                .orElse(new BotUsers(chatId, lang));
        user.setLanguageCode(lang);
        userRepository.save(user);
    }

    public Optional<String> getUserLanguage(Long chatId) {
        return userRepository.findById(chatId)
                .map(BotUsers::getLanguageCode);
    }

    public boolean exists(Long chatId) {
        return userRepository.existsById(chatId);
    }
}
