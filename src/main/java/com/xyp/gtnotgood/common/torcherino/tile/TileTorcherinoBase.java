package com.xyp.gtnotgood.common.torcherino.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.torcherino.api.ITorcherinoTile;
import com.xyp.gtnotgood.common.torcherino.util.AccelerationHelper;
import com.xyp.gtnotgood.config.Config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiTextures;

/**
 * Base tile entity for area Torcherinos with configurable speed and X/Y/Z radius sliders.
 */
public abstract class TileTorcherinoBase extends TileEntity implements IGuiHolder<PosGuiData>, ITorcherinoTile {

    protected int speedLevel = 0;
    protected int xRadius = 0;
    protected int yRadius = 0;
    protected int zRadius = 0;
    protected boolean isStopped = false;
    protected boolean isActive = true;

    private byte cachedXRadius = -1;
    private byte cachedYRadius = -1;
    private byte cachedZRadius = -1;
    private int xMin;
    private int yMin;
    private int zMin;
    private int xMax;
    private int yMax;
    private int zMax;
    private long lastTickProcessed = -1L;

    @Override
    public boolean getActive() {
        return isActive;
    }

    @Override
    public void setActive(boolean active) {
        this.isActive = active;
        markDirty();
    }

    @Override
    public boolean isStopped() {
        return isStopped;
    }

    @Override
    public int getEffectiveSpeed() {
        return speedLevel * getSpeedMultiplier();
    }

    @Override
    public int getTorchX() {
        return xCoord;
    }

    @Override
    public int getTorchY() {
        return yCoord;
    }

    @Override
    public int getTorchZ() {
        return zCoord;
    }

    @Override
    public int getXRadius() {
        return xRadius;
    }

    @Override
    public int getYRadius() {
        return yRadius;
    }

    @Override
    public int getZRadius() {
        return zRadius;
    }

    /**
     * Returns the tier multiplier applied to the GUI speed level.
     *
     * @return speed multiplier for this torch tier
     */
    protected abstract int getSpeedMultiplier();

    /**
     * Returns the localized GUI title key for this tier.
     *
     * @return title translation key
     */
    protected abstract String getGuiTitleKey();

    /**
     * Returns a stable ModularUI panel id for this tier.
     *
     * @return panel id
     */
    protected abstract String getGuiPanelId();

    /**
     * Updates the saved speed level, clamped to config.
     *
     * @param level requested speed level
     */
    public void setSpeedLevel(int level) {
        this.speedLevel = clampInt(level, 0, Config.torcherinoMaxSpeedLevel);
        markDirty();
    }

    /**
     * Updates the X radius, clamped to config.
     *
     * @param radius requested radius
     */
    public void setXRadius(int radius) {
        this.xRadius = clampInt(radius, 0, Config.torcherinoMaxXRadius);
        markDirty();
    }

    /**
     * Updates the Y radius, clamped to config.
     *
     * @param radius requested radius
     */
    public void setYRadius(int radius) {
        this.yRadius = clampInt(radius, 0, Config.torcherinoMaxYRadius);
        markDirty();
    }

    /**
     * Updates the Z radius, clamped to config.
     *
     * @param radius requested radius
     */
    public void setZRadius(int radius) {
        this.zRadius = clampInt(radius, 0, Config.torcherinoMaxZRadius);
        markDirty();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(GTNotGood.MODID, mainPanel);
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        ModularPanel panel = ModularPanel.defaultPanel(getGuiPanelId(), 180, 186)
            .background(GTGuiTextures.BACKGROUND_STANDARD)
            .child(ButtonWidget.panelCloseButton());
        int multiplier = getSpeedMultiplier();

        DoubleSyncValue speedValue = new DoubleSyncValue(
            () -> (double) clampInt(speedLevel, 0, Config.torcherinoMaxSpeedLevel),
            value -> setSpeedLevel((int) Math.round(value))).allowC2S();
        DoubleSyncValue xRadiusValue = new DoubleSyncValue(
            () -> (double) clampInt(xRadius, 0, Config.torcherinoMaxXRadius),
            value -> setXRadius((int) Math.round(value))).allowC2S();
        DoubleSyncValue yRadiusValue = new DoubleSyncValue(
            () -> (double) clampInt(yRadius, 0, Config.torcherinoMaxYRadius),
            value -> setYRadius((int) Math.round(value))).allowC2S();
        DoubleSyncValue zRadiusValue = new DoubleSyncValue(
            () -> (double) clampInt(zRadius, 0, Config.torcherinoMaxZRadius),
            value -> setZRadius((int) Math.round(value))).allowC2S();

        Rectangle sliderBg = new Rectangle().color(0xFF3A3A3A);

        panel.child(
            IKey.lang(getGuiTitleKey())
                .asWidget()
                .pos(8, 6));
        // #tr gtnotgood.torcherino.gui.speed
        // # Speed
        // # zh_CN 速度
        panel.child(
            IKey.lang("gtnotgood.torcherino.gui.speed")
                .asWidget()
                .pos(8, 22));
        panel.child(
            buildSlider(speedValue, Config.torcherinoMaxSpeedLevel, sliderBg).pos(8, 32)
                .size(160, 10));
        panel.child(
            IKey.dynamic(
                () -> speedValue.getDoubleValue() == 0.0 ? "0%"
                    : ((int) speedValue.getDoubleValue() * multiplier * 100) + "%")
                .asWidget()
                .pos(78, 44));

        // #tr gtnotgood.torcherino.gui.x_range
        // # X Range
        // # zh_CN X 范围
        panel.child(
            IKey.lang("gtnotgood.torcherino.gui.x_range")
                .asWidget()
                .pos(8, 58));
        panel.child(
            buildSlider(xRadiusValue, Config.torcherinoMaxXRadius, sliderBg).pos(8, 68)
                .size(160, 10));
        panel.child(
            IKey.dynamic(() -> formatRangeValue(xRadiusValue))
                .asWidget()
                .pos(78, 80));

        // #tr gtnotgood.torcherino.gui.y_range
        // # Y Range
        // # zh_CN Y 范围
        panel.child(
            IKey.lang("gtnotgood.torcherino.gui.y_range")
                .asWidget()
                .pos(8, 94));
        panel.child(
            buildSlider(yRadiusValue, Config.torcherinoMaxYRadius, sliderBg).pos(8, 104)
                .size(160, 10));
        panel.child(
            IKey.dynamic(() -> formatRangeValue(yRadiusValue))
                .asWidget()
                .pos(78, 116));

        // #tr gtnotgood.torcherino.gui.z_range
        // # Z Range
        // # zh_CN Z 范围
        panel.child(
            IKey.lang("gtnotgood.torcherino.gui.z_range")
                .asWidget()
                .pos(8, 130));
        panel.child(
            buildSlider(zRadiusValue, Config.torcherinoMaxZRadius, sliderBg).pos(8, 140)
                .size(160, 10));
        panel.child(
            IKey.dynamic(() -> formatRangeValue(zRadiusValue))
                .asWidget()
                .pos(78, 152));

        panel.child(
            IKey.dynamic(() -> buildRangeSummary(xRadiusValue, yRadiusValue, zRadiusValue))
                .asWidget()
                .pos(8, 166));

        return panel;
    }

