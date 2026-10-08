package com.xyp.ldlib.gui.ui;

import static org.junit.Assert.*;

import org.junit.Test;

import com.xyp.ldlib.gui.render.ScissorScope;
import com.xyp.ldlib.gui.texture.SpriteTexture;

/** Pure geometry checks for atlas sampling and nested scissor rectangles. */
public class TextureGeometryTest {

    @Test
    public void tinyDestinationShrinksBordersWithoutChangingSourceRegion() {
        float[][] split = SpriteTexture.splitAxis(32, 16, 64, 4, 4, 10, 4);
        assertArrayEquals(new float[] { 10, 12, 12, 14 }, split[0], 0.0001f);
        assertArrayEquals(new float[] { 0.5f, 0.5625f, 0.6875f, 0.75f }, split[1], 0.0001f);
    }

    @Test
    public void asymmetricBordersKeepTheirRatioWhenOversized() {
        float[][] split = SpriteTexture.splitAxis(0, 6, 6, 8, 4, 0, 3);
        assertArrayEquals(new float[] { 0, 2, 2, 3 }, split[0], 0.0001f);
    }

    @Test
    public void scissorIntersectionNeverExpandsParentOrProducesNegativeSize() {
        assertArrayEquals(new int[] { 20, 10, 10, 20 }, ScissorScope.intersect(20, 5, 20, 25, 10, 10, 20, 20));
        assertArrayEquals(new int[] { 40, 40, 0, 0 }, ScissorScope.intersect(40, 40, 10, 10, 0, 0, 20, 20));
    }
}
