package com.xyp.gtnotgood.common.mecontainer;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import cpw.mods.fml.common.Loader;
import sun.misc.Unsafe;

/** Regression tests for external automation contracts and portable resources, without requiring a running world. */
public class MEContainerTest {

    private static final Item ITEM = new Item();
    private static final Fluid FLUID = new Fluid("me_container_test_fluid");

    @BeforeClass
    public static void registerSamples() throws Exception {
        // Headless tests have no FML mod loader; seed the registry without invoking activeModContainer().
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        loader.set(null, unsafe.allocateInstance(Loader.class));
        HeadlessBlockRegistry.bootstrap();
        Method register = Item.itemRegistry.getClass()
            .getDeclaredMethod("addObjectRaw", int.class, String.class, Object.class);
        register.setAccessible(true);
        register.invoke(Item.itemRegistry, 31001, "test:me_container_item", ITEM);
        FluidRegistry.registerFluid(FLUID);
    }

    /** Replaces only world notification and side detection; all inventory/tank behavior is production code. */
    private static final class ServerTile extends TileMEContainer {

        @Override
        public boolean isServerSide() {
            return true;
        }

        @Override
        public void markDirty() {}

        long stock;
        long capacity = 10_000_000_000L;
        int itemRoom;
        int acceptedItems;

        @Override
        protected FluidStack extractNetworkFluid(FluidStack sample, int amount, boolean modulate) {
            if (sample.getFluid() != FLUID || stock <= 0) return null;
            FluidStack result = sample.copy();
            result.amount = (int) Math.min(stock, amount);
            if (modulate) stock -= result.amount;
            return result;
        }

        @Override
        protected int insertNetworkFluid(FluidStack resource, boolean modulate) {
            if (resource.getFluid() != FLUID) return 0;
            int accepted = (int) Math.min(resource.amount, Math.max(0, capacity - stock));
            if (modulate) stock += accepted;
            return accepted;
        }

        @Override
        protected ItemStack insertNetworkItem(ItemStack offered) {
            int accepted = Math.min(offered.stackSize, itemRoom);
            acceptedItems += accepted;
            itemRoom -= accepted;
            offered.stackSize -= accepted;
            return offered.stackSize == 0 ? null : offered;
        }
    }

    @Test
    public void failedHopperInsertionRestoresLastItem() {
        TileMEContainer tile = new ServerTile();
        tile.setInventorySlotContents(0, new ItemStack(ITEM));
        ItemStack before = tile.getStackInSlot(0)
            .copy();
        assertEquals(1, tile.decrStackSize(0, 1).stackSize);
        assertNull(tile.getStackInSlot(0));
        tile.setInventorySlotContents(0, before);
        assertEquals(1, tile.getStackInSlot(0).stackSize);
        assertFalse(tile.canInsertItem(0, before, 0));
    }

    @Test
    public void extractionIsBoundedAndWorksFromEverySide() {
        TileMEContainer tile = new ServerTile();
        tile.setInventorySlotContents(0, new ItemStack(ITEM, 12));
        for (int side = 0; side < 6; side++) {
            assertTrue(tile.canExtractItem(0, tile.getStackInSlot(0), side));
            assertEquals(72, tile.getAccessibleSlotsFromSide(side).length);
        }
        assertNull(tile.decrStackSize(0, 0));
        assertNull(tile.decrStackSize(1, 12));
        assertEquals(12, tile.decrStackSize(0, Integer.MAX_VALUE).stackSize);
        assertNull(tile.decrStackSize(0, 1));
    }

    private static TileMEContainer fluidTile() {
        TileMEContainer tile = new ServerTile();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("fluid", new FluidStack(FLUID, 9000).writeToNBT(new NBTTagCompound()));
        tile.readContents(tag);
        return tile;
    }

    @Test
    public void simulatedDrainAndTankInspectionDoNotConsumeFluid() {
        TileMEContainer tile = fluidTile();
        assertEquals(1000, tile.drain(ForgeDirection.UP, 1000, false).amount);
        tile.getTankInfo(ForgeDirection.UP)[36].fluid.amount = 1;
        assertEquals(9000, tile.bufferedFluid().amount);
        assertEquals(9000, tile.drain(ForgeDirection.DOWN, Integer.MAX_VALUE, true).amount);
        assertNull(tile.drain(ForgeDirection.DOWN, 1, true));
    }

    @Test
    public void wrongFluidAndInsertionCannotAlterBuffer() {
        TileMEContainer tile = fluidTile();
        assertNull(tile.drain(ForgeDirection.NORTH, new FluidStack(FluidRegistry.WATER, 1000), true));
        assertEquals(0, tile.fill(ForgeDirection.NORTH, new FluidStack(FluidRegistry.WATER, 1000), true));
        assertEquals(9000, tile.bufferedFluid().amount);
    }

