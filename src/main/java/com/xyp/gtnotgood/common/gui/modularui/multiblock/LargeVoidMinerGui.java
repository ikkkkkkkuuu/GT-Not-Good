package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidMiner;

/**
 * Main machine screen for the electric Large Void Miner.
 * <p>
 * It keeps the GT Not Good multiblock controls and adds a compact Crust Matter Aggregator style status block plus a
 * configuration button that opens the ore browser terminal.
 */
public class LargeVoidMinerGui extends GTNGModernMultiBlockBaseGui<LargeVoidMiner> {

    private static final String OPEN_CONFIG_SYNC_KEY = "gtng.vm.openConfig";

    public LargeVoidMinerGui(LargeVoidMiner multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("gtng.vm.dimLabel", new StringSyncValue(multiblock::getDimensionDisplayName));
        syncManager.syncValue("gtng.vm.dropMapValid", new BooleanSyncValue(() -> multiblock.dropMapValid));
        syncManager.syncValue("gtng.vm.energyMult", new DoubleSyncValue(multiblock::getEnergyMultiplier));
        syncManager.syncValue("gtng.vm.energyCost", new IntSyncValue(() -> (int) multiblock.getEnergyCostPerTick()));
        syncManager.syncValue("gtng.vm.directionalMode", new BooleanSyncValue(multiblock::getDirectionalMode));
        syncManager.syncValue("gtng.vm.uuMult", new DoubleSyncValue(multiblock::getUUMultiplier));
        syncManager.syncValue(OPEN_CONFIG_SYNC_KEY, new InteractionSyncHandler().setOnMousePressed(mouseData -> {
            if (!mouseData.isClient()) {
                multiblock.openConfigGui(syncManager.getPlayer());
            }
        }));
    }

    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createButtonColumn(panel, syncManager).child(createOpenConfigButton(syncManager));
    }

    private IWidget createOpenConfigButton(PanelSyncManager syncManager) {
        ButtonWidget<?> button = new ButtonWidget<>().size(16)
            .marginBottom(2)
            .overlay(GuiTextures.GEAR)
            .onMousePressed(mouseButton -> false)
            .syncHandler(syncManager.findSyncHandler(OPEN_CONFIG_SYNC_KEY, InteractionSyncHandler.class))
            // #tr gui.gtnotgood.largeVoidMiner.open_config
            // # Configure void mining
            // # zh_CN 配置虚空采矿
            .tooltip(t -> t.addLine(StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.open_config")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernButton(button, () -> true);
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        ListWidget<IWidget, ?> widget = super.createTerminalTextWidget(syncManager, parent);
        StringSyncValue dimSyncer = syncManager.findSyncHandler("gtng.vm.dimLabel", StringSyncValue.class);
        BooleanSyncValue validSyncer = syncManager.findSyncHandler("gtng.vm.dropMapValid", BooleanSyncValue.class);
        DoubleSyncValue energyMultSyncer = syncManager.findSyncHandler("gtng.vm.energyMult", DoubleSyncValue.class);
        IntSyncValue energyCostSyncer = syncManager.findSyncHandler("gtng.vm.energyCost", IntSyncValue.class);
        BooleanSyncValue directionalSyncer = syncManager
            .findSyncHandler("gtng.vm.directionalMode", BooleanSyncValue.class);
        DoubleSyncValue uuMultSyncer = syncManager.findSyncHandler("gtng.vm.uuMult", DoubleSyncValue.class);

        return widget.child(IKey.dynamic(() -> {
            String dimLabel = dimSyncer.getValue();
            String dimPart;
            if (dimLabel == null || dimLabel.isEmpty() || "None".equals(dimLabel)) {
                // #tr gui.gtnotgood.largeVoidMiner.no_dimension_short
                // # No dimension
                // # zh_CN 无维度
                dimPart = EnumChatFormatting.RED
                    + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.no_dimension_short");
            } else if (!validSyncer.getValue()) {
                // #tr gui.gtnotgood.largeVoidMiner.no_ores_short
                // # No ores
                // # zh_CN 无矿石
                dimPart = EnumChatFormatting.RED
                    + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.no_ores_short");
            } else {
                dimPart = EnumChatFormatting.GREEN + dimLabel;
            }
            // #tr gui.gtnotgood.largeVoidMiner.dimension_line
            // # Dimension:
            // # zh_CN 维度：
            return EnumChatFormatting.YELLOW
                + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.dimension_line")
                + dimPart
                + EnumChatFormatting.RESET;
        })
            .asWidget()
            .marginBottom(2)
            .fullWidth())
            .child(IKey.dynamic(() -> {
                int cost = energyCostSyncer.getIntValue();
                double mult = energyMultSyncer.getDoubleValue();
                // #tr gui.gtnotgood.largeVoidMiner.energy_line
                // # Energy:
                // # zh_CN 能耗：
                return EnumChatFormatting.YELLOW
                    + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.energy_line")
                    + EnumChatFormatting.WHITE
                    + NumberFormatUtil.formatNumber(cost)
                    + " EU/t "
                    + EnumChatFormatting.GRAY
                    + "(x"
                    + String.format("%.2f", mult)
                    + ")"
                    + EnumChatFormatting.RESET;
            })
                .asWidget()
                .marginBottom(2)
                .fullWidth())
            .child(IKey.dynamic(() -> {
                if (directionalSyncer.getValue()) {
                    double mult = uuMultSyncer.getDoubleValue();
                    // #tr gui.gtnotgood.largeVoidMiner.uu_line
                    // # UU-Matter:
                    // # zh_CN UU物质：
                    return EnumChatFormatting.YELLOW
                        + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.uu_line")
                        + EnumChatFormatting.LIGHT_PURPLE
                        + NumberFormatUtil.formatNumber(Math.round(mult))
                        + " L/s"
                        + EnumChatFormatting.RESET;
                }
                // #tr gui.gtnotgood.largeVoidMiner.directional_off
                // # Directional mode off
                // # zh_CN 定向模式关闭
                return EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.directional_off")
                    + EnumChatFormatting.RESET;
            })
                .asWidget()
                .marginBottom(2)
                .fullWidth());
    }

    @Override
    protected Flow createTerminalRightCornerColumn(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createTerminalRightCornerColumn(panel, syncManager).mainAxisAlignment(Alignment.MainAxis.END);
    }
}
