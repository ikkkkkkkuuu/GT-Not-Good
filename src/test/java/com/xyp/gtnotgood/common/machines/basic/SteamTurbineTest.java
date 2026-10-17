package com.xyp.gtnotgood.common.machines.basic;

import static gregtech.api.enums.GTValues.V;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

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

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import cpw.mods.fml.common.Loader;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GTModHandler;
import sun.misc.Unsafe;

public class SteamTurbineTest {

    private static Class<?> fixtureClass;
    private static Fluid steam;
    private static Materials originalSteam;

    @BeforeClass
    public static void loadProductionGenerator() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        if (loader.get(null) == null) loader.set(null, ((Unsafe) unsafeField.get(null)).allocateInstance(Loader.class));
        HeadlessBlockRegistry.bootstrap();
        steam = new Fluid("steam_turbine_test_steam");
        FluidRegistry.registerFluid(steam);
        originalSteam = Materials.Steam;
        if (originalSteam == null) Materials.Steam = Materials.Iron;
        GTModHandler.sAnySteamFluidIDs.add(steam.getID());

        String source = "com/xyp/gtnotgood/common/machines/basic/SteamTurbine";
        String fixture = source + "FuelFixture";
        String generator = "gregtech/api/metatileentity/implementations/MTEBasicGenerator";
        String metaTile = "gregtech/api/metatileentity/MetaTileEntity";
        ClassNode node = readClass(source);
        node.superName = Type.getInternalName(Generator.class);
        node.signature = null;
        node.interfaces.clear();
        node.fields.clear();
        List<String> methods = Arrays.asList("getEfficiency", "consumedFluidPerOperation", "getFuelValue",
            "maxAmperesOut", "maxEUStore", "getCapacity", "getPollution");
        node.methods.removeIf(method -> !methods.contains(method.name));
        for (MethodNode method : readClass(generator).methods) {
            if (method.name.equals("onPostTick") || method.name.equals("maxEUOutput")) node.methods.add(method);
        }
        for (MethodNode method : readClass(metaTile).methods) {
            if (method.name.equals("getMinimumStoredEU")) node.methods.add(method);
        }
        // Execute the production fuel cycle without GT's Forge superclass and world pollution bootstrap.
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, node.superName, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        node.methods.add(constructor);
        Map<String, String> names = new HashMap<>();
        names.put(source, fixture);
        names.put(generator, fixture);
        names.put(metaTile, fixture);
        names.put("gregtech/common/pollution/Pollution", Type.getInternalName(PollutionSink.class));
        ClassWriter writer = new ClassWriter(0);
        node.accept(new ClassRemapper(writer, new SimpleRemapper(names)));
        byte[] bytecode = writer.toByteArray();
        fixtureClass = new ClassLoader(SteamTurbineTest.class.getClassLoader()) {

            Class<?> defineFixture() {
                return defineClass(fixture.replace('/', '.'), bytecode, 0, bytecode.length);
            }
        }.defineFixture();
    }

    @AfterClass
    public static void restoreSteamRegistration() {
        if (steam != null) GTModHandler.sAnySteamFluidIDs.remove(Integer.valueOf(steam.getID()));
        Materials.Steam = originalSteam;
    }

    @Test
    public void allSixTiersConvertOneLiterToTenEuAndOutputSixtyFourAmps() throws Exception {
        for (int tier = 1; tier <= 6; tier++) {
            Generator turbine = turbine(tier);
            FluidStack fuel = new FluidStack(steam, 1);
            assertEquals(2000, turbine.getEfficiency());
            assertEquals(1, turbine.consumedFluidPerOperation(fuel));
            assertEquals(10, turbine.getFuelValue(fuel));
            assertEquals(10L, turbine.getFuelValue(fuel, true));
            assertEquals(0, turbine.getFuelValue((FluidStack) null));
            assertEquals(0, turbine.getFuelValue(new FluidStack(FluidRegistry.WATER, 1)));
            assertEquals(64L, turbine.maxAmperesOut());
            assertEquals(V[tier], turbine.maxEUOutput());
            assertEquals(V[tier] * 1280L + turbine.getMinimumStoredEU(), turbine.maxEUStore());
            assertEquals((int) Math.max(640000L * tier, V[tier] * 2560L), turbine.getCapacity());
        }
    }

    @Test
    public void tenTickFuelCyclesSustainFullOutputWithOneTenthOfTheOldFullLoadSteam() throws Exception {
        for (int tier = 1; tier <= 6; tier++) {
            Generator turbine = turbine(tier);
            turbine.mFluid = new FluidStack(steam, turbine.getCapacity());
            turbine.onPostTick(turbine.base, 0);
            int initialSteam = turbine.mFluid.amount;
            // GregTech also charges tier-based loss for each emitted packet.
            long energyPerPacket = turbine.maxEUOutput() + (1L << (tier - 1));
            long output = energyPerPacket * turbine.maxAmperesOut();
            for (long tick = 1; tick <= 100; tick++) {
                assertTrue("Tier " + tier + " ran out of EU at tick " + tick,
                    turbine.stored - turbine.getMinimumStoredEU() >= output);
                turbine.stored -= output;
                turbine.onPostTick(turbine.base, tick);
            }
            long consumed = initialSteam - turbine.mFluid.amount;
            assertEquals(output * 100 / 10, consumed);
            assertEquals(energyPerPacket * 32L * 2L * 100L, consumed * 10);
            assertEquals(turbine.maxEUStore(), turbine.stored);
        }
    }

    @Test
    public void aFullBufferAndLessThanOneFuelBatchOfSpaceConsumeNothing() throws Exception {
        Generator turbine = turbine(1);
        turbine.mFluid = new FluidStack(steam, 100);
        turbine.stored = turbine.maxEUStore();
        turbine.onPostTick(turbine.base, 0);
        assertEquals(100, turbine.mFluid.amount);
        turbine.stored -= 9;
        long previousEnergy = turbine.stored;
        turbine.onPostTick(turbine.base, 10);
        assertEquals(previousEnergy, turbine.stored);
        assertEquals(100, turbine.mFluid.amount);
        turbine.stored--;
        turbine.onPostTick(turbine.base, 20);
        assertEquals(turbine.maxEUStore(), turbine.stored);
        assertEquals(99, turbine.mFluid.amount);
    }

    @Test
    public void finiteSteamCannotGenerateMoreThanItsFuelValue() throws Exception {
        for (int liters : new int[] { 1, 7, 15 }) {
            Generator turbine = turbine(1);
            turbine.mFluid = new FluidStack(steam, liters);
            long initialEnergy = turbine.stored;
            turbine.onPostTick(turbine.base, 0);
            assertEquals(initialEnergy + liters * 10L, turbine.stored);
            assertEquals(0, turbine.mFluid.amount);
            turbine.onPostTick(turbine.base, 10);
            assertEquals(initialEnergy + liters * 10L, turbine.stored);
        }
    }

    @Test
    public void fuelIsOnlyConsumedOnAnEnabledServerFuelTick() throws Exception {
        Generator turbine = turbine(1);
        turbine.mFluid = new FluidStack(steam, 10);
        long initialEnergy = turbine.stored;
        turbine.onPostTick(turbine.base, 1);
        turbine.allowed = false;
        turbine.onPostTick(turbine.base, 10);
        turbine.allowed = true;
        turbine.serverSide = false;
        turbine.onPostTick(turbine.base, 20);
        assertEquals(initialEnergy, turbine.stored);
        assertEquals(10, turbine.mFluid.amount);
        turbine.serverSide = true;
        turbine.onPostTick(turbine.base, 30);
        assertEquals(initialEnergy + 100, turbine.stored);
        assertEquals(0, turbine.mFluid.amount);
    }

    private static ClassNode readClass(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream bytes = SteamTurbineTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(bytes);
            new ClassReader(bytes).accept(node, 0);
        }
        return node;
    }

    private static Generator turbine(int tier) throws Exception {
        Generator turbine = (Generator) fixtureClass.getConstructor().newInstance();
        turbine.mTier = (byte) tier;
        turbine.stored = turbine.getMinimumStoredEU();
        return turbine;
    }

    public abstract static class Generator {

        public byte mTier;
        public FluidStack mFluid;
        public ItemStack[] mInventory = new ItemStack[3];
        private long stored;
        private boolean allowed = true;
        private boolean serverSide = true;
        private final IGregTechTileEntity base = (IGregTechTileEntity) Proxy.newProxyInstance(
            SteamTurbineTest.class.getClassLoader(), new Class<?>[] { IGregTechTileEntity.class },
            (proxy, method, arguments) -> switch (method.getName()) {
            case "isServerSide" -> serverSide;
            case "isAllowedToWork" -> allowed;
            case "getUniversalEnergyStored" -> stored;
            case "increaseStoredEnergyUnits" -> {
            stored += (long) arguments[0];
            yield true;
            }
            case "setActive" -> null;
            default -> throw new AssertionError("Unexpected tile access: " + method.getName());
            });

        public IGregTechTileEntity getBaseMetaTileEntity() {
            return base;
        }

        public long getEUVar() {
            return stored;
        }

        public int getInputSlot() {
            return 0;
        }

        public abstract int getEfficiency();

        public abstract int consumedFluidPerOperation(FluidStack fluid);

        public abstract int getFuelValue(FluidStack fluid);

        public abstract long getFuelValue(FluidStack fluid, boolean large);

        public abstract long maxAmperesOut();

        public abstract long maxEUOutput();

        public abstract long maxEUStore();

        public abstract long getMinimumStoredEU();

        public abstract int getCapacity();

        public abstract void onPostTick(IGregTechTileEntity base, long tick);
    }

    public static class PollutionSink {

        public static void addPollution(IGregTechTileEntity base, int amount) {
            assertEquals(0, amount);
        }
    }
}
