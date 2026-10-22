// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.hatch.modular;

import java.math.BigInteger;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.machines.processing.GenerationWork;
import com.xyp.gtnotgood.common.machines.processing.ModuleWork;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTWaila;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class ExecutionCoreHatch extends ModularHatch {

    public enum CoreType {
        Normal,
        Advanced,
        Perfect
    }

    private final CoreType coreType;
    private transient int executionProgress;
    private transient int executionDuration;
    private transient long executionEut;
    private transient BigInteger executionPrepaidEU = BigInteger.ZERO;
    private transient boolean executionPaused;
    private transient boolean generating;
    private transient BigInteger generationEUt = BigInteger.ZERO;
    private transient BigInteger remainingGeneration = BigInteger.ZERO;

    public ExecutionCoreHatch(int id, String name, CoreType coreType) {
        super(id, name, coreType.ordinal() + 12, ModuleType.ExecutionCore);
        this.coreType = coreType;
    }

    private ExecutionCoreHatch(String name, CoreType coreType, ITexture[][][] textures) {
        super(name, coreType.ordinal() + 12, ModuleType.ExecutionCore, textures);
        this.coreType = coreType;
    }

    public CoreType getCoreType() {
        return coreType;
    }

    public void setExecutionInfo(ModuleWork work, boolean paused) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isClientSide()) return;
        generating = false;
        executionProgress = work == null ? 0 : work.progress();
        executionDuration = work == null ? 0 : work.duration;
        executionEut = work == null ? 0 : work.eut;
        executionPrepaidEU = work == null ? BigInteger.ZERO : work.prepaidEU;
        executionPaused = work != null && paused;
    }

    public void setGenerationInfo(GenerationWork work, boolean paused) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isClientSide()) return;
        generating = true;
        executionProgress = work == null ? 0 : work.progress();
        executionDuration = work == null ? 0 : work.duration;
        generationEUt = work == null ? BigInteger.ZERO : work.nextEnergy();
        remainingGeneration = work == null ? BigInteger.ZERO : work.remainingEnergy();
        executionPaused = work != null && paused;
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("moduleProgress", executionProgress);
        tag.setInteger("moduleDuration", executionDuration);
        tag.setBoolean("moduleGenerating", generating);
        tag.setString("moduleGenerationEUt", NumberFormatUtil.formatNumber(generationEUt));
        tag.setString("moduleRemainingEU", NumberFormatUtil.formatNumber(remainingGeneration));
        tag.setLong("moduleEut", executionEut);
        tag.setString("modulePrepaidEU", NumberFormatUtil.formatNumber(executionPrepaidEU));
        tag.setBoolean("modulePaused", executionPaused);
    }

    @Override
    public void getWailaBody(ItemStack stack, List<String> lines, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, lines, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        int duration = tag.getInteger("moduleDuration");
        if (duration <= 0) {
            lines.add(StatCollector.translateToLocal("GT5U.waila.machine.idle"));
            return;
        }
        lines.add(GTWaila.getMachineProgressString(duration, tag.getInteger("moduleProgress")));
        if (tag.getBoolean("moduleGenerating")) {
            // #tr gtng.module.core.generation
            // # Wireless output: {\\YELLOW}%s{\\RESET} EU/t
            // # zh_CN 无线输出：{\\YELLOW}%s{\\RESET} EU/t
            lines.add(StatCollector.translateToLocalFormatted("gtng.module.core.generation",
                tag.getString("moduleGenerationEUt")));
            // #tr gtng.module.core.remaining_generation
            // # Remaining fuel energy: {\\AQUA}%s{\\RESET} EU
            // # zh_CN 剩余燃料能量：{\\AQUA}%s{\\RESET} EU
            lines.add(StatCollector.translateToLocalFormatted("gtng.module.core.remaining_generation",
                tag.getString("moduleRemainingEU")));
            if (tag.getBoolean("modulePaused")) lines.add(StatCollector.translateToLocal("gtng.module.core.paused"));
            return;
        }
        // #tr gtng.module.core.power
        // # Recipe power: {\\YELLOW}%s{\\RESET} EU/t
        // # zh_CN 配方功率：{\\YELLOW}%s{\\RESET} EU/t
        lines.add(StatCollector.translateToLocalFormatted("gtng.module.core.power",
            NumberFormatUtil.formatNumber(tag.getLong("moduleEut"))));
        if (coreType != CoreType.Normal) {
            // #tr gtng.module.core.prepaid
            // # Prepaid energy: {\\AQUA}%s{\\RESET} EU
            // # zh_CN 已预付电量：{\\AQUA}%s{\\RESET} EU
            lines.add(
                StatCollector.translateToLocalFormatted("gtng.module.core.prepaid", tag.getString("modulePrepaidEU")));
        }
        if (tag.getBoolean("modulePaused")) {
            // #tr gtng.module.core.paused
            // # {\\YELLOW}Paused{\\RESET}
            // # zh_CN {\\YELLOW}已暂停{\\RESET}
            lines.add(StatCollector.translateToLocal("gtng.module.core.paused"));
        }
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new ExecutionCoreHatch(mName, coreType, mTextures);
    }

    @Override
    public String getLocalName() {
        return switch (coreType) {
            case Normal -> normalName();
            case Advanced -> advancedName();
            case Perfect -> perfectName();
        };
    }

    private String normalName() {
        // #tr gtng.module.core.normal.name
        // # Execution Core Module
        // # zh_CN 执行核心模块
        return StatCollector.translateToLocal("gtng.module.core.normal.name");
    }

    private String advancedName() {
        // #tr gtng.module.core.advanced.name
        // # Advanced Execution Core Module
        // # zh_CN 高级执行核心模块
        return StatCollector.translateToLocal("gtng.module.core.advanced.name");
    }

    private String perfectName() {
        // #tr gtng.module.core.perfect.name
        // # Perfect Execution Core Module
        // # zh_CN 完美执行核心模块
        return StatCollector.translateToLocal("gtng.module.core.perfect.name");
    }

    @Override
    public String[] getDescription() {
        // #tr gtng.module.core.description
        // # Adds an independent recipe task to compatible modular multiblocks
        // # zh_CN 为兼容模块化多方块机器增加一项独立配方任务
        String purpose = StatCollector.translateToLocal("gtng.module.core.description");
        // #tr gtng.module.core.shared
        // # Shares the controller's inputs and outputs; multiple cores may be installed
        // # zh_CN 共用控制器的输入输出；可安装多个核心
        String shared = StatCollector.translateToLocal("gtng.module.core.shared");
        // #tr gtng.module.core.requirements
        // # Controller research, computation and other recipe requirements still apply
        // # zh_CN 仍须满足主机的研究、算力等配方条件
        String requirements = StatCollector.translateToLocal("gtng.module.core.requirements");
        // #tr gtng.module.core.pause
        // # Removing or disabling this core pauses its unfinished task
        // # zh_CN 拆下或禁用本核心会暂停其未完成的任务
        String pause = StatCollector.translateToLocal("gtng.module.core.pause");
        if (coreType == CoreType.Normal) {
            // #tr gtng.module.core.normal.power
            // # Uses the controller's power mode
            // # zh_CN 沿用主机供电方式
            String power = StatCollector.translateToLocal("gtng.module.core.normal.power");
            // #tr gtng.module.core.normal.parallel
            // # With local EU: shares normal-core parallel capacity; at least 1 parallel per core
            // # zh_CN 使用本地 EU 时，普通核心共分并行上限；每核至少 1 并行
            String parallel = StatCollector.translateToLocal("gtng.module.core.normal.parallel");
            return new String[] { purpose, shared, power, parallel, requirements, pause };
        }
        // #tr gtng.module.core.wireless
        // # Prepays recipe energy from the core hatch owner's GT wireless network
        // # zh_CN 从核心仓所有者的 GT 无线电网预付配方电量
        String wireless = StatCollector.translateToLocal("gtng.module.core.wireless");
        if (coreType == CoreType.Perfect) {
            // #tr gtng.module.core.perfect.time
            // # Processing time: 20 ticks; parallel limit: 2,147,483,647
            // # zh_CN 处理时间：20 tick；并行上限：2,147,483,647
            String time = StatCollector.translateToLocal("gtng.module.core.perfect.time");
            // #tr gtng.module.core.perfect.modules
            // # No overclocking; the core's parallel limit is fixed
            // # zh_CN 不进行超频；核心自身的并行上限固定
            String modules = StatCollector.translateToLocal("gtng.module.core.perfect.modules");
            return new String[] { purpose, shared, wireless, time, modules, requirements, pause };
        }
        // #tr gtng.module.core.advanced.limits
        // # Recipes still obey the controller's energy hatch voltage and parallel limits
        // # zh_CN 配方仍受主机能源仓电压和并行上限限制
        String limits = StatCollector.translateToLocal("gtng.module.core.advanced.limits");
        return new String[] { purpose, shared, wireless, limits, requirements, pause };
    }

    @Override
    protected String getOverlayName(boolean active) {
        String grade = switch (coreType) {
            case Normal -> "Pri";
            case Advanced -> "Adv";
            case Perfect -> "Per";
        };
        return "OVERLAY_ControlCore_" + grade + (active ? "_on" : "_off");
    }
}
