package com.xyp.gtnotgood.ae2thing.nei.recipes.extractor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.ae2thing.nei.object.IRecipeExtractor;
import com.xyp.gtnotgood.ae2thing.nei.object.OrderStack;

import codechicken.nei.PositionedStack;
import gregtech.api.enums.ItemList;
import gregtech.common.items.ItemFluidDisplay;

public class GT5RecipeExtractor implements IRecipeExtractor {

    boolean removeSpecial;

    public GT5RecipeExtractor(boolean removeSpecial) {
        this.removeSpecial = removeSpecial;
    }

    @Override
    public List<OrderStack<?>> getInputIngredients(List<PositionedStack> rawInputs) {
        if (removeSpecial) removeSpecial(rawInputs);
        return packOrderStacks(rawInputs);
    }

    @Override
    public List<OrderStack<?>> getOutputIngredients(List<PositionedStack> rawOutputs) {
        return packOrderStacks(rawOutputs);
    }

    public static List<OrderStack<?>> packOrderStacks(List<PositionedStack> items) {
        List<OrderStack<?>> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            PositionedStack stack = items.get(i);
            if (stack == null || stack.items == null || stack.items.length == 0) continue;
            // Check for fluid display items BEFORE OrderStack.pack() which would wrap them as items
            Object o = getFluidFromDisplay(stack);
            if (o instanceof FluidStack fs) {
                result.add(new OrderStack<>(fs, i));
                continue;
            }
            OrderStack<?> orderStack = OrderStack.pack(stack, i);
            if (orderStack != null) {
                result.add(orderStack);
            }
        }
        return result;
    }

    public static Object getFluidFromDisplay(PositionedStack stack) {
        if (stack != null && stack.items.length > 0) {
            ItemStack item = stack.items[0].copy();
            if (item.getItem() instanceof ItemFluidDisplay) {
                if (item.getTagCompound() != null) {
                    Fluid fluid = FluidRegistry.getFluid(item.getItemDamage());
                    int amt = (int) item.getTagCompound()
                        .getLong("mFluidDisplayAmount");
                    return amt > 0 && fluid != null ? new FluidStack(fluid, amt) : null;
                }
            } else {
                return item;
            }
        }
        return null;
    }

    private void removeSpecial(List<PositionedStack> list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            PositionedStack positionedStack = list.get(i);
            if (positionedStack != null && positionedStack.items.length > 0) {
                ItemStack item = positionedStack.items[0];
                if (ItemList.Tool_DataStick.isStackEqual(item, false, true)
                    || ItemList.Tool_DataOrb.isStackEqual(item, false, true)) {
                    list.remove(i);
                    break;
                }
            }
        }
    }
}
