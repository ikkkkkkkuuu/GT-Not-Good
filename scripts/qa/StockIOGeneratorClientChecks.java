package com.xyp.gtnotgood.qa;

import static gregtech.api.util.GTRecipeConstants.FUEL_VALUE;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.common.blocks.stockio.StockIOLogic;
import com.xyp.gtnotgood.common.blocks.stockio.TileStockIOInterface;
import com.xyp.gtnotgood.common.parts.stockio.PartStockIOInterface;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.me.helpers.AENetworkProxy;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicGenerator;
import gregtech.api.recipe.maps.FuelBackend;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;

/** Four isolated real ME grids exercise native generator ticks without adding fixtures to the release jar. */
@Mod(modid = "stockiogeneratorqa", name = "Stock IO Generator QA", version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class StockIOGeneratorClientChecks {

    private final List<Fixture> fixtures = new ArrayList<>();
    private boolean started;
    private boolean stopRequested;
    private volatile boolean finished;
    private int ticks;
    private int stage;
    private int stageStart;
    private Fixture diesel;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.stockio.generator.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        if (finished && !stopRequested) {
            stopRequested = true;
            mc.shutdown();
        } else if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("stockio-generator-qa-" + System.currentTimeMillis(), "Stock IO Generator QA",
                new WorldSettings(47L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            ticks++;
            if (ticks == 1) setup(player);
            if (ticks > 1600) throw new AssertionError("Generator QA timed out at stage " + stage);
            if (stage == 0 && ticks >= 100 && fixtures.stream().allMatch(fixture -> fixture.proxy.isActive())) {
                for (Fixture fixture : fixtures) verifyDirect(fixture);
                verifySuperheated(fixtures.get(0));
                verifySuperheated(fixtures.get(1));
                verifyItemFuels(diesel);
                prepareRecovery();
                stageStart = ticks;
                stage = 1;
            } else if (stage == 1 && ticks - stageStart >= 30) {
                verifyRecovery();
                for (Fixture fixture : fixtures) prepareNatural(fixture);
                stageStart = ticks;
                stage = 2;
            } else if (stage == 2 && ticks - stageStart >= 100) {
                for (Fixture fixture : fixtures) verifyNatural(fixture);
                for (Fixture fixture : fixtures) {
                    fixture.reset();
                    fixture.setFluid(fixture.fuel, 20 * fixture.consumed());
                    fixture.offlineStock = fixture.fluid(fixture.fuel);
                    fixture.base.disableWorking();
                    fixture.base.getWorld().setBlockToAir(fixture.x - 1, 10, 0);
                }
                stageStart = ticks;
                stage = 3;
            } else if (stage == 3 && ticks - stageStart >= 40
                && fixtures.stream().noneMatch(fixture -> fixture.proxy.isActive())) {
                for (Fixture fixture : fixtures) {
                    require(!fixture.logic.isOnline(), fixture.label + ": actual unpowered ME grid is offline");
                    fixture.base.enableWorking();
                    fixture.unchanged(10, "offline grid supplies no fuel or EU");
                    fixture.base.disableWorking();
                    fixture.base.getWorld().setBlock(fixture.x - 1, 10, 0,
                        AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
                }
                stageStart = ticks;
                stage = 4;
            } else if (stage == 4 && ticks - stageStart >= 40
                && fixtures.stream().allMatch(fixture -> fixture.proxy.isActive())) {
                for (Fixture fixture : fixtures) {
                    require(fixture.fluid(fixture.fuel) == fixture.offlineStock && fixture.eu() == 0,
                        fixture.label + ": restored real ME grid retains all fuel from the offline check");
                    fixture.emptyInputs();
                }
                Files.write(new File(outputDirectory(), "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                System.out.println("STOCK_IO_GENERATOR_QA: PASS");
                finished = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        fixtures.add(fixture(player, 0, false, ItemList.Generator_Steam_Turbine_LV.get(1), Materials.Steam.getGas(1),
            "GT steam turbine"));
        fixtures.add(fixture(player, 8, true, GTNGItemList.SteamTurbineLV.get(1), Materials.Steam.getGas(1),
            "addon steam turbine"));
        diesel = fixture(player, 16, false, ItemList.Generator_Diesel_LV.get(1), Materials.Diesel.getFluid(1),
            "GT diesel generator");
        fixtures.add(diesel);
        fixtures.add(fixture(player, 24, true, ItemList.Generator_Gas_Turbine_LV.get(1), Materials.Methane.getGas(1),
            "GT gas turbine"));
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(12.5, 12, -2.5, 0, 15);
    }

    private static Fixture fixture(EntityPlayerMP player, int x, boolean part, ItemStack generator, FluidStack fuel,
        String label) {
        require(generator != null && fuel != null, label + ": registered native machine and fuel exist");
        World world = player.worldObj;
        var definitions = AEApi.instance().definitions();
        StockIOLogic logic;
        AENetworkProxy proxy;
        if (part) {
            world.setBlock(x, 10, 0, definitions.blocks().multiPart().maybeBlock().get());
            IPartHost host = (IPartHost) world.getTileEntity(x, 10, 0);
            require(host.addPart(definitions.parts().cableGlass().stack(AEColor.Transparent, 1),
                ForgeDirection.UNKNOWN, player) != null, label + ": native AE cable placed");
            require(host.addPart(GTNGItemList.StockIOInterfacePart.get(1), ForgeDirection.SOUTH, player) != null,
                label + ": native cable-part interface placed");
            PartStockIOInterface stock = (PartStockIOInterface) host.getPart(ForgeDirection.SOUTH);
            logic = stock.getLogic();
            proxy = stock.getProxy();
        } else {
            world.setBlock(x, 10, 0, Block.getBlockFromItem(GTNGItemList.StockIOInterface.get(1).getItem()));
            TileStockIOInterface stock = (TileStockIOInterface) world.getTileEntity(x, 10, 0);
            stock.setOwnerName(player.getCommandSenderName());
            stock.setTargetSide(ForgeDirection.SOUTH);
            logic = stock.getLogic();
            proxy = stock.getProxy();
        }
        world.setBlock(x - 1, 10, 0, definitions.blocks().energyCellCreative().maybeBlock().get());
        world.setBlock(x + 1, 10, 0, definitions.blocks().drive().maybeBlock().get());
        TileDrive drive = (TileDrive) world.getTileEntity(x + 1, 10, 0);
        drive.getInternalInventory().setInventorySlotContents(0, definitions.items().cell64k().maybeStack(1).get());
        drive.getInternalInventory().setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
        world.setBlock(x, 10, 1, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(x, 10, 1);
        base.setInitialValuesAsNBT(null, (short) generator.getItemDamage());
        base.setOwnerUuid(player.getUniqueID());
        base.setFrontFacing(ForgeDirection.NORTH);
        base.disableWorking();
        require(base.getMetaTileEntity() instanceof MTEBasicGenerator, label + ": actual MTEBasicGenerator instance");
        return new Fixture(label, x, fuel, logic, proxy, new MachineSource(drive), base);
    }

    private static void verifyDirect(Fixture fixture) throws Exception {
        fixture.reset();
        int consumed = fixture.consumed();
        long value = fixture.value();
        require(consumed > 0 && value > 0, fixture.label + ": native fuel value and consumed liters are positive");
        System.out.println("STOCK_IO_GENERATOR_QA: " + fixture.label + " consumes " + consumed + " mB for " + value
            + " EU at native efficiency " + fixture.generator.getEfficiency());

        fixture.setFluid(fixture.fuel, 10 * consumed);
        long directCapacity = fixture.generator.maxEUStore();
        long directOperations = Math.min(10, directCapacity / value);
        fixture.generator.onPostTick(fixture.base, 10);
        fixture.expectFuel((10 - directOperations) * consumed, directOperations * value,
            "native 10-tick path debits exact ME fuel and grants exact EU within its buffer");
        fixture.emptyInputs();

        fixture.reset();
        fixture.logic.setLimitedMode(true);
        fixture.logic.setFixedMode(true);
        fixture.logic.getPolicy(true, 0).reserve = 2L * consumed;
        fixture.logic.getPolicy(true, 0).batch = 4 * consumed;
        fixture.setFluid(fixture.fuel, 6 * consumed - 1);
        fixture.unchanged(10, "fixed shortage after reserve supplies no fuel or EU");
        fixture.setFluid(fixture.fuel, 6 * consumed);
        fixture.generator.onPostTick(fixture.base, 20);
        fixture.expectFuel(2L * consumed, 4 * value,
            "fixed availability caps a burn while retaining the ME reserve");
        fixture.base.setStoredEU(0);
        fixture.logic.setFixedMode(false);
        fixture.unchanged(30, "reserve-only stock supplies no fuel or EU");

        fixture.reset();
        fixture.logic.setFixedMode(true);
        fixture.logic.getPolicy(true, 0).batch = 2 * consumed;
        fixture.setFluid(fixture.fuel, 12 * consumed);
        fixture.generator.onPostTick(fixture.base, 10);
        fixture.expectFuel(10L * consumed, 2 * value, "one fuel check sees only the configured fixed upper bound");

        fixture.reset();
        fixture.setFluid(fixture.fuel, 12 * consumed);
        long capacity = fixture.generator.maxEUStore();
        long spare = 2 * value + value / 2;
        require(spare < capacity, fixture.label + ": room-floor fixture fits the native buffer");
        fixture.base.setStoredEU(capacity - spare);
        fixture.generator.onPostTick(fixture.base, 10);
        fixture.expectFuel(10L * consumed, capacity - value / 2,
            "fuel operations floor to whole units of remaining EU room");

        fixture.reset();
        fixture.setFluid(fixture.fuel, 20 * consumed);
        fixture.unchanged(11, "a non-10-tick call supplies no fuel or EU");
        fixture.base.setStoredEU(fixture.generator.maxEUStore());
        fixture.unchanged(10, "a full EU buffer supplies no fuel or EU");
        fixture.base.setStoredEU(0);
        fixture.base.disableWorking();
        fixture.unchanged(10, "a disabled GT generator supplies no fuel or EU");
        fixture.base.enableWorking();
        fixture.logic.setEnabled(false);
        fixture.unchanged(10, "a disabled interface supplies no fuel or EU");
        fixture.logic.setEnabled(true);

        fixture.logic.setFixedMode(true);
        fixture.logic.getPolicy(true, 0).batch = consumed;
        fixture.generator.setFillableStack(fixture.stack(consumed));
        fixture.generator.onPostTick(fixture.base, 10);
        fixture.expectFuel(20L * consumed, value, "successful local fuel takes priority without a second ME debit");
        fixture.generator.setFillableStack(null);
        fixture.base.setStoredEU(0);
        fixture.generator.onPostTick(fixture.base, 20);
        fixture.expectFuel(19L * consumed, value, "a following fuel cycle can fall back to ME");
        fixture.emptyInputs();
        if (consumed > 1) {
            fixture.base.setStoredEU(0);
            fixture.generator.setFillableStack(fixture.stack(consumed - 1));
            fixture.generator.onPostTick(fixture.base, 30);
            require(fixture.fluid(fixture.fuel) == 18L * consumed && fixture.eu() == value
                && fixture.generator.getFillableStack().amount == consumed - 1,
                fixture.label + ": insufficient local fuel is not mixed into the whole ME operation");
        }
        fixture.clearLocal();
        fixture.base.disableWorking();
    }

    private static void verifySuperheated(Fixture fixture) throws Exception {
        fixture.reset();
        FluidStack superheated = GTModHandler.getSuperHeatedSteam(1);
        require(superheated != null && GTModHandler.isSuperHeatedSteam(superheated),
            fixture.label + ": real registered superheated-steam fixture exists");
        fixture.logic.setFluidFilter(0, superheated);
        fixture.setFluid(superheated, 32000);
        fixture.generator.onPostTick(fixture.base, 10);
        require(fixture.fluid(superheated) == 32000 && fixture.eu() == 0,
            fixture.label + ": native steam rejection cannot consume ME superheated steam");
        fixture.setFluid(superheated, 0);
        fixture.reset();
        fixture.base.disableWorking();
    }

    private static void verifyItemFuels(Fixture fixture) throws Exception {
        verifyFuelWithoutContainer(fixture);
        ItemStack cell = GTOreDictUnificator.get(OrePrefixes.cell, Materials.Diesel, 1);
        require(cell != null && GTUtility.getFluidForFilledItem(cell, true) != null,
            "native diesel cell exercises the filled-container branch");
        var nativeCellRecipe = fixture.generator.getRecipeMap().findRecipeQuery().items(cell).find();
        System.out.println("STOCK_IO_GENERATOR_QA: native diesel cell int=" + fixture.generator.getFuelValue(cell)
            + ", long=" + fixture.generator.getFuelValue(cell, true) + ", empty=" + fixture.generator.getEmptyContainer(cell)
            + ", recipe inputs=" + (nativeCellRecipe == null ? "none" : Arrays.toString(nativeCellRecipe.mInputs))
            + ", fluids=" + (nativeCellRecipe == null ? "none" : Arrays.toString(nativeCellRecipe.mFluidInputs))
            + ", outputs=" + (nativeCellRecipe == null ? "none" : Arrays.toString(nativeCellRecipe.mOutputs)));
        if (fixture.generator.getFuelValue(cell, true) <= 0) {
            var nativeFuel = ((FuelBackend) fixture.generator.getRecipeMap().getBackend()).findFuel(fixture.fuel);
            require(nativeFuel != null && nativeFuel.mSpecialValue > 0, "native diesel fluid fuel metadata exists");
            require(!GTValues.RA.stdBuilder().itemInputs(cell).itemOutputs(GTUtility.getContainerItem(cell, true))
                .duration(0).eut(0).metadata(FUEL_VALUE, nativeFuel.mSpecialValue)
                .addTo(fixture.generator.getRecipeMap()).isEmpty(),
                "QA diesel cell mapping retains native fuel metadata and real container identity");
        }
        verifyItemFuel(fixture, cell, "real filled diesel cell");
        fixture.generator.setInventorySlotContents(fixture.generator.getOutputSlot(), null);
        require(!GTValues.RA.stdBuilder().itemInputs(new ItemStack(Items.paper)).itemOutputs(new ItemStack(Items.glass_bottle))
            .duration(0).eut(0).metadata(FUEL_VALUE, 1).addTo(fixture.generator.getRecipeMap()).isEmpty(),
            "QA solid fuel is registered through the native GT fuel recipe builder");
        require(fixture.generator.solidFuelOverride(new ItemStack(Items.paper)), "QA paper uses the native solid-fuel branch");
        verifyItemFuel(fixture, new ItemStack(Items.paper), "native synthetic solid fuel");
    }

    private static void verifyFuelWithoutContainer(Fixture fixture) throws Exception {
        ItemStack fuel = new ItemStack(Items.apple);
        require(!GTValues.RA.stdBuilder().itemInputs(fuel).duration(0).eut(0).metadata(FUEL_VALUE, 1)
            .addTo(fixture.generator.getRecipeMap()).isEmpty(), "QA fuel without a container uses the native fuel map");
        fixture.reset();
        fixture.logic.setFluidFilter(0, null);
        fixture.logic.setItemFilter(0, fuel);
        fixture.logic.setFixedMode(true);
        fixture.logic.getPolicy(false, 0).batch = 1;
        fixture.setItems(fuel, 3);
        long value = fixture.generator.getFuelValue(fuel, true);
        require(value > 0 && fixture.generator.getEmptyContainer(fuel) == null,
            "native solid fuel can resolve energy without creating an output container");
        fixture.generator.setInventorySlotContents(fixture.generator.getOutputSlot(), new ItemStack(Items.emerald, 64));
        fixture.generator.onPostTick(fixture.base, 10);
        ItemStack output = fixture.generator.getStackInSlot(fixture.generator.getOutputSlot());
        require(fixture.items(fuel) == 2 && fixture.eu() == value && output != null
            && output.getItem() == Items.emerald && output.stackSize == 64,
            "a full output slot still permits native ME fuel without a returned container");
        fixture.emptyInputs();
        fixture.base.disableWorking();
    }

    private static void verifyItemFuel(Fixture fixture, ItemStack fuel, String label) throws Exception {
        fixture.reset();
        fixture.logic.setFluidFilter(0, null);
        fixture.logic.setItemFilter(0, fuel);
        fixture.logic.setFixedMode(true);
        fixture.logic.getPolicy(false, 0).batch = 1;
        fixture.setItems(fuel, 3);
        long value = fixture.generator.getFuelValue(fuel);
        if (value <= 0) value = fixture.generator.getFuelValue(fuel, true);
        ItemStack empty = fixture.generator.getEmptyContainer(fuel);
        require(value > 0, label + ": native getters resolve energy and an optional returned container");
        fixture.generator.setInventorySlotContents(fixture.generator.getOutputSlot(), new ItemStack(Items.emerald, 64));
        long before = fixture.items(fuel);
        fixture.generator.onPostTick(fixture.base, 10);
        ItemStack blockedOutput = fixture.generator.getStackInSlot(fixture.generator.getOutputSlot());
        if (empty != null) {
            require(fixture.items(fuel) == before && fixture.eu() == 0,
                label + ": a blocked native output prevents ME fuel and EU changes");
        } else {
            require(fixture.items(fuel) == before - 1 && fixture.eu() == value && blockedOutput != null
                && blockedOutput.getItem() == Items.emerald && blockedOutput.stackSize == 64,
                label + ": native fuel without an empty output still burns with a full output slot");
        }
        fixture.base.setStoredEU(0);
        fixture.setItems(fuel, 3);
        before = fixture.items(fuel);
        fixture.generator.setInventorySlotContents(fixture.generator.getOutputSlot(), null);
        fixture.generator.onPostTick(fixture.base, 20);
        ItemStack output = fixture.generator.getStackInSlot(fixture.generator.getOutputSlot());
        require(fixture.items(fuel) == before - 1 && fixture.eu() == value
            && (empty == null ? output == null : output != null && GTUtility.areStacksEqual(output, empty)
                && output.stackSize == empty.stackSize),
            label + ": one ME fuel produces exact native EU and its native optional output");
        fixture.emptyInputs();
        fixture.base.disableWorking();
    }

    private void prepareRecovery() throws Exception {
        for (Fixture fixture : fixtures) {
            fixture.base.disableWorking();
            fixture.base.setStoredEU(0);
            fixture.logic.setRecycle(true);
            fixture.generator.setInventorySlotContents(fixture.generator.getInputSlot(), new ItemStack(Items.paper, 7));
            fixture.generator.setFillableStack(fixture.stack(31 * fixture.consumed()));
            fixture.protectedFluidStock = fixture.fluid(fixture.fuel);
            fixture.protectedItemStock = fixture.items(new ItemStack(Items.paper));
        }
        diesel.setItems(new ItemStack(Items.glass_bottle), 0);
        diesel.logic.setItemFilter(1, new ItemStack(Items.glass_bottle));
        require(diesel.generator.getStackInSlot(diesel.generator.getOutputSlot()) != null,
            "recovery fixture keeps the empty container produced by actual native burning");
    }

    private void verifyRecovery() throws Exception {
        for (Fixture fixture : fixtures) {
            ItemStack input = fixture.generator.getStackInSlot(fixture.generator.getInputSlot());
            FluidStack tank = fixture.generator.getFillableStack();
            require(input != null && input.getItem() == Items.paper && input.stackSize == 7
                && tank != null && tank.isFluidEqual(fixture.fuel) && tank.amount == 31 * fixture.consumed()
                && fixture.fluid(fixture.fuel) == fixture.protectedFluidStock
                && fixture.items(new ItemStack(Items.paper)) == fixture.protectedItemStock,
                fixture.label + ": native output recovery leaves fuel tank and input slot untouched");
        }
        require(diesel.generator.getStackInSlot(diesel.generator.getOutputSlot()) == null
            && diesel.items(new ItemStack(Items.glass_bottle)) == 1,
            "marked native empty container recovers from the generator output into ME");
    }

    private static void prepareNatural(Fixture fixture) throws Exception {
        fixture.reset();
        int consumed = fixture.consumed();
        fixture.logic.setLimitedMode(true);
        fixture.logic.setFixedMode(true);
        fixture.logic.getPolicy(true, 0).reserve = 2L * consumed;
        fixture.naturalValue = fixture.value();
        long capacity = fixture.generator.maxEUStore();
        fixture.naturalOperations = Math.min(3, capacity / fixture.naturalValue / 2);
        require(fixture.naturalOperations > 0, fixture.label + ": two natural batches fit the native EU buffer");
        fixture.logic.getPolicy(true, 0).batch = (int) fixture.naturalOperations * consumed;
        fixture.naturalSeed = (2 + 2 * fixture.naturalOperations) * consumed + consumed / 2;
        fixture.setFluid(fixture.fuel, (int) fixture.naturalSeed);
        fixture.base.enableWorking();
        fixture.emptyInputs();
    }

    private static void verifyNatural(Fixture fixture) throws Exception {
        fixture.base.disableWorking();
        fixture.expectFuel(fixture.naturalSeed - 2 * fixture.naturalOperations * fixture.consumed(),
            2 * fixture.naturalOperations * fixture.naturalValue,
            "ordinary world ticks consume two fixed fuel batches and stop above reserve");
        fixture.emptyInputs();
    }

    private static File outputDirectory() {
        return new File(System.getProperty("gtng.stockio.generator.qa.output", "."));
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            Files.write(new File(outputDirectory(), "result.txt").toPath(), "FAIL".getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        finished = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("STOCK_IO_GENERATOR_QA: " + message);
    }

    private static final class Fixture {

        private final String label;
        private final int x;
        private final FluidStack fuel;
        private final StockIOLogic logic;
        private final AENetworkProxy proxy;
        private final MachineSource source;
        private final BaseMetaTileEntity base;
        private final MTEBasicGenerator generator;
        private long naturalSeed;
        private long naturalValue;
        private long naturalOperations;
        private long protectedFluidStock;
        private long protectedItemStock;
        private long offlineStock;

        private Fixture(String label, int x, FluidStack fuel, StockIOLogic logic, AENetworkProxy proxy,
            MachineSource source, BaseMetaTileEntity base) {
            this.label = label;
            this.x = x;
            this.fuel = fuel;
            this.logic = logic;
            this.proxy = proxy;
            this.source = source;
            this.base = base;
            generator = (MTEBasicGenerator) base.getMetaTileEntity();
        }

        private void reset() {
            base.disableWorking();
            clearLocal();
            base.setStoredEU(0);
            logic.setEnabled(true);
            logic.setRecycle(false);
            logic.setAutoPullItems(false);
            logic.setAutoPullFluids(false);
            logic.setLimitedMode(false);
            logic.setFixedMode(false);
            logic.setItemFilter(0, null);
            logic.setItemFilter(1, null);
            logic.setFluidFilter(0, fuel);
            logic.getPolicy(true, 0).reserve = 0;
            logic.policyChanged();
            base.enableWorking();
        }

        private void clearLocal() {
            generator.setFillableStack(null);
            generator.setInventorySlotContents(generator.getInputSlot(), null);
            generator.setInventorySlotContents(generator.getOutputSlot(), null);
        }

        private void emptyInputs() {
            FluidStack tank = generator.getFillableStack();
            require((tank == null || tank.amount == 0) && generator.getStackInSlot(generator.getInputSlot()) == null,
                label + ": ME burning never fills the physical fuel tank or input slot");
        }

        private int consumed() {
            return generator.consumedFluidPerOperation(fuel.copy());
        }

        private long value() {
            long value = generator.getFuelValue(fuel.copy());
            return value > 0 ? value : generator.getFuelValue(fuel.copy(), true);
        }

        private long eu() {
            return base.getUniversalEnergyStored();
        }

        private void expectFuel(long expectedStock, long expectedEU, String message) throws Exception {
            long stock = fluid(fuel), energy = eu();
            require(stock == expectedStock && energy == expectedEU,
                label + ": " + message + " (EU=" + energy + "/" + expectedEU + ", ME=" + stock + "/" + expectedStock
                    + ", cap=" + generator.maxEUStore() + ")");
        }

        private FluidStack stack(int amount) {
            FluidStack result = fuel.copy();
            result.amount = amount;
            return result;
        }

        private void unchanged(long tick, String message) throws Exception {
            long stock = fluid(fuel), energy = eu();
            generator.onPostTick(base, tick);
            require(fluid(fuel) == stock && eu() == energy, label + ": " + message);
            emptyInputs();
        }

        private long fluid(FluidStack type) throws Exception {
            var request = AEFluidStack.create(type).setStackSize(Long.MAX_VALUE);
            var result = proxy.getStorage().getFluidInventory().extractItems(request, Actionable.SIMULATE, source);
            return result == null ? 0 : result.getStackSize();
        }

        private void setFluid(FluidStack type, int amount) throws Exception {
            var inventory = proxy.getStorage().getFluidInventory();
            inventory.extractItems(AEFluidStack.create(type).setStackSize(Long.MAX_VALUE), Actionable.MODULATE, source);
            if (amount > 0) require(inventory.injectItems(AEFluidStack.create(type).setStackSize(amount),
                Actionable.MODULATE, source) == null, label + ": native ME fluid seed accepted");
        }

        private long items(ItemStack type) throws Exception {
            var result = proxy.getStorage().getItemInventory().extractItems(
                AEItemStack.create(type).setStackSize(Long.MAX_VALUE), Actionable.SIMULATE, source);
            return result == null ? 0 : result.getStackSize();
        }

        private void setItems(ItemStack type, int amount) throws Exception {
            var inventory = proxy.getStorage().getItemInventory();
            inventory.extractItems(AEItemStack.create(type).setStackSize(Long.MAX_VALUE), Actionable.MODULATE, source);
            if (amount > 0) require(inventory.injectItems(AEItemStack.create(type).setStackSize(amount),
                Actionable.MODULATE, source) == null, label + ": native ME item seed accepted");
        }
    }
}
