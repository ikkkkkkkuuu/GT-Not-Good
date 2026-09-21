package com.xyp.gtnotgood.common.packaged;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import ggfab.mte.MTEAdvAssLine;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.AssemblyLineUtils;
import gregtech.api.util.GTRecipe.RecipeAssemblyLine;
import gregtech.common.tileentities.machines.multi.MTEAssemblyLine;

/** Disposable manual test world with real structures, independent AE grids and test-only energy replenishment. */
@Mod(
    modid = "assemblyplayground",
    name = "Assembly Core Playground",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class AssemblyLinePlayground {

    private final List<java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingJob>> benchmarkJobs = new ArrayList<>();
    private final int[] peakQueued = new int[2];
    private final int[] idleGaps = new int[2];
    private final boolean[] seenWorking = new boolean[2];
    private final boolean[] submitted = new boolean[2];
    private boolean benchmarkDone;
    private boolean launched, built, failed;
    private boolean resumed;
    private volatile boolean ready;
    private int ticks, frames;
    private RecipeAssemblyLine recipe;
    private final List<BaseMetaTileEntity> energy = new ArrayList<>();
    private final List<MTEMultiBlockBase> lines = new ArrayList<>();
    private final List<TilePackagedProvider> providers = new ArrayList<>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.assembly.playground")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (Files.deleteIfExists(new File("assembly-playground-stop.request").toPath())) {
            mc.shutdown();
            return;
        }
        if (!launched && mc.theWorld == null && mc.currentScreen != null) {
            launched = true;
            mc.gameSettings.guiScale = 2;
            mc.gameSettings.pauseOnLostFocus = false;
            com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
            String resume = System.getProperty("gtng.assembly.resume", "");
            if (!resume.isEmpty() && !resume.matches("assembly-manual-[0-9]+"))
                throw new IllegalArgumentException("Invalid test save");
            resumed = !resume.isEmpty();
            String save = resumed ? resume : "assembly-manual-" + System.currentTimeMillis();
            Files.write(new File("assembly-playground-save.txt").toPath(), save.getBytes(StandardCharsets.UTF_8));
            mc.launchIntegratedServer(
                save,
                "GTNG Assembly Core Manual Test",
                resumed ? null : new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (ready && mc.theWorld != null && ++frames == 100) {
            ScreenShotHelper.saveScreenshot(
                mc.mcDataDir,
                "assembly-playground.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || failed) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var world = player.getServerForPlayer();
        try {
            ticks++;
            if (!built) {
                built = true;
                AssemblyLineClientChecks.checkPlanning();
                recipe = RecipeAssemblyLine.sAssemblylineRecipes.stream()
                    .filter(r -> TilePackagedProvider.sameItem(r.mOutput, ItemList.Electric_Motor_LuV.get(1)))
                    .filter(
                        r -> r.mInputs.length > 0 && r.mInputs.length <= 16
                            && r.mFluidInputs.length > 0
                            && r.mFluidInputs.length <= 4
                            && r.mEUt > 0
                            && r.mEUt <= 32768
                            && java.util.Arrays.stream(r.mInputs)
                                .allMatch(i -> i != null && i.stackSize > 0 && i.stackSize <= 64)
                            && java.util.Arrays.stream(r.mFluidInputs)
                                .allMatch(f -> f.amount > 0 && f.amount <= 64000))
                    .min(
                        Comparator.comparingInt((RecipeAssemblyLine r) -> r.mEUt)
                            .thenComparingInt(r -> r.mDuration))
                    .orElseThrow(() -> new IllegalStateException("No supported real LuV motor recipe"));
                if (resumed) {
                    for (int z : new int[] { 0, 16 }) {
                        lines.add(
                            (MTEMultiBlockBase) ((BaseMetaTileEntity) world.getTileEntity(0, 10, z))
                                .getMetaTileEntity());
                        providers.add((TilePackagedProvider) world.getTileEntity(0, 8, z - 5));
                        var restored = providers.get(providers.size() - 1);
                        ItemStack oldPattern = restored.getStackInSlot(0);
                        if (oldPattern != null && oldPattern.getItem() == ItemAndBlockHolder.PATTERN
                            && restored.queuedJobs() == 0) restored.setInventorySlotContents(0, motorPattern());
                    }
                } else {
                    world.getGameRules()
                        .setOrCreateGameRule("doDaylightCycle", "false");
                    world.getGameRules()
                        .setOrCreateGameRule("doMobSpawning", "false");
                    world.setWorldTime(6000);
                    for (int x = -20; x <= 22; x++) for (int z = -10; z <= 23; z++) {
                        world.setBlock(x, 7, z, (x + z) % 2 == 0 ? Blocks.stonebrick : Blocks.quartz_block);
                    }
                    station(world, player, 0, false);
                    station(world, player, 16, true);
                    player.inventory.setInventorySlotContents(0, GTNGItemList.ItemWirelessConnector.get(1));
                    player.inventory.setInventorySlotContents(1, GTNGItemList.AssemblyLineCore.get(1));
                    player.inventory.setInventorySlotContents(2, GTNGItemList.AdvancedAssemblyLineCore.get(1));
                    player.inventory.setInventorySlotContents(3, GTNGItemList.WirelessPackagedPatternProvider.get(1));
                    player.inventory.currentItem = 0;
                    player.playerNetServerHandler.setPlayerLocation(5.5, 10, -8.5, 20, 12);
                    player.capabilities.isFlying = true;
                    player.sendPlayerAbilities();
                }
                System.out.println("ASSEMBLY_PLAYGROUND: recipe=" + recipe.mOutput.getDisplayName());
            }
            // Only this opt-in fresh playground receives free energy; ordinary machine processing is untouched.
            for (var line : lines) for (var hatch : line.mEnergyHatches) {
                var base = hatch.getBaseMetaTileEntity();
                if (base instanceof BaseMetaTileEntity tile) tile.setStoredEU(base.getEUCapacity());
            }
            if (ticks == 100) {
                if (Boolean.getBoolean("gtng.assembly.interruptQa")) {
                    try {
                        AssemblyLineClientChecks.checkInterruption(world, player);
                    } catch (Exception error) {
                        System.err.println("ASSEMBLY_INTERRUPT_QA_FAILED");
                        error.printStackTrace();
                    }
                }
                for (var line : lines) {
                    boolean formed = line.checkStructure(true, line.getBaseMetaTileEntity());
                    System.out.println(
                        "ASSEMBLY_PLAYGROUND: formed=" + formed
                            + " inputs="
                            + line.mInputBusses.size()
                            + " fluids="
                            + line.mInputHatches.size()
                            + " outputs="
                            + line.mOutputBusses.size());
                    if (!formed) throw new IllegalStateException("Assembly structure did not form");
                }
                for (var provider : providers) {
                    if (!provider.getProxy()
                        .isActive() || provider.getProxy()
                            .getCrafting()
                            .getCpus()
                            .isEmpty())
                        throw new IllegalStateException("ME network not ready");
                    if (!resumed) for (var item : recipe.mInputs) {
                        var stock = AEItemStack.create(item);
                        stock.setStackSize((long) item.stackSize * 32);
                        if (provider.getProxy()
                            .getStorage()
                            .getItemInventory()
                            .injectItems(stock, Actionable.MODULATE, new MachineSource(provider)) != null)
                            throw new IllegalStateException("Item stock rejected");
                    }
                }
            }
            if (ticks == 140) {
                if (!resumed) for (var provider : providers) for (var fluid : recipe.mFluidInputs) {
                    var request = AEFluidStack.create(fluid);
                    request.setStackSize((long) fluid.amount * 32);
                    var available = provider.getProxy()
                        .getStorage()
                        .getFluidInventory()
                        .extractItems(request, Actionable.SIMULATE, new MachineSource(provider));
                    if (available == null || available.getStackSize() < request.getStackSize())
                        throw new IllegalStateException(
                            "Fluid not visible in grid: " + fluid.getLocalizedName()
                                + "; available="
                                + (available == null ? 0 : available.getStackSize()));
                }
                world.saveAllChunks(true, null);
                server.getConfigurationManager()
                    .saveAllPlayerData();
                player.addChatMessage(
                    new ChatComponentText(
                        "测试端就绪：普通线 Z=0，进阶线 Z=16。各自在前方 ME 终端下单：" + recipe.mOutput.getDisplayName()
                            + "。每套已备 32 份材料，能源舱持续补电。"));
                Files.write(
                    new File("assembly-playground-ready.txt").toPath(),
                    ("READY\nOutput: " + recipe.mOutput.getDisplayName() + "\nNormal: 0,10,0\nAdvanced: 0,10,16")
                        .getBytes(StandardCharsets.UTF_8));
                System.out.println("ASSEMBLY_PLAYGROUND_READY: " + recipe.mOutput.getDisplayName());
                ready = true;
            }
            if (ready && Boolean.getBoolean("gtng.assembly.benchmark") && !benchmarkDone) {
                try {
                    benchmark(world);
                } catch (Exception error) {
                    // A failed measurement must not interrupt the user's test-world power or existing orders.
                    benchmarkDone = true;
                    System.err.println("ASSEMBLY_BENCHMARK_FAILED");
                    error.printStackTrace();
                }
            }
        } catch (Exception error) {
            failed = true;
            System.err.println("ASSEMBLY_PLAYGROUND_FAILED");
            error.printStackTrace();
        }
    }

    /** Runs real eight-motor AE orders in a fresh disposable world and measures refill gaps and output conservation. */
    private void benchmark(World world) throws Exception {
        if (resumed) throw new IllegalStateException("Benchmark requires a fresh save");
        if (benchmarkJobs.isEmpty()) {
            for (var provider : providers) {
                var request = AEItemStack.create(recipe.mOutput);
                request.setStackSize(8);
                benchmarkJobs.add(
                    provider.getProxy()
                        .getCrafting()
                        .beginCraftingJob(
                            world,
                            provider.getProxy()
                                .getGrid(),
                            new MachineSource(provider),
                            request,
                            null));
            }
        }
        boolean complete = true;
        for (int i = 0; i < providers.size(); i++) {
            var provider = providers.get(i);
            if (!submitted[i] && benchmarkJobs.get(i)
                .isDone()) {
                var link = provider.getProxy()
                    .getCrafting()
                    .submitJob(
                        benchmarkJobs.get(i)
                            .get(),
                        null,
                        null,
                        false,
                        new MachineSource(provider));
                if (link == null) throw new IllegalStateException("Benchmark order rejected");
                submitted[i] = true;
            }
            peakQueued[i] = Math.max(peakQueued[i], provider.queuedJobs());
            var request = AEItemStack.create(recipe.mOutput);
            request.setStackSize(64);
            var stock = provider.getProxy()
                .getStorage()
                .getItemInventory()
                .extractItems(request, Actionable.SIMULATE, new MachineSource(provider));
            long count = stock == null ? 0 : stock.getStackSize();
            var line = lines.get(i);
            if (line.mMaxProgresstime > 0) seenWorking[i] = true;
            else if (seenWorking[i] && count < 7) idleGaps[i]++;
            // Manual orders may run alongside the benchmark in this interactive playground.
            if (count < 8) complete = false;
        }
        if (complete) {
            for (int i = 0; i < providers.size(); i++) {
                if (peakQueued[i] < 2 || idleGaps[i] > 0) throw new IllegalStateException(
                    "Refill gap on line " + i + ": peak=" + peakQueued[i] + " idle=" + idleGaps[i]);
            }
            benchmarkDone = true;
            String result = "PASS: at least 8 motors returned per line; peak receipts=" + java.util.Arrays
                .toString(peakQueued) + "; idle gaps=" + java.util.Arrays.toString(idleGaps) + "; ticks=" + ticks;
            Files.write(new File("assembly-benchmark-result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
            System.out.println("ASSEMBLY_BENCHMARK: " + result);
        } else if (ticks > 2400) throw new IllegalStateException("Benchmark timed out");
    }

    /** Constructs the actual upstream casing layout and replaces only legal hatch positions. */
    private void station(World world, EntityPlayerMP player, int z, boolean advanced) {
        var controller = machine(
            world,
            0,
            10,
            z,
            advanced ? ggfab.GGItemList.AdvAssLine.get(1) : ItemList.Machine_Multi_Assemblyline.get(1),
            ForgeDirection.NORTH);
        var line = (MTEMultiBlockBase) controller.getMetaTileEntity();
        lines.add(line);
        ItemStack length = new ItemStack(Blocks.stone, 16);
        if (line instanceof MTEAssemblyLine normal) normal.construct(length, false);
        else((MTEAdvAssLine) line).construct(length, false);
        List<int[]> centers = new ArrayList<>();
        for (int x = -16; x <= 16; x++) for (int dz = -3; dz <= 3; dz++) {
            if (world.getBlock(x, 10, z + dz) == GregTechAPI.sBlockCasings2
                && world.getBlockMetadata(x, 10, z + dz) == 9) centers.add(new int[] { x, z + dz });
        }
        centers.sort(Comparator.comparingInt(p -> Math.abs(p[0])));
        if (centers.size() != 16)
            throw new IllegalStateException("Expected 16 constructed lanes, found " + centers.size());
        for (int i = 0; i < centers.size(); i++) {
            int x = centers.get(i)[0], cz = centers.get(i)[1];
            machine(world, x, 8, cz, ItemList.Hatch_Input_Bus_IV.get(1), ForgeDirection.DOWN);
            energy.add(machine(world, x, 11, cz, ItemList.Hatch_Energy_LuV.get(1), ForgeDirection.UP));
            if (i < 4) machine(world, x, 8, cz + 1, ItemList.Hatch_Input_IV.get(1), ForgeDirection.DOWN);
            if (i == 0) machine(world, x, 8, cz - 1, ItemList.Hatch_Maintenance.get(1), ForgeDirection.DOWN);
            if (i == centers.size() - 1)
                machine(world, x, 8, cz - 1, ItemList.Hatch_Output_Bus_IV.get(1), ForgeDirection.DOWN);
        }
        line.mWrench = line.mScrewdriver = line.mSoftMallet = line.mHardHammer = line.mSolderingTool = line.mCrowbar = true;
        ItemStack stick = ItemList.Tool_DataStick.get(1);
        if (!AssemblyLineUtils.setAssemblyLineRecipeOnDataStick(stick, recipe))
            throw new IllegalStateException("Data stick");
        line.setInventorySlotContents(1, stick);
        controller.enableWorking();

        int nz = z - 5;
        world.setBlock(
            0,
            8,
            nz,
            net.minecraft.block.Block.getBlockFromItem(GTNGItemList.WirelessPackagedPatternProvider.getItem()));
        var provider = (TilePackagedProvider) world.getTileEntity(0, 8, nz);
        providers.add(provider);
        provider.setOwnerName(player.getCommandSenderName());
        provider.setInventorySlotContents(
            TilePackagedProvider.CORE,
            (advanced ? GTNGItemList.AdvancedAssemblyLineCore : GTNGItemList.AssemblyLineCore).get(1));
        provider.autoReturn = true;
        if (!provider.bind(new PackagedTarget(0, 0, 10, z, 2))) throw new IllegalStateException("Binding failed");
        var blocks = AEApi.instance()
            .definitions()
            .blocks();
        place(
            world,
            -1,
            nz,
            blocks.controller()
                .maybeStack(1)
                .get());
        place(
            world,
            -1,
            nz + 1,
            blocks.energyCellCreative()
                .maybeStack(1)
                .get());
        place(
            world,
            -2,
            nz,
            blocks.drive()
                .maybeStack(1)
                .get());
        var drive = (TileDrive) world.getTileEntity(-2, 8, nz);
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
        // This AE2FC version leaves basic fluid cells with zero type slots; multi-fluid cells declare five.
        ItemStack fluidCell = new ItemStack(ItemAndBlockHolder.CELL16384KM);
        var cell = (appeng.api.storage.IMEInventoryHandler<appeng.api.storage.data.IAEFluidStack>) appeng.me.storage.CellInventory
            .getCell(fluidCell, null, appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE);
        if (cell == null) throw new IllegalStateException("No fluid cell handler");
        for (var fluid : recipe.mFluidInputs) {
            var stock = AEFluidStack.create(fluid);
            stock.setStackSize((long) fluid.amount * 32);
            var remainder = cell.injectItems(stock, Actionable.MODULATE, new MachineSource(provider));
            if (remainder != null) throw new IllegalStateException(
                "Cell rejected " + fluid.getLocalizedName() + "; remaining=" + remainder.getStackSize());
        }
        drive.getInternalInventory()
            .setInventorySlotContents(1, fluidCell);
        place(
            world,
            -2,
            nz + 1,
            blocks.craftingStorage64k()
                .maybeStack(1)
                .get());
        place(
            world,
            -3,
            nz + 1,
            blocks.craftingAccelerator()
                .maybeStack(1)
                .get());
        terminal(
            world,
            player,
            -3,
            nz,
            AEApi.instance()
                .definitions()
                .parts()
                .craftingTerminal()
                .maybeStack(1)
                .get());
        terminal(
            world,
            player,
            -4,
            nz,
            AEApi.instance()
                .definitions()
                .parts()
                .patternTerminal()
                .maybeStack(1)
                .get());
        provider.setInventorySlotContents(0, motorPattern());
        world.setBlock(1, 8, nz, Blocks.standing_sign, 8, 3);
        var sign = (TileEntitySign) world.getTileEntity(1, 8, nz);
        sign.signText[0] = advanced ? "进阶装配线" : "普通装配线";
        sign.signText[1] = "前方终端下单";
        sign.signText[2] = "材料已备32份";
        sign.signText[3] = "测试供电开启";
        sign.markDirty();
        world.markBlockForUpdate(1, 8, nz);
    }

    private static BaseMetaTileEntity machine(World world, int x, int y, int z, ItemStack stack,
        ForgeDirection facing) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        var base = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        base.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        base.setFrontFacing(facing);
        base.markDirty();
        world.markBlockForUpdate(x, y, z);
        return base;
    }

    /** Encodes native item/fluid inputs using the current GTNH ultimate processing pattern. */
    private ItemStack motorPattern() {
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        var tag = new net.minecraft.nbt.NBTTagCompound();
        var inputs = new net.minecraft.nbt.NBTTagList();
        var outputs = new net.minecraft.nbt.NBTTagList();
        for (var item : recipe.mInputs) {
            var entry = new net.minecraft.nbt.NBTTagCompound();
            appeng.util.Platform.writeStackNBT(AEItemStack.create(item), entry);
            inputs.appendTag(entry);
        }
        for (var fluid : recipe.mFluidInputs) {
            var entry = new net.minecraft.nbt.NBTTagCompound();
            appeng.util.Platform.writeStackNBT(AEFluidStack.create(fluid), entry);
            inputs.appendTag(entry);
        }
        var result = new net.minecraft.nbt.NBTTagCompound();
        appeng.util.Platform.writeStackNBT(AEItemStack.create(recipe.mOutput), result);
        outputs.appendTag(result);
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private static void place(World world, int x, int z, ItemStack stack) {
        world.setBlock(x, 8, z, net.minecraft.block.Block.getBlockFromItem(stack.getItem()), stack.getItemDamage(), 3);
    }

    private static void terminal(World world, EntityPlayerMP player, int x, int z, ItemStack terminal) {
        place(
            world,
            x,
            z,
            AEApi.instance()
                .definitions()
                .blocks()
                .multiPart()
                .maybeStack(1)
                .get());
        IPartHost host = (IPartHost) world.getTileEntity(x, 8, z);
        host.addPart(
            AEApi.instance()
                .definitions()
                .parts()
                .cableGlass()
                .stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN,
            player);
        host.addPart(terminal, ForgeDirection.NORTH, player);
    }
}
