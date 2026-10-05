package com.xyp.gtnotgood.common.gui.modularui.widget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.xyp.gtnotgood.utils.machine.factory.FactoryBatchDuration;
import com.xyp.gtnotgood.utils.machine.factory.FactoryGraph;

public class FactoryRegressionTest {

    @Test
    public void atomicBatchUsesLongestSerialPathAndOneRoundPerCycle() {
        FactoryGraph graph = new FactoryGraph();
        for (int i = 0; i < 4; i++) graph.add("");
        // 20 -> {40, 60} -> 40 = 120 ticks, regardless of page batching or parallel counts.
        int[] durations = { 20, 40, 60, 40 };
        graph.nodes.get(1).sources.add(0);
        graph.nodes.get(2).sources.add(0);
        graph.nodes.get(3).sources.add(1);
        graph.nodes.get(3).sources.add(2);
        for (FactoryGraph.Node node : graph.nodes) {
            node.wholeLineBatch = true;
            node.parallel = Integer.MAX_VALUE;
        }
        assertEquals(120, FactoryBatchDuration.ticks(graph.nodes, n -> durations[n.id]));
        graph.nodes.get(0).sources.add(2);
        // Cycle {0,2} costs 80, followed by node 1 (40), then node 3 (40).
        assertEquals(160, FactoryBatchDuration.ticks(graph.nodes, n -> durations[n.id]));
    }

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
