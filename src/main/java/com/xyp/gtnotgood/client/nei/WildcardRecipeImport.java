package com.xyp.gtnotgood.client.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;
import com.xyp.gtnotgood.common.gui.modularui.wildcard.WildcardPatternGui.RecipeImportHandler;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.util.GTRecipe;
import gregtech.nei.GTNEIDefaultHandler;

public final class WildcardRecipeImport {

    private static final int MAX_COMPONENTS = 6;

    @SubscribeEvent
    public void onRecipeButtons(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
        for (int i = 0; i < event.buttonList.size(); i++) {
            if (!(event.buttonList.get(i) instanceof GuiOverlayButton button)) continue;
            RecipeImportHandler target = target(button.firstGui);
            if (target != null && target.isValid()) event.buttonList.set(i, new ImportButton(button, target));
        }
    }

    private static RecipeImportHandler target(GuiContainer firstGui) {
        if (!(firstGui instanceof IMuiScreen screen) || screen.getScreen().getSyncManager() == null) return null;
        return screen.getScreen().getSyncManager().getMainPSM().findSyncHandlerNullable("wildcardImport",
            RecipeImportHandler.class);
    }

    private static final class ImportButton extends GuiOverlayButton {

        private final RecipeImportHandler target;
        private final RecipeData recipe;

        private ImportButton(GuiOverlayButton original, RecipeImportHandler target) {
            super(original.firstGui, original.handlerRef, original.xPosition, original.yPosition);
            this.target = target;
            recipe = readRecipe(handlerRef.handler, handlerRef.recipeIndex);
            canFillCraftingGrid = true;
            enabled = recipe.rejection == null;
        }

        @Override
        public boolean canFillCraftingGrid() {
            return recipe != null && recipe.rejection == null;
        }

        @Override
        public boolean hasOverlay() {
            return canFillCraftingGrid();
        }

        @Override
        public void setRequireShiftForOverlayRecipe(boolean require) {
            super.setRequireShiftForOverlayRecipe(false);
        }

        @Override
        protected List<ItemOverlayState> ingredientsOverlay() {
            return Collections.emptyList();
        }

        @Override
        public List<String> handleTooltip(List<String> tooltip) {
            if (recipe.rejection != null) {
                tooltip.add(recipe.rejection.text());
            } else {
                // #tr gui.wildcardpattern.nei.import
                // # Import recipe with material wildcards; save to apply
                // # zh_CN 导入配方，自动通配矿辞材料；保存后生效
                tooltip.add(StatCollector.translateToLocal("gui.wildcardpattern.nei.import"));
            }
            return tooltip;
        }

        @Override
        public void mouseReleased(int mouseX, int mouseY) {
            if (recipe.rejection != null || !target.isValid() || target(firstGui) != target) return;
            Minecraft.getMinecraft().displayGuiScreen(firstGui);
            if (target.isValid() && target(firstGui) == target) target.importRecipe(recipe.inputs, recipe.outputs);
        }
    }

