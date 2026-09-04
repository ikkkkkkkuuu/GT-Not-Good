package com.xyp.gtnotgood.common.mebridge;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Handheld channel selector and programmer for persistent wireless ME links. */
public final class ItemMEWirelessTransceiver extends Item implements IGuiHolder<PlayerInventoryGuiData> {

    public static final String ITEM_NAME = "MEWirelessTransceiver";
    private static final String CHANNEL_NBT_KEY = "mebridge_channel";

    public ItemMEWirelessTransceiver() {
        setMaxStackSize(1);
        setCreativeTab(GTNGCreativeTabs.GTNGItem);
        // #tr item.me_wireless_transceiver.name
        // # ME Wireless Transceiver
        // # zh_CN ME 无线收发器
        setUnlocalizedName("me_wireless_transceiver");
        setTextureName(GTNotGood.MODID + ":me_wireless_transceiver");
    }

    public static String getSelectedChannel(ItemStack stack) {
        if (stack == null || stack.stackTagCompound == null) return "";
        String channel = MEBridgeChannelName.normalize(stack.stackTagCompound.getString(CHANNEL_NBT_KEY));
        return MEBridgeChannelName.isValid(channel) ? channel : "";
    }

    public static void setSelectedChannel(ItemStack stack, String name) {
        if (stack == null) return;
        String channel = MEBridgeChannelName.normalize(name);
        if (!MEBridgeChannelName.isValid(channel)) return;
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setString(CHANNEL_NBT_KEY, channel);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) PlayerInventoryGuiFactory.INSTANCE.openFromMainHand(player);
        return stack;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isHeld) {
        if (!world.isRemote && isHeld && entity instanceof EntityPlayerMP) {
            MEWirelessVisualizationSync.update((EntityPlayerMP) entity);
        }
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            GTNotGood.channel.sendToServer(new MessageMEWirelessNodeAction(x, y, z, side, player.isSneaking()));
            // Suppress local block activation; the dedicated packet performs the action server-side.
            return true;
        }
        if (player instanceof EntityPlayerMP) {
            if (player.isSneaking()) {
                handleBindRequest((EntityPlayerMP) player, world, x, y, z, side);
            } else {
                handleInspectRequest((EntityPlayerMP) player, world, x, y, z, side);
            }
        }
        return true;
    }

    static void handleBindRequest(EntityPlayerMP player, World world, int x, int y, int z, int side) {
        ItemStack stack = player.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemMEWirelessTransceiver)) return;
        String channel = getSelectedChannel(stack);
        MEWirelessLinkManager.BindResult result = MEWirelessLinkManager.toggle(player, world, x, y, z, side, channel);
        player.addChatMessage(new ChatComponentTranslation(messageKey(result), channel));
    }

    static void handleInspectRequest(EntityPlayerMP player, World world, int x, int y, int z, int side) {
        ItemStack stack = player.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemMEWirelessTransceiver)) return;

        MEWirelessLinkManager.Inspection inspection = MEWirelessLinkManager.inspect(world, x, y, z, side);
        if (!inspection.validTarget) {
            player.addChatMessage(new ChatComponentTranslation("chat.me_wireless_transceiver.invalid_target"));
        } else if (inspection.channel.isEmpty()) {
            // #tr chat.me_wireless_transceiver.inspect_unlinked
            // # This ME node has no wireless link.
            // # zh_CN 此 ME 节点没有无线连接。
            player.addChatMessage(new ChatComponentTranslation("chat.me_wireless_transceiver.inspect_unlinked"));
        } else {
            // #tr chat.me_wireless_transceiver.inspect_linked
            // # This ME node is wirelessly linked to channel %s.
            // # zh_CN 此 ME 节点已无线连接至频道 %s。
            player.addChatMessage(
                new ChatComponentTranslation("chat.me_wireless_transceiver.inspect_linked", inspection.channel));
        }
    }

    private static String messageKey(MEWirelessLinkManager.BindResult result) {
        switch (result) {
            case CONNECTED:
                // #tr chat.me_wireless_transceiver.connected
                // # Wireless link connected to channel %s.
                // # zh_CN 已无线连接至频道 %s。
                return "chat.me_wireless_transceiver.connected";
            case WAITING:
                // #tr chat.me_wireless_transceiver.waiting
                // # Link saved for channel %s; waiting for both endpoints to become available.
                // # zh_CN 已保存频道 %s 的连接，正在等待两端可用。
                return "chat.me_wireless_transceiver.waiting";
            case DISCONNECTED:
                // #tr chat.me_wireless_transceiver.disconnected
                // # Wireless link disconnected from channel %s.
                // # zh_CN 已断开频道 %s 的无线连接。
                return "chat.me_wireless_transceiver.disconnected";
            case NO_CHANNEL:
                // #tr chat.me_wireless_transceiver.no_channel
                // # Select a channel before binding an ME node.
                // # zh_CN 请先选择频道，再绑定 ME 节点。
                return "chat.me_wireless_transceiver.no_channel";
            case INVALID_TARGET:
                // #tr chat.me_wireless_transceiver.invalid_target
                // # The selected block does not expose a usable ME node.
                // # zh_CN 目标方块没有可用的 ME 节点。
                return "chat.me_wireless_transceiver.invalid_target";
            default:
                // #tr chat.me_wireless_transceiver.failed
                // # The wireless ME link could not be created.
                // # zh_CN 无法创建无线 ME 连接。
                return "chat.me_wireless_transceiver.failed";
        }
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return MEWirelessTransceiverGui.build(data, syncManager);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public com.cleanroommc.modularui.screen.ModularScreen createScreen(PlayerInventoryGuiData data,
        ModularPanel mainPanel) {
        return new com.cleanroommc.modularui.screen.ModularScreen(GTNotGood.MODID, mainPanel);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        String channel = getSelectedChannel(stack);
        // #tr tooltip.me_wireless_transceiver.channel
        // # Selected channel: %s
        // # zh_CN 当前频道：%s
        tooltip.add(
            EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted(
                "tooltip.me_wireless_transceiver.channel",
                channel.isEmpty() ? "-" : channel));
        // #tr tooltip.me_wireless_transceiver.open
        // # Right-click air: open channel directory
        // # zh_CN 右键空气：打开频道目录
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.me_wireless_transceiver.open"));
        // #tr tooltip.me_wireless_transceiver.bind
        // # Shift + right-click an ME node: bind, switch, or disconnect
        // # zh_CN Shift + 右键 ME 节点：绑定、切换或断开
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.me_wireless_transceiver.bind"));
        // #tr tooltip.me_wireless_transceiver.inspect
        // # Right-click an ME node: inspect its wireless link
        // # zh_CN 右键 ME 节点：查看无线连接
        tooltip
            .add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.me_wireless_transceiver.inspect"));
    }
}
