package com.xyp.gtnotgood.common.blocks.stockio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import cpw.mods.fml.common.Loader;
import sun.misc.Unsafe;

/** Local rollback and ownership checks; the opt-in client QA exercises the actual GT recipe matcher. */
public class StockIORecipeBridgeTest {

    private static final Item item = new Item();
    private static final Item otherItem = new Item();
    private static final Fluid fluid = new Fluid("stock_io_local_test");

    @BeforeClass
    public static void registerFluid() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        if (loader.get(null) == null) loader.set(null, ((Unsafe) field.get(null)).allocateInstance(Loader.class));
        HeadlessBlockRegistry.bootstrap();
        FluidRegistry.registerFluid(fluid);
    }

    @Test
    public void lookupAndConsumptionShareCopiesWithLocalInputsFirst() {
        StockIOSnapshot snapshot = snapshot();
        ItemStack local = new ItemStack(item, 2);
        FluidStack localFluid = new FluidStack(fluid, 100);
        StockIORecipeBridge.LocalInputs inputs = new StockIORecipeBridge.LocalInputs(snapshot);
        ItemStack[] items = inputs.items(new ItemStack[] { local, null });
        FluidStack[] fluids = inputs.fluids(new FluidStack[] { localFluid });
        assertEquals(3, items.length);
        assertEquals(2, fluids.length);
        assertNotSame(local, items[0]);
        assertNotSame(localFluid, fluids[0]);
        assertSame(snapshot.items[899], items[2]);
        assertSame(snapshot.fluids[899], fluids[1]);
        assertSame(items, inputs.items(new ItemStack[] { local }));
        assertSame(fluids, inputs.fluids(new FluidStack[] { localFluid }));
    }

    @Test
    public void rejectedMixedRecipeLeavesRealInventoryAndTankUntouched() {
        StockIOSnapshot snapshot = snapshot();
        ItemStack local = tagged(4);
        FluidStack localFluid = new FluidStack(fluid, 100);
        StockIORecipeBridge.LocalInputs inputs = new StockIORecipeBridge.LocalInputs(snapshot);
        ItemStack[] items = inputs.items(new ItemStack[] { local });
        FluidStack[] fluids = inputs.fluids(new FluidStack[] { localFluid });
        items[0].stackSize = 1;
        items[1].stackSize = 0;
        fluids[0].amount = 0;
        fluids[1].amount = 25;
        assertTrue(inputs.valid());
        assertEquals(4, local.stackSize);
        assertEquals(100, localFluid.amount);
        assertNotSame(local.getTagCompound(), items[0].getTagCompound());
        assertEquals(8, snapshot.originalItems[899].stackSize);
        assertEquals(1000, snapshot.originalFluids[899].amount);
    }

    @Test
    public void acceptedMixedConsumptionAppliesOnlyLocalRemainingAmounts() {
        StockIOSnapshot snapshot = snapshot();
        ItemStack local = tagged(4);
        ItemStack circuit = new ItemStack(otherItem, 0, 7);
        FluidStack localFluid = new FluidStack(fluid, 100);
        StockIORecipeBridge.LocalInputs inputs = new StockIORecipeBridge.LocalInputs(snapshot);
        ItemStack[] items = inputs.items(new ItemStack[] { local, circuit });
        FluidStack[] fluids = inputs.fluids(new FluidStack[] { localFluid });
        items[0].stackSize = 0;
        items[2].stackSize = 5;
        fluids[0].amount = 0;
        fluids[1].amount = 550;
        assertTrue(inputs.valid());
        inputs.commit();
        assertEquals(0, local.stackSize);
        assertEquals(0, circuit.stackSize);
        assertEquals(7, circuit.getItemDamage());
        assertEquals(0, localFluid.amount);
        assertEquals(5, snapshot.items[899].stackSize);
        assertEquals(550, snapshot.fluids[899].amount);
        assertEquals(
            "variant",
            local.getTagCompound()
                .getString("identity"));
    }

    @Test
    public void changedIdentityNegativeQuantityAndGrowthCannotCommit() {
        StockIORecipeBridge.LocalInputs items = new StockIORecipeBridge.LocalInputs(snapshot());
        ItemStack[] stagedItems = items.items(new ItemStack[] { tagged(4) });
        stagedItems[0].stackSize = 5;
        assertFalse(items.valid());
        stagedItems[0].stackSize = -1;
        assertFalse(items.valid());
        stagedItems[0].stackSize = 3;
        stagedItems[0].getTagCompound()
            .setString("identity", "changed");
        assertFalse(items.valid());

        StockIORecipeBridge.LocalInputs fluids = new StockIORecipeBridge.LocalInputs(snapshot());
        FluidStack[] stagedFluids = fluids.fluids(new FluidStack[] { new FluidStack(fluid, 100) });
        stagedFluids[0].amount = 101;
        assertFalse(fluids.valid());
        stagedFluids[0].amount = -1;
        assertFalse(fluids.valid());
    }

    private static ItemStack tagged(int amount) {
        ItemStack stack = new ItemStack(item, amount);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setString("identity", "variant");
        return stack;
    }

    private static StockIOSnapshot snapshot() {
        StockIOSnapshot snapshot = new StockIOSnapshot(null);
        snapshot.items[899] = new ItemStack(item, 8);
        snapshot.originalItems[899] = snapshot.items[899].copy();
        snapshot.fluids[899] = new FluidStack(fluid, 1000);
        snapshot.originalFluids[899] = snapshot.fluids[899].copy();
        return snapshot;
    }
}
