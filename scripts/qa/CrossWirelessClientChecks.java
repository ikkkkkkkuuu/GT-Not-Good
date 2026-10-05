package com.xyp.gtnotgood.common.wireless;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtnotgood.common.machines.hatch.CrossRecipeWirelessEnergyHatch;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.common.misc.WirelessNetworkManager;

@Mod(
    modid = "crosswirelessqa",
    name = "Cross Wireless QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class CrossWirelessClientChecks {

    private static final Item INPUT_A = new Item().setUnlocalizedName("crossRecipeInputA");
    private static final Item INPUT_B = new Item().setUnlocalizedName("crossRecipeInputB");
    private static final Item CATALYST = new Item().setUnlocalizedName("crossRecipeCatalyst");
    private boolean started, checked;
    private volatile boolean finished;
    private int frames, position;
    private String failure;
    private BaseMetaTileEntity guiTile;
    private int guiDelay;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.registerItem(INPUT_A, "inputA");
        GameRegistry.registerItem(INPUT_B, "inputB");
        GameRegistry.registerItem(CATALYST, "catalyst");
        if (Boolean.getBoolean("gtng.crossWireless.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "cross-wireless-" + System.currentTimeMillis(),
                "Cross Wireless QA",
                new WorldSettings(918L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (!finished || ++frames < 60) return;
        File output = new File(System.getProperty("gtng.crossWireless.output"));
        if (failure == null && !(mc.currentScreen instanceof GuiContainerWrapper))
            failure = "Settings GUI did not open";
        ScreenShotHelper
            .saveScreenshot(output, "cross-wireless-gui.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        Files.write(
            new File(output, "result.txt").toPath(),
            (failure == null ? "PASS" : failure).getBytes(StandardCharsets.UTF_8));
        System.out.println("CROSS_WIRELESS_QA: " + (failure == null ? "PASS" : failure));
        // The result and screenshot are persisted; this disposable world does not need saving. Optional mods can
        // deadlock shutdown hooks, so terminate only this explicitly opt-in QA process after the checks finish.
        FMLCommonHandler.instance()
            .exitJava(failure == null ? 0 : 1, true);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (guiTile != null && Boolean.getBoolean("gtng.crossWireless.display")) {
            // Keep the synthetic controller usable through capture; no real structure is built in this display fixture.
            MTEMultiBlockBase display = (MTEMultiBlockBase) guiTile.getMetaTileEntity();
            display.mMachine = true;
            display.mUpdated = false;
            display.mUpdate = -10000;
            guiTile.enableWorking();
            guiTile.setShutdownStatus(false);
            guiTile.setActive(true);
            try {
                var errors = MTEMultiBlockBase.class.getDeclaredField("structureErrors");
                errors.setAccessible(true);
                ((List<?>) errors.get(display)).clear();
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
        }
        if (finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        if (checked) {
            if (++guiDelay == 40) {
                EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
                guiTile.getMetaTileEntity()
                    .onRightclick(guiTile, player, ForgeDirection.NORTH, 0.5f, 0.5f, 0.5f);
                finished = true;
            }
            return;
        }
        checked = true;
        try {
            WorldServer world = server.worldServerForDimension(0);
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            require(
                Loader.isModLoaded(ModList.GTNotLeisure.getID()) == Boolean.getBoolean("gtng.crossWireless.gtnl"),
                "Optional GTNL presence");
            require(
                Loader.isModLoaded(ModList.TwistSpaceTechnology.getID())
                    == Boolean.getBoolean("gtng.crossWireless.tst"),
                "Optional TST presence");
            checkHatchRecipe();
            checkAlloyBlastSmelter(world);
            checkMachine(world, findController("gregtech."));
            if (Boolean.getBoolean("gtng.crossWireless.gtnl")) checkMachine(world, findController("com.science.gtnl."));
            if (Boolean.getBoolean("gtng.crossWireless.tst"))
                checkMachine(world, findController("com.Nxer.TwistSpaceTechnology."));
            BaseMetaTileEntity gui = place(
                world,
                GTNGItemList.CrossRecipeWirelessEnergyHatch.get(1),
                player.getUniqueID());
            guiTile = gui;
            if (Boolean.getBoolean("gtng.crossWireless.display")) guiTile = prepareDisplay(world, player);
            player.setPositionAndUpdate(guiTile.xCoord + 1, 7, guiTile.zCoord + 2);
        } catch (Throwable error) {
            error.printStackTrace();
            failure = error.toString();
            finished = true;
        }
    }

    private BaseMetaTileEntity prepareDisplay(WorldServer world, EntityPlayerMP player) throws Exception {
        UUID owner = player.getUniqueID();
        WirelessNetworkManager.strongCheckOrAddUser(owner);
        WirelessNetworkManager.setUserEU(owner, BigInteger.valueOf(2000000));
        BaseMetaTileEntity tile = place(world, findController("gregtech."), owner);
        MTEMultiBlockBase machine = (MTEMultiBlockBase) tile.getMetaTileEntity();
        machine.mMachine = true;
        machine.mUpdated = false;
        machine.mUpdate = -10000;
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = machine.mSolderingTool = machine.mCrowbar = true;
        machine.mEfficiency = 10000;
        var startup = MTEMultiBlockBase.class.getDeclaredField("mStartUpCheck");
        startup.setAccessible(true);
        startup.setInt(machine, -10000);
        tile.enableWorking();
        CrossRecipeWirelessEnergyHatch hatch = (CrossRecipeWirelessEnergyHatch) place(
            world,
            GTNGItemList.CrossRecipeWirelessEnergyHatch.get(1),
            owner).getMetaTileEntity();
        machine.mEnergyHatches.add(hatch);
        // Placing the adjacent hatch schedules a native structure check; this fixture tests display synchronization.
        machine.mUpdated = false;
        machine.mUpdate = -10000;
        WirelessRecipeScheduler scheduler = ((WirelessControllerAccess) machine).gtng$getWirelessScheduler();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList tasks = new NBTTagList();
        tasks.appendTag(
            new WirelessWork(
                owner,
                "display-diamond",
                BigInteger.valueOf(16384),
                128,
                BigInteger.valueOf(640),
                new WirelessOutputs(new ItemStack[] { new ItemStack(Items.diamond, 640) }, null)).save());
        tasks.appendTag(
            new WirelessWork(
                owner,
                "display-fluid",
                BigInteger.valueOf(8192),
                256,
                BigInteger.valueOf(80),
                new WirelessOutputs(
                    new ItemStack[] { new ItemStack(Items.emerald, 80) },
                    new FluidStack[] { new FluidStack(FluidRegistry.WATER, 1440) })).save());
        tag.setTag("tasks", tasks);
        scheduler.load(tag);
        run(machine, tile, 900);
        require(
            scheduler.displayedEU()
                .equals(BigInteger.valueOf(160)),
            "Display sums actual independent EU debits");
        require(
            machine.mEUt == 0 && machine.mOutputItems == null && machine.mOutputFluids == null,
            "Display never populates executable native fields");
        var rows = scheduler.displayedOutputs(128);
        require(
            rows.size() == 3 && rows.get(0)
                .getString("amount")
                .equals("640"),
            "Item and fluid display rows");
        require(
            WirelessRecipeDisplay.rate(rows.get(0), false)
                .equals("100/s"),
            "First task rate uses its duration");
        require(
            WirelessRecipeDisplay.rate(rows.get(1), false)
                .equals("6.25/s"),
            "Second task has its own rate");
        NBTTagCompound hud = new NBTTagCompound();
        machine.getWailaNBTData(player, tile, hud, world, tile.xCoord, tile.yCoord, tile.zCoord);
        require(
            hud.getString("gtngCrossEU")
                .equals("160")
                && hud.getTagList("gtngCrossOutputs", 10)
                    .tagCount() == 3,
            "Waila receives exact wireless EU and output snapshots");
        System.out.println("CROSS_WIRELESS_QA: independent display rates, real EU debit and Waila snapshots PASS");
        return tile;
    }

    private void checkAlloyBlastSmelter(WorldServer world) throws Exception {
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[810];
        require(
            prototype != null && prototype.getClass()
                .getSimpleName()
                .equals("MTEAlloyBlastSmelter"),
            "Screenshot controller 810 is the alloy blast smelter");
        UUID owner = UUID.randomUUID();
        WirelessNetworkManager.strongCheckOrAddUser(owner);
        WirelessNetworkManager.setUserEU(owner, BigInteger.valueOf(1000));
        BaseMetaTileEntity tile = place(world, prototype.getStackForm(1), owner);
        MTEMultiBlockBase machine = (MTEMultiBlockBase) tile.getMetaTileEntity();
        tile.enableWorking();
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = machine.mSolderingTool = machine.mCrowbar = true;
        machine.mEfficiency = 10000;
        CrossRecipeWirelessEnergyHatch hatch = (CrossRecipeWirelessEnergyHatch) place(
            world,
            GTNGItemList.CrossRecipeWirelessEnergyHatch.get(1),
            owner).getMetaTileEntity();
        hatch.setParallelLimit(1);
        hatch.setDuration(3);
        machine.mEnergyHatches.add(hatch);
        MTEHatchInputBus input = (MTEHatchInputBus) place(world, findHatch(MTEHatchInputBus.class), owner)
            .getMetaTileEntity();
        machine.mInputBusses.add(input);
        for (int i = 0; i < 2; i++) machine.mOutputHatches
            .add((MTEHatchOutput) place(world, findHatch(MTEHatchOutput.class), owner).getMetaTileEntity());
        require(
            WirelessCompatibility.supports(machine, ((WirelessControllerAccess) machine).gtng$getProcessingLogic()),
            "Alloy blast smelter supports wireless scheduling");
        WirelessRecipeScheduler scheduler = ((WirelessControllerAccess) machine).gtng$getWirelessScheduler();
        WirelessRecipeAttempt attempt = new WirelessRecipeAttempt(machine, scheduler, hatch, BigInteger.valueOf(1000));
        WirelessRecipeAttempt.enter(attempt);
        try {
            require(
                machine.getAverageInputVoltage() == Long.MAX_VALUE && machine.getMaxInputVoltage() == Long.MAX_VALUE,
                "Native alloy voltage checks see unlimited voltage inside wireless admission");
        } finally {
            WirelessRecipeAttempt.leave();
        }
        require(machine.getAverageInputVoltage() != Long.MAX_VALUE, "Voltage override ends with transaction");
        Config.recipeSpeedMode = 0;
        ItemStack[] ingredients = { new ItemStack(INPUT_A), new ItemStack(INPUT_B) };
        for (int i = 0; i < 2; i++) {
            GTRecipe recipe = new GTRecipe(
                false,
                new ItemStack[] { ingredients[i].copy() },
                new ItemStack[0],
                null,
                new int[0],
                new int[0],
                new int[0],
                new int[] { 10000 },
                new FluidStack[0],
                new FluidStack[] { new FluidStack(i == 0 ? FluidRegistry.WATER : FluidRegistry.LAVA, 144) },
                10,
                i == 0 ? 4 : 6,
                0);
            machine.getRecipeMap()
                .getBackend()
                .compileRecipe(recipe);
            input.setInventorySlotContents(i, ingredients[i].copy());
        }
        run(machine, tile, 100);
        require(scheduler.size() == 2, "Two alloy recipes run concurrently: status=" + scheduler.status());
        for (long tick = 101; tick <= 103; tick++) run(machine, tile, tick);
        require(scheduler.size() == 0, "Both alloy recipes complete");
        require(
            WirelessNetworkManager.getUserEU(owner)
                .equals(BigInteger.valueOf(900)),
            "Exact alloy recipe cost");
        require(
            machine.mOutputHatches.stream()
                .mapToInt(output -> output.getFluidAmount())
                .sum() == 288,
            "Both independent fluid outputs delivered");
        System.out
            .println("CROSS_WIRELESS_QA: alloy blast smelter, scoped voltage bypass and concurrent fluid recipes PASS");
    }

    private void checkHatchRecipe() {
        ItemStack hatch = GTNGItemList.CrossRecipeWirelessEnergyHatch.get(1);
        GTRecipe recipe = RecipeMaps.assemblerRecipes.getAllRecipes()
            .stream()
            .filter(
                candidate -> Arrays.stream(candidate.mOutputs)
                    .anyMatch(hatch::isItemEqual))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing wireless hatch assembler recipe"));
        require(recipe.mInputs.length == 6, "Five ingredients and programmed circuit");
        require(
            Arrays.stream(recipe.mInputs)
                .allMatch(input -> input != null && input.getItem() != null),
            "No missing recipe ingredients");
        require(recipe.mOutputs.length == 1 && recipe.mOutputs[0].stackSize == 1, "One hatch output");
        require(
            recipe.mDuration == Config.getModifiedRecipeDuration(2000) && recipe.mFluidInputs.length == 1
                && recipe.mFluidInputs[0].amount == 2880,
            "Duration respects speed configuration and solder cost is 2880 L");
        ItemStack[] inputs = Arrays.stream(recipe.mInputs)
            .map(ItemStack::copy)
            .toArray(ItemStack[]::new);
        for (ItemStack input : inputs) {
            if (input.stackSize == 0) input.stackSize = 1;
        }
        FluidStack[] fluids = Arrays.stream(recipe.mFluidInputs)
            .map(FluidStack::copy)
            .toArray(FluidStack[]::new);
        require(
            RecipeMaps.assemblerRecipes.findRecipeQuery()
                .items(inputs)
                .fluids(fluids)
                .voltage(recipe.mEUt)
                .find() == recipe,
            "Native assembler recipe lookup");
        require(recipe.isRecipeInputEqual(true, fluids, inputs), "Native ingredient consumption");
        require(fluids[0].amount == 0, "Solder consumed");
        require(
            Arrays.stream(inputs)
                .mapToInt(input -> input.stackSize)
                .sum() == 1,
            "Only programmed circuit remains");
        System.out.println("CROSS_WIRELESS_QA: hatch assembler recipe lookup and consumption PASS");
    }

    private void checkMachine(WorldServer world, ItemStack controller) throws Exception {
        UUID owner = UUID.randomUUID();
        WirelessNetworkManager.strongCheckOrAddUser(owner);
        BigInteger budget = BigInteger.valueOf(100000);
        WirelessNetworkManager.setUserEU(owner, budget);
        BaseMetaTileEntity tile = place(world, controller, owner);
        MTEMultiBlockBase machine = (MTEMultiBlockBase) tile.getMetaTileEntity();
        tile.enableWorking();
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = machine.mSolderingTool = machine.mCrowbar = true;
        machine.mEfficiency = 10000;
        WirelessRecipeScheduler scheduler = ((WirelessControllerAccess) machine).gtng$getWirelessScheduler();
        require(!scheduler.tick(machine, tile, 90), "No hatch leaves native loop untouched");
        CrossRecipeWirelessEnergyHatch hatch = (CrossRecipeWirelessEnergyHatch) place(
            world,
            GTNGItemList.CrossRecipeWirelessEnergyHatch.get(1),
            owner).getMetaTileEntity();
        hatch.setDuration(12);
        require(
            hatch.getParallelLimit()
                .equals(BigInteger.valueOf(Integer.MAX_VALUE)),
            "Default parallel limit is int maximum");
        hatch.setParallelLimit(1);
        machine.mEnergyHatches.add(hatch);
        MTEHatchInputBus input = (MTEHatchInputBus) place(world, findHatch(MTEHatchInputBus.class), owner)
            .getMetaTileEntity();
        MTEHatchOutputBus output = (MTEHatchOutputBus) place(world, findHatch(MTEHatchOutputBus.class), owner)
            .getMetaTileEntity();
        machine.mInputBusses.add(input);
        machine.mOutputBusses.add(output);
        ItemStack a = new ItemStack(INPUT_A);
        ItemStack b = new ItemStack(INPUT_B);
        GTRecipe recipeA = new GTRecipe(
            false,
            new ItemStack[] { a.copy() },
            new ItemStack[] { new ItemStack(Items.gold_ingot) },
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            10,
            32,
            0);
        GTRecipe recipeB = new GTRecipe(
            false,
            new ItemStack[] { b.copy() },
            new ItemStack[] { new ItemStack(Items.diamond) },
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            7,
            48,
            0);
        Config.recipeSpeedMode = 0;
        machine.getRecipeMap()
            .getBackend()
            .compileRecipe(recipeA);
        machine.getRecipeMap()
            .getBackend()
            .compileRecipe(recipeB);
        input.setInventorySlotContents(0, a.copy());
        if (machine.getClass()
            .getSimpleName()
            .equals("TST_ManufacturingCenter")) {
            run(machine, tile, 95);
            require(
                scheduler.size() == 0 && input.getStackInSlot(0).stackSize == 1,
                "TST core tier validation rejects the recipe without consuming inputs");
            machine.getClass()
                .getField("coreTier")
                .setInt(machine, 5);
        }
        run(machine, tile, 100);
        require(
            scheduler.size() == 1,
            "First recipe started: " + machine.getClass()
                + " status="
                + scheduler.status()
                + " result="
                + machine.getCheckRecipeResult()
                    .getDisplayString());
        require(input.getStackInSlot(0) == null || input.getStackInSlot(0).stackSize == 0, "Inputs consumed once");
        hatch.setDuration(3);
        for (long tick = 101; tick <= 104; tick++) run(machine, tile, tick);
        input.setInventorySlotContents(1, b.copy());
        run(machine, tile, 105);
        require(scheduler.size() == 2, "Two different recipes overlap");
        System.out.println("CROSS_WIRELESS_QA: queued=" + scheduler.save());
        WirelessNetworkManager.setUserEU(owner, BigInteger.ZERO);
        NBTTagCompound paused = scheduler.save();
        run(machine, tile, 106);
        require(paused.equals(scheduler.save()), "Power loss preserves both tasks");
        NBTTagCompound savedController = new NBTTagCompound();
        machine.saveNBTData(savedController);
        scheduler.load(new NBTTagCompound());
        machine.loadNBTData(savedController);
        require(paused.equals(scheduler.save()), "Controller save/load restores independent tasks");
        WirelessNetworkManager.setUserEU(owner, budget);
        for (long tick = 107; tick <= 109; tick++) run(machine, tile, tick);
        require(scheduler.size() == 1, "Short later recipe finishes independently");
        System.out.println("CROSS_WIRELESS_QA: output=" + Arrays.toString(output.mInventory));
        require(
            count(output, recipeB.mOutputs[0].getItem()) == recipeB.mOutputs[0].stackSize
                && count(output, recipeA.mOutputs[0].getItem()) == 0,
            "Independent output timing");
        for (long tick = 110; tick <= 115; tick++) run(machine, tile, tick);
        require(
            scheduler.size() == 0 && count(output, recipeA.mOutputs[0].getItem()) == recipeA.mOutputs[0].stackSize,
            "Long recipe completed once");
        BigInteger paidBeforePause = BigInteger.valueOf(320)
            .multiply(BigInteger.valueOf(5))
            .divide(BigInteger.valueOf(12));
        require(
            WirelessNetworkManager.getUserEU(owner)
                .equals(
                    budget.subtract(
                        BigInteger.valueOf(656)
                            .subtract(paidBeforePause))),
            "Exact total debit after reload");
        System.out.println(
            "CROSS_WIRELESS_QA: overlapping recipes, power loss, reload and exact EU passed on " + machine.getClass()
                .getName());
        machine.setVoidingMode(VoidingMode.VOID_NONE);
        NBTTagCompound pending = new NBTTagCompound();
        NBTTagList pendingTasks = new NBTTagList();
        pendingTasks.appendTag(
            new WirelessWork(
                owner,
                "blocked-output",
                BigInteger.ZERO,
                1,
                1,
                new ItemStack[] { new ItemStack(Items.diamond) },
                new FluidStack[] { new FluidStack(FluidRegistry.WATER, 1000) }).save());
        pending.setTag("tasks", pendingTasks);
        scheduler.load(pending);
        machine.mEnergyHatches.clear();
        run(machine, tile, 120);
        require(pending.equals(scheduler.save()), "Missing hatch pauses tasks");
        machine.mEnergyHatches.add(hatch);
        hatch.getBaseMetaTileEntity()
            .setOwnerUuid(UUID.randomUUID());
        run(machine, tile, 121);
        require(pending.equals(scheduler.save()), "New owner cannot take over old task");
        hatch.getBaseMetaTileEntity()
            .setOwnerUuid(owner);
        for (int slot = 0; slot < output.getSizeInventory(); slot++)
            output.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        run(machine, tile, 122);
        require(scheduler.size() == 1 && scheduler.status() == 4, "Full output retains completed recipe");
        output.setInventorySlotContents(0, null);
        run(machine, tile, 123);
        require(
            scheduler.size() == 1 && output.getStackInSlot(0) == null,
            "Missing fluid space cannot partially commit items");
        MTEHatchOutput fluidOutput = (MTEHatchOutput) place(world, findHatch(MTEHatchOutput.class), owner)
            .getMetaTileEntity();
        machine.mOutputHatches.add(fluidOutput);
        run(machine, tile, 124);
        require(
            scheduler.size() == 0 && count(output, Items.diamond) == 1
                && fluidOutput.getFluid() != null
                && fluidOutput.getFluid().amount == 1000,
            "Atomic item/fluid output retry");
        run(machine, tile, 125);
        require(
            count(output, Items.diamond) == 1 && fluidOutput.getFluid().amount == 1000,
            "No duplicate output after retry");
        System.out.println("CROSS_WIRELESS_QA: ownership, missing hatch and atomic output retry passed");
        checkBigBatch(machine, tile, hatch, input, output, owner);
        // Keep the controller from running independently after this synchronous fixture finishes.
        tile.disableWorking();
    }

    private static void checkBigBatch(MTEMultiBlockBase machine, BaseMetaTileEntity tile,
        CrossRecipeWirelessEnergyHatch hatch, MTEHatchInputBus input, MTEHatchOutputBus output, UUID owner)
        throws Exception {
        WirelessRecipeScheduler scheduler = ((WirelessControllerAccess) machine).gtng$getWirelessScheduler();
        for (int slot = 0; slot < output.getSizeInventory(); slot++) output.setInventorySlotContents(slot, null);
        machine.mOutputHatches.clear();
        BigInteger credit = BigInteger.TEN.pow(60);
        WirelessNetworkManager.setUserEU(owner, credit);
        hatch.setDuration(1);
        hatch.setParallelLimit(0);
        // Three native int-sized input stacks must become one recipe task, with no int cap on the total.
        for (int slot = 0; slot < 3; slot++)
            input.setInventorySlotContents(slot, new ItemStack(INPUT_A, Integer.MAX_VALUE));
        run(machine, tile, 130);
        require(
            scheduler.size() == 1,
            "Big batch start: result=" + machine.getCheckRecipeResult()
                .getDisplayString()
                + ", status="
                + scheduler.status()
                + ", working="
                + tile.isAllowedToWork()
                + ", duration="
                + machine.mMaxProgresstime
                + ", inputs="
                + Arrays.toString(input.mInventory));
        BigInteger expected = BigInteger.valueOf(Integer.MAX_VALUE)
            .multiply(BigInteger.valueOf(3));
        WirelessWork work = WirelessWork.load(
            scheduler.save()
                .getTagList("tasks", 10)
                .getCompoundTagAt(0));
        require(work.parallels.equals(expected), "One task exceeds int parallel range");
        require(work.totalEU.equals(expected.multiply(BigInteger.valueOf(320))), "Big parallel exact EU");
        for (int slot = 0; slot < 3; slot++) require(
            input.getStackInSlot(slot) == null || input.getStackInSlot(slot).stackSize == 0,
            "Large inputs consumed exactly once");
        run(machine, tile, 131);
        int delivered = count(output, Items.gold_ingot);
        require(delivered > 0 && scheduler.size() == 1, "Full inventory leaves large output remainder queued");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        work = WirelessWork.load(
            scheduler.save()
                .getTagList("tasks", 10)
                .getCompoundTagAt(0));
        require(
            work.outputs.itemAmounts[0].equals(expected.subtract(BigInteger.valueOf(delivered))),
            "Partial output reload is lossless");
        BigInteger paid = WirelessNetworkManager.getUserEU(owner);
        for (int slot = 0; slot < output.getSizeInventory(); slot++) output.setInventorySlotContents(slot, null);
        run(machine, tile, 132);
        require(
            WirelessNetworkManager.getUserEU(owner)
                .equals(paid),
            "Output retries never charge again");
        WirelessWork next = WirelessWork.load(
            scheduler.save()
                .getTagList("tasks", 10)
                .getCompoundTagAt(0));
        require(
            next.outputs.itemAmounts[0]
                .equals(work.outputs.itemAmounts[0].subtract(BigInteger.valueOf(count(output, Items.gold_ingot)))),
            "Second output slice is subtracted exactly once");
        scheduler.load(new NBTTagCompound());
        machine.mMaxProgresstime = 0;
        machine.mProgresstime = 0;

        // Huge deterministic/chanced outputs remain compact and survive NBT without allocating stack-per-int arrays.
        GTRecipe huge = new GTRecipe(
            false,
            new ItemStack[] { new ItemStack(INPUT_B) },
            new ItemStack[] { new ItemStack(Items.diamond, Integer.MAX_VALUE) },
            null,
            null,
            null,
            null,
            null,
            null,
            new FluidStack[] { new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE) },
            1,
            1,
            0);
        WirelessOutputs ledger = WirelessOutputs.forRecipe(huge);
        BigInteger farBeyondLong = BigInteger.TEN.pow(50)
            .add(BigInteger.valueOf(73));
        ledger.addRecipeBatch(huge, farBeyondLong);
        BigInteger quantity = farBeyondLong.multiply(BigInteger.valueOf(Integer.MAX_VALUE));
        WirelessOutputs restored = WirelessOutputs.load(ledger.save());
        require(
            restored.itemAmounts[0].equals(quantity) && restored.fluidAmounts[0].equals(quantity),
            "Item and fluid totals exceed long range without truncation");
        require(restored.items.length == 1 && restored.fluids.length == 1, "Huge outputs use compact storage");
        GTRecipe fluidRecipe = new GTRecipe(
            false,
            null,
            new ItemStack[] { new ItemStack(Items.gold_ingot) },
            null,
            null,
            null,
            null,
            null,
            new FluidStack[] { new FluidStack(FluidRegistry.WATER, 1) },
            new FluidStack[] { new FluidStack(FluidRegistry.LAVA, 1) },
            1,
            1,
            0);
        FluidStack[] bulkFluids = { new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE),
            new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE),
            new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE) };
        WirelessOutputs fluidLedger = WirelessOutputs.forRecipe(fluidRecipe);
        require(
            WirelessInputBatch.consume(fluidRecipe, new ItemStack[0], bulkFluids, null, fluidLedger)
                .equals(expected),
            "Fluid inputs support beyond-int parallel totals");
        require(fluidLedger.fluidAmounts[0].equals(expected), "Fluid output batch total is exact");
        for (FluidStack consumed : bulkFluids) require(consumed.amount == 0, "Large fluid inputs consumed once");
        // A non-consumed catalyst lets the real controller exercise counts beyond long without inventing materials.
        GTRecipe catalystRecipe = new GTRecipe(
            false,
            new ItemStack[] { new ItemStack(CATALYST, 0) },
            new ItemStack[] { new ItemStack(Items.emerald) },
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            1,
            1,
            0);
        machine.getRecipeMap()
            .getBackend()
            .compileRecipe(catalystRecipe);
        input.setInventorySlotContents(0, new ItemStack(CATALYST));
        hatch.setParallelLimit(farBeyondLong);
        WirelessNetworkManager.setUserEU(owner, credit);
        run(machine, tile, 135);
        require(scheduler.size() == 1, "Catalyst big batch started");
        WirelessWork enormous = WirelessWork.load(
            scheduler.save()
                .getTagList("tasks", 10)
                .getCompoundTagAt(0));
        require(
            enormous.parallels.equals(farBeyondLong) && enormous.outputs.itemAmounts[0].equals(farBeyondLong),
            "Real controller keeps beyond-long parallel and output quantities");
        require(
            input.getStackInSlot(0) != null && input.getStackInSlot(0).stackSize == 1,
            "Catalyst remains unconsumed");
        run(machine, tile, 136);
        require(
            WirelessNetworkManager.getUserEU(owner)
                .equals(credit.subtract(farBeyondLong)),
            "Beyond-long task charged exactly once");
        scheduler.load(new NBTTagCompound());
        machine.mMaxProgresstime = 0;
        input.setInventorySlotContents(0, null);
        NBTTagCompound hatchTag = new NBTTagCompound();
        hatch.setParallelLimit(farBeyondLong);
        hatch.saveNBTData(hatchTag);
        hatch.setParallelLimit(1);
        hatch.loadNBTData(hatchTag);
        require(
            hatch.getParallelLimit()
                .equals(farBeyondLong),
            "Big parallel setting persists");
        hatch.setParallelLimit(0);
        hatch.saveNBTData(hatchTag);
        hatch.loadNBTData(hatchTag);
        require(
            hatch.getParallelLimit()
                .signum() == 0,
            "Unlimited setting persists");
        System.out.println(
            "CROSS_WIRELESS_QA: big parallel=" + expected
                + ", big output="
                + quantity
                + ", partial output reload PASS");
    }

    private static void run(MTEMultiBlockBase machine, BaseMetaTileEntity tile, long tick) throws Exception {
        Class<?> type = machine.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
                method.setAccessible(true);
                method.invoke(machine, tile, tick);
                return;
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new AssertionError("No native run loop");
    }

    private static ItemStack findController(String prefix) {
        for (IMetaTileEntity entry : GregTechAPI.METATILEENTITIES) {
            if (!(entry instanceof MTEMultiBlockBase) || !entry.getClass()
                .getName()
                .startsWith(prefix)) continue;
            if (prefix.equals("gregtech.") && !entry.getClass()
                .getSimpleName()
                .equals("MTEVacuumFreezer")) continue;
            if (prefix.equals("com.science.gtnl.") && !entry.getClass()
                .getSimpleName()
                .equals("LargeAssembler")) continue;
            if (prefix.equals("com.Nxer.TwistSpaceTechnology.") && !entry.getClass()
                .getSimpleName()
                .equals("TST_ManufacturingCenter")) continue;
            MTEMultiBlockBase machine = (MTEMultiBlockBase) entry.newMetaEntity(null);
            if (!WirelessCompatibility.supports(machine, ((WirelessControllerAccess) machine).gtng$getProcessingLogic())
                || machine.getRecipeMap() == null) continue;
            if (machine.getRecipeMap()
                .getFrontend()
                .getUIProperties().maxItemInputs < 1
                || machine.getRecipeMap()
                    .getFrontend()
                    .getUIProperties().maxItemOutputs < 1)
                continue;
            System.out.println(
                "CROSS_WIRELESS_QA: controller=" + entry.getClass()
                    .getName());
            return entry.getStackForm(1);
        }
        throw new AssertionError("No compatible controller: " + prefix);
    }

    private static ItemStack findHatch(Class<?> type) {
        for (IMetaTileEntity entry : GregTechAPI.METATILEENTITIES)
            if (entry != null && entry.getClass() == type && ((MTEHatch) entry).mTier == 3)
                return entry.getStackForm(1);
        throw new AssertionError("No hatch: " + type);
    }

    private BaseMetaTileEntity place(WorldServer world, ItemStack stack, UUID owner) {
        int x = position++ * 2;
        world.setBlock(x, 6, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 6, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerUuid(owner);
        tile.setFrontFacing(ForgeDirection.SOUTH);
        return tile;
    }

    private static int count(MTEHatchOutputBus output, Item item) {
        int count = 0;
        for (int i = 0; i < output.getSizeInventory(); i++) {
            ItemStack stack = output.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) count += stack.stackSize;
        }
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
