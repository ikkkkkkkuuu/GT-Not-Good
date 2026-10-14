package com.xyp.gtnotgood.qa;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.IIcon;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.gtnewhorizons.modularui.common.fluid.FluidStackTank;
import com.xyp.gtnotgood.common.machines.basic.SteamProcessingMachine;
import com.xyp.gtnotgood.config.Config;
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
import gregtech.api.enums.Textures;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.gui.modularui.widget.SteamGaugeWidget;

@Mod(
    modid = "steamchemicalqa",
    name = "Steam chemical QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class SteamChemicalClientChecks {

    private static final Item[] inputItems = { Items.paper, Items.blaze_powder, Items.sugar, Items.feather,
        Items.leather, Items.bone };
    private static final Item[] outputItems = { Items.gold_nugget, Items.glowstone_dust, Items.redstone, Items.quartz,
        Items.flint, Items.apple };

    private boolean started;
    private volatile boolean finished, opened, worldRequested, worldPositioned, autoOnChecked, autoOffChecked;
    private volatile int buttonRequest;
    private int ticks, stage, frames, visualStage, outputReadyTick, buttonStartTick;
    private BaseMetaTileEntity base, nativeBase;
    private SteamProcessingMachine machine, nativeMachine;
    private GTRecipe nativeRecipe;
    private final BaseMetaTileEntity[] extraBases = new BaseMetaTileEntity[2];
    private final SteamProcessingMachine[] extraMachines = new SteamProcessingMachine[2];
    private final GTRecipe[] extraRecipes = new GTRecipe[2];
    private final boolean[] extraCompleted = new boolean[2];
    private OutputContainer outputContainer;
    private ForgeDirection originalOutputSide;
    private boolean originalFullFluidOutput;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModularUIConfig.guiDebugMode = false;
        GameRegistry.registerTileEntity(OutputContainer.class, "steamchemicalqa.output");
        if (Boolean.getBoolean("gtng.steamchemical.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.currentScreen instanceof GuiMainMenu) {
            started = true;
            mc.launchIntegratedServer("steam-chemical-" + System.currentTimeMillis(), "Steam chemical QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            require(++ticks <= 3600, "client QA timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            World world = player.worldObj;
            if (ticks == 1) {
                base = place(world, player, 4);
                nativeBase = place(world, player, 7);
                machine = (SteamProcessingMachine) base.getMetaTileEntity();
                nativeMachine = (SteamProcessingMachine) nativeBase.getMetaTileEntity();
                extraBases[0] = place(world, player, 10, GTNGMachineID.SteamCentrifuge.id);
                extraBases[1] = place(world, player, 13, GTNGMachineID.SteamElectrolyzer.id);
                for (int i = 0; i < extraMachines.length; i++) {
                    extraMachines[i] = (SteamProcessingMachine) extraBases[i].getMetaTileEntity();
                }
                player.playerNetServerHandler.setPlayerLocation(4.5, 11, 2, 0, 15);
                player.capabilities.isFlying = true;
            }
            if (ticks == 40) {
                nativeRecipe = findNativeRecipe();
                verifyAdditionalMachines();
                verifyCrafting(world);
                addFixture(false, 32);
                addFixture(true, 128);
                verifyFluidRouting();
                verifyRejectedRecipes();
                verifyIdlePersistence(world);
                seedFixture(2, 750, 1500);
                base.setStoredSteam(0);
                base.enableWorking();
            }
            if (ticks == 80) {
                require(
                    machine.mMaxProgresstime == 0 && countInput(Items.paper) == 2
                        && fluidAmount(machine.getFluidInputTanks(), water(1)) == 750,
                    "no steam leaves idle inputs intact");
                require(base.fill(ForgeDirection.UP, Materials.Steam.getGas(32000), true) == 32000,
                    "pipe fills the independent steam store");
                require(
                    fluidAmount(machine.getFluidInputTanks(), water(1)) == 750
                        && fluidAmount(machine.getFluidInputTanks(), nitrogen(1)) == 1500,
                    "steam never replaces either reaction fluid");
                base.disableWorking();
                require(machine.checkRecipe() == 2, "LV multi-fluid fixture starts");
                require(machine.mMaxProgresstime == 40 && machine.mEUt == 32,
                    "bronze doubles the 20 tick recipe and retains 32 EU steam cost");
                require(base.getStoredEU() == 0 && !machine.isElectric() && !machine.isEnetInput(), "no EU supply");
                stage = 1;
            }
            if (ticks == 90) verifyPendingPersistence(world);
            if (stage == 1 && ticks > 90 && machine.mMaxProgresstime == 0) {
                verifyFixtureOutputs();
                for (Item item : inputItems) require(countInput(item) == 1, "exact one-batch item consumption " + item);
                require(
                    fluidAmount(machine.getFluidInputTanks(), water(1)) == 500
                        && fluidAmount(machine.getFluidInputTanks(), nitrogen(1)) == 1000,
                    "both reaction fluid remainders survive processing");
                require(base.getStoredSteam() == 16000 - 32 * 40, "exact steam consumption across tile reload");
                verifyCompletedPersistence(world);
                originalOutputSide = machine.getAutoOutputSide();
                require(originalOutputSide == ForgeDirection.DOWN, "default output side keeps the steam vent clear");
                world.setBlock(4, 9, 4, Blocks.chest, 0, 3);
                outputContainer = new OutputContainer();
                world.setTileEntity(4, 9, 4, outputContainer);
                originalFullFluidOutput = Config.recipeSpeedFullFluidOutput;
                Config.recipeSpeedFullFluidOutput = false;
                outputReadyTick = ticks;
                seedNativeRecipe();
                for (int i = 0; i < extraMachines.length; i++)
                    seedNativeRecipe(extraBases[i], extraMachines[i], extraRecipes[i]);
                stage = 2;
            }
            for (int i = 0; i < extraMachines.length; i++) {
                if (stage == 2 && !extraCompleted[i] && extraMachines[i].mMaxProgresstime == 0) {
                    verifyNativeOutputs(extraBases[i], extraMachines[i], extraRecipes[i]);
                    extraCompleted[i] = true;
                }
            }
            if (
                stage == 2 && nativeMachine.mMaxProgresstime == 0
                    && extraCompleted[0]
                    && extraCompleted[1]
                    && ticks - outputReadyTick >= 40
            ) {
                verifyNativeOutputs();
                require(nativeBase.getStoredEU() == 0, "native chemical recipe finishes using steam alone");
                verifyFixtureOutputs();
                require(!machine.mItemTransfer && !machine.mFluidTransfer && outputContainer.isEmpty(),
                    "disabled automatic outputs retain all items and both fluids");
                machine.onRightclick(base, player);
                opened = true;
                stage = 3;
                System.out.println("STEAM_CHEMICAL_QA: processing, rejection and persistence checks PASS");
            }
            if (stage == 3 && buttonRequest == 1 && machine.mItemTransfer && machine.mFluidTransfer) {
                if (buttonStartTick == 0) buttonStartTick = ticks;
                require(ticks - buttonStartTick < 300, "automatic output timeout");
                if (
                    ticks - buttonStartTick >= 40 && fluidAmount(machine.getFluidOutputTanks(), lava(1)) == 0
                        && fluidAmount(machine.getFluidOutputTanks(), oxygen(1)) == 0
                        && !autoOnChecked
                ) {
                    verifyAutoOutput();
                    autoOnChecked = true;
                }
            }
            if (
                stage == 3 && buttonRequest == 2
                    && !machine.mItemTransfer
                    && !machine.mFluidTransfer
                    && machine.getAutoOutputSide() != originalOutputSide
                    && !autoOffChecked
            ) {
                require(
                    machine.getAutoOutputSide() != base.getFrontFacing()
                        && machine.getAutoOutputSide() != machine.mMainFacing,
                    "Shift click changes the output side without selecting the vent or machine face");
                restoreFixtureOutputs();
                Config.recipeSpeedFullFluidOutput = originalFullFluidOutput;
                autoOffChecked = true;
            }
            if (stage == 3 && worldRequested) {
                player.closeScreen();
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.playerNetServerHandler.setPlayerLocation(4.5, 10, 1.5, 0, 25);
                worldPositioned = true;
                stage = 4;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x) {
        return place(world, player, x, GTNGMachineID.SteamChemicalReactor.id);
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x, int machineId) {
        world.setBlock(x, 10, 4, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 4);
        tile.setInitialValuesAsNBT(null, (short) machineId);
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.setFrontFacing(ForgeDirection.NORTH);
        ((SteamProcessingMachine) tile.getMetaTileEntity()).setMainFacing(ForgeDirection.NORTH);
        tile.disableWorking();
        return tile;
    }

    private void verifyFluidRouting() {
        reset();
        require(machine.mInputSlotCount == 6 && machine.mOutputItems.length == 6, "six item inputs and outputs");
        require(
            machine.getFluidInputTanks().length == 6 && machine.getFluidOutputTanks().length == 6
                && machine.getCapacity() == 16000
                && machine.getTankInfo(ForgeDirection.UP).length == 13,
            "six input tanks, six output tanks and the independent steam tank");
        require(
            base.fill(ForgeDirection.UP, water(1000), true) == 1000
                && base.fill(ForgeDirection.UP, nitrogen(500), true) == 500
                && base.fill(ForgeDirection.UP, water(250), true) == 250,
            "pipe chooses an existing fluid before an empty slot");
        require(sameFluid(machine.getFluidInputTanks()[0].getFluid(), water(1250))
            && sameFluid(machine.getFluidInputTanks()[1].getFluid(), nitrogen(500))
            && machine.getFluidInputTanks()[2].getFluid() == null, "different input fluids occupy separate slots");
        require(
            base.fill(ForgeDirection.UP, water(17000), false) == 17000
                && fluidAmount(machine.getFluidInputTanks(), water(1)) == 1250,
            "simulated pipe fill leaves every tank unchanged");
        reset();
        setTank(machine.getFluidOutputTanks()[0], lava(16000));
        setTank(machine.getFluidOutputTanks()[1], oxygen(700));
        setTank(machine.getFluidOutputTanks()[2], lava(1000));
        FluidStack simulated = base.drain(ForgeDirection.UP, lava(16500), false);
        require(sameFluid(simulated, lava(16500)) && fluidAmount(machine.getFluidOutputTanks(), lava(1)) == 17000,
            "simulated drain combines matching output slots without mutation");
        require(
            sameFluid(base.drain(ForgeDirection.UP, lava(16500), true), lava(16500))
                && fluidAmount(machine.getFluidOutputTanks(), lava(1)) == 500
                && fluidAmount(machine.getFluidOutputTanks(), oxygen(1)) == 700,
            "requested fluid drains across matching slots without touching another fluid");
        require(
            sameFluid(machine.drain(ForgeDirection.UP, oxygen(1), 100, false), oxygen(100))
                && fluidAmount(machine.getFluidOutputTanks(), oxygen(1)) == 700,
            "typed drain honors its explicit amount and simulation flag");
        require(
            sameFluid(machine.drain(ForgeDirection.UP, oxygen(1), 100, true), oxygen(100))
                && fluidAmount(machine.getFluidOutputTanks(), oxygen(1)) == 600,
            "typed drain reaches a later output tank");
        reset();
    }

    private void verifyRejectedRecipes() {
        seedFixture(1, 250, 0);
        require(
            machine.checkRecipe() != 2 && countInput(Items.paper) == 1
                && countInput(Items.blaze_powder) == 1
                && fluidAmount(machine.getFluidInputTanks(), water(1)) == 250,
            "missing second reaction fluid cannot consume items or the first fluid");
        seedFixture(1, 250, 500);
        for (int i = 0; i < 6; i++)
            base.setInventorySlotContents(machine.getOutputSlot() + i, new ItemStack(Items.stick, 64));
        require(
            machine.checkRecipe() != 2 && countInput(Items.paper) == 1
                && fluidAmount(machine.getFluidInputTanks(), nitrogen(1)) == 500,
            "full item outputs cannot consume any inputs");
        seedFixture(1, 250, 500);
        for (FluidStackTank tank : machine.getFluidOutputTanks()) setTank(tank, lava(16000));
        require(
            machine.checkRecipe() != 2 && countInput(Items.blaze_powder) == 1
                && fluidAmount(machine.getFluidInputTanks(), water(1)) == 250,
            "blocked second fluid output cannot consume inputs or void a fluid");
        reset();
        base.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.slime_ball));
        base.setInventorySlotContents(machine.getInputSlot() + 1, new ItemStack(Items.ender_pearl));
        setTank(machine.getFluidInputTanks()[0], water(250));
        setTank(machine.getFluidInputTanks()[1], nitrogen(500));
        require(
            machine.checkRecipe() != 2 && countInput(Items.slime_ball) == 1
                && fluidAmount(machine.getFluidInputTanks(), nitrogen(1)) == 500,
            "MV recipe is rejected before input consumption");
        reset();
    }

    private void verifyIdlePersistence(World world) {
        seedFixture(2, 750, 1500);
        base.setStoredSteam(6789);
        for (int i = 0; i < 6; i++) {
            base.setInventorySlotContents(machine.getOutputSlot() + i, new ItemStack(outputItems[i], i + 2));
            setTank(machine.getFluidInputTanks()[i], water(100 + i));
            setTank(machine.getFluidOutputTanks()[i], lava(200 + i));
        }
        reload(world);
        for (int i = 0; i < 6; i++) {
            require(countInput(inputItems[i]) == 2 && countOutput(outputItems[i]) == i + 2,
                "tile NBT retains item input and output " + i);
            require(
                sameFluid(machine.getFluidInputTanks()[i].getFluid(), water(100 + i))
                    && sameFluid(machine.getFluidOutputTanks()[i].getFluid(), lava(200 + i)),
                "tile NBT retains reaction input and output tank " + i);
        }
        require(base.getStoredSteam() == 6789, "tile NBT retains separate steam storage");
    }

    private void verifyPendingPersistence(World world) {
        int progress = machine.mProgresstime;
        long steam = base.getStoredSteam();
        require(progress > 0 && progress < 40, "fixture is running before persistence check");
        reload(world);
        require(machine.mProgresstime == progress && machine.mMaxProgresstime == 40
            && machine.mEUt == 32
            && base.getStoredSteam() == steam, "running progress and steam round trip exactly");
        for (int i = 0; i < 6; i++)
            require(machine.mOutputItems[i].stackSize == i + 2, "pending item output persists " + i);
    }

    private void verifyCompletedPersistence(World world) {
        long steam = base.getStoredSteam();
        reload(world);
        verifyFixtureOutputs();
        require(
            fluidAmount(machine.getFluidInputTanks(), water(1)) == 500
                && fluidAmount(machine.getFluidInputTanks(), nitrogen(1)) == 1000
                && base.getStoredSteam() == steam
                && machine.mMaxProgresstime == 0,
            "completed tile reload preserves remainders and cannot duplicate the batch");
    }

    private void verifyFixtureOutputs() {
        for (int i = 0; i < 6; i++) require(countOutput(outputItems[i]) == i + 2, "exact item output " + i);
        require(
            fluidAmount(machine.getFluidOutputTanks(), lava(1)) == 500
                && fluidAmount(machine.getFluidOutputTanks(), oxygen(1)) == 700,
            "both exact fluid outputs survive real ticks and tile reload");
    }

    private void reload(World world) {
        NBTTagCompound tag = new NBTTagCompound();
        base.writeToNBT(tag);
        world.removeTileEntity(4, 10, 4);
        base = new BaseMetaTileEntity();
        base.setWorldObj(world);
        base.readFromNBT(tag);
        world.setTileEntity(4, 10, 4, base);
        machine = (SteamProcessingMachine) base.getMetaTileEntity();
        world.markBlockForUpdate(4, 10, 4);
    }

    private void seedFixture(int count, int waterAmount, int nitrogenAmount) {
        reset();
        for (int i = 0; i < 6; i++)
            base.setInventorySlotContents(machine.getInputSlot() + i, new ItemStack(inputItems[i], count));
        if (waterAmount > 0) setTank(machine.getFluidInputTanks()[0], water(waterAmount));
        if (nitrogenAmount > 0) setTank(machine.getFluidInputTanks()[1], nitrogen(nitrogenAmount));
    }

    private void reset() {
        for (int i = 0; i < machine.getSizeInventory(); i++) base.setInventorySlotContents(i, null);
        for (FluidStackTank tank : machine.getFluidInputTanks()) setTank(tank, null);
        for (FluidStackTank tank : machine.getFluidOutputTanks()) setTank(tank, null);
        Arrays.fill(machine.mOutputItems, null);
        machine.mOutputFluid = null;
        machine.mProgresstime = machine.mMaxProgresstime = machine.mEUt = 0;
    }

    private static void addFixture(boolean mv, int eut) {
        ItemStack[] inputs = mv ? new ItemStack[] { new ItemStack(Items.slime_ball), new ItemStack(Items.ender_pearl) }
            : Arrays.stream(inputItems).map(ItemStack::new).toArray(ItemStack[]::new);
        ItemStack[] outputs = new ItemStack[6];
        for (int i = 0; i < 6; i++) outputs[i] = new ItemStack(outputItems[i], i + 2);
        GTRecipe recipe = new GTRecipe(false, inputs, outputs, null, null, null, null, null,
            new FluidStack[] { water(250), nitrogen(500) }, new FluidStack[] { lava(500), oxygen(700) }, 20, eut, 0);
        require(RecipeMaps.multiblockChemicalReactorRecipes.addRecipe(recipe) != null, "QA recipe registration");
        recipe.mDuration = 20;
    }

    private static GTRecipe findNativeRecipe() {
        return findNativeRecipe(RecipeMaps.multiblockChemicalReactorRecipes, true);
    }

    private static GTRecipe findNativeRecipe(RecipeMap<?> map, boolean chemical) {
        List<GTRecipe> recipes = new ArrayList<>(map.getAllRecipes());
        recipes.sort(Comparator.comparingInt(recipe -> recipe.mDuration));
        for (GTRecipe recipe : recipes) {
            if (
                !recipe.mEnabled || recipe.mFakeRecipe
                    || recipe.mEUt <= 0
                    || recipe.mEUt > 32
                    || recipe.mDuration <= 0
                    || recipe.mDuration > 1000
                    || (long) recipe.mDuration * 2 * recipe.mEUt > 14000
                    || recipe.mSpecialValue != 0
                    || recipe.mSpecialItems != null
                    || recipe.mInputs.length > 6
                    || recipe.mOutputs.length > 6
                    || chemical && recipe.mFluidInputs.length < 2
                    || recipe.mFluidInputs.length > 6
                    || chemical && recipe.mFluidOutputs.length == 0
                    || recipe.mFluidInputs.length + recipe.mFluidOutputs.length == 0
                    || recipe.mFluidOutputs.length > 6
            ) continue;
            boolean usable = true;
            for (int i = 0; i < recipe.mInputs.length; i++) usable &= recipe.getInputChance(i) == 10000;
            for (int i = 0; i < recipe.mOutputs.length; i++) usable &= recipe.getOutputChance(i) == 10000;
            for (int i = 0; i < recipe.mFluidInputs.length; i++) {
                usable &= recipe.getFluidInputChance(i) == 10000 && recipe.mFluidInputs[i].amount <= 16000
                    && !GTModHandler.isAnySteam(recipe.mFluidInputs[i]);
            }
            for (int i = 0; i < recipe.mFluidOutputs.length; i++) {
                usable &= recipe.getFluidOutputChance(i) == 10000 && recipe.mFluidOutputs[i].amount <= 16000;
            }
            if (!usable) continue;
            GTRecipe found = map.findRecipeQuery().items(recipe.mInputs).fluids(recipe.mFluidInputs).voltage(32).find();
            if (found != recipe) continue;
            System.out.println("STEAM_CHEMICAL_QA: native cell-free " + map.unlocalizedName
                + " recipe "
                + Arrays.toString(recipe.mInputs)
                + " + "
                + Arrays.toString(recipe.mFluidInputs)
                + " -> "
                + Arrays.toString(recipe.mFluidOutputs));
            return recipe;
        }
        throw new AssertionError("No compatible native LV cell-free fluid recipe in " + map.unlocalizedName);
    }

    private void seedNativeRecipe() {
        seedNativeRecipe(nativeBase, nativeMachine, nativeRecipe);
    }

    private static void seedNativeRecipe(BaseMetaTileEntity tile, SteamProcessingMachine processingMachine,
        GTRecipe recipe) {
        for (int i = 0; i < recipe.mInputs.length; i++) {
            tile.setInventorySlotContents(processingMachine.getInputSlot() + i, recipe.mInputs[i].copy());
        }
        for (FluidStack fluid : recipe.mFluidInputs)
            require(tile.fill(ForgeDirection.UP, fluid.copy(), true) == fluid.amount,
                "native fluid input arrives through the pipe API");
        tile.fill(ForgeDirection.UP, Materials.Steam.getGas(32000), true);
        require(processingMachine.checkRecipe() == 2,
            "registered native cell-free recipe starts: " + processingMachine.getRecipeMap().unlocalizedName);
        require(processingMachine.mMaxProgresstime == recipe.mDuration * 2 && processingMachine.mEUt == recipe.mEUt,
            "native bronze duration doubles without voltage overclocking");
    }

    private void verifyNativeOutputs() {
        verifyNativeOutputs(nativeBase, nativeMachine, nativeRecipe);
    }

    private static void verifyNativeOutputs(BaseMetaTileEntity tile, SteamProcessingMachine processingMachine,
        GTRecipe recipe) {
        for (ItemStack output : recipe.mOutputs) {
            int expected = 0;
            for (ItemStack other : recipe.mOutputs)
                if (GTUtility.areStacksEqual(output, other)) expected += other.stackSize;
            require(count(tile, processingMachine.getOutputSlot(), 6, output) == expected,
                "native recipe exact item output " + output.getDisplayName());
        }
        for (FluidStack output : recipe.mFluidOutputs) {
            int expected = 0;
            for (FluidStack other : recipe.mFluidOutputs) if (output.isFluidEqual(other)) expected += other.amount;
            require(fluidAmount(processingMachine.getFluidOutputTanks(), output) == expected,
                "native recipe exact fluid output " + output.getLocalizedName());
        }
        require(tile.getStoredEU() == 0 && tile.getStoredSteam() < 16000,
            "native recipe finishes with steam only: " + processingMachine.getRecipeMap().unlocalizedName);
    }

    private void verifyAdditionalMachines() {
        RecipeMap<?>[] maps = { RecipeMaps.centrifugeNonCellRecipes, RecipeMaps.electrolyzerNonCellRecipes };
        GTNGItemList[] items = { GTNGItemList.SteamCentrifuge, GTNGItemList.SteamElectrolyzer };
        for (int i = 0; i < extraMachines.length; i++) {
            SteamProcessingMachine processingMachine = extraMachines[i];
            require(
                processingMachine.getRecipeMap() == maps[i]
                    && items[i].get(1).getItemDamage() == extraBases[i].getMetaTileID(),
                "registered steam machine selects its cell-free recipe map: " + items[i]);
            require(processingMachine.mInputSlotCount == 6 && processingMachine.mOutputItems.length == 6
                && processingMachine.getFluidInputTanks().length == 6
                && processingMachine.getFluidOutputTanks().length == 6
                && !processingMachine.isElectric()
                && processingMachine.maxEUInput() == 0, "shared six-slot steam capabilities: " + items[i]);
            extraRecipes[i] = findNativeRecipe(maps[i], false);
            GTRecipe fixtureRecipe = new GTRecipe(false, new ItemStack[] { new ItemStack(Items.nether_star) },
                new ItemStack[] { new ItemStack(Items.diamond) }, null, null, null, null, null,
                new FluidStack[] { water(1) }, new FluidStack[0], 20, 32, 0);
            require(maps[i].addRecipe(fixtureRecipe) != null, "32 EU fixture registered: " + items[i]);
            fixtureRecipe.mDuration = 20;
            extraBases[i].setInventorySlotContents(processingMachine.getInputSlot(), new ItemStack(Items.nether_star));
            setTank(processingMachine.getFluidInputTanks()[0], water(1));
            require(processingMachine.checkRecipe() == 2 && processingMachine.mEUt == 32
                && processingMachine.mMaxProgresstime == 40, "32 EU ceiling and doubled duration: " + items[i]);
            for (int slot = 0; slot < processingMachine.getSizeInventory(); slot++)
                extraBases[i].setInventorySlotContents(slot, null);
            for (FluidStackTank tank : processingMachine.getFluidInputTanks()) setTank(tank, null);
            Arrays.fill(processingMachine.mOutputItems, null);
            processingMachine.mProgresstime = processingMachine.mMaxProgresstime = processingMachine.mEUt = 0;
        }
    }

    private void verifyAutoOutput() {
        for (int i = 0; i < outputItems.length; i++) {
            int found = 0;
            for (int slot = 0; slot < outputContainer.getSizeInventory(); slot++) {
                ItemStack stack = outputContainer.getStackInSlot(slot);
                if (stack != null && stack.getItem() == outputItems[i]) found += stack.stackSize;
            }
            require(found == i + 2 && countOutput(outputItems[i]) == 0,
                "enabled automatic item output moves exact stack " + i);
        }
        require(
            outputContainer.amount(lava(1)) == 500 && outputContainer.amount(oxygen(1)) == 700
                && fluidAmount(machine.getFluidOutputTanks(), lava(1)) == 0
                && fluidAmount(machine.getFluidOutputTanks(), oxygen(1)) == 0,
            "enabled automatic fluid output delivers both fluids to the same real neighboring container");
        require(outputContainer.largestRequest <= 1000, "automatic fluid output respects the configured 1000 L budget");
        require(outputContainer.limitedAcceptance,
            "partial receiver acceptance never voids the remaining source fluid");
        require(!machine.needsSteamVenting(), "output container never obstructs the steam vent");
    }

    private void restoreFixtureOutputs() {
        for (int i = 0; i < outputItems.length; i++)
            base.setInventorySlotContents(machine.getOutputSlot() + i, new ItemStack(outputItems[i], i + 2));
        setTank(machine.getFluidOutputTanks()[0], lava(500));
        setTank(machine.getFluidOutputTanks()[1], oxygen(700));
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
        ItemStack[] ingredients = { plate, rotor, plate, new ItemStack(Blocks.glass), ItemList.Hull_Bronze.get(1),
            new ItemStack(Blocks.glass), pipe, pipe, pipe };
        for (int i = 0; i < ingredients.length; i++) grid.setInventorySlotContents(i, ingredients[i].copy());
        ItemStack output = CraftingManager.getInstance().findMatchingRecipe(grid, world);
        require(GTUtility.areStacksEqual(output, GTNGItemList.SteamChemicalReactor.get(1)) && output.stackSize == 1,
            "registered bronze crafting grid produces the machine");
    }

    private int countInput(Item item) {
        return count(base, machine.getInputSlot(), 6, new ItemStack(item));
    }

    private int countOutput(Item item) {
        return count(base, machine.getOutputSlot(), 6, new ItemStack(item));
    }

    private static int count(BaseMetaTileEntity tile, int start, int size, ItemStack expected) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            ItemStack stack = tile.getStackInSlot(start + i);
            if (GTUtility.areStacksEqual(stack, expected)) count += stack.stackSize;
        }
        return count;
    }

    private static int fluidAmount(FluidStackTank[] tanks, FluidStack expected) {
        int amount = 0;
        for (FluidStackTank tank : tanks) if (tank.getFluid() != null && tank.getFluid().isFluidEqual(expected)) {
            amount += tank.getFluid().amount;
        }
        return amount;
    }

    private static boolean sameFluid(FluidStack actual, FluidStack expected) {
        return actual != null && actual.isFluidEqual(expected) && actual.amount == expected.amount;
    }

    private static void setTank(FluidStackTank tank, FluidStack fluid) {
        tank.drain(Integer.MAX_VALUE, true);
        if (fluid != null) require(tank.fill(fluid, true) == fluid.amount, "QA tank fixture fills completely");
    }

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.WATER, amount);
    }

    private static FluidStack nitrogen(int amount) {
        return Materials.Nitrogen.getGas(amount);
    }

    private static FluidStack lava(int amount) {
        return new FluidStack(FluidRegistry.LAVA, amount);
    }

    private static FluidStack oxygen(int amount) {
        return Materials.Oxygen.getGas(amount);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !opened || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (visualStage == 3) {
                if (!worldPositioned) return;
                if (++frames < 60) return;
                ScreenShotHelper.saveScreenshot(outputDir(), "steam-chemical-reactor-world.png", mc.displayWidth,
                    mc.displayHeight, mc.getFramebuffer());
                Files.write(new File(outputDir(), "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                System.out.println(
                    "STEAM_CHEMICAL_QA: PASS native cell-free chemistry, six item I/O, multi-fluid I/O, NBT and GUI");
                finished = true;
                mc.shutdown();
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper)) return;
            ModularPanel panel = ((GuiContainerWrapper) mc.currentScreen).getScreen().getMainPanel();
            ToggleButton fluidButton = findButton(panel, "auto_fluid_output");
            ToggleButton itemButton = findButton(panel, "auto_item_output");
            if (visualStage == 1) {
                if (!autoOnChecked || ++frames < 20) return;
                require(fluidButton.isValueSelected() && itemButton.isValueSelected(),
                    "both automatic output buttons synchronize enabled states");
                fluidButton.onMousePressed(0);
                itemButton.onMousePressed(0);
                shiftClick(fluidButton);
                buttonRequest = 2;
                visualStage = 2;
                frames = 0;
                return;
            }
            if (visualStage == 2) {
                if (!autoOffChecked || ++frames < 30) return;
                require(!fluidButton.isValueSelected() && !itemButton.isValueSelected(),
                    "both automatic output buttons synchronize disabled states");
                ScreenShotHelper.saveScreenshot(outputDir(), "steam-chemical-reactor.png", mc.displayWidth,
                    mc.displayHeight, mc.getFramebuffer());
                verifyTextures(panel, mc);
                mc.thePlayer.closeScreen();
                worldRequested = true;
                visualStage = 3;
                frames = 0;
                return;
            }
            if (++frames < 60) return;
            List<FluidSlot> fluids = collect(panel, FluidSlot.class);
            require(fluids.size() == 12, "native GUI has all twelve reaction fluid slots");
            for (FluidStack expected : new FluidStack[] { water(500), nitrogen(1000), lava(500), oxygen(700) }) {
                require(fluids.stream().anyMatch(slot -> sameFluid(slot.getFluidStack(), expected)),
                    "reaction fluid synchronizes into GUI: " + expected.getLocalizedName());
            }
            Set<Integer> indices = new HashSet<>();
            for (ItemSlot slot : collect(panel, ItemSlot.class)) {
                if (!ModularSlot.isPlayerSlot(slot.getSlot())) indices.add(slot.getSlot().getSlotIndex());
            }
            for (int i = 0; i < 6; i++)
                require(indices.contains(machine.getInputSlot() + i) && indices.contains(machine.getOutputSlot() + i),
                    "native GUI retains recipe item input/output " + i);
            require(collect(panel, SteamGaugeWidget.class).size() == 1, "native steam gauge is visible");
            require(
                collect(panel, ToggleButton.class).stream()
                    .filter(button -> "auto_fluid_output".equals(button.getName())
                        || "auto_item_output".equals(button.getName()))
                    .count() == 2 && !fluidButton.isValueSelected() && !itemButton.isValueSelected(),
                "GUI has two independent automatic output buttons, disabled by default");
            verifyFrontTextures(mc);
            ScreenShotHelper.saveScreenshot(outputDir(), "steam-chemical-reactor.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            verifyTextures(panel, mc);
            fluidButton.onMousePressed(0);
            itemButton.onMousePressed(0);
            buttonRequest = 1;
            visualStage = 1;
            frames = 0;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void verifyFrontTextures(Minecraft mc) {
        for (String texture : new String[] { "OVERLAY_FRONT", "OVERLAY_FRONT_ACTIVE", "OVERLAY_FRONT_ACTIVE_GLOW" }) {
            IIcon icon = Textures.BlockIcons
                .customOptional(ModList.GregTech.getResourceLocation(), "basicmachines/chemical_reactor/" + texture)
                .getIcon();
            require(
                icon != null && !icon.getIconName().equals("missingno")
                    && !icon.getIconName().toLowerCase().contains("invisible"),
                "registered chemical reactor front texture " + texture);
        }
    }

    private static ToggleButton findButton(IWidget root, String name) {
        for (ToggleButton button : collect(root, ToggleButton.class)) if (name.equals(button.getName())) return button;
        throw new AssertionError("Missing automatic output button: " + name);
    }

    private static void shiftClick(ToggleButton button) throws Exception {
        Field field = Keyboard.class.getDeclaredField("keyDownBuffer");
        field.setAccessible(true);
        ByteBuffer keys = (ByteBuffer) field.get(null);
        byte previous = keys.get(Keyboard.KEY_LSHIFT);
        try {
            keys.put(Keyboard.KEY_LSHIFT, (byte) 1);
            button.onMousePressed(0);
        } finally {
            keys.put(Keyboard.KEY_LSHIFT, previous);
        }
    }

    private static void verifyTextures(ModularPanel panel, Minecraft mc) throws Exception {
        BufferedImage screenshot = ImageIO.read(new File(outputDir(), "screenshots/steam-chemical-reactor.png"));
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
        require(missing <= 16, "native GUI has no missing magenta textures");
    }

    private static <T> List<T> collect(IWidget widget, Class<T> type) {
        List<T> found = new ArrayList<>();
        if (type.isInstance(widget)) found.add(type.cast(widget));
        for (IWidget child : widget.getChildren()) found.addAll(collect(child, type));
        return found;
    }

    private static File outputDir() {
        return new File(System.getProperty("gtng.steamchemical.qa.output", "."));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("STEAM_CHEMICAL_QA: " + message);
        if (!message.equals("client QA timeout")) System.out.println("STEAM_CHEMICAL_QA: " + message);
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

    public static final class OutputContainer extends TileEntityChest implements IFluidHandler {

        private final Map<String, FluidStack> fluids = new LinkedHashMap<>();
        private int largestRequest;
        private boolean limitedAcceptance;

        private int amount(FluidStack fluid) {
            FluidStack stored = fluids.get(fluid.getFluid().getName());
            return stored == null ? 0 : stored.amount;
        }

        private boolean isEmpty() {
            for (int i = 0; i < getSizeInventory(); i++) if (getStackInSlot(i) != null) return false;
            return fluids.values().stream().allMatch(fluid -> fluid.amount == 0);
        }

        @Override
        public int fill(ForgeDirection side, FluidStack fluid, boolean doFill) {
            if (fluid == null) return 0;
            int accepted = Math.min(fluid.amount, 16000 - amount(fluid));
            largestRequest = Math.max(largestRequest, fluid.amount);
            if (doFill && accepted > 100) {
                accepted = 100;
                limitedAcceptance = true;
            }
            if (doFill && accepted > 0) {
                FluidStack stored = fluids.computeIfAbsent(fluid.getFluid().getName(), key -> {
                    FluidStack copy = fluid.copy();
                    copy.amount = 0;
                    return copy;
                });
                stored.amount += accepted;
                markDirty();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(ForgeDirection side, FluidStack requested, boolean doDrain) {
            if (requested == null || amount(requested) <= 0) return null;
            FluidStack drained = requested.copy();
            drained.amount = Math.min(requested.amount, amount(requested));
            if (doDrain) fluids.get(requested.getFluid().getName()).amount -= drained.amount;
            return drained;
        }

        @Override
        public FluidStack drain(ForgeDirection side, int maxDrain, boolean doDrain) {
            for (FluidStack fluid : fluids.values()) if (fluid.amount > 0) {
                FluidStack requested = fluid.copy();
                requested.amount = maxDrain;
                return drain(side, requested, doDrain);
            }
            return null;
        }

        @Override
        public boolean canFill(ForgeDirection side, Fluid fluid) {
            return fluid != null;
        }

        @Override
        public boolean canDrain(ForgeDirection side, Fluid fluid) {
            return fluid != null && fluids.containsKey(fluid.getName());
        }

        @Override
        public FluidTankInfo[] getTankInfo(ForgeDirection side) {
            return fluids.values().stream().map(fluid -> new FluidTankInfo(fluid, 16000)).toArray(FluidTankInfo[]::new);
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            NBTTagList list = new NBTTagList();
            for (FluidStack fluid : fluids.values()) list.appendTag(fluid.writeToNBT(new NBTTagCompound()));
            tag.setTag("Fluids", list);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            fluids.clear();
            NBTTagList list = tag.getTagList("Fluids", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(list.getCompoundTagAt(i));
                if (fluid != null) fluids.put(fluid.getFluid().getName(), fluid);
            }
        }
    }
}
