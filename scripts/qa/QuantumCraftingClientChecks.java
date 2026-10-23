package com.xyp.gtnotgood.common.compactae;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtnotgood.client.quantum.CraftingCycleIndex;
import com.xyp.gtnotgood.client.quantum.ICraftingTreeCycles;
import com.xyp.gtnotgood.client.quantum.QuantumCraftingClientConfig;
import com.xyp.gtnotgood.client.quantum.QuantumCraftingModeButton;
import com.xyp.gtnotgood.common.crafting.cycle.RecursivePatternFinder;
import com.xyp.gtnotgood.common.crafting.cycle.RecursivePatternFinder.Cycle;
import com.xyp.gtnotgood.common.crafting.quantum.ae2.IQuantumCraftAmountGui;
import com.xyp.gtnotgood.common.crafting.quantum.ae2.IQuantumCraftConfirm;
import com.xyp.gtnotgood.common.crafting.quantum.ae2.QuantumCraftingJob;
import com.xyp.gtnotgood.common.crafting.quantum.ae2.QuantumCraftingMode;
import com.xyp.gtnotgood.common.crafting.quantum.ae2.QuantumCraftingTree;
import com.xyp.gtnotgood.common.machines.multiblock.AssemblerMatrix;
import com.xyp.gtnotgood.common.machines.multiblock.QuantumComputer;
import com.xyp.gtnotgood.mixins.late.appliedenergistics.quantum.AccessorOptimizePattern;
import com.xyp.gtnotgood.utils.DireCraftingPatternDetails;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.parts.IPartHost;
import appeng.api.storage.ITerminalHost;
import appeng.api.util.AEColor;
import appeng.client.gui.implementations.GuiCraftAmount;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiCraftingTree;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.implementations.ContainerOptimizePatterns;
import appeng.core.sync.GuiBridge;
import appeng.me.cache.CraftingGridCache;
import appeng.tile.storage.TileDrive;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import appeng.util.item.AESharedNBT;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;

