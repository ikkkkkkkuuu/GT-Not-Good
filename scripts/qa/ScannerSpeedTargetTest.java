package com.xyp.gtnotgood.mixins.late.CutCorners;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Verifies the shared runtime duration read after scanner requirements and before overclocking. */
public class ScannerSpeedTargetTest {

    @Test
    public void dynamicResultsAreValidatedBeforeTheirSingleOverclockDurationRead() throws Exception {
        ClassNode node = read("gregtech/common/tileentities/machines/basic/MTEScanner");
        var processing = node.methods.stream()
            .filter(method -> method.name.equals("checkRecipe") && method.desc.equals("()I"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing scanner checkRecipe()"));
        int validations = 0;
        int voltageChecks = 0;
        int durationReads = 0;
        int overclocks = 0;
        int registryQueries = 0;
        for (var instruction : processing.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("isNotMet")) validations++;
                if (call.name.equals("findRecipeWithCache")) registryQueries++;
                if (call.name.equals("calculateOverclockedNess")) {
                    assertEquals("(II)V", call.desc);
                    assertEquals(1, durationReads);
                    FieldInsnNode duration = (FieldInsnNode) call.getPrevious();
                    assertEquals("duration", duration.name);
                    assertEquals("gregtech/api/util/GTScannerResult", duration.owner);
                    overclocks++;
                }
                assertFalse(
                    "Dynamic scans must not pass through the registration speed hook",
                    call.owner.equals("gregtech/api/recipe/RecipeMapBackend") && call.name.equals("compileRecipe"));
            }
            if (!(instruction instanceof FieldInsnNode field)) continue;
            if (field.owner.equals("gregtech/api/util/GTScannerResult$ALScannerResult") && field.name.equals("eut")) {
                voltageChecks++;
            }
            if (field.owner.equals("gregtech/api/util/GTScannerResult") && field.name.equals("duration")) {
                assertEquals("Cached results must not be modified", Opcodes.GETFIELD, field.getOpcode());
                assertEquals("I", field.desc);
                assertEquals("Invalid results must be rejected before applying speed", 1, validations);
                assertEquals("Research voltage gate must precede speed", 1, voltageChecks);
                durationReads++;
            }
        }
        assertEquals(1, registryQueries);
        assertEquals("All handlers must share one duration read", 1, durationReads);
        assertEquals(1, overclocks);
    }

    @Test
    public void assemblyLineScanResultsShareTheNativeDurationContract() throws Exception {
        ClassNode node = read("gregtech/api/util/GTScannerResult$ALScannerResult");
        assertEquals("gregtech/api/util/GTScannerResult", node.superName);
        assertFalse(
            node.fields.stream()
                .anyMatch(field -> field.name.equals("duration")));
    }

    private static ClassNode read(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream source = ScannerSpeedTargetTest.class.getClassLoader()
            .getResourceAsStream(name + ".class")) {
            assertNotNull(name, source);
            new ClassReader(source).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return node;
    }
}
