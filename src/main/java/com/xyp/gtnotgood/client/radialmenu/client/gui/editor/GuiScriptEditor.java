package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatAllowedCharacters;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.Ui;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiScreen;
import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;
import com.xyp.gtnotgood.client.radialmenu.core.script.LuaSyntax;
import com.xyp.gtnotgood.client.radialmenu.core.script.ScriptIcons;
import com.xyp.gtnotgood.client.radialmenu.core.script.ScriptSnippets;
import com.xyp.gtnotgood.client.radialmenu.core.text.Folds;
import com.xyp.gtnotgood.client.radialmenu.core.text.IndentGuides;
import com.xyp.gtnotgood.client.radialmenu.core.text.TextBuffer;

public class GuiScriptEditor extends UiScreen implements GuiIconPicker.Callback {

    public interface Result {

        void onScriptEdited(String text);
    }

    /** Line pitch: the font's eight pixels plus enough that descenders do not touch the row below. */
    private static final int LINE_HEIGHT = 11;

    private static final int TEXT_PAD = 3;

    private static final int TEXT_LEFT_PAD = 7;
    private static final int GUTTER_PAD = 4;

    private static final int FOLD_WIDTH = 7;
    private static final int SCROLL_ROWS = 3;

    private static final int ID_SNIPPETS = 50;
    private static final int ID_ICON = 51;
    private static final int SNIPPET_WIDTH = 96;
    private static final int SNIPPET_ROW = 10;
    private static final int SNIPPET_PAD = 3;

    private static final List<ScriptSnippets.Snippet> SNIPPETS = ScriptSnippets.all();

    /** Kept between openings rather than per screen: whoever closed it did not want it back on the next script. */
    private static boolean snippetsOpen = true;

    private int snippetScroll;
    private static final String INDENT = "  ";

    private static final char SECTION_SIGN = (char) 0xA7;
    private static final char CURRENCY_SIGN = (char) 0xA4;

    private final String titleKey;
    private final Result result;
    private final TextBuffer buffer;

    /** The last error this entry's script produced, and the line it names. Null and -1 when there is none. */
    private final String errorText;
    private final int errorLine;

    private List<List<LuaSyntax.Token>> tokens;

    /** Leading spaces per line, blank lines filled in. Worked out with the tokens, since both change on an edit. */
    private int[] indentDepths;

    private final Folds folds = new Folds();

    private int[] visible;

    private int editFrom;
    private int editTo;
    private int linesBefore;

    private int topRow;
    private int leftPixel;
    private int blinkTicks;
    private boolean dragging;

    public GuiScriptEditor(String titleKey, String source, String errorText, Result result) {
        this.titleKey = titleKey;
        this.result = result;
        this.buffer = new TextBuffer(source);
        this.errorText = errorText;
        this.errorLine = lineOf(errorText);
        retokenize();
        rememberSpan();
    }

    private static int lineOf(String error) {
        if (error == null) {
            return -1;
        }
        int first = error.indexOf(':');
        int second = error.indexOf(':', first + 1);
        if (first < 0 || second < 0) {
            return -1;
        }
        try {
            return Integer.parseInt(error.substring(first + 1, second).trim()) - 1;
        } catch (NumberFormatException ignored) {
            // Some other message shape. No line to point at, which is not an error in itself.
            return -1;
        }
    }

    @Override
    protected String titleKey() {
        return titleKey;
    }

    @Override
    protected int panelWidth() {
        // Wider than the other dialogs on purpose: a line of Lua is longer than a label and a control.
        return 460;
    }

    /** The status line under the box, which never scrolls. */
    @Override
    protected int footerHeight() {
        return 10;
    }

