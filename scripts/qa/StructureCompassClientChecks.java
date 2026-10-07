package com.xyp.gtnotgood.common.items.compass;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Exercises live seed prediction, both generation hooks, persistence, switching, safe travel and rendering. */
@Mod(modid = "structurecompassqa", name = "Structure Compass QA", version = "1", dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class StructureCompassClientChecks {

    private boolean started;
    private int ticks, serverTicks, stage, frames;
    private volatile boolean verified;
    private ItemStack compass;
    private volatile int guiStep;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
        if (Boolean.getBoolean("gtng.compass.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("compass-qa-" + System.currentTimeMillis(), "Compass QA",
                new WorldSettings(83171L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (++ticks > 6000) throw new AssertionError("Compass QA timed out");
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        WorldServer world = (WorldServer) player.worldObj;
        if (++serverTicks < 140) return;
        if (stage == 0) {
            checkData();
            compass = GTNGItemList.StructureCompass.get(1);
            player.inventory.mainInventory[0] = compass;
            player.inventory.currentItem = 0;
            player.setSneaking(true);
            compass.getItem().onItemRightClick(compass, world, player);
            player.setSneaking(false);
            require(StructureCompassItem.mode(compass) == 1, "mode switch");
            int loadedBefore = world.getChunkProvider().getLoadedChunkCount();
            StructureCompassItem.search(player, compass);
            for (int i = 0; i < 1000 && !StructureCompassItem.data(compass).getBoolean("Found"); i++)
                StructureSearch.INSTANCE.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            require(StructureCompassItem.data(compass).getBoolean("Found"), "bounded candidate search completes");
            require(world.getChunkProvider().getLoadedChunkCount() == loadedBefore, "seed search must not load any chunks");
            stage = 1;
        } else if (stage == 1 && StructureCompassItem.data(compass).getBoolean("Found")) {
            NBTTagCompound tag = compass.getTagCompound();
            require(!tag.getBoolean("Confirmed"), "flat-world candidate must stay unconfirmed");
            // Build thick ground for the real LootGames generator; default superflat is otherwise too shallow.
            for (int x = 310; x <= 330; x++) for (int z = 310; z <= 330; z++)
                for (int y = 20; y <= 40; y++) world.setBlock(x, y, z, Blocks.stone, 0, 2);
            Class<?> generatorClass = Class.forName("eu.usrv.legacylootgames.StructureGenerator");
            Object generator = generatorClass.getConstructor().newInstance();
            boolean result = (Boolean) generatorClass.getMethod("generatePuzzleMicroDungeon", net.minecraft.world.World.class, int.class, int.class).invoke(generator, world, 320, 320);
            require(result, "actual LootGames generation");
            require(StructureLocations.get(world).nearest(1, 320, 320, 4) != null, "LootGames return hook");
            tag.setInteger("X", 320); tag.setInteger("Z", 320);
            compass.getItem().onUpdate(compass, world, player, 0, true);
            // onUpdate only resolves once a second; stage 2 waits for that normal lifecycle.
            stage = 2;
        } else if (stage == 2 && compass.getTagCompound().getBoolean("Confirmed")) {
            Class<?> editorClass = Class.forName("greymerk.roguelike.worldgen.WorldEditor");
            Object editor = editorClass.getConstructor(net.minecraft.world.World.class).newInstance(world);
            Class<?> coordClass = Class.forName("greymerk.roguelike.worldgen.Coord");
            Object coord = coordClass.getConstructor(int.class, int.class, int.class).newInstance(400, 50, 400);
            Object house = Class.forName("greymerk.roguelike.dungeon.towers.HouseTower").getConstructor().newInstance();
            Object theme = Class.forName("greymerk.roguelike.theme.ThemeHouse").getConstructor().newInstance();
            house.getClass().getMethod("generate", Class.forName("greymerk.roguelike.worldgen.IWorldEditor"), java.util.Random.class,
                Class.forName("greymerk.roguelike.theme.ITheme"), coordClass).invoke(house, editor, new java.util.Random(1), theme, coord);
            require(StructureLocations.get(world).nearest(0, 400, 400, 4) != null, "real red-house generation hook");
            world.setBlock(10, 5, 10, Blocks.lava);
            require(!CompassTravel.safe(world, 10, 6, 10), "reject lava landing");
            world.setBlock(10, 5, 10, Blocks.stone);
            world.setBlock(10, 6, 10, Blocks.stone);
            require(!CompassTravel.safe(world, 10, 6, 10), "reject suffocation");
            world.setBlockToAir(10, 6, 10);
            require(CompassTravel.safe(world, 10, 6, 10), "accept safe landing");
            CompassTravel.teleport(player, compass);
            require(Math.abs(player.posX - 320) < 9 && Math.abs(player.posZ - 320) < 9, "explicit teleport destination");
            require(!player.isEntityInsideOpaqueBlock(), "safe teleport body");
            // Simulate an already loaded old game room with no discovery record. Keep the house fixture.
            StructureLocations index = StructureLocations.get(world);
            var houseLocation = index.nearest(0, 400, 400, 4);
            index.readFromNBT(new NBTTagCompound());
            index.add(0, houseLocation.x, houseLocation.y, houseLocation.z);
            require(index.nearest(1, 320, 320, 256) == null, "old room begins without an index entry");
            StructureCompassItem.selectMode(player, compass, 1);
            stage = 3;
            // Exercise the actual normal-right-click handler, including vanilla's post-open stack copy.
            ItemStack openingStack = compass;
            player.playerNetServerHandler.processPlayerBlockPlacement(
                new C08PacketPlayerBlockPlacement(-1, -1, -1, 255, compass.copy(), 0, 0, 0));
            compass = player.getHeldItem();
            require(compass != openingStack, "normal right-click replaces the opening stack reference");
            verified = true;
        } else if (stage == 3 && guiStep == 10 && compass.getTagCompound().getBoolean("Found")) {
            require(compass.getTagCompound().getBoolean("Confirmed"), "GUI discovers an already loaded unindexed game room");
            require(Math.abs(compass.getTagCompound().getInteger("X") - 320) < 4, "GUI finds visible game core");
            guiStep = 11;
        } else if (stage == 3 && guiStep == 1 && StructureCompassItem.mode(compass) == 0) {
            require(!compass.getTagCompound().getBoolean("Found"), "GUI target change clears old destination");
            guiStep = 2;
        } else if (stage == 3 && guiStep == 3 && compass.getTagCompound().getBoolean("Found")) {
            require(compass.getTagCompound().getBoolean("Confirmed"), "GUI search resolves recorded house");
            require(compass.getTagCompound().getInteger("X") == 400, "GUI correct mode destination");
            // Clear the existing fixture teleport's cooldown before testing the GUI teleport action.
            player.getEntityData().removeTag("GTNGCompassTravel");
            guiStep = 4;
        } else if (stage == 3 && guiStep == 5 && Math.abs(player.posX - 400) < 9 && Math.abs(player.posZ - 400) < 9) {
            require(!player.isEntityInsideOpaqueBlock(), "GUI safe teleport");
            guiStep = 6;
        }
    }

    private static void checkData() {
        StructureSearch.Spiral spiral = new StructureSearch.Spiral();
        Set<String> positions = new HashSet<>();
        for (int i = 0; i < 81; i++) {
            require(Math.abs(spiral.x) <= 4 && Math.abs(spiral.z) <= 4, "spiral extent");
            require(positions.add(spiral.x + "," + spiral.z), "spiral duplicate");
            spiral.advance();
        }
        StructureLocations index = new StructureLocations("qa");
        NBTTagCompound level = new NBTTagCompound(), master = new NBTTagCompound();
        master.setString("id", "LOOTGAMES_MASTER_TE"); master.setInteger("x", -50); master.setInteger("y", 30); master.setInteger("z", 400);
        NBTTagList tiles = new NBTTagList(); tiles.appendTag(master); level.setTag("TileEntities", tiles);
        StructureMarkers.scan(level, index, 45);
        require(index.nearest(1, -50, 400, 1) != null, "old-map LootGames marker");
        require(index.nearest(0, -50, 400, 100) == null, "mode isolation");
        NBTTagCompound save = new NBTTagCompound(); index.writeToNBT(save);
        StructureLocations restored = new StructureLocations("qa"); restored.readFromNBT(save);
        require(restored.nearest(1, -50, 400, 1) != null, "NBT persistence");
        require(restored.nearest(1, 0, 0, 10) == null, "range bound");
        NBTTagCompound chest = new NBTTagCompound(), book = new NBTTagCompound(), bookTag = new NBTTagCompound();
        chest.setString("id", "Chest"); chest.setInteger("x", 100); chest.setInteger("y", 65); chest.setInteger("z", 200);
        bookTag.setString("author", "Greymerk"); bookTag.setString("title", "Statistics"); book.setTag("tag", bookTag);
        NBTTagList items = new NBTTagList(); items.appendTag(book); chest.setTag("Items", items);
        NBTTagList houseTiles = new NBTTagList(); houseTiles.appendTag(chest); level.setTag("TileEntities", houseTiles);
        NBTTagCompound section = new NBTTagCompound(); section.setByte("Y", (byte) 4);
        byte[] brickIds = new byte[4096]; java.util.Arrays.fill(brickIds, 0, 16, (byte) 45);
        section.setByteArray("Blocks", brickIds);
        NBTTagList sections = new NBTTagList(); sections.appendTag(section); level.setTag("Sections", sections);
        StructureMarkers.scan(level, index, 45);
        require(index.nearest(0, 100, 200, 1) != null, "old red-house marker");
        byte[] extended = new byte[2048]; java.util.Arrays.fill(extended, (byte) 0x11); section.setByteArray("BlocksB2Hi", extended);
        StructureLocations falseHouse = new StructureLocations("falseHouse"); StructureMarkers.scan(level, falseHouse, 45);
        require(falseHouse.nearest(0, 100, 200, 1) == null, "EndlessIDs high bits must not alias vanilla bricks");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        if (!verified || event.phase != TickEvent.Phase.END || ++frames % 60 != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        File output = new File(System.getProperty("gtng.compass.qa.output")); output.mkdirs();
        if (guiStep == 6) {
        Files.write(new File(output, "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
        System.out.println("STRUCTURE_COMPASS_QA_PASS seed-no-load, generation hooks, markers, persistence, safe travel, real-right-click stack copy, unindexed loaded game room, GUI mode/search/teleport");
        mc.shutdown();
        return;
        }
        if (!(mc.currentScreen instanceof com.cleanroommc.modularui.screen.GuiContainerWrapper wrapper)) return;
        var screen = wrapper.getScreen();
        if (guiStep == 0) {
            guiStep = 10;
            click(screen, 60, 145);
        } else if (guiStep == 11) {
            ScreenShotHelper.saveScreenshot(output, "compass-gui-lootgames.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            guiStep = 1;
            click(screen, 70, 45);
        } else if (guiStep == 2) {
            guiStep = 3;
            click(screen, 60, 145);
        } else if (guiStep == 4) {
            ScreenShotHelper.saveScreenshot(output, "compass-gui-house.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            guiStep = 5;
            click(screen, 170, 174);
        }
    }

    private static void click(com.cleanroommc.modularui.screen.ModularScreen screen, int x, int y) {
        var area = screen.getMainPanel().getArea();
        screen.getContext().updateState(area.x + x, area.y + y, 0);
        screen.onFrameUpdate();
        screen.onMousePressed(0);
        screen.onMouseRelease(0);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("COMPASS_QA: " + message);
    }
}
