package com.xyp.gtnotgood.mixins.late.CutCorners;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import com.xyp.gtnotgood.config.Config;

public class ThermalBoilerSpeedTest {

    @Test
    public void fixedModeCaps128TickBatchButOtherModesKeepTheirRecipeScaling() {
        int mode = Config.recipeSpeedMode;
        int fixed = Config.recipeSpeedFixedDuration;
        try {
            Config.recipeSpeedMode = 1;
            Config.recipeSpeedFixedDuration = 1;
            assertEquals(1, limitBatchDuration(128));
            Config.recipeSpeedFixedDuration = 5;
            assertEquals(5, limitBatchDuration(128));
            assertEquals(1, limitBatchDuration(1));
            assertEquals(0, limitBatchDuration(0));
            Config.recipeSpeedMode = 2;
            assertEquals(256, limitBatchDuration(256));
            Config.recipeSpeedMode = 0;
            assertEquals(2560, limitBatchDuration(2560));
        } finally {
            Config.recipeSpeedMode = mode;
            Config.recipeSpeedFixedDuration = fixed;
        }
    }

    private static int limitBatchDuration(int duration) {
        return invokeHelper("limitBatchDuration", duration);
    }

    private static int invokeHelper(String name, int... arguments) {
        try {
            Class<?>[] types = new Class<?>[arguments.length];
            Object[] values = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                types[i] = int.class;
                values[i] = arguments[i];
            }
            Method method = ThermalBoilerSpeedMixin.class.getDeclaredMethod(name, types);
            assertTrue("Mixin static helpers must be private", Modifier.isPrivate(method.getModifiers()));
            method.setAccessible(true);
            return (Integer) method.invoke(null, values);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    @Test
    public void warmupUsesFixedAndMultiplierPoliciesAndPreservesDisabledMode() {
        int mode = Config.recipeSpeedMode;
        int fixed = Config.recipeSpeedFixedDuration;
        float multiplier = Config.recipeSpeedMultiplier;
        try {
            Config.recipeSpeedMode = 0;
            assertEquals(12, invokeHelper("scaledWarmupRate", 12, 10000));
            Config.recipeSpeedMode = 1;
            Config.recipeSpeedFixedDuration = 1;
            assertEquals(10000, invokeHelper("scaledWarmupRate", 12, 10000));
            Config.recipeSpeedFixedDuration = 5;
            assertEquals(2000, invokeHelper("scaledWarmupRate", 12, 10000));
            Config.recipeSpeedMode = 2;
            Config.recipeSpeedMultiplier = 0.1F;
            int gain = invokeHelper("scaledWarmupRate", 12, 10000);
            assertEquals(83, (10000 + gain - 1) / gain);
            Config.recipeSpeedMultiplier = 1F;
            assertEquals(12, invokeHelper("scaledWarmupRate", 12, 10000));
        } finally {
            Config.recipeSpeedMode = mode;
            Config.recipeSpeedFixedDuration = fixed;
            Config.recipeSpeedMultiplier = multiplier;
        }
    }

    @Test
    public void largeSteamBatchesKeepTheirEfficiencyScaledOutput() {
        assertEquals(1024000, invokeHelper("scaleSteamOutput", 2048000, 5000, 10000));
        assertEquals(2045542, invokeHelper("scaleSteamOutput", 2048000, 9988, 10000));
        assertEquals(2048000, invokeHelper("scaleSteamOutput", 2048000, 10000, 10000));
        assertEquals(Integer.MAX_VALUE, invokeHelper("scaleSteamOutput", Integer.MAX_VALUE, 10000, 10000));
        assertEquals(1, invokeHelper("scaleSteamOutput", 2048000, 0, 10000));
    }

    @Test
    public void bothBoilersDeriveSteamAndWarmupAfterTheBaseProcessingHook() throws Exception {
        for (String name : new String[] { "gregtech/common/tileentities/machines/multi/MTEThermalBoiler",
            "gtPlusPlus/xmod/gregtech/common/tileentities/machines/multi/production/MTEThermalBoilerLegacy" }) {
            ClassNode node = new ClassNode();
            try (InputStream source = getClass().getClassLoader()
                .getResourceAsStream(name + ".class")) {
                assertNotNull(name, source);
                new ClassReader(source).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
            var processing = node.methods.stream()
                .filter(method -> method.name.equals("checkProcessing"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing processing method"));
            int baseChecks = 0;
            int durationReads = 0;
            int steamWrites = 0;
            int warmupCalls = 0;
            for (var instruction : processing.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call && call.name.equals("checkProcessing")) {
                    assertEquals(Opcodes.INVOKESPECIAL, call.getOpcode());
                    assertEquals("()Lgregtech/api/recipe/check/CheckRecipeResult;", call.desc);
                    baseChecks++;
                }
                if (instruction instanceof FieldInsnNode field && field.name.equals("mMaxProgresstime")) {
                    assertEquals("Boiler must not overwrite the clamped duration", Opcodes.GETFIELD, field.getOpcode());
                    assertEquals("Duration-dependent boiler work must follow the injection", 1, baseChecks);
                    durationReads++;
                }
                if (instruction instanceof MethodInsnNode call && call.name.equals("getEfficiencyIncrease")) {
                    warmupCalls++;
                }
                if (instruction instanceof FieldInsnNode field
                    && field.owner.equals("net/minecraftforge/fluids/FluidStack")
                    && field.name.equals("amount")
                    && field.getOpcode() == Opcodes.PUTFIELD) {
                    if (steamWrites == 0) {
                        MethodInsnNode scaling = (MethodInsnNode) field.getPrevious();
                        assertEquals("java/lang/Math", scaling.owner);
                        assertEquals("max", scaling.name);
                        assertEquals("(II)I", scaling.desc);
                    } else {
                        assertEquals(
                            "Missing-water output must still be zeroed",
                            Opcodes.ICONST_0,
                            field.getPrevious()
                                .getOpcode());
                    }
                    steamWrites++;
                }
            }
            assertEquals(name, 1, baseChecks);
            assertEquals("Warmup and both steam display rates must use the clamped duration", 3, durationReads);
            assertEquals("The warmup hook must run once per successful cycle", 1, warmupCalls);
            assertEquals("Redirect only efficiency scaling, preserving the missing-water write", 2, steamWrites);
        }
    }
}
