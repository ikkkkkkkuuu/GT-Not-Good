package com.xyp.gtnotgood.common.blocks.webscreen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class WebScreenShapeTest {

    @Test
    public void discoversRectanglesFromEveryHorizontalFacing() {
        for (int facing = 2; facing <= 5; facing++) {
            boolean horizontalX = facing <= 3;
            WebScreenShape shape = WebScreenShape.discover(12, 72, -5, facing, (x, y, z) -> {
                int horizontal = horizontalX ? x : z;
                return (horizontalX ? z == -5 : x == 12) && horizontal >= (horizontalX ? 10 : -7)
                    && horizontal <= (horizontalX ? 13 : -4)
                    && y >= 70
                    && y <= 72;
            });
            assertEquals(horizontalX ? 10 : 12, shape.anchorX);
            assertEquals(70, shape.anchorY);
            assertEquals(horizontalX ? -5 : -7, shape.anchorZ);
            assertEquals(4, shape.width);
            assertEquals(3, shape.height);
            assertEquals(facing, shape.facing);
            assertTrue(shape.contains(12, 72, -5));
            assertFalse(shape.contains(12, 73, -5));
            assertFalse(shape.contains(horizontalX ? 14 : 12, 70, horizontalX ? -5 : -3));
            assertFalse(shape.contains(horizontalX ? 12 : 11, 70, horizontalX ? -4 : -5));
        }
    }

    @Test
    public void permitsTheMaximumRectangleWithoutTraversingOtherPlanes() {
        AtomicInteger matches = new AtomicInteger();
        WebScreenShape shape = WebScreenShape.discover(-1, 8, 20, 2, (x, y, z) -> {
            assertEquals(20, z);
            if (x < -16 || x > -1 || y < 0 || y > 8) return false;
            matches.incrementAndGet();
            return true;
        });
        assertEquals(16, shape.width);
        assertEquals(9, shape.height);
        assertEquals(144, matches.get());
        assertTrue(shape.contains(-16, 0, 20));
    }

    @Test
    public void fallsBackForAnLShapeAndAHole() {
        WebScreenShape lShape = WebScreenShape.discover(0, 0, 0, 3,
            (x, y, z) -> z == 0 && x >= 0 && x <= 1 && y >= 0 && y <= 1 && (x == 0 || y == 0));
        assertSingle(lShape, 0, 0, 0);
        WebScreenShape hole = WebScreenShape.discover(2, 2, 0, 3,
            (x, y, z) -> z == 0 && x >= 0 && x <= 2 && y >= 0 && y <= 2 && !(x == 1 && y == 1));
        assertSingle(hole, 2, 2, 0);
    }

    @Test
    public void doesNotMergeDisconnectedComponents() {
        WebScreenShape shape = WebScreenShape.discover(1, 1, 0, 3,
            (x, y, z) -> z == 0 && y >= 0 && y <= 1 && (x >= 0 && x <= 1 || x >= 3 && x <= 4));
        assertEquals(0, shape.anchorX);
        assertEquals(2, shape.width);
        assertEquals(2, shape.height);
        assertFalse(shape.contains(3, 1, 0));
    }

    @Test
    public void rejectsOversizedDimensionsAndBoundsTheSearch() {
        WebScreenShape tooWide = WebScreenShape.discover(0, 0, 0, 3,
            (x, y, z) -> z == 0 && x >= 0 && x <= 16 && y >= 0 && y <= 8);
        assertSingle(tooWide, 0, 0, 0);
        WebScreenShape tooTall = WebScreenShape.discover(0, 0, 0, 4,
            (x, y, z) -> x == 0 && z >= 0 && z <= 15 && y >= 0 && y <= 9);
        assertSingle(tooTall, 0, 0, 0);
        AtomicInteger lookups = new AtomicInteger();
        WebScreenShape infinite = WebScreenShape.discover(0, 0, 0, 5, (x, y, z) -> {
            lookups.incrementAndGet();
            return true;
        });
        assertSingle(infinite, 0, 0, 0);
        assertTrue(lookups.get() <= WebScreenShape.MAX_AREA + 1);
    }

    @Test
    public void returnsSingleWhenTheSelectedCellIsUnavailable() {
        AtomicInteger lookups = new AtomicInteger();
        WebScreenShape shape = WebScreenShape.discover(10, 20, 30, 2, (x, y, z) -> {
            lookups.incrementAndGet();
            return false;
        });
        assertSingle(shape, 10, 20, 30);
        assertEquals(1, lookups.get());
    }

    private static void assertSingle(WebScreenShape shape, int x, int y, int z) {
        assertEquals(x, shape.anchorX);
        assertEquals(y, shape.anchorY);
        assertEquals(z, shape.anchorZ);
        assertEquals(1, shape.width);
        assertEquals(1, shape.height);
        assertTrue(shape.contains(x, y, z));
    }
}
