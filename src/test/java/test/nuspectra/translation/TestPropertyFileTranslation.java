package test.nuspectra.translation;

import com.nuspectra.translation.TranslateProperties;
import org.junit.Test;

import java.io.File;

public class TestPropertyFileTranslation {

    @Test
    public void testPropertyFileTranslationFromEnglishToAllTheSupportedLanguages() throws Exception {
        if (!TranslateProperties.hasAPIKey())
        {
            return;
        }

        File test1 = new File("test", "base.properties");
        String args[] = {test1.getAbsolutePath(), "ar,de,es,fr"};
        TranslateProperties.main(args);
    }
}

