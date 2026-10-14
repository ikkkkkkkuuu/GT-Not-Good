package com.xyp.gtnotgood.qa;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.gtnewhorizon.cropsnh.api.CropsNHCrops;
import com.gtnewhorizon.cropsnh.api.CropsNHItemList;
import com.gtnewhorizon.cropsnh.farming.SeedData;
import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.farming.registries.FertilizerRegistry;
import com.gtnewhorizon.cropsnh.init.CropsNHBlocks;
import com.gtnewhorizon.cropsnh.tileentity.TileEntityCropSticks;
import com.xyp.gtnotgood.common.machines.basic.SteamCropManager;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.common.gui.modularui.widget.SteamGaugeWidget;

@Mod(
    modid = "steamcropmanagerqa",
    name = "Steam crop manager QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class SteamCropManagerClientChecks {

    private final List<NativeCrop> inRange = new ArrayList<>();
    private final List<NativeCrop> outside = new ArrayList<>();
    private boolean started;
    private volatile boolean finished, opened, togglesChecked, worldRequested, worldPositioned;
    private volatile int buttonRequest;
    private int ticks, frames, visualStage;
    private BaseMetaTileEntity base;
    private SteamCropManager machine;
    private NativeCrop young;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModularUIConfig.guiDebugMode = false;
        GameRegistry.registerTileEntity(NativeCrop.class, "steamcropmanagerqa.crop");
        if (Boolean.getBoolean("gtng.steamcropmanager.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.currentScreen instanceof GuiMainMenu) {
            started = true;
            mc.launchIntegratedServer("steam-crop-manager-" + System.currentTimeMillis(), "Steam crop manager QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            require(++ticks <= 2400, "client QA timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            World world = player.worldObj;
            if (ticks == 1) {
                world.setBlock(4, 10, 4, GregTechAPI.sBlockMachines, 0, 3);
                base = (BaseMetaTileEntity) world.getTileEntity(4, 10, 4);
                base.setInitialValuesAsNBT(null, (short) GTNGMachineID.SteamCropManager.id);
                base.setOwnerName(player.getCommandSenderName());
                base.setOwnerUuid(player.getUniqueID());
                base.setFrontFacing(ForgeDirection.NORTH);
                base.disableWorking();
                machine = (SteamCropManager) base.getMetaTileEntity();
                inRange.add(crop(world, 5, 10, 4, true));
                inRange.add(crop(world, 7, 10, 7, true));
                inRange.add(crop(world, 1, 8, 4, true));
                inRange.add(crop(world, 4, 12, 1, true));
                young = crop(world, 4, 10, 5, false);
                outside.add(crop(world, 8, 10, 4, true));
                outside.add(crop(world, 4, 13, 4, true));
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.playerNetServerHandler.setPlayerLocation(4.5, 11, 1.5, 0, 20);
            }
            if (ticks == 40) {
                verifyCrafting(world);
                verifyFluidRouting();
                verifyHarvesting();
                verifyFullOutput();
                verifySecondary(world);
                verifyPersistence(world);
                machine.onRightclick(base, player);
                opened = true;
                System.out.println("STEAM_CROP_MANAGER_QA: crop API, range, supplies, steam and persistence PASS");
            }
            if (
                buttonRequest == 1 && !machine.harvestEnabled
                    && machine.waterEnabled
                    && machine.weedEXEnabled
                    && machine.fertilizerEnabled
            ) {
                togglesChecked = true;
            }
            if (worldRequested && !worldPositioned) {
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(4.5, 11, 1.5, 0, 25);
                worldPositioned = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void verifyFluidRouting() {
        require(machine.getSizeInventory() == 21 && machine.mInventory.length == 21,
            "two Weed-EX, four fertilizer and fifteen output slots");
        require(!machine.isElectric() && !machine.isEnetInput()
            && machine.maxEUStore() == 0
            && machine.rechargerSlotCount() == 0
            && machine.dechargerSlotCount() == 0, "no EU or battery slots");
        require(machine.harvestEnabled && !machine.waterEnabled && !machine.fertilizerEnabled && !machine.weedEXEnabled,
            "harvest enabled and three support modes disabled by default");
        require(
            base.fill(ForgeDirection.UP, Materials.Steam.getGas(32000), false) == 32000 && base.getStoredSteam() == 0
                && machine.getWaterAmount() == 0,
            "simulated steam pipe fill leaves both stores unchanged");
        require(base.fill(ForgeDirection.UNKNOWN, Materials.Steam.getGas(32000), false) == 32000
            && base.getStoredSteam() == 0
            && machine.getWaterAmount() == 0, "unknown-side steam simulation leaves both stores unchanged");
        require(
            base.fill(ForgeDirection.UP, Materials.Steam.getGas(32000), true) == 32000 && base.getStoredSteam() == 16000
                && machine.getWaterAmount() == 0,
            "32000 L steam fills the independent 16000-unit steam store");
        require(base.fill(ForgeDirection.UP, new FluidStack(FluidRegistry.WATER, 1000), false) == 1000
            && machine.getWaterAmount() == 0
            && base.getStoredSteam() == 16000, "simulated water pipe fill cannot mutate supplies or steam");
        require(base.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.WATER, 1000), false) == 1000
            && machine.fill(new FluidStack(FluidRegistry.WATER, 1000), false) == 1000
            && machine.getWaterAmount() == 0
            && base.getStoredSteam() == 16000, "unknown-side and direct water simulation preserve both stores");
        require(base.fill(ForgeDirection.UP, new FluidStack(FluidRegistry.WATER, 1000), true) == 1000
            && machine.getWaterAmount() == 1000
            && base.getStoredSteam() == 16000, "water arrives through pipes without replacing steam");
        require(
            base.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.WATER, 250), true) == 250
                && machine.getWaterAmount() == 1250
                && base.getStoredSteam() == 16000,
            "unknown-side actual water fill respects the independent crop supply store");
        require(base.fill(ForgeDirection.UP, new FluidStack(FluidRegistry.LAVA, 100), true) == 0,
            "unregistered crop supplies are rejected");
        require(base.drain(ForgeDirection.UP, 100, true) == null, "crop supplies cannot drain through pipes");
        machine.setWaterAmount(0);
        base.setStoredSteam(0);
    }

    private void verifyHarvesting() {
        machine.onPostTick(base, 50);
        require(inRange.stream().allMatch(crop -> crop.harvestCalls == 0), "disabled machine pauses harvesting");
        base.enableWorking();
        machine.onPostTick(base, 100);
        require(inRange.stream().allMatch(crop -> crop.harvestCalls == 0), "no steam preserves mature crops");
        base.setStoredSteam(100);
        machine.onPostTick(base, 150);
        require(
            inRange.stream().allMatch(
                crop -> crop.harvestCalls == 1 && crop.lastMultiplier == 1.0 && crop.hasCrop() && !crop.canHarvest()),
            "mature CropsNH crops harvest once with native 1.0 multiplier");
        require(base.getStoredSteam() == 84, "four harvests cost exactly 32 L steam");
        require(outside.stream().allMatch(crop -> crop.harvestCalls == 0 && crop.canHarvest()),
            "horizontal radius three and vertical radius two exclude outer crops");
        require(young.hasCrop() && young.harvestCalls == 0 && young.getGrowthProgress() == 0,
            "young crop and planted seed remain intact");
        int expected = inRange.stream().mapToInt(crop -> crop.drops.stream().mapToInt(stack -> stack.stackSize).sum())
            .sum();
        require(outputCount(new ItemStack(Items.carrot)) == expected && expected > 0,
            "native carrot harvest results enter the machine without bonus or loss");
    }

    private void verifyFullOutput() {
        for (int slot = SteamCropManager.SLOT_OUTPUT_START; slot <= SteamCropManager.SLOT_OUTPUT_END; slot++)
            base.setInventorySlotContents(slot, new ItemStack(Blocks.cobblestone, 64));
        NativeCrop mature = inRange.get(0);
        mature.setGrowthProgress(CropsNHCrops.Carrot.getGrowthDuration());
        base.setStoredSteam(100);
        machine.onPostTick(base, 200);
        require(mature.canHarvest() && mature.harvestCalls == 1 && base.getStoredSteam() == 100,
            "full output pauses harvest without consuming crops or steam");
        clearOutputs();
    }

    private void verifySecondary(World world) {
        for (NativeCrop crop : inRange) world.setBlockToAir(crop.xCoord, crop.yCoord, crop.zCoord);
        inRange.clear();
        machine.harvestEnabled = false;
        machine.waterEnabled = machine.weedEXEnabled = machine.fertilizerEnabled = true;
        machine.setWaterAmount(1000);
        machine.setWeedEXAmount(100);
        machine.setLiquidFertilizerAmount(1000);
        base.setStoredSteam(0);
        machine.onPostTick(base, 600);
        require(young.getWaterStorage() == 0 && young.getFertilizerStorage() == 0
            && young.getWeedExStorage() == 0
            && machine.getWaterAmount() == 1000
            && machine.getWeedEXAmount() == 100
            && machine.getLiquidFertilizerAmount() == 1000, "no steam preserves all crop care supplies");
        base.disableWorking();
        base.setStoredSteam(100);
        machine.onPostTick(base, 600);
        require(young.getWaterStorage() == 0 && base.getStoredSteam() == 100,
            "disabled machine pauses crop care without consuming steam");
        base.enableWorking();
        machine.onPostTick(base, 600);
        require(young.getWaterStorage() == 200 && young.getFertilizerStorage() == 200 && young.getWeedExStorage() == 75,
            "native crop receives water, liquid fertilizer and Weed-EX");
        require(machine.getWaterAmount() == 800 && machine.getLiquidFertilizerAmount() == 800
            && machine.getWeedEXAmount() == 90
            && base.getStoredSteam() == 97, "three support operations consume exact supplies and 6 L steam");
        require(outside.stream().allMatch(
            crop -> crop.getWaterStorage() == 0 && crop.getFertilizerStorage() == 0 && crop.getWeedExStorage() == 0),
            "support operations respect the same crop range");
        machine.onPostTick(base, 650);
        require(machine.getWaterAmount() == 800 && machine.getLiquidFertilizerAmount() == 800
            && young.getWeedExStorage() == 150
            && machine.getWeedEXAmount() == 80
            && base.getStoredSteam() == 96, "full hydration and fertilizer skip their supply and steam costs");
        machine.onPostTick(base, 700);
        require(base.getStoredSteam() == 96, "all satisfied support thresholds consume no steam");
        world.setBlockToAir(young.xCoord, young.yCoord, young.zCoord);
        young = crop(world, 4, 10, 5, false);
        machine.setWaterAmount(0);
        machine.setWeedEXAmount(0);
        machine.setLiquidFertilizerAmount(0);
        ItemStack fertilizer = CropsNHItemList.fertilizer.get(2);
        int potency = FertilizerRegistry.instance.getPotency(fertilizer);
        require(potency > 0, "CropsNH fertilizer fixture is natively registered");
        base.setInventorySlotContents(SteamCropManager.SLOT_FERT_START, fertilizer);
        base.setInventorySlotContents(SteamCropManager.SLOT_WEEDEX_START, CropsNHItemList.weedEX.get(1));
        machine.onPostTick(base, 1200);
        require(
            young.getFertilizerStorage() == Math.min(200, potency)
                && base.getStackInSlot(SteamCropManager.SLOT_FERT_START).stackSize == 1,
            "item fertilizer applies its native potency and consumes one item");
        require(young.getWeedExStorage() == 75 && machine.getWeedEXAmount() > 0 && base.getStoredSteam() == 94,
            "Weed-EX can refills the supply while two actual support actions cost 4 L steam");
        base.disableWorking();
        require(base.getStoredEU() == 0, "all crop work uses steam alone");
    }

    @SuppressWarnings("unchecked")
    private void verifyPersistence(World world) throws Exception {
        machine.setWaterAmount(1234);
        machine.setWeedEXAmount(123);
        machine.setLiquidFertilizerAmount(2345);
        base.setStoredSteam(5678);
        machine.harvestEnabled = true;
        machine.waterEnabled = machine.weedEXEnabled = machine.fertilizerEnabled = false;
        base.setInventorySlotContents(SteamCropManager.SLOT_OUTPUT_START, new ItemStack(Items.carrot, 7));
        ItemStack usedCan = CropsNHItemList.weedEX.get(1);
        usedCan.setItemDamage(3);
        base.setInventorySlotContents(SteamCropManager.SLOT_WEEDEX_START, usedCan);
        Field overflowField = SteamCropManager.class.getDeclaredField("pendingDrops");
        overflowField.setAccessible(true);
        List<ItemStack> overflow = (List<ItemStack>) overflowField.get(machine);
        overflow.add(new ItemStack(Items.carrot, 257));
        NBTTagCompound tag = new NBTTagCompound();
        base.writeToNBT(tag);
        world.removeTileEntity(4, 10, 4);
        base = new BaseMetaTileEntity();
        base.setWorldObj(world);
        base.readFromNBT(tag);
        world.setTileEntity(4, 10, 4, base);
        machine = (SteamCropManager) base.getMetaTileEntity();
        world.markBlockForUpdate(4, 10, 4);
        require(
            machine.getWaterAmount() == 1234 && machine.getWeedEXAmount() == 123
                && machine.getLiquidFertilizerAmount() == 2345
                && base.getStoredSteam() == 5678,
            "tile reload preserves the independent supplies and inherited steam store");
        require(machine.harvestEnabled && !machine.waterEnabled
            && !machine.weedEXEnabled
            && !machine.fertilizerEnabled
            && !base.isAllowedToWork(), "tile reload preserves modes and power switch");
        require(
            outputCount(new ItemStack(Items.carrot)) == 7
                && base.getStackInSlot(SteamCropManager.SLOT_FERT_START).stackSize == 1
                && base.getStackInSlot(SteamCropManager.SLOT_WEEDEX_START).getItemDamage() == 3,
            "inherited GT inventory NBT retains fertilizer, used Weed-EX can and harvested items");
        machine.loadNBTData(tag);
        overflow = (List<ItemStack>) overflowField.get(machine);
        require(overflow.stream().mapToInt(stack -> stack.stackSize).sum() == 257,
            "repeated NBT load preserves overflow without duplication");
        base.enableWorking();
        machine.onPostTick(base, 1800);
        require(outputCount(new ItemStack(Items.carrot)) == 264 && overflow.isEmpty() && base.getStoredSteam() == 5678,
            "restored overflow drains without harvesting or steam costs");
        base.disableWorking();
    }

    private static NativeCrop crop(World world, int x, int y, int z, boolean mature) {
        world.setBlock(x, y - 1, z, Blocks.farmland, 7, 3);
        world.setBlock(x, y, z, CropsNHBlocks.blockCropSticks, 0, 3);
        NativeCrop crop = new NativeCrop();
        world.setTileEntity(x, y, z, crop);
        crop.plantSeed(new SeedData(CropsNHCrops.Carrot, new SeedStats((byte) 31, (byte) 31, (byte) 31)));
        crop.setGrowthProgress(mature ? CropsNHCrops.Carrot.getGrowthDuration() : 0);
        require(crop.hasCrop() && crop.canHarvest() == mature, "native crop fixture at " + x + "," + y + "," + z);
        return crop;
    }

    private static void verifyCrafting(World world) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        ItemStack plate = GTOreDictUnificator.get(OrePrefixes.plate, Materials.Bronze, 1);
        ItemStack rotor = GTOreDictUnificator.get(OrePrefixes.rotor, Materials.Bronze, 1);
        ItemStack pipe = GTOreDictUnificator.get(OrePrefixes.pipeSmall, Materials.Bronze, 1);
        ItemStack[] ingredients = { plate, new ItemStack(Items.iron_hoe), plate, rotor, ItemList.Hull_Bronze.get(1),
            rotor, pipe, new ItemStack(Items.bucket), pipe };
        for (int i = 0; i < ingredients.length; i++) grid.setInventorySlotContents(i, ingredients[i].copy());
        ItemStack output = CraftingManager.getInstance().findMatchingRecipe(grid, world);
        require(GTUtility.areStacksEqual(output, GTNGItemList.SteamCropManager.get(1)) && output.stackSize == 1,
            "registered bronze crafting grid produces the steam crop manager");
    }

    private int outputCount(ItemStack expected) {
        int amount = 0;
        for (int slot = SteamCropManager.SLOT_OUTPUT_START; slot <= SteamCropManager.SLOT_OUTPUT_END; slot++) {
            ItemStack stack = base.getStackInSlot(slot);
            if (GTUtility.areStacksEqual(stack, expected)) amount += stack.stackSize;
        }
        return amount;
    }

    private void clearOutputs() {
        for (int slot = SteamCropManager.SLOT_OUTPUT_START; slot <= SteamCropManager.SLOT_OUTPUT_END; slot++)
            base.setInventorySlotContents(slot, null);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !opened || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (visualStage == 2) {
                if (!worldPositioned || ++frames < 60) return;
                ScreenShotHelper.saveScreenshot(outputDir(), "steam-crop-manager-world.png", mc.displayWidth,
                    mc.displayHeight, mc.getFramebuffer());
                Files.write(new File(outputDir(), "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                System.out.println("STEAM_CROP_MANAGER_QA: PASS native CropsNH work, NBT, pipes and GUI");
                finished = true;
                mc.shutdown();
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper)) return;
            ModularPanel panel = ((GuiContainerWrapper) mc.currentScreen).getScreen().getMainPanel();
            if (visualStage == 1) {
                if (!togglesChecked || ++frames < 30) return;
                require(
                    !findButton(panel, "harvest").isValueSelected() && findButton(panel, "water").isValueSelected()
                        && findButton(panel, "weedEX").isValueSelected()
                        && findButton(panel, "fertilizer").isValueSelected(),
                    "four GUI mode changes synchronize to server and back");
                ScreenShotHelper.saveScreenshot(outputDir(), "steam-crop-manager.png", mc.displayWidth,
                    mc.displayHeight, mc.getFramebuffer());
                verifyTextures(panel, mc);
                mc.thePlayer.closeScreen();
                worldRequested = true;
                visualStage = 2;
                frames = 0;
                return;
            }
            if (++frames < 60) return;
            Set<Integer> indices = new HashSet<>();
            for (ItemSlot slot : collect(panel, ItemSlot.class)) {
                if (!ModularSlot.isPlayerSlot(slot.getSlot())) indices.add(slot.getSlot().getSlotIndex());
            }
            require(indices.size() == 21, "GUI exposes exactly twenty-one machine slots");
            for (int slot = 0; slot < 21; slot++) require(indices.contains(slot), "GUI exposes machine slot " + slot);
            require(collect(panel, SteamGaugeWidget.class).size() == 1, "GUI displays the native steam gauge");
            require(findButton(panel, "harvest").isValueSelected() && !findButton(panel, "water").isValueSelected()
                && !findButton(panel, "weedEX").isValueSelected()
                && !findButton(panel, "fertilizer").isValueSelected(), "GUI displays all four persisted modes");
            for (String name : new String[] { "water", "weedEX", "fertilizer", "harvest" })
                findButton(panel, name).onMousePressed(0);
            buttonRequest = 1;
            visualStage = 1;
            frames = 0;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static ToggleButton findButton(IWidget root, String name) {
        for (ToggleButton button : collect(root, ToggleButton.class)) if (name.equals(button.getName())) return button;
        throw new AssertionError("Missing crop mode button: " + name);
    }

    private static <T> List<T> collect(IWidget widget, Class<T> type) {
        List<T> found = new ArrayList<>();
        if (type.isInstance(widget)) found.add(type.cast(widget));
        for (IWidget child : widget.getChildren()) found.addAll(collect(child, type));
        return found;
    }

    private static void verifyTextures(ModularPanel panel, Minecraft mc) throws Exception {
        BufferedImage screenshot = ImageIO.read(new File(outputDir(), "screenshots/steam-crop-manager.png"));
        require(screenshot != null, "native GUI screenshot saved");
        int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        var area = panel.getArea();
        int missing = 0;
        for (int y = Math.max(0, area.y * scale); y
            < Math.min(screenshot.getHeight(), (area.y + area.height) * scale); y++) {
            for (int x = Math.max(0, area.x * scale); x
                < Math.min(screenshot.getWidth(), (area.x + area.width) * scale); x++) {
                int rgb = screenshot.getRGB(x, y);
                if ((rgb >>> 16 & 255) >= 200 && (rgb >>> 8 & 255) <= 50 && (rgb & 255) >= 200) missing++;
            }
        }
        require(missing <= 16, "native crop manager GUI has no missing magenta textures");
    }

    private static File outputDir() {
        return new File(System.getProperty("gtng.steamcropmanager.qa.output", "."));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("STEAM_CROP_MANAGER_QA: " + message);
        if (!message.equals("client QA timeout")) System.out.println("STEAM_CROP_MANAGER_QA: " + message);
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            Files.write(new File(outputDir(), "result.txt").toPath(),
                ("FAIL: " + failure).getBytes(StandardCharsets.UTF_8));
        } catch (Exception writeFailure) {
            writeFailure.printStackTrace();
        }
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    public static final class NativeCrop extends TileEntityCropSticks {

        private final List<ItemStack> drops = new ArrayList<>();
        private int harvestCalls;
        private double lastMultiplier;

        @Override
        public void updateEntity() {}

        @Override
        public ArrayList<ItemStack> harvest(double multiplier) {
            harvestCalls++;
            lastMultiplier = multiplier;
            ArrayList<ItemStack> nativeDrops = super.harvest(multiplier);
            if (nativeDrops != null) for (ItemStack stack : nativeDrops) drops.add(stack.copy());
            return nativeDrops;
        }
    }
}
