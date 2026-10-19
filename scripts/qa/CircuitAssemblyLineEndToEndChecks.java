package com.xyp.gtnotgood.common.blocks.packaged;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xyp.gtnotgood.utils.enums.ModList;

import bartworks.API.enums.CircuitImprint;
import bartworks.API.recipe.BartWorksRecipeMaps;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

@Mod(
    modid = "circuitassemblye2e",
    name = "Circuit Assembly Line End To End QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class CircuitAssemblyLineEndToEndChecks {

    private boolean started;
    private volatile boolean verified, finished;
    private String saveName;
    private int clientTicks, ticks, stage, submittedAt, stableTicks, frames;
    private int runningTicks, starts, peakProgress, peakReceipts;
    private boolean lastRunning;
    private long peakEut, peakStoredEnergy, probeBefore, probeDrop;
    private double peakCableEut;
    private int probeAt;
    private GTRecipe recipe;
    private CircuitAssemblyLineQaStructure.Rig rig;
    private CircuitAssemblyLineQaNetwork network;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.circuitAssembly.e2e")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks < 14000, "client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.guiScale = 2;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                saveName = "circuit-assembly-full-" + System.currentTimeMillis();
                mc.launchIntegratedServer(saveName, "Circuit Assembly Full Test",
                    new WorldSettings(742L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (verified && mc.theWorld != null) {
                mc.displayGuiScreen(null);
                mc.gameSettings.hideGUI = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || verified) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            if (++ticks >= 12000) throw new AssertionError("server timeout: " + diagnostics());
            var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (ticks == 1) setup(player);
            sample();
            if (ticks % 20 == 0) writeProgress();
            if (stage == 0) {
                if (ticks < 140 || !rig.line.mMachine || !network.ready() || rig.storedEnergy() < 512) return;
                rig.verifyStructure();
                require(rig.line.getCircuitImprint() == CircuitImprint.Microprocessor,
                    "native permanent imprint retained before the AE order");
                require(rig.line.getRepairStatus() == rig.line.getIdealStatus(), "native maintenance completed");
                network.supplyAndPlan();
                stage = 1;
                log("STRUCTURE_AND_AE_READY " + diagnostics());
            } else if (stage == 1) {
                if (!network.submitIfReady()) return;
                submittedAt = ticks;
                stage = 2;
                log("AE_ORDER_SUBMITTED " + network.expectedOutput() + " " + recipe.mOutputs[0].getDisplayName());
            } else if (stage == 2) {
                require(!network.link.isCanceled(), "native AE order was not canceled");
                require(network.storedOutput() <= network.expectedOutput(), "no duplicated result in ME storage");
                if (
                    probeAt == 0 && rig.line.mMaxProgresstime > 8
                        && rig.line.mProgresstime > 0
                        && rig.storedEnergy() > 6L * Math.abs(rig.line.mEUt)
                ) {
                    probeBefore = rig.storedEnergy();
                    probeAt = ticks;
                    rig.setPower(false);
                    log("POWER_PROBE_BEGIN storedEU=" + probeBefore);
                } else if (probeAt > 0 && probeDrop == 0 && ticks >= probeAt + 4) {
                    probeDrop = probeBefore - rig.storedEnergy();
                    rig.setPower(true);
                    require(probeDrop > 0, "ordinary energy hatch actually supplies EU to the running CAL");
                    log("POWER_PROBE_END consumedEU=" + probeDrop);
                }
                if (!network.complete()) {
                    stableTicks = 0;
                    return;
                }
                if (++stableTicks < 40) return;
                verifyAndSave(player);
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) throws Exception {
        recipe = BartWorksRecipeMaps.circuitAssemblyLineRecipes.getAllRecipes().stream()
            .filter(candidate -> candidate.mSpecialItems instanceof ItemStack imprint
                && GTUtility.areStacksEqual(imprint, CircuitImprint.Microprocessor.imprint.get(1), true))
            .filter(CircuitAssemblyLineEndToEndChecks::suitable)
            .min(Comparator.comparingInt(candidate -> candidate.mDuration))
            .orElseThrow(() -> new IllegalStateException("No suitable real microprocessor CAL recipe"));
        int tier = Math.max(1, GTUtility.getTier(recipe.mEUt));
        rig = CircuitAssemblyLineQaStructure.build(player, 8, 12, 8, recipe.mInputs.length, tier);
        ItemStack held = player.inventory.getCurrentItem();
        try {
            player.inventory.setInventorySlotContents(player.inventory.currentItem,
                CircuitImprint.Microprocessor.imprint.get(1));
            rig.line.onLeftclick(rig.base, player);
            require(rig.line.getCircuitImprint() == CircuitImprint.Microprocessor
                && player.inventory.getCurrentItem() == null, "native imprint interaction consumes its imprint");
        } finally {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, held);
        }
        network = new CircuitAssemblyLineQaNetwork(player, recipe, new PackagedTarget(player.dimension, 8, 12, 8, 2), 3,
            10, -3, 3);
        rig.base.enableWorking();
        player.worldObj.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        player.worldObj.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        player.worldObj.setWorldTime(6000);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(14, 17, -12, 38, 21);
        log("WORLD=" + saveName
            + "; realRecipe="
            + recipe.mOutputs[0].getDisplayName()
            + "; outputPerBatch="
            + recipe.mOutputs[0].stackSize
            + "; inputs="
            + recipe.mInputs.length
            + "; duration="
            + recipe.mDuration
            + "; EUt="
            + recipe.mEUt
            + "; energyTier="
            + tier);
    }

    private static boolean suitable(GTRecipe candidate) {
        if (
            candidate.mFakeRecipe || !candidate.mEnabled
                || candidate.mInputs.length == 0
                || candidate.mInputs.length > 6
                || candidate.mFluidInputs.length != 1
                || candidate.mOutputs.length != 1
                || candidate.mFluidOutputs.length != 0
                || candidate.mOutputs[0] == null
                || candidate.mOutputs[0].stackSize <= 0
                || candidate.mOutputs[0].stackSize > candidate.mOutputs[0].getMaxStackSize()
                || candidate.mDuration < 80
                || candidate.mDuration > 10000
                || candidate.mEUt <= 0
                || candidate.getOutputChance(0) != 10000
        ) return false;
        for (int i = 0; i < candidate.mInputs.length; i++) {
            ItemStack input = candidate.mInputs[i];
            if (
                input == null || input.stackSize <= 0
                    || input.stackSize > input.getMaxStackSize()
                    || candidate.getInputChance(i) != 10000
            ) return false;
        }
        return candidate.mFluidInputs[0] != null && candidate.mFluidInputs[0].amount > 0
            && candidate.mFluidInputs[0].amount <= 16000
            && candidate.getFluidInputChance(0) == 10000;
    }

    private void sample() {
        if (rig == null) return;
        long stored = rig.storedEnergy();
        peakStoredEnergy = Math.max(peakStoredEnergy, stored);
        var cableReadout = rig.cableReadout();
        if (cableReadout != null) peakCableEut = Math.max(peakCableEut, cableReadout.eut());
        boolean running = rig.line.mMaxProgresstime > 0;
        if (running && !lastRunning) {
            starts++;
            log("NATIVE_PROCESS_STARTED batch=" + starts
                + "; duration="
                + rig.line.mMaxProgresstime
                + "; EUt="
                + rig.line.mEUt
                + "; storedEU="
                + stored);
        }
        if (!running && lastRunning) log("NATIVE_PROCESS_FINISHED batch=" + starts + "; tick=" + ticks);
        if (running) {
            runningTicks++;
            peakProgress = Math.max(peakProgress, rig.line.mProgresstime);
            peakEut = Math.max(peakEut, Math.abs((long) rig.line.mEUt));
        }
        lastRunning = running;
        peakReceipts = Math.max(peakReceipts, network.provider.queuedJobs());
    }

    private void verifyAndSave(EntityPlayerMP player) throws Exception {
        rig.verifyStructure();
        network.verifyConsumed();
        require(starts == 3 && runningTicks > 0 && peakProgress > 0 && peakEut > 0,
            "three native recipe runs made natural progress while consuming power");
        require(probeDrop > 0 && peakStoredEnergy > 0 && peakCableEut > 0,
            "real cable supply and measurable energy consumption");
        require(rig.line.mMaxProgresstime == 0 && rig.line.getCircuitImprint() == CircuitImprint.Microprocessor,
            "finished controller is idle with its original permanent imprint");
        for (var bus : rig.inputs) require(empty(bus), "every ordered input bus is empty after natural processing");
        require(rig.input.getFluid() == null || rig.input.getFluid().amount == 0, "fluid input hatch is empty");
        require(empty(rig.output), "naturally produced items have left the physical output bus");
        for (int slot = TilePackagedProvider.PATTERNS; slot < TilePackagedProvider.CORE; slot++) {
            require(network.provider.getStackInSlot(slot) == null, "provider return slots fully flushed to ME storage");
        }
        writeReport();
        ((WorldServer) player.worldObj).saveAllChunks(true, null);
        FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().saveAllPlayerData();
        Files.write(new File(output(), "save-name.txt").toPath(), saveName.getBytes(StandardCharsets.UTF_8));
        log("NATIVE_AE_ORDER_COMPLETE output=" + network.storedOutput()
            + "; cell="
            + network.storedOutputInCell()
            + "; starts="
            + starts
            + "; runningTicks="
            + runningTicks
            + "; probeConsumedEU="
            + probeDrop);
        verified = true;
    }

    private void writeReport() throws Exception {
        JsonObject report = new JsonObject();
        report.addProperty("worldSave", saveName);
        report.addProperty("recipeOutput", recipe.mOutputs[0].getDisplayName());
        report.addProperty("recipeOutputPerBatch", recipe.mOutputs[0].stackSize);
        report.addProperty("recipeDurationTicks", recipe.mDuration);
        report.addProperty("recipeEUt", recipe.mEUt);
        report.addProperty("batches", 3);
        report.addProperty("machineFormed", rig.line.mMachine);
        report.addProperty("permanentImprint", rig.line.getCircuitImprint().name());
        report.addProperty("nativeProcessStarts", starts);
        report.addProperty("observedRunningTicks", runningTicks);
        report.addProperty("maximumObservedProgress", peakProgress);
        report.addProperty("maximumObservedEUt", peakEut);
        report.addProperty("energyHatchPeakEU", peakStoredEnergy);
        report.addProperty("peakNativeCableEUt", peakCableEut);
        report.addProperty("energyConsumedDuringSourceOffProbe", probeDrop);
        report.addProperty("peakProviderReceipts", peakReceipts);
        report.addProperty("aeOrderDone", network.orderDone());
        report.addProperty("nativeCpuObservedBusy", network.observedBusy);
        report.addProperty("nativeCpuCompletionCallback", network.completedByCpu);
        report.addProperty("nativeCpuCompletedOutputCount", network.completedOutputCount);
        report.addProperty("nativeCpuCompletionElapsedNanos", network.completedElapsedTime);
        report.addProperty("nativeCpuBusyAfterCompletion", network.cpu.isBusy());
        report.addProperty("nativeCpuUsedBytesAfterCompletion", network.cpu.getUsedStorage());
        report.addProperty("nativeCpuRemainingItemsAfterCompletion", network.cpu.getRemainingItemCount());
        report.addProperty("providerReceiptsAfterCompletion", network.provider.queuedJobs());
        report.addProperty("aeOutputCount", network.storedOutput());
        report.addProperty("physicalCellOutputCount", network.storedOutputInCell());
        report.addProperty("ticksFromSubmission", ticks - submittedAt);
        JsonArray inputs = new JsonArray();
        for (ItemStack item : recipe.mInputs) {
            JsonObject entry = new JsonObject();
            entry.addProperty("item", item.getDisplayName());
            entry.addProperty("amountPerBatch", item.stackSize);
            entry.addProperty("remainingInME", network.storedItem(item));
            inputs.add(entry);
        }
        report.add("itemInputs", inputs);
        JsonArray fluids = new JsonArray();
        for (var fluid : recipe.mFluidInputs) {
            JsonObject entry = new JsonObject();
            entry.addProperty("fluid", fluid.getLocalizedName());
            entry.addProperty("amountPerBatch", fluid.amount);
            entry.addProperty("remainingInME", network.storedFluid(fluid));
            fluids.add(entry);
        }
        report.add("fluidInputs", fluids);
        Files.write(new File(output(), "report.json").toPath(),
            new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
    }

    private String diagnostics() {
        try {
            return "tick=" + ticks
                + "; stage="
                + stage
                + "; formed="
                + (rig != null && rig.line.mMachine)
                + "; progress="
                + (rig == null ? "n/a" : rig.line.mProgresstime + "/" + rig.line.mMaxProgresstime)
                + "; storedEU="
                + (rig == null ? 0 : rig.storedEnergy())
                + "; starts="
                + starts
                + "; network="
                + (network == null ? "n/a" : network.status());
        } catch (Exception unavailable) {
            return "tick=" + ticks + "; stage=" + stage + "; grid=" + unavailable;
        }
    }

    private void writeProgress() throws Exception {
        Files.write(new File(output(), "progress.txt").toPath(), diagnostics().getBytes(StandardCharsets.UTF_8));
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !verified || finished || ++frames != 60) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            File screenshot = new File(output(), "screenshots/circuit-assembly-full.png");
            Files.deleteIfExists(screenshot.toPath());
            ScreenShotHelper.saveScreenshot(output(), "circuit-assembly-full.png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            require(screenshot.isFile() && screenshot.length() > 0 && ImageIO.read(screenshot) != null,
                "fresh world screenshot is a valid image");
            Files.write(new File(output(), "result.txt").toPath(),
                "PASS\nReal CAL structure, permanent imprint, native AE CPU order, physical cable power, three natural recipe completions and exact physical ME-cell return.\n"
                    .getBytes(StandardCharsets.UTF_8));
            finished = true;
            log("PASS");
            mc.shutdown();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            if (rig != null) rig.setPower(true);
            var trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(),
                ("FAIL\n" + diagnostics() + "\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable reportFailure) {
            reportFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static boolean empty(IInventory inventory) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (inventory.getStackInSlot(slot) != null) return false;
        }
        return true;
    }

    private static File output() {
        File output = new File(System.getProperty("gtng.circuitAssembly.output"));
        output.mkdirs();
        return output;
    }

    private static void log(String message) {
        System.out.println("CIRCUIT_ASSEMBLY_E2E: " + message);
        try {
            Files.write(new File(output(), "events.log").toPath(), (message + "\n").getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("CIRCUIT_ASSEMBLY_E2E: " + message);
    }
}
