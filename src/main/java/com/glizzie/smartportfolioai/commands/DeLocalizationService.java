package com.glizzie.smartportfolioai.commands;

import org.springframework.stereotype.Component;

@Component
public class DeLocalizationService implements LocalizationService {
    @Override
    public String getLanguageCode() {
        return "DE";
    }

    @Override
    public String getWelcomeMessage() {
        return "Wählen Sie einen Menübereich oder stellen Sie einfach eine Frage:";
    }

    @Override
    public String getCancelMessage() {
        return "Analyse abgebrochen.";
    }

    @Override
    public String getFeedbackSuccessMessage() {
        return "✨ Danke für Ihr Feedback!";
    }

    @Override
    public String getFeedbackPromptText() {
        return "Verstanden! Bitte schreiben Sie Ihr Feedback in einer einzigen Nachricht (oder drücken Sie Abbrechen):";
    }

    @Override
    public String getVacancyAnalysisPrompt() {
        return "Bitte senden Sie den Text der Stellenanzeige. Ich werde analysieren, wie gut mein Tech-Stack zu Ihren Anforderungen passt! 👇";
    }

    @Override
    public String getVacancyWaitingMessage() {
        return "🔄 Analyse der Stellenbeschreibung läuft... Bitte warten.";
    }

    @Override
    public String getAiHintMessage() {
        return "Stellen Sie mir eine Frage! Zum Beispiel: 'Erzähl mir von deiner Erfahrung mit Kafka' oder 'Welche Spring AI Projekte hast du gebaut?'";
    }

    @Override
    public String getThinkingMessage() {
        return "Ich denke nach... 🧠";
    }

    @Override
    public String getErrorMessage() {
        return "Ein Fehler ist aufgetreten. Bitte kontaktieren Sie @LizzieGri_INC";
    }

    @Override
    public String getFeedbackSelectionPrompt() {
        return "Wie möchten Sie Ihr Feedback senden?";
    }

    @Override
    public String getResumePdfCaption() {
        return "Hier ist mein Lebenslauf im PDF-Format 📄";
    }

    @Override
    public String getAnonymousName() {
        return "Anonym";
    }

    @Override
    public String getResumePdfFileName() {
        return "resume_de.pdf";
    }

    @Override
    public String getResumeMarkdownFileName() {
        return "resume_de.md";
    }

    // Тексты кнопок главного меню
    @Override
    public String getAiBtnText() {
        return "🤖 AI Fragen";
    }

    @Override
    public String getLangBtnText() {
        return "🌐 Sprache ändern";
    }

    @Override
    public String getContactBtnText() {
        return "📞 Kontakte";
    }

    @Override
    public String getPdfBtnText() {
        return "📄 PDF Herunterladen";
    }

    @Override
    public String getAiAnalysisBtnText() {
        return "✨ Stellenanalyse";
    }

    @Override
    public String getFeedbackBtnText() {
        return "✍️ Feedback hinterlassen";
    }

    @Override
    public String getCancelBtnText() {
        return "❌ Abbrechen";
    }

    // Кнопки для контактов
    @Override
    public String getGithubBtnText() {
        return "GitHub öffnen";
    }

    @Override
    public String getTelegramBtnText() {
        return "Nachricht senden";
    }

    @Override
    public String getVacancyAnalysisSystemPrompt(String resumeText) {
        return """
                Du bist ein professioneller IT-Recruiter und Tech-Talent-Experte.
                Deine Aufgabe ist es, objektiv zu analysieren, wie gut die Kandidatin Elizaveta auf die Stellenanzeige passt.
                
                HIER IST ELIZAVETAS VOLLSTÄNDIGER LEBENSLAUF:
                %s
                
                ANALYSE-RICHTLINIEN:
                1. Sei objektiv und professionell. Wenn die Stelle eine Fähigkeit erfordert, die im Lebenslauf fehlt, weise darauf hin.
                2. Hebe "Match" hervor (Übereinstimmungen im Tech-Stack und der kommerziellen Erfahrung).
                3. Hebe "Gaps" hervor (welche Fähigkeiten fehlen oder welche Themen die Kandidatin vor dem Interview auffrischen sollte).
                4. Gib eine kurze Empfehlung für den Recruiter: Sollte er Elizaveta zu einem technischen Interview einladen und welche 2-3 gezielten Fragen sollte er ihr stellen?
                
                Formatiere die Antwort ansprechend mit Markdown (Listen, Fettdruck für Hervorhebungen).
                Antworte strikt auf DEUTSCH.
                """.formatted(resumeText);
    }

    @Override
    public String getContactsMessage(String emailAddress) {
        return "🤝 <b>Lassen Sie uns in Kontakt bleiben!</b>\n\n"
                + "💻 <b>Sie können meinen Code auf GitHub einsehen oder mich direkt kontaktieren.</b>\n"
                + "💬 <b>Ich bin immer offen für interessante Angebote und Diskussionen über Java/KI-Technologien.</b>\n\n"
                + "📧 <b>E-Mail (zum Kopieren anklicken):</b>\n"
                + "<code>" + emailAddress + "</code>";
    }
}
