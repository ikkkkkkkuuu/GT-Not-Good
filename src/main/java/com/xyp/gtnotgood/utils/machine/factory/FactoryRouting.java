package com.xyp.gtnotgood.utils.machine.factory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTRecipe;

/** Adapts the BOX route-list workflow to server-owned recipes and the existing production scheduler. */
public final class FactoryRouting {

    private FactoryRouting() {}

    /** Rebuilds material links from GT matching rules; reserved catalysts never create a material dependency. */
    public static void connect(FactoryGraph graph) {
        for (FactoryGraph.Node target : graph.nodes) {
            target.sources.clear();
            target.target = true;
            FactoryRecipeCatalog.Entry to = FactoryRecipeCatalog.get(target.recipe);
            if (to == null) continue;
            for (FactoryGraph.Node source : graph.nodes) {
                FactoryRecipeCatalog.Entry from = FactoryRecipeCatalog.get(source.recipe);
                if (from != null && connects(from, to)) target.sources.add(source.id);
            }
        }
    }

    private static boolean connects(FactoryRecipeCatalog.Entry from, FactoryRecipeCatalog.Entry to) {
        for (GTRecipe.RecipeItemInput input : to.consumableRecipe.getCachedCombinedItemInputs())
            for (ItemStack output : from.recipe.mOutputs) if (output != null && input.matchesType(output)) return true;
        for (FluidStack input : to.recipe.mFluidInputs) for (FluidStack output : from.recipe.mFluidOutputs)
            if (input != null && output != null && input.isFluidEqual(output)) return true;
        return false;
    }

    /** BOX doubles or halves all route counts atomically; odd counts cannot be halved. */
    public static boolean scale(FactoryGraph graph, boolean doubleCounts) {
        if (graph.nodes.isEmpty()) return false;
        for (FactoryGraph.Node node : graph.nodes) if (doubleCounts ? node.parallel > FactoryGraph.MAX_PARALLEL / 2
            : node.parallel < 2 || node.parallel % 2 != 0) return false;
        for (FactoryGraph.Node node : graph.nodes) node.parallel = doubleCounts ? node.parallel * 2 : node.parallel / 2;
        return true;
    }

    /** Portable recipe identities only: importing never trusts client-supplied inputs, outputs or recipe costs. */
    public static String encode(FactoryGraph graph) {
        StringBuilder text = new StringBuilder("GTNG1");
        for (FactoryGraph.Node node : graph.nodes) text.append(';')
            .append(node.recipe)
            .append(',')
            .append(node.parallel)
            .append(',')
            .append(node.overclocks)
            .append(',')
            .append(node.customEUt);
        return text.toString();
    }

    /** Fully validates a bounded route code before the caller replaces its draft. */
    public static FactoryGraph decode(String text) {
        if (text.length() > 8192) throw new IllegalArgumentException();
        String[] rows = text.trim()
            .split(";", -1);
        if (!rows[0].equals("GTNG1") || rows.length < 2 || rows.length > FactoryGraph.MAX_NODES + 1)
            throw new IllegalArgumentException();
        FactoryGraph graph = new FactoryGraph();
        for (int i = 1; i < rows.length; i++) {
            String[] parts = rows[i].split(",", -1);
            if (parts.length != 4 || FactoryRecipeCatalog.get(parts[0]) == null) throw new IllegalArgumentException();
            int parallel = Integer.parseInt(parts[1]), oc = Integer.parseInt(parts[2]);
            long eut = Long.parseLong(parts[3]);
            if (parallel < 1 || parallel > FactoryGraph.MAX_PARALLEL || oc < 0 || oc > 14 || eut < -1)
                throw new IllegalArgumentException();
            graph.add(parts[0]);
            FactoryGraph.Node node = graph.nodes.get(graph.nodes.size() - 1);
            node.parallel = parallel;
            node.overclocks = oc;
            node.customEUt = eut;
        }
        connect(graph);
        return graph;
    }
}
