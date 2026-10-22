package com.xyp.gtnotgood.common.machines.multiblock.modularmachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.AfterClass;
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

import com.xyp.gtnotgood.common.machines.processing.ModuleWork;
import com.xyp.gtnotgood.common.recipe.machine.DimensionalFocusResearch;
import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;
import com.xyp.gtnotgood.utils.HeadlessItemRegistry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.GTMod;
import gregtech.api.interfaces.tileentity.IVoidable;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.SingleRecipeCheck;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import gregtech.common.GTProxy;
import sun.misc.Unsafe;

public class DimensionalFocusProcessingTest {

    private static final String SOURCE = "com/xyp/gtnotgood/common/machines/multiblock/modularmachine/DimensionalFocusEngravingArray";
    private static final String FIXTURE = SOURCE + "ProcessingFixture";
    private static Class<?> fixtureClass;
    private static Item input;
    private static Item output;
    private static File originalHome;
    private static Side originalSide;
    private static GTProxy originalProxy;

    @BeforeClass
    public static void loadProductionLogicWithoutForgeMachineBootstrap() throws Exception {
        Unsafe unsafe = (Unsafe) field(Unsafe.class, "theUnsafe").get(null);
        if (field(Loader.class, "instance").get(null) == null)
            field(Loader.class, "instance").set(null, unsafe.allocateInstance(Loader.class));
        if (field(Loader.class, "namedMods").get(Loader.instance()) == null)
            field(Loader.class, "namedMods").set(Loader.instance(), new HashMap<>());
        originalHome = (File) field(FMLInjectionData.class, "minecraftHome").get(null);
        if (originalHome == null) {
            File home = new File("build/test-runtime/dimensional-focus").getAbsoluteFile();
            new File(home, "config").mkdirs();
            field(FMLInjectionData.class, "minecraftHome").set(null, home);
        }
        originalSide = (Side) field(FMLRelaunchLog.class, "side").get(null);
        if (originalSide == null) field(FMLRelaunchLog.class, "side").set(null, Side.SERVER);
        HeadlessBlockRegistry.bootstrap();
        HeadlessItemRegistry.bootstrap();
        originalProxy = GTMod.proxy;
        if (originalProxy == null) GTMod.proxy = (GTProxy) unsafe.allocateInstance(GTProxy.class);
        input = registerItem(31240, "test:dimensional_focus_input");
        output = registerItem(31241, "test:dimensional_focus_output");
        String debug = System.getProperty("gt.recipebuilder.debug");
        System.setProperty("gt.recipebuilder.debug", "false");
        try {
            Class.forName(GTRecipeBuilder.class.getName(), true, GTRecipeBuilder.class.getClassLoader());
        } finally {
            if (debug == null) System.clearProperty("gt.recipebuilder.debug");
            else System.setProperty("gt.recipebuilder.debug", debug);
        }

        Map<String, String> remap = new HashMap<>();
        remap.put(SOURCE, FIXTURE);
        remap.put(SOURCE + "$1", FIXTURE + "$1");
        remap.put("com/xyp/gtnotgood/common/machines/multiblock/multiMachineBase/GTNGMultiBlockBase",
            Type.getInternalName(Controller.class));
        remap.put("com/xyp/gtnotgood/common/machines/multiblock/multiMachineBase/GTNGModularMachineBase",
            Type.getInternalName(Controller.class));
        remap.put("gregtech/api/metatileentity/implementations/MTEMultiBlockBase",
            Type.getInternalName(Controller.class));
        ClassNode outer = readClass(SOURCE);
        outer.superName = Type.getInternalName(Controller.class);
        outer.signature = null;
        outer.interfaces.clear();
        outer.fields.removeIf(f -> !f.name.equals("requiredComputation") && !f.name.equals("researchHatches"));
        outer.methods.removeIf(m -> !Arrays.asList("createMachineProcessingLogic", "parallelForHeat", "onRunningTick",
            "saveNBTData", "loadNBTData", "getRequiredComputation").contains(m.name) && !m.name.startsWith("access$"));
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, outer.superName, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        outer.methods.add(constructor);
        FixtureLoader loader = new FixtureLoader();
        fixtureClass = loader.define(outer, remap);
        loader.define(readClass(SOURCE + "$1"), remap);
    }

    @AfterClass
    public static void restoreEnvironment() throws Exception {
        GTMod.proxy = originalProxy;
        field(FMLInjectionData.class, "minecraftHome").set(null, originalHome);
        field(FMLRelaunchLog.class, "side").set(null, originalSide);
    }

