package com.xyp.gtnotgood.common.machines.hatch.me;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.inv.MEInventoryCrafting;
import cpw.mods.fml.common.Loader;
import sun.misc.Unsafe;

public class LongCircuitBufferTest {

    private static final Item MATERIAL = new Item();
    private static final ItemStack[] NO_MOLDS = new ItemStack[0];

    @BeforeClass
    public static void initializeFluids() throws ReflectiveOperationException {
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        Object previousLoader = loader.get(null);
        Object temporaryLoader = null;
        if (previousLoader == null) {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            temporaryLoader = ((Unsafe) unsafeField.get(null)).allocateInstance(Loader.class);
            loader.set(null, temporaryLoader);
        }
        try {
            HeadlessBlockRegistry.bootstrap();
            Class.forName(FluidRegistry.class.getName(), true, FluidRegistry.class.getClassLoader());
        } finally {
            if (temporaryLoader != null && loader.get(null) == temporaryLoader) loader.set(null, previousLoader);
        }
    }

    @Test
    public void longDebitsRefillTheSameItemAndFluidViews() {
        Fixture fixture = new Fixture();
        long amount = (long) Integer.MAX_VALUE + 32;
        assertTrue(fixture.buffer.insert(table(item(0, amount), fluid(FluidRegistry.WATER, amount)), NO_MOLDS));
        ItemStack itemView = fixture.items.get(0);
        FluidStack fluidView = fixture.fluids.get(0);
        itemView.stackSize -= 11;
        fluidView.amount -= 17;

        assertSame(fixture.items, fixture.buffer.itemViews());
        assertSame(fixture.fluids, fixture.buffer.fluidViews());
        assertSame(itemView, fixture.items.get(0));
        assertSame(fluidView, fixture.fluids.get(0));
        assertEquals(Integer.MAX_VALUE, itemView.stackSize);
        assertEquals(Integer.MAX_VALUE, fluidView.amount);
        assertEquals(amount - 11, fixture.buffer.storedItems().get(0).getStackSize());
        assertEquals(amount - 17, fixture.buffer.storedFluids().get(0).getStackSize());
        int changes = fixture.changes;
        for (int i = 0; i < 100; i++) {
            assertSame(fixture.items, fixture.buffer.itemViews());
            assertSame(fixture.fluids, fixture.buffer.fluidViews());
        }
        assertEquals(changes, fixture.changes);
    }

    @Test
    public void consumedTypesAreRemovedFromBothListsAndTheirCapacityIsReusable() {
        Fixture fixture = new Fixture();
        IAEStack<?>[] types = new IAEStack<?>[64];
        for (int i = 0; i < types.length; i++) types[i] = item(i, 1);
        assertTrue(fixture.buffer.insert(table(types), NO_MOLDS));
        for (ItemStack view : fixture.items) view.stackSize = 0;
        assertTrue(fixture.buffer.itemViews().isEmpty());
        assertTrue(fixture.buffer.storedItems().isEmpty());
        assertTrue(fixture.buffer.insert(table(item(64, 2), fluid(FluidRegistry.WATER, 3)), NO_MOLDS));
        assertEquals(64, fixture.items.get(0).getItemDamage());

        fixture.buffer.clear();
        assertTrue(fixture.items.isEmpty());
        assertTrue(fixture.fluids.isEmpty());
        assertSame(fixture.items, fixture.buffer.itemViews());
        assertSame(fixture.fluids, fixture.buffer.fluidViews());
    }

    @Test
    public void aLateFluidOverflowRejectsAllStagedItems() {
        Fixture fixture = new Fixture();
        assertTrue(fixture.buffer.insert(table(item(0, 5), fluid(FluidRegistry.WATER, Long.MAX_VALUE)), NO_MOLDS));
        ItemStack existing = fixture.items.get(0);
        int changes = fixture.changes;

        assertFalse(fixture.buffer.insert(table(item(0, 7), item(1, 3), fluid(FluidRegistry.WATER, 1)), NO_MOLDS));
        assertEquals(changes, fixture.changes);
        assertEquals(1, fixture.items.size());
        assertSame(existing, fixture.items.get(0));
        assertEquals(5, fixture.buffer.storedItems().get(0).getStackSize());
        assertEquals(Long.MAX_VALUE, fixture.buffer.storedFluids().get(0).getStackSize());
    }

