package com.xyp.gtnotgood.common.beekeeping;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeType;
import forestry.apiculture.genetics.BeeDefinition;
import forestry.apiculture.gui.GuiBeeHousing;
import forestry.apiculture.tiles.TileApiary;
import forestry.plugins.PluginApiculture;

/** Exercises real registered blocks, transformed product rolls, native breeding and the Forestry GUI. */
@Mod(modid = "workingapiaryqa", name = "Working Apiary QA", version = "1", dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class WorkingApiaryClientChecks {
    private boolean started;
    private volatile boolean verified;
    private volatile boolean failed;
    private int frames;
    private int ticks;
    private volatile boolean clientReady;
    private boolean guiOpened;
    private final File output = new File(System.getProperty("gtng.apiary.qa.output", "build/working-apiary-qa"));

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.apiary.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (failed) { mc.shutdown(); return; }
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("apiary-qa-" + System.currentTimeMillis(), "Apiary QA",
                new WorldSettings(23L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (++ticks > 6000) fail(new AssertionError("GUI verification timed out"));
        if (verified && mc.theWorld != null && mc.theWorld.getTileEntity(0, 5, 0) instanceof TileWorkingApiary) clientReady = true;
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || failed) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        WorldServer world = (WorldServer) player.worldObj;
        if (verified) {
            if (clientReady && !guiOpened) {
                guiOpened = true;
                ((TileWorkingApiary) world.getTileEntity(0, 5, 0)).openGui(player);
            }
            return;
        }
        float speed = Config.workingApiarySpeed, products = Config.workingApiaryProducts;
        float specialties = Config.workingApiarySpecialties;
        boolean jubilant = Config.enableBeeAlwaysJubilant;
        try {
            require(Config.workingApiaryLifespan == 2_147_436F, "default lifespan");
            Block block = Block.getBlockFromItem(GTNGItemList.WorkingApiary.get(1).getItem());
            world.setBlock(0, 5, 0, block, 0, 3);
            world.setBlock(2, 5, 0, PluginApiculture.blocks.apiculture, 0, 3);
            TileWorkingApiary working = (TileWorkingApiary) world.getTileEntity(0, 5, 0);
            TileApiary nativeTile = (TileApiary) world.getTileEntity(2, 5, 0);
            working.getAccessHandler().setOwner(player.getGameProfile());
            nativeTile.getAccessHandler().setOwner(player.getGameProfile());
            require(working.getSizeInventory() == nativeTile.getSizeInventory(), "native slot count");

            Config.enableBeeAlwaysJubilant = true;
            Config.workingApiaryProducts = Config.workingApiarySpecialties = 1;
            var bee = BeeDefinition.VALIANT.getIndividual();
            int ordinary = 0, special = 0;
            for (int seed = 0; seed < 500; seed++) {
                world.rand.setSeed(seed);
                ItemStack[] expected = bee.produceStacks(nativeTile);
                world.rand.setSeed(seed);
                ItemStack[] actual = bee.produceStacks(working);
                require(expected.length == actual.length, "neutral roll count");
                for (int i = 0; i < actual.length; i++) require(ItemStack.areItemStacksEqual(expected[i], actual[i]), "neutral seeded output");
                ordinary += count(expected, false);
                special += count(expected, true);
                Config.workingApiaryProducts = 3;
                Config.workingApiarySpecialties = 5;
                world.rand.setSeed(seed);
                actual = bee.produceStacks(working);
                require(count(actual, false) == count(expected, false) * 3, "ordinary multiplier");
                require(count(actual, true) == count(expected, true) * 5, "specialty multiplier");
                world.rand.setSeed(seed);
                actual = bee.produceStacks(nativeTile);
                require(count(actual, false) == count(expected, false) && count(actual, true) == count(expected, true), "native housing isolation");
                Config.workingApiaryProducts = Config.workingApiarySpecialties = 0;
                world.rand.setSeed(seed);
                require(bee.produceStacks(working).length == 0, "disabled output");
                Config.workingApiaryProducts = Config.workingApiarySpecialties = 1;
            }
            require(ordinary > 0 && special > 0, "both product types exercised");
            float nativeLife = BeeManager.beeRoot.createBeeHousingModifier(nativeTile).getLifespanModifier(bee.getGenome(), null, 1);
            float workingLife = BeeManager.beeRoot.createBeeHousingModifier(working).getLifespanModifier(bee.getGenome(), null, 1);
            require(workingLife == nativeLife * 2_147_436F, "native lifespan composition");

            working.getBeeInventory().setQueen(BeeDefinition.MEADOWS.getMemberStack(EnumBeeType.PRINCESS));
            working.getBeeInventory().setDrone(BeeDefinition.MEADOWS.getMemberStack(EnumBeeType.DRONE));
            Config.workingApiarySpeed = 3;
            for (int i = 0; i < 10; i++) working.updateServerSide();
            NBTTagCompound saved = new NBTTagCompound();
            working.writeToNBT(saved);
            require(saved.getInteger("BreedingTime") == 30, "3x native breeding clock");
            Config.workingApiarySpeed = 0.5F;
            working.updateServerSide();
            working.writeToNBT(saved);
            require(saved.getDouble("WorkingApiaryRemainder") == 0.5, "fractional clock saved");
            TileWorkingApiary reloaded = new TileWorkingApiary();
            reloaded.setWorldObj(world);
            reloaded.readFromNBT(saved);
            reloaded.updateServerSide();
            NBTTagCompound resumed = new NBTTagCompound();
            reloaded.writeToNBT(resumed);
            require(resumed.getInteger("BreedingTime") == 31, "fractional clock resumed");
            require(ItemStack.areItemStacksEqual(working.getBeeInventory().getDrone(), reloaded.getBeeInventory().getDrone()), "inventory persisted");

            List<ItemStack> split = new ArrayList<>();
            ItemStack source = new ItemStack(Items.sugar, 40);
            source.setTagCompound(new NBTTagCompound());
            source.getTagCompound().setString("QA", "retained");
            WorkingApiaryProducts.add(split, source, 3, new Random(0));
            require(split.size() == 2 && split.get(0).stackSize == 64 && split.get(1).stackSize == 56, "split max stacks");
            require(source.stackSize == 40 && ItemStack.areItemStackTagsEqual(source, split.get(1)), "quantity and NBT isolation");
            split.clear();
            Random rng = new Random(123);
            for (int i = 0; i < 10000; i++) WorkingApiaryProducts.add(split, new ItemStack(Items.sugar), 0.5, rng);
            require(split.size() > 4800 && split.size() < 5200, "fractional yield distribution");

            Config.workingApiarySpeed = 1;
            player.setPositionAndUpdate(0.5, 5, 3.5);
            verified = true;
            System.out.println("WORKING_APIARY_QA: server PASS (500 seeded production comparisons, lifecycle, persistence, multipliers)");
        } catch (Throwable error) {
            fail(error);
        } finally {
            Config.workingApiarySpeed = speed;
            Config.workingApiaryProducts = products;
            Config.workingApiarySpecialties = specialties;
            Config.enableBeeAlwaysJubilant = jubilant;
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (!verified || failed || event.phase != TickEvent.Phase.END || ++frames < 120) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiBeeHousing)) return;
        try {
            TileWorkingApiary tile = (TileWorkingApiary) mc.theWorld.getTileEntity(0, 5, 0);
            Block block = tile.getBlockType();
            for (ForgeDirection direction : new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST }) {
                tile.setOrientation(direction);
                for (int side = 0; side < 6; side++) {
                    require(block.getIcon(mc.theWorld, 0, 5, 0, side) == PluginApiculture.blocks.apiculture.getIcon(mc.theWorld, 0, 5, 0, side), "native oriented icon identity");
                    require(block.getIcon(side, 0) == PluginApiculture.blocks.apiculture.getIcon(side, 0), "native item icon identity");
                }
            }
            require("工作蜂箱".equals(GTNGItemList.WorkingApiary.get(1).getDisplayName()), "localized name");
            output.mkdirs();
            ScreenShotHelper.saveScreenshot(output, "working-apiary-gui.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            Files.write(new File(output, "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
            System.out.println("WORKING_APIARY_QA: GUI PASS");
            mc.shutdown();
        } catch (Throwable error) { fail(error); }
    }

    private static int count(ItemStack[] products, boolean specialty) {
        int count = 0;
        for (ItemStack product : products) if ((product.getItem() == Items.sugar) == specialty) count += product.stackSize;
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void fail(Throwable error) {
        error.printStackTrace();
        failed = true;
        try {
            output.mkdirs();
            Files.write(new File(output, "result.txt").toPath(), ("FAIL: " + error).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }
}
