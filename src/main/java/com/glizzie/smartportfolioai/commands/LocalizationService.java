package com.glizzie.smartportfolioai.commands;

public interface LocalizationService {
    String getLanguageCode();
    String getWelcomeMessage();
    String getCancelMessage();
    String getFeedbackSuccessMessage();
    String getFeedbackPromptText();
    String getVacancyAnalysisPrompt();
    String getVacancyWaitingMessage();
    String getAiHintMessage();
    String getThinkingMessage();
    String getErrorMessage();
    String getFeedbackSelectionPrompt();
    String getResumePdfCaption();
    String getAnonymousName();
    String getResumePdfFileName();
    String getResumeMarkdownFileName();
    String getAiBtnText();
    String getLangBtnText();
    String getContactBtnText();
    String getPdfBtnText();
    String getAiAnalysisBtnText();
    String getFeedbackBtnText();
    String getCancelBtnText();
    String getVacancyAnalysisSystemPrompt(String resumeText);
    String getContactsMessage(String emailAddress);
    String getGithubBtnText();
    String getTelegramBtnText();
}