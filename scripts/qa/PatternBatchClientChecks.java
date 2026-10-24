package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
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

import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.multiblock.LightHunterSpaceStation;
import com.xyp.gtnotgood.common.machines.multiblock.NexusSatelliteFactory;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGModularMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.parallelmachine.LargeFurnace;
import com.xyp.gtnotgood.common.machines.processing.ModuleWork;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.storage.data.IAEFluidStack;
import appeng.container.ContainerNull;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.VoidingMode;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IDualInputInventory;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

@Mod(
    modid = "patternbatchqa",
    name = "Pattern Batch QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class PatternBatchClientChecks {

    private static final Item A = new Item(), B = new Item(), OUT_A = new Item(), OUT_B = new Item();
    private final Set<RecipeMap<?>> seeded = Collections.newSetFromMap(new IdentityHashMap<>());
    private final StringBuilder report = new StringBuilder();
    private boolean started, checked;
    private volatile boolean finished;
    private World world;
    private EntityPlayerMP player;
    private int position;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.registerItem(A, "inputA");
        GameRegistry.registerItem(B, "inputB");
        GameRegistry.registerItem(OUT_A, "outputA");
        GameRegistry.registerItem(OUT_B, "outputB");
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
        mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
        if (finished) {
            FMLCommonHandler.instance().exitJava(0, false);
            return;
        }
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("pattern-batch-" + System.currentTimeMillis(), "Pattern Batch QA",
                new WorldSettings(879L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || checked) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        checked = true;
        File output = new File(System.getProperty("gtng.patternBatch.output"));
        output.mkdirs();
        try {
            player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            world = player.worldObj;
            ItemStack[] hatches = { ItemList.Hatch_CraftingInput_Bus_ME.get(1),
                GTNGItemList.SuperMTEHatchCraftingInputME.get(1),
                GTNGItemList.CompactSuperMTEHatchCraftingInputME.get(1), GTNGItemList.CircuitMEPatternBuffer.get(1) };
            for (ItemStack hatch : hatches) {
                furnace(hatch);
                for (GTNGItemList controller : new GTNGItemList[] { GTNGItemList.MageAssembler,
                    GTNGItemList.FieldExtruderFactory, GTNGItemList.SuperParticleCollider,
                    GTNGItemList.SuperconductingElectromagneticFactory, GTNGItemList.KerrNewmanHomogenizer,
                    GTNGItemList.DimensionalFocusEngravingArray, GTNGItemList.NyarlathotepsTentacle })
                    modular(hatch, controller);
                for (GTNGItemList controller : new GTNGItemList[] { GTNGItemList.NexusSatelliteFactoryMkI,
                    GTNGItemList.NexusSatelliteFactoryMkII, GTNGItemList.NexusSatelliteFactoryMkIII,
                    GTNGItemList.NexusSatelliteFactoryMkIV }) satellite(hatch, controller);
            }
            furnace(GTNGItemList.SuperMTEHatchCraftingInputBusME.get(1));
            report.insert(0,
                "PASS\nReal hatches and production controllers; controlled admission/ticks, not full structures.\n");
            Files.write(new File(output, "result.txt").toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("PATTERN_BATCH_QA PASS");
        } catch (Throwable error) {
            error.printStackTrace();
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output, "result.txt").toPath(),
                ("FAIL\n" + report + trace).getBytes(StandardCharsets.UTF_8));
        } finally {
            finished = true;
        }
    }

    private void furnace(ItemStack hatch) throws Exception {
        LargeFurnace machine = (LargeFurnace) place(GTNGItemList.LargeFurnace.get(1)).getMetaTileEntity();
        configure(machine);
        IDualInputHatch buffer = buffer(machine, hatch);
        seed(buffer, 0, new ItemStack(Blocks.iron_ore, 64), null, new ItemStack(Items.iron_ingot));
        seed(buffer, 1, new ItemStack(Blocks.gold_ore, 64), null, new ItemStack(Items.gold_ingot));
        blockOutputs(machine);
        require(!machine.checkProcessing().wasSuccessful() && itemCount(buffer) == 128,
            "furnace full output preserves both pattern jobs");
        clearOutputs(machine);
        require(machine.checkProcessing().wasSuccessful(), "furnace starts with " + hatch.getDisplayName());
        require(machine.getBatchRecipeCount() == 2 && machine.getBatchParallels() == 128 && itemCount(buffer) == 0,
            "furnace admits two real patterns in one 128-parallel batch");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        require(machine.getBatchRecipeCount() == 2 && machine.getBatchParallels() == 128,
            "furnace pending batch survives NBT");
        invoke(machine, "outputAfterRecipe");
        require(outputCount(machine, Items.iron_ingot) == 64 && outputCount(machine, Items.gold_ingot) == 64,
            "furnace exact outputs from both patterns");
        invoke(machine, "outputAfterRecipe");
        require(outputCount(machine, Items.iron_ingot) == 64, "furnace does not duplicate outputs");
        report.append("Furnace: ").append(hatch.getDisplayName()).append(" passed.\n");
    }

    private void modular(ItemStack hatch, GTNGItemList controller) throws Exception {
        GTNGModularMachineBase<?> machine = (GTNGModularMachineBase<?>) place(controller.get(1)).getMetaTileEntity();
        configure(machine);
        recipes(machine.getRecipeMap());
        BaseMetaTileEntity core = place(GTNGItemList.ExecutionCore.get(1));
        core.enableWorking();
        require(machine.addModularHatchToMachineList(core, 0), "accept execution core");
        IDualInputHatch buffer = buffer(machine, hatch);
        seed(buffer, 0, new ItemStack(A, 64), new FluidStack(FluidRegistry.WATER, 6400), new ItemStack(OUT_A));
        seed(buffer, 1, new ItemStack(B, 64), new FluidStack(FluidRegistry.LAVA, 6400), new ItemStack(OUT_B));
        machine.getBaseMetaTileEntity().enableWorking();
        invoke(machine, "runMachine", machine.getBaseMetaTileEntity(), 1000L);
        List<ModuleWork> works = machine.getModuleWorks();
        require(works.size() == 2, controller + " has two independent tasks: " + works.size());
        require(workOutput(works.get(0)) != workOutput(works.get(1)),
            controller + " assigns distinct patterns to cores");
        require(itemCount(buffer) == 126 && fluidCount(buffer) == 12600, "each core consumes one exact item/fluid job");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        require(machine.getModuleWorks().size() == 2 && itemCount(buffer) == 126,
            "modular NBT does not reconsume jobs");
        machine.getBaseMetaTileEntity().disableWorking();
        report.append(controller).append(": ").append(hatch.getDisplayName()).append(" passed.\n");
    }

    private void satellite(ItemStack hatch, GTNGItemList controller) throws Exception {
        LightHunterSpaceStation station = (LightHunterSpaceStation) place(GTNGItemList.LightHunterSpaceStation.get(1))
            .getMetaTileEntity();
        station.mMachine = true;
        station.mMaxProgresstime = 1000;
        station.getBaseMetaTileEntity().setActive(true);
        int[] pos = station.getModulePositions().get(0);
        BaseMetaTileEntity base = place(controller.get(1), pos[0], pos[1], pos[2]);
        base.setFrontFacing(ForgeDirection.EAST);
        NexusSatelliteFactory machine = (NexusSatelliteFactory) base.getMetaTileEntity();
        configure(machine);
        field(machine.getClass(), "host").set(machine, station);
        require(machine.getHost() == station && station.isStationWorking(), "real satellite host admission");
        recipes(machine.getRecipeMap());
        IDualInputHatch buffer = buffer(machine, hatch);
        seed(buffer, 0, new ItemStack(A, 3), new FluidStack(FluidRegistry.WATER, 300), new ItemStack(OUT_A));
        seed(buffer, 1, new ItemStack(B, 5), new FluidStack(FluidRegistry.LAVA, 500), new ItemStack(OUT_B));
        blockOutputs(machine);
        require(!machine.checkProcessing().wasSuccessful() && itemCount(buffer) == 8 && fluidCount(buffer) == 800,
            "satellite output protection preserves all patterns");
        clearOutputs(machine);
        require(machine.checkProcessing().wasSuccessful(), "satellite starts " + controller);
        require(field(NexusSatelliteFactory.class, "batchRecipeCount").getInt(machine) == 2
            && field(NexusSatelliteFactory.class, "batchParallels").getLong(machine) == 8
            && itemCount(buffer) == 0
            && fluidCount(buffer) == 0, "satellite processes both native fluid patterns");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        invoke(machine, "outputAfterRecipe");
        require(outputCount(machine, OUT_A) == 3 && outputCount(machine, OUT_B) == 5,
            "exact satellite outputs after NBT");
        clearOutputs(machine);
        var longRecipe = machine.getRecipeMap().findRecipeQuery().items(new ItemStack(A), new ItemStack(Items.stick))
            .fluids(new FluidStack(FluidRegistry.WATER, 100)).findAll().findFirst().orElseThrow();
        int originalDuration = longRecipe.mDuration;
        try {
            longRecipe.mDuration = 20000;
            seed(buffer, 0, new ItemStack(A, 64), new FluidStack(FluidRegistry.WATER, 6400), new ItemStack(OUT_A));
            seed(buffer, 1, new ItemStack(B, 4), new FluidStack(FluidRegistry.LAVA, 400), new ItemStack(OUT_B));
            require(machine.checkProcessing().wasSuccessful() && itemCount(buffer) == 4,
                "first long pattern reaches the existing per-batch work bound");
            invoke(machine, "outputAfterRecipe");
            seed(buffer, 0, new ItemStack(A, 64), new FluidStack(FluidRegistry.WATER, 6400), new ItemStack(OUT_A));
            require(machine.checkProcessing().wasSuccessful() && itemCount(buffer) == 0,
                "refilling the first long pattern does not starve the second");
            invoke(machine, "outputAfterRecipe");
            require(outputCount(machine, OUT_A) == 128 && outputCount(machine, OUT_B) == 4,
                "long-pattern rotation produces both exact outputs");
        } finally {
            longRecipe.mDuration = originalDuration;
        }
        station.mMachine = false;
        report.append(controller).append(": ").append(hatch.getDisplayName()).append(" passed.\n");
    }

    private void configure(MTEMultiBlockBase machine) {
        machine.mMachine = true;
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = machine.mSolderingTool = machine.mCrowbar = true;
        machine.addEnergyInputToMachineList(place(ItemList.Hatch_Energy_LV.get(1)), 0);
        machine.addOutputBusToMachineList(place(ItemList.Hatch_Output_Bus_UV.get(1)), 0);
        machine.addOutputHatchToMachineList(place(ItemList.Hatch_Output_UV.get(1)), 0);
        machine.setVoidingMode(VoidingMode.VOID_NONE);
        machine.setAlwaysMaxParallel(true);
    }

    private IDualInputHatch buffer(MTEMultiBlockBase machine, ItemStack item) {
        BaseMetaTileEntity base = place(item);
        require(machine.addInputBusToMachineList(base, 0), "native hatch adder accepts " + item.getDisplayName());
        IDualInputHatch hatch = (IDualInputHatch) base.getMetaTileEntity();
        require(machine.mDualInputHatches.contains(hatch), "input is registered as a dual input hatch");
        return hatch;
    }

    private void recipes(RecipeMap<?> map) {
        if (!seeded.add(map)) return;
        for (int i = 0; i < 2; i++)
            GTRecipeBuilder.builder().itemInputs(new ItemStack(i == 0 ? A : B), new ItemStack(Items.stick, 0))
                .fluidInputs(new FluidStack(i == 0 ? FluidRegistry.WATER : FluidRegistry.LAVA, 100))
                .itemOutputs(new ItemStack(i == 0 ? OUT_A : OUT_B)).duration(i == 0 ? 200 : 400).eut(8).addTo(map);
    }

    private void seed(IDualInputHatch hatch, int slot, ItemStack items, FluidStack fluids, ItemStack output)
        throws Exception {
        ItemStack encoded = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList(), outputs = new NBTTagList();
        NBTTagCompound item = new NBTTagCompound();
        ItemStack unit = items.copy();
        unit.stackSize = 1;
        Platform.writeStackNBT(AEItemStack.create(unit), item);
        inputs.appendTag(item);
        if (fluids != null) {
            NBTTagCompound tool = new NBTTagCompound();
            Platform.writeStackNBT(AEItemStack.create(new ItemStack(Items.stick)), tool);
            inputs.appendTag(tool);
            NBTTagCompound fluid = new NBTTagCompound();
            FluidStack one = fluids.copy();
            one.amount = 100;
            Platform.writeStackNBT(AEFluidStack.create(one), fluid);
            inputs.appendTag(fluid);
        }
        while (inputs.tagCount() < 16) inputs.appendTag(new NBTTagCompound());
        NBTTagCompound out = new NBTTagCompound();
        Platform.writeStackNBT(AEItemStack.create(output), out);
        outputs.appendTag(out);
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        encoded.setTagCompound(tag);
        encoded = ItemStack.loadItemStackFromNBT(encoded.writeToNBT(new NBTTagCompound()));
        var details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, world);
        require(details != null, "native Ultimate Encoded Pattern loads");
        hatch.setInventorySlotContents(slot, encoded);
        List<IDualInputInventory> inventories = new ArrayList<>();
        hatch.inventories().forEachRemaining(inventories::add);
        MEInventoryCrafting delivered = new MEInventoryCrafting(new ContainerNull(), 4, 4);
        delivered.setInventorySlotContents(0, AEItemStack.create(items));
        if (fluids != null) {
            delivered.setInventorySlotContents(1, AEFluidStack.create(fluids));
            delivered.setInventorySlotContents(2, AEItemStack.create(new ItemStack(Items.stick)));
            require(delivered.getAEStackInSlot(1) instanceof IAEFluidStack, "delivery uses native fluid stack");
        }
        IDualInputInventory inventory = inventories.get(slot);
        boolean accepted = inventory instanceof MTEHatchCraftingInputME.PatternSlot<?>nativeSlot
            ? nativeSlot.insertItemsAndFluids(delivered)
            : ((SuperMTEHatchCraftingInputME.PatternSlot<?>) inventory).insertItemsAndFluids(delivered);
        require(accepted, "pattern accepts delivered inputs");
    }

    private static long itemCount(IDualInputHatch hatch) {
        long amount = 0;
        for (var it = hatch.inventories(); it.hasNext();) for (ItemStack item : it.next().getItemInputs()) if (
            item != null && (item.getItem() == A || item.getItem() == B
                || item.getItem() == Item.getItemFromBlock(Blocks.iron_ore)
                || item.getItem() == Item.getItemFromBlock(Blocks.gold_ore))
        ) amount += item.stackSize;
        return amount;
    }

    private static long fluidCount(IDualInputHatch hatch) {
        long amount = 0;
        for (var it = hatch.inventories(); it.hasNext();)
            for (FluidStack fluid : it.next().getFluidInputs()) if (fluid != null) amount += fluid.amount;
        return amount;
    }

    private static Item workOutput(ModuleWork work) {
        return ItemStack.loadItemStackFromNBT(
            work.outputs.save().getTagList("items", 10).getCompoundTagAt(0).getCompoundTag("stack")).getItem();
    }

    private static void blockOutputs(MTEMultiBlockBase machine) {
        for (var bus : machine.mOutputBusses) for (int i = 0; i < bus.getSizeInventory(); i++)
            bus.setInventorySlotContents(i, new ItemStack(Items.stick, 64));
        for (IDualInputHatch input : machine.mDualInputHatches)
            if (input instanceof SuperMTEHatchCraftingInputME hatch) {
                NBTTagCompound full = new NBTTagCompound(), entry = new NBTTagCompound();
                NBTTagList items = new NBTTagList();
                entry.setTag("stack", GTUtility.saveItem(new ItemStack(Items.stick)));
                entry.setLong("amount", Long.MAX_VALUE);
                items.appendTag(entry);
                full.setTag("patternMEItemOutput", items);
                hatch.getMEOutput().load(full);
            }
    }

    private static void clearOutputs(MTEMultiBlockBase machine) {
        for (var bus : machine.mOutputBusses)
            for (int i = 0; i < bus.getSizeInventory(); i++) bus.setInventorySlotContents(i, null);
        for (IDualInputHatch input : machine.mDualInputHatches)
            if (input instanceof SuperMTEHatchCraftingInputME hatch) hatch.getMEOutput().load(new NBTTagCompound());
    }

    private static long outputCount(MTEMultiBlockBase machine, Item item) {
        long amount = 0;
        for (var bus : machine.mOutputBusses) for (int i = 0; i < bus.getSizeInventory(); i++) {
            ItemStack stack = bus.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) amount += stack.stackSize;
        }
        for (IDualInputHatch input : machine.mDualInputHatches)
            if (input instanceof SuperMTEHatchCraftingInputME hatch) {
                NBTTagCompound tag = new NBTTagCompound();
                hatch.getMEOutput().save(tag);
                NBTTagList cache = tag.getTagList("patternMEItemOutput", 10);
                for (int i = 0; i < cache.tagCount(); i++) {
                    NBTTagCompound entry = cache.getCompoundTagAt(i);
                    ItemStack stack = GTUtility.loadItem(entry.getCompoundTag("stack"));
                    if (stack != null && stack.getItem() == item) amount += entry.getLong("amount");
                }
            }
        return amount;
    }

    private BaseMetaTileEntity place(ItemStack stack) {
        return place(stack, (position++ % 24) * 5, 100, (position / 24) * 5);
    }

    private BaseMetaTileEntity place(ItemStack stack, int x, int y, int z) {
        world.setBlockToAir(x, y, z);
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.setOwnerUuid(player.getUniqueID());
        tile.disableWorking();
        return tile;
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void invoke(Object target, String name, Object... args) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass())
            for (Method method : type.getDeclaredMethods())
                if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                    method.setAccessible(true);
                    method.invoke(target, args);
                    return;
                }
        throw new NoSuchMethodException(name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
