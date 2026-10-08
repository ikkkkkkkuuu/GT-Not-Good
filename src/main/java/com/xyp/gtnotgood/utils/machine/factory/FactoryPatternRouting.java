package com.xyp.gtnotgood.utils.machine.factory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.objects.GTDualInputPattern;
import gregtech.api.util.GTRecipe;

/** Binds a pattern's complete input signature to one installed page, never to a matching subset on another page. */
public final class FactoryPatternRouting {

    public static final class Binding {

        public final Set<Integer> nodes;
        public final FactoryText failure;

        private Binding(Set<Integer> nodes, FactoryText failure) {
            this.nodes = Collections.unmodifiableSet(nodes);
            this.failure = failure;
        }
    }

    private static final class Target {

        int page;
        Set<Integer> nodes = new HashSet<>();
        GTDualInputPattern inputs;
        boolean sensitive;
        boolean wholePage;
    }

    private final List<Target> targets = new ArrayList<>();
    private final Map<String, Binding> cache = new HashMap<>();

    /** Rebuilt only when the installed graph changes; preview/cycle discovery never runs per inventory per tick. */
    public FactoryPatternRouting(FactoryGraph installed) {
        Map<Integer, FactoryGraph> pages = new LinkedHashMap<>();
        for (FactoryGraph.Node node : installed.nodes)
            pages.computeIfAbsent(node.page, ignored -> new FactoryGraph()).nodes.add(node);
        for (Map.Entry<Integer, FactoryGraph> page : pages.entrySet()) {
            addBoundary(page.getKey(), page.getValue(), true);
            Map<Integer, List<FactoryGraph.Node>> cycles = FactoryCycles.groups(page.getValue());
            Set<List<FactoryGraph.Node>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (FactoryGraph.Node node : page.getValue().nodes) {
                List<FactoryGraph.Node> cycle = cycles.get(node.id);
                if (cycle != null) {
                    if (seen.add(cycle)) {
                        FactoryGraph component = new FactoryGraph();
                        component.nodes.addAll(cycle);
                        addBoundary(page.getKey(), component, false);
                    }
                    continue;
                }
                FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
                if (entry == null) continue;
                Target target = new Target();
                target.page = page.getKey();
                target.nodes.add(node.id);
                target.inputs = new GTDualInputPattern(entry.consumableRecipe.mInputs, entry.recipe.mFluidInputs);
                target.sensitive = entry.recipe.isNBTSensitive;
                targets.add(target);
            }
        }
    }

    private void addBoundary(int page, FactoryGraph graph, boolean wholePage) {
        FactoryPreview.Snapshot preview = FactoryPreview.describe(graph);
        if (preview.inputs.isEmpty()) return;
        try {
            long[] amounts = FactoryPatternExport.batchCounts(preview,
                preview.inputs.stream().mapToDouble(i -> i.rate).toArray());
            List<ItemStack> items = new ArrayList<>();
            List<FluidStack> fluids = new ArrayList<>();
            for (int i = 0; i < amounts.length; i++) {
                int amount = Math.toIntExact(amounts[i]);
                FactoryPreview.Ingredient ingredient = preview.inputs.get(i);
                if (ingredient.item != null) {
                    ItemStack item = ingredient.item.copy();
                    item.stackSize = amount;
                    items.add(item);
                } else {
                    FluidStack fluid = ingredient.fluid.copy();
                    fluid.amount = amount;
                    fluids.add(fluid);
                }
            }
            Target target = new Target();
            target.page = page;
            target.wholePage = wholePage;
            target.inputs = new GTDualInputPattern(items.toArray(new ItemStack[0]), fluids.toArray(new FluidStack[0]));
            for (FactoryGraph.Node node : graph.nodes) {
                target.nodes.add(node.id);
                FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
                target.sensitive |= entry != null && entry.recipe.isNBTSensitive;
            }
            targets.add(target);
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            // Unrepresentable boundaries are unavailable; individual supported recipes can still be addressed.
        }
    }