    @Test
    public void aLateTypeLimitFailureDoesNotCommitEarlierMerges() {
        Fixture fixture = new Fixture();
        IAEStack<?>[] types = new IAEStack<?>[64];
        for (int i = 0; i < types.length; i++) types[i] = item(i, 1);
        assertTrue(fixture.buffer.insert(table(types), NO_MOLDS));

        assertFalse(fixture.buffer.insert(table(item(0, 9), item(64, 1)), NO_MOLDS));
        assertEquals(64, fixture.items.size());
        assertEquals(1, fixture.buffer.storedItems().get(0).getStackSize());
    }

    @Test
    public void mergingAfterRemovalPreservesTheRemainingEntryIdentities() {
        Fixture fixture = new Fixture();
        assertTrue(fixture.buffer.insert(table(item(0, 2), item(1, 3), item(2, 4)), NO_MOLDS));
        ItemStack first = fixture.items.get(0);
        ItemStack last = fixture.items.get(2);
        fixture.items.get(1).stackSize = 0;
        assertEquals(2, fixture.buffer.itemViews().size());

        assertTrue(fixture.buffer.insert(table(item(2, 5), item(0, 7), item(3, 8)), NO_MOLDS));
        assertSame(first, fixture.items.get(0));
        assertSame(last, fixture.items.get(1));
        assertEquals(9, first.stackSize);
        assertEquals(9, last.stackSize);
        assertEquals(3, fixture.items.get(2).getItemDamage());
        assertEquals(8, fixture.items.get(2).stackSize);
    }

    private static IAEItemStack item(int variant, long amount) {
        long[] count = { amount };
        return (IAEItemStack) Proxy.newProxyInstance(IAEItemStack.class.getClassLoader(),
            new Class<?>[] { IAEItemStack.class }, (proxy, method, arguments) -> switch (method.getName()) {
            case "getStackSize" -> count[0];
            case "setStackSize" -> {
            count[0] = (long) arguments[0];
            yield proxy;
            }
            case "getItem" -> MATERIAL;
            case "getItemDamage" -> variant;
            case "getItemStack" -> new ItemStack(MATERIAL, (int) Math.min(Integer.MAX_VALUE, count[0]), variant);
            case "copy" -> item(variant, count[0]);
            case "isSameType" -> arguments[0] instanceof IAEItemStack other && other.getItem() == MATERIAL
                && other.getItemDamage() == variant;
            default -> throw new AssertionError(method.getName());
            });
    }

    private static IAEFluidStack fluid(Fluid type, long amount) {
        long[] count = { amount };
        return (IAEFluidStack) Proxy.newProxyInstance(IAEFluidStack.class.getClassLoader(),
            new Class<?>[] { IAEFluidStack.class }, (proxy, method, arguments) -> switch (method.getName()) {
            case "getStackSize" -> count[0];
            case "setStackSize" -> {
            count[0] = (long) arguments[0];
            yield proxy;
            }
            case "getFluidStack" -> new FluidStack(type, (int) Math.min(Integer.MAX_VALUE, count[0]));
            case "copy" -> fluid(type, count[0]);
            case "isSameType" -> arguments[0] instanceof IAEFluidStack other
                && other.getFluidStack().getFluid() == type;
            default -> throw new AssertionError(method.getName());
            });
    }

    private static MEInventoryCrafting table(IAEStack<?>... inputs) {
        return new MEInventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, inputs.length, 1) {

            @Override
            public IAEStack<?> getAEStackInSlot(int index) {
                return inputs[index];
            }
        };
    }

    private static final class Fixture {

        private final List<ItemStack> items = new ArrayList<>();
        private final List<FluidStack> fluids = new ArrayList<>();
        private int changes;
        private final LongCircuitBuffer buffer = new LongCircuitBuffer(null, () -> changes++, items, fluids);
    }
}
