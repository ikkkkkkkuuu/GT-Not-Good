package com.xyp.gtnotgood.utils.machine.factory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTRecipe;

/** Exact throughput balance for linked recipe outputs. Changes the draft only after a complete valid solution. */
public final class FactoryBalancer {

    private FactoryBalancer() {}

    /** One material port with an exact rate per tick and per parallel. */
    private static final class Port {

        int node;
        ItemStack item;
        FluidStack fluid;
        GTRecipe.RecipeItemInput match;
        Fraction rate;
    }

    public static boolean balance(FactoryGraph graph) {
        int size = graph.nodes.size();
        if (size == 0) return false;
        List<Port> outputs = new ArrayList<>(), inputs = new ArrayList<>();
        for (int n = 0; n < size; n++) {
            FactoryGraph.Node node = graph.nodes.get(n);
            FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
            if (entry == null) return false;
            long ticks = FactoryGraph.timing(1, entry.recipe.mDuration, 1, node.overclocks)[1];
            for (GTRecipe.RecipeItemInput input : entry.consumableRecipe.getCachedCombinedItemInputs()) {
                Port port = new Port();
                port.node = n;
                port.match = input;
                port.rate = new Fraction(input.inputAmount, ticks);
                inputs.add(port);
            }
            for (FluidStack fluid : entry.recipe.mFluidInputs) if (fluid != null && fluid.amount > 0) {
                Port port = new Port();
                port.node = n;
                port.fluid = fluid;
                port.rate = new Fraction(fluid.amount, ticks);
                inputs.add(port);
            }
            for (int i = 0; i < entry.recipe.mOutputs.length; i++) {
                ItemStack item = entry.recipe.mOutputs[i];
                if (item == null || item.stackSize <= 0 || entry.recipe.getOutputChance(i) <= 0) continue;
                Port port = new Port();
                port.node = n;
                port.item = item;
                port.rate = new Fraction((long) item.stackSize * entry.recipe.getOutputChance(i), ticks * 10000);
                outputs.add(port);
            }
            for (FluidStack fluid : entry.recipe.mFluidOutputs) if (fluid != null && fluid.amount > 0) {
                Port port = new Port();
                port.node = n;
                port.fluid = fluid;
                port.rate = new Fraction(fluid.amount, ticks);
                outputs.add(port);
            }
        }
        java.util.function.BiPredicate<Integer, Integer> links = (o, i) -> {
            Port out = outputs.get(o), in = inputs.get(i);
            if (!graph.nodes.get(in.node).sources.contains(graph.nodes.get(out.node).id)) return false;
            return out.item != null ? in.match != null && in.match.matchesType(out.item)
                : in.fluid != null && in.fluid.isFluidEqual(out.fluid);
        };
        int[] owners = new int[outputs.size() + inputs.size()];
        Fraction[] rates = new Fraction[owners.length];
        for (int i = 0; i < owners.length; i++) {
            Port port = i < outputs.size() ? outputs.get(i) : inputs.get(i - outputs.size());
            owners[i] = port.node;
            rates[i] = port.rate;
        }
        Fraction[][] equations = materialEquations(size, outputs.size(), owners, rates, links);
        int[] solution = solve(equations, size, FactoryGraph.MAX_PARALLEL);
        if (solution == null) return false;
        // Alternative ore matches may share a component without being fully interchangeable.
        // Check that the proposed ratios actually route before changing any user settings.
        double[] supply = new double[outputs.size()], demand = new double[inputs.size()];
        for (int i = 0; i < supply.length; i++) supply[i] = value(outputs.get(i), solution);
        for (int i = 0; i < demand.length; i++) demand[i] = value(inputs.get(i), solution);
        double[] original = demand.clone();
        FactoryPreview.allocate(supply, demand, links);
        for (int i = 0; i < demand.length; i++) {
            boolean linked = false;
            for (int o = 0; o < supply.length; o++) linked |= links.test(o, i);
            if (linked && demand[i] > Math.max(1e-9, original[i] * 1e-9)) return false;
        }
        for (int i = 0; i < size; i++) graph.nodes.get(i).parallel = solution[i];
        return true;
    }

    private static double value(Port port, int[] solution) {
        return port.rate.n.doubleValue() / port.rate.d.doubleValue() * solution[port.node];
    }

