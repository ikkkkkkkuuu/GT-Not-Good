package com.xyp.gtnotgood.qa;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class HipCompressorHeatTest {

    private static final String TARGET = "gregtech/common/tileentities/machines/multi/compressor/MTEHIPCompressor";
    private static final String MIXIN = "com/xyp/gtnotgood/mixins/late/gregtech/HipCompressorHeatMixin";
    private static final String HANDLER = "gtnotgood$preventOverheating";
    private static final List<String> REDIRECTED = Arrays.asList("onPostTick", "loadNBTData", "onValueUpdate");
    private static final String TILE = "gregtech/api/interfaces/tileentity/IGregTechTileEntity";
    private static final String NBT = "net/minecraft/nbt/NBTTagCompound";
    private static final String FIXTURE = "com/xyp/gtnotgood/qa/HipCompressorHeatFixture";

    @Test
    public void redirectMatchesExactlyOneFlagWriteInEachNativeMethod() throws Exception {
        ClassNode target = read(TARGET);
        for (String name : REDIRECTED) {
            MethodNode method = target.methods.stream().filter(candidate -> candidate.name.equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError(name));
            assertEquals(name, 1,
                Arrays.stream(method.instructions.toArray()).filter(HipCompressorHeatTest::overheatWrite).count());
        }
        ClassNode mixin = read(MIXIN);
        AnnotationNode targetAnnotation = annotation(mixin.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        assertEquals(Arrays.asList(Type.getObjectType(TARGET)), value(targetAnnotation, "value"));
        assertEquals(Boolean.FALSE, value(targetAnnotation, "remap"));
        MethodNode handler = method(mixin, HANDLER);
        assertEquals("(L" + TARGET + ";Z)V", handler.desc);
        AnnotationNode redirect = annotation(handler.visibleAnnotations,
            "Lorg/spongepowered/asm/mixin/injection/Redirect;");
        assertEquals(REDIRECTED, value(redirect, "method"));
        assertEquals(Integer.valueOf(3), value(redirect, "require"));
        assertEquals(Integer.valueOf(3), value(redirect, "allow"));
        AnnotationNode at = value(redirect, "at");
        assertEquals("FIELD", value(at, "value"));
        assertEquals("L" + TARGET + ";overheated:Z", value(at, "target"));
        assertEquals(Integer.valueOf(Opcodes.PUTFIELD), value(at, "opcode"));
    }

    @Test
    public void nativeHeatingStaysAtMaximumWithoutOverheatingAndIdleCoolingUpdatesSensors() throws Exception {
        Object machine = fixture().getConstructor().newInstance();
        CompressorProbe probe = (CompressorProbe) machine;
        SensorProbe sensor = new SensorProbe();
        sensors(machine).add(sensor);
        probe.duration = 100;
        for (int second = 1; second <= 100; second++) {
            tick(machine, second * 20L);
            assertFalse(field(machine, "overheated").getBoolean(machine));
            assertEquals(Math.min(100F, second * 5F), field(machine, "heat").getFloat(machine), 0F);
            assertEquals(field(machine, "heat").getFloat(machine), sensor.heat, 0F);
        }
        assertEquals(100, sensor.updates);
        assertEquals(12, machine.getClass().getMethod("getMaxParallelRecipes").invoke(machine));
        probe.duration = 0;
        tick(machine, 2020);
        assertEquals(98F, sensor.heat, 0F);
        tick(machine, 2040);
        assertEquals(94F, sensor.heat, 0F);
        for (int second = 103; second <= 120; second++) tick(machine, second * 20L);
        assertEquals(0F, sensor.heat, 0F);
        assertFalse(field(machine, "overheated").getBoolean(machine));
        assertEquals(120, probe.ticks);
    }

    @Test
    public void nativeTicksKeepCoilScalingAndServerSecondBoundaries() throws Exception {
        Object machine = fixture().getConstructor().newInstance();
        CompressorProbe probe = (CompressorProbe) machine;
        probe.duration = 100;
        field(machine, "coilTier").setInt(machine, 2);
        tick(machine, 1);
        assertEquals(0F, field(machine, "heat").getFloat(machine), 0F);
        probe.tile.client = true;
        tick(machine, 20);
        assertEquals(0F, field(machine, "heat").getFloat(machine), 0F);
        probe.tile.client = false;
        tick(machine, 40);
        assertEquals(4.05F, field(machine, "heat").getFloat(machine), 0.00001F);
        assertEquals(3, probe.ticks);
    }

    @Test
    public void loadedCoolingFlagsAndClientUpdatesCannotRestoreOverheating() throws Exception {
        Object machine = fixture().getConstructor().newInstance();
        NbtProbe nbt = new NbtProbe();
        nbt.setFloat("heat", 83.5F);
        nbt.setBoolean("cooling", true);
        nbt.setInteger("coilTier", 7);
        nbt.setInteger("coolingTimer", 14);
        machine.getClass().getMethod("loadNBTData", NbtProbe.class).invoke(machine, nbt);
        assertFalse(field(machine, "overheated").getBoolean(machine));
        assertEquals(83.5F, field(machine, "heat").getFloat(machine), 0F);
        assertEquals(7, field(machine, "coilTier").getInt(machine));
        assertEquals(14, field(machine, "coolingTimer").getInt(machine));
        assertEquals(1, ((CompressorProbe) machine).loads);
        machine.getClass().getMethod("onValueUpdate", byte.class).invoke(machine, (byte) 1);
        assertFalse(field(machine, "overheated").getBoolean(machine));
        assertEquals((byte) 0, machine.getClass().getMethod("getUpdateData").invoke(machine));
        NbtProbe saved = new NbtProbe();
        machine.getClass().getMethod("saveNBTData", NbtProbe.class).invoke(machine, saved);
        assertFalse(saved.getBoolean("cooling"));
        assertEquals(83.5F, saved.getFloat("heat"), 0F);
        assertEquals(7, saved.getInteger("coilTier"));
        assertEquals(14, saved.getInteger("coolingTimer"));
    }

    @Test
    public void nativeProcessingKeepsNormalSpeedEnergyAndHipRecipesAtMaximumHeat() throws Exception {
        Class<?> type = fixture();
        Object machine = type.getConstructor().newInstance();
        CompressorProbe probe = (CompressorProbe) machine;
        probe.duration = 100;
        for (int second = 1; second <= 100; second++) tick(machine, second * 20L);
        Class<?> logicType = type.getClassLoader().loadClass((FIXTURE + "$Logic").replace('/', '.'));
        LogicProbe logic = (LogicProbe) logicType.getConstructor(type).newInstance(machine);
        RecipeProbe hip = new RecipeProbe(1);
        assertSame(ResultRegistryProbe.SUCCESS,
            logicType.getMethod("validateRecipe", RecipeProbe.class).invoke(logic, hip));
        assertEquals(1D / 3.5D, logic.speed, 0.0000001D);
        assertEquals(0.75D, logic.euModifier, 0D);
        assertSame(ResultRegistryProbe.SUCCESS,
            logicType.getMethod("onRecipeStart", RecipeProbe.class).invoke(logic, hip));
        assertEquals(0, probe.stops);
        assertSame(ResultRegistryProbe.NO_RECIPE,
            logicType.getMethod("validateRecipe", RecipeProbe.class).invoke(logic, new RecipeProbe(2)));
    }

    private static Class<?> fixture() throws Exception {
        ClassNode node = read(TARGET);
        String parent = node.superName;
        MethodNode handler = method(read(MIXIN), HANDLER);
        handler.visibleAnnotations = null;
        handler.invisibleAnnotations = null;
        node.methods.removeIf(method -> !REDIRECTED.contains(method.name) && !method.name.equals("saveNBTData")
            && !method.name.equals("getUpdateData")
            && !method.name.equals("getMaxParallelRecipes")
            && !method.name.endsWith("$get$overheated"));
        // Preserve native bodies and the real Redirect handler without starting Forge.
        for (MethodNode method : node.methods) {
            if (!REDIRECTED.contains(method.name)) continue;
            int temporary = method.maxLocals;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!overheatWrite(instruction)) continue;
                method.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ISTORE, temporary));
                method.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ASTORE, temporary + 1));
                method.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ALOAD, 0));
                method.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ALOAD, temporary + 1));
                method.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ILOAD, temporary));
                method.instructions.set(instruction,
                    new MethodInsnNode(Opcodes.INVOKESPECIAL, TARGET, HANDLER, handler.desc, false));
            }
        }
        node.methods.add(handler);
        node.fields.removeIf(field -> !Arrays.asList("heat", "overheated", "coilTier", "coolingTimer", "sensorHatches")
            .contains(field.name));
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, parent, "<init>", "()V", false);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
        constructor.visitInsn(Opcodes.DUP);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        constructor.visitFieldInsn(Opcodes.PUTFIELD, TARGET, "sensorHatches", "Ljava/util/ArrayList;");
        constructor.visitInsn(Opcodes.RETURN);
        node.methods.add(constructor);
        Map<String, String> names = new HashMap<>();
        names.put(TARGET, FIXTURE);
        names.put(TARGET + "$1", FIXTURE + "$Logic");
        names.put(MIXIN, FIXTURE);
        names.put(parent, Type.getInternalName(CompressorProbe.class));
        names.put(TILE, Type.getInternalName(TileAccess.class));
        names.put(NBT, Type.getInternalName(NbtProbe.class));
        names.put("gregtech/common/tileentities/machines/MTEHeatSensor", Type.getInternalName(SensorProbe.class));
        names.put("gregtech/api/util/GTUtility", Type.getInternalName(UtilityProbe.class));
        names.put("bartworks/util/MathUtils", Type.getInternalName(UtilityProbe.class));
        names.put("gregtech/api/logic/ProcessingLogic", Type.getInternalName(LogicProbe.class));
        names.put("gregtech/api/util/GTRecipe", Type.getInternalName(RecipeProbe.class));
        names.put("gregtech/api/util/GTRecipeConstants", Type.getInternalName(RecipeConstantsProbe.class));
        names.put("gregtech/api/recipe/RecipeMetadataKey", Type.getInternalName(MetadataProbe.class));
        names.put("gregtech/api/recipe/check/CheckRecipeResult", Type.getInternalName(ResultProbe.class));
        names.put("gregtech/api/recipe/check/CheckRecipeResultRegistry",
            Type.getInternalName(ResultRegistryProbe.class));
        names.put("gregtech/api/util/shutdown/ShutDownReason", Type.getInternalName(ShutdownProbe.class));
        names.put("gregtech/api/util/shutdown/ShutDownReasonRegistry",
            Type.getInternalName(ShutdownRegistryProbe.class));
        ClassNode logic = read(TARGET + "$1");
        logic.access = Opcodes.ACC_PUBLIC;
        for (MethodNode method : logic.methods) method.access = Opcodes.ACC_PUBLIC;
        Map<String, byte[]> classes = new HashMap<>();
        classes.put(FIXTURE.replace('/', '.'), remap(node, names));
        classes.put((FIXTURE + "$Logic").replace('/', '.'), remap(logic, names));
        return new ClassLoader(HipCompressorHeatTest.class.getClassLoader()) {

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytecode = classes.get(name);
                if (bytecode == null) throw new ClassNotFoundException(name);
                return defineClass(name, bytecode, 0, bytecode.length);
            }
        }.loadClass(FIXTURE.replace('/', '.'));
    }

    private static byte[] remap(ClassNode node, Map<String, String> names) {
        node.signature = null;
        node.interfaces.clear();
        node.innerClasses.clear();
        node.outerClass = null;
        node.visibleAnnotations = null;
        node.invisibleAnnotations = null;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(new ClassRemapper(writer, new SimpleRemapper(names)));
        return writer.toByteArray();
    }

    private static boolean overheatWrite(AbstractInsnNode instruction) {
        return instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD
            && field.owner.equals(TARGET)
            && field.name.equals("overheated")
            && field.desc.equals("Z");
    }

    private static void tick(Object machine, long tick) throws Exception {
        machine.getClass().getMethod("onPostTick", TileAccess.class, long.class).invoke(machine,
            ((CompressorProbe) machine).tile, tick);
    }

    @SuppressWarnings("unchecked")
    private static List<SensorProbe> sensors(Object machine) throws Exception {
        return (List<SensorProbe>) field(machine, "sensorHatches").get(machine);
    }

    private static Field field(Object machine, String name) throws Exception {
        Field field = machine.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static ClassNode read(String name) throws Exception {
        try (InputStream bytes = HipCompressorHeatTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(name, bytes);
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(method -> method.name.equals(name)).findFirst()
            .orElseThrow(() -> new AssertionError(name));
    }

    private static AnnotationNode annotation(List<AnnotationNode> annotations, String descriptor) {
        assertNotNull(descriptor, annotations);
        return annotations.stream().filter(annotation -> annotation.desc.equals(descriptor)).findFirst()
            .orElseThrow(() -> new AssertionError(descriptor));
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(AnnotationNode annotation, String name) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (annotation.values.get(index).equals(name)) return (T) annotation.values.get(index + 1);
        }
        throw new AssertionError(name);
    }

    public interface TileAccess {

        boolean isClientSide();

        void issueTextureUpdate();
    }

    public static class TileProbe implements TileAccess {

        private boolean client;

        public boolean isClientSide() {
            return client;
        }

        public void issueTextureUpdate() {}
    }

    public static class CompressorProbe {

        private final TileProbe tile = new TileProbe();
        private int duration;
        private int ticks;
        private int loads;
        private int stops;

        public int maxProgresstime() {
            return duration;
        }

        public long getMaxInputVoltage() {
            return 512;
        }

        public TileAccess getBaseMetaTileEntity() {
            return tile;
        }

        public void onPostTick(TileAccess tile, long tick) {
            ticks++;
        }

        public void loadNBTData(NbtProbe nbt) {
            loads++;
        }

        public void saveNBTData(NbtProbe nbt) {}

        public void stopMachine(ShutdownProbe reason) {
            stops++;
        }
    }

    public static class SensorProbe {

        private float heat;
        private int updates;

        public void setHeatValue(float value) {
            heat = value;
            updates++;
        }
    }

    public static class UtilityProbe {

        public static double powInt(double value, int exponent) {
            return Math.pow(value, exponent);
        }

        public static float clamp(float value, float minimum, float maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }

        public static byte getTier(long voltage) {
            assertEquals(512L, voltage);
            return 3;
        }
    }

    public static class NbtProbe {

        private final Map<String, Object> values = new HashMap<>();

        public boolean hasKey(String key) {
            return values.containsKey(key);
        }

        public void setFloat(String key, float value) {
            values.put(key, value);
        }

        public float getFloat(String key) {
            return (Float) values.get(key);
        }

        public void setBoolean(String key, boolean value) {
            values.put(key, value);
        }

        public boolean getBoolean(String key) {
            return (Boolean) values.get(key);
        }

        public void setInteger(String key, int value) {
            values.put(key, value);
        }

        public int getInteger(String key) {
            return (Integer) values.get(key);
        }
    }

    public static class LogicProbe {

        private double speed;
        private double euModifier;

        public LogicProbe setSpeedBonus(double value) {
            speed = value;
            return this;
        }

        public LogicProbe setEuModifier(double value) {
            euModifier = value;
            return this;
        }

        protected ResultProbe validateRecipe(RecipeProbe recipe) {
            return ResultRegistryProbe.SUCCESS;
        }

        protected ResultProbe onRecipeStart(RecipeProbe recipe) {
            return ResultRegistryProbe.SUCCESS;
        }
    }

    public static class RecipeProbe {

        private final int compressionTier;

        public RecipeProbe(int tier) {
            compressionTier = tier;
        }

        public Object getMetadataOrDefault(MetadataProbe key, Object fallback) {
            return compressionTier;
        }
    }

    public static class MetadataProbe {
    }

    public static class RecipeConstantsProbe {

        public static final MetadataProbe COMPRESSION_TIER = new MetadataProbe();
    }

    public static class ResultProbe {
    }

    public static class ResultRegistryProbe {

        public static final ResultProbe NO_RECIPE = new ResultProbe();
        public static final ResultProbe SUCCESS = new ResultProbe();
    }

    public static class ShutdownProbe {
    }

    public static class ShutdownRegistryProbe {

        public static final ShutdownProbe OVERHEAT_FAIL = new ShutdownProbe();
    }
}
