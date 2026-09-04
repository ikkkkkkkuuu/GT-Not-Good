package com.xyp.gtnotgood.ae2thing.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.xyp.gtnotgood.ae2thing.integration.Mods;
import com.xyp.gtnotgood.ae2thing.nei.ButtonConstants;
import com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config;
import com.xyp.gtnotgood.ae2thing.nei.object.OrderStack;
import com.xyp.gtnotgood.ae2thing.quickterminal.RecipeTransferPayload;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;

import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import blockrenderer6343.client.renderer.WorldSceneRenderer;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.GuiRecipe;

/**
 * Hooks BlockRenderer6343's multiblock structure preview so the NEI overlay button can fill the quick terminal's
 * processing-pattern inputs and use a renamed paper as the placeholder output.
 */
public class BRUtil {

    public static final ItemStack paper = new ItemStack(Items.paper);

    private static String multiBlockName = "";

    public interface ITransferHandler {

        ImmutablePair<List<OrderStack<?>>, List<OrderStack<?>>> handler(List<ItemStack> ingredients);
    }

    public static final ITransferHandler handler = ingredients -> {
        String defaultName = StatCollector.translateToLocal("blockrenderer6343.multiblock.structure");
        List<OrderStack<?>> in = new ArrayList<>();
        List<OrderStack<?>> out = new ArrayList<>();
        ItemStack item;
        for (int i = 0; i < ingredients.size(); i++) {
            item = ingredients.get(i);
            if (!((Mods.isGt5UnofficialLoaded() || Mods.isLegacyGt5Loaded())
                && NEI_TH_Config.getConfigValue(ButtonConstants.BLOCK_RENDER)
                && GTUtil.isHatchItem(item))) {
                in.add(new OrderStack<>(item, i));
            }
        }
        try {
            ItemStack object = paper.copy();
            String name = ((GuiRecipe<?>) Minecraft.getMinecraft().currentScreen).getHandler()
                .getRecipeName();
            object.setStackDisplayName(name.equals(defaultName) ? multiBlockName : name);
            out.add(new OrderStack<>(object, 0));
        } catch (Exception ignored) {}
        return new ImmutablePair<>(in, out);
    };

    public static void setMultiBlockName(String name) {
        BRUtil.multiBlockName = name;
    }

    public static String getMultiBlockName() {
        return BRUtil.multiBlockName;
    }

    public static boolean sendToServer(List<ItemStack> ingredients) {
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiRecipe<?>recipeScreen)
            || !(recipeScreen.firstGui instanceof GuiQuickEncodingTerminal terminal)) return false;
        try {
            ImmutablePair<List<OrderStack<?>>, List<OrderStack<?>>> result = handler.handler(ingredients);
            if (result.left.size() > RecipeTransferPayload.SLOT_COUNT) {
                terminal.showProcessingLimit(true);
                return true;
            }
            if (result.right.size() > RecipeTransferPayload.SLOT_COUNT) {
                terminal.showProcessingLimit(false);
                return true;
            }
            String interfaceSearch = null;
            if (NEI_TH_Config.getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL)) {
                // #tr gtnotgood.gui.multiblock_structure
                // # Multiblock Structure
                // # zh_CN 多方块结构
                interfaceSearch = StatCollector.translateToLocal(NameConst.GUI_MULTIBLOCK_STRUCTURE);
            }
            terminal.transferRecipe(
                new RecipeTransferPayload(
                    false,
                    GuiScreen.isShiftKeyDown(),
                    4,
                    false,
                    toAEStacks(result.left),
                    toAEStacks(result.right)),
                interfaceSearch);
            Minecraft.getMinecraft()
                .displayGuiScreen(recipeScreen.firstGui);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static IAEStack<?>[] toAEStacks(List<OrderStack<?>> stacks) {
        IAEStack<?>[] result = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
        for (int index = 0; index < stacks.size(); index++) {
            Object stack = stacks.get(index)
                .getStack();
            if (stack instanceof ItemStack item) {
                result[index] = AEItemStack.create(item.copy());
            } else if (stack instanceof FluidStack fluid) {
                result[index] = AEFluidStack.create(fluid.copy());
            }
        }
        return result;
    }

    public static List<ItemStack> getIngredients(WorldSceneRenderer renderer) {
        List<ItemStack> ingredients = new ArrayList<>();
        for (long renderedBlock : renderer.renderedBlocks) {
            int x = CoordinatePacker.unpackX(renderedBlock);
            int y = CoordinatePacker.unpackY(renderedBlock);
            int z = CoordinatePacker.unpackZ(renderedBlock);
            Block block = renderer.world.getBlock(x, y, z);
            if (block.equals(Blocks.air)) continue;
            int meta = renderer.world.getBlockMetadata(x, y, z);
            int qty = block.quantityDropped(renderer.world.rand);
            ArrayList<ItemStack> itemStacks = new ArrayList<>();
            if (qty != 1) {
                itemStacks.add(new ItemStack(block));
            } else {
                itemStacks = block.getDrops(renderer.world, x, y, z, meta, 0);
            }
            if (itemStacks.isEmpty()) continue;
            boolean added = false;
            for (ItemStack ingredient : ingredients) {
                if (NEIClientUtils.areStacksSameTypeWithNBT(ingredient, itemStacks.get(0))) {
                    ingredient.stackSize++;
                    added = true;
                    break;
                }
            }
            if (!added) ingredients.add(itemStacks.get(0));
        }
        return ingredients;
    }
}
