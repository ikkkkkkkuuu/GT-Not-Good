package com.xyp.gtnotgood.client.radialmenu.core.text;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FoldsTest {

    /** Line numbers down the side, because every assertion below is one. */
    private static final List<String> SCRIPT = Arrays.asList("local homes = {}", // 0
        "", // 1
        "for name in list do", // 2
        "  homes[#homes + 1] = {", // 3
        "    key = name,", // 4
        "", // 5
        "    label = name", // 6
        "  }", // 7
        "end", // 8
        "", // 9
        "if pick then", // 10
        "  go()", // 11
        "end"); // 12

    private static int[] depths(List<String> lines) {
        return IndentGuides.depths(lines);
    }

    private static int endOf(int line) {
        return Folds.endOf(SCRIPT, depths(SCRIPT), line);
    }

    @Test
    public void aBlockRunsToTheLastDeeperLine() {
        assertEquals(7, endOf(2));
        assertEquals(6, endOf(3));
        assertEquals(11, endOf(10));
    }

    @Test
    public void theClosingLineStaysOutOfIt() {
        // "end" and "}" sit level with the line that opened the block, so they are what is left showing under it.
        assertEquals(-1, endOf(8));
        assertEquals(-1, endOf(7));
    }

    @Test
    public void aLineWithNothingDeeperUnderItDoesNotFold() {
        assertEquals(-1, endOf(0));
        assertEquals(-1, endOf(4));
        assertEquals(-1, endOf(12));
    }

    @Test
    public void aBlankLineIsNeverAnOpener() {
        // Line 5 borrows a depth of four from its neighbours, and a blank line at the top of a block borrows the
        // block's - neither makes it something to click on.
        assertEquals(-1, endOf(5));
        assertEquals(-1, Folds.endOf(Arrays.asList("", "  a()"), new int[] { 2, 2 }, 0));
    }

    @Test
    public void foldingHidesTheBlockAndKeepsItsOpener() {
        Folds folds = new Folds();
        assertTrue(folds.toggle(SCRIPT, depths(SCRIPT), 2));
        assertArrayEquals(new int[] { 0, 1, 2, 8, 9, 10, 11, 12 }, folds.visibleLines(SCRIPT, depths(SCRIPT)));

        assertTrue(folds.toggle(SCRIPT, depths(SCRIPT), 2));
        assertEquals(SCRIPT.size(), folds.visibleLines(SCRIPT, depths(SCRIPT)).length);
    }

    @Test
    public void aLineThatOpensNothingRefusesToFold() {
        Folds folds = new Folds();
        assertFalse(folds.toggle(SCRIPT, depths(SCRIPT), 0));
        assertTrue(folds.isEmpty());
    }

    @Test
    public void aFoldInsideAFoldIsRememberedButTheOuterOneDecides() {
        Folds folds = new Folds();
        folds.toggle(SCRIPT, depths(SCRIPT), 3);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 7, 8, 9, 10, 11, 12 }, folds.visibleLines(SCRIPT, depths(SCRIPT)));

        folds.toggle(SCRIPT, depths(SCRIPT), 2);
        assertArrayEquals(new int[] { 0, 1, 2, 8, 9, 10, 11, 12 }, folds.visibleLines(SCRIPT, depths(SCRIPT)));
        assertEquals(2, folds.hiddenUnder(SCRIPT, depths(SCRIPT), 4));
        assertEquals(2, folds.hiddenUnder(SCRIPT, depths(SCRIPT), 3));

        // Opening the outer one brings the inner back still folded.
        folds.toggle(SCRIPT, depths(SCRIPT), 2);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 7, 8, 9, 10, 11, 12 }, folds.visibleLines(SCRIPT, depths(SCRIPT)));
    }

    @Test
    public void aLineOnShowIsHiddenUnderNothing() {
        Folds folds = new Folds();
        folds.toggle(SCRIPT, depths(SCRIPT), 2);
        assertEquals(-1, folds.hiddenUnder(SCRIPT, depths(SCRIPT), 2));
        assertEquals(-1, folds.hiddenUnder(SCRIPT, depths(SCRIPT), 8));
        assertEquals(2, folds.hiddenUnder(SCRIPT, depths(SCRIPT), 7));
    }

    @Test
    public void revealOpensEveryFoldOverALine() {
        Folds folds = new Folds();
        folds.toggle(SCRIPT, depths(SCRIPT), 3);
        folds.toggle(SCRIPT, depths(SCRIPT), 2);
        folds.toggle(SCRIPT, depths(SCRIPT), 10);

        assertTrue(folds.reveal(SCRIPT, depths(SCRIPT), 5));
        assertFalse(folds.isFolded(2));
        assertFalse(folds.isFolded(3));
        // A fold somewhere else is none of its business.
        assertTrue(folds.isFolded(10));
        assertFalse(folds.reveal(SCRIPT, depths(SCRIPT), 5));
    }

    @Test
    public void aFoldBelowAnEditMovesWithItsBlock() {
        Folds folds = new Folds();
        folds.toggle(SCRIPT, depths(SCRIPT), 10);

        List<String> longer = new ArrayList<>(SCRIPT);
        longer.add(1, "local extra = 1");
        folds.edited(0, 0, 1, longer, depths(longer));

        assertFalse(folds.isFolded(10));
        assertTrue(folds.isFolded(11));
    }

    @Test
    public void aFoldAboveAnEditStaysWhereItIs() {
        Folds folds = new Folds();
        folds.toggle(SCRIPT, depths(SCRIPT), 2);

        List<String> longer = new ArrayList<>(SCRIPT);
        longer.add(11, "  wait()");
        folds.edited(10, 10, 1, longer, depths(longer));

        assertTrue(folds.isFolded(2));
    }

    @Test
    public void aFoldWhoseOpenerWasDeletedIsForgotten() {
        Folds folds = new Folds();
        folds.toggle(SCRIPT, depths(SCRIPT), 3);
        folds.toggle(SCRIPT, depths(SCRIPT), 10);

        // Lines 2 to 8 selected and deleted, which leaves one empty line where they were.
        List<String> shorter = new ArrayList<>(SCRIPT);
        for (int i = 0; i < 6; i++) {
            shorter.remove(2);
        }
        shorter.set(2, "");
        folds.edited(2, 8, -6, shorter, depths(shorter));

        assertFalse(folds.isFolded(3));
        assertTrue(folds.isFolded(4));
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 6 }, folds.visibleLines(shorter, depths(shorter)));
    }

    @Test
    public void aFoldWhoseBlockWasOutdentedAwayIsForgotten() {
        Folds folds = new Folds();
        folds.toggle(SCRIPT, depths(SCRIPT), 10);

        List<String> flat = new ArrayList<>(SCRIPT);
        flat.set(11, "go()");
        folds.edited(11, 11, 0, flat, depths(flat));

        assertTrue(folds.isEmpty());
    }

    @Test
    public void typingInsideALineLeavesFoldsAlone() {
        Folds folds = new Folds();
        folds.toggle(SCRIPT, depths(SCRIPT), 10);

        List<String> typed = new ArrayList<>(SCRIPT);
        typed.set(10, "if pick and ready then");
        folds.edited(10, 10, 0, typed, depths(typed));

        assertTrue(folds.isFolded(10));
    }
}
