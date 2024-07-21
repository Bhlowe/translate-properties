package com.nuspectra.translation;

public enum FixTranslation {
    instance;

    String[] knownMacros = {"%s", "%d", "\n"};

    public int count(String text, String find) {
        int index = 0, count = 0, length = find.length();
        while ((index = text.indexOf(find, index)) != -1) {
            index += length;
            count++;
        }
        return count;
    }

    int countMacros(String in) {
        int c = 0;
        for (String m : knownMacros) {
            c += count(in, m);
        }
        return c;
    }

    boolean checkOutputOK(String translated, String orig) {
        int oc = countMacros(orig);
        int tc = countMacros(translated);
        if (oc != tc) {
            System.out.println("Macro Error!\n" + orig + "\n" + translated);
            return false;
        }
        return true;
    }


    private String fixPercent(String s, char c) {
        String n = "%" + c;
        String[] bad = {"% " + c, "% " + Character.toUpperCase(c)};
        for (String b : bad) {
            s = s.replace(b, "%s");
        }
        // make sure there is one space in front of %s (so not, XYZ%s)
        s = s.replace(n, " " + n);
        return s;
    }

    public String fixLine(final String s, String inputString) {
        String copy = s;
        int count = 0;

        for (int x = 0; x < 100; x++) {
            String out = _fixLine(copy, inputString);
            if (out.equals(copy))
                return out;
            count++;
            System.out.println("taking another attempt: " + s);
            copy = out;
        }
        return copy;
    }

    private String _fixLine(String s, String inputString) {
        boolean ok = checkOutputOK(s, inputString);
        assert (ok);

        s = s.replace("\\ N", "\\n");
        s = s.replace("\\ n", "\\n");
        s = s.replace("\\ t", "\\t");
        s = fixPercent(s, 's');
        s = fixPercent(s, 'd');

        s = s.replace(" ...", "...");
        s = s.replace(" \\n", "\\n");
        s = s.replace("\\n ", "\\n");
        s = s.replace("  ", " ");

        s = s.replace('\uFF05', '%'); // full width percent, used in .jp.

        s = s.replace("&quot;", "\"");
        s = s.replace("&#39;", "'");


        s = s.replace("' %d'", "'%d'");
        s = s.replace("' %s'", "'%s'");

        s = s.replace("( %", "(%");


        if (s.contains("&")) {
            // Unescape failed... ?
            System.out.println("& found in output:" + s);
        }

        // s = s.replace(" .", ".");       // This actually is correct in some languages.

        s = s.trim();

        boolean ok2 = checkOutputOK(s, inputString);
        if (!ok2) {
            System.err.println("Failure with macro counts:" + s + " from " + inputString);
            assert (ok2);
        }
        return s;
    }
}
