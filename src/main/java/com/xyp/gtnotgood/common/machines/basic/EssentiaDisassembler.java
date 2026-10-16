package com.xyp.gtnotgood.common.machines.basic;

import java.util.EnumSet;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.util.AECableType;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import gregtech.api.enums.Textures;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.BasicUIProperties;
import gregtech.api.render.TextureFactory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

/** Consumed inputs remain in a persistent batch until processing and ME insertion finish. */
public final class EssentiaDisassembler extends MTEBasicMachine {

    private static final int MAX_OUTPUT_RETRY_DELAY = 20;
    private static final int ESSENTIA_CACHE_DURATION = 20;

    private AENetworkProxy proxy;
    private AspectList batch = new AspectList();
    private boolean finished;
    private boolean transferring;
    private long nextOutputAttemptTick;
    private int outputRetryDelay = 1;
    private ItemStack cachedInput;
    private AspectList cachedEssentia = new AspectList();
    private long essentiaCacheExpiresAt;
    private ItemStack blockedInput;

    enum DisplayState {
        Idle,
        Processing,
        NoPower,
        NetworkUnavailable,
        WaitingOutput,
        Disabled,
        NoEssentia
    }

    public EssentiaDisassembler(int id, String name, String localizedName) {
        super(id, name, localizedName, 3, 1, description(), 1, 0);
    }

    private EssentiaDisassembler(String name, String[] description, ITexture[][][] textures) {
        super(name, 3, 1, description, textures, 1, 0);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new EssentiaDisassembler(mName, mDescriptionArray, mTextures);
    }

    private static String[] description() {
        return new String[] {
            // #tr tooltip.essentia_disassembler.function
            // # Breaks items down into essentia and stores it in ME
            // # zh_CN 将物品分解为源质并存入 ME 网络
            StatCollector.translateToLocal("tooltip.essentia_disassembler.function"),
            // #tr tooltip.essentia_disassembler.processing
            // # Base processing time: 1 s at 128 EU/t; affected by recipe speed settings
            // # zh_CN 基础耗时：1 秒，128 EU/t；受配方提速设置影响
            StatCollector.translateToLocal("tooltip.essentia_disassembler.processing"),
            // #tr tooltip.essentia_disassembler.network
            // # Requires a powered ME channel and essentia storage
            // # zh_CN 需要 ME 供电、一个频道和源质存储空间
            StatCollector.translateToLocal("tooltip.essentia_disassembler.network"),
            // #tr tooltip.essentia_disassembler.buffer
            // # Waits when ME storage is full; buffered essentia survives saves and wrench pickup
            // # zh_CN ME 存储满时等待；缓存源质在保存和扳手拆卸后保留
            StatCollector.translateToLocal("tooltip.essentia_disassembler.buffer") };
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    /** Dynamic aspect processing uses the standard GT progress widget without a static recipe map. */
    @Override
    protected BasicUIProperties getUIProperties() {
        return super.getUIProperties().toBuilder()
            .progressBarTexture(GTUITextures.fallbackableProgressbar("extract", GTUITextures.PROGRESSBAR_EXTRACT))
            .progressBarTextureMUI2(GTGuiTextures.PROGRESSBAR_EXTRACT).build();
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new EssentiaDisassemblerGui(this, getUIProperties()).build(data, syncManager, uiSettings);
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    public ITexture[] getFrontFacingActive(byte color) {
        return new ITexture[] { Textures.BlockIcons.MACHINE_CASINGS[mTier][color + 1],
            TextureFactory.of(Textures.BlockIcons.OVERLAY_FRONT_INDUSTRIAL_EXTRACTOR_ACTIVE) };
    }

    @Override
    public ITexture[] getFrontFacingInactive(byte color) {
        return new ITexture[] { Textures.BlockIcons.MACHINE_CASINGS[mTier][color + 1],
            TextureFactory.of(Textures.BlockIcons.OVERLAY_FRONT_INDUSTRIAL_EXTRACTOR) };
    }

    @Override
    public AENetworkProxy getProxy() {
        if (proxy == null && getBaseMetaTileEntity() instanceof IGridProxyable host) {
            proxy = new AENetworkProxy(host, "essentia_proxy", GTNGItemList.EssentiaDisassembler.get(1), true);
            proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            proxy.setIdlePowerUsage(1);
            proxy.setValidSides(EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)));
        }
        return proxy;
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection side) {
        return AECableType.SMART;
    }