    @Override
    protected void buildControls() {
        // Nothing scrolls in the framework's sense - this screen scrolls its own text by whole lines.
        setContentHeight(0);
        addBottomBar("gui.done", null, "gui.cancel");

        // #tr radialmenu.script.snippets.hide
        // # Hide API
        // # zh_CN 隐藏 API
        String toggleKey = "radialmenu.script.snippets.hide";
        if (!snippetsOpen) {
            // #tr radialmenu.script.snippets.show
            // # Show API
            // # zh_CN 显示 API
            toggleKey = "radialmenu.script.snippets.show";
        }
        GuiButton toggle = new GuiButton(ID_SNIPPETS, panelRight - SNIPPET_WIDTH - Ui.PAD, Ui.bottomBarY(this.height),
            SNIPPET_WIDTH, Ui.ROW, I18n.format(toggleKey));
        this.buttonList.add(toggle);
        markFooter(ID_SNIPPETS);
        // #tr radialmenu.script.snippets.tip
        // # Click an entry to write it into the script where the caret is.
        // # zh_CN 点击条目，将其写入光标所在位置。
        tooltip(ID_SNIPPETS, I18n.format("radialmenu.script.snippets.tip"));

        // Opposite the API toggle and the same width, so the bar stays balanced around Done and Cancel.
        this.buttonList
            .add(new GuiButton(ID_ICON, panelLeft + Ui.PAD, Ui.bottomBarY(this.height), SNIPPET_WIDTH, Ui.ROW,
                // #tr radialmenu.script.icon
                // # Insert icon
                // # zh_CN 插入图标
                I18n.format("radialmenu.script.icon")));
        markFooter(ID_ICON);
        // #tr radialmenu.script.icon.tip
        // # Insert an icon name, replacing the quoted string around the caret if any.
        // # zh_CN 插入图标名称；若光标处于引号字符串内，则替换该字符串。
        tooltip(ID_ICON, I18n.format("radialmenu.script.icon.tip"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_PRIMARY) {
            result.onScriptEdited(buffer.text());
            GuiStack.pop();
            return;
        }
        if (button.id == ID_SECONDARY) {
            onCancel();
            return;
        }
        if (button.id == ID_SNIPPETS) {
            snippetsOpen = !snippetsOpen;
            // The code area changes width, so what was in view may not be any more.
            afterMove();
            requestRebuild();
            return;
        }
        if (button.id == ID_ICON) {
            GuiStack.push(new GuiIconPicker(this));
        }
    }

    @Override
    public void onIconPicked(IconSpec icon) {
        int line = buffer.caretLine();
        if (!buffer.hasSelection()) {
            LuaSyntax.Token string = ScriptIcons.stringAt(line < tokens.size() ? tokens.get(line) : null,
                buffer.line(line), buffer.caretColumn());
            if (string != null) {
                buffer.moveTo(line, string.start, false);
                buffer.moveTo(line, string.end, true);
            }
        }
        buffer.insert(ScriptIcons.spell(icon));
        afterEdit();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        blinkTicks++;
    }

    private int boxTop() {
        return viewportTop();
    }

    private int boxBottom() {
        return viewportBottom();
    }

    /** Right edge of the code box: the panel, less the snippet column when it is open. */
    private int boxRight() {
        return snippetsOpen ? viewportRight() - SNIPPET_WIDTH - Ui.GAP : viewportRight();
    }

    private int snippetLeft() {
        return viewportRight() - SNIPPET_WIDTH;
    }

    private int snippetRows() {
        return Math.max(1, (boxBottom() - boxTop() - 2 * SNIPPET_PAD) / SNIPPET_ROW);
    }

    private int gutterWidth() {
        return this.fontRendererObj.getStringWidth(String.valueOf(Math.max(99, buffer.lineCount()))) + GUTTER_PAD
            + 2
            + FOLD_WIDTH;
    }

    private int gutterRight() {
        return viewportLeft() + 1 + gutterWidth();
    }

    private int textLeft() {
        return viewportLeft() + 1 + gutterWidth() + 1 + TEXT_LEFT_PAD;
    }

    private int textRight() {
        return boxRight() - 1 - TEXT_PAD;
    }

    private int textTop() {
        return boxTop() + 1 + TEXT_PAD;
    }

    private int visibleRows() {
        return Math.max(1, (boxBottom() - 1 - TEXT_PAD - textTop()) / LINE_HEIGHT);
    }

    private static String display(String line) {
        return line.indexOf(SECTION_SIGN) < 0 ? line : line.replace(SECTION_SIGN, CURRENCY_SIGN);
    }

    /** Pixels from the start of a line to a column. The only converter, used by the caret and by the text alike. */
    private int widthTo(int line, int column) {
        String text = display(buffer.line(line));
        int end = Math.max(0, Math.min(column, text.length()));
        return this.fontRendererObj.getStringWidth(text.substring(0, end));
    }

    private int columnAt(int line, int x) {
        String text = display(buffer.line(line));
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int charWidth = this.fontRendererObj.getCharWidth(text.charAt(i));
            if (x < width + charWidth / 2) {
                return i;
            }
            width += charWidth;
        }
        return text.length();
    }

