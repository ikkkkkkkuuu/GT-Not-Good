package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;
import static net.minecraft.util.StatCollector.translateToLocal;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.gui.modularui.widget.BeeSpeciesDropTextField;
import com.xyp.gtnotgood.common.machines.bee.BeeBreedingHelper;
import com.xyp.gtnotgood.common.machines.multiblock.LargeBeeBreeder;

import cpw.mods.fml.common.network.ByteBufUtils;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;

/**
 * Modern ModularUI panel for the electric large bee breeder.
 * <p>
 * Features:
 * <ul>
 * <li>Species name text input to set the breeding target</li>
 * <li>Terminal display showing breeding pool state, chain progress, pending outputs</li>
 * <li>Drone Pool button to view all available species in the pool</li>
 * </ul>
 */
public class LargeBeeBreederGui extends GTNGModernMultiBlockBaseGui<LargeBeeBreeder> {

    private final LargeBeeBreeder breeder;
    private String targetDraft;

    private static final int DISPLAY_ROW_HEIGHT = 12;

    public LargeBeeBreederGui(LargeBeeBreeder breeder) {
        super(breeder);
        this.breeder = breeder;
        this.targetDraft = breeder.getTargetBeeSpecies();
    }

    // ==================== Sync Values ====================

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("targetSpecies", new StringSyncValue(breeder::getTargetBeeSpecies, value -> {}));
        syncManager
            .syncValue("targetDraft", new StringSyncValue(() -> targetDraft, value -> targetDraft = value).allowC2S());
        syncManager.syncValue("targetInputValid", new BooleanSyncValue(breeder::isTargetInputValid));
        syncManager.syncValue("applyTarget", new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.side.isServer() && mouse.mouseButton == 0) {
                breeder.setTargetBeeSpecies(targetDraft);
                if (breeder.isTargetInputValid()) {
                    targetDraft = breeder.getTargetBeeSpecies();
                }
            }
        }));
        syncManager.syncValue("poolSize", new IntSyncValue(breeder::getSyncedPoolSize));
        syncManager.syncValue("chainTotal", new IntSyncValue(breeder::getChainTotalSteps));
        syncManager.syncValue("chainCompleted", new IntSyncValue(breeder::getChainCompletedSteps));
        syncManager.syncValue("pendingOutputs", new IntSyncValue(breeder::getPendingPrincessOutputs));
        syncManager.syncValue("euPerAttempt", new IntSyncValue(() -> (int) breeder.getEuPerBreedingAttempt()));
        syncManager.syncValue("maxParallelSteps", new IntSyncValue(breeder::getMaxParallelBreedingSteps));
        syncManager.syncValue("chanceBonus", new IntSyncValue(breeder::getMutationChanceBonus));
        syncManager.syncValue("allBlocked", new BooleanSyncValue(breeder::isAllTasksBlocked));
        syncManager.syncValue("missingSpecies", new StringSyncValue(breeder::getSyncedMissingInfo, val -> {}));
        syncManager.syncValue(
            "poolSpecies",
            new GenericListSyncHandler<>(
                breeder::getSyncedPoolSpecies,
                breeder::setSyncedPoolSpecies,
                ByteBufUtils::readUTF8String,
                ByteBufUtils::writeUTF8String,
                String::equals,
                value -> value));
        syncManager.syncValue(
            "chainSteps",
            new GenericListSyncHandler<>(
                breeder::getSyncedChainSteps,
                breeder::setSyncedChainSteps,
                ByteBufUtils::readTag,
                ByteBufUtils::writeTag,
                NBTTagCompound::equals,
                value -> (NBTTagCompound) value.copy()));
    }

    @Override
    public Flow createMainColumn(ModularPanel panel, PanelSyncManager syncManager) {
        panel.background(GTNGGuiTextures.MODERN_BACKGROUND);
        return Flow.column()
            .padding(4)
            .child(createTerminalRow(panel, syncManager))
            .child(createSpeciesInputRow(panel, syncManager))
            .childIf(multiblock.canBeMuffled(), this::createMuffleButton)
            .childIf(multiblock.supportsInventoryRow(), () -> createInventoryRow(panel, syncManager));
    }

    private IWidget createSpeciesInputRow(ModularPanel panel, PanelSyncManager syncManager) {
        StringSyncValue targetSync = syncManager.findSyncHandler("targetSpecies", StringSyncValue.class);
        StringSyncValue draftSync = syncManager.findSyncHandler("targetDraft", StringSyncValue.class);
        BooleanSyncValue validSync = syncManager.findSyncHandler("targetInputValid", BooleanSyncValue.class);
        InteractionSyncHandler applySync = syncManager.findSyncHandler("applyTarget", InteractionSyncHandler.class);
        IntSyncValue chainTotalSync = syncManager.findSyncHandler("chainTotal", IntSyncValue.class);
        IntSyncValue chainCompletedSync = syncManager.findSyncHandler("chainCompleted", IntSyncValue.class);
        // #tr gui.gtnotgood.bee_breeder.target
        // # Target
        // # zh_CN 目标
        TextWidget label = new TextWidget(IKey.lang("gui.gtnotgood.bee_breeder.target"));
        label.widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);
        BeeSpeciesDropTextField input = new BeeSpeciesDropTextField();
        input.value(draftSync);
        input.size(96, 14);
        // #tr gt.blockmachines.multimachine.bee.species.tooltip
        // # Input the target bee species name, or drag a bee from NEI
        // # zh_CN 输入目标蜜蜂品种名称，或从NEI拖入蜜蜂
        input.tooltipBuilder(t -> t.addLine(translateToLocal("gt.blockmachines.multimachine.bee.species.tooltip")));
        ButtonWidget applyButton = new ButtonWidget<>().size(20, 18)
            .background(
                new DynamicDrawable(
                    () -> validSync.getBoolValue()
                        && java.util.Objects.equals(draftSync.getStringValue(), targetSync.getStringValue())
                            ? GTNGGuiTextures.MODERN_BUTTON_PRESSED
                            : GTNGGuiTextures.MODERN_BUTTON))
            .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER)
            .overlay(
                IKey.dynamic(
                    () -> validSync.getBoolValue() ? EnumChatFormatting.GREEN + "\u2713"
                        : EnumChatFormatting.RED + "!"))
            .syncHandler(applySync)
            // #tr gui.gtnotgood.bee_breeder.apply
            // # Apply target
            // # zh_CN 应用目标
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.bee_breeder.apply")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        IPanelHandler chainPanel = syncManager
            .syncedPanel("chainPanel", false, (sm, sh) -> createChainPanel(syncManager, panel));
        ButtonWidget chainBtn = new ButtonWidget<>().size(18, 18)
            .background(
                new DynamicDrawable(
                    () -> chainPanel.isPanelOpen() ? GTNGGuiTextures.MODERN_BUTTON_PRESSED
                        : GTNGGuiTextures.MODERN_BUTTON))
            .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER)
            .overlay(IKey.dynamic(() -> {
                int total = chainTotalSync.getIntValue();
                int completed = chainCompletedSync.getIntValue();
                if (total == 0) return EnumChatFormatting.DARK_GRAY + "--";
                return EnumChatFormatting.YELLOW + "" + completed + "/" + total;
            }))
            .onMousePressed(d -> {
                if (!chainPanel.isPanelOpen()) {
                    chainPanel.openPanel();
                } else {
                    chainPanel.closePanel();
                }
                return true;
            })
            // #tr gui.gtnotgood.bee_breeder.view_chain
            // # View breeding plan
            // # zh_CN 查看繁育计划
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.bee_breeder.view_chain")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return Flow.row()
            .fullWidth()
            .height(20)
            .paddingLeft(5)
            .paddingRight(5)
            .marginTop(4)
            .marginBottom(2)
            .child(label)
            .child(input)
            .child(applyButton)
            .child(chainBtn);
    }

    @Override
    protected boolean shouldDisplayVoidExcess() {
        return false;
    }

    @Override
    protected boolean shouldDisplayInputSeparation() {
        return false;
    }

    @Override
    protected boolean shouldDisplayBatchMode() {
        return false;
    }

    @Override
    protected boolean shouldDisplayRecipeLock() {
        return false;
    }

    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createButtonColumn(panel, syncManager).child(createDronePoolButton(syncManager, panel));
    }

    private IWidget createDronePoolButton(PanelSyncManager syncManager, ModularPanel parent) {
        IPanelHandler poolPanel = syncManager
            .syncedPanel("dronePoolPanel", false, (sm, sh) -> createPoolPanel(syncManager, parent));
        return new ButtonWidget<>().size(18, 18)
            .background(
                new DynamicDrawable(
                    () -> poolPanel.isPanelOpen() ? GTNGGuiTextures.MODERN_BUTTON_PRESSED
                        : GTNGGuiTextures.MODERN_BUTTON))
            .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_WHITELIST)
            .onMousePressed(d -> {
                if (!poolPanel.isPanelOpen()) {
                    poolPanel.openPanel();
                } else {
                    poolPanel.closePanel();
                }
                return true;
            })
            // #tr gui.gtnotgood.bee_breeder.view_archive
            // # View species archive
            // # zh_CN 查看物种档案
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.bee_breeder.view_archive")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    // ==================== Drone Pool Sub-Panel ====================

    private ModularPanel createPoolPanel(PanelSyncManager syncManager, ModularPanel parent) {
        GenericListSyncHandler<String> poolSpecies = syncManager
            .findSyncHandler("poolSpecies", GenericListSyncHandler.class);
        IntSyncValue poolSize = syncManager.findSyncHandler("poolSize", IntSyncValue.class);

        ModularPanel panel = ModularPanel.defaultPanel("dronePoolPanel")
            .relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(140, 160)
            .background(GTNGGuiTextures.MODERN_DISPLAY);

        ListWidget<IWidget, ?> list = new ListWidget<>();
        list.padding(6)
            .size(140, 160)
            .child(
                // #tr gui.gtnotgood.bee_breeder.archive
                // # Species Archive
                // # zh_CN 物种档案
                IKey.lang("gui.gtnotgood.bee_breeder.archive")
                    .asWidget()
                    .height(12)
                    .marginBottom(2))
            .child(new TextWidget<>(IKey.dynamic(() -> {
                List<String> species = poolSpecies.getValue();
                if (species == null || species.isEmpty()) {
                    // #tr gui.gtnotgood.bee_breeder.empty
                    // # (empty)
                    // # zh_CN （空）
                    return EnumChatFormatting.GRAY + translateToLocal("gui.gtnotgood.bee_breeder.empty");
                }
                StringBuilder sb = new StringBuilder();
                sb.append(EnumChatFormatting.GOLD)
                    // #tr gui.gtnotgood.bee_breeder.count
                    // # Count
                    // # zh_CN 数量
                    .append(translateToLocal("gui.gtnotgood.bee_breeder.count"))
                    .append(": ")
                    .append(poolSize.getIntValue())
                    .append("\n\n")
                    .append(EnumChatFormatting.AQUA);
                for (String uid : species) {
                    if (uid != null && !uid.isEmpty()) {
                        sb.append("  \u2022 ")
                            .append(BeeBreedingHelper.getSpeciesDisplayName(uid))
                            .append("\n");
                    }
                }
                return sb.toString();
            })).widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE));

        return panel.child(list);
    }

    // ==================== Chain Status Sub-Panel ====================

    private ModularPanel createChainPanel(PanelSyncManager syncManager, ModularPanel parent) {
        GenericListSyncHandler<NBTTagCompound> chainSteps = syncManager
            .findSyncHandler("chainSteps", GenericListSyncHandler.class);
        IntSyncValue chainTotal = syncManager.findSyncHandler("chainTotal", IntSyncValue.class);
        IntSyncValue chainCompleted = syncManager.findSyncHandler("chainCompleted", IntSyncValue.class);

        ModularPanel panel = ModularPanel.defaultPanel("chainPanel")
            .relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(180, 160)
            .background(GTNGGuiTextures.MODERN_DISPLAY);

        ListWidget<IWidget, ?> list = new ListWidget<>();
        list.padding(6)
            .size(180, 160)
            .child(
                // #tr gui.gtnotgood.bee_breeder.plan
                // # Breeding Plan
                // # zh_CN 繁育计划
                IKey.lang("gui.gtnotgood.bee_breeder.plan")
                    .asWidget()
                    .height(12)
                    .marginBottom(2))
            .child(new TextWidget<>(IKey.dynamic(() -> {
                int total = chainTotal.getIntValue();
                int completed = chainCompleted.getIntValue();
                List<NBTTagCompound> steps = chainSteps.getValue();
                StringBuilder sb = new StringBuilder();
                sb.append(EnumChatFormatting.GOLD)
                    // #tr gui.gtnotgood.bee_breeder.progress
                    // # Progress
                    // # zh_CN 进度
                    .append(translateToLocal("gui.gtnotgood.bee_breeder.progress"))
                    .append(": ")
                    .append(completed)
                    .append("/")
                    .append(total)
                    .append("\n\n");
                if (steps == null || steps.isEmpty()) {
                    sb.append(EnumChatFormatting.GRAY)
                        .append(translateToLocal("gui.gtnotgood.bee_breeder.empty"));
                } else {
                    for (int i = 0; i < steps.size(); i++) {
                        NBTTagCompound step = steps.get(i);
                        byte status = step.getByte("status");
                        String p1 = BeeBreedingHelper.getSpeciesDisplayName(step.getString("parent1"));
                        String p2 = BeeBreedingHelper.getSpeciesDisplayName(step.getString("parent2"));
                        String result = BeeBreedingHelper.getSpeciesDisplayName(step.getString("result"));
                        String chance = String.format("%.1f", step.getDouble("chance"));
                        String icon;
                        String color;
                        if (status == 2) {
                            icon = "\u2713";
                            color = EnumChatFormatting.GREEN.toString();
                        } else if (status == 1) {
                            icon = "\u2192";
                            color = EnumChatFormatting.YELLOW.toString();
                        } else {
                            icon = "\u2717";
                            color = EnumChatFormatting.RED.toString();
                        }
                        sb.append(color)
                            .append("[")
                            .append(icon)
                            .append("] ")
                            .append(EnumChatFormatting.WHITE)
                            .append(p1)
                            .append(" + ")
                            .append(p2)
                            .append(" \u2192 ")
                            .append(result)
                            .append(" (")
                            .append(EnumChatFormatting.GOLD)
                            .append(chance)
                            .append("%")
                            .append(EnumChatFormatting.WHITE)
                            .append(")");
                        if (i < steps.size() - 1) {
                            sb.append("\n");
                        }
                    }
                }
                return sb.toString();
            })).widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE));

        return panel.child(list);
    }

    // ==================== Terminal Text Widget ====================

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        return super.createTerminalTextWidget(syncManager, parent).child(createTargetSpeciesRow(syncManager))
            .child(createCurrentChainRow(syncManager))
            .child(createPendingOutputRow(syncManager))
            .child(createBreedingPolicyRow(syncManager))
            .child(createMissingRow(syncManager));
    }

    private IWidget createCurrentChainRow(PanelSyncManager syncManager) {
        GenericListSyncHandler<NBTTagCompound> chainSteps = syncManager
            .findSyncHandler("chainSteps", GenericListSyncHandler.class);
        return new TextWidget<>(IKey.dynamic(() -> {
            List<NBTTagCompound> steps = chainSteps.getValue();
            if (steps == null || steps.isEmpty()) return "";
            for (NBTTagCompound step : steps) {
                if (step.getByte("status") != 1) continue;
                return EnumChatFormatting.YELLOW + "\u2192 "
                    + EnumChatFormatting.WHITE
                    + BeeBreedingHelper.getSpeciesDisplayName(step.getString("parent1"))
                    + " + "
                    + BeeBreedingHelper.getSpeciesDisplayName(step.getString("parent2"))
                    + " \u2192 "
                    + EnumChatFormatting.GOLD
                    + BeeBreedingHelper.getSpeciesDisplayName(step.getString("result"))
                    + EnumChatFormatting.WHITE
                    + " ("
                    + EnumChatFormatting.AQUA
                    + String.format("%.1f", step.getDouble("chance"))
                    + "%"
                    + EnumChatFormatting.WHITE
                    + ")";
            }
            return "";
        })).height(DISPLAY_ROW_HEIGHT)
            .scale(0.75f)
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);
    }

    private IWidget createTargetSpeciesRow(PanelSyncManager syncManager) {
        StringSyncValue targetSync = syncManager.findSyncHandler("targetSpecies", StringSyncValue.class);
        return Flow.row()
            .fullWidth()
            .height(DISPLAY_ROW_HEIGHT)
            .child(
                new TextWidget<>(IKey.lang("gui.gtnotgood.bee_breeder.target"))
                    .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE)
                    .scale(0.75f))
            .child(new TextWidget<>(IKey.dynamic(() -> {
                String val = targetSync.getStringValue();
                return (val == null || val.isEmpty())
                    // #tr gt.blockmachines.multimachine.notset
                    // # Not Set
                    // # zh_CN 未设置
                    ? EnumChatFormatting.GRAY + translateToLocal("gt.blockmachines.multimachine.notset")
                    : EnumChatFormatting.GOLD + BeeBreedingHelper.getSpeciesDisplayName(val);
            })).widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE)
                .scale(0.75f));
    }

    private IWidget createPendingOutputRow(PanelSyncManager syncManager) {
        IntSyncValue pendingSync = syncManager.findSyncHandler("pendingOutputs", IntSyncValue.class);
        return new TextWidget<>(IKey.dynamic(() -> {
            int pending = pendingSync.getIntValue();
            // #tr gt.blockmachines.multimachine.pending
            // # Pending
            // # zh_CN 待输出
            return EnumChatFormatting.AQUA + translateToLocal("gt.blockmachines.multimachine.pending")
                + ": "
                + EnumChatFormatting.GREEN
                + pending;
        })).height(DISPLAY_ROW_HEIGHT)
            .scale(0.75f)
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);
    }

    private IWidget createBreedingPolicyRow(PanelSyncManager syncManager) {
        IntSyncValue euSync = syncManager.findSyncHandler("euPerAttempt", IntSyncValue.class);
        IntSyncValue parallelSync = syncManager.findSyncHandler("maxParallelSteps", IntSyncValue.class);
        IntSyncValue chanceSync = syncManager.findSyncHandler("chanceBonus", IntSyncValue.class);
        return new TextWidget<>(IKey.dynamic(() -> {
            // #tr gui.gtnotgood.bee_breeder.power
            // # Power
            // # zh_CN 电力
            String power = translateToLocal("gui.gtnotgood.bee_breeder.power");
            // #tr gui.gtnotgood.bee_breeder.parallel
            // # Parallel
            // # zh_CN 并行
            String parallel = translateToLocal("gui.gtnotgood.bee_breeder.parallel");
            // #tr gui.gtnotgood.bee_breeder.bonus
            // # Bonus
            // # zh_CN 加成
            String bonus = translateToLocal("gui.gtnotgood.bee_breeder.bonus");
            return EnumChatFormatting.AQUA + power
                + ": "
                + EnumChatFormatting.GREEN
                + NumberFormatUtil.formatNumber(euSync.getIntValue())
                + " EU/t  "
                + EnumChatFormatting.AQUA
                + parallel
                + ": "
                + EnumChatFormatting.GREEN
                + parallelSync.getIntValue()
                + "  "
                + EnumChatFormatting.AQUA
                + bonus
                + ": "
                + EnumChatFormatting.GREEN
                + chanceSync.getIntValue();
        })).height(DISPLAY_ROW_HEIGHT)
            .scale(0.75f)
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);
    }

    private IWidget createMissingRow(PanelSyncManager syncManager) {
        BooleanSyncValue blockedSync = syncManager.findSyncHandler("allBlocked", BooleanSyncValue.class);
        StringSyncValue missingSync = syncManager.findSyncHandler("missingSpecies", StringSyncValue.class);
        return new TextWidget<>(IKey.dynamic(() -> {
            boolean blocked = blockedSync.getBoolValue();
            if (!blocked) return "";
            String missing = missingSync.getStringValue();
            // #tr gt.blockmachines.multimachine.missing
            // # Missing
            // # zh_CN 缺少
            return EnumChatFormatting.RED + translateToLocal("gt.blockmachines.multimachine.missing")
                + ": "
                + EnumChatFormatting.GOLD
                + (missing != null && !missing.isEmpty() ? BeeBreedingHelper.getSpeciesDisplayName(missing) : "?");
        })).height(DISPLAY_ROW_HEIGHT)
            .scale(0.75f)
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);
    }

    // ==================== Muffler Position ====================

    @Override
    protected int getMufflerPosFromRightOutwards() {
        return 15;
    }
}
