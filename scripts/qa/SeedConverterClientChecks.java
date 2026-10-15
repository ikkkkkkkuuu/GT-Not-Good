package com.xyp.gtnotgood.qa;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.ModularUIConfig;
import com.gtnewhorizon.cropsnh.api.CropsNHCrops;
import com.gtnewhorizon.cropsnh.api.CropsNHItemList;
import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;
import com.gtnewhorizon.cropsnh.items.ItemGenericSeed;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import codechicken.nei.recipe.GuiCraftingRecipe;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.GTMod;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

@Mod(
    modid = "seedconverterqa",
    name = "Seed converter QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class SeedConverterClientChecks {

    private final StringBuilder checks = new StringBuilder();
    private boolean started;
    private volatile boolean finished, opened, worldRequested, worldPositioned;
    private int ticks, frames, visualStage, automationDeadline;
    private BaseMetaTileEntity base;
    private MTEBasicMachine machine;
    private TileEntityHopper hopper;
    private IInventory chest;
    private volatile String recipeMapName;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.seedconverter.qa")) return;
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.currentScreen instanceof GuiMainMenu) {
            started = true;
            mc.launchIntegratedServer("seed-converter-" + System.currentTimeMillis(), "Seed converter QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            if (++ticks > 3600) throw new AssertionError("Client QA timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            World world = player.worldObj;
            if (ticks == 1) setup(world, player);
            if (ticks == 40) {
                verifyCrafting(world);
                verifyAllMappings();
                verifyConversion(new ItemStack(Items.wheat_seeds), "vanilla wheat");
                verifyConversion(new ItemStack(Items.carrot), "vanilla carrot");
                verifyModSeed();
                verifyBlockedAndUnsupported();
                verifyPersistence(world);
                verifySidedInventory();
                beginAutomation(world);
            }
            if (automationDeadline > 0 && ticks == automationDeadline) {
                ItemStack expected = CropsNHCrops.Wheat.getSeedItem(SeedStats.DEFAULT_ANALYZED);
                require(count(chest, expected) == 3 && count(hopper, new ItemStack(Items.wheat_seeds)) == 0,
                    "real hopper inserts three seeds and native machine auto-output delivers three seeds to chest");
                require(inputCount() == 0 && outputCount() == 0, "automated run leaves no duplicate buffered items");
                base.disableWorking();
                machine.mItemTransfer = false;
                base.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.wheat_seeds, 16));
                base.setInventorySlotContents(machine.getOutputSlot(), expected);
                machine.onRightclick(base, player);
                opened = true;
            }
            if (worldRequested && !worldPositioned) {
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(3.0, 11.0, 1.5, -25, 25);
                worldPositioned = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(World world, EntityPlayerMP player) {
        world.setWorldTime(6000);
        for (int x = 1; x <= 7; x++) for (int z = 1; z <= 7; z++) world.setBlock(x, 9, z, Blocks.planks);
        world.setBlock(4, 10, 4, GregTechAPI.sBlockMachines, 0, 3);
        base = (BaseMetaTileEntity) world.getTileEntity(4, 10, 4);
        base.setInitialValuesAsNBT(null, (short) GTNGItemList.SeedConverter.get(1).getItemDamage());
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.setFrontFacing(ForgeDirection.NORTH);
        base.disableWorking();
        machine = (MTEBasicMachine) base.getMetaTileEntity();
        machine.setMainFacing(ForgeDirection.NORTH);
        require(machine.mInputSlotCount == 1 && machine.mOutputItems.length == 1,
            "LV machine has one recipe input and one recipe output");
        require(machine.maxEUInput() == 32 && machine.isElectric(), "machine accepts LV power");
        require(machine.getRecipeMap() != null && !machine.getRecipeMap().getAllRecipes().isEmpty(),
            "conversion recipes are registered after CropsNH alternate seeds");
        recipeMapName = machine.getRecipeMap().unlocalizedName;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(4.5, 11, 1.5, 0, 25);
    }

    private void verifyConversion(ItemStack input, String label) {
        reset();
        ICropCard crop = CropRegistry.instance.fromAlternateSeed(input);
        require(crop != null, label + " has a native alternate-seed mapping");
        ItemStack expected = crop.getSeedItem(SeedStats.DEFAULT_ANALYZED);
        GTRecipe recipe = machine.getRecipeMap().findRecipeQuery().items(input.copy()).voltage(32).find();
        require(recipe != null && recipe.mInputs.length == 1
            && recipe.mOutputs.length == 1
            && recipe.mInputs[0].stackSize == 1
            && recipe.mOutputs[0].stackSize == 1, label + " has an NEI-visible one-to-one recipe");
        require(
            GTUtility.areStacksEqual(recipe.mOutputs[0], expected) && recipe.mEUt == 16
                && recipe.mDuration == Config.getModifiedRecipeDuration(20),
            label + " recipe preserves CropsNH initial stats and uses 16 EU/t for configured base 20 ticks");
        input = input.copy();
        input.stackSize = 2;
        base.setInventorySlotContents(machine.getInputSlot(), input);
        base.setStoredEU(base.getEUCapacity());
        require(machine.checkRecipe() == 2 && inputCount() == 1, label + " consumes exactly one input");
        require(GTUtility.areStacksEqual(machine.mOutputItems[0], expected), label + " stages correct native seed NBT");
        long energy = base.getStoredEU();
        int duration = machine.mMaxProgresstime;
        int eut = machine.mEUt;
        finishRecipe();
        require(GTUtility.areStacksEqual(base.getStackInSlot(machine.getOutputSlot()), expected) && outputCount() == 1,
            label + " actually completes into its inventory");
        require(energy - base.getStoredEU() == (long) duration * eut,
            label + " actual ticks consume the exact recipe energy");
    }

    private void verifyModSeed() {
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (crop.getAlternateSeeds() == null) continue;
            for (ItemStack input : crop.getAlternateSeeds()) {
                if (
                    input == null || input.getItem() instanceof ItemGenericSeed
                        || CropRegistry.instance.fromAlternateSeed(input) == null
                ) continue;
                var id = GameRegistry.findUniqueIdentifierFor(input.getItem());
                if (id != null && !"minecraft".equals(id.modId)) {
                    verifyConversion(input, "mod alternate seed " + id + ":" + input.getItemDamage());
                    return;
                }
            }
        }
        throw new AssertionError("No registered mod alternate seed found in QA runtime");
    }

    private void verifyAllMappings() {
        int mapped = 0;
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (crop.getAlternateSeeds() == null) continue;
            for (ItemStack input : crop.getAlternateSeeds()) {
                if (input == null || input.getItem() instanceof ItemGenericSeed) continue;
                ICropCard target = CropRegistry.instance.fromAlternateSeed(input);
                if (target == null) continue;
                GTRecipe recipe = machine.getRecipeMap().findRecipeQuery().items(input.copy()).voltage(32).find();
                if (
                    recipe == null
                        || !GTUtility.areStacksEqual(recipe.mOutputs[0], target.getSeedItem(SeedStats.DEFAULT_ANALYZED))
                ) {
                    throw new AssertionError("Alternate-seed mapping differs from CropsNH: " + input);
                }
                mapped++;
            }
        }
        require(mapped >= 3, "all " + mapped + " native alternate-seed mappings resolve to their authoritative crop");
    }

    private void verifyBlockedAndUnsupported() {
        reset();
        base.setStoredEU(base.getEUCapacity());
        base.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.wheat_seeds, 2));
        base.setInventorySlotContents(machine.getOutputSlot(), new ItemStack(Blocks.cobblestone, 64));
        long energy = base.getStoredEU();
        require(machine.checkRecipe() != 2 && inputCount() == 2 && base.getStoredEU() == energy,
            "incompatible full output leaves seeds and energy untouched");
        ItemStack full = CropsNHCrops.Wheat.getSeedItem(SeedStats.DEFAULT_ANALYZED);
        full.stackSize = full.getMaxStackSize();
        base.setInventorySlotContents(machine.getOutputSlot(), full);
        require(machine.checkRecipe() != 2 && inputCount() == 2 && base.getStoredEU() == energy,
            "full matching output also preserves inputs");
        reset();
        base.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.stick));
        require(machine.checkRecipe() != 2 && inputCount() == 1, "unregistered input has no conversion");
        base.setInventorySlotContents(machine.getInputSlot(),
            CropsNHCrops.Wheat.getSeedItem(SeedStats.DEFAULT_ANALYZED));
        require(machine.checkRecipe() != 2 && inputCount() == 1, "native CropsNH seeds are not converted again");
        base.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.wheat_seeds));
        base.setStoredEU(0);
        base.enableWorking();
        machine.onPostTick(base, 600);
        require(inputCount() == 1 && machine.mMaxProgresstime == 0, "unpowered machine cannot consume seeds");
        base.disableWorking();
    }

    private void verifyPersistence(World world) {
        reset();
        base.setStoredEU(base.getEUCapacity());
        base.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.carrot, 2));
        require(machine.checkRecipe() == 2, "carrot processing starts before tile save");
        int duration = machine.mMaxProgresstime;
        ItemStack expected = machine.mOutputItems[0].copy();
        NBTTagCompound tag = new NBTTagCompound();
        base.writeToNBT(tag);
        world.removeTileEntity(4, 10, 4);
        base = new BaseMetaTileEntity();
        base.setWorldObj(world);
        base.readFromNBT(tag);
        world.setTileEntity(4, 10, 4, base);
        machine = (MTEBasicMachine) base.getMetaTileEntity();
        world.markBlockForUpdate(4, 10, 4);
        require(machine.mMaxProgresstime == duration && inputCount() == 1
            && GTUtility.areStacksEqual(machine.mOutputItems[0], expected)
            && !base.isAllowedToWork(), "NBT reload retains pending output, input, duration and disabled switch");
        finishRecipe();
        require(
            outputCount() == 1 && inputCount() == 1
                && GTUtility.areStacksEqual(base.getStackInSlot(machine.getOutputSlot()), expected),
            "saved in-flight recipe resumes exactly once with original seed NBT");
        NBTTagCompound completed = new NBTTagCompound();
        base.writeToNBT(completed);
        BaseMetaTileEntity copy = new BaseMetaTileEntity();
        copy.setWorldObj(world);
        copy.readFromNBT(completed);
        require(GTUtility.areStacksEqual(copy.getStackInSlot(machine.getOutputSlot()), expected),
            "completed seed inventory survives a second NBT reload");
    }

    private void verifySidedInventory() {
        reset();
        ItemStack input = new ItemStack(Items.wheat_seeds);
        require(machine.allowPutStack(base, machine.getInputSlot(), ForgeDirection.UP, input),
            "top automation can insert a registered alternate seed");
        require(!machine.allowPutStack(base, machine.getOutputSlot(), ForgeDirection.UP, input),
            "automation cannot insert into output");
        require(
            machine.allowPullStack(base, machine.getOutputSlot(), ForgeDirection.SOUTH, input)
                && !machine.allowPullStack(base, machine.getInputSlot(), ForgeDirection.SOUTH, input),
            "sided extraction exposes output without extracting pending inputs");
    }

    private void beginAutomation(World world) {
        reset();
        base.setStoredEU(base.getEUCapacity());
        world.setBlock(4, 11, 4, Blocks.hopper, 0, 3);
        hopper = (TileEntityHopper) world.getTileEntity(4, 11, 4);
        hopper.setInventorySlotContents(0, new ItemStack(Items.wheat_seeds, 3));
        world.setBlock(4, 10, 5, Blocks.chest, 0, 3);
        chest = (IInventory) world.getTileEntity(4, 10, 5);
        machine.mItemTransfer = true;
        base.enableWorking();
        automationDeadline = ticks + 120 + 6 * Math.max(1, Config.getModifiedRecipeDuration(20));
    }

    private void verifyCrafting(World world) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        ItemStack circuit = GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 1);
        ItemStack wire = GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Tin, 1);
        ItemStack[] inputs = { circuit, CropsNHItemList.cropSticks.get(1), circuit, ItemList.Conveyor_Module_LV.get(1),
            ItemList.Hull_LV.get(1), ItemList.Conveyor_Module_LV.get(1), wire, ItemList.Electric_Motor_LV.get(1),
            wire };
        for (int i = 0; i < inputs.length; i++) grid.setInventorySlotContents(i, inputs[i].copy());
        ItemStack crafted = CraftingManager.getInstance().findMatchingRecipe(grid, world);
        require(GTUtility.areStacksEqual(crafted, GTNGItemList.SeedConverter.get(1)) && crafted.stackSize == 1,
            "registered LV crafting grid produces one seed converter");
    }

    private void finishRecipe() {
        int duration = machine.mMaxProgresstime;
        for (int i = 0; i < duration; i++) machine.onPostTick(base, 601 + i);
        require(machine.mMaxProgresstime == 0 && machine.mOutputItems[0] == null,
            "native processing clears its pending recipe after completion");
    }

    private void reset() {
        base.disableWorking();
        Arrays.fill(machine.mInventory, null);
        Arrays.fill(machine.mOutputItems, null);
        machine.mProgresstime = machine.mMaxProgresstime = machine.mEUt = 0;
        machine.mItemTransfer = false;
    }

    private int inputCount() {
        ItemStack stack = base.getStackInSlot(machine.getInputSlot());
        return stack == null ? 0 : stack.stackSize;
    }

    private int outputCount() {
        ItemStack stack = base.getStackInSlot(machine.getOutputSlot());
        return stack == null ? 0 : stack.stackSize;
    }

    private static int count(IInventory inventory, ItemStack expected) {
        int count = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (GTUtility.areStacksEqual(stack, expected)) count += stack.stackSize;
        }
        return count;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !opened || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (visualStage == 0) {
                if (!(mc.currentScreen instanceof GuiContainer) || ++frames < 60) return;
                capture("seed-converter-gui");
                verifyScreenshot("seed-converter-gui");
                GTMod.proxy.reloadNEICache();
                require(GuiCraftingRecipe.openRecipeGui(recipeMapName),
                    "NEI opens the registered conversion recipe page");
                frames = 0;
                visualStage = 1;
            } else if (visualStage == 1) {
                if (++frames < 60) return;
                require(mc.currentScreen instanceof GuiCraftingRecipe, "native NEI conversion screen is visible");
                capture("seed-converter-nei");
                mc.thePlayer.closeScreen();
                worldRequested = true;
                frames = 0;
                visualStage = 2;
            } else {
                if (!worldPositioned || ++frames < 60) return;
                capture("seed-converter-world");
                Files.write(new File(outputDir(), "result.txt").toPath(),
                    ("PASS\n" + checks).getBytes(StandardCharsets.UTF_8));
                System.out.println("SEED_CONVERTER_QA: PASS");
                finished = true;
                mc.shutdown();
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void verifyScreenshot(String name) throws Exception {
        BufferedImage screenshot = ImageIO.read(new File(outputDir(), "screenshots/" + name + ".png"));
        require(screenshot != null && screenshot.getWidth() >= 800, "native machine GUI screenshot saved");
        int missing = 0;
        for (int y = 150; y < screenshot.getHeight() - 150; y++) {
            for (int x = screenshot.getWidth() / 2 - 176; x < screenshot.getWidth() / 2 + 176; x++) {
                int rgb = screenshot.getRGB(x, y);
                if ((rgb >>> 16 & 255) >= 200 && (rgb >>> 8 & 255) <= 50 && (rgb & 255) >= 200) missing++;
            }
        }
        require(missing <= 16, "machine GUI has no missing magenta textures");
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(outputDir(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File outputDir() {
        return new File(System.getProperty("gtng.seedconverter.qa.output", "."));
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("SEED_CONVERTER_QA: " + message);
        checks.append(message).append('\n');
        System.out.println("SEED_CONVERTER_QA: " + message);
    }

    private void fail(Throwable failure) {
        if (finished) return;
        failure.printStackTrace();
        try {
            Files.write(new File(outputDir(), "result.txt").toPath(),
                ("FAIL: " + failure + "\n" + checks).getBytes(StandardCharsets.UTF_8));
        } catch (Exception writeFailure) {
            writeFailure.printStackTrace();
        }
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }
}
