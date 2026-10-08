package com.xyp.gtnotgood.common.parts.mestock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.xyp.gtnotgood.common.blocks.mestock.TileMERequester;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridCache;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;

/**
 * Network-owned requester scheduler. At most eight devices are visited per tick, two plans calculate concurrently,
 * and one new plan starts per ten ticks. CPU outputs are indexed once per twenty ticks when demanded.
 * Exact stack watchers coalesce wakeups; delayed retries are removed eagerly, bounding queue size by device count.
 * Standalone AE jobs return products to storage and persist in their CPUs, including requester unloads/restarts.
 */
public final class StockGridCache implements IGridCache {

    public static final int devicesPerTick = 8;
    public static final int maxPlanning = 2;
    private final IGrid grid;
    private final LinkedHashSet<TileMERequester> ready = new LinkedHashSet<>();
    private final IdentityHashMap<TileMERequester, Long> scheduled = new IdentityHashMap<>();
    private final TreeMap<Long, LinkedHashSet<TileMERequester>> delayed = new TreeMap<>();
    private final LinkedHashSet<TileMERequester> machines = new LinkedHashSet<>();
    private final Map<IAEStack<?>, Long> inFlight = new HashMap<>();
    private final List<Plan> plans = new ArrayList<>();
    private List<TileMERequester> listing = Collections.emptyList();
    private boolean listingDirty;
    private long directoryRevision;
    private long tick;
    private long nextPlanTick;
    private long snapshotTick = Long.MIN_VALUE;
    public long cpuScans;
    public long deviceVisits;
    public long plansStarted;

    public StockGridCache(IGrid grid) {
        this.grid = grid;
    }

    public void schedule(TileMERequester tile, int delay) {
        if (!machines.contains(tile)) return;
        long due = tick + Math.max(0, delay);
        Long old = scheduled.get(tile);
        if (ready.contains(tile) || old != null && old <= due) return;
        if (old != null) {
            var group = delayed.get(old);
            group.remove(tile);
            if (group.isEmpty()) delayed.remove(old);
        }
        if (due <= tick) {
            scheduled.remove(tile);
            ready.add(tile);
        } else {
            scheduled.put(tile, due);
            delayed.computeIfAbsent(due, ignored -> new LinkedHashSet<>()).add(tile);
        }
    }

    public List<TileMERequester> listing() {
        if (listingDirty) {
            ArrayList<TileMERequester> next = new ArrayList<>(machines);
            next.sort(Comparator.comparingInt((TileMERequester t) -> t.getWorldObj().provider.dimensionId)
                .thenComparingInt(t -> t.xCoord).thenComparingInt(t -> t.yCoord).thenComparingInt(t -> t.zCoord));
            listing = Collections.unmodifiableList(next);
            listingDirty = false;
        }
        return listing;
    }

    public void directoryChanged() {
        directoryRevision++;
    }

    long directoryRevision() {
        return directoryRevision;
    }

    public long pending(IAEStack<?> key) {
        ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
        if (snapshotTick == Long.MIN_VALUE || tick - snapshotTick >= 20) {
            inFlight.clear();
            for (var cpu : crafting.getCpus()) {
                if (!cpu.isBusy()) continue;
                IAEStack<?> output = cpu.getFinalMultiOutput();
                if (output != null) addPending(output, output.getStackSize());
            }
            snapshotTick = tick;
            cpuScans++;
        }
        long result = inFlight.getOrDefault(key, 0L);
        for (Plan plan : plans)
            if (plan.key.isSameType(key)) result = StockResources.add(result, plan.key.getStackSize());
        return result;
    }

    private void addPending(IAEStack<?> key, long amount) {
        IAEStack<?> identity = key.copy().setStackSize(1);
        inFlight.put(identity, StockResources.add(inFlight.getOrDefault(identity, 0L), Math.max(0, amount)));
    }

    public boolean calculating(TileMERequester tile, int row) {
        for (Plan plan : plans) if (plan.tile == tile && plan.row == row) return true;
        return false;
    }

    public boolean start(TileMERequester tile, int row, long amount) throws GridAccessException {
        if (plans.size() >= maxPlanning || tick < nextPlanTick) return false;
        IAEStack<?> output = tile.stockConfig().key(row).copy().setStackSize(amount);
        ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
        Future<ICraftingJob> future = crafting.beginCraftingJob(tile.getWorldObj(), grid, new MachineSource(tile),
            output, null);
        plans.add(new Plan(tile, row, output, tile.stockConfig().revision(), future, tick));
        nextPlanTick = tick + 10;
        plansStarted++;
        return true;
    }

