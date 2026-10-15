package com.xyp.gtnotgood.client.radialmenu.core.text;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class IndentGuidesTest {

    private static int[] depths(String... lines) {
        return IndentGuides.depths(Arrays.asList(lines));
    }

    @Test
    public void aLineIsAsDeepAsItsLeadingSpaces() {
        assertArrayEquals(new int[] { 0, 2, 4, 2, 0 }, depths("if a then", "  if b then", "    go()", "  end", "end"));
    }

    @Test
    public void aBlankLineInsideABlockKeepsTheRuleGoing() {
        assertArrayEquals(new int[] { 0, 2, 2, 2, 0 }, depths("if a then", "  one()", "", "  two()", "end"));
    }

    @Test
    public void aBlankLineAtEitherEdgeOfABlockIsStillInsideIt() {
        assertArrayEquals(new int[] { 0, 2, 2, 2, 0 }, depths("if a then", "", "  one()", "", "end"));
    }

    @Test
    public void aBlankLineBetweenTwoBlocksIsAtTheMargin() {
        assertArrayEquals(new int[] { 0, 2, 0, 0, 0, 2, 0 }, depths("do", "  a()", "end", "", "do", "  b()", "end"));
    }

    @Test
    public void severalBlankLinesInARowAreSettledTogether() {
        assertArrayEquals(new int[] { 2, 4, 4, 4, 4 }, depths("  a", "", "", "", "    b"));
    }

    @Test
    public void aLineOfNothingButSpacesIsBlank() {
        assertArrayEquals(new int[] { 0, 2, 2, 0 }, depths("do", "      ", "  a()", "end"));
    }

    @Test
    public void blankLinesAtTheEndsHaveOnlyOneNeighbour() {
        assertArrayEquals(new int[] { 2, 2, 2 }, depths("", "  a()", ""));
        assertArrayEquals(new int[] { 0 }, depths(""));
    }

    @Test
    public void oneRulePerIndentStartingAtTheMargin() {
        assertEquals(0, IndentGuides.count(0, 2));
        assertEquals(1, IndentGuides.count(2, 2));
        assertEquals(2, IndentGuides.count(4, 2));
        // Three spaces in is inside two levels: rules at columns zero and two, both left of the text.
        assertEquals(2, IndentGuides.count(3, 2));
        assertEquals(1, IndentGuides.count(1, 2));
    }
}
