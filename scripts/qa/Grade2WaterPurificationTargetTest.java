package com.xyp.gtnotgood.mixins.late.treatedwater;

import static org.junit.Assert.*;

import java.io.InputStream;
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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.xyp.gtnotgood.config.MainConfig;

public class Grade2WaterPurificationTargetTest {

    private static final String BASE = "gregtech/common/tileentities/machines/multi/purification/MTEPurificationUnitBase";
    private static final String OZONATION = "gregtech/common/tileentities/machines/multi/purification/MTEPurificationUnitOzonation";
    private static final String MIXIN = "com/xyp/gtnotgood/mixins/late/treatedwater/Grade2WaterPurificationMixin";
    private static final String RESULT = "Lgregtech/api/recipe/check/CheckRecipeResult;";
    private static final String CALLBACK = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    private boolean savedEnabled;
    private Class<?> fixture;

    @Before
    public void createFixture() throws Exception {
        savedEnabled = MainConfig.grade2WaterPurificationEnabled;
        MainConfig.grade2WaterPurificationEnabled = true;
        fixture = fixture();
    }

    @After
    public void restoreConfiguration() {
        MainConfig.grade2WaterPurificationEnabled = savedEnabled;
    }

    @Test
    public void injectionCancelsAtTheHeadOfTheActualOzonationCheck() throws Exception {
        ClassNode mixin = read(MIXIN);
        AnnotationNode target = annotation(mixin.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        assertEquals(Arrays.asList(Type.getObjectType(OZONATION)), value(target, "value"));
        assertEquals(Boolean.FALSE, value(target, "remap"));
        MethodNode handler = method(mixin, "injectCheckProcessing", "(L" + CALLBACK + ";)V");
        AnnotationNode injection = annotation(handler.visibleAnnotations,
            "Lorg/spongepowered/asm/mixin/injection/Inject;");
        assertEquals(Arrays.asList("checkProcessing"), value(injection, "method"));
        assertEquals(Boolean.TRUE, value(injection, "cancellable"));
        assertEquals(Integer.valueOf(1), value(injection, "require"));
        assertEquals(Integer.valueOf(1), value(injection, "allow"));
        List<AnnotationNode> points = value(injection, "at");
        assertEquals(1, points.size());
        assertEquals("HEAD", value(points.get(0), "value"));
        assertEquals(1,
            read(OZONATION).methods.stream()
                .filter(candidate -> candidate.name.equals("checkProcessing") && candidate.desc.equals("()" + RESULT))
                .count());
        MethodNode nativeCheck = method(read(OZONATION), "checkProcessing", "()" + RESULT);
        assertEquals(1, calls(nativeCheck, BASE, "checkProcessing"));
        assertEquals(1, calls(nativeCheck, OZONATION, "explodeMultiblock"));
        assertEquals(1, calls(handler, BASE, "checkProcessing"));
    }

    @Test
    public void waterAloneRunsAtFullChanceAndOnlyConsumesParallelWater() throws Exception {
        WaterState state = state(2500, 0);
        assertSame(Results.SUCCESSFUL, check(state));
        assertEquals(2, state.effectiveParallel);
        assertEquals(100.0f, chance(state), 0.0f);
        assertFalse(state.exploded);
        assertSame(state.inputFluids, state.storedFluids);
        assertEquals(1, state.inputFluids.size());
        assertEquals(2500, state.inputFluids.get(0).amount);
        assertNotSame(state.sharedRecipe, state.currentRecipe);
        assertEquals(1, state.currentRecipe.mFluidInputs.length);
        assertEquals(2, state.sharedRecipe.mFluidInputs.length);
        assertEquals(1000, state.sharedRecipe.mFluidInputs[1].amount);
        assertEquals(Integer.MAX_VALUE, state.lookupFluids[1].amount);
        fixture.getMethod("depleteRecipeInputs").invoke(state);
        assertEquals(500, state.inputFluids.get(0).amount);
        assertEquals(1000, state.sharedRecipe.mFluidInputs[0].amount);
    }

    @Test
    public void missingOrInsufficientWaterStillFails() throws Exception {
        assertSame(Results.NO_RECIPE, check(state(0, 0)));
        assertSame(Results.NO_RECIPE, check(state(999, 5000000)));
    }

    @Test
    public void failedRecipeChecksKeepTheirOriginalResult() throws Exception {
        for (Result failure : Arrays.asList(Results.FLUID_OUTPUT_FULL, Results.ITEM_OUTPUT_FULL,
            Results.OTHER_FAILURE)) {
            WaterState state = state(2500, 5000000);
            state.lookupFailure = failure;
            assertSame(failure, check(state));
            assertNull(state.currentRecipe);
            assertFalse(state.exploded);
            assertEquals(2500, state.inputFluids.get(0).amount);
            assertEquals(5000000, state.inputFluids.get(1).amount);
        }
    }

    @Test
    public void arbitraryStoredOzoneNeverExplodesOrGetsConsumedWhenEnabled() throws Exception {
        for (int ozone : new int[] { 256000, 1024001, 5000000, Integer.MAX_VALUE }) {
            WaterState state = state(2500, ozone);
            Fluid storedOzone = state.inputFluids.get(1);
            assertSame(Results.SUCCESSFUL, check(state));
            assertFalse(state.exploded);
            assertEquals(100.0f, chance(state), 0.0f);
            assertEquals(2, state.lookupFluids.length);
            assertNotSame(storedOzone, state.lookupFluids[1]);
            fixture.getMethod("depleteRecipeInputs").invoke(state);
            assertEquals(ozone, storedOzone.amount);
            assertSame(storedOzone, state.inputFluids.get(1));
            assertEquals(2, state.inputFluids.size());
            assertEquals(2, state.sharedRecipe.mFluidInputs.length);
        }
    }

    @Test
    public void disablingRestoresOzoneRequirementConsumptionChanceAndExplosion() throws Exception {
        MainConfig.grade2WaterPurificationEnabled = false;
        assertSame(Results.NO_RECIPE, check(state(2500, 0)));
        WaterState normal = state(2500, 1000);
        assertSame(Results.SUCCESSFUL, check(normal));
        assertSame(normal.sharedRecipe, normal.currentRecipe);
        assertFalse(normal.exploded);
        assertEquals(20.0f, chance(normal), 0.0f);
        fixture.getMethod("depleteRecipeInputs").invoke(normal);
        assertEquals(500, normal.inputFluids.get(0).amount);
        assertEquals(0, normal.inputFluids.get(1).amount);
        WaterState excessive = state(2500, 1024001);
        assertSame(Results.SUCCESSFUL, check(excessive));
        assertTrue(excessive.exploded);
    }

    private WaterState state(int water, int ozone) throws Exception {
        WaterState state = (WaterState) fixture.getConstructor().newInstance();
        if (water > 0) state.inputFluids.add(new Fluid("water1", water));
        if (ozone > 0) state.inputFluids.add(new Fluid("ozone", ozone));
        return state;
    }

    private Result check(WaterState state) throws Exception {
        return (Result) fixture.getMethod("checkProcessing").invoke(state);
    }

    private float chance(WaterState state) throws Exception {
        return (Float) fixture.getMethod("calculateBoostedSuccessChance").invoke(state);
    }

    private static int calls(MethodNode method, String owner, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name))
                count++;
        }
        return count;
    }

    private static Class<?> fixture() throws Exception {
        Map<String, String> names = new HashMap<>();
        names.put(BASE, MIXIN + "BaseFixture");
        names.put(OZONATION, MIXIN + "Fixture");
        names.put(MIXIN, MIXIN + "Fixture");
        names.put("net/minecraftforge/fluids/FluidStack", Type.getInternalName(Fluid.class));
        names.put("net/minecraft/item/ItemStack", Type.getInternalName(Stack.class));
        names.put("gregtech/api/util/GTRecipe", Type.getInternalName(Recipe.class));
        names.put("gregtech/api/enums/Materials", Type.getInternalName(Materials.class));
        names.put("gregtech/api/recipe/check/CheckRecipeResult", Type.getInternalName(Result.class));
        names.put("gregtech/api/recipe/check/CheckRecipeResultRegistry", Type.getInternalName(Results.class));
        names.put("gregtech/api/recipe/metadata/PurificationPlantBaseChanceKey", Type.getInternalName(ChanceKey.class));
        names.put("gregtech/api/recipe/RecipeMetadataKey", Type.getInternalName(MetadataKey.class));
        names.put(CALLBACK, Type.getInternalName(Callback.class));
        FixtureLoader loader = new FixtureLoader();
        ClassNode base = read(BASE);
        List<MethodNode> baseMethods = new ArrayList<>();
        for (String name : new String[] { "checkProcessing", "overrideRecipeCheck", "depleteRecipeInputs",
            "calculateBoostedSuccessChance" }) {
            baseMethods.add(base.methods.stream().filter(candidate -> candidate.name.equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError(name)));
        }
        prepare(base, Type.getInternalName(WaterState.class), baseMethods);
        loader.define(names.get(BASE), bytes(base, names));
        ClassNode ozone = read(OZONATION);
        MethodNode check = method(ozone, "checkProcessing", "()" + RESULT);
        ClassNode mixin = read(MIXIN);
        List<MethodNode> methods = new ArrayList<>();
        methods.add(check);
        for (MethodNode candidate : mixin.methods) {
            if (!candidate.name.equals("<init>")) methods.add(candidate);
        }
        prepare(ozone, BASE, methods);
        injectHead(check);
        return loader.define(names.get(OZONATION), bytes(ozone, names));
    }

    private static void prepare(ClassNode node, String parent, List<MethodNode> methods) {
        node.superName = parent;
        node.signature = null;
        node.interfaces.clear();
        node.fields.clear();
        node.methods = methods;
        node.visibleAnnotations = null;
        node.invisibleAnnotations = null;
        for (MethodNode method : methods) {
            method.visibleAnnotations = null;
            method.invisibleAnnotations = null;
        }
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, parent, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        node.methods.add(constructor);
    }

    private static void injectHead(MethodNode check) {
        int callbackSlot = check.maxLocals;
        LabelNode nativeCheck = new LabelNode();
        InsnList head = new InsnList();
        head.add(new TypeInsnNode(Opcodes.NEW, CALLBACK));
        head.add(new InsnNode(Opcodes.DUP));
        head.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, CALLBACK, "<init>", "()V", false));
        head.add(new VarInsnNode(Opcodes.ASTORE, callbackSlot));
        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
        head.add(new VarInsnNode(Opcodes.ALOAD, callbackSlot));
        head.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, OZONATION, "injectCheckProcessing", "(L" + CALLBACK + ";)V",
            false));
        head.add(new VarInsnNode(Opcodes.ALOAD, callbackSlot));
        head.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CALLBACK, "isCancelled", "()Z", false));
        head.add(new JumpInsnNode(Opcodes.IFEQ, nativeCheck));
        head.add(new VarInsnNode(Opcodes.ALOAD, callbackSlot));
        head.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CALLBACK, "getReturnValue", "()Ljava/lang/Object;", false));
        head.add(new TypeInsnNode(Opcodes.CHECKCAST, "gregtech/api/recipe/check/CheckRecipeResult"));
        head.add(new InsnNode(Opcodes.ARETURN));
        head.add(nativeCheck);
        check.instructions.insert(head);
    }

    private static byte[] bytes(ClassNode node, Map<String, String> names) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(new ClassRemapper(writer, new SimpleRemapper(names)));
        return writer.toByteArray();
    }

    private static ClassNode read(String name) throws Exception {
        try (InputStream bytes = Grade2WaterPurificationTargetTest.class.getClassLoader()
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

    private static class FixtureLoader extends ClassLoader {

        private FixtureLoader() {
            super(Grade2WaterPurificationTargetTest.class.getClassLoader());
        }

        private Class<?> define(String name, byte[] bytes) {
            return defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
        }
    }

    public static class WaterState {

        public static final int MAX_OZONE_GAS_FOR_EXPLOSION = 1024000;
        public ArrayList<Fluid> storedFluids;
        public final ArrayList<Fluid> inputFluids = new ArrayList<>();
        public Recipe currentRecipe;
        public final Recipe sharedRecipe = new Recipe();
        public int maxParallel = 4;
        public int effectiveParallel = 1;
        public boolean exploded;
        public Result lookupFailure;
        public Fluid[] lookupFluids;

        public ArrayList<Fluid> getStoredFluids() {
            return inputFluids;
        }

        protected Result findRecipeForInputs(Fluid[] fluids, Stack... items) {
            lookupFluids = fluids;
            for (Fluid required : sharedRecipe.mFluidInputs) {
                long available = Arrays.stream(fluids).filter(fluid -> fluid.isFluidEqual(required))
                    .mapToLong(fluid -> fluid.amount).sum();
                if (available < required.amount) return Results.NO_RECIPE;
            }
            if (lookupFailure != null) return lookupFailure;
            currentRecipe = sharedRecipe;
            return Results.SUCCESSFUL;
        }

        public boolean depleteInput(Fluid required) {
            int remaining = required.amount;
            for (Fluid stored : inputFluids) {
                if (!stored.isFluidEqual(required)) continue;
                int drain = Math.min(stored.amount, remaining);
                stored.amount -= drain;
                remaining -= drain;
            }
            return remaining == 0;
        }

        public boolean isWaterBoosted(Recipe recipe) {
            return false;
        }

        public void explodeMultiblock() {
            exploded = true;
        }
    }

    public static class Fluid {

        public final String name;
        public int amount;

        public Fluid(String name, int amount) {
            this.name = name;
            this.amount = amount;
        }

        public Fluid copy() {
            return new Fluid(name, amount);
        }

        public boolean isFluidEqual(Fluid other) {
            return other != null && name.equals(other.name);
        }
    }

    public static class Stack {
    }

    public static class Recipe {

        public Fluid[] mFluidInputs = { new Fluid("water1", 1000), new Fluid("ozone", 1000) };

        public Recipe copy() {
            Recipe result = new Recipe();
            result.mFluidInputs = Arrays.stream(mFluidInputs).map(Fluid::copy).toArray(Fluid[]::new);
            return result;
        }

        public Object getMetadataOrDefault(MetadataKey key, Object fallback) {
            return Float.valueOf(20.0f);
        }
    }

    public static class MetadataKey {
    }

    public static class ChanceKey extends MetadataKey {

        public static final ChanceKey INSTANCE = new ChanceKey();
    }

    public static class Materials {

        public static final Materials Ozone = new Materials();

        public Fluid getGas(long amount) {
            return new Fluid("ozone", (int) amount);
        }
    }

    public interface Result {

        boolean wasSuccessful();
    }

    public static class ResultValue implements Result {

        private final boolean successful;

        public ResultValue(boolean successful) {
            this.successful = successful;
        }

        @Override
        public boolean wasSuccessful() {
            return successful;
        }
    }

    public static class Results {

        public static final Result SUCCESSFUL = new ResultValue(true);
        public static final Result NO_RECIPE = new ResultValue(false);
        public static final Result FLUID_OUTPUT_FULL = new ResultValue(false);
        public static final Result ITEM_OUTPUT_FULL = new ResultValue(false);
        public static final Result OTHER_FAILURE = new ResultValue(false);
        public static final Result INTERNAL_ERROR = new ResultValue(false);
    }

    public static class Callback {

        private Object value;
        private boolean cancelled;

        public void setReturnValue(Object result) {
            value = result;
            cancelled = true;
        }

        public Object getReturnValue() {
            return value;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