    @Override
    public void onFirstTick(IGregTechTileEntity tile) {
        super.onFirstTick(tile);
        if (tile.isServerSide()) {
            var owner = tile.getWorld().getPlayerEntityByName(tile.getOwnerName());
            if (owner != null) getProxy().setOwner(owner);
            getProxy().onReady();
        }
    }

    /** Matches the TC furnace, including bonus tags, while isolating shared registry data from mutation. */
    public static AspectList getEssentia(ItemStack input) {
        if (input == null || input.stackSize <= 0) return new AspectList();
        ItemStack single = input.copy();
        single.stackSize = 1;
        AspectList base = ThaumcraftCraftingManager.getObjectTags(single);
        AspectList result = ThaumcraftCraftingManager.getBonusTags(single, base == null ? null : base.copy());
        AspectList clean = new AspectList();
        if (result != null) for (Aspect aspect : result.getAspects()) {
            if (aspect != null && result.getAmount(aspect) > 0) clean.add(aspect, result.getAmount(aspect));
        }
        return clean;
    }

    private AspectList getCachedEssentia(ItemStack input) {
        if (input == null || input.stackSize <= 0) return new AspectList();
        long tick = getBaseMetaTileEntity().getTimer();
        if (!isSameInput(cachedInput, input) || tick >= essentiaCacheExpiresAt) {
            cachedInput = input.copy();
            cachedInput.stackSize = 1;
            cachedEssentia = getEssentia(cachedInput);
            // Refresh even unchanged stacks so runtime aspect registrations cannot leave a permanent stale result.
            essentiaCacheExpiresAt = tick + ESSENTIA_CACHE_DURATION;
        }
        return cachedEssentia.copy();
    }

    private static boolean isSameInput(ItemStack first, ItemStack second) {
        return first != null && second != null
            && first.getItem() == second.getItem()
            && first.getItemDamage() == second.getItemDamage()
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    AspectList getDisplayedEssentia() {
        return batch.size() > 0 ? batch.copy() : getCachedEssentia(getInputAt(0));
    }

    DisplayState getDisplayState() {
        var tile = getBaseMetaTileEntity();
        if (batch.size() > 0) {
            if (!finished) {
                return mProgresstime < 0 || tile.getStoredEU() < mEUt ? DisplayState.NoPower : DisplayState.Processing;
            }
            return getProxy().isActive() ? DisplayState.WaitingOutput : DisplayState.NetworkUnavailable;
        }
        if (!tile.isAllowedToWork()) return DisplayState.Disabled;
        ItemStack input = getInputAt(0);
        if (input == null || input.stackSize <= 0) return DisplayState.Idle;
        if (getCachedEssentia(input).size() == 0) return DisplayState.NoEssentia;
        if (!getProxy().isActive()) return DisplayState.NetworkUnavailable;
        if (isSameInput(blockedInput, input)) return DisplayState.WaitingOutput;
        return hasEnoughEnergyToCheckRecipe() ? DisplayState.Idle : DisplayState.NoPower;
    }

    @SuppressWarnings("unchecked")
    private IMEMonitor<AEEssentiaStack> monitor() throws GridAccessException {
        return (IMEMonitor<AEEssentiaStack>) getProxy().getStorage()
            .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
    }

    /** Only consumes input after all aspects pass the storage simulation; the batch then owns the output. */
    @Override
    public int checkRecipe() {
        if (batch.size() > 0 || !getProxy().isActive()) return DID_NOT_FIND_RECIPE;
        blockedInput = null;
        AspectList output = getCachedEssentia(getInputAt(0));
        if (output.size() == 0) return DID_NOT_FIND_RECIPE;
        try {
            var inventory = monitor();
            var source = new MachineSource((IActionHost) getBaseMetaTileEntity());
            for (Aspect aspect : output.getAspects()) {
                var rest = inventory.injectItems(new AEEssentiaStack(aspect, output.getAmount(aspect)),
                    Actionable.SIMULATE, source);
                if (rest != null && rest.getStackSize() > 0) {
                    blockedInput = getInputAt(0).copy();
                    return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
                }
            }
            batch = output;
            finished = false;
            getBaseMetaTileEntity().decrStackSize(getInputSlot(), 1);
            mEUt = 128;
            mMaxProgresstime = processingDuration();
            markDirty();
            return FOUND_AND_SUCCESSFULLY_USED_RECIPE;
        } catch (GridAccessException ignored) {
            blockedInput = getInputAt(0).copy();
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }
    }

    @Override
    protected boolean allowPutStackValidated(IGregTechTileEntity tile, int slot, ForgeDirection side, ItemStack stack) {
        return super.allowPutStackValidated(tile, slot, side, stack) && getCachedEssentia(stack).size() > 0;
    }

    @Override
    public void endProcess() {
        finished = true;
        resetOutputRetry();
        markDirty();
        flushBatch();
    }

    /** Flushes before GT checks the next recipe, allowing consecutive one-tick recipes. */
    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        if (tile.isServerSide()) {
            flushBatch();
            if (mMaxProgresstime <= 0 && batch.size() == 0 && tick % 20 == 0 && getInputAt(0) != null) {
                tile.markInventoryBeenModified();
            }
        }
        super.onPostTick(tile, tick);
    }

