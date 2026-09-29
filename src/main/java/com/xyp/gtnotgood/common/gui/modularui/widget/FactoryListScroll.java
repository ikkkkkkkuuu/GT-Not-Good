package com.xyp.gtnotgood.common.gui.modularui.widget;

import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.cleanroommc.modularui.widget.scroll.ScrollData;

/** Keeps factory rows visible across the pre-layout opening tick, page changes and list shrinkage. */
public final class FactoryListScroll {

    private int previousCount = -1;
    private int previousPage = -1;

    public void update(ScrollArea area, int count, int page, boolean followAppends) {
        ScrollData scroll = area.getScrollY();
        scroll.setScrollSize(count * 20);
        // MUI calls the first update before layout; do not treat initial synchronization as an append.
        if (page != previousPage || previousCount < 0) {
            scroll.scrollTo(area, 0);
        } else if (followAppends && count > previousCount && area.height > 0) {
            scroll.scrollTo(area, Integer.MAX_VALUE);
        }
        // setScrollSize does not clamp an offset left over from a larger list or an unlaid-out viewport.
        scroll.clamp(area);
        previousCount = count;
        previousPage = page;
    }
}
