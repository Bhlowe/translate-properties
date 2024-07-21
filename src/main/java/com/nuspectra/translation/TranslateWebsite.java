package com.nuspectra.translation;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class TranslateWebsite {

    final Translate translate = TranslateOptions.getDefaultInstance().getService();
    String[] defaultLanguages = {"ar", "fr", "es", "ru", "hi", "de", "it"};       // jp is failing

    final List<File> files;
    final File destDir; // where to save the translated files. Each will be in a subdirectory named for the language.

    private static final Log log = LogFactory.getLog(TranslateWebsite.class);

    boolean verbose = true;
    int count;
    HashSet<String> languages = new HashSet<>();

    // use for tests to check if key exists.


    public static Set<File> updateHTML(ArrayList<File> files, File destFile, String langs, boolean fix, boolean update, boolean write) throws Exception {
        TranslateWebsite t = new TranslateWebsite(files, destFile, langs);
        return t.run();
    }


    Set<File> getChangedFiles() throws IOException {
        HashSet<File> changed = new HashSet<>();       // Keys to update.
        for (File f : files) {
            File cache = new File(f.getParentFile(), "en");
            String html = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
            String prev = cache.exists() ? FileUtils.readFileToString(cache, StandardCharsets.UTF_8) : null;
            if (html.equals(prev))
                continue;
            changed.add(f);
        }
        return changed;
    }

    private Set<File> run() throws Exception {


        Set<File> changedFiles = getChangedFiles();


        for (File f : files) {

            File cache = getDestFile(f, "en");


            // For all languages desired.. translate the keys.
            for (String l : getLanguages()) {
                File dest = getDestFile(f, l);
                if (!dest.exists() || changedFiles.contains(f)) {
                    boolean changed = translateHtml(l, f, dest);

                    if (changed)
                        changedFiles.add(dest);

                } else {
                    log.info("Skip unchanged " + dest.getAbsolutePath());
                }
            }

            FileUtils.writeStringToFile(cache, FileUtils.readFileToString(f, StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        }

        return changedFiles;
    }


    // string of comma separated languages... eg. en, fr, es
    public void setLanguages(String languages) {
        this.languages.clear();
        for (String l : languages.trim().split(","))
            this.languages.add(l.trim());
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
    protected File getDestFile(File f, String lang) {
        File dir = new File(destDir, lang);
        dir.mkdirs();
        File out = new File(dir, f.getName());
        return out;
    }

    public TranslateWebsite(List<File> files, File destDir, String langs) {
        this.files = files;
        this.destDir = destDir;
        this.languages.addAll(Arrays.asList(langs.split(",")));
    }


    // Do translations for all selected languages and save as a properties file.
    // key set is either a list of keys that have changed translations (and need to be re-translated)
    // or it is all keys.
    public boolean translateHtml(String targetLanguage, File sourceFile, File destFile) throws
            Exception {

        String srcHtml = FileUtils.readFileToString(sourceFile, "UTF-8");
        String dstHtml = srcHtml;
        // File hashFile = hashFile(destFile);     // possible hash of srcHTML

        if (targetLanguage.equals(getSourceLanguage())) {
            log.info("Skipping source language " + targetLanguage);
            return false;
        }


        dstHtml = GoogleTranslate.instance.translateHTML(srcHtml, getSourceLanguage(), targetLanguage);


        FileUtils.writeStringToFile(destFile, dstHtml, StandardCharsets.UTF_8);
        // writeHash(hashFile, srcHtml);
        log.info("Saved " + destFile.getAbsolutePath());


        return true;
    }

}

