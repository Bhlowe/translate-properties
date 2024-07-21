package com.nuspectra.translation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;


public class TranslateProperties {

    final Properties baseProperties;    // input key=value properties

    String[] defaultLanguages = {"ar", "fr", "es", "ru", "hi", "de"};       // jp is failing

    final File dir; // directory of propertyFiles to work on.
    final File inputFile;       // basePropertyFile
    final String baseName;      // If basePropertyFile is /path/foobar.properties, baseName = foobar.
    boolean verbose = true;
    int count;
    HashSet<String> languages = new HashSet<>();
    boolean requireUpperCaseKeys = true;

    // use for tests to check if key exists.
    public static boolean hasAPIKey() {
        String k = System.getenv("GOOGLE_API_KEY");
        boolean hasKey = k != null && !k.isEmpty();
        if (!hasKey) {
            System.out.println("Error: GOOGLE_API_KEY not defined");
        }
        return hasKey;
    }


    // TranslateProperties.main([file], [languages])
    // File of base properties file to translate (defaults to test directory.)
    // Languages is a comma separated list of language identifiers. (defaults to defaultLanguages)
    public static void main(String[] args) {
        try {
            File f = args.length > 0 ? new File(args[0]) : new File("test", "base.properties");
            String languages = null;
            if (args.length > 1)
                languages = args[1];   // if language list is provided, use that, otherwise use defaultLanguages

            boolean fix = true; // re-run the fix code. Useful to run after fixtranslation code has been modified.
            boolean update = false;
            boolean write = true;

            updateProperties(f, languages, fix, update, write);

        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static Set<String> updateProperties(File mainProperty, String languages) throws Exception {
        return updateProperties(mainProperty, languages, true, true, true);
    }

    public static Set<String> updateProperties(File mainProperty, String languages, boolean fix, boolean update, boolean saveChanges) throws Exception {

        if (!mainProperty.exists())
            throw new FileNotFoundException("fnf:" + mainProperty.getAbsolutePath());

        TranslateProperties tp = new TranslateProperties(mainProperty);
        // set languages to translate.
        if (languages != null && languages.length() > 1) {
            tp.setLanguages(languages);
        }

        return tp.run(fix, update, saveChanges);
    }

    Set<String> getChangedStrings() throws IOException {
        HashSet<String> keySet = new HashSet<>();       // Keys to update.

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

        return keySet;
    }

    private Set<String> run(boolean fix, boolean update, boolean saveChanges) throws Exception {
        if (fix) {
            fixTranslations(saveChanges);
        }

        Set<String> keySet = getChangedStrings();
        if (update) {

            // For all languages desired.. translate the keys.
            for (String l : getLanguages())
                translateStrings(l, keySet, saveChanges);
        }

        // save a copy of the properties so we know which have been modified next time run.
        if (saveChanges)
            writeProperties(baseProperties, getChangedFile());
        System.out.println("write=" + saveChanges + " fix=" + fix + " translated " + count + "\n" + inspect());
        return keySet;
    }

    public String inspect() throws IOException {

        String out = getSourceLanguage() + "\t" + baseProperties.size() + "\t" + wordCount(baseProperties) + "\n";

        for (String l : getLanguages()) {
            Properties p = getProperties(l);
            out += l + "\t" + p.size() + "\t" + wordCount(p) + "\n";
        }

        return out;

    }

    // string of comma separated languages... eg. en, fr, es
    public void setLanguages(String languages) {
        this.languages.clear();
        for (String l : languages.trim().split(","))
            this.languages.add(l.trim());
    }

    // Translate strings that have changed since last translation
    // Also translates any missing translations.
    public Set<String> updateAll() throws Exception {
        return run(true, true, true);
    }


    // override or change if source language is not english.
    public String getSourceLanguage() {
        return "en";
    }

    // return list of ISO-639-1 language identifiers, from:
    // https://cloud.google.com/translate/docs/languages
    // This should NOT return the source language (usually english!)
    public Collection<String> getLanguages() {
        if (languages.isEmpty())
            languages.addAll(Arrays.asList(defaultLanguages.clone()));

        assert (!languages.contains(getSourceLanguage()));

        return languages;
    }


    // this saves the {baseName}_en.properties file. It could be saved elsewhere
    protected File getChangedFile() {
        return getFileDest(getSourceLanguage());
    }

/*
    private Set<String> translateOrUpdateAll(boolean updateOnly, boolean cleanOnly) throws Exception {
        HashSet<String> keySet = new HashSet<>();       // Keys to update.
        if (!updateOnly) {
            for (Object o : baseProperties.keySet())
                keySet.add(o.toString());       // properties use object as key, we use "string" so addAll doesn't work.
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
        return keySet;
    }*/

    public Properties getProperties(String lang) throws IOException {
        if (lang.isEmpty())
            return baseProperties;
        assert (languages.contains(lang));
        File file = getFileDest(lang);
        if (file.exists()) {
            return PropertyUtils.readProperties(file);
        }
        return new Properties();
    }


    public Collection<String> fixTranslations(boolean saveChanges) throws IOException {
        Collection<String> changes = new HashSet<>();


        for (String l : getLanguages()) {
            File file = getFileDest(l);
            if (file.exists()) {
                Properties props = PropertyUtils.readProperties(file);


                int changed = fixTranslations(file.getName(), props);
                if (changed > 0) {
                    changes.add(l);
                    System.out.println("Changed " + file.getAbsolutePath() + " = " + changed + ", saveChanges=" + saveChanges);
                    if (saveChanges) {
                        // write the properties.
                        writeProperties(props, file);
                    }
                }
            }
        }


        return changes;
    }

    public Collection<String> checkInputs() throws Exception {
        Collection<String> changes = new HashSet<>();


        for (String l : getLanguages()) {
            File file = getFileDest(l);
            if (file.exists()) {
                checkInput(file);
            }
        }


        return changes;
    }

    private void checkInput(File file) throws Exception {
        if (file.exists()) {
            Properties props = PropertyUtils.readProperties(file);
            for (Object k : props.keySet()) {
                String key = k.toString();
                if (!key.equals(key.toUpperCase()))
                    throw new Exception("Expected uppercase key:" + key + " in " + file);

            }
        }
    }


    private int wordCount(Properties properties) {
        int wc = 0;
        for (Object k : properties.keySet()) {
            String v = properties.getProperty(k.toString());
            if (!v.isEmpty()) {
                if (v.contains("  ")) {
                    v = v.replace("  ", " ");
                }
                wc++;
                for (char c : v.toCharArray()) {
                    if (Character.isWhitespace(c))
                        wc++;
                }
            }
        }
        return wc;
    }


    private int fixTranslations(String which, Properties properties) {
        int count = 0;

        HashSet<String> toDelete = new HashSet<>();

        for (Object k : properties.keySet()) {
            String key = k.toString();
            boolean badCase = false;
            if (!key.equals(key.toUpperCase())) {
                badCase = true;
                if (requireUpperCaseKeys)
                    key = key.toUpperCase();
            }


            String v = properties.getProperty(key);
            String o = baseProperties.getProperty(key);
            if (o == null) {
                System.out.println("Key no longer in base properties:" + k + " but exists in translated file. Will be deleted:\n" + k + "->" + v);
                toDelete.add(k.toString());
                count++;
                continue;
            }

            String c = FixTranslation.instance.fixLine(v, baseProperties.getProperty(k.toString()));
            if (!v.equals(c) || badCase) {
                count++;
                if (verbose)
                    System.out.println(which + " " + k + " fixed from/to\n" + v + "\n" + c);
                properties.put(k.toString(), c);
            }

            if (c.contains(" !")) {
                System.out.println("Found non-breaking ( !) space in " + k+" for "+which);
                toDelete.add(k.toString());
            }

        }

        for (String k : toDelete)
            properties.remove(k);

        return count;
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
    public Properties translateStrings(String targetLanguage, Set<String> mustUpdateKeySet, boolean saveChanges) throws
            Exception {
        File targetFile = getFileDest(targetLanguage);
        Properties p = targetFile.exists() ? PropertyUtils.readProperties(targetFile) : new Properties();
        assert (!targetLanguage.equals(getSourceLanguage()));
        if (!targetLanguage.equals(getSourceLanguage())) {
            for (Map.Entry<Object, Object> e : baseProperties.entrySet()) {
                String key = e.getKey().toString();
                String value = e.getValue().toString();
                String foreignValue = p.getProperty(key);
                boolean needTranslation = mustUpdateKeySet.contains(key) || foreignValue == null || foreignValue.contains("&#") || foreignValue.isEmpty();
                if (key.equals("COMMAND_COPY") && targetLanguage.equals("ja"))
                    System.currentTimeMillis();

                if (needTranslation) {
                    try {
                        String translatedString = GoogleTranslate.instance.translateFormattedString(value, getSourceLanguage(), targetLanguage);
                        p.put(key, translatedString);
                        count++;
                        if (verbose) {
                            System.out.println(count + ". " + key + ":" + value + " -> " + translatedString);
                        }
                    } catch (Throwable th) {
                        System.err.println("Unable to translate:" + value);
                    }
                }
            }
        }
        if (saveChanges)
            writeProperties(p, targetFile);
        return p;
    }


}

