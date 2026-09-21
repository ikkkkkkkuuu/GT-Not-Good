// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

/** Upstream animated selection tool, with normal-select / Shift-bind controls that bypass target GUIs. */
public final class ItemWirelessConnector extends Item {

    public ItemWirelessConnector() {
        // #tr item.packaged_wireless_connector.name
        // # Item Wireless Connector
        // # zh_CN 物品无线连接器
        setUnlocalizedName("packaged_wireless_connector");
        setMaxStackSize(1);
        setCreativeTab(GTNGCreativeTabs.GTNGItem);
        setTextureName(ModList.GTNotGood.getResourcePath("packaged/wireless_connector"));
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) GTNotGood.channel.sendToServer(new MessagePackagedConnector(x, y, z, side));
        // The custom request is the sole mutation path; vanilla must not perform the action twice.
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey("SelectedProvider")) {
            stack.getTagCompound()
                .removeTag("SelectedProvider");
            if (stack.getTagCompound()
                .hasNoTags()) stack.setTagCompound(null);
            // #tr chat.packaged.deselected
            // # Provider selection cleared.
            // # zh_CN 已取消选择供应器。
            player.addChatMessage(new ChatComponentTranslation("chat.packaged.deselected"));
        }
        return stack;
    }

    public static PackagedTarget selection(ItemStack stack) {
        return stack != null && stack.hasTagCompound() ? PackagedTarget.read(
            stack.getTagCompound()
                .getCompoundTag("SelectedProvider"))
            : null;
    }

    static void use(EntityPlayer player, int x, int y, int z, int face) {
        World world = player.worldObj;
        ItemStack held = player.getHeldItem();
        if (world.getTileEntity(x, y, z) instanceof TilePackagedProvider provider) {
            if (player.isSneaking() || !provider.canConfigure(player)) return;
            if (!held.hasTagCompound()) held.setTagCompound(new NBTTagCompound());
            NBTTagCompound selected = new PackagedTarget(world.provider.dimensionId, x, y, z, face).write();
            selected.setString("HostType", "provider");
            held.getTagCompound()
                .setTag("SelectedProvider", selected);
            // #tr chat.packaged.selected
            // # Selected provider at %s, %s, %s.
            // # zh_CN 已选择供应器：%s，%s，%s。
            player.addChatMessage(new ChatComponentTranslation("chat.packaged.selected", x, y, z));
            return;
        }
        if (!player.isSneaking()) return;
        PackagedTarget selected = selection(held);
        if (selected == null || !(selected.resolve(world) instanceof TilePackagedProvider provider)
            || !provider.canConfigure(player)) {
            // #tr chat.packaged.no_selection
            // # Select an available provider in this dimension first.
            // # zh_CN 请先选择本维度已加载的供应器。
            player.addChatMessage(new ChatComponentTranslation("chat.packaged.no_selection"));
            return;
        }
        if (provider.bind(new PackagedTarget(world.provider.dimensionId, x, y, z, face))) {
            // #tr chat.packaged.bound
            // # Target connected: %s, %s, %s.
            // # zh_CN 已连接目标：%s，%s，%s。
            player.addChatMessage(new ChatComponentTranslation("chat.packaged.bound", x, y, z));
        } else {
            // #tr chat.packaged.bind_failed
            // # Cannot connect: duplicate, invalid target, or connection limit reached.
            // # zh_CN 无法连接：重复、无效目标或连接数量已达上限。
            player.addChatMessage(new ChatComponentTranslation("chat.packaged.bind_failed"));
        }
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        // #tr tooltip.packaged.connector_select
        // # Right click a Wireless Provider to select it.
        // # zh_CN 普通右键无线供应器以选择。
        lines.add(StatCollector.translateToLocal("tooltip.packaged.connector_select"));
        // #tr tooltip.packaged.connector_bind
        // # Shift + right click a target to connect; right click air to clear selection.
        // # zh_CN Shift右键目标以连接；右键空气取消选择。
        lines.add(StatCollector.translateToLocal("tooltip.packaged.connector_bind"));
        PackagedTarget selected = selection(stack);
        if (selected != null) {
            // #tr tooltip.packaged.connector_selection
            // # Selected: dimension %s, %s / %s / %s
            // # zh_CN 已选择：维度 %s，%s / %s / %s
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "tooltip.packaged.connector_selection",
                    selected.dimension,
                    selected.x,
                    selected.y,
                    selected.z));
        }
    }
}
