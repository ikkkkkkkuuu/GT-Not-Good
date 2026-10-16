package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.common.compat.AutomaticMachineCircuit;
import com.xyp.gtnotgood.common.compat.VirtualMachineMolds;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitMEPatternBuffer;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitPatternCodec;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.DualityInterface;
import appeng.me.cache.CraftingGridCache;
import appeng.tile.misc.TileInterface;
import appeng.tile.storage.TileDrive;
import appeng.util.inv.MEInventoryCrafting;
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
import gregtech.api.interfaces.tileentity.IVoidable;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

@Mod(
    modid = "automaticcircuitpatternqa",
    name = "Automatic circuit pattern QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class AutomaticCircuitPatternChecks {

    private boolean started;
    private volatile boolean finished;
    private int ticks;
    private int migrationTick;
    private TileInterface itemInterface, fluidInterface;
    private BaseMetaTileEntity itemTile, fluidTile;
    private CircuitMEPatternBuffer hatch;
    private ItemStack firstPattern, secondPattern, fluidPattern, consumedPattern, legacyPattern;
    private ItemStack firstOriginal, fluidOriginal;
    private ICraftingPatternDetails firstDetails, secondDetails, fluidDetails, consumedDetails, legacyDetails;
    private final List<Future<ICraftingJob>> plans = new ArrayList<>();
    private final ItemStack itemOutput = named(Items.feather, "Automatic circuit QA item");
    private final ItemStack fluidOutput = named(Items.sugar, "Automatic circuit QA fluid");
    private final ItemStack consumedOutput = named(Items.quartz, "Automatic circuit QA consumed mold");
    private final ItemStack legacyOutput = named(Items.stick, "Automatic circuit QA legacy");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.automatic.circuit.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            mc.shutdown();
            return;
        }
        mc.gameSettings.pauseOnLostFocus = false;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("automatic-circuit-pattern-" + System.currentTimeMillis(),
                "Automatic circuit pattern QA",
                new WorldSettings(47L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (++ticks > 1600) throw new AssertionError("automatic circuit pattern QA timeout");
            if (ticks == 1) setup(player);
            if (ticks == 120) inspectAndPlan(player);
            if (!plans.isEmpty() && migrationTick == 0 && plans.stream().allMatch(Future::isDone)) {
                checkPlans();
                checkSingleBlocks(player.worldObj);
                migrate(player.worldObj);
                migrationTick = ticks;
            }
            if (migrationTick > 0 && ticks >= migrationTick + 40) {
                checkMigrated(player.worldObj);
                finish("PASS");
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish("FAIL");
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        ItemStack plate = ItemList.Shape_Extruder_Plate.get(0);
        for (int circuit : new int[] { 1, 2 }) register(RecipeMaps.extruderRecipes,
            new ItemStack[] { new ItemStack(Items.apple), plate.copy() }, circuit, null, itemOutput);
        register(RecipeMaps.extruderRecipes,
            new ItemStack[] { new ItemStack(Items.apple), ItemList.Shape_Extruder_Rod.get(0) }, 3, null, legacyOutput);
        register(RecipeMaps.fluidSolidifierRecipes, new ItemStack[] { ItemList.Shape_Mold_Plate.get(0) }, 21,
            new FluidStack(FluidRegistry.WATER, 144), fluidOutput);
        register(RecipeMaps.fluidSolidifierRecipes, new ItemStack[] { ItemList.Shape_Extruder_Rod.get(3) }, -1,
            new FluidStack(FluidRegistry.WATER, 144), consumedOutput);
        itemTile = place(world, player, 0, 10, 0, ItemList.Machine_LV_Extruder.get(1));
        fluidTile = place(world, player, 6, 10, 0, ItemList.Machine_MV_FluidSolidifier.get(1));
        require(AutomaticMachineCircuit.supports(itemTile) && AutomaticMachineCircuit.supports(fluidTile),
            "both test single-block machines accept automatic plans");
        itemInterface = network(world, player, 0, false);
        fluidInterface = network(world, player, 6, true);
        hatch = (CircuitMEPatternBuffer) place(world, player, 12, 10, 0, GTNGItemList.CircuitMEPatternBuffer.get(1))
            .getMetaTileEntity();
        hatch.setConnectsToAllSides(true);
        world.setBlock(12, 10, 1, AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get());
        firstPattern = pattern(itemOutput, AEItemStack.create(new ItemStack(Items.apple)), tool(plate), circuit(1));
        secondPattern = pattern(itemOutput, AEItemStack.create(new ItemStack(Items.apple)), tool(plate), circuit(2));
        fluidPattern = pattern(fluidOutput, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 144)),
            tool(ItemList.Shape_Mold_Plate.get(1)), circuit(21));
        consumedPattern = pattern(consumedOutput, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 144)),
            AEItemStack.create(ItemList.Shape_Extruder_Rod.get(3)));
        legacyPattern = pattern(legacyOutput, AEItemStack.create(new ItemStack(Items.apple)));
        firstOriginal = firstPattern.copy();
        fluidOriginal = fluidPattern.copy();
        itemInterface.getInterfaceDuality().getPatterns().setInventorySlotContents(0, firstPattern);
        itemInterface.getInterfaceDuality().getPatterns().setInventorySlotContents(1, secondPattern);
        itemInterface.getInterfaceDuality().getPatterns().setInventorySlotContents(2, legacyPattern);
        fluidInterface.getInterfaceDuality().getPatterns().setInventorySlotContents(0, fluidPattern);
        fluidInterface.getInterfaceDuality().getPatterns().setInventorySlotContents(1, consumedPattern);
        player.playerNetServerHandler.setPlayerLocation(2, 12, -2, 0, 0);
    }

    private static TileInterface network(World world, EntityPlayerMP player, int x, boolean fluid) {
        var blocks = AEApi.instance().definitions().blocks();
        world.setBlock(x, 10, 1, fluid ? ItemAndBlockHolder.INTERFACE : blocks.iface().maybeBlock().get());
        TileInterface tile = (TileInterface) world.getTileEntity(x, 10, 1);
        tile.setOrientation(ForgeDirection.UP, ForgeDirection.SOUTH);
        world.setBlock(x + 1, 10, 1, blocks.energyCellCreative().maybeBlock().get());
        world.setBlock(x - 1, 10, 1, blocks.drive().maybeBlock().get());
        TileDrive drive = (TileDrive) world.getTileEntity(x - 1, 10, 1);
        drive.getInternalInventory().setInventorySlotContents(0,
            AEApi.instance().definitions().items().cell64k().maybeStack(1).get());
        drive.getInternalInventory().setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
        return tile;
    }

    private void inspectAndPlan(EntityPlayerMP player) throws Exception {
        DualityInterface items = itemInterface.getInterfaceDuality();
        DualityInterface fluids = fluidInterface.getInterfaceDuality();
        require(items.getProxy().isActive() && fluids.getProxy().isActive() && hatch.isActive(), "active QA grids");
        items.updateCraftingList();
        fluids.updateCraftingList();
        firstDetails = find(items, itemOutput, 1);
        secondDetails = find(items, itemOutput, 2);
        fluidDetails = find(fluids, fluidOutput, 21);
        consumedDetails = find(fluids, consumedOutput, -1);
        legacyDetails = find(items, legacyOutput, -1);
        for (ICraftingPatternDetails details : new ICraftingPatternDetails[] { firstDetails, secondDetails,
            fluidDetails }) {
            require(details.getCondensedAEInputs().length == 1, "native provider indexes only consumed materials");
            for (IAEStack<?> input : details.getAEInputs())
                if (
                    input != null
                ) require(!CircuitPatternCodec.isCircuit(input)
                    && !(input instanceof IAEItemStack item && VirtualMachineMolds.indexOf(item.getItemStack()) >= 0),
                    "AE inputs omit reusable tools");
            ICraftingPatternDetails restored = CircuitPatternCodec.decode(details.getPattern().copy(), player.worldObj);
            require(details.equals(restored) && details.hashCode() == restored.hashCode(),
                "native pattern decode preserves CPU identity");
        }
        require(!firstDetails.equals(secondDetails), "same materials and output retain separate circuit identities");
        require(fluidDetails.getAEInputs()[0] instanceof IAEFluidStack,
            "Ultimate pattern provider preserves native fluid stacks");
        require(
            consumedDetails.getCondensedAEInputs().length == 2
                && CircuitPatternCodec.molds(consumedDetails).length == 0,
            "positive-quantity mold remains an AE input");
        require(
            ItemStack.areItemStacksEqual(firstOriginal, firstPattern)
                && ItemStack.areItemStacksEqual(fluidOriginal, fluidPattern),
            "provider leaves physical encoded tools intact");
        supply(items, false);
        supply(fluids, true);
        for (int i = 0; i < 4; i++) {
            DualityInterface provider = i < 2 ? items : fluids;
            ItemStack output = i == 0 ? itemOutput : i == 1 ? legacyOutput : i == 2 ? fluidOutput : consumedOutput;
            plans.add(provider.getProxy().getCrafting().beginCraftingJob(player.worldObj, provider.getProxy().getGrid(),
                provider.getActionSource(), AEItemStack.create(output.copy()), null));
        }
    }

    private static void supply(DualityInterface provider, boolean fluid) throws Exception {
        var storage = provider.getProxy().getStorage();
        var source = provider.getActionSource();
        require(storage.getItemInventory().injectItems(AEItemStack.create(new ItemStack(Items.apple, 16)),
            Actionable.MODULATE, source) == null, "QA consumed items are available in ME");
        if (
            fluid
        ) require(
            storage.getFluidInventory().injectItems(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 10000)),
                Actionable.MODULATE, source) == null,
            "QA native water is available in ME");
        for (ItemStack tool : new ItemStack[] { ItemList.Shape_Extruder_Plate.get(1), ItemList.Shape_Mold_Plate.get(1),
            ItemList.Shape_Extruder_Rod.get(1), GTUtility.getIntegratedCircuit(1), GTUtility.getIntegratedCircuit(21) })
            require(storage.getItemInventory().extractItems(AEItemStack.create(tool).setStackSize(1),
                Actionable.SIMULATE, source) == null, "QA network contains no physical tool");
    }

    private void checkPlans() throws Exception {
        for (int i = 0; i < plans.size(); i++) {
            ICraftingJob job = plans.get(i).get();
            require(job.isSimulation() == (i == 3), "only the physically consumed mold produces missing materials");
            var plan = AEApi.instance().storage().createAEStackList();
            job.populatePlan(plan);
            boolean consumed = false;
            boolean material = false;
            for (IAEStack<?> stack : plan) {
                require(!CircuitPatternCodec.isCircuit(stack), "native crafting plan does not index a circuit");
                if (stack instanceof IAEItemStack item && VirtualMachineMolds.indexOf(item.getItemStack()) >= 0) {
                    require(i == 3
                        && GTUtility.areStacksEqual(item.getItemStack(), ItemList.Shape_Extruder_Rod.get(1), true)
                        && item.getStackSize() >= 3, "only consumed mold is present in native material plan");
                    consumed = true;
                }
                if (i < 2 && stack instanceof IAEItemStack item && item.getItem() == Items.apple) material = true;
                if (
                    i >= 2 && stack instanceof IAEFluidStack water
                        && water.getFluid() == FluidRegistry.WATER
                        && water.getStackSize() >= 144
                ) material = true;
            }
            require(consumed == (i == 3) && material, "native plan retains the required consumed quantities");
        }
    }

    private void checkSingleBlocks(World world) throws Exception {
        DualityInterface items = itemInterface.getInterfaceDuality();
        DualityInterface fluids = fluidInterface.getInterfaceDuality();
        for (int i = 0; i < 2; i++) {
            ICraftingPatternDetails details = i == 0 ? firstDetails : secondDetails;
            require(items.pushPattern(details, table(AEItemStack.create(new ItemStack(Items.apple)))),
                "ordinary interface pushes tool-free CPU batch");
            MTEBasicMachine machine = (MTEBasicMachine) itemTile.getMetaTileEntity();
            require(machine.getStackInSlot(machine.getCircuitSlot()).getItemDamage() == i + 1,
                "encoded circuit constrains identical material and output recipes");
            require(VirtualMachineMolds.get(machine).isItemEqual(ItemList.Shape_Extruder_Plate.get(1)),
                "encoded nonconsumed mold is selected without a physical tool");
            consume(itemTile, itemOutput);
        }
        require(items.pushPattern(legacyDetails, table(AEItemStack.create(new ItemStack(Items.apple)))),
            "old pattern omitting circuit and mold remains accepted");
        require(
            ((MTEBasicMachine) itemTile.getMetaTileEntity())
                .getStackInSlot(((MTEBasicMachine) itemTile.getMetaTileEntity()).getCircuitSlot()).getItemDamage() == 3,
            "legacy pattern still infers its unique circuit");
        consume(itemTile, legacyOutput);
        ICraftingPatternDetails restored = CircuitPatternCodec.decode(fluidDetails.getPattern().copy(), world);
        MEInventoryCrafting fluidTable = table(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 144)));
        require(fluids.pushPattern(restored, fluidTable), "fluid interface accepts CPU-redecoded runtime pattern");
        require(fluidTable.getAEStackInSlot(0) instanceof IAEFluidStack
            && fluidTable.getAEStackInSlot(0).getStackSize() == 144, "native CPU fluid table is not mutated");
        require(((MTEBasicMachine) fluidTile.getMetaTileEntity())
            .getStackInSlot(((MTEBasicMachine) fluidTile.getMetaTileEntity()).getCircuitSlot()).getItemDamage() == 21,
            "fluid-only delivery restores encoded circuit");
        consume(fluidTile, fluidOutput);
        require(
            !fluids.pushPattern(consumedDetails, table(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 144)))),
            "consumed mold cannot be omitted from CPU delivery");
        require(
            fluids.pushPattern(consumedDetails,
                table(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 144)),
                    AEItemStack.create(ItemList.Shape_Extruder_Rod.get(3)))),
            "consumed mold is delivered as a real ingredient");
        consume(fluidTile, consumedOutput);
    }

    private void migrate(World world) {
        itemInterface.getInterfaceDuality().getPatterns().setInventorySlotContents(0, null);
        fluidInterface.getInterfaceDuality().getPatterns().setInventorySlotContents(0, null);
        hatch.setInventorySlotContents(0, firstPattern);
        hatch.setInventorySlotContents(1, fluidPattern);
        require(
            ItemStack.areItemStacksEqual(firstOriginal, hatch.getStackInSlot(0))
                && ItemStack.areItemStacksEqual(fluidOriginal, hatch.getStackInSlot(1)),
            "the same physical patterns move to the circuit buffer without re-encoding");
    }

    private void checkMigrated(World world) throws Exception {
        for (int i = 0; i < 2; i++) {
            ItemStack physical = i == 0 ? firstPattern : fluidPattern;
            var slot = find(hatch, physical);
            ICraftingPatternDetails previous = i == 0 ? firstDetails : fluidDetails;
            require(slot.getPatternDetails().equals(previous), "migration retains native runtime pattern identity");
            require(((CraftingGridCache) hatch.getProxy().getCrafting()).getMediums(slot.getPatternDetails())
                .contains(hatch), "migrated circuit buffer is registered as the native crafting medium");
            MEInventoryCrafting delivered = i == 0 ? table(AEItemStack.create(new ItemStack(Items.apple)))
                : table(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 144)));
            require(hatch.pushPattern(slot.getPatternDetails(), delivered),
                "migrated pattern receives tool-free batch");
            RecipeMap<?> map = i == 0 ? RecipeMaps.extruderRecipes : RecipeMaps.fluidSolidifierRecipes;
            ProcessingLogic logic = new ProcessingLogic().setRecipeMapSupplier(() -> map)
                .setMachine(
                    (IVoidable) GregTechAPI.METATILEENTITIES[GTNGItemList.AssemblerMatrix.get(1).getItemDamage()])
                .setVoidProtection(false, false).setAvailableVoltage(128).setAvailableAmperage(1).setMaxParallel(1)
                .setInputItems(slot.getItemInputs()).setInputFluids(slot.getFluidInputs());
            require(logic.process().wasSuccessful() && slot.isEmpty(),
                "native multiblock processing consumes migrated materials and preserves virtual tools");
        }
    }

    private static void consume(BaseMetaTileEntity tile, ItemStack output) throws Exception {
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        Method getInputs = MTEBasicMachine.class.getDeclaredMethod("getAllInputs");
        getInputs.setAccessible(true);
        ItemStack[] items = (ItemStack[]) getInputs.invoke(machine);
        FluidStack fluid = machine.getFillableStack();
        FluidStack[] fluids = fluid == null ? new FluidStack[0] : new FluidStack[] { fluid };
        GTRecipe recipe = machine.getRecipeMap().findRecipeQuery().items(items).fluids(fluids)
            .voltage(GTValues.V[machine.mTier]).find();
        require(
            recipe != null && GTUtility.areStacksEqual(recipe.mOutputs[0], output, false)
                && recipe.isRecipeInputEqual(true, fluids, items),
            "native GT executes the selected single-block recipe");
        for (int i = 0; i < machine.mInputSlotCount; i++) {
            int slot = machine.getInputSlot() + i;
            ItemStack remaining = machine.getStackInSlot(slot);
            require(remaining == null || remaining.stackSize == 0, "single-block consumes each real item exactly once");
            machine.setInventorySlotContents(slot, null);
        }
        require(fluid == null || fluid.amount == 0, "single-block consumes native fluid exactly once");
        machine.setFillableStack(null);
    }

    private static ICraftingPatternDetails find(DualityInterface provider, ItemStack output, int circuit) {
        if (provider.craftingList != null) for (ICraftingPatternDetails details : provider.craftingList) {
            NBTTagCompound tag = details.getPattern().getTagCompound();
            int config = tag.hasKey("gtngPatternCircuit") ? tag.getInteger("gtngPatternCircuit") : -1;
            if (config != circuit) continue;
            for (IAEStack<?> stack : details.getAEOutputs())
                if (stack instanceof IAEItemStack item && GTUtility.areStacksEqual(item.getItemStack(), output, false))
                    return details;
        }
        throw new AssertionError("native provider did not register pattern for circuit " + circuit);
    }

    private static SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME> find(
        CircuitMEPatternBuffer hatch, ItemStack pattern) {
        var slots = hatch.inventories();
        while (slots.hasNext()) {
            var slot = slots.next();
            if (!slot.hasChanged(pattern, hatch.getBaseMetaTileEntity().getWorld())) return slot;
        }
        throw new AssertionError("migrated pattern slot missing");
    }

    private static void register(RecipeMap<?> map, ItemStack[] inputs, int circuit, FluidStack fluid,
        ItemStack output) {
        GTRecipeBuilder builder = GTRecipeBuilder.builder().itemInputs(inputs).itemOutputs(output.copy()).duration(100)
            .eut(8);
        if (circuit >= 0) builder.circuit(circuit);
        if (fluid != null) builder.fluidInputs(fluid);
        require(!builder.addTo(map).isEmpty(), "deterministic native recipe fixture registered");
    }

    private static ItemStack pattern(ItemStack output, IAEStack<?>... inputs) {
        ItemStack pattern = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList();
        for (IAEStack<?> input : inputs) {
            NBTTagCompound entry = new NBTTagCompound();
            input.writeToNBTGeneric(entry);
            in.appendTag(entry);
        }
        NBTTagList out = new NBTTagList();
        NBTTagCompound entry = new NBTTagCompound();
        AEItemStack.create(output.copy()).writeToNBTGeneric(entry);
        out.appendTag(entry);
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private static IAEItemStack tool(ItemStack mold) {
        return AEItemStack.create(mold.copy()).setStackSize(1);
    }

    private static IAEItemStack circuit(int config) {
        return AEItemStack.create(GTUtility.getIntegratedCircuit(config)).setStackSize(1);
    }

    private static MEInventoryCrafting table(IAEStack<?>... inputs) {
        MEInventoryCrafting table = new MEInventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, inputs.length, 1);
        for (int i = 0; i < inputs.length; i++) table.setInventorySlotContents(i, inputs[i]);
        return table;
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x, int y, int z, ItemStack stack) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.enableWorking();
        return tile;
    }

    private static ItemStack named(Item item, String name) {
        return new ItemStack(item).setStackDisplayName(name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("AUTOMATIC_CIRCUIT_PATTERN_QA: " + message);
    }

    private void finish(String result) {
        try {
            Files.write(new File("result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        System.out.println("AUTOMATIC_CIRCUIT_PATTERN_QA: " + result);
        finished = true;
    }
}
