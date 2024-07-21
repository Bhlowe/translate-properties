package com.nuspectra.translation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;


public class PropertiesToHTML {
    private static final Log log = LogFactory.getLog(PropertiesToHTML.class);

    public static String toHTML(String content) {
        String html = "<html><body>";
        for (String line : content.split("\n")) {
            if (line.startsWith("#")) continue;
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("=");
            if (parts.length != 2) continue;
            String key = parts[0].trim();
            String value = parts[1].trim();
            String entry = "<pre id='" + key + "'>";
            String escaped = escape(value);
            entry += escaped;
            entry += "</pre>";
            html += entry + "\n";
        }
        html += "</body></html>";
        return html;
    }

    private static String escape(String value) {


        if (value.contains("%")) {
            // log.info("percent: "+value);
        }


        return value;
    }

    public static String toHTML(File propertiesFile) throws IOException {
        return toHTML(FileUtils.readFileToString(propertiesFile, StandardCharsets.UTF_8));
    }


    public static HashMap<String, String> htmlToMap(String html) {
        HashMap<String, String> preTagsMap = new HashMap<>();
        Document doc = Jsoup.parse(html);

        Elements preTags = doc.select("pre");

        for (Element preTag : preTags) {
            String id = preTag.id();
            String text = preTag.text().trim();
            preTagsMap.put(id, text);
        }

        return preTagsMap;
    }

    public static String htmlToProperties(String html) {
        String out = "";
        Document doc = Jsoup.parse(html);

        Elements preTags = doc.select("pre");

        for (Element preTag : preTags) {
            String id = preTag.id();
            String text = preTag.text().trim();
            out += id + "=" + text + "\n";
        }

        return out;
    }


}