    private void retokenize() {
        tokens = LuaSyntax.tokenize(buffer.lines());
        indentDepths = IndentGuides.depths(buffer.lines());
        visible = folds.visibleLines(buffer.lines(), indentDepths);
    }

    private void rememberSpan() {
        if (buffer.hasSelection()) {
            TextBuffer.Span span = buffer.selection();
            editFrom = span.startLine;
            editTo = span.endLine;
        } else {
            editFrom = buffer.caretLine();
            editTo = buffer.caretLine();
        }
        linesBefore = buffer.lineCount();
    }

    private void afterEdit() {
        tokens = LuaSyntax.tokenize(buffer.lines());
        indentDepths = IndentGuides.depths(buffer.lines());
        folds.edited(editFrom, editTo, buffer.lineCount() - linesBefore, buffer.lines(), indentDepths);
        // Typing must never happen where it cannot be seen: an edit that left the caret under a fold opens it.
        folds.reveal(buffer.lines(), indentDepths, buffer.caretLine());
        visible = folds.visibleLines(buffer.lines(), indentDepths);
        afterMove();
    }

    private int rowOf(int line) {
        int found = Arrays.binarySearch(visible, line);
        return found >= 0 ? found : Math.max(0, -found - 2);
    }

    private int lineAtRow(int row) {
        return visible[Math.max(0, Math.min(visible.length - 1, row))];
    }

    /** Folds or opens the block a line starts. Returns false when the line starts none. */
    private boolean toggleFold(int line) {
        if (!folds.toggle(buffer.lines(), indentDepths, line)) {
            return false;
        }
        visible = folds.visibleLines(buffer.lines(), indentDepths);
        if (folds.hiddenUnder(buffer.lines(), indentDepths, buffer.caretLine()) >= 0) {
            buffer.moveTo(line, buffer.line(line).length(), false);
        }
        afterMove();
        return true;
    }

    private void leaveFold(boolean forward, boolean sideways, boolean select) {
        int header = folds.hiddenUnder(buffer.lines(), indentDepths, buffer.caretLine());
        if (header < 0) {
            return;
        }
        int after = Folds.endOf(buffer.lines(), indentDepths, header) + 1;
        if (forward && after < buffer.lineCount()) {
            buffer.moveTo(after, sideways ? 0 : buffer.caretColumn(), select);
            return;
        }
        // Backwards, or forwards with nothing after the block: the opening line, at its end unless a column was
        // being kept.
        boolean atEnd = sideways || forward;
        buffer.moveTo(header, atEnd ? buffer.line(header).length() : buffer.caretColumn(), select);
    }

    private void afterMove() {
        blinkTicks = 0;
        int rows = visibleRows();
        int caretRow = rowOf(buffer.caretLine());
        if (caretRow < topRow) {
            topRow = caretRow;
        } else if (caretRow >= topRow + rows) {
            topRow = caretRow - rows + 1;
        }
        clampScroll();

        int caretX = widthTo(buffer.caretLine(), buffer.caretColumn());
        int width = textRight() - textLeft();
        if (caretX - leftPixel < 0) {
            leftPixel = caretX;
        } else if (caretX - leftPixel > width - 2) {
            leftPixel = caretX - width + 2;
        }
        leftPixel = Math.max(0, leftPixel);
        rememberSpan();
    }

