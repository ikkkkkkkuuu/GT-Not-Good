package com.xyp.gtnotgood.common.machines.basic;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtnotgood.common.machines.basic.EssentiaDisassembler.DisplayState;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.recipe.BasicUIProperties;
import gregtech.common.gui.modularui.singleblock.base.MTEBasicMachineBaseGui;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

final class EssentiaDisassemblerGui extends MTEBasicMachineBaseGui<EssentiaDisassembler> {

    private static final int COLUMNS = 3;
    private static final int MAX_ICONS = 9;
    private static final int ICON_SIZE = 16;
    private static final DisplayState[] DISPLAY_STATES = DisplayState.values();

    private StringSyncValue essentiaSync;
    private IntSyncValue stateSync;
    private String displayedRaw = "";
    private final List<AspectDisplay> displayedAspects = new ArrayList<>();

    EssentiaDisassemblerGui(EssentiaDisassembler machine, BasicUIProperties properties) {
        super(machine, properties);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        essentiaSync = new StringSyncValue(() -> "", () -> encodeAspects(machine.getDisplayedEssentia()));
        stateSync = new IntSyncValue(() -> DisplayState.Idle.ordinal(), () -> machine.getDisplayState().ordinal());
        syncManager.syncValue("essentiaPreview", essentiaSync);
        syncManager.syncValue("essentiaState", stateSync);
    }

    @Override
    protected ParentWidget<?> createItemOutputSlots(ModularPanel panel, PanelSyncManager syncManager) {
        return new ParentWidget<>().size(COLUMNS * SLOT_SIZE)
            .child(new IDrawable.DrawableWidget(new EssentiaPreview()).size(COLUMNS * SLOT_SIZE)
                .tooltipDynamic(this::buildEssentiaTooltip).tooltipAutoUpdate(true).tooltipShowUpTimer(TOOLTIP_DELAY));
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row().coverChildren().verticalCenter().leftRel(0)
            .child(new TextWidget<>(IKey.dynamic(this::stateText)).size(68, SLOT_SIZE).scale(0.8f)
                .tooltipDynamic(this::buildStateTooltip).tooltipAutoUpdate(true).tooltipShowUpTimer(TOOLTIP_DELAY));
    }

    private DisplayState displayState() {
        int ordinal = stateSync.getIntValue();
        return ordinal >= 0 && ordinal < DISPLAY_STATES.length ? DISPLAY_STATES[ordinal] : DisplayState.Idle;
    }

    private String stateText() {
        return switch (displayState()) {
            case Idle -> {
                // #tr gui.essentia_disassembler.idle
                // # Ready
                // # zh_CN 待机
                yield StatCollector.translateToLocal("gui.essentia_disassembler.idle");
            }
            case Processing -> {
                // #tr gui.essentia_disassembler.processing
                // # Processing
                // # zh_CN 分解中
                yield StatCollector.translateToLocal("gui.essentia_disassembler.processing");
            }
            case NoPower -> {
                // #tr gui.essentia_disassembler.no_power
                // # No EU
                // # zh_CN 电量不足
                yield StatCollector.translateToLocal("gui.essentia_disassembler.no_power");
            }
            case NetworkUnavailable -> {
                // #tr gui.essentia_disassembler.network_unavailable
                // # ME unavailable
                // # zh_CN ME 不可用
                yield StatCollector.translateToLocal("gui.essentia_disassembler.network_unavailable");
            }
            case WaitingOutput -> {
                // #tr gui.essentia_disassembler.waiting_output
                // # Waiting for ME
                // # zh_CN 等待 ME 输出
                yield StatCollector.translateToLocal("gui.essentia_disassembler.waiting_output");
            }
            case Disabled -> {
                // #tr gui.essentia_disassembler.disabled
                // # Disabled
                // # zh_CN 已关闭
                yield StatCollector.translateToLocal("gui.essentia_disassembler.disabled");
            }
            case NoEssentia -> {
                // #tr gui.essentia_disassembler.no_essentia
                // # No essentia
                // # zh_CN 物品无源质
                yield StatCollector.translateToLocal("gui.essentia_disassembler.no_essentia");
            }
        };
    }

