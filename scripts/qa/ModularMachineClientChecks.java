package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.Language;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.text.RichText;
import com.cleanroommc.modularui.screen.ClientScreenHandler;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.RichTooltipEvent;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.FluidDisplayWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.RecipeOutputFormatting;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.machines.hatch.modular.ModularHatch;
import com.xyp.gtnotgood.common.machines.hatch.modular.ModuleType;
import com.xyp.gtnotgood.common.machines.hatch.modular.ParameterModuleHatch;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.DimensionalFocusEngravingArray;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.FieldExtruderFactory;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGModularMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiModeMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.MageAssemblerStructure;
import com.xyp.gtnotgood.common.machines.multiblock.structure.SuperconductingElectromagneticStructure;
import com.xyp.gtnotgood.common.machines.processing.ModularProcessingLogic;
import com.xyp.gtnotgood.common.machines.processing.ModuleInputPreflight;
import com.xyp.gtnotgood.common.machines.processing.ModuleWork;
import com.xyp.gtnotgood.common.machines.processing.ModuleWorkDisplay;
import com.xyp.gtnotgood.common.recipe.machine.DimensionalFocusResearch;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeDisplay;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.ISecondaryDescribable;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.AssemblyLineUtils;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTScannerResult;
import gregtech.api.util.GTUtility;
import gregtech.api.util.OverclockCalculator;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.MTEHatchInputME;
import gregtech.loaders.postload.ScannerHandlerLoader;
import tectech.mechanics.dataTransport.QuantumDataPacket;
import tectech.thing.CustomItemList;