    @Test
    public void portableContentsPreserveNbtAndExcludeNetworkIdentity() {
        TileMEContainer tile = fluidTile();
        ItemStack stack = new ItemStack(ITEM, 37);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setString("variant", "specific");
        tile.setInventorySlotContents(0, stack);
        tile.itemFilters[0] = stack.copy();
        tile.fluidFilters[0] = new FluidStack(FLUID, 1);
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeContents(tag);
        assertFalse(tag.hasKey("mebridge_proxy"));
        assertFalse(tag.hasKey("mebridge_owner"));
        TileMEContainer restored = new ServerTile();
        restored.readContents(tag);
        assertEquals(37, restored.getStackInSlot(0).stackSize);
        assertEquals(9000, restored.bufferedFluid().amount);
        assertEquals(1, restored.itemFilters[0].stackSize);
        assertEquals(
            "specific",
            restored.itemFilters[0].getTagCompound()
                .getString("variant"));
        assertTrue(restored.fluidFilters[0].isFluidEqual(tile.fluidFilters[0]));
        assertFalse(TileMEContainer.sameItem(restored.itemFilters[0], new ItemStack(ITEM)));
    }

    @Test
    public void changingOrClearingFiltersKeepsActualContents() {
        TileMEContainer tile = fluidTile();
        tile.setInventorySlotContents(0, new ItemStack(ITEM, 4));
        tile.itemFilters[0] = null;
        tile.fluidFilters[0] = new FluidStack(FluidRegistry.WATER, 1);
        assertEquals(4, tile.decrStackSize(0, 64).stackSize);
        assertEquals(9000, tile.drain(ForgeDirection.WEST, 16000, true).amount);
    }

    @Test
    public void liveFluidTransfersExceedOldCapacityAndKeepLongStock() {
        ServerTile tile = new ServerTile();
        tile.setFluidFilter(35, new FluidStack(FLUID, 1));
        tile.stock = 5_000_000_000L;
        assertEquals(36, tile.getTankInfo(ForgeDirection.UP).length);
        assertEquals(Integer.MAX_VALUE, tile.getTankInfo(ForgeDirection.UP)[35].fluid.amount);
        assertEquals(5_000_000_000L, tile.stock);
        assertEquals(2_000_000, tile.drain(ForgeDirection.UP, 2_000_000, false).amount);
        assertEquals(5_000_000_000L, tile.stock);
        assertEquals(Integer.MAX_VALUE, tile.drain(ForgeDirection.UP, Integer.MAX_VALUE, true).amount);
        assertEquals(5_000_000_000L - Integer.MAX_VALUE, tile.stock);
    }

    @Test
    public void fluidInputNeedsNoFilterAndReturnsOnlyAcceptedAmount() {
        ServerTile tile = new ServerTile();
        FluidStack offered = new FluidStack(FLUID, 1_000_000);
        assertEquals(1_000_000, tile.fill(ForgeDirection.WEST, offered, false));
        assertEquals(0, tile.stock);
        assertEquals(1_000_000, tile.fill(ForgeDirection.WEST, offered, true));
        assertEquals(1_000_000, offered.amount);
        tile.capacity = 1_001_234;
        assertEquals(1234, tile.fill(ForgeDirection.WEST, offered, true));
        assertEquals(0, tile.fill(ForgeDirection.WEST, offered, true));
        assertEquals(1_001_234, tile.stock);
    }

    @Test
    public void inputItemsRemainSafeWhenNetworkIsFullThenFlushPartially() throws Exception {
        ServerTile tile = new ServerTile();
        assertTrue(tile.canInsertItem(71, new ItemStack(ITEM), 0));
        assertFalse(tile.canExtractItem(71, new ItemStack(ITEM), 0));
        tile.setInventorySlotContents(71, new ItemStack(ITEM, 64));
        tile.flushInputs();
        assertEquals(64, tile.getStackInSlot(71).stackSize);
        tile.itemRoom = 20;
        tile.flushInputs();
        assertEquals(44, tile.getStackInSlot(71).stackSize);
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeContents(tag);
        ServerTile restored = new ServerTile();
        restored.readContents(tag);
        assertEquals(44, restored.getStackInSlot(71).stackSize);
        restored.itemRoom = 64;
        restored.flushInputs();
        assertEquals(44, restored.acceptedItems);
        assertNull(restored.getStackInSlot(71));
    }

    @Test
    public void allThirtySixSamplesSurviveSaveLoadWithoutBecomingRealItems() {
        ServerTile tile = new ServerTile();
        for (int i = 0; i < 36; i++) {
            ItemStack item = new ItemStack(ITEM);
            item.setTagCompound(new NBTTagCompound());
            item.getTagCompound()
                .setInteger("variant", i);
            FluidStack fluid = new FluidStack(FLUID, 1);
            fluid.tag = new NBTTagCompound();
            fluid.tag.setInteger("variant", i);
            tile.setItemFilter(i, item);
            tile.setFluidFilter(i, fluid);
        }
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeContents(tag);
        ServerTile restored = new ServerTile();
        restored.readContents(tag);
        for (int i = 0; i < 36; i++) {
            assertEquals(
                i,
                restored.itemFilters[i].getTagCompound()
                    .getInteger("variant"));
            assertEquals(i, restored.fluidFilters[i].tag.getInteger("variant"));
            assertNull(restored.getStackInSlot(i));
        }
    }

    @Test
    public void duplicateFluidSampleMovesInsteadOfMultiplyingReportedStock() {
        ServerTile tile = new ServerTile();
        tile.setFluidFilter(0, new FluidStack(FLUID, 1));
        tile.setFluidFilter(35, new FluidStack(FLUID, 1));
        assertNull(tile.fluidFilters[0]);
        assertNotNull(tile.fluidFilters[35]);
    }
}
