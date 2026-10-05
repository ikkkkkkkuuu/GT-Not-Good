package com.xyp.gtnotgood.common.gui.modularui.widget;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.IntegratedProductionFactoryGui;
import com.xyp.gtnotgood.common.machines.multiblock.IntegratedProductionFactory;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.machine.factory.FactoryControllers;
import com.xyp.gtnotgood.utils.machine.factory.FactoryCycles;
import com.xyp.gtnotgood.utils.machine.factory.FactoryGraph;
import com.xyp.gtnotgood.utils.machine.factory.FactoryInputs;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPatternExport;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPatternRouting;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPresets;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPreview;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRecipeCatalog;
import com.xyp.gtnotgood.utils.machine.factory.FactoryReservations;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRouting;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRuntime;
import com.xyp.gtnotgood.utils.machine.factory.FactoryText;
import com.xyp.gtnotgood.utils.machine.factory.FactoryWholeBatch;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.Materials;
import gregtech.api.objects.GTDualInputPattern;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;

/** Runs after real recipe registration and renders the production editor twice with four locked routes. */
@Mod(modid = "factoryqa", name = "Factory QA", version = "1", dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class FactoryClientChecks {

    private int stage, ticks;
    private boolean started;
    private ModularPanel panel;
    private String recipeId;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
        if (Boolean.getBoolean("gtng.factory.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (stage == 0) {
            if (!started && mc.currentScreen instanceof GuiMainMenu) {
                started = true;
                mc.launchIntegratedServer(
                    "factory-qa-" + System.currentTimeMillis(),
                    "Factory QA",
                    new WorldSettings(83171L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (mc.theWorld == null || mc.thePlayer == null || ++ticks < 100) return;
            if (Boolean.getBoolean("gtng.factory.qa.platinum")) {
                checkPlatinumExecution();
                openEditor(true);
                stage = 5;
                ticks = 0;
                return;
            }
            checkRecipes();
            checkPresets();
            checkPresetPageIsolation();
            checkReservationUpdates();
            checkIndustrialControllers();
            checkPatternRouting();
            checkNativePatternInventory();
            openEditor();
            stage = 1;
            ticks = 0;
        } else if (++ticks == 40) {
            if (stage <= 2) {
                ScrollWidget<?> list = (ScrollWidget<?>) panel.getChildren()
                    .stream()
                    .filter(w -> w instanceof ScrollWidget)
                    .findFirst()
                    .get();
                require(list.getScrollY() == 0, "four routes stay within viewport");
                require(
                    list.getChildren()
                        .stream()
                        .filter(w -> w.isEnabled())
                        .count() == 4,
                    "four enabled rows");
            }
            File output = new File(System.getProperty("gtng.factory.qa.output"));
            output.mkdirs();
            ScreenShotHelper.saveScreenshot(
                output,
                "factory-open-" + stage + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (stage == 1) {
                ClientGUI.open(new GuiMainMenu());
                stage = 2;
                ticks = 0;
            } else if (stage == 2) {
                openPresets();
                stage = 3;
                ticks = 0;
            } else if (stage == 3) {
                ScrollWidget<?> list = (ScrollWidget<?>) panel.getChildren()
                    .stream()
                    .filter(w -> w instanceof ScrollWidget)
                    .findFirst()
                    .get();
                require(list.getScrollY() == 0, "preset menu opens at the top");
                require(
                    list.getChildren()
                        .size() == FactoryPresets.Preset.values().length,
                    "all presets have rows");
                list.getScrollArea()
                    .getScrollY()
                    .scrollTo(list.getScrollArea(), Integer.MAX_VALUE);
                stage = 4;
                ticks = 0;
            } else if (stage == 4) {
                ScrollWidget<?> list = (ScrollWidget<?>) panel.getChildren()
                    .stream()
                    .filter(w -> w instanceof ScrollWidget)
                    .findFirst()
                    .get();
                require(list.getScrollY() > 0, "last preset is reachable by scrolling");
                openEditor(true);
                stage = 5;
                ticks = 0;
            } else if (stage == 7) {
                openWholePreview();
                stage = 8;
                ticks = 0;
            } else if (stage == 8) {
                Files.write(new File(output, "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                mc.shutdown();
            } else {
                ScrollWidget<?> list = (ScrollWidget<?>) panel.getChildren()
                    .stream()
                    .filter(w -> w instanceof ScrollWidget)
                    .findFirst()
                    .get();
                require(
                    list.getChildren()
                        .stream()
                        .filter(w -> w.isEnabled())
                        .count()
                        == FactoryPresets.resolve(FactoryPresets.Preset.PlatinumGroup)
                            .size(),
                    "all platinum rows visible");
                require(list.getScrollY() == 0, "large page reopens at top");
                list.getScrollArea()
                    .getScrollY()
                    .scrollTo(list.getScrollArea(), Integer.MAX_VALUE);
                require(list.getScrollY() > 0, "large page reaches final rows");
                if (stage == 5) {
                    openEditor(true);
                    stage = 6;
                    ticks = 0;
                    return;
                }
                if (Boolean.getBoolean("gtng.factory.qa.platinum")) {
                    openWholeDetails();
                    stage = 7;
                    ticks = 0;
                    return;
                }
                Files.write(new File(output, "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                mc.shutdown();
            }
        } else if (stage == 2 && ticks == 5) {
            openEditor();
        }
    }

    private void checkPlatinumExecution() throws Exception {
        File output = new File(System.getProperty("gtng.factory.qa.output"));
        output.mkdirs();
        StringBuilder report = new StringBuilder();
        FactoryPresets.Result preset = FactoryPresets.resolve(FactoryPresets.Preset.PlatinumGroup);
        report.append("Preset available: ")
            .append(preset.available())
            .append(' ')
            .append(preset.problem)
            .append('\n');
        require(preset.available(), "platinum preset resolves");
        FactoryGraph graph = preset.copy();
        report.append(FactoryRouting.encode(graph))
            .append('\n');
        int[] reported = { 6187500, 962500, 13200000, 4180000, 41250, 40140, 1237500, 5280000, 68750, 550000, 30000,
            57000, 85500, 171000, 7695, 42750, 384750, 427500, 338580, 615600, 305280, 128250, 25650, 256500, 256500,
            256500, 92340, 12825, 297000, 29700, 297000, 89100, 178200, 4752, 356400, 169290, 169290, 169290 };
        boolean match = graph.nodes.size() == reported.length;
        for (int i = 0; i < graph.nodes.size() && i < reported.length; i++)
            match &= graph.nodes.get(i).parallel == reported[i];
        report.append("Reported parallel vector matches preset: ")
            .append(match)
            .append('\n');
        try {
            int period = FactoryRuntime.batchPeriod(graph.nodes);
            report.append("Common period: ")
                .append(period)
                .append(" ticks\n");
            for (FactoryGraph.Node node : graph.nodes) {
                GTRecipe recipe = FactoryRecipeCatalog.get(node.recipe).recipe;
                long repetitions = (long) node.parallel * (period / Math.max(1, recipe.mDuration));
                report.append("Node ")
                    .append(node.id)
                    .append(" parallel=")
                    .append(node.parallel)
                    .append(" duration=")
                    .append(recipe.mDuration)
                    .append(" repetitions=")
                    .append(repetitions)
                    .append('\n');
                for (FluidStack fluid : recipe.mFluidInputs)
                    if ((long) fluid.amount * repetitions > Integer.MAX_VALUE) report.append("OVERFLOW input ")
                        .append(
                            fluid.getFluid()
                                .getName())
                        .append(' ')
                        .append(fluid.amount)
                        .append(" * ")
                        .append(repetitions)
                        .append(" = ")
                        .append((long) fluid.amount * repetitions)
                        .append('\n');
                for (FluidStack fluid : recipe.mFluidOutputs)
                    if ((long) fluid.amount * repetitions > Integer.MAX_VALUE) report.append("OVERFLOW output ")
                        .append(
                            fluid.getFluid()
                                .getName())
                        .append(' ')
                        .append(fluid.amount)
                        .append(" * ")
                        .append(repetitions)
                        .append(" = ")
                        .append((long) fluid.amount * repetitions)
                        .append('\n');
            }
            FactoryCycles.Plan plan = FactoryCycles.prepare(graph.nodes, 1, new Random(1));
            report.append("Runtime batch preparation: PASS, EU/t=")
                .append(plan.eut)
                .append('\n');
        } catch (ArithmeticException failure) {
            report.append("Runtime batch preparation: FAIL\n");
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            report.append(trace);
        }
        try {
            FactoryWholeBatchChecks.check(graph, report);
        } finally {
            Files.write(
                new File(output, "platinum-execution.txt").toPath(),
                report.toString()
                    .getBytes(StandardCharsets.UTF_8));
        }
        Files.write(new File(output, "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
    }

    private void checkPresets() throws Exception {
        StringBuilder report = new StringBuilder();
        boolean valid = true;
        for (FactoryPresets.Preset preset : FactoryPresets.Preset.values()) {
            FactoryPresets.Result result = FactoryPresets.resolve(preset);
            report.append(preset)
                .append(": ")
                .append(result.size())
                .append(" ")
                .append(result.problem)
                .append('\n');
            valid &= result.available();
            if (!result.available()) continue;
            for (var node : result.copy().nodes) {
                var r = FactoryRecipeCatalog.get(node.recipe).recipe;
                report.append("node ")
                    .append(node.id)
                    .append(" x")
                    .append(node.parallel)
                    .append(" ticks ")
                    .append(r.mDuration)
                    .append('\n');
            }
            var snapshot = FactoryPreview.describe(result.copy());
            if (preset == FactoryPresets.Preset.Netherite) {
                FactoryGraph whole = result.copy();
                require(whole.nodes.size() == 11, "netherite fits one page");
                require(
                    FactoryCycles.groups(whole)
                        .get(whole.nodes.get(0).id)
                        .size() == 11,
                    "netherite settles as one batch");
                require(
                    FactoryRouting.decode(FactoryRouting.encode(whole)).nodes.stream()
                        .allMatch(n -> n.wholeLineBatch),
                    "route export preserves page batch");
                require(
                    snapshot.outputs.stream()
                        .anyMatch(
                            i -> i.fluid != null && i.rate > 0
                                && i.fluid.isFluidEqual(Materials.NefariousGas.getFluid(1))
                                && !i.internal),
                    "surplus nefarious gas is exportable");
                for (FactoryGraph.Node node : whole.nodes) node.wholeLineBatch = false;
                require(
                    FactoryPreview.describe(whole)
                        .exportIssue() == FactoryText.PATTERN_INTERNAL,
                    "manual line retains existing surplus rule");
                FactoryGraph otherPage = result.copy();
                int offset = whole.nodes.size();
                for (FactoryGraph.Node node : whole.nodes) node.wholeLineBatch = true;
                for (FactoryGraph.Node node : otherPage.nodes) {
                    node.page = 2;
                    node.id += offset;
                    var sources = new HashSet<>(node.sources);
                    node.sources.clear();
                    for (int source : sources) node.sources.add(source + offset);
                    whole.nodes.add(node);
                }
                var groups = FactoryCycles.groups(whole);
                require(
                    groups.get(0)
                        .size() == 11
                        && groups.get(offset)
                            .size() == 11
                        && groups.get(0) != groups.get(offset),
                    "whole batches cannot cross pages");
            } else if (preset != FactoryPresets.Preset.PlatinumGroup) {
                require(
                    result.copy().nodes.stream()
                        .noneMatch(n -> n.wholeLineBatch),
                    "existing presets remain unchanged");
            }
            if (preset == FactoryPresets.Preset.PlatinumGroup) {
                FactoryGraph whole = result.copy();
                require(
                    whole.nodes.size() > 32 && whole.nodes.size() <= FactoryGraph.MAX_NODES,
                    "platinum exceeds old limit");
                require(whole.copy().nodes.size() == whole.nodes.size(), "large page survives NBT");
                require(
                    FactoryRouting.decode(FactoryRouting.encode(whole)).nodes.size() == whole.nodes.size(),
                    "large page route round trip");
                require(
                    whole.nodes.stream()
                        .map(n -> n.recipe)
                        .distinct()
                        .count() == whole.nodes.size(),
                    "recovery recipes deduplicated");
                require(
                    FactoryCycles.groups(whole)
                        .get(whole.nodes.get(0).id)
                        .size() == whole.nodes.size(),
                    "platinum settles together");
                FactoryGraph full = whole.copy();
                while (full.nodes.size() < FactoryGraph.MAX_NODES) full.add(whole.nodes.get(0).recipe);
                full.add(whole.nodes.get(0).recipe);
                require(full.nodes.size() == FactoryGraph.MAX_NODES, "node cap is enforced");
                for (FactoryGraph.Node node : full.nodes) {
                    node.parallel = FactoryGraph.MAX_PARALLEL;
                    node.customEUt = Long.MAX_VALUE;
                }
                require(full.copy().nodes.size() == FactoryGraph.MAX_NODES, "full page persists");
                require(
                    FactoryRouting.decode(FactoryRouting.encode(full)).nodes.size() == FactoryGraph.MAX_NODES,
                    "full page with long counts imports without truncation");
            }
            if (preset == FactoryPresets.Preset.EpoxyPropene) {
                require(result.size() == 5, "epoxy recovery stays on one page");
                require(
                    snapshot.inputs.stream()
                        .noneMatch(
                            i -> (i.item != null && (i.item.isItemEqual(Materials.Sodium.getDust(1))
                                || i.item.isItemEqual(Materials.SodiumHydroxide.getDust(1))))
                                || (i.fluid != null && i.fluid.isFluidEqual(Materials.HydrochloricAcid.getFluid(1)))),
                    "epoxy recovers sodium and acid");
                require(
                    snapshot.inputs.stream()
                        .anyMatch(
                            i -> i.fluid != null && i.rate > 0 && i.fluid.isFluidEqual(Materials.Chlorine.getGas(1))),
                    "epoxy needs chlorine makeup");
                require(
                    snapshot.outputs.stream()
                        .anyMatch(
                            i -> i.fluid != null && i.rate > 0 && i.fluid.isFluidEqual(Materials.Epoxid.getMolten(1))),
                    "epoxy exports molten resin");
            }
            if (preset == FactoryPresets.Preset.Polybenzimidazole) {
                require(result.size() == 11, "complete PBI chain stays on one page");
                require(
                    snapshot.inputs.stream()
                        .noneMatch(
                            i -> i.fluid != null && (i.fluid.isFluidEqual(Materials.Phenol.getFluid(1))
                                || i.fluid.isFluidEqual(Materials.Chlorine.getGas(1)))),
                    "PBI recycles all phenol and chlorine");
                require(
                    snapshot.inputs.stream()
                        .anyMatch(
                            i -> i.fluid != null && i.rate > 0
                                && i.fluid.isFluidEqual(Materials.SulfuricAcid.getFluid(1))),
                    "PBI still needs real sulfuric acid makeup");
                require(
                    snapshot.outputs.stream()
                        .anyMatch(
                            i -> i.fluid != null && i.rate > 0
                                && i.fluid.isFluidEqual(Materials.Polybenzimidazole.getMolten(1))),
                    "PBI exports molten polymer");
            }
            report.append("export: ")
                .append(snapshot.exportIssue())
                .append(" period: ")
                .append(snapshot.batchTicks)
                .append('\n');
            for (var output : snapshot.outputs) if (output.internal) report.append("internal: ")
                .append(output.name())
                .append(" ")
                .append(output.rate)
                .append('\n');
            try {
                ItemStack encoded = FactoryPatternExport.create(result.copy());
                require(encoded != null, "preset export");
                var details = ((ICraftingPatternItem) encoded.getItem())
                    .getPatternForItem(encoded, Minecraft.getMinecraft().theWorld);
                require(details != null, "exported preset decodes");
                GTDualInputPattern patternInputs = nativeInputs(details.getAEInputs());
                var binding = new FactoryPatternRouting(result.copy()).bind(patternInputs);
                require(binding.failure == null && binding.nodes.size() == result.size(), "export binds whole preset");
                FactoryGraph exported = result.copy();
                var plan = exported.nodes.stream()
                    .allMatch(node -> node.wholeLineBatch) ? FactoryWholeBatch.prepare(exported.nodes)
                        : FactoryCycles.prepare(exported.nodes, 1, new Random(1));
                require(
                    FactoryPatternRouting.matches(
                        new GTDualInputPattern(plan.inputs.mInputs, plan.inputs.mFluidInputs),
                        patternInputs,
                        false),
                    "export input matches exact runtime period");
                List<ItemStack> items = new ArrayList<>();
                List<FluidStack> fluids = new ArrayList<>();
                for (var job : plan.jobs.values()) {
                    items.addAll(job.pendingItems);
                    fluids.addAll(job.pendingFluids);
                }
                require(
                    FactoryPatternRouting.matches(
                        new GTDualInputPattern(items.toArray(new ItemStack[0]), fluids.toArray(new FluidStack[0])),
                        nativeInputs(details.getAEOutputs()),
                        false),
                    "export output matches deterministic runtime settlement");
            } catch (RuntimeException failure) {
                report.append(failure.toString())
                    .append('\n');
                valid = false;
            }
            FactoryGraph draft = new FactoryGraph();
            require(!FactoryPresets.apply(draft, true, preset.name()), "locked preset insertion rejected");
            require(FactoryPresets.apply(draft, false, preset.name()), "empty draft accepts preset");
            String before = draft.write()
                .toString();
            require(!FactoryPresets.apply(draft, false, preset.name()), "nonempty draft preserved");
            require(
                before.equals(
                    draft.write()
                        .toString()),
                "failed insertion is atomic");
            require(
                draft.nodes.size() <= FactoryGraph.MAX_NODES && draft.hasTargetsForAllComponents(),
                "normal graph limits");
            for (FactoryGraph.Node node : draft.nodes) {
                require(node.customEUt == -1 && node.overclocks == 0 && node.parallel > 0, "preset preserves costs");
                require(FactoryRecipeCatalog.get(node.recipe) != null, "preset only references registered recipes");
                GTRecipe recipe = FactoryRecipeCatalog.get(node.recipe).recipe;
                int batch = FactoryRuntime.outputBatch(recipe);
                if (batch > 1) {
                    boolean rejected = false;
                    try {
                        FactoryRuntime.prepare(recipe, batch - 1, 0, new Random(1));
                    } catch (ArithmeticException incomplete) {
                        rejected = true;
                    }
                    require(rejected, "fractional batch is rejected, never rounded or randomly completed");
                }
                var first = FactoryRuntime.prepare(recipe, batch, 0, new Random(1));
                var second = FactoryRuntime.prepare(recipe, batch, 0, new Random(999));
                require(
                    FactoryRecipeCatalog.items(first.pendingItems.toArray(new ItemStack[0]))
                        .equals(FactoryRecipeCatalog.items(second.pendingItems.toArray(new ItemStack[0]))),
                    "deterministic outputs");
            }
            for (var cycle : new java.util.HashSet<>(
                FactoryCycles.groups(draft)
                    .values())) {
                var plan = FactoryCycles.prepare(cycle, 1, new Random(1));
                require(
                    plan.eut > 0 && plan.jobs.size() == cycle.size(),
                    "preset cycle has a valid bounded execution plan");
            }
            draft.nodes.clear();
            require(result.copy().nodes.size() == result.size(), "templates are immutable");
        }
        File output = new File(System.getProperty("gtng.factory.qa.output"));
        output.mkdirs();
        Files.write(
            new File(output, "presets.txt").toPath(),
            report.toString()
                .getBytes(StandardCharsets.UTF_8));
        require(valid, report.toString());
    }

    private void checkIndustrialControllers() throws Exception {
        var ordinary = Arrays.asList(
            RecipeMaps.electrolyzerRecipes,
            RecipeMaps.centrifugeRecipes,
            RecipeMaps.mixerRecipes,
            RecipeMaps.chemicalDehydratorRecipes,
            RecipeMaps.chemicalReactorRecipes);
        var industrial = Arrays.asList(
            RecipeMaps.electrolyzerNonCellRecipes,
            RecipeMaps.centrifugeNonCellRecipes,
            RecipeMaps.mixerNonCellRecipes,
            RecipeMaps.chemicalDehydratorNonCellRecipes,
            RecipeMaps.multiblockChemicalReactorRecipes);
        for (int i = 0; i < ordinary.size(); i++) {
            String oldKey = ordinary.get(i).unlocalizedName, key = industrial.get(i).unlocalizedName;
            ItemStack controller = FactoryControllers.representative(key);
            require(
                controller != null && FactoryControllers.supports(oldKey, controller)
                    && FactoryControllers.supports(key, controller),
                "industrial controller accepts both recipe maps: " + oldKey);
            require(!FactoryControllers.supports(key, new ItemStack(Items.iron_ingot)), "non-controller rejected");
            FactoryRecipeCatalog.Entry first = null, second = null;
            for (GTRecipe recipe : ordinary.get(i)
                .getAllRecipes()) {
                first = FactoryRecipeCatalog.find(ordinary.get(i), recipe);
                if (first != null) break;
            }
            for (GTRecipe recipe : industrial.get(i)
                .getAllRecipes()) {
                second = FactoryRecipeCatalog.find(industrial.get(i), recipe);
                if (second != null) break;
            }
            require(first != null && second != null, "real recipes available");
            FactoryReservations held = new FactoryReservations();
            List<ItemStack> supply = new ArrayList<>();
            supply.add(controller.copy());
            held.collect(0, first, supply, stack -> FactoryControllers.supports(oldKey, stack));
            require(supply.get(0).stackSize == 0 && held.hasHost(key) && held.hasHost(oldKey), "one controller shared");
            ItemStack extra = controller.copy();
            held.collect(1, second, Arrays.asList(extra), stack -> FactoryControllers.supports(key, stack));
            require(extra.stackSize == 1, "second recipe map does not consume another controller");
            FactoryGraph graph = new FactoryGraph();
            graph.add(first.id);
            graph.add(second.id);
            IntegratedProductionFactory machine = new IntegratedProductionFactory("factory.qa.controllers");
            Field installed = IntegratedProductionFactory.class.getDeclaredField("installed");
            installed.setAccessible(true);
            installed.set(machine, graph);
            long rows = machine.getRequirementTags()
                .stream()
                .filter(
                    row -> row.getCompoundTag("key")
                        .hasKey("map"))
                .count();
            require(rows == 1, "ordinary and industrial requirements merge into one row");
            NBTTagCompound saved = held.write();
            NBTTagList hosts = saved.getTagList("hosts", 10);
            hosts.getCompoundTagAt(0)
                .setString("key", oldKey);
            FactoryReservations migrated = new FactoryReservations();
            migrated.read(saved);
            require(
                migrated.hasHost(key) && migrated.refundUnused(graph, stack -> false),
                "old deposit migrated without refund");
            NBTTagCompound duplicate = (NBTTagCompound) hosts.getCompoundTagAt(0)
                .copy();
            duplicate.setString("key", key);
            hosts.appendTag(duplicate);
            migrated.read(saved);
            List<ItemStack> refunds = new ArrayList<>();
            require(
                migrated.remaining()
                    .size() == 2,
                "legacy duplicate preserved");
            require(migrated.refundUnused(graph, stack -> {
                refunds.add(stack);
                return true;
            }) && refunds.size() == 1
                && migrated.remaining()
                    .size() == 1
                && migrated.hasHost(oldKey), "only redundant controller returned");
        }
        require(
            FactoryControllers.representative(RecipeMaps.sifterRecipes.unlocalizedName) != null,
            "sifter displays a real multiblock controller");
        for (FactoryPresets.Preset preset : FactoryPresets.Preset.values())
            for (FactoryGraph.Node node : FactoryPresets.resolve(preset)
                .copy().nodes) {
                    var entry = FactoryRecipeCatalog.get(node.recipe);
                    ItemStack icon = FactoryControllers.representative(entry.map.unlocalizedName);
                    require(
                        icon != null && IntegratedProductionFactory.supportsHost(entry, icon),
                        "preset machine icon and acceptance agree: " + preset + " / " + entry.map.unlocalizedName);
                }
    }

    private void checkReservationUpdates() {
        FactoryGraph expanded = FactoryPresets.resolve(FactoryPresets.Preset.RocketRp1)
            .copy();
        FactoryGraph old = new FactoryGraph();
        old.add(expanded.nodes.get(0).recipe);
        FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(old.nodes.get(0).recipe);
        FactoryReservations held = new FactoryReservations();
        List<ItemStack> supplied = new ArrayList<>();
        ItemStack machine = new ItemStack(Items.iron_ingot);
        machine.setTagCompound(new NBTTagCompound());
        machine.getTagCompound()
            .setString("depositOwner", "old page");
        supplied.add(machine);
        for (ItemStack input : entry.recipe.mInputs) if (input != null && input.stackSize == 0) {
            ItemStack copy = input.copy();
            copy.stackSize = 1;
            supplied.add(copy);
        }
        require(
            held.collect(0, entry, supplied, stack -> stack.getItem() == Items.iron_ingot) == null,
            "original page deposits collected");
        NBTTagCompound before = held.write();
        List<ItemStack> refunded = new ArrayList<>();
        require(held.refundUnused(expanded, item -> {
            refunded.add(item);
            return true;
        }), "append retains deposits");
        require(
            refunded.isEmpty() && before.equals(held.write()),
            "adding pages never ejects previous machines or circuits");
        require(
            held.remainingUnused(expanded)
                .isEmpty(),
            "retained items hidden from refund list");
        require(
            held.collect(99, entry, new ArrayList<>(), stack -> false) == null,
            "renumbered existing node needs no replacement machine");
        FactoryRecipeCatalog.Entry added = FactoryRecipeCatalog.get(expanded.nodes.get(2).recipe);
        require(
            held.collect(2, added, new ArrayList<>(), stack -> false) == FactoryText.HOST,
            "new map still requires its own machine");
        FactoryReservations restored = new FactoryReservations();
        restored.read(held.write());
        require(
            restored.refundUnused(expanded, item -> false) && before.equals(restored.write()),
            "saved deposits survive page update with blocked output");
        require(restored.refundUnused(old, item -> false), "removing a page preserves shared machines");
        require(
            !restored.refundUnused(new FactoryGraph(), item -> false) && before.equals(restored.write()),
            "blocked refund never deletes deposits");
        require(restored.refundUnused(new FactoryGraph(), item -> {
            refunded.add(item);
            return true;
        }), "clearing last page refunds deposits");
        require(
            restored.remaining()
                .isEmpty()
                && refunded.stream()
                    .anyMatch(
                        item -> item.hasTagCompound() && "old page".equals(
                            item.getTagCompound()
                                .getString("depositOwner"))),
            "actual tagged machine returned");
        int count = refunded.size();
        require(restored.refundUnused(new FactoryGraph(), item -> {
            refunded.add(item);
            return true;
        }) && refunded.size() == count, "refund cannot duplicate items");
    }

    private void checkPresetPageIsolation() {
        FactoryGraph installed = new FactoryGraph();
        int page = 0;
        for (FactoryPresets.Preset preset : FactoryPresets.Preset.values()) {
            FactoryGraph graph = FactoryPresets.resolve(preset)
                .copy();
            int offset = installed.nodes.size();
            page++;
            for (FactoryGraph.Node node : graph.nodes) {
                node.page = page;
                node.id += offset;
                var sources = new HashSet<>(node.sources);
                node.sources.clear();
                for (int source : sources) node.sources.add(source + offset);
                installed.nodes.add(node);
            }
        }
        FactoryPatternRouting routing = new FactoryPatternRouting(installed);
        page = 0;
        for (FactoryPresets.Preset preset : FactoryPresets.Preset.values()) {
            ItemStack encoded = FactoryPatternExport.create(
                FactoryPresets.resolve(preset)
                    .copy());
            var details = ((ICraftingPatternItem) encoded.getItem())
                .getPatternForItem(encoded, Minecraft.getMinecraft().theWorld);
            var binding = routing.bind(nativeInputs(details.getAEInputs()));
            final int expectedPage = ++page;
            require(
                binding.failure == null && binding.nodes.size() == FactoryPresets.resolve(preset)
                    .size()
                    && binding.nodes.stream()
                        .allMatch(id -> installed.find(id).page == expectedPage),
                "whole-line patterns stay isolated with all presets installed: " + preset);
        }
    }

    private static GTDualInputPattern nativeInputs(IAEStack<?>[] stacks) {
        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (IAEStack<?> stack : stacks) {
            if (stack instanceof IAEItemStack item) items.add(item.getItemStack());
            if (stack instanceof IAEFluidStack fluid) fluids.add(fluid.getFluidStack());
        }
        return new GTDualInputPattern(items.toArray(new ItemStack[0]), fluids.toArray(new FluidStack[0]));
    }

    private void checkPatternRouting() {
        GTDualInputPattern expected = new GTDualInputPattern(
            new ItemStack[] { new ItemStack(Items.iron_ingot, 2) },
            new FluidStack[] { new FluidStack(FluidRegistry.WATER, 1000) });
        GTDualInputPattern actual = new GTDualInputPattern(
            new ItemStack[] { new ItemStack(Items.iron_ingot, 3), new ItemStack(Items.iron_ingot, 1) },
            new FluidStack[] { new FluidStack(FluidRegistry.WATER, 2000) });
        require(FactoryPatternRouting.matches(expected, actual, false), "split stacks and whole pattern multipliers");
        actual.inputFluid[0].amount = 1000;
        require(!FactoryPatternRouting.matches(expected, actual, false), "wrong mixed item/fluid ratio rejected");
        actual.inputFluid[0] = new FluidStack(FluidRegistry.LAVA, 2000);
        require(!FactoryPatternRouting.matches(expected, actual, false), "wrong fluid rejected");
        actual.inputFluid = new FluidStack[0];
        require(!FactoryPatternRouting.matches(expected, actual, false), "missing fluid rejected");
        actual.inputItems = new ItemStack[] { new ItemStack(Items.iron_ingot, 2), new ItemStack(Items.gold_ingot) };
        actual.inputFluid = expected.inputFluid;
        require(!FactoryPatternRouting.matches(expected, actual, false), "extra item cannot route to subset recipe");
        FactoryGraph graph = FactoryPresets.resolve(FactoryPresets.Preset.Osmium)
            .copy();
        FactoryGraph.Node first = graph.nodes.get(0);
        GTRecipe recipe = FactoryRecipeCatalog.get(first.recipe).consumableRecipe;
        GTDualInputPattern pattern = new GTDualInputPattern(recipe.mInputs, recipe.mFluidInputs);
        require(
            new FactoryPatternRouting(graph).bind(pattern).nodes.contains(first.id),
            "unique process accepts pattern");
        FactoryGraph.Node duplicate = new FactoryGraph.Node();
        duplicate.id = 100;
        duplicate.page = 2;
        duplicate.recipe = first.recipe;
        graph.nodes.add(duplicate);
        FactoryPatternRouting.Binding ambiguous = new FactoryPatternRouting(graph).bind(pattern);
        require(
            ambiguous.nodes.isEmpty() && ambiguous.failure == FactoryText.PatternAmbiguous,
            "identical page inputs fail closed");
        duplicate.page = first.page;
        ambiguous = new FactoryPatternRouting(graph).bind(pattern);
        require(ambiguous.nodes.isEmpty(), "same-page duplicate input signatures also fail closed");
        ItemStack tagged = new ItemStack(Items.iron_ingot, 2);
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound()
            .setString("identity", "original");
        GTDualInputPattern taggedExpected = new GTDualInputPattern(new ItemStack[] { tagged }, new FluidStack[0]);
        ItemStack other = tagged.copy();
        other.getTagCompound()
            .setString("identity", "other");
        require(
            !FactoryPatternRouting
                .matches(taggedExpected, new GTDualInputPattern(new ItemStack[] { other }, new FluidStack[0]), true),
            "NBT-sensitive item signature preserved");
        FluidStack taggedFluid = new FluidStack(FluidRegistry.WATER, 1000);
        taggedFluid.tag = new NBTTagCompound();
        taggedFluid.tag.setInteger("batch", 1);
        require(
            !FactoryPatternRouting.matches(
                new GTDualInputPattern(new ItemStack[0], new FluidStack[] { taggedFluid }),
                new GTDualInputPattern(
                    new ItemStack[0],
                    new FluidStack[] { new FluidStack(FluidRegistry.WATER, 1000) }),
                false),
            "fluid NBT preserved");
    }

    private void checkNativePatternInventory() {
        ItemStack encoded = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        IAEStack<?>[] encodedInputs = { AEItemStack.create(new ItemStack(Items.iron_ingot, 2)),
            AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1000)) };
        for (IAEStack<?> stack : encodedInputs) {
            NBTTagCompound data = new NBTTagCompound();
            stack.writeToNBTGeneric(data);
            in.appendTag(data);
        }
        NBTTagCompound output = new NBTTagCompound();
        AEItemStack.create(new ItemStack(Items.gold_ingot))
            .writeToNBTGeneric(output);
        out.appendTag(output);
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        encoded.setTagCompound(tag);
        var details = ((ICraftingPatternItem) encoded.getItem())
            .getPatternForItem(encoded, Minecraft.getMinecraft().theWorld);
        require(details != null, "native ultimate pattern decodes");
        MEInventoryCrafting table = new MEInventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 4, 4);
        int slot = 0;
        List<ItemStack> itemInputs = new ArrayList<>();
        List<FluidStack> fluidInputs = new ArrayList<>();
        for (IAEStack<?> stack : details.getAEInputs()) {
            if (stack == null) continue;
            table.setInventorySlotContents(slot, stack.copy());
            IAEStack<?> nativeStack = table.getAEStackInSlot(slot++);
            if (nativeStack instanceof IAEItemStack item) itemInputs.add(item.getItemStack());
            if (nativeStack instanceof IAEFluidStack fluid) fluidInputs.add(fluid.getFluidStack());
        }
        require(fluidInputs.size() == 1 && fluidInputs.get(0).amount == 1000, "native fluids are not item packets");
        ItemStack shared = new ItemStack(Items.diamond);
        GTDualInputPattern signature = new GTDualInputPattern(
            new ItemStack[] { shared, itemInputs.get(0)
                .copy() },
            new FluidStack[] { fluidInputs.get(0)
                .copy() });
        IDualInputInventoryWithPattern inventory = (IDualInputInventoryWithPattern) Proxy.newProxyInstance(
            IDualInputInventoryWithPattern.class.getClassLoader(),
            new Class<?>[] { IDualInputInventoryWithPattern.class },
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getPatternInputs":
                        return signature;
                    case "getItemInputs":
                        return itemInputs.toArray(new ItemStack[0]);
                    case "getFluidInputs":
                        return fluidInputs.toArray(new FluidStack[0]);
                    case "isEmpty":
                        return false;
                    case "shouldBeCached":
                        return true;
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
        FactoryInputs view = new FactoryInputs(Collections.singletonList(shared), inventory);
        require(
            view.items.size() == 1 && view.pattern.inputItems.length == 1,
            "shared deposits excluded from signature and consumption");
        GTDualInputPattern expected = new GTDualInputPattern(
            new ItemStack[] { new ItemStack(Items.iron_ingot, 2) },
            new FluidStack[] { new FluidStack(FluidRegistry.WATER, 1000) });
        require(
            FactoryPatternRouting.matches(expected, view.pattern, false),
            "native item and fluid signature matches");
        view.items.get(0).stackSize--;
        view.fluids.get(0).amount -= 500;
        require(
            itemInputs.get(0).stackSize == 1 && fluidInputs.get(0).amount == 500,
            "live buffers are debited by reference");
        FactoryInputs partial = new FactoryInputs(Collections.singletonList(shared), inventory);
        require(
            FactoryPatternRouting.matches(expected, partial.pattern, false),
            "partially consumed buffer keeps original pattern identity");
        require(
            shared.stackSize == 1 && signature.inputItems[1].stackSize == 2,
            "matching never mutates shared deposits or pattern");
    }

    private void openPresets() throws Exception {
        IntegratedProductionFactoryGui gui = new IntegratedProductionFactoryGui(
            new IntegratedProductionFactory("factory.qa"));
        Method create = IntegratedProductionFactoryGui.class.getDeclaredMethod("createPresets");
        create.setAccessible(true);
        panel = (ModularPanel) create.invoke(gui);
        ClientGUI.open(new ModularScreen(ModList.GTNotGood.getID(), panel));
    }

    private void checkRecipes() {
        GTRecipe recipe = new GTRecipe(false, null, null, null, null, null, null, null, null, null, 20, 30, 7);
        require(
            FactoryRecipeCatalog.unsupportedReason(RecipeMaps.chemicalPlantRecipes, recipe) == null,
            "waive chemical tier");
        require(
            FactoryRecipeCatalog.unsupportedReason(RecipeMaps.assemblerRecipes, recipe) == FactoryText.IMPORT_SPECIAL,
            "other maps protected");
        recipe.mSpecialValue = -200;
        require(
            FactoryRecipeCatalog.unsupportedReason(RecipeMaps.chemicalPlantRecipes, recipe)
                == FactoryText.IMPORT_SPECIAL,
            "environment protected");
        recipe.mSpecialValue = 7;
        recipe.mSpecialItems = new Object();
        require(
            FactoryRecipeCatalog.unsupportedReason(RecipeMaps.chemicalPlantRecipes, recipe)
                == FactoryText.IMPORT_SPECIAL,
            "special item protected");
        for (GTRecipe registered : RecipeMaps.chemicalPlantRecipes.getAllRecipes()) {
            if (registered.mSpecialValue <= 0
                || FactoryRecipeCatalog.unsupportedReason(RecipeMaps.chemicalPlantRecipes, registered) != null)
                continue;
            FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.find(RecipeMaps.chemicalPlantRecipes, registered);
            require(entry != null, "registered chemical recipe resolves");
            recipeId = entry.id;
            break;
        }
        require(recipeId != null, "at least one tiered chemical recipe imports");
    }

    private void openEditor() throws Exception {
        openEditor(false);
    }

    private void openWholeDetails() throws Exception {
        IntegratedProductionFactoryGui gui = new IntegratedProductionFactoryGui(
            new IntegratedProductionFactory("factory.qa.details"));
        Field graphField = IntegratedProductionFactoryGui.class.getDeclaredField("visibleGraph");
        graphField.setAccessible(true);
        FactoryGraph graph = (FactoryGraph) graphField.get(gui);
        graph.read(
            FactoryPresets.resolve(FactoryPresets.Preset.PlatinumGroup)
                .copy()
                .write());
        Field selected = IntegratedProductionFactoryGui.class.getDeclaredField("selected");
        selected.setAccessible(true);
        selected.setInt(gui, graph.nodes.get(0).id);
        Method create = IntegratedProductionFactoryGui.class.getDeclaredMethod("createDetails");
        create.setAccessible(true);
        panel = (ModularPanel) create.invoke(gui);
        ClientGUI.open(new ModularScreen(ModList.GTNotGood.getID(), panel));
    }

    private void openWholePreview() throws Exception {
        IntegratedProductionFactoryGui gui = new IntegratedProductionFactoryGui(
            new IntegratedProductionFactory("factory.qa.preview"));
        FactoryGraph graph = FactoryPresets.resolve(FactoryPresets.Preset.PlatinumGroup)
            .copy();
        Field snapshot = IntegratedProductionFactoryGui.class.getDeclaredField("previewSnapshot");
        snapshot.setAccessible(true);
        snapshot.set(gui, FactoryPreview.describe(graph));
        Method create = IntegratedProductionFactoryGui.class.getDeclaredMethod("createPreview");
        create.setAccessible(true);
        panel = (ModularPanel) create.invoke(gui);
        ClientGUI.open(new ModularScreen(ModList.GTNotGood.getID(), panel));
    }

    private void openEditor(boolean platinum) throws Exception {
        IntegratedProductionFactoryGui gui = new IntegratedProductionFactoryGui(
            new IntegratedProductionFactory("factory.qa"));
        Field graphField = IntegratedProductionFactoryGui.class.getDeclaredField("visibleGraph");
        graphField.setAccessible(true);
        FactoryGraph graph = (FactoryGraph) graphField.get(gui);
        if (platinum) graph.read(
            FactoryPresets.resolve(FactoryPresets.Preset.PlatinumGroup)
                .copy()
                .write());
        else for (int i = 0; i < 4; i++) graph.add(recipeId);
        Field lockedField = IntegratedProductionFactoryGui.class.getDeclaredField("locked");
        lockedField.setAccessible(true);
        lockedField.setBoolean(gui, true);
        PanelSyncManager manager = new PanelSyncManager(new ModularSyncManager(true), true);
        manager.syncValue("factoryEditorStatus", new StringSyncValue(() -> "", value -> {}));
        Method create = IntegratedProductionFactoryGui.class.getDeclaredMethod("createEditor", PanelSyncManager.class);
        create.setAccessible(true);
        panel = (ModularPanel) create.invoke(gui, manager);
        ClientGUI.open(new ModularScreen(ModList.GTNotGood.getID(), panel));
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
