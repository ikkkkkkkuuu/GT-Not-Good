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
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.common.compat.AutomaticMachineCircuit;
import com.xyp.gtnotgood.common.compat.VirtualMachineMolds;
import com.xyp.gtnotgood.common.items.patternsorter.PatternSorter;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitMEPatternBuffer;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitPatternCodec;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.parts.IPartHost;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
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
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.GTRecipe_WithAlt;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

@Mod(
    modid = "circuitlenspatternqa",
    name = "Circuit lens pattern QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class CircuitLensPatternChecks {

    private boolean started;
    private volatile boolean finished;
    private int ticks;
    private final List<Case> cases = new ArrayList<>();
    private final List<ProviderCase> providers = new ArrayList<>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.circuit.lens.qa")) FMLCommonHandler.instance().bus().register(this);
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
            mc.launchIntegratedServer("circuit-lens-pattern-" + System.currentTimeMillis(), "Circuit lens pattern QA",
                new WorldSettings(53L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (++ticks > 1600) throw new AssertionError("circuit lens pattern QA timeout");
            if (ticks == 1) setup(player);
            if (ticks == 120) inspect(player);
            if (ticks == 160) beginPlans(player);
            if (
                ticks > 160 && cases.stream().allMatch(test -> test.plan != null && test.plan.isDone())
                    && providers.stream().allMatch(test -> test.plan != null && test.plan.isDone())
            ) {
                checkPlans();
                checkProviders(player);
                finish("PASS");
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish("FAIL");
        }
    }

    private void setup(EntityPlayerMP player) {
        boolean whiteAlternative = addNative("white", "craftingLensWhite", Materials.Dilithium,
            ItemList.Circuit_Wafer_CPU.get(1));
        boolean yellowAlternative = addNative("yellow", "craftingLensYellow", Materials.GarnetYellow,
            ItemList.Circuit_Wafer_LPIC.get(1));
        require(whiteAlternative || yellowAlternative, "native regression includes a non-representative lens");
        ItemStack consumed = new ItemStack(Items.quartz).setStackDisplayName("Circuit lens QA consumed output");
        require(
            !GTRecipeBuilder.builder()
                .itemInputs(new ItemStack(Items.apple), new OreDictItemStack("craftingLensWhite", 3))
                .itemOutputs(consumed.copy()).duration(20).eut(8).addTo(RecipeMaps.assemblerRecipes).isEmpty(),
            "positive-quantity native oredict lens fixture registered");
        cases.add(new Case("consumed oredict lens", new ItemStack(Items.apple),
            GTOreDictUnificator.get(OrePrefixes.lens, Materials.Dilithium, 3), consumed, true));
        for (int i = 0; i < cases.size(); i++) {
            Case test = cases.get(i);
            int x = i * 6;
            World world = player.worldObj;
            world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
            BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
            tile.setInitialValuesAsNBT(null, (short) GTNGItemList.CircuitMEPatternBuffer.get(1).getItemDamage());
            tile.setOwnerName(player.getCommandSenderName());
            tile.setOwnerUuid(player.getUniqueID());
            tile.enableWorking();
            test.hatch = (CircuitMEPatternBuffer) tile.getMetaTileEntity();
            test.hatch.setConnectsToAllSides(true);
            var definitions = AEApi.instance().definitions();
            world.setBlock(x, 10, 1, definitions.blocks().multiPart().maybeBlock().get());
            ((IPartHost) world.getTileEntity(x, 10, 1)).addPart(
                definitions.parts().cableGlass().stack(AEColor.Transparent, 1), ForgeDirection.UNKNOWN, player);
            world.setBlock(x + 1, 10, 1, definitions.blocks().energyCellCreative().maybeBlock().get());
            world.setBlock(x - 1, 10, 1, definitions.blocks().drive().maybeBlock().get());
            TileDrive drive = (TileDrive) world.getTileEntity(x - 1, 10, 1);
            drive.getInternalInventory().setInventorySlotContents(0, definitions.items().cell64k().maybeStack(1).get());
            test.pattern = pattern(test);
            test.hatch.setInventorySlotContents(0, test.pattern.copy());
        }
        setupProviders(player);
        player.playerNetServerHandler.setPlayerLocation(2, 12, -2, 0, 0);
    }

    private boolean addNative(String label, String ore, Materials material, ItemStack output) {
        GTRecipe recipe = RecipeMaps.laserEngraverRecipes.getAllRecipes().stream()
            .filter(candidate -> candidate instanceof GTRecipe_WithAlt && candidate.mFluidInputs.length == 0
                && candidate.mOutputs.length == 1
                && GTUtility.areStacksEqual(candidate.mOutputs[0], output, true)
                && candidate.mInputs.length == 2
                && GTUtility.areStacksEqual(candidate.mInputs[0], ItemList.Circuit_Silicon_Wafer.get(1), true))
            .findFirst().orElseThrow(() -> new AssertionError("native " + label + " wafer recipe missing"));
        GTRecipe_WithAlt alternatives = (GTRecipe_WithAlt) recipe;
        int lensSlot = -1;
        for (int i = 0; i < recipe.mInputs.length; i++) {
            if (alternatives.mOreDictIds != null && alternatives.mOreDictIds[i] == OreDictionary.getOreID(ore))
                lensSlot = i;
        }
        require(lensSlot >= 0 && recipe.mInputs[lensSlot].stackSize == 0,
            "native " + label + " wafer recipe uses a zero-quantity oredict lens");
        ItemStack requested = GTOreDictUnificator.get(OrePrefixes.lens, material, 1);
        ItemStack representative = recipe.mInputs[lensSlot].copy();
        representative.stackSize = 1;
        require(VirtualMachineMolds.indexOf(requested) >= 0 && VirtualMachineMolds.indexOf(representative) >= 0,
            label + " requested and representative lenses are in the virtual catalog");
        ItemStack input = recipe.mInputs[1 - lensSlot].copy();
        require(recipe.isRecipeInputEqual(false, new FluidStack[0], input, requested),
            label + " requested lens is accepted by the native recipe");
        cases.add(new Case(label + " " + material.mName, input, requested, recipe.mOutputs[0], false));
        cases.add(new Case(label + " representative", input, representative, recipe.mOutputs[0], false));
        return !GTUtility.areStacksEqual(requested, representative, true);
    }

    private void setupProviders(EntityPlayerMP player) {
        Case cpu = cases.get(0);
        providers.add(new ProviderCase("ordinary lens interface", cpu.input, cpu.lens, cpu.output, null, -1));
        ItemStack output = new ItemStack(Items.feather).setStackDisplayName("Circuit lens QA fluid output");
        FluidStack water = new FluidStack(FluidRegistry.WATER, 37);
        require(
            !GTRecipeBuilder.builder()
                .itemInputs(new ItemStack(Items.apple), new OreDictItemStack("craftingLensWhite", 0)).circuit(17)
                .fluidInputs(water.copy()).itemOutputs(output.copy()).duration(100).eut(8)
                .addTo(RecipeMaps.laserEngraverRecipes).isEmpty(),
            "native fluid and alternative lens fixture registered");
        providers
            .add(new ProviderCase("fluid lens interface", new ItemStack(Items.apple), cpu.lens, output, water, 17));
        World world = player.worldObj;
        var definitions = AEApi.instance().definitions();
        for (int i = 0; i < providers.size(); i++) {
            ProviderCase test = providers.get(i);
            int x = 36 + i * 6;
            world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
            test.tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
            test.tile.setInitialValuesAsNBT(null, (short) ItemList.Machine_MV_LaserEngraver.get(1).getItemDamage());
            test.tile.setOwnerName(player.getCommandSenderName());
            test.tile.setOwnerUuid(player.getUniqueID());
            test.tile.enableWorking();
            require(AutomaticMachineCircuit.supports(test.tile), test.label + " supports automatic reception");
            world.setBlock(x, 10, 1,
                test.fluid == null ? definitions.blocks().iface().maybeBlock().get() : ItemAndBlockHolder.INTERFACE);
            test.host = (TileInterface) world.getTileEntity(x, 10, 1);
            test.host.setOrientation(ForgeDirection.UP, ForgeDirection.SOUTH);
            world.setBlock(x + 1, 10, 1, definitions.blocks().energyCellCreative().maybeBlock().get());
            world.setBlock(x - 1, 10, 1, definitions.blocks().drive().maybeBlock().get());
            TileDrive drive = (TileDrive) world.getTileEntity(x - 1, 10, 1);
            drive.getInternalInventory().setInventorySlotContents(0, definitions.items().cell64k().maybeStack(1).get());
            if (test.fluid != null)
                drive.getInternalInventory().setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
            List<IAEStack<?>> inputs = new ArrayList<>();
            inputs.add(AEItemStack.create(test.input.copy()));
            inputs.add(AEItemStack.create(test.lens.copy()).setStackSize(1));
            if (test.circuit >= 0)
                inputs.add(AEItemStack.create(GTUtility.getIntegratedCircuit(test.circuit)).setStackSize(1));
            if (test.fluid != null) inputs.add(AEFluidStack.create(test.fluid.copy()));
            test.pattern = pattern(test.output, inputs.toArray(new IAEStack<?>[0]));
            test.host.getInterfaceDuality().getPatterns().setInventorySlotContents(0, test.pattern.copy());
        }
    }

    private void checkSorter(World world) {
        ItemStack[] inventory = new ItemStack[36];
        inventory[0] = providers.get(0).pattern.copy();
        inventory[1] = providers.get(1).pattern.copy();
        inventory[2] = pattern(providers.get(0).output, AEItemStack.create(providers.get(0).input.copy()),
            AEItemStack.create(cases.get(2).lens.copy()));
        inventory[3] = pattern(new ItemStack(Blocks.planks, 4), AEItemStack.create(new ItemStack(Blocks.log)));
        inventory[3].getTagCompound().setBoolean("crafting", true);
        var entries = PatternSorter.classify(inventory, world, RecipeMaps.laserEngraverRecipes);
        require(entries.size() == 4, "sorter classifies all processing and workbench patterns");
        for (PatternSorter.Entry entry : entries) {
            if (
                entry.slot < 2
            ) require(
                entry.status == PatternSorter.MATCHED
                    && entry.mold == VirtualMachineMolds.indexOf(providers.get(entry.slot).lens)
                    && entry.circuit == providers.get(entry.slot).circuit,
                "sorter recognizes retained alternative lens and circuit " + entry.slot);
            else require(entry.status == (entry.slot == 2 ? PatternSorter.UNMATCHED : PatternSorter.CRAFTING),
                "sorter preserves wrong-color and workbench classifications " + entry.slot);
            require(ItemStack.areItemStacksEqual(inventory[entry.slot], entry.stack),
                "sorter preserves the physical pattern " + entry.slot);
        }
        ICraftingPatternDetails wrong = CircuitPatternCodec.decode(inventory[2], world);
        ICraftingPatternDetails unchanged = AutomaticMachineCircuit.interfacePattern(wrong, providers.get(0).host);
        require(!AutomaticMachineCircuit.hasVirtualItems(unchanged) && unchanged.getCondensedAEInputs().length == 2,
            "wrong-color lens remains a physical AE requirement");
        ICraftingPatternDetails consumed = CircuitPatternCodec.runtime(cases.get(4).pattern, world);
        require(!AutomaticMachineCircuit.hasVirtualItems(consumed) && consumed.getCondensedAEInputs().length == 2,
            "positive-quantity lens remains a physical AE requirement");
    }

    private void inspect(EntityPlayerMP player) throws Exception {
        for (Case test : cases) {
            require(test.hatch.isActive(), test.label + " isolated AE network is active");
            test.details = test.hatch.inventories().next().getPatternDetails();
            require(test.details != null, test.label + " has indexed runtime pattern details");
            boolean lensInput = false;
            for (IAEStack<?> input : test.details.getCondensedAEInputs()) {
                if (input instanceof IAEItemStack item && VirtualMachineMolds.indexOf(item.getItemStack()) >= 0)
                    lensInput = true;
            }
            require(lensInput == test.consumed, test.label + " indexes only physically consumed lens requirements");
            require(
                ((CraftingGridCache) test.hatch.getProxy().getCrafting()).getMediums(test.details).contains(test.hatch),
                test.label + " is registered in the real AE crafting cache");
            require(ItemStack.areItemStacksEqual(test.pattern, test.hatch.getStackInSlot(0)),
                test.label + " indexing preserves the physical encoded pattern");
            ICraftingPatternDetails restored = CircuitPatternCodec.decode(test.details.getPattern().copy(),
                player.worldObj);
            require(test.details.equals(restored), test.label + " runtime identity survives native redecoding");
            var storage = test.hatch.getProxy().getStorage().getItemInventory();
            var source = test.hatch.getMEOutputActionSource();
            require(
                storage.injectItems(AEItemStack.create(test.input.copy()).setStackSize(64), Actionable.MODULATE, source)
                    == null,
                test.label + " consumed wafer or fixture material is stocked");
            require(
                storage.extractItems(AEItemStack.create(test.lens.copy()).setStackSize(1), Actionable.SIMULATE, source)
                    == null,
                test.label + " network has no physical lens");
        }
        for (ProviderCase test : providers) {
            DualityInterface provider = test.host.getInterfaceDuality();
            require(provider.getProxy().isActive(), test.label + " isolated provider network is active");
            provider.updateCraftingList();
            require(provider.craftingList != null && provider.craftingList.size() == 1,
                test.label + " publishes its processing pattern");
            test.details = provider.craftingList.iterator().next();
            require(test.details.getCondensedAEInputs().length == (test.fluid == null ? 1 : 2),
                test.label + " publishes only consumed items and native fluids");
            for (IAEStack<?> input : test.details.getAEInputs()) require(
                !CircuitPatternCodec.isCircuit(input)
                    && !CircuitPatternCodec.isMold(input, new ItemStack[] { test.lens }),
                test.label + " removes the encoded alternative lens and circuit from AE demands");
            require(ItemStack.areItemStacksEqual(test.pattern, provider.getPatterns().getStackInSlot(0)),
                test.label + " retains the physical encoded tools");
            var storage = provider.getProxy().getStorage();
            require(storage.getItemInventory().injectItems(AEItemStack.create(test.input.copy()).setStackSize(64),
                Actionable.MODULATE, provider.getActionSource()) == null, test.label + " material is stocked");
            if (test.fluid != null)
                require(
                    storage.getFluidInventory().injectItems(AEFluidStack.create(test.fluid.copy()).setStackSize(148),
                        Actionable.MODULATE, provider.getActionSource()) == null,
                    test.label + " native water is stocked");
            require(storage.getItemInventory().extractItems(AEItemStack.create(test.lens.copy()), Actionable.SIMULATE,
                provider.getActionSource()) == null, test.label + " has no physical lens");
        }
        checkSorter(player.worldObj);
    }

    private void beginPlans(EntityPlayerMP player) throws Exception {
        for (Case test : cases) test.plan = test.hatch.getProxy().getCrafting().beginCraftingJob(player.worldObj,
            test.hatch.getProxy().getGrid(), test.hatch.getMEOutputActionSource(),
            AEItemStack.create(test.output.copy()).setStackSize(4), null);
        for (ProviderCase test : providers) {
            DualityInterface provider = test.host.getInterfaceDuality();
            test.plan = provider.getProxy().getCrafting().beginCraftingJob(player.worldObj,
                provider.getProxy().getGrid(), provider.getActionSource(),
                AEItemStack.create(test.output.copy()).setStackSize(4), null);
        }
    }

    private void checkPlans() throws Exception {
        for (Case test : cases) {
            ICraftingJob job = test.plan.get();
            require(job.isSimulation() == test.consumed, test.label + " real AE plan needs only consumed materials");
            var plan = AEApi.instance().storage().createAEStackList();
            job.populatePlan(plan);
            boolean lens = false;
            boolean material = false;
            for (IAEStack<?> stack : plan) {
                if (!(stack instanceof IAEItemStack item)) continue;
                if (VirtualMachineMolds.indexOf(item.getItemStack()) >= 0) {
                    require(test.consumed && GTUtility.areStacksEqual(item.getItemStack(), test.lens, true)
                        && item.getStackSize() >= 12, test.label + " consumed lens retains its exact batch demand");
                    lens = true;
                }
                if (GTUtility.areStacksEqual(item.getItemStack(), test.input, true) && item.getStackSize() >= 4)
                    material = true;
            }
            require(lens == test.consumed && material, test.label + " plan excludes reusable lenses and keeps wafers");
        }
    }

    private void checkProviders(EntityPlayerMP player) throws Exception {
        for (ProviderCase test : providers) {
            ICraftingJob job = test.plan.get();
            require(!job.isSimulation(), test.label + " real AE plan succeeds without physical tools");
            var plan = AEApi.instance().storage().createAEStackList();
            job.populatePlan(plan);
            boolean material = false, fluid = test.fluid == null;
            for (IAEStack<?> stack : plan) {
                require(!CircuitPatternCodec.isCircuit(stack), test.label + " plan omits circuits");
                if (stack instanceof IAEItemStack item) {
                    require(VirtualMachineMolds.indexOf(item.getItemStack()) < 0, test.label + " plan omits lenses");
                    if (GTUtility.areStacksEqual(item.getItemStack(), test.input, true)) material = true;
                } else if (
                    stack instanceof IAEFluidStack water && test.fluid != null
                        && water.getFluid() == test.fluid.getFluid()
                        && water.getStackSize() >= 148
                ) fluid = true;
            }
            require(material && fluid, test.label + " plan keeps exact consumed inputs");
            ICraftingPatternDetails restored = CircuitPatternCodec.decode(test.details.getPattern().copy(),
                player.worldObj);
            require(test.details.equals(restored) && test.details.hashCode() == restored.hashCode(),
                test.label + " runtime identity survives native redecoding");
            MEInventoryCrafting batch = test.fluid == null ? table(AEItemStack.create(test.input.copy()))
                : table(AEItemStack.create(test.input.copy()), AEFluidStack.create(test.fluid.copy()));
            require(test.host.getInterfaceDuality().pushPattern(restored, batch),
                test.label + " pushes a tool-free CPU batch");
            MTEBasicMachine machine = (MTEBasicMachine) test.tile.getMetaTileEntity();
            require(GTUtility.areStacksEqual(VirtualMachineMolds.get(machine), test.lens, false),
                test.label + " selects the encoded lens rather than the recipe representative");
            if (test.circuit >= 0)
                require(machine.getStackInSlot(machine.getCircuitSlot()).getItemDamage() == test.circuit,
                    test.label + " preserves the encoded circuit");
            consume(test);
        }
    }

    private static void consume(ProviderCase test) throws Exception {
        MTEBasicMachine machine = (MTEBasicMachine) test.tile.getMetaTileEntity();
        Method getInputs = MTEBasicMachine.class.getDeclaredMethod("getAllInputs");
        getInputs.setAccessible(true);
        ItemStack[] items = (ItemStack[]) getInputs.invoke(machine);
        FluidStack fluid = machine.getFillableStack();
        FluidStack[] fluids = fluid == null ? new FluidStack[0] : new FluidStack[] { fluid };
        GTRecipe recipe = machine.getRecipeMap().findRecipeQuery().items(items).fluids(fluids)
            .voltage(GTValues.V[machine.mTier]).find();
        require(
            recipe != null && GTUtility.areStacksEqual(recipe.mOutputs[0], test.output, false)
                && recipe.isRecipeInputEqual(true, fluids, items),
            test.label + " native GT consumes the selected recipe");
        for (int i = 0; i < machine.mInputSlotCount; i++) {
            int slot = machine.getInputSlot() + i;
            ItemStack remaining = machine.getStackInSlot(slot);
            require(remaining == null || remaining.stackSize == 0, test.label + " consumes each material once");
            machine.setInventorySlotContents(slot, null);
        }
        require(fluid == null || fluid.amount == 0, test.label + " consumes the native fluid once");
        machine.setFillableStack(null);
        require(GTUtility.areStacksEqual(VirtualMachineMolds.get(machine), test.lens, false),
            test.label + " retains its virtual lens after consumption");
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

    private static ItemStack pattern(Case test) {
        return pattern(test.output, AEItemStack.create(test.input.copy()), AEItemStack.create(test.lens.copy()));
    }

    private static ItemStack pattern(ItemStack output, IAEStack<?>... encodedInputs) {
        boolean ultimate = false;
        for (IAEStack<?> input : encodedInputs) if (input instanceof IAEFluidStack) ultimate = true;
        ItemStack pattern = (ultimate ? AEApi.instance().definitions().items().encodedUltimatePattern()
            : AEApi.instance().definitions().items().encodedPattern()).maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList();
        for (IAEStack<?> input : encodedInputs) {
            NBTTagCompound entry = new NBTTagCompound();
            if (ultimate) input.writeToNBTGeneric(entry);
            else input.writeToNBT(entry);
            inputs.appendTag(entry);
        }
        NBTTagList outputs = new NBTTagList();
        NBTTagCompound entry = new NBTTagCompound();
        if (ultimate) AEItemStack.create(output.copy()).writeToNBTGeneric(entry);
        else AEItemStack.create(output.copy()).writeToNBT(entry);
        outputs.appendTag(entry);
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("CIRCUIT_LENS_PATTERN_QA: " + message);
    }

    private void finish(String result) {
        try {
            Files.write(new File("result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        System.out.println("CIRCUIT_LENS_PATTERN_QA: " + result);
        finished = true;
    }

    private static final class Case {

        private final String label;
        private final ItemStack input, lens, output;
        private final boolean consumed;
        private CircuitMEPatternBuffer hatch;
        private ItemStack pattern;
        private ICraftingPatternDetails details;
        private Future<ICraftingJob> plan;

        private Case(String label, ItemStack input, ItemStack lens, ItemStack output, boolean consumed) {
            this.label = label;
            this.input = input.copy();
            this.lens = lens.copy();
            this.output = output.copy();
            this.consumed = consumed;
        }
    }

    private static final class ProviderCase {

        private final String label;
        private final ItemStack input, lens, output;
        private final FluidStack fluid;
        private final int circuit;
        private BaseMetaTileEntity tile;
        private TileInterface host;
        private ItemStack pattern;
        private ICraftingPatternDetails details;
        private Future<ICraftingJob> plan;

        private ProviderCase(String label, ItemStack input, ItemStack lens, ItemStack output, FluidStack fluid,
            int circuit) {
            this.label = label;
            this.input = input.copy();
            this.lens = lens.copy();
            this.output = output.copy();
            this.fluid = fluid == null ? null : fluid.copy();
            this.circuit = circuit;
        }
    }
}
