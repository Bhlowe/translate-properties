package com.nuspectra.translation;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class TranslateProperties {

    final Properties baseProperties;

    final Translate translate = TranslateOptions.getDefaultInstance().getService();
    String defaultLanguages[] = {"ar", "fr", "es", "ru", "hi", "de"};       // jp is failing

    final File dir; // directory of propertyFiles to work on.
    final File inputFile;       // basePropertyFile
    final String baseName;      // If basePropertyFile is /path/foobar.properties, baseName = foobar.
    boolean verbose = true;
    int count;
    HashSet<String> languages = new HashSet<>();

    // use for tests to check if key exists.
    public static boolean hasAPIKey()
    {
        String k = System.getenv("GOOGLE_API_KEY");
        boolean hasKey =  k!=null && !k.isEmpty();
        if (!hasKey)
        {
            System.out.println("Warning: GOOGLE_API_KEY not defined");
        }
        return hasKey;
    }

    // TranslateProperties.main([file], [languages])
    // File of base properties file to translate (defaults to test directory.)
    // Languages is comma separated list of language identifiers. (defaults to defaultLanguages)
    public static void main(String[] args) {
        try {
            File f = args.length > 0 ? new File(args[0]) : new File("test");
            if (f.exists()) {
                TranslateProperties tp = new TranslateProperties(f);
                // set languages to translate.
                if (args.length > 1) {
                    for (String l : args[1].split(","))
                        tp.languages.add(l.trim());
                }
                tp.updateAll();
            } else {
                System.out.println("Specify source properties file to be translated. File not found: "+f.getAbsolutePath());
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    // Translate strings that have changed since last translation
    // Also translates any missing translations.
    public void updateAll() throws Exception {
        translateOrUpdateAll(true);
    }

    // Translate all strings, replacing any existing ones.
    public void translateAll() throws Exception {
        translateOrUpdateAll(false);
    }

    // override or change if source language is not english.
    public String getSourceLanguage() {
        return "en";
    }

    // return list of ISO-639-1 language identifiers, from:
    // https://cloud.google.com/translate/docs/languages
    public Collection<String> getLanguages() {
        if (languages.isEmpty())
            languages.addAll(Arrays.asList(defaultLanguages.clone()));
        return languages;
    }

    // this saves the {baseName}_en.properties file. It could be saved elsewhere
    protected File getChangedFile()
    {
        return getFileDest(getSourceLanguage());
    }


    private void translateOrUpdateAll(boolean updateOnly) throws Exception {

        HashSet<String> keySet = new HashSet<>();       // Keys to update.
        if (!updateOnly) {
            for (Object o : baseProperties.keySet())
                keySet.add(o.toString());

        } else {

            // get last translated list. Typically, baseName_en.properties.
            File diffFile = getFileDest(getSourceLanguage());
            if (diffFile.exists()) {
                Properties lastSavedProperties = PropertyUtils.readProperties(diffFile);
                Set<Map.Entry<Object, Object>> entrySet = baseProperties.entrySet();
                for (Map.Entry e : entrySet) {

                    String key = e.getKey().toString();
                    String value = e.getValue().toString();
                    String previous = lastSavedProperties.getProperty(key);
                    if (previous == null || !previous.equals(value)) {
                        keySet.add(key);
                    }
                }
                System.out.println("Found " + keySet.size() + " changed keys.");
            } else {
                // add all keys.
                for (Object o : baseProperties.keySet())
                    keySet.add(o.toString());
            }
        }

        // For all languages desired.. translate the keys.
        for (String l : getLanguages())
            if (!l.equals(getSourceLanguage()))
                translateStrings(l, keySet);


        // save a copy of the properties so we know which have been modified next time run.
        writeProperties(baseProperties, getChangedFile());

        System.out.println("Updated. Translated " + count);
    }

    public TranslateProperties(File baseFile) throws Exception {
        this.dir = baseFile.getParentFile();
        inputFile = baseFile;
        baseName = inputFile.getName().substring(0, inputFile.getName().lastIndexOf(".properties"));
        baseProperties = PropertyUtils.readProperties(baseFile);
    }

    public void writeProperties(final Properties newProperties, File destinationFile) throws IOException {
        PropertyUtils.writeProperties(newProperties, destinationFile, true);
    }

    public File getFileDest(String language_code) {
        String name = baseName + "_" + language_code;
        name += ".properties";
        File targetFile = new File(dir, name);
        return targetFile;
    }

    // Do translations for all selected languages and save as a properties file.
    // key set is either a list of keys that have changed translations (and need to be re-translated)
    // or it is all keys.
    public Properties translateStrings(String targetLanguage, Set<String> keySet) throws Exception {
        File targetFile = getFileDest(targetLanguage);
        Properties p = targetFile.exists() ? PropertyUtils.readProperties(targetFile) : new Properties();
        assert (!targetLanguage.equals(getSourceLanguage()));
        if (!targetLanguage.equals(getSourceLanguage())) {
            for (Map.Entry<Object, Object> e : baseProperties.entrySet()) {
                String key = e.getKey().toString();
                String value = e.getValue().toString();
                if (keySet.contains(key)||!p.containsKey(key)) {
                    String translatedString = GoogleTranslate.instance.translateFormattedString(value, getSourceLanguage(), targetLanguage);
                    p.put(key, translatedString);
                    count++;
                    if (verbose) {
                        System.out.println(count + ". " + key + ":" + value + " -> " + translatedString);
                    }
                }
            }
        }
        writeProperties(p, targetFile);
        return p;
    }

}

