package com.xyp.gtnotgood.common.blocks.packaged;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Guards the fixed cycle injection and the upstream handoff of its duration to linked units. */
public class PurificationPlantSpeedTargetTest {

    @Test
    public void normalCycleIsAssignedBeforeLinkedUnitsStart() throws Exception {
        String plant = "gregtech/common/tileentities/machines/multi/purification/MTEPurificationPlant";
        ClassNode node = new ClassNode();
        try (InputStream source = getClass().getClassLoader()
            .getResourceAsStream(plant + ".class")) {
            assertNotNull(source);
            new ClassReader(source).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        var cycle = node.methods.stream()
            .filter(method -> method.name.equals("startCycle") && method.desc.equals("()V"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing plant startCycle()"));
        int normalCycles = 0;
        int debugCycles = 0;
        int handoffs = 0;
        for (var instruction : cycle.instructions.toArray()) {
            if (instruction instanceof IntInsnNode constant && constant.operand == 2400) {
                normalCycles++;
                assertTrue(constant.getNext() instanceof FieldInsnNode);
                FieldInsnNode assignment = (FieldInsnNode) constant.getNext();
                assertEquals(Opcodes.PUTFIELD, assignment.getOpcode());
                assertEquals("mMaxProgresstime", assignment.name);
            }
            if (instruction instanceof IntInsnNode constant && constant.operand == 600) {
                debugCycles++;
            }
            if (instruction instanceof MethodInsnNode call && call.name.equals("startCycle")
                && call.owner.equals("gregtech/common/tileentities/machines/multi/purification/MTEPurificationUnitBase")) {
                assertEquals("(II)V", call.desc);
                assertEquals("The normal cycle must be set before starting units", 1, normalCycles);
                FieldInsnNode progress = (FieldInsnNode) call.getPrevious();
                FieldInsnNode duration = (FieldInsnNode) progress.getPrevious()
                    .getPrevious();
                assertEquals(Opcodes.GETFIELD, duration.getOpcode());
                assertEquals("mMaxProgresstime", duration.name);
                assertEquals(Opcodes.GETFIELD, progress.getOpcode());
                assertEquals("mProgresstime", progress.name);
                handoffs++;
            }
        }
        assertEquals("The mixin must match exactly one normal cycle constant", 1, normalCycles);
        assertEquals("Debug mode must have a separate duration", 1, debugCycles);
        assertEquals("All units must start from the plant's shared duration and progress", 1, handoffs);
    }
}