    private static SliderWidget buildSlider(DoubleSyncValue value, int configuredMax, Rectangle sliderBg) {
        return new SliderWidget().value(value)
            .bounds(0, Math.max(1, configuredMax))
            .stopper(1.0D)
            .background(sliderBg);
    }

    private static String formatRangeValue(DoubleSyncValue value) {
        return String.valueOf((int) value.getDoubleValue() * 2 + 1);
    }

    private static String buildRangeSummary(DoubleSyncValue xRadiusValue, DoubleSyncValue yRadiusValue,
        DoubleSyncValue zRadiusValue) {
        int x = (int) xRadiusValue.getDoubleValue() * 2 + 1;
        int y = (int) yRadiusValue.getDoubleValue() * 2 + 1;
        int z = (int) zRadiusValue.getDoubleValue() * 2 + 1;
        return x + "x" + y + "x" + z;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        speedLevel = clampInt(compound.getInteger("SpeedLevel"), 0, Config.torcherinoMaxSpeedLevel);
        isStopped = compound.getBoolean("IsStopped");
        isActive = !compound.hasKey("IsActive") || compound.getBoolean("IsActive");
        xRadius = clampInt(compound.getInteger("XRadius"), 0, Config.torcherinoMaxXRadius);
        yRadius = clampInt(compound.getInteger("YRadius"), 0, Config.torcherinoMaxYRadius);
        zRadius = clampInt(compound.getInteger("ZRadius"), 0, Config.torcherinoMaxZRadius);

        if (compound.hasKey("TimeRate") && !compound.hasKey("SpeedLevel")) {
            speedLevel = clampInt(
                compound.getInteger("TimeRate") / getSpeedMultiplier(),
                0,
                Config.torcherinoMaxSpeedLevel);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("SpeedLevel", speedLevel);
        compound.setBoolean("IsStopped", isStopped);
        compound.setBoolean("IsActive", isActive);
        compound.setInteger("XRadius", xRadius);
        compound.setInteger("YRadius", yRadius);
        compound.setInteger("ZRadius", zRadius);
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;

        long currentTick = worldObj.getTotalWorldTime();
        if (lastTickProcessed == currentTick) return;
        lastTickProcessed = currentTick;

        if (!isActive || isStopped || speedLevel <= 0) return;

        int effectiveSpeed = getEffectiveSpeed();
        if (effectiveSpeed <= 0) return;

        if (cachedXRadius != (byte) xRadius || cachedYRadius != (byte) yRadius || cachedZRadius != (byte) zRadius) {
            updateCachedBounds();
        }

        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    AccelerationHelper.accelerateAtPosition(worldObj, xCoord, yCoord, zCoord, effectiveSpeed, x, y, z);
                }
            }
        }
    }

    private void updateCachedBounds() {
        xMin = xCoord - xRadius;
        yMin = yCoord - yRadius;
        zMin = zCoord - zRadius;
        xMax = xCoord + xRadius;
        yMax = yCoord + yRadius;
        zMax = zCoord + zRadius;
        cachedXRadius = (byte) xRadius;
        cachedYRadius = (byte) yRadius;
        cachedZRadius = (byte) zRadius;
    }

    private static int clampInt(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }
}
