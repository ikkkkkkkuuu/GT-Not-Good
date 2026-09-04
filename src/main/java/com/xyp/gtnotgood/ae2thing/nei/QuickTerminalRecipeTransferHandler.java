package com.xyp.gtnotgood.ae2thing.nei;

import static com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config.getConfigValue;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Keyboard;

import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.glodblock.github.util.Util;
import com.xyp.gtnotgood.ae2thing.integration.Mods;
import com.xyp.gtnotgood.ae2thing.nei.object.OrderStack;
import com.xyp.gtnotgood.ae2thing.nei.recipes.FluidRecipe;
import com.xyp.gtnotgood.ae2thing.proxy.ClientProxy;
import com.xyp.gtnotgood.ae2thing.quickterminal.RecipeTransferPayload;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.util.GTUtil;

import appeng.api.AEApi;
import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.core.AELog;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;
import gregtech.common.items.ItemFluidDisplay;

/** Transfers NEI recipes directly into the native-AE2 quick terminal container. */
public final class QuickTerminalRecipeTransferHandler implements IOverlayHandler {

    public static final QuickTerminalRecipeTransferHandler INSTANCE = new QuickTerminalRecipeTransferHandler();

    private static final int CRAFTING_RECIPE_X = 25;
    private static final int CRAFTING_RECIPE_Y = 6;
    private static final int RECIPE_SLOT_SIZE = 18;

    private QuickTerminalRecipeTransferHandler() {}

    @Override
    public boolean requireShiftForOverlayRecipe() {
        return false;
    }

    @Override
    public void overlayRecipe(GuiContainer gui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        if (!(gui instanceof GuiQuickEncodingTerminal terminal)) return;

        try {
            boolean crafting = isCraftingRecipe(recipe);
            List<OrderStack<?>> namedInputs = FluidRecipe
                .getPackageInputs(recipe, recipeIndex, !crafting && terminal.shouldPrioritizeFluids());
            String interfaceSearch = getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL)
                ? interfaceSearchText(recipe, namedInputs, crafting)
                : null;
            List<OrderStack<?>> transferInputs = namedInputs;
            List<OrderStack<?>> transferOutputs = FluidRecipe
                .getPackageOutputs(recipe, recipeIndex, useOtherStacks(recipe));
            if (!crafting) {
                if (terminal.shouldCombine()) {
                    transferInputs = NEIUtils.compress(transferInputs);
                    transferOutputs = NEIUtils.compress(transferOutputs);
                }
                transferInputs = NEIUtils.clearNull(transferInputs);
                transferOutputs = NEIUtils.clearNull(transferOutputs);
                if (transferInputs.size() > RecipeTransferPayload.SLOT_COUNT) {
                    terminal.showProcessingLimit(true);
                    return;
                }
                if (transferOutputs.size() > RecipeTransferPayload.SLOT_COUNT) {
                    terminal.showProcessingLimit(false);
                    return;
                }
            }
            IAEStack<?>[] inputs = crafting ? collectCraftingInputs(recipe, recipeIndex)
                : collectStacks(transferInputs);
            IAEStack<?>[] outputs = crafting ? new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT]
                : collectStacks(transferOutputs);
            boolean hasInterchangeableInputs = hasInterchangeableInputs(recipe, recipeIndex, crafting);
            boolean encode = (shift || isAltDown()) && !hasInterchangeableInputs;

