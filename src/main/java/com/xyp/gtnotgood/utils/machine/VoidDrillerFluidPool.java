package com.xyp.gtnotgood.utils.machine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import gregtech.GTMod;
import gregtech.api.objects.GTUODimension;
import gregtech.api.objects.GTUODimensionList;
import gregtech.api.objects.GTUOFluid;

public final class VoidDrillerFluidPool {

    private static final Map<String, String> DIMENSION_ALIASES = new HashMap<>();
    private static Class<?> dimensionItemClass;
    private static Method readDimensionMethod;
    private static boolean dimensionMetadataInitialized;
    private static Integer[] cachedDimensionIds;
    private static Map<Integer, RegisteredDimension> registeredDimensions = Collections.emptyMap();

    private VoidDrillerFluidPool() {}

    public static boolean isDimensionDisplayItem(ItemStack stack) {
        if (stack == null) return false;
        initializeDimensionMetadata();
        return dimensionItemClass != null && dimensionItemClass.isInstance(stack.getItem());
    }

    public static String readDimensionAbbr(ItemStack stack) {
        if (!isDimensionDisplayItem(stack) || readDimensionMethod == null) return null;
        try {
            Object value = readDimensionMethod.invoke(null, stack);
            if (!(value instanceof String)) return null;
            String dimension = ((String) value).trim();
            return dimension.isEmpty() || "None".equals(dimension) ? null : dimension;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    public static String getDimensionLabel(int dimensionId) {
        return getLocalDimensionLabel(dimensionId, getRegisteredDimensions());
    }

    public static List<Entry> getEntries(int localDimensionId, List<String> selectedAbbrs) {
        GTUODimensionList configuration = getConfiguration();
        Map<Integer, RegisteredDimension> registry = getRegisteredDimensions();
        Map<String, GTUODimension> dimensions = new LinkedHashMap<>();
        Set<String> selected = new HashSet<>();
        if (selectedAbbrs == null || selectedAbbrs.isEmpty()) {
            String label = getLocalDimensionLabel(localDimensionId, registry);
            selected.add(normalize(label));
            if (configuration != null) {
                GTUODimension dimension = getConfiguredDimension(configuration, localDimensionId, registry);
                if (dimension != null) dimensions.put(label, dimension);
            }
        } else {
            for (String abbr : selectedAbbrs) {
                if (abbr != null && !abbr.trim().isEmpty()) {
                    String label = resolveDimensionLabel(abbr, registry);
                    selected.add(normalize(label));
                    if (configuration != null) {
                        GTUODimension dimension = getSelectedDimension(configuration, label, registry);
                        if (dimension != null) dimensions.put(label, dimension);
                    }
                }
            }
        }
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, GTUODimension> dimension : dimensions.entrySet()) {
            appendEntries(entries, dimension.getValue(), dimension.getKey());
        }
        appendOceans(entries, selected);
        appendGasRecipes(entries, selected);
        return normalizeEntries(entries);
    }

    private static List<Entry> normalizeEntries(List<Entry> entries) {
        Map<String, Double> totals = new HashMap<>();
        for (Entry entry : entries) totals.merge(entry.dimension, entry.weight, Double::sum);
        List<Entry> normalized = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            normalized.add(new Entry(entry.fluidName, entry.dimension, entry.weight / totals.get(entry.dimension),
                entry.minAmount, entry.maxAmount));
        }
        normalized.sort((left, right) -> {
            int dimensionOrder = left.dimension.compareTo(right.dimension);
            return dimensionOrder == 0 ? left.fluidName.compareTo(right.fluidName) : dimensionOrder;
        });
        return Collections.unmodifiableList(normalized);
    }

    private static void appendOceans(List<Entry> entries, Set<String> selected) {
        appendOcean(entries, selected, "Ow", FluidRegistry.WATER);
        appendOcean(entries, selected, "Ne", FluidRegistry.LAVA);
        appendOcean(entries, selected, "Eu",
            readOceanFluid("galaxyspace.SolarSystem.moons.europa.dimension.ChunkProviderEuropa", FluidRegistry.WATER));
        appendOcean(entries, selected, "TE", readOceanFluid(
            "galaxyspace.TCetiSystem.planets.tcetiE.dimension.ChunkProviderTCetiE", FluidRegistry.WATER));
        appendOcean(entries, selected, "Ti",
            readRegisteredFluid("galaxyspace.core.register.GSFluids", "LiquidMethane"));
        appendOcean(entries, selected, "BC", readRegisteredFluid("galaxyspace.BarnardsSystem.BRFluids", "UnknowWater"));
    }

