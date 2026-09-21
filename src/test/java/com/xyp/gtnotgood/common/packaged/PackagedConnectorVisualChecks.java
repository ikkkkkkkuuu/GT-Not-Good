package com.xyp.gtnotgood.common.packaged;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.ScreenShotHelper;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in disposable playground checks for actual chunk-watcher sync and held-connector screenshots. */
@Mod(
    modid = "packagedconnectorchecks",
    name = "Packaged Connector Visual Checks",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class PackagedConnectorVisualChecks {

    private int ticks;
    private volatile int stage;
    private int captured;
    private int frames;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.packaged.connector.qa")) {
            if (!System.getProperty("gtng.packaged.playground.resume", "")
                .isEmpty()) {
                throw new IllegalStateException("Connector QA requires a fresh disposable playground");
            }
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var world = player.getServerForPlayer();
        if (++ticks < 220 || ticks > 620 || ticks % 80 != 60) return;
        TilePackagedProvider provider = (TilePackagedProvider) world.getTileEntity(0, 8, 0);
        int step = (ticks - 220) / 80 + 1;
        player.inventory.currentItem = 0;
        if (step == 1) {
            player.inventory.setInventorySlotContents(0, GTNGItemList.ItemWirelessConnector.get(1));
            var packet = (S35PacketUpdateTileEntity) provider.getDescriptionPacket();
            if (packet.func_148857_g()
                .func_150296_c()
                .size() != 1) throw new AssertionError("Snapshot contains private state");
        }
        if (step == 2) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("SelectedProvider", new PackagedTarget(world.provider.dimensionId, 0, 8, 0, 1).write());
            player.getHeldItem()
                .setTagCompound(tag);
        }
        if (step == 4) player.inventory.currentItem = 1;
        player.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(player.inventory.currentItem));
        if (step == 5 && !provider.removeTarget(0)) throw new AssertionError("Unbind failed");
        if (step == 6 && !provider.bind(new PackagedTarget(world.provider.dimensionId, 6, 10, 0, 1))) {
            throw new AssertionError("Rebind failed");
        }
        double x = step == 3 ? -1.5 : 3, y = step == 3 ? 10 : 11, z = step == 3 ? -3 : -7;
        double dx = (step == 3 ? -1.5 : 3.5) - x;
        double dy = (step == 3 ? 8.5 : 9.5) - (y + player.getEyeHeight());
        double dz = .5 - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
        player.inventoryContainer.detectAndSendChanges();
        stage = step;
    }

    @SubscribeEvent
    public void client(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || stage == 0 || captured == stage || mc.theWorld == null) return;
        if (++frames < 35) return;
        frames = 0;
        TilePackagedProvider provider = (TilePackagedProvider) mc.theWorld.getTileEntity(0, 8, 0);
        int expected = stage == 5 ? 0 : 1;
        if (provider == null || provider.connections()
            .size() != expected) throw new AssertionError("Client binding snapshot did not update");
        boolean connector = mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem()
            .getItem() instanceof ItemWirelessConnector;
        if (connector == (stage == 4)) throw new AssertionError("Held item did not synchronize");
        ScreenShotHelper.saveScreenshot(
            mc.mcDataDir,
            "packaged-connector-" + stage + ".png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
        captured = stage;
        System.out.println("PACKAGED_CONNECTOR_VISUAL_STAGE=" + stage + "; bindings=" + expected);
        if (stage == 6) {
            System.out.println("PACKAGED_CONNECTOR_SYNC_PASS");
            mc.shutdown();
        }
    }
}