    public Binding bind(GTDualInputPattern pattern) {
        if (pattern == null) return new Binding(Collections.emptySet(), FactoryText.PatternMismatch);
        String key = FactoryRecipeCatalog.items(pattern.inputItems == null ? new ItemStack[0] : pattern.inputItems)
            .toString()
            + FactoryRecipeCatalog.fluids(pattern.inputFluid == null ? new FluidStack[0] : pattern.inputFluid)
                .toString();
        Binding cached = cache.get(key);
        if (cached != null) return cached;
        int page = -1;
        Set<Integer> nodes = new HashSet<>();
        FactoryText failure = FactoryText.PatternMismatch;
        List<Target> matches = new ArrayList<>();
        for (Target target : targets) {
            if (!matches(target.inputs, pattern, target.sensitive)) continue;
            if (page != -1 && page != target.page) {
                nodes.clear();
                failure = FactoryText.PatternAmbiguous;
                break;
            }
            page = target.page;
            matches.add(target);
            nodes.addAll(target.nodes);
            failure = null;
        }
        Set<Integer> process = null;
        boolean ambiguousProcess = false;
        for (Target target : matches) {
            if (target.wholePage) continue;
            if (process != null && !process.equals(target.nodes)) ambiguousProcess = true;
            process = target.nodes;
        }
        if (
            failure == null
                && (ambiguousProcess || matches.stream().noneMatch(target -> target.nodes.containsAll(nodes)))
        ) {
            nodes.clear();
            failure = FactoryText.PatternAmbiguous;
        }
        Binding result = new Binding(nodes, failure);
        if (cache.size() >= 256) cache.clear();
        cache.put(key, result);
        return result;
    }

    /** Exact type coverage and one common positive quantity ratio; extra, missing or differently tagged fluids fail. */
    public static boolean matches(GTDualInputPattern expected, GTDualInputPattern actual, boolean sensitive) {
        List<Object> types = new ArrayList<>();
        List<BigInteger> required = new ArrayList<>();
        if (!collect(expected, types, required, sensitive, false)) return false;
        List<BigInteger> supplied = new ArrayList<>(Collections.nCopies(types.size(), BigInteger.ZERO));
        if (!collect(actual, types, supplied, sensitive, true) || types.isEmpty()) return false;
        BigInteger numerator = supplied.get(0), denominator = required.get(0);
        if (numerator.signum() <= 0) return false;
        for (int i = 0; i < types.size(); i++)
            if (!supplied.get(i).multiply(denominator).equals(required.get(i).multiply(numerator))) return false;
        return true;
    }

    private static boolean collect(GTDualInputPattern pattern, List<Object> types, List<BigInteger> amounts,
        boolean sensitive, boolean existingOnly) {
        if (pattern == null) return false;
        if (pattern.inputItems != null) for (ItemStack item : pattern.inputItems) {
            if (item == null) continue;
            if (item.stackSize < 0) return false;
            if (item.stackSize > 0 && !add(types, amounts, item, item.stackSize, sensitive, existingOnly)) return false;
        }
        if (pattern.inputFluid != null) for (FluidStack fluid : pattern.inputFluid) {
            if (fluid == null) continue;
            if (fluid.amount <= 0) return false;
            if (!add(types, amounts, fluid, fluid.amount, sensitive, existingOnly)) return false;
        }
        return true;
    }

    private static boolean add(List<Object> types, List<BigInteger> amounts, Object value, int amount,
        boolean sensitive, boolean existingOnly) {
        int found = -1;
        for (int i = 0; i < types.size(); i++) {
            Object type = types.get(i);
            boolean match = type instanceof ItemStack item && value instanceof ItemStack candidate
                ? new GTRecipe.RecipeItemInput(item, sensitive).matchesType(candidate)
                : type instanceof FluidStack fluid && value instanceof FluidStack candidateFluid
                    && fluid.isFluidEqual(candidateFluid);
            if (!match) continue;
            if (found != -1) return false;
            found = i;
        }
        if (found == -1) {
            if (existingOnly) return false;
            types.add(value);
            amounts.add(BigInteger.valueOf(amount));
        } else amounts.set(found, amounts.get(found).add(BigInteger.valueOf(amount)));
        return true;
    }
}