    /** Sum each connected material pool once, including all producers and consumers and duplicate output slots. */
    static Fraction[][] materialEquations(int size, int outputCount, int[] owners, Fraction[] rates,
        java.util.function.BiPredicate<Integer, Integer> links) {
        int[] parent = new int[owners.length];
        boolean[] linked = new boolean[owners.length];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        for (int o = 0; o < outputCount; o++) for (int i = outputCount; i < owners.length; i++) {
            if (!links.test(o, i - outputCount)) continue;
            parent[root(parent, i)] = root(parent, o);
            linked[o] = linked[i] = true;
        }
        java.util.Map<Integer, Fraction[]> rows = new java.util.LinkedHashMap<>();
        for (int i = 0; i < owners.length; i++) {
            if (!linked[i]) continue;
            Fraction[] row = rows.computeIfAbsent(root(parent, i), ignored -> zeros(size));
            Fraction signed = i < outputCount ? new Fraction(0, 1).subtract(rates[i]) : rates[i];
            row[owners[i]] = row[owners[i]].subtract(signed);
        }
        return rows.values()
            .toArray(new Fraction[0][]);
    }

    private static int root(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    /** RREF with rational arithmetic; bounded free-variable search cannot stall the server tick indefinitely. */
    static int[] solve(Fraction[][] source, int size, int limit) {
        long deadline = System.nanoTime() + 100_000_000L;
        Fraction[][] matrix = new Fraction[source.length][];
        for (int i = 0; i < source.length; i++) matrix[i] = source[i].clone();
        int[] pivots = new int[size];
        boolean[] pivot = new boolean[size];
        int rank = 0;
        for (int col = 0; col < size && rank < matrix.length; col++) {
            if (System.nanoTime() > deadline) return null;
            int row = rank;
            while (row < matrix.length && matrix[row][col].zero()) row++;
            if (row == matrix.length) continue;
            Fraction[] swap = matrix[rank];
            matrix[rank] = matrix[row];
            matrix[row] = swap;
            Fraction divisor = matrix[rank][col];
            for (int c = 0; c < size; c++) matrix[rank][c] = matrix[rank][c].divide(divisor);
            for (int r = 0; r < matrix.length; r++) {
                if (r == rank || matrix[r][col].zero()) continue;
                Fraction factor = matrix[r][col];
                for (int c = 0; c < size; c++) matrix[r][c] = matrix[r][c].subtract(factor.multiply(matrix[rank][c]));
            }
            pivot[col] = true;
            pivots[rank++] = col;
        }
        if (rank == size) return null;
        int[] free = new int[size - rank];
        for (int c = 0, i = 0; c < size; c++) if (!pivot[c]) free[i++] = c;
        for (int attempt = 0; attempt < 65536; attempt++) {
            if (System.nanoTime() > deadline) return null;
            Fraction[] vector = zeros(size);
            int code = attempt;
            for (int c : free) {
                vector[c] = new Fraction(1 + code % 64, 1);
                code /= 64;
            }
            if (attempt > 0 && code != 0) break;
            for (int r = 0; r < rank; r++) for (int c : free) {
                vector[pivots[r]] = vector[pivots[r]].subtract(matrix[r][c].multiply(vector[c]));
            }
            int[] result = integers(vector, limit);
            if (result != null) return result;
            if (free.length == 1) break;
        }
        return null;
    }

    private static int[] integers(Fraction[] values, int limit) {
        BigInteger lcm = BigInteger.ONE;
        for (Fraction value : values) {
            if (value.n.signum() <= 0) return null;
            lcm = lcm.divide(lcm.gcd(value.d))
                .multiply(value.d);
        }
        BigInteger[] scaled = new BigInteger[values.length];
        BigInteger gcd = BigInteger.ZERO;
        for (int i = 0; i < values.length; i++) {
            scaled[i] = values[i].n.multiply(lcm.divide(values[i].d));
            gcd = gcd.gcd(scaled[i]);
        }
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            BigInteger value = scaled[i].divide(gcd);
            if (value.compareTo(BigInteger.valueOf(limit)) > 0) return null;
            result[i] = value.intValueExact();
        }
        return result;
    }

    private static Fraction[] zeros(int size) {
        Fraction[] values = new Fraction[size];
        Arrays.fill(values, new Fraction(0, 1));
        return values;
    }

    /** Reduced immutable fraction prevents rounding drift in long or cyclic production chains. */
    static final class Fraction {

        final BigInteger n;
        final BigInteger d;

        Fraction(long n, long d) {
            this(BigInteger.valueOf(n), BigInteger.valueOf(d));
        }

        Fraction(BigInteger n, BigInteger d) {
            if (d.signum() == 0) throw new ArithmeticException("Zero denominator");
            BigInteger gcd = n.gcd(d)
                .multiply(BigInteger.valueOf(d.signum()));
            this.n = n.divide(gcd);
            this.d = d.divide(gcd);
        }

        boolean zero() {
            return n.signum() == 0;
        }

        Fraction subtract(Fraction b) {
            return new Fraction(
                n.multiply(b.d)
                    .subtract(b.n.multiply(d)),
                d.multiply(b.d));
        }

        Fraction multiply(Fraction b) {
            return new Fraction(n.multiply(b.n), d.multiply(b.d));
        }

        Fraction divide(Fraction b) {
            return new Fraction(n.multiply(b.d), d.multiply(b.n));
        }
    }
}
