package com.xyp.gtnotgood.mixins.late.CutCorners;

import static org.junit.Assert.*;

import java.io.InputStream;

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
            assertEquals(1, ThermalBoilerSpeedMixin.limitBatchDuration(128));
            Config.recipeSpeedFixedDuration = 5;
            assertEquals(5, ThermalBoilerSpeedMixin.limitBatchDuration(128));
            assertEquals(1, ThermalBoilerSpeedMixin.limitBatchDuration(1));
            assertEquals(0, ThermalBoilerSpeedMixin.limitBatchDuration(0));
            Config.recipeSpeedMode = 2;
            assertEquals(256, ThermalBoilerSpeedMixin.limitBatchDuration(256));
            Config.recipeSpeedMode = 0;
            assertEquals(2560, ThermalBoilerSpeedMixin.limitBatchDuration(2560));
        } finally {
            Config.recipeSpeedMode = mode;
            Config.recipeSpeedFixedDuration = fixed;
        }
    }

    @Test
    public void bothBoilersDeriveSteamAndWarmupAfterTheBaseProcessingHook() throws Exception {
        for (String name : new String[] { "gregtech/common/tileentities/machines/multi/MTEThermalBoiler",
            "gtPlusPlus/xmod/gregtech/common/tileentities/machines/multi/production/MTEThermalBoilerLegacy" }) {
            ClassNode node = new ClassNode();
            try (InputStream source = getClass().getClassLoader().getResourceAsStream(name + ".class")) {
                assertNotNull(name, source);
                new ClassReader(source).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
            var processing = node.methods.stream()
                .filter(method -> method.name.equals("checkProcessing"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing processing method"));
            int baseChecks = 0;
            int durationReads = 0;
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
            }
            assertEquals(name, 1, baseChecks);
            assertEquals("Warmup and both steam display rates must use the clamped duration", 3, durationReads);
        }
    }
}
