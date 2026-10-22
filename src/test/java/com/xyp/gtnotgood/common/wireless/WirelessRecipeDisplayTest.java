package com.xyp.gtnotgood.common.wireless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;
import com.xyp.gtnotgood.utils.HeadlessItemRegistry;

import cpw.mods.fml.common.Loader;
import sun.misc.Unsafe;

public class WirelessRecipeDisplayTest {

    private static Item item;
    private static Unsafe unsafe;

    @BeforeClass
    public static void bootstrapNativeStacks() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        if (loader.get(null) == null) loader.set(null, unsafe.allocateInstance(Loader.class));
        Field namedMods = Loader.class.getDeclaredField("namedMods");
        namedMods.setAccessible(true);
        if (namedMods.get(Loader.instance()) == null) namedMods.set(Loader.instance(), new HashMap<>());
        HeadlessBlockRegistry.bootstrap();
        HeadlessItemRegistry.bootstrap();
        String name = "test:wireless_recipe_display";
        item = (Item) Item.itemRegistry.getObject(name);
        if (item == null) {
            item = new Item();
            Method register = Item.itemRegistry.getClass().getDeclaredMethod("addObjectRaw", int.class, String.class,
                Object.class);
            register.setAccessible(true);
            register.invoke(Item.itemRegistry, 31255, name, item);
        }
    }

    @Test
    public void preservesEachTasksDurationAndStopsRatesForFinishedTasks() {
        WirelessWork fast = work(40, 40);
        WirelessWork slow = work(40, 80);
        WirelessWork finished = work(40, 1);
        assertTrue(finished.tick(cost -> true));

        List<NBTTagCompound> rows = WirelessRecipeDisplay.rows(Arrays.asList(fast, slow, finished), 10);

        assertEquals(3, rows.size());
        assertEquals(40, rows.get(0).getInteger("ticks"));
        assertEquals(80, rows.get(1).getInteger("ticks"));
        assertEquals("20/s", WirelessRecipeDisplay.rate(rows.get(0), false));
        assertEquals("10/s", WirelessRecipeDisplay.rate(rows.get(1), false));
        assertEquals(0, rows.get(2).getInteger("ticks"));
        assertEquals("0/s", WirelessRecipeDisplay.rate(rows.get(2), false));
    }

    @Test
    public void retainsExactHugeAmountsAndDistinctStackNbt() {
        ItemStack first = stack("first");
        ItemStack second = stack("second");
        FluidStack fluid = new FluidStack(FluidRegistry.WATER, 1000);
        fluid.tag = tag("water");
        FluidStack otherFluid = new FluidStack(FluidRegistry.WATER, 2000);
        otherFluid.tag = tag("distilled");
        WirelessOutputs outputs = new WirelessOutputs(new ItemStack[] { first, second },
            new FluidStack[] { fluid, otherFluid });
        BigInteger huge = BigInteger.ONE.shiftLeft(100).add(BigInteger.valueOf(37));
        outputs.itemAmounts[0] = huge;
        outputs.itemAmounts[1] = huge.add(BigInteger.ONE);
        outputs.fluidAmounts[0] = huge.multiply(BigInteger.TEN);
        outputs.fluidAmounts[1] = huge.multiply(BigInteger.TEN).add(BigInteger.ONE);
        List<NBTTagCompound> rows = new ArrayList<>();

        WirelessRecipeDisplay.appendRows(rows, outputs, 60, 10);

        assertEquals(4, rows.size());
        assertEquals(huge.toString(), rows.get(0).getString("amount"));
        assertEquals(huge.add(BigInteger.ONE).toString(), rows.get(1).getString("amount"));
        assertEquals(huge.multiply(BigInteger.TEN).toString(), rows.get(2).getString("amount"));
        assertEquals(huge.multiply(BigInteger.TEN).add(BigInteger.ONE).toString(), rows.get(3).getString("amount"));
        ItemStack firstSnapshot = ItemStack.loadItemStackFromNBT(rows.get(0).getCompoundTag("item"));
        ItemStack secondSnapshot = ItemStack.loadItemStackFromNBT(rows.get(1).getCompoundTag("item"));
        assertEquals(item, firstSnapshot.getItem());
        assertEquals(7, firstSnapshot.getItemDamage());
        assertEquals("first", firstSnapshot.getTagCompound().getString("variant"));
        assertEquals("second", secondSnapshot.getTagCompound().getString("variant"));
        FluidStack fluidSnapshot = FluidStack.loadFluidStackFromNBT(rows.get(2).getCompoundTag("fluid"));
        assertEquals(FluidRegistry.WATER, fluidSnapshot.getFluid());
        assertEquals("water", fluidSnapshot.tag.getString("variant"));
        FluidStack otherFluidSnapshot = FluidStack.loadFluidStackFromNBT(rows.get(3).getCompoundTag("fluid"));
        assertEquals(FluidRegistry.WATER, otherFluidSnapshot.getFluid());
        assertEquals("distilled", otherFluidSnapshot.tag.getString("variant"));
        assertEquals(huge, outputs.itemAmounts[0]);
        assertEquals(1, outputs.items[0].stackSize);
        assertEquals(1, outputs.fluids[0].amount);
    }

    @Test
    public void repeatedMaximumStacksAcrossTasksRemainExactAfterNbtRoundTrip() {
        ItemStack[] items = new ItemStack[8];
        FluidStack[] fluids = new FluidStack[8];
        Arrays.setAll(items, index -> new ItemStack(item, Integer.MAX_VALUE, 7));
        Arrays.setAll(fluids, index -> new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE));
        List<WirelessWork> tasks = Arrays.asList(
            new WirelessWork(new UUID(0, 1), "fast", BigInteger.ZERO, 20, 1, items, fluids),
            new WirelessWork(new UUID(0, 1), "slow", BigInteger.ZERO, 40, 1, items, fluids));
        List<WirelessWork> restored = new ArrayList<>();
        for (WirelessWork task : tasks) restored.add(WirelessWork.load(task.save()));

        List<NBTTagCompound> rows = WirelessRecipeDisplay.rows(restored, 64);

        assertEquals(4, rows.size());
        assertEquals(WirelessRecipeDisplay.rows(tasks, 64), rows);
        BigInteger itemTotal = BigInteger.ZERO;
        BigInteger fluidTotal = BigInteger.ZERO;
        for (int index = 0; index < rows.size(); index++) {
            NBTTagCompound row = rows.get(index);
            BigInteger amount = new BigInteger(row.getString("amount"));
            assertEquals(new BigInteger("17179869176"), amount);
            assertEquals(index < 2 ? 20 : 40, row.getInteger("ticks"));
            String expectedRate = index < 2 ? "17,179,869,176" : "8,589,934,588";
            String rate = WirelessRecipeDisplay.rate(row, false);
            assertFalse(rate.contains("-"));
            if (row.hasKey("item")) {
                itemTotal = itemTotal.add(amount);
                assertEquals(expectedRate + "/s", rate);
                assertEquals(1, ItemStack.loadItemStackFromNBT(row.getCompoundTag("item")).stackSize);
            } else {
                fluidTotal = fluidTotal.add(amount);
                assertEquals(expectedRate + " L/s", rate);
                assertEquals(1, FluidStack.loadFluidStackFromNBT(row.getCompoundTag("fluid")).amount);
            }
        }
        assertEquals(new BigInteger("34359738352"), itemTotal);
        assertEquals(new BigInteger("34359738352"), fluidTotal);
        for (WirelessWork task : restored) {
            for (BigInteger amount : task.outputs.itemAmounts)
                assertEquals(BigInteger.valueOf(Integer.MAX_VALUE), amount);
            for (BigInteger amount : task.outputs.fluidAmounts)
                assertEquals(BigInteger.valueOf(Integer.MAX_VALUE), amount);
        }
    }

    @Test
    public void sixteenIdenticalMaximumStacksMergeBeforeApplyingTheRowLimit() {
        ItemStack[] items = new ItemStack[16];
        FluidStack[] fluids = new FluidStack[16];
        Arrays.setAll(items, index -> new ItemStack(item, Integer.MAX_VALUE, 7));
        Arrays.setAll(fluids, index -> new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE));
        WirelessOutputs outputs = new WirelessOutputs(items, fluids);
        List<NBTTagCompound> rows = new ArrayList<>();

        WirelessRecipeDisplay.appendRows(rows, WirelessOutputs.load(outputs.save()), 20, 2);

        assertEquals(2, rows.size());
        assertTrue(rows.get(0).hasKey("item"));
        assertTrue(rows.get(1).hasKey("fluid"));
        for (NBTTagCompound row : rows) {
            assertEquals("34359738352", row.getString("amount"));
            assertTrue(WirelessRecipeDisplay.amount(row, false).startsWith("34,359,738,352"));
            assertTrue(WirelessRecipeDisplay.rate(row, false).startsWith("34,359,738,352"));
            assertFalse(WirelessRecipeDisplay.rate(row, true).contains("-"));
        }
    }

    @Test
    public void skipsEmptyOutputsAndSharesTheLimitAcrossAppends() {
        WirelessOutputs outputs = new WirelessOutputs(
            new ItemStack[] { null, new ItemStack(item, 0), new ItemStack(item, 3) },
            new FluidStack[] { null, new FluidStack(FluidRegistry.WATER, 0), new FluidStack(FluidRegistry.LAVA, 100) });
        List<NBTTagCompound> rows = new ArrayList<>();
        WirelessRecipeDisplay.appendRows(rows, outputs, 20, 1);
        WirelessRecipeDisplay.appendRows(rows, outputs, 0, 3);
        WirelessRecipeDisplay.appendRows(rows, outputs, 40, 3);

        assertEquals(3, rows.size());
        assertTrue(rows.get(0).hasKey("item"));
        assertEquals("3", rows.get(0).getString("amount"));
        assertEquals(20, rows.get(0).getInteger("ticks"));
        assertTrue(rows.get(1).hasKey("item"));
        assertTrue(rows.get(2).hasKey("fluid"));
        assertFalse(rows.get(2).hasKey("item"));
        assertEquals("100", rows.get(2).getString("amount"));
        assertEquals("0 L/s", WirelessRecipeDisplay.rate(rows.get(2), false));
        assertEquals(BigInteger.valueOf(3), outputs.itemAmounts[2]);
        assertEquals(BigInteger.valueOf(100), outputs.fluidAmounts[2]);
    }

    @Test
    public void snapshotsDoNotShareNestedNbtWithLiveOutputsOrOtherSnapshots() {
        FluidStack fluid = new FluidStack(FluidRegistry.WATER, 1000);
        fluid.tag = tag("original");
        WirelessOutputs outputs = new WirelessOutputs(new ItemStack[] { stack("original") },
            new FluidStack[] { fluid });
        List<NBTTagCompound> first = new ArrayList<>();
        List<NBTTagCompound> second = new ArrayList<>();
        WirelessRecipeDisplay.appendRows(first, outputs, 20, 10);
        WirelessRecipeDisplay.appendRows(second, outputs, 20, 10);

        first.get(0).getCompoundTag("item").getCompoundTag("tag").setString("variant", "snapshot");
        first.get(1).getCompoundTag("fluid").getCompoundTag("Tag").setString("variant", "snapshot");
        assertEquals("original", outputs.items[0].getTagCompound().getString("variant"));
        assertEquals("original", outputs.fluids[0].tag.getString("variant"));
        outputs.items[0].getTagCompound().setString("variant", "live");
        outputs.fluids[0].tag.setString("variant", "live");

        assertEquals("original", second.get(0).getCompoundTag("item").getCompoundTag("tag").getString("variant"));
        assertEquals("original", second.get(1).getCompoundTag("fluid").getCompoundTag("Tag").getString("variant"));
    }

    private static WirelessWork work(int amount, int duration) {
        return new WirelessWork(new UUID(0, 1), "display", BigInteger.ZERO, duration, 1,
            new ItemStack[] { new ItemStack(item, amount) }, null);
    }

    private static ItemStack stack(String variant) {
        ItemStack stack = new ItemStack(item, 64, 7);
        stack.setTagCompound(tag(variant));
        return stack;
    }

    private static NBTTagCompound tag(String variant) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("variant", variant);
        return tag;
    }
}
