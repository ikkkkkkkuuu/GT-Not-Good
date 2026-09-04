package com.xyp.gtnotgood.utils.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;

/**
 * Stores item tooltip suppliers and appends their current text during the Forge tooltip event.
 * <p>
 * Tooltips are stored as {@link Supplier Suppliers} instead of fixed strings because animated tooltips need to be
 * recalculated every frame. The class is subscribed only on the client, so callers may safely register tooltip
 * suppliers from common registration code while the actual rendering hook remains client-side.
 *
 * @see ItemTooltipEvent
 * @see EventBusSubscriber
 */
@EventBusSubscriber(side = Side.CLIENT)
public class AnimatedTooltipHandler {

    /**
     * ItemStack-keyed tooltip registry used by {@link #renderTooltip(ItemTooltipEvent)}.
     * <p>
     * The GTNHLib {@link ItemStackMap} compares stacks by item, metadata, and NBT instead of object identity, which
     * lets registration code use a copied stack while the tooltip event receives another stack instance.
     */
    public static final Map<ItemStack, List<Supplier<String>>> tooltipMap = new ItemStackMap<>(false);

    public static final String AQUA;
    public static final String BLUE;
    public static final String GOLD;
    public static final String GREEN;
    public static final String LIGHT_PURPLE;
    public static final String RED;
    public static final String RESET;
    public static final String YELLOW;

    /**
     * Combines multiple dynamic text suppliers into one dynamic supplier.
     * <p>
     * Each part is evaluated when the resulting supplier is queried. This is important for animated text, because
     * evaluating all parts at registration time would freeze the colors forever.
     *
     * @param parts text suppliers to append in order
     * @return a supplier that returns all current parts concatenated
     */
    @SafeVarargs
    public static Supplier<String> chain(Supplier<String>... parts) {
        return () -> {
            StringBuilder builder = new StringBuilder();
            for (Supplier<String> text : parts) {
                builder.append(text.get());
            }
            return builder.toString();
        };
    }

    /**
     * Wraps a fixed string as a supplier for APIs that expect dynamic tooltip text.
     *
     * @param text fixed text to return
     * @return a supplier returning {@code text}
     */
    public static Supplier<String> text(String text) {
        return () -> text;
    }

    /**
     * Creates a formatting supplier using {@link Locale#ROOT}.
     * <p>
     * Use this for tooltip numbers or fixed technical strings where the output should not depend on the player's
     * operating-system locale. Localized player-facing text should still come from translation keys.
     *
     * @param format {@link String#format(Locale, String, Object...)} format string
     * @param args   format arguments
     * @return a supplier that formats the text whenever it is queried
     */
    public static Supplier<String> text(String format, Object... args) {
        return () -> String.format(Locale.ROOT, format, args);
    }

    /**
     * Builds a color-cycling supplier for one line of tooltip text.
     * <p>
     * The color for each character is selected from {@code formattingArray}. The selected color shifts over wall-clock
     * time, so the same supplier can be rendered repeatedly to create a scrolling rainbow effect.
     *
     * @param text            unformatted text to animate
     * @param posstep         color index step per character; zero is treated as one
     * @param delay           milliseconds between color shifts; values below one are clamped to one
     * @param formattingArray Minecraft formatting strings used as the color cycle
     * @return a supplier returning the currently colorized text, or an empty supplier for invalid input
     */
    public static Supplier<String> animatedText(String text, int posstep, int delay, String... formattingArray) {
        if (text == null || text.isEmpty() || formattingArray == null || formattingArray.length == 0) return () -> "";

        final int finalDelay = Math.max(delay, 1);
        final int finalPosstep = posstep == 0 ? 1 : posstep;

        return () -> {
            StringBuilder builder = new StringBuilder(text.length() * 3);
            int length = formattingArray.length;
            int offset = (int) ((System.currentTimeMillis() / finalDelay) % length);

            for (int i = 0; i < text.length(); i++) {
                int colorIndex = Math.floorMod(i * finalPosstep - offset, length);
                builder.append(formattingArray[colorIndex]);
                builder.append(text.charAt(i));
            }
            return builder.toString();
        };
    }

    /**
     * Registers a tooltip supplier by resolving an item from the Forge game registry.
     *
     * @param modID        registry namespace containing the item
     * @param registryName registry path of the item
     * @param meta         metadata value used for the tooltip stack key
     * @param tooltip      tooltip supplier to append when the matching stack is shown
     * @see GameRegistry#findItem(String, String)
     */
    public static void addItemTooltip(String modID, String registryName, int meta, Supplier<String> tooltip) {
        Item item = GameRegistry.findItem(modID, registryName);
        if (item == null || tooltip == null) return;
        addItemTooltip(new ItemStack(item, 1, meta), tooltip);
    }

    /**
     * Registers a tooltip supplier for an exact stack key.
     * <p>
     * The stack is used only as the key in {@link #tooltipMap}. The supplier itself is called later during the client
     * tooltip event, which is why animated text continues to move after registration.
     *
     * @param item    stack key to match in item tooltips
     * @param tooltip dynamic tooltip text to append
     */
    public static void addItemTooltip(ItemStack item, Supplier<String> tooltip) {
        if (item == null || tooltip == null) return;
        List<Supplier<String>> list = tooltipMap.computeIfAbsent(item, key -> new ArrayList<>());
        list.add(tooltip);
    }

    /**
     * Appends all registered tooltip lines for the stack currently being rendered.
     * <p>
     * Tooltip suppliers may return text containing newlines. Those are split here so Minecraft receives one tooltip
     * entry per visible line.
     *
     * @param event Forge item tooltip event fired on the client
     */
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void renderTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.itemStack;
        List<Supplier<String>> tooltips = tooltipMap.get(stack);
        if (tooltips == null) return;

        for (Supplier<String> tooltip : tooltips) {
            String text = tooltip.get();
            if (text != null) {
                event.toolTip.addAll(Arrays.asList(text.split("\n")));
            }
        }
    }

    static {
        AQUA = EnumChatFormatting.AQUA.toString();
        BLUE = EnumChatFormatting.BLUE.toString();
        GOLD = EnumChatFormatting.GOLD.toString();
        GREEN = EnumChatFormatting.GREEN.toString();
        LIGHT_PURPLE = EnumChatFormatting.LIGHT_PURPLE.toString();
        RED = EnumChatFormatting.RED.toString();
        RESET = EnumChatFormatting.RESET.toString();
        YELLOW = EnumChatFormatting.YELLOW.toString();
    }
}
