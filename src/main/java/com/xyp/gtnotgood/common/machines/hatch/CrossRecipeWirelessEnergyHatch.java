package com.xyp.gtnotgood.common.machines.hatch;

import java.math.BigInteger;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.misc.WirelessNetworkManager;

/** Account access and settings only; recipe inventories and paid progress belong to the controller. */
@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class CrossRecipeWirelessEnergyHatch extends MTEHatchEnergy {

    public static final int MAX_TASKS = 64;
    private int duration = 128;
    private int taskLimit = 16;
    private BigInteger parallelLimit = BigInteger.valueOf(Integer.MAX_VALUE);

    public CrossRecipeWirelessEnergyHatch(int id, String name) {
        super(id, name, "", 14);
    }

    private CrossRecipeWirelessEnergyHatch(String name, String[] description, ITexture[][][] textures) {
        super(name, 14, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new CrossRecipeWirelessEnergyHatch(mName, mDescriptionArray, mTextures);
    }

    @Override
    public String getLocalName() {
        // #tr gtng.cross_wireless.name
        // # Cross-Recipe Wireless Energy Hatch
        // # zh_CN 跨配方无线能源仓
        return StatCollector.translateToLocal("gtng.cross_wireless.name");
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr gtng.cross_wireless.desc.0
            // # Runs different recipes concurrently using the owner's GT wireless EU.
            // # zh_CN 使用仓主的GT无线电网，同时运行不同配方。
            StatCollector.translateToLocal("gtng.cross_wireless.desc.0"),
            // #tr gtng.cross_wireless.desc.1
            // # Base recipe energy cost; adjustable time, minimum 1 tick.
            // # zh_CN 按配方基础总耗电扣费；时间可调，最低1 tick。
            StatCollector.translateToLocal("gtng.cross_wireless.desc.1"),
            // #tr gtng.cross_wireless.desc.2
            // # Right-click to configure. Requires a compatible recipe-map controller.
            // # zh_CN 右键设置。需要兼容的配方表多方块控制器。
            StatCollector.translateToLocal("gtng.cross_wireless.desc.2") };
    }

    public int getDuration() {
        return duration;
    }

    public int getTaskLimit() {
        return taskLimit;
    }

    public BigInteger getParallelLimit() {
        return parallelLimit;
    }

    public void setDuration(int value) {
        duration = Math.max(1, value);
        changed();
    }

    public void setTaskLimit(int value) {
        taskLimit = Math.max(1, Math.min(MAX_TASKS, value));
        changed();
    }

    public void setParallelLimit(int value) {
        setParallelLimit(BigInteger.valueOf(value));
    }

    public void setParallelLimit(BigInteger value) {
        parallelLimit = value.max(BigInteger.ZERO);
        changed();
    }

    private void setParallelText(String value) {
        // Bound network text parsing, not the actual batch counter; zero removes the configured limit.
        if (value == null || value.length() > 128 || !value.matches("[0-9]+")) return;
        setParallelLimit(new BigInteger(value));
    }

    private void changed() {
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) markDirty();
    }

    @Override
    public long maxEUStore() {
        return 0;
    }

    @Override
    public long maxEUInput() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long maxAmperesIn() {
        return 1;
    }

    @Override
    public boolean isEnetInput() {
        return false;
    }

    @Override
    public boolean isInputFacing(ForgeDirection side) {
        return false;
    }

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return new ITexture[] { base, Textures.BlockIcons.OVERLAYS_ENERGY_ON_WIRELESS_LASER[15] };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return getTexturesActive(base);
    }

    @Override
    public void onFirstTick(IGregTechTileEntity tile) {
        super.onFirstTick(tile);
        if (tile.isServerSide()) WirelessNetworkManager.processInitialSettings(tile);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger("crossDuration", duration);
        tag.setInteger("crossTasks", taskLimit);
        tag.setString("crossParallel", parallelLimit.toString());
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        if (tag.hasKey("crossDuration")) duration = Math.max(1, tag.getInteger("crossDuration"));
        if (tag.hasKey("crossTasks")) taskLimit = Math.max(1, Math.min(MAX_TASKS, tag.getInteger("crossTasks")));
        if (tag.hasKey("crossParallel", 8)) setParallelText(tag.getString("crossParallel"));
        else if (tag.hasKey("crossParallel"))
            parallelLimit = BigInteger.valueOf(Math.max(0, tag.getInteger("crossParallel")));
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
        if (tile.isServerSide()) openGui(player);
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new SettingsGui(this).build(data, sync, settings);
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    private static final class SettingsGui extends MTEHatchBaseGui<CrossRecipeWirelessEnergyHatch> {

        private SettingsGui(CrossRecipeWirelessEnergyHatch machine) {
            super(machine);
        }

        @Override
        protected int getBasePanelHeight() {
            return 208;
        }

        @Override
        protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager sync) {
            ParentWidget<?> content = getEmptyContent();
            IntSyncValue duration = new IntSyncValue(machine::getDuration, machine::setDuration).allowC2S();
            IntSyncValue tasks = new IntSyncValue(machine::getTaskLimit, machine::setTaskLimit).allowC2S();
            StringSyncValue parallels = new StringSyncValue(() -> machine.getParallelLimit().toString(),
                machine::setParallelText).allowC2S();
            sync.syncValue("duration", duration);
            sync.syncValue("tasks", tasks);
            sync.syncValue("parallels", parallels);
            // #tr gtng.cross_wireless.duration
            // # Completion time (ticks)
            // # zh_CN 完成时间（tick）
            content.child(IKey.lang("gtng.cross_wireless.duration").asWidget().pos(0, 0));
            content.child(field(duration, Integer.MAX_VALUE).pos(0, 12));
            // #tr gtng.cross_wireless.tasks
            // # Concurrent recipes
            // # zh_CN 同时运行的配方数
            content.child(IKey.lang("gtng.cross_wireless.tasks").asWidget().pos(0, 34));
            content.child(field(tasks, MAX_TASKS).pos(0, 46));
            // #tr gtng.cross_wireless.parallels
            // # Parallel limit (0 = unlimited)
            // # zh_CN 单配方并行上限（0=无限制）
            content.child(IKey.lang("gtng.cross_wireless.parallels").asWidget().pos(0, 68));
            content.child(new TextFieldWidget().value(parallels).autoUpdateOnChange(false).setMaxLength(128)
                .setPattern(Pattern.compile("[0-9]*")).background(GTGuiTextures.BACKGROUND_TEXT_FIELD).size(156, 18)
                .pos(0, 80));
            return content;
        }

        private TextFieldWidget field(IntSyncValue value, int max) {
            return new TextFieldWidget().value(value).numbersInt(1, max).background(GTGuiTextures.BACKGROUND_TEXT_FIELD)
                .size(140, 18);
        }
    }
}
