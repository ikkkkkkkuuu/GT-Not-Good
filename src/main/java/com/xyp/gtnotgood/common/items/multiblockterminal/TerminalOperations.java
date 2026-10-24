package com.xyp.gtnotgood.common.items.multiblockterminal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.ChannelDescription;
import com.gtnewhorizon.structurelib.ConfigurationHandler;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.IAlignmentProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISpecialItemBlock;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElement.BlocksToPlace;
import com.gtnewhorizon.structurelib.structure.IStructureElementNoPlacement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalStructureCapture.Position;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

public final class TerminalOperations {

    private static final ThreadLocal<Plan> activePlan = new ThreadLocal<>();
    private static final Map<EntityPlayerMP, Long> lastUse = new WeakHashMap<>();
    private static final ThreadLocal<Map<PieceKey, List<Position>>> layoutCache = new ThreadLocal<>();

    private TerminalOperations() {}

    public static boolean project(ItemStack terminal, EntityPlayer player, World world, int x, int y, int z, int side) {
        if (
            !TerminalSettings.fullBuild(terminal) && !TerminalSettings.dismantle(terminal)
        ) return ConstructableUtility.handle(TerminalSettings.projectorTrigger(terminal), player, world, x, y, z, side);
        TileEntity tile = world.getTileEntity(x, y, z);
        IAlignment alignment = alignment(tile, constructable(tile, side));
        boolean mirror = TerminalSettings.mirror(terminal)
            ^ (alignment != null && alignment.getFlip().isHorizontallyFlipped());
        Plan plan = new Plan(mirror, world.isRemote, player);
        if (mirror) activePlan.set(plan);
        try {
            return ConstructableUtility.handle(TerminalSettings.trigger(terminal), player, world, x, y, z, side);
        } finally {
            if (mirror) activePlan.remove();
        }
    }

    /** Intercepts only this terminal's synchronous structure request; normal StructureLib calls are unchanged. */
    public static boolean capture(Object context, ItemStack trigger, IStructureElement<?>[] elements, World world,
        ExtendedFacing facing, int x, int y, int z, int a, int b, int c, boolean hintsOnly, Boolean checkBlocks) {
        Plan plan = activePlan.get();
        if (plan != null && plan.checks && checkBlocks != null) {
            prepareChecked(context, elements, world, facing, x, y, z, a, b, c);
            return false;
        }
        if (plan == null || checkBlocks != null || plan.checks || plan.executingPiece) return false;
        if (plan.liveBuild) return captureSurvival(context, trigger, elements, world, facing, x, y, z, a, b, c);
        List<Position> positions = positions(plan, context, elements, world, facing, x, y, z, a, b, c);
        if (plan.project) {
            for (Position position : positions) {
                IStructureElement<Object> element = element(position);
                element.spawnHint(context, world, position.x(), position.y(), position.z(), trigger);
                if (
                    ChannelDataAccessor.hasSubChannel(trigger, StructureLibAPI.CHANNEL_SHOW_ERROR)
                        && !element.couldBeValid(context, world, position.x(), position.y(), position.z(), trigger)
                ) StructureLibAPI.markHintParticleError(plan.player, world, position.x(), position.y(), position.z());
            }
        } else {
            plan.positions.addAll(positions);
            if (
                !plan.recordSurvival
            ) plan.pieces.add(new TerminalBuildPiece(context, elements, world, facing, x, y, z, a, b, c, plan.mirror));
        }
        return true;
    }