/** Exercises actual StructureLib formation, AE CPU dispatch, matrix persistence and both MUI2 screens. */
@Mod(
    modid = "quantumcraftingqa",
    name = "Compact AE QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class QuantumCraftingClientChecks {

    private boolean started;
    private boolean finished;
    private int ticks;
    private int stage;
    private volatile int screenshot;
    private int frames;
    private AssemblerMatrix matrix;
    private QuantumComputer computer;
    private Future<ICraftingJob> job;
    private Future<ICraftingJob> secondJob;
    private TileDrive drive;
    private int requestedSticks;
    private int networkResumeTick;
    private volatile boolean clickedQuantum;
    private volatile List<Cycle> cycleFixture;
    private boolean treeOpened;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        QuantumCraftingClientConfig.setQuantumCraftingMode(false);
        if (Boolean.getBoolean("gtng.quantumCrafting.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        ModularUIConfig.guiDebugMode = false;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("quantum-crafting-qa-" + System.currentTimeMillis(), "Compact AE QA",
                new WorldSettings(25L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        World world = player.worldObj;
        try {
            ticks++;
            if (ticks == 1) setup(world, player);
            if (ticks < 100 || screenshot != 0) return;
            if (ticks > 2400) throw new AssertionError("QA timeout at stage " + stage);
            if (stage == 0) {
                require(matrix.checkStructure(true, matrix.getBaseMetaTileEntity()),
                    "matrix forms without energy hatches");
                require(computer.checkStructure(true, computer.getBaseMetaTileEntity()),
                    "computer forms from five casings");
                require(
                    matrix.getPatterns().getSizeInventory() == AssemblerMatrix.PATTERN_CAPACITY && matrix.rows() == 2,
                    AssemblerMatrix.PATTERN_CAPACITY + " slots / 2 initial terminal rows");
                require(AEApi.instance().registries().interfaceTerminal().getSupportedClasses()
                    .contains(AssemblerMatrix.class), "matrix registered in interface terminal");
                matrix.setShowPattern(false);
                require(!terminalDisplays(matrix), "matrix hidden from interface terminal");
                matrix.setShowPattern(true);
                require(terminalDisplays(matrix), "matrix visible in interface terminal");
                require(matrix.getProxy().isActive() && computer.isActive(), "AE network connection");
                require(matrix.getProxy().getGrid().getMachines(AssemblerMatrix.class).iterator().hasNext(),
                    "interface terminal can enumerate matrix on AE grid");
                require(computer.getProxy().getCrafting().getCpus().size() == 1, "one virtual CPU visible");
                require(computer.virtualCPU.getCoProcessors() == Integer.MAX_VALUE, "maximum coprocessors");
                ItemStack encoded = pattern();
                var details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, world);
                require(details != null, "valid native crafting pattern");
                requestedSticks = (int) details.getCondensedOutputs()[0].getStackSize() * 32;
                System.out.println("QUANTUM_CRAFTING_QA: requested sticks=" + requestedSticks);
                matrix.getPatterns().setInventorySlotContents(AssemblerMatrix.PATTERN_CAPACITY - 1, encoded);
                matrix.setMachineMode(AssemblerMatrix.MODE_OPERATING);
                matrix.getBaseMetaTileEntity().enableWorking();
                matrix.getProxy().getStorage().getItemInventory().injectItems(
                    AEItemStack.create(new ItemStack(Blocks.planks, 64)), Actionable.MODULATE,
                    new MachineSource(matrix));
                stage = 1;
                return;
            }
            if (stage == 1) {
                if (
                    matrix.getProxy().getCrafting()
                        .getCraftingFor(AEItemStack.create(new ItemStack(Items.stick)), null, 0, world).isEmpty()
                ) return;
                job = matrix.getProxy().getCrafting().beginCraftingJob(world, matrix.getProxy().getGrid(),
                    new MachineSource(matrix), AEItemStack.create(new ItemStack(Items.stick, requestedSticks)), null);
                stage = 2;
            } else if (stage == 2 && job.isDone()) {
                require(!job.get().isSimulation(), "32-craft job has all ingredients");
                var link = matrix.getProxy().getCrafting().submitJob(job.get(), null, null, false,
                    new MachineSource(matrix));
                require(link != null, "quantum CPU accepts order");
                require(computer.cpus.size() == 1 && computer.virtualCPU != null, "CPU splits dynamically");
                // No native AE energy extraction is permitted during the actual dispatch below.
                IEnergyGrid noPower = (IEnergyGrid) Proxy.newProxyInstance(IEnergyGrid.class.getClassLoader(),
                    new Class<?>[] { IEnergyGrid.class },
                    (proxy, method, args) -> {
                        throw new AssertionError("Unexpected CPU energy access: " + method.getName());
                    });
                computer.cpus.get(0).updateCraftingLogic(matrix.getProxy().getGrid(), noPower,
                    (CraftingGridCache) matrix.getProxy().getCrafting());
                require((matrix.mMaxProgresstime == 1 && matrix.lEUt == 0) || matrix.checkProcessing().wasSuccessful(),
                    "matrix starts a free batch");
                require(matrix.mMaxProgresstime == 1 && matrix.lEUt == 0, "1 tick, zero EU");
                matrix.stopMachine(ShutDownReasonRegistry.NONE);
                NBTTagCompound saved = new NBTTagCompound();
                matrix.getBaseMetaTileEntity().writeToNBT(saved);
                world.removeTileEntity(0, 10, 0);
                BaseMetaTileEntity restored = new BaseMetaTileEntity();
                restored.setWorldObj(world);
                restored.readFromNBT(saved);
                world.setTileEntity(0, 10, 0, restored);
                matrix = (AssemblerMatrix) restored.getMetaTileEntity();
                require(matrix.getPatterns().getStackInSlot(AssemblerMatrix.PATTERN_CAPACITY - 1) != null,
                    "last pattern slot survives NBT");
                matrix.getBaseMetaTileEntity().enableWorking();
                stage = 3;
            } else if (stage == 3) {
                if (!matrix.getProxy().isActive()) return;
                // Reloaded patterns must return without touching the cable or manually refreshing the matrix.
                if (
                    matrix.getProxy().getCrafting()
                        .getCraftingFor(AEItemStack.create(new ItemStack(Items.stick)), null, 0, world).isEmpty()
                ) return;
                var count = matrix.getProxy().getStorage().getItemInventory().extractItems(
                    AEItemStack.create(new ItemStack(Items.stick, 256)), Actionable.SIMULATE,
                    new MachineSource(matrix));
                if (count == null || count.getStackSize() < requestedSticks) return;
                require(count.getStackSize() == requestedSticks,
                    "exact batch output after interrupted cycle and NBT reload");
                var planks = matrix.getProxy().getStorage().getItemInventory().extractItems(
                    AEItemStack.create(new ItemStack(Blocks.planks, 64)), Actionable.SIMULATE,
                    new MachineSource(matrix));
                require(planks == null, "exact input consumption");
                require(matrix.lEUt == 0, "no recipe power draw");
                stage = 6;
            } else if (stage == 6) {
                if (ticks < 400) return;
                QuantumCraftingMode.setEnabled(player, true);
                require(QuantumCraftingMode.shouldUse(matrix.getProxy().getGrid(), new PlayerSource(player, matrix)),
                    "quantum mode available on connected computer");
                matrix.getProxy().getStorage().getItemInventory().injectItems(
                    AEItemStack.create(new ItemStack(Blocks.planks, 128)), Actionable.MODULATE,
                    new MachineSource(matrix));
                job = matrix.getProxy().getCrafting().beginCraftingJob(world, matrix.getProxy().getGrid(),
                    new PlayerSource(player, matrix), AEItemStack.create(new ItemStack(Items.stick, requestedSticks)),
                    null);
                secondJob = matrix.getProxy().getCrafting().beginCraftingJob(world, matrix.getProxy().getGrid(),
                    new PlayerSource(player, matrix), AEItemStack.create(new ItemStack(Items.stick, requestedSticks)),
                    null);
                stage = 7;
            } else if (stage == 7 && job.isDone() && secondJob.isDone()) {
                require(job.get() instanceof QuantumCraftingJob && secondJob.get() instanceof QuantumCraftingJob,
                    "player mode selects quantum planner for both orders");
                require(!job.get().isSimulation(), "new order after idle reload has ingredients");
                require(!secondJob.get().isSimulation(), "second independent order has ingredients");
                int previousCpuCount = computer.cpus.size();
                var firstLink = matrix.getProxy().getCrafting().submitJob(job.get(), null, null, false,
                    new MachineSource(matrix));
                require(firstLink != null, "first independent order accepted");
                require(((CraftingGridCache) matrix.getProxy().getCrafting()).hasCpu(computer.virtualCPU),
                    "replacement CPU is visible before the next AE tick");
                var secondLink = matrix.getProxy().getCrafting().submitJob(secondJob.get(), null, null, false,
                    new MachineSource(matrix));
                require(secondLink != null, "second independent order accepted in the same tick");
                require(computer.cpus.size() == previousCpuCount + 2,
                    "independent orders receive separate accelerated CPUs");
                require(
                    computer.cpus.get(previousCpuCount).getCoProcessors() == Integer.MAX_VALUE
                        && computer.cpus.get(previousCpuCount + 1).getCoProcessors() == Integer.MAX_VALUE,
                    "both orders retain maximum coprocessors");
                for (var cpu : computer.cpus) {
                    cpu.updateCraftingLogic(matrix.getProxy().getGrid(), matrix.getProxy().getEnergy(),
                        (CraftingGridCache) matrix.getProxy().getCrafting());
                }
                // Interrupt AE after dispatch but before the matrix's next recipe check. No switch toggles or
                // manual checkProcessing calls are allowed when the network returns.
                matrix.getProxy().setValidSides(EnumSet.noneOf(ForgeDirection.class));
                networkResumeTick = ticks + 40;
                stage = 8;
            } else if (stage == 8) {
                if (ticks < networkResumeTick) return;
                require(!matrix.isActive() && matrix.isBusy(), "offline matrix rejects further dispatch");
                matrix.updateValidGridProxySides();
                stage = 9;
            } else if (stage == 9) {
                if (!matrix.isActive()) return;
                var count = matrix.getProxy().getStorage().getItemInventory().extractItems(
                    AEItemStack.create(new ItemStack(Items.stick, requestedSticks * 3)), Actionable.SIMULATE,
                    new MachineSource(matrix));
                if (count == null || count.getStackSize() < requestedSticks * 3) return;
                require(count.getStackSize() == requestedSticks * 3,
                    "accepted work resumes after AE interruption without controller toggle");
                player.playerNetServerHandler.setPlayerLocation(.5, 10, -1.5, 0, 15);
                matrix.onRightclick(matrix.getBaseMetaTileEntity(), player);
                screenshot = 1;
                stage = 4;
            } else if (stage == 4) {
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(4.5, 10, -1.5, 0, 15);
                computer.onRightclick(computer.getBaseMetaTileEntity(), player);
                screenshot = 2;
                stage = 5;
            } else if (stage == 5) {
                verifyPatternHashSurvivesAECacheEviction(world);
                QuantumCraftingMode.setEnabled(player, false);
                player.closeScreen();
                Platform.openGUI(player, world.getTileEntity(2, 10, 1), ForgeDirection.UP,
                    GuiBridge.GUI_CRAFTING_AMOUNT);
                ((ContainerCraftAmount) player.openContainer)
                    .setItemToCraft(AEItemStack.create(new ItemStack(Items.stick)));
                screenshot = 3;
                stage = 10;
            } else if (stage == 10) {
                if (!clickedQuantum || !QuantumCraftingMode.isEnabled(new PlayerSource(player, matrix))) return;
                require(true, "real GUI button synchronizes quantum mode to server");
                var crafting = (CraftingGridCache) matrix.getProxy().getCrafting();
                var processing = processingPattern(new ItemStack(Items.iron_ingot), new ItemStack(Items.gold_ingot));
                crafting.addCraftingOption(matrix,
                    ((ICraftingPatternItem) processing.getItem()).getPatternForItem(processing, world));
                crafting.setMockPatternsFromMethods();
                matrix.getProxy().getStorage().getItemInventory().injectItems(
                    AEItemStack.create(new ItemStack(Items.iron_ingot, 16)), Actionable.MODULATE,
                    new MachineSource(matrix));
                job = crafting.beginCraftingJob(world, matrix.getProxy().getGrid(), new PlayerSource(player, matrix),
                    AEItemStack.create(new ItemStack(Items.gold_ingot, 16)), null);
                stage = 11;
            } else if (stage == 11 && job.isDone()) {
                require(job.get() instanceof QuantumCraftingJob && !job.get().isSimulation(),
                    "processing quantum plan succeeds");
                var host = (ITerminalHost) ((IPartHost) world.getTileEntity(2, 10, 1)).getPart(ForgeDirection.UP);
                var optimizer = new ContainerOptimizePatterns(player.inventory, host);
                optimizer.setResult(job.get());
                var field = ContainerOptimizePatterns.class.getDeclaredField("patterns");
                field.setAccessible(true);
                var patterns = (Map<?, ?>) field.get(optimizer);
                require(patterns.size() == 1, "optimizer accepts quantum processing plan");
                var entry = (AccessorOptimizePattern) patterns.values().iterator().next();
                require(entry.gtng$getRequestedCrafts() == 16, "optimizer preserves requested craft count");
                require(!QuantumCraftingTree.createChunks(world, (QuantumCraftingJob<?>) job.get()).isEmpty(),
                    "quantum crafting tree serializes into native AE packets");
                Platform.openGUI(player, world.getTileEntity(2, 10, 1), ForgeDirection.UP,
                    GuiBridge.GUI_CRAFTING_CONFIRM);
                var confirm = (ContainerCraftConfirm) player.openContainer;
                confirm.setItemToCraft(AEItemStack.create(new ItemStack(Items.gold_ingot, 16)));
                confirm.setJob(job);
                screenshot = 4;
                stage = 12;
            } else if (stage == 12) {
                require(((IQuantumCraftConfirm) player.openContainer).gtng$isQuantumJob(),
                    "confirmation sync identifies quantum plan");
                player.closeScreen();
                job = matrix.getProxy().getCrafting().beginCraftingJob(world, matrix.getProxy().getGrid(),
                    new PlayerSource(player, matrix), AEItemStack.create(new ItemStack(Items.gold_ingot, 32)), null);
                stage = 13;
            } else if (stage == 13 && job.isDone()) {
                require(job.get().isSimulation(), "missing inputs produce simulation without taking inventory");
                var crafting = (CraftingGridCache) matrix.getProxy().getCrafting();
                var feedback = processingPattern(new ItemStack(Items.iron_ingot), new ItemStack(Items.iron_ingot, 2));
                crafting.addCraftingOption(matrix,
                    ((ICraftingPatternItem) feedback.getItem()).getPatternForItem(feedback, world));
                crafting.setMockPatternsFromMethods();
                var cycles = RecursivePatternFinder.find(crafting, AEItemStack.create(new ItemStack(Items.iron_ingot)),
                    world);
                cycleFixture = cycles;
                require(!cycles.isEmpty() && cycles.get(0).netOutput(), "recursive self-producing pattern is detected");
                job = crafting.beginCraftingJob(world, matrix.getProxy().getGrid(), new PlayerSource(player, matrix),
                    AEItemStack.create(new ItemStack(Items.iron_ingot, 64)), null);
                stage = 14;
            } else if (stage == 14 && job.isDone()) {
                require(!job.get().isSimulation(), "positive-output cycle plans from available seeds");
                Platform.openGUI(player, world.getTileEntity(2, 10, 1), ForgeDirection.UP,
                    GuiBridge.GUI_CRAFTING_CONFIRM);
                var confirm = (ContainerCraftConfirm) player.openContainer;
                confirm.setItemToCraft(AEItemStack.create(new ItemStack(Items.iron_ingot, 64)));
                confirm.setJob(job);
                screenshot = 5;
                stage = 15;
            } else if (stage == 15) {
                Files.write(new File("quantum-crafting-qa-result.txt").toPath(),
                    "PASS".getBytes(StandardCharsets.UTF_8));
                System.out.println("QUANTUM_CRAFTING_QA: PASS");
                finished = true;
                Minecraft.getMinecraft().shutdown();
            }
        } catch (Throwable failure) {
            finished = true;
            System.err.println("QUANTUM_CRAFTING_QA: FAIL stage=" + stage);
            failure.printStackTrace();
            Minecraft.getMinecraft().shutdown();
        }
    }

    private void setup(World world, EntityPlayerMP player) {
        matrix = (AssemblerMatrix) controller(world, player, 0, GTNGItemList.AssemblerMatrix.get(1))
            .getMetaTileEntity();
        computer = (QuantumComputer) controller(world, player, 4, GTNGItemList.QuantumComputer.get(1))
            .getMetaTileEntity();
        matrix.construct(new ItemStack(Items.stick), false);
        computer.construct(new ItemStack(Items.stick), false);
        var definitions = AEApi.instance().definitions();
        for (int x = 0; x <= 4; x++) {
            world.setBlock(x, 10, 1, definitions.blocks().multiPart().maybeBlock().get());
            ((IPartHost) world.getTileEntity(x, 10, 1)).addPart(
                definitions.parts().cableGlass().stack(AEColor.Transparent, 1), ForgeDirection.UNKNOWN, player);
        }
        world.setBlock(2, 10, 2, definitions.blocks().energyCellCreative().maybeBlock().get());
        world.setBlock(2, 10, 0, definitions.blocks().drive().maybeBlock().get());
        drive = (TileDrive) world.getTileEntity(2, 10, 0);
        drive.getInternalInventory().setInventorySlotContents(0, definitions.items().cell64k().maybeStack(1).get());
        ((IPartHost) world.getTileEntity(2, 10, 1)).addPart(definitions.parts().terminal().maybeStack(1).get(),
            ForgeDirection.UP, player);
        player.playerNetServerHandler.setPlayerLocation(2, 10, -4, 0, 15);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
    }

    private static BaseMetaTileEntity controller(World world, EntityPlayerMP player, int x, ItemStack stack) {
        world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private static ItemStack pattern() {
        ItemStack result = AEApi.instance().definitions().items().encodedPattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            NBTTagCompound entry = new NBTTagCompound();
            if (i == 0 || i == 3) new ItemStack(Blocks.planks).writeToNBT(entry);
            in.appendTag(entry);
        }
        NBTTagList out = new NBTTagList();
        NBTTagCompound output = new NBTTagCompound();
        new ItemStack(Items.stick, 4).writeToNBT(output);
        out.appendTag(output);
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", true);
        result.setTagCompound(tag);
        return result;
    }

    private static ItemStack processingPattern(ItemStack input, ItemStack output) {
        ItemStack result = AEApi.instance().definitions().items().encodedPattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList();
        in.appendTag(input.writeToNBT(new NBTTagCompound()));
        NBTTagList out = new NBTTagList();
        out.appendTag(output.writeToNBT(new NBTTagCompound()));
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        result.setTagCompound(tag);
        return result;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || screenshot == 0) return;
        if (++frames < 40) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (screenshot == 5) {
            if (!(mc.currentScreen instanceof GuiCraftConfirm)) return;
            try {
                if (!treeOpened) {
                    var buttonField = GuiCraftConfirm.class.getDeclaredField("switchDisplayMode");
                    buttonField.setAccessible(true);
                    var click = GuiCraftConfirm.class.getDeclaredMethod("actionPerformed", GuiButton.class);
                    click.setAccessible(true);
                    click.invoke(mc.currentScreen, buttonField.get(mc.currentScreen));
                    var treeField = GuiCraftConfirm.class.getDeclaredField("craftingTree");
                    treeField.setAccessible(true);
                    var tree = (GuiCraftingTree) treeField.get(mc.currentScreen);
                    var cycles = (ICraftingTreeCycles) tree;
                    var request = AEItemStack.create(new ItemStack(Items.iron_ingot, 64));
                    cycles.gtng$setCycles(CraftingCycleIndex.of(request, cycleFixture, Collections.emptyList()));
                    require(cycles.gtng$openCycle(cycleFixture.get(0), request),
                        "cycle opens as a ring in real crafting tree");
                    require(cycles.gtng$ringView() != null, "cycle ring view is present");
                    treeOpened = true;
                    frames = 0;
                    return;
                }
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        } else if (screenshot == 4) {
            if (!(mc.currentScreen instanceof GuiCraftConfirm)) return;
            require(((IQuantumCraftConfirm) mc.thePlayer.openContainer).gtng$isQuantumJob(),
                "client confirmation receives quantum state");
        } else if (screenshot == 3) {
            if (!(mc.currentScreen instanceof GuiCraftAmount)) return;
            try {
                var field = GuiScreen.class.getDeclaredField("buttonList");
                field.setAccessible(true);
                QuantumCraftingModeButton button = null;
                for (Object value : (List<?>) field.get(mc.currentScreen)) {
                    if (value instanceof QuantumCraftingModeButton mode) button = mode;
                }
                require(button != null && button.visible, "quantum mode button is visible for network");
                require(!button.getState(), "quantum mode defaults off");
                var click = GuiCraftAmount.class.getDeclaredMethod("actionPerformed", GuiButton.class);
                click.setAccessible(true);
                click.invoke(mc.currentScreen, button);
                require(button.getState() && QuantumCraftingClientConfig.isQuantumCraftingMode(),
                    "mode button enables preference");
                ((IQuantumCraftAmountGui) mc.currentScreen).gtng$setQuantumAvailable(false);
                require(!button.visible, "mode button hidden without a quantum CPU");
                ((IQuantumCraftAmountGui) mc.currentScreen).gtng$setQuantumAvailable(true);
                require(button.visible, "mode button returns with quantum CPU");
                clickedQuantum = true;
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        } else {
            require(mc.currentScreen instanceof GuiContainerWrapper, "MUI2 screen opens: " + mc.currentScreen);
        }
        ScreenShotHelper.saveScreenshot(new File("."), "quantum-crafting-" + screenshot + ".png", mc.displayWidth,
            mc.displayHeight, mc.getFramebuffer());
        frames = 0;
        screenshot = 0;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("QUANTUM_CRAFTING_QA: " + message);
    }

    /** Simulates AE's weak NBT cache eviction without depending on nondeterministic garbage collection. */
    private static void verifyPatternHashSurvivesAECacheEviction(World world) throws Exception {
        ItemStack encoded = pattern();
        encoded.getTagCompound().setString("gtngHashRegression", "isolated-fixture");
        var nativeDetails = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, world);
        var details = new DireCraftingPatternDetails(nativeDetails);
        var map = new HashMap<DireCraftingPatternDetails, String>();
        map.put(details, "registered matrix");
        int before = details.hashCode();
        int oldHash = 31 * AEItemStack.create(nativeDetails.getPattern()).hashCode() + 1;
        var cacheField = AESharedNBT.class.getDeclaredField("SHARED_TAG_COMPOUND");
        cacheField.setAccessible(true);
        synchronized (AESharedNBT.class) {
            ((Map<?, ?>) cacheField.get(null)).clear();
        }
        int after = details.hashCode();
        int oldHashAfterEviction = 31 * AEItemStack.create(nativeDetails.getPattern()).hashCode() + 1;
        require(oldHash != oldHashAfterEviction,
            "old hash formula reproduces cache-lifetime drift: " + oldHash + " -> " + oldHashAfterEviction);
        require(before == after, "pattern hash survives AE weak-cache eviction: " + before + " -> " + after);
        require("registered matrix".equals(map.get(details)), "existing task still finds its registered matrix");
        var equivalent = new DireCraftingPatternDetails(nativeDetails);
        require(details.equals(equivalent) && "registered matrix".equals(map.get(equivalent)),
            "equivalent reloaded pattern finds the same matrix");
    }

    /** Exercises the exact visibility predicate used by AE2's interface terminal after mixin application. */
    private static boolean terminalDisplays(AssemblerMatrix matrix) throws ReflectiveOperationException {
        Method visibility = ContainerInterfaceTerminal.class.getDeclaredMethod("getTerminalVisibility",
            appeng.api.util.IInterfaceViewable.class);
        visibility.setAccessible(true);
        return (Boolean) visibility.invoke(null, matrix);
    }
}
