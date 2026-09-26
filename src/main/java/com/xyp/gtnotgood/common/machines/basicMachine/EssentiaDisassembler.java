package com.xyp.gtnotgood.common.machines.basicMachine;

import java.util.EnumSet;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

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
import gregtech.api.recipe.BasicUIProperties;
import gregtech.api.render.TextureFactory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

/**
 * HV GregTech single-block machine with native EU processing and direct ME essentia output.
 * The consumed item's yield remains in a persistent batch until processing and all ME insertions finish.
 * GT's base tile owns the AE proxy's unload/invalidation lifecycle.
 */
public final class EssentiaDisassembler extends MTEBasicMachine {

    private AENetworkProxy proxy;
    private AspectList batch = new AspectList();
    private boolean finished;
    private boolean transferring;

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
            // #tr tooltip.essentia_disassembler.processing
            // # Base duration: 20 ticks at 128 EU/t; follows global recipe speed settings
            // # zh_CN 基础耗时 20 tick，128 EU/t；遵循全局配方提速设置
            StatCollector.translateToLocal("tooltip.essentia_disassembler.processing"),
            // #tr tooltip.essentia_disassembler.network
            // # Outputs native essentia to ME; requires a powered channel and essentia storage
            // # zh_CN 源质直接存入 ME；需要供电、频道和源质存储
            StatCollector.translateToLocal("tooltip.essentia_disassembler.network"),
            // #tr tooltip.essentia_disassembler.buffer
            // # Pauses when full; buffered essentia survives saves and wrench removal
            // # zh_CN 存储满时暂停；缓存源质在存档和扳手拆卸后保留
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
            .build();
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
            var owner = tile.getWorld()
                .getPlayerEntityByName(tile.getOwnerName());
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

    @SuppressWarnings("unchecked")
    private IMEMonitor<AEEssentiaStack> monitor() throws GridAccessException {
        return (IMEMonitor<AEEssentiaStack>) getProxy().getStorage()
            .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
    }

    /** Only consumes input after all aspects pass the storage simulation; the batch then owns the output. */
    @Override
    public int checkRecipe() {
        if (batch.size() > 0 || !getProxy().isActive()) return DID_NOT_FIND_RECIPE;
        AspectList output = getEssentia(getInputAt(0));
        if (output.size() == 0) return DID_NOT_FIND_RECIPE;
        try {
            var inventory = monitor();
            var source = new MachineSource((IActionHost) getBaseMetaTileEntity());
            for (Aspect aspect : output.getAspects()) {
                var rest = inventory
                    .injectItems(new AEEssentiaStack(aspect, output.getAmount(aspect)), Actionable.SIMULATE, source);
                if (rest != null && rest.getStackSize() > 0) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
            }
            batch = output;
            finished = false;
            getBaseMetaTileEntity().decrStackSize(getInputSlot(), 1);
            mEUt = 128;
            mMaxProgresstime = processingDuration();
            markDirty();
            return FOUND_AND_SUCCESSFULLY_USED_RECIPE;
        } catch (GridAccessException ignored) {
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }
    }

    @Override
    protected boolean allowPutStackValidated(IGregTechTileEntity tile, int slot, ForgeDirection side, ItemStack stack) {
        return super.allowPutStackValidated(tile, slot, side, stack) && getEssentia(stack).size() > 0;
    }

    @Override
    public void endProcess() {
        finished = true;
        markDirty();
        flushBatch();
    }

    /** Flushes before GT checks the next recipe, allowing consecutive one-tick recipes. */
    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        if (tile.isServerSide()) {
            flushBatch();
            if (mMaxProgresstime <= 0 && batch.size() == 0 && tick % 20 == 0 && getInputAt(0) != null) {
                tile.setInventorySlotContents(getInputSlot(), getInputAt(0));
            }
        }
        super.onPostTick(tile, tick);
    }

    /** Debits only actual accepted essentia and blocks reentrant storage callbacks. */
    private void flushBatch() {
        if (!finished || batch.size() == 0 || transferring || !getProxy().isActive()) return;
        transferring = true;
        try {
            var inventory = monitor();
            var source = new MachineSource((IActionHost) getBaseMetaTileEntity());
            for (Aspect aspect : batch.getAspects()) {
                int amount = batch.getAmount(aspect);
                var rest = inventory.injectItems(new AEEssentiaStack(aspect, amount), Actionable.MODULATE, source);
                int remaining = rest == null ? 0 : (int) Math.min(amount, Math.max(0, rest.getStackSize()));
                batch.remove(aspect);
                if (remaining > 0) batch.add(aspect, remaining);
                markDirty();
            }
        } catch (GridAccessException ignored) {
            // Retain the batch until the network is accessible again.
        } finally {
            transferring = false;
        }
    }

    /**
     * Dynamic TC yields do not pass RecipeMapBackend.compileRecipe. Like the dynamic furnace backend,
     * apply the same central policy as RecipeSpeedMixin exactly once to the original duration.
     *
     * @return configured duration in ticks, at least one
     */
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
