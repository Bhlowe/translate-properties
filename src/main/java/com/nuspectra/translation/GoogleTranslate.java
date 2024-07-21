package com.nuspectra.translation;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// singleton class to access GoogleTranslate.
public enum GoogleTranslate {
    instance;
    private static final Log log = LogFactory.getLog(GoogleTranslate.class);


    // simple english translation without text substitutions
    public String translate(String text, String targetLanguage) {
        return translate(text, "en", targetLanguage, "text");
    }

    public String translate(String text, String sourceLanguage, String targetLanguage, String format) {

        // Instantiates a client
        Translate translate = TranslateOptions.getDefaultInstance().getService();
        Translation translation =
                translate.translate(
                        text,
                        Translate.TranslateOption.sourceLanguage(sourceLanguage),
                        Translate.TranslateOption.targetLanguage(targetLanguage),
                        Translate.TranslateOption.format(format));
        String out = translation.getTranslatedText();

        return out;
    }

    public String translateFormattedString(String text, String sourceLanguage, String targetLanguage) throws Exception {
        String translated = translate(text, sourceLanguage, targetLanguage, "text");
        return translated;

    }


    GoogleTranslate() {
    }


    // Call this to add text that shouldn't be translated.


    public String translateHTML(String srcHtml, String sourceLanguage, String targetLanguage) {

        Translate translate = TranslateOptions.getDefaultInstance().getService();
        Translation translation =
                translate.translate(
                        srcHtml,
                        Translate.TranslateOption.sourceLanguage(sourceLanguage),
                        Translate.TranslateOption.targetLanguage(targetLanguage),
                        Translate.TranslateOption.format("html"));
        String out = translation.getTranslatedText();

        return out;
    }
}
