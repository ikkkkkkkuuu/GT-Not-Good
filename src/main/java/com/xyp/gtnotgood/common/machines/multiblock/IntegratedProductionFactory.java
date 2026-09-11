package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.IntegratedProductionFactoryGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGCleanWirelessMultiMachineBase;
import com.xyp.gtnotgood.utils.machine.factory.FactoryBalancer;
import com.xyp.gtnotgood.utils.machine.factory.FactoryBatching;
import com.xyp.gtnotgood.utils.machine.factory.FactoryCycles;
import com.xyp.gtnotgood.utils.machine.factory.FactoryGraph;
import com.xyp.gtnotgood.utils.machine.factory.FactoryInputs;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPatternExport;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPreview;
import com.xyp.gtnotgood.utils.machine.factory.FactoryProgress;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRecipeCatalog;
import com.xyp.gtnotgood.utils.machine.factory.FactoryReservations;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRouting;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRuntime;
import com.xyp.gtnotgood.utils.machine.factory.FactoryText;

import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.HatchElementBuilder;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.blocks.ItemMachines;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.WirelessNetworkManager;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;

/**
 * A bounded production graph executor with automatic wired/wireless selection and a ModularUI2 editor.
 * Structure/maintenance remain managed by GT; runMachine executes graph jobs instead of the single-recipe loop.
 * Draft changes never mutate installed jobs. Submission first finishes and unloads the old graph without voiding.
 */