    public static boolean captureSurvival(Object context, ItemStack trigger, IStructureElement<?>[] elements,
        World world, ExtendedFacing facing, int x, int y, int z, int a, int b, int c) {
        Plan plan = activePlan.get();
        if (plan == null || plan.executingPiece || !plan.liveBuild && !plan.recordSurvival) return false;
        plan.positions.addAll(positions(plan, context, elements, world, facing, x, y, z, a, b, c));
        if (plan.recordSurvival) return true;
        TerminalBuildPiece piece = new TerminalBuildPiece(context, elements, world, facing, x, y, z, a, b, c,
            plan.mirror);
        plan.pieces.add(piece);
        if (plan.liveBuild) {
            plan.executingPiece = true;
            plan.enforceOverlay = plan.plannedPieces
                .contains(new PieceKey(context, elements, world, facing, x, y, z, a, b, c, plan.mirror));
            try {
                plan.changed += Math.max(0,
                    piece.build(plan.terminal, trigger, (EntityPlayerMP) plan.player, plan.materials, plan.refunds));
            } finally {
                plan.executingPiece = false;
                plan.enforceOverlay = false;
            }
        }
        return true;
    }

    public static void captureChecked(Object context, ItemStack trigger, IStructureElement<?>[] elements, World world,
        ExtendedFacing facing, int x, int y, int z, int a, int b, int c) {
        Plan plan = activePlan.get();
        if (plan == null || !plan.checks || context != plan.owner) return;
        PieceKey key = new PieceKey(context, elements, world, facing, x, y, z, a, b, c, plan.mirror);
        if (!plan.checkedPieces.add(key)) return;
        PreparedPiece prepared = plan.prepared.remove(key);
        if (prepared == null)
            plan.positions.addAll(positions(plan, context, elements, world, facing, x, y, z, a, b, c));
        else {
            plan.positions.addAll(prepared.positions());
            prepared.candidates()
                .forEach((coordinate, filter) -> plan.checkedCandidates.merge(coordinate, filter, Predicate::or));
        }
    }

    /** Reads type filters before the native check fills context-sensitive hatch counters. */
    public static void prepareChecked(Object context, IStructureElement<?>[] elements, World world,
        ExtendedFacing facing, int x, int y, int z, int a, int b, int c) {
        Plan plan = activePlan.get();
        if (plan == null || !plan.checks || context != plan.owner) return;
        PieceKey key = new PieceKey(context, elements, world, facing, x, y, z, a, b, c, plan.mirror);
        if (plan.checkedPieces.contains(key) || plan.prepared.containsKey(key)) return;
        List<Position> points = positions(plan, context, elements, world, facing, x, y, z, a, b, c);
        Map<Coordinate, Predicate<ItemStack>> filters = new LinkedHashMap<>();
        ItemStack trigger = TerminalSettings.trigger(plan.terminal);
        AutoPlaceEnvironment environment = AutoPlaceEnvironment.fromLegacy((predicate, simulate, count) -> Map.of(),
            plan.player, ignored -> {});
        for (Position point : points) {
            if (!isHatch(world.getTileEntity(point.x(), point.y(), point.z()))) continue;
            BlocksToPlace candidates = candidates(point, trigger, environment);
            if (candidates != null)
                filters.put(new Coordinate(point.x(), point.y(), point.z()), candidates.getPredicate());
        }
        plan.prepared.put(key, new PreparedPiece(points, filters));
    }

    public static void beginCheckedPiece(Object context, IStructureElement<?>[] elements, World world,
        ExtendedFacing facing, int x, int y, int z, int a, int b, int c) {
        Plan plan = activePlan.get();
        if (plan == null || !plan.checks || context != plan.owner) return;
        prepareChecked(context, elements, world, facing, x, y, z, a, b, c);
        plan.checkingPieces.push(new PieceKey(context, elements, world, facing, x, y, z, a, b, c, plan.mirror));
    }

    public static void finishCheckedPiece(Object context, IStructureElement<?>[] elements, World world,
        ExtendedFacing facing, int x, int y, int z, int a, int b, int c, boolean successful) {
        Plan plan = activePlan.get();
        if (plan == null || !plan.checks || context != plan.owner) return;
        PieceKey key = new PieceKey(context, elements, world, facing, x, y, z, a, b, c, plan.mirror);
        try {
            if (successful) captureChecked(context, null, elements, world, facing, x, y, z, a, b, c);
            else plan.prepared.remove(key);
        } finally {
            plan.checkingPieces.removeFirstOccurrence(key);
        }
    }