            // GT-Not-Cool has one processing layout: every processing recipe is
            // transferred as 4x4, while crafting recipes keep their shaped 3x3 positions.
            terminal.transferRecipe(
                new RecipeTransferPayload(crafting, encode, 4, false, inputs, outputs),
                interfaceSearch);
        } catch (RuntimeException | LinkageError failure) {
            AELog.warn(failure, "Failed to transfer an NEI recipe to the GT-Not-Cool quick terminal");
        }
    }

    private static String interfaceSearchText(IRecipeHandler recipe, List<OrderStack<?>> inputs, boolean crafting) {
        if (crafting) {
            ItemStack assembler = AEApi.instance()
                .definitions()
                .blocks()
                .molecularAssembler()
                .maybeStack(1)
                .orNull();
            return assembler == null ? recipe.getRecipeName() : Platform.getItemDisplayName(assembler);
        }
        if (Mods.isGt5UnofficialLoaded() || Mods.isLegacyGt5Loaded()) {
            // Preserve the old terminal's configured circuit/mold suffix rules.
            // Those suffixes match GT-Not-Cool's custom input-assembly interface names.
            return GTUtil.getRecipeName(recipe, inputs);
        }
        return recipe.getRecipeName();
    }

    private static boolean isCraftingRecipe(IRecipeHandler recipe) {
        String identifier = recipe.getOverlayIdentifier();
        if (identifier == null) return false;
        identifier = identifier.toLowerCase(Locale.ROOT);
        return "crafting".equals(identifier) || "crafting2x2".equals(identifier);
    }

    private static boolean useOtherStacks(IRecipeHandler recipe) {
        String identifier = recipe.getOverlayIdentifier();
        return !"smelting".equals(identifier) && !"brewing".equals(identifier);
    }

    private static IAEStack<?>[] collectCraftingInputs(IRecipeHandler recipe, int recipeIndex) {
        IAEStack<?>[] result = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
        int fallbackSlot = 0;
        for (PositionedStack positioned : safeList(recipe.getIngredientStacks(recipeIndex))) {
            if (positioned == null) continue;
            int column = (positioned.relx - CRAFTING_RECIPE_X) / RECIPE_SLOT_SIZE;
            int row = (positioned.rely - CRAFTING_RECIPE_Y) / RECIPE_SLOT_SIZE;
            int slot = column >= 0 && column < 3 && row >= 0 && row < 3 ? column + row * 3 : fallbackSlot;
            fallbackSlot = Math.max(fallbackSlot, slot + 1);
            if (slot >= 9) continue;
            IAEStack<?> stack = toAEStack(positioned, false);
            if (stack != null) result[slot] = stack.setStackSize(1);
        }
        return result;
    }

    private static IAEStack<?>[] collectStacks(List<OrderStack<?>> ordered) {
        IAEStack<?>[] result = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
        if (ordered == null) return result;
        int fallbackSlot = 0;
        for (OrderStack<?> order : ordered) {
            if (order == null) continue;
            int slot = order.getIndex();
            if (slot < 0 || slot >= result.length) slot = fallbackSlot;
            fallbackSlot = Math.max(fallbackSlot, slot + 1);
            if (slot >= result.length) break;
            result[slot] = toAEStack(order.getStack());
        }
        return result;
    }

    private static IAEStack<?> toAEStack(Object stack) {
        if (stack instanceof ItemStack item) return toProcessingStack(item);
        if (stack instanceof FluidStack fluid) return AEFluidStack.create(fluid.copy());
        return null;
    }

    private static IAEStack<?> toAEStack(PositionedStack positioned, boolean allowFluid) {
        if (positioned == null) return null;
        ItemStack item = positioned.item;
        if (item == null && positioned.items != null) {
            for (ItemStack alternative : positioned.items) {
                if (alternative != null) {
                    item = alternative;
                    break;
                }
            }
        }
        if (item == null) return null;
        ItemStack copy = item.copy();
        return allowFluid ? toProcessingStack(copy) : AEItemStack.create(copy);
    }

    private static boolean hasInterchangeableInputs(IRecipeHandler recipe, int recipeIndex, boolean crafting) {
        for (PositionedStack positioned : safeList(recipe.getIngredientStacks(recipeIndex))) {
            if (positioned == null || positioned.items == null || positioned.items.length < 2) continue;
            IAEStack<?> firstType = null;
            for (ItemStack alternative : positioned.items) {
                if (alternative == null) continue;
                ItemStack copy = alternative.copy();
                IAEStack<?> candidate = crafting ? AEItemStack.create(copy) : toProcessingStack(copy);
                if (candidate == null) continue;
                candidate.setStackSize(1);
                if (firstType == null) {
                    firstType = candidate;
                } else if (!firstType.toNBTGeneric()
                    .equals(candidate.toNBTGeneric())) {
                        return true;
                    }
            }
        }
        return false;
    }

    private static List<PositionedStack> safeList(List<PositionedStack> stacks) {
        return stacks == null ? java.util.Collections.emptyList() : stacks;
    }

    private static boolean isAltDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    /** Restores the old terminal's Shift+wheel substitution for the new virtual pattern slots. */
    public static boolean cycleRecipeIngredient(GuiQuickEncodingTerminal terminal, int wheel) {
        GuiOverlayButton overlay = ClientProxy.getOverlayButton();
        IAEStack<?> current = terminal.getHoveredRecipeInput();
        if (overlay == null || current == null || current.getStackSize() <= 0) return false;

        int direction = wheel < 0 ? 1 : -1;
        List<PositionedStack> ingredients = safeList(
            overlay.handlerRef.handler.getIngredientStacks(overlay.handlerRef.recipeIndex));
        for (PositionedStack ingredient : ingredients) {
            IAEStack<?> replacement = findAdjacentAlternative(
                ingredient,
                current,
                direction,
                terminal.isCraftingEncodingMode());
            if (replacement == null) continue;
            replacement.setStackSize(terminal.isCraftingEncodingMode() ? 1 : current.getStackSize());
            terminal.replaceRecipeIngredient(current, replacement);
            return true;
        }
        return false;
    }

    private static IAEStack<?> findAdjacentAlternative(PositionedStack ingredient, IAEStack<?> current, int direction,
        boolean crafting) {
        if (ingredient == null || ingredient.items == null || ingredient.items.length < 2) return null;

        int currentIndex = -1;
        for (int index = 0; index < ingredient.items.length; index++) {
            IAEStack<?> candidate = toAlternativeStack(ingredient.items[index], crafting);
            if (sameIngredientType(candidate, current)) {
                currentIndex = index;
                break;
            }
        }
        if (currentIndex < 0) return null;

        for (int step = 1; step < ingredient.items.length; step++) {
            int index = Math.floorMod(currentIndex + direction * step, ingredient.items.length);
            IAEStack<?> candidate = toAlternativeStack(ingredient.items[index], crafting);
            if (candidate != null && !sameIngredientType(candidate, current)) return candidate;
        }
        return null;
    }

    private static IAEStack<?> toAlternativeStack(ItemStack item, boolean crafting) {
        if (item == null) return null;
        ItemStack copy = item.copy();
        return crafting ? AEItemStack.create(copy) : toProcessingStack(copy);
    }

    private static boolean sameIngredientType(IAEStack<?> first, IAEStack<?> second) {
        return first != null && second != null
            && first.getStackType() == second.getStackType()
            && first.isSameType((Object) second);
    }

    /** Converts NEI's display item into any AE stack type registered by integrations, including essentia. */
    private static IAEStack<?> toProcessingStack(ItemStack item) {
        if (item == null) return null;

        // Filled cells, buckets, and other real containers are item ingredients.
        // Only NEI's synthetic fluid representations belong in an AE fluid slot.
        IAEStack<?> fluid = isFluidDisplay(item) ? Util.getAEFluidFromItem(item) : null;
        if (fluid != null) return fluid;

        for (IAEStackType<?> type : AEStackTypeRegistry.getSortedTypes()) {
            if ("item".equals(type.getId()) || "fluid".equals(type.getId())) continue;
            IAEStack<?> converted = type.convertStackFromItem(item);
            if (converted != null) return converted;
        }
        return AEItemStack.create(item.copy());
    }

    private static boolean isFluidDisplay(ItemStack stack) {
        return stack.getItem() instanceof ItemFluidDisplay || stack.getItem() instanceof ItemFluidDrop
            || stack.getItem() instanceof ItemFluidPacket;
    }
}
