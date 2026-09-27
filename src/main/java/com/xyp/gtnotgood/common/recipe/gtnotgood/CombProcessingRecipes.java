package com.xyp.gtnotgood.common.recipe.gtnotgood;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.common.items.ItemComb;

/** Imports upstream comb conversions after mods have populated the native recipe maps. */
public class CombProcessingRecipes {

    private static final RecipeMap<?> RM = GTNGRecipeMaps.CombProcessingRecipes;
    /** Forestry IItemBeeComb 接口，用于检测非 GT 的 Forestry 体系蜂窝 */
    private static Class<?> forestryCombInterface;
    private static boolean forestryChecked;
    /** 跨源去重：同种蜂窝只保留先遇到的 */
    private static final Set<String> seenCombs = new HashSet<>();

    private static boolean loaded;

    /** Registers the imported recipes once, during Forge load-complete. */
    public static void loadRecipes() {
        if (loaded) return;
        loaded = true;
        seenCombs.clear();
        GTNotGood.LOG.info("Loading Comb Processing Recipes...");
        int count = 0;
        count += importFromCentrifuge();
        count += importFromChemical();
        count += importFromFluidExtractor();
        GTNotGood.LOG.info("Loaded {} Comb Processing Recipes", count);
    }

    // ==================== 离心机 → 直接复制 ====================

    private static int importFromCentrifuge() {
        Collection<GTRecipe> recipes = RecipeMaps.centrifugeRecipes.getAllRecipes();
        int count = 0;
        for (GTRecipe r : recipes) {
            if (r.mInputs == null || r.mInputs.length == 0) continue;
            if (!isComb(r.mInputs[0])) continue;
            if (!seenCombs.add(combId(r.mInputs[0]))) continue;
            // 粉尘配方：直接复制离心机产物（保留原输入数量）
            GTRecipeBuilder.builder()
                .itemInputs(r.mInputs[0])
                .itemOutputs(r.mOutputs)
                .outputChances(r.mOutputChances)
                .fluidOutputs(r.mFluidOutputs)
                .eut(30)
                .duration(r.mDuration)
                .addTo(RM);
            count++;
        }
        return count;
    }

    // ==================== 化学反应釜(LCR nocell) → 去酸转粉 ====================

    private static int importFromChemical() {
        Collection<GTRecipe> recipes = RecipeMaps.multiblockChemicalReactorRecipes.getAllRecipes();
        // 去重：同种蜂窝被不同等级酸处理时，保留产出最高者
        Map<String, GTRecipe> best = new HashMap<>();
        for (GTRecipe r : recipes) {
            if (r.mInputs == null || r.mInputs.length == 0) continue;
            if (!isComb(r.mInputs[0])) continue;
            if (r.mFluidInputs == null || r.mFluidInputs.length == 0) continue;
            String key = combId(r.mInputs[0]);
            GTRecipe existing = best.get(key);
            if (existing == null || outputPerComb(r) > outputPerComb(existing)) {
                best.put(key, r);
            }
        }
        int count = 0;
        for (GTRecipe r : best.values()) {
            if (!seenCombs.add(combId(r.mInputs[0]))) continue;
            ItemStack comb = copyAmount(r.mInputs[0], 1);
            ItemStack[] outputs = convertOutputsToDust(r.mOutputs);
            GTRecipeBuilder.builder()
                .itemInputs(comb)
                .itemOutputs(outputs)
                .eut(30)
                .duration(100)
                .addTo(RM);
            count++;
        }
        return count;
    }

    // ==================== 流体提取机 → 直接复制 ====================

    private static int importFromFluidExtractor() {
        Collection<GTRecipe> recipes = RecipeMaps.fluidExtractionRecipes.getAllRecipes();
        int count = 0;
        for (GTRecipe r : recipes) {
            if (r.mInputs == null || r.mInputs.length == 0) continue;
            if (!isComb(r.mInputs[0])) continue;
            if (!seenCombs.add(combId(r.mInputs[0]))) continue;
            GTRecipeBuilder.builder()
                .itemInputs(r.mInputs[0])
                .fluidOutputs(r.mFluidOutputs)
                .eut(30)
                .duration(r.mDuration)
                .addTo(RM);
            count++;
        }
        return count;
    }

