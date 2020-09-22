package test.nuspectra.translation;

import com.nuspectra.translation.GoogleTranslate;
import org.junit.Assert;
import org.junit.Test;

public class TestGoogleTextTranslation {

    @Test
    public void testGoogleSingleTextTranslation(){
        String translated = GoogleTranslate.instance.translate("Hello world", "fr");
        Assert.assertTrue("Bonjour le monde".equalsIgnoreCase(translated));
    }
}

