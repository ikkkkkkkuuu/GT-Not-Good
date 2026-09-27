package com.xyp.gtnotgood.ae2thing.client.event;

import net.minecraftforge.client.event.GuiScreenEvent;

import codechicken.nei.recipe.GuiOverlayButton;
import lombok.Getter;

public class GuiOverlayButtonEvent extends GuiScreenEvent {

    @Getter
    private final GuiOverlayButton button;

    public GuiOverlayButtonEvent(GuiOverlayButton btn) {
        super(btn.firstGui);
        button = btn;
    }

}
