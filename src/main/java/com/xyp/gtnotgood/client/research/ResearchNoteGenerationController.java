package com.xyp.gtnotgood.client.research;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.lib.PlayerNotifications;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketPlayerCompleteToServer;
import thaumcraft.common.lib.research.ResearchManager;

public final class ResearchNoteGenerationController {

    private static final int ACK_TIMEOUT_TICKS = 100;

    public enum EndReason {

        Complete("tcautores.generate_reason.complete"),
        InventoryFull("tcautores.generate_reason.inventory_full"),
        WorkspaceRequired("tcautores.generate_reason.workspace_required"),
        NoPaper("tcautores.generate_reason.no_paper"),
        NoInk("tcautores.generate_reason.no_ink"),
        Timeout("tcautores.generate_reason.timeout"),
        Cancelled("tcautores.generate_reason.cancelled");

        public final String translationKey;

        EndReason(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    public static final class Result {

        public final int generated;
        public final int total;
        public final EndReason reason;
        public final String pendingKey;

        private Result(int generated, int total, EndReason reason, String pendingKey) {
            this.generated = generated;
            this.total = total;
            this.reason = reason;
            this.pendingKey = pendingKey;
        }
    }

    public interface Listener {

        Listener NONE = result -> {};

        void onFinished(Result result);
    }

    private static Task active;
    private static volatile long revision;
    private static volatile Result lastResult;

    private ResearchNoteGenerationController() {}

    public static void start(EntityPlayer player, Minecraft mc, Collection<ResearchItem> research, Listener listener,
        boolean notify) {
        start(player, mc, research, listener, notify, 0);
    }

    public static void start(EntityPlayer player, Minecraft mc, Collection<ResearchItem> research, Listener listener,
        boolean notify, int reservedEmptySlots) {
        cancel();
        ArrayDeque<String> keys = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        for (ResearchItem item : research) {
            if (item != null && seen.add(item.key)) keys.addLast(item.key);
        }
        Listener completionListener = listener == null ? Listener.NONE : listener;
        if (keys.isEmpty()) {
            finish(new Result(0, 0, EndReason.Complete, ""), completionListener, notify, player);
            return;
        }
        active = new Task(player, mc, keys, completionListener, notify, Math.max(0, reservedEmptySlots));
        revision++;
    }

    public static void cancel() {
        Task current = active;
        active = null;
        if (current != null) {
            lastResult = new Result(current.generated, current.total, EndReason.Cancelled, current.pendingKey);
            revision++;
        }
    }

    public static boolean isRunning() {
        return active != null;
    }

    public static long revision() {
        return revision;
    }

    public static Result lastResult() {
        return lastResult;
    }

    public static String progressText() {
        Task current = active;
        if (current == null) return StatCollector.translateToLocal("tcautores.generate_visible");
        return String.format(StatCollector.translateToLocal("tcautores.generate_progress"), current.generated,
            current.total);
    }

    public static void clientTick() {
        Task current = active;
        if (current != null) current.tick();
    }

    private static void finish(Result result, Listener listener, boolean notify, EntityPlayer player) {
        active = null;
        lastResult = result;
        revision++;
        if (notify && player != null) {
            String message = String.format(StatCollector.translateToLocal("tcautores.generate_finished"),
                result.generated, result.total, reasonText(result.reason));
            PlayerNotifications.addNotification(message);
            player.addChatMessage(new ChatComponentText(message));
        }
        listener.onFinished(result);
    }

    private static String reasonText(EndReason reason) {
        return StatCollector.translateToLocal(reason.translationKey);
    }

    static int generatedNoteCapacity(ItemStack[] inventory, int reservedEmptySlots) {
        int availableSlots = 0;
        if (inventory == null) return 0;
        for (ItemStack stack : inventory) {
            if (stack == null || stack.stackSize <= 0) availableSlots++;
            else if (stack.getItem() == Items.paper && stack.stackSize == 1) availableSlots++;
        }
        return Math.max(0, availableSlots - Math.max(0, reservedEmptySlots));
    }

    private static final class Task {

        final EntityPlayer player;
        final Minecraft mc;
        final ArrayDeque<String> keys;
        final Listener listener;
        final boolean notify;
        final int reservedEmptySlots;
        final int total;
        int generated;
        int tick;
        int deadline;
        String pendingKey = "";

        private Task(EntityPlayer player, Minecraft mc, ArrayDeque<String> keys, Listener listener, boolean notify,
            int reservedEmptySlots) {
            this.player = player;
            this.mc = mc;
            this.keys = keys;
            this.listener = listener;
            this.notify = notify;
            this.reservedEmptySlots = reservedEmptySlots;
            this.total = keys.size();
        }

        void tick() {
            if (active != this || mc.thePlayer != player) {
                stop(EndReason.Cancelled);
                return;
            }
            tick++;
            if (!pendingKey.isEmpty()) {
                if (ResearchManager.getResearchSlot(player, pendingKey) >= 0) {
                    generated++;
                    pendingKey = "";
                    deadline = 0;
                    revision++;
                } else if (tick >= deadline) {
                    stop(EndReason.Timeout);
                }
                return;
            }

            while (!keys.isEmpty()) {
                String key = keys.removeFirst();
                if (
                    ResearchManager.isResearchComplete(player.getCommandSenderName(), key)
                        || ResearchManager.getResearchSlot(player, key) >= 0
                ) continue;
                if (generatedNoteCapacity(player.inventory.mainInventory, reservedEmptySlots) <= 0) {
                    stop(reservedEmptySlots > 0 ? EndReason.WorkspaceRequired : EndReason.InventoryFull);
                    return;
                }
                if (!player.inventory.hasItem(Items.paper)) {
                    stop(EndReason.NoPaper);
                    return;
                }
                if (!ResearchManager.consumeInkFromPlayer(player, false)) {
                    stop(EndReason.NoInk);
                    return;
                }
                pendingKey = key;
                deadline = tick + ACK_TIMEOUT_TICKS;
                PacketHandler.INSTANCE.sendToServer(new PacketPlayerCompleteToServer(key, player.getCommandSenderName(),
                    player.worldObj.provider.dimensionId, (byte) 1));
                return;
            }
            stop(EndReason.Complete);
        }

        private void stop(EndReason reason) {
            if (active != this && reason != EndReason.Cancelled) return;
            finish(new Result(generated, total, reason, pendingKey), listener, notify, player);
        }
    }
}
