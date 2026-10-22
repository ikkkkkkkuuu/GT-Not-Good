package com.xyp.gtnotgood.common.recipe.machine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.IGTScannerHandler;
import gregtech.api.metatileentity.implementations.MTEHatchDataAccess;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.recipe.metadata.SimpleRecipeMetadataKey;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTScannerResult;

public final class DimensionalFocusResearch {

    public static final RecipeMetadataKey<Integer> ORIGINAL_EUT = SimpleRecipeMetadataKey.create(Integer.class,
        ModList.GTNotGood.getID() + ".dimensional_focus.original_eut");
    public static final RecipeMetadataKey<Long> COMPUTATION = SimpleRecipeMetadataKey.create(Long.class,
        ModList.GTNotGood.getID() + ".dimensional_focus.computation");
    public static final RecipeMetadataKey<String> RESEARCH = SimpleRecipeMetadataKey.create(String.class,
        ModList.GTNotGood.getID() + ".dimensional_focus.research");
    private static final String TOKEN_KEY = ModList.GTNotGood.getID() + ".dimensional_focus_research";
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final Map<String, ResearchEntry> ENTRIES = new LinkedHashMap<>();
    private static boolean registered;

    private DimensionalFocusResearch() {}

    public static long requiredComputation(GTRecipe recipe) {
        return recipe.getMetadataOrDefault(COMPUTATION, 0L);
    }

    public static boolean requiresResearch(GTRecipe recipe) {
        return !recipe.getMetadataOrDefault(RESEARCH, "").isEmpty();
    }

    public static boolean hasResearch(GTRecipe recipe, Iterable<MTEHatchDataAccess> hatches) {
        String required = recipe.getMetadataOrDefault(RESEARCH, "");
        if (required.isEmpty()) return true;
        for (MTEHatchDataAccess hatch : hatches) {
            if (hatch == null || !hatch.isValid()) continue;
            for (int slot = 0; slot < hatch.getSizeInventory(); slot++) {
                if (hasToken(hatch.getStackInSlot(slot), required)) return true;
            }
        }
        return false;
    }

    public static boolean hasToken(ItemStack stack, String required) {
        return stack != null && stack.stackSize > 0
            && ItemList.Tool_DataStick.isStackEqual(stack, false, true)
            && stack.hasTagCompound()
            && required.equals(stack.getTagCompound().getString(TOKEN_KEY));
    }

    public static String fingerprint(ItemStack stack) {
        String identity = Item.itemRegistry.getNameForObject(stack.getItem()) + ":"
            + stack.getItemDamage()
            + ":"
            + (stack.hasTagCompound() ? canonical(stack.getTagCompound()) : "");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            char[] result = new char[digest.length * 2];
            for (int index = 0; index < digest.length; index++) {
                int value = digest[index] & 255;
                result[index * 2] = HEX[value >>> 4];
                result[index * 2 + 1] = HEX[value & 15];
            }
            return new String(result);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String canonical(NBTBase tag) {
        if (tag instanceof NBTTagCompound compound) {
            List<String> keys = new ArrayList<>(compound.func_150296_c());
            keys.sort(String::compareTo);
            StringBuilder result = new StringBuilder("{");
            for (String key : keys) {
                String value = canonical(compound.getTag(key));
                result.append(key.length()).append(':').append(key).append(value.length()).append(':').append(value);
            }
            return result.append('}').toString();
        }
        if (tag instanceof NBTTagList list) {
            StringBuilder result = new StringBuilder("[");
            NBTTagList copy = (NBTTagList) list.copy();
            while (copy.tagCount() > 0) {
                String value = canonical(copy.removeTag(copy.tagCount() - 1));
                result.append(value.length()).append(':').append(value);
            }
            return result.append(']').toString();
        }
        return tag.getId() + ":" + tag;
    }

    public static ItemStack createResearchStick(ItemStack sample) {
        ItemStack output = ItemList.Tool_DataStick.get(1);
        NBTTagCompound data = new NBTTagCompound();
        data.setString(TOKEN_KEY, fingerprint(sample));
        output.setTagCompound(data);
        // #tr gtng.dimensional_focus.research.name
        // # Engraving Research: %s
        // # zh_CN 蚀刻研究：%s
        output.setStackDisplayName(
            StatCollector.translateToLocalFormatted("gtng.dimensional_focus.research.name", sample.getDisplayName()));
        return output;
    }

    public static void registerRecipes(Collection<GTRecipe> recipes) {
        for (GTRecipe recipe : recipes) {
            if (!requiresResearch(recipe)) continue;
            String key = recipe.getMetadata(RESEARCH);
            ItemStack sample = recipe.mOutputs[0];
            int eut = recipe.getMetadata(ORIGINAL_EUT);
            ResearchEntry previous = ENTRIES.get(key);
            if (previous == null || previous.eut > eut) ENTRIES.put(key, new ResearchEntry(sample.copy(), eut));
        }
        if (registered) return;
        registered = true;
        IGTScannerHandler handler = (scanner, input, special, fluid) -> {
            ResearchEntry entry = ENTRIES.get(fingerprint(input));
            if (
                entry == null || !ItemList.Tool_DataStick.isStackEqual(special, false, true)
                    || !ItemStack.areItemStackTagsEqual(special, ItemList.Tool_DataStick.get(1))
            ) return GTScannerResult.NOT_FOUND;
            return new GTScannerResult(entry.eut, 2400, 1, 1, 0, createResearchStick(entry.sample));
        };
        RecipeMaps.scannerHandlers.addFirst(handler);
        for (ResearchEntry entry : ENTRIES.values()) {
            GTRecipeBuilder.builder().itemInputs(entry.sample.copy()).special(ItemList.Tool_DataStick.get(1))
                .itemOutputs(createResearchStick(entry.sample)).duration(2400).eut(entry.eut).fake().ignoreCollision()
                .addTo(RecipeMaps.scannerFakeRecipes);
        }
    }

    private static final class ResearchEntry {

        private final ItemStack sample;
        private final int eut;

        private ResearchEntry(ItemStack sample, int eut) {
            this.sample = sample;
            this.sample.stackSize = 1;
            this.eut = eut;
        }
    }
}
