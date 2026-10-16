package com.xyp.gtnotgood.common.machines.multiblock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import gregtech.api.enums.OutputBusType;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.IOutputBus;
import gregtech.api.interfaces.IOutputBusTransaction;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.IVoidable;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTUtility;
import gregtech.api.util.VoidProtectionHelper;
import sun.misc.Unsafe;

public class LargeCropBreederOutputTest {

    private static Class<?> fixtureClass;
    private static Method prepare;
    private static Method running;
    private static Method settle;
    private static Unsafe unsafe;
    private static Field featherField;
    private static Item originalFeather;
    private Controller controller;
    private Item seed;

    @BeforeClass
    public static void loadProductionOutput() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);
        featherField = Items.class.getDeclaredField("feather");
        originalFeather = Items.feather;
        if (originalFeather == null) setFeather(new Item());
        String source = "com/xyp/gtnotgood/common/machines/multiblock/LargeCropBreeder";
        String fixture = source + "OutputFixture";
        ClassNode node = new ClassNode();
        try (InputStream bytes = LargeCropBreederOutputTest.class.getClassLoader()
            .getResourceAsStream(source + ".class")) {
            assertNotNull(bytes);
            new ClassReader(bytes).accept(node, 0);
        }
        String originalSuper = node.superName;
        node.superName = Type.getInternalName(Controller.class);
        node.signature = null;
        node.interfaces.clear();
        List<String> methods = Arrays.asList("prepareSeedOutput", "prepareArchiveCycle", "settleActiveOperation",
            "onRunningTick", "supportsVoidProtection", "protectsExcessItem", "activeSeedStats", "markDisplayDirty",
            "markBaseTileDirty", "invalidateBreedingPlan");
        // Keep the production methods while bypassing the GT superclass's Forge bootstrap.
        node.methods.removeIf(method -> !methods.contains(method.name));
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, node.superName, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        node.methods.add(constructor);
        ClassWriter writer = new ClassWriter(0);
        Map<String, String> names = new HashMap<>();
        names.put(source, fixture);
        names.put(originalSuper, node.superName);
        node.accept(new ClassRemapper(writer, new SimpleRemapper(names)));
        byte[] bytecode = writer.toByteArray();
        fixtureClass = new ClassLoader(LargeCropBreederOutputTest.class.getClassLoader()) {

            Class<?> defineFixture() {
                return defineClass(fixture.replace('/', '.'), bytecode, 0, bytecode.length);
            }
        }.defineFixture();
        prepare = fixtureClass.getDeclaredMethod("prepareSeedOutput", ItemStack.class);
        prepare.setAccessible(true);
        running = fixtureClass.getMethod("onRunningTick", ItemStack.class);
        settle = fixtureClass.getDeclaredMethod("settleActiveOperation");
        settle.setAccessible(true);
    }

    @AfterClass
    public static void restoreVanillaItem() {
        if (featherField != null && originalFeather == null) setFeather(null);
    }

    private static void setFeather(Item item) {
        unsafe.putObject(unsafe.staticFieldBase(featherField), unsafe.staticFieldOffset(featherField), item);
    }

    @Before
    public void createController() throws Exception {
        controller = (Controller) fixtureClass.getConstructor().newInstance();
        seed = new Item();
        set("pendingSeedOutputs", 200);
    }

    @Test
    public void convertsOneStackInTwentyTicksAndSettlesExactlyOnce() throws Exception {
        assertTrue(controller.protectsExcessItem());
        assertSame(CheckRecipeResultRegistry.SUCCESSFUL, prepare(new ItemStack(seed)));
        assertEquals(64, controller.mOutputItems[0].stackSize);
        assertEquals(20, controller.mMaxProgresstime);
        assertEquals(0, controller.lEUt);
        assertEquals(200, getPending());
        controller.mProgresstime = 18;
        assertTrue(runTick());
        assertEquals(200, getPending());
        controller.mProgresstime = 19;
        assertTrue(runTick());
        assertEquals(136, getPending());
        assertTrue(runTick());
        assertEquals(136, getPending());
    }

    @Test
    public void partialQueueAndItemStackLimitBoundTheBatch() throws Exception {
        set("pendingSeedOutputs", 7);
        assertTrue(prepare(new ItemStack(seed)).wasSuccessful());
        assertEquals(7, controller.mOutputItems[0].stackSize);
        settle.invoke(controller);
        assertEquals(0, getPending());
        set("pendingSeedOutputs", 200);
        assertTrue(prepare(new ItemStack(new Item().setMaxStackSize(16))).wasSuccessful());
        assertEquals(16, controller.mOutputItems[0].stackSize);
    }

    @Test
    public void outputCapacityAllowsPartialBatchesAndRejectsFullBusses() throws Exception {
        controller.bus.occupied = 58;
        assertTrue(prepare(new ItemStack(seed)).wasSuccessful());
        assertEquals(6, controller.mOutputItems[0].stackSize);
        assertEquals(58, controller.bus.occupied);
        settle.invoke(controller);
        assertEquals(194, getPending());
        controller.mOutputItems = null;
        controller.bus.occupied = 64;
        assertSame(CheckRecipeResultRegistry.ITEM_OUTPUT_FULL, prepare(new ItemStack(seed)));
        assertNull(controller.mOutputItems);
        assertEquals(194, getPending());
    }

    @Test
    public void outputFilledDuringTheCycleWaitsWithoutConsumingRequests() throws Exception {
        assertTrue(prepare(new ItemStack(seed)).wasSuccessful());
        controller.mProgresstime = 19;
        controller.bus.occupied = 1;
        assertFalse(runTick());
        assertEquals(200, getPending());
        assertEquals(64, controller.mOutputItems[0].stackSize);
        controller.bus.occupied = 0;
        assertTrue(runTick());
        assertEquals(136, getPending());
        assertTrue(runTick());
        assertEquals(136, getPending());
    }

    @Test
    public void restoredLegacyAndBatchOutputsDebitTheirStoredQuantity() throws Exception {
        for (int count : new int[] { 1, 32, 64 }) {
            set("pendingSeedOutputs", 100);
            set("activeOperation", (byte) 2);
            controller.mOutputItems = new ItemStack[] { new ItemStack(seed, count) };
            settle.invoke(controller);
            settle.invoke(controller);
            assertEquals(100 - count, getPending());
        }
    }

    private CheckRecipeResult prepare(ItemStack output) throws Exception {
        return (CheckRecipeResult) prepare.invoke(controller, output);
    }

    private boolean runTick() throws Exception {
        return (boolean) running.invoke(controller, new Object[] { null });
    }

    private int getPending() throws Exception {
        Field field = fixtureClass.getDeclaredField("pendingSeedOutputs");
        field.setAccessible(true);
        return field.getInt(controller);
    }

    private void set(String name, Object value) throws Exception {
        Field field = fixtureClass.getDeclaredField(name);
        field.setAccessible(true);
        field.set(controller, value);
    }

    public static class Controller implements IVoidable {

        public ItemStack[] mOutputItems;
        public int mProgresstime, mMaxProgresstime, mEfficiency, mEfficiencyIncrease;
        public long lEUt;
        private final OutputBus bus = new OutputBus();

        public boolean onRunningTick(ItemStack stack) {
            return true;
        }

        public boolean canOutputAll(ItemStack[] outputs) {
            return new VoidProtectionHelper().setMachine(this).setItemOutputs(outputs).build().getMaxParallel() == 1;
        }

        public IGregTechTileEntity getBaseMetaTileEntity() {
            return null;
        }

        @Override
        public boolean supportsVoidProtection() {
            return false;
        }

        @Override
        public VoidingMode getVoidingMode() {
            return VoidingMode.VOID_ALL;
        }

        @Override
        public void setVoidingMode(VoidingMode mode) {}

        @Override
        public List<IOutputBus> getOutputBusses() {
            return Collections.singletonList(bus);
        }

        @Override
        public List<IOutputHatch> getOutputHatches() {
            return Collections.emptyList();
        }

        @Override
        public boolean canDumpItemToME(List<GTUtility.ItemId> outputs) {
            return false;
        }

        @Override
        public boolean canDumpFluidToME(List<GTUtility.FluidId> outputs) {
            return false;
        }
    }

    private static class OutputBus implements IOutputBus {

        private int occupied;

        @Override
        public boolean isFiltered() {
            return false;
        }

        @Override
        public boolean isFilteredToItem(GTUtility.ItemId id) {
            return true;
        }

        @Override
        public OutputBusType getBusType() {
            return OutputBusType.StandardUnfiltered;
        }

        @Override
        public boolean storePartial(ItemStack stack, boolean simulate) {
            throw new AssertionError("Recipe checks must use a transaction");
        }

        @Override
        public IOutputBusTransaction createTransaction() {
            return new IOutputBusTransaction() {

                private int space = 64 - occupied;

                @Override
                public IOutputBus getBus() {
                    return OutputBus.this;
                }

                @Override
                public boolean hasAvailableSpace() {
                    return space > 0;
                }

                @Override
                public boolean storePartial(GTUtility.ItemId id, ItemStack stack, long totalPerParallel,
                    long perParallel) {
                    int inserted = Math.min(space, stack.stackSize);
                    space -= inserted;
                    stack.stackSize -= inserted;
                    return inserted > 0;
                }

                @Override
                public void complete(GTUtility.ItemId id) {}

                @Override
                public void commit() {
                    throw new AssertionError("Recipe checks must not commit output");
                }
            };
        }
    }
}
