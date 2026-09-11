package com.xyp.gtnotgood.utils.machine.factory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * Lazy catalog of ordinary GT recipes. Special-machine recipes are excluded until their requirements have an adapter.
 * Recipe hashes include full inputs, outputs and timing so changed pack recipes cannot silently replace saved ones.
 */
public final class FactoryRecipeCatalog {

    private static Map<String, Entry> catalog;

    /** Authoritative recipe reference; clients can select the ID but cannot supply recipe contents. */
    public static final class Entry {

        public final String id;
        public final RecipeMap<?> map;
        public final GTRecipe recipe;
        /** Consumable-only view; reserved catalysts never enter material matching or consumption. */
        public final GTRecipe consumableRecipe;

        private Entry(String id, RecipeMap<?> map, GTRecipe recipe) {
            this.id = id;
            this.map = map;
            this.recipe = recipe;
            this.consumableRecipe = recipe.copy();
            this.consumableRecipe.mInputs = Arrays.stream(recipe.mInputs)
                .filter(stack -> stack != null && stack.stackSize > 0)
                .map(ItemStack::copy)
                .toArray(ItemStack[]::new);
        }

        public String title() {
            for (ItemStack item : recipe.mOutputs) if (item != null) return item.getDisplayName();
            for (FluidStack fluid : recipe.mFluidOutputs) if (fluid != null) return fluid.getLocalizedName();
            return StatCollector.translateToLocal(map.unlocalizedName);
        }

        public String summary() {
            return title() + " | "
                + StatCollector.translateToLocal(map.unlocalizedName)
                + " | "
                + recipe.mEUt
                + " EU/t";
        }
    }

    private FactoryRecipeCatalog() {}

    public static Entry get(String id) {
        return entries().get(id);
    }

    /** Resolves an NEI recipe to the same server-owned identity used by saved nodes. */
    public static Entry find(RecipeMap<?> map, GTRecipe recipe) {
        return map == null || unsupportedReason(map, recipe) != null ? null
            : get(map.unlocalizedName + ":" + fingerprint(recipe));
    }

