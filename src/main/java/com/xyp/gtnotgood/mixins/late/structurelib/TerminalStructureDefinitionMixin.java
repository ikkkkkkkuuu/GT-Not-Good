package com.xyp.gtnotgood.mixins.late.structurelib;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalOperations;

@Mixin(value = IStructureDefinition.class, remap = false)
public interface TerminalStructureDefinitionMixin {

    @Inject(
        method = "iterate(Ljava/lang/Object;Lnet/minecraft/item/ItemStack;[Lcom/gtnewhorizon/structurelib/structure/IStructureElement;Lnet/minecraft/world/World;Lcom/gtnewhorizon/structurelib/alignment/enumerable/ExtendedFacing;IIIIIIZLjava/lang/Boolean;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 1,
        allow = 1)
    static void gtnotgood$captureTerminalStructure(Object context, ItemStack trigger, IStructureElement<?>[] elements,
        World world, ExtendedFacing facing, int x, int y, int z, int a, int b, int c, boolean hintsOnly,
        Boolean checkBlocks, CallbackInfoReturnable<Boolean> cir) {
        if (
            TerminalOperations.capture(context, trigger, elements, world, facing, x, y, z, a, b, c, hintsOnly,
                checkBlocks)
        ) cir.setReturnValue(true);
    }

    @Inject(
        method = "survivalBuild(Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Ljava/lang/String;Lnet/minecraft/world/World;Lcom/gtnewhorizon/structurelib/alignment/enumerable/ExtendedFacing;IIIIIIILcom/gtnewhorizon/structurelib/structure/ISurvivalBuildEnvironment;Z)I",
        at = @At("HEAD"),
        cancellable = true,
        require = 1,
        allow = 1)
    default void gtnotgood$captureTerminalSurvival(Object context, ItemStack trigger, String piece, World world,
        ExtendedFacing facing, int x, int y, int z, int a, int b, int c, int budget,
        ISurvivalBuildEnvironment environment, boolean check, CallbackInfoReturnable<Integer> cir) {
        if (
            TerminalOperations.captureSurvival(context, trigger,
                ((IStructureDefinition<?>) this).getStructureFor(piece), world, facing, x, y, z, a, b, c)
        ) cir.setReturnValue(-1);
    }

    @Inject(
        method = "iterate(Ljava/lang/Object;Lnet/minecraft/item/ItemStack;[Lcom/gtnewhorizon/structurelib/structure/IStructureElement;Lnet/minecraft/world/World;Lcom/gtnewhorizon/structurelib/alignment/enumerable/ExtendedFacing;IIIIIIZLjava/lang/Boolean;)Z",
        at = @At("RETURN"),
        require = 1)
    static void gtnotgood$captureCheckedTerminalStructure(Object context, ItemStack trigger,
        IStructureElement<?>[] elements, World world, ExtendedFacing facing, int x, int y, int z, int a, int b, int c,
        boolean hintsOnly, Boolean checkBlocks, CallbackInfoReturnable<Boolean> cir) {
        if (checkBlocks != null && cir.getReturnValue())
            TerminalOperations.captureChecked(context, trigger, elements, world, facing, x, y, z, a, b, c);
    }
}
