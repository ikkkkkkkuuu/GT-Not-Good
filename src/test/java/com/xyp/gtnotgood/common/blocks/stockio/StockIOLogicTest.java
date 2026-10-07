package com.xyp.gtnotgood.common.blocks.stockio;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.BeforeClass;
import org.junit.Test;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import appeng.util.item.FluidList;
import appeng.util.item.ItemList;

/** Runs inside the opt-in native client so AE's optional interfaces and ASM compatibility are transformed normally. */
public class StockIOLogicTest {

    private static final Item INPUT = new Item();
    private static final Item PRODUCT = new Item();
    private static final Fluid FLUID = new Fluid("stock_io_test_fluid");

    @BeforeClass
    public static void registerSamples() throws Exception {
        assumeTrue(
            "Run with scripts/stockio-qa.init.gradle in the initialized native client",
            Boolean.getBoolean("gtng.stockio.qa"));
        Method register = Item.itemRegistry.getClass()
            .getDeclaredMethod("addObjectRaw", int.class, String.class, Object.class);
        register.setAccessible(true);
        int inputId = 32000;
        while (Item.itemRegistry.getObjectById(inputId) != null) inputId++;
        register.invoke(Item.itemRegistry, inputId, "test:stock_io_input", INPUT);
        int productId = inputId + 1;
        while (Item.itemRegistry.getObjectById(productId) != null) productId++;
        register.invoke(Item.itemRegistry, productId, "test:stock_io_product", PRODUCT);
        FluidRegistry.registerFluid(FLUID);
    }

    @Test
    public void nativeItemAndFluidInputsAreConsumedOnlyAfterRecipeAcceptance() {
        Host host = configuredHost();
        StockIOSnapshot snapshot = host.logic.startRecipe();
        assertNotNull(snapshot);
        assertEquals(128, snapshot.items[0].stackSize);
        assertEquals(5000, snapshot.fluids[0].amount);
        snapshot.items[0].stackSize -= 7;
        snapshot.fluids[0].amount -= 750;
        assertEquals(128, host.items.amount(item()));
        assertEquals(5000, host.fluids.amount(fluid()));
        assertTrue(host.logic.endRecipe(snapshot));
        assertEquals(121, host.items.amount(item()));
        assertEquals(4250, host.fluids.amount(fluid()));
        assertFalse(host.logic.endRecipe(snapshot));
        snapshot = host.logic.startRecipe();
        assertNotNull(snapshot);
        host.logic.abortRecipe(snapshot);
    }

    @Test
    public void machineEffectRunsOnceOnlyAfterCompleteFuelDebit() {
        Host host = configuredHost();
        StockIOSnapshot snapshot = host.logic.startRecipe();
        snapshot.fluids[0].amount -= 1000;
        int[] effects = { 0 };
        assertTrue(host.logic.endRecipe(snapshot, () -> {
            assertEquals(4000, host.fluids.amount(fluid()));
            effects[0]++;
            return true;
        }));
        assertEquals(1, effects[0]);
        assertEquals(128, host.items.amount(item()));
        assertFalse(host.logic.endRecipe(snapshot, () -> {
            effects[0]++;
            return true;
        }));
        assertEquals(1, effects[0]);
    }

    @Test
    public void shortFuelExtractionNeverGrantsMachineEffects() {
        Host host = configuredHost();
        StockIOSnapshot snapshot = host.logic.startRecipe();
        snapshot.fluids[0].amount -= 1000;
        host.fluids.extractionLimit = 9;
        int[] effects = { 0 };
        assertFalse(host.logic.endRecipe(snapshot, () -> {
            effects[0]++;
            return true;
        }));
        assertEquals(0, effects[0]);
        assertEquals(5000, host.fluids.amount(fluid()));
        assertFalse(host.logic.hasPending());
    }

    @Test
    public void rejectedMachineEffectsRefundFuelOrPreserveDurableEscrow() {
        Host host = configuredHost();
        StockIOSnapshot snapshot = host.logic.startRecipe();
        snapshot.fluids[0].amount -= 1000;
        assertFalse(host.logic.endRecipe(snapshot, () -> false));
        assertEquals(5000, host.fluids.amount(fluid()));
        assertFalse(host.logic.hasPending());

        host.fluids.acceptInsert = false;
        snapshot = host.logic.startRecipe();
        snapshot.fluids[0].amount -= 1000;
        assertFalse(host.logic.endRecipe(snapshot, () -> false));
        assertEquals(4000, host.fluids.amount(fluid()));
        assertTrue(host.logic.hasPending());
        assertNull(host.logic.startRecipe());
        NBTTagCompound contents = new NBTTagCompound();
        host.logic.writeContents(contents);
        Host restored = new Host();
        restored.fluids.set(fluid(), 4000);
        restored.logic.readContents(contents);
        assertTrue(restored.logic.hasPending());
        restored.logic.tick();
        assertFalse(restored.logic.hasPending());
        assertEquals(5000, restored.fluids.amount(fluid()));
    }