    /** Records the exact hatch type accepted by the native legacy adder, without running the check again. */
    public static void captureAcceptedHatch(Object context, World world, int x, int y, int z) {
        Plan plan = activePlan.get();
        if (plan == null || !plan.checks || context != plan.owner || plan.checkingPieces.isEmpty()) return;
        PieceKey key = plan.checkingPieces.peek();
        if (key.context() != context || key.world() != world) return;
        PreparedPiece prepared = plan.prepared.get(key);
        if (prepared == null || !isHatch(world.getTileEntity(x, y, z))) return;
        boolean contained = prepared.positions().stream()
            .anyMatch(point -> point.world() == world && point.x() == x && point.y() == y && point.z() == z);
        if (!contained) return;
        IGregTechTileEntity tile = (IGregTechTileEntity) world.getTileEntity(x, y, z);
        ItemStack accepted = tile.getMetaTileEntity().getStackForm(1);
        if (accepted == null || accepted.getItem() == null) return;
        Item item = accepted.getItem();
        int metadata = accepted.getItemDamage();
        Predicate<ItemStack> filter = candidate -> candidate != null && candidate.getItem() == item
            && candidate.getItemDamage() == metadata;
        prepared.candidates().merge(new Coordinate(x, y, z), filter, Predicate::or);
    }

    public static boolean isCapturingChecks() {
        Plan plan = activePlan.get();
        return plan != null && plan.checks;
    }

    static boolean shouldBuild(Object context, IStructureElement<?> element, World world, int x, int y, int z) {
        Plan plan = activePlan.get();
        if (plan == null || !plan.enforceOverlay) return true;
        Position expected = plan.finalPositions.get(new PlannedPosition(world, x, y, z));
        return expected == null || expected.element() == element && expected.context() == context;
    }

    private static List<Position> positions(Plan plan, Object context, IStructureElement<?>[] elements, World world,
        ExtendedFacing facing, int x, int y, int z, int a, int b, int c) {
        PieceKey key = new PieceKey(context, elements, world, facing, x, y, z, a, b, c, plan.mirror);
        plan.signature.add(key);
        Map<PieceKey, List<Position>> cache = layoutCache.get();
        if (cache != null) return cache.computeIfAbsent(key, ignored -> TerminalStructureCapture.collect(context,
            elements, world, facing, x, y, z, a, b, c, plan.mirror));
        return TerminalStructureCapture.collect(context, elements, world, facing, x, y, z, a, b, c, plan.mirror);
    }

