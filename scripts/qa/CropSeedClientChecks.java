package com.xyp.gtnotgood.common.crop;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.gtnewhorizon.cropsnh.api.CropsNHCrops;
import com.gtnewhorizon.cropsnh.init.CropsNHBlocks;
import com.gtnewhorizon.cropsnh.farming.SeedData;
import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.tileentity.TileEntityCropSticks;
import com.xyp.gtnotgood.config.Config;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in seed regression checks against the actual Mixin-transformed CropsNH tile. */
@Mod(modid = "cropseedqa", name = "Crop Seed QA", version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public final class CropSeedClientChecks {

    private boolean started;
    private volatile boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.cropseed.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) { mc.shutdown(); return; }
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("crop-seed-qa-" + System.currentTimeMillis(), "Crop Seed QA",
                new WorldSettings(26L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        boolean oldMax = Config.enableCropMaxStats;
        boolean oldDrop = Config.enableCropGuaranteedSeedDrop;
        boolean oldCreative = player.capabilities.isCreativeMode;
        try {
            System.out.println("CROP_SEED_QA: configured guaranteed=" + oldDrop);
            Config.enableCropMaxStats = false;
            Config.enableCropGuaranteedSeedDrop = true;
            player.capabilities.isCreativeMode = false;
            for (int amount : new int[] { 1, 2, 64 }) {
                int x = 100 + amount * 4;
                player.worldObj.setBlock(x, 80, 0, Blocks.farmland, 7, 3);
                player.worldObj.setBlock(x, 81, 0, CropsNHBlocks.blockCropSticks, 0, 3);
                TileEntityCropSticks crop = (TileEntityCropSticks) player.worldObj.getTileEntity(x, 81, 0);
                ItemStack held = CropsNHCrops.Wheat.getSeedItem(new SeedStats((byte) 1, (byte) 1, (byte) 0));
                held.stackSize = amount;
                ItemStack expected = held.copy();
                player.inventory.setInventorySlotContents(player.inventory.currentItem, held);
                crop.onRightClick(player, held);
                if (!crop.hasCrop() || held.stackSize != amount - 1) throw new AssertionError("Planting failed");
                System.out.println("CROP_SEED_QA: planted " + amount + ", stored seed amount=" + crop.getSeedStack().stackSize);
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                CropsNHBlocks.blockCropSticks.onBlockClicked(player.worldObj, x, 81, 0, player);
                List<EntityItem> items = player.worldObj.getEntitiesWithinAABB(EntityItem.class,
                    AxisAlignedBB.getBoundingBox(x - 1, 80, -1, x + 2, 83, 2));
                int seeds = items.stream().map(EntityItem::getEntityItem)
                    .filter(s -> s.isItemEqual(expected) && ItemStack.areItemStackTagsEqual(s, expected))
                    .mapToInt(s -> s.stackSize).sum();
                if (seeds != 1 || crop.hasCrop()) throw new AssertionError("Planted stack=" + amount + ", dropped seeds=" + seeds);
            }
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            for (boolean mature : new boolean[] { false, true }) {
                for (int resistance : new int[] { 0, 1, 10, 31 }) {
                    for (int attempt = 0; attempt < 100; attempt++) {
                        CaptureTile tile = new CaptureTile(mature);
                        tile.setWorldObj(player.worldObj);
                        tile.plantSeed(new SeedData(CropsNHCrops.Wheat, new SeedStats((byte) 1, (byte) 1, (byte) resistance)));
                        ItemStack expected = tile.getSeedStack();
                        tile.onLeftClick(player, null);
                        long seeds = tile.drops.stream().filter(s -> s != null && s.isItemEqual(expected)
                            && ItemStack.areItemStackTagsEqual(s, expected)).mapToInt(s -> s.stackSize).sum();
                        if (seeds != 1 || tile.hasCrop()) throw new AssertionError("mature=" + mature
                            + " resistance=" + resistance + " seeds=" + seeds);
                    }
                }
            }
            Files.write(Paths.get("crop-seed-qa-result.txt"), "PASS: survival planting/entity drops for stacks 1/2/64; 800 empty-hand removals".getBytes());
            System.out.println("CROP_SEED_QA: PASS");
        } catch (Throwable failure) {
            failure.printStackTrace();
            try { Files.write(Paths.get("crop-seed-qa-result.txt"), ("FAIL: " + failure).getBytes()); }
            catch (Exception ignored) { }
        } finally {
            Config.enableCropMaxStats = oldMax;
            Config.enableCropGuaranteedSeedDrop = oldDrop;
            player.capabilities.isCreativeMode = oldCreative;
            finished = true;
        }
    }

    /** Captures drops while retaining the real harvest, removal and injected seed-drop methods. */
    public static final class CaptureTile extends TileEntityCropSticks {
        private final boolean mature;
        private final List<ItemStack> drops = new ArrayList<>();
        public CaptureTile(boolean mature) { this.mature = mature; }
        @Override
        public boolean canHarvest() { return mature; }
        @Override
        public void dropItem(ItemStack stack) { drops.add(stack.copy()); }
    }
}
