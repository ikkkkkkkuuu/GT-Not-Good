package com.xyp.gtnotgood.ae2thing.client.render;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SlotRender {

    // LinkedHashMap: iteration MUST follow registration order. IGuiDrawSlot.drawSlot dispatches to the first
    // matching renderer that returns false, and both RenderFluidDrop and RenderFluidPacketPatternSlot match an
    // ItemFluidDrop slot. RenderFluidDrop (registered first) is the one that actually paints the fluid texture via
    // drawWidget; if RenderFluidPacketPatternSlot wins it recurses under a guard, skips baseDraw, and the slot ends
    // up blank. A plain HashMap orders Class keys by identity hashcode (effectively arbitrary per run), so the tie
    // was resolved unstably — fine upstream, blank here. Registration order makes the dispatch deterministic.
    private static final Map<Class<? extends ISlotRender>, ISlotRender> renders = new LinkedHashMap<>();
    private static final SlotRender API = new SlotRender();

    private SlotRender() {
        registerSlotRenderHandler(RenderFluidDrop.class, new RenderFluidDrop());
        registerSlotRenderHandler(RenderFluidPacketPatternSlot.class, new RenderFluidPacketPatternSlot());
        registerSlotRenderHandler(RenderPatternSlotFake.class, new RenderPatternSlotFake());
    }

    public static SlotRender instance() {
        return API;
    }

    public void registerSlotRenderHandler(Class<? extends ISlotRender> cls, ISlotRender render) {
        renders.putIfAbsent(cls, render);
    }

    public Collection<ISlotRender> getRenders() {
        return renders.values();
    }

}