    public static boolean execute(ItemStack terminal, EntityPlayerMP player, World world, int x, int y, int z,
        int side) {
        if (
            world.isRemote || player.worldObj != world
                || player instanceof FakePlayer
                || !world.blockExists(x, y, z)
                || !world.canMineBlock(player, x, y, z)
                || !player.canPlayerEdit(x, y, z, side, terminal)
        ) return false;
        IConstructable constructable = constructable(world.getTileEntity(x, y, z), side);
        if (constructable == null) return false;
        if (
            !TerminalSettings.dismantle(terminal) && !TerminalSettings.fullBuild(terminal)
        ) return ConstructableUtility.handle(TerminalSettings.projectorTrigger(terminal), player, world, x, y, z, side);
        if (
            !TerminalSettings.dismantle(terminal) && !TerminalSettings.replace(terminal)
                && constructable instanceof MTEMultiBlockBase machine
                && machine.mMachine
        ) return true;
        long now = System.currentTimeMillis();
        long delay = ConfigurationHandler.INSTANCE.getAutoPlaceInterval();
        if (now - lastUse.getOrDefault(player, 0L) < delay) return true;
        lastUse.put(player, now);
        ItemStack trigger = TerminalSettings.trigger(terminal);
        IAlignment alignment = alignment(world.getTileEntity(x, y, z), constructable);
        if (alignment != null && !TerminalSettings.dismantle(terminal)) {
            boolean vertical = alignment.getFlip().isVerticallyFliped();
            Flip flip = TerminalSettings.mirror(terminal) ? vertical ? Flip.BOTH : Flip.HORIZONTAL
                : vertical ? Flip.VERTICAL : Flip.NONE;
            if (!alignment.toolSetFlip(flip)) {
                // #tr terminal.multiblock.mirror_unsupported
                // # This controller does not support the selected mirror orientation.
                // # zh_CN 此控制器不支持所选的镜像方向。
                player.addChatMessage(new ChatComponentTranslation("terminal.multiblock.mirror_unsupported"));
                return true;
            }
        }
        TerminalItemSource source = new TerminalItemSource(terminal, player);
        IItemSource filtered = new HatchFilteredSource(source, TerminalSettings.noHatches(terminal));
        boolean mirror = alignment == null && TerminalSettings.mirror(terminal);
        Plan plan = new Plan(mirror, false, player);
        boolean builtLive = false;
        if (TerminalSettings.dismantle(terminal) && constructable instanceof MTEMultiBlockBase machine) {
            Plan actual = new Plan(false, false, player);
            actual.checks = true;
            actual.owner = constructable;
            actual.terminal = terminal;
            activePlan.set(actual);
            boolean formed;
            try {
                formed = machine.checkStructure(true, machine.getBaseMetaTileEntity());
            } finally {
                activePlan.remove();
            }
            if (formed && !actual.positions.isEmpty()) plan = actual;
            else plan.positions.addAll(TerminalLayoutSearch.find(constructable, trigger, player, mirror, source));
        } else if (!TerminalSettings.dismantle(terminal) && constructable instanceof ISurvivalConstructable survival) {
            LayoutSnapshot requested = layout(constructable, trigger, player, mirror);
            for (Position position : requested.positions()) plan.finalPositions
                .put(new PlannedPosition(position.world(), position.x(), position.y(), position.z()), position);
            if (requested.signature() instanceof List<?>signature)
                for (Object key : signature) if (key instanceof PieceKey pieceKey) plan.plannedPieces.add(pieceKey);
            if (constructable instanceof MTEMultiBlockBase machine) {
                machine.clearHatches();
                machine.mMachine = false;
            }
            plan.liveBuild = true;
            plan.terminal = terminal;
            plan.materials = filtered;
            plan.refunds = source;
            activePlan.set(plan);
            try {
                survival.survivalConstruct(trigger, ConfigurationHandler.INSTANCE.getAutoPlaceBudget(),
                    ISurvivalBuildEnvironment.create(filtered, player));
            } finally {
                activePlan.remove();
                plan.liveBuild = false;
            }
            builtLive = !plan.positions.isEmpty();
        }
        if (plan.positions.isEmpty()) plan = hintLayout(constructable, trigger, player, mirror);
        if (plan.positions.isEmpty()) {
            if (!TerminalSettings.dismantle(terminal))
                return ConstructableUtility.handle(trigger, player, world, x, y, z, side);
            // #tr terminal.multiblock.unsupported
            // # This controller does not expose a dismantlable StructureLib layout.
            // # zh_CN 此控制器未提供可拆除的 StructureLib 结构。
            player.addChatMessage(new ChatComponentTranslation("terminal.multiblock.unsupported"));
            return true;
        }
        AutoPlaceEnvironment environment = AutoPlaceEnvironment.fromLegacy(filtered, player, ignored -> {});
        int changed = plan.changed;
        if (TerminalSettings.dismantle(terminal) && constructable instanceof MTEMultiBlockBase machine)
            machine.clearHatches();
        if (!TerminalSettings.dismantle(terminal) && !builtLive) {
            if (constructable instanceof MTEMultiBlockBase machine) machine.clearHatches();
            for (TerminalBuildPiece piece : plan.pieces)
                changed += Math.max(0, piece.build(terminal, trigger, player, filtered, source));
        }
        Map<Coordinate, Position> unique = new LinkedHashMap<>();
        for (Position position : plan.positions)
            unique.put(new Coordinate(position.x(), position.y(), position.z()), position);
        for (Position position : unique.values()) {
            if (!TerminalSettings.dismantle(terminal) && !TerminalSettings.replace(terminal)) continue;
            if (position.world() != world || position.x() == x && position.y() == y && position.z() == z) continue;
            IStructureElement<Object> element = element(position);
            if (
                element == StructureUtility.isAir() || element == StructureUtility.notAir()
                    || element == StructureUtility.error()
            ) continue;
            if (!TerminalSettings.dismantle(terminal) && element instanceof IStructureElementNoPlacement<?>) continue;
            if (!world.blockExists(position.x(), position.y(), position.z())) continue;
            TileEntity tile = world.getTileEntity(position.x(), position.y(), position.z());
            if (TerminalSettings.noHatches(terminal) && isHatch(tile)) continue;
            BlocksToPlace candidates = candidates(position, trigger, environment);
            if (
                needsProbe(candidates)
                    || world.getBlock(position.x(), position.y(), position.z()).getMaterial().isLiquid()
            ) {
                var hint = TerminalHintProbe.probe(position, trigger);
                if (TerminalFluidOperations.handles(hint)) {
                    boolean altered = TerminalSettings.dismantle(terminal)
                        ? TerminalFluidOperations.dismantle(position, hint, player, source)
                        : TerminalFluidOperations.place(position, hint, trigger, player, source);
                    if (altered) changed++;
                    continue;
                }
            }
            if (TerminalSettings.dismantle(terminal)) {
                ItemStack existing = pick(position, player);
                Predicate<ItemStack> checked = plan.checkedCandidates
                    .get(new Coordinate(position.x(), position.y(), position.z()));
                if (
                    existing != null && (checked != null && checked.test(existing)
                        || matches(position, existing, trigger, environment))
                ) {
                    if (TerminalBlockRecovery.harvest(player, world, position.x(), position.y(), position.z(), source))
                        changed++;
                }
            } else if (TerminalSettings.replace(terminal) && tile == null && candidates != null) {
                ItemStack existing = pick(position, player);
                if (
                    existing != null && !candidates.getPredicate().test(existing)
                        && !ChannelDescription.iterate(existing).isEmpty()
                        && matches(position, existing, trigger, environment)
                        && TerminalPlacement.replace(position, trigger, player, filtered, source, candidates)
                ) changed++;
            }
        }
        if (constructable instanceof MTEMultiBlockBase machine)
            machine.checkStructure(true, machine.getBaseMetaTileEntity());
        player.inventory.markDirty();
        player.openContainer.detectAndSendChanges();
        // #tr terminal.multiblock.result
        // # Structure operation completed: %s blocks changed.
        // # zh_CN 结构操作完成：已处理 %s 个方块。
        player.addChatMessage(new ChatComponentTranslation("terminal.multiblock.result", changed));
        return true;
    }

