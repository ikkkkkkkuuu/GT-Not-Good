package com.xyp.gtnotgood.client.radialmenu.core.text;

import java.util.List;

public final class IndentGuides {

    private IndentGuides() {}

    public static int[] depths(List<String> lines) {
        int count = lines.size();
        int[] depths = new int[count];
        boolean[] blank = new boolean[count];

        for (int i = 0; i < count; i++) {
            String line = lines.get(i);
            int spaces = 0;
            while (line != null && spaces < line.length() && line.charAt(spaces) == ' ') {
                spaces++;
            }
            blank[i] = line == null || spaces == line.length();
            depths[i] = blank[i] ? 0 : spaces;
        }

        int above = 0;
        int i = 0;
        while (i < count) {
            if (!blank[i]) {
                above = depths[i];
                i++;
                continue;
            }
            int end = i;
            while (end < count && blank[end]) {
                end++;
            }
            int below = end < count ? depths[end] : 0;
            int depth = Math.max(above, below);
            for (int line = i; line < end; line++) {
                depths[line] = depth;
            }
            i = end;
        }
        return depths;
    }

    public static int count(int depth, int indentWidth) {
        if (depth <= 0 || indentWidth <= 0) {
            return 0;
        }
        return (depth + indentWidth - 1) / indentWidth;
    }
}
