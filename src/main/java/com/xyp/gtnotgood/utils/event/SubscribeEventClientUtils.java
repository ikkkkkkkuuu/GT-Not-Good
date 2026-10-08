package com.xyp.gtnotgood.utils.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;

import org.lwjgl.input.Mouse;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.items.veinmining.VeinMiningPickaxe;
import com.xyp.gtnotgood.common.items.veinmining.VeinMiningSettings;
import com.xyp.gtnotgood.common.items.veinmining.VeinMiningSettings.Setting;
import com.xyp.gtnotgood.common.network.UpdateVeinMiningSetting;
import com.xyp.gtnotgood.utils.item.SubtitleDisplay;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SubscribeEventClientUtils {

    /**
     * 处理鼠标滚轮事件
     * Handle mouse wheel events for Vein Mining Pickaxe adjustments
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || event.dwheel == 0) return;

        ItemStack held = player.getCurrentEquippedItem();
        if (held == null) return;

        if (!(held.getItem() instanceof VeinMiningPickaxe)) return;

        boolean rightClickHeld = Mouse.isButtonDown(1);
        Setting setting;
        String subtitle;
        if (player.isSneaking() && !rightClickHeld) {
            setting = Setting.Range;
            // #tr Tooltip_VeinMiningPickaxe_00
            // # Range: %d
            // # zh_CN 范围: %d
            subtitle = "Tooltip_VeinMiningPickaxe_00";
        } else if (!player.isSneaking() && rightClickHeld) {
            setting = Setting.Amount;
            // #tr Tooltip_VeinMiningPickaxe_01
            // # Amount: %d
            // # zh_CN 数量: %d
            subtitle = "Tooltip_VeinMiningPickaxe_01";
        } else {
            return;
        }

        int value = setting.scroll(held, event.dwheel);
        if (value == setting.read(held)) return;
        int slot = player.inventory.currentItem;
        if (!VeinMiningSettings.apply(player.inventory, slot, setting, value)) return;
        if (mc.theWorld.isRemote && held.getItem() instanceof SubtitleDisplay display) {
            display.showSubtitle(subtitle, value);
        }
        GTNotGood.channel.sendToServer(new UpdateVeinMiningSetting(slot, setting, value));
        event.setCanceled(true);
    }

}
