package com.xyp.gtnotgood.ae2thing.inventory.gui;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.ae2thing.api.Constants;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftingTerm;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;
import baubles.api.BaublesApi;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Opens the vanilla AE2 wireless crafting terminal ({@link GuiCraftingTerm} / {@link ContainerCraftingTerm}) backed by
 * the ME Wireless Dual Interface Terminal item. The terminal item is a registered {@link IWirelessTermHandler}, so
 * wrapping it in AE2's {@link WirelessCraftingTerminalGuiObject} reuses AE2's own network locating, range check and
 * energy handling against the exact same ME network the dual interface terminal is linked to. Mirrors how AE2FC's
 * wireless ultra terminal opens its CRAFTING mode.
 */
public class WirelessCraftingTerminalGuiFactory implements IGuiFactory {

    @Nullable
    private static WirelessCraftingTerminalGuiObject buildHost(EntityPlayer player, World world, int x, int y, int z) {
        ItemStack item = getItem(player, x);
        if (item == null || item.getItem() == null) {
            return null;
        }
        IWirelessTermHandler handler = AEApi.instance()
            .registries()
            .wireless()
            .getWirelessTerminalHandler(item);
        if (handler == null) {
            return null;
        }
        // AEBaseContainer.checkItem()/lockPlayerInventorySlot revalidate the terminal item every tick using AE2's own
        // baubles-slot encoding (Platform.baublesSlotsOffset). Our slot value uses Constants.BAUBLE_SLOT_OFFSET, so a
        // bauble slot must be re-encoded to AE2's offset before it becomes the GuiObject's inventorySlot; otherwise
        // AE2 decodes it to a wrong/out-of-range slot, sees no matching item and closes the GUI. Main-inventory slots
        // pass through unchanged.
        int aeSlot = x;
        if (x >= Constants.BAUBLE_SLOT_OFFSET) {
            aeSlot = appeng.util.Platform.baublesSlotsOffset + (x - Constants.BAUBLE_SLOT_OFFSET);
        }
        return new WirelessCraftingTerminalGuiObject(handler, item, player, world, aeSlot, y, z);
    }

    @Nullable
    private static ItemStack getItem(EntityPlayer player, int x) {
        if (x == -1) {
            return player.getCurrentEquippedItem();
        } else if (x >= Constants.BAUBLE_SLOT_OFFSET) {
            net.minecraft.inventory.IInventory baubles = BaublesApi.getBaubles(player);
            int slot = x - Constants.BAUBLE_SLOT_OFFSET;
            if (baubles != null && slot >= 0 && slot < baubles.getSizeInventory()) {
                return baubles.getStackInSlot(slot);
            }
            return null;
        } else if (x >= 0 && x < player.inventory.getSizeInventory()) {
            return player.inventory.getStackInSlot(x);
        }
        return null;
    }

    @Nullable
    @Override
    public Object createServerGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
        WirelessCraftingTerminalGuiObject host = buildHost(player, world, x, y, z);
        if (host == null) {
            return null;
        }
        ContainerCraftingTerm container = new ContainerCraftingTerm(player.inventory, host);
        ContainerOpenContext ctx = new ContainerOpenContext(host);
        ctx.setWorld(world);
        ctx.setX(x);
        ctx.setY(y);
        ctx.setZ(z);
        ctx.setSide(face);
        ((AEBaseContainer) container).setOpenContext(ctx);
        return container;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public Object createClientGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
        WirelessCraftingTerminalGuiObject host = buildHost(player, world, x, y, z);
        if (host == null) {
            if (Minecraft.getMinecraft().currentScreen != null) {
                player.closeScreen();
            }
            return null;
        }
        return new GuiCraftingTerm(player.inventory, host);
    }
}
