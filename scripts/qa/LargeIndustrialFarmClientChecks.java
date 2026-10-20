package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.utils.item.SlotItemHandler;
import com.gtnewhorizon.cropsnh.api.CropsNHCrops;
import com.gtnewhorizon.cropsnh.api.CropsNHItemList;
import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.IGrowthRequirement;
import com.gtnewhorizon.cropsnh.api.IMachineGrowthRequirement;
import com.gtnewhorizon.cropsnh.api.ISeedData;
import com.gtnewhorizon.cropsnh.blocks.BlockSeedBed;
import com.gtnewhorizon.cropsnh.farming.SeedData;
import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.farming.registries.FertilizerRegistry;
import com.gtnewhorizon.cropsnh.farming.registries.HydrationRegistry;
import com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement;
import com.gtnewhorizon.cropsnh.init.CropsNHBlocks;
import com.gtnewhorizon.cropsnh.init.CropsNHFluids;
import com.gtnewhorizon.cropsnh.tileentity.multi.MTEIndustrialFarm;
import com.gtnewhorizon.cropsnh.tileentity.multi.MTEIndustrialFarmGui;
import com.gtnewhorizon.cropsnh.utility.CropsNHUtils;
import com.gtnewhorizon.cropsnh.utility.IFDropTable;
import com.xyp.gtnotgood.common.machines.hatch.WirelessMultiAmpEnergyHatch;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialFarm;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import bartworks.common.loaders.ItemRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.VoidingMode;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;
import gregtech.common.misc.WirelessNetworkManager;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyTunnel;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

