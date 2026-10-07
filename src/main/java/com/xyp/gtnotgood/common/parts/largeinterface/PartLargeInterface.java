package com.xyp.gtnotgood.common.parts.largeinterface;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.glodblock.github.common.parts.PartFluidInterface;
import com.glodblock.github.util.DualityFluidInterface;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceGuiFactory;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceHost;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfacePatternInventory;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceSupport;

import appeng.api.parts.PartItemStack;
import appeng.api.storage.data.IAEFluidStack;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.util.SettingsFrom;

/** Cable-mounted counterpart retaining the native interface model, upgrades, and output return paths. */
public final class PartLargeInterface extends PartFluidInterface implements LargeInterfaceHost {

    private final AppEngInternalAEInventory fluidConfig = LargeInterfaceSupport
        .emptyItemConfig(DualityFluidInterface.NUMBER_OF_TANKS);

    public PartLargeInterface(ItemStack stack) {
        super(stack);
    }

    @Override
    public void onPlacement(EntityPlayer player, ItemStack held, ForgeDirection side) {
        super.onPlacement(player, held, side);
        if (held.hasTagCompound()) uploadSettings(SettingsFrom.DISMANTLE_ITEM, held.getTagCompound());
    }

    /** Saves current settings for shape conversion; patterns and upgrade cards remain native separate drops. */
    @Override
    public ItemStack getItemStack(PartItemStack type) {
        ItemStack stack = super.getItemStack(type);
        if (type != PartItemStack.Wrench) return stack;
        ItemStack portable = stack.copy();
        NBTTagCompound settings = downloadSettings(SettingsFrom.DISMANTLE_ITEM);
        portable.setTagCompound(settings.hasNoTags() ? null : settings);
        return portable;
    }

    @Override
    public int rows() {
        return ((LargeInterfacePatternInventory) getInterfaceDuality().getPatterns()).getTerminalRows();
    }

    @Override
    public int rowSize() {
        return PATTERN_COLUMNS;
    }

    @Override
    public int numSlots() {
        return rows() * PATTERN_COLUMNS;
    }

    @Override
    public boolean onPartActivate(EntityPlayer player, Vec3 pos) {
        if (player.isSneaking()) return false;
        if (!player.worldObj.isRemote) LargeInterfaceGuiFactory.INSTANCE.open(player, this);
        return true;
    }

    @Override
    public AppEngInternalAEInventory getConfig() {
        return fluidConfig;
    }

    @Override
    public void setConfig(int slot, IAEFluidStack fluid) {}

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(LargeInterfaceSupport.withoutStockConfig(data));
    }

    @Override
    public void uploadSettings(SettingsFrom from, NBTTagCompound data) {
        super.uploadSettings(from, LargeInterfaceSupport.withoutStockConfig(data));
    }

    @Override
    public NBTTagCompound downloadSettings(SettingsFrom from) {
        return LargeInterfaceSupport.withoutStockConfig(super.downloadSettings(from));
    }

    @Override
    public ItemStack getPrimaryGuiIcon() {
        return getItemStack().copy();
    }
}
