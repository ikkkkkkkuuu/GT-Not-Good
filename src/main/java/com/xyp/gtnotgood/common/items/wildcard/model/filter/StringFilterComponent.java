package com.xyp.gtnotgood.common.items.wildcard.model.filter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.nbt.NBTTagCompound;

import gregtech.api.enums.Materials;

/**
 * 字符串通配/正则过滤（兼容旧玩法）：按材料名匹配，支持 * ? 通配和正则元字符。
 */
public final class StringFilterComponent extends AbstractFilterComponent {

    public static final String TYPE = "string";

    private static final String KEY_PATTERN = "Pattern";
    private static final String KEY_EXACT = "Exact";

    private String pattern;
    private boolean exact;
    private transient Pattern compiled;
    private transient String compiledFor;

    public StringFilterComponent(String pattern, boolean whitelist) {
        this(pattern, false, whitelist);
    }

    public StringFilterComponent(String pattern, boolean exact, boolean whitelist) {
        super(whitelist);
        this.pattern = pattern == null ? "" : pattern.trim();
        this.exact = exact;
    }

    public static StringFilterComponent empty() {
        return new StringFilterComponent("", false, true);
    }

    /** 精确名黑名单（用于预览页 X 快速排除某个材料）。 */
    public static StringFilterComponent exactBlacklist(String materialName) {
        return new StringFilterComponent(materialName, true, false);
    }

    public static StringFilterComponent readData(NBTTagCompound data) {
        return new StringFilterComponent(data.getString(KEY_PATTERN), data.getBoolean(KEY_EXACT), readWhitelist(data));
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern == null ? "" : pattern.trim();
    }

    public boolean isExact() {
        return exact;
    }

    @Override
    protected boolean matches(Materials material) {
        if (material == null || material.mName == null) return false;
        String value = pattern;
        if (value.isEmpty()) return false;
        // 归一化：去掉空格，让 "Stainless steel" 能匹配到内部名 "StainlessSteel"
        String normPattern = value.replace(" ", "");
        String normName = material.mName.replace(" ", "");
        if (exact) {
            return normName.equalsIgnoreCase(normPattern);
        }
        Pattern p = compile(normPattern);
        return p != null && p.matcher(normName)
            .find();
    }

    private Pattern compile(String value) {
        if (compiled != null && value.equals(compiledFor)) return compiled;
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern result;
        try {
            if (value.indexOf('*') >= 0 || value.indexOf('?') >= 0) {
                result = Pattern.compile(wildcardToRegex(value), flags);
            } else if (containsRegexMeta(value)) {
                result = Pattern.compile(value, flags);
            } else {
                result = Pattern.compile(Pattern.quote(value), flags);
            }
        } catch (PatternSyntaxException ignored) {
            try {
                result = Pattern.compile(wildcardToRegex(value), flags);
            } catch (PatternSyntaxException ignoredAgain) {
                result = null;
            }
        }
        compiled = result;
        compiledFor = value;
        return result;
    }

    private static boolean containsRegexMeta(String value) {
        for (int i = 0; i < value.length(); i++) {
            if ("\\.^$|()[]{}+".indexOf(value.charAt(i)) >= 0) return true;
        }
        return false;
    }

    private static String wildcardToRegex(String value) {
        StringBuilder builder = new StringBuilder("^");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '*') builder.append(".*");
            else if (c == '?') builder.append('.');
            else builder.append(Pattern.quote(String.valueOf(c)));
        }
        return builder.append('$')
            .toString();
    }

    @Override
    public String describe() {
        return (isWhitelist() ? "+" : "-") + (pattern.isEmpty() ? "*" : pattern);
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = baseData();
        data.setString(KEY_PATTERN, pattern == null ? "" : pattern);
        data.setBoolean(KEY_EXACT, exact);
        return data;
    }
}
