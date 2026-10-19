package com.xyp.gtnotgood.common.machines.hatch.me;

import static com.xyp.gtnotgood.common.gui.slots.WildcardPatternSlotSuper.KEY_ACTIVE_PATTERN;
import static com.xyp.gtnotgood.common.gui.slots.WildcardPatternSlotSuper.KEY_ACTIVE_PATTERN_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.ArrayUtils;

import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternCache;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternGenerator;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.inv.MEInventoryCrafting;
import gregtech.api.enums.GTValues;
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
    private final Map<ICraftingPatternDetails, CircuitRecipe> craftingSlots = new HashMap<>();
    private boolean craftingSlotsDirty = true;
    private boolean inputsChanged;
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
        return new CircuitSlot(pattern, saved, this);
    }

    private void rebuildCraftingSlots() {
        craftingSlots.clear();
        for (PatternSlot<SuperMTEHatchCraftingInputME> slot : iterableSlots()) {
            if (!(slot instanceof CircuitSlot circuitSlot)) continue;
            for (CircuitRecipe recipe : circuitSlot.recipes()) craftingSlots.put(recipe.details, recipe);
        }
        craftingSlotsDirty = false;
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper tracker) {
        if (!isActive()) return;
        rebuildCraftingSlots();
        for (ICraftingPatternDetails details : craftingSlots.keySet()) tracker.addCraftingOption(this, details);
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails details, InventoryCrafting table) {
        if (
            !isActive() || !getBaseMetaTileEntity().isAllowedToWork() || !(table instanceof MEInventoryCrafting meTable)
        ) return false;
        if (craftingSlotsDirty) rebuildCraftingSlots();
        CircuitRecipe recipe = craftingSlots.get(details);
        if (recipe == null || !recipe.slot.insert(recipe, meTable)) return false;
        inputsChanged = true;
        return true;
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
        craftingSlotsDirty = true;
        if (previous instanceof CircuitSlot slot && previous != getPatternSlot(index) && !slot.isEmpty()) {
            pendingRefunds.add(slot);
            markDirty();
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long timer) {
        super.onPostTick(tile, timer);
        if (tile.isServerSide() && inputsChanged) {
            notifyWatchers();
            for (var mirror : getProxyHatches()) mirror.onParentInvChange();
            inputsChanged = false;
        }
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
        for (PatternSlot<SuperMTEHatchCraftingInputME> slot : iterableSlots()) {
            if (!(slot instanceof CircuitSlot circuitSlot) || !circuitSlot.needsRecoveryRefund()) continue;
            pendingRefunds.add(circuitSlot.takeRecoveryRefund());
        }
        craftingSlots.clear();
        craftingSlotsDirty = true;
        inputsChanged = iterableSlots().stream().anyMatch(slot -> !slot.isEmpty());
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
        // # Pending input returns: %s buffers; reconnect ME to return them
        // # zh_CN 待退还原料：%s 份缓存；接回 ME 网络后自动退还
        return ArrayUtils.add(lines,
            StatCollector.translateToLocalFormatted("tooltip.gtng.circuit_buffer.refunds", pendingRefunds.size()));
    }

    @Override
    public String[] getDescription() {
        // #tr tooltip.gtng.circuit_buffer.virtual
        // # Supplies each pattern's circuit and reusable molds without requesting them from ME
        // # zh_CN 自动提供各样板的电路和不消耗模具，无需 ME 合成任务索取
        String circuit = StatCollector.translateToLocal("tooltip.gtng.circuit_buffer.virtual");
        // #tr tooltip.gtng.circuit_buffer.capacity
        // # 900 patterns; each holds 64 item types and 64 fluid types, up to 2^63 - 1 per type
        // # zh_CN 900 个样板槽；每份缓存最多 64 种物品和 64 种流体，每种上限 2^63 - 1
        String capacity = StatCollector.translateToLocal("tooltip.gtng.circuit_buffer.capacity");
        // #tr tooltip.gtng.circuit_buffer.recipe_view
        // # Each recipe check can use up to 2^31 - 1 per type; remaining stock stays buffered
        // # zh_CN 每次配方检查每种原料最多提供 2^31 - 1，剩余原料继续保留在缓存中
        String recipeView = StatCollector.translateToLocal("tooltip.gtng.circuit_buffer.recipe_view");
        // #tr tooltip.gtng.circuit_buffer.mirror
        // # Supports Circuit Pattern Input Mirrors (ME) and Super Pattern Input Mirrors (ME)
        // # zh_CN 可连接电路样板输入镜像 (ME) 或超级样板输入镜像 (ME)
        String mirror = StatCollector.translateToLocal("tooltip.gtng.circuit_buffer.mirror");
        // #tr tooltip.gtng.circuit_buffer.wildcard
        // # Wildcard patterns expand automatically; each slot handles one generated recipe until its buffer is empty
        // # zh_CN 自动展开通配符样板；每个槽位的缓存清空后才能切换展开配方
        String wildcard = StatCollector.translateToLocal("tooltip.gtng.circuit_buffer.wildcard");
        return ArrayUtils.addAll(super.getDescription(), circuit, capacity, recipeView, mirror, wildcard);
    }

    /** Live stacks are debited by GT; phantom tools never make an empty buffer runnable. */
    public static final class CircuitSlot extends PatternSlot<SuperMTEHatchCraftingInputME> {

        private final LongCircuitBuffer buffer;
        private final boolean wildcard;
        private final boolean legacyInventory;
        private final WildcardPatternCache<List<CircuitRecipe>> expansionCache = new WildcardPatternCache<>();
        private CircuitRecipe activeRecipe;

        private CircuitSlot(ItemStack pattern, NBTTagCompound saved, CircuitMEPatternBuffer parent) {
            super(pattern, null, parent, null);
            buffer = new LongCircuitBuffer(saved, parent::markDirty, itemInventory, fluidInventory);
            wildcard = WildcardPatternGenerator.isWildcardPattern(pattern)
                && !WildcardPatternGenerator.isGeneratedPattern(pattern);
            legacyInventory = saved != null && !saved.hasKey("longItems") && !saved.hasKey("longFluids");
            if (!wildcard) activeRecipe = decodeRecipe(pattern);
            else if (saved != null && !isEmpty()) {
                String savedId = saved.getString(KEY_ACTIVE_PATTERN_ID);
                for (CircuitRecipe recipe : recipes()) {
                    if (
                        !savedId.isEmpty()
                            && savedId.equals(WildcardPatternGenerator.getGeneratedPatternId(recipe.original))
                    ) {
                        activeRecipe = recipe;
                        break;
                    }
                }
                if (activeRecipe == null && saved.hasKey(KEY_ACTIVE_PATTERN, Constants.NBT.TAG_COMPOUND)) {
                    ItemStack active = ItemStack.loadItemStackFromNBT(saved.getCompoundTag(KEY_ACTIVE_PATTERN));
                    if (WildcardPatternGenerator.isGeneratedPattern(active)) activeRecipe = decodeRecipe(active);
                }
            }
        }

        private CircuitRecipe decodeRecipe(ItemStack encoded) {
            World world = parentMTE.getBaseMetaTileEntity().getWorld();
            ICraftingPatternDetails original = CircuitPatternCodec.decode(encoded, world);
            ItemStack[] molds = CircuitPatternCodec.molds(original);
            ICraftingPatternDetails details = CircuitPatternCodec.runtime(encoded, world, original, molds);
            return details == null ? null
                : new CircuitRecipe(this, encoded, details, CircuitPatternCodec.circuit(original), molds);
        }

        private List<CircuitRecipe> recipes() {
            if (!wildcard)
                return activeRecipe == null ? Collections.emptyList() : Collections.singletonList(activeRecipe);
            World world = parentMTE.getBaseMetaTileEntity().getWorld();
            return expansionCache.get(pattern, world, () -> {
                List<CircuitRecipe> result = new ArrayList<>();
                for (ICraftingPatternDetails details : WildcardPatternGenerator.generateAllDetails(pattern, world)) {
                    CircuitRecipe recipe = decodeRecipe(details.getPattern());
                    if (recipe != null) result.add(recipe);
                }
                return result;
            });
        }

        private boolean needsRecoveryRefund() {
            if (!wildcard || isEmpty()) return false;
            if (activeRecipe == null) return true;
            if (!legacyInventory) return false;
            for (IAEItemStack item : buffer.storedItems())
                if (CircuitPatternCodec.isCircuit(item) || CircuitPatternCodec.isMold(item, activeRecipe.molds))
                    return true;
            return false;
        }

        private CircuitSlot takeRecoveryRefund() {
            CircuitSlot refund = new CircuitSlot(pattern, writeToNBT(new NBTTagCompound()),
                (CircuitMEPatternBuffer) parentMTE);
            buffer.clear();
            return refund;
        }

        private boolean insert(CircuitRecipe recipe, MEInventoryCrafting table) {
            if (!isEmpty() && (activeRecipe == null || !Objects.equals(activeRecipe.details, recipe.details)))
                return false;
            if (!buffer.insert(table, recipe.molds)) return false;
            activeRecipe = recipe;
            return true;
        }

        @Override
        public ICraftingPatternDetails getPatternDetails() {
            return activeRecipe == null ? null : activeRecipe.details;
        }

        @Override
        public boolean hasChanged(ItemStack replacement, World world) {
            return !ItemStack.areItemStacksEqual(pattern, replacement);
        }

        @Override
        public void updateSlotItems() {
            buffer.itemViews();
        }

        @Override
        public void updateSlotFluids() {
            buffer.fluidViews();
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
            if (wildcard && activeRecipe != null && !isEmpty()) {
                tag.setTag(KEY_ACTIVE_PATTERN, activeRecipe.original.writeToNBT(new NBTTagCompound()));
                tag.setString(KEY_ACTIVE_PATTERN_ID,
                    WildcardPatternGenerator.getGeneratedPatternId(activeRecipe.original));
            } else {
                tag.removeTag(KEY_ACTIVE_PATTERN);
                tag.removeTag(KEY_ACTIVE_PATTERN_ID);
            }
            return tag;
        }

        @Override
        public void refund(AENetworkProxy proxy, BaseActionSource source, boolean shouldDrop)
            throws GridAccessException {
            buffer.refund(proxy, source);
        }

        private ItemStack[] recipeTools() {
            if (activeRecipe == null) return GTValues.emptyItemStackArray;
            ItemStack[] tools = new ItemStack[activeRecipe.molds.length + (activeRecipe.circuit >= 0 ? 1 : 0)];
            addRecipeTools(tools, 0);
            return tools;
        }

        private void addRecipeTools(ItemStack[] inputs, int index) {
            if (activeRecipe == null) return;
            if (activeRecipe.circuit >= 0) inputs[index++] = GTUtility.getIntegratedCircuit(activeRecipe.circuit);
            for (ItemStack mold : activeRecipe.molds) inputs[index++] = mold.copy();
        }

        @Override
        public ItemStack[] getItemInputs() {
            List<ItemStack> items = buffer.itemViews();
            if (items.isEmpty() && buffer.fluidViews().isEmpty()) return GTValues.emptyItemStackArray;
            int toolCount = activeRecipe == null ? 0 : activeRecipe.molds.length + (activeRecipe.circuit >= 0 ? 1 : 0);
            if (items.isEmpty() && toolCount == 0) return GTValues.emptyItemStackArray;
            ItemStack[] inputs = items.toArray(new ItemStack[items.size() + toolCount]);
            addRecipeTools(inputs, items.size());
            return inputs;
        }

        @Override
        public FluidStack[] getFluidInputs() {
            List<FluidStack> fluids = buffer.fluidViews();
            return fluids.isEmpty() ? GTValues.emptyFluidStackArray : fluids.toArray(new FluidStack[fluids.size()]);
        }

        @Override
        public GTDualInputPattern getPatternInputs() {
            GTDualInputPattern inputs = new GTDualInputPattern();
            List<ItemStack> items = new ArrayList<>();
            List<FluidStack> fluids = new ArrayList<>();
            Collections.addAll(items, parentMTE.getSharedItems());
            ICraftingPatternDetails details = getPatternDetails();
            if (details != null) for (IAEStack<?> input : details.getAEInputs()) {
                if (input instanceof IAEItemStack item) items.add(item.getItemStack());
                else if (input instanceof IAEFluidStack fluid) fluids.add(fluid.getFluidStack());
            }
            Collections.addAll(items, recipeTools());
            inputs.inputItems = items.toArray(new ItemStack[0]);
            inputs.inputFluid = fluids.toArray(new FluidStack[0]);
            return inputs;
        }

        @Override
        public boolean insertItemsAndFluids(MEInventoryCrafting table) {
            return activeRecipe != null && buffer.insert(table, activeRecipe.molds);
        }
    }

    private static final class CircuitRecipe {

        private final CircuitSlot slot;
        private final ItemStack original;
        private final ICraftingPatternDetails details;
        private final int circuit;
        private final ItemStack[] molds;

        private CircuitRecipe(CircuitSlot slot, ItemStack original, ICraftingPatternDetails details, int circuit,
            ItemStack[] molds) {
            this.slot = slot;
            this.original = original;
            this.details = details;
            this.circuit = circuit;
            this.molds = molds;
        }
    }
}
