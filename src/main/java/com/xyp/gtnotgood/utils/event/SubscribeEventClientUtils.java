package com.xyp.gtnotgood.utils.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.MouseEvent;

import org.lwjgl.input.Mouse;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.items.VeinMiningPickaxe.VeinMiningPickaxe;
import com.xyp.gtnotgood.common.packet.SyncVeinPickaxeNBT;
import com.xyp.gtnotgood.config.Config;
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
        if (player == null) return;

        ItemStack held = player.getCurrentEquippedItem();
        if (held == null) return;

        if (!(held.getItem() instanceof VeinMiningPickaxe)) return;

        NBTTagCompound nbt = held.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            held.setTagCompound(nbt);
        }

        boolean rightClickHeld = Mouse.isButtonDown(1);

        // Shift + 滚轮: 调节范围 (Range)
        // Shift + Scroll: Adjust range
        if (player.isSneaking() && !rightClickHeld) {
            if (event.dwheel == 0) return;
            int oldRange = nbt.hasKey("range") ? nbt.getInteger("range") : 3;
            int newRange = oldRange;

            if (event.dwheel > 0) {
                newRange++;
            } else {
                newRange--;
            }

            // 限制范围: 0 ~ maxRange
            // Clamp range: 0 ~ maxRange
            if (newRange < 0) newRange = 0;
            if (newRange > Config.VeinMinerPickaxe.maxRange) newRange = Config.VeinMinerPickaxe.maxRange;

            if (newRange != oldRange) {
                nbt.setInteger("range", newRange);

                // 显示副标题反馈
                // Show subtitle feedback
                if (mc.theWorld.isRemote && held.getItem() instanceof SubtitleDisplay) {
                    ((SubtitleDisplay) held.getItem()).showSubtitle("Tooltip_VeinMiningPickaxe_00", newRange);
                }

                // 同步到服务器
                // Sync to server
                int slot = player.inventory.currentItem;
                GTNotGood.channel.sendToServer(new SyncVeinPickaxeNBT(slot, nbt));
                event.setCanceled(true);
            }
        }

        // 右键按住 + 滚轮: 调节数量 (Amount)
        // Right-click hold + Scroll: Adjust amount
        if (!player.isSneaking() && rightClickHeld) {
            if (event.dwheel == 0) return;
            int oldAmount = nbt.hasKey("amount") ? nbt.getInteger("amount") : 327670;
            int newAmount = oldAmount;

            if (event.dwheel > 0) {
                newAmount += 10000;
            } else {
                newAmount -= 10000;
            }

            // 限制数量: 0 ~ maxAmount
            // Clamp amount: 0 ~ maxAmount
            if (newAmount < 0) newAmount = 0;
            if (newAmount > Config.VeinMinerPickaxe.maxAmount) newAmount = Config.VeinMinerPickaxe.maxAmount;

            if (newAmount != oldAmount) {
                nbt.setInteger("amount", newAmount);

                // 显示副标题反馈
                // Show subtitle feedback
                if (mc.theWorld.isRemote && held.getItem() instanceof SubtitleDisplay) {
                    ((SubtitleDisplay) held.getItem()).showSubtitle("Tooltip_VeinMiningPickaxe_01", newAmount);
                }

                // 同步到服务器
                // Sync to server
                int slot = player.inventory.currentItem;
                GTNotGood.channel.sendToServer(new SyncVeinPickaxeNBT(slot, nbt));
                event.setCanceled(true);
            }
        }
    }

}
