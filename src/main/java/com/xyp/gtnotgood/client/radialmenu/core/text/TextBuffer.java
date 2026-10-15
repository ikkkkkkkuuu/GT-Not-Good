package com.xyp.gtnotgood.client.radialmenu.core.text;

import java.util.ArrayList;
import java.util.List;

public final class TextBuffer {

    public static final class Span {

        public final int startLine;
        public final int startColumn;
        public final int endLine;
        public final int endColumn;

        Span(int startLine, int startColumn, int endLine, int endColumn) {
            this.startLine = startLine;
            this.startColumn = startColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;
        }

        public boolean isEmpty() {
            return startLine == endLine && startColumn == endColumn;
        }
    }

    private final List<String> lines = new ArrayList<>();

    private int caretLine;
    private int caretColumn;

    private int anchorLine;
    private int anchorColumn;

    public TextBuffer(String text) {
        setText(text);
    }

    public void setText(String text) {
        lines.clear();
        for (String line : (text == null ? "" : text).split("\r?\n", -1)) {
            lines.add(line);
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        caretLine = 0;
        caretColumn = 0;
        clearSelection();
    }

    public String text() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                text.append('\n');
            }
            text.append(lines.get(i));
        }
        return text.toString();
    }

    public List<String> lines() {
        return lines;
    }

    public int lineCount() {
        return lines.size();
    }

    public String line(int index) {
        return index < 0 || index >= lines.size() ? "" : lines.get(index);
    }

    public int caretLine() {
        return caretLine;
    }

    public int caretColumn() {
        return caretColumn;
    }

    public boolean hasSelection() {
        return caretLine != anchorLine || caretColumn != anchorColumn;
    }

    public Span selection() {
        if (anchorLine < caretLine || (anchorLine == caretLine && anchorColumn <= caretColumn)) {
            return new Span(anchorLine, anchorColumn, caretLine, caretColumn);
        }
        return new Span(caretLine, caretColumn, anchorLine, anchorColumn);
    }

    public String selectedText() {
        Span span = selection();
        if (span.isEmpty()) {
            return "";
        }
        if (span.startLine == span.endLine) {
            return line(span.startLine).substring(span.startColumn, span.endColumn);
        }
        StringBuilder text = new StringBuilder(line(span.startLine).substring(span.startColumn));
        for (int i = span.startLine + 1; i < span.endLine; i++) {
            text.append('\n').append(line(i));
        }
        return text.append('\n').append(line(span.endLine).substring(0, span.endColumn)).toString();
    }

    public void clearSelection() {
        anchorLine = caretLine;
        anchorColumn = caretColumn;
    }

    public void selectAll() {
        anchorLine = 0;
        anchorColumn = 0;
        caretLine = lines.size() - 1;
        caretColumn = line(caretLine).length();
    }

    public void moveTo(int lineIndex, int column, boolean select) {
        caretLine = clamp(lineIndex, 0, lines.size() - 1);
        caretColumn = clamp(column, 0, line(caretLine).length());
        if (!select) {
            clearSelection();
        }
    }

    public void moveLeft(boolean select) {
        if (!select && hasSelection()) {
            Span span = selection();
            moveTo(span.startLine, span.startColumn, false);
            return;
        }
        if (caretColumn > 0) {
            moveTo(caretLine, caretColumn - 1, select);
        } else if (caretLine > 0) {
            moveTo(caretLine - 1, line(caretLine - 1).length(), select);
        } else if (!select) {
            clearSelection();
        }
    }

    public void moveRight(boolean select) {
        if (!select && hasSelection()) {
            Span span = selection();
            moveTo(span.endLine, span.endColumn, false);
            return;
        }
        if (caretColumn < line(caretLine).length()) {
            moveTo(caretLine, caretColumn + 1, select);
        } else if (caretLine < lines.size() - 1) {
            moveTo(caretLine + 1, 0, select);
        } else if (!select) {
            clearSelection();
        }
    }

    public void moveUp(int rows, boolean select) {
        moveTo(caretLine - rows, caretColumn, select);
    }

    public void moveDown(int rows, boolean select) {
        moveTo(caretLine + rows, caretColumn, select);
    }

    public void moveToLineStart(boolean select) {
        String current = line(caretLine);
        int firstText = 0;
        while (firstText < current.length() && isSpace(current.charAt(firstText))) {
            firstText++;
        }
        moveTo(caretLine, caretColumn == firstText ? 0 : firstText, select);
    }

    public void moveToLineEnd(boolean select) {
        moveTo(caretLine, line(caretLine).length(), select);
    }

    public void moveToStart(boolean select) {
        moveTo(0, 0, select);
    }

    public void moveToEnd(boolean select) {
        moveTo(lines.size() - 1, line(lines.size() - 1).length(), select);
    }

    public void moveWordLeft(boolean select) {
        if (caretColumn == 0) {
            moveLeft(select);
            return;
        }
        String current = line(caretLine);
        int column = caretColumn;
        while (column > 0 && !isWord(current.charAt(column - 1))) {
            column--;
        }
        while (column > 0 && isWord(current.charAt(column - 1))) {
            column--;
        }
        moveTo(caretLine, column, select);
    }

    public void moveWordRight(boolean select) {
        String current = line(caretLine);
        if (caretColumn >= current.length()) {
            moveRight(select);
            return;
        }
        int column = caretColumn;
        while (column < current.length() && isWord(current.charAt(column))) {
            column++;
        }
        while (column < current.length() && !isWord(current.charAt(column))) {
            column++;
        }
        moveTo(caretLine, column, select);
    }

    public void insert(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        deleteSelection();

        String[] parts = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        String current = line(caretLine);
        String before = current.substring(0, caretColumn);
        String after = current.substring(caretColumn);

        if (parts.length == 1) {
            lines.set(caretLine, before + parts[0] + after);
            caretColumn = before.length() + parts[0].length();
        } else {
            lines.set(caretLine, before + parts[0]);
            for (int i = 1; i < parts.length; i++) {
                lines.add(caretLine + i, parts[i]);
            }
            caretLine += parts.length - 1;
            caretColumn = parts[parts.length - 1].length();
            lines.set(caretLine, lines.get(caretLine) + after);
        }
        clearSelection();
    }

    public void newline(String indentUnit, int extraIndent) {
        String current = line(caretLine);
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < caretColumn && i < current.length() && isSpace(current.charAt(i)); i++) {
            indent.append(current.charAt(i));
        }
        for (int i = 0; i < extraIndent; i++) {
            indent.append(indentUnit == null ? "  " : indentUnit);
        }
        insert("\n" + indent);
    }

    public void backspace() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }
        if (caretColumn > 0) {
            String current = line(caretLine);
            int from = caretColumn - 1;

            if (isIndentOnly(current, caretColumn)) {
                from = Math.max(0, caretColumn - indentStep(caretColumn));
            }
            lines.set(caretLine, current.substring(0, from) + current.substring(caretColumn));
            caretColumn = from;
        } else if (caretLine > 0) {
            String previous = line(caretLine - 1);
            String current = lines.remove(caretLine);
            caretLine--;
            caretColumn = previous.length();
            lines.set(caretLine, previous + current);
        }
        clearSelection();
    }

    public void delete() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }
        String current = line(caretLine);
        if (caretColumn < current.length()) {
            lines.set(caretLine, current.substring(0, caretColumn) + current.substring(caretColumn + 1));
        } else if (caretLine < lines.size() - 1) {
            lines.set(caretLine, current + lines.remove(caretLine + 1));
        }
        clearSelection();
    }

    public void deleteSelection() {
        if (!hasSelection()) {
            return;
        }
        Span span = selection();
        String head = line(span.startLine).substring(0, span.startColumn);
        String tail = line(span.endLine).substring(span.endColumn);
        for (int i = span.endLine; i > span.startLine; i--) {
            lines.remove(i);
        }
        lines.set(span.startLine, head + tail);
        caretLine = span.startLine;
        caretColumn = span.startColumn;
        clearSelection();
    }

    public void indent(String indentUnit, boolean outdent) {
        String unit = indentUnit == null ? "  " : indentUnit;
        if (!hasSelection() && !outdent) {
            insert(unit);
            return;
        }

        Span span = selection();
        int lastLine = span.endLine;

        if (span.endColumn == 0 && span.endLine > span.startLine) {
            lastLine--;
        }

        for (int i = span.startLine; i <= lastLine; i++) {
            String current = line(i);
            if (outdent) {
                int remove = 0;
                while (remove < unit.length() && remove < current.length() && isSpace(current.charAt(remove))) {
                    remove++;
                }
                lines.set(i, current.substring(remove));
            } else if (!current.isEmpty() || i == caretLine) {
                lines.set(i, unit + current);
            }
        }

        anchorLine = span.startLine;
        anchorColumn = 0;
        caretLine = lastLine;
        caretColumn = line(lastLine).length();
    }

    private int indentStep(int column) {
        return column % 2 == 0 ? 2 : 1;
    }

    private boolean isIndentOnly(String line, int upTo) {
        for (int i = 0; i < upTo && i < line.length(); i++) {
            if (!isSpace(line.charAt(i))) {
                return false;
            }
        }
        return upTo > 0;
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t';
    }

    private static boolean isWord(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