    @Test
    public void reserveAndFixedAvailabilityRecheckAtCommit() {
        Host host = configuredHost();
        host.logic.getPolicy(false, 0).reserve = 80;
        host.logic.getPolicy(false, 0).batch = 40;
        host.logic.setLimitedMode(true);
        host.logic.setFixedMode(true);
        StockIOSnapshot snapshot = host.logic.startRecipe();
        assertEquals(40, snapshot.items[0].stackSize);
        snapshot.items[0].stackSize -= 1;
        host.items.set(item(), 119);
        assertFalse(host.logic.endRecipe(snapshot));
        assertEquals(119, host.items.amount(item()));
        snapshot = host.logic.startRecipe();
        assertNull(snapshot.items[0]);
        host.logic.abortRecipe(snapshot);
        host.logic.setFixedMode(false);
        snapshot = host.logic.startRecipe();
        assertEquals(39, snapshot.items[0].stackSize);
        host.logic.abortRecipe(snapshot);
        assertEquals(0, new StockIOPolicy(false).offered(Long.MIN_VALUE, true, false));
    }

    @Test
    public void shortFluidCommitRefundsEveryAlreadyConsumedResource() {
        Host host = configuredHost();
        StockIOSnapshot snapshot = host.logic.startRecipe();
        snapshot.items[0].stackSize -= 8;
        snapshot.fluids[0].amount -= 1000;
        host.fluids.extractionLimit = 9;
        assertFalse(host.logic.endRecipe(snapshot));
        assertEquals(128, host.items.amount(item()));
        assertEquals(5000, host.fluids.amount(fluid()));
        assertFalse(host.logic.hasPending());
        snapshot = host.logic.startRecipe();
        assertNotNull(snapshot);
        host.logic.abortRecipe(snapshot);
    }

    @Test
    public void refusedRefundsArePortableAndExcludedFromCopiedSettings() {
        Host host = configuredHost();
        StockIOSnapshot snapshot = host.logic.startRecipe();
        snapshot.items[0].stackSize -= 8;
        snapshot.fluids[0].amount -= 1000;
        host.fluids.extractionLimit = 0;
        host.items.acceptInsert = false;
        assertFalse(host.logic.endRecipe(snapshot));
        assertEquals(120, host.items.amount(item()));
        assertTrue(host.logic.hasPending());
        assertNull(host.logic.startRecipe());
        NBTTagCompound contents = new NBTTagCompound();
        host.logic.writeContents(contents);
        NBTTagCompound settings = new NBTTagCompound();
        host.logic.writeSettings(settings);
        assertFalse(settings.hasKey("stockIOEscrow"));
        assertFalse(contents.hasKey("mebridge_proxy"));
        Host restored = new Host();
        restored.items.set(item(), 120);
        restored.logic.readContents(contents);
        assertTrue(restored.logic.hasPending());
        restored.logic.tick();
        assertFalse(restored.logic.hasPending());
        assertEquals(128, restored.items.amount(item()));
        assertEquals(1, restored.logic.itemFilters[0].stackSize);
    }

    @Test
    public void cancellationAndSimulatedDirectPullLeaveMeUntouched() {
        Host host = configuredHost();
        StockIOSnapshot snapshot = host.logic.startRecipe();
        snapshot.items[0].stackSize = 0;
        host.logic.cancelRecipe(snapshot);
        assertEquals(128, host.items.amount(item()));
        assertEquals(7, host.logic.extractItem(0, 7, true).stackSize);
        assertEquals(128, host.items.amount(item()));
        assertEquals(7, host.logic.extractItem(0, 7, false).stackSize);
        assertEquals(121, host.items.amount(item()));
        assertEquals(333, host.logic.extractFluid(0, 333, true).amount);
        assertEquals(5000, host.fluids.amount(fluid()));
        host.online = false;
        assertNull(host.logic.extractItem(0, 1, false));
        assertNull(host.logic.startRecipe());
    }

    @Test
    public void recyclingProtectsMarkedInputsAndIgnoresMeReserves() {
        Host host = configuredHost();
        host.logic.getPolicy(false, 0).reserve = Long.MAX_VALUE;
        host.logic.setLimitedMode(true);
        assertEquals(0, host.logic.recyclableAmount(item().setStackSize(80)));
        assertEquals(12, host.logic.recyclableAmount(AEItemStack.create(new ItemStack(PRODUCT, 12))));
        host.logic.setFixedMode(true);
        assertEquals(0, host.logic.recyclableAmount(item().setStackSize(80)));
        host.logic.setRegulate(true);
        assertEquals(16, host.logic.recyclableAmount(item().setStackSize(80)));
        host.logic.setRecycle(false);
        assertEquals(0, host.logic.recyclableAmount(AEItemStack.create(new ItemStack(PRODUCT, 12))));
    }