    private static void appendOcean(List<Entry> entries, Set<String> selected, String dimension, Fluid fluid) {
        if (
            fluid != null && selected.contains(normalize(dimension)) && FluidRegistry.getFluid(fluid.getName()) != null
        ) {
            entries.add(new Entry(fluid.getName(), dimension, 1, 1000, 1000));
        }
    }

    private static Fluid readRegisteredFluid(String className, String fieldName) {
        try {
            Object value = Class.forName(className, false, VoidDrillerFluidPool.class.getClassLoader())
                .getField(fieldName).get(null);
            return value instanceof Fluid ? (Fluid) value : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static Fluid readOceanFluid(String providerClass, Fluid fluid) {
        try {
            Class.forName(providerClass, false, VoidDrillerFluidPool.class.getClassLoader());
            return fluid;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }

    private static void appendGasRecipes(List<Entry> entries, Set<String> selected) {
        try {
            Class<?> recipes = Class.forName("gtnhintergalactic.recipe.GasSiphonRecipes", false,
                VoidDrillerFluidPool.class.getClassLoader());
            Object registered = recipes.getField("RECIPES").get(null);
            if (!(registered instanceof Map<?, ?>)) return;
            for (Map.Entry<?, ?> recipe : ((Map<?, ?>) registered).entrySet()) {
                if (!(recipe.getKey() instanceof String) || recipe.getValue() == null) continue;
                String dimension = getLabel((String) recipe.getKey());
                if (!selected.contains(normalize(dimension))) continue;
                Object depths = recipe.getValue().getClass().getField("depths").get(recipe.getValue());
                if (!(depths instanceof Map<?, ?>)) continue;
                List<FluidStack> fluids = new ArrayList<>();
                for (Object value : ((Map<?, ?>) depths).values()) {
                    if (value instanceof FluidStack) {
                        FluidStack fluid = (FluidStack) value;
                        if (
                            fluid.amount > 0 && fluid.getFluid() != null
                                && FluidRegistry.getFluid(fluid.getFluid().getName()) != null
                        ) fluids.add(fluid);
                    }
                }
                for (FluidStack fluid : fluids) {
                    entries.add(new Entry(fluid.getFluid().getName(), dimension, 1.0 / fluids.size(), fluid.amount,
                        fluid.amount));
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {}
    }

    private static String getGasPlanet(WorldProvider provider) {
        try {
            Object orbit = provider.getClass().getMethod("getPlanetToOrbit").invoke(provider);
            if (orbit instanceof String) return getGasLabel((String) orbit);
        } catch (ReflectiveOperationException | LinkageError ignored) {}
        try {
            Object body = provider.getClass().getMethod("getCelestialBody").invoke(provider);
            if (body == null) return null;
            Object name = body.getClass().getMethod("getUnlocalizedName").invoke(body);
            return name instanceof String ? getGasLabel((String) name) : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static String getGasLabel(String planet) {
        String label = getLabel(planet);
        return "Jupiter".equals(label) || "Saturn".equals(label) || "Uranus".equals(label) || "Neptune".equals(label)
            ? label
            : null;
    }

    private static GTUODimensionList getConfiguration() {
        return GTMod.proxy == null ? null : GTMod.proxy.mUndergroundOil;
    }

    private static GTUODimension getConfiguredDimension(GTUODimensionList configuration, int dimensionId,
        Map<Integer, RegisteredDimension> registry) {
        if (isBlacklisted(configuration, dimensionId)) return null;
        GTUODimension direct = configuration.getDimensionList().get(Integer.toString(dimensionId));
        if (direct != null) return direct;
        RegisteredDimension registered = registry.get(dimensionId);
        if (registered != null) {
            for (GTUODimension dimension : configuration.getDimensionList().values()) {
                if (
                    dimension != null && dimension.Dimension != null
                        && registered.providerClass.contains(dimension.Dimension)
                ) {
                    return dimension;
                }
            }
        }
        return configuration.getDimensionList().get("Default");
    }

    private static GTUODimension getSelectedDimension(GTUODimensionList configuration, String label,
        Map<Integer, RegisteredDimension> registry) {
        boolean registered = false;
        for (RegisteredDimension dimension : registry.values()) {
            if (!normalize(dimension.label).equals(normalize(label))) continue;
            registered = true;
            GTUODimension configured = getConfiguredDimension(configuration, dimension.id, registry);
            if (configured != null) return configured;
        }
        if (registered) return null;
        for (Map.Entry<String, GTUODimension> entry : configuration.getDimensionList().entrySet()) {
            if (!normalize(resolveDimensionLabel(entry.getKey(), registry)).equals(normalize(label))) continue;
            try {
                if (isBlacklisted(configuration, Integer.parseInt(entry.getKey()))) continue;
            } catch (NumberFormatException ignored) {}
            return entry.getValue();
        }
        return null;
    }

    private static boolean isBlacklisted(GTUODimensionList configuration, int dimensionId) {
        for (int blocked : configuration.blackList) {
            if (blocked == dimensionId) return true;
        }
        return false;
    }

    private static String resolveDimensionLabel(String dimension, Map<Integer, RegisteredDimension> registry) {
        try {
            RegisteredDimension registered = registry.get(Integer.parseInt(dimension));
            if (registered != null) return registered.label;
        } catch (NumberFormatException ignored) {}
        String label = getLabel(dimension);
        if (DIMENSION_ALIASES.containsKey(normalize(dimension))) return label;
        for (RegisteredDimension registered : registry.values()) {
            if (registered.providerClass.contains(dimension)) return registered.label;
        }
        return label;
    }

    private static String getLocalDimensionLabel(int dimensionId, Map<Integer, RegisteredDimension> registry) {
        WorldServer loaded = DimensionManager.getWorld(dimensionId);
        if (loaded != null && loaded.provider != null) {
            try {
                return getProviderLabel(loaded.provider, dimensionId);
            } catch (RuntimeException | LinkageError ignored) {}
        }
        return resolveDimensionLabel(Integer.toString(dimensionId), registry);
    }

    private static synchronized Map<Integer, RegisteredDimension> getRegisteredDimensions() {
        Integer[] dimensionIds = DimensionManager.getStaticDimensionIDs();
        Arrays.sort(dimensionIds);
        if (Arrays.equals(cachedDimensionIds, dimensionIds)) return registeredDimensions;
        Map<Integer, RegisteredDimension> registry = new LinkedHashMap<>();
        for (int dimensionId : dimensionIds) {
            try {
                WorldServer loaded = DimensionManager.getWorld(dimensionId);
                WorldProvider provider = loaded == null ? DimensionManager.createProviderFor(dimensionId)
                    : loaded.provider;
                if (provider != null) {
                    String label = getLabel(Integer.toString(dimensionId));
                    try {
                        label = getProviderLabel(provider, dimensionId);
                    } catch (RuntimeException | LinkageError ignored) {}
                    registry.put(dimensionId,
                        new RegisteredDimension(dimensionId, provider.getClass().getName(), label));
                }
            } catch (RuntimeException | LinkageError ignored) {}
        }
        cachedDimensionIds = dimensionIds;
        registeredDimensions = Collections.unmodifiableMap(registry);
        return registeredDimensions;
    }

    private static String getProviderLabel(WorldProvider provider, int dimensionId) {
        String planet = getGasPlanet(provider);
        if (planet != null) return planet;
        try {
            Object body = provider.getClass().getMethod("getCelestialBody").invoke(provider);
            if (body != null) {
                Object name = body.getClass().getMethod("getUnlocalizedName").invoke(body);
                if (name instanceof String) {
                    String rawName = (String) name;
                    String label = getLabel(rawName.substring(rawName.lastIndexOf('.') + 1));
                    if (DIMENSION_ALIASES.containsKey(normalize(label))) return label;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {}
        String name = provider.getDimensionName();
        String label = getLabel(name);
        if (DIMENSION_ALIASES.containsKey(normalize(name))) return label;
        String type = provider.getClass().getSimpleName();
        if (type.startsWith("WorldProvider")) type = type.substring("WorldProvider".length());
        if (DIMENSION_ALIASES.containsKey(normalize(type))) return getLabel(type);
        return name == null || name.trim().isEmpty() ? getLabel(Integer.toString(dimensionId)) : label;
    }

    private static void appendEntries(List<Entry> entries, GTUODimension dimension, String label) {
        double totalWeight = 0;
        for (GTUOFluid fluid : dimension.getFluids().values()) {
            if (isValid(fluid)) totalWeight += fluid.Chance;
        }
        if (totalWeight <= 0) return;
        for (GTUOFluid fluid : dimension.getFluids().values()) {
            if (isValid(fluid)) {
                entries.add(
                    new Entry(fluid.Registry, label, fluid.Chance / totalWeight, fluid.MinAmount, fluid.MaxAmount));
            }
        }
    }

    private static boolean isValid(GTUOFluid fluid) {
        return fluid != null && fluid.Chance > 0
            && fluid.MinAmount >= 0
            && fluid.MaxAmount > 0
            && fluid.MaxAmount >= fluid.MinAmount
            && fluid.getFluid() != null;
    }

    private static String getLabel(String dimension) {
        initializeDimensionMetadata();
        return DIMENSION_ALIASES.getOrDefault(normalize(dimension), dimension);
    }

    private static String normalize(String dimension) {
        return dimension == null ? "" : dimension.trim().toLowerCase(Locale.ROOT);
    }

    private static synchronized void initializeDimensionMetadata() {
        if (dimensionMetadataInitialized) return;
        dimensionMetadataInitialized = true;
        ClassLoader loader = VoidDrillerFluidPool.class.getClassLoader();
        try {
            dimensionItemClass = Class.forName("gtneioreplugin.plugin.item.ItemDimensionDisplay", false, loader);
            readDimensionMethod = dimensionItemClass.getMethod("getDimension", ItemStack.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            dimensionItemClass = null;
        }
        try {
            Class<?> helper = Class.forName("gtneioreplugin.util.DimensionHelper", false, loader);
            Object records = helper.getMethod("getAllDim").invoke(null);
            if (records instanceof Collection<?>) {
                for (Object record : (Collection<?>) records) {
                    if (record == null) continue;
                    Class<?> type = record.getClass();
                    String abbr = (String) type.getMethod("abbr").invoke(record);
                    addAlias(abbr, abbr);
                    for (String accessor : new String[] { "fullName", "internalName", "trimmedName" }) {
                        addAlias((String) type.getMethod(accessor).invoke(record), abbr);
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {}
        addAlias("0", "Ow");
        addAlias("Overworld", "Ow");
        addAlias("-1", "Ne");
        addAlias("Nether", "Ne");
        addAlias("Europa", "Eu");
        addAlias("Titan", "Ti");
        addAlias("aCentauriBb", "CB");
        addAlias("BarnardaC", "BC");
        addAlias("BarnardaE", "BE");
        addAlias("BarnardaF", "BF");
        addAlias("TCetiE", "TE");
        for (String planet : new String[] { "Jupiter", "Saturn", "Uranus", "Neptune" }) {
            addAlias(planet, planet);
            addAlias("planet." + planet, planet);
        }
    }

    private static void addAlias(String name, String abbr) {
        if (name != null && abbr != null) {
            DIMENSION_ALIASES.put(normalize(name), abbr);
            DIMENSION_ALIASES.put(normalize(abbr), abbr);
        }
    }

    private static final class RegisteredDimension {

        private final int id;
        private final String providerClass;
        private final String label;

        private RegisteredDimension(int id, String providerClass, String label) {
            this.id = id;
            this.providerClass = providerClass;
            this.label = label;
        }
    }

    public static final class Entry {

        public final String fluidName;
        public final String dimension;
        public final double weight;
        public final int minAmount;
        public final int maxAmount;

        private Entry(String fluidName, String dimension, double weight, int minAmount, int maxAmount) {
            this.fluidName = fluidName;
            this.dimension = dimension;
            this.weight = weight;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }
    }
}
