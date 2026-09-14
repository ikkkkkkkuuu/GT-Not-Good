package com.xyp.gtnotgood.common.user;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.xyp.gtnotgood.loader.BlockLoader;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in client integration checks loaded only from the test classpath. Uses a disposable flat world. */
@Mod(
    modid = "mechanicaluserqa",
    name = "Mechanical User QA",
    version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public class MechanicalUserClientChecks {

    private boolean started;
    private boolean checked;
    private int frames;
    private int guiDelay;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("mechanicaluser.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END) return;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "mechanical-user-qa-" + System.currentTimeMillis(),
                "Mechanical User QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (checked) {
            if (++guiDelay == 60) {
                net.minecraft.entity.player.EntityPlayerMP viewer = (net.minecraft.entity.player.EntityPlayerMP) FMLCommonHandler
                    .instance()
                    .getMinecraftServerInstance()
                    .getConfigurationManager().playerEntityList.get(0);
                com.cleanroommc.modularui.factory.TileEntityGuiFactory.INSTANCE.open(viewer, 0, 6, 0);
            }
            return;
        }
        net.minecraft.server.MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        checked = true;
        try {
            WorldServer world = server.worldServerForDimension(0);
            int x = 0, y = 6, z = 0;
            world.setBlock(x, y, z, BlockLoader.mechanicalUser, 5, 3);
            world.setBlockToAir(1, y, z);
            TileMechanicalUser tile = (TileMechanicalUser) world.getTileEntity(x, y, z);
            tile.mode = 1;
            tile.setInventorySlotContents(0, new ItemStack(Blocks.cobblestone, 2));
            tile.updateEntity();
            require(world.getBlock(1, y, z) == Blocks.cobblestone, "place block");
            require(tile.getStackInSlot(0).stackSize == 1, "placement consumes exactly one");
            world.setBlock(1, y, z, Blocks.dirt);
            world.setBlockToAir(1, y + 1, z);
            tile.mode = 2;
            tile.setInventorySlotContents(0, new ItemStack(Items.iron_hoe));
            tick(tile);
            require(world.getBlock(1, y, z) == Blocks.farmland, "hoe use on block");
            require(
                tile.getStackInSlot(0)
                    .getItemDamage() == 1,
                "hoe durability");
            world.setBlockToAir(1, y, z);
            EntitySheep sheep = new EntitySheep(world);
            sheep.setPosition(1.5, y, 0.5);
            world.spawnEntityInWorld(sheep);
            tile.mode = 5;
            tile.setInventorySlotContents(0, new ItemStack(Items.shears));
            tick(tile);
            require(sheep.getSheared(), "shear sheep");
            sheep.setDead();
            EntitySheep victim = new EntitySheep(world);
            victim.setPosition(1.5, y, 0.5);
            world.spawnEntityInWorld(victim);
            tile.leftClick = true;
            tile.setInventorySlotContents(0, new ItemStack(Items.diamond_sword));
            tick(tile);
            require(victim.getHealth() <= 1, "sword attack attributes");
            tile.leftClick = false;
            tile.mode = 0;
            net.minecraft.entity.player.EntityPlayerMP viewer = (net.minecraft.entity.player.EntityPlayerMP) server
                .getConfigurationManager().playerEntityList.get(0);
            viewer.playerNetServerHandler.setPlayerLocation(4, 8, -4, 38, 18);

            viewer.capabilities.isFlying = true;
            viewer.sendPlayerAbilities();
            System.out.println("MECHANICAL_USER_QA: interactions passed");
        } catch (Throwable error) {
            error.printStackTrace();
            System.out.println("MECHANICAL_USER_QA: FAILED");
        }
    }

    private static void tick(TileMechanicalUser tile) {
        for (int i = 0; i < 20; i++) tile.updateEntity();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || !checked || mc.theWorld == null || guiDelay < 65) return;
        if (++frames == 180) {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "mechanical-user-qa.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            System.out.println("MECHANICAL_USER_QA: screenshot saved");
        }
        if (frames == 200) mc.thePlayer.closeScreen();
        if (frames == 260) ScreenShotHelper.saveScreenshot(
            new File("."),
            "mechanical-user-world.png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
        if (frames == 300) mc.shutdown();
    }
}
