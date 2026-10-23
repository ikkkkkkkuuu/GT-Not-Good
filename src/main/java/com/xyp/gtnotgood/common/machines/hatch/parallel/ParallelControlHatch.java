package com.xyp.gtnotgood.common.machines.hatch.parallel;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.common.gui.modularui.hatch.ParallelControlHatchGui;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class ParallelControlHatch extends MTEHatch {

    private static final IIconContainer[] OVERLAYS = new IIconContainer[4];
    static {
        for (int i = 0; i < 4; i++) OVERLAYS[i] = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
            "parallel/hatch_" + (i + 1));
    }
    private int parallel = 1;

    public ParallelControlHatch(int id, String name, int tier) {
        super(id, name, "", tier, 0, new String[0]);
    }

    private ParallelControlHatch(String name, int tier, ITexture[][][] textures) {
        super(name, tier, 0, new String[0], textures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new ParallelControlHatch(mName, mTier, mTextures);
    }

    public int getMaximumParallel() {
        return (int) Math.min(Integer.MAX_VALUE, 1L << (2 * Math.max(0, mTier - 4)));
    }

    public int getParallel() {
        return parallel;
    }

    public void setParallel(int value) {
        parallel = Math.max(1, Math.min(getMaximumParallel(), value));
        markDirty();
    }

    @Override
    public String getLocalName() {
        // #tr gtng.parallel_hatch.name
        // # %s Parallel Control Hatch
        // # zh_CN %s 并行控制仓
        return StatCollector.translateToLocalFormatted("gtng.parallel_hatch.name", GTValues.VN[mTier]);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr gtng.parallel_hatch.purpose
            // # Multiplies the base parallel limit of compatible multiblocks
            // # zh_CN 倍增兼容多方块的基准并行数
            StatCollector.translateToLocal("gtng.parallel_hatch.purpose"),
            // #tr gtng.parallel_hatch.range
            // # Adjustable multiplier: 1 - %s; affects new batches
            // # zh_CN 可调倍率：1 - %s；对新批次生效
            StatCollector.translateToLocalFormatted("gtng.parallel_hatch.range", getMaximumParallel()),
            // #tr gtng.parallel_hatch.limit
            // # At most one per machine; does not add threads or energy
            // # zh_CN 每台机器最多 1 个；不增加线程或供电能力
            StatCollector.translateToLocal("gtng.parallel_hatch.limit") };
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger("parallel", parallel);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        parallel = Math.max(1, Math.min(getMaximumParallel(), tag.getInteger("parallel")));
    }

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return getTexturesInactive(base);
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(OVERLAYS[Math.min(3, Math.max(0, mTier - 5))]) };
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new ParallelControlHatchGui(this).build(data, sync, settings);
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
        if (tile.isServerSide()) openGui(player);
        return true;
    }

    @Override
    public boolean isFacingValid(ForgeDirection side) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isValidSlot(int index) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity t, int i, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity t, int i, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean willExplodeInRain() {
        return false;
    }
}