    private static RecipeData readRecipe(IRecipeHandler handler, int index) {
        try {
            List<ItemStack> inputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            if (handler instanceof GTNEIDefaultHandler gtHandler) {
                if (
                    index < 0 || index >= gtHandler.arecipes.size()
                        || !(gtHandler.arecipes.get(index) instanceof GTNEIDefaultHandler.CachedDefaultRecipe cached)
                ) return RecipeData.rejected(Rejection.Unsupported);
                GTRecipe recipe = cached.mRecipe;
                if (recipe.mFakeRecipe) return RecipeData.rejected(Rejection.Unsupported);
                for (int i = 0; i < recipe.mOutputs.length; i++) {
                    if (recipe.mOutputs[i] != null && recipe.getOutputChance(i) != 10000) {
                        return RecipeData.rejected(Rejection.Chance);
                    }
                }
                for (int i = 0; i < recipe.mFluidOutputs.length; i++) {
                    if (recipe.mFluidOutputs[i] != null && recipe.getFluidOutputChance(i) != 10000) {
                        return RecipeData.rejected(Rejection.Chance);
                    }
                }
                for (ItemStack stack : recipe.mInputs) addItem(inputs, stack, true);
                for (ItemStack stack : recipe.mOutputs) addItem(outputs, stack, false);
                for (FluidStack stack : recipe.mFluidInputs) addFluid(inputs, stack);
                for (FluidStack stack : recipe.mFluidOutputs) addFluid(outputs, stack);
            } else {
                String identifier = handler.getOverlayIdentifier();
                if (!"smelting".equals(identifier)) return RecipeData.rejected(Rejection.Unsupported);
                for (PositionedStack stack : handler.getIngredientStacks(index)) {
                    if (stack != null) addItem(inputs, stack.item, false);
                }
                PositionedStack result = handler.getResultStack(index);
                if (result != null) addItem(outputs, result.item, false);
            }
            if (inputs.isEmpty() || outputs.isEmpty()) return RecipeData.rejected(Rejection.Empty);
            if (inputs.size() > MAX_COMPONENTS || outputs.size() > MAX_COMPONENTS) {
                return RecipeData.rejected(Rejection.Slots);
            }
            return new RecipeData(inputs, outputs, null);
        } catch (RuntimeException failure) {
            return RecipeData.rejected(Rejection.Unsupported);
        }
    }

    private static void addItem(List<ItemStack> stacks, ItemStack stack, boolean allowNonConsumable) {
        if (stack == null) return;
        if (stack.stackSize < 0 || (stack.stackSize == 0 && !allowNonConsumable)) {
            throw new IllegalArgumentException("Invalid recipe item amount");
        }
        ItemStack copy = stack.copy();
        stacks.add(copy);
    }

    private static void addFluid(List<ItemStack> stacks, FluidStack stack) {
        if (stack == null) return;
        ItemStack fluid = FluidDropCompat.newStack(stack.copy());
        if (fluid == null) throw new IllegalArgumentException("Invalid recipe fluid amount");
        if (stack.tag != null) fluid.getTagCompound().setTag("FluidTag", stack.tag.copy());
        stacks.add(fluid);
    }

    private static final class RecipeData {

        private final List<ItemStack> inputs;
        private final List<ItemStack> outputs;
        private final Rejection rejection;

        private RecipeData(List<ItemStack> inputs, List<ItemStack> outputs, Rejection rejection) {
            this.inputs = inputs;
            this.outputs = outputs;
            this.rejection = rejection;
        }

        private static RecipeData rejected(Rejection rejection) {
            return new RecipeData(Collections.emptyList(), Collections.emptyList(), rejection);
        }
    }

    private enum Rejection {

        Unsupported,
        Chance,
        Empty,
        Slots;

        private String text() {
            return switch (this) {
                case Unsupported -> {
                    // #tr gui.wildcardpattern.nei.unsupported
                    // # This recipe cannot be imported
                    // # zh_CN 此配方无法导入
                    yield StatCollector.translateToLocal("gui.wildcardpattern.nei.unsupported");
                }
                case Chance -> {
                    // #tr gui.wildcardpattern.nei.chance
                    // # Recipes with chance outputs cannot be imported
                    // # zh_CN 无法导入含概率产出的配方
                    yield StatCollector.translateToLocal("gui.wildcardpattern.nei.chance");
                }
                case Empty -> {
                    // #tr gui.wildcardpattern.nei.empty
                    // # This recipe needs both inputs and outputs
                    // # zh_CN 配方必须同时包含输入和输出
                    yield StatCollector.translateToLocal("gui.wildcardpattern.nei.empty");
                }
                case Slots -> {
                    // #tr gui.wildcardpattern.nei.slots
                    // # At most 6 inputs and 6 outputs; fluids share these slots
                    // # zh_CN 最多 6 项输入和 6 项输出，物品与流体共用数量限制
                    yield StatCollector.translateToLocal("gui.wildcardpattern.nei.slots");
                }
            };
        }
    }
}
