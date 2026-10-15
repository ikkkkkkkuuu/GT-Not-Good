package com.xyp.gtnotgood.client.radialmenu.client.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.client.input.KeyBindingLookup;
import com.xyp.gtnotgood.client.radialmenu.client.input.KeybindStateTracker;
import com.xyp.gtnotgood.client.radialmenu.client.input.VanillaKeyEffects;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.action.Modifiers;

public class KeyActionExecutor implements IActionExecutor {

    private static final int SNEAK_LEAD_TICKS = 1;

    private static final int SNEAK_HOLD_TICKS = 4;

    @Override
    public String typeId() {
        return ActionTypes.KEYBIND;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        String description = spec.getString(ActionTypes.PARAM_BINDING, null);
        String category = spec.getString(ActionTypes.PARAM_CATEGORY, null);

        KeyBinding binding = KeyBindingLookup.find(description, category);
        if (binding == null) {
            RadialMenuMod.LOG.warn("Keybinding '" + description + "' is not registered by any mod");
            return false;
        }

        if (VanillaKeyEffects.tryRun(binding)) {
            return true;
        }

        KeybindStateTracker.Mode mode = spec.getEnum(ActionTypes.PARAM_MODE, KeybindStateTracker.Mode.class,
            KeybindStateTracker.Mode.Tap);
        int holdTicks = spec.getInt(ActionTypes.PARAM_HOLD_TICKS, 20);

        if (spec.getBoolean(ActionTypes.PARAM_SNEAK, false)) {
            return sneakThenPress(spec, mode, holdTicks);
        }

        boolean activated = KeybindStateTracker.activate(binding, mode, holdTicks, Modifiers.of(spec));
        SneakDiagnostics.log("pressed " + description, binding);
        return activated;
    }

    /** Player movement updates must observe sneaking before the synthetic key event. */
    private boolean sneakThenPress(ActionSpec spec, KeybindStateTracker.Mode mode, int holdTicks) {
        KeyBinding sneak = Minecraft.getMinecraft().gameSettings.keyBindSneak;

        int sneakTicks = mode == KeybindStateTracker.Mode.Hold ? Math.max(SNEAK_HOLD_TICKS, holdTicks)
            : SNEAK_HOLD_TICKS;
        KeybindStateTracker.activate(sneak, KeybindStateTracker.Mode.Hold, sneakTicks, Modifiers.NONE);
        SneakDiagnostics.log("sneak started", sneak);

        ActionSpec pressOnly = spec.copy();
        pressOnly.set(ActionTypes.PARAM_SNEAK, "false");
        DelayedActions.scheduleAction(spec, pressOnly, SNEAK_LEAD_TICKS);
        return true;
    }
}