    @Override
    public void onUpdateTick() {
        tick++;
        finishPlans();
        int woke = 0;
        while (!delayed.isEmpty() && delayed.firstKey() <= tick && woke < devicesPerTick) {
            var group = delayed.firstEntry().getValue();
            var iterator = group.iterator();
            TileMERequester tile = iterator.next();
            iterator.remove();
            scheduled.remove(tile);
            ready.add(tile);
            if (group.isEmpty()) delayed.pollFirstEntry();
            woke++;
        }
        for (int visited = 0; visited < devicesPerTick && !ready.isEmpty(); visited++) {
            var iterator = ready.iterator();
            TileMERequester tile = iterator.next();
            iterator.remove();
            deviceVisits++;
            tile.service(this);
        }
    }

    private void finishPlans() {
        for (int i = plans.size() - 1; i >= 0; i--) {
            Plan plan = plans.get(i);
            boolean stale = !machines.contains(plan.tile) || plan.tile.isInvalid()
                || plan.tile.stockConfig().revision() != plan.revision
                || !plan.tile.canRequest()
                || tick - plan.started > 1200;
            if (!stale && !plan.future.isDone()) continue;
            plans.remove(i);
            if (stale) {
                plan.future.cancel(true);
                schedule(plan.tile, 20);
                continue;
            }
            int delay = 20;
            try {
                ICraftingJob job = plan.future.get(); // isDone checked above; never waits on the server tick.
                long stored = StockResources.count(plan.tile.getProxy().getStorage(), plan.key);
                long missing = StockResources.missing(plan.tile.stockConfig().amount(plan.row), stored,
                    pending(plan.key));
                if (missing == 0) {
                    plan.tile.setStatus(plan.row, TileMERequester.ready);
                } else if (job.isSimulation()) {
                    plan.tile.setStatus(plan.row, TileMERequester.missingMaterials);
                    delay = 200;
                } else if (missing < plan.key.getStackSize()) {
                    // Inventory/config changes during calculation must not turn into excessive background orders.
                    plan.tile.setStatus(plan.row, TileMERequester.waiting);
                } else {
                    ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
                    var link = crafting.submitJob(job, null, null, false, new MachineSource(plan.tile));
                    if (link == null) {
                        plan.tile.setStatus(plan.row, TileMERequester.waitingCPU);
                        delay = 100;
                    } else {
                        addPending(plan.key, job.getOutput().getStackSize());
                        plan.tile.setStatus(plan.row, TileMERequester.crafting);
                    }
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                plan.tile.setStatus(plan.row, TileMERequester.failed);
                delay = 200;
            } catch (ExecutionException | GridAccessException | RuntimeException error) {
                plan.tile.setStatus(plan.row, TileMERequester.failed);
                delay = 200;
            }
            plan.tile.delayRow(plan.row, tick + delay);
            schedule(plan.tile, delay);
        }
    }

    @Override
    public void addNode(IGridNode node, IGridHost host) {
        if (host instanceof TileMERequester tile) {
            machines.add(tile);
            tile.attach(this);
            listingDirty = true;
            directoryChanged();
            schedule(tile, 1);
        }
    }

    @Override
    public void removeNode(IGridNode node, IGridHost host) {
        if (!(host instanceof TileMERequester tile)) return;
        machines.remove(tile);
        ready.remove(tile);
        Long old = scheduled.remove(tile);
        if (old != null) {
            var group = delayed.get(old);
            group.remove(tile);
            if (group.isEmpty()) delayed.remove(old);
        }
        tile.detach(this);
        listingDirty = true;
        directoryChanged();
        for (int i = plans.size() - 1; i >= 0; i--) {
            if (plans.get(i).tile == tile) {
                plans.remove(i).future.cancel(true);
            }
        }
    }

    @Override
    public void onSplit(IGridStorage storage) {
        snapshotTick = Long.MIN_VALUE;
    }

    @Override
    public void onJoin(IGridStorage storage) {
        snapshotTick = Long.MIN_VALUE;
    }

    @Override
    public void populateGridStorage(IGridStorage storage) {}

    public int queueSize() {
        return ready.size() + scheduled.size();
    }

    public int planningCount() {
        return plans.size();
    }

    /** Returns the scheduler tick used for requester retry deadlines. */
    public long now() {
        return tick;
    }

    private static final class Plan {

        final TileMERequester tile;
        final int row;
        final IAEStack<?> key;
        final long revision;
        final Future<ICraftingJob> future;
        final long started;

        Plan(TileMERequester tile, int row, IAEStack<?> key, long revision, Future<ICraftingJob> future, long started) {
            this.tile = tile;
            this.row = row;
            this.key = key;
            this.revision = revision;
            this.future = future;
            this.started = started;
        }
    }
}
