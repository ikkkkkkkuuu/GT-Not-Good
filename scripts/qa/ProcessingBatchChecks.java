package com.xyp.gtnotgood.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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

import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.multiblock.QuantumComputer;
import com.xyp.gtnotgood.mixins.late.AppliedEnergistics.compact.AccessorTaskProgress;
import com.xyp.gtnotgood.utils.ECraftingCPUCluster;
import com.xyp.gtnotgood.utils.crafting.CraftingBatchPlanner.MediumStrategy;
import com.xyp.gtnotgood.utils.crafting.CraftingBatchPlannerImpl;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.parts.IPartHost;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.api.util.WorldCoord;
import appeng.crafting.MECraftingInventory;
import appeng.me.cache.CraftingGridCache;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/** Uses the transformed AE CPU and native Ultimate patterns in an isolated world. */
@Mod(
    modid = "processingbatchqa",
    name = "Processing batch QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class ProcessingBatchChecks {

    private boolean started, finished;
    private int ticks;
    private SuperMTEHatchCraftingInputME hatch;
    private QuantumComputer computer;
    private ItemStack encoded;
    private ICraftingPatternDetails details;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "batch-" + System.currentTimeMillis(),
                "Batch QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (++ticks == 1) setup(player.worldObj, player);
            if (ticks == 60)
                require(computer.checkStructure(true, computer.getBaseMetaTileEntity()), "quantum structure");
            if (ticks < 120) return;
            require(hatch.isActive() && computer.isActive(), "active network");
            runChecks();
            finish("PASS");
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish("FAIL");
        }
    }

    private void setup(World world, EntityPlayerMP player) {
        hatch = (SuperMTEHatchCraftingInputME) place(
            world,
            player,
            0,
            GTNGItemList.CompactSuperMTEHatchCraftingInputME.get(1)).getMetaTileEntity();
        hatch.setConnectsToAllSides(true);
        computer = (QuantumComputer) place(world, player, 4, GTNGItemList.QuantumComputer.get(1)).getMetaTileEntity();
        computer.construct(new ItemStack(Items.stick), false);
        var definitions = AEApi.instance()
            .definitions();
        for (int x = 0; x <= 4; x++) {
            world.setBlock(
                x,
                10,
                1,
                definitions.blocks()
                    .multiPart()
                    .maybeBlock()
                    .get());
            ((IPartHost) world.getTileEntity(x, 10, 1)).addPart(
                definitions.parts()
                    .cableGlass()
                    .stack(AEColor.Transparent, 1),
                ForgeDirection.UNKNOWN,
                player);
        }
        world.setBlock(
            2,
            10,
            2,
            definitions.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        encoded = pattern();
        details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, world);
        require(details != null, "native Ultimate pattern");
        hatch.getPatterns()
            .setInventorySlotContents(0, encoded);
    }

    private void runChecks() throws Exception {
        require(computer.checkStructure(true, computer.getBaseMetaTileEntity()), "quantum computer formed");
        var cache = (CraftingGridCache) hatch.getProxy()
            .getCrafting();
        require(
            cache.getMediums(details)
                .contains(hatch),
            "hatch registered for pattern");
        var inputs = details.getAEInputs();
        var planner = new CraftingBatchPlannerImpl();
        TestCpu cpu = new TestCpu(computer, details, 1_000_000_000L);
        var plan = planner.plan(
            1_000_000_000L,
            MediumStrategy.BATCH,
            Arrays.asList(inputs),
            details.getCondensedAEOutputs(),
            cpu.stock(),
            requested -> requested);
        require(plan.getCrafts() == 1_000_000_000L, "fluid planner accepts billion-craft batch");
        long startedAt = System.nanoTime();
        cpu.updateCraftingLogic(
            hatch.getProxy()
                .getGrid(),
            hatch.getProxy()
                .getEnergy(),
            cache);
        long elapsed = (System.nanoTime() - startedAt) / 1_000_000;
        require(
            cpu.left() == 0 && cpu.stock()
                .isEmpty(),
            "one dispatch consumes exactly one billion mixed inputs");
        require(
            cpu.awaited(details.getCondensedAEOutputs()[0]) == 1_000_000_000L,
            "fluid expected output count matches batch");
        require(elapsed < 3000, "billion-craft dispatch under 3 seconds: " + elapsed + " ms");
        var slot = hatch.inventories()
            .next();
        require(
            itemTotal(slot) == 1_000_000_000L && fluidTotal(slot) == 1_000_000_000L,
            "hatch receives exact mixed batch");

        // Rejection must return AE's scaled extraction without changing progress or expected outputs.
        hatch.getBaseMetaTileEntity()
            .disableWorking();
        TestCpu rejected = new TestCpu(computer, details, 1_000_000L);
        rejected.updateCraftingLogic(
            hatch.getProxy()
                .getGrid(),
            hatch.getProxy()
                .getEnergy(),
            cache);
        require(
            rejected.left() == 1_000_000L && rejected.awaited(details.getCondensedAEOutputs()[0]) == 0,
            "rejected batch preserves task and expected outputs");
        for (var input : inputs) require(
            rejected.stock()
                .getAvailableItem(input)
                .getStackSize() == 1_000_000L,
            "rejected input fully refunded");
        hatch.getBaseMetaTileEntity()
            .enableWorking();
        rejected.updateCraftingLogic(
            hatch.getProxy()
                .getGrid(),
            hatch.getProxy()
                .getEnergy(),
            cache);
        require(rejected.left() == 0, "rejected task resumes");

        TestCpu bounded = new TestCpu(computer, details, 3_000_000_000L);
        bounded.updateCraftingLogic(
            hatch.getProxy()
                .getGrid(),
            hatch.getProxy()
                .getEnergy(),
            cache);
        require(bounded.left() == 3_000_000_000L - Integer.MAX_VALUE, "physical input limit yields between batches");
        bounded.updateCraftingLogic(
            hatch.getProxy()
                .getGrid(),
            hatch.getProxy()
                .getEnergy(),
            cache);
        require(
            bounded.left() == 0 && bounded.awaited(details.getCondensedAEOutputs()[0]) == 3_000_000_000L,
            "remaining batch completes without overflow");
        long total = 4_001_000_000L;
        require(itemTotal(slot) == total && fluidTotal(slot) == total, "cross-int-boundary input conservation");
        var restored = new SuperMTEHatchCraftingInputME.PatternSlot<>(
            encoded,
            slot.writeToNBT(new NBTTagCompound()),
            hatch);
        require(itemTotal(restored) == total && fluidTotal(restored) == total, "batch inventory survives NBT reload");

        // Fill a partial earlier stack while a later matching stack still has space.
        var table = new MEInventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 2, 1);
        table.setInventorySlotContents(
            0,
            inputs[0].copy()
                .setStackSize(3_000_000_000L));
        table.setInventorySlotContents(
            1,
            inputs[1].copy()
                .setStackSize(3_000_000_000L));
        restored.getItemInputs()[0].stackSize = 10;
        restored.getFluidInputs()[0].amount = 10;
        long before = itemTotal(restored);
        restored.insertItemsAndFluids(table);
        require(
            itemTotal(restored) == before + 3_000_000_000L && fluidTotal(restored) == before + 3_000_000_000L,
            "partially consumed multi-stack merge preserves all inputs");
        require(
            table.getAEStackInSlot(0)
                .getStackSize() == 3_000_000_000L
                && table.getAEStackInSlot(1)
                    .getStackSize() == 3_000_000_000L,
            "insertion does not mutate AE extraction");

        ItemStack fuzzyPattern = encoded.copy();
        fuzzyPattern.getTagCompound()
            .setBoolean("substitute", true);
        ICraftingPatternDetails fuzzy = ((ICraftingPatternItem) fuzzyPattern.getItem()).getPatternForItem(
            fuzzyPattern,
            hatch.getBaseMetaTileEntity()
                .getWorld());
        hatch.getPatterns()
            .setInventorySlotContents(1, fuzzyPattern);
        hatch.gridChanged();
        // Rebuild the cache synchronously to exercise the newly registered fallback pattern.
        cache.addCraftingOption(hatch, fuzzy);
        TestCpu nativeCpu = new TestCpu(computer, fuzzy, 1_000_000L);
        nativeCpu.updateCraftingLogic(
            hatch.getProxy()
                .getGrid(),
            hatch.getProxy()
                .getEnergy(),
            cache);
        require(nativeCpu.left() == 1_000_000L - 1024, "substitution fallback obeys per-tick work budget");
    }

    private static final class TestCpu extends ECraftingCPUCluster {

        private final TaskProgress progress = new TaskProgress();

        private TestCpu(QuantumComputer owner, ICraftingPatternDetails pattern, long count) {
            super(new WorldCoord(0, 0, 0), new WorldCoord(0, 0, 0));
            setVirtualCPUOwner(owner);
            setAccelerators(Integer.MAX_VALUE);
            isComplete = false;
            inventory = new MECraftingInventory();
            ((AccessorTaskProgress) progress).setValue(count);
            tasks.put(pattern, progress);
            for (var input : pattern.getAEInputs()) inventory.injectItems(
                input.copy()
                    .setStackSize(count),
                Actionable.MODULATE);
        }

        private MECraftingInventory stock() {
            return inventory;
        }

        private long left() {
            return ((AccessorTaskProgress) progress).getValue();
        }

        private long awaited(IAEStack<?> output) {
            var stack = waitingFor.findPrecise(output);
            return stack == null ? 0 : stack.getStackSize();
        }
    }

    private static long itemTotal(SuperMTEHatchCraftingInputME.PatternSlot<?> slot) {
        return Arrays.stream(slot.getItemInputs())
            .mapToLong(s -> s.stackSize)
            .sum();
    }

    private static long fluidTotal(SuperMTEHatchCraftingInputME.PatternSlot<?> slot) {
        return Arrays.stream(slot.getFluidInputs())
            .mapToLong(s -> s.amount)
            .sum();
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x, ItemStack stack) {
        world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        var tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.enableWorking();
        return tile;
    }

    private static ItemStack pattern() {
        var result = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        var tag = new NBTTagCompound();
        var in = new NBTTagList();
        var out = new NBTTagList();
        for (IAEStack<?> input : new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.iron_ingot)),
            AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)) }) {
            var entry = new NBTTagCompound();
            input.writeToNBTGeneric(entry);
            in.appendTag(entry);
        }
        var output = new NBTTagCompound();
        AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1))
            .writeToNBTGeneric(output);
        out.appendTag(output);
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        tag.setBoolean("beSubstitute", true);
        result.setTagCompound(tag);
        return result;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        System.out.println("PROCESSING_BATCH_QA " + message);
    }

    private void finish(String result) {
        finished = true;
        try {
            Files.write(new File("result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        Minecraft.getMinecraft()
            .shutdown();
    }
}