    static boolean matches(Position position, ItemStack existing, ItemStack trigger, AutoPlaceEnvironment environment) {
        if (accepts(position, existing, trigger, environment)) return true;
        var mappings = ChannelDescription.iterate(existing);
        if (mappings.isEmpty()) return false;
        ItemStack oldTrigger = trigger.copy();
        for (var mapping : mappings)
            ChannelDataAccessor.setChannelData(oldTrigger, mapping.getKey(), mapping.getValue());
        if (accepts(position, existing, oldTrigger, environment)) return true;
        for (var mapping : mappings) {
            oldTrigger.stackSize = mapping.getValue();
            if (accepts(position, existing, oldTrigger, environment)) return true;
        }
        return false;
    }

    private static boolean accepts(Position position, ItemStack existing, ItemStack trigger,
        AutoPlaceEnvironment environment) {
        BlocksToPlace candidates = candidates(position, trigger, environment);
        if (candidates != null && !needsProbe(candidates)) return candidates.getPredicate().test(existing);
        var hint = TerminalHintProbe.probe(position, trigger);
        return hint != null && position.world().getBlock(position.x(), position.y(), position.z()) == hint.block()
            && position.world().getBlockMetadata(position.x(), position.y(), position.z()) == hint.meta();
    }

    private static BlocksToPlace candidates(Position position, ItemStack trigger, AutoPlaceEnvironment environment) {
        try {
            return element(position).getBlocksToPlace(position.context(), position.world(), position.x(), position.y(),
                position.z(), trigger, environment);
        } catch (RuntimeException | LinkageError unsupportedDescriptor) {
            return null;
        }
    }

