package com.xyp.gtnotgood.common.machines.hatch.me;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.ArrayUtils;

import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.inv.MEInventoryCrafting;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.objects.GTDualInputPattern;
import gregtech.api.util.GTUtility;

/** Per-pattern virtual circuits and molds with atomic ingredient buffers using GTNH's native dual-input API. */
public class CircuitMEPatternBuffer extends SuperMTEHatchCraftingInputME {

    public static final int patternCount = 900;
    public static final int bufferTypeLimit = 64;
    private final List<CircuitSlot> pendingRefunds = new ArrayList<>();
    private boolean inputsSerializedIntoDrop;

    public CircuitMEPatternBuffer(int id, String name, String regionalName) {
        super(id, name, regionalName, true, patternCount / 9);
    }

    private CircuitMEPatternBuffer(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, description, textures, true, patternCount / 9);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new CircuitMEPatternBuffer(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    protected PatternSlot<SuperMTEHatchCraftingInputME> createPatternSlot(ItemStack pattern, NBTTagCompound saved) {
        World world = getBaseMetaTileEntity().getWorld();
        ICraftingPatternDetails original = CircuitPatternCodec.decode(pattern, world);
        ItemStack[] molds = CircuitPatternCodec.molds(original);
        return new CircuitSlot(pattern, saved, this, CircuitPatternCodec.runtime(pattern, world, original, molds),
            CircuitPatternCodec.circuit(original), molds);
    }

    /** Each pattern supplies its own circuit and molds; only physical manual slots are shared. */
    @Override
    public ItemStack[] getSharedItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = getManualSlotStart(); i < getMoldSlot(); i++) {
            if (mInventory[i] != null) items.add(mInventory[i]);
        }
        return items.toArray(new ItemStack[0]);
    }

    @Override
    public boolean allowSelectCircuit() {
        return false;
    }

    @Override
    public boolean hasVirtualMoldSlot() {
        return false;
    }

    /**
     * Retains rejected long refunds after a pattern is removed; they cannot enter another pattern's recipe inventory.
     */
    @Override
    public void onPatternChange(int index, ItemStack replacement) {
        PatternSlot<SuperMTEHatchCraftingInputME> previous = getPatternSlot(index);
        super.onPatternChange(index, replacement);
        if (previous instanceof CircuitSlot slot && previous != getPatternSlot(index) && !slot.isEmpty()) {
            pendingRefunds.add(slot);
            markDirty();
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long timer) {
        super.onPostTick(tile, timer);
        if (tile.isServerSide() && timer % 20 == 0 && !pendingRefunds.isEmpty() && isActive()) retryRefunds(4);
    }

    private void retryRefunds(int limit) {
        int attempts = Math.min(limit, pendingRefunds.size());
        while (attempts-- > 0) {
            CircuitSlot slot = pendingRefunds.remove(0);
            try {
                slot.refund(getProxy(), getMEOutputActionSource(), false);
            } catch (GridAccessException ignored) {}
            if (slot.isEmpty()) markDirty();
            else pendingRefunds.add(slot);
        }
    }

    @Override
    public void refundAll(boolean shouldDrop) {
        if (shouldDrop && inputsSerializedIntoDrop) {
            for (PatternSlot<SuperMTEHatchCraftingInputME> slot : iterableSlots())
                if (slot instanceof CircuitSlot circuitSlot) circuitSlot.buffer.clear();
            pendingRefunds.clear();
            return;
        }
        super.refundAll(false);
        retryRefunds(pendingRefunds.size());
    }

    private List<PatternSlot<SuperMTEHatchCraftingInputME>> iterableSlots() {
        List<PatternSlot<SuperMTEHatchCraftingInputME>> slots = new ArrayList<>();
        inventories().forEachRemaining(slots::add);
        return slots;
    }

    private void writeRefunds(NBTTagCompound tag, boolean includeActive) {
        NBTTagList list = new NBTTagList();
        List<PatternSlot<SuperMTEHatchCraftingInputME>> slots = new ArrayList<>(pendingRefunds);
        if (includeActive) slots.addAll(iterableSlots());
        for (PatternSlot<SuperMTEHatchCraftingInputME> slot : slots)
            if (!slot.isEmpty()) list.appendTag(slot.writeToNBT(new NBTTagCompound()));
        if (list.tagCount() == 0) tag.removeTag("circuitBufferRefunds");
        else tag.setTag("circuitBufferRefunds", list);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        writeRefunds(tag, false);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        mInventory[getCircuitSlot()] = null;
        mInventory[getMoldSlot()] = null;
        pendingRefunds.clear();
        inputsSerializedIntoDrop = false;
        NBTTagList list = tag.getTagList("circuitBufferRefunds", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound saved = list.getCompoundTagAt(i);
            ItemStack pattern = ItemStack.loadItemStackFromNBT(saved.getCompoundTag("pattern"));
            if (CircuitPatternCodec.decode(pattern, getBaseMetaTileEntity().getWorld()) != null) {
                CircuitSlot slot = (CircuitSlot) createPatternSlot(pattern, saved);
                if (!slot.isEmpty()) pendingRefunds.add(slot);
            }
        }
    }

    /**
     * Stores rejected input refunds on the actual dropped hatch; its physical patterns are dropped separately by GT.
     */
    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        writeRefunds(tag, true);
        inputsSerializedIntoDrop = true;
    }

    @Override
    public String[] getInfoData() {
        String[] lines = super.getInfoData();
        if (pendingRefunds.isEmpty()) return lines;
        // #tr tooltip.gtng.circuit_buffer.refunds
        // # Pending input refunds: %s buffers; reconnect ME to return them.
        // # zh_CN 待退料缓冲：%s份；接回ME网络后自动退回。
        return ArrayUtils.add(lines,
            StatCollector.translateToLocalFormatted("tooltip.gtng.circuit_buffer.refunds", pendingRefunds.size()));
    }

    @Override
    public String[] getDescription() {
        // #tr tooltip.gtng.circuit_buffer.virtual
        // # Reads each pattern's circuit and reusable molds; AE does not request these tools.
        // # zh_CN 自动读取每份样板的电路和不消耗模具，AE下单不索取这些工具。
        String circuit = StatCollector.translateToLocal("tooltip.gtng.circuit_buffer.virtual");
        // #tr tooltip.gtng.circuit_buffer.capacity
        // # 900 patterns; each buffers 64 item types and 64 fluid types, up to 2^63-1 each.
        // # zh_CN 900个样板槽；每份缓冲64种物品和64种流体，每种上限2^63-1。
        String capacity = StatCollector.translateToLocal("tooltip.gtng.circuit_buffer.capacity");
        // #tr tooltip.gtng.circuit_buffer.recipe_view
        // # GT sees up to 2^31-1 of each type per recipe check; the long reserve refills it.
        // # zh_CN 单次配方检查每种最多提供2^31-1，剩余long库存自动补充。
        String recipeView = StatCollector.translateToLocal("tooltip.gtng.circuit_buffer.recipe_view");
        return ArrayUtils.addAll(super.getDescription(), circuit, capacity, recipeView);
    }

    /** Live stacks are debited by GT; phantom tools never make an empty buffer runnable. */
    public static final class CircuitSlot extends PatternSlot<SuperMTEHatchCraftingInputME> {

        private final int circuit;
        private final ItemStack[] molds;
        private final LongCircuitBuffer buffer;

        private CircuitSlot(ItemStack pattern, NBTTagCompound saved, CircuitMEPatternBuffer parent,
            ICraftingPatternDetails details, int circuit, ItemStack[] molds) {
            super(pattern, null, parent, details);
            this.circuit = circuit;
            this.molds = molds;
            buffer = new LongCircuitBuffer(saved, parent::markDirty);
        }

        @Override
        public boolean hasChanged(ItemStack replacement, World world) {
            return !ItemStack.areItemStacksEqual(pattern, replacement);
        }

        @Override
        public void updateSlotItems() {
            itemInventory.clear();
            itemInventory.addAll(buffer.itemViews());
        }

        @Override
        public void updateSlotFluids() {
            fluidInventory.clear();
            fluidInventory.addAll(buffer.fluidViews());
        }

        @Override
        public List<IAEItemStack> getStoredItems() {
            return buffer.storedItems();
        }

        @Override
        public List<IAEFluidStack> getStoredFluids() {
            return buffer.storedFluids();
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            tag.setTag("pattern", pattern.writeToNBT(new NBTTagCompound()));
            buffer.write(tag);
            return tag;
        }

        @Override
        public void refund(AENetworkProxy proxy, BaseActionSource source, boolean shouldDrop)
            throws GridAccessException {
            buffer.refund(proxy, source);
        }

        private ItemStack[] recipeTools() {
            List<ItemStack> tools = new ArrayList<>();
            if (circuit >= 0) tools.add(GTUtility.getIntegratedCircuit(circuit));
            for (ItemStack mold : molds) tools.add(mold.copy());
            return tools.toArray(new ItemStack[0]);
        }

        @Override
        public ItemStack[] getItemInputs() {
            ItemStack[] inputs = super.getItemInputs();
            return isEmpty() ? inputs : ArrayUtils.addAll(inputs, recipeTools());
        }

        @Override
        public FluidStack[] getFluidInputs() {
            updateSlotFluids();
            return super.getFluidInputs();
        }

        @Override
        public GTDualInputPattern getPatternInputs() {
            GTDualInputPattern inputs = super.getPatternInputs();
            inputs.inputItems = ArrayUtils.addAll(inputs.inputItems, recipeTools());
            return inputs;
        }

        @Override
        public boolean insertItemsAndFluids(MEInventoryCrafting table) {
            return patternDetails != null && buffer.insert(table, molds);
        }
    }
}
