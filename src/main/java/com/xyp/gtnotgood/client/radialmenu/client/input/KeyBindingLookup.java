package com.xyp.gtnotgood.client.radialmenu.client.input;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public final class KeyBindingLookup {

    private KeyBindingLookup() {}

    public static List<KeyBinding> all() {
        List<KeyBinding> bindings = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings == null || mc.gameSettings.keyBindings == null) {
            return bindings;
        }
        for (KeyBinding binding : mc.gameSettings.keyBindings) {
            if (binding != null) {
                bindings.add(binding);
            }
        }
        return bindings;
    }

    public static KeyBinding find(String description, String category) {
        if (description == null || description.isEmpty()) {
            return null;
        }
        KeyBinding descriptionOnlyMatch = null;
        for (KeyBinding binding : all()) {
            if (!description.equalsIgnoreCase(binding.getKeyDescription())) {
                continue;
            }
            if (category != null && !category.isEmpty() && category.equalsIgnoreCase(binding.getKeyCategory())) {
                return binding;
            }
            if (descriptionOnlyMatch == null) {
                descriptionOnlyMatch = binding;
            }
        }
        return descriptionOnlyMatch;
    }

    public static boolean isUnbound(KeyBinding binding) {
        return binding != null && binding.getKeyCode() == 0;
    }
}
