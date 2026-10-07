package com.xyp.gtnotgood.common.parts.mestock;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;

import com.xyp.gtnotgood.client.mestock.StockModelRenderer;

import appeng.api.parts.IPartRenderHelper;
import appeng.parts.reporting.PartTerminal;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Native cable terminal geometry; paged requester access is authorized against the terminal's live network. */
public final class PartRequesterTerminal extends PartTerminal {

    public PartRequesterTerminal(ItemStack stack) {
        super(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventory(IPartRenderHelper helper, RenderBlocks renderer) {
        StockModelRenderer.inventory("merequester:item/requester_terminal");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(int x, int y, int z, IPartRenderHelper helper, RenderBlocks renderer) {
        StockModelRenderer.part("ae2:part/display_base", x, y, z, helper, renderer, getColor());
        StockModelRenderer.part(
            isPowered() ? "merequester:part/requester_terminal_on" : "merequester:part/requester_terminal_off",
            x,
            y,
            z,
            helper,
            renderer,
            getColor());
        StockModelRenderer.part(
            StockModelRenderer.indicator("display_status", getClientFlags()),
            x,
            y,
            z,
            helper,
            renderer,
            getColor());
    }

    @Override
    public boolean onPartActivate(EntityPlayer player, Vec3 pos) {
        if (player.isSneaking()) return false;
        if (!player.worldObj.isRemote) StockGuiFactory.instance.open(player, this);
        return true;
    }
}
