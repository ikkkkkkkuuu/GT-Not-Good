package com.xyp.gtnotgood.client.radialmenu.core.script;

import java.util.List;

import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;

public final class ScriptIcons {

    private static final String[] PREFIXES = { "sprite", "file", "effect", "player", "item" };

    private ScriptIcons() {}

    public static String spell(IconSpec icon) {
        if (icon == null || icon.id == null || icon.id.trim().isEmpty()) {
            return "\"\"";
        }

        boolean tintable = icon.kind == IconSpec.Kind.Sprite || icon.kind == IconSpec.Kind.File;
        if (tintable && icon.color != null && !icon.color.trim().isEmpty()) {
            return "{ kind = " + quote(icon.kind.name().toLowerCase())
                + ", id = "
                + quote(icon.id)
                + ", color = "
                + quote(icon.color.trim())
                + " }";
        }
        return quote(oneString(icon));
    }

    private static String oneString(IconSpec icon) {
        switch (icon.kind) {
            case Sprite:
                return "sprite:" + icon.id;
            case File:
                return "file:" + icon.id;
            case Effect:
                return "effect:" + icon.id;
            case Player:
                return "player:" + icon.id;
            case Item:
            default:

                String name = icon.meta == 0 ? icon.id : icon.id + ":" + icon.meta;
                return looksPrefixed(icon.id) ? "item:" + name : name;
        }
    }

    private static boolean looksPrefixed(String id) {
        for (String prefix : PREFIXES) {
            if (id.regionMatches(true, 0, prefix + ":", 0, prefix.length() + 1)) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public static LuaSyntax.Token stringAt(List<LuaSyntax.Token> lineTokens, String line, int column) {
        if (lineTokens == null || line == null) {
            return null;
        }
        for (LuaSyntax.Token token : lineTokens) {
            if (token.kind != LuaSyntax.Kind.String || token.start >= line.length() || token.end > line.length()) {
                continue;
            }
            char opener = line.charAt(token.start);
            if (opener != '"' && opener != '\'') {
                continue;
            }
            boolean closed = token.length() >= 2 && line.charAt(token.end - 1) == opener;
            if (column > token.start && (column < token.end || (!closed && column == token.end))) {
                return token;
            }
        }
        return null;
    }
}
