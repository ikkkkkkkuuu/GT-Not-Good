package com.xyp.gtnotgood.common.blocks.packaged;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;

/** Guards the upstream duration reads, especially the advanced controller's synthetic supplier method. */
public class AssemblyLineSpeedTargetTest {

    @Test
    public void ordinaryControllerReadsDurationOnceForProcessing() throws Exception {
        assertDurationRead("gregtech/common/tileentities/machines/multi/MTEAssemblyLine", false);
    }

    @Test
    public void advancedControllerReadsDurationOnceInsideItsSupplier() throws Exception {
        assertDurationRead("ggfab/mte/MTEAdvAssLine", true);
    }

    private static void assertDurationRead(String className, boolean synthetic) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream source = AssemblyLineSpeedTargetTest.class.getClassLoader()
            .getResourceAsStream(className + ".class")) {
            assertNotNull(className, source);
            new ClassReader(source).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        int reads = 0;
        for (var method : node.methods) {
            for (var instruction : method.instructions.toArray()) {
                if (
                    !(instruction instanceof FieldInsnNode field)
                        || !field.owner.equals("gregtech/api/util/GTRecipe$RecipeAssemblyLine")
                        || !field.name.equals("mDuration")
                ) continue;
                assertEquals("Duration must never be written by the controller", Opcodes.GETFIELD, field.getOpcode());
                assertEquals("I", field.desc);
                assertEquals(synthetic, (method.access & Opcodes.ACC_SYNTHETIC) != 0);
                assertTrue("Unexpected duration consumer: " + method.name, method.name.contains("checkProcessing"));
                reads++;
            }
        }
        assertEquals("The speed mixin requires exactly one duration read per controller", 1, reads);
    }
}