    @Test
    public void subtickOverclockCannotExceedAvailableComputation() throws Exception {
        Controller machine = machine(30);
        ProcessingLogic logic = logic(machine);
        ItemStack stock = new ItemStack(input, 100);
        logic.setInputItems(stock).setAvailableVoltage(8_388_608).setAvailableAmperage(1).setMaxParallel(4);
        GTRecipe recipe = GTRecipeBuilder.builder().itemInputs(new ItemStack(input, 1))
            .itemOutputs(new ItemStack(output, 1)).duration(1).eut(32)
            .metadata(DimensionalFocusResearch.COMPUTATION, 10L).build().get();
        Method create = logic.getClass().getDeclaredMethod("createParallelHelper", GTRecipe.class);
        create.setAccessible(true);
        ParallelHelper helper = (ParallelHelper) create.invoke(logic, recipe);
        IVoidable voidable = (IVoidable) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { IVoidable.class }, (proxy, method, args) -> switch (method.getName()) {
            case "getItemOutputLimit", "getFluidOutputLimit" -> 1;
            default -> throw new AssertionError("Unexpected output access: " + method.getName());
            });
        helper.setMachine(voidable, false, false).setCalculator(new OverclockCalculator().setRecipeEUt(32)
            .setDuration(1).setEUt(8_388_608).setAmperage(1).enablePerfectOC()).build();
        assertTrue(helper.getResult().wasSuccessful());
        assertEquals(3, helper.getCurrentParallel());
        assertEquals(97, stock.stackSize);
        assertEquals(3, helper.getItemOutputs()[0].stackSize);
    }

    @Test
    public void computationLossGatesTheRunningTickBeforePowerUse() throws Exception {
        Controller machine = machine(29);
        field(fixtureClass, "requiredComputation").setLong(machine, 30);
        Method running = fixtureClass.getMethod("onRunningTick", ItemStack.class);
        assertFalse((boolean) running.invoke(machine, new Object[] { null }));
        assertEquals(0, machine.powerTicks);
        machine.computation = 30;
        assertTrue((boolean) running.invoke(machine, new Object[] { null }));
        assertEquals(1, machine.powerTicks);
    }

    @Test
    public void researchComputationSurvivesReloadAndRejectsNegativeSavedValues() throws Exception {
        Controller machine = machine(0);
        field(fixtureClass, "requiredComputation").setLong(machine, 3_000_000_000L);
        NBTTagCompound saved = new NBTTagCompound();
        fixtureClass.getMethod("saveNBTData", NBTTagCompound.class).invoke(machine, saved);
        Controller restored = machine(0);
        restored.batchMode = true;
        fixtureClass.getMethod("loadNBTData", NBTTagCompound.class).invoke(restored, saved);
        assertEquals(3_000_000_000L, field(fixtureClass, "requiredComputation").getLong(restored));
        assertFalse(restored.batchMode);
        saved.setLong("focusRequiredComputation", -1);
        fixtureClass.getMethod("loadNBTData", NBTTagCompound.class).invoke(restored, saved);
        assertEquals(0, field(fixtureClass, "requiredComputation").getLong(restored));
    }

    @Test
    public void coilParallelRemainsMonotonicAndSaturatesWithoutOverflow() throws Exception {
        Method parallel = fixtureClass.getMethod("parallelForHeat", long.class);
        assertEquals(1, parallel.invoke(null, Long.MIN_VALUE));
        assertEquals(1, parallel.invoke(null, 0L));
        assertEquals(4, parallel.invoke(null, 1801L));
        assertEquals(32793, parallel.invoke(null, 13501L));
        assertEquals(Integer.MAX_VALUE, parallel.invoke(null, 27_900L));
        assertEquals(Integer.MAX_VALUE, parallel.invoke(null, Long.MAX_VALUE));
    }

    private static Controller machine(long computation) throws Exception {
        Controller machine = (Controller) fixtureClass.getConstructor().newInstance();
        machine.computation = computation;
        return machine;
    }

    private static ProcessingLogic logic(Controller machine) throws Exception {
        Method method = fixtureClass.getDeclaredMethod("createMachineProcessingLogic");
        method.setAccessible(true);
        return (ProcessingLogic) method.invoke(machine);
    }

    private static ClassNode readClass(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = DimensionalFocusProcessingTest.class.getClassLoader()
            .getResourceAsStream(name + ".class")) {
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private static Item registerItem(int id, String name) throws Exception {
        Item item = new Item();
        Method register = Item.itemRegistry.getClass().getDeclaredMethod("addObjectRaw", int.class, String.class,
            Object.class);
        register.setAccessible(true);
        register.invoke(Item.itemRegistry, id, name, item);
        field(item.delegate.getClass(), "name").set(item.delegate, name);
        return item;
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class FixtureLoader extends ClassLoader {

        private FixtureLoader() {
            super(DimensionalFocusProcessingTest.class.getClassLoader());
        }

        private Class<?> define(ClassNode node, Map<String, String> names) {
            ClassWriter writer = new ClassWriter(0);
            node.accept(new ClassRemapper(writer, new SimpleRemapper(names)));
            byte[] bytes = writer.toByteArray();
            return defineClass(names.get(node.name).replace('/', '.'), bytes, 0, bytes.length);
        }
    }

    public static class Controller {

        public long computation;
        public int powerTicks;
        public int mMaxProgresstime = 1;
        public boolean batchMode;
        public boolean mLockedToSingleRecipe;

        public long getAvailableComputation() {
            return computation;
        }

        public long getAvailableModuleComputation() {
            return computation;
        }

        public long getModuleUsedComputation() {
            return 0;
        }

        public List<ModuleWork> getModuleWorks() {
            return Collections.emptyList();
        }

        public int getTrueParallel() {
            return 4;
        }

        public boolean onRunningTick(ItemStack stack) {
            powerTicks++;
            return true;
        }

        public void saveNBTData(NBTTagCompound tag) {}

        public void loadNBTData(NBTTagCompound tag) {}

        public void setSingleRecipeCheck(SingleRecipeCheck check) {}
    }
}
