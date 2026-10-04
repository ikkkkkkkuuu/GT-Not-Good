package com.xyp.gtnotgood.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.gui.modularui.util.PatternSlot;

@Mod(
    modid = "compacthatchqa",
    name = "Compact hatch QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class CompactHatchChecks {

    private boolean started, finished;
    private volatile boolean opened;
    private int ticks, frames;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "compact-hatch-" + System.currentTimeMillis(),
                "Compact hatch QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            if (++ticks > 1200) throw new AssertionError("GUI timeout");
            if (ticks == 120) {
                EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
                BaseMetaTileEntity tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(0, 10, 0);
                ((SuperMTEHatchCraftingInputME) tile.getMetaTileEntity()).onRightclick(tile, player);
                opened = true;
            }
            if (ticks != 60) return;
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            var world = player.worldObj;
            world.setBlock(0, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
            BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(0, 10, 0);
            tile.setInitialValuesAsNBT(
                null,
                (short) GTNGItemList.CompactSuperMTEHatchCraftingInputME.get(1)
                    .getItemDamage());
            tile.setOwnerName(player.getCommandSenderName());
            tile.setOwnerUuid(player.getUniqueID());
            var hatch = (SuperMTEHatchCraftingInputME) tile.getMetaTileEntity();
            require(hatch.getPatternCount() == 9 && hatch.rows() == 1, "single pattern row");
            require(
                hatch.getSizeInventory() == 20 && hatch.getCircuitSlot() == 9 && hatch.getMoldSlot() == 19,
                "auxiliary slot boundaries");
            require(hatch.supportsFluids(), "fluid support");
            hatch.setInventorySlotContents(hatch.getManualSlotStart(), new ItemStack(Items.diamond));
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeToNBT(tag);
            world.removeTileEntity(0, 10, 0);
            tile = new BaseMetaTileEntity();
            tile.setWorldObj(world);
            tile.readFromNBT(tag);
            world.setTileEntity(0, 10, 0, tile);
            hatch = (SuperMTEHatchCraftingInputME) tile.getMetaTileEntity();
            require(
                hatch.getPatternCount() == 9 && hatch.getSharedItems()[0].getItem() == Items.diamond,
                "save and reload");
            var original = (SuperMTEHatchCraftingInputME) GregTechAPI.METATILEENTITIES[GTNGItemList.SuperMTEHatchCraftingInputME
                .get(1)
                .getItemDamage()];
            require(original.getPatternCount() == 900 && original.getMoldSlot() == 910, "original inventory unchanged");
            player.playerNetServerHandler.setPlayerLocation(0, 10, -2, 0, 0);
            player.worldObj.markBlockForUpdate(0, 10, 0);
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !opened || finished || ++frames < 40) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainerWrapper)) return;
            var panel = ((GuiContainerWrapper) mc.currentScreen).getScreen()
                .getMainPanel();
            require(count(panel) == 9, "exactly nine pattern widgets");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "compact-hatch.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            Files.write(new File("result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
            finished = true;
            mc.shutdown();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static int count(IWidget widget) {
        int result = widget instanceof PatternSlot ? 1 : 0;
        for (IWidget child : widget.getChildren()) result += count(child);
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("COMPACT_HATCH_QA: " + message);
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        finished = true;
        Minecraft.getMinecraft()
            .shutdown();
    }
}
