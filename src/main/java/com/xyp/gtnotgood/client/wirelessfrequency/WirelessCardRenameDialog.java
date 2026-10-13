// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.client.wirelessfrequency;

import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.RENAME;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.RENAME_SOURCE;

import java.util.function.LongSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardContainer;

import appeng.core.localization.ColorUtils;
import appeng.core.localization.GuiText;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WirelessCardRenameDialog extends Dialog<Void> {

    public final TextFieldWidget name;
    private final WirelessCardPanel owner;
    private final LongSupplier sourceTarget;
    private final WirelessCardContainer container;
    private final Minecraft mc = Minecraft.getMinecraft();
    private String target;
    private long nameRevision;
    private boolean finished;
    private long sourceId;
    private String originalName;

    WirelessCardRenameDialog(WirelessCardDetailsPanel owner) {
        this(owner, null);
    }

    WirelessCardRenameDialog(WirelessCardPanel owner, LongSupplier sourceTarget) {
        super("wireless_card_rename");
        this.owner = owner;
        this.sourceTarget = sourceTarget;
        this.container = owner.container;
        this.name = owner.textField(32);
        size(260, 96);
        background(owner.getBackground());
        child(ButtonWidget.panelCloseButton());
        child(IKey.str(GuiText.Renamer.getLocal()).asWidget().color(ColorUtils.guiTextColorGray.getColor()).pos(10, 10)
            .size(220, 18));
        child(name.pos(10, 34).size(240, 16));
        child(dialogButton("gui.cancel", this::closeIfOpen).pos(10, 64));
        child(dialogButton("gui.done", this::submit).pos(134, 64));
    }

    private ButtonWidget<?> dialogButton(String key, Runnable action) {
        return new ButtonWidget<>().size(116, 20).background(WirelessCardButton.BUTTON_TEXTURES[1])
            .hoverBackground(WirelessCardButton.BUTTON_TEXTURES[2])
            .overlay(IKey.lang(key).color(0xffe0e0e0).shadow(true)).onMousePressed(button -> {
                if (button != 0 || finished) return false;
                action.run();
                return true;
            });
    }

    @Override
    public void onOpen(ModularScreen screen) {
        NBTTagCompound data = container.display;
        target = data.getString("detailTarget");
        nameRevision = data.getLong("nameRevision");
        finished = false;
        String text = data.getBoolean("multipleNames") ? "" : data.getString("name");
        if (sourceTarget != null) {
            sourceId = sourceTarget.getAsLong();
            NBTTagCompound entry = sourceEntry();
            target = entry.getString("sourceKey");
            originalName = entry.getString("name");
            text = originalName;
        }
        name.getStringValue().setStringValue(text);
        name.setText(text);
        super.onOpen(screen);
        getContext().focus(name);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!isOpen() || isClosing()) return;
        if (finished) closeIfOpen();
        else validateTarget();
    }

    private boolean validateTarget() {
        NBTTagCompound data = container.display;
        if (sourceTarget != null) {
            NBTTagCompound entry = sourceEntry();
            if (
                data.getBoolean("sourcesPage") && !target.isEmpty()
                    && target.equals(entry.getString("sourceKey"))
                    && originalName.equals(entry.getString("name"))
            ) return true;
            closeIfOpen();
            return false;
        }
        if (
            data.getBoolean("detail") && data.getBoolean("renameable")
                && target.equals(data.getString("detailTarget"))
                && nameRevision == data.getLong("nameRevision")
        ) return true;
        closeIfOpen();
        // #tr gtnotgood.wireless.gui.stale
        // # Physical cluster changed. Retry after refreshing.
        // # zh_CN 物理簇已变化，请在刷新后重新操作。
        mc.thePlayer.addChatMessage(new ChatComponentTranslation("gtnotgood.wireless.gui.stale"));
        return false;
    }

    private NBTTagCompound sourceEntry() {
        var sources = container.display.getTagList("sources", 10);
        for (int i = 0; i < sources.tagCount(); i++) {
            NBTTagCompound entry = sources.getCompoundTagAt(i);
            if (entry.getLong("targetId") == sourceId) return entry;
        }
        return new NBTTagCompound();
    }

    private void submit() {
        if (finished || !owner.layoutReady() || !validateTarget()) return;
        if (sourceTarget != null) container.sync.request(RENAME_SOURCE, sourceId, name.getText(), 0);
        else container.sync.request(RENAME, container.display.getLong("detailId"), name.getText(), nameRevision);
        closeIfOpen();
    }

    @Override
    public boolean onKeyPressed(char character, int key) {
        if (finished) return true;
        if (key == Keyboard.KEY_ESCAPE) {
            closeIfOpen();
            return true;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            submit();
            return true;
        }
        return super.onKeyPressed(character, key);
    }

    @Override
    public void closeIfOpen() {
        // Retry closing after the opening animation, without allowing another submission.
        finished = true;
        super.closeIfOpen();
    }

    @Override
    public void onClose() {
        if (name.isFocused()) getContext().removeFocus();
        super.onClose();
    }
}
