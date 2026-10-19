package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.utils.item.SlotItemHandler;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnotgood.common.api.gui.FluidEntryInfo;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidDriller;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.machine.VoidDrillerFluidPool;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.elements.TextField;
import com.xyp.ldlib.gui.ui.elements.VirtualScrollerView;
import com.xyp.ldlib.integration.modularui.LDLibModularScreen;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.GTMod;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.VoidingMode;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.objects.GTUODimension;
import gregtech.api.objects.GTUODimensionList;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.StructureError;
import gregtech.common.misc.WirelessNetworkManager;
import gtneioreplugin.plugin.item.ItemDimensionDisplay;
import gtnhintergalactic.recipe.GasSiphonRecipes;
import me.eigenraven.lwjgl3ify.api.InputEvents;

@Mod(
    modid = "voiddrillerqa",
    name = "Void Driller QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class VoidDrillerClientChecks {

    private BaseMetaTileEntity base;
    private volatile LargeVoidDriller machine;
    private MTEHatchOutput outputHatch;
    private MTEHatchInput inputHatch;
    private Map<String, GTUODimension> previousDimensions;
    private GTUODimensionList configuration;
    private int[] previousBlacklist;
    private BigInteger initialBalance;
    private final StringBuilder report = new StringBuilder();
    private boolean started, openedFromPrimary;
    private volatile boolean checked, finished;
    private volatile int requested, verified;
    private int clientTicks, ticks, productionStarted, frames, stage;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.voidDriller.qa")) return;
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0.0f);
            require(++clientTicks <= 6000, "client timeout at GUI stage " + stage);
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("void-driller-qa-" + System.currentTimeMillis(), "Void Driller QA",
                    new WorldSettings(28530L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            require(++ticks <= 5000, "server timeout at GUI stage " + stage);
            if (machine == null) {
                setup(player);
                verifyConfiguration();
                verifyDimensionItemsAndGases();
                verifyWireless();
                verifyOverclockAndPersistence();
                verifyFailures();
                verifyAtomicPendingOutput();
                verifySelectionSnapshot();
                machine.mOutputFluids = null;
                machine.mMaxProgresstime = machine.mProgresstime = 0;
                machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = true;
                machine.mSolderingTool = machine.mCrowbar = true;
                machine.mEfficiency = 10000;
                initialBalance = BigInteger.TEN.pow(18);
                WirelessNetworkManager.setUserEU(player.getUniqueID(), initialBalance);
                productionStarted = ticks;
                base.enableWorking();
                return;
            }
            if (!checked) {
                if (outputHatch.getFluidAmount() > 0) {
                    require(
                        outputHatch.getFluid().getFluid() == FluidRegistry.WATER
                            || outputHatch.getFluid().getFluid() == FluidRegistry.LAVA,
                        "native output hatch receives fluid");
                    require(outputHatch.getFluidAmount() >= 1000, "completed cycle emits at least one bucket");
                    require(WirelessNetworkManager.getUserEU(player.getUniqueID()).compareTo(initialBalance) < 0,
                        "native production ticks debit the wireless owner");
                    base.disableWorking();
                    checked = true;
                    report.append("完整结构通过原生 tick 完成产液周期并消耗无线电量。\n");
                    machine.onRightclick(base, player);
                } else {
                    require(ticks - productionStarted < 1800, "formed driller completes a native production cycle");
                }
                return;
            }
            if (requested == 1 && machine.getOverclockLevel() == 1) verified = 1;
            if (requested == 2 && machine.getDirectionalMode()) verified = 2;
            if (requested == 3 && machine.getFluidEntries().stream().anyMatch(entry -> entry.aimed)) verified = 3;
            if (requested == 4 && machine.getFluidEntries().stream().noneMatch(entry -> entry.aimed)) verified = 4;
            if (requested == 5 && machine.getGasDimensionMask() == 1) verified = 5;
            if (requested == 6 && machine.getGasDimensionMask() == 0) verified = 6;
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        configuration = GTMod.proxy.mUndergroundOil;
        previousDimensions = new LinkedHashMap<>(configuration.getDimensionList());
        previousBlacklist = configuration.blackList;
        configuration.blackList = new int[0];
        configuration.getDimensionList().clear();
        configuration.getDimensionList().put("0", nativeDimension());
        World world = player.worldObj;
        world.setWorldTime(6000);
        base = place(world, 0, 12, 0, GTNGMachineID.LargeVoidDriller.id);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        machine = (LargeVoidDriller) base.getMetaTileEntity();
        machine.construct(new ItemStack(Items.diamond), false);
        outputHatch = (MTEHatchOutput) place(world, -2, 12, 0, ItemList.Hatch_Output_EV.get(1).getItemDamage())
            .getMetaTileEntity();
        place(world, 2, 12, 0, ItemList.Hatch_Maintenance.get(1).getItemDamage());
        inputHatch = (MTEHatchInput) place(world, -2, 11, 0, ItemList.Hatch_Input_EV.get(1).getItemDamage())
            .getMetaTileEntity();
        for (int warmup = 0; warmup < 10; warmup++) {
            ((BaseMetaTileEntity) outputHatch.getBaseMetaTileEntity()).updateEntity();
            ((BaseMetaTileEntity) inputHatch.getBaseMetaTileEntity()).updateEntity();
        }
        List<StructureError> errors = new ArrayList<>();
        machine.clearHatches();
        machine.checkMachine(base, null, errors);
        require(errors.isEmpty(),
            "creative construction with output, input and maintenance hatches validates: " + errors);
        require(machine.checkStructure(true, base), "native structure check accepts complete 7 x 9 x 7 driller");
        require(machine.mEnergyHatches.isEmpty() && machine.getExoticEnergyHatches().isEmpty(),
            "no energy hatch required");
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(3, 13, -3, 40, 15);
        report.append("真实 7×9×7 结构、输出仓与维护仓验证通过，无能源仓。\n");
    }

    private static GTUODimension nativeDimension() {
        ConfigCategory dimension = new ConfigCategory("voidDrillerQa");
        dimension.put("Dimension", new Property("Dimension", "0", Property.Type.STRING));
        fluidCategory(dimension, "water", 1000, 1000, 1);
        fluidCategory(dimension, "lava", 2000, 2000, 3);
        return new GTUODimension(dimension);
    }

    private static void fluidCategory(ConfigCategory parent, String fluid, int minimum, int maximum, int chance) {
        ConfigCategory category = new ConfigCategory(fluid, parent);
        category.put("Registry", new Property("Registry", fluid, Property.Type.STRING));
        category.put("MinAmount", new Property("MinAmount", Integer.toString(minimum), Property.Type.INTEGER));
        category.put("MaxAmount", new Property("MaxAmount", Integer.toString(maximum), Property.Type.INTEGER));
        category.put("Chance", new Property("Chance", Integer.toString(chance), Property.Type.INTEGER));
    }

    private void verifyConfiguration() {
        machine.forceRefreshPool();
        require(machine.isPoolValid(), "native GT underground fluid configuration supplies the local pool");
        List<FluidEntryInfo> entries = machine.getFluidEntries();
        require(entries.size() == 2, "water and lava remain distinct pool entries");
        double sum = 0;
        for (FluidEntryInfo entry : entries) {
            sum += entry.weight;
            boolean water = entry.fluid.getFluid() == FluidRegistry.WATER;
            require(entry.minAmount == (water ? 1000 : 2000) && entry.maxAmount == entry.minAmount,
                "native configured amount range preserved");
            require(Math.abs(entry.weight - (water ? 0.625 : 0.375)) < 0.000001,
                "underground and ocean weights normalized together");
            require(entry.dimAbbrs.contains("Ow"), "local dimension abbreviation displayed");
        }
        require(Math.abs(sum - 1) < 0.000001, "fluid pool weights sum to one");
        require(machine.getPluginSlotInventory().getSizeInventory() == 25, "twenty-five dimension slots");
        machine.getPluginSlotInventory().setInventorySlotContents(24, new ItemStack(Items.diamond));
        require(machine.getPluginSlotInventory().getStackInSlot(24) == null, "dimension slots reject ordinary items");
        report.append("GT 原生地下流体配置的种类、权重、范围、维度与 25 槽校验通过。\n");
    }

    private void verifyDimensionItemsAndGases() {
        ItemStack marker = ItemDimensionDisplay.getItem("Ne");
        require(marker != null && "Ne".equals(ItemDimensionDisplay.getDimension(marker)),
            "native Nether dimension item");
        require(marker.stackSize == 1, "native dimension marker contains exactly one item: " + marker.stackSize);
        machine.getPluginSlotInventory().setInventorySlotContents(24, marker);
        require(machine.getPluginSlotInventory().getStackInSlot(24) == marker,
            "dimension slot accepts the native marker before NBT serialization");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.getPluginSlotInventory().setInventorySlotContents(24, null);
        machine.loadNBTData(saved);
        ItemStack restored = machine.getPluginSlotInventory().getStackInSlot(24);
        System.out.println("VOID_DRILLER_QA dimension marker=" + marker.writeToNBT(new NBTTagCompound())
            + ", saved="
            + saved.getTagList("dimensions", 10)
            + ", restored="
            + (restored == null ? "null" : restored.writeToNBT(new NBTTagCompound()))
            + ", dimension="
            + VoidDrillerFluidPool.readDimensionAbbr(restored)
            + ", display="
            + machine.getDimensionDisplayName());
        List<FluidEntryInfo> selected = machine.getFluidEntries();
        for (FluidEntryInfo entry : selected)
            System.out.println("VOID_DRILLER_QA selected fluid=" + entry.fluid.getFluid().getName()
                + ", dimensions="
                + entry.dimAbbrs
                + ", weight="
                + entry.weight
                + ", range="
                + entry.minAmount
                + ".."
                + entry.maxAmount);
        require(ItemStack.areItemStacksEqual(marker, restored),
            "twenty-fifth dimension slot preserves complete item NBT");
        require("Ne".equals(VoidDrillerFluidPool.readDimensionAbbr(restored)),
            "restored dimension item resolves Nether");
        require(selected.size() == 1, "Nether dimension selects exactly one fixture fluid, actual=" + selected.size());
        require(selected.get(0).fluid.getFluid() == FluidRegistry.LAVA, "Nether fixture selects native lava");
        machine.getPluginSlotInventory().setInventorySlotContents(24, null);
        String[] planets = { "planet.jupiter", "planet.saturn", "planet.uranus", "planet.neptune" };
        for (int index = 0; index < planets.length; index++) {
            machine.toggleGasDimension(index);
            var recipe = GasSiphonRecipes.RECIPES.get(planets[index]);
            require(recipe != null && !recipe.depths.isEmpty(), "native gas siphon recipe for " + planets[index]);
            for (FluidStack expected : recipe.depths.values()) require(
                machine.getFluidEntries().stream()
                    .anyMatch(entry -> entry.fluid.isFluidEqual(expected) && entry.minAmount == expected.amount
                        && entry.maxAmount == expected.amount),
                "native gas fluid and amount preserved for " + planets[index]);
            saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            machine.toggleGasDimension(index);
            machine.loadNBTData(saved);
            require(machine.getGasDimensionMask() == (1 << index), "gas selection NBT at index " + index);
            machine.toggleGasDimension(index);
        }
        machine.toggleGasDimension(-1);
        machine.toggleGasDimension(4);
        require(machine.getGasDimensionMask() == 0 && machine.getFluidEntries().size() == 2,
            "invalid gas indexes are ignored and local water/lava pool restored");
        var oceans = VoidDrillerFluidPool.getEntries(0, Arrays.asList("Eu", "TE", "Ti", "BC"));
        for (String dimension : Arrays.asList("Eu", "TE", "Ti", "BC")) {
            require(
                oceans.stream().anyMatch(
                    entry -> entry.dimension.equals(dimension) && entry.minAmount == 1000 && entry.maxAmount == 1000),
                "registered planetary ocean for " + dimension);
        }
        report.append("真实维度物品第 25 槽 NBT、下界流体选择、四巨行星原生气体及数量、气体选择 NBT 与非法索引通过。\n");
        report.append("木卫二、天仓五e、土卫六与巴纳德C的已注册海洋流体各输出 1000 L。\n");
    }

    private void verifyWireless() {
        WirelessNetworkManager.strongCheckOrAddUser(base.getOwnerUuid());
        WirelessNetworkManager.setUserEU(base.getOwnerUuid(), BigInteger.valueOf(1000));
        require(machine.getEnergyCostPerTick() == 480, "default wireless consumption is 480 EU/t");
        require(machine.drainEnergyInput(480), "wireless debit succeeds without a hatch");
        require(WirelessNetworkManager.getUserEU(base.getOwnerUuid()).equals(BigInteger.valueOf(520)),
            "wireless debit charges exactly 480 EU");
        require(!machine.drainEnergyInput(521), "insufficient wireless balance blocks the debit");
        require(WirelessNetworkManager.getUserEU(base.getOwnerUuid()).equals(BigInteger.valueOf(520)),
            "failed wireless debit preserves the balance");
        report.append("无线电网精确扣电及余额不足保护通过。\n");
    }

    private void verifyOverclockAndPersistence() {
        int[] durations = { 400, 100, 25, 6, 1 };
        for (int level = 0; level < durations.length; level++) {
            require(machine.getOverclockLevel() == level && machine.getCycleDurationTicks() == durations[level],
                "perfect overclock duration at level " + level);
            require(machine.getEnergyCostPerTick() == 480L * (1L << (level * 2)),
                "perfect overclock energy at level " + level);
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            machine.mMaxProgresstime = 77;
            machine.cycleOverclockLevel();
            require(machine.mMaxProgresstime == 77, "overclock configuration does not mutate an active cycle");
            machine.loadNBTData(saved);
            require(machine.getOverclockLevel() == level, "overclock NBT round trip at level " + level);
            machine.cycleOverclockLevel();
        }
        require(machine.getOverclockLevel() == 0, "overclock wraps after level four");
        String first = machine.getFluidEntries().get(0).fluid.getFluid().getName();
        machine.toggleFluidSelection(first);
        require(machine.getFluidEntries().stream().anyMatch(entry -> entry.filtered), "ordinary mode records a filter");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.clearCurrentModeConfig();
        machine.loadNBTData(saved);
        machine.forceRefreshPool();
        require(machine.getFluidEntries().stream().anyMatch(entry -> entry.filtered), "filter survives NBT round trip");
        machine.clearCurrentModeConfig();
        machine.toggleDirectionalMode();
        machine.toggleFluidSelection(first);
        saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.clearCurrentModeConfig();
        machine.toggleDirectionalMode();
        machine.loadNBTData(saved);
        machine.forceRefreshPool();
        require(machine.getDirectionalMode() && machine.getFluidEntries().stream().anyMatch(entry -> entry.aimed),
            "directional mode and selection survive NBT round trip");
        require(
            machine.getPluginSlotInventory().getSizeInventory() == 25
                && machine.getPluginSlotInventory().getStackInSlot(24) == null,
            "empty dimension slots survive NBT round trip");
        machine.clearCurrentModeConfig();
        machine.toggleDirectionalMode();
        report.append("五档完美超频、当前周期稳定性、过滤与定向选择 NBT 往返通过。\n");
    }

    private void verifyFailures() {
        ItemStack unsupported = ItemDimensionDisplay.getItem("ED");
        require(unsupported != null, "native End dimension item available for empty source fixture");
        machine.getPluginSlotInventory().setInventorySlotContents(0, unsupported);
        machine.forceRefreshPool();
        require(!machine.isPoolValid() && !machine.checkProcessing().wasSuccessful(),
            "unsupported dimension refuses processing");
        machine.getPluginSlotInventory().setInventorySlotContents(0, null);
        machine.forceRefreshPool();
        machine.toggleDirectionalMode();
        machine.clearCurrentModeConfig();
        require(!machine.checkProcessing().wasSuccessful(), "empty directional selection refuses processing");
        machine.toggleFluidSelection(FluidRegistry.WATER.getName());
        inputHatch.setFillableStack(null);
        require(!machine.checkProcessing().wasSuccessful(),
            "directional drilling without UU-Matter refuses processing");
        machine.clearCurrentModeConfig();
        machine.toggleDirectionalMode();
        machine.toggleFluidSelection(FluidRegistry.LAVA.getName());
        outputHatch.setFillableStack(new FluidStack(FluidRegistry.WATER, outputHatch.getCapacity()));
        machine.setVoidingMode(VoidingMode.VOID_NONE);
        require(machine.checkProcessing() == CheckRecipeResultRegistry.FLUID_OUTPUT_FULL,
            "full protected native output hatch refuses a new cycle");
        require(outputHatch.getFluidAmount() == outputHatch.getCapacity(), "output protection preserves stored fluid");
        outputHatch.setFillableStack(null);
        machine.clearCurrentModeConfig();
        machine.forceRefreshPool();
        require(machine.checkProcessing().wasSuccessful(), "cleared configuration resumes normal processing");
        report.append("不支持维度、空定向、缺少 UU 与满输出仓均阻止启动；清理后可恢复。\n");
    }

    private void verifyAtomicPendingOutput() throws ReflectiveOperationException {
        machine.mMaxProgresstime = machine.mProgresstime = 0;
        machine.clearCurrentModeConfig();
        machine.toggleFluidSelection(FluidRegistry.LAVA.getName());
        machine.forceRefreshPool();
        setInternalFluid("pendingOutput", new FluidStack(FluidRegistry.WATER, 1000));
        int partialAmount = outputHatch.getCapacity() - 500;
        outputHatch.setFillableStack(new FluidStack(FluidRegistry.WATER, partialAmount));
        for (int attempt = 0; attempt < 3; attempt++) {
            require(machine.checkProcessing() == CheckRecipeResultRegistry.FLUID_OUTPUT_FULL,
                "partial output space blocks pending batch attempt " + attempt);
            require(outputHatch.getFluidAmount() == partialAmount,
                "failed pending transfer does not commit partial fluid attempt " + attempt);
            require(internalFluid("pendingOutput").amount == 1000,
                "failed pending transfer preserves the complete batch attempt " + attempt);
        }
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        setInternalFluid("pendingOutput", null);
        machine.loadNBTData(saved);
        require(
            machine.checkProcessing() == CheckRecipeResultRegistry.FLUID_OUTPUT_FULL
                && outputHatch.getFluidAmount() == partialAmount
                && internalFluid("pendingOutput").amount == 1000,
            "pending batch survives NBT reload without partial insertion or duplication");
        outputHatch.setFillableStack(null);
        require(machine.checkProcessing().wasSuccessful(), "empty output hatch accepts the complete pending batch");
        require(outputHatch.getFluidAmount() == 1000 && outputHatch.getFluid().getFluid() == FluidRegistry.WATER
            && internalFluid("pendingOutput") == null, "pending recovery transfers exactly 1000 L once");
        machine.mMaxProgresstime = machine.mProgresstime = 0;
        machine.clearCurrentModeConfig();
        machine.forceRefreshPool();
        outputHatch.setFillableStack(null);
        report.append("待输出 1000 L 在仅余 500 L 空间时多次重试与 NBT 重载均不部分写入；排空后恰好转出一次。\n");
    }

    private void verifySelectionSnapshot() throws ReflectiveOperationException {
        machine.clearCurrentModeConfig();
        setInternalFluid("activeOutput", new FluidStack(FluidRegistry.WATER, 1000));
        machine.toggleFluidSelection(FluidRegistry.WATER.getName());
        require(internalFluid("activeOutput") == null, "idle filter change clears the unstarted fluid selection");
        machine.clearCurrentModeConfig();
        FluidStack snapshot = new FluidStack(FluidRegistry.WATER, 1000);
        setInternalFluid("activeOutput", snapshot);
        machine.mMaxProgresstime = 100;
        machine.mProgresstime = 10;
        machine.toggleFluidSelection(FluidRegistry.WATER.getName());
        machine.toggleDirectionalMode();
        machine.forceRefreshPool();
        machine.clearCurrentModeConfig();
        require(
            internalFluid("activeOutput") == snapshot && snapshot.amount == 1000
                && machine.mMaxProgresstime == 100
                && machine.mProgresstime == 10,
            "running cycle retains its fluid snapshot and progress through configuration changes");
        machine.toggleDirectionalMode();
        machine.clearCurrentModeConfig();
        machine.mMaxProgresstime = machine.mProgresstime = 0;
        machine.forceRefreshPool();
        require(internalFluid("activeOutput") == null, "idle refresh clears the finished cycle fixture");
        report.append("待机修改过滤重选流体，运行中修改过滤、模式与刷新保留当前流体与进度快照。\n");
    }

    private FluidStack internalFluid(String name) throws ReflectiveOperationException {
        Field field = LargeVoidDriller.class.getDeclaredField(name);
        field.setAccessible(true);
        return (FluidStack) field.get(machine);
    }

    private void setInternalFluid(String name, FluidStack fluid) throws ReflectiveOperationException {
        Field field = LargeVoidDriller.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(machine, fluid);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
            if (!(wrapper.getScreen() instanceof LDLibModularScreen screen)) {
                if (!openedFromPrimary && ++frames >= 60) {
                    capture("primary");
                    ButtonWidget<?> button = configButton(wrapper.getScreen().getMainPanel());
                    require(button != null, "primary GUI has the real driller configuration button");
                    button.onMousePressed(0);
                    openedFromPrimary = true;
                    frames = 0;
                }
                return;
            }
            require(openedFromPrimary, "right click reaches the primary GUI before configuration");
            if (++frames < 60 || requested != verified) return;
            frames = 0;
            switch (stage++) {
                case 0:
                    require(
                        screen.getMainPanel().getArea().width == 475 && screen.getMainPanel().getArea().height == 365,
                        "configuration panel is 475 x 365");
                    require(fluidRows(screen).getItems().size() == 2, "real GUI synchronizes both fluid rows");
                    verifyGuiSlots(mc);
                    capture("configuration");
                    click(screen, 290, 36);
                    requested = 1;
                    break;
                case 1:
                    click(screen, 50, 194);
                    requested = 2;
                    break;
                case 2:
                    click(screen, 425, 122);
                    requested = 3;
                    break;
                case 3:
                    capture("directional-selection");
                    click(screen, 415, 60);
                    break;
                case 4:
                    capture("clear-confirmation");
                    click(screen, 180, 158);
                    requested = 4;
                    break;
                case 5:
                    capture("cleared");
                    String name = fluidRows(screen).getItems().get(0).fluid.getLocalizedName();
                    click(screen, 250, 80);
                    screen.onTextEvent(new InputEvents.TextEvent(name));
                    for (char character : name.toCharArray()) screen.onKeyPressed(character, Keyboard.KEY_NONE);
                    screen.onKeyEvent(new InputEvents.KeyEvent(Keyboard.KEY_BACK, 0, 0, 0, 0,
                        InputEvents.KeyAction.PRESSED, (short) 0, 0L));
                    screen.onKeyPressed('\0', Keyboard.KEY_BACK);
                    screen.onTextEvent(new InputEvents.TextEvent(name.substring(name.length() - 1)));
                    screen.onKeyPressed(name.charAt(name.length() - 1), Keyboard.KEY_NONE);
                    require(searchField(screen).getText().equals(name),
                        "native text and backspace ignore legacy duplicates");
                    break;
                case 6:
                    capture("fluid-search");
                    require(fluidRows(screen).getItems().size() == 1, "fluid search narrows the native list");
                    click(screen, 250, 80);
                    screen.onMousePressed(1);
                    screen.onMouseRelease(1);
                    screen.onTextEvent(new InputEvents.TextEvent(FluidRegistry.LAVA.getName()));
                    for (char character : FluidRegistry.LAVA.getName().toCharArray())
                        screen.onKeyPressed(character, Keyboard.KEY_NONE);
                    require(searchField(screen).getText().equals(FluidRegistry.LAVA.getName()),
                        "registry search ignores legacy character duplicates");
                    require(fluidRows(screen).getItems().size() == 1,
                        "registry name search accepts native text events");
                    screen.onMousePressed(1);
                    screen.onMouseRelease(1);
                    click(screen, 40, 36);
                    requested = 5;
                    break;
                case 7:
                    require(fluidRows(screen).getItems().size() >= 4,
                        "Jupiter GUI toggle synchronizes native gas rows");
                    capture("gas-dimensions");
                    click(screen, 40, 36);
                    requested = 6;
                    break;
                case 8:
                    require(fluidRows(screen).getItems().size() == 2,
                        "Jupiter GUI toggle restores local water/lava rows");
                    report.append("真实两层 GUI、25 维度槽、36 玩家槽、超频、定向、选择、清空、搜索与巨行星开关通过。\n");
                    finish(null);
                    break;
                default:
                    throw new AssertionError("Unexpected GUI stage");
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void verifyGuiSlots(Minecraft mc) {
        int playerSlots = 0, dimensionSlots = 0;
        for (Object entry : mc.thePlayer.openContainer.inventorySlots) {
            Slot slot = (Slot) entry;
            if (slot instanceof SlotItemHandler handler && handler.getItemHandler().getSlots() == 25) dimensionSlots++;
            for (int index = 0; index < 36; index++)
                if (slot.isSlotInInventory(mc.thePlayer.inventory, index)) playerSlots++;
        }
        require(dimensionSlots == 25 && playerSlots == 36, "configuration has 25 dimension and 36 player slots");
    }

    @SuppressWarnings("unchecked")
    private static VirtualScrollerView<FluidEntryInfo> fluidRows(LDLibModularScreen screen) throws Exception {
        Field controls = LDLibModularScreen.class.getDeclaredField("controls");
        controls.setAccessible(true);
        Object view = controls.get(screen);
        Field rows = view.getClass().getDeclaredField("rows");
        rows.setAccessible(true);
        return (VirtualScrollerView<FluidEntryInfo>) rows.get(view);
    }

    private static TextField searchField(LDLibModularScreen screen) throws Exception {
        Field controls = LDLibModularScreen.class.getDeclaredField("controls");
        controls.setAccessible(true);
        for (UIElement child : ((UIElement) controls.get(screen)).getChildren()) {
            if (child instanceof TextField field) return field;
        }
        throw new AssertionError("Missing native search field");
    }

    private static ButtonWidget<?> configButton(IWidget widget) {
        if (
            widget instanceof ButtonWidget<?>button && button.isSynced()
                && button.getSyncHandler().getKey().startsWith("gtng.vd.openConfig")
        ) return button;
        for (IWidget child : widget.getChildren()) {
            ButtonWidget<?> found = configButton(child);
            if (found != null) return found;
        }
        return null;
    }

    private static void click(LDLibModularScreen screen, int x, int y) {
        var area = screen.getMainPanel().getArea();
        screen.getContext().updateState(area.x + x, area.y + y, 0);
        screen.onMousePressed(0);
        screen.onMouseRelease(0);
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name + ".png", mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.voidDriller.output", "."));
        folder.mkdirs();
        return folder;
    }

    private synchronized void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        if (configuration != null && previousDimensions != null) {
            configuration.getDimensionList().clear();
            configuration.getDimensionList().putAll(previousDimensions);
            configuration.blackList = previousBlacklist;
        }
        try {
            if (failure == null) {
                Files.write(new File(output(), "result.txt").toPath(),
                    ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
                System.out.println("VOID_DRILLER_QA_PASS\n" + report);
            } else {
                failure.printStackTrace();
                StringWriter trace = new StringWriter();
                failure.printStackTrace(new PrintWriter(trace));
                Files.write(new File(output(), "result.txt").toPath(),
                    ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("VOID_DRILLER_QA: " + message);
    }
}
