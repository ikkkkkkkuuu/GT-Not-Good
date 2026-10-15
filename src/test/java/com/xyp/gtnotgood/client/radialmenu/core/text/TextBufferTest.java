package com.xyp.gtnotgood.client.radialmenu.core.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The editing rules, where they can be asked rather than squinted at. */
public class TextBufferTest {

    private static TextBuffer buffer(String text) {
        return new TextBuffer(text);
    }

    @Test
    public void splitsAndRejoinsText() {
        TextBuffer buffer = buffer("one\ntwo\nthree");
        assertEquals(3, buffer.lineCount());
        assertEquals("two", buffer.line(1));
        assertEquals("one\ntwo\nthree", buffer.text());
    }

    @Test
    public void emptyTextIsOneEmptyLineRatherThanNone() {
        // A buffer with no lines has nowhere to put the caret, and every caller would need a special case for it.
        assertEquals(1, buffer("").lineCount());
        assertEquals(1, new TextBuffer(null).lineCount());
    }

    @Test
    public void insertsAtTheCaret() {
        TextBuffer buffer = buffer("ab");
        buffer.moveTo(0, 1, false);
        buffer.insert("X");
        assertEquals("aXb", buffer.text());
        assertEquals(2, buffer.caretColumn());
    }

    @Test
    public void pastedTextSplitsIntoLines() {
        TextBuffer buffer = buffer("start|end");
        buffer.moveTo(0, 6, false);
        buffer.insert("one\ntwo\r\nthree");
        assertEquals("start|one\ntwo\nthree" + "end", buffer.text());
        assertEquals(2, buffer.caretLine());
        assertEquals(5, buffer.caretColumn());
    }

    @Test
    public void positionsOutOfRangeAreClampedRatherThanThrown() {
        TextBuffer buffer = buffer("one\ntwo");
        buffer.moveTo(99, 99, false);
        assertEquals(1, buffer.caretLine());
        assertEquals(3, buffer.caretColumn());
        buffer.moveTo(-5, -5, false);
        assertEquals(0, buffer.caretLine());
        assertEquals(0, buffer.caretColumn());
    }

    @Test
    public void leftAtTheStartOfALineWrapsToTheEndOfThePrevious() {
        TextBuffer buffer = buffer("one\ntwo");
        buffer.moveTo(1, 0, false);
        buffer.moveLeft(false);
        assertEquals(0, buffer.caretLine());
        assertEquals(3, buffer.caretColumn());
    }

    @Test
    public void anUnshiftedArrowCollapsesTheSelectionToItsEdge() {
        TextBuffer buffer = buffer("hello world");
        buffer.moveTo(0, 6, false);
        buffer.moveTo(0, 11, true);
        buffer.moveLeft(false);
        assertEquals(6, buffer.caretColumn());
        assertFalse(buffer.hasSelection());
    }

    @Test
    public void homeGoesToTheTextThenToTheMargin() {
        TextBuffer buffer = buffer("    indented");
        buffer.moveToLineEnd(false);
        buffer.moveToLineStart(false);
        assertEquals(4, buffer.caretColumn());
        buffer.moveToLineStart(false);
        assertEquals(0, buffer.caretColumn());
    }

    @Test
    public void wordJumpsStopAtWordStarts() {
        TextBuffer buffer = buffer("chat.send('/home')");
        buffer.moveToLineEnd(false);
        buffer.moveWordLeft(false);
        assertEquals("home", buffer.line(0).substring(buffer.caretColumn(), buffer.caretColumn() + 4));
        buffer.moveWordLeft(false);
        assertEquals(5, buffer.caretColumn());
    }

    @Test
    public void selectionReadsBackWhicheverWayItWasDragged() {
        TextBuffer buffer = buffer("one\ntwo\nthree");
        buffer.moveTo(2, 2, false);
        buffer.moveTo(0, 1, true);
        assertTrue(buffer.hasSelection());
        assertEquals("ne\ntwo\nth", buffer.selectedText());
    }

    @Test
    public void deletingASelectionJoinsWhatIsLeft() {
        TextBuffer buffer = buffer("one\ntwo\nthree");
        buffer.moveTo(0, 1, false);
        buffer.moveTo(2, 2, true);
        buffer.deleteSelection();
        assertEquals("oree", buffer.text());
        assertEquals(0, buffer.caretLine());
        assertEquals(1, buffer.caretColumn());
    }

