package com.xyp.gtnotgood.client.research;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.client.lib.PlayerNotifications;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.Config;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketPlayerCompleteToServer;
import thaumcraft.common.lib.research.ResearchManager;

public final class TargetResearchController {

    private static final int TRANSFER_TIMEOUT_TICKS = 60;
    private static final int TRANSFER_RESTART_DELAY_TICKS = 20;
    private static final int USE_RETRY_INTERVAL_TICKS = 40;
    private static final int USE_TIMEOUT_TICKS = 400;
    private static final int MAX_USE_ATTEMPTS = 4;

    private static Task active;

    private TargetResearchController() {}

    public static void start(GuiResearchTableHelperInterface helper, EntityPlayer player, Minecraft mc,
        ResearchItem target, boolean includePrerequisites) {
        List<ResearchItem> plan = ResearchCatalog.prerequisitePlan(target, includePrerequisites);
        startPlan(helper, player, mc, plan, target == null ? "" : target.key);
    }

    public static void startPlan(GuiResearchTableHelperInterface helper, EntityPlayer player, Minecraft mc,
        Collection<ResearchItem> research, String label) {
        startPlan(helper, player, mc, research, label, null, null);
    }

    public static void startAllResearchable(GuiResearchTableHelperInterface helper, EntityPlayer player, Minecraft mc,
        ResearchCatalog.Scope scope, String category) {
        startPlan(helper, player, mc, ResearchCatalog.actionable(player, scope, category),
            scope == ResearchCatalog.Scope.CurrentCategory ? category : "ALL", scope, category);
    }

    public static void startExistingNotes(GuiResearchTableHelperInterface helper, EntityPlayer player, Minecraft mc) {
        List<ResearchItem> research = new ArrayList<>();
        int discoveries = 0;
        int incomplete = 0;
        for (String key : helper.researchNoteKeys()) {
            ResearchItem item = ResearchCategories.getResearch(key);
            if (item != null && !ResearchManager.isResearchComplete(player.getCommandSenderName(), key)) {
                research.add(item);
                if (helper.findCompletedResearchNoteSlot(key) >= 0) discoveries++;
                else if (helper.countIncompleteResearchNotes(key) > 0) incomplete++;
            }
        }
        if (research.isEmpty()) {
            PlayerNotifications.addNotification(StatCollector.translateToLocal("tcautores.batch_none"));
            return;
        }
        PlayerNotifications.addNotification(String.format(StatCollector.translateToLocal("tcautores.batch_plan"),
            discoveries, incomplete, research.size()));
        startPlan(helper, player, mc, research, "NOTES");
    }

    private static void startPlan(GuiResearchTableHelperInterface helper, EntityPlayer player, Minecraft mc,
        Collection<ResearchItem> research, String label, ResearchCatalog.Scope dynamicScope, String category) {
        cancel();
        ResearchNoteGenerationController.cancel();
        BatchResearchController.cancel();
        ResearchSolveController.cancel();
        LinkedHashMap<String, ResearchItem> unique = new LinkedHashMap<>();
        if (research != null) {
            for (ResearchItem item : research) if (item != null) unique.put(item.key, item);
        }
        List<ResearchItem> plan = new ArrayList<>(unique.values());
        if (plan.isEmpty()) {
            notifyFailure(player, "tcautores.target_invalid");
            return;
        }
        active = new Task(helper, player, mc, plan, label == null ? "" : label, dynamicScope, category);
    }

    public static void attach(GuiResearchTableHelperInterface helper, EntityPlayer player, Minecraft mc) {
        Task current = active;
        if (current != null && current.player == player) current.attach(helper, mc);
    }

    public static void onResearchTableClosed(GuiResearchTableHelperInterface helper) {
        Task current = active;
        if (current != null && current.helper == helper) current.detach();
    }

    public static void cancel() {
        Task current = active;
        active = null;
        ResearchNoteGenerationController.cancel();
        BatchResearchController.cancel();
        ContainerTransferController.clear();
        if (current != null) ResearchSolveController.cancel();
    }

