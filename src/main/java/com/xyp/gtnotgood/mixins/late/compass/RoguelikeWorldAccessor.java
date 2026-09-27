package com.xyp.gtnotgood.mixins.late.compass;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import greymerk.roguelike.worldgen.WorldEditor;

/** Exposes the generating world's identity for the dimension-local compass index. */
@Mixin(value = WorldEditor.class, remap = false)
public interface RoguelikeWorldAccessor {

    @Accessor("world")
    World gtng$getWorld();
}
