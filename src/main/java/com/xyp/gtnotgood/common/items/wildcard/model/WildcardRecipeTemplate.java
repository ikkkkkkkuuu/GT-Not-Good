package com.xyp.gtnotgood.common.items.wildcard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.xyp.gtnotgood.common.compat.FluidDropCompat;
import com.xyp.gtnotgood.common.items.wildcard.model.io.FluidIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.SimpleIOComponent;

import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.common.items.ItemIntegratedCircuit;

public final class WildcardRecipeTemplate {

    private static final int MAX_COMPONENTS = 6;
    private static final FluidState[] FLUID_STATES = { FluidState.MOLTEN, FluidState.LIQUID, FluidState.GAS,
        FluidState.PLASMA };

    private final List<IWildcardIOComponent> inputs;
    private final List<IWildcardIOComponent> outputs;

    private WildcardRecipeTemplate(List<IWildcardIOComponent> inputs, List<IWildcardIOComponent> outputs) {
        this.inputs = Collections.unmodifiableList(inputs);
        this.outputs = Collections.unmodifiableList(outputs);
    }

    public static WildcardRecipeTemplate create(List<ItemStack> inputs, List<ItemStack> outputs) {
        return new WildcardRecipeTemplate(readIngredients(inputs, true), readIngredients(outputs, false));
    }

    public List<IWildcardIOComponent> getInputs() {
        return inputs;
    }

    public List<IWildcardIOComponent> getOutputs() {
        return outputs;
    }

    private static List<IWildcardIOComponent> readIngredients(List<ItemStack> stacks, boolean input) {
        if (stacks == null || stacks.isEmpty() || stacks.size() > MAX_COMPONENTS) {
            throw new IllegalArgumentException("A recipe side must contain between one and six components");
        }
        List<IWildcardIOComponent> ingredients = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getItem() == null || stack.stackSize < 0 || !input && stack.stackSize == 0) {
                throw new IllegalArgumentException(
                    "Recipe components must have a valid identity and positive quantity");
            }
            ItemStack copy = stack.copy();
            if (copy.stackSize == 0) {
                copy.stackSize = 1;
                ingredients.add(new SimpleIOComponent(copy, 1));
            } else ingredients.add(readIngredient(copy));
        }
        return ingredients;
    }

    private static IWildcardIOComponent readIngredient(ItemStack stack) {
        FluidStack fluid = FluidDropCompat.isFluidDrop(stack) ? FluidDropCompat.getFluidStack(stack) : null;
        if (fluid != null) {
            FluidIOComponent component = readFluid(stack, fluid);
            if (component != null) return component;
        }
        if (!(stack.getItem() instanceof ItemIntegratedCircuit)) {
            for (int oreId : OreDictionary.getOreIDs(stack)) {
                String oreName = OreDictionary.getOreName(oreId);
                OrePrefixes prefix = materialPrefix(oreName);
                if (prefix == null) continue;
                PrefixIOComponent component = new PrefixIOComponent(prefix, stack.stackSize);
                String materialName = oreName.substring(prefix.name().length());
                if (sameItemTags(stack, component.apply(materialName))) return component;
            }
            WildcardMaterials.PrefixMaterial parsed = WildcardMaterials.parseItem(stack);
            if (
                parsed.prefix != null && parsed.prefix.isMaterialBased()
                    && WildcardMaterials.isExpandableMaterial(parsed.material)
            ) {
                PrefixIOComponent component = new PrefixIOComponent(parsed.prefix, stack.stackSize);
                if (sameItemTags(stack, component.apply(parsed.material))) return component;
            }
        }
        return new SimpleIOComponent(stack, stack.stackSize);
    }

    private static OrePrefixes materialPrefix(String oreName) {
        OrePrefixes best = null;
        for (OrePrefixes prefix : OrePrefixes.VALUES) {
            String name = prefix.name();
            if (
                prefix.isMaterialBased() && oreName.startsWith(name)
                    && oreName.length() > name.length()
                    && (best == null || name.length() > best.name().length())
            ) best = prefix;
        }
        return best;
    }

    private static FluidIOComponent readFluid(ItemStack stack, FluidStack fluid) {
        FluidIOComponent match = null;
        for (Materials material : Materials.getAll()) {
            if (!WildcardMaterials.isExpandableMaterial(material)) continue;
            for (FluidState state : FLUID_STATES) {
                if (!WildcardMaterials.hasFluid(material, state)) continue;
                FluidIOComponent component = new FluidIOComponent(state, fluid.amount);
                if (!sameIdentity(stack, component.apply(material))) continue;
                if (match != null && match.getState() != state) return null;
                if (match == null) match = component;
            }
        }
        return match;
    }

    private static boolean sameIdentity(ItemStack original, ItemStack recreated) {
        return recreated != null && original.isItemEqual(recreated)
            && ItemStack.areItemStackTagsEqual(original, recreated);
    }

    private static boolean sameItemTags(ItemStack original, ItemStack recreated) {
        return recreated != null && ItemStack.areItemStackTagsEqual(original, recreated);
    }

}
