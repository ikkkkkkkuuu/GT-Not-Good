package com.xyp.gtnotgood.utils.text;

import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.chain;

import java.util.function.Supplier;

import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.effect.TextEffectStyle;
import com.xyp.gtnotgood.utils.text.effect.TextEffects;

/**
 * Provides reusable animated text suppliers for item tooltip credits.
 * <p>
 * Keep long or frequently reused animated lines here instead of rebuilding them inside each loader. Registration code
 * should pass these suppliers to {@link AnimatedTooltipHandler#addItemTooltip(net.minecraft.item.ItemStack, Supplier)}
 * so all machines can share the same mod credit style.
 */
public class AnimatedText {

    private static volatile TextEffectStyle creditStyle = TextEffects.EXOTIC_RAINBOW;
    private static volatile boolean creditBold;
    private static volatile boolean creditItalic;

    /**
     * Changes the client-visible machine credit without rebuilding the tooltip registry.
     * The existing supplier reads these fields whenever a tooltip is displayed.
     *
     * @param style  renderer, palette and speed for the credit
     * @param bold   whether to apply Minecraft's bold format
     * @param italic whether to apply Minecraft's italic format
     */
    public static void configureCredit(TextEffectStyle style, boolean bold, boolean italic) {
        creditStyle = java.util.Objects.requireNonNull(style, "style");
        creditBold = bold;
        creditItalic = italic;
    }

    /** @return currently selected machine credit effect */
    public static TextEffectStyle creditStyle() {
        return creditStyle;
    }

    /** @return whether the machine credit is bold */
    public static boolean creditBold() {
        return creditBold;
    }

    /** @return whether the machine credit is italic */
    public static boolean creditItalic() {
        return creditItalic;
    }

    /**
     * Builds the current shader span at display time and resets native font formatting afterward.
     *
     * @return complete mod-name credit in the selected effect
     */
    private static String machineCredit() {
        String formatting = (creditBold ? "\u00a7l" : "") + (creditItalic ? "\u00a7o" : "");
        return TextEffects.apply(formatting + ModList.GTNotGood.getDisplayName(), creditStyle) + "\u00a7r";
    }

    /**
     * Standard animated "Mod Added by" tooltip line for GT Not Good machines.
     * <p>
     * The translation comment must stay directly above the translation key usage, because {@code addon.gradle} extracts
     * language entries from Java comments during {@code processResources}.
     */
    public static final Supplier<String> GT_NOT_GOOD = chain(
        // #tr tooltip.gtnotgood.adder
        // # Mod Added by:
        // # zh_CN 添加模组：
        () -> StatCollector.translateToLocal("tooltip.gtnotgood.adder"),
        AnimatedText::machineCredit);

    private AnimatedText() {}
}
