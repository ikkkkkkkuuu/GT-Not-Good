package com.xyp.gtnotgood.common.items.wildcard.model.io;

import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials;

import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;

/**
 * 流体组件：用流体状态（MOLTEN/PLASMA/GAS/LIQUID）+ 数量，把当前材料变成对应流体（AE2FC ItemFluidDrop）。
 */
public final class FluidIOComponent implements IWildcardIOComponent {

    public static final String TYPE = "fluid";

    private static final String KEY_STATE = "State";
    private static final String KEY_AMOUNT = "Amount";

    private FluidState state;
    private long amount;

    public FluidIOComponent(FluidState state, long amount) {
        this.state = state;
        this.amount = Math.max(1L, amount);
    }

    public static FluidIOComponent empty() {
        return new FluidIOComponent(FluidState.MOLTEN, 144L);
    }

    public static FluidIOComponent readData(NBTTagCompound data) {
        FluidState state = FluidState.MOLTEN;
        String stored = data.getString(KEY_STATE);
        if (stored != null && !stored.isEmpty()) {
            try {
                state = FluidState.valueOf(stored.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }
        long amount = Math.max(1L, data.getLong(KEY_AMOUNT));
        return new FluidIOComponent(state, amount);
    }

    public FluidState getState() {
        return state;
    }

    public void setState(FluidState state) {
        this.state = state;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = Math.max(1L, amount);
    }

    @Override
    public ItemStack apply(Materials material) {
        return WildcardMaterials.makeFluidStack(state, material, amount);
    }

    @Override
    public ItemStack getDisplayStack() {
        return WildcardMaterials.makeFluidStack(state, Materials.Iron, Math.max(1L, amount));
    }

    @Override
    public boolean isEmpty() {
        return state == null;
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = new NBTTagCompound();
        data.setString(KEY_STATE, state == null ? FluidState.MOLTEN.name() : state.name());
        data.setLong(KEY_AMOUNT, amount);
        return data;
    }
}