    /** Debits only actual accepted essentia and blocks reentrant storage callbacks. */
    private void flushBatch() {
        if (!finished || batch.size() == 0 || transferring || !getProxy().isActive()) return;
        long tick = getBaseMetaTileEntity().getTimer();
        if (tick < nextOutputAttemptTick) return;
        transferring = true;
        boolean changed = false;
        try {
            var inventory = monitor();
            var source = new MachineSource((IActionHost) getBaseMetaTileEntity());
            for (Aspect aspect : batch.getAspects()) {
                int amount = batch.getAmount(aspect);
                var rest = inventory.injectItems(new AEEssentiaStack(aspect, amount), Actionable.MODULATE, source);
                int remaining = rest == null ? 0 : (int) Math.min(amount, Math.max(0, rest.getStackSize()));
                if (remaining == amount) continue;
                batch.remove(aspect);
                if (remaining > 0) batch.add(aspect, remaining);
                changed = true;
            }
        } catch (GridAccessException ignored) {
            // Retain the batch until the network is accessible again.
        } finally {
            transferring = false;
            if (changed) markDirty();
            if (batch.size() == 0) resetOutputRetry();
            else if (changed) {
                outputRetryDelay = 1;
                nextOutputAttemptTick = tick + 1;
            } else {
                nextOutputAttemptTick = tick + outputRetryDelay;
                outputRetryDelay = Math.min(MAX_OUTPUT_RETRY_DELAY, outputRetryDelay * 2);
            }
        }
    }

    private void resetOutputRetry() {
        nextOutputAttemptTick = 0;
        outputRetryDelay = 1;
    }

    /** Applies global speed settings once to each dynamic recipe. */
    public static int processingDuration() {
        Config.ensureLoaded();
        return Config.getModifiedRecipeDuration(20);
    }

    private void saveBatch(NBTTagCompound tag) {
        NBTTagCompound contents = new NBTTagCompound();
        batch.writeToNBT(contents);
        contents.setBoolean("Finished", finished);
        tag.setTag("EssentiaBatch", contents);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        saveBatch(tag);
        if (getProxy() != null) getProxy().writeToNBT(tag);
    }

    /** Wrench drops keep the batch but never copy an AE node identity into the machine item. */
    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        saveBatch(tag);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        resetOutputRetry();
        cachedInput = null;
        cachedEssentia = new AspectList();
        essentiaCacheExpiresAt = 0;
        blockedInput = null;
        NBTTagCompound contents = tag.getCompoundTag("EssentiaBatch");
        batch = new AspectList();
        batch.readFromNBT(contents);
        finished = contents.getBoolean("Finished");
        if (!finished && batch.size() > 0 && mMaxProgresstime <= 0) {
            mEUt = 128;
            mMaxProgresstime = processingDuration();
            mProgresstime = 0;
        }
        if (tag.hasKey("essentia_proxy") && getProxy() != null) getProxy().readFromNBT(tag);
    }
}
