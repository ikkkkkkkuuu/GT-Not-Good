package com.xyp.gtnotgood.mixins.late.cutcorners;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
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
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.xyp.gtnotgood.config.Config;

public class SteamFurnaceSpeedTargetTest {

    private static final String STEAM_PACKAGE = "gregtech/common/tileentities/machines/steam/";
    private static final String MIXIN = "com/xyp/gtnotgood/mixins/late/cutcorners/SteamFurnaceSpeedMixin";
    private static final String HANDLER = "gtnotgood$modifySteamFurnaceDuration";
    private int savedMode;
    private int savedFixedDuration;
    private float savedMultiplier;
    private boolean savedLoaded;
    private Field loaded;

    @Before
    public void preserveConfiguration() throws Exception {
        savedMode = Config.recipeSpeedMode;
        savedFixedDuration = Config.recipeSpeedFixedDuration;
        savedMultiplier = Config.recipeSpeedMultiplier;
        loaded = Config.class.getDeclaredField("configLoaded");
        loaded.setAccessible(true);
        savedLoaded = loaded.getBoolean(null);
        loaded.setBoolean(null, true);
    }

    @After
    public void restoreConfiguration() throws Exception {
        Config.recipeSpeedMode = savedMode;
        Config.recipeSpeedFixedDuration = savedFixedDuration;
        Config.recipeSpeedMultiplier = savedMultiplier;
        loaded.setBoolean(null, savedLoaded);
    }

    @Test
    public void eachNativeCheckHasOneDurationConstantAndKeepsTheSmeltingContract() throws Exception {
        for (String variant : new String[] { "Bronze", "Steel" }) {
            MethodNode check = method(read(STEAM_PACKAGE + "MTESteamFurnace" + variant), "checkRecipe", "()I");
            int durations = 0;
            int smeltingCalls = 0;
            int energyWrites = 0;
            for (AbstractInsnNode instruction : check.instructions) {
                if (durationConstant(instruction)) {
                    assertEquals(variant.equals("Bronze") ? 256 : 128, ((IntInsnNode) instruction).operand);
                    FieldInsnNode assignment = (FieldInsnNode) instruction.getNext();
                    assertEquals(Opcodes.PUTFIELD, assignment.getOpcode());
                    assertEquals("mMaxProgresstime", assignment.name);
                    durations++;
                }
                if (instruction instanceof MethodInsnNode call && call.name.equals("getSmeltingOutput")) {
                    assertEquals("gregtech/api/util/GTModHandler", call.owner);
                    assertEquals(
                        "(Lnet/minecraft/item/ItemStack;ZLnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;",
                        call.desc);
                    smeltingCalls++;
                }
                if (instruction instanceof FieldInsnNode field && field.name.equals("mEUt")) {
                    assertEquals(Opcodes.PUTFIELD, field.getOpcode());
                    assertEquals(variant.equals("Bronze") ? Opcodes.ICONST_4 : Opcodes.BIPUSH,
                        field.getPrevious().getOpcode());
                    if (variant.equals("Steel")) assertEquals(8, ((IntInsnNode) field.getPrevious()).operand);
                    energyWrites++;
                }
            }
            assertEquals("Each target must match exactly once", 1, durations);
            assertEquals(1, smeltingCalls);
            assertEquals(1, energyWrites);
        }
    }

