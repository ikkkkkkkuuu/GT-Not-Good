package com.xyp.gtnotgood.common.items.wildcard;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;

/**
 * 通配样板符预览详情
 * 用于在GUI中显示代表性的输入/输出,而非完整配方
 */
public class WildcardPreviewPatternDetails implements ICraftingPatternDetails {

    private final ItemStack patternStack;
    private final IAEItemStack[] inputs;
    private final IAEItemStack[] outputs;

    public WildcardPreviewPatternDetails(ItemStack patternStack, ItemStack representativeInput,
        ItemStack representativeOutput) {
        this.patternStack = patternStack;

        // 构建输入数组
        if (representativeInput != null) {
            this.inputs = new IAEItemStack[] { AEItemStack.create(representativeInput) };
        } else {
            this.inputs = new IAEItemStack[0];
        }

        // 构建输出数组
        if (representativeOutput != null) {
            this.outputs = new IAEItemStack[] { AEItemStack.create(representativeOutput) };
        } else {
            this.outputs = new IAEItemStack[0];
        }
    }

    @Override
    public ItemStack getPattern() {
        return patternStack;
    }

    @Override
    public boolean isValidItemForSlot(int slotIndex, ItemStack itemStack, World world) {
        return false; // 预览模式不支持验证
    }

    @Override
    public boolean isCraftable() {
        return false; // 预览不可直接合成
    }

    @Override
    public IAEItemStack[] getInputs() {
        return inputs;
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return inputs;
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return outputs;
    }

    @Override
    public IAEItemStack[] getOutputs() {
        return outputs;
    }

    @Override
    public boolean canSubstitute() {
        return false;
    }

    @Override
    public boolean canBeSubstitute() {
        return false;
    }

    @Override
    public ItemStack getOutput(InventoryCrafting craftingInv, World world) {
        return outputs.length > 0 ? outputs[0].getItemStack() : null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void setPriority(int priority) {
        // 预览模式不支持设置优先级
    }
}
