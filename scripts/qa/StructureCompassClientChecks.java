package com.xyp.gtnotgood.common.items.compass;

import java.io.DataOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.storage.RegionFileCache;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.xyp.gtnotgood.client.StructureCompassRenderer;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = "structurecompassqa",
    name = "Structure Compass QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class StructureCompassClientChecks {

    private boolean started;
    private int ticks, serverTicks, stage, frames, highlightFrames;
    private volatile boolean verified;
    private ItemStack compass;
    private volatile int guiStep;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModularUIConfig.guiDebugMode = false;
        if (Boolean.getBoolean("gtng.compass.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings != null) mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
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
            search(player, compass);
            require(world.getChunkProvider().getLoadedChunkCount() == loadedBefore, "seed search must not load chunks");
            stage = 1;
        } else if (stage == 1 && StructureCompassItem.data(compass).getBoolean("Found")) {
            NBTTagCompound tag = compass.getTagCompound();
            require(!tag.getBoolean("Confirmed"), "flat-world candidate must stay unconfirmed");
            for (int x = 310; x <= 330; x++) for (int z = 310; z <= 330; z++)
                for (int y = 20; y <= 40; y++) world.setBlock(x, y, z, Blocks.stone, 0, 2);
            Class<?> generatorClass = Class.forName("eu.usrv.legacylootgames.StructureGenerator");
            Object generator = generatorClass.getConstructor().newInstance();
            boolean result = (Boolean) generatorClass
                .getMethod("generatePuzzleMicroDungeon", World.class, int.class, int.class)
                .invoke(generator, world, 320, 320);
            require(result, "actual LootGames generation");
            require(StructureLocations.get(world).nearest(1, 320, 320, 4) != null, "LootGames return hook");
            tag.setInteger("X", 320);
            tag.setInteger("Z", 320);
            compass.getItem().onUpdate(compass, world, player, 0, true);
            stage = 2;
        } else if (stage == 2 && compass.getTagCompound().getBoolean("Confirmed")) {
            checkTravel(player, world);
            checkSavedHive(player, world);
            for (int x = 390; x <= 410; x++)
                for (int z = 390; z <= 410; z++) world.setBlock(x, 4, z, Blocks.stone, 0, 2);
            world.setBlock(400, 5, 400, hive(), 0, 3);
            HiveMarkers.scan(world.getChunkFromBlockCoords(400, 400), StructureLocations.get(world));
            require(StructureLocations.get(world).nearest(0, 400, 400, 2) != null, "loaded hive marker");
            StructureCompassItem.selectMode(player, compass, 0);
            int loadedBefore = world.getChunkProvider().getLoadedChunkCount();
            search(player, compass);
            require(world.getChunkProvider().getLoadedChunkCount() == loadedBefore, "hive search must not load chunks");
            require(compass.getTagCompound().getBoolean("Confirmed"), "hives are confirmed block positions");
            require(compass.getTagCompound().getBoolean("HiveTarget"), "hive target migration marker");
            require(compass.getTagCompound().getInteger("X") == 400, "loaded hive destination");
            world.setBlockToAir(400, 5, 400);
            stage = 3;
        } else if (stage == 3 && !StructureCompassItem.data(compass).getBoolean("Found")) {
            require(StructureLocations.get(world).nearest(0, 400, 400, 2) == null, "destroyed hive leaves index");
            world.setBlock(404, 5, 400, hive(), 0, 3);
            search(player, compass);
            require(compass.getTagCompound().getInteger("X") == 404, "rescan finds replacement hive");
            world.setBlockToAir(404, 5, 400);
            world.setBlock(400, 5, 400, hive(), 0, 3);
            StructureLocations.get(world).readFromNBT(new NBTTagCompound());
            require(StructureLocations.get(world).nearest(1, 320, 320, 256) == null, "unindexed loaded game room");
            StructureCompassItem.selectMode(player, compass, 1);
            stage = 4;
            ItemStack openingStack = compass;
            player.playerNetServerHandler.processPlayerBlockPlacement(
                new C08PacketPlayerBlockPlacement(-1, -1, -1, 255, compass.copy(), 0, 0, 0));
            compass = player.getHeldItem();
            require(compass != openingStack, "normal right-click replaces opening stack reference");
            verified = true;
        } else if (stage == 4 && guiStep == 10 && compass.getTagCompound().getBoolean("Found")) {
            require(compass.getTagCompound().getBoolean("Confirmed"), "GUI discovers unindexed loaded game room");
            require(Math.abs(compass.getTagCompound().getInteger("X") - 320) < 4, "GUI finds game core");
            guiStep = 11;
        } else if (stage == 4 && guiStep == 1 && StructureCompassItem.mode(compass) == 0) {
            require(!compass.getTagCompound().getBoolean("Found"), "GUI mode change clears destination");
            guiStep = 2;
        } else if (stage == 4 && guiStep == 3 && compass.getTagCompound().getBoolean("Found")) {
            require(compass.getTagCompound().getBoolean("Confirmed"), "GUI discovers unindexed loaded hive");
            require(compass.getTagCompound().getInteger("X") == 400, "GUI hive destination");
            player.getEntityData().removeTag("GTNGCompassTravel");
            guiStep = 4;
        } else if (stage == 4 && guiStep == 5 && Math.abs(player.posX - 400) < 9 && Math.abs(player.posZ - 400) < 9) {
            require(!player.isEntityInsideOpaqueBlock(), "GUI safe teleport");
            world.setWorldTime(6000);
            world.getWorldInfo().setRaining(false);
            world.getWorldInfo().setThundering(false);
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            player.playerNetServerHandler.setPlayerLocation(400.5, 5, 390.5, 0, 6);
            guiStep = 6;
        }
    }

    private void checkTravel(EntityPlayerMP player, WorldServer world) {
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
    }

    private static void search(EntityPlayerMP player, ItemStack stack) {
        StructureCompassItem.search(player, stack);
        for (int i = 0; i < 20000 && !StructureCompassItem.data(stack).getBoolean("Found"); i++)
            StructureSearch.INSTANCE.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        require(StructureCompassItem.data(stack).getBoolean("Found"), "bounded search completes");
    }

    private void checkSavedHive(EntityPlayerMP player, WorldServer world) throws Exception {
        require(!world.getChunkProvider().chunkExists(40, 40), "saved hive fixture starts unloaded");
        StructureLocations index = StructureLocations.get(world);
        NBTTagCompound restore = new NBTTagCompound();
        index.writeToNBT(restore);
        index.readFromNBT(new NBTTagCompound());
        NBTTagCompound level = savedHive(Block.getIdFromBlock(hive()), 1), root = new NBTTagCompound();
        level.setInteger("xPos", 40);
        level.setInteger("zPos", 40);
        level.removeTag("TileEntities");
        level.setBoolean("TerrainPopulated", true);
        root.setTag("Level", level);
        File folder = world.getSaveHandler().getWorldDirectory();
        if (world.provider.getSaveFolder() != null) folder = new File(folder, world.provider.getSaveFolder());
        try (DataOutputStream output = RegionFileCache.getChunkOutputStream(folder, 40, 40)) {
            CompressedStreamTools.write(root, output);
        }
        StructureCompassItem.selectMode(player, compass, 0);
        int loadedBefore = world.getChunkProvider().getLoadedChunkCount();
        search(player, compass);
        require(compass.getTagCompound().getInteger("X") == 643 && compass.getTagCompound().getInteger("Z") == 649,
            "unindexed saved Anvil hive search");
        require(!world.getChunkProvider().chunkExists(40, 40), "saved hive remains unloaded");
        require(world.getChunkProvider().getLoadedChunkCount() == loadedBefore, "saved hive search loads no terrain");
        index.readFromNBT(restore);
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
        master.setString("id", "LOOTGAMES_MASTER_TE");
        master.setInteger("x", -50);
        master.setInteger("y", 30);
        master.setInteger("z", 400);
        NBTTagList tiles = new NBTTagList();
        tiles.appendTag(master);
        level.setTag("TileEntities", tiles);
        StructureMarkers.scan(level, index);
        require(index.nearest(1, -50, 400, 1) != null, "old-map LootGames marker");
        require(index.nearest(0, -50, 400, 100) == null, "mode isolation");
        require(HiveMarkers.isHive(hive()), "Forestry hive identity");
        require(!HiveMarkers.isHive(Blocks.brick_block), "red bricks are no longer hive targets");
        for (ModList mod : new ModList[] { ModList.MagicBees, ModList.ExtraBees }) {
            Block wildHive = GameRegistry.findBlock(mod.getID(), "hive");
            if (wildHive != null) require(HiveMarkers.isHive(wildHive), mod.getID() + " hive identity");
        }
        int hiveId = Block.getIdFromBlock(hive());
        for (int format = 0; format < 3; format++) {
            if (format == 0 && hiveId > 4095) continue;
            StructureLocations hives = new StructureLocations("hives");
            NBTTagCompound saved = savedHive(hiveId, format);
            saved.removeTag("TileEntities");
            HiveMarkers.scan(saved, hives);
            var target = hives.nearest(0, -61, -103, 1);
            require(target != null && target.x == -61 && target.y == 69 && target.z == -103,
                "saved hive format " + format + " with negative chunk coordinates");
        }
        NBTTagCompound aliased = savedHive(hiveId | 0x10000, 1);
        StructureLocations falseHive = new StructureLocations("falseHive");
        HiveMarkers.scan(aliased, falseHive);
        require(falseHive.nearest(0, -61, -103, 1) == null, "EndlessIDs high bits must not alias a hive");
        index.add(0, -61, 69, -103);
        NBTTagCompound save = new NBTTagCompound();
        index.writeToNBT(save);
        StructureLocations restored = new StructureLocations("qa");
        restored.readFromNBT(save);
        require(restored.nearest(0, -61, -103, 1) != null, "hive NBT persistence");
        require(restored.nearest(1, -50, 400, 1) != null, "game NBT persistence");
        require(restored.nearest(0, 0, 0, 10) == null, "range bound");
        save.removeTag("HiveTargets");
        restored.readFromNBT(save);
        require(restored.nearest(0, -61, -103, 1) == null, "legacy red-house index discarded");
        require(restored.nearest(1, -50, 400, 1) != null, "legacy game index preserved");
        ItemStack legacy = GTNGItemList.StructureCompass.get(1);
        NBTTagCompound old = new NBTTagCompound();
        old.setBoolean("Found", true);
        old.setBoolean("Confirmed", true);
        legacy.setTagCompound(old);
        require(!StructureCompassItem.data(legacy).getBoolean("Found"), "legacy red-house destination discarded");
    }

    private static NBTTagCompound savedHive(int id, int format) {
        NBTTagCompound level = new NBTTagCompound(), section = new NBTTagCompound(), swarm = new NBTTagCompound();
        level.setInteger("xPos", -4);
        level.setInteger("zPos", -7);
        swarm.setString("id", "forestry.Swarm");
        swarm.setInteger("x", -61);
        swarm.setInteger("y", 69);
        swarm.setInteger("z", -103);
        NBTTagList tiles = new NBTTagList();
        tiles.appendTag(swarm);
        level.setTag("TileEntities", tiles);
        section.setByte("Y", (byte) 4);
        int slot = (5 << 8) | (9 << 4) | 3;
        if (format == 2) {
            byte[] ids = new byte[8192];
            ids[slot * 2] = (byte) (id >> 8);
            ids[slot * 2 + 1] = (byte) id;
            section.setByteArray("Blocks16", ids);
        } else {
            byte[] ids = new byte[4096], add = new byte[2048];
            ids[slot] = (byte) id;
            add[slot >> 1] = (byte) (((id >> 8) & 15) << ((slot & 1) * 4));
            section.setByteArray("Blocks", ids);
            section.setByteArray("Add", add);
            if (format == 1) {
                byte[] high = new byte[2048], highest = new byte[4096];
                high[slot >> 1] = (byte) (((id >> 12) & 15) << ((slot & 1) * 4));
                highest[slot] = (byte) (id >> 16);
                section.setByteArray("BlocksB2Hi", high);
                section.setByteArray("BlocksB3", highest);
            }
        }
        NBTTagList sections = new NBTTagList();
        sections.appendTag(section);
        level.setTag("Sections", sections);
        return level;
    }

    private static Block hive() {
        Block hive = GameRegistry.findBlock(ModList.Forestry.getID(), "beehives");
        require(hive != null, "registered Forestry hive");
        return hive;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        if (!verified || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        File output = new File(System.getProperty("gtng.compass.qa.output"));
        output.mkdirs();
        if (guiStep == 6 && mc.currentScreen == null) {
            if (highlightFrames == 0) mc.ingameGUI.getChatGUI().clearChatMessages();
            if (++highlightFrames < 180) return;
            checkUnloadedTarget(mc);
            ScreenShotHelper.saveScreenshot(output, "compass-world-hive.png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            Files.write(new File(output, "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
            System.out.println(
                "STRUCTURE_COMPASS_QA_PASS hive scan/removal, saved IDs, migration, LootGames, no terrain load, GUI mode/search/teleport, world highlight");
            mc.shutdown();
            return;
        }
        if (++frames % 60 != 0 || !(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
        ModularScreen screen = wrapper.getScreen();
        if (guiStep == 0) {
            guiStep = 10;
            click(screen, 60, 145);
        } else if (guiStep == 11) {
            ScreenShotHelper.saveScreenshot(output, "compass-gui-lootgames.png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            guiStep = 1;
            click(screen, 70, 45);
        } else if (guiStep == 2) {
            guiStep = 3;
            click(screen, 60, 145);
        } else if (guiStep == 4) {
            ScreenShotHelper.saveScreenshot(output, "compass-gui-hive.png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            guiStep = 5;
            click(screen, 170, 174);
        }
    }

    private static void click(ModularScreen screen, int x, int y) {
        var area = screen.getMainPanel().getArea();
        screen.getContext().updateState(area.x + x, area.y + y, 0);
        screen.onFrameUpdate();
        screen.onMousePressed(0);
        screen.onMouseRelease(0);
    }

    private static void checkUnloadedTarget(Minecraft mc) throws Exception {
        NBTTagCompound target = new NBTTagCompound();
        target.setBoolean("HiveTarget", true);
        target.setInteger("X", 160003);
        target.setInteger("Y", 69);
        target.setInteger("Z", 160009);
        require(mc.theWorld.getChunkFromChunkCoords(10000, 10000) instanceof EmptyChunk, "remote client chunk absent");
        var predicate = StructureCompassRenderer.class.getDeclaredMethod("isHiveTarget", Minecraft.class,
            NBTTagCompound.class);
        predicate.setAccessible(true);
        require((Boolean) predicate.invoke(null, mc, target), "unloaded hive keeps pointer and world marker");
        target.setInteger("X", 390);
        target.setInteger("Y", 6);
        target.setInteger("Z", 390);
        require(!(mc.theWorld.getChunkFromChunkCoords(24, 24) instanceof EmptyChunk), "near client chunk loaded");
        require(!(Boolean) predicate.invoke(null, mc, target), "loaded empty target suppresses marker");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("COMPASS_QA: " + message);
    }
}