    @Test
    public void automaticMarksRetainExistingPoliciesWhenStockDisappears() {
        Host host = configuredHost();
        host.logic.getPolicy(false, 0).reserve = 31;
        host.logic.getPolicy(false, 0).batch = 17;
        host.logic.setAutoPullItems(true);
        host.logic.setAutoPullFluids(true);
        host.items.set(AEItemStack.create(new ItemStack(PRODUCT)), 100);
        host.logic.tick();
        assertEquals(PRODUCT, host.logic.itemFilters[1].getItem());
        host.items.set(item(), 0);
        host.logic.policyChanged();
        host.logic.tick();
        assertEquals(INPUT, host.logic.itemFilters[0].getItem());
        assertEquals(31, host.logic.getPolicy(false, 0).reserve);
        assertEquals(17, host.logic.getPolicy(false, 0).batch);
    }

    @Test
    public void aeTickSchedulingMayStartBetweenWorldTimeMultiples() {
        Host host = configuredHost();
        host.timer = 2;
        host.logic.tick();
        assertEquals(128, host.logic.networkItems[0]);
        host.items.set(item(), 61);
        host.timer = 7;
        host.logic.tick();
        assertEquals(128, host.logic.networkItems[0]);
        host.timer = 22;
        host.logic.tick();
        assertEquals(61, host.logic.networkItems[0]);
    }

    private static Host configuredHost() {
        Host host = new Host();
        host.items.set(item(), 128);
        host.fluids.set(fluid(), 5000);
        host.logic.setItemFilter(0, new ItemStack(INPUT));
        host.logic.setFluidFilter(0, new FluidStack(FLUID, 1));
        return host;
    }

    private static IAEStack<?> item() {
        return AEItemStack.create(new ItemStack(INPUT));
    }

    private static IAEStack<?> fluid() {
        return AEFluidStack.create(new FluidStack(FLUID, 1));
    }

    private static final class Host implements StockIOLogic.Host {

        private final Store items = new Store(false);
        private final Store fluids = new Store(true);
        private final BaseActionSource source = new BaseActionSource();
        private final IEnergyGrid energy = proxy(
            IEnergyGrid.class,
            (method, args) -> method.equals("extractAEPower") ? args[0] : null);
        private final IStorageGrid storage = proxy(
            IStorageGrid.class,
            (method, args) -> method.equals("getItemInventory") ? items.monitor
                : method.equals("getFluidInventory") ? fluids.monitor : null);
        private final AENetworkProxy network = new AENetworkProxy(null, "test", null, false) {

            @Override
            public boolean isActive() {
                return online;
            }

            @Override
            public IStorageGrid getStorage() {
                return storage;
            }

            @Override
            public IEnergyGrid getEnergy() {
                return energy;
            }
        };
        private boolean online = true;
        private long timer;
        private ForgeDirection side = ForgeDirection.NORTH;
        private final StockIOLogic logic = new StockIOLogic(this);

        @Override
        public boolean isServerSide() {
            return true;
        }

        @Override
        public AENetworkProxy getProxy() {
            return network;
        }

        @Override
        public BaseActionSource getActionSource() {
            return source;
        }

        @Override
        public TileEntity getTarget() {
            return null;
        }

        @Override
        public ForgeDirection getTargetSide() {
            return side;
        }

        @Override
        public void setTargetSide(ForgeDirection value) {
            side = value;
        }

        @Override
        public void markDirty() {}

        @Override
        public long getTimer() {
            return timer;
        }
    }

    private static final class Store {

        private final boolean fluid;
        private final List<IAEStack<?>> stacks = new ArrayList<>();
        private final IMEMonitor<?> monitor = proxy(IMEMonitor.class, this::invoke);
        private long extractionLimit = Long.MAX_VALUE;
        private boolean acceptInsert = true;

        private Store(boolean fluid) {
            this.fluid = fluid;
        }

        private long amount(IAEStack<?> key) {
            for (IAEStack<?> stack : stacks) if (stack.isSameType(key)) return stack.getStackSize();
            return 0;
        }

        private void set(IAEStack<?> key, long quantity) {
            for (IAEStack<?> stack : stacks) {
                if (stack.isSameType(key)) {
                    stack.setStackSize(quantity);
                    return;
                }
            }
            stacks.add(
                key.copy()
                    .setStackSize(quantity));
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Object invoke(String method, Object[] args) {
            if (method.equals("getStorageList")) {
                IItemList result = fluid ? new FluidList() : new ItemList();
                for (IAEStack<?> stack : stacks) result.add(stack.copy());
                return result;
            }
            if (method.equals("extractItems")) {
                IAEStack<?> key = (IAEStack<?>) args[0];
                boolean modulate = args[1] == Actionable.MODULATE;
                long quantity = Math.min(key.getStackSize(), amount(key));
                if (modulate) quantity = Math.min(quantity, extractionLimit);
                if (quantity <= 0) return null;
                if (modulate) set(key, amount(key) - quantity);
                return key.copy()
                    .setStackSize(quantity);
            }
            if (method.equals("injectItems")) {
                IAEStack<?> stack = (IAEStack<?>) args[0];
                if (!acceptInsert) return stack.copy();
                if (args[1] == Actionable.MODULATE) set(stack, amount(stack) + stack.getStackSize());
                return null;
            }
            return null;
        }
    }

    @FunctionalInterface
    private interface Invocation {

        Object invoke(String method, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(
            Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] { type },
                (proxy, method, args) -> invocation.invoke(method.getName(), args)));
    }
}