    @Test
    public void insertingOverASelectionReplacesIt() {
        TextBuffer buffer = buffer("one\ntwo");
        buffer.selectAll();
        buffer.insert("new");
        assertEquals("new", buffer.text());
    }

    @Test
    public void backspaceAtTheStartOfALineJoinsItToThePrevious() {
        TextBuffer buffer = buffer("one\ntwo");
        buffer.moveTo(1, 0, false);
        buffer.backspace();
        assertEquals("onetwo", buffer.text());
        assertEquals(3, buffer.caretColumn());
    }

    @Test
    public void backspaceInIndentationTakesAWholeLevel() {
        TextBuffer buffer = buffer("    code");
        buffer.moveTo(0, 4, false);
        buffer.backspace();
        assertEquals("  code", buffer.text());
        assertEquals(2, buffer.caretColumn());
    }

    @Test
    public void backspaceInsideTextTakesOneCharacter() {
        TextBuffer buffer = buffer("  code");
        buffer.moveToLineEnd(false);
        buffer.backspace();
        assertEquals("  cod", buffer.text());
    }

    @Test
    public void deleteAtTheEndOfALinePullsTheNextOneUp() {
        TextBuffer buffer = buffer("one\ntwo");
        buffer.moveToLineEnd(false);
        buffer.delete();
        assertEquals("onetwo", buffer.text());
    }

    @Test
    public void newlineCarriesTheIndentationOver() {
        TextBuffer buffer = buffer("  local x = 1");
        buffer.moveToLineEnd(false);
        buffer.newline("  ", 0);
        assertEquals("  local x = 1\n  ", buffer.text());
        assertEquals(2, buffer.caretColumn());
    }

    @Test
    public void newlineCanOpenABlockOneLevelDeeper() {
        TextBuffer buffer = buffer("  if x then");
        buffer.moveToLineEnd(false);
        buffer.newline("  ", 1);
        assertEquals("  if x then\n    ", buffer.text());
    }

    @Test
    public void newlineInTheMiddleDoesNotCarryIndentationItIsBefore() {
        // The caret is inside the indentation, so there is none behind it to copy.
        TextBuffer buffer = buffer("    code");
        buffer.moveTo(0, 2, false);
        buffer.newline("  ", 0);
        assertEquals("  \n    code", buffer.text());
    }

    @Test
    public void tabWithNoSelectionIsJustAnInsert() {
        TextBuffer buffer = buffer("code");
        buffer.moveTo(0, 0, false);
        buffer.indent("  ", false);
        assertEquals("  code", buffer.text());
    }

    @Test
    public void tabIndentsEveryLineOfASelection() {
        TextBuffer buffer = buffer("one\ntwo\nthree");
        buffer.moveTo(0, 1, false);
        buffer.moveTo(1, 1, true);
        buffer.indent("  ", false);
        assertEquals("  one\n  two\nthree", buffer.text());
        // And keeps them selected, or indenting twice would need the player to re-select.
        assertTrue(buffer.hasSelection());
    }

    @Test
    public void shiftTabTakesAnIndentBackOff() {
        TextBuffer buffer = buffer("    one\n  two");
        buffer.moveTo(0, 0, false);
        buffer.moveTo(1, 1, true);
        buffer.indent("  ", true);
        assertEquals("  one\ntwo", buffer.text());
    }

    @Test
    public void aSelectionEndingAtAColumnZeroDoesNotReachThatLine() {
        // The third line is not highlighted on screen, so indenting it would move a line the player cannot see chosen.
        TextBuffer buffer = buffer("one\ntwo\nthree");
        buffer.moveTo(0, 0, false);
        buffer.moveTo(2, 0, true);
        buffer.indent("  ", false);
        assertEquals("  one\n  two\nthree", buffer.text());
    }

    @Test
    public void selectAllCoversEveryLine() {
        TextBuffer buffer = buffer("one\ntwo\nthree");
        buffer.selectAll();
        assertEquals("one\ntwo\nthree", buffer.selectedText());
    }
}
