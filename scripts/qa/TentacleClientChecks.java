package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing;
import com.xyp.gtnotgood.common.blocks.tentacle.TentacleCasing;
import com.xyp.gtnotgood.common.blocks.tentacle.TentacleConnectedIcons;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.NyarlathotepsTentacle;
import com.xyp.gtnotgood.common.machines.multiblock.structure.NyarlathotepsTentacleStructure;
import com.xyp.gtnotgood.common.recipe.machine.TentacleAssemblyRecipes;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import bartworks.API.recipe.BartWorksRecipeMaps;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.IBlockWithActiveOffset;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.AssemblyLineUtils;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.RecipeAssemblyLine;
import gregtech.api.util.GTUtility;
import gregtech.common.data.GTCoilTracker;

@Mod(modid = "tentacleqa", name = "Tentacle QA", version = "1", dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class TentacleClientChecks {

    private final StringBuilder report = new StringBuilder();
    private BaseMetaTileEntity tile;
    private NyarlathotepsTentacle machine;
    private List<int[]> slots;
    private boolean launched;
    private volatile boolean checked;
    private volatile boolean finished;
    private int ticks;
    private int frames;
    private long visualStart;
    private volatile boolean requestedGui;
    private volatile boolean preparingGui, guiTileReady;
    private int guiWaitTicks;
    private volatile int openedMode = -1;
    private int capturedMode = -1;
    private volatile int requestedVisualStage;
    private volatile int visualStage;
    private double targetX = 0.5, targetY = 98.5, targetZ = 23.5;
    private RecipeAssemblyLine visualRecipe;
    private int runningTicks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.tentacle.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (preparingGui && mc.theWorld != null) {
            guiTileReady = mc.theWorld.getTileEntity(0, 100, 0) instanceof BaseMetaTileEntity clientTile
                && clientTile.getMetaTileEntity() instanceof NyarlathotepsTentacle;
        }
        if (finished) {
            FMLCommonHandler.instance().exitJava(0, false);
            return;
        }
        if (!launched && mc.theWorld == null && mc.currentScreen != null) {
            launched = true;
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.renderDistanceChunks = 16;
            mc.gameSettings.clouds = false;
            mc.gameSettings.gammaSetting = 1;
            mc.launchIntegratedServer("tentacle-qa-" + System.currentTimeMillis(), "Tentacle QA",
                new WorldSettings(83L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (checked) {
                if (requestedVisualStage != visualStage) {
                    visualStage(player, requestedVisualStage);
                    visualStage = requestedVisualStage;
                }
                if (visualStage == 2 || visualStage == 3) {
                    for (var hatch : machine.mEnergyHatches) ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity())
                        .setStoredEU(hatch.getBaseMetaTileEntity().getEUCapacity());
                    if (machine.getModuleWorks().isEmpty()) supplyVisualRecipe();
                    if (tile.isActive()) runningTicks++;
                }
                if (requestedGui) {
                    require(++guiWaitTicks < 400, "client controller synchronizes before opening GUI");
                    if (!preparingGui) {
                        preparingGui = true;
                        player.closeScreen();
                        player.playerNetServerHandler.setPlayerLocation(0.5, 101, -3.5, 0, 0);
                    }
                    if (!guiTileReady) return;
                    requestedGui = false;
                    machine.setMachineMode(0);
                    machine.onRightclick(tile, player);
                    openedMode = 0;
                }
                if (openedMode >= 0) openedMode = machine.getMachineMode();
                return;
            }
            if (++ticks == 1) build(player);
            if (ticks == 80) {
                verify();
                if (Boolean.getBoolean("gtng.tentacle.guiOnly")) {
                    tile.disableWorking();
                    requestedGui = true;
                    report.append("GUI-only run: processing and running visuals were not checked.\n");
                } else processing();
                machine.setMachineMode(0);
                NBTTagCompound mode = new NBTTagCompound();
                machine.saveNBTData(mode);
                machine.setMachineMode(1);
                machine.loadNBTData(mode);
                require(machine.getMachineMode() == 0, "selected mode persists");
                player.capabilities.allowFlying = player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                double dx = 0, dy = 98.5 - 155, dz = 23.5 - (-90.5);
                player.playerNetServerHandler.setPlayerLocation(0.5, 155, -90.5,
                    (float) Math.toDegrees(Math.atan2(dz, dx)) - 90,
                    (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
                checked = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void visualStage(EntityPlayerMP player, int stage) {
        if (stage == 1) {
            String[][] shape = NyarlathotepsTentacleStructure.shape();
            int[] nearest = null;
            for (int y = 0; y < shape.length; y++) for (int z = 0; z < shape[y].length; z++)
                for (int x = 0; x < shape[y][z].length(); x++) if (shape[y][z].charAt(x) == 'E') {
                    int[] p = position(new int[] { x, y, z });
                    if (nearest == null || p[2] < nearest[2]) nearest = p;
                }
            require(nearest != null, "hyper core sample exists");
            targetX = nearest[0] + 0.5;
            targetY = nearest[1] + 0.5;
            targetZ = nearest[2] + 0.5;
            player.playerNetServerHandler.setPlayerLocation(targetX + 7, targetY + 4, targetZ - 12, 0, 0);
        }
        if (stage == 2) {
            require(machine.mCoils.size() == 2671, "all 2671 active blocks tracked, including 154 hyper cores");
            machine.setMachineMode(0);
            visualRecipe = RecipeAssemblyLine.sAssemblylineRecipes.stream()
                .filter(recipe -> recipe.mEUt > 0 && recipe.mEUt <= TierEU.RECIPE_UHV
                    && recipe.mInputs.length <= 16
                    && recipe.mFluidInputs.length <= 4
                    && recipe.mOutput != null)
                .max(Comparator.comparingLong(recipe -> (long) recipe.mDuration * recipe.mEUt))
                .orElseThrow(() -> new AssertionError("No native recipe for running appearance"));
            ItemStack stick = ItemList.Tool_DataStick.get(1);
            require(AssemblyLineUtils.setAssemblyLineRecipeOnDataStick(stick, visualRecipe), "visual recipe research");
            machine.getDataHatches().get(0).setInventorySlotContents(0, stick);
            supplyVisualRecipe();
            tile.enableWorking();
            require(machine.checkProcessing().wasSuccessful(), "visual recipe starts");
            NBTTagCompound visualTask = new NBTTagCompound();
            machine.saveNBTData(visualTask);
            // Hold a real accepted task long enough to capture both sides of the native activation sync.
            visualTask.getTagList("gtngModuleWork", 10).getCompoundTagAt(0).setInteger("duration", 20000);
            machine.loadNBTData(visualTask);
        }
        if (stage == 3) {
            require(runningTicks >= 20, "machine naturally runs for at least twenty server ticks");
            checkActiveBlocks(tile.getWorld(), true);
            targetX = 0.5;
            targetY = 98.5;
            targetZ = 23.5;
            player.playerNetServerHandler.setPlayerLocation(0.5, 155, -90.5, 0, 0);
        }
        if (stage == 4) tile.disableWorking();
    }

    private void supplyVisualRecipe() {
        for (var bus : machine.mInputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (var bus : machine.mOutputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (int i = 0; i < visualRecipe.mInputs.length; i++)
            machine.mInputBusses.get(0).setInventorySlotContents(i, visualRecipe.mInputs[i].copy());
        for (var hatch : machine.mInputHatches) hatch.mFluid = null;
        for (int i = 0; i < visualRecipe.mFluidInputs.length; i++)
            machine.mInputHatches.get(i).mFluid = visualRecipe.mFluidInputs[i].copy();
    }

    private void checkActiveBlocks(World world, boolean active) {
        String[][] shape = NyarlathotepsTentacleStructure.shape();
        int count = 0;
        for (int y = 0; y < shape.length; y++) for (int z = 0; z < shape[y].length; z++)
            for (int x = 0; x < shape[y][z].length(); x++) if ("BDEQX".indexOf(shape[y][z].charAt(x)) >= 0) {
                int[] p = position(new int[] { x, y, z });
                var block = world.getBlock(p[0], p[1], p[2]);
                require(block instanceof IBlockWithActiveOffset, "active block interface");
                int offset = ((IBlockWithActiveOffset) block).getActiveOffset();
                boolean actual = world.isRemote ? world.getBlockMetadata(p[0], p[1], p[2]) >= offset
                    : GTCoilTracker.isCoilActive(world, p[0], p[1], p[2]);
                require(actual == active,
                    "active appearance " + shape[y][z].charAt(x)
                        + " at "
                        + p[0]
                        + ","
                        + p[1]
                        + ","
                        + p[2]
                        + " remote="
                        + world.isRemote
                        + " meta="
                        + world.getBlockMetadata(p[0], p[1], p[2])
                        + " tracker="
                        + GTCoilTracker.isCoilActive(tile.getWorld(), p[0], p[1], p[2])
                        + " coils="
                        + machine.mCoils.size()
                        + " dead="
                        + tile.isDead());
                count++;
            }
        require(count == 2671, "full active block coverage");
    }

    private void build(EntityPlayerMP player) {
        World world = player.worldObj;
        world.setWorldTime(6000);
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        for (int x = -7; x <= 7; x++) for (int z = -2; z <= 5; z++) world.getChunkFromChunkCoords(x, z);
        tile = place(world, 0, 100, 0, GTNGMachineID.NyarlathotepsTentacle.id, player);
        machine = (NyarlathotepsTentacle) tile.getMetaTileEntity();
        machine.construct(new ItemStack(Items.diamond), false);
        slots = slots(NyarlathotepsTentacleStructure.shape());
        ItemStack[] hatches = { ItemList.Hatch_Input_Bus_UV.get(1), ItemList.Hatch_Output_Bus_UV.get(1),
            ItemList.Hatch_Input_UV.get(1), ItemList.Hatch_Energy_UHV.get(1), ItemList.Hatch_DataAccess_UV.get(1),
            GTNGItemList.StaticParallelControllerT1.get(1), GTNGItemList.StaticSpeedControllerT1.get(1),
            GTNGItemList.PowerConsumptionControllerT1.get(1), GTNGItemList.LowSpeedPerfectOverclockController.get(1),
            GTNGItemList.ExecutionCore.get(1), GTNGItemList.AdvancedExecutionCore.get(1),
            GTNGItemList.PerfectExecutionCore.get(1), ItemList.Hatch_Energy_UHV.get(1), ItemList.Hatch_Input_UV.get(1),
            ItemList.Hatch_Input_UV.get(1), ItemList.Hatch_Input_UV.get(1) };
        for (int i = 0; i < hatches.length; i++) {
            int[] position = position(slots.get(i));
            place(world, position[0], position[1], position[2], hatches[i].getItemDamage(), player);
        }
    }

    private void verify() {
        List<StructureError> errors = form();
        require(errors.isEmpty(), "full original structure forms: " + errors);
        require(machine.mMachine, "native full structure check succeeds");
        require(machine.mEnergyHatches.size() == 2, "multiple normal energy hatches accepted");
        require(machine.getDataHatches().size() == 1, "one data access hatch");
        require(machine.getModularHatches().size() == 7, "four parameter modules and three execution cores discovered");
        require(machine.getModuleTimeReduction() == 4 && machine.getModulePowerIncrease() == 4,
            "overclock module is applied");
        require(machine.getModuleSpeedMultiplier() > 1 && machine.getModulePowerMultiplier() < 1,
            "speed and power modules are applied");
        require(!GTNGRecipeMaps.TentacleAssemblyRecipes.getAllRecipes().isEmpty(), "native assembly pool imported");
        require(GTNGRecipeMaps.TentacleAssemblyRecipes.getAllRecipes().stream()
            .anyMatch(recipe -> recipe.mEUt >= TierEU.RECIPE_UIV), "UIV recipes included in unified assembly pool");
        for (TentacleCasing casing : TentacleCasing.values()) {
            require(casing.getStack(1).getItem() != null, "registered casing " + casing);
            require(RecipeMaps.assemblerRecipes.getAllRecipes().stream().anyMatch(recipe -> {
                for (ItemStack output : recipe.mOutputs)
                    if (GTUtility.areStacksEqual(output, casing.getStack(1))) return true;
                return false;
            }), "survival acquisition recipe " + casing);
        }
        int[] missing = position(slots.get(4));
        tile.getWorld().setBlock(missing[0], missing[1], missing[2], TentacleCasing.MolecularCasing.getBlock(),
            TentacleCasing.MolecularCasing.getMetadata(), 3);
        require(form().isEmpty(), "structure accepts no optional data hatch");
        EntityPlayerMP player = (EntityPlayerMP) FMLCommonHandler.instance().getMinecraftServerInstance()
            .getConfigurationManager().playerEntityList.get(0);
        place(tile.getWorld(), missing[0], missing[1], missing[2], ItemList.Hatch_DataAccess_UV.get(1).getItemDamage(),
            player);
        require(form().isEmpty(), "restored data hatch reforms");
        tile.setFrontFacing(ForgeDirection.EAST);
        require(!form().isEmpty(), "rotated controller rejects unrotated structure");
        tile.setFrontFacing(ForgeDirection.NORTH);
        require(form().isEmpty(), "original facing reforms");
        report.append(
            "Original 165x48x49 structure, fourteen casing recipes, modules, all core types, two energy hatches, optional data hatch and facing checks verified.\n");
    }

    private List<StructureError> form() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(tile, null, errors);
        if (errors.isEmpty()) require(machine.checkStructure(true, tile), "StructureLib native check");
        return errors;
    }

    private void processing() throws Exception {
        machine.setMachineMode(0);
        RecipeAssemblyLine source = RecipeAssemblyLine.sAssemblylineRecipes.stream()
            .filter(recipe -> recipe.mEUt >= TierEU.RECIPE_LuV && recipe.mEUt <= TierEU.RECIPE_UV
                && recipe.mInputs.length <= 16
                && recipe.mFluidInputs.length <= 4
                && recipe.mDuration >= 200)
            .filter(recipe -> {
                for (ItemStack stack : recipe.mInputs)
                    if (stack == null || stack.stackSize < 1 || stack.stackSize > 64) return false;
                return true;
            }).findFirst().orElseThrow(() -> new AssertionError("No native assembly recipe for fixture"));
        var input = machine.mInputBusses.get(0);
        for (int slot = 0; slot < input.getSizeInventory(); slot++) input.setInventorySlotContents(slot, null);
        for (int slot = 0; slot < source.mInputs.length; slot++)
            input.setInventorySlotContents(slot, source.mInputs[slot].copy());
        for (int i = 0; i < source.mFluidInputs.length; i++)
            machine.mInputHatches.get(i).mFluid = source.mFluidInputs[i].copy();
        for (var hatch : machine.mEnergyHatches) ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity())
            .setStoredEU(hatch.getBaseMetaTileEntity().getEUCapacity());
        require(machine.checkProcessing().wasSuccessful(), "native assembly recipe starts without research");
        require(machine.getModuleWorks().size() == 1, "controller owns one execution task");
        require(items(input.mInventory) == 0, "native ingredients committed once");
        GTRecipe converted = TentacleAssemblyRecipes.convert(source);
        require(converted.mEUt == source.mEUt && converted.mDuration == source.mDuration,
            "import keeps original recipe energy and duration");
        NBTTagCompound saved = new NBTTagCompound();
        tile.writeToNBT(saved);
        World world = tile.getWorld();
        world.removeTileEntity(0, 100, 0);
        tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        tile.readFromNBT(saved);
        world.setTileEntity(0, 100, 0, tile);
        machine = (NyarlathotepsTentacle) tile.getMetaTileEntity();
        require(form().isEmpty(), "controller reload retains full structure");
        require(machine.getModuleWorks().size() == 1, "running task survives controller NBT reload");
        tile.enableWorking();
        Method run = MTEMultiBlockBase.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
        run.setAccessible(true);
        for (int tick = 0; tick < 5000 && !machine.getModuleWorks().isEmpty(); tick++) {
            for (var hatch : machine.mEnergyHatches) ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity())
                .setStoredEU(hatch.getBaseMetaTileEntity().getEUCapacity());
            run.invoke(machine, tile, 100000L + tick);
        }
        require(machine.getModuleWorks().isEmpty(), "reloaded task actually completes");
        int produced = 0;
        for (var bus : machine.mOutputBusses) for (ItemStack output : bus.mInventory)
            if (GTUtility.areStacksEqual(output, source.mOutput)) produced += output.stackSize;
        require(produced == source.mOutput.stackSize, "original physical output delivered exactly once");
        machine.getDataHatches().get(0).setInventorySlotContents(0, null);
        input = machine.mInputBusses.get(0);
        for (int slot = 0; slot < source.mInputs.length; slot++)
            input.setInventorySlotContents(slot, source.mInputs[slot].copy());
        for (int i = 0; i < source.mFluidInputs.length; i++)
            machine.mInputHatches.get(i).mFluid = source.mFluidInputs[i].copy();
        require(machine.checkProcessing().wasSuccessful() && items(input.mInventory) == 0,
            "cached recipe starts without research");
        for (int tick = 0; tick < 5000 && !machine.getModuleWorks().isEmpty(); tick++) {
            for (var hatch : machine.mEnergyHatches) ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity())
                .setStoredEU(hatch.getBaseMetaTileEntity().getEUCapacity());
            run.invoke(machine, tile, 150000L + tick);
        }
        require(machine.getModuleWorks().isEmpty(), "cached task completes");
        circuitAssembly();
        tile.disableWorking();
        report.append("Native recipe: ").append(source.mOutput.getDisplayName()).append(
            "; no research media, funded module task, full NBT reload, cached repeat and exact output verified.\n");
    }

    private void circuitAssembly() throws Exception {
        GTRecipe recipe = BartWorksRecipeMaps.circuitAssemblyLineRecipes.getAllRecipes().stream()
            .filter(candidate -> candidate.mSpecialItems instanceof ItemStack && candidate.mEUt <= TierEU.RECIPE_UHV
                && candidate.mEUt > 0
                && candidate.mDuration > 0
                && candidate.mFluidInputs.length <= 4
                && candidate.mInputs.length <= 16
                && candidate.mOutputs.length == 1)
            .findFirst().orElseThrow(() -> new AssertionError("No native imprinted circuit assembly recipe"));
        machine.setMachineMode(1);
        var input = machine.mInputBusses.get(0);
        for (int slot = 0; slot < input.getSizeInventory(); slot++) input.setInventorySlotContents(slot, null);
        for (var hatch : machine.mInputHatches) hatch.mFluid = null;
        for (int slot = 0; slot < recipe.mInputs.length; slot++)
            input.setInventorySlotContents(slot, recipe.mInputs[slot].copy());
        for (int i = 0; i < recipe.mFluidInputs.length; i++)
            machine.mInputHatches.get(i).mFluid = recipe.mFluidInputs[i].copy();
        machine.setInventorySlotContents(1, null);
        require(machine.checkProcessing().wasSuccessful(), "native circuit assembly starts without imprint");
        Method run = MTEMultiBlockBase.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
        run.setAccessible(true);
        for (int tick = 0; tick < 5000 && !machine.getModuleWorks().isEmpty(); tick++) {
            for (var hatch : machine.mEnergyHatches) ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity())
                .setStoredEU(hatch.getBaseMetaTileEntity().getEUCapacity());
            run.invoke(machine, tile, 200000L + tick);
        }
        require(machine.getModuleWorks().isEmpty(), "circuit task completes without imprint");
        int produced = 0;
        for (var bus : machine.mOutputBusses) for (ItemStack stack : bus.mInventory)
            if (GTUtility.areStacksEqual(stack, recipe.mOutputs[0])) produced += stack.stackSize;
        require(produced == recipe.mOutputs[0].stackSize, "circuit output delivered exactly once");
        report.append("Native circuit assembly without imprint, execution and exact output verified.\n");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !checked || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.theWorld == null) return;
            if (openedMode >= 0) {
                mc.gameSettings.hideGUI = false;
                if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
                ButtonWidget<?> button = (ButtonWidget<?>) named(wrapper.getScreen().getMainPanel(),
                    "gtngModeSelector");
                require(button != null, "GUI exposes the mode selector");
                IntSyncValue mode = wrapper.getScreen().getSyncManager().getMainPSM().findSyncHandler("machineMode",
                    IntSyncValue.class);
                var popup = wrapper.getScreen().getPanelManager().getOpenPanels().stream()
                    .filter(panel -> panel.getName().equals("gtngMachineModes")).findFirst().orElse(null);
                int expected = capturedMode + 1;
                if (openedMode != expected || mode.getIntValue() != expected) {
                    if (popup != null && expected == 1) {
                        require(named(popup, "gtngMode:0") != null && named(popup, "gtngMode:2") == null,
                            "selector contains exactly two modes");
                        ((ButtonWidget<?>) named(popup, "gtngMode:1")).onMousePressed(0);
                    }
                    frames = 0;
                    return;
                }
                if (popup != null) {
                    popup.closeIfOpen();
                    frames = 0;
                    return;
                }
                if (++frames < 50) return;
                ScreenShotHelper.saveScreenshot(output(), "tentacle-mode-" + openedMode + ".png", mc.displayWidth,
                    mc.displayHeight, mc.getFramebuffer());
                capturedMode = openedMode;
                frames = 0;
                if (capturedMode < 1) button.onMousePressed(0);
                else {
                    report.append("Actual MUI2 mode list selects both modes with server/client synchronization.\n");
                    Files.write(new File(output(), "result.txt").toPath(),
                        ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
                    finished = true;
                }
                return;
            }
            if (requestedGui) return;
            if (visualStage != requestedVisualStage) return;
            double dx = targetX - mc.thePlayer.posX, dy = targetY - mc.thePlayer.posY;
            double dz = targetZ - mc.thePlayer.posZ;
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
            mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = yaw;
            mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = pitch;
            mc.gameSettings.clouds = false;
            mc.gameSettings.hideGUI = true;
            if (visualStart == 0) visualStart = System.currentTimeMillis();
            if (++frames < 100 || System.currentTimeMillis() - visualStart < (visualStage == 0 ? 15000 : 5000)) return;
            for (TentacleCasing casing : TentacleCasing.values()) for (int side = 0; side < 6; side++) {
                var icon = casing.getBlock().getIcon(side, casing.getMetadata());
                require(icon != null && !icon.getIconName().contains("missing"), "original icon " + casing);
            }
            boolean active = visualStage == 2 || visualStage == 3;
            checkActiveBlocks(mc.theWorld, active);
            var core = LightHunterCasing.HyperCore;
            require(core.getBlock().getIcon(2, core.getActiveMetadata()) != null, "hyper core active texture");
            TextureAtlasSprite animated = (TextureAtlasSprite) TentacleConnectedIcons.getIcon("hollow_casing", 0);
            require(animated.getIconName().contains("animated/") && animated.getFrameCount() > 2,
                "interpolated CTM sprite loaded in real atlas");
            String[] captures = { "tentacle-structure", "tentacle-idle-close", "tentacle-active-close",
                "tentacle-active", "tentacle-stopped" };
            ScreenShotHelper.saveScreenshot(output(), captures[visualStage] + ".png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            frames = 0;
            visualStart = 0;
            if (visualStage < 4) requestedVisualStage = visualStage + 1;
            else {
                checkActiveBlocks(tile.getWorld(), false);
                report.append(
                    "All 2671 active blocks (154 hyper cores) synchronize idle/running/stopped; interpolated atlas verified. Visual fixture extends one accepted task to 20000 ticks only in the QA save; natural running ticks: ")
                    .append(runningTicks).append(".\n");
                requestedGui = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static IWidget named(IWidget widget, String name) {
        if (name.equals(widget.getName())) return widget;
        for (IWidget child : widget.getChildren()) {
            IWidget result = named(child, name);
            if (result != null) return result;
        }
        return null;
    }

    private static int items(ItemStack[] inventory) {
        int total = 0;
        for (ItemStack stack : inventory) if (stack != null) total += stack.stackSize;
        return total;
    }

    private static List<int[]> slots(String[][] shape) {
        List<int[]> result = new ArrayList<>();
        for (int y = 0; y < shape.length; y++)
            for (int z = 0; z < shape[y].length; z++) for (int x = 0; x < shape[y][z].length(); x++)
                if (shape[y][z].charAt(x) == 'A') result.add(new int[] { x, y, z });
        return result;
    }

    private int[] position(int[] local) {
        int[] result = new int[3];
        machine.getExtendedFacing().getWorldOffset(new int[] { local[0] - 82, local[1] - 23, local[2] }, result);
        return new int[] { result[0], 100 + result[1], result[2] };
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id, EntityPlayerMP player) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.disableWorking();
        return tile;
    }

    private static File output() {
        File output = new File(System.getProperty("gtng.tentacle.output"));
        output.mkdirs();
        return output;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reporting) {
            reporting.printStackTrace();
        }
        finished = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
