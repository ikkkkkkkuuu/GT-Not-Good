package com.xyp.gtnotgood.mixins.early.radialmenu;

import net.minecraft.client.settings.KeyBinding;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {

    @Accessor("pressed")
    void radialmenu$setPressed(boolean pressed);

    @Accessor("pressed")
    boolean radialmenu$isPressed();

    @Accessor("pressTime")
    int radialmenu$getPressTime();

    @Accessor("pressTime")
    void radialmenu$setPressTime(int pressTime);
}
