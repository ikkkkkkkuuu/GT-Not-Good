package com.xyp.gtnotgood.common.compactae;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Future;
import java.util.EnumSet;

import net.minecraft.client.Minecraft;
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

import com.xyp.gtnotgood.common.machines.multiblock.AssemblerMatrix;
import com.xyp.gtnotgood.common.machines.multiblock.QuantumComputer;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.me.cache.CraftingGridCache;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/** Exercises actual StructureLib formation, AE CPU dispatch, matrix persistence and both MUI2 screens. */
@Mod(
    modid = "compactaeqa",
    name = "Compact AE QA",
    version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public final class CompactAEClientChecks {

    private boolean started;
    private boolean finished;
    private int ticks;
    private int stage;
    private volatile int screenshot;
    private int frames;
    private AssemblerMatrix matrix;
    private QuantumComputer computer;
    private Future<ICraftingJob> job;
    private TileDrive drive;
    private int requestedSticks;
    private int networkResumeTick;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.compactae.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "compact-ae-qa-" + System.currentTimeMillis(),
                "Compact AE QA",
                new WorldSettings(25L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        World world = player.worldObj;
        try {
            ticks++;
            if (ticks == 1) setup(world, player);
            if (ticks < 100 || screenshot != 0) return;
            if (ticks > 1200) throw new AssertionError("QA timeout at stage " + stage);
            if (stage == 0) {
                require(
                    matrix.checkStructure(true, matrix.getBaseMetaTileEntity()),
                    "matrix forms without energy hatches");
                require(
                    computer.checkStructure(true, computer.getBaseMetaTileEntity()),
                    "computer forms from five casings");
                require(
                    matrix.getPatterns()
                        .getSizeInventory() == 144 && matrix.rows() == 16,
                    "144 slots / 16 terminal rows");
                require(
                    AEApi.instance()
                        .registries()
                        .interfaceTerminal()
                        .getSupportedClasses()
                        .contains(AssemblerMatrix.class),
                    "matrix registered in interface terminal");
                matrix.setShowPattern(false);
                require(!terminalDisplays(matrix), "matrix hidden from interface terminal");
                matrix.setShowPattern(true);
                require(terminalDisplays(matrix), "matrix visible in interface terminal");
                require(
                    matrix.getProxy()
                        .isActive() && computer.isActive(),
                    "AE network connection");
                require(
                    matrix.getProxy()
                        .getGrid()
                        .getMachines(AssemblerMatrix.class)
                        .iterator()
                        .hasNext(),
                    "interface terminal can enumerate matrix on AE grid");
                require(
                    computer.getProxy()
                        .getCrafting()
                        .getCpus()
                        .size() == 1,
                    "one virtual CPU visible");
                require(computer.virtualCPU.getCoProcessors() == Integer.MAX_VALUE, "maximum coprocessors");
                ItemStack encoded = pattern();
                var details = ((appeng.api.implementations.ICraftingPatternItem) encoded.getItem())
                    .getPatternForItem(encoded, world);
                require(details != null, "valid native crafting pattern");
                requestedSticks = (int) details.getCondensedOutputs()[0].getStackSize() * 32;
                System.out.println("COMPACT_AE_QA: requested sticks=" + requestedSticks);
                matrix.getPatterns()
                    .setInventorySlotContents(143, encoded);
                matrix.setMachineMode(AssemblerMatrix.MODE_OPERATING);
                matrix.getBaseMetaTileEntity()
                    .enableWorking();
                matrix.getProxy()
                    .getStorage()
                    .getItemInventory()
                    .injectItems(
                        AEItemStack.create(new ItemStack(Blocks.planks, 64)),
                        Actionable.MODULATE,
                        new MachineSource(matrix));
                stage = 1;
                return;
            }
            if (stage == 1) {
                if (matrix.getProxy()
                    .getCrafting()
                    .getCraftingFor(AEItemStack.create(new ItemStack(Items.stick)), null, 0, world)
                    .isEmpty()) return;
                job = matrix.getProxy()
                    .getCrafting()
                    .beginCraftingJob(
                        world,
                        matrix.getProxy()
                            .getGrid(),
                        new MachineSource(matrix),
                        AEItemStack.create(new ItemStack(Items.stick, requestedSticks)),
                        null);
                stage = 2;
            } else if (stage == 2 && job.isDone()) {
                require(
                    !job.get()
                        .isSimulation(),
                    "32-craft job has all ingredients");
                var link = matrix.getProxy()
                    .getCrafting()
                    .submitJob(job.get(), null, null, false, new MachineSource(matrix));
                require(link != null, "quantum CPU accepts order");
                require(computer.cpus.size() == 1 && computer.virtualCPU != null, "CPU splits dynamically");
                // No native AE energy extraction is permitted during the actual dispatch below.
                IEnergyGrid noPower = (IEnergyGrid) Proxy.newProxyInstance(
                    IEnergyGrid.class.getClassLoader(),
                    new Class<?>[] { IEnergyGrid.class },
                    (proxy, method, args) -> {
                        throw new AssertionError("Unexpected CPU energy access: " + method.getName());
                    });
                computer.cpus.get(0)
                    .updateCraftingLogic(
                        matrix.getProxy()
                            .getGrid(),
                        noPower,
                        (CraftingGridCache) matrix.getProxy()
                            .getCrafting());
                require(
                    (matrix.mMaxProgresstime == 1 && matrix.lEUt == 0) || matrix.checkProcessing()
                        .wasSuccessful(),
                    "matrix starts a free batch");
                require(matrix.mMaxProgresstime == 1 && matrix.lEUt == 0, "1 tick, zero EU");
                matrix.stopMachine(gregtech.api.util.shutdown.ShutDownReasonRegistry.NONE);
                NBTTagCompound saved = new NBTTagCompound();
                matrix.getBaseMetaTileEntity()
                    .writeToNBT(saved);
                world.removeTileEntity(0, 10, 0);
                BaseMetaTileEntity restored = new BaseMetaTileEntity();
                restored.setWorldObj(world);
                restored.readFromNBT(saved);
                world.setTileEntity(0, 10, 0, restored);
                matrix = (AssemblerMatrix) restored.getMetaTileEntity();
                require(
                    matrix.getPatterns()
                        .getStackInSlot(143) != null,
                    "last pattern slot survives NBT");
                matrix.getBaseMetaTileEntity()
                    .enableWorking();
                stage = 3;
            } else if (stage == 3) {
                if (!matrix.getProxy()
                    .isActive()) return;
                // Reloaded patterns must return without touching the cable or manually refreshing the matrix.
                if (matrix.getProxy()
                    .getCrafting()
                    .getCraftingFor(AEItemStack.create(new ItemStack(Items.stick)), null, 0, world)
                    .isEmpty()) return;
                var count = matrix.getProxy()
                    .getStorage()
                    .getItemInventory()
                    .extractItems(
                        AEItemStack.create(new ItemStack(Items.stick, 256)),
                        Actionable.SIMULATE,
                        new MachineSource(matrix));
                if (count == null || count.getStackSize() < requestedSticks) return;
                require(
                    count.getStackSize() == requestedSticks,
                    "exact batch output after interrupted cycle and NBT reload");
                var planks = matrix.getProxy()
                    .getStorage()
                    .getItemInventory()
                    .extractItems(
                        AEItemStack.create(new ItemStack(Blocks.planks, 64)),
                        Actionable.SIMULATE,
                        new MachineSource(matrix));
                require(planks == null, "exact input consumption");
                require(matrix.lEUt == 0, "no recipe power draw");
                stage = 6;
            } else if (stage == 6) {
                // Let startup structure checks finish before testing a second order on the reloaded matrix.
                if (ticks < 400) return;
                matrix.getProxy().getStorage().getItemInventory().injectItems(
                    AEItemStack.create(new ItemStack(Blocks.planks, 64)), Actionable.MODULATE, new MachineSource(matrix));
                job = matrix.getProxy().getCrafting().beginCraftingJob(
                    world, matrix.getProxy().getGrid(), new MachineSource(matrix),
                    AEItemStack.create(new ItemStack(Items.stick, requestedSticks)), null);
                stage = 7;
            } else if (stage == 7 && job.isDone()) {
                require(!job.get().isSimulation(), "new order after idle reload has ingredients");
                var link = matrix.getProxy().getCrafting().submitJob(
                    job.get(), null, null, false, new MachineSource(matrix));
                require(link != null, "new order after idle reload accepted");
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
                    AEItemStack.create(new ItemStack(Items.stick, requestedSticks * 2)), Actionable.SIMULATE,
                    new MachineSource(matrix));
                if (count == null || count.getStackSize() < requestedSticks * 2) return;
                require(count.getStackSize() == requestedSticks * 2,
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
                Files.write(new File("compact-ae-qa-result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                System.out.println("COMPACT_AE_QA: PASS");
                finished = true;
                Minecraft.getMinecraft()
                    .shutdown();
            }
        } catch (Throwable failure) {
            finished = true;
            System.err.println("COMPACT_AE_QA: FAIL stage=" + stage);
            failure.printStackTrace();
            Minecraft.getMinecraft()
                .shutdown();
        }
    }

    private void setup(World world, EntityPlayerMP player) {
        matrix = (AssemblerMatrix) controller(world, player, 0, GTNGItemList.AssemblerMatrix.get(1))
            .getMetaTileEntity();
        computer = (QuantumComputer) controller(world, player, 4, GTNGItemList.QuantumComputer.get(1))
            .getMetaTileEntity();
        matrix.construct(new ItemStack(Items.stick), false);
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
        world.setBlock(
            2,
            10,
            0,
            definitions.blocks()
                .drive()
                .maybeBlock()
                .get());
        drive = (TileDrive) world.getTileEntity(2, 10, 0);
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                definitions.items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
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
        ItemStack result = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .get();
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

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || screenshot == 0) return;
        if (++frames < 40) return;
        Minecraft mc = Minecraft.getMinecraft();
        require(
            mc.currentScreen instanceof com.cleanroommc.modularui.screen.GuiContainerWrapper,
            "MUI2 screen opens: " + mc.currentScreen);
        ScreenShotHelper.saveScreenshot(
            new File("."),
            "compact-ae-" + screenshot + ".png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
        frames = 0;
        screenshot = 0;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("COMPACT_AE_QA: " + message);
    }

    /** Exercises the exact visibility predicate used by AE2's interface terminal after mixin application. */
    private static boolean terminalDisplays(AssemblerMatrix matrix) throws ReflectiveOperationException {
        Method visibility = ContainerInterfaceTerminal.class
            .getDeclaredMethod("getTerminalVisibility", appeng.api.util.IInterfaceViewable.class);
        visibility.setAccessible(true);
        return (Boolean) visibility.invoke(null, matrix);
    }
}
