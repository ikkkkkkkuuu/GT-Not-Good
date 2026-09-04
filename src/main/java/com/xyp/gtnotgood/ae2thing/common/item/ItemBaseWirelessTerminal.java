package com.xyp.gtnotgood.ae2thing.common.item;

import java.util.EnumSet;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;

import com.xyp.gtnotgood.ae2thing.inventory.InventoryHandler;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.ae2thing.inventory.item.IItemInventory;
import com.xyp.gtnotgood.ae2thing.util.BlockPos;

import appeng.api.AEApi;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.features.IWirelessTermRegistry;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.core.features.AEFeature;
import appeng.core.localization.PlayerMessages;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;
import appeng.items.tools.powered.ToolWirelessTerminal;
import appeng.util.Platform;
import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;

public abstract class ItemBaseWirelessTerminal extends ToolWirelessTerminal
    implements IItemInventory, IBauble, IBaubleExpanded, IGuiItem {

    public ItemBaseWirelessTerminal() {
        super();
        this.setFeature(EnumSet.of(AEFeature.WirelessAccessTerminal, AEFeature.PoweredTools));
    }

    public ItemStack stack() {
        return new ItemStack(this, 1);
    }

    @Override
    public boolean canHandle(final ItemStack is) {
        return is != null && is.getItem() instanceof ItemBaseWirelessTerminal;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    /**
     * Allow the terminal to be placed into every registered bauble slot type, not just AE2's dedicated terminal slot.
     */
    @Override
    public String[] getBaubleTypes(ItemStack itemstack) {
        java.util.List<String> types = new java.util.ArrayList<>();
        for (String type : BaubleExpandedSlots.getCurrentlyRegisteredTypes()) {
            if (type == null || type.equals(BaubleExpandedSlots.unknownType)
                || type.equals(BaubleExpandedSlots.invalidType)) {
                continue;
            }
            types.add(type);
        }
        return types.toArray(new String[0]);
    }

    @Override
    public ItemStack onItemRightClick(final ItemStack item, final World w, final EntityPlayer player) {
        if (ForgeEventFactory.onItemUseStart(player, item, 1) > 0) {
            if (Platform.isClient()) return item;
            IWirelessTermRegistry term = AEApi.instance()
                .registries()
                .wireless();
            if (!term.isWirelessTerminal(item)) {
                player.addChatMessage(PlayerMessages.DeviceNotWirelessTerminal.get());
                return item;
            }
            final IWirelessTermHandler handler = term.getWirelessTerminalHandler(item);
            final String unparsedKey = handler.getEncryptionKey(item);
            if (unparsedKey.isEmpty()) {
                player.addChatMessage(PlayerMessages.DeviceNotLinked.get());
                return item;
            }
            final long parsedKey = Long.parseLong(unparsedKey);
            final ILocatable securityStation = AEApi.instance()
                .registries()
                .locatable()
                .getLocatableBy(parsedKey);
            if (securityStation == null) {
                player.addChatMessage(PlayerMessages.StationCanNotBeLocated.get());
                return item;
            }
            if (handler.hasPower(player, 0.5, item)) {
                InventoryHandler.openGui(
                    player,
                    w,
                    new BlockPos(player.inventory.currentItem, 0, 0),
                    ForgeDirection.UNKNOWN,
                    this.guiGuiType(item));
            } else {
                player.addChatMessage(PlayerMessages.DeviceNotPowered.get());
            }
        }

        return item;
    }

    @Override
    public boolean onItemUseFirst(ItemStack itemstack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        return super.onItemUseFirst(itemstack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    /**
     * When the AE2 wireless crafting terminal path (WirelessTerminalGuiObject / AEBaseContainer.portableSourceTick)
     * drives this terminal it drains real AE power unless the handler reports infinite power. Our own two-in-one GUI
     * already treats the infinity energy card as "no drain" (WirelessObject#extractPower), so mirror that here so both
     * GUIs behave the same: energy card -> infinite power, infinity booster card -> infinite range.
     */
    @Override
    public boolean hasInfinityPower(ItemStack is) {
        return com.xyp.gtnotgood.ae2thing.api.WirelessObject.hasEnergyCard(is);
    }

    @Override
    public boolean hasInfinityRange(ItemStack is) {
        return com.xyp.gtnotgood.ae2thing.api.WirelessObject.hasInfinityBoosterCard(is);
    }

    /**
     * When AE2's own GUI switching (PacketSwitchGuis / GuiBridge) reopens a terminal from its item stack — e.g. the
     * "back" button on the crafting-status screen returning to GUI_CRAFTING_TERMINAL — it rebuilds the host via
     * {@link appeng.core.sync.GuiBridge#getGuiObject}. Without this override that path wraps any wireless terminal in a
     * plain {@link appeng.helpers.WirelessTerminalGuiObject}, which is NOT an
     * {@link appeng.api.parts.ICraftingTerminal},
     * so GUI_CRAFTING_TERMINAL's host check fails, a ContainerNull is returned and the GUI closes. Returning a
     * {@link WirelessCraftingTerminalGuiObject} (an ICraftingTerminal superset of the plain object) makes the crafting
     * terminal reopen correctly while remaining valid for the plain ME/status GUIs too. Because IGuiItem is checked
     * before the wireless-handler fallback in getGuiObject, this takes effect for every AE2-driven reopen of our
     * terminal.
     */
    @Override
    public IGuiItemObject getGuiObject(ItemStack is, World world, EntityPlayer player, int x, int y, int z) {
        IWirelessTermHandler handler = AEApi.instance()
            .registries()
            .wireless()
            .getWirelessTerminalHandler(is);
        if (handler == null) {
            return null;
        }
        return new WirelessCraftingTerminalGuiObject(handler, is, player, world, x, y, z);
    }

    protected abstract GuiType guiGuiType(ItemStack item);
}
