package com.xyp.gtnotgood.common.machines.hatch.me;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import com.xyp.gtnotgood.common.compat.CircuitPatternMolds;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.Platform;
import gregtech.api.util.GTUtility;
import gregtech.common.items.ItemIntegratedCircuit;

/** Runtime AE inputs preserve fluid NBT and leave the physical pattern's virtual tools unchanged. */
public final class CircuitPatternCodec {

    private CircuitPatternCodec() {}

    public static ICraftingPatternDetails decode(ItemStack pattern, World world) {
        if (pattern == null || !(pattern.getItem() instanceof ICraftingPatternItem item)) return null;
        try {
            return item.getPatternForItem(pattern, world);
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    public static boolean isCircuit(IAEStack<?> stack) {
        return stack instanceof IAEItemStack item && item.getItem() instanceof ItemIntegratedCircuit;
    }

    public static boolean isMold(IAEStack<?> stack, ItemStack[] molds) {
        if (!(stack instanceof IAEItemStack item)) return false;
        for (ItemStack mold : molds) if (GTUtility.areStacksEqual(mold, item.getItemStack())) return true;
        return false;
    }

    /**
     * Resolves encoded catalog molds that the matching GT recipes require without consuming them.
     *
     * @param details original processing pattern with its encoded tools
     * @return distinct zero-size mold templates; genuine consumable molds remain AE inputs
     */
    public static ItemStack[] molds(ICraftingPatternDetails details) {
        return CircuitPatternMolds.resolve(details);
    }

    /** @return -1 for no circuit, -2 for conflicting or unsupported configurations. */
    public static int circuit(ICraftingPatternDetails details) {
        int result = -1;
        if (details == null || details.isCraftable()) return result;
        for (IAEStack<?> stack : details.getAEInputs()) {
            if (!isCircuit(stack)) continue;
            int config = ((IAEItemStack) stack).getItemStack()
                .getItemDamage();
            if (config < 0 || config > 24 || result >= 0 && result != config) return -2;
            result = config;
        }
        return result;
    }

    /**
     * Removes virtual circuits and molds from AE demands and records them in the runtime pattern identity.
     * The native pattern item can reconstruct the same details after an AE crafting CPU save/reload.
     */
    public static ICraftingPatternDetails runtime(ItemStack pattern, World world) {
        ICraftingPatternDetails original = decode(pattern, world);
        return runtime(pattern, world, original, molds(original));
    }

    static ICraftingPatternDetails runtime(ItemStack pattern, World world, ICraftingPatternDetails original,
        ItemStack[] molds) {
        if (original == null || original.isCraftable() || original.isInputOnly()) return null;
        int config = circuit(original);
        if (config == -2) return null;
        if (config < 0 && molds.length == 0) return original;
        ItemStack runtime = withoutVirtualItems(pattern, molds);
        if (runtime == null) return null;
        if (config >= 0) runtime.getTagCompound()
            .setInteger("gtngPatternCircuit", config);
        if (molds.length > 0) {
            NBTTagList tools = new NBTTagList();
            for (ItemStack mold : molds) tools.appendTag(mold.writeToNBT(new NBTTagCompound()));
            runtime.getTagCompound()
                .setTag("gtngPatternMolds", tools);
        }
        ICraftingPatternDetails decoded = decode(runtime, world);
        if (decoded == null || decoded.getCondensedAEInputs().length == 0 || circuit(decoded) != -1) return null;
        return decoded;
    }

    private static ItemStack withoutVirtualItems(ItemStack pattern, ItemStack[] molds) {
        if (pattern == null || !pattern.hasTagCompound()) return null;
        ItemStack edited = pattern.copy();
        NBTTagCompound tag = edited.getTagCompound();
        NBTTagList original = tag.getTagList("in", Constants.NBT.TAG_COMPOUND);
        NBTTagList inputs = new NBTTagList();
        for (int i = 0; i < original.tagCount(); i++) {
            NBTTagCompound entry = original.getCompoundTagAt(i);
            IAEStack<?> stack = Platform.readStackNBT(entry, true);
            if (stack == null) continue;
            if (!isCircuit(stack) && !isMold(stack, molds)) inputs.appendTag(entry.copy());
        }
        if (inputs.tagCount() == 0) return null;
        tag.setTag("in", inputs);
        tag.removeTag("InvalidPattern");
        tag.removeTag("gtngPatternCircuit");
        tag.removeTag("gtngPatternMolds");
        return edited;
    }
}
