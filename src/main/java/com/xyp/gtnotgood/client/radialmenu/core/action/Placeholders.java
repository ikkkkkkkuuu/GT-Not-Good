package com.xyp.gtnotgood.client.radialmenu.core.action;

import java.util.Map;

public final class Placeholders {

    public static final String PLAYER = "player";
    public static final String DIMENSION = "dim";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String Z = "z";

    private Placeholders() {}

    public static String apply(String text, Map<String, String> values) {
        if (text == null || text.indexOf('{') < 0 || values == null) {
            return text;
        }

        StringBuilder result = new StringBuilder(text.length());
        int index = 0;

        while (index < text.length()) {
            char c = text.charAt(index);
            if (c != '{') {
                result.append(c);
                index++;
                continue;
            }

            int close = text.indexOf('}', index + 1);
            if (close < 0) {
                result.append(text.substring(index));
                break;
            }

            String name = text.substring(index + 1, close);
            String value = values.get(name);
            result.append(value == null ? text.substring(index, close + 1) : value);
            index = close + 1;
        }
        return result.toString();
    }

    public static String[] splitLines(String text) {
        if (text == null) {
            return new String[0];
        }
        String[] raw = text.split("\\r?\\n");
        int count = 0;
        for (String line : raw) {
            if (!line.trim().isEmpty()) {
                count++;
            }
        }
        String[] lines = new String[count];
        int i = 0;
        for (String line : raw) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines[i++] = trimmed;
            }
        }
        return lines;
    }
}