    @Test
    public void mixinSelectsBothFurnacesAndOnlyTheirDurationConstants() throws Exception {
        ClassNode mixin = read(MIXIN);
        AnnotationNode target = annotation(mixin.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        List<Type> classes = value(target, "value");
        assertEquals(2, classes.size());
        assertEquals(STEAM_PACKAGE + "MTESteamFurnaceBronze", classes.get(0).getInternalName());
        assertEquals(STEAM_PACKAGE + "MTESteamFurnaceSteel", classes.get(1).getInternalName());
        assertEquals(Boolean.FALSE, value(target, "remap"));
        MethodNode handler = method(mixin, HANDLER, "(I)I");
        AnnotationNode injection = annotation(handler.visibleAnnotations,
            "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;");
        assertEquals(Arrays.asList("checkRecipe"), value(injection, "method"));
        assertEquals(Integer.valueOf(1), value(injection, "require"));
        assertEquals(Integer.valueOf(1), value(injection, "allow"));
        List<AnnotationNode> constants = value(injection, "constant");
        List<Integer> durations = new ArrayList<>();
        for (AnnotationNode constant : constants) durations.add(value(constant, "intValue"));
        assertEquals(Arrays.asList(256, 128), durations);
    }

    @Test
    public void nativeChecksWithTheActualHandlerRespectEverySpeedMode() throws Exception {
        for (String variant : new String[] { "Bronze", "Steel" }) {
            Class<?> fixture = fixture(variant);
            int original = variant.equals("Bronze") ? 256 : 128;
            Config.recipeSpeedMode = 0;
            assertSuccessfulCheck(fixture, original, variant);
            Config.recipeSpeedMode = 1;
            Config.recipeSpeedFixedDuration = 1;
            assertSuccessfulCheck(fixture, 1, variant);
            Config.recipeSpeedFixedDuration = 7;
            assertSuccessfulCheck(fixture, 7, variant);
            Config.recipeSpeedFixedDuration = 0;
            assertSuccessfulCheck(fixture, 1, variant);
            Config.recipeSpeedMode = 2;
            Config.recipeSpeedMultiplier = 0.1F;
            assertSuccessfulCheck(fixture, variant.equals("Bronze") ? 25 : 12, variant);
            Config.recipeSpeedMultiplier = 1F;
            assertSuccessfulCheck(fixture, original, variant);
            Config.recipeSpeedMultiplier = 0F;
            assertSuccessfulCheck(fixture, 1, variant);
        }
    }

    @Test
    public void failedSmeltingChecksLeaveEnergyDurationAndInputUntouched() throws Exception {
        Config.recipeSpeedMode = 1;
        Config.recipeSpeedFixedDuration = 1;
        for (String variant : new String[] { "Bronze", "Steel" }) {
            Class<?> fixture = fixture(variant);
            FurnaceState state = (FurnaceState) fixture.getConstructor().newInstance();
            state.mEUt = 71;
            state.mMaxProgresstime = 73;
            SmeltingProbe.result = null;
            assertEquals(0, fixture.getMethod("checkRecipe").invoke(state));
            assertEquals(71, state.mEUt);
            assertEquals(73, state.mMaxProgresstime);
            assertEquals(3, state.input.stackSize);
            assertNull(state.mOutputItems[0]);
        }
    }

    private static void assertSuccessfulCheck(Class<?> fixture, int duration, String variant) throws Exception {
        FurnaceState state = (FurnaceState) fixture.getConstructor().newInstance();
        SmeltingProbe.result = new StackProbe();
        assertEquals(2, fixture.getMethod("checkRecipe").invoke(state));
        assertEquals(duration, state.mMaxProgresstime);
        assertEquals(variant.equals("Bronze") ? 4 : 8, state.mEUt);
        assertEquals("Exactly one item must be consumed", 2, state.input.stackSize);
        assertEquals(3, state.output.stackSize);
        assertSame(SmeltingProbe.result, state.mOutputItems[0]);
        assertSame(state.output, SmeltingProbe.output);
        assertTrue("The native helper must still receive the consume flag", SmeltingProbe.consume);
    }

    private static Class<?> fixture(String variant) throws Exception {
        ClassNode node = read(STEAM_PACKAGE + "MTESteamFurnace" + variant);
        String source = node.name;
        String fixture = MIXIN + "Fixture" + variant;
        MethodNode check = method(node, "checkRecipe", "()I");
        MethodNode handler = method(read(MIXIN), HANDLER, "(I)I");
        handler.visibleAnnotations = null;
        handler.invisibleAnnotations = null;
        // Execute the real method bodies with ModifyConstant's instance-call shape, without a Forge bootstrap.
        for (AbstractInsnNode instruction : check.instructions.toArray()) {
            if (!durationConstant(instruction)) continue;
            check.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ALOAD, 0));
            check.instructions.insert(instruction,
                new MethodInsnNode(Opcodes.INVOKESPECIAL, source, HANDLER, "(I)I", false));
        }
        node.superName = Type.getInternalName(FurnaceState.class);
        node.signature = null;
        node.interfaces.clear();
        node.fields.clear();
        node.methods.clear();
        node.methods.add(check);
        node.methods.add(handler);
        node.visibleAnnotations = null;
        node.invisibleAnnotations = null;
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, node.superName, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        node.methods.add(constructor);
        Map<String, String> names = new HashMap<>();
        names.put(source, fixture);
        names.put("net/minecraft/item/ItemStack", Type.getInternalName(StackProbe.class));
        names.put("gregtech/api/util/GTModHandler", Type.getInternalName(SmeltingProbe.class));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(new ClassRemapper(writer, new SimpleRemapper(names)));
        byte[] bytecode = writer.toByteArray();
        return new ClassLoader(SteamFurnaceSpeedTargetTest.class.getClassLoader()) {

            Class<?> defineFixture() {
                return defineClass(fixture.replace('/', '.'), bytecode, 0, bytecode.length);
            }
        }.defineFixture();
    }

    private static boolean durationConstant(AbstractInsnNode instruction) {
        return instruction instanceof IntInsnNode constant && (constant.operand == 256 || constant.operand == 128);
    }

    private static ClassNode read(String name) throws Exception {
        try (InputStream bytes = SteamFurnaceSpeedTargetTest.class.getClassLoader()
            .getResourceAsStream(name + ".class")) {
            assertNotNull(name, bytes);
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static MethodNode method(ClassNode node, String name, String descriptor) {
        return node.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
            .findFirst().orElseThrow(() -> new AssertionError(name + descriptor));
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

    public static class StackProbe {

        public int stackSize = 3;
    }

    public static class FurnaceState {

        public final StackProbe input = new StackProbe();
        public final StackProbe output = new StackProbe();
        public final StackProbe[] mOutputItems = new StackProbe[1];
        public int mEUt;
        public int mMaxProgresstime;

        protected StackProbe getInputAt(int slot) {
            assertEquals(0, slot);
            return input;
        }

        protected StackProbe getOutputAt(int slot) {
            assertEquals(0, slot);
            return output;
        }
    }

    public static class SmeltingProbe {

        private static StackProbe result;
        private static StackProbe output;
        private static boolean consume;

        public static StackProbe getSmeltingOutput(StackProbe input, boolean removeInput, StackProbe outputSlot) {
            output = outputSlot;
            consume = removeInput;
            if (result != null && removeInput) input.stackSize--;
            return result;
        }
    }
}
