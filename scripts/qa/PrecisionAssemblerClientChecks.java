package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.Language;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.xyp.gtnotgood.common.machines.multiblock.PrecisionAssembler;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import bartworks.API.BorosilicateGlass;
import bartworks.common.loaders.ItemRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.GregTechTileClientEvents;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

@Mod(
    modid = "precisionassemblerqa",
    name = "Precision Assembler QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class PrecisionAssemblerClientChecks {

    private Item fixture;
    private BaseMetaTileEntity base;
    private volatile PrecisionAssembler machine;
    private final List<int[]> casingPositions = new ArrayList<>();
    private final List<int[]> machinePositions = new ArrayList<>();
    private final StringBuilder report = new StringBuilder();
    private boolean started, structureCaptured;
    private volatile boolean checked, finished;
    private volatile int requestedMode = -1, openedMode = -1, requestedCasingTier = -1;
    private int clientTicks, serverTicks, frames, capturedMode = -1, capturedCasingTier = -1, stageFrames;
    private int tooltipStage = -1;
    private MultiblockTooltipBuilder currentTooltip;
    private final int[] nativeCounts = new int[2];

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.precisionAssembler.qa")) return;
        fixture = new Item().setHasSubtypes(true).setUnlocalizedName("precisionAssemblerFixture");
        GameRegistry.registerItem(fixture, "fixture");
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks <= 6000, "Client QA timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.guiScale = tooltipStage >= 0 ? 1 : 2;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0F);
            mc.gameSettings.limitFramerate = 60;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("precision-assembler-qa-" + System.currentTimeMillis(),
                    "Precision Assembler QA",
                    new WorldSettings(733L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            require(++serverTicks <= 3600, "Server QA timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks == 1) setup(player);
            if (serverTicks == 60) {
                verifyStructure();
                verifyCasingTiers();
                verifyGlass();
                verifyModePersistence();
                verifyControllerRecipe();
                registerFixtures();
                verifyProcessing();
                verifyVoltageAndOverclocks();
                verifyParallel();
                verifyRecipeLockSwitch();
                resetProcessing();
                machine.setRecipeLocking(false);
                machine.setMachineMode(0);
                setCasingTier(0);
                setMachineTier(3);
                setEnergy(ItemList.Hatch_Energy_MV, true);
                verifyStructure();
                cleanupFixtures();
                checked = true;
            }
            if (checked && requestedCasingTier >= 0) {
                setCasingTier(requestedCasingTier);
                verifyStructure();
                requestedCasingTier = -1;
            }
            if (requestedMode >= 0) {
                int mode = requestedMode;
                requestedMode = -1;
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(0, 13, -4, 0, 0);
                machine.setMachineMode(mode);
                machine.onRightclick(base, player);
                openedMode = mode;
            }
            if (checked && openedMode >= 0) openedMode = machine.getMachineMode();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        require(!Loader.isModLoaded(ModList.ModIds.GT_NOT_LEISURE), "QA runs without GTNL");
        require(GTNGItemList.PrecisionAssembler.get(1).getItemDamage() == GTNGMachineID.PrecisionAssembler.id,
            "Controller registration");
        World world = player.worldObj;
        world.setWorldTime(6000);
        base = place(world, 0, 12, 0, GTNGMachineID.PrecisionAssembler.id);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        machine = (PrecisionAssembler) base.getMetaTileEntity();
        ItemStack trigger = new ItemStack(Items.diamond);
        ChannelDataAccessor.setChannelData(trigger, "glass", 1);
        machine.construct(trigger, false);
        place(world, -3, 12, 0, ItemList.Hatch_Input_Bus_EV.get(1).getItemDamage());
        place(world, -2, 12, 0, ItemList.Hatch_Output_Bus_EV.get(1).getItemDamage());
        place(world, -1, 12, 0, ItemList.Hatch_Input_EV.get(1).getItemDamage());
        place(world, 1, 12, 0, ItemList.Hatch_Maintenance.get(1).getItemDamage());
        place(world, 2, 12, 0, ItemList.Hatch_Muffler_MV.get(1).getItemDamage());
        setEnergy(ItemList.Hatch_Energy_MV, true);
        ItemStack casing = casing(0);
        for (int x = -4; x <= 4; x++) for (int y = 12; y <= 16; y++) for (int z = 0; z <= 4; z++) {
            if (
                world.getBlock(x, y, z) == Block.getBlockFromItem(casing.getItem())
                    && world.getBlockMetadata(x, y, z) == casing.getItemDamage()
            ) casingPositions.add(new int[] { x, y, z });
            if (world.getBlock(x, y, z) == GregTechAPI.sBlockCasings1) machinePositions.add(new int[] { x, y, z });
        }
        require(casingPositions.size() >= 42 && machinePositions.size() == 21, "Constructed casing counts");
        setMachineTier(3);
        machine.setAlwaysMaxParallel(false);
        machine.setPowerPanelMaxParallel(1);
        player.playerNetServerHandler.setPlayerLocation(-10, 18, -10, -40, 18);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private void verifyStructure() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(errors.isEmpty(),
            "Structure: " + errors.stream().map(StructureError::getDisplayString).collect(Collectors.joining("; ")));
        require(machine.checkStructure(true, base), "Real structure validation");
        require(
            machine.mInputBusses.size() == 1 && machine.mOutputBusses.size() == 1 && machine.mInputHatches.size() == 1,
            "Required real hatches");
    }

    private void verifyCasingTiers() {
        for (int tier = 0; tier <= 4; tier++) {
            setCasingTier(tier);
            verifyStructure();
            require(machine.getMechanicalCasingTier() == tier, "Mechanical casing tier " + tier);
            require(machine.getCasingTextureId() == casingTexture(tier), "Controller casing texture " + tier);
            PrecisionAssembler clientCopy = new PrecisionAssembler("textureSyncFixture");
            clientCopy.receiveClientEvent(GregTechTileClientEvents.CHANGE_CUSTOM_DATA, machine.getUpdateData());
            require(clientCopy.getCasingTextureId() == casingTexture(tier),
                "Controller custom byte casing sync " + tier);
            for (List<? extends MTEHatch> hatches : Arrays.<List<? extends MTEHatch>>asList(machine.mInputBusses,
                machine.mInputHatches, machine.mOutputBusses, machine.mMaintenanceHatches, machine.mMufflerHatches,
                machine.mEnergyHatches)) {
                for (MTEHatch hatch : hatches)
                    require(hatch.getCasingTexture() == Textures.BlockIcons.getCasingTextureForId(casingTexture(tier)),
                        "Attached hatch casing texture " + tier);
            }
            machine.setMachineMode(1);
            require(machine.getMaxParallelRecipes() == (16 << tier), "Common parallel tier " + tier);
            machine.setMachineMode(0);
            require(machine.getMaxParallelRecipes() == 1, "Precise mode keeps one parallel");
        }
        setCasingTier(0);
        int[] position = casingPositions.get(0);
        setBlock(position, casing(1));
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(!errors.isEmpty() && !machine.checkStructure(true, base), "Mixed mechanical casing tiers rejected");
        setCasingTier(0);
        verifyStructure();
        require(machine.getMechanicalCasingTier() == 0, "Downgrade clears the previous casing tier");
        report.append("Five casing tiers, 16/32/64/128/256 common parallel limits, mixed casing rejection.\n");
    }

    private void setCasingTier(int tier) {
        for (int[] position : casingPositions) setBlock(position, casing(tier));
        if (base.getWorld().getBlock(3, 12, 0) != GregTechAPI.sBlockMachines)
            setBlock(new int[] { 3, 12, 0 }, casing(tier));
    }

    private static ItemStack casing(int tier) {
        return switch (tier) {
            case 0 -> Casings.SolidSteelMachineCasing.toStack(1);
            case 1 -> Casings.ChemicallyInertMachineCasing.toStack(1);
            case 2 -> Casings.CleanStainlessSteelMachineCasing.toStack(1);
            case 3 -> Casings.StableTitaniumMachineCasing.toStack(1);
            case 4 -> Casings.RobustTungstenSteelMachineCasing.toStack(1);
            default -> throw new AssertionError("Unexpected casing tier " + tier);
        };
    }

    private static int casingTexture(int tier) {
        return switch (tier) {
            case 0 -> Casings.SolidSteelMachineCasing.getTextureId();
            case 1 -> Casings.ChemicallyInertMachineCasing.getTextureId();
            case 2 -> Casings.CleanStainlessSteelMachineCasing.getTextureId();
            case 3 -> Casings.StableTitaniumMachineCasing.getTextureId();
            case 4 -> Casings.RobustTungstenSteelMachineCasing.getTextureId();
            default -> throw new AssertionError("Unexpected casing tier " + tier);
        };
    }

    private void verifyGlass() {
        World world = base.getWorld();
        int[] position = null;
        for (int x = -4; x <= 4 && position == null; x++)
            for (int y = 12; y <= 16 && position == null; y++) for (int z = 0; z <= 4 && position == null; z++)
                if (BorosilicateGlass.getTier(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z)) >= 4)
                    position = new int[] { x, y, z };
        require(position != null, "Real EV or higher borosilicate glass constructed");
        Block original = world.getBlock(position[0], position[1], position[2]);
        int originalMeta = world.getBlockMetadata(position[0], position[1], position[2]);
        for (Block replacement : new Block[] { Blocks.glass, GregTechAPI.sBlockGlass1, ItemRegistry.bw_realglas }) {
            world.setBlock(position[0], position[1], position[2], replacement, 0, 3);
            machine.clearHatches();
            List<StructureError> errors = new ArrayList<>();
            machine.checkMachine(base, null, errors);
            require(!errors.isEmpty() && !machine.checkStructure(true, base), "Invalid or below-EV glass rejected");
            world.setBlock(position[0], position[1], position[2], original, originalMeta, 3);
            verifyStructure();
        }
        report.append("EV borosilicate glass accepted; vanilla, other EV glass and HV borosilicate glass rejected.\n");
    }

    private void setBlock(int[] position, ItemStack stack) {
        base.getWorld().setBlock(position[0], position[1], position[2], Block.getBlockFromItem(stack.getItem()),
            stack.getItemDamage(), 3);
    }

    private void setMachineTier(int tier) {
        for (int[] position : machinePositions)
            base.getWorld().setBlock(position[0], position[1], position[2], GregTechAPI.sBlockCasings1, tier, 3);
    }

    private void setEnergy(ItemList energy, boolean dual) {
        place(base.getWorld(), -4, 12, 0, energy.get(1).getItemDamage());
        if (dual) place(base.getWorld(), 3, 12, 0, energy.get(1).getItemDamage());
        else setBlock(new int[] { 3, 12, 0 }, casing(Math.max(0, machine.getMechanicalCasingTier())));
    }

    private void verifyModePersistence() {
        for (int mode = 0; mode < 2; mode++) {
            machine.setMachineMode(mode);
            NBTTagCompound saved = new NBTTagCompound();
            base.writeToNBT(saved);
            World world = base.getWorld();
            world.removeTileEntity(0, 12, 0);
            BaseMetaTileEntity restored = new BaseMetaTileEntity();
            restored.setWorldObj(world);
            restored.readFromNBT(saved);
            world.setTileEntity(0, 12, 0, restored);
            base = restored;
            base.disableWorking();
            machine = (PrecisionAssembler) base.getMetaTileEntity();
            require(machine.getMachineMode() == mode && machine.getRecipeMap() == map(mode), "Reload mode " + mode);
            verifyStructure();
        }
        for (int invalid : new int[] { -1, 2, Integer.MAX_VALUE }) {
            machine.setMachineMode(invalid);
            int mode = machine.getMachineMode();
            require(mode >= 0 && mode < 2 && machine.getRecipeMap() == map(mode), "Invalid mode " + invalid);
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            saved.setInteger("machineMode", invalid);
            machine.loadNBTData(saved);
            mode = machine.getMachineMode();
            require(mode >= 0 && mode < 2 && machine.getRecipeMap() == map(mode), "Invalid saved mode " + invalid);
        }
        report.append("Both native recipe maps persist across real tile replacement; invalid modes normalize.\n");
    }

    private void verifyControllerRecipe() {
        List<GTRecipe> recipes = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
            .filter(recipe -> Arrays.stream(recipe.mOutputs)
                .anyMatch(output -> GTUtility.areStacksEqual(output, GTNGItemList.PrecisionAssembler.get(1))))
            .collect(Collectors.toList());
        require(recipes.size() == 1, "Exactly one controller assembler recipe");
        GTRecipe recipe = recipes.get(0);
        require(recipe.mEUt == 120 && recipe.mDuration == 600,
            "MV controller recipe power and duration: " + recipe.mEUt + " EU/t, " + recipe.mDuration + " ticks");
        FluidStack[] fluids = { Materials.SolderingAlloy.getMolten(576) };
        ItemStack[] inputs = { ItemList.Machine_MV_Assembler.get(2), ItemList.Robot_Arm_MV.get(4),
            ItemList.Conveyor_Module_MV.get(4), GTOreDictUnificator.get(OrePrefixes.circuit, Materials.MV, 4),
            ItemList.Casing_SolidSteel.get(4), GTUtility.getIntegratedCircuit(24) };
        require(recipe.isRecipeInputEqual(false, false, fluids, inputs),
            "MV recipe accepts exact registered ingredients");
        ProcessingLogic logic = new ProcessingLogic().setMachine(machine)
            .setRecipeMapSupplier(() -> RecipeMaps.assemblerRecipes).setVoidProtection(false, false)
            .setAvailableVoltage(128).setAvailableAmperage(1).setMaxParallel(1).setInputItems(inputs)
            .setInputFluids(fluids);
        require(logic.process().wasSuccessful() && logic.getDuration() == 600 && logic.getCalculatedEut() == 120,
            "Controller recipe processes on MV power with its original duration");
        require(
            logic.getOutputItems().length == 1
                && GTUtility.areStacksEqual(logic.getOutputItems()[0], GTNGItemList.PrecisionAssembler.get(1))
                && logic.getOutputItems()[0].stackSize == 1,
            "Controller recipe produces exactly one registered machine");
        report.append("MV controller assembler recipe: 120 EU/t, 600 ticks, exact six inputs and 576 L solder.\n");
    }

    private void registerFixtures() {
        for (int mode = 0; mode < 2; mode++) {
            nativeCounts[mode] = map(mode).getAllRecipes().size();
            require(nativeCounts[mode] > 0, "Native recipes loaded in mode " + mode);
            for (int meta : new int[] { mode, 10 }) recipe(meta, mode, 30, 0).addTo(map(mode));
        }
        recipe(11, 0, 30, 1).addTo(map(0));
        recipe(12, 1, 480, 0).addTo(map(1));
        recipe(13, 1, 120, 0).addTo(map(1));
    }

    private GTRecipeBuilder recipe(int input, int mode, int power, int tier) {
        return GTRecipeBuilder.builder().itemInputs(stack(input, 1)).itemOutputs(stack(20 + mode, 1)).duration(128)
            .eut(power).specialValue(tier);
    }

    private void verifyProcessing() {
        machine.setRecipeLocking(false);
        machine.setMachineMode(1);
        seed(0, 1);
        require(!machine.checkProcessing().wasSuccessful() && inputAmount() == 1, "Wrong mode preserves input");
        for (int mode = 0; mode < 2; mode++) {
            resetProcessing();
            machine.setMachineMode(mode);
            seed(mode, 1);
            require(machine.checkProcessing().wasSuccessful(), "Real processing mode " + mode);
            require(inputAmount() == 0 && outputAmount() == 1, "Exact item consumption and output mode " + mode);
        }
        resetProcessing();
        machine.setMachineMode(0);
        seed(11, 1);
        require(!machine.checkProcessing().wasSuccessful() && inputAmount() == 1, "Low mechanical tier rejects recipe");
        setCasingTier(1);
        verifyStructure();
        require(machine.checkProcessing().wasSuccessful() && outputAmount() == 1,
            "Mechanical tier upgrade unlocks recipe");
        report
            .append("Both modes process independently; wrong mode and insufficient precision tier preserve inputs.\n");
    }

    private void verifyVoltageAndOverclocks() {
        resetProcessing();
        machine.setMachineMode(1);
        setMachineTier(2);
        setEnergy(ItemList.Hatch_Energy_MV, true);
        verifyStructure();
        seed(12, 1);
        require(!machine.checkProcessing().wasSuccessful() && inputAmount() == 1,
            "MV machine casing rejects HV recipe");
        setMachineTier(3);
        verifyStructure();
        require(machine.checkProcessing().wasSuccessful() && outputAmount() == 1,
            "Two MV energy hatches run HV recipe");
        resetProcessing();
        setMachineTier(2);
        setEnergy(ItemList.Hatch_Energy_EV, false);
        verifyStructure();
        seed(1, 1);
        require(machine.checkProcessing().wasSuccessful(), "High energy hatch accepts lower tier recipe");
        require(machine.mMaxProgresstime == 1 && machine.lEUt == -1920 && outputAmount() == 1,
            "MV internal casing allows LV recipe perfect overclock to EV with 200% common speed: "
                + machine.mMaxProgresstime
                + " ticks, "
                + machine.lEUt
                + " EU/t");
        resetProcessing();
        seed(13, 1);
        require(
            machine.checkProcessing().wasSuccessful() && machine.mMaxProgresstime == 4
                && machine.lEUt == -1920
                && outputAmount() == 1,
            "MV internal casing allows MV recipe perfect overclock to EV with 200% common speed: "
                + machine.mMaxProgresstime
                + " ticks, "
                + machine.lEUt
                + " EU/t");
        resetProcessing();
        machine.setMachineMode(0);
        seed(0, 1);
        require(
            machine.checkProcessing().wasSuccessful() && machine.mMaxProgresstime == 2
                && machine.lEUt == -1920
                && outputAmount() == 1,
            "MV internal casing allows precise LV recipe perfect overclock to EV at original base speed: "
                + machine.mMaxProgresstime
                + " ticks, "
                + machine.lEUt
                + " EU/t");
        report.append("Dual MV hatches run HV recipes with HV internal casing; MV internal casing rejects HV recipes. ")
            .append("EV energy hatches overclock LV/MV recipes beyond MV internal casing: recipe tier cap, ").append(
                "no energy/OC tier cap; perfect overclock and common 200% speed verified by exact time and EU/t.\n");
    }

    private void verifyParallel() throws Exception {
        resetProcessing();
        machine.setMachineMode(1);
        machine.setAlwaysMaxParallel(true);
        Method create = PrecisionAssembler.class.getDeclaredMethod("createProcessingLogic");
        create.setAccessible(true);
        for (int tier = 0; tier <= 4; tier++) {
            setCasingTier(tier);
            verifyStructure();
            int parallel = 16 << tier;
            ProcessingLogic logic = (ProcessingLogic) create.invoke(machine);
            ItemStack[] inputs = new ItemStack[5];
            Arrays.setAll(inputs, slot -> stack(1, 64));
            logic.setMachine(machine).setRecipeMapSupplier(machine::getRecipeMap).setVoidProtection(false, false)
                .setAvailableVoltage(32).setAvailableAmperage(1024).setInputItems(inputs);
            require(logic.process().wasSuccessful(), "Parallel fixture tier " + tier);
            int produced = Arrays.stream(logic.getOutputItems()).mapToInt(output -> output.stackSize).sum();
            require(produced == parallel, "Actual common parallel tier " + tier + ": " + produced);
        }
        machine.setAlwaysMaxParallel(false);
        machine.setPowerPanelMaxParallel(1);
        report.append("Actual recipe yields reach all five common parallel limits.\n");
    }

    private void verifyRecipeLockSwitch() {
        resetProcessing();
        setMachineTier(3);
        verifyStructure();
        machine.setRecipeLocking(true);
        for (int mode : new int[] { 0, 1, 0 }) {
            machine.setMachineMode(mode);
            require(machine.getSingleRecipeCheck() == null, "Mode switch clears the old recipe lock");
            seed(10, 1);
            require(machine.checkProcessing().wasSuccessful(), "Recipe locking mode " + mode);
            require(machine.getSingleRecipeCheck() != null, "Recipe lock established mode " + mode);
            resetProcessing();
        }
        report.append("Switching modes invalidates recipe locks and establishes the new map lock.\n");
    }

    private void seed(int meta, int amount) {
        var bus = machine.mInputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        bus.setInventorySlotContents(0, stack(meta, amount));
    }

    private int inputAmount() {
        ItemStack input = machine.mInputBusses.get(0).getStackInSlot(0);
        return input == null ? 0 : input.stackSize;
    }

    private int outputAmount() {
        return machine.mOutputItems == null ? 0
            : Arrays.stream(machine.mOutputItems).mapToInt(stack -> stack.stackSize).sum();
    }

    private void resetProcessing() {
        machine.mMaxProgresstime = 0;
        machine.mProgresstime = 0;
        machine.mOutputItems = null;
        machine.mOutputFluids = null;
        machine.lEUt = 0;
        if (!machine.mInputBusses.isEmpty()) seed(0, 0);
    }

    private void cleanupFixtures() {
        for (int mode = 0; mode < 2; mode++) {
            RecipeMap<?> recipes = map(mode);
            recipes.getBackend().removeRecipes(recipes.getAllRecipes().stream().filter(
                recipe -> Arrays.stream(recipe.mInputs).anyMatch(input -> input != null && input.getItem() == fixture))
                .collect(Collectors.toList()));
            require(recipes.getAllRecipes().size() == nativeCounts[mode],
                "Native recipes remain unchanged mode " + mode);
        }
    }

    private ItemStack stack(int meta, int amount) {
        return new ItemStack(fixture, amount, meta);
    }

    private static RecipeMap<?> map(int mode) {
        return mode == 0 ? GoodGeneratorRecipeMaps.preciseAssemblerRecipes : RecipeMaps.assemblerRecipes;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            require(++stageFrames <= 1200, "Visual stage timeout at mode " + openedMode);
            if (tooltipStage >= 0) {
                if (!(mc.currentScreen instanceof TooltipProbeGui) || ++frames < 40) return;
                String locale = tooltipStage < 2 ? "zh_CN" : "en_US";
                capture("tooltip-" + locale + (tooltipStage % 2 == 0 ? "-normal" : "-structure"));
                frames = stageFrames = 0;
                if (++tooltipStage == 4) {
                    report.append(
                        "Four rendered tooltip screenshots: normal and Shift structure, Chinese and English.\n");
                    finish();
                    return;
                }
                showTooltip();
                return;
            }
            if (openedMode < 0) {
                if (structureCaptured || requestedCasingTier >= 0) return;
                int tier = capturedCasingTier + 1;
                if (!verifyClientTextures(tier)) {
                    frames = 0;
                    return;
                }
                if (++frames < 40) return;
                capture("structure-" + tier);
                capturedCasingTier = tier;
                frames = stageFrames = 0;
                if (tier < 4) {
                    requestedCasingTier = tier + 1;
                    return;
                }
                structureCaptured = true;
                report.append("All five controller and attached hatch textures synchronize to the client.\n");
                requestedMode = 0;
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
            CycleButtonWidget button = findModeButton(wrapper.getScreen().getMainPanel());
            require(button != null, "Native two-mode GUI button");
            int expected = capturedMode + 1;
            if (openedMode != expected || button.getIntValue().getIntValue() != expected) {
                frames = 0;
                return;
            }
            if (++frames < 40) return;
            capture("mode-" + openedMode);
            capturedMode = openedMode;
            frames = stageFrames = 0;
            if (capturedMode == 0) {
                button.next();
                return;
            }
            tooltipStage = 0;
            showTooltip();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static boolean verifyClientTextures(int tier) {
        World world = Minecraft.getMinecraft().theWorld;
        if (
            !(world.getTileEntity(0, 12, 0) instanceof BaseMetaTileEntity clientBase)
                || !(clientBase.getMetaTileEntity() instanceof PrecisionAssembler clientMachine)
                || clientMachine.getMechanicalCasingTier() != tier
                || clientMachine.getCasingTextureId() != casingTexture(tier)
        ) return false;
        for (int x : new int[] { -4, -3, -2, -1, 1, 2, 3 }) {
            if (
                !(world.getTileEntity(x, 12, 0) instanceof BaseMetaTileEntity hatchBase)
                    || !(hatchBase.getMetaTileEntity() instanceof MTEHatch hatch)
                    || hatch.getCasingTexture() != Textures.BlockIcons.getCasingTextureForId(casingTexture(tier))
            ) return false;
        }
        return true;
    }

    private MultiblockTooltipBuilder verifyTooltips(String locale) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        for (Language language : mc.getLanguageManager().getLanguages()) {
            if (language.getLanguageCode().equals(locale)) {
                mc.getLanguageManager().setCurrentLanguage(language);
                mc.gameSettings.language = locale;
                mc.refreshResources();
                break;
            }
        }
        Method create = PrecisionAssembler.class.getDeclaredMethod("createTooltip");
        create.setAccessible(true);
        MultiblockTooltipBuilder tooltip = (MultiblockTooltipBuilder) create.invoke(machine);
        String text = String.join("\n", tooltip.getInformation()) + "\n\n"
            + String.join("\n", tooltip.getStructureInformation());
        require(!text.contains("gtng.precision.") && !text.contains("gt.mbtt."),
            "All tooltip translations resolve: " + locale);
        require(!text.contains("平均等级") && !text.contains("average tier") && !text.contains("MK-I"),
            "Tooltip excludes removed hatch restriction and original precision casing names: " + locale);
        require(text.contains("16") && text.contains("256") && text.contains("9") && text.contains("42"),
            "Tooltip includes real parallel and structure values: " + locale);
        Files.write(new File(output(), "tooltip-" + locale + ".txt").toPath(), text.getBytes(StandardCharsets.UTF_8));
        report.append("Tooltip translations resolve: ").append(locale).append(".\n");
        return tooltip;
    }

    private void showTooltip() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.guiScale = 1;
        if (tooltipStage % 2 == 0) currentTooltip = verifyTooltips(tooltipStage < 2 ? "zh_CN" : "en_US");
        List<String> lines = new ArrayList<>();
        lines.add(GTNGItemList.PrecisionAssembler.get(1).getDisplayName());
        lines.addAll(Arrays.asList(
            tooltipStage % 2 == 0 ? currentTooltip.getInformation() : currentTooltip.getStructureInformation()));
        mc.displayGuiScreen(new TooltipProbeGui(lines));
    }

    private static final class TooltipProbeGui extends GuiScreen {

        private final List<String> lines;
        private int tooltipX, tooltipY;

        private TooltipProbeGui(List<String> lines) {
            this.lines = lines;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void initGui() {
            int textWidth = lines.stream().mapToInt(fontRendererObj::getStringWidth).max().orElse(0);
            int textHeight = 8 + Math.max(0, lines.size() - 1) * 10 + (lines.size() > 1 ? 2 : 0);
            require(textWidth + 32 <= width && textHeight + 32 <= height,
                "Rendered tooltip fits viewport: " + textWidth + "x" + textHeight + " in " + width + "x" + height);
            tooltipX = (width - textWidth) / 2 - 12;
            tooltipY = (height - textHeight) / 2 + 12;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawRect(0, 0, width, height, 0xFF303238);
            drawHoveringText(lines, tooltipX, tooltipY, fontRendererObj);
        }
    }

    private static CycleButtonWidget findModeButton(IWidget widget) {
        if (widget instanceof CycleButtonWidget button && button.getStateCount() == 2) return button;
        for (IWidget child : widget.getChildren()) {
            CycleButtonWidget button = findModeButton(child);
            if (button != null) return button;
        }
        return null;
    }

    private void finish() throws Exception {
        Files.write(new File(output(), "result.txt").toPath(),
            ("PASS\n" + report + "GUI server packet round trip and both mode screenshots.\n")
                .getBytes(StandardCharsets.UTF_8));
        System.out.println("PRECISION_ASSEMBLER_QA_PASS " + report);
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.precisionAssembler.output"));
        folder.mkdirs();
        return folder;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