    // ==================== helpers ====================

    private static boolean isComb(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        Item item = stack.getItem();
        // GT 蜂窝：ItemComb 实例
        if (item instanceof ItemComb) return true;
        // Forestry 接口检测：IItemBeeComb
        if (isForestryComb(item)) return true;
        // 类名回退：Forestry / MagicBees / ExtraBees 等模组的蜂窝，
        // 在 GTNH 中可能既不是 ItemComb 也不实现 IItemBeeComb，但类名必含 "Comb"
        if (isCombByClassName(item)) return true;
        return false;
    }

    private static boolean isForestryComb(Item item) {
        if (!forestryChecked) {
            try {
                forestryCombInterface = Class.forName("forestry.api.apiculture.IItemBeeComb");
            } catch (ClassNotFoundException ignored) {}
            forestryChecked = true;
        }
        if (forestryCombInterface == null) return false;
        return forestryCombInterface.isInstance(item);
    }

    /** 类名回退检测：所有蜂类模组的蜂窝 item 类名都包含 "Comb" */
    private static boolean isCombByClassName(Item item) {
        return item.getClass()
            .getSimpleName()
            .toLowerCase(java.util.Locale.ROOT)
            .contains("comb");
    }

    private static ItemStack[] convertOutputsToDust(ItemStack[] outputs) {
        if (outputs == null) return null;
        ItemStack[] result = new ItemStack[outputs.length];
        for (int i = 0; i < outputs.length; i++) {
            result[i] = convertToDust(outputs[i]);
        }
        return result;
    }

    private static ItemStack convertToDust(ItemStack stack) {
        if (stack == null) return null;
        ItemData assoc = GTOreDictUnificator.getAssociation(stack);
        if (assoc != null && assoc.mPrefix == OrePrefixes.crushedPurified
            && assoc.mMaterial != null
            && assoc.mMaterial.mMaterial != null) {
            return GTOreDictUnificator.get(OrePrefixes.dust, assoc.mMaterial.mMaterial, stack.stackSize);
        }
        // nugget → dust (Platinum, Osmium, Iridium, Neutronium 等特殊金属)
        if (assoc != null && assoc.mPrefix == OrePrefixes.nugget
            && assoc.mMaterial != null
            && assoc.mMaterial.mMaterial != null) {
            int dustCount = Math.max(1, stack.stackSize / 9);
            return GTOreDictUnificator.get(OrePrefixes.dust, assoc.mMaterial.mMaterial, dustCount);
        }
        return stack;
    }

    /** 蜂窝唯一标识：itemId:damage */
    private static String combId(ItemStack stack) {
        return Item.getIdFromItem(stack.getItem()) + ":" + stack.getItemDamage();
    }

    /** 计算每蜂窝产出（dust 等效量），按输入数量归一化 */
    private static float outputPerComb(GTRecipe recipe) {
        int inputCount = recipe.mInputs[0].stackSize;
        if (inputCount <= 0) inputCount = 1;
        return (float) totalOutput(recipe) / inputCount;
    }

    /** 计算配方总产出（dust 等效量），含熔融金属流体 */
    private static int totalOutput(GTRecipe recipe) {
        int total = 0;
        if (recipe.mOutputs != null) {
            for (ItemStack s : recipe.mOutputs) {
                ItemStack dust = convertToDust(s);
                if (dust != null) total += dust.stackSize;
            }
        }
        // 熔融金属流体也计入产出：144mb = 1 dust
        if (recipe.mFluidOutputs != null) {
            for (FluidStack f : recipe.mFluidOutputs) {
                if (f != null) total += f.amount / 144;
            }
        }
        return total;
    }

    private static ItemStack copyAmount(ItemStack stack, int amount) {
        ItemStack copy = GTUtility.copy(1, stack);
        copy.stackSize = amount;
        return copy;
    }

}
