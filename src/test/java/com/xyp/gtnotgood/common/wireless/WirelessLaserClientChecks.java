package com.xyp.gtnotgood.common.wireless;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.recipe.machine.EasyWirelessRecipes;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.config.MainConfig;
import com.xyp.gtnotgood.loader.WirelessHatchLoader;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.RecipeMaps;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.multi.MTEElectricBlastFurnace;
import gregtech.common.tileentities.machines.multi.MTEVacuumFreezer;
import tectech.thing.CustomItemList;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessDynamoMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

/** Opt-in real GT registration, recipes, server transfers, NBT and native GUI checks in a fresh disposable world. */
@Mod(
    modid = "wirelesslaserqa",
    name = "Wireless Laser QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public class WirelessLaserClientChecks {

    private boolean started;
    private boolean checked;
    private volatile boolean finished;
    private volatile Throwable failure;
    private long deadline;
    private int frames;
    private BaseMetaTileEntity displayTile;
    private int guiTicks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.wireless.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (deadline == 0) deadline = System.currentTimeMillis() + 300_000;
            require(System.currentTimeMillis() < deadline, "client QA completed within five minutes");
            if (failure != null) throw new AssertionError("server checks failed", failure);
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("wireless-laser-qa-" + System.currentTimeMillis(), "Wireless Multi-Amp QA",
                    new WorldSettings(91L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (finished && ++frames == 80) {
                require(mc.currentScreen instanceof GuiContainerWrapper, "native GUI opened");
                ScreenShotHelper.saveScreenshot(new File("."), "wireless-laser-gui.png", mc.displayWidth,
                    mc.displayHeight, mc.getFramebuffer());
                writeResult("PASS");
                System.out.println("WIRELESS_LASER_QA: PASS");
                mc.shutdown();
            }
        } catch (Throwable problem) {
            problem.printStackTrace();
            StringWriter trace = new StringWriter();
            problem.printStackTrace(new PrintWriter(trace));
            writeResult("FAIL\n" + trace);
            mc.shutdown();
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (checked) {
            if (!finished && ++guiTicks == 30) {
                displayTile.getMetaTileEntity().onScrewdriverRightClick(ForgeDirection.SOUTH, player, 0.5f, 0.5f, 0.5f,
                    null);
                finished = true;
            }
            return;
        }
        checked = true;
        try {
            checkCatalogAndRecipes();
            WorldServer world = server.worldServerForDimension(0);
            player.setPositionAndUpdate(2, 7, 3);
            UUID owner = UUID.randomUUID();
            WirelessNetworkManager.strongCheckOrAddUser(owner);
            checkMachineAcceptance(world, owner);
            for (int tier = 1; tier <= 14; tier++) {
                BaseMetaTileEntity tile = place(world, WirelessHatchLoader.energy(tier, 6).get(1), owner);
                MTEHatchWirelessMulti hatch = (MTEHatchWirelessMulti) tile.getMetaTileEntity();
                BigInteger budget = BigInteger.valueOf(hatch.maxEUStore()).multiply(BigInteger.valueOf(3));
                WirelessNetworkManager.setUserEU(owner, budget);
                hatch.onFirstTick(tile);
                long stored = hatch.getEUVar();
                require(stored > 0 && stored <= hatch.maxEUStore(), "positive bounded buffer " + tier);
                require(budget.equals(WirelessNetworkManager.getUserEU(owner).add(BigInteger.valueOf(stored))),
                    "input conserves EU " + tier);
                hatch.setEUVar(0);
                WirelessNetworkManager.setUserEU(owner, BigInteger.ZERO);
                hatch.tryFetchingEnergy();
                require(hatch.getEUVar() == 0, "empty network cannot create EU " + tier);
                hatch.setAmperes(256);
                require(hatch.getHatchType() == 1, "adjusting amperage keeps multi-amp classification " + tier);
                NBTTagCompound nbt = new NBTTagCompound();
                tile.writeToNBT(nbt);
                BaseMetaTileEntity loaded = new BaseMetaTileEntity();
                loaded.readFromNBT(nbt);
                MTEHatchWirelessMulti restored = (MTEHatchWirelessMulti) loaded.getMetaTileEntity();
                require(
                    restored.getAmperes() == 256 && restored.maxAmperes == 1_048_576 && restored.getHatchType() == 1,
                    "amperage and registered ceiling survive NBT " + tier);

                tile = place(world, WirelessHatchLoader.dynamo(tier).get(1), owner);
                MTEHatchWirelessDynamoMulti dynamo = (MTEHatchWirelessDynamoMulti) tile.getMetaTileEntity();
                dynamo.onPreTick(tile, 1);
                long deposit = GTValues.V[tier] * WirelessHatchLoader.DYNAMO_AMPERES;
                dynamo.setEUVar(deposit);
                dynamo.onPreTick(tile, WirelessNetworkManager.ticks_between_energy_addition);
                require(
                    dynamo.getEUVar() == 0
                        && WirelessNetworkManager.getUserEU(owner).equals(BigInteger.valueOf(deposit)),
                    "dynamo conserves EU " + tier);
                dynamo.onPreTick(tile, 2 * WirelessNetworkManager.ticks_between_energy_addition);
                require(WirelessNetworkManager.getUserEU(owner).equals(BigInteger.valueOf(deposit)),
                    "no duplicate deposit");
            }
            displayTile = place(world, WirelessHatchLoader.energy(14, 6).get(1), player.getUniqueID());
            System.out.println("WIRELESS_LASER_QA: all 14 voltage tiers passed transfer and NBT checks");
        } catch (Throwable failure) {
            failure.printStackTrace();
            this.failure = failure;
        }
    }

    private static BaseMetaTileEntity place(WorldServer world, ItemStack stack, UUID owner) {
        return place(world, stack, owner, 0);
    }

    private static BaseMetaTileEntity place(WorldServer world, ItemStack stack, UUID owner, int x) {
        world.setBlockToAir(x, 6, 0);
        world.setBlock(x, 6, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 6, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerUuid(owner);
        tile.setFrontFacing(ForgeDirection.SOUTH);
        return tile;
    }

    private static void checkMachineAcceptance(WorldServer world, UUID owner) {
        require(MainConfig.electricBlastFurnaceMultiAmpEnergy && MainConfig.vacuumFreezerMultiAmpEnergy,
            "both machine options enabled for client QA");
        MTEElectricBlastFurnace furnace = (MTEElectricBlastFurnace) place(world,
            ItemList.Machine_Multi_BlastFurnace.get(1), owner, 3).getMetaTileEntity();
        MTEVacuumFreezer freezer = (MTEVacuumFreezer) place(world, ItemList.Machine_Multi_VacuumFreezer.get(1), owner,
            4).getMetaTileEntity();
        IStructureElement<MTEElectricBlastFurnace> furnaceElement = ((StructureDefinition<MTEElectricBlastFurnace>) furnace
            .getStructureDefinition()).getElements().get('b');
        IStructureElement<MTEVacuumFreezer> freezerElement = ((StructureDefinition<MTEVacuumFreezer>) freezer
            .getStructureDefinition()).getElements().get('C');
        for (int tier = 1; tier <= 14; tier++) {
            for (int variant = 0; variant < 7; variant++) {
                BaseMetaTileEntity tile = place(world, WirelessHatchLoader.energy(tier, variant).get(1), owner);
                furnace.getExoticEnergyHatches().clear();
                freezer.getExoticEnergyHatches().clear();
                require(
                    furnaceElement.check(furnace, world, 0, 6, 0)
                        && furnace.getExoticEnergyHatches().contains(tile.getMetaTileEntity()),
                    "EBF structure accepts wireless multi-amp " + tier + "/" + variant);
                require(
                    freezerElement.check(freezer, world, 0, 6, 0)
                        && freezer.getExoticEnergyHatches().contains(tile.getMetaTileEntity()),
                    "freezer structure accepts wireless multi-amp " + tier + "/" + variant);
            }
        }
        for (CustomItemList nativeLaser : new CustomItemList[] { CustomItemList.eM_energyWirelessTunnel8_UXV,
            CustomItemList.eM_energyWirelessTunnel9_UXV }) {
            MTEHatchWirelessMulti laser = (MTEHatchWirelessMulti) place(world, nativeLaser.get(1), owner)
                .getMetaTileEntity();
            require(laser.getHatchType() == 2, "uncatalogued native laser retains classification");
            furnace.getExoticEnergyHatches().clear();
            freezer.getExoticEnergyHatches().clear();
            require(!furnaceElement.check(furnace, world, 0, 6, 0) && furnace.getExoticEnergyHatches().isEmpty(),
                "EBF rejects uncatalogued native laser");
            require(!freezerElement.check(freezer, world, 0, 6, 0) && freezer.getExoticEnergyHatches().isEmpty(),
                "freezer rejects uncatalogued native laser");
        }
        System.out.println("WIRELESS_LASER_QA: both native structures accepted all 98 inputs and rejected native 8/9");
    }

    private static void writeResult(String result) {
        try {
            Files.write(new File("wireless-laser-qa-result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
    }

    static void checkCatalogAndRecipes() {
        Set<Integer> ids = new HashSet<>();
        for (int tier = 1; tier <= 14; tier++) {
            for (int variant = 0; variant < 7; variant++) {
                ItemStack stack = WirelessHatchLoader.energy(tier, variant).get(1);
                require(ids.add(stack.getItemDamage()), "unique input ID");
                MTEHatchWirelessMulti hatch = (MTEHatchWirelessMulti) GregTechAPI.METATILEENTITIES[stack
                    .getItemDamage()];
                require(hatch.mTier == tier && hatch.maxAmperes == WirelessHatchLoader.amperes(variant),
                    "correct rating");
                require(hatch.getHatchType() == 1 && hatch.getConnectionType() == MTEHatch.ConnectionType.WIRELESS,
                    "wireless multi-amp classification");
                require(hatch.newMetaEntity(null) instanceof MTEHatchWirelessMulti placed && placed.getHatchType() == 1,
                    "placed subtype and multi-amp classification");
                require(hatch.maxEUStore() > 0 && hatch.eu_transferred_per_operation_long > 0
                    && hatch.actualTicksBetweenEnergyAddition > 0, "no capacity overflow");
                require(!hatch.getLocalName().contains("gtng."), "translated item name");
            }
            require(ids.add(WirelessHatchLoader.dynamo(tier).get(1).getItemDamage()), "unique dynamo ID");
        }
        require(ids.size() == 112, "full LV-MAX catalog");
        int supported = 0;
        for (IMetaTileEntity machine : GregTechAPI.METATILEENTITIES) {
            if (!EasyWirelessRecipes.supports(machine)) continue;
            MTEHatch hatch = (MTEHatch) machine;
            boolean dynamo = hatch instanceof MTEHatchWirelessDynamoMulti;
            int amps = dynamo ? ((MTEHatchWirelessDynamoMulti) hatch).maxAmperes
                : hatch instanceof MTEHatchWirelessMulti ? ((MTEHatchWirelessMulti) hatch).maxAmperes : 2;
            ItemStack wired = amps > 64 ? EasyWirelessRecipes.findWiredLaser(hatch.mTier, amps, dynamo) : null;
            ItemStack base = (dynamo ? ItemList.HATCHES_DYNAMO : ItemList.HATCHES_ENERGY)[Math.min(13, hatch.mTier)]
                .get(1);
            ItemStack expectedBase = wired != null ? wired : base;
            long recipes = (wired != null ? RecipeMaps.laserEngraverRecipes : RecipeMaps.assemblerRecipes)
                .getAllRecipes().stream()
                .filter(recipe -> recipe.mOutputs != null && recipe.mOutputs.length == 1
                    && recipe.mOutputs[0] != null
                    && recipe.mOutputs[0].isItemEqual(hatch.getStackForm(1))
                    && recipe.mInputs != null
                    && recipe.mInputs.length > 0
                    && recipe.mInputs[0] != null
                    && recipe.mInputs[0].isItemEqual(expectedBase))
                .peek(recipe -> {
                    if (amps <= 64) return;
                    if (wired != null) {
                        require(recipe.mInputs[1].stackSize == 8, "eight native wireless covers");
                        require(
                            RecipeMaps.assemblerRecipes.getAllRecipes().stream()
                                .noneMatch(old -> old.mOutputs != null && old.mOutputs.length == 1
                                    && old.mOutputs[0] != null
                                    && old.mOutputs[0].isItemEqual(hatch.getStackForm(1))
                                    && old.mInputs != null
                                    && old.mInputs.length > 0
                                    && old.mInputs[0] != null
                                    && old.mInputs[0].isItemEqual(base)),
                            "no cheap assembler bypass for wired conversion");
                    } else {
                        int count = 1 << (Integer.numberOfTrailingZeros(amps / 256) / 2);
                        for (int slot = 1; slot <= 3; slot++) {
                            require(recipe.mInputs[slot].stackSize == count, "scaled lens/sensor/pump cost");
                        }
                        require(recipe.mInputs[4].stackSize == 8, "eight native wireless covers");
                        require(recipe.mFluidInputs[0].amount == 144 * count, "scaled solder cost");
                    }
                }).count();
            require(recipes == 1, "one simple recipe for " + hatch.getLocalName() + ", found " + recipes);
            supported++;
        }
        int before = RecipeMaps.assemblerRecipes.getAllRecipes().size()
            + RecipeMaps.laserEngraverRecipes.getAllRecipes().size();
        boolean enabled = Config.enableEasyWirelessRecipes;
        try {
            Config.enableEasyWirelessRecipes = false;
            EasyWirelessRecipes.loadRecipes();
            require(before == RecipeMaps.assemblerRecipes.getAllRecipes().size()
                + RecipeMaps.laserEngraverRecipes.getAllRecipes().size(), "disabled recipes add nothing");
        } finally {
            Config.enableEasyWirelessRecipes = enabled;
        }
        require(supported == 162, "native wireless recipes included");
        System.out.println("WIRELESS_LASER_QA: 112 catalog entries and " + supported + " simple recipes passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
