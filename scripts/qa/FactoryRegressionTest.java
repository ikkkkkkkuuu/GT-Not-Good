package com.xyp.gtnotgood.common.gui.modularui.widget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;

public class FactoryRegressionTest {

    @Test
    public void openingFourExistingRoutesBeforeLayoutKeepsThemVisible() {
        ScrollArea area = area(0);
        FactoryListScroll state = new FactoryListScroll();
        state.update(area, 4, 1, true);
        area.height = 180;
        state.update(area, 4, 1, true);
        assertEquals(
            0,
            area.getScrollY()
                .getScroll());
        assertEquals(
            80,
            area.getScrollY()
                .getScrollSize());
    }

    @Test
    public void appendingFollowsBottomButSwitchingPagesResetsPosition() {
        ScrollArea area = area(180);
        FactoryListScroll state = new FactoryListScroll();
        state.update(area, 20, 1, true);
        assertEquals(
            0,
            area.getScrollY()
                .getScroll());
        state.update(area, 21, 1, true);
        assertEquals(
            240,
            area.getScrollY()
                .getScroll());
        state.update(area, 30, 2, true);
        assertEquals(
            0,
            area.getScrollY()
                .getScroll());
    }

    @Test
    public void shrinkingListAndStalePreLayoutOffsetAreClamped() {
        ScrollArea area = area(180);
        FactoryListScroll state = new FactoryListScroll();
        state.update(area, 32, 1, true);
        area.getScrollY()
            .scrollTo(area, Integer.MAX_VALUE);
        state.update(area, 4, 1, true);
        assertEquals(
            0,
            area.getScrollY()
                .getScroll());
        area.height = 0;
        area.getScrollY()
            .scrollTo(area, Integer.MAX_VALUE);
        area.height = 180;
        state.update(area, 4, 1, true);
        assertEquals(
            0,
            area.getScrollY()
                .getScroll());
    }

    @Test
    public void changingSelectedRouteResetsIngredientScroll() {
        ScrollArea area = area(148);
        FactoryListScroll state = new FactoryListScroll();
        state.update(area, 30, 1, false);
        area.getScrollY()
            .scrollTo(area, 300);
        state.update(area, 30, 1, false);
        assertEquals(
            300,
            area.getScrollY()
                .getScroll());
        state.update(area, 30, 2, false);
        assertEquals(
            0,
            area.getScrollY()
                .getScroll());
    }

    private static ScrollArea area(int height) {
        ScrollArea area = new ScrollArea(0, 0, 110, height);
        area.setScrollDataY(new VerticalScrollData());
        return area;
    }
}