    public static boolean isRunning() {
        return active != null;
    }

    public static String buttonText() {
        Task current = active;
        if (current == null) return StatCollector.translateToLocal("tcautores.target_search");
        return String.format(StatCollector.translateToLocal("tcautores.target_stop_progress"), current.completed,
            current.total);
    }

    public static void clientTick() {
        Task current = active;
        if (current != null) current.tick();
    }

    private static void notifyFailure(EntityPlayer player, String key) {
        String message = StatCollector.translateToLocal("tcautores.target_failed") + ": "
            + StatCollector.translateToLocal(key);
        PlayerNotifications.addNotification(message);
        if (player != null) player.addChatMessage(new ChatComponentText(message));
    }

    private enum Phase {
        WaitGui,
        Advance,
        MovePenOut,
        Generating,
        MovePenBack,
        Solving,
        MoveDiscoveryOut,
        SwapDiscoveryIn,
        WaitUse,
        SwapHeldBack,
        DirectResearch,
        RestartWait
    }

    private static final class Task {

        final EntityPlayer player;
        final ArrayDeque<ResearchItem> plan;
        final Set<String> seen = new HashSet<>();
        final String targetKey;
        final ResearchCatalog.Scope dynamicScope;
        final String category;
        Minecraft mc;
        GuiResearchTableHelperInterface helper;
        GuiResearchTable gui;
        ResearchItem current;
        Phase phase = Phase.WaitGui;
        int completed;
        int total;
        int tick;
        int deadline;
        int acceptedTick = -1;
        Phase recoveryPhase = Phase.Advance;
        Phase pausedPhase = Phase.Advance;
        boolean penMoved;
        int discoverySlot = -1;
        int discoveryHotbarIndex = -1;
        int useAttempts;
        int nextUseRetryTick;
        ItemStack originalHeld;

        private Task(GuiResearchTableHelperInterface helper, EntityPlayer player, Minecraft mc, List<ResearchItem> plan,
            String targetKey, ResearchCatalog.Scope dynamicScope, String category) {
            this.player = player;
            this.mc = mc;
            this.plan = new ArrayDeque<>(plan);
            this.total = plan.size();
            this.targetKey = targetKey;
            this.dynamicScope = dynamicScope;
            this.category = category;
            for (ResearchItem item : plan) seen.add(item.key);
            attach(helper, mc);
        }

        void attach(GuiResearchTableHelperInterface newHelper, Minecraft newMinecraft) {
            helper = newHelper;
            gui = (GuiResearchTable) newHelper;
            mc = newMinecraft;
            if (phase == Phase.WaitGui) {
                Phase resumePhase = pausedPhase;
                pausedPhase = Phase.Advance;
                if (resumePhase == Phase.WaitUse) {
                    phase = Phase.WaitUse;
                    nextUseRetryTick = tick + USE_RETRY_INTERVAL_TICKS;
                    deadline = tick + USE_TIMEOUT_TICKS;
                } else if (resumePhase == Phase.RestartWait) {
                    phase = Phase.RestartWait;
                    deadline = tick;
                } else if (isTransferPhase(resumePhase)) {
                    recoveryPhase = resumePhase;
                    phase = Phase.RestartWait;
                    deadline = tick;
                } else {
                    phase = Phase.Advance;
                }
            }
        }

        void detach() {
            if (phase != Phase.WaitGui) pausedPhase = phase;
            if (helper != null) BatchResearchController.onResearchTableClosed(helper);
            ResearchNoteGenerationController.cancel();
            ResearchSolveController.cancel();
            ContainerTransferController.clear();
            helper = null;
            gui = null;
            phase = Phase.WaitGui;
        }

        private static boolean isTransferPhase(Phase candidate) {
            return candidate == Phase.MovePenOut || candidate == Phase.MovePenBack
                || candidate == Phase.MoveDiscoveryOut
                || candidate == Phase.SwapDiscoveryIn
                || candidate == Phase.SwapHeldBack
                || candidate == Phase.RestartWait;
        }

