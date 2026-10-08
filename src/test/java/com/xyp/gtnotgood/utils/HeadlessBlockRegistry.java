package com.xyp.gtnotgood.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.material.Material;

/** Seeds native vanilla blocks before the static Blocks fields are first read in headless tests. */
public final class HeadlessBlockRegistry {

    private HeadlessBlockRegistry() {}

    /** Registers air and fluids without a Forge mod container, preserving any existing registry objects. */
    public static void bootstrap() throws ReflectiveOperationException {
        Method register = Block.blockRegistry.getClass().getDeclaredMethod("addObjectRaw", int.class, String.class,
            Object.class);
        register.setAccessible(true);
        Constructor<BlockAir> air = BlockAir.class.getDeclaredConstructor();
        air.setAccessible(true);
        register(register, 0, "air", air.newInstance());
        Constructor<BlockDynamicLiquid> flowing = BlockDynamicLiquid.class.getDeclaredConstructor(Material.class);
        flowing.setAccessible(true);
        Constructor<BlockStaticLiquid> still = BlockStaticLiquid.class.getDeclaredConstructor(Material.class);
        still.setAccessible(true);
        register(register, 8, "flowing_water", flowing.newInstance(Material.water));
        register(register, 9, "water", still.newInstance(Material.water));
        register(register, 10, "flowing_lava", flowing.newInstance(Material.lava));
        register(register, 11, "lava", still.newInstance(Material.lava));
    }

    private static void register(Method register, int id, String name, Block block)
        throws ReflectiveOperationException {
        String key = "minecraft:" + name;
        if (!Block.blockRegistry.containsKey(key))
            register.invoke(Block.blockRegistry, id, key, block.setBlockName(name));
    }
}
