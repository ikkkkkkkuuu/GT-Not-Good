package com.xyp.gtnotgood.common.packaged;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Guards the shared success exit after both ritual timing and ordinary batch/overclock calculations. */
public class ExtremeEntityCrusherSpeedTargetTest {

    @Test
    public void bothProcessingBranchesFinishAtOneSuccessReturn() throws Exception {
        String crusher = "kubatech/tileentity/gregtech/multiblock/MTEExtremeEntityCrusher";
        ClassNode node = new ClassNode();
        try (InputStream source = getClass().getClassLoader()
            .getResourceAsStream(crusher + ".class")) {
            assertNotNull(source);
            new ClassReader(source).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        var processing = node.methods.stream()
            .filter(method -> method.name.equals("checkProcessing"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing checkProcessing()"));
        int durationWrites = 0;
        int overclocks = 0;
        int successReturns = 0;
        for (var instruction : processing.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.name.equals("mMaxProgresstime")
                && field.getOpcode() == Opcodes.PUTFIELD) {
                durationWrites++;
            }
            if (instruction instanceof MethodInsnNode call) {
                assertFalse("Mob processing must not also apply the ordinary recipe speed hook",
                    call.owner.equals("gregtech/api/recipe/RecipeMapBackend") && call.name.equals("compileRecipe"));
                if (call.name.equals("calculatePerfectOverclock")) overclocks++;
            }
            if (instruction instanceof FieldInsnNode field && field.name.equals("SUCCESSFUL")
                && field.owner.equals("gregtech/api/recipe/check/CheckRecipeResultRegistry")) {
                assertEquals(Opcodes.ARETURN, field.getNext().getOpcode());
                assertEquals("Ritual timing and batch scaling must precede success", 2, durationWrites);
                assertEquals("Ordinary overclocking must precede success", 1, overclocks);
                successReturns++;
            }
        }
        assertEquals("One success exit must cover both branches", 1, successReturns);
    }
}