    private static boolean needsProbe(BlocksToPlace candidates) {
        if (candidates == null) return true;
        if (candidates.getStacks() != null) {
            for (ItemStack stack : candidates.getStacks()) if (stack == null || stack.getItem() == null) return true;
        }
        return false;
    }

    static ItemStack pick(Position position, EntityPlayerMP player) {
        World world = position.world();
        Block block = world.getBlock(position.x(), position.y(), position.z());
        if (block.isAir(world, position.x(), position.y(), position.z())) return null;
        TileEntity tile = world.getTileEntity(position.x(), position.y(), position.z());
        if (tile instanceof IGregTechTileEntity gt && gt.getMetaTileEntity() != null)
            return gt.getMetaTileEntity().getStackForm(1);
        Item item = Item.getItemFromBlock(block);
        if (item == null) return null;
        int metadata = world.getBlockMetadata(position.x(), position.y(), position.z());
        int itemMetadata = item instanceof ISpecialItemBlock special ? special.getItemMetaFromBlockMeta(block, metadata)
            : block.damageDropped(metadata);
        return new ItemStack(item, 1, itemMetadata);
    }

    private static boolean isHatch(TileEntity tile) {
        return tile instanceof IGregTechTileEntity gt && gt.getMetaTileEntity() instanceof MTEHatch;
    }

    private static boolean isHatch(ItemStack stack) {
        if (stack == null || stack.getItem() != Item.getItemFromBlock(GregTechAPI.sBlockMachines)) return false;
        int id = stack.getItemDamage();
        return id >= 0 && id < GregTechAPI.METATILEENTITIES.length
            && GregTechAPI.METATILEENTITIES[id] instanceof MTEHatch;
    }

    private static IConstructable constructable(TileEntity tile, int side) {
        if (tile instanceof IConstructableProvider provider) return provider.getConstructable();
        if (tile instanceof IConstructable constructable) return constructable;
        if (tile != null && IMultiblockInfoContainer.contains(tile.getClass())) {
            ExtendedFacing facing = tile instanceof IAlignment alignment ? alignment.getExtendedFacing()
                : ExtendedFacing.of(ForgeDirection.getOrientation(side));
            return IMultiblockInfoContainer.<TileEntity>get(tile.getClass()).toConstructable(tile, facing);
        }
        return null;
    }

    private static IAlignment alignment(TileEntity tile, IConstructable constructable) {
        if (constructable instanceof IAlignmentProvider provider) return provider.getAlignment();
        return tile instanceof IAlignmentProvider provider ? provider.getAlignment() : null;
    }

    private static Plan hintLayout(IConstructable constructable, ItemStack trigger, EntityPlayerMP player,
        boolean mirror) {
        return hintLayout(constructable, trigger, player, mirror, false);
    }

    private static Plan hintLayout(IConstructable constructable, ItemStack trigger, EntityPlayerMP player,
        boolean mirror, boolean recordOnly) {
        Plan plan = new Plan(mirror, false, player);
        plan.recordSurvival = recordOnly;
        activePlan.set(plan);
        try {
            constructable.construct(trigger, true);
        } catch (RuntimeException | LinkageError unsupportedLayout) {
            plan.positions.clear();
            plan.pieces.clear();
        } finally {
            activePlan.remove();
        }
        return plan;
    }

