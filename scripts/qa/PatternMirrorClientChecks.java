package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputSlave;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.tileentities.machines.multi.MTEIndustrialCuttingMachine;

@Mod(
    modid = "patternmirrorqa",
    name = "Pattern Mirror QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class PatternMirrorClientChecks {

    private boolean started;
    private volatile boolean finished;
    private volatile boolean serverChecked;
    private int clientTicks;
    private int serverTicks;
    private int frames;
    private BaseMetaTileEntity controllerBase;
    private MTEIndustrialCuttingMachine controller;
    private int[] inputPosition;
    private final StringBuilder report = new StringBuilder();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.patternMirror.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            if (++clientTicks > 6000) throw new AssertionError("Client QA timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("pattern-mirror-qa-" + System.currentTimeMillis(), "Pattern Mirror QA",
                    new WorldSettings(738L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || serverChecked) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            if (++serverTicks != 20) return;
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            setup(player);
            checkRegistration();
            checkLifecycle(player);
            serverChecked = true;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        world.setWorldTime(6000);
        ItemStack controllerStack = null;
        for (var machine : GregTechAPI.METATILEENTITIES) {
            if (machine instanceof MTEIndustrialCuttingMachine) {
                controllerStack = machine.getStackForm(1);
                break;
            }
        }
        require(controllerStack != null, "native industrial cutting machine is registered");
        controllerBase = place(player, 0, 12, 0, controllerStack);
        controllerBase.disableWorking();
        controller = (MTEIndustrialCuttingMachine) controllerBase.getMetaTileEntity();
        controller.construct(new ItemStack(Items.diamond), false);
        List<int[]> casings = new ArrayList<>();
        for (int y = 8; y <= 18; y++) {
            for (int x = -10; x <= 10; x++) {
                for (int z = -2; z <= 6; z++) {
                    if (
                        world.getBlock(x, y, z) == Casings.CuttingFactoryFrame.getBlock()
                            && world.getBlockMetadata(x, y, z) == Casings.CuttingFactoryFrame.getBlockMeta()
                    ) {
                        casings.add(new int[] { x, y, z });
                    }
                }
            }
        }
        require(casings.size() >= 15, "construct builds the complete cutting factory casing fixture");
        inputPosition = casings.get(0);
        ItemStack[] hatches = { GTNGItemList.SuperMTEHatchCraftingInputME.get(1), ItemList.Hatch_Output_Bus_EV.get(1),
            ItemList.Hatch_Energy_EV.get(1), ItemList.Hatch_Maintenance.get(1), ItemList.Hatch_Muffler_EV.get(1) };
        for (int i = 0; i < hatches.length; i++) {
            int[] position = casings.get(i);
            place(player, position[0], position[1], position[2], hatches[i]);
        }
        checkStructure("direct fluid master control");
        player.playerNetServerHandler.setPlayerLocation(-10, 16, -10, -45, 18);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
    }

    private void checkRegistration() {
        for (GTNGItemList item : new GTNGItemList[] { GTNGItemList.SuperMTEHatchCraftingInputME,
            GTNGItemList.SuperMTEHatchCraftingInputBusME, GTNGItemList.CompactSuperMTEHatchCraftingInputME,
            GTNGItemList.SuperMTEHatchCraftingInputSlave, GTNGItemList.CircuitMEPatternMirror,
            GTNGItemList.CircuitMEPatternBuffer }) {
            ItemStack stack = item.get(1);
            require(HatchElement.InputHatch.matchesHatch(GregTechAPI.METATILEENTITIES[stack.getItemDamage()]),
                "native InputHatch class matching accepts " + item.name());
            require(GTStructureUtility.filterByMTEClass(HatchElement.InputHatch.mteClasses()).test(stack),
                "survival hatch filter accepts " + item.name());
        }
    }

    private void checkLifecycle(EntityPlayerMP player) throws Exception {
        checkMissingInput(player);
        var fluidMaster = (SuperMTEHatchCraftingInputME) place(player, 0, 12, -5,
            GTNGItemList.SuperMTEHatchCraftingInputME.get(1)).getMetaTileEntity();
        var itemMaster = (SuperMTEHatchCraftingInputME) place(player, 2, 12, -5,
            GTNGItemList.SuperMTEHatchCraftingInputBusME.get(1)).getMetaTileEntity();
        var circuitMaster = (SuperMTEHatchCraftingInputME) place(player, 4, 12, -5,
            GTNGItemList.CircuitMEPatternBuffer.get(1)).getMetaTileEntity();
        var mirror = replaceInput(player, GTNGItemList.SuperMTEHatchCraftingInputSlave);
        require(mirror.getMaster() == null && !mirror.supportsFluids(), "unbound mirror has no fluid data source");
        checkRoutes(mirror, "unbound mirror");
        checkStructure("unbound mirror");
        require(mirror.trySetMasterFromCoord(0, 12, -5) == fluidMaster && mirror.supportsFluids(),
            "mirror binds a fluid master");
        checkRoutes(mirror, "fluid mirror");
        checkStructure("fluid mirror");

        NBTTagCompound saved = new NBTTagCompound();
        mirror.saveNBTData(saved);
        mirror = replaceInput(player, GTNGItemList.SuperMTEHatchCraftingInputSlave);
        mirror.loadNBTData(saved);
        require(mirror.getMaster() == null && !mirror.supportsFluids(),
            "reloaded mirror has saved coordinates before its master lookup");
        checkStructure("mirror awaiting master lookup");
        mirror.onPostTick(mirror.getBaseMetaTileEntity(), 100);
        require(mirror.getMaster() == fluidMaster && mirror.supportsFluids(),
            "normal server tick resolves the saved fluid master");
        checkStructure("resolved saved mirror");

        mirror = replaceInput(player, GTNGItemList.SuperMTEHatchCraftingInputSlave);
        mirror.loadNBTData(saved);
        World world = player.worldObj;
        NBTTagCompound masterSaved = new NBTTagCompound();
        fluidMaster.saveNBTData(masterSaved);
        world.setBlockToAir(0, 12, -5);
        mirror.onPostTick(mirror.getBaseMetaTileEntity(), 100);
        require(mirror.getMaster() == null && !mirror.supportsFluids(),
            "saved mirror tolerates an unavailable master tile");
        checkRoutes(mirror, "unavailable master mirror");
        checkStructure("unavailable master mirror");
        fluidMaster = (SuperMTEHatchCraftingInputME) place(player, 0, 12, -5,
            GTNGItemList.SuperMTEHatchCraftingInputME.get(1)).getMetaTileEntity();
        fluidMaster.loadNBTData(masterSaved);
        mirror.onPostTick(mirror.getBaseMetaTileEntity(), 200);
        require(mirror.getMaster() == fluidMaster, "mirror reconnects when its master tile becomes available");
        checkStructure("master available again");

        require(mirror.trySetMasterFromCoord(2, 12, -5) == itemMaster && !mirror.supportsFluids(),
            "rebinding to an item-only master preserves its actual fluid capability");
        checkRoutes(mirror, "item-only mirror");
        checkStructure("item-only mirror");
        require(mirror.trySetMasterFromCoord(0, 12, -5) == fluidMaster && mirror.supportsFluids(),
            "rebinding restores the actual fluid capability");
        checkStructure("rebound fluid mirror");

        mirror = replaceInput(player, GTNGItemList.CircuitMEPatternMirror);
        checkRoutes(mirror, "unbound circuit mirror");
        checkStructure("unbound circuit mirror");
        require(mirror.trySetMasterFromCoord(4, 12, -5) == circuitMaster && mirror.supportsFluids(),
            "dedicated circuit mirror binds its actual circuit master");
        checkStructure("bound circuit mirror");
    }

    private SuperMTEHatchCraftingInputSlave replaceInput(EntityPlayerMP player, GTNGItemList item) {
        player.worldObj.setBlockToAir(inputPosition[0], inputPosition[1], inputPosition[2]);
        return (SuperMTEHatchCraftingInputSlave) place(player, inputPosition[0], inputPosition[1], inputPosition[2],
            item.get(1)).getMetaTileEntity();
    }

    private void checkRoutes(SuperMTEHatchCraftingInputSlave mirror, String label) throws Exception {
        Method check = MTEEnhancedMultiBlockBase.class.getDeclaredMethod("checkHasInputHatch", List.class);
        check.setAccessible(true);
        for (int route = 0; route < 3; route++) {
            controller.clearHatches();
            boolean accepted = switch (route) {
                case 0 -> controller.addInputHatchToMachineList(mirror.getBaseMetaTileEntity(), 0);
                case 1 -> controller.addInputBusToMachineList(mirror.getBaseMetaTileEntity(), 0);
                default -> controller.addToMachineList(mirror.getBaseMetaTileEntity(), 0);
            };
            require(
                accepted && controller.mDualInputHatches.size() == 1 && controller.mDualInputHatches.get(0) == mirror,
                label + " registers input route " + route);
            require(controller.addInputHatchToMachineList(mirror.getBaseMetaTileEntity(), 0)
                && controller.mDualInputHatches.size() == 1, label + " repeat fluid route stays unique " + route);
            require(HatchElement.InputHatch.count(controller) == 1 && HatchElement.InputBus.count(controller) == 1,
                label + " supplies one native fluid-input and item-input structure role " + route);
            List<StructureError> errors = new ArrayList<>();
            check.invoke(controller, errors);
            require(errors.isEmpty(), label + " passes native checkHasInputHatch " + route);
        }
    }

    private void checkMissingInput(EntityPlayerMP player) throws Exception {
        World world = player.worldObj;
        world.setBlockToAir(inputPosition[0], inputPosition[1], inputPosition[2]);
        world.setBlock(inputPosition[0], inputPosition[1], inputPosition[2], Casings.CuttingFactoryFrame.getBlock(),
            Casings.CuttingFactoryFrame.getBlockMeta(), 3);
        controller.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        Method check = MTEEnhancedMultiBlockBase.class.getDeclaredMethod("checkHasInputHatch", List.class);
        check.setAccessible(true);
        check.invoke(controller, errors);
        require(errors.size() == 1 && HatchElement.InputHatch.count(controller) == 0,
            "negative control: native checkHasInputHatch rejects a machine without any input hatch");
        errors.clear();
        controller.checkMachine(controllerBase, null, errors);
        require(!errors.isEmpty() && !controller.checkStructure(true, controllerBase),
            "negative control: complete cutting factory without an input hatch fails to form");
    }

    private void checkStructure(String label) {
        controller.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        controller.checkMachine(controllerBase, null, errors);
        require(errors.isEmpty(), label + " native checkMachine: "
            + errors.stream().map(StructureError::getDisplayString).collect(Collectors.joining("; ")));
        require(controller.checkStructure(true, controllerBase), label + " complete cutting factory forms");
        require(HatchElement.InputHatch.count(controller) == 1, label + " has exactly one fluid-input role");
        require(controller.mInputHatches.isEmpty() && controller.mDualInputHatches.size() == 1,
            label + " forms using its pattern input alone");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !serverChecked || finished || ++frames < 30) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            ScreenShotHelper.saveScreenshot(output(), "pattern-mirror-cutting-factory.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            Files.write(new File(output(), "checks.txt").toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
            Files.write(new File(output(), "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
            finished = true;
            mc.shutdown();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static BaseMetaTileEntity place(EntityPlayerMP player, int x, int y, int z, ItemStack stack) {
        World world = player.worldObj;
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        report.append(message).append('\n');
        System.out.println("PATTERN_MIRROR_QA: " + message);
    }

    private static File output() {
        File directory = new File(System.getProperty("gtng.patternMirror.output", "."));
        directory.mkdirs();
        return directory;
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "failure.txt").toPath(), trace.toString().getBytes(StandardCharsets.UTF_8));
            Files.write(new File(output(), "checks.txt").toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
            Files.write(new File(output(), "result.txt").toPath(), "FAIL".getBytes(StandardCharsets.UTF_8));
        } catch (Exception writeFailure) {
            writeFailure.printStackTrace();
        }
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }
}
