package com.glizzie.smartportfolioai.commands;

public class EnLocalizationService implements LocalizationService {
    @Override
    public String getLanguageCode() {
        return "EN";
    }

    @Override
    public String getWelcomeMessage() {
        return "Please select a menu item or just ask a question:";
    }

    @Override
    public String getCancelMessage() {
        return "Analysis cancelled.";
    }

    @Override
    public String getFeedbackSuccessMessage() {
        return "✨ Thanks for your feedback!";
    }

    @Override
    public String getFeedbackPromptText() {
        return "Got it! Now please write your feedback in one message (or press Cancel):";
    }

    @Override
    public String getVacancyAnalysisPrompt() {
        return "Please send the job description text or a file (PDF/DOCX). I will analyze how my stack matches your requirements! 👇";
    }

    @Override
    public String getVacancyWaitingMessage() {
        return "🔄 Analyzing the job description... Please wait.";
    }

    @Override
    public String getAiHintMessage() {
        return "Send me any question! For example: 'Tell me about your Kafka experience' or 'What Spring AI projects have you built?'";
    }

    @Override
    public String getThinkingMessage() {
        return "Thinking... 🧠";
    }

    @Override
    public String getErrorMessage() {
        return "An error occurred. Please contact @LizzieGri_INC";
    }

    @Override
    public String getFeedbackSelectionPrompt() {
        return "How would you like to send feedback?";
    }

    @Override
    public String getResumePdfCaption() {
        return "Here is my resume in PDF format 📄";
    }

    @Override
    public String getAnonymousName() {
        return "Anonymous";
    }

    @Override
    public String getResumePdfFileName() {
        return "resume_en.pdf";
    }

    @Override
    public String getResumeMarkdownFileName() {
        return "resume_en.md";
    }

    @Override
    public String getAiBtnText() {
        return "🤖 Ask AI";
    }

    @Override
    public String getLangBtnText() {
        return "🌐 Change Language";
    }

    @Override
    public String getContactBtnText() {
        return "📞 Contacts";
    }

    @Override
    public String getPdfBtnText() {
        return "📄 Download PDF";
    }

    @Override
    public String getAiAnalysisBtnText() {
        return "✨ Job Analysis";
    }

    @Override
    public String getFeedbackBtnText() {
        return "✍️ Leave Feedback";
    }

    @Override
    public String getCancelBtnText() {
        return "❌ Cancel";
    }

    @Override
    public String getVacancyAnalysisSystemPrompt(String resumeText) {
        return """
                You are a professional technical IT Recruiter and Talent Acquisition expert. 
                Your task is to objectively analyze how well the candidate, Elizaveta, matches the job description requirements.
                
                HERE IS ELIZAVETA'S FULL RESUME:
                %s
                
                ANALYSIS GUIDELINES:
                1. Be objective and professional. If the job description requires a skill that is missing from the resume, make sure to point it out.
                2. Highlight "Match" (key alignments in tech stack, frameworks, and commercial experience).
                3. Highlight "Gaps" (what skills are missing or what topics the candidate should brush up on before the interview).
                4. Provide a brief recommendation for the recruiter: should they invite Elizaveta to a technical interview, and what 2-3 specific questions should they ask her?
                
                Format the response beautifully using Markdown (use lists, bold text for emphasis).
                Respond strictly in ENGLISH.
                """.formatted(resumeText);
    }

    @Override
    public String getContactsMessage(String emailAddress) {
        return "🤝 <b>Let's stay in touch!</b>\n\n"
               + "💻 <b>You can check out my code on GitHub or contact me directly.</b>\n"
               + "💬 <b>I'm always open to interesting offers and discussions about Java/AI technologies.</b>\n\n"
               + "📧 <b>Email (click to copy):</b>\n"
               + "<code>" + emailAddress + "</code>";
    }

    @Override
    public String getGithubBtnText() {
        return "Open GitHub";
    }

    @Override
    public String getTelegramBtnText() {
        return "Send a Message";
    }
}