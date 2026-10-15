package com.xyp.gtnotgood.client.radialmenu.core.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LuaSyntax {

    public enum Kind {
        Plain,

        Keyword,

        Declaration,

        Constant,

        Self,

        Api,

        Function,

        Field,
        String,
        Number,
        Comment,
        Operator
    }

    public static final class Token {

        public final int start;
        public final int end;
        public final Kind kind;

        Token(int start, int end, Kind kind) {
            this.start = start;
            this.end = end;
            this.kind = kind;
        }

        public int length() {
            return end - start;
        }

        @Override
        public String toString() {
            return kind + "[" + start + "," + end + ")";
        }
    }

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList("and", "break", "do", "else", "elseif",
        "end", "for", "function", "goto", "if", "in", "not", "or", "repeat", "return", "then", "until", "while"));

    private static final Set<String> CONSTANTS = new HashSet<>(Arrays.asList("nil", "true", "false"));

    private static final Set<String> STANDARD = new HashSet<>(Arrays.asList("string", "table", "math"));

    private static final Set<String> GLOBALS = new HashSet<>(Arrays.asList("chat", "menu", "player", "chunk", "world",
        "inventory", "store", "prompt", "action", "notify", "log", "sleep", "print", "string", "table", "math"));

    private static final Set<String> MEMBERS = new HashSet<>(
        Arrays.asList("send", "await", "awaitAll", "line", "open", "close", "update", "run", "maxEntries", "name",
            "dim", "x", "y", "z", "chunkX", "chunkZ", "xInChunk", "zInChunk", "yaw", "pitch", "facing", "of", "offset",
            "lookingAt", "time", "day", "isDay", "items", "count", "has", "get", "set", "health", "food", "air", "held",
            "meta", "slot", "kind", "toWorld", "size", "key", "label", "icon", "color", "onPick"));

    private static final String OPERATOR_CHARS = "+-*/%^#=~<>(){}[];:,.";

    private LuaSyntax() {}

    public static boolean isGlobal(String name) {
        return GLOBALS.contains(name);
    }

    public static boolean isMember(String name) {
        return MEMBERS.contains(name);
    }

    public static List<List<Token>> tokenize(String text) {
        return tokenize(Arrays.asList((text == null ? "" : text).split("\r?\n", -1)));
    }

    public static List<List<Token>> tokenize(List<String> lines) {
        List<List<Token>> all = new ArrayList<>();
        if (lines == null) {
            return all;
        }

        int longLevel = -1;
        boolean longIsComment = false;

        for (String raw : lines) {
            String line = raw == null ? "" : raw;
            List<Token> tokens = new ArrayList<>();
            int index = 0;
            int plainFrom = -1;

            while (index < line.length()) {
                if (longLevel >= 0) {
                    index = flushPlain(tokens, plainFrom, index);
                    plainFrom = -1;
                    int close = indexOfLongClose(line, index, longLevel);
                    int end = close < 0 ? line.length() : close + longLevel + 2;
                    tokens.add(new Token(index, end, longIsComment ? Kind.Comment : Kind.String));
                    index = end;
                    if (close >= 0) {
                        longLevel = -1;
                    }
                    continue;
                }

                char c = line.charAt(index);

                if (c == '-' && index + 1 < line.length() && line.charAt(index + 1) == '-') {
                    plainFrom = flushPlainAt(tokens, plainFrom, index);
                    int level = longBracketLevel(line, index + 2);
                    if (level >= 0) {
                        int openerEnd = index + 2 + level + 2;
                        tokens.add(new Token(index, openerEnd, Kind.Comment));
                        longLevel = level;
                        longIsComment = true;
                        index = openerEnd;
                    } else {
                        tokens.add(new Token(index, line.length(), Kind.Comment));
                        index = line.length();
                    }
                    continue;
                }

                if (c == '[') {
                    int level = longBracketLevel(line, index);
                    if (level >= 0) {
                        plainFrom = flushPlainAt(tokens, plainFrom, index);
                        int openerEnd = index + level + 2;
                        tokens.add(new Token(index, openerEnd, Kind.String));
                        longLevel = level;
                        longIsComment = false;
                        index = openerEnd;
                        continue;
                    }
                }

                if (c == '"' || c == '\'') {
                    plainFrom = flushPlainAt(tokens, plainFrom, index);
                    int end = endOfShortString(line, index);
                    tokens.add(new Token(index, end, Kind.String));
                    index = end;
                    continue;
                }

                if (isDigit(c) || (c == '.' && index + 1 < line.length() && isDigit(line.charAt(index + 1)))) {
                    plainFrom = flushPlainAt(tokens, plainFrom, index);
                    int end = endOfNumber(line, index);
                    tokens.add(new Token(index, end, Kind.Number));
                    index = end;
                    continue;
                }

                if (isWordStart(c)) {
                    plainFrom = flushPlainAt(tokens, plainFrom, index);
                    int end = index;
                    while (end < line.length() && isWordPart(line.charAt(end))) {
                        end++;
                    }
                    tokens.add(new Token(index, end,
                        wordKind(line.substring(index, end), ownerBeforeDot(line, index), isCalled(line, end))));
                    index = end;
                    continue;
                }

                if (OPERATOR_CHARS.indexOf(c) >= 0) {
                    plainFrom = flushPlainAt(tokens, plainFrom, index);
                    int end = index;
                    while (end < line.length() && OPERATOR_CHARS.indexOf(line.charAt(end)) >= 0) {
                        end++;
                    }
                    tokens.add(new Token(index, end, Kind.Operator));
                    index = end;
                    continue;
                }

                if (plainFrom < 0) {
                    plainFrom = index;
                }
                index++;
            }

            flushPlain(tokens, plainFrom, line.length());
            all.add(Collections.unmodifiableList(tokens));
        }
        return all;
    }

    private static String ownerBeforeDot(String line, int wordStart) {
        int index = skipSpacesBack(line, wordStart - 1);
        if (index < 0 || line.charAt(index) != '.' || (index > 0 && line.charAt(index - 1) == '.')) {
            return null;
        }
        int end = skipSpacesBack(line, index - 1) + 1;
        int start = end;
        while (start > 0 && isWordPart(line.charAt(start - 1))) {
            start--;
        }
        return line.substring(start, end);
    }

    private static int skipSpacesBack(String line, int from) {
        int index = from;
        while (index >= 0 && (line.charAt(index) == ' ' || line.charAt(index) == '\t')) {
            index--;
        }
        return index;
    }

    private static boolean isCalled(String line, int wordEnd) {
        int index = wordEnd;
        while (index < line.length() && (line.charAt(index) == ' ' || line.charAt(index) == '\t')) {
            index++;
        }
        return index < line.length() && line.charAt(index) == '(';
    }

    private static Kind wordKind(String word, String owner, boolean called) {
        if (KEYWORDS.contains(word)) {
            return Kind.Keyword;
        }
        if ("local".equals(word)) {
            return Kind.Declaration;
        }
        if (CONSTANTS.contains(word)) {
            return Kind.Constant;
        }
        if ("self".equals(word)) {
            return Kind.Self;
        }
        if (owner != null) {
            boolean known = MEMBERS.contains(word);
            if (GLOBALS.contains(owner) && !STANDARD.contains(owner) && !known) {
                return Kind.Plain;
            }
            if (called) {
                return Kind.Function;
            }
            return known ? Kind.Field : Kind.Plain;
        }
        if (called) {
            return Kind.Function;
        }
        return GLOBALS.contains(word) ? Kind.Api : Kind.Plain;
    }

    private static int longBracketLevel(String line, int at) {
        if (at >= line.length() || line.charAt(at) != '[') {
            return -1;
        }
        int level = 0;
        int index = at + 1;
        while (index < line.length() && line.charAt(index) == '=') {
            level++;
            index++;
        }
        return index < line.length() && line.charAt(index) == '[' ? level : -1;
    }

    private static int indexOfLongClose(String line, int from, int level) {
        StringBuilder closer = new StringBuilder("]");
        for (int i = 0; i < level; i++) {
            closer.append('=');
        }
        closer.append(']');
        return line.indexOf(closer.toString(), from);
    }

    private static int endOfShortString(String line, int start) {
        char quote = line.charAt(start);
        int index = start + 1;
        while (index < line.length()) {
            char c = line.charAt(index);
            if (c == '\\') {
                index += 2;
                continue;
            }
            index++;
            if (c == quote) {
                return index;
            }
        }
        return line.length();
    }

    private static int endOfNumber(String line, int start) {
        int index = start;
        boolean hex = line.startsWith("0x", start) || line.startsWith("0X", start);
        if (hex) {
            index += 2;
        }
        while (index < line.length()) {
            char c = line.charAt(index);
            if (isDigit(c) || c == '.' || (hex && isHexDigit(c))) {
                index++;
            } else if (!hex && (c == 'e' || c == 'E')) {
                index++;
                if (index < line.length() && (line.charAt(index) == '+' || line.charAt(index) == '-')) {
                    index++;
                }
            } else {
                break;
            }
        }
        return index;
    }

    private static int flushPlainAt(List<Token> tokens, int plainFrom, int index) {
        flushPlain(tokens, plainFrom, index);
        return -1;
    }

    private static int flushPlain(List<Token> tokens, int plainFrom, int until) {
        if (plainFrom >= 0 && until > plainFrom) {
            tokens.add(new Token(plainFrom, until, Kind.Plain));
        }
        return until;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isWordStart(char c) {
        return c == '_' || Character.isLetter(c);
    }

    private static boolean isWordPart(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }
}
