package com.xyp.gtnotgood.common.blocks.largeinterface;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.glodblock.github.common.tile.TileFluidInterface;
import com.glodblock.github.util.DualityFluidInterface;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.storage.data.IAEFluidStack;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.util.SettingsFrom;

/** Native dual interface with a larger pattern inventory and no stock request configuration. */
public final class TileLargeInterface extends TileFluidInterface implements LargeInterfaceHost {

    private final AppEngInternalAEInventory fluidConfig = LargeInterfaceSupport
        .emptyItemConfig(DualityFluidInterface.NUMBER_OF_TANKS);

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
    public AppEngInternalAEInventory getConfig() {
        return fluidConfig;
    }

    @Override
    public void setConfig(int slot, IAEFluidStack fluid) {}

    @Override
    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readFromNBTEvent(NBTTagCompound data) {
        super.readFromNBTEvent(LargeInterfaceSupport.withoutStockConfig(data));
    }

    @Override
    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public NBTTagCompound writeToNBTEvent(NBTTagCompound data) {
        super.writeToNBTEvent(data);
        data.removeTag("ConfigInv");
        return data;
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
    protected ItemStack getItemFromTile(Object tile) {
        return tile instanceof TileLargeInterface ? GTNGItemList.LargeInterface.get(1) : super.getItemFromTile(tile);
    }

    @Override
    public ItemStack getPrimaryGuiIcon() {
        return GTNGItemList.LargeInterface.get(1);
    }
}