        void tick() {
            if (active != this) return;
            if (mc == null || mc.thePlayer != player) {
                fail("tcautores.target_player_changed");
                return;
            }
            tick++;

            if (phase == Phase.WaitGui || helper == null || gui == null) return;
            if (phase == Phase.RestartWait) {
                if (mc.currentScreen != gui || tick < deadline) return;
                resumeAfterTransferFailure();
                return;
            }

            if (phase == Phase.WaitUse) {
                tickWaitUse();
                return;
            }
            if (phase == Phase.DirectResearch) {
                tickDirectResearch();
                return;
            }
            if (
                current != null && phase != Phase.SwapDiscoveryIn
                    && phase != Phase.SwapHeldBack
                    && ResearchManager.isResearchComplete(player.getCommandSenderName(), current.key)
            ) {
                completeCurrent();
            }

            if (phase == Phase.WaitGui || phase == Phase.Generating || phase == Phase.Solving) return;
            if (mc.currentScreen != gui) {
                detach();
                return;
            }
            if (phase == Phase.Advance) advance();
            else if (phase == Phase.MovePenOut) tickPenOut();
            else if (phase == Phase.MovePenBack) tickPenBack();
            else if (phase == Phase.MoveDiscoveryOut) tickDiscoveryOut();
            else if (phase == Phase.SwapDiscoveryIn) tickSwapIn();
            else if (phase == Phase.SwapHeldBack) tickSwapBack();
        }

        private void advance() {
            while (current == null && !plan.isEmpty()) {
                ResearchItem candidate = plan.removeFirst();
                if (ResearchManager.isResearchComplete(player.getCommandSenderName(), candidate.key)) {
                    completed++;
                } else {
                    current = candidate;
                }
            }
            if (current == null && appendNewlyResearchable()) advance();
            if (current == null) {
                finish();
                return;
            }
            if (!isRevealed(current)) {
                failReason(String.format(StatCollector.translateToLocal("tcautores.target_hidden_detail"),
                    current.getName(), current.key, HiddenResearchUnlocks.describe(current)));
                return;
            }
            if (current.isVirtual() || current.isStub() || current.isAutoUnlock()) {
                fail("tcautores.target_not_note");
                return;
            }
            if (hasCompletedDiscovery(current.key)) {
                startAutomaticLearning();
                return;
            }
            if (helper.countIncompleteResearchNotes(current.key) > 0) {
                startSolve();
                return;
            }
            if (!ResearchManager.doesPlayerHaveRequisites(player.getCommandSenderName(), current.key)) {
                failPrerequisites();
                return;
            }
            if (isDirect(current)) {
                startDirectResearch();
                return;
            }
            startGeneration();
        }

        private boolean appendNewlyResearchable() {
            if (dynamicScope == null) return false;
            int added = 0;
            for (ResearchItem item : ResearchCatalog.actionable(player, dynamicScope, category)) {
                if (seen.add(item.key)) {
                    plan.addLast(item);
                    added++;
                }
            }
            total += added;
            return added > 0;
        }

        private void failPrerequisites() {
            List<String> completedResearch = ResearchManager.getResearchForPlayerSafe(player.getCommandSenderName());
            List<String> missing = ResearchCatalog.missingPrerequisiteKeys(current, completedResearch);
            if (missing.isEmpty()) {
                fail("tcautores.target_prerequisites");
                return;
            }
            List<String> descriptions = new ArrayList<>();
            List<String> hiddenParents = current.parentsHidden == null ? Collections.emptyList()
                : Arrays.asList(current.parentsHidden);
            for (String key : missing) {
                ResearchItem prerequisite = ResearchCategories.getResearch(key);
                String name = prerequisite == null ? key : prerequisite.getName() + " [" + key + "]";
                if (hiddenParents.contains(key))
                    name = StatCollector.translateToLocal("tcautores.target_hidden_parent") + " " + name;
                descriptions.add(name);
            }
            failReason(String.format(StatCollector.translateToLocal("tcautores.target_missing_prerequisites"),
                current.getName(), String.join(", ", descriptions)));
        }