    private void clampScroll() {
        topRow = Math.max(0, Math.min(topRow, Math.max(0, visible.length - visibleRows())));
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        int left = viewportLeft();
        int right = boxRight();
        int top = boxTop();
        int bottom = boxBottom();
        if (snippetsOpen) {
            drawSnippets(mouseX, mouseY);
        }

        Ui.list(left, top, right, bottom);
        clampScroll();

        int gutterRight = gutterRight();
        Gui.drawRect(left + 1, top + 1, gutterRight, bottom - 1, 0x30000000);
        Gui.drawRect(gutterRight, top + 1, gutterRight + 1, bottom - 1, 0x30FFFFFF);

        int rows = visibleRows();
        int textTop = textTop();

        for (int row = 0; row < rows; row++) {
            if (topRow + row >= visible.length) {
                break;
            }
            int line = visible[topRow + row];
            int y = textTop + row * LINE_HEIGHT;
            if (line == errorLine) {
                Gui.drawRect(left + 1, y - 1, right - 1, y + LINE_HEIGHT - 1, Ui.CODE_ERROR_LINE);
            } else if (line == buffer.caretLine() && !buffer.hasSelection()) {
                Gui.drawRect(left + 1, y - 1, right - 1, y + LINE_HEIGHT - 1, Ui.CODE_CURRENT_LINE);
            }

            String number = String.valueOf(line + 1);
            this.fontRendererObj.drawString(number,
                gutterRight - FOLD_WIDTH - 2 - this.fontRendererObj.getStringWidth(number), y,
                line == errorLine ? Ui.TEXT_ERROR : line == buffer.caretLine() ? Ui.TEXT_MUTED : Ui.CODE_GUTTER);
            drawFoldMark(line, gutterRight, y);
        }

        // The text is the only thing that moves sideways, so it is the only thing clipped - the gutter has to stay
        // readable and the frame has to keep its edges.
        Ui.beginClip(gutterRight + 1, top + 1, right - 1, bottom - 1);
        for (int row = 0; row < rows; row++) {
            if (topRow + row >= visible.length) {
                break;
            }
            int line = visible[topRow + row];
            drawIndentGuides(line, textTop + row * LINE_HEIGHT);
            drawSelection(line, textTop + row * LINE_HEIGHT);
            drawLine(line, textTop + row * LINE_HEIGHT);
            if (folds.isFolded(line)) {
                drawFoldedTail(line, textTop + row * LINE_HEIGHT);
            }
        }
        drawCaret(textTop, rows);
        Ui.endClip();

        drawStatus(bottom + Ui.GAP);
    }

    private void drawFoldMark(int line, int gutterRight, int y) {
        boolean folded = folds.isFolded(line);
        if (!folded && Folds.endOf(buffer.lines(), indentDepths, line) < 0) {
            return;
        }
        int colour = folded ? Ui.TEXT : Ui.CODE_GUTTER;
        int centreX = gutterRight - 1 - FOLD_WIDTH / 2;
        int centreY = y + 3;
        Gui.drawRect(centreX - 2, centreY, centreX + 3, centreY + 1, colour);
        if (folded) {
            Gui.drawRect(centreX, centreY - 2, centreX + 1, centreY + 3, colour);
        }
    }

    private void drawFoldedTail(int line, int y) {
        int x = textLeft() + widthTo(line, buffer.line(line).length()) - leftPixel + 4;
        int width = this.fontRendererObj.getStringWidth("...");
        Gui.drawRect(x - 2, y - 1, x + width + 2, y + LINE_HEIGHT - 2, Ui.CODE_FOLDED);
        this.fontRendererObj.drawString("...", x, y, Ui.CODE_COMMENT);
    }

    private void drawIndentGuides(int line, int y) {
        if (line >= indentDepths.length) {
            return;
        }
        int step = INDENT.length() * this.fontRendererObj.getCharWidth(' ');
        int guides = IndentGuides.count(indentDepths[line], INDENT.length());
        for (int level = 0; level < guides; level++) {
            int x = textLeft() + level * step - leftPixel;
            for (int dot = y - 1; dot < y + LINE_HEIGHT - 1; dot++) {
                if ((dot & 1) == 0) {
                    Gui.drawRect(x, dot, x + 1, dot + 1, Ui.CODE_INDENT_GUIDE);
                }
            }
        }
    }

    private void drawLine(int line, int y) {
        String text = display(buffer.line(line));
        List<LuaSyntax.Token> lineTokens = line < tokens.size() ? tokens.get(line) : null;
        int x = textLeft() - leftPixel;

        if (lineTokens == null || lineTokens.isEmpty()) {
            if (!text.isEmpty()) {
                this.fontRendererObj.drawString(text, x, y, Ui.CODE_PLAIN);
            }
            return;
        }

        for (LuaSyntax.Token token : lineTokens) {
            int end = Math.min(token.end, text.length());
            if (token.start >= end) {
                continue;
            }
            String part = text.substring(token.start, end);
            // No shadow: a shadow under nine-pixel text smears the difference between a dot and a comma, and the
            // widths this walks are the unshadowed ones.
            this.fontRendererObj.drawString(part, x, y, colourOf(token.kind));
            x += this.fontRendererObj.getStringWidth(part);
        }
    }

