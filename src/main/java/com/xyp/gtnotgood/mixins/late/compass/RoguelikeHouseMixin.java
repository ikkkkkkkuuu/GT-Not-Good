package com.xyp.gtnotgood.mixins.late.compass;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.items.compass.StructureLocations;

import greymerk.roguelike.dungeon.towers.HouseTower;
import greymerk.roguelike.theme.ITheme;
import greymerk.roguelike.worldgen.Coord;
import greymerk.roguelike.worldgen.IWorldEditor;

/** Records completed HOUSE entrances, excluding other Roguelike tower designs. */
@Mixin(value = HouseTower.class, remap = false)
public abstract class RoguelikeHouseMixin {

    @Inject(method = "generate", at = @At("RETURN"), remap = false)
    private void gtng$recordHouse(IWorldEditor editor, Random random, ITheme theme, Coord origin, CallbackInfo ci) {
        if (!(editor instanceof RoguelikeWorldAccessor)) return;
        World world = ((RoguelikeWorldAccessor) editor).gtng$getWorld();
        if (world.isRemote) return;
        // A custom HOUSE theme can be non-brick; inspect the finished wall next to the entrance shaft.
        int x = origin.getX(), z = origin.getZ();
        for (int y = 64; y <= 140; y++) {
            if (world.getBlock(x + 2, y, z + 2) == Blocks.brick_block) {
                StructureLocations.get(world)
                    .add(0, x, y, z);
                return;
            }
        }
    }
}
