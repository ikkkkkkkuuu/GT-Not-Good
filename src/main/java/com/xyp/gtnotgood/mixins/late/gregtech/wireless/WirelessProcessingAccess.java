package com.xyp.gtnotgood.mixins.late.gregtech.wireless;

import java.util.stream.Stream;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import gregtech.api.interfaces.tileentity.IVoidable;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GTRecipe;

@Mixin(value = ProcessingLogic.class, remap = false)
public interface WirelessProcessingAccess {

    @Accessor("machine")
    IVoidable gtng$getMachine();

    @Accessor("inputItems")
    ItemStack[] gtng$getItems();

    @Accessor("inputFluids")
    FluidStack[] gtng$getFluids();

    @Accessor("calculatedParallels")
    void gtng$setParallels(int value);

    @Invoker("getCurrentRecipeMap")
    RecipeMap<?> gtng$recipeMap();

    @Invoker("findRecipeMatches")
    Stream<GTRecipe> gtng$matches(RecipeMap<?> map);

    @Invoker("prepareCatalyst")
    ItemStack[] gtng$prepareCatalyst(ItemStack[] items);

    @Invoker("validateRecipe")
    CheckRecipeResult gtng$validate(GTRecipe recipe);

    @Invoker("onRecipeStart")
    CheckRecipeResult gtng$start(GTRecipe recipe);
}