    private static int colourOf(LuaSyntax.Kind kind) {
        switch (kind) {
            case Keyword:
                return Ui.CODE_KEYWORD;
            case Declaration:
                return Ui.CODE_DECLARATION;
            case Constant:
                return Ui.CODE_CONSTANT;
            case Self:
                return Ui.CODE_SELF;
            case Api:
                return Ui.CODE_API;
            case Function:
                return Ui.CODE_FUNCTION;
            case Field:
                return Ui.CODE_FIELD;
            case String:
                return Ui.CODE_STRING;
            case Number:
                return Ui.CODE_NUMBER;
            case Comment:
                return Ui.CODE_COMMENT;
            case Operator:
                return Ui.CODE_OPERATOR;
            default:
                return Ui.CODE_PLAIN;
        }
    }

    private void drawSelection(int line, int y) {
        if (!buffer.hasSelection()) {
            return;
        }
        TextBuffer.Span span = buffer.selection();
        if (line < span.startLine || line > span.endLine) {
            return;
        }

        int from = line == span.startLine ? span.startColumn : 0;
        int to = line == span.endLine ? span.endColumn : buffer.line(line).length();
        int x1 = textLeft() + widthTo(line, from) - leftPixel;
        int x2 = textLeft() + widthTo(line, to) - leftPixel;
        // A line selected to its end includes the break after it, and showing that as a couple of pixels past the
        // last character is how the selection reads as covering whole lines.
        if (line < span.endLine) {
            x2 += 3;
        }
        Gui.drawRect(x1, y - 1, Math.max(x1 + 1, x2), y + LINE_HEIGHT - 1, Ui.CODE_SELECTION);
    }

    private void drawCaret(int textTop, int rows) {
        if (blinkTicks / 6 % 2 != 0) {
            return;
        }
        int row = rowOf(buffer.caretLine()) - topRow;
        if (row < 0 || row >= rows) {
            return;
        }
        int x = textLeft() + widthTo(buffer.caretLine(), buffer.caretColumn()) - leftPixel;
        int y = textTop + row * LINE_HEIGHT;
        Gui.drawRect(x, y - 1, x + 1, y + LINE_HEIGHT - 2, Ui.TEXT);
    }

    private void drawStatus(int y) {
        if (errorText != null) {
            this.fontRendererObj.drawString(Ui.fit(errorText, viewportRight() - viewportLeft()), viewportLeft(), y,
                Ui.TEXT_ERROR);
            return;
        }
        String position = I18n
            // #tr radialmenu.script.position
            // # Ln %d, Col %d - %d lines
            // # zh_CN 第 %d 行，第 %d 列 - 共 %d 行
            .format("radialmenu.script.position", buffer.caretLine() + 1, buffer.caretColumn() + 1, buffer.lineCount());
        this.fontRendererObj.drawString(position, viewportLeft(), y, Ui.TEXT_MUTED);
    }

    private void drawSnippets(int mouseX, int mouseY) {
        int left = snippetLeft();
        int right = viewportRight();
        int top = boxTop();
        int bottom = boxBottom();

        Ui.list(left, top, right, bottom);
        clampSnippetScroll();

        int hovered = snippetAt(mouseX, mouseY);
        int rows = snippetRows();
        for (int row = 0; row < rows; row++) {
            int index = snippetScroll + row;
            if (index >= SNIPPETS.size()) {
                break;
            }
            int y = top + SNIPPET_PAD + row * SNIPPET_ROW;
            if (index == hovered) {
                Gui.drawRect(left + 1, y - 1, right - 1, y + SNIPPET_ROW - 1, Ui.ROW_HOVER);
            }
            this.fontRendererObj.drawString(Ui.fit(SNIPPETS.get(index).label, right - left - 2 * SNIPPET_PAD - 2),
                left + SNIPPET_PAD, y, index == hovered ? Ui.TEXT : Ui.CODE_API);
        }

        if (SNIPPETS.size() > rows) {
            // A list that scrolls says so: the column is too narrow for a scrollbar, so the marker is a thin track.
            int trackTop = top + SNIPPET_PAD;
            int trackHeight = rows * SNIPPET_ROW;
            int thumb = Math.max(8, trackHeight * rows / SNIPPETS.size());
            int offset = (trackHeight - thumb) * snippetScroll / Math.max(1, SNIPPETS.size() - rows);
            Gui.drawRect(right - 3, trackTop, right - 2, trackTop + trackHeight, 0x30FFFFFF);
            Gui.drawRect(right - 3, trackTop + offset, right - 2, trackTop + offset + thumb, 0x90FFFFFF);
        }
    }