    private static synchronized Map<String, Entry> entries() {
        if (catalog != null) return catalog;
        Map<String, Entry> result = new LinkedHashMap<>();
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (unsupportedReason(map, recipe) != null) continue;
                String id = map.unlocalizedName + ":" + fingerprint(recipe);
                result.put(id, new Entry(id, map, recipe));
            }
        }
        catalog = Collections.unmodifiableMap(result);
        return catalog;
    }

    /** Returns a precise reason instead of treating unadapted recipe features as missing recipes. */
    public static FactoryText unsupportedReason(RecipeMap<?> map, GTRecipe recipe) {
        boolean ignoreHeat = map == gregtech.api.recipe.RecipeMaps.blastFurnaceRecipes
            || map == gregtech.api.recipe.RecipeMaps.plasmaForgeRecipes;
        if (recipe == null) return FactoryText.IMPORT_UNREGISTERED;
        if (recipe.mFakeRecipe) return FactoryText.IMPORT_VIRTUAL;
        if (!recipe.mEnabled || recipe.mHidden) return FactoryText.IMPORT_DISABLED;
        if (recipe.getClass() != GTRecipe.class) return FactoryText.IMPORT_CUSTOM;
        // Explicitly authorized: blast-furnace and plasma-forge heat is ignored; other special conditions remain
        // validated.
        if (recipe.mSpecialItems != null || (recipe.mSpecialValue != 0 && !(ignoreHeat && recipe.mSpecialValue > 0)))
            return FactoryText.IMPORT_SPECIAL;
        if (recipe.getMetadataStorage() != null) {
            for (Map.Entry<gregtech.api.recipe.RecipeMetadataKey<?>, Object> metadata : recipe.getMetadataStorage()
                .getEntries()) {
                if (ignoreHeat && metadata.getKey()
                    .equals(gregtech.api.util.GTRecipeConstants.COIL_HEAT)
                    && metadata.getValue() instanceof Integer heat
                    && heat >= 0) continue;
                if (!inactiveEnvironmentRequirement(metadata.getKey(), metadata.getValue()))
                    return FactoryText.IMPORT_METADATA;
            }
        }
        if (recipe.mEUt <= 0 || recipe.mDuration <= 0) return FactoryText.IMPORT_TIMING;
        if (recipe.mInputs.length > 16 || recipe.mOutputs.length > 16
            || recipe.mFluidInputs.length > 16
            || recipe.mFluidOutputs.length > 16) return FactoryText.IMPORT_SLOTS;
        if (recipe.mInputChances != null)
            for (int chance : recipe.mInputChances) if (chance != 10000) return FactoryText.IMPORT_CHANCES;
        if (recipe.mFluidInputChances != null)
            for (int chance : recipe.mFluidInputChances) if (chance != 10000) return FactoryText.IMPORT_CHANCES;
        if (recipe.mFluidOutputChances != null)
            for (int chance : recipe.mFluidOutputChances) if (chance != 10000) return FactoryText.IMPORT_CHANCES;
        return null;
    }

    /** Explicit false means the environment is not required (UniversalChemical adds CLEANROOM=false to LCR). */
    static boolean inactiveEnvironmentRequirement(gregtech.api.recipe.RecipeMetadataKey<?> key, Object value) {
        return Boolean.FALSE.equals(value) && (key.equals(gregtech.api.util.GTRecipeConstants.CLEANROOM)
            || key.equals(gregtech.api.util.GTRecipeConstants.LOW_GRAVITY));
    }

    private static String fingerprint(GTRecipe recipe) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("in", items(recipe.mInputs));
        tag.setTag("out", items(recipe.mOutputs));
        tag.setTag("fin", fluids(recipe.mFluidInputs));
        tag.setTag("fout", fluids(recipe.mFluidOutputs));
        int[] chances = new int[recipe.mOutputs.length];
        for (int i = 0; i < chances.length; i++) chances[i] = recipe.getOutputChance(i);
        tag.setIntArray("chance", chances);
        tag.setInteger("eut", recipe.mEUt);
        tag.setInteger("ticks", recipe.mDuration);
        // Keep old ordinary recipe IDs stable while distinguishing newly supported heat-bearing recipes.
        if (recipe.mSpecialValue != 0) tag.setInteger("special", recipe.mSpecialValue);
        tag.setBoolean("nbt", recipe.isNBTSensitive);
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(canonical(tag).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Canonical compound ordering also normalizes nested item NBT before hashing. */
    private static String canonical(NBTBase tag) {
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            List<String> keys = new ArrayList<>(compound.func_150296_c());
            Collections.sort(keys);
            StringBuilder result = new StringBuilder("{");
            for (String key : keys) result.append(key.length())
                .append(':')
                .append(key)
                .append(canonical(compound.getTag(key)));
            return result.append('}')
                .toString();
        }
        if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tag;
            if (list.func_150303_d() != 10) return list.toString();
            StringBuilder result = new StringBuilder("[");
            for (int i = 0; i < list.tagCount(); i++) result.append(canonical(list.getCompoundTagAt(i)));
            return result.append(']')
                .toString();
        }
        return tag.getId() + ":" + tag.toString();
    }

    public static NBTTagList items(ItemStack[] values) {
        NBTTagList list = new NBTTagList();
        for (ItemStack value : values) {
            NBTTagCompound tag = new NBTTagCompound();
            if (value != null) {
                value.writeToNBT(tag);
                tag.setInteger("amount", value.stackSize);
            }
            list.appendTag(tag);
        }
        return list;
    }

    public static NBTTagList fluids(FluidStack[] values) {
        NBTTagList list = new NBTTagList();
        for (FluidStack value : values) {
            NBTTagCompound tag = new NBTTagCompound();
            if (value != null) value.writeToNBT(tag);
            list.appendTag(tag);
        }
        return list;
    }
}