public class IntegratedProductionFactory extends GTNGCleanWirelessMultiMachineBase<IntegratedProductionFactory>
    implements ISurvivalConstructable {

    private static final String MAIN = "main";
    private static final IStructureDefinition<IntegratedProductionFactory> STRUCTURE = StructureDefinition
        .<IntegratedProductionFactory>builder()
        .addShape(
            MAIN,
            transpose(new String[][] { { "CCC", "CCC", "CCC" }, { "C~C", "CAC", "CCC" }, { "CCC", "CCC", "CCC" } }))
        .addElement('A', isAir())
        .addElement(
            'C',
            // Prefer casing placement in previews; real hatches still fall through to their registration element.
            ofChain(
                Casings.RobustTungstenSteelMachineCasing.asElement(),
                HatchElementBuilder.<IntegratedProductionFactory>builder()
                    .anyOf(
                        HatchElement.InputBus,
                        HatchElement.InputHatch,
                        HatchElement.OutputBus,
                        HatchElement.OutputHatch,
                        HatchElement.Energy.or(HatchElement.ExoticEnergy))
                    .casingIndex(Casings.RobustTungstenSteelMachineCasing.textureId)
                    .hint(1)
                    .build()))
        .build();

    private FactoryGraph draft = new FactoryGraph();
    private FactoryGraph installed = new FactoryGraph();
    private FactoryGraph pending = new FactoryGraph();
    private final FactoryRuntime runtime = new FactoryRuntime();
    private final FactoryReservations reservations = new FactoryReservations();
    private final Map<Integer, FactoryText> nodeStatus = new HashMap<>();
    private final Random random = new Random();
    private boolean draining;
    private int schedulingCursor;
    /** Cached traversal timing and saved credits; rebuilt only when the installed graph changes. */
    private FactoryProgress lineProgress;
    private boolean lineTimingUnavailable;
    private NBTTagCompound savedLineProgress = new NBTTagCompound();
    private boolean itemOutputBlocked, fluidOutputBlocked;

    public boolean isDraining() {
        return draining;
    }

    private boolean routingLocked;

    public boolean isRoutingLocked() {
        return routingLocked;
    }

    private Map<Integer, List<FactoryGraph.Node>> cycleGroups;
    private final Map<Integer, FactoryCycles.Plan> cyclePlans = new HashMap<>();
    private FactoryText status = FactoryText.IDLE;
    private FactoryText editorStatus;
    private long lastBalanceTick = Long.MIN_VALUE;

    public IntegratedProductionFactory(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public IntegratedProductionFactory(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity base) {
        return new IntegratedProductionFactory(mName);
    }

    @Override
    public IStructureDefinition<IntegratedProductionFactory> getStructureDefinition() {
        return STRUCTURE;
    }

    @Override
    public void checkMachine(IGregTechTileEntity base, ItemStack stack, List<StructureError> errors) {
        setWirelessModeAvailable(false);
        setWirelessModeEnabled(false);
        if (!checkPiece(MAIN, 1, 1, 0, errors)) return;
        setWirelessModeAvailable(areEnergyHatchesEmpty());
        setWirelessModeEnabled(areEnergyHatchesEmpty());
        if (mOutputBusses.isEmpty() && mOutputHatches.isEmpty())
            errors.add(StructureErrors.of("GT5U.gui.text.structure_error.no_output"));
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(MAIN, stack, hintsOnly, 1, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment environment) {
        return mMachine ? -1 : survivalBuildPiece(MAIN, stack, 1, 1, 0, budget, environment, false, true);
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new IntegratedProductionFactoryGui(this);
    }

    /** Automatic job sizing may use the full signed-integer range when material, power and buffer limits permit. */
    @Override
    public int getMaxParallelRecipes() {
        return Integer.MAX_VALUE;
    }

    /** Node settings define routing ratios and bounded buffer watermarks, independently of automatic batch size. */
    private int effectiveParallel(FactoryGraph.Node node) {
        return Math.max(1, node.parallel);
    }

    public FactoryGraph getDraft() {
        return draft;
    }

    /** Submitted graph requirements only; reads reservations without consuming or probing ordinary inputs. */
    public List<NBTTagCompound> getRequirementTags() {
        List<NBTTagCompound> result = new ArrayList<>();
        if (draining) {
            for (ItemStack item : reservations.remaining()) {
                NBTTagCompound key = new NBTTagCompound();
                key.setTag("item", item.writeToNBT(new NBTTagCompound()));
                addRequirement(result, key, false);
            }
            return result;
        }
        FactoryGraph graph = installed;
        for (FactoryGraph.Node node : graph.nodes) {
            FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
            if (entry == null) continue;
            NBTTagCompound host = new NBTTagCompound();
            host.setString("map", entry.map.unlocalizedName);
            addRequirement(result, host, !draining && reservations.get(node.id, 0) != null);
            for (int i = 0; i < entry.recipe.mInputs.length; i++) {
                ItemStack input = entry.recipe.mInputs[i];
                if (input == null || input.stackSize != 0) continue;
                NBTTagCompound catalyst = new NBTTagCompound();
                ItemStack display = input.copy();
                display.stackSize = 1;
                catalyst.setTag("item", display.writeToNBT(new NBTTagCompound()));
                addRequirement(result, catalyst, !draining && reservations.get(node.id, i + 1) != null);
            }
        }
        return result;
    }

    /** Aggregates identical requirements while keeping actual deposited counts separate. */
    private static void addRequirement(List<NBTTagCompound> rows, NBTTagCompound key, boolean received) {
        for (NBTTagCompound row : rows) {
            if (!row.getCompoundTag("key")
                .equals(key)) continue;
            row.setInteger("required", row.getInteger("required") + 1);
            if (received) row.setInteger("received", row.getInteger("received") + 1);
            return;
        }
        NBTTagCompound row = new NBTTagCompound();
        row.setTag("key", key);
        row.setInteger("required", 1);
        row.setInteger("received", received ? 1 : 0);
        rows.add(row);
    }

    public NBTTagCompound getReservationsTag() {
        return reservations.write();
    }

    public String getEditorStatus() {
        return editorStatus == null ? getFactoryStatus() : editorStatus.text();
    }

    /** Reports remaining work and real buffers while unloading, including when paused or starved of power. */
    public String getDrainDetails() {
        if (!draining) return "";
        int jobs = 0;
        long items = 0, fluids = 0;
        for (FactoryRuntime.State state : runtime.states.values()) {
            if (state.remaining > 0) jobs++;
            for (ItemStack item : state.items) items += Math.max(0, item.stackSize);
            for (FluidStack fluid : state.fluids) fluids += Math.max(0, fluid.amount);
        }
        return FactoryText.DRAIN_JOBS.text() + jobs
            + " | "
            + FactoryText.DRAIN_ITEMS.text()
            + items
            + " | "
            + FactoryText.DRAIN_FLUIDS.text()
            + fluids
            + " L";
    }

    public String getFactoryStatus() {
        return (isWirelessModeActive() ? FactoryText.WIRELESS.text() : FactoryText.WIRED.text()) + " | "
            + status.text();
    }

    public String getNodeStatus(int id) {
        FactoryRuntime.State state = runtime.states.get(id);
        FactoryGraph.Node current = installed.find(id);
        FactoryGraph.Node edit = draft.find(id);
        if (current == null || edit == null || !current.recipe.equals(edit.recipe)) return "";
        if (state != null && state.remaining > 0)
            return (state.duration - state.remaining) + "/" + state.duration + " t";
        return nodeStatus.getOrDefault(id, FactoryText.INPUT)
            .text();
    }

    /** Server-only bounded commands; recipe data is always resolved from the local registry. */
    public void edit(int command, int id, int a, int b, String recipe) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || !base.isServerSide()) return;
        if (routingLocked && command != 6 && command != 16 && command != 18) return;
        if (command != 9) editorStatus = null;
        FactoryGraph.Node node = draft.find(id);
        switch (command) {
            case 18:
                exportPattern();
                break;
            case 0:
                if (FactoryRecipeCatalog.get(recipe) != null) draft.add(recipe);
                break;
            case 1:
                draft.remove(id);
                break;
            case 2:
                if (node != null) {
                    node.x = Math.max(0, Math.min(2048, a));
                    node.y = Math.max(0, Math.min(2048, b));
                }
                break;
            case 3:
                if (node != null && draft.find(a) != null && !node.sources.remove(a)) node.sources.add(a);
                break;
            case 4:
                if (node != null) {
                    node.parallel = Math.max(1, a);
                    node.overclocks = Math.max(0, Math.min(14, b));
                }
                break;
            case 5:
                if (!recipe.equals(FactoryRouting.encode(draft))) {
                    editorStatus = FactoryText.PREVIEW_STALE;
                    return;
                }
                FactoryRouting.connect(draft);
                if (!draft.hasTargetsForAllComponents()) {
                    editorStatus = FactoryText.NO_TARGET;
                    return;
                }
                for (FactoryGraph.Node candidate : draft.nodes) {
                    if (FactoryRecipeCatalog.get(candidate.recipe) == null) {
                        status = FactoryText.INVALID;
                        return;
                    }
                }
                routingLocked = true;
                pending = draft.copy();
                draining = true;
                status = FactoryText.DRAINING;
                break;
            case 16:
                draft.read(new NBTTagCompound());
            case 6:
                routingLocked = false;
                pending = new FactoryGraph();
                draining = true;
                status = FactoryText.DRAINING;
                break;
            case 7:
                draft.add("");
                break;
            case 12:
                if (node != null) node.target = a != 0;
                break;
            case 10:
                if (node == null) return;
                try {
                    node.customEUt = Math.max(-1L, Long.parseLong(recipe));
                } catch (NumberFormatException invalid) {
                    return;
                }
                break;
            case 11:
                // Retired global multiplier command: old clients cannot mutate automatic batching.
                return;
            case 9:
                FactoryRouting.connect(draft);
                if (!draft.hasTargetsForAllComponents()) {
                    editorStatus = FactoryText.NO_TARGET;
                    return;
                }
                long now = base.getWorld()
                    .getTotalWorldTime();
                if (lastBalanceTick != Long.MIN_VALUE && now - lastBalanceTick < 20) return;
                lastBalanceTick = now;
                editorStatus = FactoryBalancer.balance(draft) ? FactoryText.BALANCED : FactoryText.BALANCE_FAILED;
                break;
            case 8:
                if (node != null && FactoryRecipeCatalog.get(recipe) != null) {
                    node.recipe = recipe;
                    node.customEUt = -1;
                } else {
                    status = FactoryText.INVALID;
                    return;
                }
                break;
            case 13:
                if (FactoryRecipeCatalog.get(recipe) == null) return;
                draft.add(recipe);
                break;
            case 14:
            case 15:
                if (!FactoryRouting.scale(draft, command == 14)) editorStatus = FactoryText.LIMIT;
                break;
            case 17:
                if (!draft.nodes.isEmpty()) return;
                try {
                    draft.read(
                        FactoryRouting.decode(recipe)
                            .write());
                } catch (IllegalArgumentException invalid) {
                    editorStatus = FactoryText.INVALID;
                    return;
                }
                break;
            default:
                return;
        }
        if (command == 0 || command == 1 || command == 8 || command == 13) FactoryRouting.connect(draft);
        base.markDirty();
    }

    /** Creates a free pattern from the same server-owned graph and parallel values shown in the production preview. */
    private void exportPattern() {
        if (!routingLocked || draining || installed.nodes.isEmpty()) {
            editorStatus = FactoryText.PATTERN_LOCK_FIRST;
            return;
        }
        try {
            FactoryPreview.Snapshot snapshot = FactoryPreview.describe(installed);
            if (snapshot.exportIssue() != null) {
                editorStatus = snapshot.exportIssue();
                return;
            }
            ItemStack encoded = FactoryPatternExport.create(snapshot);
            editorStatus = addOutputAtomic(encoded) ? FactoryText.PATTERN_EXPORTED : FactoryText.PATTERN_OUTPUT_FULL;
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            editorStatus = FactoryText.PATTERN_INVALID;
        }
    }

    /** Pays all active jobs before advancing them. Wired shortages never fall through to the wireless grid. */
    @Override
    protected void runMachine(IGregTechTileEntity base, long tick) {
        mEfficiency = 10000;
        if (!base.isAllowedToWork()) {
            status = FactoryText.PAUSED;
            mMaxProgresstime = 0;
            lEUt = 0;
            return;
        }
        ensureLineProgress();
        long eut;
        try {
            eut = runtime.totalEUt();
        } catch (ArithmeticException e) {
            status = FactoryText.LIMIT;
            return;
        }
        if (eut > 0 && !payEnergy(eut)) {
            status = FactoryText.POWER;
            lEUt = 0;
            mMaxProgresstime = 0;
            return;
        }
        if (lineProgress != null) lineProgress.advance(runtime.states);
        boolean finished = runtime.states.values()
            .stream()
            .anyMatch(job -> job.remaining == 1);
        runtime.advance();
        flushBuffers();
        if (draining && runtime.empty() && reservations.refund(this::addOutputAtomic)) {
            installed = pending;
            lineProgress = null;
            lineTimingUnavailable = false;
            savedLineProgress = new NBTTagCompound();
            cycleGroups = null;
            cyclePlans.clear();
            pending = new FactoryGraph();
            runtime.states.clear();
            nodeStatus.clear();
            draining = false;
        }
        ensureLineProgress();
        // Missing-input retries are bounded to twice per second; active jobs still advance each paid tick.
        if (!draining && (finished || tick % 10 == 0) && !installed.nodes.isEmpty()) {
            startRecipeProcessing();
            try {
                List<ItemStack> liveItems = getStoredInputs();
                // Circuit selector slots are ghost configuration, never physical reservations or ingredients.
                liveItems.removeIf(
                    stack -> stack == getStackInSlot(1) || mInputBusses.stream()
                        .anyMatch(
                            bus -> bus != null && bus.isValid() && stack == bus.getStackInSlot(bus.getCircuitSlot())));
                List<FluidStack> liveFluids = getStoredFluids();
                List<FactoryInputs> craftingInputs = new ArrayList<>();
                List<ItemStack> depositItems = new ArrayList<>(liveItems);
                java.util.Set<ItemStack> depositRefs = java.util.Collections
                    .newSetFromMap(new java.util.IdentityHashMap<>());
                depositRefs.addAll(depositItems);
                for (gregtech.common.tileentities.machines.IDualInputHatch hatch : mDualInputHatches) {
                    List<ItemStack> shared = new ArrayList<>();
                    ItemStack ghost = hatch instanceof gregtech.api.metatileentity.implementations.MTEHatchInputBus bus
                        ? bus.getStackInSlot(bus.getCircuitSlot())
                        : null;
                    ItemStack[] sharedItems = hatch.getSharedItems();
                    if (sharedItems != null) for (ItemStack item : sharedItems) {
                        if (item == null || item.stackSize <= 0 || item == ghost) continue;
                        shared.add(item);
                        if (depositRefs.add(item)) depositItems.add(item);
                    }
                    for (var inventories = hatch.inventories(); inventories.hasNext();) {
                        gregtech.common.tileentities.machines.IDualInputInventory inventory = inventories.next();
                        if (inventory != null && !inventory.isEmpty())
                            craftingInputs.add(new FactoryInputs(shared, inventory));
                    }
                }
                boolean ready = true;
                for (FactoryGraph.Node candidate : installed.nodes) {
                    FactoryRecipeCatalog.Entry recipe = FactoryRecipeCatalog.get(candidate.recipe);
                    FactoryText missing = recipe == null ? FactoryText.INVALID
                        : reservations
                            .collect(candidate.id, recipe, depositItems, stack -> supportsHost(recipe, stack));
                    if (missing != null) {
                        nodeStatus.put(candidate.id, missing);
                        ready = false;
                    } else nodeStatus.put(
                        candidate.id,
                        runtime.state(candidate.id).remaining > 0 ? FactoryText.RUNNING : FactoryText.IDLE);
                }
                for (int i = 0; ready && i < installed.nodes.size(); i++) {
                    FactoryGraph.Node node = installed.nodes.get((i + schedulingCursor) % installed.nodes.size());
                    FactoryText failure = null;
                    for (FactoryInputs input : craftingInputs) {
                        tryStart(node, input.items, input.fluids);
                        if (runtime.state(node.id).remaining > 0) break;
                        FactoryText reason = nodeStatus.get(node.id);
                        if (reason != null && reason != FactoryText.INPUT && reason != FactoryText.IDLE)
                            failure = reason;
                    }
                    if (runtime.state(node.id).remaining <= 0) {
                        tryStart(node, liveItems, liveFluids);
                        if (runtime.state(node.id).remaining <= 0 && failure != null
                            && nodeStatus.get(node.id) == FactoryText.INPUT) nodeStatus.put(node.id, failure);
                    }
                }
                schedulingCursor = (schedulingCursor + 1) % installed.nodes.size();
                updateSlots();
                // Dual-input hatches are not in the regular input-bus list updated by the base class.
                for (gregtech.common.tileentities.machines.IDualInputHatch hatch : mDualInputHatches)
                    if (hatch.getBaseMetaTileEntity() != null) hatch.getBaseMetaTileEntity()
                        .markDirty();
            } finally {
                endRecipeProcessing();
            }
        }
        long active = runtime.totalEUt();
        lEUt = -active;
        boolean running = runtime.states.values()
            .stream()
            .anyMatch(job -> job.remaining > 0);
        // The base uses max progress to signal activity; node jobs remain the authority for processing.
        long totalTicks = lineProgress == null ? runtime.states.values()
            .stream()
            .filter(job -> job.remaining > 0)
            .mapToInt(job -> job.duration)
            .max()
            .orElse(0) : lineProgress.totalTicks();
        mMaxProgresstime = running ? (int) Math.min(Integer.MAX_VALUE, totalTicks) : 0;
        mProgresstime = mMaxProgresstime == 0 || lineProgress == null ? 0
            : (int) (lineProgress.progressTicks() * (double) mMaxProgresstime / totalTicks);
        if (draining) {
            status = runtime.states.values()
                .stream()
                .anyMatch(job -> job.remaining > 0)
                    ? FactoryText.DRAIN_RUNNING
                    : !runtime.empty()
                        ? (fluidOutputBlocked ? FactoryText.FLUID_OUTPUT_BLOCKED : FactoryText.ITEM_OUTPUT_BLOCKED)
                        : FactoryText.REFUND_BLOCKED;
        } else if (installed.nodes.isEmpty()) status = FactoryText.IDLE;
        else {
            status = active > 0 ? FactoryText.RUNNING : FactoryText.INPUT;
            for (FactoryText reason : new FactoryText[] { FactoryText.POWER, FactoryText.LIMIT, FactoryText.HOST,
                FactoryText.CATALYST_MISSING, FactoryText.BLOCKED }) {
                if (nodeStatus.containsValue(reason)) {
                    status = reason;
                    break;
                }
            }
            if (itemOutputBlocked) status = FactoryText.ITEM_OUTPUT_BLOCKED;
            if (fluidOutputBlocked) status = FactoryText.FLUID_OUTPUT_BLOCKED;
        }
        base.markDirty();
    }

    /** Caches display timing without preventing old jobs from draining when a recipe becomes unavailable. */
    private void ensureLineProgress() {
        if (lineProgress == null && !lineTimingUnavailable) {
            try {
                lineProgress = new FactoryProgress(installed, node -> {
                    FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
                    if (entry == null) throw new ArithmeticException("Missing progress recipe");
                    return entry.recipe.mDuration;
                });
                lineProgress.read(savedLineProgress);
            } catch (ArithmeticException invalid) {
                lineTimingUnavailable = true;
            }
        }
    }

    /** Available per-tick headroom after running jobs; wireless balances are narrowed only after clamping. */
    private long availableBatchEUt() {
        long active = runtime.totalEUt();
        if (!isWirelessModeActive()) return Math.max(0, getMaxInputEu() - active);
        if (ownerUUID == null) return 0;
        BigInteger available = WirelessNetworkManager.getUserEU(ownerUUID)
            .subtract(BigInteger.valueOf(active));
        return available.max(BigInteger.ZERO)
            .min(BigInteger.valueOf(Long.MAX_VALUE - active))
            .longValue();
    }

    private boolean payEnergy(long eut) {
        if (!isWirelessModeActive()) return eut <= getMaxInputEu() && drainEnergyInput(eut);
        if (ownerUUID == null) return false;
        costingEU = BigInteger.valueOf(eut);
        costingEUText = costingEU.toString();
        return WirelessNetworkManager.addEUToGlobalEnergyMap(ownerUUID, costingEU.negate());
    }

    private void tryStart(FactoryGraph.Node node, List<ItemStack> liveItems, List<FluidStack> liveFluids) {
        if (cycleGroups == null) cycleGroups = FactoryCycles.groups(installed);
        List<FactoryGraph.Node> cycle = cycleGroups.get(node.id);
        if (cycle != null) {
            if (cycle.get(0).id == node.id) tryStartCycle(cycle, liveItems, liveFluids);
            return;
        }
        FactoryRuntime.State old = runtime.state(node.id);
        if (old.remaining > 0) return;
        old.compact();
        FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
        if (entry == null) {
            nodeStatus.put(node.id, FactoryText.INVALID);
            return;
        }
        if (bufferFull(node, entry, old)) {
            nodeStatus.put(node.id, FactoryText.BLOCKED);
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (int source : node.sources) {
            FactoryRuntime.State buffer = runtime.states.get(source);
            if (buffer == null) continue;
            buffer.compact();
            items.addAll(buffer.items);
            fluids.addAll(buffer.fluids);
        }
        for (ItemStack item : liveItems) if (item != null && item.stackSize > 0) items.add(item);
        fluids.addAll(liveFluids);
        ItemStack[] itemRefs = items.toArray(new ItemStack[0]);
        FluidStack[] fluidRefs = fluids.toArray(new FluidStack[0]);
        int parallel = (int) entry.consumableRecipe
            .maxParallelCalculatedByInputs(Integer.MAX_VALUE, fluidRefs, itemRefs);
        if (parallel <= 0) {
            nodeStatus.put(node.id, FactoryText.INPUT);
            return;
        }
        try {
            long unitEUt = FactoryGraph.timing(
                node.customEUt < 0 ? entry.recipe.mEUt : node.customEUt,
                entry.recipe.mDuration,
                1,
                node.overclocks)[0];
            parallel = FactoryBatching.powerLimit(parallel, unitEUt, availableBatchEUt());
            if (parallel <= 0) {
                nodeStatus.put(node.id, FactoryText.POWER);
                return;
            }
            parallel = FactoryBatching.recipeLimit(entry.recipe, old, parallel);
            parallel = FactoryBatching.align(parallel, node.parallel);
            if (parallel <= 0) {
                nodeStatus.put(node.id, FactoryText.BLOCKED);
                return;
            }
            long[] timing = FactoryGraph.timing(
                node.customEUt < 0 ? entry.recipe.mEUt : node.customEUt,
                entry.recipe.mDuration,
                parallel,
                node.overclocks);
            long aggregate = Math.addExact(runtime.totalEUt(), timing[0]);
            if (!isWirelessModeActive()
                && (timing[0] / parallel > getMaxInputVoltage() || aggregate > getMaxInputEu())) {
                nodeStatus.put(node.id, FactoryText.POWER);
                return;
            }
            if (isWirelessModeActive() && (ownerUUID == null || WirelessNetworkManager.getUserEU(ownerUUID)
                .compareTo(BigInteger.valueOf(aggregate)) < 0)) {
                nodeStatus.put(node.id, FactoryText.POWER);
                return;
            }
            FactoryRuntime.State job = FactoryRuntime.prepare(
                entry.recipe,
                parallel,
                node.overclocks,
                random,
                node.customEUt < 0 ? entry.recipe.mEUt : node.customEUt);
            runtime.checkCapacity(node.id, job);
            entry.consumableRecipe.consumeInput(parallel, fluidRefs, itemRefs);
            runtime.start(node.id, job);
            nodeStatus.put(node.id, FactoryText.RUNNING);
        } catch (ArithmeticException e) {
            nodeStatus.put(node.id, FactoryText.LIMIT);
        }
    }

    /** Commits an entire closed component atomically after its net external inputs, output space and power pass. */
    private void tryStartCycle(List<FactoryGraph.Node> cycle, List<ItemStack> liveItems, List<FluidStack> liveFluids) {
        for (FactoryGraph.Node node : cycle) {
            if (runtime.state(node.id).remaining > 0) return;
            FactoryRecipeCatalog.Entry recipe = FactoryRecipeCatalog.get(node.recipe);
            if (recipe == null || bufferFull(node, recipe, runtime.state(node.id))) {
                nodeStatus.put(node.id, FactoryText.BLOCKED);
                return;
            }
        }
        int leader = cycle.get(0).id;
        try {
            FactoryCycles.Plan plan = cyclePlans.computeIfAbsent(leader, id -> FactoryCycles.prepare(cycle, 1, random));
            List<ItemStack> items = new ArrayList<>(liveItems);
            List<FluidStack> fluids = new ArrayList<>(liveFluids);
            java.util.Set<Integer> sources = new java.util.HashSet<>();
            for (FactoryGraph.Node node : cycle) sources.addAll(node.sources);
            for (FactoryGraph.Node node : cycle) sources.remove(node.id);
            for (int source : sources) {
                FactoryRuntime.State buffer = runtime.states.get(source);
                if (buffer != null) {
                    items.addAll(buffer.items);
                    fluids.addAll(buffer.fluids);
                }
            }
            ItemStack[] itemRefs = items.toArray(new ItemStack[0]);
            FluidStack[] fluidRefs = fluids.toArray(new FluidStack[0]);
            int batches = (int) plan.inputs.maxParallelCalculatedByInputs(Integer.MAX_VALUE, fluidRefs, itemRefs);
            if (batches <= 0) {
                for (FactoryGraph.Node node : cycle) nodeStatus.put(node.id, FactoryText.INPUT);
                return;
            }
            batches = FactoryBatching.powerLimit(batches, plan.eut, availableBatchEUt());
            if (batches <= 0) {
                for (FactoryGraph.Node node : cycle) nodeStatus.put(node.id, FactoryText.POWER);
                return;
            }
            batches = FactoryBatching.inputLimit(plan.inputs, batches);
            for (FactoryGraph.Node member : cycle) {
                GTRecipe recipe = FactoryRecipeCatalog.get(member.recipe).recipe;
                int duration = (int) FactoryGraph.timing(0, recipe.mDuration, 1, member.overclocks)[1];
                long repetitions = (long) member.parallel * (plan.jobs.get(member.id).duration / duration);
                int capacity = FactoryBatching.recipeLimit(recipe, runtime.state(member.id), Integer.MAX_VALUE);
                batches = (int) Math.min(batches, capacity / repetitions);
            }
            if (batches <= 0) {
                for (FactoryGraph.Node node : cycle) nodeStatus.put(node.id, FactoryText.BLOCKED);
                return;
            }
            if (batches > 1) plan = FactoryCycles.prepare(cycle, batches, random);
            if (plan.inputs.maxParallelCalculatedByInputs(1, fluidRefs, itemRefs) < 1) {
                for (FactoryGraph.Node node : cycle) nodeStatus.put(node.id, FactoryText.INPUT);
                return;
            }
            long aggregate = Math.addExact(runtime.totalEUt(), plan.eut);
            if ((!isWirelessModeActive() && (plan.voltage > getMaxInputVoltage() || aggregate > getMaxInputEu()))
                || (isWirelessModeActive() && (ownerUUID == null || WirelessNetworkManager.getUserEU(ownerUUID)
                    .compareTo(BigInteger.valueOf(aggregate)) < 0))) {
                for (FactoryGraph.Node node : cycle) nodeStatus.put(node.id, FactoryText.POWER);
                return;
            }
            for (FactoryGraph.Node node : cycle) runtime.checkCapacity(node.id, plan.jobs.get(node.id));
            plan.inputs.consumeInput(1, fluidRefs, itemRefs);
            for (FactoryGraph.Node node : cycle) {
                runtime.start(node.id, plan.jobs.get(node.id));
                nodeStatus.put(node.id, FactoryText.RUNNING);
            }
            cyclePlans.remove(leader);
        } catch (ArithmeticException invalid) {
            for (FactoryGraph.Node node : cycle) nodeStatus.put(node.id, FactoryText.LIMIT);
        }
    }

    /**
     * Circulating quantities are already cancelled within a net batch; only edges leaving the component retain output.
     */
    private boolean sameCycle(int first, int second) {
        if (cycleGroups == null) cycleGroups = FactoryCycles.groups(installed);
        return cycleGroups.containsKey(first) && cycleGroups.get(first) == cycleGroups.get(second);
    }

    /** Matches multiblock controllers by their advertised recipe maps, as in SuperFactory. */
    /** Shared controller eligibility for deposits and the route-list machine icon. */
    public static boolean supportsHost(FactoryRecipeCatalog.Entry entry, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemMachines)) return false;
        IMetaTileEntity meta = ItemMachines.getMetaTileEntity(stack);
        if (!(meta instanceof MTEMultiBlockBase) || !(meta instanceof RecipeMapWorkable workable)) return false;
        if (workable.getAvailableRecipeMaps() != null) {
            for (gregtech.api.recipe.RecipeMap<?> map : workable.getAvailableRecipeMaps()) {
                if (map != null && map.unlocalizedName.equals(entry.map.unlocalizedName)) return true;
            }
        }
        return workable.getRecipeMap() != null
            && workable.getRecipeMap().unlocalizedName.equals(entry.map.unlocalizedName);
    }

    /** Saturates only buffer estimates; actual recipe consumption and output amounts still use checked arithmetic. */
    private static long saturatedAmountSum(long a, long b) {
        return a > Long.MAX_VALUE - 2 - b ? Long.MAX_VALUE - 2 : a + b;
    }

    /**
     * Bounded watermarks allow small upstream batches to accumulate into larger downstream recipes.
     * Space for a complete worst-case output batch is reserved before any inputs are consumed.
     */
    private boolean bufferFull(FactoryGraph.Node node, FactoryRecipeCatalog.Entry entry, FactoryRuntime.State state) {
        long batches = 64;
        for (FactoryGraph.Node consumer : installed.nodes) {
            if (!consumer.sources.contains(node.id)) continue;
            FactoryRecipeCatalog.Entry target = FactoryRecipeCatalog.get(consumer.recipe);
            if (target == null) continue;
            for (ItemStack output : entry.recipe.mOutputs) {
                if (output == null || output.stackSize <= 0) continue;
                long need = 0;
                for (GTRecipe.RecipeItemInput input : target.recipe.getCachedCombinedItemInputs()) {
                    if (input.matchesType(output))
                        need = saturatedAmountSum(need, Math.max(1L, input.inputAmount) * effectiveParallel(consumer));
                }
                batches = Math.max(batches, need / output.stackSize + (need % output.stackSize == 0 ? 0 : 1) + 1);
            }
            for (FluidStack output : entry.recipe.mFluidOutputs) {
                if (output == null || output.amount <= 0) continue;
                long need = 0;
                for (FluidStack input : target.recipe.mFluidInputs) {
                    if (input != null && input.isFluidEqual(output))
                        need = saturatedAmountSum(need, (long) input.amount * effectiveParallel(consumer));
                }
                batches = Math.max(batches, need / output.amount + (need % output.amount == 0 ? 0 : 1) + 1);
            }
        }
        for (ItemStack stored : state.items) {
            long batch = 0;
            for (ItemStack output : entry.recipe.mOutputs) {
                if (output != null && stored.isItemEqual(output) && ItemStack.areItemStackTagsEqual(stored, output))
                    batch = saturatedAmountSum(batch, (long) output.stackSize * effectiveParallel(node));
            }
            long watermark = batch == 0 ? 0
                : Math.min(
                    Integer.MAX_VALUE - batch,
                    batches > Integer.MAX_VALUE / batch ? Integer.MAX_VALUE : batches * batch);
            if (stored.stackSize >= watermark || !internalItem(node.id, stored)) return true;
        }
        for (FluidStack stored : state.fluids) {
            long batch = 0;
            for (FluidStack output : entry.recipe.mFluidOutputs) if (output != null && stored.isFluidEqual(output))
                batch = saturatedAmountSum(batch, (long) output.amount * effectiveParallel(node));
            long watermark = batch == 0 ? 0
                : Math.min(
                    Integer.MAX_VALUE - batch,
                    batches > Integer.MAX_VALUE / batch ? Integer.MAX_VALUE : batches * batch);
            if (stored.amount >= watermark || !internalFluid(node.id, stored)) return true;
        }
        return false;
    }

    private boolean internalItem(int source, ItemStack stack) {
        if (draining) return false;
        for (FactoryGraph.Node target : installed.nodes) {
            if (!target.sources.contains(source) || sameCycle(source, target.id)) continue;
            FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(target.recipe);
            if (entry != null) for (GTRecipe.RecipeItemInput input : entry.recipe.getCachedCombinedItemInputs()) {
                if (input.matchesType(stack)) return true;
            }
        }
        return false;
    }

    private boolean internalFluid(int source, FluidStack stack) {
        if (draining) return false;
        for (FactoryGraph.Node target : installed.nodes) {
            if (!target.sources.contains(source) || sameCycle(source, target.id)) continue;
            FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(target.recipe);
            if (entry != null) for (FluidStack input : entry.recipe.mFluidInputs) {
                if (input != null && input.isFluidEqual(stack)) return true;
            }
        }
        return false;
    }

    /** Atomic bounded chunks preserve unsent amounts, including when ME or ordinary output storage is full. */
    private void flushBuffers() {
        itemOutputBlocked = fluidOutputBlocked = false;
        int budget = 32;
        for (Map.Entry<Integer, FactoryRuntime.State> entry : runtime.states.entrySet()) {
            FactoryRuntime.State buffer = entry.getValue();
            for (ItemStack stack : buffer.items) {
                if (stack.stackSize <= 0 || internalItem(entry.getKey(), stack)) continue;
                if (--budget < 0) return;
                ItemStack part = stack.copy();
                int amount = part.stackSize = Math.min(stack.stackSize, stack.getMaxStackSize());
                if (addOutputAtomic(part)) stack.stackSize -= amount;
                else itemOutputBlocked = true;
            }
            for (FluidStack stack : buffer.fluids) {
                if (stack.amount <= 0 || internalFluid(entry.getKey(), stack)) continue;
                if (--budget < 0) return;
                FluidStack part = stack.copy();
                int amount = part.amount = Math.min(stack.amount, 64000);
                if (addOutputAtomic(part)) stack.amount -= amount;
                else fluidOutputBlocked = true;
            }
            buffer.compact();
        }
    }

    /** Drops real reserved items on controller removal; chunk unload only persists them. */
    @Override
    public void onRemoval() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.isServerSide()) {
            reservations.refund(
                stack -> base.getWorld()
                    .spawnEntityInWorld(
                        new net.minecraft.entity.item.EntityItem(
                            base.getWorld(),
                            base.getXCoord() + 0.5,
                            base.getYCoord() + 0.5,
                            base.getZCoord() + 0.5,
                            stack)));
        }
        super.onRemoval();
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setTag("factoryDraft", draft.write());
        tag.setTag("factoryInstalled", installed.write());
        tag.setTag("factoryPending", pending.write());
        tag.setTag("factoryRuntime", runtime.write());
        tag.setTag("factoryProgress", lineProgress == null ? savedLineProgress : lineProgress.write());
        tag.setTag("factoryReservations", reservations.write());
        tag.setBoolean("factoryDraining", draining);
        tag.setBoolean("factoryRoutingLocked", routingLocked);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        draft.read(tag.getCompoundTag("factoryDraft"));
        installed.read(tag.getCompoundTag("factoryInstalled"));
        cycleGroups = null;
        cyclePlans.clear();
        pending.read(tag.getCompoundTag("factoryPending"));
        runtime.read(tag.getCompoundTag("factoryRuntime"));
        lineProgress = null;
        lineTimingUnavailable = false;
        savedLineProgress = tag.getCompoundTag("factoryProgress");
        reservations.read(tag.getCompoundTag("factoryReservations"));
        draining = tag.getBoolean("factoryDraining");
        routingLocked = tag.getBoolean("factoryRoutingLocked");
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    @Override
    public boolean protectsExcessItem() {
        return true;
    }

    @Override
    public boolean protectsExcessFluid() {
        return true;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType(FactoryText.NAME.text())
            .addInfo(FactoryText.POWER_HELP.text())
            .addInfo(FactoryText.GRAPH_HELP.text())
            .addInfo(FactoryText.HOST.text())
            .addInfo(FactoryText.RESERVED.text())
            .addInfo(FactoryText.SAFETY_HELP.text())
            .addInfo(FactoryText.RECIPE_HELP.text())
            .beginStructureBlock(3, 3, 3, true)
            .addController("Front center")
            .addCasingInfoMin("Robust Tungstensteel Machine Casing", 0, false)
            .addInputBus("Any casing", 1)
            .addInputHatch("Any casing", 1)
            .addOutputBus("Any casing", 1)
            .addOutputHatch("Any casing", 1)
            .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity base, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = TextureFactory.of(
            Casings.RobustTungstenSteelMachineCasing.getBlock(),
            Casings.RobustTungstenSteelMachineCasing.getBlockMeta());
        return side == facing
            ? new ITexture[] { casing,
                TextureFactory.of(active ? TexturesGtBlock.oMCAQFTActive : TexturesGtBlock.oMCAQFT) }
            : new ITexture[] { casing };
    }
}
