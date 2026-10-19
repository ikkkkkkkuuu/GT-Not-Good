package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.Locale;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidDriller;

public final class LargeVoidDrillerGui extends GTNGModernMultiBlockBaseGui<LargeVoidDriller> {

    private static final String OPEN_CONFIG_SYNC_KEY = "gtng.vd.openConfig";

    public LargeVoidDrillerGui(LargeVoidDriller multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("gtng.vd.dimension", new StringSyncValue(multiblock::getDimensionDisplayName));
        syncManager.syncValue("gtng.vd.poolValid", new BooleanSyncValue(multiblock::isPoolValid));
        syncManager.syncValue("gtng.vd.energyMult", new DoubleSyncValue(multiblock::getEnergyMultiplier));
        syncManager.syncValue("gtng.vd.energyCost", new LongSyncValue(multiblock::getEnergyCostPerTick));
        syncManager.syncValue("gtng.vd.directional", new BooleanSyncValue(multiblock::getDirectionalMode));
        syncManager.syncValue("gtng.vd.uu", new DoubleSyncValue(multiblock::getUURatePerSecond));
        syncManager.syncValue(OPEN_CONFIG_SYNC_KEY, new InteractionSyncHandler().setOnMousePressed(
            mouseData -> { if (!mouseData.isClient()) multiblock.openConfigGui(syncManager.getPlayer()); }));
    }

    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createButtonColumn(panel, syncManager).child(createOpenConfigButton(syncManager));
    }

    private IWidget createOpenConfigButton(PanelSyncManager syncManager) {
        ButtonWidget<?> button = new ButtonWidget<>().size(16).marginBottom(2).overlay(GuiTextures.GEAR)
            .onMousePressed(mouseButton -> false)
            .syncHandler(syncManager.findSyncHandler(OPEN_CONFIG_SYNC_KEY, InteractionSyncHandler.class))
            // #tr gui.gtnotgood.largeVoidDriller.open_config
            // # Configure void drilling
            // # zh_CN 配置虚空钻取
            .tooltip(t -> t.addLine(StatCollector.translateToLocal("gui.gtnotgood.largeVoidDriller.open_config")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernButton(button, () -> true);
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        ListWidget<IWidget, ?> widget = super.createTerminalTextWidget(syncManager, parent);
        StringSyncValue dimension = syncManager.findSyncHandler("gtng.vd.dimension", StringSyncValue.class);
        BooleanSyncValue poolValid = syncManager.findSyncHandler("gtng.vd.poolValid", BooleanSyncValue.class);
        DoubleSyncValue energyMult = syncManager.findSyncHandler("gtng.vd.energyMult", DoubleSyncValue.class);
        LongSyncValue energyCost = syncManager.findSyncHandler("gtng.vd.energyCost", LongSyncValue.class);
        BooleanSyncValue directional = syncManager.findSyncHandler("gtng.vd.directional", BooleanSyncValue.class);
        DoubleSyncValue uu = syncManager.findSyncHandler("gtng.vd.uu", DoubleSyncValue.class);
        return widget.child(IKey.dynamic(() -> {
            String label = dimension.getValue();
            String status;
            if (label == null || label.isEmpty() || "None".equals(label)) {
                // #tr gui.gtnotgood.largeVoidDriller.no_dimension_short
                // # No dimension
                // # zh_CN 无维度
                status = EnumChatFormatting.RED
                    + StatCollector.translateToLocal("gui.gtnotgood.largeVoidDriller.no_dimension_short");
            } else if (!poolValid.getValue()) {
                // #tr gui.gtnotgood.largeVoidDriller.no_fluids_short
                // # No fluids
                // # zh_CN 无流体
                status = EnumChatFormatting.RED
                    + StatCollector.translateToLocal("gui.gtnotgood.largeVoidDriller.no_fluids_short");
            } else {
                status = EnumChatFormatting.GREEN + label;
            }
            return EnumChatFormatting.YELLOW + StatCollector.translateToLocal(VoidDrillerGuiText.DIMENSION_LINE)
                + status
                + EnumChatFormatting.RESET;
        }).asWidget().marginBottom(2).fullWidth()).child(IKey.dynamic(() -> {
            // #tr gui.gtnotgood.largeVoidDriller.energy_line
            // # Energy:
            // # zh_CN 能耗：
            return EnumChatFormatting.YELLOW
                + StatCollector.translateToLocal("gui.gtnotgood.largeVoidDriller.energy_line")
                + EnumChatFormatting.WHITE
                + NumberFormatUtil.formatNumber(energyCost.getLongValue())
                + " EU/t "
                + EnumChatFormatting.GRAY
                + "(x"
                + String.format(Locale.ROOT, "%.2f", energyMult.getDoubleValue())
                + ")"
                + EnumChatFormatting.RESET;
        }).asWidget().marginBottom(2).fullWidth()).child(IKey.dynamic(() -> {
            if (directional.getValue()) {
                return EnumChatFormatting.YELLOW + StatCollector.translateToLocal(VoidDrillerGuiText.UU_COST)
                    + EnumChatFormatting.LIGHT_PURPLE
                    + String.format(Locale.ROOT, "%.2f", uu.getDoubleValue())
                    + " L/s"
                    + EnumChatFormatting.RESET;
            }
            // #tr gui.gtnotgood.largeVoidDriller.directional_off
            // # Directional mode off
            // # zh_CN 定向模式关闭
            return EnumChatFormatting.GRAY
                + StatCollector.translateToLocal("gui.gtnotgood.largeVoidDriller.directional_off")
                + EnumChatFormatting.RESET;
        }).asWidget().marginBottom(2).fullWidth());
    }
}
