package com.xyp.gtnotgood.common.gui.modularui.widget;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
import com.xyp.gtnotgood.utils.machine.factory.FactoryCycles;
import com.xyp.gtnotgood.utils.machine.factory.FactoryGraph;
import com.xyp.gtnotgood.utils.machine.factory.FactoryInputs;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPatternExport;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPatternRouting;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPresets;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPreview;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRecipeCatalog;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRouting;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRuntime;
import com.xyp.gtnotgood.utils.machine.factory.FactoryText;

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
            checkRecipes();
            checkPresets();
            checkPresetPageIsolation();
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
            } else {
                ScrollWidget<?> list = (ScrollWidget<?>) panel.getChildren()
                    .stream()
                    .filter(w -> w instanceof ScrollWidget)
                    .findFirst()
                    .get();
                require(list.getScrollY() > 0, "last preset is reachable by scrolling");
                Files.write(new File(output, "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                mc.shutdown();
            }
        } else if (stage == 2 && ticks == 5) {
            openEditor();
        }
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
            } else {
                require(
                    result.copy().nodes.stream()
                        .noneMatch(n -> n.wholeLineBatch),
                    "existing presets remain unchanged");
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
                var plan = FactoryCycles.prepare(result.copy().nodes, 1, new java.util.Random(1));
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
            require(draft.nodes.size() <= 32 && draft.hasTargetsForAllComponents(), "normal graph limits");
            for (FactoryGraph.Node node : draft.nodes) {
                require(node.customEUt == -1 && node.overclocks == 0 && node.parallel > 0, "preset preserves costs");
                require(FactoryRecipeCatalog.get(node.recipe) != null, "preset only references registered recipes");
                GTRecipe recipe = FactoryRecipeCatalog.get(node.recipe).recipe;
                int batch = FactoryRuntime.outputBatch(recipe);
                if (batch > 1) {
                    boolean rejected = false;
                    try {
                        FactoryRuntime.prepare(recipe, batch - 1, 0, new java.util.Random(1));
                    } catch (ArithmeticException incomplete) {
                        rejected = true;
                    }
                    require(rejected, "fractional batch is rejected, never rounded or randomly completed");
                }
                var first = FactoryRuntime.prepare(recipe, batch, 0, new java.util.Random(1));
                var second = FactoryRuntime.prepare(recipe, batch, 0, new java.util.Random(999));
                require(
                    FactoryRecipeCatalog.items(first.pendingItems.toArray(new ItemStack[0]))
                        .equals(FactoryRecipeCatalog.items(second.pendingItems.toArray(new ItemStack[0]))),
                    "deterministic outputs");
            }
            for (var cycle : new java.util.HashSet<>(
                FactoryCycles.groups(draft)
                    .values())) {
                var plan = FactoryCycles.prepare(cycle, 1, new java.util.Random(1));
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
        IntegratedProductionFactoryGui gui = new IntegratedProductionFactoryGui(
            new IntegratedProductionFactory("factory.qa"));
        Field graphField = IntegratedProductionFactoryGui.class.getDeclaredField("visibleGraph");
        graphField.setAccessible(true);
        FactoryGraph graph = (FactoryGraph) graphField.get(gui);
        for (int i = 0; i < 4; i++) graph.add(recipeId);
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
