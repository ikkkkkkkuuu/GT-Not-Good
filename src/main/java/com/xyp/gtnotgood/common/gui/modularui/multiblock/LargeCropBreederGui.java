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
import com.xyp.gtnotgood.common.gui.modularui.widget.CropSeedDropTextField;
import com.xyp.gtnotgood.common.machines.multiblock.LargeCropBreeder;

import cpw.mods.fml.common.network.ByteBufUtils;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;

/**
 * Modern ModularUI panel for the electric large crop breeder.
 * <p>
 * The panel keeps the original terminal-style workflow: type or drag a target crop, inspect the planned mutation chain,
 * and browse the internal crop archive. All state displayed here is synchronized from the server-side controller.
 */
public class LargeCropBreederGui extends GTNGModernMultiBlockBaseGui<LargeCropBreeder> {

    private static final int DISPLAY_ROW_HEIGHT = 12;

    private final LargeCropBreeder breeder;
    private String targetDraft;

    public LargeCropBreederGui(LargeCropBreeder breeder) {
        super(breeder);
        this.breeder = breeder;
        this.targetDraft = breeder.getTargetCropId();
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("targetCrop", new StringSyncValue(breeder::getTargetCropId, value -> {}));
        syncManager
            .syncValue("targetDraft", new StringSyncValue(() -> targetDraft, value -> targetDraft = value).allowC2S());
        syncManager.syncValue("targetInputValid", new BooleanSyncValue(breeder::isTargetInputValid));
        syncManager.syncValue("applyTarget", new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.side.isServer() && mouse.mouseButton == 0) {
                breeder.setTargetCropId(targetDraft);
                if (breeder.isTargetInputValid()) {
                    targetDraft = breeder.getTargetCropId();
                }
            }
        }));
        syncManager.syncValue("archiveSize", new IntSyncValue(breeder::getSyncedArchiveSize));
        syncManager.syncValue("pendingOutputs", new IntSyncValue(breeder::getPendingSeedOutputs));
        syncManager.syncValue("chainTotal", new IntSyncValue(breeder::getChainTotalSteps));
        syncManager.syncValue("chainCompleted", new IntSyncValue(breeder::getChainCompletedSteps));
        syncManager.syncValue("allBlocked", new BooleanSyncValue(breeder::isAllTasksBlocked));
        syncManager.syncValue("missingCrop", new StringSyncValue(breeder::getSyncedMissingInfo, value -> {}));
        syncManager.syncValue("missingRequirements", new BooleanSyncValue(breeder::isMissingBreedingRequirements));
        syncManager.syncValue("poolEUt", new IntSyncValue(() -> (int) breeder.getPoolBreedingEUt()));
        syncManager.syncValue("poolDuration", new IntSyncValue(breeder::getPoolBreedingDuration));
        syncManager.syncValue(
            "archiveCrops",
            new GenericListSyncHandler<>(
                breeder::getSyncedArchiveCrops,
                breeder::setSyncedArchiveCrops,
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
            .child(createTargetInputRow(panel, syncManager))
            .childIf(multiblock.canBeMuffled(), this::createMuffleButton)
            .childIf(multiblock.supportsInventoryRow(), () -> createInventoryRow(panel, syncManager));
    }

    private IWidget createTargetInputRow(ModularPanel parent, PanelSyncManager syncManager) {
        StringSyncValue targetSync = syncManager.findSyncHandler("targetCrop", StringSyncValue.class);
        StringSyncValue draftSync = syncManager.findSyncHandler("targetDraft", StringSyncValue.class);
        BooleanSyncValue validSync = syncManager.findSyncHandler("targetInputValid", BooleanSyncValue.class);
        InteractionSyncHandler applySync = syncManager.findSyncHandler("applyTarget", InteractionSyncHandler.class);
        IntSyncValue chainTotalSync = syncManager.findSyncHandler("chainTotal", IntSyncValue.class);
        IntSyncValue chainCompletedSync = syncManager.findSyncHandler("chainCompleted", IntSyncValue.class);

        // #tr gui.gtnotgood.crop_breeder.target
        // # Target
        // # zh_CN 目标
        TextWidget<?> label = new TextWidget<>(IKey.lang("gui.gtnotgood.crop_breeder.target"));
        label.widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);

        CropSeedDropTextField input = new CropSeedDropTextField();
        input.value(draftSync)
            .size(94, 14)
            .background(GTGuiTextures.BACKGROUND_TEXT_FIELD)
            // #tr gui.gtnotgood.crop_breeder.target.tooltip
            // # Input the target crop id or name, or drag a CropsNH seed from NEI
            // # zh_CN 输入目标作物 ID 或名称，或从 NEI 拖入 CropsNH 种子
            .tooltipBuilder(t -> t.addLine(translateToLocal("gui.gtnotgood.crop_breeder.target.tooltip")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);

        ButtonWidget<?> applyButton = new ButtonWidget<>().size(22, 18)
            .background(
                new DynamicDrawable(
                    () -> validSync.getBoolValue()
                        && java.util.Objects.equals(draftSync.getStringValue(), targetSync.getStringValue())
                            ? GTNGGuiTextures.MODERN_BUTTON_PRESSED
                            : GTNGGuiTextures.MODERN_BUTTON))
            .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER)
            .overlay(
                IKey.dynamic(
                    () -> validSync.getBoolValue() ? EnumChatFormatting.GREEN + "OK" : EnumChatFormatting.RED + "!"))
            .syncHandler(applySync)
            // #tr gui.gtnotgood.crop_breeder.apply
            // # Apply target
            // # zh_CN 应用目标
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.crop_breeder.apply")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);

        IPanelHandler chainPanel = syncManager
            .syncedPanel("cropChainPanel", false, (sm, sh) -> createChainPanel(syncManager, parent));
        ButtonWidget<?> chainButton = new ButtonWidget<>().size(22, 18)
            .background(
                new DynamicDrawable(
                    () -> chainPanel.isPanelOpen() ? GTNGGuiTextures.MODERN_BUTTON_PRESSED
                        : GTNGGuiTextures.MODERN_BUTTON))
            .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER)
            .overlay(IKey.dynamic(() -> {
                int total = chainTotalSync.getIntValue();
                if (total == 0) return EnumChatFormatting.DARK_GRAY + "--";
                return EnumChatFormatting.YELLOW + "" + chainCompletedSync.getIntValue() + "/" + total;
            }))
            .onMousePressed(mouse -> {
                if (chainPanel.isPanelOpen()) {
                    chainPanel.closePanel();
                } else {
                    chainPanel.openPanel();
                }
                return true;
            })
            // #tr gui.gtnotgood.crop_breeder.view_chain
            // # View breeding plan
            // # zh_CN 查看繁育计划
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.crop_breeder.view_chain")))
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
            .child(chainButton);
    }

    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createButtonColumn(panel, syncManager).child(createArchiveButton(syncManager, panel));
    }

    private IWidget createArchiveButton(PanelSyncManager syncManager, ModularPanel parent) {
        IPanelHandler archivePanel = syncManager
            .syncedPanel("cropArchivePanel", false, (sm, sh) -> createArchivePanel(syncManager, parent));
        return new ButtonWidget<>().size(18, 18)
            .background(
                new DynamicDrawable(
                    () -> archivePanel.isPanelOpen() ? GTNGGuiTextures.MODERN_BUTTON_PRESSED
                        : GTNGGuiTextures.MODERN_BUTTON))
            .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_WHITELIST)
            .onMousePressed(mouse -> {
                if (archivePanel.isPanelOpen()) {
                    archivePanel.closePanel();
                } else {
                    archivePanel.openPanel();
                }
                return true;
            })
            // #tr gui.gtnotgood.crop_breeder.view_archive
            // # View crop archive
            // # zh_CN 查看作物档案
            .tooltipBuilder(t -> t.addLine(IKey.lang("gui.gtnotgood.crop_breeder.view_archive")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    private ModularPanel createArchivePanel(PanelSyncManager syncManager, ModularPanel parent) {
        GenericListSyncHandler<String> archiveCrops = syncManager
            .findSyncHandler("archiveCrops", GenericListSyncHandler.class);
        IntSyncValue archiveSize = syncManager.findSyncHandler("archiveSize", IntSyncValue.class);
        ModularPanel panel = ModularPanel.defaultPanel("cropArchivePanel")
            .relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(150, 160)
            .background(GTNGGuiTextures.MODERN_DISPLAY);

        ListWidget<IWidget, ?> list = new ListWidget<>();
        list.padding(6)
            .size(150, 160)
            .child(
                // #tr gui.gtnotgood.crop_breeder.archive
                // # Crop Archive
                // # zh_CN 作物档案
                IKey.lang("gui.gtnotgood.crop_breeder.archive")
                    .asWidget()
                    .height(12)
                    .marginBottom(2))
            .child(new TextWidget<>(IKey.dynamic(() -> {
                List<String> crops = archiveCrops.getValue();
                if (crops == null || crops.isEmpty()) {
                    // #tr gui.gtnotgood.crop_breeder.empty
                    // # (empty)
                    // # zh_CN （空）
                    return EnumChatFormatting.GRAY + translateToLocal("gui.gtnotgood.crop_breeder.empty");
                }
                StringBuilder sb = new StringBuilder();
                sb.append(EnumChatFormatting.GOLD)
                    // #tr gui.gtnotgood.crop_breeder.count
                    // # Count
                    // # zh_CN 数量
                    .append(translateToLocal("gui.gtnotgood.crop_breeder.count"))
                    .append(": ")
                    .append(archiveSize.getIntValue())
                    .append("\n\n")
                    .append(EnumChatFormatting.AQUA);
                for (String cropId : crops) {
                    if (cropId != null && !cropId.isEmpty()) {
                        sb.append("  * ")
                            .append(LargeCropBreeder.getCropDisplayName(cropId))
                            .append("\n");
                    }
                }
                return sb.toString();
            })).widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE));
        return panel.child(list);
    }

    private ModularPanel createChainPanel(PanelSyncManager syncManager, ModularPanel parent) {
        GenericListSyncHandler<NBTTagCompound> chainSteps = syncManager
            .findSyncHandler("chainSteps", GenericListSyncHandler.class);
        IntSyncValue chainTotal = syncManager.findSyncHandler("chainTotal", IntSyncValue.class);
        IntSyncValue chainCompleted = syncManager.findSyncHandler("chainCompleted", IntSyncValue.class);
        ModularPanel panel = ModularPanel.defaultPanel("cropChainPanel")
            .relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(200, 160)
            .background(GTNGGuiTextures.MODERN_DISPLAY);

        ListWidget<IWidget, ?> list = new ListWidget<>();
        list.padding(6)
            .size(200, 160)
            .child(
                // #tr gui.gtnotgood.crop_breeder.plan
                // # Breeding Plan
                // # zh_CN 繁育计划
                IKey.lang("gui.gtnotgood.crop_breeder.plan")
                    .asWidget()
                    .height(12)
                    .marginBottom(2))
            .child(new TextWidget<>(IKey.dynamic(() -> {
                StringBuilder sb = new StringBuilder();
                sb.append(EnumChatFormatting.GOLD)
                    // #tr gui.gtnotgood.crop_breeder.progress
                    // # Progress
                    // # zh_CN 进度
                    .append(translateToLocal("gui.gtnotgood.crop_breeder.progress"))
                    .append(": ")
                    .append(chainCompleted.getIntValue())
                    .append("/")
                    .append(chainTotal.getIntValue())
                    .append("\n\n");
                List<NBTTagCompound> steps = chainSteps.getValue();
                if (steps == null || steps.isEmpty()) {
                    sb.append(EnumChatFormatting.GRAY)
                        .append(translateToLocal("gui.gtnotgood.crop_breeder.empty"));
                } else {
                    for (NBTTagCompound step : steps) {
                        sb.append(formatStep(step))
                            .append("\n");
                    }
                }
                return sb.toString();
            })).widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE));
        return panel.child(list);
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        return super.createTerminalTextWidget(syncManager, parent).child(createTargetCropRow(syncManager))
            .child(createCurrentChainRow(syncManager))
            .child(createPendingOutputRow(syncManager))
            .child(createPowerRow(syncManager))
            .child(createMissingRow(syncManager));
    }

    private IWidget createTargetCropRow(PanelSyncManager syncManager) {
        StringSyncValue targetSync = syncManager.findSyncHandler("targetCrop", StringSyncValue.class);
        return Flow.row()
            .fullWidth()
            .height(DISPLAY_ROW_HEIGHT)
            .child(
                new TextWidget<>(IKey.lang("gui.gtnotgood.crop_breeder.target"))
                    .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE)
                    .scale(0.75f))
            .child(new TextWidget<>(IKey.dynamic(() -> {
                String target = targetSync.getStringValue();
                if (target == null || target.isEmpty()) {
                    // #tr gt.blockmachines.multimachine.notset
                    // # Not Set
                    // # zh_CN 未设置
                    return EnumChatFormatting.GRAY + translateToLocal("gt.blockmachines.multimachine.notset");
                }
                return EnumChatFormatting.GOLD + LargeCropBreeder.getCropDisplayName(target);
            })).widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE)
                .scale(0.75f));
    }

    private IWidget createCurrentChainRow(PanelSyncManager syncManager) {
        GenericListSyncHandler<NBTTagCompound> chainSteps = syncManager
            .findSyncHandler("chainSteps", GenericListSyncHandler.class);
        return new TextWidget<>(IKey.dynamic(() -> {
            List<NBTTagCompound> steps = chainSteps.getValue();
            if (steps == null || steps.isEmpty()) return "";
            for (NBTTagCompound step : steps) {
                if (step.getByte("status") == 1) return formatStep(step);
            }
            return "";
        })).height(DISPLAY_ROW_HEIGHT)
            .scale(0.75f)
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);
    }

    private IWidget createPendingOutputRow(PanelSyncManager syncManager) {
        IntSyncValue pendingSync = syncManager.findSyncHandler("pendingOutputs", IntSyncValue.class);
        return new TextWidget<>(IKey.dynamic(() -> {
            // #tr gt.blockmachines.multimachine.pending
            // # Pending
            // # zh_CN 待输出
            return EnumChatFormatting.AQUA + translateToLocal("gt.blockmachines.multimachine.pending")
                + ": "
                + EnumChatFormatting.GREEN
                + pendingSync.getIntValue();
        })).height(DISPLAY_ROW_HEIGHT)
            .scale(0.75f)
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);
    }

    private IWidget createPowerRow(PanelSyncManager syncManager) {
        IntSyncValue poolEUt = syncManager.findSyncHandler("poolEUt", IntSyncValue.class);
        IntSyncValue poolDuration = syncManager.findSyncHandler("poolDuration", IntSyncValue.class);
        return new TextWidget<>(IKey.dynamic(() -> {
            // #tr gui.gtnotgood.crop_breeder.power
            // # Pool Power
            // # zh_CN 池模式功率
            String power = translateToLocal("gui.gtnotgood.crop_breeder.power");
            // #tr gui.gtnotgood.crop_breeder.duration
            // # Duration
            // # zh_CN 时长
            String duration = translateToLocal("gui.gtnotgood.crop_breeder.duration");
            return EnumChatFormatting.AQUA + power
                + ": "
                + EnumChatFormatting.GREEN
                + NumberFormatUtil.formatNumber(poolEUt.getIntValue())
                + " EU/t  "
                + EnumChatFormatting.AQUA
                + duration
                + ": "
                + EnumChatFormatting.GREEN
                + (poolDuration.getIntValue() / 20.0f)
                + "s";
        })).height(DISPLAY_ROW_HEIGHT)
            .scale(0.75f)
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);
    }

    private IWidget createMissingRow(PanelSyncManager syncManager) {
        BooleanSyncValue blockedSync = syncManager.findSyncHandler("allBlocked", BooleanSyncValue.class);
        BooleanSyncValue requirementsSync = syncManager.findSyncHandler("missingRequirements", BooleanSyncValue.class);
        StringSyncValue missingSync = syncManager.findSyncHandler("missingCrop", StringSyncValue.class);
        return new TextWidget<>(IKey.dynamic(() -> {
            if (!blockedSync.getBoolValue()) return "";
            String missing = missingSync.getStringValue();
            if (missing == null || missing.isEmpty()) {
                if (!requirementsSync.getBoolValue()) return "";
                // #tr gui.gtnotgood.crop_breeder.missing_requirements
                // # mutation catalyst/condition
                // # zh_CN 杂交催化物/条件
                missing = translateToLocal("gui.gtnotgood.crop_breeder.missing_requirements");
            } else {
                missing = LargeCropBreeder.getCropDisplayName(missing);
            }
            // #tr gt.blockmachines.multimachine.missing
            // # Missing
            // # zh_CN 缺少
            return EnumChatFormatting.RED + translateToLocal("gt.blockmachines.multimachine.missing")
                + ": "
                + EnumChatFormatting.GOLD
                + missing;
        })).height(DISPLAY_ROW_HEIGHT)
            .scale(0.75f)
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);
    }

    private static String formatStep(NBTTagCompound step) {
        byte status = step.getByte("status");
        String color = status == 2 ? EnumChatFormatting.GREEN.toString()
            : status == 1 ? EnumChatFormatting.YELLOW.toString() : EnumChatFormatting.RED.toString();
        String icon = status == 2 ? "[OK]" : status == 1 ? "[>]" : "[!]";
        StringBuilder parents = new StringBuilder();
        int parentCount = step.getInteger("parentCount");
        for (int i = 0; i < parentCount; i++) {
            if (i > 0) parents.append(" + ");
            parents.append(LargeCropBreeder.getCropDisplayName(step.getString("parent" + i)));
        }
        return color + icon
            + " "
            + EnumChatFormatting.WHITE
            + parents
            + " -> "
            + EnumChatFormatting.GOLD
            + LargeCropBreeder.getCropDisplayName(step.getString("result"));
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
}
