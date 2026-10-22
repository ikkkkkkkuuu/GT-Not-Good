package com.xyp.gtnotgood.common.gui.modularui.multiblock.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeDisplay;
import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import cpw.mods.fml.common.Loader;
import gregtech.api.util.GTUtility;
import sun.misc.Unsafe;

public class RecipeOutputFormattingTest {

    private static final String[] rateLabels = { "per_tick", "per_second", "per_minute", "per_hour", "per_day" };
    private static final Map<String, String> previousTranslations = new HashMap<>();
    private static Map<String, String> translations;
    private static Locale previousLocale;
    private static Item item;

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void bootstrapNativeFormatting() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        if (loader.get(null) == null) loader.set(null, unsafe.allocateInstance(Loader.class));
        Field namedMods = Loader.class.getDeclaredField("namedMods");
        namedMods.setAccessible(true);
        if (namedMods.get(Loader.instance()) == null) namedMods.set(Loader.instance(), new HashMap<>());
        HeadlessBlockRegistry.bootstrap();
        previousLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        Field localizedName = StatCollector.class.getDeclaredField("localizedName");
        localizedName.setAccessible(true);
        Field languageList = StringTranslate.class.getDeclaredField("languageList");
        languageList.setAccessible(true);
        translations = (Map<String, String>) languageList.get(localizedName.get(null));
        register("GT5U.gui.text.amount", "\u00a7fAmount:");
        register("GT5U.gui.text.per_tick", "\u00a7fPer tick:");
        register("GT5U.gui.text.per_second", "\u00a7fPer second:");
        register("GT5U.gui.text.per_minute", "\u00a7fPer minute:");
        register("GT5U.gui.text.per_hour", "\u00a7fPer hour:");
        register("GT5U.gui.text.per_day", "\u00a7fPer day:");
        register("GT5U.gui.text.rate_short", "(\u00a77%s/s\u00a7f)");
        register("GT5U.gui.text.rate_short_slow", "(\u00a77%ss/each\u00a7f)");
        register("GT5U.gui.text.rate_large_suffix", "\u00a7f[\u00a77%s\u00a7f]");
        register("GT5U.gui.text.item_amount_display", "\u00a7b%s\u00a7f x \u00a76%s\u00a7f");
        register("GT5U.gui.text.fluid_amount_display", "\u00a7b%s\u00a7f x \u00a76%sL\u00a7f");
        register("item.recipeOutputFormatting.name", "Formatting output");
        String name = "test:recipe_output_formatting";
        item = (Item) Item.itemRegistry.getObject(name);
        if (item == null) {
            item = new Item().setUnlocalizedName("recipeOutputFormatting");
            Method register = Item.itemRegistry.getClass().getDeclaredMethod("addObjectRaw", int.class, String.class,
                Object.class);
            register.setAccessible(true);
            register.invoke(Item.itemRegistry, 31256, name, item);
        }
    }

    @AfterClass
    public static void restoreNativeFormatting() {
        if (previousLocale != null) Locale.setDefault(previousLocale);
        if (translations == null) return;
        for (Map.Entry<String, String> entry : previousTranslations.entrySet()) {
            if (entry.getValue() == null) translations.remove(entry.getKey());
            else translations.put(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void matchesNativeItemAndFluidTextAndTooltipForOrdinaryRates() {
        long[][] cases = { { 192000, 128 }, { 2600, 128 }, { 1000000, 20 }, { 1000001, 20 }, { 1, 128 }, { 1, 20 },
            { 1, 200 } };
        for (boolean fluid : new boolean[] { false, true }) for (long[] example : cases) {
            long amount = example[0];
            int ticks = (int) example[1];
            NBTTagCompound row = row(fluid, BigInteger.valueOf(amount), ticks);
            String message = "fluid=" + fluid + ", amount=" + amount + ", ticks=" + ticks;

            assertEquals(message, nativeAmountLine(fluid, amount, ticks), RecipeOutputFormatting.amountLine(row));
            assertEquals(message, nativeTooltip(row, fluid, amount, ticks), RecipeOutputFormatting.tooltip(row));
        }
    }

    @Test
    public void taskDurationControlsEveryDisplayedRateIndependently() {
        NBTTagCompound fast = row(true, BigInteger.valueOf(192000), 128);
        NBTTagCompound slow = row(true, BigInteger.valueOf(192000), 256);

        assertEquals(nativeAmountLine(true, 192000, 128), RecipeOutputFormatting.amountLine(fast));
        assertEquals(nativeAmountLine(true, 192000, 256), RecipeOutputFormatting.amountLine(slow));
        assertEquals(nativeTooltip(fast, true, 192000, 128), RecipeOutputFormatting.tooltip(fast));
        assertEquals(nativeTooltip(slow, true, 192000, 256), RecipeOutputFormatting.tooltip(slow));
        assertFalse(RecipeOutputFormatting.amountLine(fast).equals(RecipeOutputFormatting.amountLine(slow)));
        assertEquals("192000", fast.getString("amount"));
        assertEquals("192000", slow.getString("amount"));
    }

    @Test
    public void pausedTasksRetainQuantityAndShowZeroAtAllTimeScales() {
        for (boolean fluid : new boolean[] { false, true }) {
            NBTTagCompound row = row(fluid, BigInteger.valueOf(192000), 0);
            NBTTagCompound before = (NBTTagCompound) row.copy();
            String amountLine = RecipeOutputFormatting.amountLine(row);
            String tooltip = RecipeOutputFormatting.tooltip(row);
            String expectedAmount = StatCollector.translateToLocalFormatted(
                "GT5U.gui.text." + (fluid ? "fluid" : "item") + "_amount_display", "",
                GTUtility.formatShortenedLong(192000));
            assertEquals(
                expectedAmount + " " + StatCollector.translateToLocalFormatted("GT5U.gui.text.rate_short", "0"),
                amountLine);
            String cleanTooltip = EnumChatFormatting.getTextWithoutFormattingCodes(tooltip);
            assertTrue(cleanTooltip.contains("Amount: " + NumberFormatUtil.formatNumber(192000) + (fluid ? "L" : "")));
            for (String label : rateLabels) {
                String text = EnumChatFormatting
                    .getTextWithoutFormattingCodes(StatCollector.translateToLocal("GT5U.gui.text." + label));
                assertTrue(cleanTooltip.contains(text + " " + NumberFormatUtil.formatNumber(0) + (fluid ? "L" : "")));
            }
            assertFiniteAndNonnegative(amountLine);
            assertFiniteAndNonnegative(tooltip);
            assertEquals(before, row);
        }
    }

    @Test
    public void repeatedMaximumStacksAndBeyondLongAmountsNeverOverflow() {
        BigInteger[] amounts = { BigInteger.valueOf(Integer.MAX_VALUE).multiply(BigInteger.valueOf(16)),
            BigInteger.valueOf(Long.MAX_VALUE), BigInteger.TEN.pow(40).add(BigInteger.valueOf(37)) };
        for (boolean fluid : new boolean[] { false, true }) for (BigInteger amount : amounts) {
            NBTTagCompound row = row(fluid, amount, 1);
            NBTTagCompound before = (NBTTagCompound) row.copy();
            String amountLine = RecipeOutputFormatting.amountLine(row);
            String tooltip = RecipeOutputFormatting.tooltip(row);

            assertFiniteAndNonnegative(amountLine);
            assertFiniteAndNonnegative(tooltip);
            assertTrue(tooltip.contains(NumberFormatUtil.formatNumber(amount)));
            assertEquals(amount.toString(), row.getString("amount"));
            assertEquals(before, row);
        }
    }

    private static void register(String key, String value) {
        previousTranslations.put(key, translations.put(key, value));
    }

    private static NBTTagCompound row(boolean fluid, BigInteger amount, int ticks) {
        NBTTagCompound row = new NBTTagCompound();
        row.setString("amount", amount.toString());
        row.setInteger("ticks", ticks);
        row.setTag(fluid ? "fluid" : "item",
            fluid ? new FluidStack(FluidRegistry.WATER, 1).writeToNBT(new NBTTagCompound())
                : new ItemStack(item, 1).writeToNBT(new NBTTagCompound()));
        return row;
    }

    private static String nativeAmountLine(boolean fluid, long amount, int ticks) {
        return StatCollector.translateToLocalFormatted(
            "GT5U.gui.text." + (fluid ? "fluid" : "item") + "_amount_display", "",
            GTUtility.formatShortenedLong(amount)) + GTUtility.appendRate(fluid, amount, true, ticks);
    }

    private static String nativeTooltip(NBTTagCompound row, boolean fluid, long amount, int ticks) {
        return EnumChatFormatting.AQUA + WirelessRecipeDisplay.name(row)
            + "\n"
            + GTUtility.appendRate(fluid, amount, false, ticks);
    }

    private static void assertFiniteAndNonnegative(String value) {
        String text = EnumChatFormatting.getTextWithoutFormattingCodes(value);
        assertFalse(text, text.contains("NaN"));
        assertFalse(text, text.contains("Infinity"));
        assertFalse(text, text.contains("\u221e"));
        assertFalse(text, text.contains("-"));
    }
}