    private void buildStateTooltip(RichTooltip tooltip) {
        tooltip.addLine(stateText());
        if (displayState() == DisplayState.NetworkUnavailable) {
            // #tr gui.essentia_disassembler.network_hint
            // # Connect a powered ME network with a free channel.
            // # zh_CN 请连接已供电且有空闲频道的 ME 网络。
            tooltip.addLine(StatCollector.translateToLocal("gui.essentia_disassembler.network_hint"));
        } else if (displayState() == DisplayState.WaitingOutput) {
            // #tr gui.essentia_disassembler.output_hint
            // # ME needs space for essentia; the machine retains any pending output.
            // # zh_CN ME 需要可用源质存储；待输出源质会保留在机器内。
            tooltip.addLine(StatCollector.translateToLocal("gui.essentia_disassembler.output_hint"));
        }
    }

    private void buildEssentiaTooltip(RichTooltip tooltip) {
        refreshDisplay();
        // #tr gui.essentia_disassembler.essentia
        // # Essentia output
        // # zh_CN 输出源质
        tooltip.addLine(StatCollector.translateToLocal("gui.essentia_disassembler.essentia"));
        tooltip.addLine(stateText());
        for (AspectDisplay entry : displayedAspects) {
            tooltip
                .addLine(entry.aspect.getName() + " (" + entry.aspect.getLocalizedDescription() + "): " + entry.amount);
        }
    }

    private void refreshDisplay() {
        String raw = essentiaSync.getValue();
        if (raw == null) raw = "";
        if (raw.equals(displayedRaw)) return;
        displayedRaw = raw;
        displayedAspects.clear();
        for (String entry : raw.split("\n")) {
            int separator = entry.lastIndexOf('=');
            if (separator <= 0) continue;
            Aspect aspect = Aspect.getAspect(entry.substring(0, separator));
            if (aspect == null) continue;
            try {
                int amount = Integer.parseInt(entry.substring(separator + 1));
                if (amount > 0) displayedAspects.add(new AspectDisplay(aspect, amount));
            } catch (NumberFormatException ignored) {}
        }
        displayedAspects.sort(Comparator.comparingInt((AspectDisplay entry) -> entry.amount).reversed()
            .thenComparing(entry -> entry.aspect.getTag()));
    }

    private static String encodeAspects(AspectList aspects) {
        StringBuilder encoded = new StringBuilder();
        for (Aspect aspect : aspects.getAspects()) {
            if (aspect == null) continue;
            int amount = aspects.getAmount(aspect);
            if (amount <= 0) continue;
            encoded.append(aspect.getTag()).append('=').append(amount).append('\n');
        }
        return encoded.toString();
    }

    private static final class AspectDisplay {

        private final Aspect aspect;
        private final int amount;
        private final String amountText;
        private final UITexture texture;

        private AspectDisplay(Aspect aspect, int amount) {
            this.aspect = aspect;
            this.amount = amount;
            amountText = Integer.toString(amount);
            texture = UITexture.builder().location(aspect.getImage()).fullImage().nonOpaque().build()
                .withColorOverride(0xFF000000 | aspect.getColor());
        }
    }

    private final class EssentiaPreview implements IDrawable {

        @Override
        @SideOnly(Side.CLIENT)
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
            refreshDisplay();
            int count = Math.min(MAX_ICONS, displayedAspects.size());
            int rows = (count + COLUMNS - 1) / COLUMNS;
            int top = y + (height - rows * SLOT_SIZE) / 2;
            for (int index = 0; index < count; index++) {
                AspectDisplay entry = displayedAspects.get(index);
                int iconX = x + index % COLUMNS * SLOT_SIZE + 1;
                int iconY = top + index / COLUMNS * SLOT_SIZE + 1;
                entry.texture.draw(context, iconX, iconY, ICON_SIZE, ICON_SIZE, widgetTheme);
                applyColor(widgetTheme.getColor());
                drawAmount(entry.amountText, iconX, iconY);
            }
        }

        @SideOnly(Side.CLIENT)
        private void drawAmount(String text, int x, int y) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int textWidth = font.getStringWidth(text);
            float scale = Math.min(1.0f, (float) ICON_SIZE / Math.max(1, textWidth));
            GL11.glPushMatrix();
            GL11.glTranslatef(x + ICON_SIZE - textWidth * scale, y + ICON_SIZE - font.FONT_HEIGHT * scale, 0);
            GL11.glScalef(scale, scale, 1);
            font.drawStringWithShadow(text, 0, 0, 0xFFFFFF);
            GL11.glPopMatrix();
        }
    }
}