        private boolean isRevealed(ResearchItem research) {
            List<String> completedResearch = ResearchManager.getResearchForPlayerSafe(player.getCommandSenderName());
            return ResearchCatalog.isRevealed(research, completedResearch);
        }

        private static boolean isDirect(ResearchItem research) {
            return ResearchCatalog.completesDirectly(research, Config.researchDifficulty);
        }

        private void startDirectResearch() {
            if (!ResearchCatalog.isDirectResearchAffordable(player, current)) {
                fail("tcautores.insufficient");
                return;
            }
            phase = Phase.DirectResearch;
            deadline = tick + USE_TIMEOUT_TICKS;
            PacketHandler.INSTANCE.sendToServer(new PacketPlayerCompleteToServer(current.key,
                player.getCommandSenderName(), player.worldObj.provider.dimensionId, (byte) 0));
        }

        private void tickDirectResearch() {
            if (ResearchManager.isResearchComplete(player.getCommandSenderName(), current.key)) {
                completeCurrent();
                return;
            }
            if (tick >= deadline) fail("tcautores.execution_timeout");
        }

        private boolean hasCompletedDiscovery(String key) {
            if (helper != null) {
                ItemStack tableStack = helper.researchNoteStack();
                if (ResearchNoteItems.hasKey(tableStack, key) && ResearchNoteItems.isComplete(tableStack)) return true;
            }
            for (ItemStack stack : player.inventory.mainInventory) {
                if (ResearchNoteItems.hasKey(stack, key) && ResearchNoteItems.isComplete(stack)) return true;
            }
            return false;
        }

        private void startGeneration() {
            if (ResearchManager.consumeInkFromPlayer(player, false)) {
                beginGeneration();
                return;
            }
            if (!helper.hasInk()) {
                fail("tcautores.generate_reason.no_ink");
                return;
            }
            penMoved = true;
            if (!beginTransfer(0, Phase.MovePenOut)) fail("tcautores.batch_transfer_failed");
        }

        private void tickPenOut() {
            if (!transferAccepted()) return;
            boolean ready = helper.scribingToolsStack() == null && ResearchManager.consumeInkFromPlayer(player, false);
            if (!ContainerTransferController.observePostState(ready)) {
                if (tick >= deadline) fail("tcautores.batch_transfer_state_timeout");
                return;
            }
            clearTransfer();
            beginGeneration();
        }

        private void beginGeneration() {
            phase = Phase.Generating;
            int reservedSlots = helper.researchNoteStack() == null ? 0 : 1;
            ResearchNoteGenerationController.start(player, mc, Collections.singletonList(current),
                this::generationFinished, false, reservedSlots);
        }

        private void generationFinished(ResearchNoteGenerationController.Result result) {
            if (active != this) return;
            if (penMoved) {
                pendingGenerationResult = result;
                int penSlot = helper == null ? -1 : helper.findUsableScribingToolsSlot();
                if (penSlot < 0) {
                    fail("tcautores.generate_reason.no_ink");
                    return;
                }
                if (!beginTransfer(penSlot, Phase.MovePenBack)) fail("tcautores.batch_transfer_failed");
                return;
            }
            afterGeneration(result);
        }

        private ResearchNoteGenerationController.Result pendingGenerationResult;

        private void tickPenBack() {
            if (!transferAccepted()) return;
            if (!ContainerTransferController.observePostState(helper.hasInk())) {
                if (tick >= deadline) fail("tcautores.batch_transfer_state_timeout");
                return;
            }
            clearTransfer();
            penMoved = false;
            ResearchNoteGenerationController.Result result = pendingGenerationResult;
            pendingGenerationResult = null;
            afterGeneration(result);
        }

