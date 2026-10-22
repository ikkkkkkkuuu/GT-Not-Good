// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.hatch.modular;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class ParameterModuleHatch extends ModularHatch {

    private static final int[] PARALLEL = { 8, 128, 2048, 32768, 524288, 8388608, 134217728, Integer.MAX_VALUE };
    private static final double[] POWER = { 0.95, 0.9, 0.85, 0.8, 0.75, 0.7, 0.5, 0.25 };
    private final int moduleTier;
    private final boolean dynamic;
    private int parameter = 1;

    public ParameterModuleHatch(int id, String name, ModuleType type, int moduleTier, boolean dynamic) {
        super(id, name, type == ModuleType.Overclock ? moduleTier + 11 : moduleTier + 6, type);
        this.moduleTier = moduleTier;
        this.dynamic = dynamic;
    }

    private ParameterModuleHatch(String name, ModuleType type, int moduleTier, boolean dynamic,
        ITexture[][][] textures) {
        super(name, type == ModuleType.Overclock ? moduleTier + 11 : moduleTier + 6, type, textures);
        this.moduleTier = moduleTier;
        this.dynamic = dynamic;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new ParameterModuleHatch(mName, getModuleType(), moduleTier, dynamic, mTextures);
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public int getParameter() {
        return dynamic ? parameter : getMaximumParameter();
    }

    public int getMaximumParameter() {
        return getModuleType() == ModuleType.Parallel ? PARALLEL[moduleTier - 1] : 1 << moduleTier;
    }

    public void setParameter(int value) {
        if (!dynamic) return;
        parameter = Math.max(1, Math.min(getMaximumParameter(), value));
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) markDirty();
    }

    public double getPowerMultiplier() {
        return getModuleType() == ModuleType.PowerConsumption ? POWER[moduleTier - 1] : 1;
    }

    public int getTimeReduction() {
        return getTimeReduction(moduleTier);
    }

    public static int getTimeReduction(int moduleTier) {
        return moduleTier == 1 ? 4 : 8;
    }

    public int getPowerIncrease() {
        return getPowerIncrease(moduleTier);
    }

    public static int getPowerIncrease(int moduleTier) {
        return moduleTier == 3 ? 2 : 4;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        if (dynamic) tag.setInteger("moduleParameter", parameter);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        if (dynamic && tag.hasKey("moduleParameter")) {
            parameter = Math.max(1, Math.min(getMaximumParameter(), tag.getInteger("moduleParameter")));
        }
    }

    @Override
    public String getLocalName() {
        return switch (getModuleType()) {
            case Parallel -> parallelName();
            case Speed -> speedName();
            case PowerConsumption -> powerName();
            case Overclock -> overclockName();
            default -> throw new IllegalStateException("Unexpected parameter module");
        };
    }

    private String parallelName() {
        if (dynamic) {
            // #tr gtng.module.parallel.dynamic.name
            // # Dynamic Parallel Controller Module T%s
            // # zh_CN 动态并行控制器模块 T%s
            return StatCollector.translateToLocalFormatted("gtng.module.parallel.dynamic.name", moduleTier);
        }
        // #tr gtng.module.parallel.static.name
        // # Static Parallel Controller Module T%s
        // # zh_CN 静态并行控制器模块 T%s
        return StatCollector.translateToLocalFormatted("gtng.module.parallel.static.name", moduleTier);
    }

    private String speedName() {
        if (dynamic) {
            // #tr gtng.module.speed.dynamic.name
            // # Dynamic Speed Controller Module T%s
            // # zh_CN 动态速度控制器模块 T%s
            return StatCollector.translateToLocalFormatted("gtng.module.speed.dynamic.name", moduleTier);
        }
        // #tr gtng.module.speed.static.name
        // # Static Speed Controller Module T%s
        // # zh_CN 静态速度控制器模块 T%s
        return StatCollector.translateToLocalFormatted("gtng.module.speed.static.name", moduleTier);
    }

    private String powerName() {
        // #tr gtng.module.power.name
        // # Power Consumption Controller Module T%s
        // # zh_CN 能耗控制器模块 T%s
        return StatCollector.translateToLocalFormatted("gtng.module.power.name", moduleTier);
    }

    private String overclockName() {
        if (moduleTier == 1) {
            // #tr gtng.module.overclock.low.name
            // # Low Speed Perfect Overclock Controller Module
            // # zh_CN 低速无损超频控制器模块
            return StatCollector.translateToLocal("gtng.module.overclock.low.name");
        }
        if (moduleTier == 2) {
            // #tr gtng.module.overclock.perfect.name
            // # Perfect Overclock Controller Module
            // # zh_CN 无损超频控制器模块
            return StatCollector.translateToLocal("gtng.module.overclock.perfect.name");
        }
        // #tr gtng.module.overclock.singularity.name
        // # Singularity Perfect Overclock Controller Module
        // # zh_CN 奇点无损超频控制器模块
        return StatCollector.translateToLocal("gtng.module.overclock.singularity.name");
    }

    @Override
    public String[] getDescription() {
        // #tr gtng.module.parameter.description
        // # Adjusts processing parameters of compatible modular multiblocks
        // # zh_CN 调整兼容模块化多方块机器的处理参数
        String purpose = StatCollector.translateToLocal("gtng.module.parameter.description");
        // #tr gtng.module.parameter.limit
        // # One module of each parameter type per machine
        // # zh_CN 每台机器的每类参数模块限装一个
        String limit = StatCollector.translateToLocal("gtng.module.parameter.limit");
        String effect = switch (getModuleType()) {
            case Parallel -> parallelDescription();
            case Speed -> speedDescription();
            case PowerConsumption -> powerDescription();
            case Overclock -> overclockDescription();
            default -> throw new IllegalStateException("Unexpected parameter module");
        };
        if (dynamic) {
            // #tr gtng.module.parameter.controls
            // # Right-click to set the parameter; changes apply to new recipe tasks
            // # zh_CN 右键设置参数；修改对新配方任务生效
            return new String[] { purpose, effect, limit,
                StatCollector.translateToLocal("gtng.module.parameter.controls") };
        }
        return new String[] { purpose, effect, limit };
    }

    private String parallelDescription() {
        // #tr gtng.module.parallel.effect
        // # Parallel limit: base x (1 + parameter); parameter up to {\\AQUA}%s{\\RESET}
        // # zh_CN 并行上限：基础值 x（1 + 参数）；参数最高 {\\AQUA}%s{\\RESET}
        return StatCollector.translateToLocalFormatted("gtng.module.parallel.effect",
            NumberFormatUtil.formatNumber(getMaximumParameter()));
    }

    private String speedDescription() {
        // #tr gtng.module.speed.effect
        // # Processing speed multiplier: up to {\\AQUA}%sx{\\RESET}
        // # zh_CN 处理速度倍率：最高 {\\AQUA}%s 倍{\\RESET}
        return StatCollector.translateToLocalFormatted("gtng.module.speed.effect", getMaximumParameter());
    }

    private String powerDescription() {
        // #tr gtng.module.power.effect
        // # EU/t multiplier: {\\AQUA}%s%%{\\RESET}
        // # zh_CN EU/t 倍率：{\\AQUA}%s%%{\\RESET}
        return StatCollector.translateToLocalFormatted("gtng.module.power.effect",
            Math.round(getPowerMultiplier() * 100));
    }

    private String overclockDescription() {
        // #tr gtng.module.overclock.effect
        // # Each overclock: processing time / {\\AQUA}%s{\\RESET}, EU/t x {\\YELLOW}%s{\\RESET}
        // # zh_CN 每次超频：耗时 / {\\AQUA}%s{\\RESET}，EU/t x {\\YELLOW}%s{\\RESET}
        return StatCollector.translateToLocalFormatted("gtng.module.overclock.effect", getTimeReduction(),
            getPowerIncrease());
    }

    @Override
    protected String getOverlayName(boolean active) {
        return switch (getModuleType()) {
            case Parallel -> "OVERLAY_ParallelController";
            case Speed -> "OVERLAY_SpeedController";
            case PowerConsumption -> "OVERLAY_PowerController";
            case Overclock -> "OVERLAY_OverClockController_" + switch (moduleTier) {
                    case 1 -> "Pri";
                    case 2 -> "Adv";
                    default -> "Per";
                };
            default -> throw new IllegalStateException("Unexpected parameter module");
        };
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
        if (dynamic && tile.isServerSide()) openGui(player);
        return true;
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new SettingsGui(this).build(data, sync, settings);
    }

    private static final class SettingsGui extends MTEHatchBaseGui<ParameterModuleHatch> {

        private SettingsGui(ParameterModuleHatch machine) {
            super(machine);
        }

        @Override
        protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager sync) {
            ParentWidget<?> content = getEmptyContent();
            IntSyncValue value = new IntSyncValue(machine::getParameter, machine::setParameter).allowC2S();
            sync.syncValue("module_parameter", value);
            // #tr gtng.module.parameter.label
            // # Parameter
            // # zh_CN 参数
            content.child(IKey.lang("gtng.module.parameter.label").asWidget().pos(24, 0));
            content.child(new TextFieldWidget().value(value).numbersInt(1, machine.getMaximumParameter())
                .background(GTGuiTextures.BACKGROUND_TEXT_FIELD).size(120, 18).pos(20, 16));
            return content;
        }
    }
}
