package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceHost;

import appeng.api.util.IInterfaceViewable;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;

/** Refreshes enlarged interface rows without sending partial updates beyond the client's current inventory. */
@Mixin(value = ContainerInterfaceTerminal.class, remap = false)
public abstract class LargeInterfaceTerminalRowsMixin {

    @Shadow
    @Final
    private Map<IInterfaceViewable, ?> tracked;

    @Shadow
    @Final
    private Map<Long, ?> trackedById;

    @Shadow
    public abstract void scheduleUpdate();

    /** updateList also runs inside the native constructor, before mixin field initializers would execute. */
    @Unique
    private Map<LargeInterfaceHost, Integer> largeinterface$knownRows;

    @Unique
    private Map<LargeInterfaceHost, Integer> largeinterface$rowSnapshots() {
        if (largeinterface$knownRows == null) largeinterface$knownRows = new HashMap<>();
        return largeinterface$knownRows;
    }

    @Inject(method = "updateList", at = @At("RETURN"), remap = false)
    private void largeinterface$recordRows(CallbackInfoReturnable<PacketInterfaceTerminalUpdate> cir) {
        if (Platform.isClient()) return;
        Map<LargeInterfaceHost, Integer> snapshots = largeinterface$rowSnapshots();
        snapshots.clear();
        for (IInterfaceViewable machine : tracked.keySet()) {
            if (machine instanceof LargeInterfaceHost host) snapshots.put(host, host.rows());
        }
    }

    @Inject(method = "detectAndSendChanges", at = @At("HEAD"), remap = false)
    private void largeinterface$refreshChangedRows(CallbackInfo ci) {
        if (Platform.isClient() || largeinterface$knownRows == null) return;
        for (Map.Entry<LargeInterfaceHost, Integer> snapshot : largeinterface$knownRows.entrySet()) {
            if (snapshot.getKey()
                .rows() != snapshot.getValue()) {
                scheduleUpdate();
                return;
            }
        }
    }

    @Inject(method = "doAction", at = @At("HEAD"), cancellable = true, remap = false)
    private void largeinterface$deferStaleInventoryAction(EntityPlayerMP player, InventoryAction action, int slot,
        long id, CallbackInfo ci) {
        if (Platform.isClient() || largeinterface$knownRows == null) return;
        Object tracker = trackedById.get(id);
        if (tracker == null) return;
        for (Map.Entry<LargeInterfaceHost, Integer> snapshot : largeinterface$knownRows.entrySet()) {
            LargeInterfaceHost host = snapshot.getKey();
            if (tracked.get(host) != tracker) continue;
            if (host.rows() != snapshot.getValue()) {
                scheduleUpdate();
                ci.cancel();
            }
            return;
        }
    }
}