@Mod(
    modid = "industrialfarmqa",
    name = "Industrial Farm QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class LargeIndustrialFarmClientChecks {

    private BaseMetaTileEntity base;
    private LargeIndustrialFarm machine;
    private Block frame;
    private int frameMeta, multiAmpId, normalEnergyId, laserId, ticks, clientTicks, frames, stage;
    private boolean started;
    private volatile boolean finished, checked, openRequested, opened, selectLastRequested, selectedLast;
    private volatile ItemStack expectedGuiSeed;
    private volatile ItemStack expectedGuiFirstEnvironment, expectedGuiSecondEnvironment;
    private final StringBuilder report = new StringBuilder();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.industrialFarm.qa")) return;
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks <= 6000, "client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.guiScale = 2;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0.0f);
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("industrial-farm-qa-" + System.currentTimeMillis(), "Industrial Farm QA",
                    new WorldSettings(1701L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
            require(++ticks <= 3600, "server timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (ticks == 1) setup(player);
            if (ticks == 40) {
                verifyStructures(player.worldObj);
                verifyWirelessEnergy(player.worldObj);
                verifyCrafting(player.worldObj);
                verifyProcessing();
                verifyPersistence(player.worldObj);
                verifyLargeDropCollection();
                verifyControllerDrops(player.worldObj);
                verifyCropSelection();
                checked = true;
            }
            if (openRequested && !opened) {
                player.playerNetServerHandler.setPlayerLocation(0.5, 12, -3, 0, 0);
                machine.setSelectedCrop(0);
                machine.mOutputItems = new ItemStack[0];
                expectedGuiSeed = machine.getCropSeed(0).copy();
                expectedGuiFirstEnvironment = machine.getCropEnvironment(0, 0).copy();
                expectedGuiSecondEnvironment = machine.getCropEnvironment(0, 1).copy();
                machine.onRightclick(base, player);
                verifyGuiTransfer(player);
                opened = true;
            }
            if (opened && selectLastRequested && !selectedLast) {
                machine.setSelectedCrop(35);
                expectedGuiSeed = machine.getCropSeed(35).copy();
                expectedGuiFirstEnvironment = machine.getCropEnvironment(35, 0).copy();
                expectedGuiSecondEnvironment = machine.getCropEnvironment(35, 1).copy();
                player.openContainer.detectAndSendChanges();
                selectedLast = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        world.setWorldTime(6000);
        base = place(world, 0, 12, 0, GTNGMachineID.LargeIndustrialFarm.id, ForgeDirection.NORTH);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        require(base.getMetaTileEntity() instanceof LargeIndustrialFarm, "new controller keeps its subclass");
        machine = (LargeIndustrialFarm) base.getMetaTileEntity();
        machine.construct(new ItemStack(Items.diamond), false);
        frame = world.getBlock(0, 14, 1);
        frameMeta = world.getBlockMetadata(0, 14, 1);
        require(frame != Blocks.air, "creative construction supplies all twelve wood frames");
        multiAmpId = findHatch(MTEHatchEnergyMulti.class);
        normalEnergyId = findHatch(MTEHatchEnergy.class);
        laserId = findHatch(MTEHatchEnergyTunnel.class);
        place(world, -1, 12, 0, multiAmpId, ForgeDirection.NORTH);
        place(world, 1, 12, 0, ItemList.Hatch_Input_EV.get(1).getItemDamage(), ForgeDirection.NORTH);
        place(world, -1, 13, 0, ItemList.Hatch_Output_Bus_EV.get(1).getItemDamage(), ForgeDirection.NORTH);
        place(world, 0, 13, 0, ItemList.Hatch_Maintenance.get(1).getItemDamage(), ForgeDirection.NORTH);
        place(world, 1, 13, 0, ItemList.Hatch_Input_Bus_EV.get(1).getItemDamage(), ForgeDirection.NORTH);
        place(world, 0, 14, 0, ItemList.Hatch_Input_EV.get(1).getItemDamage(), ForgeDirection.NORTH);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(-9, 17, -8, -36, 16);
    }

    private void verifyStructures(World world) {
        BaseMetaTileEntity largeTrigger = place(world, 32, 12, 0, GTNGMachineID.LargeIndustrialFarm.id,
            ForgeDirection.NORTH);
        largeTrigger.disableWorking();
        ((LargeIndustrialFarm) largeTrigger.getMetaTileEntity()).construct(new ItemStack(Items.diamond, 64), false);
        require(occupied(world, 0).equals(occupied(world, 32)),
            "one and sixty-four diamonds construct identical bounds");
        require(count(world, 0, CropsNHBlocks.blockSeedBed) == 36 && count(world, 32, CropsNHBlocks.blockSeedBed) == 36,
            "fixed maximum structure has thirty-six seed beds");
        require(count(world, 0, ItemRegistry.bw_realglas) == 48,
            "fixed structure has forty-eight borosilicate glass blocks");
        require(occupied(world, 0).size() == 150, "fixed 5 x 4 x 14 native farm silhouette");
        for (int x : new int[] { 64, 96 }) {
            BaseMetaTileEntity original = place(world, x, 12, 0,
                CropsNHItemList.IndustrialFarmController.get(1).getItemDamage(), ForgeDirection.NORTH);
            original.disableWorking();
            require(
                original.getMetaTileEntity().getClass() == MTEIndustrialFarm.class
                    && original.getMetaTileEntity().getMetaName().equals("multimachine.industrialfarm"),
                "original controller keeps its native class and name");
            ((MTEIndustrialFarm) original.getMetaTileEntity()).construct(new ItemStack(Items.diamond, x == 64 ? 1 : 64),
                false);
        }
        require(count(world, 64, CropsNHBlocks.blockSeedBed) == 3 && count(world, 96, CropsNHBlocks.blockSeedBed) == 36,
            "original farm retains its variable structure length");
        beds(VoltageIndex.MV);
        valid("MV beds accept UV or higher multi-amp energy without an overclock upgrade");
        require(machine.expectedOCs == 0 && machine.seedCapacity == 12 * BlockSeedBed.getCapacity(VoltageIndex.MV),
            "every seed type receives twelve times native MV capacity");
        require(machine.seedCapacity == 2700 && machine.getTotalSeedCapacity() == 97200,
            "MV provides 2700 independent seeds per type and 97200 seeds across thirty-six types");
        require(machine.getCropInventory().getSlots() == 108,
            "each of thirty-six seed types has two independent environment slots without soil storage");
        upgrade(0, CropsNHBlocks.blockEnvironmentalEnhancementUnit, VoltageIndex.MV);
        upgrade(1, CropsNHBlocks.blockEnvironmentalEnhancementUnit, VoltageIndex.UXV);
        upgrade(2, CropsNHBlocks.blockFertilizerUnit, VoltageIndex.IV);
        upgrade(3, CropsNHBlocks.blockAdvancedHarvestingUnit, VoltageIndex.EV);
        upgrade(4, CropsNHBlocks.blockAdvancedHarvestingUnit, VoltageIndex.UV);
        upgrade(5, CropsNHBlocks.blockOverclockedGrowthAccelerationUnit, VoltageIndex.ZPM);
        valid("MV beds accept mixed independent module tiers and a ZPM overclock module");
        require(machine.environmentalEnhancementUnitCount == 2 && machine.fertilizerUnitCount == 1
            && machine.advancedHarvestingUnitCount == 2
            && machine.overclockedGrowthAccelerationUnitCount == 1
            && machine.upgradeTier == VoltageIndex.MV
            && machine.expectedOCs > 0, "native upgrade effects and overclocking remain active on MV beds");
        upgrade(6, CropsNHBlocks.blockGrowthAccelerationUnit, VoltageIndex.UXV);
        valid("ordinary growth acceleration and overclock acceleration coexist");
        require(machine.growthAccelerationUnitCount == 1 && machine.overclockedGrowthAccelerationUnitCount == 1,
            "coexisting growth modules are both counted");
        frame(6);
        world.setBlock(-1, 12, 1, CropsNHBlocks.blockSeedBed, VoltageIndex.HV, 3);
        invalid("nonuniform seed beds rejected");
        world.setBlock(-1, 12, 1, CropsNHBlocks.blockSeedBed, VoltageIndex.MV, 3);
        upgrade(2, CropsNHBlocks.blockFertilizerUnit, VoltageIndex.LV);
        invalid("invalid upgrade metadata rejected");
        upgrade(2, CropsNHBlocks.blockFertilizerUnit, VoltageIndex.IV);
        upgrade(5, CropsNHBlocks.blockOverclockedGrowthAccelerationUnit, VoltageIndex.MV);
        invalid("overclock modules retain their native ZPM minimum metadata");
        upgrade(5, CropsNHBlocks.blockOverclockedGrowthAccelerationUnit, VoltageIndex.ZPM);
        world.setBlock(-1, 14, 1, Blocks.glass, 0, 3);
        invalid("ordinary glass rejected");
        world.setBlock(-1, 14, 1, ItemRegistry.bw_realglas, 0, 3);
        world.setBlockToAir(0, 14, 13);
        invalid("short or incomplete maximum structure rejected");
        casing(0, 14, 13);
        Block[] limited = { CropsNHBlocks.blockEnvironmentalEnhancementUnit, CropsNHBlocks.blockFertilizerUnit,
            CropsNHBlocks.blockAdvancedHarvestingUnit, CropsNHBlocks.blockOverclockedGrowthAccelerationUnit };
        String[] limits = { "environment unit count", "fertilizer unit count", "harvesting unit count",
            "overclock unit count" };
        for (int i = 0; i < limited.length; i++) {
            upgrade(6, limited[i], VoltageIndex.UXV);
            invalid(limits[i] + " retained");
            frame(6);
        }
        place(world, 0, 14, 13, multiAmpId, ForgeDirection.SOUTH);
        invalid("multiple multi-amp hatches rejected");
        place(world, 0, 14, 13, normalEnergyId, ForgeDirection.SOUTH);
        invalid("mixing normal and multi-amp energy hatches rejected");
        casing(0, 14, 13);
        place(world, -1, 12, 0, laserId, ForgeDirection.NORTH);
        invalid("laser energy hatch rejected");
        place(world, -1, 12, 0, normalEnergyId, ForgeDirection.NORTH);
        valid("borosilicate glass accepts high-tier normal energy hatch");
        place(world, -1, 12, 0, multiAmpId, ForgeDirection.NORTH);
        for (int i = 0; i < 12; i++) frame(i);
        beds(VoltageIndex.UXV);
        valid("uniform UXV seed beds remain accepted at the same fixed size");
        require(machine.seedCapacity == 12 * BlockSeedBed.getCapacity(VoltageIndex.UXV),
            "twelve times UXV seed capacity for each seed type");
        require(machine.seedCapacity == 41772 && machine.getTotalSeedCapacity() == 1503792,
            "UXV provides 41772 independent seeds per type");
        beds(VoltageIndex.MV);
        valid("restore MV farm without upgrades");
    }

    private void verifyWirelessEnergy(World world) {
        GTNGItemList[] fixtures = { GTNGItemList.WirelessMultiAmpEnergyMV256A,
            GTNGItemList.WirelessMultiAmpEnergyUV256A, GTNGItemList.WirelessMultiAmpEnergyUXV256A };
        for (int index = 0; index < fixtures.length; index++) {
            int id = fixtures[index].get(1).getItemDamage();
            BaseMetaTileEntity wirelessBase = place(world, -1, 12, 0, id, ForgeDirection.NORTH);
            require(wirelessBase.getMetaTileEntity() instanceof MTEHatchWirelessMulti,
                "catalog multi-amp hatch retains its native wireless energy implementation");
            MTEHatchWirelessMulti wireless = (MTEHatchWirelessMulti) wirelessBase.getMetaTileEntity();
            require(
                index == 2 ? !(wireless instanceof WirelessMultiAmpEnergyHatch)
                    : wireless instanceof WirelessMultiAmpEnergyHatch,
                "project hatches and reused native UXV alias are covered");
            valid("MV seed beds and standard borosilicate glass accept " + fixtures[index]
                + " without overclock upgrades");
            require(
                wireless.getHatchType() == 1 && wireless.getConnectionType() == MTEHatch.ConnectionType.WIRELESS
                    && machine.getExoticEnergyHatches().size() == 1
                    && machine.getExoticEnergyHatches().contains(wireless)
                    && machine.mEnergyHatches.isEmpty(),
                "wireless multi-amp hatches register as exotic wireless inputs");
            long powerBudget = wirelessBase.getInputVoltage() * wireless.maxWorkingAmperesIn();
            require(
                machine.getMaxInputEu() == powerBudget && machine.expectedOCs == 0
                    && machine.expectedEUt == BlockSeedBed.getBaseEUt(VoltageIndex.MV),
                "unupgraded farm reads the wireless hatch's actual power budget without forced overclocking");
            place(world, 0, 14, 13, id, ForgeDirection.SOUTH);
            invalid("two wireless multi-amp hatches retain the one-exotic-input limit");
            place(world, 0, 14, 13, normalEnergyId, ForgeDirection.SOUTH);
            invalid("wireless multi-amp and normal energy hatches remain mutually exclusive");
            casing(0, 14, 13);
            upgrade(0, CropsNHBlocks.blockOverclockedGrowthAccelerationUnit, VoltageIndex.ZPM);
            valid("wireless multi-amp input power enables normal farm overclock calculation");
            long expectedPower = BlockSeedBed.getBaseEUt(VoltageIndex.MV);
            int expectedOverclocks = 0;
            while (expectedPower <= powerBudget / 4) {
                expectedPower *= 4;
                expectedOverclocks++;
            }
            require(
                machine.expectedOCs == expectedOverclocks && machine.expectedEUt == expectedPower
                    && machine.expectedOCs > 0,
                "wireless voltage and amperage supply the correct overclock count and EU/t");
            frame(0);
            valid("wireless farm also remains valid after removing its overclock module");
            verifyWirelessFunding(wirelessBase, wireless);
        }
        place(world, -1, 12, 0, laserId, ForgeDirection.NORTH);
        invalid("ordinary wired laser energy hatches remain rejected after wireless support");
        place(world, -1, 12, 0, multiAmpId, ForgeDirection.NORTH);
        valid("restore the standard multi-amp farm after wireless energy checks");
        report.append(
            "Project MV/UV and native UXV wireless multi-amp hatches form, fund, overclock and power the farm.\n");
    }

    private void verifyWirelessFunding(BaseMetaTileEntity wirelessBase, MTEHatchWirelessMulti wireless) {
        UUID owner = UUID.randomUUID();
        wirelessBase.setOwnerName(base.getOwnerName());
        wirelessBase.setOwnerUuid(owner);
        wireless.setEUVar(0);
        WirelessNetworkManager.strongCheckOrAddUser(owner);
        BigInteger credit = BigInteger.valueOf(wireless.eu_transferred_per_operation_long)
            .multiply(BigInteger.valueOf(3));
        WirelessNetworkManager.setUserEU(owner, credit);
        wireless.onFirstTick(wirelessBase);
        long funded = wirelessBase.getStoredEU();
        require(
            owner.equals(wireless.owner_uuid) && funded > 0
                && WirelessNetworkManager.getUserEU(owner).equals(credit.subtract(BigInteger.valueOf(funded))),
            "placed wireless hatch debits its real owner's network and fills its physical EU buffer");
        BigInteger networkBefore = WirelessNetworkManager.getUserEU(owner);
        long demand = machine.expectedEUt;
        require(
            machine.drainEnergyInput(demand) && wirelessBase.getStoredEU() == funded - demand
                && WirelessNetworkManager.getUserEU(owner).equals(networkBefore),
            "the real farm energy drain consumes EU from its registered wireless hatch");
        long beforeRefill = wirelessBase.getStoredEU();
        wireless.tryFetchingEnergy();
        long refilled = wirelessBase.getStoredEU() - beforeRefill;
        require(
            refilled > 0
                && WirelessNetworkManager.getUserEU(owner).equals(networkBefore.subtract(BigInteger.valueOf(refilled))),
            "wireless refills conserve network and buffered EU exactly");
        WirelessNetworkManager.setUserEU(owner, BigInteger.ZERO);
        wireless.setEUVar(0);
        wireless.tryFetchingEnergy();
        require(
            wirelessBase.getStoredEU() == 0 && !machine.drainEnergyInput(demand)
                && WirelessNetworkManager.getUserEU(owner).signum() == 0,
            "an unfunded wireless hatch cannot power the farm");
    }

    private void verifyProcessing() throws Exception {
        verifyInventoryLimits();
        verifyUpgradeScaling();
        clearCrops();
        machine.mInputBusses.get(0).mInventory[0] = seed(CropsNHCrops.Carrot, 16);
        machine.setMachineMode(MTEIndustrialFarm.MODE_INPUT);
        require(machine.checkProcessing().wasSuccessful() && machine.getCropSeed(0).stackSize == 16,
            "input mode ingests analyzed CropsNH carrot seeds");
        machine.mInputBusses.get(0).mInventory[0] = seed(CropsNHCrops.Carrot, 3);
        require(machine.checkProcessing().wasSuccessful() && machine.getCropSeed(0).stackSize == 19
            && machine.getOccupiedCropCount() == 1, "input mode tops up an existing matching crop");
        machine.mInputBusses.get(0).mInventory[0] = seed(CropsNHCrops.Potato, 7);
        require(machine.checkProcessing().wasSuccessful() && machine.getCropSeed(1).stackSize == 7,
            "input mode assigns a different crop to the next empty crop");
        machine.setMachineMode(MTEIndustrialFarm.MODE_FARM);
        machine.outputTracker.addDrop(new ItemStack(Items.carrot), 0.375);
        NBTTagCompound before = snapshot();
        require(machine.checkProcessing() == MTEIndustrialFarm.NOT_ENOUGH_WATER && before.equals(snapshot()),
            "insufficient water preserves fractional output and all crops");
        MTEHatchInput water = machine.mInputHatches.get(0);
        MTEHatchInput fertilizer = machine.mInputHatches.get(1);
        supplies();
        long hydration = machine.getWaterRequirement(26);
        long fertility = machine.getFertilizerRequirement(26);
        require(hydration == (int) Math.ceil(26 * MTEIndustrialFarm.CYCLE_TICK_RATE_SCALAR),
            "water is charged for the actual twenty-six seeds across both crops");
        successfulCycle();
        require(
            water.mFluid.amount
                == 10000000 - ceiling(hydration, HydrationRegistry.instance.getPotency(FluidRegistry.WATER))
                && fertilizer.mFluid.amount
                    == 10000000 - ceiling(fertility, FertilizerRegistry.instance.getPotency(CropsNHFluids.fertilizer)),
            "both crops consume exactly their actual water and ordinary fertilizer requirements");
        verifyOutputProtection();
        upgrade(0, CropsNHBlocks.blockFertilizerUnit, VoltageIndex.UXV);
        valid("UXV fertilizer module works on MV beds");
        supplies();
        before = snapshot();
        require(
            machine.checkProcessing() == MTEIndustrialFarm.NOT_ENOUGH_Enriched_FERTILIZER
                && water.mFluid.amount == 10000000
                && fertilizer.mFluid.amount == 10000000
                && before.getTag("fraction").equals(machine.outputTracker.save()),
            "enriched fertilizer failure consumes no water and preserves fractional output");
        fertility = machine.getFertilizerRequirement(26);
        require(fertility
            == (long) Math.ceil(26 * MTEIndustrialFarm.CYCLE_TICK_RATE_SCALAR / machine.getFertilizerStrength())
            && fertility < hydration, "higher-tier fertilizer modules reduce the required fertilizer potency");
        fertilizer.mFluid = new FluidStack(CropsNHFluids.enrichedFertilizer, 10000000);
        successfulCycle();
        require(fertilizer.mFluid.amount == 10000000 - fertility, "enriched fertilizer retains native unit potency");
        frame(0);
        valid("restore farm without fertilizer module");
        supplies();
        fertilizer.mFluid = new FluidStack(CropsNHFluids.fertilizer, 1);
        require(machine.checkProcessing().wasSuccessful() && !machine.hasFertilizer && fertilizer.mFluid.amount == 1,
            "partial optional fertilizer remains unconsumed without a fertilizer unit");
        require(machine.getUsedSeedCount() == 26, "farming preserves planted seed counts in every crop");
        verifySoilFreeFarming();
        verifyIndependentEnvironments();
        verifyThirtySixCrops();
        report.append("Inventory limits, tier bonuses, perfect overclocks, actual resource use and 36 crops passed.\n");
    }

    private void successfulCycle() {
        successfulCycle(-1);
    }

    private void successfulCycle(int excludedCrop) {
        machine.hasFertilizer = true;
        machine.mMaxProgresstime = 0;
        int selectedCrop = machine.getSelectedCrop();
        IFDropTable expected = new IFDropTable(machine.outputTracker.save());
        for (int crop = 0; crop < 36; crop++) {
            if (crop == excludedCrop) continue;
            ItemStack seeds = machine.getCropSeed(crop);
            if (seeds == null) continue;
            machine.setSelectedCrop(crop);
            machine.getDropsPerCycle(CropsNHUtils.getSeedData(seeds, false, true)).addTo(expected, seeds.stackSize);
        }
        machine.setSelectedCrop(selectedCrop);
        ItemStack[] drops = expected.getDrops(false);
        CheckRecipeResult result = machine.checkProcessing();
        require(result.wasSuccessful() && machine.hasFertilizer
            && machine.mMaxProgresstime == MTEIndustrialFarm.CYCLE_DURATION
            && machine.lEUt == -machine.expectedEUt, "production cycle, duration and EU usage: " + result.getID());
        require(
            !expected.isEmpty() && sameItems(drops, machine.mOutputItems) && sameDrops(expected, machine.outputTracker),
            "aggregated crop yield and fractional drop tracking for every eligible crop");
    }

    private void verifyInventoryLimits() {
        clearCrops();
        ItemStack oversized = seed(CropsNHCrops.Carrot, machine.seedCapacity + 9);
        require(machine.canEditCrops(), "stopped controller permits crop editing");
        ItemStack remainder = machine.getCropInventory().insertItem(0, oversized, true);
        require(remainder != null && remainder.stackSize == 9 && machine.getUsedSeedCount() == 0,
            "manual insertion simulation respects one seed type's capacity without changing inventory");
        remainder = machine.getCropInventory().insertItem(0, oversized, false);
        require(remainder != null && remainder.stackSize == 9 && machine.getUsedSeedCount() == machine.seedCapacity,
            "manual insertion accepts the full expanded capacity and returns the excess");
        ItemStack other = seed(CropsNHCrops.Potato, 4);
        remainder = machine.getCropInventory().insertItem(3, other, false);
        require(
            remainder == null && machine.getCropSeed(1).stackSize == 4
                && machine.getUsedSeedCount() == machine.seedCapacity + 4,
            "a full first seed type does not block manual insertion of a second type");
        remainder = machine.getCropInventory().insertItem(3, seed(CropsNHCrops.Potato, machine.seedCapacity + 9),
            false);
        require(
            remainder != null && remainder.stackSize == 13 && machine.getCropSeed(1).stackSize == machine.seedCapacity,
            "the second seed type independently fills its own capacity and returns excess seeds");
        remainder = machine.getCropInventory().insertItem(6, seed(CropsNHCrops.Carrot, 1), false);
        require(remainder != null && remainder.stackSize == 1 && machine.getCropSeed(2) == null,
            "duplicate seeds cannot create another page to bypass their per-type capacity");
        machine.setCropSeed(1, null);
        machine.setMachineMode(MTEIndustrialFarm.MODE_INPUT);
        machine.mInputBusses.get(0).mInventory[0] = seed(CropsNHCrops.Potato, machine.seedCapacity + 7);
        require(
            machine.checkProcessing().wasSuccessful() && machine.getCropSeed(1).stackSize == machine.seedCapacity
                && machine.getUsedSeedCount() == 2 * machine.seedCapacity
                && machine.mInputBusses.get(0).mInventory[0].stackSize == 7,
            "input mode fills a second type independently while the first type is full");
        machine.mInputBusses.get(0).mInventory[0] = seed(CropsNHCrops.Carrot, 3);
        require(
            !machine.checkProcessing().wasSuccessful() && machine.getCropSeed(0).stackSize == machine.seedCapacity
                && machine.mInputBusses.get(0).mInventory[0].stackSize == 3,
            "input mode leaves excess seeds of an individually full type unconsumed");
        machine.mInputBusses.get(0).mInventory[0] = null;
        machine.mMaxProgresstime = 5;
        require(!machine.canEditCrops() && machine.getCropInventory().extractItem(0, 1, false) == null,
            "running cycles lock all crop extraction");
        machine.mMaxProgresstime = 0;
        base.enableWorking();
        require(!machine.canEditCrops(), "enabled controller locks manual crop editing");
        base.disableWorking();
        require(machine.getCropInventory().extractItem(0, 1, false).stackSize == 1,
            "stopped controller unlocks manual extraction");
        clearCrops();
    }

    private void verifyUpgradeScaling() {
        ISeedData carrot = CropsNHUtils.getSeedData(seed(CropsNHCrops.Carrot, 1), false, true);
        machine.hasFertilizer = true;
        double baseGrowth = machine.getGrowthProgressPerCycle(carrot);
        upgrade(0, CropsNHBlocks.blockGrowthAccelerationUnit, VoltageIndex.MV);
        valid("minimum-tier growth module accepted");
        double minimumGrowth = machine.getGrowthProgressPerCycle(carrot);
        require(minimumGrowth > baseGrowth && close(machine.getGrowthAccelerationStrength(), 1),
            "minimum-tier growth module provides native strength");
        upgrade(0, CropsNHBlocks.blockGrowthAccelerationUnit, VoltageIndex.UXV);
        valid("high-tier growth module accepted on MV beds");
        double upgradedGrowth = machine.getGrowthProgressPerCycle(carrot);
        require(upgradedGrowth > minimumGrowth && close(machine.getGrowthAccelerationStrength(), 3.75),
            "UXV module gains one-quarter strength per tier above MV");
        upgrade(1, CropsNHBlocks.blockOverclockedGrowthAccelerationUnit, VoltageIndex.ZPM);
        valid("ordinary and overclock growth upgrades both apply");
        int actualOCs = machine.expectedOCs;
        require(actualOCs > 0, "high-tier multi-amp energy enables native overclock count");
        machine.expectedOCs = 0;
        double noOC = machine.getGrowthProgressPerCycle(carrot);
        long noOCWater = machine.getWaterRequirement(17);
        machine.expectedOCs = 2;
        require(
            close(machine.getGrowthProgressPerCycle(carrot), noOC * 16)
                && close(machine.getPerfectOverclockMultiplier(), 16)
                && machine.getWaterRequirement(17)
                    == (long) Math.ceil(17 * MTEIndustrialFarm.CYCLE_TICK_RATE_SCALAR * 16)
                && machine.getWaterRequirement(17) > noOCWater,
            "two perfect overclocks multiply growth and actual-seed resource use by sixteen");
        machine.expectedOCs = actualOCs;
        upgrade(1, CropsNHBlocks.blockOverclockedGrowthAccelerationUnit, VoltageIndex.UV);
        valid("higher-tier overclock module accepted");
        machine.expectedOCs = 2;
        require(close(machine.getPerfectOverclockMultiplier(), 20),
            "UV overclock module adds its independent 1.25 tier strength");
        machine.expectedOCs = 32;
        require(machine.getWaterRequirement(machine.seedCapacity) > Integer.MAX_VALUE,
            "extreme perfect-overclock demand cannot wrap into a small fluid charge");
        frame(0);
        frame(1);
        upgrade(0, CropsNHBlocks.blockEnvironmentalEnhancementUnit, VoltageIndex.MV);
        valid("minimum environmental module accepted");
        int nutrients = machine.getNutrientScore(carrot);
        upgrade(0, CropsNHBlocks.blockEnvironmentalEnhancementUnit, VoltageIndex.UXV);
        valid("high-tier environmental module accepted");
        require(close(machine.getEnvironmentalStrength(), 3.75) && machine.getNutrientScore(carrot) > nutrients,
            "environmental module tiers increase available nutrients");
        int highTierNutrients = machine.getNutrientScore(carrot);
        upgrade(1, CropsNHBlocks.blockEnvironmentalEnhancementUnit, VoltageIndex.MV);
        valid("mixed-tier environment modules accepted");
        require(machine.getNutrientScore(carrot) >= highTierNutrients,
            "adding a low-tier environment module cannot reduce nutrient efficiency");
        frame(1);
        frame(0);
        upgrade(0, CropsNHBlocks.blockFertilizerUnit, VoltageIndex.MV);
        upgrade(1, CropsNHBlocks.blockAdvancedHarvestingUnit, VoltageIndex.MV);
        valid("minimum fertilizer and harvesting modules accepted");
        double minimumYield = dropCount(machine.getDropsPerCycle(carrot));
        upgrade(0, CropsNHBlocks.blockFertilizerUnit, VoltageIndex.UXV);
        upgrade(1, CropsNHBlocks.blockAdvancedHarvestingUnit, VoltageIndex.UXV);
        valid("high-tier fertilizer and harvesting modules accepted");
        require(
            close(machine.getFertilizerStrength(), 3.75) && close(machine.getHarvestingStrength(), 3.75)
                && dropCount(machine.getDropsPerCycle(carrot)) > minimumYield,
            "fertilizer and harvesting tiers improve growth and crop yield");
        frame(0);
        frame(1);
        valid("restore unupgraded farm after module tests");
    }

    private void verifyOutputProtection() {
        var output = machine.mOutputBusses.get(0);
        ItemStack[] saved = new ItemStack[output.mInventory.length];
        for (int slot = 0; slot < saved.length; slot++) {
            saved[slot] = output.mInventory[slot];
            output.mInventory[slot] = new ItemStack(Items.diamond, 64);
        }
        supplies();
        NBTTagCompound before = snapshot();
        require(machine.checkProcessing() == CheckRecipeResultRegistry.ITEM_OUTPUT_FULL,
            "full output bus rejects the complete multi-crop production cycle");
        require(before.equals(snapshot()), "output protection preserves every crop, fluid and fractional drop");
        for (int slot = 0; slot < saved.length; slot++) output.mInventory[slot] = saved[slot];
    }

    private void verifySoilFreeFarming() throws Exception {
        clearCrops();
        ICropCard soilCrop = null;
        for (Field field : CropsNHCrops.class.getFields()) {
            if (field.getType() != ICropCard.class) continue;
            ICropCard card = (ICropCard) field.get(null);
            if (card == null || card.getMinSeedBedTier() > machine.upgradeTier) continue;
            ISeedData data = CropsNHUtils.getSeedData(seed(card, 2), false, true);
            if (machine.getGrowthSpeedUnscaled(data) <= 0 || !requirementsMet(data, null)) continue;
            boolean needsSoil = card.getGrowthRequirements().stream().anyMatch(SubSoilRequirement.class::isInstance);
            if (!needsSoil) continue;
            soilCrop = card;
            break;
        }
        require(soilCrop != null, "registered sub-soil crop fixture");
        machine.setMachineMode(MTEIndustrialFarm.MODE_INPUT);
        machine.mInputBusses.get(0).mInventory[0] = seed(soilCrop, 32);
        machine.mInputBusses.get(0).mInventory[1] = new ItemStack(Blocks.dirt, 16);
        require(
            machine.checkProcessing().wasSuccessful() && machine.getCropSeed(0).stackSize == 32
                && machine.getSubSoilStack() == null
                && itemCount(machine.mInputBusses.get(0).mInventory, new ItemStack(Blocks.dirt)) == 16,
            "a crop with native soil requirements accepts input without consuming unrelated soil blocks");
        supplies();
        machine.setMachineMode(MTEIndustrialFarm.MODE_FARM);
        successfulCycle();
        verifyResourceCharge(32, "soil-free farming charges only the actual seed count");
        machine.mMaxProgresstime = 0;
        ItemStack extracted = machine.getCropInventory().extractItem(0, 1, false);
        require(extracted != null && extracted.stackSize == 1 && machine.getCropSeed(0).stackSize == 31,
            "native soil requirements do not prevent manual seed extraction");
        require(machine.getCropInventory().insertItem(0, extracted, false) == null
            && machine.getCropSeed(0).stackSize == 32, "manually extracted seeds can be reinserted without soil");
        ItemStack rejected = machine.ifStackHandler.insertItem(1, new ItemStack(Blocks.stone, 16), false);
        require(rejected != null && rejected.stackSize == 16 && machine.ifStackHandler.getStackInSlot(1) == null,
            "the removed soil bridge slot rejects insertion without consuming items");
        boolean unsupported = false;
        try {
            machine.ifStackHandler.setStackInSlot(1, new ItemStack(Blocks.stone));
        } catch (UnsupportedOperationException expected) {
            unsupported = true;
        }
        require(unsupported, "direct writes cannot populate the removed soil bridge slot");
        machine.setMachineMode(MTEIndustrialFarm.MODE_OUTPUT);
        int occupied = machine.getOccupiedCropCount();
        require(
            machine.checkProcessing().wasSuccessful() && machine.getOccupiedCropCount() == occupied - 1
                && machine.mMaxProgresstime == 5
                && sameItems(new ItemStack[] { seed(soilCrop, 32) }, machine.mOutputItems),
            "output mode safely ejects only seeds from one crop per five-tick transaction");
        require(itemCount(machine.mInputBusses.get(0).mInventory, new ItemStack(Blocks.dirt)) == 16,
            "unrelated soil input remains untouched through farming and seed output");
        report.append("Native soil-requiring crop grows and supports input, extraction and output without soil.\n");
        clearCrops();
    }

    private void verifyIndependentEnvironments() {
        clearCrops();
        upgrade(0, CropsNHBlocks.blockEnvironmentalEnhancementUnit, VoltageIndex.MV);
        upgrade(1, CropsNHBlocks.blockEnvironmentalEnhancementUnit, VoltageIndex.MV);
        valid("two environmental enhancement units unlock two cards for every seed type");
        machine.setCropSeed(0, seed(CropsNHCrops.Carrot, 32));
        machine.setCropSeed(1, seed(CropsNHCrops.Potato, 32));
        machine.setCropEnvironment(0, 0, CropsNHItemList.environmentalModule_HOT.get(1));
        machine.setCropEnvironment(0, 1, CropsNHItemList.environmentalModule_FOREST.get(1));
        machine.setCropEnvironment(1, 0, CropsNHItemList.environmentalModule_WET.get(1));
        machine.setCropEnvironment(1, 1, CropsNHItemList.environmentalModule_LUSH.get(1));
        ItemStack extracted = machine.getCropInventory().extractItem(1, 1, false);
        require(
            sameStack(CropsNHItemList.environmentalModule_HOT.get(1), extracted)
                && machine.getCropEnvironment(0, 0) == null
                && sameStack(CropsNHItemList.environmentalModule_FOREST.get(1), machine.getCropEnvironment(0, 1))
                && sameStack(CropsNHItemList.environmentalModule_WET.get(1), machine.getCropEnvironment(1, 0)),
            "manual environment extraction changes only the selected seed type");
        require(machine.getCropInventory().insertItem(1, extracted, false) == null,
            "environment cards reinsert into their own seed page");
        ISeedData carrot = CropsNHUtils.getSeedData(seed(CropsNHCrops.Carrot, 1), false, true);
        machine.hasFertilizer = true;
        machine.setSelectedCrop(0);
        int hotNutrients = machine.getNutrientScore(carrot);
        machine.setSelectedCrop(1);
        int wetNutrients = machine.getNutrientScore(carrot);
        require(hotNutrients > wetNutrients, "HOT and WET cards affect the same crop only in their own page context");
        machine.setCropEnvironment(0, 0, CropsNHItemList.environmentalModule_COLD.get(1));
        require(machine.getNutrientScore(carrot) == wetNutrients,
            "editing another page's environment cards cannot change this page's nutrients");
        machine.setSelectedCrop(0);
        require(machine.getNutrientScore(carrot) < hotNutrients,
            "the edited page immediately uses its own replacement environment card");
        machine.setCropEnvironment(0, 0, CropsNHItemList.environmentalModule_HOT.get(1));
        machine.setSelectedCrop(1);
        extracted = machine.ifStackHandler.extractItem(2, 1, false);
        require(
            sameStack(CropsNHItemList.environmentalModule_WET.get(1), extracted)
                && machine.getCropEnvironment(1, 0) == null
                && sameStack(CropsNHItemList.environmentalModule_HOT.get(1), machine.getCropEnvironment(0, 0)),
            "the native environment slot edits only the current seed page");
        require(machine.ifStackHandler.insertItem(2, extracted, false) == null,
            "the native environment slot restores the current page's card");
        machine.setMachineMode(MTEIndustrialFarm.MODE_FARM);
        supplies();
        successfulCycle();
        verifyResourceCharge(64, "two distinct environment configurations farm together with correct resource charges");
        clearCrops();
        machine.setSelectedCrop(0);
        frame(0);
        frame(1);
        valid("restore the unupgraded farm after independent environment checks");
        report
            .append("Each seed type's two environment cards independently affect nutrients, editing and production.\n");
    }

    private void verifyThirtySixCrops() throws Exception {
        clearCrops();
        machine.hasFertilizer = true;
        List<ICropCard> cards = simpleCrops();
        require(cards.size() >= 36, "at least thirty-six registered crops can grow in this fixture: " + cards.size());
        machine.setMachineMode(MTEIndustrialFarm.MODE_INPUT);
        for (int crop = 0; crop < 36; crop++) {
            machine.mInputBusses.get(0).mInventory[0] = seed(cards.get(crop), 1 + crop % 3);
            require(machine.checkProcessing().wasSuccessful(), "input routing for crop " + cards.get(crop).getId());
        }
        Set<String> planted = new HashSet<>();
        for (int crop = 0; crop < 36; crop++) {
            ItemStack stack = machine.getCropSeed(crop);
            require(stack != null && stack.stackSize == 1 + crop % 3, "independent input count in crop " + crop);
            planted.add(CropsNHUtils.getSeedData(stack, false, true).getCrop().getId());
        }
        require(planted.size() == 36 && machine.getOccupiedCropCount() == 36 && machine.getUsedSeedCount() == 72,
            "all thirty-six crops hold distinct registered crop types simultaneously");
        ItemStack extraSeeds = seed(cards.get(cards.size() > 36 ? 36 : 0), 3);
        if (cards.size() == 36) {
            if (!extraSeeds.hasTagCompound()) extraSeeds.setTagCompound(new NBTTagCompound());
            extraSeeds.getTagCompound().setBoolean("industrialFarmQaDistinctSeed", true);
        }
        for (int crop = 0; crop < 36; crop++) require(!GTUtility.areStacksEqual(extraSeeds, machine.getCropSeed(crop)),
            "distinct thirty-seventh seed fixture");
        machine.mInputBusses.get(0).mInventory[0] = extraSeeds;
        require(
            !machine.checkProcessing().wasSuccessful() && machine.getUsedSeedCount() == 72
                && machine.mInputBusses.get(0).mInventory[0].stackSize == 3,
            "a thirty-seventh seed identity remains in the input when every crop is occupied");
        machine.mInputBusses.get(0).mInventory[0] = null;
        machine.setMachineMode(MTEIndustrialFarm.MODE_FARM);
        machine.outputTracker.clear();
        supplies();
        successfulCycle();
        require(
            machine.mInputHatches.get(0).mFluid.amount == 10000000
                - ceiling(machine.getWaterRequirement(72), HydrationRegistry.instance.getPotency(FluidRegistry.WATER)),
            "all thirty-six crop types participate in the same correctly charged production cycle");
        require(machine.seedCapacity == 2700, "full-capacity production fixture uses MV seed beds");
        for (int crop = 0; crop < 36; crop++) {
            ItemStack full = machine.getCropSeed(crop).copy();
            full.stackSize = machine.seedCapacity;
            machine.setCropSeed(crop, full);
        }
        require(machine.getUsedSeedCount() == 97200 && machine.getUsedSeedCount() == machine.getTotalSeedCapacity(),
            "all thirty-six independent MV pages hold 2700 seeds each");
        VoidingMode originalMode = machine.getVoidingMode();
        machine.setVoidingMode(machine.getAllowedVoidingModes().stream().filter(mode -> !mode.protectItem).findFirst()
            .orElseThrow(() -> new AssertionError("Missing item voiding mode for bulk production fixture")));
        try {
            machine.outputTracker.clear();
            supplies();
            successfulCycle();
            verifyResourceCharge(97200, "full thirty-six-page production charges water and fertilizer for 97200 seeds");
            ItemStack overflowing = machine.getCropSeed(35).copy();
            overflowing.stackSize++;
            machine.setCropSeed(35, overflowing);
            machine.outputTracker.clear();
            supplies();
            successfulCycle(35);
            require(
                machine.getCropSeed(35).stackSize == machine.seedCapacity + 1
                    && !machine.getCropStatus(35).equals(machine.getCropStatus(0)),
                "an overflowing page stays intact and does not stop the other thirty-five pages");
            verifyResourceCharge(94500, "an overflowing page contributes no output or fluid charge");
            overflowing.stackSize--;
            machine.setCropSeed(35, overflowing);
        } finally {
            machine.setVoidingMode(originalMode);
        }
        for (int crop = 0; crop < 36; crop++) {
            ItemStack original = machine.getCropSeed(crop).copy();
            original.stackSize = 1 + crop % 3;
            machine.setCropSeed(crop, original);
        }
        report.append("36 independent MV pages produced from 97200 seeds; page overflow stayed isolated.\n");
        report.append("36 crop types: ").append(String.join(", ", planted)).append('\n');
    }

    private void verifyResourceCharge(int seedCount, String message) {
        require(machine.mInputHatches.get(0).mFluid.amount
            == 10000000 - ceiling(machine.getWaterRequirement(seedCount),
                HydrationRegistry.instance.getPotency(FluidRegistry.WATER))
            && machine.mInputHatches.get(1).mFluid.amount
                == 10000000 - ceiling(machine.getFertilizerRequirement(seedCount),
                    FertilizerRegistry.instance.getPotency(CropsNHFluids.fertilizer)),
            message);
    }

    private List<ICropCard> simpleCrops() throws Exception {
        List<ICropCard> cards = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Field field : CropsNHCrops.class.getFields()) {
            if (field.getType() != ICropCard.class) continue;
            ICropCard card = (ICropCard) field.get(null);
            if (
                card == null || card == CropsNHCrops.Weed
                    || card == CropsNHCrops.Migrator
                    || card.getDropTable().isEmpty()
                    || card.getMinSeedBedTier() > machine.upgradeTier
                    || !seen.add(card.getId())
            ) continue;
            ISeedData data = CropsNHUtils.getSeedData(seed(card, 1), false, true);
            if (!requirementsMet(data, null) || machine.getGrowthSpeedUnscaled(data) <= 0) continue;
            cards.add(card);
        }
        return cards;
    }

    private boolean requirementsMet(ISeedData seed, ItemStack soil) {
        ItemStack[] catalysts = soil == null ? new ItemStack[0] : new ItemStack[] { soil };
        for (IGrowthRequirement requirement : seed.getCrop().getGrowthRequirements()) {
            if (requirement instanceof SubSoilRequirement) continue;
            if (
                requirement instanceof IMachineGrowthRequirement machineRequirement
                    && !machineRequirement.canGrow(seed, base, catalysts)
            ) return false;
        }
        return true;
    }

    private void clearCrops() {
        machine.mMaxProgresstime = 0;
        for (int slot = 0; slot < machine.getCropInventory().getSlots(); slot++)
            machine.getCropInventory().setStackInSlot(slot, null);
        for (var input : machine.mInputBusses) Arrays.fill(input.mInventory, null);
        machine.outputTracker.clear();
    }

    private void supplies() {
        machine.mInputHatches.get(0).mFluid = new FluidStack(FluidRegistry.WATER, 10000000);
        machine.mInputHatches.get(1).mFluid = new FluidStack(CropsNHFluids.fertilizer, 10000000);
    }

    private NBTTagCompound snapshot() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("crops", machine.getCropInventory().serializeNBT());
        tag.setTag("fraction", machine.outputTracker.save());
        for (int hatch = 0; hatch < machine.mInputHatches.size(); hatch++) {
            FluidStack fluid = machine.mInputHatches.get(hatch).mFluid;
            if (fluid != null) tag.setTag("fluid" + hatch, fluid.writeToNBT(new NBTTagCompound()));
        }
        return tag;
    }

    private static ItemStack seed(ICropCard crop, int count) {
        SeedData data = new SeedData(crop, new SeedStats((byte) 31, (byte) 31, (byte) 31));
        data.setAnalyzed(true);
        ItemStack stack = data.getStack();
        stack.stackSize = count;
        return stack;
    }

    private static boolean sameDrops(IFDropTable expected, IFDropTable actual) {
        return expected.entrySet().stream()
            .allMatch(entry -> close(entry.getValue(), actual.getItemAmount(entry.getKey())))
            && actual.entrySet().stream()
                .allMatch(entry -> close(entry.getValue(), expected.getItemAmount(entry.getKey())));
    }

    private static double dropCount(IFDropTable drops) {
        return drops.entrySet().stream().mapToDouble(Map.Entry::getValue).sum();
    }

    private static boolean close(double expected, double actual) {
        return Math.abs(expected - actual) <= 1.0E-8 * Math.max(1, Math.abs(expected));
    }

    private void verifyPersistence(World world) {
        beds(VoltageIndex.UXV);
        upgrade(0, CropsNHBlocks.blockEnvironmentalEnhancementUnit, VoltageIndex.MV);
        upgrade(1, CropsNHBlocks.blockEnvironmentalEnhancementUnit, VoltageIndex.MV);
        valid("every seed type has two independent environmental cards");
        ItemStack largeSeed = machine.getCropSeed(0).copy();
        largeSeed.stackSize = 300;
        machine.setCropSeed(0, largeSeed);
        Set<String> environmentPairs = new HashSet<>();
        for (int crop = 0; crop < 36; crop++) {
            ItemStack first = fixtureEnvironment(crop, 0);
            ItemStack second = fixtureEnvironment(crop, 1);
            machine.setCropEnvironment(crop, 0, first);
            machine.setCropEnvironment(crop, 1, second);
            environmentPairs.add(first.getItemDamage() + ":" + second.getItemDamage());
        }
        require(environmentPairs.size() == 36, "all thirty-six seed pages retain distinct environment card pairs");
        NBTTagCompound crops = machine.getCropInventory().serializeNBT();
        require(crops.getTagList("Items", 10).tagCount() == 108,
            "all 108 seed and environment slots contain independent data");
        NBTTagCompound tracker = new NBTTagCompound();
        tracker.setTag("drops", machine.outputTracker.save());
        machine.mMaxProgresstime = 0;
        machine.setSelectedCrop(35);
        NBTTagCompound tag = new NBTTagCompound();
        base.writeToNBT(tag);
        int outputCrop = tag.getInteger("industrialFarmOutputCursor");
        ItemStack seed = machine.getCropSeed(outputCrop).copy();
        world.removeTileEntity(0, 12, 0);
        base = new BaseMetaTileEntity();
        base.setWorldObj(world);
        base.readFromNBT(tag);
        world.setTileEntity(0, 12, 0, base);
        machine = (LargeIndustrialFarm) base.getMetaTileEntity();
        world.markBlockForUpdate(0, 12, 0);
        require(crops.equals(machine.getCropInventory().serializeNBT()), "real tile NBT preserves all 108 crop slots");
        require(machine.getSelectedCrop() == 35, "real tile NBT preserves the selected crop");
        require(machine.getCropSeed(0).stackSize == 300 && machine.getSubSoilStack() == null,
            "real tile NBT preserves large seed counts without soil storage");
        require(machine.hasFertilizer && machine.getMachineMode() == MTEIndustrialFarm.MODE_FARM,
            "real tile NBT preserves fertilizer state and farming mode");
        require(sameDrops(new IFDropTable(tracker, "drops"), machine.outputTracker),
            "real tile NBT preserves every fractional output regardless of map iteration order");
        machine.loadNBTData(tag);
        require(
            crops.equals(machine.getCropInventory().serializeNBT())
                && sameDrops(new IFDropTable(tracker, "drops"), machine.outputTracker),
            "repeated NBT load is idempotent");
        valid("reloaded subclass validates its fixed structure");
        verifyLegacyMigration(world, tag);
        machine.mMaxProgresstime = 0;
        machine.setMachineMode(MTEIndustrialFarm.MODE_OUTPUT);
        int occupied = machine.getOccupiedCropCount();
        require(
            machine.checkProcessing().wasSuccessful() && machine.getCropSeed(outputCrop) == null
                && machine.getOccupiedCropCount() == occupied - 1
                && sameItems(new ItemStack[] { seed }, machine.mOutputItems),
            "output mode preserves its saved cursor and safely ejects one seed type");
        machine.setCropSeed(outputCrop, seed);
        machine.mMaxProgresstime = 0;
        machine.setMachineMode(MTEIndustrialFarm.MODE_FARM);
        report.append("108-slot persistence, per-page environment cards, large seed counts and seed output passed.\n");
    }

    private static ItemStack fixtureEnvironment(int crop, int index) {
        if (crop == 0) return index == 0 ? CropsNHItemList.environmentalModule_HOT.get(1)
            : CropsNHItemList.environmentalModule_FOREST.get(1);
        if (crop == 1) return index == 0 ? CropsNHItemList.environmentalModule_WET.get(1)
            : CropsNHItemList.environmentalModule_LUSH.get(1);
        List<CropsNHItemList> cards = Arrays.stream(CropsNHItemList.values()).filter(
            card -> card.name().startsWith("environmentalModule_") && card != CropsNHItemList.environmentalModule_base)
            .collect(Collectors.toList());
        return cards.get(index == 0 ? crop % cards.size() : 1 + crop / cards.size()).get(1);
    }

    private void verifyLegacyMigration(World world, NBTTagCompound current) {
        MTEIndustrialFarm legacy = (MTEIndustrialFarm) ((BaseMetaTileEntity) world.getTileEntity(64, 12, 0))
            .getMetaTileEntity();
        legacy.ifStackHandler.setStackInSlot(0, seed(CropsNHCrops.Carrot, 175));
        legacy.ifStackHandler.setStackInSlot(1, new ItemStack(Blocks.dirt, 175));
        legacy.ifStackHandler.setStackInSlot(2, CropsNHItemList.environmentalModule_HOT.get(1));
        legacy.ifStackHandler.setStackInSlot(3, CropsNHItemList.environmentalModule_FOREST.get(1));
        NBTTagCompound previous = new NBTTagCompound();
        legacy.saveNBTData(previous);
        machine.loadNBTData(previous);
        require(
            machine.getCropSeed(0).stackSize == 175 && machine.getSubSoilStack() == null
                && machine.getCropEnvironment(0, 0) != null
                && machine.getCropEnvironment(0, 1) != null
                && machine.getOccupiedCropCount() == 1,
            "native four-slot migration preserves seeds and the first page's cards without soil");
        for (int crop = 1; crop < 36; crop++)
            require(machine.getCropSeed(crop) == null, "native migration clears stale seed type " + crop);
        verifySingleEnvironmentOwner(0);
        verifyQueuedReturns(new ItemStack[] { new ItemStack(Blocks.dirt, 175) }, 175);
        verifyLegacyCropMigration(previous, 2);
        verifyLegacyCropMigration(previous, 4);
        verifyLegacySharedEnvironmentMigration(previous);
        machine.loadNBTData(current);
        report.append(
            "Legacy 4/38/74/144-slot saves preserve seeds, assign cards without duplication and return every soil.\n");
    }

    private void verifyLegacyCropMigration(NBTTagCompound legacy, int stride) {
        ItemStackHandler oldStorage = new ItemStackHandler(stride == 2 ? 74 : 144);
        oldStorage.setStackInSlot(0, seed(CropsNHCrops.Carrot, 175));
        oldStorage.setStackInSlot(35 * stride, seed(CropsNHCrops.Potato, 42));
        List<ItemStack> expectedReturns = new ArrayList<>();
        for (int crop = 0; crop < 36; crop++) {
            ItemStack soil = new ItemStack(crop == 35 ? Blocks.stone : Blocks.dirt,
                crop == 0 ? 2100 : crop == 35 ? 261 : crop + 1);
            oldStorage.setStackInSlot(crop * stride + 1, soil);
            expectedReturns.add(soil.copy());
            if (stride == 4) {
                ItemStack first = fixtureEnvironment(crop, 0);
                ItemStack second = fixtureEnvironment(crop, 1);
                oldStorage.setStackInSlot(crop * stride + 2, first);
                oldStorage.setStackInSlot(crop * stride + 3, second);
            }
        }
        if (stride == 2) {
            oldStorage.setStackInSlot(72, CropsNHItemList.environmentalModule_HOT.get(1));
            oldStorage.setStackInSlot(73, CropsNHItemList.environmentalModule_FOREST.get(1));
        }
        NBTTagCompound previous = (NBTTagCompound) legacy.copy();
        previous.setTag("industrialFarmPlots", oldStorage.serializeNBT());
        previous.setInteger("industrialFarmSelectedCrop", 35);
        machine.loadNBTData(previous);
        require(
            machine.getCropInventory().getSlots() == 108 && machine.getCropSeed(0).stackSize == 175
                && machine.getCropSeed(35).stackSize == 42
                && machine.getUsedSeedCount() == 217
                && machine.getSubSoilStack() == null,
            "legacy seed and soil storage migrates into 108 slots without retaining soil");
        if (stride == 4) {
            for (int crop = 0; crop < 36; crop++) require(
                sameStack(fixtureEnvironment(crop, 0), machine.getCropEnvironment(crop, 0))
                    && sameStack(fixtureEnvironment(crop, 1), machine.getCropEnvironment(crop, 1)),
                "old 144-slot storage preserves both cards for seed page " + crop);
        } else verifySingleEnvironmentOwner(35);
        verifyQueuedReturns(expectedReturns.toArray(new ItemStack[0]), 217);
    }

    private void verifyLegacySharedEnvironmentMigration(NBTTagCompound legacy) {
        ItemStackHandler oldStorage = new ItemStackHandler(38);
        oldStorage.setStackInSlot(0, seed(CropsNHCrops.Carrot, 175));
        oldStorage.setStackInSlot(35, seed(CropsNHCrops.Potato, 42));
        oldStorage.setStackInSlot(36, CropsNHItemList.environmentalModule_HOT.get(1));
        oldStorage.setStackInSlot(37, CropsNHItemList.environmentalModule_FOREST.get(1));
        for (int selected : new int[] { 35, 0 }) {
            NBTTagCompound previous = (NBTTagCompound) legacy.copy();
            previous.setTag("industrialFarmPlots", oldStorage.serializeNBT());
            if (selected == 35) previous.setInteger("industrialFarmSelectedCrop", selected);
            else previous.removeTag("industrialFarmSelectedCrop");
            machine.loadNBTData(previous);
            require(machine.getSelectedCrop() == selected && machine.getUsedSeedCount() == 217,
                "old shared-card storage preserves its selection and all seeds");
            verifySingleEnvironmentOwner(selected);
            NBTTagCompound migrated = new NBTTagCompound();
            machine.saveNBTData(migrated);
            require(queuedReturns(migrated).length == 0,
                "shared-card migration neither duplicates nor returns active cards");
            machine.loadNBTData(migrated);
            verifySingleEnvironmentOwner(selected);
        }
    }

    private void verifySingleEnvironmentOwner(int owner) {
        for (int crop = 0; crop < 36; crop++) {
            ItemStack first = machine.getCropEnvironment(crop, 0);
            ItemStack second = machine.getCropEnvironment(crop, 1);
            if (
                crop == owner
            ) require(
                sameStack(CropsNHItemList.environmentalModule_HOT.get(1), first)
                    && sameStack(CropsNHItemList.environmentalModule_FOREST.get(1), second),
                "legacy shared cards belong to the saved selected seed page");
            else require(first == null && second == null,
                "legacy shared cards are not duplicated into seed page " + crop);
        }
    }

    private void verifyQueuedReturns(ItemStack[] expected, int plantedSeeds) {
        NBTTagCompound migrated = new NBTTagCompound();
        machine.saveNBTData(migrated);
        require(sameItemTotals(expected, queuedReturns(migrated)),
            "every removed soil and extra card is queued for return");
        machine.loadNBTData(migrated);
        NBTTagCompound reloaded = new NBTTagCompound();
        machine.saveNBTData(reloaded);
        require(migrated.getTag("industrialFarmReturns").equals(reloaded.getTag("industrialFarmReturns")),
            "queued legacy returns survive save and reload without duplication");
        machine.mMaxProgresstime = 0;
        machine.setMachineMode(MTEIndustrialFarm.MODE_OUTPUT);
        var output = machine.mOutputBusses.get(0);
        ItemStack[] savedOutput = output.mInventory.clone();
        Arrays.fill(output.mInventory, new ItemStack(Items.diamond, 64));
        require(machine.checkProcessing() == CheckRecipeResultRegistry.ITEM_OUTPUT_FULL,
            "a full output bus safely blocks migrated soil and card returns");
        NBTTagCompound blocked = new NBTTagCompound();
        machine.saveNBTData(blocked);
        require(sameItemTotals(expected, queuedReturns(blocked)) && machine.getUsedSeedCount() == plantedSeeds,
            "blocked legacy returns preserve every queued item and planted seed");
        System.arraycopy(savedOutput, 0, output.mInventory, 0, savedOutput.length);
        List<ItemStack> returned = new ArrayList<>();
        int cycles = 0;
        while (blocked.getTagList("industrialFarmReturns", 10).tagCount() > 0) {
            require(++cycles <= 512, "legacy return queue drains in bounded cycles");
            machine.mMaxProgresstime = 0;
            require(
                machine.checkProcessing().wasSuccessful() && machine.mMaxProgresstime == 5
                    && machine.mOutputItems.length == 1
                    && machine.getUsedSeedCount() == plantedSeeds,
                "output mode safely returns queued items before planted seeds");
            returned.add(machine.mOutputItems[0].copy());
            blocked = new NBTTagCompound();
            machine.saveNBTData(blocked);
        }
        require(sameItemTotals(expected, returned.toArray(new ItemStack[0])),
            "all migrated soil and extra environment cards return exactly once, including partial stacks");
    }

    private static ItemStack[] queuedReturns(NBTTagCompound tag) {
        List<ItemStack> stacks = new ArrayList<>();
        var list = tag.getTagList("industrialFarmReturns", 10);
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound item = list.getCompoundTagAt(index);
            ItemStack stack = ItemStack.loadItemStackFromNBT(item);
            require(stack != null, "queued legacy item can be decoded");
            stack.stackSize = item.getInteger("Count");
            stacks.add(stack);
        }
        return stacks.toArray(new ItemStack[0]);
    }

    private static boolean sameItemTotals(ItemStack[] expected, ItemStack[] actual) {
        return Arrays.stream(expected).mapToInt(stack -> stack.stackSize).sum()
            == Arrays.stream(actual).mapToInt(stack -> stack.stackSize).sum()
            && Arrays.stream(expected).allMatch(stack -> itemCount(expected, stack) == itemCount(actual, stack));
    }

    private static int itemCount(ItemStack[] stacks, ItemStack template) {
        return Arrays.stream(stacks).filter(stack -> stack != null && GTUtility.areStacksEqual(stack, template))
            .mapToInt(stack -> stack.stackSize).sum();
    }

    private void verifyCropSelection() throws Exception {
        Method gui = MTEIndustrialFarm.class.getDeclaredMethod("getGui");
        gui.setAccessible(true);
        require(gui.invoke(machine) instanceof MTEIndustrialFarmGui,
            "controller retains the native industrial farm GUI factory and layout");
        require(machine.ifStackHandler.getSlots() == 4,
            "native GUI bridge retains compatible indices with hidden soil");
        for (int crop = 0; crop < 36; crop++) {
            machine.setSelectedCrop(crop);
            require(
                machine.getSelectedCrop() == crop
                    && sameStack(machine.getCropSeed(crop), machine.ifStackHandler.getStackInSlot(0))
                    && machine.ifStackHandler.getStackInSlot(1) == null
                    && sameStack(machine.getCropEnvironment(crop, 0), machine.ifStackHandler.getStackInSlot(2))
                    && sameStack(machine.getCropEnvironment(crop, 1), machine.ifStackHandler.getStackInSlot(3)),
                "native seed and environment view selects crop " + crop + " without a soil slot");
        }
        machine.setSelectedCrop(0);
        report.append("Native GUI selection exposes all 36 seed types and their own environment cards without soil.\n");
    }

    private static boolean sameStack(ItemStack expected, ItemStack actual) {
        if (expected == null || actual == null) return expected == actual;
        return expected.stackSize == actual.stackSize && GTUtility.areStacksEqual(expected, actual);
    }

    private void verifyGuiTransfer(EntityPlayerMP player) {
        NBTTagCompound saved = machine.getCropInventory().serializeNBT();
        ItemStack previous = player.inventory.getStackInSlot(9);
        Slot source = null;
        for (Object entry : player.openContainer.inventorySlots) {
            Slot slot = (Slot) entry;
            if (slot.isSlotInInventory(player.inventory, 9)) {
                source = slot;
                break;
            }
        }
        require(source != null, "server GUI exposes player inventory slots");
        for (int crop = 0; crop < 2; crop++) {
            machine.setSelectedCrop(crop);
            int otherSeeds = machine.getUsedSeedCount() - machine.getCropSeed(crop).stackSize;
            ItemStack nearFull = machine.getCropSeed(crop).copy();
            nearFull.stackSize = machine.seedCapacity - 2;
            machine.setCropSeed(crop, nearFull);
            ItemStack offered = nearFull.copy();
            offered.stackSize = 64;
            player.inventory.setInventorySlotContents(9, offered);
            player.openContainer.transferStackInSlot(player, source.slotNumber);
            require(
                machine.getCropSeed(crop).stackSize == machine.seedCapacity
                    && machine.getUsedSeedCount() == otherSeeds + machine.seedCapacity
                    && player.inventory.getStackInSlot(9) != null
                    && player.inventory.getStackInSlot(9).stackSize == 62,
                "real GUI shift-click independently fills the last two spaces of seed page " + crop);
        }
        machine.getCropInventory().deserializeNBT(saved);
        machine.setSelectedCrop(0);
        player.inventory.setInventorySlotContents(9, previous);
        player.openContainer.detectAndSendChanges();
        report.append("Real server GUI shift-click obeys each selected page's independent capacity.\n");
    }

    private void verifyLargeDropCollection() throws Exception {
        Method collect = LargeIndustrialFarm.class.getDeclaredMethod("collectDrops", IFDropTable.class, boolean.class);
        collect.setAccessible(true);
        ItemStack carrot = new ItemStack(Items.carrot);
        IFDropTable tracker = new IFDropTable();
        double total = Integer.MAX_VALUE + 2048.375;
        tracker.addDrop(carrot, total);
        ItemStack[] simulated = (ItemStack[]) collect.invoke(null, tracker, true);
        require(
            simulated.length == 1 && simulated[0].stackSize == Integer.MAX_VALUE
                && tracker.getItemAmount(carrot) == total,
            "large drop simulation saturates without consuming pending yield");
        ItemStack[] first = (ItemStack[]) collect.invoke(null, tracker, false);
        require(
            first.length == 1 && first[0].stackSize == Integer.MAX_VALUE && tracker.getItemAmount(carrot) == 2048.375,
            "large drop collection retains yield above the integer limit");
        ItemStack[] second = (ItemStack[]) collect.invoke(null, tracker, false);
        require(second.length == 1 && second[0].stackSize == 2048 && tracker.getItemAmount(carrot) == 0.375,
            "the following collection emits remaining whole items and preserves fractional yield");
        report.append("Drop collection preserves pending yield above Integer.MAX_VALUE.\n");
    }

    private void verifyControllerDrops(World world) {
        BaseMetaTileEntity isolatedBase = place(world, 128, 12, 0, GTNGMachineID.LargeIndustrialFarm.id,
            ForgeDirection.NORTH);
        isolatedBase.disableWorking();
        LargeIndustrialFarm isolated = (LargeIndustrialFarm) isolatedBase.getMetaTileEntity();
        ItemStack seeds = seed(CropsNHCrops.Carrot, 513);
        ItemStack stone = new ItemStack(Blocks.stone, 261);
        ItemStackHandler legacy = new ItemStackHandler(74);
        legacy.setStackInSlot(70, seeds);
        legacy.setStackInSlot(71, stone);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("industrialFarmPlots", legacy.serializeNBT());
        isolated.loadNBTData(tag);
        AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(126, 10, -2, 131, 15, 3);
        List<EntityItem> existing = new ArrayList<>(world.getEntitiesWithinAABB(EntityItem.class, bounds));
        isolated.onBlockDestroyed();
        List<EntityItem> entities = new ArrayList<>(world.getEntitiesWithinAABB(EntityItem.class, bounds));
        entities.removeAll(existing);
        int seedCount = 0;
        int stoneCount = 0;
        for (EntityItem entity : entities) {
            ItemStack stack = entity.getEntityItem();
            require(stack != null && stack.stackSize > 0 && stack.stackSize <= stack.getMaxStackSize(),
                "destroyed controller emits only native-size entity stacks");
            NBTTagCompound entityTag = new NBTTagCompound();
            entity.writeToNBT(entityTag);
            EntityItem restored = new EntityItem(world);
            restored.readFromNBT(entityTag);
            require(
                GTUtility.areStacksEqual(stack, restored.getEntityItem())
                    && stack.stackSize == restored.getEntityItem().stackSize,
                "every dropped entity stack preserves its count through vanilla NBT");
            if (GTUtility.areStacksEqual(stack, seeds)) seedCount += stack.stackSize;
            else if (GTUtility.areStacksEqual(stack, stone)) stoneCount += stack.stackSize;
            else require(false, "unexpected item dropped by isolated controller");
        }
        require(seedCount == 513 && stoneCount == 261,
            "controller destruction preserves all seeds and queued legacy soil");
        for (int slot = 0; slot < isolated.getCropInventory().getSlots(); slot++)
            require(isolated.getCropInventory().getStackInSlot(slot) == null,
                "controller destruction empties crop slot " + slot);
        NBTTagCompound destroyed = new NBTTagCompound();
        isolated.saveNBTData(destroyed);
        require(queuedReturns(destroyed).length == 0,
            "controller destruction clears already dropped legacy soil returns");
        report.append("Controller destruction splits 513 seeds and 261 soil into safe persistent entity stacks.\n");
    }

    private static boolean sameItems(ItemStack[] expected, ItemStack[] actual) {
        if (expected == null || actual == null || expected.length != actual.length) return false;
        return Arrays.stream(expected)
            .allMatch(wanted -> Arrays.stream(actual).filter(stack -> GTUtility.areStacksEqual(wanted, stack))
                .mapToInt(stack -> stack.stackSize).sum() == wanted.stackSize);
    }

    private static long ceiling(long amount, int potency) {
        require(potency > 0, "native supply registry fixture");
        return (amount + potency - 1) / potency;
    }

    private static void verifyCrafting(World world) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        grid.setInventorySlotContents(4, CropsNHItemList.IndustrialFarmController.get(1));
        ItemStack output = CraftingManager.getInstance().findMatchingRecipe(grid, world);
        require(GTUtility.areStacksEqual(output, GTNGItemList.LargeIndustrialFarm.get(1)) && output.stackSize == 1,
            "registered controller upgrade crafting recipe");
    }

    private void valid(String message) {
        List<StructureError> errors = errors();
        require(errors.isEmpty(),
            message + ": " + errors.stream().map(StructureError::getDisplayString).collect(Collectors.joining("; ")));
        require(machine.checkStructure(true, base), message + " (real structure validation)");
        report.append(message).append('\n');
    }

    private void invalid(String message) {
        require(!errors().isEmpty(), message);
        report.append(message).append('\n');
    }

    private List<StructureError> errors() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        return errors;
    }

    private void beds(int tier) {
        for (int z = 1; z <= 12; z++)
            for (int x = -1; x <= 1; x++) base.getWorld().setBlock(x, 12, z, CropsNHBlocks.blockSeedBed, tier, 3);
    }

    private void upgrade(int slot, Block block, int tier) {
        base.getWorld().setBlock(0, 14, slot + 1, block, tier, 3);
    }

    private void frame(int slot) {
        base.getWorld().setBlock(0, 14, slot + 1, frame, frameMeta, 3);
    }

    private void casing(int x, int y, int z) {
        base.getWorld().setBlock(x, y, z, CropsNHBlocks.blockCasings1, 0, 3);
    }

    private static int findHatch(Class<? extends MTEHatch> type) {
        for (int id = 0; id < GregTechAPI.METATILEENTITIES.length; id++) {
            IMetaTileEntity entry = GregTechAPI.METATILEENTITIES[id];
            if (entry != null && entry.getClass() == type && ((MTEHatch) entry).mTier >= VoltageIndex.UV) return id;
        }
        throw new AssertionError("Missing native UV+ hatch fixture " + type.getSimpleName());
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id, ForgeDirection facing) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(facing);
        return tile;
    }

    private static Set<String> occupied(World world, int offset) {
        Set<String> blocks = new HashSet<>();
        for (int x = -3; x <= 3; x++) for (int y = 10; y <= 15; y++)
            for (int z = -1; z <= 15; z++) if (!world.isAirBlock(x + offset, y, z)) blocks.add(x + "," + y + "," + z);
        return blocks;
    }

    private static int count(World world, int offset, Block block) {
        int count = 0;
        for (int x = -2; x <= 2; x++) for (int y = 11; y <= 14; y++)
            for (int z = 0; z <= 13; z++) if (world.getBlock(x + offset, y, z) == block) count++;
        return count;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (stage == 0) {
                if (++frames < 60) return;
                capture("large-industrial-farm-structure");
                stage = 1;
                frames = 0;
                openRequested = true;
                return;
            }
            require(++frames <= 1800, "native GUI open timeout");
            if (!(mc.currentScreen instanceof GuiContainerWrapper) || frames < 60) return;
            if (stage == 1) {
                verifyGuiSlots(mc);
                capture("large-industrial-farm-gui");
                selectLastRequested = true;
                stage = 2;
                frames = 0;
                return;
            }
            if (!selectedLast) return;
            BaseMetaTileEntity clientBase = (BaseMetaTileEntity) mc.theWorld.getTileEntity(0, 12, 0);
            require(((LargeIndustrialFarm) clientBase.getMetaTileEntity()).getSelectedCrop() == 35,
                "the native GUI synchronizes selection of the thirty-sixth crop");
            verifyGuiSlots(mc);
            capture("large-industrial-farm-gui-last-crops");
            Files.write(new File(output(), "result.txt").toPath(), ("PASS\n" + report
                + "36 crop types, expanded capacity, module tiers, perfect overclocks, transactions, NBT and GUI passed.\n")
                    .getBytes(StandardCharsets.UTF_8));
            System.out.println("INDUSTRIAL_FARM_QA: PASS\n" + report);
            finished = true;
            mc.shutdown();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void verifyGuiSlots(Minecraft mc) {
        Set<Integer> slots = new HashSet<>();
        int playerSlots = 0;
        for (Object entry : mc.thePlayer.openContainer.inventorySlots) {
            Slot slot = (Slot) entry;
            for (int index = 0; index < mc.thePlayer.inventory.mainInventory.length; index++)
                if (slot.isSlotInInventory(mc.thePlayer.inventory, index)) playerSlots++;
            if (!(slot instanceof SlotItemHandler handler) || handler.getItemHandler().getSlots() != 4) continue;
            slots.add(slot.getSlotIndex());
            require(slot.getStack() != null, "client sync supplies crop slot " + slot.getSlotIndex());
            if (slot.getSlotIndex() == 0)
                require(sameStack(expectedGuiSeed, slot.getStack()), "native GUI seed slot displays the selected crop");
            if (slot.getSlotIndex() == 2) require(sameStack(expectedGuiFirstEnvironment, slot.getStack()),
                "native GUI displays this page's first environment card");
            if (slot.getSlotIndex() == 3) require(sameStack(expectedGuiSecondEnvironment, slot.getStack()),
                "native GUI displays this page's second environment card");
        }
        require(slots.size() == 3 && slots.contains(0) && slots.contains(2) && slots.contains(3) && !slots.contains(1),
            "the native GUI displays only the seed and two environment slots");
        require(playerSlots == 36 && mc.thePlayer.openContainer.inventorySlots.size() == playerSlots + slots.size() + 1,
            "native GUI has 36 player slots, three farm slots and one controller slot");
        BaseMetaTileEntity clientBase = (BaseMetaTileEntity) mc.theWorld.getTileEntity(0, 12, 0);
        LargeIndustrialFarm clientFarm = (LargeIndustrialFarm) clientBase.getMetaTileEntity();
        require(clientFarm.getUsedSeedCount() == machine.getUsedSeedCount(),
            "native GUI synchronizes the global seed count independently of the three visible slots");
        report.append(
            "Native GUI synchronizes the selected seed and its two independent environment cards without soil.\n");
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.industrialFarm.output", "."));
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
        if (!condition) throw new AssertionError("INDUSTRIAL_FARM_QA: " + message);
    }
}
