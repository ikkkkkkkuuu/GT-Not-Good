package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

/** Converts AE2's serialized suffixes to the combined terminal's client-localized, bracket-free display text. */
final class InterfaceTerminalSuffix {

    private InterfaceTerminalSuffix() {}

    /**
     * Resolves chat components before removing display brackets so JSON arrays and component arguments stay intact.
     * Legacy plain-text suffixes are accepted as well. The result remains a serialized component for AE2 to consume.
     *
     * @param suffix serialized component or legacy text received from the server
     * @return normalized serialized text, or the original null/empty value
     */
    static String normalize(String suffix) {
        if (suffix == null || suffix.isEmpty()) return suffix;
        String text = suffix;
        try {
            IChatComponent component = IChatComponent.Serializer.func_150699_a(suffix);
            if (component != null) text = component.getUnformattedText();
        } catch (RuntimeException ignored) {
            // Older providers may send plain text rather than a serialized component.
        }
        String cleaned = text.replace("[", "")
            .replace("]", "")
            .trim()
            .replaceAll("\\s+", " ");
        return IChatComponent.Serializer.func_150696_a(new ChatComponentText(cleaned.isEmpty() ? "" : " " + cleaned));
    }
}
