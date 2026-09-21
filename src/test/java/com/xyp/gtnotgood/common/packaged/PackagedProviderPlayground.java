package com.xyp.gtnotgood.common.packaged;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.IBlockDefinition;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJarFillable;

/** Opt-in, test-classpath-only builder of a fresh manual playground; never closes the client or starts a craft. */
@Mod(
    modid = "packagedplayground",
    name = "Packaged Provider Playground",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class PackagedProviderPlayground {

    private boolean launched;
    private volatile boolean ready;
    private boolean stopped;
    private int ticks;
    private int frames;
    private TilePackagedProvider provider;
    private InfusionRecipe recipe;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.packaged.playground")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!launched && mc.theWorld == null && mc.currentScreen != null) {
            launched = true;
            mc.gameSettings.guiScale = 2;
            mc.gameSettings.pauseOnLostFocus = false;
            com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
            String resume = System.getProperty("gtng.packaged.playground.resume", "");
            if (!resume.isEmpty() && !resume.matches("packaged-manual-[0-9]+"))
                throw new IllegalArgumentException("Not a playground save");
            String save = resume.isEmpty() ? "packaged-manual-" + System.currentTimeMillis() : resume;
            stopped = !resume.isEmpty();
            mc.launchIntegratedServer(
                save,
                "GTNG Provider - TC4 Manual Test",
                resume.isEmpty() ? new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT)
                    : null);
            try {
                if (!Boolean.getBoolean("gtng.packaged.connector.qa")) {
                    Files.write(
                        new File("packaged-playground-save.txt").toPath(),
                        save.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }
        if (ready && mc.theWorld != null && ++frames == 100) {
            ScreenShotHelper.saveScreenshot(
                mc.mcDataDir,
                "packaged-playground.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || stopped) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var world = player.getServerForPlayer();
        try {
            if (++ticks == 1) {
                recipe = (InfusionRecipe) ConfigResearch.recipes.get("WandRodQuartz");
                if (recipe == null) throw new IllegalStateException("Missing real TC quartz rod recipe");
                world.getGameRules()
                    .setOrCreateGameRule("doDaylightCycle", "false");
                world.getGameRules()
                    .setOrCreateGameRule("doMobSpawning", "false");
                world.setWorldTime(6000);
                for (int x = -8; x <= 14; x++) for (int z = -8; z <= 8; z++) {
                    world.setBlock(x, 7, z, (x + z) % 2 == 0 ? Blocks.stonebrick : Blocks.quartz_block);
                }
                world.setBlock(
                    0,
                    8,
                    0,
                    net.minecraft.block.Block.getBlockFromItem(GTNGItemList.WirelessPackagedPatternProvider.getItem()));
                provider = (TilePackagedProvider) world.getTileEntity(0, 8, 0);
                provider.setOwnerName(player.getCommandSenderName());
                provider
                    .setInventorySlotContents(TilePackagedProvider.CORE, GTNGItemList.ThaumcraftInfusionCore.get(1));
                provider.autoReturn = true;
                var blocks = AEApi.instance()
                    .definitions()
                    .blocks();
                place(world, -1, 0, blocks.controller());
                place(world, -1, 1, blocks.energyCellCreative());
                place(world, -2, 0, blocks.drive());
                ((TileDrive) world.getTileEntity(-2, 8, 0)).getInternalInventory()
                    .setInventorySlotContents(
                        0,
                        AEApi.instance()
                            .definitions()
                            .items()
                            .cell64k()
                            .maybeStack(1)
                            .get());
                place(world, -2, 1, blocks.craftingStorage64k());
                place(world, -3, 1, blocks.craftingAccelerator());
                terminal(
                    world,
                    player,
                    -3,
                    AEApi.instance()
                        .definitions()
                        .parts()
                        .craftingTerminal()
                        .maybeStack(1)
                        .get());
                terminal(
                    world,
                    player,
                    -4,
                    AEApi.instance()
                        .definitions()
                        .parts()
                        .patternTerminal()
                        .maybeStack(1)
                        .get());
                terminal(
                    world,
                    player,
                    -5,
                    AEApi.instance()
                        .definitions()
                        .parts()
                        .interfaceTerminal()
                        .maybeStack(1)
                        .get());
                world.setBlock(6, 10, 0, ConfigBlocks.blockStoneDevice, 2, 3);
                world.setBlock(6, 8, 0, ConfigBlocks.blockStoneDevice, 1, 3);
                for (int x : new int[] { -1, 1 }) for (int z : new int[] { -1, 1 }) {
                    world.setBlock(6 + x, 8, z, ConfigBlocks.blockStoneDevice, 3, 3);
                }
                for (int[] offset : new int[][] { { -3, 0 }, { 3, 0 }, { 0, -3 }, { 0, 3 }, { -3, -3 }, { 3, 3 },
                    { -3, 3 }, { 3, -3 } }) {
                    world.setBlock(6 + offset[0], 8, offset[1], ConfigBlocks.blockStoneDevice, 1, 3);
                }
                for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                    if (Math.abs(x) == 4 || Math.abs(z) == 4)
                        world.setBlock(6 + x, 8, z, ConfigBlocks.blockCandle, 0, 3);
                }
                // A symmetric stabilizer layer below the floor leaves the working area unobstructed.
                for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                    world.setBlock(6 + x, 5, z, Blocks.stonebrick);
                    world.setBlock(6 + x, 6, z, ConfigBlocks.blockCandle, 0, 3);
                }
                int jar = 0;
                for (Aspect aspect : recipe.getAspects()
                    .getAspects()) {
                    for (int n = 0; n < 4; n++) {
                        int x = 3 + jar++, z = 6;
                        world.setBlock(x, 8, z, ConfigBlocks.blockJar, 0, 3);
                        ((TileJarFillable) world.getTileEntity(x, 8, z)).addToContainer(aspect, 64);
                        world.getTileEntity(x, 8, z)
                            .markDirty();
                        world.markBlockForUpdate(x, 8, z);
                    }
                }
                Thaumcraft.proxy.getResearchManager()
                    .completeResearch(player, recipe.getResearch());
                player.inventory.setInventorySlotContents(0, GTNGItemList.ItemWirelessConnector.get(1));
                player.inventory.setInventorySlotContents(1, GTNGItemList.ThaumcraftInfusionCore.get(1));
                player.inventory.setInventorySlotContents(2, GTNGItemList.WirelessPackagedPatternProvider.get(1));
                player.inventory.setInventorySlotContents(
                    3,
                    AEApi.instance()
                        .definitions()
                        .materials()
                        .blankPattern()
                        .maybeStack(64)
                        .get());
                player.inventory.currentItem = 0;
                player.setSneaking(true);
                ItemWirelessConnector.use(player, 0, 8, 0, 1);
                player.setSneaking(false);
                ItemWirelessConnector.use(player, 6, 10, 0, 1);
                ItemStack pattern = AEApi.instance()
                    .definitions()
                    .items()
                    .encodedPattern()
                    .maybeStack(1)
                    .get();
                NBTTagCompound tag = new NBTTagCompound();
                tag.setBoolean("crafting", false);
                NBTTagList inputs = new NBTTagList();
                inputs.appendTag(
                    recipe.getRecipeInput()
                        .writeToNBT(new NBTTagCompound()));
                for (ItemStack stack : recipe.getComponents()) inputs.appendTag(stack.writeToNBT(new NBTTagCompound()));
                NBTTagList outputs = new NBTTagList();
                outputs.appendTag(((ItemStack) recipe.getRecipeOutput()).writeToNBT(new NBTTagCompound()));
                tag.setTag("in", inputs);
                tag.setTag("out", outputs);
                pattern.setTagCompound(tag);
                provider.setInventorySlotContents(0, pattern);
                sign(world, -4, -2, "AE terminals", "Craft | Pattern", "Interface", "Quartz wand core");
                sign(world, 0, -2, "Wireless Provider", "TC4 core installed", "Altar bound", "Auto-return ON");
                sign(world, 6, -6, "TC4 infusion", "Quartz wand core", "Order/Magic/Crystal", "Request at AE");
                player.playerNetServerHandler.setPlayerLocation(-0.5, 8, -5.5, -35, 12);
                player.capabilities.isFlying = false;
                player.sendPlayerAbilities();
            }
            if (ticks == 100) {
                ((TileInfusionMatrix) world.getTileEntity(6, 10, 0))
                    .onWandRightClick(world, null, player, 6, 10, 0, 1, 0);
                stock(recipe.getRecipeInput());
                for (ItemStack item : recipe.getComponents()) stock(item);
            }
            if (ticks == 160) {
                if (!provider.getProxy()
                    .isActive()) throw new IllegalStateException("Provider has no powered channel");
                if (!((TileInfusionMatrix) world.getTileEntity(6, 10, 0)).active)
                    throw new IllegalStateException("Altar inactive");
                int cpus = provider.getProxy()
                    .getCrafting()
                    .getCpus()
                    .size();
                if (cpus == 0) throw new IllegalStateException("Missing crafting CPU");
                provider.markDirty();
                world.saveAllChunks(true, null);
                server.getConfigurationManager()
                    .saveAllPlayerData();
                System.out.println(
                    "PACKAGED_PLAYGROUND_READY: real quartz wand rod recipe; CPU=" + cpus
                        + "; provider=0,8,0; altar=6,10,0; symmetry="
                        + ((TileInfusionMatrix) world.getTileEntity(6, 10, 0)).symmetry);
                ready = true;
                stopped = true;
            }
        } catch (Exception error) {
            stopped = true;
            System.err.println("PACKAGED_PLAYGROUND_FAILED");
            error.printStackTrace();
        }
    }

    private void stock(ItemStack ingredient) throws Exception {
        ItemStack stack = ingredient.copy();
        stack.stackSize = 64;
        var remainder = provider.getProxy()
            .getStorage()
            .getItemInventory()
            .injectItems(AEItemStack.create(stack), Actionable.MODULATE, new MachineSource(provider));
        if (remainder != null) throw new IllegalStateException("AE storage rejected materials");
    }

    private static void place(World world, int x, int z, IBlockDefinition definition) {
        ItemStack stack = definition.maybeStack(1)
            .get();
        world.setBlock(
            x,
            8,
            z,
            definition.maybeBlock()
                .get(),
            stack.getItemDamage(),
            3);
    }

    private static void terminal(World world, EntityPlayerMP player, int x, ItemStack terminal) {
        place(
            world,
            x,
            0,
            AEApi.instance()
                .definitions()
                .blocks()
                .multiPart());
        IPartHost host = (IPartHost) world.getTileEntity(x, 8, 0);
        host.addPart(
            AEApi.instance()
                .definitions()
                .parts()
                .cableGlass()
                .stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN,
            player);
        if (host.addPart(terminal, ForgeDirection.NORTH, player) == null)
            throw new IllegalStateException("Terminal placement failed");
    }

    private static void sign(World world, int x, int z, String... lines) {
        world.setBlock(x, 8, z, Blocks.standing_sign, 8, 3);
        TileEntitySign sign = (TileEntitySign) world.getTileEntity(x, 8, z);
        System.arraycopy(lines, 0, sign.signText, 0, 4);
        sign.markDirty();
        world.markBlockForUpdate(x, 8, z);
    }
}