    /** The whole call under the cursor, since the row only has space for its name. */
    @Override
    protected void drawOverlay(int mouseX, int mouseY, float partialTicks) {
        int index = snippetAt(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (String line : SNIPPETS.get(index).text.replace(String.valueOf(ScriptSnippets.CARET), "").split("\n")) {
            lines.add(line);
        }
        drawHoveringText(lines, mouseX, mouseY, this.fontRendererObj);
    }

    private void clampSnippetScroll() {
        snippetScroll = Math.max(0, Math.min(snippetScroll, Math.max(0, SNIPPETS.size() - snippetRows())));
    }

    /** Which row the cursor is on, or -1 if it is not over the list at all. */
    private int snippetAt(int mouseX, int mouseY) {
        if (!snippetsOpen || mouseX < snippetLeft() || mouseX >= viewportRight()) {
            return -1;
        }
        int row = (mouseY - boxTop() - SNIPPET_PAD) / SNIPPET_ROW;
        if (row < 0 || row >= snippetRows()) {
            return -1;
        }
        int index = snippetScroll + row;
        return index < SNIPPETS.size() ? index : -1;
    }

    private void insertSnippet(ScriptSnippets.Snippet snippet) {
        String text = snippet.text;
        int caret = text.indexOf(ScriptSnippets.CARET);
        if (caret >= 0) {
            text = text.replace(String.valueOf(ScriptSnippets.CARET), "");
        }

        buffer.insert(text);
        if (caret >= 0) {
            for (int i = text.length(); i > caret; i--) {
                buffer.moveLeft(false);
            }
        }
        afterEdit();
    }

    private boolean insideText(int mouseX, int mouseY) {
        return mouseX >= viewportLeft() && mouseX < boxRight() && mouseY >= boxTop() && mouseY < boxBottom();
    }

    private void placeCaret(int mouseX, int mouseY, boolean select) {
        int line = lineAtRow(topRow + Math.max(0, (mouseY - textTop() + 1) / LINE_HEIGHT));
        buffer.moveTo(line, columnAt(line, mouseX - textLeft() + leftPixel), select);
        afterMove();
    }

    /** The line whose gutter the cursor is on, or -1 when it is not on the gutter or below the last line. */
    private int gutterLineAt(int mouseX, int mouseY) {
        if (mouseX < viewportLeft() || mouseX > gutterRight() || mouseY < boxTop() || mouseY >= boxBottom()) {
            return -1;
        }
        int row = topRow + (mouseY - textTop() + 1) / LINE_HEIGHT;
        return row >= 0 && row < visible.length ? visible[row] : -1;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            int snippet = snippetAt(mouseX, mouseY);
            if (snippet >= 0) {
                insertSnippet(SNIPPETS.get(snippet));
                return;
            }
        }
        // The whole gutter and not just the mark: the mark is five pixels across, and a line number is where the eye
        // already is. A line that opens nothing falls through and takes the caret, as the gutter always did.
        if (mouseButton == 0) {
            int gutterLine = gutterLineAt(mouseX, mouseY);
            if (gutterLine >= 0 && toggleFold(gutterLine)) {
                return;
            }
        }
        if (mouseButton == 0 && insideText(mouseX, mouseY)) {
            dragging = true;
            placeCaret(mouseX, mouseY, isShiftKeyDown());
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceClick) {
        if (dragging && mouseButton == 0) {
            // Always extending, never starting a new selection: the press set the anchor and the drag is the rest.
            placeCaret(mouseX, mouseY, true);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        if (which == 0) {
            dragging = false;
        }
        super.mouseMovedOrUp(mouseX, mouseY, which);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }

        // Whichever list the cursor is over is the one that moves. Scrolling the code while reading the snippets is
        // the sort of thing that is only noticed as "the wheel does not work here".
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        if (snippetsOpen && mouseX >= snippetLeft()) {
            snippetScroll += wheel > 0 ? -SCROLL_ROWS : SCROLL_ROWS;
            clampSnippetScroll();
            return;
        }

        topRow += wheel > 0 ? -SCROLL_ROWS : SCROLL_ROWS;
        clampScroll();
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        boolean shift = isShiftKeyDown();
        boolean control = isCtrlKeyDown();

        if (control && handleShortcut(keyCode)) {
            return true;
        }

        switch (keyCode) {
            case Keyboard.KEY_LEFT:
                if (control) {
                    buffer.moveWordLeft(shift);
                } else {
                    buffer.moveLeft(shift);
                }
                leaveFold(false, true, shift);
                afterMove();
                return true;
            case Keyboard.KEY_RIGHT:
                if (control) {
                    buffer.moveWordRight(shift);
                } else {
                    buffer.moveRight(shift);
                }
                leaveFold(true, true, shift);
                afterMove();
                return true;
            case Keyboard.KEY_UP:
                buffer.moveUp(1, shift);
                leaveFold(false, false, shift);
                afterMove();
                return true;
            case Keyboard.KEY_DOWN:
                buffer.moveDown(1, shift);
                leaveFold(true, false, shift);
                afterMove();
                return true;
            case Keyboard.KEY_PRIOR:
                buffer.moveUp(visibleRows(), shift);
                leaveFold(false, false, shift);
                afterMove();
                return true;
            case Keyboard.KEY_NEXT:
                buffer.moveDown(visibleRows(), shift);
                leaveFold(true, false, shift);
                afterMove();
                return true;
            case Keyboard.KEY_HOME:
                if (control) {
                    buffer.moveToStart(shift);
                } else {
                    buffer.moveToLineStart(shift);
                }
                afterMove();
                return true;
            case Keyboard.KEY_END:
                if (control) {
                    buffer.moveToEnd(shift);
                    leaveFold(true, true, shift);
                } else {
                    buffer.moveToLineEnd(shift);
                }
                afterMove();
                return true;
            case Keyboard.KEY_BACK:
                buffer.backspace();
                afterEdit();
                return true;
            case Keyboard.KEY_DELETE:
                buffer.delete();
                afterEdit();
                return true;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                buffer.newline(INDENT, opensBlock(buffer.line(buffer.caretLine())) ? 1 : 0);
                afterEdit();
                return true;
            case Keyboard.KEY_TAB:
                buffer.indent(INDENT, shift);
                afterEdit();
                return true;
            default:
                break;
        }

        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            buffer.insert(String.valueOf(typedChar));
            afterEdit();
            return true;
        }
        return true;
    }

