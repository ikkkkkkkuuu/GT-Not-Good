package com.xyp.gtnotgood.ae2thing.common.item;

import static com.xyp.gtnotgood.ae2thing.api.WirelessObject.hasEnergyCard;
import static com.xyp.gtnotgood.ae2thing.api.WirelessObject.hasInfinityBoosterCard;
import static com.xyp.gtnotgood.ae2thing.loader.recipe.WirelessTerminalEnergyRecipe.getEnergyCard;
import static com.xyp.gtnotgood.ae2thing.loader.recipe.WirelessTerminalQuantumBridgeRecipe.getInfinityBoosterCard;
import static net.minecraft.client.gui.GuiScreen.isCtrlKeyDown;
import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.ae2thing.api.WirelessObject;
import com.xyp.gtnotgood.ae2thing.common.tabs.AE2ThingTabs;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.ae2thing.inventory.item.IItemInventory;
import com.xyp.gtnotgood.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.xyp.gtnotgood.ae2thing.loader.KeybindLoader;
import com.xyp.gtnotgood.ae2thing.util.NameConst;
import com.xyp.gtnotgood.ae2thing.util.Util;

import appeng.api.AEApi;
import appeng.core.features.AEFeature;
import appeng.core.localization.PlayerMessages;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemWirelessDualInterfaceTerminal extends ItemBaseWirelessTerminal implements IItemInventory {

    public ItemWirelessDualInterfaceTerminal() {
        AEApi.instance().registries().wireless().registerWirelessHandler(this);
        this.setFeature(EnumSet.of(AEFeature.WirelessAccessTerminal, AEFeature.PoweredTools));
        // #tr item.wireless_dual_interface_terminal.name
        // # ME Wireless Dual Interface Terminal
        // # zh_CN ME无线二合一接口终端
        setUnlocalizedName(NameConst.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL);
        setTextureName(AE2Thing.resource(NameConst.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL).toString());
    }

    @Override
    protected GuiType guiGuiType(ItemStack item) {
        // Restore whichever view (dual interface vs AE2 crafting terminal) the player last switched to on this stack.
        return Util.getLastGuiMode(item, GuiType.WirelessDualInterfaceTerminal);
    }

    @Override
    public Object getInventory(ItemStack stack, World world, int x, int y, int z, EntityPlayer player) {
        try {
            return new WirelessObject(stack, world, x, y, z, player)
                .getInventory(WirelessDualInterfaceTerminalInventory.class);
        } catch (Exception e) {
            player.addChatMessage(PlayerMessages.OutOfRange.get());
            return null;
        }
    }

    @Override
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> toolTip,
        boolean displayMoreInfo) {
        super.addCheckedInformation(stack, player, toolTip, displayMoreInfo);
        if (isShiftKeyDown()) {
            // #tr gtnotgood.tooltip.wireless_dual_interface_terminal.desc
            // # Access ME interfaces and encode patterns wirelessly
            // # zh_CN 无线访问 ME 接口并编码样板
            toolTip.add(I18n.format(NameConst.TT_INTERFACE_TERMINAL_DESC));
            // #tr gtnotgood.tooltip.wireless_dual_interface_terminal.key_open
            // # §7Press §b%s§7 to open the terminal
            // # zh_CN §7按 §b%s§7 打开终端
            toolTip.add(I18n.format(NameConst.TT_INTERFACE_TERMINAL_KEY_OPEN,
                keyName(KeybindLoader.openDualInterfaceTerminal)));
            // #tr gtnotgood.tooltip.wireless_dual_interface_terminal.key_send
            // # §7Press §b%s§7 to send the held item to the ME network
            // # zh_CN §7按 §b%s§7 将手持物品存入 ME 网络
            toolTip.add(
                I18n.format(NameConst.TT_INTERFACE_TERMINAL_KEY_SEND, keyName(KeybindLoader.sendHeldItemToNetwork)));
            // #tr gtnotgood.tooltip.wireless_dual_interface_terminal.nei_to_inv
            // # §7Shift/Ctrl+middle-click an NEI item to request it from the ME network
            // # zh_CN §7Shift/Ctrl+中键点击 NEI 物品，从 ME 网络提取到物品栏
            toolTip.add(I18n.format(NameConst.TT_INTERFACE_TERMINAL_NEI_TO_INV));
        } else {
            // #tr gtnotgood.tooltip.shift_for_more
            // # §7Hold §bShift§7 for controls and functions
            // # zh_CN §7按住 §bShift§7 查看功能和操作
            toolTip.add(I18n.format(NameConst.TT_SHIFT_FOR_MORE));
        }
        if (isCtrlKeyDown()) {
            // #tr gtnotgood.tooltip.wireless.installed
            // # Installed cards:
            // # zh_CN 已安装的卡：
            toolTip.add(I18n.format(NameConst.TT_WIRELESS_INSTALLED));
            if (hasInfinityBoosterCard(stack) && getInfinityBoosterCard() != null) {
                toolTip.add("  " + EnumChatFormatting.GOLD + getInfinityBoosterCard().getDisplayName());
            }
            if (hasEnergyCard(stack) && getEnergyCard() != null) {
                toolTip.add("  " + EnumChatFormatting.GOLD + getEnergyCard().getDisplayName());
            }
        } else {
            // #tr gtnotgood.tooltip.ctrl_for_more
            // # §7Hold §bCtrl§7 to view installed cards
            // # zh_CN §7按住 §bCtrl§7 查看已安装的卡
            toolTip.add(I18n.format(NameConst.TT_CTRL_FOR_MORE));
        }

    }

    /**
     * Live display name of a keybind for the tooltip, read fresh each render so it reflects the player's current
     * setting in Controls. Returns a localized "unbound" label when the key has no binding.
     */
    @SideOnly(Side.CLIENT)
    private static String keyName(KeyBinding key) {
        if (key == null || key.getKeyCode() == Keyboard.CHAR_NONE) {
            // #tr gtnotgood.tooltip.key_unbound
            // # Unbound
            // # zh_CN 未绑定
            return I18n.format(NameConst.TT_KEY_UNBOUND);
        }
        return GameSettings.getKeyDisplayString(key.getKeyCode());
    }

    public ItemWirelessDualInterfaceTerminal register() {
        GameRegistry.registerItem(this, NameConst.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL, AE2Thing.MODID);
        setCreativeTab(AE2ThingTabs.INSTANCE);
        return this;
    }

}