        private void afterGeneration(ResearchNoteGenerationController.Result result) {
            if (
                result == null || result.reason != ResearchNoteGenerationController.EndReason.Complete
                    || helper.countIncompleteResearchNotes(current.key) <= 0
            ) {
                String reasonKey = result == null ? "tcautores.generate_reason.timeout" : result.reason.translationKey;
                fail(reasonKey);
                return;
            }
            startSolve();
        }

        private void startSolve() {
            phase = Phase.Solving;
            BatchResearchController.startForKey(helper, player, mc, current.key,
                new BatchResearchController.Listener() {

                    @Override
                    public void onSuccess(int solved) {
                        if (active != Task.this) return;
                        if (solved <= 0 && !hasCompletedDiscovery(current.key)) {
                            fail("tcautores.target_note_missing");
                            return;
                        }
                        startAutomaticLearning();
                    }

                    @Override
                    public void onFailure(String key, int solved) {
                        if (active == Task.this) fail(key);
                    }
                }, false);
        }

        private void startAutomaticLearning() {
            int slot = helper.findCompletedResearchNoteSlot(current.key);
            if (slot == 1) {
                if (!beginTransfer(1, Phase.MoveDiscoveryOut)) fail("tcautores.batch_transfer_failed");
            } else if (slot >= 2) {
                startSwapIn(slot);
            } else {
                fail("tcautores.target_note_missing");
            }
        }

        private void tickDiscoveryOut() {
            if (!transferAccepted()) return;
            if (!ContainerTransferController.observePostState(helper.researchNoteStack() == null)) {
                if (tick >= deadline) fail("tcautores.batch_transfer_state_timeout");
                return;
            }
            clearTransfer();
            int slot = helper.findCompletedResearchNoteSlot(current.key);
            if (slot < 2) {
                fail("tcautores.target_note_missing");
                return;
            }
            startSwapIn(slot);
        }

        private void startSwapIn(int slot) {
            if (slot == 29 + player.inventory.currentItem) {
                originalHeld = null;
                discoverySlot = -1;
                useHeldDiscovery();
                return;
            }
            originalHeld = player.getHeldItem() == null ? null : player.getHeldItem().copy();
            discoverySlot = slot;
            if (!beginSwapTransfer(slot, Phase.SwapDiscoveryIn)) {
                fail("tcautores.target_use_failed");
                return;
            }
        }

        private void tickSwapIn() {
            if (!transferAccepted()) return;
            ItemStack held = player.getHeldItem();
            boolean ready = ResearchNoteItems.isComplete(held) && ResearchNoteItems.hasKey(held, current.key);
            if (!ContainerTransferController.observePostState(ready)) {
                if (tick >= deadline) fail("tcautores.batch_transfer_state_timeout");
                return;
            }
            clearTransfer();
            useHeldDiscovery();
        }

        private void useHeldDiscovery() {
            ItemStack held = player.getHeldItem();
            if (!ResearchNoteItems.isComplete(held) || !ResearchNoteItems.hasKey(held, current.key)) {
                fail("tcautores.target_use_failed");
                return;
            }
            mc.playerController.sendUseItem(player, player.worldObj, held);
            discoveryHotbarIndex = player.inventory.currentItem;
            useAttempts = 1;
            nextUseRetryTick = tick + USE_RETRY_INTERVAL_TICKS;
            phase = Phase.WaitUse;
            deadline = tick + USE_TIMEOUT_TICKS;
        }

        private void tickWaitUse() {
            if (player.inventory.currentItem != discoveryHotbarIndex) {
                fail("tcautores.target_held_changed");
                return;
            }
            ItemStack held = player.getHeldItem();
            boolean discoveryStillHeld = ResearchNoteItems.isComplete(held)
                && ResearchNoteItems.hasKey(held, current.key);
            boolean researchLearned = ResearchManager.isResearchComplete(player.getCommandSenderName(), current.key);

            if (researchLearned) {
                finishAutomaticLearning();
                return;
            }

            if (!discoveryStillHeld) {
                // ItemResearchNotes does not shrink the stack on the remote world. Its disappearance therefore
                // comes from the server's authoritative inventory update and proves that learning succeeded even
                // when PacketResearchComplete reaches the client late or is suppressed by another coremod.
                if (!researchLearned) {
                    Thaumcraft.proxy.getResearchManager().completeResearch(player, current.key);
                }
                finishAutomaticLearning();
                return;
            }

            if (tick >= nextUseRetryTick && useAttempts < MAX_USE_ATTEMPTS) {
                mc.playerController.sendUseItem(player, player.worldObj, held);
                useAttempts++;
                nextUseRetryTick = tick + USE_RETRY_INTERVAL_TICKS;
            }

            if (tick >= deadline) fail("tcautores.target_use_timeout");
        }