    private boolean handleShortcut(int keyCode) {
        switch (keyCode) {
            case Keyboard.KEY_A:
                buffer.selectAll();
                afterMove();
                return true;
            case Keyboard.KEY_C:
                if (buffer.hasSelection()) {
                    setClipboardString(buffer.selectedText());
                }
                return true;
            case Keyboard.KEY_X:
                if (buffer.hasSelection()) {
                    setClipboardString(buffer.selectedText());
                    buffer.deleteSelection();
                    afterEdit();
                }
                return true;
            case Keyboard.KEY_V:
                String pasted = filter(getClipboardString());
                if (!pasted.isEmpty()) {
                    buffer.insert(pasted);
                    afterEdit();
                }
                return true;
            default:
                return false;
        }
    }

    private static String filter(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder kept = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                kept.append('\n');
            } else if (c == '\t') {
                kept.append(INDENT);
            } else if (ChatAllowedCharacters.isAllowedCharacter(c)) {
                kept.append(c);
            }
        }
        return kept.toString();
    }

    private static boolean opensBlock(String line) {
        String trimmed = line.trim();
        if (
            trimmed.endsWith("then") || trimmed.endsWith("do")
                || trimmed.endsWith("else")
                || trimmed.endsWith("{")
                || trimmed.endsWith("(")
                || trimmed.endsWith("repeat")
        ) {
            return true;
        }
        return trimmed.startsWith("function") || trimmed.contains("function(") || trimmed.endsWith("function()");
    }
}
