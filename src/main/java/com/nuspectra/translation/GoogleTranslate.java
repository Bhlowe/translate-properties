package com.nuspectra.translation;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.HashMap;
import java.util.Map;

// singleton class to access GoogleTranslate.
public enum GoogleTranslate {
    instance;

    final HashMap<String, String> escapeMap = new HashMap<>();  // strings that shall not be translated. (such as %s)

    // simple english translation without text substitutions
    public String translate(String text, String targetLanguge) {
        return translate(text, "en", targetLanguge, "text");
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


    public String translateFormattedString(String string, String sourceLanguage, String targetLanguage) throws Exception {
        String text = escapeString(string);

        try {

            String translated = translate(text, sourceLanguage, targetLanguage, "html");
            boolean ok1 = FixTranslation.instance.checkOutputOK(string, translated);
            String unescaped = unescapeString(translated);
            unescaped = FixTranslation.instance.fixLine(unescaped, text);
            boolean ok2 = FixTranslation.instance.checkOutputOK(string, translated);

            if (!ok1 || !ok2)
                throw new Exception("Failed to preserve macros... failing");

            return unescaped;
        } catch (Exception th) {
            System.err.println("Error translating to " + targetLanguage + " " + text);
            throw th;
        }
    }


    GoogleTranslate() {
        initEscapedSubstitutionMap();
    }

    // this is not an all-inclusive list. too many to know: %-d, % s, etc.
    // Typical string substitutions. you need to make sure the ones used in your properties file are in this list.
    // TODO: Add logic to detect when a string contains an unmapped substitution
    protected void initEscapedSubstitutionMap() {
        String format_specifiers[] = {"%s", "%d", "%f", "%n", "%a", "%%", "%S", "%e", "%E"};
        for (String s : format_specifiers) {
            doNotTranslate(s);
        }
        escapeMap.put("\\n", "<br>");
        escapeMap.put("\\t", "  ");

        // Alternate method, but could fail (?) if non-arabic numerals are translated.
        // But might give better translations so google knows a number is part of the translation
        if (false) {
            escapeMap.put("%f", "12345.98765");
            escapeMap.put("%d", "54321");
        }

    }

    // Call this to add text that shouldn't be translated.
    public void doNotTranslate(String text)
    {
        escapeMap.put(text, "<span translate=\"no\">" + text + "+</span>");
    }

    public String escapeString(String line) {
        if (escapeMap.isEmpty()) initEscapedSubstitutionMap();

        String out = line;
        for (Map.Entry<String, String> e : escapeMap.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }

        // now there may be other strings to replace..
        // for instance, '%s' often gets translated to ' %s ' which we can "fix" here if we know all of the quirks of google translate.
        // so replace ' %s ', "'%s'"


        return out;
    }

    public String unescapeString(String line) {
        String out = StringEscapeUtils.unescapeHtml4(line);

        for (Map.Entry<String, String> e : escapeMap.entrySet()) {
            out = out.replace(e.getValue(), e.getKey());
        }
        // convert &#nnn; escaped characters.
        return out;
    }



}
