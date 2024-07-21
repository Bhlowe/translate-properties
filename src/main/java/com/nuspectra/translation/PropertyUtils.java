package com.nuspectra.translation;


import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PropertyUtils {

    public static Properties readProperties(File propertyFile) throws IOException {
        final Properties props = new Properties();
        InputStream is = new FileInputStream(propertyFile);
        if (is == null) throw new IOException("Property file could not be read " + propertyFile.getAbsolutePath());
        String contents = FileUtils.readFileToString(propertyFile, StandardCharsets.UTF_8);
        int lineNum = 0;

        for (String line : contents.split("\n")) {
            lineNum++;
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) continue;
            if (!line.contains("="))
                throw new IOException("Expected = in line:" + propertyFile.getAbsolutePath() + ":" + lineNum + " " + line);

            String[] propertyTokens = line.split("=");
            String key = propertyTokens[0];
            String val = propertyTokens.length > 1 ? propertyTokens[1] : "";
            props.put(key, val);
        }

        return props;
    }

    public static void writeProperties(final Properties properties, File destinationFile, boolean sort) throws IOException {
        List<String> keyList = new ArrayList<>();
        for (Object k : properties.keySet())
            keyList.add(k.toString());

        if (sort) Collections.sort(keyList);
        String out = "";
        for (String k : keyList) {
            out += k + "=" + properties.getProperty(k) + "\n";
        }
        FileUtils.writeStringToFile(destinationFile, out, StandardCharsets.UTF_8);
        String check = FileUtils.readFileToString(destinationFile, StandardCharsets.UTF_8);
        if (!out.equals(check)) {
            throw new IOException("Expected file to be same: " + out + "\n" + check);
        }
    }

    public static String compareProperties(File f1, File f2) throws IOException {
        String out = "";
        out += "Comparing " + f1.getAbsolutePath() + " and " + f2.getAbsolutePath() + "\n";
        Properties p1 = readProperties(f1);
        Properties p2 = readProperties(f2);
        Set<String> set = new HashSet<>();
        set.addAll(p1.stringPropertyNames());
        set.addAll(p2.stringPropertyNames());

        List<String> keys = new ArrayList<>();
        keys.addAll(set);
        Collections.sort(keys);
        if (p1.size() == p2.size())
            out += "Same size: " + p1.size() + "\n";
        else {
            out += "Diff size: " + p1.size() + " " + p2.size() + "\n";

            for (String k : set) {
                if (p1.get(k) == null)
                    out += "missing from p1: " + k + "\n";
                if (p2.get(k) == null)
                    out += "missing from p2: " + k + "\n";
            }
        }
        int differences = 0;

        for (String k : keys) {
            String s1 = p1.getProperty(k);
            String s2 = p2.getProperty(k);
            if (s1 == null) {
                s1 = "";
                out += "MISSING1:";
            }
            if (s2 == null) {
                s2 = "";
                out += "MISSING2:";
            }

            if (!s1.equals(s2)) {
                out += "diff: " + k + " " + s1 + "!=" + s2 + "\n";
                differences++;
            }
        }

        out += "differences=" + differences;

        return out;
    }

}