        private void finishAutomaticLearning() {
            if (originalHeld == null) {
                completeCurrent();
                return;
            }
            ItemStack displaced = player.openContainer.getSlot(discoverySlot).getStack();
            if (!ItemStack.areItemStacksEqual(originalHeld, displaced)) {
                fail("tcautores.target_held_changed");
                return;
            }
            ContainerTransferController.clear();
            if (!beginSwapTransfer(discoverySlot, Phase.SwapHeldBack)) {
                fail("tcautores.target_use_failed");
                return;
            }
        }

        private void tickSwapBack() {
            if (!transferAccepted()) return;
            if (
                !ContainerTransferController
                    .observePostState(ItemStack.areItemStacksEqual(originalHeld, player.getHeldItem()))
            ) {
                if (tick >= deadline) fail("tcautores.batch_transfer_state_timeout");
                return;
            }
            clearTransfer();
            completeCurrent();
        }

        private void completeCurrent() {
            completed++;
            current = null;
            originalHeld = null;
            discoverySlot = -1;
            discoveryHotbarIndex = -1;
            useAttempts = 0;
            nextUseRetryTick = 0;
            phase = gui == null ? Phase.WaitGui : Phase.Advance;
        }

        private boolean beginTransfer(int slot, Phase nextPhase) {
            ContainerTransferController.clear();
            if (!ContainerTransferController.begin(mc, player, slot)) return false;
            phase = nextPhase;
            deadline = tick + TRANSFER_TIMEOUT_TICKS;
            acceptedTick = -1;
            return true;
        }

        private boolean beginSwapTransfer(int slot, Phase nextPhase) {
            ContainerTransferController.clear();
            if (!ContainerTransferController.beginSwap(mc, player, slot, player.inventory.currentItem)) return false;
            phase = nextPhase;
            deadline = tick + TRANSFER_TIMEOUT_TICKS;
            acceptedTick = -1;
            return true;
        }

        private boolean transferAccepted() {
            if (!ContainerTransferController.matchesOpenContainer(player)) {
                fail("tcautores.batch_container_changed");
                return false;
            }
            ContainerTransferController.Status status = ContainerTransferController.status();
            if (status == ContainerTransferController.Status.Rejected) {
                if (tick >= deadline) scheduleTransferRecovery();
                return false;
            }
            if (status == ContainerTransferController.Status.Resynchronized) {
                if (!ContainerTransferController.retryReady(tick)) return false;
                if (!ContainerTransferController.canRetry() || !ContainerTransferController.retry(mc, player)) {
                    scheduleTransferRecovery();
                    return false;
                }
                acceptedTick = -1;
                deadline = tick + TRANSFER_TIMEOUT_TICKS;
                return false;
            }
            if (status == ContainerTransferController.Status.Accepted) {
                if (!ContainerTransferController.hasServerStateUpdate()) {
                    if (tick >= deadline) fail("tcautores.batch_transfer_state_timeout");
                    return false;
                }
                if (acceptedTick < 0) {
                    acceptedTick = tick;
                    deadline = Math.max(deadline, tick + TRANSFER_TIMEOUT_TICKS);
                    return false;
                }
                return ContainerTransferController.hasSettled(tick, acceptedTick);
            }
            if (tick >= deadline) scheduleTransferRecovery();
            return false;
        }

