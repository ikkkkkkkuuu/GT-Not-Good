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

    private static final String[] HULL_TIERS = { "iv", "luv", "zpm", "uv", "uhv", "uev", "uiv", "uxv", "opv", "max" };
    private static final ITexture[][] HULLS = new ITexture[HULL_TIERS.length][3];
    private static final ITexture[][] OVERLAYS = new ITexture[HULL_TIERS.length][4];
    static {
        String[] faces = { "bottom", "top", "side" };
        String[] states = { "", "_emissive", "_active", "_active_emissive" };
        for (int i = 0; i < HULL_TIERS.length; i++) {
            for (int face = 0; face < faces.length; face++)
                HULLS[i][face] = texture("parallel/hulls/" + HULL_TIERS[i] + "/" + faces[face], false);
            for (int state = 0; state < states.length; state++)
                OVERLAYS[i][state] = texture("parallel/hatch_" + (i + 1) + states[state], state % 2 == 1);
        }
    }
    private int parallel = 1;

    private static ITexture texture(String path, boolean glow) {
        IIconContainer icon = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(), path);
        return glow ? TextureFactory.builder().addIcon(icon).glow().build() : TextureFactory.of(icon);
    }

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
        return new ITexture[] { base, OVERLAYS[mTier - 5][2], OVERLAYS[mTier - 5][3] };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return new ITexture[] { base, OVERLAYS[mTier - 5][0], OVERLAYS[mTier - 5][1] };
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int colorIndex,
        boolean active, boolean redstone) {
        ITexture casing = HULLS[mTier - 5][side == ForgeDirection.DOWN ? 0 : side == ForgeDirection.UP ? 1 : 2];
        if (side != facing) return new ITexture[] { casing };
        return active ? getTexturesActive(casing) : getTexturesInactive(casing);
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
