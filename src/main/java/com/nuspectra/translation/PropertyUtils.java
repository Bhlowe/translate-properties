package com.nuspectra.translation;


import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class PropertyUtils {

    public static Properties readProperties(File propertyFile) throws IOException {
        final Properties basePropery = new Properties();
        InputStream is = new FileInputStream(propertyFile);
        if (is == null) throw new IOException("Property file could not be read " + propertyFile.getAbsolutePath());

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(propertyFile), "UTF-8"))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#")) continue;
                if (!line.contains("="))
                    throw new IOException("Expected = in line:"+line);

                String[] propertyTokens = line.split("=");
                basePropery.put(propertyTokens[0], propertyTokens[1]);
            }
        }
        return basePropery;
    }

    public static void writeProperties(final Properties properties, File destinationFile, boolean sort) throws IOException {
        List<String> keyList = new ArrayList<>();
        for (Object k:properties.keySet())
            keyList.add(k.toString());

        if (sort) Collections.sort(keyList);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(destinationFile))) {
            for (String k : keyList) {
                bufferedWriter.write(k + "=" + properties.getProperty(k) + "\n");
            }
        }
    }


}