@Mod(
    modid = "modularmachineqa",
    name = "Modular Machine QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class ModularMachineClientChecks {

    private final boolean tooltipOnly = Boolean.getBoolean("gtng.modularMachine.tooltipOnly");
    private final boolean outputGuiOnly = Boolean.getBoolean("gtng.modularMachine.outputGuiOnly");
    private final boolean tstCoexistence = Boolean.getBoolean("gtng.modularMachine.tstCoexistence");
    private final boolean maintenanceOnly = Boolean.getBoolean("gtng.modularMachine.maintenanceOnly");
    private final List<ModularHatch> prototypes = new ArrayList<>();
    private final List<ParameterModuleHatch> dynamicModules = new ArrayList<>();
    private final StringBuilder report = new StringBuilder();
    private final List<Rig> rigs = new ArrayList<>();
    private final GTNGItemList[] tooltipControllers = { GTNGItemList.FieldExtruderFactory,
        GTNGItemList.DimensionalFocusEngravingArray, GTNGItemList.KerrNewmanHomogenizer,
        GTNGItemList.SuperParticleCollider, GTNGItemList.MageAssembler,
        GTNGItemList.SuperconductingElectromagneticFactory, GTNGItemList.HeartOfTheUniverse };
    private final String[] tooltipSlugs = { "field-extruder", "dimensional-focus", "kerr-newman", "particle-collider",
        "mage-assembler", "electromagnetic-factory", "heart-of-the-universe" };
    private Item fixture;
    private Fluid maintenanceFluid;
    private boolean started;
    private boolean recipesChecked;
    private volatile boolean finished, serverDone;
    private int clientTicks, serverTicks, rigIndex, moduleIndex;
    private Rig rig;
    private Object builder;
    private BaseMetaTileEntity base;
    private GTNGModularMachineBase<?> machine;
    private List<SavedBlock> slots;
    private long controlledTick = 1_000_000;
    private volatile ParameterModuleHatch guiHatch;
    private volatile int guiIndex = -1, guiPhase;
    private volatile GuiTarget guiTarget;
    private volatile int clientGuiReady = -1, clientGuiTileId = -1;
    private int guiRequestedAt;
    private int guiFrames;
    private int tooltipStage = -1, tooltipFrames;
    private String tooltipLocale;
    private boolean iconsShown;
    private volatile int outputGuiPhase;
    private int outputGuiFrames;
    private volatile List<NBTTagCompound> outputGuiExpected = Collections.emptyList();
    private volatile List<NBTTagCompound> outputGuiTaskExpected = Collections.emptyList();
    private List<IWidget> outputGuiStableWidgets;
    private ListWidget<?, ?> outputGuiTerminal;
    private int outputGuiScroll;
    private String outputGuiControllerProgress;
    private volatile int outputGuiAdvanceMinimum;
    private int outputGuiHoverStage, outputGuiHoverFrames;
    private IWidget outputGuiHoverText;
    private NBTTagCompound outputGuiHoverOutput;
    private boolean outputGuiHoverDone;
    private boolean outputGuiTooltipRendered;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) throws Exception {
        if (!Boolean.getBoolean("gtng.modularMachine.qa")) return;
        fixture = new Item().setHasSubtypes(true).setUnlocalizedName("modularFixture");
        GameRegistry.registerItem(fixture, "modularFixture");
        if (maintenanceOnly) {
            maintenanceFluid = new Fluid("modular_maintenance_qa");
            require(FluidRegistry.registerFluid(maintenanceFluid), "Unique maintenance regression fluid registers");
        }
        for (IMetaTileEntity candidate : GregTechAPI.METATILEENTITIES) if (candidate instanceof ModularHatch hatch) {
            prototypes.add(hatch);
            if (hatch instanceof ParameterModuleHatch parameter && parameter.isDynamic()) dynamicModules.add(parameter);
        }
        require(prototypes.size() == 46 && dynamicModules.size() == 16,
            "All 46 modules and 16 dynamic modules register");
        rigs.add(new Rig("field-extruder", new FieldExtruderClientChecks(), 'A'));
        rigs.add(new Rig("dimensional-focus", new DimensionalFocusClientChecks(), 'G'));
        rigs.add(new Rig("kerr-newman", new KerrNewmanClientChecks(), 'K'));
        rigs.add(new Rig("particle-collider", new SuperParticleColliderClientChecks(), 'b'));
        rigs.add(assemblyRig(false));
        rigs.add(assemblyRig(true));
        FMLCommonHandler.instance().bus().register(this);
        if (outputGuiOnly) MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks < 24000, "Modular client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0F);
            mc.gameSettings.guiScale = tooltipStage >= 0 ? 1 : 2;
            mc.gameSettings.limitFramerate = 60;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("modular-machine-qa-" + System.currentTimeMillis(), "Modular Machine QA",
                    new WorldSettings(733L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (outputGuiOnly) {
                clientOutputGui(mc);
                return;
            }
            if (!serverDone) return;
            if (tstCoexistence || maintenanceOnly) {
                if (maintenanceOnly) {
                    verifyMaintenanceTooltips();
                    Files.write(new File(output(), "result.txt").toPath(),
                        ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
                }
                finished = true;
                mc.shutdown();
                return;
            }
            GuiTarget target = guiTarget;
            if (guiPhase == -1 && target != null && target.index == guiIndex && mc.theWorld != null) {
                clientGuiTileId = -1;
                if (
                    mc.theWorld.blockExists(target.x, target.y, target.z)
                        && mc.theWorld.getTileEntity(target.x, target.y, target.z) instanceof BaseMetaTileEntity tile
                ) {
                    clientGuiTileId = tile.getMetaTileID();
                    if (
                        !tile.isInvalid() && tile.getMetaTileID() == target.id
                            && tile.getMetaTileEntity() instanceof ParameterModuleHatch parameter
                            && parameter.isDynamic()
                            && mc.thePlayer != null
                            && mc.thePlayer.getDistanceSq(target.x, target.y, target.z) < 64
                    ) {
                        clientGuiReady = target.index;
                    }
                }
            }
            if (
                guiIndex >= 0 && guiIndex < dynamicModules.size()
                    && guiPhase >= 0
                    && mc.currentScreen instanceof GuiContainerWrapper wrapper
            ) {
                List<TextFieldWidget> fields = new ArrayList<>();
                collectFields(wrapper.getScreen().getMainPanel(), fields);
                require(fields.size() == 1, "Dynamic module has one synchronized parameter field");
                TextFieldWidget field = fields.get(0);
                if (guiPhase == 0 && ++guiFrames >= 20) {
                    field.setText(Integer.toString(guiHatch.getMaximumParameter()));
                    field.onRemoveFocus(wrapper.getScreen().getContext());
                    guiPhase = 1;
                    guiFrames = 0;
                } else if (guiPhase == 2 && field.getText().replaceAll("[^0-9]", "").equals("2") && ++guiFrames >= 20) {
                    capture("dynamic-module-" + guiHatch.getStackForm(1).getItemDamage());
                    guiPhase = 3;
                    guiFrames = 0;
                }
            }
            if (guiIndex == dynamicModules.size() && !iconsShown) {
                iconsShown = true;
                mc.displayGuiScreen(new ModuleIconsScreen(prototypes));
                guiFrames = 0;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            require(++serverTicks < 16000, "Modular server timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks < 40) return;
            if (outputGuiOnly) {
                serverOutputGui(player);
                return;
            }
            if (maintenanceOnly) {
                if (!serverDone) {
                    buildNextRig(player);
                    verifyMaintenanceFree();
                    rig = null;
                    if (++rigIndex == rigs.size()) {
                        serverDone = true;
                    }
                }
                return;
            }
            if (tstCoexistence) {
                if (!serverDone) {
                    report.append("仅 TST 共存配方、研究和数据棒验证，未重跑结构、加工、GUI 或 tooltip 截图。\n");
                    verifyModuleRecipes();
                    verifyTstCoexistence();
                    Files.write(new File(output(), "result.txt").toPath(),
                        ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
                    serverDone = true;
                }
                return;
            }
            if (tooltipOnly) {
                if (!serverDone) {
                    report.append("仅 tooltip 验证，未重跑加工/GUI。\n");
                    guiIndex = dynamicModules.size();
                    serverDone = true;
                }
                return;
            }
            if (!recipesChecked) {
                verifyModuleRecipes();
                recipesChecked = true;
            }
            if (!serverDone) {
                if (rig == null) buildNextRig(player);
                if (moduleIndex < prototypes.size()) {
                    verifyModule(prototypes.get(moduleIndex++));
                    return;
                }
                verifyCombinedParameters();
                verifyPhysicalEnergyRequired();
                verifyUnlimitedEnergyHatches();
                if (rig.slug.equals("field-extruder")) verifyFieldProcessing();
                if (rig.slug.equals("mage-assembler")) verifyScheduler();
                if (rig.slug.equals("dimensional-focus")) verifyFocusGates();
                report.append(rig.slug).append(": default 2/4 OC, all 46 physical modules, duplicate-type limits, ")
                    .append("combined parameter effects and physical energy requirement pass.\n");
                rig = null;
                if (++rigIndex == rigs.size()) {
                    serverDone = true;
                    guiIndex = 0;
                    openModuleGui(player);
                }
                return;
            }
            if (guiIndex < dynamicModules.size()) {
                if (guiPhase == -1) {
                    require(serverTicks - guiRequestedAt <= 1200,
                        "Waiting for synchronized client module tile: expected=" + guiTarget.id
                            + ", actual="
                            + clientGuiTileId
                            + ", index="
                            + guiIndex);
                    if (clientGuiReady == guiIndex) {
                        BaseMetaTileEntity tile = (BaseMetaTileEntity) guiHatch.getBaseMetaTileEntity();
                        require(tile.getMetaTileID() == guiTarget.id && tile.getMetaTileEntity() == guiHatch,
                            "Server module still matches the acknowledged client tile");
                        guiHatch.onRightclick(tile, player);
                        guiPhase = 0;
                    }
                } else if (guiPhase == 1 && guiHatch.getParameter() == guiHatch.getMaximumParameter()) {
                    NBTTagCompound saved = new NBTTagCompound();
                    guiHatch.saveNBTData(saved);
                    guiHatch.setParameter(1);
                    guiHatch.loadNBTData(saved);
                    require(guiHatch.getParameter() == guiHatch.getMaximumParameter(), "Dynamic parameter persists");
                    guiHatch.setParameter(2);
                    guiPhase = 2;
                } else if (guiPhase == 3) {
                    report.append("Dynamic GUI C2S/S2C and NBT: ").append(guiHatch.getStackForm(1).getItemDamage())
                        .append('\n');
                    player.closeScreen();
                    if (++guiIndex < dynamicModules.size()) openModuleGui(player);
                }
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void serverOutputGui(EntityPlayerMP player) throws Exception {
        if (outputGuiPhase == 0) {
            rigIndex = 5;
            buildNextRig(player);
            ((GTNGMultiModeMachineBase<?>) machine).setMachineMode(2);
            machine.setPowerPanelMaxParallel(1);
            invoke(builder, "setEnergy", new Class<?>[] { ItemList.class }, ItemList.Hatch_Energy_LV);
            install(0, core(CoreType.Normal));
            invoke(builder, "verifyStructure", new Class<?>[0]);
            resetWork();
            verifyLegacyOutputTask();
            verifyLargeOutputLedger();
            for (int index = 0; index < 2; index++) GTRecipeBuilder.builder().itemInputs(stack(200 + index, 1))
                .itemOutputs(new ItemStack(Items.coal, 8)).fluidOutputs(new FluidStack(FluidRegistry.WATER, 1600))
                .duration(2000 * (index + 1)).eut(32).addTo(machine.getRecipeMap());
            seed(200, 1);
            require(machine.checkProcessing().wasSuccessful(), "Output GUI controller recipe starts");
            seed(201, 1);
            tick();
            require(machine.getModuleWorks().size() == 2, "Output GUI has two independent execution tasks");
            require(machine.getModuleWorks().stream().map(work -> work.duration).distinct().count() == 2,
                "Output GUI task durations differ");
            for (ModuleWork work : machine.getModuleWorks()) work.advance(work.core.equals("controller") ? 240 : 80);
            require(machine.mOutputItems == null && machine.mOutputFluids == null,
                "GUI snapshots never populate the native output buffers");
            base.enableWorking();
            refreshOutputGuiExpected();
            require(
                outputGuiExpected.size() == 4
                    && outputGuiExpected.stream().allMatch(row -> row.getInteger("ticks") > 0),
                "Both item and fluid outputs carry each task's real duration");
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            player.playerNetServerHandler.setPlayerLocation(base.getXCoord(), base.getYCoord(), base.getZCoord() - 3, 0,
                0);
            base.issueTileUpdate();
            guiTarget = new GuiTarget(0, base.getMetaTileID(), base.getXCoord(), base.getYCoord(), base.getZCoord());
            clientGuiReady = -1;
            guiRequestedAt = serverTicks;
            base.enableWorking();
            outputGuiPhase = 1;
        }
        if (
            outputGuiPhase <= 2 || outputGuiPhase == 6
                || outputGuiPhase == 11
                || outputGuiPhase == 12
                || outputGuiPhase == 20
        ) {
            for (var hatch : machine.mEnergyHatches) {
                BaseMetaTileEntity energy = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
                energy.setStoredEU(energy.getEUCapacity());
            }
        }
        if (outputGuiPhase == 1) {
            require(serverTicks - guiRequestedAt < 1200, "Controller output GUI client tile arrives");
            if (clientGuiReady == 0 && machine.getmStartUpCheck() <= 0) {
                machine.onRightclick(base, player);
                outputGuiPhase = 2;
            }
        } else if (outputGuiPhase == 11) {
            ModuleWork controller = machine.getModuleWorks().stream().filter(work -> work.core.equals("controller"))
                .findFirst().get();
            controller.advance(200);
            outputGuiAdvanceMinimum = controller.progress();
            refreshOutputGuiExpected();
            outputGuiPhase = 12;
        } else if (outputGuiPhase == 3) {
            base.disableWorking();
            invoke(machine, "runMachine", new Class<?>[] { IGregTechTileEntity.class, long.class }, base,
                controlledTick++);
            refreshOutputGuiExpected();
            require(outputGuiExpected.stream().allMatch(row -> row.getInteger("ticks") == 0),
                "Paused tasks retain their quantities and show zero rates");
            outputGuiPhase = 4;
        } else if (outputGuiPhase == 5) {
            slots.get(0).restore();
            invoke(builder, "verifyStructure", new Class<?>[0]);
            tick();
            base.enableWorking();
            refreshOutputGuiExpected();
            require(outputGuiExpected.stream().filter(row -> row.getInteger("ticks") == 0).count() == 2,
                "Only detached core outputs have zero rates while the controller continues");
            outputGuiPhase = 6;
        } else if (outputGuiPhase == 7) {
            install(0, core(CoreType.Normal));
            invoke(builder, "verifyStructure", new Class<?>[0]);
            for (var bus : machine.mOutputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++)
                bus.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
            for (ModuleWork work : machine.getModuleWorks()) work.advance(Integer.MAX_VALUE);
            tick();
            require(
                machine.getModuleWorks().size() == 2
                    && machine.getModuleWorks().stream().allMatch(ModuleWork::finished),
                "Completed outputs remain behind protected full storage");
            base.enableWorking();
            refreshOutputGuiExpected();
            require(
                outputGuiExpected.size() == 4
                    && outputGuiExpected.stream().allMatch(row -> row.getInteger("ticks") == 0),
                "Completed tasks keep remaining item and fluid amounts with zero rates");
            outputGuiPhase = 8;
        } else if (outputGuiPhase == 13) {
            base.disableWorking();
            invoke(machine, "runMachine", new Class<?>[] { IGregTechTileEntity.class, long.class }, base,
                controlledTick++);
            refreshOutputGuiExpected();
            require(
                outputGuiTaskExpected.stream().filter(task -> task.getInteger("duration") > 0)
                    .allMatch(task -> task.getBoolean("finished") && task.getBoolean("paused")),
                "Completed tasks retain 100 percent progress when paused");
            outputGuiPhase = 14;
        } else if (outputGuiPhase == 9) {
            resetWork();
            refreshOutputGuiExpected();
            require(outputGuiExpected.isEmpty(), "Cleared task ledger has no stale output snapshots");
            outputGuiPhase = 10;
        }
    }

    private void refreshOutputGuiExpected() {
        outputGuiExpected = machine.getDisplayedModuleOutputs(128);
        outputGuiTaskExpected = machine.getDisplayedModuleTasks(128);
    }

    private void verifyLegacyOutputTask() throws Exception {
        NBTTagCompound original = new NBTTagCompound();
        machine.saveNBTData(original);
        try {
            machine.mOutputItems = new ItemStack[] { new ItemStack(Items.coal, 3) };
            machine.mOutputFluids = new FluidStack[] { new FluidStack(FluidRegistry.WATER, 1000) };
            machine.mProgresstime = 37;
            machine.mMaxProgresstime = 80;
            NBTTagCompound legacy = new NBTTagCompound();
            machine.saveNBTData(legacy);
            legacy.setBoolean("gtngModuleLegacyRun", true);
            machine.loadNBTData(legacy);
            base.enableWorking();
            List<NBTTagCompound> tasks = machine.getDisplayedModuleTasks(128);
            require(
                tasks.stream().filter(task -> task.getInteger("duration") > 0).count() == 1
                    && tasks.get(0).getString("core").equals("controller")
                    && tasks.get(0).getInteger("duration") == 80
                    && tasks.get(0).getTagList("outputs", 10).tagCount() == 2,
                "A restored native legacy recipe is represented by one controller group with both outputs");
            require(machine.getDisplayedModuleProgress().getInteger("controller") == 37,
                "Legacy controller task retains its native progress");
            NBTTagList outputs = tasks.get(0).getTagList("outputs", 10);
            require(
                outputs.getCompoundTagAt(0).hasKey("item")
                    && outputs.getCompoundTagAt(0).getString("amount").equals("3")
                    && outputs.getCompoundTagAt(1).hasKey("fluid")
                    && outputs.getCompoundTagAt(1).getString("amount").equals("1000"),
                "Legacy controller groups preserve exact native item and fluid quantities");
            require(!machine.showRecipeTextInGUI(), "Modular task groups replace the ordinary global progress widget");
            report.append(
                "Legacy native recipe: controller group keeps duration, item/fluid outputs and progress without a duplicate global line.\n");
        } finally {
            base.disableWorking();
            machine.loadNBTData(original);
            resetWork();
        }
    }

    private void clientOutputGui(Minecraft mc) throws Exception {
        GuiTarget target = guiTarget;
        if (
            outputGuiPhase == 1 && target != null
                && mc.theWorld != null
                && mc.thePlayer != null
                && mc.theWorld.getTileEntity(target.x, target.y, target.z) instanceof BaseMetaTileEntity tile
                && tile.getMetaTileID() == target.id
                && tile.getMetaTileEntity() instanceof GTNGModularMachineBase<?>
                && mc.thePlayer.getDistanceSq(target.x, target.y, target.z) < 64
        ) clientGuiReady = 0;
        int phase = outputGuiPhase;
        if (phase < 2 || phase % 2 != 0 || !(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
        List<IWidget> widgets = new ArrayList<>();
        collectWidgets(wrapper.getScreen().getMainPanel(), widgets);
        if (phase == 20) {
            clientOutputGuiTooltips(widgets);
            return;
        }
        List<String> text = widgets.stream().filter(widget -> widget instanceof TextWidget<?>)
            .map(widget -> tooltipText(((TextWidget<?>) widget).getKey().getFormatted())).collect(Collectors.toList());
        List<NBTTagCompound> expected = outputGuiExpected;
        GenericSyncValue<?, ?> progressSync = wrapper.getScreen().getSyncManager().getMainPSM()
            .findSyncHandler("gtngModuleProgress", GenericSyncValue.class);
        NBTTagCompound progress = (NBTTagCompound) progressSync.getValue();
        long itemIcons = widgets.stream().filter(widget -> widget instanceof ItemDisplayWidget).count();
        long fluidIcons = widgets.stream().filter(widget -> widget instanceof FluidDisplayWidget).count();
        boolean matches = itemIcons == expected.stream().filter(row -> row.hasKey("item")).count()
            && fluidIcons == expected.stream().filter(row -> row.hasKey("fluid")).count();
        Map<String, Integer> expectedLines = new HashMap<>();
        for (NBTTagCompound row : expected)
            expectedLines.merge(tooltipText(RecipeOutputFormatting.amountLine(row)), 1, Integer::sum);
        for (Map.Entry<String, Integer> entry : expectedLines.entrySet())
            matches &= text.stream().filter(entry.getKey()::equals).count() >= entry.getValue();
        matches &= matchesModuleTaskGroups(widgets, progress);
        if (phase == 2) {
            String attached = outputGuiTaskExpected.stream().map(task -> task.getString("core"))
                .filter(core -> !core.equals("controller")).findFirst().get();
            matches &= progress.getInteger("controller") > progress.getInteger(attached) + 100;
        }
        if (phase == 12) matches &= progress.getInteger("controller") >= outputGuiAdvanceMinimum;
        if (!matches) {
            outputGuiFrames = 0;
            require(serverTicks - guiRequestedAt < 1800,
                "Output GUI rows synchronize: phase=" + phase
                    + ", items="
                    + itemIcons
                    + ", fluids="
                    + fluidIcons
                    + ", expected="
                    + expectedLines
                    + ", task groups="
                    + outputGuiTaskExpected
                    + ", text="
                    + text);
            return;
        }
        if (++outputGuiFrames < 20) return;
        if (phase == 12) {
            require(widgets.containsAll(outputGuiStableWidgets),
                "Progress updates retain existing item, fluid and progress widget instances");
            require(outputGuiTerminal.getScrollData().getScroll() == outputGuiScroll,
                "Progress updates preserve the terminal scroll offset");
            String controllerProgress = moduleTaskProgressText(widgets, "controller");
            require(!controllerProgress.equals(outputGuiControllerProgress),
                "The controller progress text changes in place after advancing only its task");
        }
        String state = switch (phase) {
            case 2 -> "running";
            case 12 -> "progress-advanced";
            case 4 -> "paused";
            case 6 -> "detached-core";
            case 8 -> "completed-output-full";
            case 14 -> "completed-paused";
            default -> "cleared";
        };
        capture("controller-output-" + state);
        report.append("Real controller GUI ").append(state).append(": item icons=").append(itemIcons)
            .append(", fluid icons=").append(fluidIcons).append(", amount/rate rows=").append(expectedLines)
            .append(", task progress=").append(progress).append('\n');
        outputGuiFrames = 0;
        if (phase == 10) {
            Files.write(new File(output(), "result.txt").toPath(),
                ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
            finished = true;
            mc.shutdown();
        } else if (phase == 2) {
            outputGuiControllerProgress = moduleTaskProgressText(widgets, "controller");
            outputGuiStableWidgets = widgets.stream()
                .filter(widget -> widget instanceof ItemDisplayWidget || widget instanceof FluidDisplayWidget
                    || widget instanceof TextWidget<?> && isProgressText((TextWidget<?>) widget))
                .collect(Collectors.toList());
            outputGuiTerminal = widgets.stream().filter(widget -> widget.isName("gtngModuleTerminal"))
                .map(widget -> (ListWidget<?, ?>) widget).findFirst().get();
            outputGuiTerminal.getScrollData().scrollTo(outputGuiTerminal.getScrollArea(), 5);
            outputGuiScroll = outputGuiTerminal.getScrollData().getScroll();
            require(outputGuiScroll > 0, "Two independently grouped task sections require terminal scrolling");
            outputGuiPhase = 11;
        } else if (phase == 12) {
            outputGuiTerminal.getScrollData().scrollTo(outputGuiTerminal.getScrollArea(), 0);
            report.append("Independent progress updates preserve output/progress widgets and terminal scrolling.\n");
            outputGuiHoverStage = outputGuiHoverFrames = 0;
            outputGuiPhase = 20;
        } else if (phase == 8) {
            outputGuiPhase = 13;
        } else if (phase == 14) {
            outputGuiPhase = 9;
        } else outputGuiPhase = phase + 1;
    }

    private boolean matchesModuleTaskGroups(List<IWidget> widgets, NBTTagCompound progress) {
        List<NBTTagCompound> tasks = outputGuiTaskExpected;
        long groups = widgets.stream()
            .filter(widget -> widget.getName() != null && widget.getName().startsWith("gtngModuleTask:")).count();
        if (groups != tasks.size()) return false;
        long progressLines = widgets.stream()
            .filter(widget -> widget instanceof TextWidget<?> && widget.areAncestorsEnabled()
                && isProgressText((TextWidget<?>) widget))
            .count();
        if (progressLines != tasks.stream().filter(task -> task.getInteger("duration") > 0).count()) return false;
        for (NBTTagCompound task : tasks) {
            String core = task.getString("core");
            IWidget group = widgets.stream().filter(widget -> widget.isName("gtngModuleTask:" + core)).findFirst()
                .orElse(null);
            if (group == null) return false;
            List<IWidget> children = new ArrayList<>();
            collectWidgets(group, children);
            List<String> lines = children.stream().filter(widget -> widget instanceof TextWidget<?>)
                .map(widget -> tooltipText(((TextWidget<?>) widget).getKey().getFormatted()))
                .collect(Collectors.toList());
            String label = core.equals("controller") ? StatCollector.translateToLocal("gtng.modular.gui.controller")
                : StatCollector.translateToLocalFormatted("gtng.modular.gui.core", task.getInteger("index"),
                    StatCollector
                        .translateToLocal("gtng.modular.gui." + task.getString("type").toLowerCase(Locale.ROOT)));
            if (!lines.contains(tooltipText(label))) return false;
            int duration = task.getInteger("duration");
            if (duration > 0) {
                String expectedProgress = tooltipText(ModuleWorkDisplay.progress(progress.getInteger(core), duration));
                if (!lines.contains(expectedProgress)) return false;
            }
            String status = duration <= 0 ? "gtng.modular.gui.idle"
                : task.getBoolean("paused") ? "gtng.module.core.paused"
                    : task.getBoolean("finished") ? "gtng.modular.gui.waiting_output" : "";
            if (
                !status.isEmpty() && lines.stream()
                    .noneMatch(line -> line.contains(tooltipText(StatCollector.translateToLocal(status))))
            ) return false;
            NBTTagList outputs = task.getTagList("outputs", 10);
            long expectedItems = 0, expectedFluids = 0;
            for (int index = 0; index < outputs.tagCount(); index++) {
                NBTTagCompound output = outputs.getCompoundTagAt(index);
                if (output.hasKey("item")) expectedItems++;
                else expectedFluids++;
                if (!lines.contains(tooltipText(RecipeOutputFormatting.amountLine(output)))) return false;
            }
            if (
                children.stream().filter(widget -> widget instanceof ItemDisplayWidget).count() != expectedItems
                    || children.stream().filter(widget -> widget instanceof FluidDisplayWidget).count()
                        != expectedFluids
            ) return false;
        }
        return true;
    }

    private static String moduleTaskProgressText(List<IWidget> widgets, String core) {
        IWidget group = widgets.stream().filter(widget -> widget.isName("gtngModuleTask:" + core)).findFirst().get();
        List<IWidget> children = new ArrayList<>();
        collectWidgets(group, children);
        return children.stream().filter(widget -> widget.isName("gtngModuleProgress:" + core))
            .map(widget -> tooltipText(((TextWidget<?>) widget).getKey().getFormatted())).findFirst().get();
    }

    private static boolean isProgressText(TextWidget<?> widget) {
        String pattern = StatCollector.translateToLocal("GT5U.gui.text.progress_recipe");
        String prefix = tooltipText(pattern.substring(0, pattern.indexOf('%')));
        return tooltipText(widget.getKey().getFormatted()).startsWith(prefix);
    }

    private void clientOutputGuiTooltips(List<IWidget> widgets) {
        if (outputGuiHoverStage >= 5) {
            outputGuiHoverText = null;
            outputGuiHoverOutput = null;
            outputGuiTerminal.getScrollData().scrollTo(outputGuiTerminal.getScrollArea(), 0);
            outputGuiPhase = 3;
            return;
        }
        if (outputGuiHoverStage == 2) {
            outputGuiTerminal.getScrollData().scrollTo(outputGuiTerminal.getScrollArea(), Integer.MAX_VALUE);
            if (++outputGuiHoverFrames < 20) return;
            String core = outputGuiTaskExpected.stream().map(task -> task.getString("core"))
                .filter(name -> !name.equals("controller")).findFirst().get();
            IWidget progress = widgets.stream().filter(widget -> widget.isName("gtngModuleProgress:" + core))
                .findFirst().get();
            int progressY = progress.getArea().y - outputGuiTerminal.getScrollData().getScroll();
            require(
                progressY >= outputGuiTerminal.getArea().y
                    && progressY + progress.getArea().height <= outputGuiTerminal.getArea().ey(),
                "Scrolled attached-core progress is visible together with its output rows");
            capture("controller-output-attached-core-scrolled");
            report.append("Scrolled attached-core group visibly renders its own progress and native output rows.\n");
            outputGuiHoverFrames = 0;
            outputGuiHoverStage++;
            return;
        }
        if (outputGuiHoverDone) {
            outputGuiHoverDone = false;
            outputGuiHoverText = null;
            outputGuiHoverOutput = null;
            outputGuiHoverFrames = 0;
            outputGuiHoverStage++;
            return;
        }
        if (outputGuiHoverText != null) return;
        boolean controller = outputGuiHoverStage < 2;
        boolean fluid = outputGuiHoverStage == 1 || outputGuiHoverStage == 4;
        NBTTagCompound task = outputGuiTaskExpected.stream()
            .filter(candidate -> candidate.getString("core").equals("controller") == controller).findFirst().get();
        NBTTagList outputs = task.getTagList("outputs", 10);
        for (int index = 0; index < outputs.tagCount(); index++) {
            NBTTagCompound row = outputs.getCompoundTagAt(index);
            if (row.hasKey("fluid") == fluid) {
                outputGuiHoverOutput = row;
                break;
            }
        }
        require(outputGuiHoverOutput != null, "The hovered task contains the requested output type");
        IWidget group = widgets.stream().filter(widget -> widget.isName("gtngModuleTask:" + task.getString("core")))
            .findFirst().get();
        List<IWidget> children = new ArrayList<>();
        collectWidgets(group, children);
        outputGuiHoverText = children.stream().filter(widget -> widget.isName("gtngRecipeOutputText"))
            .filter(widget -> widget.getParent().getChildren().stream().anyMatch(
                sibling -> fluid ? sibling instanceof FluidDisplayWidget : sibling instanceof ItemDisplayWidget))
            .findFirst().get();
        outputGuiTerminal.getScrollData().scrollTo(outputGuiTerminal.getScrollArea(),
            controller ? 0 : Integer.MAX_VALUE);
    }

    @SubscribeEvent
    public void outputTooltipDrawn(RichTooltipEvent.PostText event) {
        if (!outputGuiOnly || outputGuiHoverOutput == null || !(event.getTooltip() instanceof RichText text)) return;
        String actual = tooltipText(String.join("\n", text.getAsStrings()));
        if (actual.equals(tooltipText(RecipeOutputFormatting.tooltip(outputGuiHoverOutput)))) {
            require(event.getWidth() > 0 && event.getHeight() > 0, "Native rich tooltip renders a nonempty rectangle");
            outputGuiTooltipRendered = true;
        }
    }

    private void renderOutputGuiTooltip(float partialTicks) {
        if (
            outputGuiPhase != 20 || outputGuiHoverText == null
                || outputGuiHoverDone
                || !(Minecraft.getMinecraft().currentScreen instanceof GuiContainerWrapper wrapper)
        ) return;
        RichTooltip tooltip = ((Widget<?>) outputGuiHoverText).getTooltip();
        require(tooltip != null, "Native output text column exposes its rich tooltip");
        tooltip.buildTooltip();
        String actual = String.join("\n", ((RichText) tooltip.getRichText()).getAsStrings());
        String expected = RecipeOutputFormatting.tooltip(outputGuiHoverOutput);
        require(tooltipText(actual).equals(tooltipText(expected)),
            "Actual native tooltip retains the output name, exact quantity and all five time scales for its own core");
        require(actual.split("\\R").length == 7,
            "Native output tooltip contains its title and six quantity/rate lines");
        int mouseX = outputGuiHoverText.getArea().x + 3;
        int mouseY = outputGuiHoverText.getArea().y + 3 - outputGuiTerminal.getScrollData().getScroll();
        require(mouseY >= outputGuiTerminal.getArea().y && mouseY < outputGuiTerminal.getArea().ey(),
            "Hovered native output text is inside the visible terminal viewport");
        outputGuiTooltipRendered = false;
        wrapper.getScreen().getContext().updateState(mouseX, mouseY, partialTicks);
        wrapper.getScreen().onFrameUpdate();
        ClientScreenHandler.drawScreen(wrapper.getScreen(), wrapper, mouseX, mouseY, partialTicks);
        boolean hovered = outputGuiHoverText.isHovering();
        if (!outputGuiTooltipRendered) tooltip.draw(wrapper.getScreen().getContext());
        require(outputGuiTooltipRendered, "The actual widget tooltip completes the native rich-tooltip draw event");
        if (++outputGuiHoverFrames < 20) return;
        String core = outputGuiHoverStage < 2 ? "controller" : "attached-core";
        String kind = outputGuiHoverOutput.hasKey("fluid") ? "fluid" : "item";
        capture("controller-output-" + core + "-" + kind + "-tooltip");
        report.append("Native output rich tooltip rendered: ").append(core).append(' ').append(kind).append(", hover=")
            .append(hovered).append(", duration=").append(outputGuiHoverOutput.getInteger("ticks")).append(", text=")
            .append(tooltipText(actual)).append('\n');
        outputGuiHoverDone = true;
    }

    private void verifyLargeOutputLedger() {
        ItemStack[] items = new ItemStack[8];
        FluidStack[] fluids = new FluidStack[8];
        Arrays.setAll(items, index -> new ItemStack(Items.coal, Integer.MAX_VALUE));
        Arrays.setAll(fluids, index -> new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE));
        WirelessOutputs outputs = new WirelessOutputs(items, fluids);
        BigInteger initial = new BigInteger("17179869176");
        BigInteger deliveredItems = BigInteger.ZERO;
        BigInteger deliveredFluids = BigInteger.ZERO;
        try {
            clearLargeOutputStorage();
            require(
                largeOutputRemaining(outputs, false).equals(initial)
                    && largeOutputRemaining(outputs, true).equals(initial),
                "Eight repeated maximum stacks retain 17,179,869,176 items and liters in the ledger");
            List<NBTTagCompound> rows = new ArrayList<>();
            WirelessRecipeDisplay.appendRows(rows, outputs, 20, 128);
            require(
                rows.size() == 2 && rows.stream()
                    .allMatch(row -> row.getString("amount").equals(initial.toString())
                        && !WirelessRecipeDisplay.rate(row, false).contains("-")),
                "Large repeated item and fluid rows retain positive exact amounts and rates");
            for (int attempt = 0; attempt < 3; attempt++) {
                NBTTagCompound saved = outputs.save();
                outputs = WirelessOutputs.load(saved);
                require(saved.equals(outputs.save()), "Oversized output ledger survives native NBT restoration");
                require(!outputs.flush(machine), "A small physical output cannot accept the entire oversized ledger");
                BigInteger storedItems = largeOutputStored(false);
                BigInteger storedFluids = largeOutputStored(true);
                require(storedItems.signum() > 0 && storedFluids.signum() > 0,
                    "Native output transactions commit a positive bounded item/fluid slice");
                deliveredItems = deliveredItems.add(storedItems);
                deliveredFluids = deliveredFluids.add(storedFluids);
                require(
                    largeOutputRemaining(outputs, false).add(deliveredItems).equals(initial)
                        && largeOutputRemaining(outputs, true).add(deliveredFluids).equals(initial),
                    "Measured physical delivery plus oversized remaining quantities exactly conserves both outputs");
                clearLargeOutputStorage();
            }
            for (var bus : machine.mOutputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++)
                bus.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
            for (var hatch : machine.mOutputHatches)
                hatch.setFillableStack(new FluidStack(FluidRegistry.WATER, hatch.getCapacity()));
            NBTTagCompound blocked = outputs.save();
            require(!outputs.flush(machine) && blocked.equals(outputs.save()),
                "Full native output storage stalls without changing the oversized remaining ledger");
            report.append("Large output ledger: 8 duplicate Integer.MAX_VALUE item and fluid stacks each; ")
                .append("initial per type=").append(initial).append(", physical items delivered=")
                .append(deliveredItems).append(", physical fluid delivered=").append(deliveredFluids).append(
                    "; 3 exact native partial transactions, NBT reload, positive GUI rows and full-output stall pass.\n");
        } finally {
            clearLargeOutputStorage();
        }
    }

    private static BigInteger largeOutputRemaining(WirelessOutputs outputs, boolean fluid) {
        NBTTagList entries = outputs.save().getTagList(fluid ? "fluids" : "items", 10);
        BigInteger sum = BigInteger.ZERO;
        for (int index = 0; index < entries.tagCount(); index++) {
            BigInteger amount = new BigInteger(
                entries.getCompoundTagAt(index).getString(fluid ? "bigAmount" : "amount"));
            require(amount.signum() >= 0, "Oversized output ledger never contains a negative quantity");
            sum = sum.add(amount);
        }
        return sum;
    }

    private BigInteger largeOutputStored(boolean fluid) {
        BigInteger sum = BigInteger.ZERO;
        if (fluid) {
            for (var hatch : machine.mOutputHatches) {
                FluidStack stored = hatch.getFillableStack();
                if (stored == null) continue;
                require(stored.getFluid() == FluidRegistry.WATER && stored.amount >= 0,
                    "Measured native fluid output is nonnegative water");
                sum = sum.add(BigInteger.valueOf(stored.amount));
            }
        } else {
            for (var bus : machine.mOutputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
                ItemStack stored = bus.getStackInSlot(slot);
                if (stored == null) continue;
                require(stored.getItem() == Items.coal && stored.stackSize >= 0,
                    "Measured native item output is nonnegative coal");
                sum = sum.add(BigInteger.valueOf(stored.stackSize));
            }
        }
        return sum;
    }

    private void clearLargeOutputStorage() {
        for (var bus : machine.mOutputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (var hatch : machine.mOutputHatches) hatch.setFillableStack(null);
    }

    private static void collectWidgets(IWidget widget, List<IWidget> widgets) {
        widgets.add(widget);
        for (IWidget child : widget.getChildren()) collectWidgets(child, widgets);
    }

    private void verifyModuleRecipes() {
        for (ModularHatch prototype : prototypes) {
            ItemStack expected = prototype.getStackForm(1);
            long assembler = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
                .filter(recipe -> Arrays.stream(recipe.mOutputs).anyMatch(
                    output -> output != null && output.stackSize > 0 && GTUtility.areStacksEqual(output, expected)))
                .count();
            long assemblyLine = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
                .filter(recipe -> recipe.mOutput != null && recipe.mOutput.stackSize > 0
                    && GTUtility.areStacksEqual(recipe.mOutput, expected))
                .count();
            require(assembler + assemblyLine > 0,
                "Survival recipe exists for module " + expected.getItemDamage() + " " + prototype.getLocalName());
            if (GTUtility.areStacksEqual(expected, GTNGItemList.SingularityPerfectOverclockController.get(1))) {
                require(assemblyLine == 1, "Exactly one singularity overclock assembly-line survival recipe");
                GTRecipe.RecipeAssemblyLine recipe = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
                    .filter(
                        candidate -> candidate.mOutput != null && GTUtility.areStacksEqual(candidate.mOutput, expected))
                    .findFirst().get();
                int[] amounts = Arrays.stream(recipe.mInputs).mapToInt(input -> input == null ? 0 : input.stackSize)
                    .toArray();
                int[] required = { 1, 2048, 4096, 6144, 8192, 8192, 8192, 8192, 65536, 131072 };
                require(
                    Arrays.equals(amounts, required) && recipe.mDuration == 20 * 3600 * 24 * 64
                        && recipe.mEUt == TierEU.RECIPE_MAX,
                    "Singularity overclock keeps exact oversized ingredients and gates: " + Arrays
                        .toString(amounts) + ", duration=" + recipe.mDuration + ", EU/t=" + recipe.mEUt);
                report.append("Singularity overclock assembly line: inputs=").append(Arrays.toString(amounts))
                    .append(", duration=").append(recipe.mDuration).append(", EU/t=").append(recipe.mEUt).append('\n');
            }
            report.append("Module survival recipe ").append(expected.getItemDamage()).append(": assembler=")
                .append(assembler).append(", assemblyLine=").append(assemblyLine).append('\n');
        }
        report.append("All 46 modules have assembler or assembly-line survival recipes after world startup.\n");
    }

    private void verifyTstCoexistence() throws Exception {
        Class<?> items = Class.forName("com.Nxer.TwistSpaceTechnology.common.GTCMItemList");
        Method get = items.getMethod("get", int.class, Object[].class);
        RecipeMap<?> unresearched = (RecipeMap<?>) Class
            .forName("com.Nxer.TwistSpaceTechnology.common.recipeMap.GTCMRecipe")
            .getField("AssemblyLineWithoutResearchRecipeMap").get(null);
        for (ModularHatch module : prototypes) {
            String name = canonicalModuleName(module);
            String tstName = name.startsWith("PowerConsumption") ? "Static" + name : name;
            ItemStack output = (ItemStack) get.invoke(items.getField(tstName).get(null), 1, new Object[0]);
            report.append("TST recipe inventory ").append(name).append(": metadata=").append(output.getItemDamage())
                .append(", assembler=").append(recipesByOutput(RecipeMaps.assemblerRecipes, output).size())
                .append(", nativeAL=").append(assemblyLinesByOutput(output).size()).append(", unresearchedAL=")
                .append(recipesByOutput(unresearched, output).size()).append("; GTNG metadata=")
                .append(module.getStackForm(1).getItemDamage()).append(", unresearchedAL=")
                .append(recipesByOutput(unresearched, module.getStackForm(1)).size()).append('\n');
        }
        int assemblerPairs = 0, staticPairs = 0, dynamicPairs = 0, assemblyLines = 0, tstOutputs = 0;
        int removedMirrors = 0, mirroredOutputs = 0, singularityRecipes = 0;
        for (ModularHatch module : prototypes) {
            String name = canonicalModuleName(module);
            String tstName = name.startsWith("PowerConsumption") ? "Static" + name : name;
            Object container = items.getField(tstName).get(null);
            ItemStack own = module.getStackForm(1);
            ItemStack upstream = (ItemStack) get.invoke(container, 1, new Object[0]);
            require(
                upstream != null && upstream.getItemDamage() < GregTechAPI.METATILEENTITIES.length
                    && GregTechAPI.METATILEENTITIES[upstream.getItemDamage()] != null
                    && GregTechAPI.METATILEENTITIES[upstream.getItemDamage()].getClass().getName()
                        .startsWith("com.Nxer.TwistSpaceTechnology.")
                    && !GTUtility.areStacksEqual(own, upstream),
                "Actual independent TST module is registered: " + name);
            List<GTRecipe> ownAssembler = recipesByOutput(RecipeMaps.assemblerRecipes, own);
            List<GTRecipe> tstAssembler = recipesByOutput(RecipeMaps.assemblerRecipes, upstream);
            List<GTRecipe.RecipeAssemblyLine> ownLines = assemblyLinesByOutput(own);
            List<GTRecipe.RecipeAssemblyLine> tstLines = assemblyLinesByOutput(upstream);
            List<GTRecipe> ownUnresearched = recipesByOutput(unresearched, own);
            List<GTRecipe> tstUnresearched = recipesByOutput(unresearched, upstream);
            require(ownAssembler.size() + ownLines.size() == 1,
                "One actual GTNG survival recipe is registered: " + name);
            boolean singularity = name.equals("SingularityPerfectOverclockController");
            int tstCount = tstAssembler.size() + tstLines.size() + (singularity ? tstUnresearched.size() : 0);
            require(tstCount == 1,
                "One canonical TST survival recipe is registered: " + name
                    + "; metadata="
                    + upstream.getItemDamage()
                    + "; assembler="
                    + tstAssembler.size()
                    + "; assemblyLine="
                    + tstLines.size()
                    + "; unresearched="
                    + tstUnresearched.size()
                    + (tstCount == 1 ? "" : registeredRecipeDetails(upstream)));
            require(ownUnresearched.isEmpty(), "GTNG research recipes are absent from TST's unresearched map: " + name
                + (ownUnresearched.isEmpty() ? "" : registeredRecipeDetails(own)));
            tstOutputs++;
            if (!ownAssembler.isEmpty()) {
                require(tstAssembler.size() == 1, "Both assembler recipes exist: " + name);
                GTRecipe ownRecipe = ownAssembler.get(0), tstRecipe = tstAssembler.get(0);
                verifyAssemblerCircuit(ownRecipe, 19, name + " GTNG");
                verifyAssemblerCircuit(tstRecipe, 18, name + " TST");
                verifyAssemblerChoice(ownRecipe, own, upstream, name + " GTNG");
                verifyAssemblerChoice(tstRecipe, upstream, own, name + " TST");
                assemblerPairs++;
                if (module instanceof ParameterModuleHatch parameter && parameter.isDynamic()) {
                    String precursorName = name.replace("Dynamic", "Static");
                    ItemStack ownPrecursor = GTNGItemList.valueOf(precursorName).get(1);
                    ItemStack tstPrecursor = (ItemStack) get.invoke(items.getField(precursorName).get(null), 1,
                        new Object[0]);
                    require(
                        Arrays.stream(ownRecipe.mInputs)
                            .anyMatch(input -> input != null && GTUtility.areStacksEqual(input, ownPrecursor))
                            && Arrays.stream(tstRecipe.mInputs)
                                .anyMatch(input -> input != null && GTUtility.areStacksEqual(input, tstPrecursor)),
                        "Dynamic upgrades use their own static module: " + name);
                    dynamicPairs++;
                } else {
                    staticPairs++;
                }
                if (GTUtility.areStacksEqual(own, GTNGItemList.LowSpeedPerfectOverclockController.get(1)))
                    verifyLowSpeedCachedSelection(ownRecipe, tstRecipe, own, upstream);
            }
            for (GTRecipe.RecipeAssemblyLine recipe : ownLines) {
                verifyScannerAndDataStick(recipe, own, upstream, name + " GTNG");
                removedMirrors++;
                assemblyLines++;
            }
            for (GTRecipe.RecipeAssemblyLine recipe : tstLines) {
                verifyScannerAndDataStick(recipe, upstream, own, name + " TST");
                require(!tstUnresearched.isEmpty(), "TST retains its intended unresearched mirror: " + name);
                verifyUnresearchedChoice(unresearched, realRecipeItems(recipe.mInputs, false, false),
                    copyRecipeFluids(recipe.mFluidInputs), recipe.mEUt, upstream, own, name + " native AL inputs");
                report.append("TST unresearched mirror ").append(name).append(": count=").append(tstUnresearched.size())
                    .append("; native AL ingredients select only TST; GTNG copies=0\n");
                mirroredOutputs++;
                assemblyLines++;
            }
            if (singularity) {
                require(tstLines.isEmpty() && tstUnresearched.size() == 1,
                    "TST singularity retains only its original unresearched recipe");
                GTRecipe recipe = tstUnresearched.get(0);
                verifyUnresearchedChoice(unresearched, recipeItems(recipe, false, false), recipeFluids(recipe),
                    recipe.mEUt, upstream, own, name + " original inputs");
                singularityRecipes++;
            } else if (tstLines.isEmpty())
                require(tstUnresearched.isEmpty(), "Only researched TST modules have unresearched mirrors: " + name);
            report.append("TST coexistence native selection and research: ").append(name).append('\n');
        }
        require(
            tstOutputs == 46 && assemblerPairs == 41
                && staticPairs == 25
                && dynamicPairs == 16
                && assemblyLines == 9
                && removedMirrors == 5
                && mirroredOutputs == 4
                && singularityRecipes == 1,
            "All canonical outputs, assembler pairs, nine research routes and intended TST mirror routes pass");
        report
            .append("Actual TST + GTNG: 46 + 46 survival outputs, 41 assembler pairs selected without output filters, ")
            .append(
                "25 base pairs isolated, 16 dynamic upgrades isolated, nine native scanner/data-stick routes pass.\n")
            .append("GTNG's five unresearched copies are absent; four TST native-AL mirrors and TST singularity ")
            .append("remain selectable from real ingredients with no GTNG output candidates.\n");
    }

    private List<GTRecipe> recipesByOutput(RecipeMap<?> map, ItemStack expected) {
        return map.getAllRecipes().stream().filter(recipe -> hasOutput(recipe, expected)).collect(Collectors.toList());
    }

    private static String canonicalModuleName(ModularHatch module) throws Exception {
        String registeredName = (String) value(module, "mName");
        return Arrays.stream(GTNGItemList.values()).filter(entry -> entry.name().equalsIgnoreCase(registeredName))
            .map(GTNGItemList::name).findFirst()
            .orElseThrow(() -> new IllegalStateException("Unknown registered modular item: " + registeredName));
    }

    private String registeredRecipeDetails(ItemStack expected) {
        StringBuilder details = new StringBuilder("\nItem: ").append(expected.writeToNBT(new NBTTagCompound()));
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[expected.getItemDamage()];
        details.append("; registeredClass=").append(prototype == null ? "none" : prototype.getClass().getName());
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values())
            for (GTRecipe recipe : recipesByOutput(map, expected)) {
                details.append("\nMap=").append(map.unlocalizedName).append("; class=")
                    .append(recipe.getClass().getName()).append("; fake=").append(recipe.mFakeRecipe)
                    .append("; outputs=").append(Arrays.toString(recipe.mOutputs)).append("; inputs=")
                    .append(Arrays.toString(recipe.mInputs)).append("; fluids=")
                    .append(Arrays.toString(recipe.mFluidInputs)).append("; duration=").append(recipe.mDuration)
                    .append("; EUt=").append(recipe.mEUt);
            }
        for (GTRecipe.RecipeAssemblyLine recipe : assemblyLinesByOutput(expected)) {
            details.append("\nNativeAssemblyLine; class=").append(recipe.getClass().getName()).append("; output=")
                .append(recipe.mOutput.writeToNBT(new NBTTagCompound())).append("; inputs=")
                .append(Arrays.toString(recipe.mInputs)).append("; fluids=")
                .append(Arrays.toString(recipe.mFluidInputs)).append("; research=").append(recipe.mResearchItem)
                .append("; researchTime=").append(recipe.mResearchTime).append("; researchVoltage=")
                .append(recipe.mResearchVoltage).append("; duration=").append(recipe.mDuration).append("; EUt=")
                .append(recipe.mEUt);
        }
        return details.toString();
    }

    private List<GTRecipe.RecipeAssemblyLine> assemblyLinesByOutput(ItemStack expected) {
        return GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
            .filter(recipe -> recipe.mOutput != null && GTUtility.areStacksEqual(recipe.mOutput, expected))
            .collect(Collectors.toList());
    }

    private static boolean hasOutput(GTRecipe recipe, ItemStack expected) {
        return recipe != null && Arrays.stream(recipe.mOutputs)
            .anyMatch(output -> output != null && GTUtility.areStacksEqual(output, expected));
    }

    private static ItemStack[] recipeItems(GTRecipe recipe, boolean reverse, boolean circuitFirst) {
        return realRecipeItems(recipe.mInputs, reverse, circuitFirst);
    }

    private static ItemStack[] realRecipeItems(ItemStack[] inputs, boolean reverse, boolean circuitFirst) {
        List<ItemStack> ingredients = new ArrayList<>();
        List<ItemStack> circuits = new ArrayList<>();
        Item integratedCircuit = GTUtility.getIntegratedCircuit(0).getItem();
        for (ItemStack input : inputs) if (input != null) {
            ItemStack copy = GTUtility.copyAmountUnsafe(Math.max(1, input.stackSize), input);
            (copy.getItem() == integratedCircuit ? circuits : ingredients).add(copy);
        }
        if (reverse) Collections.reverse(ingredients);
        ingredients.addAll(circuitFirst ? 0 : ingredients.size(), circuits);
        return ingredients.toArray(new ItemStack[0]);
    }

    private static FluidStack[] recipeFluids(GTRecipe recipe) {
        return copyRecipeFluids(recipe.mFluidInputs);
    }

    private static FluidStack[] copyRecipeFluids(FluidStack[] fluids) {
        return Arrays.stream(fluids).map(fluid -> fluid == null ? null : fluid.copy()).toArray(FluidStack[]::new);
    }

    private void verifyUnresearchedChoice(RecipeMap<?> map, ItemStack[] inputs, FluidStack[] fluids, int eut,
        ItemStack expected, ItemStack forbidden, String label) {
        GTRecipe found = map.findRecipeQuery().items(inputs).fluids(fluids).voltage(eut).find();
        require(hasOutput(found, expected),
            "TST unresearched map selects its intended output from real inputs: " + label
                + "; found="
                + (found == null ? "none" : Arrays.toString(found.mOutputs)));
        require(
            !map.findRecipeQuery().items(inputs).fluids(fluids).voltage(eut).findAll()
                .anyMatch(candidate -> hasOutput(candidate, forbidden)),
            "TST unresearched candidates do not expose the GTNG research-gated output: " + label);
        report.append("TST unresearched actual candidates ").append(label).append(": ")
            .append(map.findRecipeQuery().items(inputs).fluids(fluids).voltage(eut).findAll()
                .map(recipe -> Arrays.toString(recipe.mOutputs)).collect(Collectors.joining("; ")))
            .append('\n');
    }

    private void verifyAssemblerChoice(GTRecipe recipe, ItemStack expected, ItemStack forbidden, String label) {
        GTRecipe found = RecipeMaps.assemblerRecipes.findRecipeQuery().items(recipeItems(recipe, false, false))
            .fluids(recipeFluids(recipe)).voltage(recipe.mEUt).find();
        require(hasOutput(found, expected),
            "Native assembler selects the intended output: " + label
                + "; found="
                + (found == null ? "none" : Arrays.toString(found.mOutputs)));
        require(
            !RecipeMaps.assemblerRecipes.findRecipeQuery().items(recipeItems(recipe, false, false))
                .fluids(recipeFluids(recipe)).voltage(recipe.mEUt).findAll()
                .anyMatch(candidate -> hasOutput(candidate, forbidden)),
            "The other mod cannot match the same actual assembler inputs: " + label);
    }

    private void verifyAssemblerCircuit(GTRecipe recipe, int configuration, String label) {
        List<ItemStack> circuits = Arrays.stream(recipe.mInputs)
            .filter(input -> input != null && input.getItem() == GTUtility.getIntegratedCircuit(0).getItem())
            .collect(Collectors.toList());
        require(
            circuits.size() == 1 && circuits.get(0).stackSize == 0 && circuits.get(0).getItemDamage() == configuration,
            "Assembler uses its independent non-consumed circuit configuration " + configuration + ": " + label);
    }

    private void verifyLowSpeedCachedSelection(GTRecipe ownRecipe, GTRecipe tstRecipe, ItemStack own, ItemStack tst)
        throws Exception {
        GTNGModularMachineBase<?> host = (GTNGModularMachineBase<?>) GregTechAPI.METATILEENTITIES[GTNGItemList.MageAssembler
            .get(1).getItemDamage()];
        require(host != null && host.getItemOutputLimit() >= 1 && host.getFluidOutputLimit() >= 0,
            "Registered Mage Assembler exposes native output limits for ProcessingLogic");
        for (boolean reverse : new boolean[] { false, true }) for (boolean circuitFirst : new boolean[] { false, true })
            for (boolean ownFirst : new boolean[] { false, true }) {
                GTRecipe first = ownFirst ? ownRecipe : tstRecipe, second = ownFirst ? tstRecipe : ownRecipe;
                ItemStack firstOutput = ownFirst ? own : tst, secondOutput = ownFirst ? tst : own;
                ProcessingLogic logic = new ProcessingLogic().setMachine(host).setVoidProtection(false, false)
                    .setRecipeLocking(host, false).setBatchSize(1).setRecipeMap(RecipeMaps.assemblerRecipes)
                    .setAvailableVoltage(first.mEUt).setAvailableAmperage(1).setMaxParallel(1);
                logic.setInputItems(recipeItems(first, reverse, circuitFirst)).setInputFluids(recipeFluids(first));
                CheckRecipeResult firstResult = logic.process();
                require(
                    firstResult.wasSuccessful()
                        && Arrays.stream(logic.getOutputItems())
                            .anyMatch(output -> output != null && GTUtility.areStacksEqual(output, firstOutput))
                        && value(logic, "lastRecipe") == first,
                    "Native ProcessingLogic starts the intended low-speed OC and populates lastRecipe: "
                        + firstResult.getID());
                logic.clear();
                logic.setAvailableVoltage(second.mEUt).setInputItems(recipeItems(second, reverse, circuitFirst))
                    .setInputFluids(recipeFluids(second));
                CheckRecipeResult secondResult = logic.process();
                require(
                    secondResult.wasSuccessful() && Arrays.stream(logic.getOutputItems())
                        .anyMatch(output -> output != null && GTUtility.areStacksEqual(output, secondOutput)),
                    "The other mod's cached low-speed recipe cannot steal circuit inputs; ownFirst=" + ownFirst
                        + ", reverse="
                        + reverse
                        + ", circuitFirst="
                        + circuitFirst
                        + ", result="
                        + secondResult.getID());
            }
        report.append(
            "Low-speed OC: both native lastRecipe directions, forward/reversed inputs, circuit first/last pass.\n");
    }

    private void verifyScannerAndDataStick(GTRecipe.RecipeAssemblyLine recipe, ItemStack expected, ItemStack forbidden,
        String label) {
        MetaTileEntity scanner = (MetaTileEntity) GregTechAPI.METATILEENTITIES[ItemList.Machine_LV_Scanner.get(1)
            .getItemDamage()];
        GTScannerResult result = ScannerHandlerLoader.doAssemblyLineResearch(scanner, recipe.mResearchItem.copy(),
            ItemList.Tool_DataStick.get(1), null);
        require(
            result instanceof GTScannerResult.ALScannerResult
                && ((GTScannerResult.ALScannerResult) result).alRecipe == recipe
                && result.eut == recipe.mResearchVoltage
                && result.duration == recipe.mResearchTime
                && GTUtility.areStacksEqual(AssemblyLineUtils.getDataStickOutput(result.output), expected),
            "Native scanner generates the intended assembly-line data stick: " + label);
        Collection<GTRecipe.RecipeAssemblyLine> selected = AssemblyLineUtils.findALRecipeFromDataStick(result.output);
        require(
            selected != null && selected.contains(recipe)
                && selected.stream().noneMatch(candidate -> GTUtility.areStacksEqual(candidate.mOutput, forbidden)),
            "Actual data-stick NBT selects its own assembly-line recipe: " + label);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            if (outputGuiOnly) {
                renderOutputGuiTooltip(event.renderTickTime);
                return;
            }
            if (!iconsShown) return;
            if (tooltipStage >= 0) {
                if (
                    !(Minecraft.getMinecraft().currentScreen instanceof NativeTooltipScreen screen)
                        || ++tooltipFrames < 40
                ) return;
                capture(screen.filename);
                report.append("Native item tooltip rendered: ").append(screen.filename).append('\n');
                tooltipFrames = 0;
                if (++tooltipStage < tooltipControllers.length * 4) {
                    showNativeTooltip();
                    return;
                }
                report.append(
                    "All seven controllers render matching native primary/secondary item tooltips in Chinese and English.\n");
                Files.write(new File(output(), "result.txt").toPath(),
                    ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
                finished = true;
                Minecraft.getMinecraft().shutdown();
                return;
            }
            if (++guiFrames < 60) return;
            capture("all-module-original-textures");
            report.append(tooltipOnly ? "All 46 original module item textures rendered.\n"
                : "All 46 original module item textures rendered; 16 real parameter GUIs synchronized.\n");
            tooltipStage = 0;
            showNativeTooltip();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void showNativeTooltip() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        int machineIndex = tooltipStage / 2 % tooltipControllers.length;
        String locale = tooltipStage < tooltipControllers.length * 2 ? "zh_CN" : "en_US";
        boolean shift = tooltipStage % 2 != 0;
        mc.gameSettings.guiScale = 1;
        mc.gameSettings.advancedItemTooltips = false;
        if (!locale.equals(tooltipLocale)) {
            boolean found = false;
            for (Language language : mc.getLanguageManager().getLanguages())
                if (language.getLanguageCode().equals(locale)) {
                    mc.getLanguageManager().setCurrentLanguage(language);
                    mc.gameSettings.language = locale;
                    mc.refreshResources();
                    found = true;
                    break;
                }
            require(found, "Native tooltip locale is available: " + locale);
            tooltipLocale = locale;
        }
        String filename = tooltipSlugs[machineIndex] + "-tooltip-" + locale + (shift ? "-shift" : "-normal");
        mc.displayGuiScreen(new NativeTooltipScreen(tooltipControllers[machineIndex].get(1), shift, filename));
    }

    private static <T> T withTooltipShift(boolean shift, Callable<T> action) throws Exception {
        Field field;
        try {
            field = Keyboard.class.getDeclaredField("keyDownBuffer");
        } catch (NoSuchFieldException ignored) {
            return withSdlTooltipShift(shift, action);
        }
        field.setAccessible(true);
        ByteBuffer keys = (ByteBuffer) field.get(null);
        byte left = keys.get(Keyboard.KEY_LSHIFT), right = keys.get(Keyboard.KEY_RSHIFT);
        try {
            keys.put(Keyboard.KEY_LSHIFT, (byte) (shift ? 1 : 0));
            keys.put(Keyboard.KEY_RSHIFT, (byte) 0);
            return action.call();
        } finally {
            keys.put(Keyboard.KEY_LSHIFT, left);
            keys.put(Keyboard.KEY_RSHIFT, right);
        }
    }

    private static <T> T withSdlTooltipShift(boolean shift, Callable<T> action) throws Exception {
        Field field = Class.forName("org.lwjglx.input.Keyboard").getField("sdlKeyPressedArray");
        Method scancode = Class.forName("org.lwjglx.input.KeyCodes").getMethod("lwjglToSdlScancode", int.class);
        int left = (int) scancode.invoke(null, Keyboard.KEY_LSHIFT);
        int right = (int) scancode.invoke(null, Keyboard.KEY_RSHIFT);
        ByteBuffer original = (ByteBuffer) field.get(null);
        require(original != null && left > 0 && right > 0 && left < original.limit() && right < original.limit(),
            "Native tooltip SDL keyboard state is available");
        ByteBuffer copy = original.duplicate();
        copy.position(0);
        ByteBuffer keys = ByteBuffer.allocate(original.limit());
        keys.put(copy);
        keys.clear();
        keys.put(left, (byte) (shift ? 1 : 0));
        keys.put(right, (byte) 0);
        try {
            field.set(null, keys);
            require(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) == shift && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT),
                "Native tooltip reads temporary SDL Shift state");
            return action.call();
        } finally {
            field.set(null, original);
        }
    }

    private static String tooltipText(String line) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(line).replace("%%%", "").replaceAll("\\s+", " ").trim();
    }

    private void buildNextRig(EntityPlayerMP player) throws Exception {
        rig = rigs.get(rigIndex);
        builder = rig.builder;
        String[][] shape = (String[][]) value(builder, "shape");
        if (hasField(builder.getClass(), "hatchPositions")) {
            @SuppressWarnings("unchecked")
            List<int[]> positions = (List<int[]>) value(builder, "hatchPositions");
            for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
                for (int col = 0; col < shape[row][depth].length(); col++)
                    if (shape[row][depth].charAt(col) == rig.hatchSymbol) positions.add(new int[] { col, row, depth });
        }
        invoke(builder, "buildRig",
            new Class<?>[] { EntityPlayerMP.class, int.class, int.class, int.class, ForgeDirection.class }, player,
            rigIndex * 256, 100, 0, ForgeDirection.NORTH);
        base = (BaseMetaTileEntity) value(builder, "base");
        require(base.getMetaTileEntity() instanceof GTNGModularMachineBase<?>, rig.slug + " uses modular controller");
        machine = (GTNGModularMachineBase<?>) base.getMetaTileEntity();
        base.disableWorking();
        invoke(builder, "verifyStructure", new Class<?>[0]);
        require(machine.getModuleTimeReduction() == 2 && machine.getModulePowerIncrease() == 4,
            rig.slug + " default overclock is time /2, EU/t x4");
        verifyDefaultOverclock();
        slots = new ArrayList<>();
        for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
            for (int col = 0; col < shape[row][depth].length(); col++)
                if (shape[row][depth].charAt(col) == rig.hatchSymbol) {
                    int[] pos = (int[]) invoke(builder, "position", new Class<?>[] { int.class, int.class, int.class },
                        col, row, depth);
                    if (base.getWorld().getBlock(pos[0], pos[1], pos[2]) != GregTechAPI.sBlockMachines)
                        slots.add(new SavedBlock(base.getWorld(), pos));
                }
        require(slots.size() >= 6, "Six spare original hatch positions " + rig.slug);
        moduleIndex = 0;
        System.out.println("MODULAR_MACHINE_QA_RIG " + rig.slug);
    }

    private void verifyDefaultOverclock() throws Exception {
        ModularProcessingLogic logic = (ModularProcessingLogic) invoke(machine, "createProcessingLogic",
            new Class<?>[0]);
        invoke(machine, "setupProcessingLogic", new Class<?>[] { ProcessingLogic.class }, logic);
        logic.setAvailableVoltage(128).setAvailableAmperage(1).setMaxParallel(1);
        GTRecipe recipe = GTRecipeBuilder.builder().itemInputs(stack(90, 1)).itemOutputs(stack(91, 1)).duration(7680)
            .eut(32).build().get();
        OverclockCalculator calculator = (OverclockCalculator) invoke(logic, "createOverclockCalculator",
            new Class<?>[] { GTRecipe.class }, recipe);
        calculator.calculate();
        require(
            calculator.getConsumption() == 128
                && calculator.getDuration() == (int) (7680 / machine.getModuleSpeedMultiplier() / 2),
            "Actual recipe calculator uses lossy 2/4 overclock " + rig.slug);
        logic.finishModuleRecipe();
    }

    private void verifyMaintenanceFree() throws Exception {
        require(machine.mMaintenanceHatches.size() == 1, "Legacy maintenance hatch remains accepted: " + rig.slug);
        var original = machine.mMaintenanceHatches.get(0).getBaseMetaTileEntity();
        int[] position = { original.getXCoord(), original.getYCoord(), original.getZCoord() };
        SavedBlock casing = slots.get(0);
        base.getWorld().setBlock(position[0], position[1], position[2], casing.block, casing.metadata, 3);
        machine.clearHatches();
        require(machine.checkStructure(true, base) && machine.mMaintenanceHatches.isEmpty(),
            "Physical structure forms without maintenance hatch: " + rig.slug);
        require(!machine.getDefaultHasMaintenanceChecks() && !machine.shouldCheckMaintenance(),
            "Maintenance stays disabled independently of saved settings: " + rig.slug);
        resetWork();
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = false;
        machine.mSolderingTool = machine.mCrowbar = false;
        machine.mEfficiency = 1000;
        NBTTagCompound legacy = new NBTTagCompound();
        machine.saveNBTData(legacy);
        machine.loadNBTData(legacy);
        require(machine.getRepairStatus() == machine.getIdealStatus(),
            "Broken legacy repair flags cannot create maintenance faults: " + rig.slug);
        machine.checkMaintenance();
        require(machine.mWrench && machine.mScrewdriver
            && machine.mSoftMallet
            && machine.mHardHammer
            && machine.mSolderingTool
            && machine.mCrowbar, "Legacy repair flags are normalized for the native GUI: " + rig.slug);
        for (int attempt = 0; attempt < 10000; attempt++)
            require((boolean) invoke(machine, "doRandomMaintenanceDamage", new Class<?>[0]),
                "Random maintenance damage cannot block work: " + rig.slug);
        GTRecipeBuilder recipe = GTRecipeBuilder.builder().duration(20 * 3600 * 24).eut(8);
        if (rig.slug.equals("particle-collider")) {
            recipe.fluidInputs(new FluidStack(maintenanceFluid, 100))
                .fluidOutputs(new FluidStack(maintenanceFluid, 10));
            machine.mInputHatches.get(0).setFillableStack(new FluidStack(maintenanceFluid, 100));
        } else if (machine.getRecipeMap() == RecipeMaps.extruderRecipes) {
            recipe.itemInputs(stack(180, 1), ItemList.Shape_Extruder_Rod.get(0)).itemOutputs(stack(181, 1));
            seedInputs(stack(180, 1), ItemList.Shape_Extruder_Rod.get(1));
        } else {
            recipe.itemInputs(stack(180, 1)).itemOutputs(stack(181, 1));
            seed(180, 1);
        }
        Collection<GTRecipe> registered = recipe.addTo(machine.getRecipeMap());
        require(registered.size() == 1,
            "One valid maintenance fixture recipe registers: " + rig.slug
                + "; map="
                + machine.getRecipeMap().unlocalizedName
                + "; registered="
                + registered.size());
        for (var hatch : machine.mEnergyHatches) {
            BaseMetaTileEntity energy = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
            energy.setStoredEU(energy.getEUCapacity());
        }
        CheckRecipeResult result = machine.checkProcessing();
        require(result.wasSuccessful() && machine.getModuleWorks().size() == 1,
            "Actual recipe starts without repairs or a maintenance hatch: " + rig.slug
                + "; result="
                + result.getID()
                + "; registered="
                + registered.size()
                + "; tasks="
                + machine.getModuleWorks().size());
        require(machine.mEfficiency == 10000, "Starting recipe has no maintenance efficiency penalty: " + rig.slug);
        ModuleWork work = machine.getModuleWorks().get(0);
        base.enableWorking();
        invoke(machine, "runMachine", new Class<?>[] { IGregTechTileEntity.class, long.class }, base, controlledTick++);
        base.disableWorking();
        require(work.progress() > 0 && machine.mEfficiency == 10000,
            "Actual recipe advances at full maintenance efficiency without QA repair injection: " + rig.slug);
        resetWork();
        NBTTagCompound originalState = new NBTTagCompound();
        machine.saveNBTData(originalState);
        try {
            machine.mProgresstime = 37;
            machine.mMaxProgresstime = 80;
            machine.mEfficiency = 4000;
            machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = false;
            machine.mSolderingTool = machine.mCrowbar = false;
            NBTTagCompound oldRecipe = new NBTTagCompound();
            machine.saveNBTData(oldRecipe);
            oldRecipe.removeTag("gtngModuleWork");
            machine.loadNBTData(oldRecipe);
            require(
                machine.mProgresstime == 37 && machine.mMaxProgresstime == 80
                    && machine.mEfficiency == 10000
                    && machine.getRepairStatus() == machine.getIdealStatus(),
                "Legacy ongoing recipe restores full efficiency without losing progress: " + rig.slug);
        } finally {
            machine.loadNBTData(originalState);
        }
        report.append(rig.slug).append(": no maintenance hatch, legacy broken repair NBT, 10000 damage checks and ")
            .append("actual recipe start/progress at full efficiency passed; legacy ongoing recipe normalizes ")
            .append("old efficiency without losing progress; legacy hatch remains optional.\n");
    }

    private void verifyMaintenanceTooltips() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        for (String locale : new String[] { "zh_CN", "en_US" }) {
            boolean found = false;
            for (Language language : mc.getLanguageManager().getLanguages())
                if (language.getLanguageCode().equals(locale)) {
                    mc.getLanguageManager().setCurrentLanguage(language);
                    mc.gameSettings.language = locale;
                    mc.refreshResources();
                    found = true;
                    break;
                }
            require(found, "Native maintenance tooltip locale exists: " + locale);
            for (int index = 0; index < tooltipControllers.length; index++)
                for (boolean shift : new boolean[] { false, true }) {
                    ItemStack stack = tooltipControllers[index].get(1);
                    List<String> lines = withTooltipShift(shift, () -> stack.getTooltip(mc.thePlayer, false));
                    String text = tooltipText(String.join("\n", lines));
                    require(!text.contains("维护仓") && !text.toLowerCase(Locale.ROOT).contains("maintenance hatch"),
                        "Native tooltip has no maintenance-hatch requirement: " + tooltipControllers[index]
                            + " "
                            + locale);
                    String filename = tooltipSlugs[index] + "-maintenance-tooltip-"
                        + locale
                        + (shift ? "-shift" : "-normal");
                    Files.write(new File(output(), filename + ".txt").toPath(), lines, StandardCharsets.UTF_8);
                }
        }
        report.append("All seven native item tooltips omit maintenance hatches in Chinese/English, normal/Shift.\n");
    }

    private void verifyModule(ModularHatch prototype) throws Exception {
        ModularHatch installed = install(0, prototype);
        invoke(builder, "verifyStructure", new Class<?>[0]);
        require(
            machine.getModularHatches().size() == 1
                && machine.getModularHatches().get(0).getModuleType() == prototype.getModuleType(),
            "Physical module discovered " + rig.slug + " " + prototype.getStackForm(1).getItemDamage());
        if (prototype.getModuleType() == ModuleType.Overclock) verifyInstalledOverclock(prototype);
        if (installed instanceof ParameterModuleHatch parameter && parameter.isDynamic()) {
            parameter.setParameter(Integer.MIN_VALUE);
            require(parameter.getParameter() == 1, "Dynamic lower bound");
            parameter.setParameter(Integer.MAX_VALUE);
            require(parameter.getParameter() == parameter.getMaximumParameter(), "Dynamic upper bound");
        }
        install(1, prototype);
        machine.clearHatches();
        boolean duplicateAccepted = machine.checkStructure(true, base);
        require(duplicateAccepted == (prototype.getModuleType() == ModuleType.ExecutionCore),
            "Only execution cores may repeat " + rig.slug + " " + prototype.getModuleType());
        slots.get(0).restore();
        slots.get(1).restore();
        invoke(builder, "verifyStructure", new Class<?>[0]);
    }

    private void verifyInstalledOverclock(ModularHatch prototype) throws Exception {
        GTNGItemList[] identities = { GTNGItemList.LowSpeedPerfectOverclockController,
            GTNGItemList.PerfectOverclockController, GTNGItemList.SingularityPerfectOverclockController };
        int[] timeReduction = { 4, 8, 8 }, powerIncrease = { 4, 4, 2 };
        int[] expectedEut = { 128, 128, 64 }, expectedDuration = { 1920, 960, 960 };
        int index = -1;
        for (int i = 0; i < identities.length; i++)
            if (GTUtility.areStacksEqual(prototype.getStackForm(1), identities[i].get(1))) index = i;
        require(index >= 0, "Installed overclock module matches a registered controller identity");
        require(
            machine.getModuleTimeReduction() == timeReduction[index]
                && machine.getModulePowerIncrease() == powerIncrease[index],
            "Independent overclock contract " + identities[index] + " " + rig.slug);
        double speed = machine.getModuleSpeedMultiplier();
        int duration = (int) (7680 * speed);
        require(duration > 0 && duration == 7680 * speed, "Base speed permits an exact normalized recipe duration");
        GTRecipe recipe = GTRecipeBuilder.builder().itemInputs(stack(92, 1)).itemOutputs(stack(93, 1))
            .duration(duration).eut(32).build().get();
        ModularProcessingLogic logic = (ModularProcessingLogic) invoke(machine, "createProcessingLogic",
            new Class<?>[0]);
        try {
            invoke(machine, "setupProcessingLogic", new Class<?>[] { ProcessingLogic.class }, logic);
            logic.setAvailableVoltage(128).setAvailableAmperage(1).setMaxParallel(1);
            OverclockCalculator calculator = (OverclockCalculator) invoke(logic, "createOverclockCalculator",
                new Class<?>[] { GTRecipe.class }, recipe);
            calculator.calculate();
            require(
                calculator.getConsumption() == expectedEut[index]
                    && calculator.getDuration() == expectedDuration[index],
                "Installed overclock calculation " + identities[index]
                    + " "
                    + rig.slug
                    + ": "
                    + calculator.getConsumption()
                    + " EU/t, "
                    + calculator.getDuration()
                    + " ticks at 128 V");
            report.append(rig.slug).append(" ").append(identities[index]).append(": 128 V -> ")
                .append(calculator.getConsumption()).append(" EU/t, ").append(calculator.getDuration())
                .append(" ticks");
            if (identities[index] == GTNGItemList.SingularityPerfectOverclockController) {
                logic.setAvailableVoltage(512);
                calculator = (OverclockCalculator) invoke(logic, "createOverclockCalculator",
                    new Class<?>[] { GTRecipe.class }, recipe);
                calculator.calculate();
                require(calculator.getConsumption() == 128 && calculator.getDuration() == 120,
                    "Singularity overclock takes two native voltage steps at 512 V " + rig.slug);
                report.append("; 512 V -> 128 EU/t, 120 ticks");
            }
            report.append('\n');
        } finally {
            logic.finishModuleRecipe();
        }
    }

    private void verifyCombinedParameters() throws Exception {
        int originalParallel = machine.getMaxParallelRecipes();
        double originalSpeed = machine.getModuleSpeedMultiplier();
        Map<ModuleType, ParameterModuleHatch> selected = new EnumMap<>(ModuleType.class);
        for (ModularHatch prototype : prototypes)
            if (prototype instanceof ParameterModuleHatch parameter) selected.put(parameter.getModuleType(), parameter);
        int index = 0;
        for (ParameterModuleHatch parameter : selected.values()) {
            ParameterModuleHatch installed = (ParameterModuleHatch) install(index++, parameter);
            installed.setParameter(installed.getMaximumParameter());
        }
        invoke(builder, "verifyStructure", new Class<?>[0]);
        ParameterModuleHatch parallel = selected.get(ModuleType.Parallel);
        require(
            machine.getMaxParallelRecipes()
                == (int) Math.min(Integer.MAX_VALUE, (long) originalParallel * (1L + parallel.getMaximumParameter())),
            "Parallel capacity uses 1 + parameter");
        require(
            machine.getModuleSpeedMultiplier() == originalSpeed * selected.get(ModuleType.Speed).getMaximumParameter(),
            "Speed module applies its multiplier");
        require(machine.getModulePowerMultiplier() == selected.get(ModuleType.PowerConsumption).getPowerMultiplier(),
            "EU module applies its multiplier");
        require(
            machine.getModuleTimeReduction() == selected.get(ModuleType.Overclock).getTimeReduction()
                && machine.getModulePowerIncrease() == selected.get(ModuleType.Overclock).getPowerIncrease(),
            "Overclock module replaces the 2/4 default");
        restoreModules();
    }

    private void verifyPhysicalEnergyRequired() throws Exception {
        BaseMetaTileEntity energy = (BaseMetaTileEntity) machine.mEnergyHatches.get(0).getBaseMetaTileEntity();
        int[] pos = { energy.getXCoord(), energy.getYCoord(), energy.getZCoord() };
        NBTTagCompound saved = new NBTTagCompound();
        energy.writeToNBT(saved);
        int energyId = energy.getMetaTileID();
        install(0, core(CoreType.Perfect));
        SavedBlock casing = slots.get(0);
        base.getWorld().setBlock(pos[0], pos[1], pos[2], casing.block, casing.metadata, 3);
        machine.clearHatches();
        require(!machine.checkStructure(true, base), "Wireless execution core cannot replace a physical energy hatch");
        BaseMetaTileEntity restored = place(base.getWorld(), pos, energyId);
        restored.readFromNBT(saved);
        restoreModules();
    }

    private void verifyUnlimitedEnergyHatches() throws Exception {
        require(machine.mEnergyHatches.size() == 1 && machine.getExoticEnergyHatches().isEmpty(),
            "Energy-count fixture starts with one ordinary hatch: " + rig.slug);
        BaseMetaTileEntity original = (BaseMetaTileEntity) machine.mEnergyHatches.get(0).getBaseMetaTileEntity();
        int[] position = { original.getXCoord(), original.getYCoord(), original.getZCoord() };
        int originalId = original.getMetaTileID();
        NBTTagCompound saved = new NBTTagCompound();
        original.writeToNBT(saved);
        try {
            place(base.getWorld(), position, ItemList.Hatch_Energy_LV.get(1).getItemDamage());
            for (int index = 0; index < 2; index++)
                place(base.getWorld(), slots.get(index).position, ItemList.Hatch_Energy_LV.get(1).getItemDamage());
            verifyEnergyStructure();
            verifyEnergyCapacity(3, 0, 32, 192);
            if (rig.slug.equals("mage-assembler")) {
                ((GTNGMultiModeMachineBase<?>) machine).setMachineMode(0);
                addRecipe(140, 141, 128, 200);
                resetWork();
                seed(140, 1);
                require(
                    !machine.checkProcessing().wasSuccessful() && inputCount(140) == 1
                        && machine.getModuleWorks().isEmpty(),
                    "Three LV hatches cannot unlock an MV recipe despite 192 EU/t aggregate power");
                place(base.getWorld(), position, ItemList.Hatch_Energy_MV.get(1).getItemDamage());
                verifyEnergyStructure();
                require(
                    machine.checkProcessing().wasSuccessful() && inputCount(140) == 0
                        && machine.getModuleWorks().size() == 1,
                    "The same recipe starts only after installing a real MV hatch");
                resetWork();
                place(base.getWorld(), position, ItemList.Hatch_Energy_LV.get(1).getItemDamage());
                report.append("Three LV hatches retain 32 V; the 128 EU/t recipe requires a real MV hatch.\n");
            }
            place(base.getWorld(), slots.get(2).position, ItemList.Hatch_Energy_LV.get(1).getItemDamage());
            place(base.getWorld(), slots.get(3).position, CustomItemList.eM_energyMulti4_EV.get(1).getItemDamage());
            place(base.getWorld(), slots.get(4).position, CustomItemList.eM_energyMulti16_EV.get(1).getItemDamage());
            verifyEnergyStructure();
            verifyEnergyCapacity(4, 2, 2048, 41216);
            List<MTEHatch> hatches = energyHatches();
            long stored = 0;
            for (MTEHatch hatch : hatches) {
                BaseMetaTileEntity tile = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
                tile.setStoredEU(tile.getEUCapacity());
                stored = Math.addExact(stored, tile.getStoredEU());
            }
            require(machine.drainEnergyInput(41216), "Mixed physical hatches supply their full aggregate EU/t");
            long remaining = 0;
            for (MTEHatch hatch : hatches)
                remaining = Math.addExact(remaining, hatch.getBaseMetaTileEntity().getStoredEU());
            require(stored - remaining == 41216, "Mixed hatch drain consumes exactly the requested EU");
            SavedBlock casing = slots.get(0);
            base.getWorld().setBlock(position[0], position[1], position[2], casing.block, casing.metadata, 3);
            for (int index = 0; index < 3; index++) slots.get(index).restore();
            verifyEnergyStructure();
            verifyEnergyCapacity(0, 2, 2048, 40960);
            for (SavedBlock slot : slots) slot.restore();
            machine.clearHatches();
            require(
                !machine.checkStructure(true, base) && machine.mEnergyHatches.isEmpty()
                    && machine.getExoticEnergyHatches().isEmpty(),
                "Valid casings without any physical energy hatch cannot form: " + rig.slug);
            report.append(rig.slug)
                .append(": three LV, four ordinary + two multi-amp, and two multi-amp-only hatches form; ")
                .append("all inputs aggregate at highest single-hatch voltage; zero energy hatches fail.\n");
        } finally {
            for (SavedBlock slot : slots) slot.restore();
            BaseMetaTileEntity restored = place(base.getWorld(), position, originalId);
            restored.readFromNBT(saved);
            invoke(builder, "verifyStructure", new Class<?>[0]);
        }
    }

    private List<MTEHatch> energyHatches() {
        List<MTEHatch> hatches = new ArrayList<>(machine.mEnergyHatches);
        hatches.addAll(machine.getExoticEnergyHatches());
        return hatches;
    }

    private void verifyEnergyStructure() {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        require(errors.isEmpty(),
            "Energy-count structure: " + rig.slug
                + "; "
                + String.join("; ", errors.stream().map(StructureError::getDisplayString).toArray(String[]::new)));
        machine.clearHatches();
        require(machine.checkStructure(true, base), "Physical energy-count structure forms: " + rig.slug);
    }

    private void verifyEnergyCapacity(int ordinary, int exotic, long expectedVoltage, long expectedPower) {
        require(machine.mEnergyHatches.size() == ordinary && machine.getExoticEnergyHatches().size() == exotic,
            "All physical energy hatches are discovered: " + rig.slug);
        long voltage = 0, power = 0;
        for (MTEHatch hatch : energyHatches()) {
            long inputVoltage = hatch.getBaseMetaTileEntity().getInputVoltage();
            voltage = Math.max(voltage, inputVoltage);
            power = Math.addExact(power, Math.multiplyExact(inputVoltage, hatch.maxWorkingAmperesIn()));
        }
        require(voltage == expectedVoltage && machine.getMaxInputVoltage() == voltage,
            "Input voltage is the highest single hatch, without summing: " + rig.slug);
        require(power == expectedPower && machine.getMaxInputEu() == power,
            "Input EU/t includes every ordinary and multi-amp working amperage: " + rig.slug);
    }

    private void verifyFieldProcessing() throws Exception {
        FieldExtruderFactory field = (FieldExtruderFactory) machine;
        ItemStack mold = ItemList.Shape_Extruder_Rod.get(1);
        try {
            field.setMachineMode(0);
            machine.setPowerPanelMaxParallel(64);
            invoke(builder, "setEnergy", new Class<?>[] { ItemList.class }, ItemList.Hatch_Energy_LV);
            invoke(builder, "verifyStructure", new Class<?>[0]);
            require(machine.getMaxParallelRecipes() == 1, "Field controller has the common one-parallel base");
            for (int metadata = 120; metadata <= 121; metadata++)
                GTRecipeBuilder.builder().itemInputs(stack(metadata, 1), ItemList.Shape_Extruder_Rod.get(0))
                    .itemOutputs(stack(metadata + 2, 1)).duration(80).eut(8).addTo(machine.getRecipeMap());
            resetWork();
            seedInputs(stack(120, 64), mold.copy());
            require(
                machine.checkProcessing().wasSuccessful() && machine.getModuleWorks().size() == 1
                    && machine.getModuleWorks().get(0).parallels == 1
                    && inputCount(120) == 63
                    && inputCount(mold) == 1
                    && pendingOutputCount(122) == 1,
                "Field starts one ordinary recipe without an extra sixty-four-fold batch");

            resetWork();
            seedInputs(stack(120, 3), stack(121, 2), mold.copy());
            require(
                machine.checkProcessing().wasSuccessful() && machine.getModuleWorks().size() == 1
                    && pendingOutputCount(122) + pendingOutputCount(123) == 1
                    && inputCount(120) + inputCount(121) == 4
                    && inputCount(mold) == 1,
                "A Field task selects one recipe from mixed inputs and follows GT non-consumed ingredient rules");

            ModularHatch parallel = prototypes.stream().filter(candidate -> GTUtility
                .areStacksEqual(candidate.getStackForm(1), GTNGItemList.DynamicParallelControllerT1.get(1))).findFirst()
                .get();
            ParameterModuleHatch installed = (ParameterModuleHatch) install(0, parallel);
            installed.setParameter(3);
            invoke(builder, "verifyStructure", new Class<?>[0]);
            machine.setPowerPanelMaxParallel(4);
            resetWork();
            seedInputs(stack(120, 16), mold.copy());
            require(
                machine.getMaxParallelRecipes() == 4 && machine.checkProcessing().wasSuccessful()
                    && machine.getModuleWorks().size() == 1
                    && machine.getModuleWorks().get(0).parallels == 4
                    && inputCount(120) == 12
                    && inputCount(mold) == 1
                    && pendingOutputCount(122) == 4,
                "Field parallel module applies the common base times one-plus-parameter limit");

            install(1, core(CoreType.Normal));
            invoke(builder, "verifyStructure", new Class<?>[0]);
            resetWork();
            seedInputs(stack(120, 16), mold.copy());
            require(machine.checkProcessing().wasSuccessful() && machine.getModuleWorks().get(0).parallels == 2,
                "Field controller shares the four-parallel limit with an ordinary execution core");
            tick();
            require(
                machine.getModuleWorks().size() == 2
                    && machine.getModuleWorks().stream().allMatch(work -> work.parallels == 2)
                    && inputCount(120) == 12
                    && inputCount(mold) == 1
                    && pendingOutputCount(122) == 4,
                "Field's ordinary core starts its own task within the shared parallel limit");

            resetWork();
            restoreModules();
            field.setMachineMode(1);
            GTRecipeBuilder.builder().itemInputs(stack(124, 1), ItemList.Shape_Extruder_Rod.get(1))
                .itemOutputs(stack(125, 1)).duration(80).eut(8).addTo(machine.getRecipeMap());
            seedInputs(stack(124, 1), mold.copy());
            require(
                machine.checkProcessing().wasSuccessful() && inputCount(124) == 0
                    && inputCount(mold) == 0
                    && pendingOutputCount(125) == 1,
                "A positive-count mold is consumed according to its recipe without a Field-specific exemption");
            report.append("Field common processing: one base parallel, no extra x64 batch or mixed recipe task, ")
                .append("module limit 4 shared as 2+2, and GT-defined consumed/non-consumed molds pass.\n");
        } finally {
            resetWork();
            restoreModules();
            field.setMachineMode(0);
            machine.setPowerPanelMaxParallel(8);
            invoke(builder, "setEnergy", new Class<?>[] { ItemList.class }, ItemList.Hatch_Energy_EV);
            invoke(builder, "verifyStructure", new Class<?>[0]);
        }
    }

    private void seedInputs(ItemStack... inputs) {
        var bus = machine.mInputBusses.get(0);
        for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (int slot = 0; slot < inputs.length; slot++) bus.setInventorySlotContents(slot, inputs[slot]);
    }

    private int inputCount(ItemStack expected) {
        int amount = 0;
        for (var bus : machine.mInputBusses) for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
            ItemStack stack = bus.getStackInSlot(slot);
            if (stack != null && GTUtility.areStacksEqual(stack, expected)) amount += stack.stackSize;
        }
        return amount;
    }

    private int pendingOutputCount(int metadata) {
        BigInteger amount = BigInteger.ZERO;
        for (ModuleWork work : machine.getModuleWorks()) {
            NBTTagList items = work.outputs.save().getTagList("items", 10);
            for (int index = 0; index < items.tagCount(); index++) {
                NBTTagCompound entry = items.getCompoundTagAt(index);
                ItemStack stack = GTUtility.loadItem(entry, "stack");
                if (stack != null && stack.getItem() == fixture && stack.getItemDamage() == metadata)
                    amount = amount.add(new BigInteger(entry.getString("amount")));
            }
        }
        return amount.intValueExact();
    }

    private void verifyScheduler() throws Exception {
        ((GTNGMultiModeMachineBase<?>) machine).setMachineMode(0);
        machine.setPowerPanelMaxParallel(1);
        invoke(builder, "setEnergy", new Class<?>[] { ItemList.class }, ItemList.Hatch_Energy_LV);
        invoke(builder, "verifyStructure", new Class<?>[0]);
        addRecipe(0, 10, 32, 20);
        addRecipe(1, 11, 32, 200);
        install(0, core(CoreType.Normal));
        invoke(builder, "verifyStructure", new Class<?>[0]);
        verifyNormalCorePowerRetry();
        resetWork();
        seed(0, 1);
        require(machine.checkProcessing().wasSuccessful(), "Controller starts short recipe");
        seed(1, 1);
        tick();
        require(machine.getModuleWorks().size() == 2, "Normal core starts a distinct concurrent recipe");
        require(machine.getModuleWorks().stream().map(work -> work.duration).distinct().count() == 2,
            "Concurrent recipes retain independent durations");
        ModuleWork detached = machine.getModuleWorks().stream().filter(work -> !work.core.equals("controller"))
            .findFirst().get();
        int detachedProgress = detached.progress();
        slots.get(0).restore();
        invoke(builder, "verifyStructure", new Class<?>[0]);
        tick();
        require(detached.progress() == detachedProgress, "Removing a core pauses its task without deleting it");
        install(0, core(CoreType.Advanced));
        invoke(builder, "verifyStructure", new Class<?>[0]);
        tick();
        require(detached.progress() == detachedProgress, "A different core grade cannot adopt the detached task");
        install(0, core(CoreType.Normal));
        invoke(builder, "verifyStructure", new Class<?>[0]);
        tick();
        require(detached.progress() > detachedProgress, "The matching replacement core resumes its task");
        for (int i = 0; i < 25; i++) tick();
        require(outputCount(10) == 1 && outputCount(11) == 0 && machine.getModuleWorks().size() == 1,
            "Short output arrives while the long recipe is still running");
        ModuleWork remaining = machine.getModuleWorks().get(0);
        int progress = remaining.progress();
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        machine.loadNBTData(saved);
        require(machine.getModuleWorks().size() == 1 && machine.getModuleWorks().get(0).progress() == progress,
            "Repeated NBT load replaces the ledger without duplicating tasks");
        var output = machine.mOutputBusses.get(0);
        for (int slot = 0; slot < output.getSizeInventory(); slot++)
            output.setInventorySlotContents(slot, new ItemStack(Items.stick, 64));
        for (int i = 0; i < 220; i++) tick();
        require(machine.getModuleWorks().size() == 1 && machine.getModuleWorks().get(0).finished(),
            "A completed task waits behind full protected output storage");
        for (int slot = 0; slot < output.getSizeInventory(); slot++) output.setInventorySlotContents(slot, null);
        tick();
        require(machine.getModuleWorks().isEmpty() && outputCount(11) == 1,
            "Clearing output storage flushes exactly the pending output");
        tick();
        require(outputCount(11) == 1, "A flushed output does not reappear on a later tick");
        restoreModules();
        verifyPrepayment(CoreType.Advanced);
        verifyPrepayment(CoreType.Perfect);
        verifyMePreflight();
        resetWork();
        report.append("Mage scheduler: distinct completion, persisted ledger, output-full wait, no duplicate flush, ")
            .append("advanced/perfect exact prepayment and failure rollback pass.\n");
    }

    private void verifyNormalCorePowerRetry() throws Exception {
        require(machine.getMaxInputEu() == 64 && machine.getMaxInputVoltage() == 32,
            "The original two-amp LV hatch supplies 64 EU/t at 32 V");
        try {
            invoke(builder, "setEnergy", new Class<?>[] { ItemList.class }, ItemList.Hatch_Energy_MV);
            install(1, core(CoreType.Normal));
            invoke(builder, "verifyStructure", new Class<?>[0]);
            long availablePower = machine.getMaxInputEu();
            require(availablePower == 256 && machine.getMaxInputVoltage() == 128,
                "The two-amp MV hatch supplies 256 EU/t at 128 V");
            addRecipe(70, 71, 128, 200);
            resetWork();
            seed(70, 1);
            require(machine.checkProcessing().wasSuccessful(),
                "A 128 EU/t recipe retries the full budget when three idle cores would each reserve only 85 EU/t");
            require(
                machine.getModuleWorks().size() == 1 && machine.getModuleWorks().get(0).eut == 128
                    && inputCount(70) == 0
                    && -machine.lEUt <= availablePower,
                "Full-budget retry starts exactly one funded recipe within the real input limit");
            tick();
            require(
                machine.getModuleWorks().size() == 1 && machine.getModuleWorks().stream()
                    .filter(work -> work.controllerPower).mapToLong(work -> work.eut).sum() <= availablePower,
                "Remaining idle normal cores do not create an extra task or overspend the physical supply");
            report.append("Normal-core power retry: LV supply=64 EU/t at 32 V; MV supply=256 EU/t at 128 V; ")
                .append("a 128 EU/t recipe survives the initial three-way 85 EU/t allocation.\n");
        } finally {
            resetWork();
            slots.get(1).restore();
            invoke(builder, "setEnergy", new Class<?>[] { ItemList.class }, ItemList.Hatch_Energy_LV);
            invoke(builder, "verifyStructure", new Class<?>[0]);
        }
    }

    private void verifyMePreflight() throws Exception {
        List<MTEHatchInputBus> originalBusses = new ArrayList<>(machine.mInputBusses);
        List<MTEHatchInput> originalHatches = new ArrayList<>(machine.mInputHatches);
        int[] busPosition = { base.getXCoord() + 80, 100, -4 };
        int[] hatchPosition = { base.getXCoord() + 82, 100, -4 };
        try {
            BaseMetaTileEntity busTile = place(base.getWorld(), busPosition,
                ItemList.Hatch_Input_Bus_ME.get(1).getItemDamage());
            BaseMetaTileEntity hatchTile = place(base.getWorld(), hatchPosition,
                ItemList.Hatch_Input_ME.get(1).getItemDamage());
            MTEHatchInputBusME bus = (MTEHatchInputBusME) busTile.getMetaTileEntity();
            MTEHatchInputME hatch = (MTEHatchInputME) hatchTile.getMetaTileEntity();
            PreflightNetwork network = new PreflightNetwork();
            field(MTEHatchInputBusME.class, "gridProxy").set(bus, network.proxy);
            field(MTEHatchInputME.class, "gridProxy").set(hatch, network.proxy);
            field(MTEHatchInputBusME.class, "processingRecipe").setBoolean(bus, true);
            field(MTEHatchInputBusME.class, "cachedActivity").setBoolean(bus, true);
            field(MTEHatchInputME.class, "processingRecipe").setBoolean(hatch, true);
            field(MTEHatchInputME.class, "cachedActivity").setBoolean(hatch, true);
            machine.mInputBusses.clear();
            machine.mInputBusses.add(bus);
            machine.mInputHatches.clear();
            machine.mInputHatches.add(hatch);
            MTEHatchInputBusME.Slot[] itemSlots = (MTEHatchInputBusME.Slot[]) value(bus, "slots");
            MTEHatchInputME.Slot[] fluidSlots = (MTEHatchInputME.Slot[]) value(hatch, "slots");

            ItemStack first = stockSlot(itemSlots, 0, 60);
            ItemStack second = stockSlot(itemSlots, 1, 60);
            network.items.put(60, 64L);
            PreflightLogic logic = new PreflightLogic(40, 0);
            logic.setInputItems(first, second);
            logic.prepareModuleRecipe(Long.MAX_VALUE, false);
            require(logic.process().wasSuccessful(), "Duplicate native ME shadows prepare a recipe");
            require(!ModuleInputPreflight.canCommit(machine, logic) && first.stackSize == 64
                && second.stackSize == 64
                && network.items.get(60) == 64, "Duplicate shadows cannot consume eighty items from sixty-four");

            second = stockSlot(itemSlots, 1, 61);
            network.items.put(61, 64L);
            network.aeEnergy = 30;
            logic = new PreflightLogic(20, 0);
            logic.setInputItems(first, second);
            logic.prepareModuleRecipe(Long.MAX_VALUE, false);
            logic.process();
            require(!ModuleInputPreflight.canCommit(machine, logic) && network.aeEnergy == 30,
                "ME energy sufficient per slot but insufficient in total never modulates");

            fluidSlots[0] = new MTEHatchInputME.Slot(new FluidStack(FluidRegistry.WATER, 1));
            fluidSlots[0].extracted = new FluidStack(FluidRegistry.WATER, 1000);
            fluidSlots[0].extractedAmount = 1000;
            FluidStack fluid = fluidSlots[0].extracted;
            network.aeEnergy = 40;
            logic = new PreflightLogic(20, 250);
            logic.setInputItems(first, second);
            logic.setInputFluids(fluid);
            logic.prepareModuleRecipe(Long.MAX_VALUE, false);
            logic.process();
            require(!ModuleInputPreflight.canCommit(machine, logic), "Items and fluid share the same AE energy budget");
            network.aeEnergy = 41;
            require(
                ModuleInputPreflight.canCommit(machine, logic) && first.stackSize == 64
                    && second.stackSize == 64
                    && fluid.amount == 1000
                    && network.aeEnergy == 41,
                "Exact shared ME item/fluid energy permits commit without modifying the network");
            require(network.modulations == 0, "Every ME preflight action is a simulation");
            PreflightLogic untouched = new PreflightLogic(0, 0);
            untouched.setInputItems(first, second);
            untouched.setInputFluids(fluid);
            untouched.prepareModuleRecipe(Long.MAX_VALUE, false);
            untouched.process();
            require(untouched.getPlannedConsumption(first) == 0 && untouched.getPlannedConsumption(fluid) == 0,
                "Offline ME checks have no planned network extraction");
            PreflightNetwork online = new PreflightNetwork();
            network.offline = true;
            field(MTEHatchInputME.class, "gridProxy").set(hatch, online.proxy);
            require(!ModuleInputPreflight.canCommit(machine, untouched),
                "An offline item stocking bus rejects a transaction even without planned consumption");
            field(MTEHatchInputBusME.class, "gridProxy").set(bus, online.proxy);
            field(MTEHatchInputME.class, "gridProxy").set(hatch, network.proxy);
            require(!ModuleInputPreflight.canCommit(machine, untouched),
                "An offline fluid stocking hatch rejects a transaction even without planned consumption");
            require(
                network.modulations == 0 && online.modulations == 0
                    && first.stackSize == 64
                    && second.stackSize == 64
                    && fluid.amount == 1000,
                "Offline checks preserve all local shadows and perform no network modulation");
            report.append("Native ME stocking shadows: duplicate stock, aggregate AE energy, shared fluid energy, ")
                .append(
                    "exact-budget success, offline networks without consumption, and zero preflight extraction pass.\n");
        } finally {
            machine.mInputBusses.clear();
            machine.mInputBusses.addAll(originalBusses);
            machine.mInputHatches.clear();
            machine.mInputHatches.addAll(originalHatches);
            base.getWorld().setBlockToAir(busPosition[0], busPosition[1], busPosition[2]);
            base.getWorld().setBlockToAir(hatchPosition[0], hatchPosition[1], hatchPosition[2]);
        }
    }

    private ItemStack stockSlot(MTEHatchInputBusME.Slot[] slots, int slot, int metadata) {
        slots[slot] = new MTEHatchInputBusME.Slot(stack(metadata, 1));
        slots[slot].extracted = stack(metadata, 64);
        slots[slot].extractedAmount = 64;
        return slots[slot].extracted;
    }

    private void verifyPrepayment(CoreType type) throws Exception {
        resetWork();
        ExecutionCoreHatch hatch = (ExecutionCoreHatch) install(0, core(type));
        UUID payer = UUID.randomUUID();
        hatch.getBaseMetaTileEntity().setOwnerUuid(payer);
        WirelessNetworkManager.strongCheckOrAddUser(payer);
        WirelessNetworkManager.setUserEU(payer, BigInteger.ZERO);
        invoke(builder, "verifyStructure", new Class<?>[0]);
        seed(1, 1);
        require(machine.checkProcessing().wasSuccessful(), "Controller occupies its own core");
        seed(0, 1);
        tick();
        require(inputCount(0) == 1 && machine.getModuleWorks().size() == 1,
            "Unfunded " + type + " does not eat inputs");
        BigInteger credit = BigInteger.valueOf(1_000_000);
        WirelessNetworkManager.setUserEU(payer, credit);
        for (int i = 0; i < 5 && machine.getModuleWorks().size() < 2; i++) tick();
        ModuleWork paid = machine.getModuleWorks().stream().filter(work -> work.coreType == type).findFirst()
            .orElseThrow(() -> new AssertionError("Funded core starts " + type));
        require(
            inputCount(0) == 0 && !paid.controllerPower
                && paid.prepaidEU.signum() > 0
                && paid.owner.equals(payer)
                && WirelessNetworkManager.getUserEU(payer).equals(credit.subtract(paid.prepaidEU)),
            "Core owner prepays exactly once " + type);
        if (type == CoreType.Perfect) require(paid.duration == 20, "Perfect core runs for exactly 20 ticks");
        NBTTagCompound hud = coreHud(hatch);
        require(
            hud.getInteger("moduleProgress") == paid.progress() && hud.getInteger("moduleDuration") == paid.duration
                && hud.getLong("moduleEut") == paid.eut
                && !hud.getString("modulePrepaidEU").isEmpty(),
            "Execution core HUD contains its own progress and prepayment " + type);
        base.disableWorking();
        invoke(machine, "runMachine", new Class<?>[] { IGregTechTileEntity.class, long.class }, base, controlledTick++);
        require(coreHud(hatch).getBoolean("modulePaused"), "Disabled controller synchronizes the core's paused HUD");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        BigInteger afterPayment = WirelessNetworkManager.getUserEU(payer);
        machine.loadNBTData(saved);
        machine.loadNBTData(saved);
        tick();
        require(WirelessNetworkManager.getUserEU(payer).equals(afterPayment),
            "NBT restore does not repay or refund " + type);
        resetWork();
        restoreModules();
    }

    private NBTTagCompound coreHud(ExecutionCoreHatch hatch) {
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
        NBTTagCompound tag = new NBTTagCompound();
        hatch.getWailaNBTData(player, tile, tag, tile.getWorld(), tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
        return tag;
    }

    private void verifyFocusGates() throws Exception {
        DimensionalFocusEngravingArray focus = (DimensionalFocusEngravingArray) machine;
        resetWork();
        String research = DimensionalFocusResearch.fingerprint(stack(22, 1));
        GTRecipeBuilder.builder().itemInputs(stack(20, 1)).itemOutputs(stack(22, 1)).duration(200).eut(32)
            .metadata(DimensionalFocusResearch.COMPUTATION, 4L).metadata(DimensionalFocusResearch.RESEARCH, research)
            .addTo(machine.getRecipeMap());
        for (CoreType type : new CoreType[] { CoreType.Advanced, CoreType.Perfect }) {
            ExecutionCoreHatch hatch = (ExecutionCoreHatch) install(0, core(type));
            UUID payer = UUID.randomUUID();
            hatch.getBaseMetaTileEntity().setOwnerUuid(payer);
            WirelessNetworkManager.strongCheckOrAddUser(payer);
            WirelessNetworkManager.setUserEU(payer, BigInteger.valueOf(1_000_000));
            invoke(builder, "verifyStructure", new Class<?>[0]);
            focus.getComputationHatches().get(0).setContents(new QuantumDataPacket(4L));
            focus.getResearchHatches().get(0).setInventorySlotContents(0, null);
            seed(20, 1);
            tick();
            require(inputCount(20) == 1 && machine.getModuleWorks().isEmpty(),
                "Core cannot bypass missing research " + type);
            focus.getResearchHatches().get(0).setInventorySlotContents(0,
                DimensionalFocusResearch.createResearchStick(stack(22, 1)));
            focus.getComputationHatches().get(0).setContents(new QuantumDataPacket(3L));
            for (int i = 0; i < 5; i++) tick();
            require(inputCount(20) == 1 && machine.getModuleWorks().isEmpty(),
                "Core cannot bypass computation gate " + type);
            focus.getComputationHatches().get(0).setContents(new QuantumDataPacket(4L));
            for (int i = 0; i < 5 && machine.getModuleWorks().isEmpty(); i++) tick();
            require(machine.getModuleWorks().size() == 1 && machine.getModuleUsedComputation() == 4,
                "Core reserves exact computation " + type);
            ModuleWork work = machine.getModuleWorks().get(0);
            int progress = work.progress();
            focus.getComputationHatches().get(0).setContents(new QuantumDataPacket(3L));
            tick();
            require(work.progress() == progress, "Losing computation pauses prepaid work " + type);
            resetWork();
            restoreModules();
        }
        report
            .append("Focus: advanced/perfect cores preserve research and computation gates and pause on data loss.\n");
    }

    private void addRecipe(int input, int output, int eut, int duration) {
        GTRecipeBuilder.builder().itemInputs(stack(input, 1)).itemOutputs(stack(output, 1)).duration(duration).eut(eut)
            .addTo(machine.getRecipeMap());
    }

    private void tick() throws Exception {
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = true;
        machine.mSolderingTool = machine.mCrowbar = true;
        machine.mEfficiency = 10000;
        for (var hatch : machine.mEnergyHatches) {
            BaseMetaTileEntity energy = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
            energy.setStoredEU(energy.getEUCapacity());
        }
        base.enableWorking();
        invoke(machine, "runMachine", new Class<?>[] { IGregTechTileEntity.class, long.class }, base, controlledTick++);
        base.disableWorking();
    }

    private void resetWork() throws Exception {
        base.disableWorking();
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        saved.setTag("gtngModuleWork", new NBTTagList());
        saved.setBoolean("gtngModuleLegacyRun", false);
        machine.loadNBTData(saved);
        for (var bus : machine.mInputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
        for (var bus : machine.mOutputBusses)
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) bus.setInventorySlotContents(slot, null);
    }

    private void seed(int metadata, int amount) {
        machine.mInputBusses.get(0).setInventorySlotContents(0, stack(metadata, amount));
    }

    private int inputCount(int metadata) {
        return machine.mInputBusses.stream().mapToInt(bus -> {
            int count = 0;
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                ItemStack stack = bus.getStackInSlot(i);
                if (stack != null && stack.getItem() == fixture && stack.getItemDamage() == metadata)
                    count += stack.stackSize;
            }
            return count;
        }).sum();
    }

    private int outputCount(int metadata) {
        return machine.mOutputBusses.stream().mapToInt(bus -> {
            int count = 0;
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                ItemStack stack = bus.getStackInSlot(i);
                if (stack != null && stack.getItem() == fixture && stack.getItemDamage() == metadata)
                    count += stack.stackSize;
            }
            return count;
        }).sum();
    }

    private ItemStack stack(int metadata, int amount) {
        return new ItemStack(fixture, amount, metadata);
    }

    private ExecutionCoreHatch core(CoreType type) {
        return prototypes.stream()
            .filter(hatch -> hatch instanceof ExecutionCoreHatch execution && execution.getCoreType() == type)
            .map(hatch -> (ExecutionCoreHatch) hatch).findFirst().get();
    }

    private ModularHatch install(int slot, ModularHatch prototype) {
        BaseMetaTileEntity tile = place(base.getWorld(), slots.get(slot).position,
            prototype.getStackForm(1).getItemDamage());
        tile.setOwnerName(base.getOwnerName());
        tile.setOwnerUuid(base.getOwnerUuid());
        if (prototype instanceof ExecutionCoreHatch) tile.enableWorking();
        return (ModularHatch) tile.getMetaTileEntity();
    }

    private void restoreModules() throws Exception {
        for (SavedBlock slot : slots) slot.restore();
        invoke(builder, "verifyStructure", new Class<?>[0]);
    }

    private void openModuleGui(EntityPlayerMP player) {
        int[] pos = { base.getXCoord() + 60, 100, -4 };
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(pos[0], pos[1], pos[2] - 3, 0, 0);
        int id = dynamicModules.get(guiIndex).getStackForm(1).getItemDamage();
        BaseMetaTileEntity tile = place(base.getWorld(), pos, id);
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.issueTileUpdate();
        guiHatch = (ParameterModuleHatch) tile.getMetaTileEntity();
        guiFrames = 0;
        clientGuiReady = clientGuiTileId = -1;
        guiRequestedAt = serverTicks;
        guiTarget = new GuiTarget(guiIndex, id, pos[0], pos[1], pos[2]);
        guiPhase = -1;
    }

    private static Rig assemblyRig(boolean electromagnetic) throws Exception {
        AssemblyPlantClientChecks builder = new AssemblyPlantClientChecks();
        Class<?> type = Class.forName(AssemblyPlantClientChecks.class.getName() + "$Rig");
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        String[][] shape = electromagnetic ? SuperconductingElectromagneticStructure.shape()
            : MageAssemblerStructure.shape();
        RecipeMap<?>[] maps = electromagnetic
            ? new RecipeMap<?>[] { RecipeMaps.polarizerRecipes, RecipeMaps.electroMagneticSeparatorRecipes,
                RecipeMaps.electrolyzerRecipes, GTNGRecipeMaps.LightningProcessorRecipes, RecipeMaps.arcFurnaceRecipes }
            : new RecipeMap<?>[] { RecipeMaps.assemblerRecipes, GoodGeneratorRecipeMaps.preciseAssemblerRecipes,
                RecipeMaps.circuitAssemblerRecipes };
        String slug = electromagnetic ? "electromagnetic-factory" : "mage-assembler";
        Object original = constructor.newInstance(slug,
            electromagnetic ? GTNGItemList.SuperconductingElectromagneticFactory : GTNGItemList.MageAssembler,
            electromagnetic ? GTNGMachineID.SuperconductingElectromagneticFactory.id : GTNGMachineID.MageAssembler.id,
            shape, electromagnetic ? 7 : 4, electromagnetic ? 20 : 5, 0, electromagnetic ? 'F' : 'a',
            electromagnetic ? "ABCDEH" : "cdefghi", electromagnetic ? "CDE" : "f", electromagnetic ? 1628 : 1377,
            electromagnetic ? 496 : 55, 0, maps, new double[0][]);
        field(builder.getClass(), "rig").set(builder, original);
        field(builder.getClass(), "rigIndex").setInt(builder, electromagnetic ? 1 : 0);
        field(builder.getClass(), "shape").set(builder, shape);
        return new Rig(slug, builder, electromagnetic ? 'F' : 'a');
    }

    private static BaseMetaTileEntity place(World world, int[] pos, int id) {
        world.setBlock(pos[0], pos[1], pos[2], GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(pos[0], pos[1], pos[2]);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.disableWorking();
        return tile;
    }

    private static Object invoke(Object owner, String name, Class<?>[] parameters, Object... args) throws Exception {
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                try {
                    return method.invoke(owner, args);
                } catch (InvocationTargetException failure) {
                    if (failure.getCause() instanceof Exception exception) throw exception;
                    if (failure.getCause() instanceof Error error) throw error;
                    throw failure;
                }
            } catch (NoSuchMethodException missing) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Object value(Object owner, String name) throws Exception {
        return field(owner.getClass(), name).get(owner);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static boolean hasField(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(field -> field.getName().equals(name));
    }

    private static void collectFields(IWidget widget, List<TextFieldWidget> fields) {
        if (widget instanceof TextFieldWidget field) fields.add(field);
        for (IWidget child : widget.getChildren()) collectFields(child, fields);
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.modularMachine.output"));
        folder.mkdirs();
        return folder;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(),
                ("FAIL\n" + trace + "\n" + report).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportingFailure) {
            reportingFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Rig {

        private final String slug;
        private final Object builder;
        private final char hatchSymbol;

        private Rig(String slug, Object builder, char hatchSymbol) {
            this.slug = slug;
            this.builder = builder;
            this.hatchSymbol = hatchSymbol;
        }
    }

    private static final class GuiTarget {

        private final int index, id, x, y, z;

        private GuiTarget(int index, int id, int x, int y, int z) {
            this.index = index;
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class SavedBlock {

        private final World world;
        private final int[] position;
        private final Block block;
        private final int metadata;

        private SavedBlock(World world, int[] position) {
            this.world = world;
            this.position = position;
            block = world.getBlock(position[0], position[1], position[2]);
            metadata = world.getBlockMetadata(position[0], position[1], position[2]);
        }

        private void restore() {
            world.setBlock(position[0], position[1], position[2], block, metadata, 3);
        }
    }

    private final class NativeTooltipScreen extends GuiScreen {

        private final ItemStack stack;
        private final boolean shift;
        private final String filename;
        private int tooltipX, tooltipY;

        private NativeTooltipScreen(ItemStack stack, boolean shift, String filename) {
            this.stack = stack;
            this.shift = shift;
            this.filename = filename;
        }

        @Override
        public void initGui() {
            try {
                List<String> lines = withTooltipShift(shift, () -> stack.getTooltip(mc.thePlayer, false));
                Files.write(new File(output(), filename + ".txt").toPath(), lines, StandardCharsets.UTF_8);
                require(lines.size() > 1, "Native machine item tooltip contains a description: " + filename);
                IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[stack.getItemDamage()];
                require(prototype instanceof ISecondaryDescribable, "Native controller exposes both descriptions");
                ISecondaryDescribable descriptions = (ISecondaryDescribable) prototype;
                String[] expected = withTooltipShift(shift,
                    () -> shift ? descriptions.getSecondaryDescription() : descriptions.getPrimaryDescription());
                String actual = tooltipText(String.join("\n", lines));
                for (String line : lines) {
                    String text = tooltipText(line);
                    String lower = text.toLowerCase(Locale.ROOT);
                    require(!text.contains("维护仓") && !lower.contains("maintenance hatch"),
                        "Native tooltip has no maintenance-hatch requirement: " + filename + "; line=" + text);
                    if (
                        text.contains("能源仓") || lower.contains("energy hatch")
                    ) require(
                        !text.contains("最多") && !lower.contains("at most")
                            && !lower.contains("up to")
                            && !lower.contains("maximum")
                            && !text.matches(".*[01]\\s*[-–—]\\s*[123].*"),
                        "Native tooltip has no old energy-hatch count limit: " + filename + "; line=" + text);
                }
                if (!shift) {
                    String prefix = tooltipLocale.equals("zh_CN") ? "模块化机器：" : "Modular Machine:";
                    String typeLine = null;
                    for (String line : lines) {
                        String text = tooltipText(line);
                        require(!text.contains("机器类型:") && !text.contains("机器类型：") && !text.contains("Machine Type:"),
                            "Native tooltip has no old machine-type label: " + filename);
                        if (text.startsWith(prefix)) {
                            require(typeLine == null, "Native tooltip has one modular machine type line: " + filename);
                            typeLine = text;
                        }
                    }
                    require(typeLine != null && typeLine.length() > prefix.length(),
                        "Native tooltip has the localized modular machine type label: " + filename);
                    require(!typeLine.contains("gt.recipe.") && !typeLine.contains("recipe.gtnotgood."),
                        "Native modular machine type has no recipe translation keys: " + filename);
                }
                for (String source : expected) {
                    String text = tooltipText(source);
                    require(text.isEmpty() || actual.contains(text),
                        "Native generated item tooltip differs from " + (shift ? "secondary" : "primary")
                            + " description: "
                            + filename
                            + "; missing="
                            + text);
                }
                FontRenderer font = stack.getItem().getFontRenderer(stack);
                if (font == null) font = fontRendererObj;
                int textWidth = lines.stream().mapToInt(font::getStringWidth).max().orElse(0);
                int textHeight = 8 + Math.max(0, lines.size() - 1) * 10 + (lines.size() > 1 ? 2 : 0);
                require(textWidth + 48 <= width && textHeight + 64 <= height,
                    "Native tooltip does not fit without clipping: " + filename
                        + "; text="
                        + textWidth
                        + "x"
                        + textHeight
                        + "; viewport="
                        + width
                        + "x"
                        + height
                        + "; lines="
                        + lines.size());
                tooltipX = (width - textWidth) / 2 - 12;
                tooltipY = (height - textHeight) / 2 + 12;
            } catch (Throwable failure) {
                fail(failure);
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            try {
                drawRect(0, 0, width, height, 0xFF303238);
                drawCenteredString(fontRendererObj, filename, width / 2, 12, 0xFFFFFF);
                withTooltipShift(shift, () -> {
                    renderToolTip(stack, tooltipX, tooltipY);
                    return null;
                });
            } catch (Throwable failure) {
                fail(failure);
            }
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }

    private static final class ModuleIconsScreen extends GuiScreen {

        private final List<ModularHatch> modules;
        private final RenderItem renderer = new RenderItem();

        private ModuleIconsScreen(List<ModularHatch> modules) {
            this.modules = modules;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, width, height, 0xFF303238);
            drawCenteredString(fontRendererObj, "TST modules: original hatch overlays", width / 2, 12, 0xFFFFFF);
            RenderHelper.enableGUIStandardItemLighting();
            for (int i = 0; i < modules.size(); i++) {
                int x = 12 + i % 8 * 78, y = 42 + i / 8 * 60;
                ItemStack stack = modules.get(i).getStackForm(1);
                renderer.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x + 20, y);
                fontRendererObj.drawString(Integer.toString(stack.getItemDamage()), x + 14, y + 22, 0xFFFFFF);
                fontRendererObj.drawString(modules.get(i).getModuleType().name().substring(0, 3), x + 17, y + 34,
                    0xAAAAAA);
            }
            RenderHelper.disableStandardItemLighting();
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }

    private static final class PreflightLogic extends ModularProcessingLogic {

        private final int itemConsumption, fluidConsumption;

        private PreflightLogic(int itemConsumption, int fluidConsumption) {
            this.itemConsumption = itemConsumption;
            this.fluidConsumption = fluidConsumption;
        }

        @Override
        protected CheckRecipeResult processRecipe() {
            if (inputItems != null)
                for (ItemStack stack : inputItems) if (stack != null) stack.stackSize -= itemConsumption;
            if (inputFluids != null)
                for (FluidStack stack : inputFluids) if (stack != null) stack.amount -= fluidConsumption;
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }
    }

    private static final class PreflightNetwork {

        private final Map<Integer, Long> items = new HashMap<>();
        private double aeEnergy = 1000;
        private boolean offline;
        private int modulations;
        private final AENetworkProxy proxy;

        @SuppressWarnings("unchecked")
        private PreflightNetwork() {
            IMEMonitor<IAEItemStack> itemInventory = (IMEMonitor<IAEItemStack>) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { IMEMonitor.class }, (owner, method, args) -> {
                    if (!method.getName().equals("extractItems")) throw new AssertionError(method.getName());
                    if (args[1] != Actionable.SIMULATE) modulations++;
                    IAEItemStack request = (IAEItemStack) args[0];
                    long available = items.getOrDefault(request.getItemStack().getItemDamage(), 0L);
                    return request.copy().setStackSize(Math.min(available, request.getStackSize()));
                });
            IMEMonitor<IAEFluidStack> fluidInventory = (IMEMonitor<IAEFluidStack>) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { IMEMonitor.class }, (owner, method, args) -> {
                    if (!method.getName().equals("extractItems")) throw new AssertionError(method.getName());
                    if (args[1] != Actionable.SIMULATE) modulations++;
                    IAEFluidStack request = (IAEFluidStack) args[0];
                    return request.copy().setStackSize(Math.min(1000, request.getStackSize()));
                });
            IEnergyGrid energy = (IEnergyGrid) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IEnergyGrid.class }, (owner, method, args) -> {
                    if (!method.getName().equals("extractAEPower")) throw new AssertionError(method.getName());
                    if (args[1] != Actionable.SIMULATE) modulations++;
                    return Math.min(aeEnergy, ((Number) args[0]).doubleValue());
                });
            IStorageGrid storage = (IStorageGrid) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IStorageGrid.class }, (owner, method, args) -> switch (method.getName()) {
                case "getItemInventory" -> itemInventory;
                case "getFluidInventory" -> fluidInventory;
                default -> throw new AssertionError(method.getName());
                });
            proxy = new AENetworkProxy(null, "modularQa", null, false) {

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public IStorageGrid getStorage() throws GridAccessException {
                    if (offline) throw new GridAccessException();
                    return storage;
                }

                @Override
                public IEnergyGrid getEnergy() {
                    return energy;
                }
            };
        }
    }
}
