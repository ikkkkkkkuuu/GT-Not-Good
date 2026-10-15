package com.xyp.gtnotgood.client.radialmenu.client.input;

import net.minecraft.client.settings.KeyBinding;

public final class HeldKeyResync {

    private HeldKeyResync() {}

    public static void resyncHeldKeys() {
        for (KeyBinding binding : KeyBindingLookup.all()) {
            if (KeyInjector.isPhysicallyDown(binding)) {
                KeyInjector.hold(binding);
            }
        }
    }
}