    static LayoutSnapshot layout(IConstructable constructable, ItemStack trigger, EntityPlayerMP player,
        boolean mirror) {
        Plan plan = new Plan(mirror, false, player);
        if (constructable instanceof ISurvivalConstructable survival) {
            MTEMultiBlockBase machine = constructable instanceof MTEMultiBlockBase multi ? multi : null;
            boolean formed = machine != null && machine.mMachine;
            if (machine != null) {
                machine.clearHatches();
                machine.mMachine = false;
            }
            plan.recordSurvival = true;
            activePlan.set(plan);
            try {
                survival.survivalConstruct(trigger, ConfigurationHandler.INSTANCE.getAutoPlaceBudget(),
                    ISurvivalBuildEnvironment.create((predicate, simulate, count) -> Map.of(), player));
            } catch (RuntimeException | LinkageError unsupportedLayout) {
                plan.positions.clear();
                plan.signature.clear();
            } finally {
                activePlan.remove();
                if (machine != null) machine.mMachine = formed;
            }
        }
        if (plan.positions.isEmpty()) plan = hintLayout(constructable, trigger, player, mirror, true);
        return new LayoutSnapshot(plan.positions, List.copyOf(plan.signature));
    }

    static LayoutScope beginLayoutSearch() {
        return new LayoutScope();
    }

    static final class LayoutScope implements AutoCloseable {

        private final Map<PieceKey, List<Position>> previous = layoutCache.get();

        private LayoutScope() {
            layoutCache.set(new LinkedHashMap<>());
        }

        @Override
        public void close() {
            if (previous == null) layoutCache.remove();
            else layoutCache.set(previous);
        }
    }

    record LayoutSnapshot(List<Position> positions, Object signature) {}

    @SuppressWarnings("unchecked")
    static IStructureElement<Object> element(Position position) {
        return (IStructureElement<Object>) position.element();
    }

    private static final class Plan {

        private final boolean mirror, project;
        private final EntityPlayer player;
        private final List<Position> positions = new ArrayList<>();
        private final List<TerminalBuildPiece> pieces = new ArrayList<>();
        private final List<PieceKey> signature = new ArrayList<>();
        private boolean checks, liveBuild, recordSurvival, executingPiece, enforceOverlay;
        private ItemStack terminal;
        private IItemSource materials;
        private TerminalItemSource refunds;
        private Object owner;
        private final Map<PieceKey, PreparedPiece> prepared = new LinkedHashMap<>();
        private final Deque<PieceKey> checkingPieces = new ArrayDeque<>();
        private final Set<PieceKey> checkedPieces = new HashSet<>();
        private final Map<Coordinate, Predicate<ItemStack>> checkedCandidates = new LinkedHashMap<>();
        private final Map<PlannedPosition, Position> finalPositions = new LinkedHashMap<>();
        private final Set<PieceKey> plannedPieces = new HashSet<>();
        private int changed;

        private Plan(boolean mirror, boolean project, EntityPlayer player) {
            this.mirror = mirror;
            this.project = project;
            this.player = player;
        }
    }

    private record Coordinate(int x, int y, int z) {}

    private record PlannedPosition(World world, int x, int y, int z) {}

    private record PreparedPiece(List<Position> positions, Map<Coordinate, Predicate<ItemStack>> candidates) {}

    private record PieceKey(Object context, IStructureElement<?>[] elements, World world, ExtendedFacing facing, int x,
        int y, int z, int a, int b, int c, boolean mirror) {}

    private static final class HatchFilteredSource implements IItemSource {

        private final TerminalItemSource source;
        private final boolean noHatches;

        private HatchFilteredSource(TerminalItemSource source, boolean noHatches) {
            this.source = source;
            this.noHatches = noHatches;
        }

        @Override
        @Nonnull
        public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
            return source.take(candidate -> (!noHatches || !isHatch(candidate)) && predicate.test(candidate), simulate,
                count);
        }

        @Override
        public boolean takeOne(ItemStack stack, boolean simulate) {
            return (!noHatches || !isHatch(stack)) && source.takeOne(stack, simulate);
        }
    }
}