        private void scheduleTransferRecovery() {
            if (phase == Phase.RestartWait) return;
            recoveryPhase = phase;
            ContainerTransferController.clear();
            ResearchNoteGenerationController.cancel();
            BatchResearchController.cancel();
            ResearchSolveController.cancel();
            acceptedTick = -1;
            phase = Phase.RestartWait;
            deadline = tick + TRANSFER_RESTART_DELAY_TICKS;
        }

        private void resumeAfterTransferFailure() {
            Phase failedPhase = recoveryPhase;
            recoveryPhase = Phase.Advance;
            switch (failedPhase) {
                case MovePenOut:
                    if (helper.scribingToolsStack() == null && ResearchManager.consumeInkFromPlayer(player, false)) {
                        beginGeneration();
                    } else if (!beginTransfer(0, Phase.MovePenOut)) {
                        phase = Phase.Advance;
                    }
                    return;
                case MovePenBack:
                    if (helper.hasInk()) {
                        penMoved = false;
                        ResearchNoteGenerationController.Result result = pendingGenerationResult;
                        pendingGenerationResult = null;
                        afterGeneration(result);
                    } else {
                        int penSlot = helper.findUsableScribingToolsSlot();
                        if (penSlot < 0 || !beginTransfer(penSlot, Phase.MovePenBack)) phase = Phase.Advance;
                    }
                    return;
                case MoveDiscoveryOut:
                    if (helper.researchNoteStack() == null) {
                        int slot = helper.findCompletedResearchNoteSlot(current.key);
                        if (slot < 2) {
                            phase = Phase.Advance;
                        } else {
                            startSwapIn(slot);
                        }
                    } else if (!beginTransfer(1, Phase.MoveDiscoveryOut)) {
                        phase = Phase.Advance;
                    }
                    return;
                case SwapDiscoveryIn:
                    if (
                        ResearchNoteItems.isComplete(player.getHeldItem())
                            && ResearchNoteItems.hasKey(player.getHeldItem(), current.key)
                    ) {
                        useHeldDiscovery();
                    } else if (discoverySlot < 0 || !beginSwapTransfer(discoverySlot, Phase.SwapDiscoveryIn)) {
                        phase = Phase.Advance;
                    }
                    return;
                case SwapHeldBack:
                    if (ItemStack.areItemStacksEqual(originalHeld, player.getHeldItem())) {
                        completeCurrent();
                    } else if (discoverySlot < 0 || !beginSwapTransfer(discoverySlot, Phase.SwapHeldBack)) {
                        phase = Phase.Advance;
                    }
                    return;
                default:
                    phase = Phase.Advance;
                    return;
            }
        }

        private void clearTransfer() {
            ContainerTransferController.clear();
            acceptedTick = -1;
            deadline = 0;
        }

        private void finish() {
            if (active != this) return;
            active = null;
            ContainerTransferController.clear();
            String message = String.format(StatCollector.translateToLocal("tcautores.target_complete"), completed,
                total, targetKey);
            PlayerNotifications.addNotification(message);
            player.addChatMessage(new ChatComponentText(message));
        }

        private void fail(String key) {
            if (isRecoverableTransferFailure(key)) {
                scheduleTransferRecovery();
                return;
            }
            failReason(StatCollector.translateToLocal(key));
        }

        private static boolean isRecoverableTransferFailure(String key) {
            return "tcautores.batch_transfer_failed".equals(key) || "tcautores.batch_transfer_rejected".equals(key)
                || "tcautores.batch_transfer_timeout".equals(key)
                || "tcautores.batch_transfer_state_timeout".equals(key)
                || "tcautores.batch_container_changed".equals(key);
        }

        private void failReason(String reason) {
            if (active != this) return;
            active = null;
            ResearchNoteGenerationController.cancel();
            BatchResearchController.cancel();
            ContainerTransferController.clear();
            ResearchSolveController.cancel();
            String message = StatCollector.translateToLocal("tcautores.target_failed") + ": " + reason;
            PlayerNotifications.addNotification(message);
            player.addChatMessage(new ChatComponentText(message));
        }
    }
}
