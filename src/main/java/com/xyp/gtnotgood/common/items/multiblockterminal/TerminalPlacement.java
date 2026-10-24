package com.xyp.gtnotgood.common.items.multiblockterminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;

import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureElement.BlocksToPlace;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalStructureCapture.Position;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;

public final class TerminalPlacement {

    private static final ThreadLocal<Attempt> activePlacement = new ThreadLocal<>();

    static {
        MinecraftForge.EVENT_BUS.register(new TerminalPlacement());
    }

    private TerminalPlacement() {}

    static boolean place(Position position, ItemStack trigger, EntityPlayerMP player, IItemSource materials,
        TerminalItemSource refunds) {
        return place(position, trigger, player, materials, refunds,
            AutoPlaceEnvironment.fromLegacy(materials, player, ignored -> {}));
    }

    static boolean place(Position position, ItemStack trigger, EntityPlayerMP player, IItemSource materials,
        TerminalItemSource refunds, AutoPlaceEnvironment environment) {
        World world = position.world();
        if (
            !world.canMineBlock(player, position.x(), position.y(), position.z())
                || !player.canPlayerEdit(position.x(), position.y(), position.z(), 1, trigger)
        ) return false;
        BlockSnapshot before = BlockSnapshot.getBlockSnapshot(world, position.x(), position.y(), position.z());
        boolean creative = player.capabilities.isCreativeMode;
        IItemSource inputs = creative ? creativeSource(position, trigger, environment) : materials;
        RecordingSource source = new RecordingSource(inputs, !creative);
        Attempt attempt = new Attempt(position);
        boolean success = false;
        activePlacement.set(attempt);
        try {
            var element = TerminalOperations.element(position);
            element.survivalPlaceBlock(position.context(), world, position.x(), position.y(), position.z(), trigger,
                environment.withSource(source));
            boolean changed = world.getBlock(position.x(), position.y(), position.z()) != before.replacedBlock
                || world.getBlockMetadata(position.x(), position.y(), position.z()) != before.meta;
            if (creative && !changed && !attempt.eventSeen) {
                element.placeBlock(position.context(), world, position.x(), position.y(), position.z(), trigger);
                changed = world.getBlock(position.x(), position.y(), position.z()) != before.replacedBlock
                    || world.getBlockMetadata(position.x(), position.y(), position.z()) != before.meta;
            }
            if (changed && !creative && source.consumed.isEmpty()) changed = false;
            if (
                changed && creative
                    && TerminalSettings.noHatches(trigger)
                    && world.getTileEntity(position.x(), position.y(), position.z()) instanceof IGregTechTileEntity tile
                    && tile.getMetaTileEntity() instanceof MTEHatch
            ) changed = false;
            if (changed && !attempt.eventSeen) {
                attempt.canceled |= ForgeEventFactory.onPlayerBlockPlace(player, before, ForgeDirection.UP)
                    .isCanceled();
            }
            success = changed && !attempt.canceled;
            return success;
        } finally {
            activePlacement.remove();
            if (!success) {
                restore(before, world);
                source.refund(refunds);
            }
        }
    }

    private static TerminalCreativeSource creativeSource(Position position, ItemStack trigger,
        AutoPlaceEnvironment environment) {
        BlocksToPlace candidates;
        try {
            candidates = TerminalOperations.element(position).getBlocksToPlace(position.context(), position.world(),
                position.x(), position.y(), position.z(), trigger, environment);
        } catch (RuntimeException | LinkageError unsupportedDescriptor) {
            candidates = null;
        }
        return new TerminalCreativeSource(candidates, TerminalSettings.noHatches(trigger));
    }

    static boolean replace(Position position, ItemStack trigger, EntityPlayerMP player, IItemSource materials,
        TerminalItemSource refunds, BlocksToPlace candidates) {
        ItemStack reserved = player.capabilities.isCreativeMode ? null
            : materials.takeOne(candidates.getPredicate(), false);
        if (!player.capabilities.isCreativeMode && reserved == null) return false;
        World world = position.world();
        BlockSnapshot before = BlockSnapshot.getBlockSnapshot(world, position.x(), position.y(), position.z());
        List<ItemStack> oldDrops = new ArrayList<>();
        boolean success = false;
        boolean harvested = false;
        SingleItemSource escrow = new SingleItemSource(reserved);
        try {
            if (!TerminalBlockRecovery.harvest(player, world, position.x(), position.y(), position.z(), oldDrops::add))
                return false;
            harvested = true;
            success = place(position, trigger, player, escrow, refunds);
            if (success) {
                for (ItemStack drop : oldDrops) refunds.give(drop);
            }
            return success;
        } finally {
            if (harvested && !success) restore(before, world);
            if (reserved != null && (!success || !escrow.used)) refunds.give(reserved);
        }
    }

    static void restore(BlockSnapshot snapshot, World world) {
        boolean restoring = world.restoringBlockSnapshots;
        world.restoringBlockSnapshots = true;
        try {
            snapshot.restore(true, true);
        } finally {
            world.restoringBlockSnapshots = restoring;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void observePlacement(BlockEvent.PlaceEvent event) {
        Attempt attempt = activePlacement.get();
        if (attempt == null || event.world != attempt.position.world()) return;
        Position position = attempt.position;
        boolean contains = event.x == position.x() && event.y == position.y() && event.z == position.z();
        if (!contains && event instanceof BlockEvent.MultiPlaceEvent multiple) {
            for (BlockSnapshot snapshot : multiple.getReplacedBlockSnapshots()) {
                if (snapshot.x == position.x() && snapshot.y == position.y() && snapshot.z == position.z()) {
                    contains = true;
                    break;
                }
            }
        }
        if (contains) {
            attempt.eventSeen = true;
            attempt.canceled |= event.isCanceled();
        }
    }

    private static final class Attempt {

        private final Position position;
        private boolean eventSeen, canceled;

        private Attempt(Position position) {
            this.position = position;
        }
    }

    private static final class RecordingSource implements IItemSource {

        private final IItemSource source;
        private final boolean record;
        private final List<ItemStack> consumed = new ArrayList<>();

        private RecordingSource(IItemSource source, boolean record) {
            this.source = source;
            this.record = record;
        }

        @Override
        @Nonnull
        public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
            Map<ItemStack, Integer> result = source.take(predicate, simulate, count);
            if (!simulate && record) {
                for (var entry : result.entrySet()) {
                    int remaining = entry.getValue();
                    while (remaining > 0) {
                        ItemStack stack = entry.getKey().copy();
                        stack.stackSize = Math.min(remaining, stack.getMaxStackSize());
                        consumed.add(stack);
                        remaining -= stack.stackSize;
                    }
                }
            }
            return result;
        }

        private void refund(TerminalItemSource refunds) {
            if (!record) return;
            if (source instanceof SingleItemSource escrow) {
                escrow.used = false;
                return;
            }
            for (ItemStack stack : consumed) refunds.give(stack);
        }

        @Override
        public boolean takeOne(ItemStack stack, boolean simulate) {
            if (!source.takeOne(stack, simulate)) return false;
            if (!simulate && record) consumed.add(stack.copy());
            return true;
        }
    }

    private static final class SingleItemSource implements IItemSource {

        private final ItemStack stack;
        private boolean used;

        private SingleItemSource(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        @Nonnull
        public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
            Map<ItemStack, Integer> result = new ItemStackMap<>(true);
            if (stack != null && !used && count > 0 && predicate.test(stack)) {
                result.put(stack.copy(), 1);
                if (!simulate) used = true;
            }
            return result;
        }
    }
}
