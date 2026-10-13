// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.client.wirelessfrequency;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.items.wirelessfrequency.WirelessFrequencyCard;
import com.xyp.gtnotgood.common.network.wirelessfrequency.OpenWirelessCardPacket;
import com.xyp.gtnotgood.common.network.wirelessfrequency.ToggleWirelessCardPacket;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardBinding;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardVisualisation;

import appeng.core.sync.packets.PacketNetworkVisualiserData;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.Unpooled;

@SideOnly(Side.CLIENT)
public class WirelessCardInput {

    // #tr key.gtnotgood.wireless_card_auto
    // # Toggle frequency card auto-connect
    // # zh_CN 切换频率卡自动连接
    private static final KeyBinding TOGGLE = new KeyBinding("key.gtnotgood.wireless_card_auto", Keyboard.KEY_NONE,
        "key.categories.gtnotgood");
    private long nextInput;
    // #tr key.gtnotgood.wireless_card_open
    // # Open frequency card manager
    // # zh_CN 打开频率卡管理界面
    private static final KeyBinding OPEN = new KeyBinding("key.gtnotgood.wireless_card_open", Keyboard.KEY_NONE,
        "key.categories.gtnotgood");
    // #tr key.gtnotgood.wireless_card_visualisation
    // # Cycle frequency card network view
    // # zh_CN 切换频率卡网络视图
    private static final KeyBinding VISUALISATION = new KeyBinding("key.gtnotgood.wireless_card_visualisation",
        Keyboard.KEY_NONE, "key.categories.gtnotgood");
    private static World visualWorld;
    private static WirelessCardBinding visualBinding;
    private static ItemStack visualTool;
    private static int visualMode;
    private static long visualUntil;
    private static boolean visualVisible;

    public static void register() {
        ClientRegistry.registerKeyBinding(TOGGLE);
        ClientRegistry.registerKeyBinding(OPEN);
        ClientRegistry.registerKeyBinding(VISUALISATION);
        WirelessCardInput handler = new WirelessCardInput();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    @SubscribeEvent
    public void key(InputEvent.KeyInputEvent event) {
        keys();
    }

    @SubscribeEvent
    public void mouseKey(InputEvent.MouseInputEvent event) {
        keys();
    }

    private void keys() {
        if (OPEN.isPressed() && acceptInput()) GTNotGood.channel.sendToServer(new OpenWirelessCardPacket());
        if (TOGGLE.isPressed() && acceptInput())
            GTNotGood.channel.sendToServer(new ToggleWirelessCardPacket(false, 0, false, false));
        if (VISUALISATION.isPressed() && acceptInput())
            GTNotGood.channel.sendToServer(new ToggleWirelessCardPacket(false, 0, true, false));
    }

    public static boolean visualisationKey(int key) {
        return key != Keyboard.KEY_NONE && key == VISUALISATION.getKeyCode();
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.world == visualWorld) clearVisualisation();
    }

    public static void clearVisualisation() {
        visualWorld = null;
        visualBinding = null;
        visualTool = null;
        visualUntil = 0;
        visualVisible = false;
    }

    public static void receiveVisualisation(Minecraft mc, WirelessCardBinding binding, int dimension, byte[] payload) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.dimension != dimension) return;
        ItemStack held = WirelessCardVisualisation.activeCard(mc.thePlayer);
        if (
            held == null || !binding.belongsTo(mc.thePlayer.getUniqueID())
                || !binding.equals(WirelessCardBinding.read(held.getTagCompound()))
        ) return;
        int mode = WirelessCardVisualisation.mode(held.getTagCompound());
        if (mode == 0) return;
        clearVisualisation();
        if (payload.length == 0) return;
        var buffer = Unpooled.wrappedBuffer(payload);
        try {
            var packet = new PacketNetworkVisualiserData(buffer);
            packet.clientPacketData(null, packet, mc.thePlayer);
            visualTool = WirelessCardVisualisation.tool(binding, dimension, mode);
            visualMode = mode;
            visualBinding = binding;
            visualWorld = mc.theWorld;
            visualUntil = Minecraft.getSystemTime() + 15_000;
        } catch (IOException | RuntimeException malformed) {
            clearVisualisation();
        } finally {
            buffer.release();
        }
    }

    public static ItemStack visualiserStack(ItemStack held) {
        Minecraft mc = Minecraft.getMinecraft();
        if (
            mc.theWorld != visualWorld || mc.thePlayer == null
                || visualBinding != null && !visualBinding.belongsTo(mc.thePlayer.getUniqueID())
        ) clearVisualisation();
        ItemStack card = WirelessCardVisualisation.activeCard(mc.thePlayer);
        if (card == null) {
            // Match AE: hiding the view does not discard its nodes/links or compiled display list.
            visualVisible = false;
            return held; // Ordinary AE tools retain their original renderer.
        }
        int mode = WirelessCardVisualisation.mode(card.getTagCompound());
        if (
            mode == 0 || visualBinding == null || !visualBinding.equals(WirelessCardBinding.read(card.getTagCompound()))
        ) {
            clearVisualisation();
            return null;
        }
        long now = Minecraft.getSystemTime();
        if (!visualVisible) {
            visualVisible = true;
            // Show the retained snapshot immediately while the server refreshes it. If the source
            // remains unavailable, stop drawing after the same grace period as an active view.
            visualUntil = now + 15_000;
        }
        if (now >= visualUntil) return null;
        if (visualMode != mode) {
            visualMode = mode;
            visualTool = WirelessCardVisualisation.tool(visualBinding, mc.thePlayer.dimension, mode);
        }
        return visualTool;
    }

    private boolean acceptInput() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.thePlayer == null || mc.theWorld == null) return false;
        long now = System.nanoTime();
        if (now < nextInput) return false;
        nextInput = now + 300_000_000L;
        return true;
    }

    @SubscribeEvent
    public void mouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.thePlayer == null || event.dwheel == 0 || !mc.thePlayer.isSneaking()) return;
        ItemStack held = mc.thePlayer.getCurrentEquippedItem();
        if (held == null || !(held.getItem() instanceof WirelessFrequencyCard)) return;
        event.setCanceled(true); // Consume wheel input before vanilla changes the selected hotbar slot.
        if (!acceptInput()) return;
        GTNotGood.channel.sendToServer(
            new ToggleWirelessCardPacket(true, mc.thePlayer.inventory.currentItem, true, event.dwheel > 0));
    }
}
