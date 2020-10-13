package test.nuspectra.translation;

import com.nuspectra.translation.GoogleTranslate;
import com.nuspectra.translation.TranslateProperties;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class TestGoogleTextTranslation {

    @Test
    public void testGoogleSingleTextTranslation(){
        if (!TranslateProperties.hasAPIKey())
        {
            System.out.println("Skipping test: GOOGLE_API_KEY not defined");
            return;
        }

        String translated = GoogleTranslate.instance.translate("Hello world", "fr");
        Assert.assertTrue("Bonjour le monde".equalsIgnoreCase(translated));
    }
}

