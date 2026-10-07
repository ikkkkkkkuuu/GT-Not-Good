package com.xyp.gtnotgood.mixins.late.railcraft;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Method;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.xyp.gtnotgood.config.Config;

public class RailcraftBoilerTest {

    @Test
    public void defaultBoostAndLargeOutputsAreSafe() throws Exception {
        assertTrue(Config.railcraftBoilerInstantHeat);
        assertEquals(10F, Config.railcraftBoilerSteamMultiplier, 0F);
        Method scale = MultiblockBoilerMixin.class.getDeclaredMethod("scaleSteamOutput", int.class, float.class);
        scale.setAccessible(true);
        // Full 36-tank HP boiler produces 11,520 L per native conversion.
        assertEquals(115200, scale.invoke(null, 11520, 10F));
        assertEquals(11520, scale.invoke(null, 11520, 1F));
        assertEquals(0, scale.invoke(null, 0, 10F));
        assertEquals(Integer.MAX_VALUE, scale.invoke(null, Integer.MAX_VALUE / 2, 10F));
        assertEquals(11520, scale.invoke(null, 11520, Float.NaN));
    }

    @Test
    public void installedBoilerHasExactInjectionTargetsAndChecksFuelBeforeConversion() throws Exception {
        ClassNode boiler = readClass("mods/railcraft/common/util/steam/SteamBoiler");
        MethodNode convert = method(boiler, "convertSteam", "(I)I");
        int steamFactories = 0;
        int waterDrains = 0;
        for (AbstractInsnNode instruction : convert.instructions) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.owner.equals("mods/railcraft/common/fluids/Fluids") && call.name.equals("get")
                && call.desc.equals("(I)Lnet/minecraftforge/fluids/FluidStack;")) steamFactories++;
            if (call.name.equals("drain")) waterDrains++;
        }
        assertEquals(1, steamFactories);
        assertEquals(2, waterDrains);
        method(boiler, "increaseHeat", "(I)V");
        MethodNode tick = method(boiler, "tick", "(I)V");
        boolean checkedFuel = false;
        boolean converted = false;
        for (AbstractInsnNode instruction : tick.instructions) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.name.equals("getFuelPerCycle")) checkedFuel = true;
            if (call.name.equals("convertSteam")) {
                assertTrue(checkedFuel);
                converted = true;
            }
        }
        assertTrue(converted);
    }

    private static ClassNode readClass(String name) throws Exception {
        try (InputStream input = RailcraftBoilerTest.class.getClassLoader()
            .getResourceAsStream(name + ".class")) {
            assertNotNull(name, input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static MethodNode method(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) return method;
        }
        throw new AssertionError(name + descriptor);
    }
}
