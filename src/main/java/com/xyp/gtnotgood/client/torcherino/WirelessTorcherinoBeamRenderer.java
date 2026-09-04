package com.xyp.gtnotgood.client.torcherino;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.common.torcherino.tile.TileWirelessTorcherinoBase;
import com.xyp.gtnotgood.common.torcherino.util.BoundMachineEntry;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws blue guide beams from the wireless Torcherino the player is looking at to all currently bound machines.
 */
public class WirelessTorcherinoBeamRenderer {

    private static final Minecraft MC = Minecraft.getMinecraft();

    /**
     * Renders wireless binding beams during the final world render pass.
     *
     * @param event render event containing partial-tick interpolation data
     */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (MC.theWorld == null || MC.thePlayer == null) return;

        MovingObjectPosition hit = MC.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        TileEntity tile = MC.theWorld.getTileEntity(hit.blockX, hit.blockY, hit.blockZ);
        if (!(tile instanceof TileWirelessTorcherinoBase)) return;
        TileWirelessTorcherinoBase torch = (TileWirelessTorcherinoBase) tile;

        List<BoundMachineEntry> bound = torch.getBoundMachines();
        if (bound.isEmpty()) return;

        EntityPlayerSP player = MC.thePlayer;
        double viewX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double viewY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double viewZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(-viewX, -viewY, -viewZ);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(3.0F);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);

        double torchX = torch.xCoord + 0.5D;
        double torchY = torch.yCoord + 0.5D;
        double torchZ = torch.zCoord + 0.5D;

        for (BoundMachineEntry entry : bound) {
            if (entry.dim != MC.theWorld.provider.dimensionId) continue;

            double dx = entry.x + 0.5D - torchX;
            double dy = entry.y + 0.5D - torchY;
            double dz = entry.z + 0.5D - torchZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float alpha = (float) Math.max(0.3D, 1.0D - distance / 32.0D);

            tessellator.setColorRGBA_F(0.2F, 0.6F, 1.0F, alpha);
            tessellator.addVertex(torchX, torchY, torchZ);
            tessellator.addVertex(entry.x + 0.5D, entry.y + 0.5D, entry.z + 0.5D);
        }

        tessellator.draw();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
