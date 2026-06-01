package com.glizzie.smartportfolioai.commands;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LocalizationManager {
    private final Map<String, LocalizationService> services;

    public LocalizationManager(List<LocalizationService> localizationServices) {
        this.services = localizationServices.stream()
                .collect(Collectors.toMap(LocalizationService::getLanguageCode, s -> s));
    }

    public LocalizationService get(String lang) {
        return services.getOrDefault(lang.toUpperCase(), services.get("EN"));
    }
}
