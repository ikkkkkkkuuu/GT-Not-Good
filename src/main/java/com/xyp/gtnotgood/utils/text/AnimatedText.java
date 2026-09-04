package com.xyp.gtnotgood.utils.text;

import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.AQUA;
import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.BLUE;
import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.GOLD;
import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.GREEN;
import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.LIGHT_PURPLE;
import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.RED;
import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.YELLOW;
import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.animatedText;
import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.chain;
import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.text;

import java.util.function.Supplier;

import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.utils.enums.ModList;

/**
 * Provides reusable animated text suppliers for item tooltip credits.
 * <p>
 * Keep long or frequently reused animated lines here instead of rebuilding them inside each loader. Registration code
 * should pass these suppliers to {@link AnimatedTooltipHandler#addItemTooltip(net.minecraft.item.ItemStack, Supplier)}
 * so all machines can share the same mod credit style.
 */
public class AnimatedText {

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
        text(StatCollector.translateToLocal("tooltip.gtnotgood.adder")),
        animatedText(ModList.GTNotGood.getDisplayName(), 1, 80, RED, GOLD, YELLOW, GREEN, AQUA, BLUE, LIGHT_PURPLE));

    private AnimatedText() {}
}
