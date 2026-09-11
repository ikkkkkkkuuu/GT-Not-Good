package com.xyp.gtnotgood.config;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/** Explicit server-editable option allowlist; network input can never select arbitrary fields or files. */
public final class ServerConfigOptions {

    private ServerConfigOptions() {}

    public static Map<String, Option> options() {
        Map<String, Option> options = new LinkedHashMap<>();
        add(
            options,
            false,
            "Wireless_Multiblock",
            "crossRecipeDurationTicks",
            "wirelessCrossRecipeDurationTicks",
            true);
        add(
            options,
            false,
            "Wireless_Multiblock",
            "crossRecipeParallelLimit",
            "wirelessCrossRecipeParallelLimit",
            true);
        add(
            options,
            false,
            "Torcherino",
            "gregTechAccelerationDiscount",
            "torcherinoGregTechAccelerationDiscount",
            true);
        add(options, false, "Torcherino", "maxXRadius", "torcherinoMaxXRadius", false);
        add(options, false, "Torcherino", "maxYRadius", "torcherinoMaxYRadius", false);
        add(options, false, "Torcherino", "maxZRadius", "torcherinoMaxZRadius", false);
        add(options, false, "Torcherino", "maxSpeedLevel", "torcherinoMaxSpeedLevel", false);
        add(options, false, "Torcherino", "enableTickBudget", "torcherinoEnableTickBudget", true);
        add(options, false, "Torcherino", "tickBudgetNanos", "torcherinoTickBudgetNanos", true);
        add(options, false, "Torcherino", "enableStackingAcceleration", "torcherinoEnableStackingAcceleration", true);
        add(options, false, "Torcherino", "enableOverlapDetection", "torcherinoEnableOverlapDetection", true);
        add(options, false, "Torcherino", "enableWirelessTorcherino", "enableWirelessTorcherino", false);
        add(options, false, "Torcherino", "wirelessTorcherinoRadius", "wirelessTorcherinoRadius", false);
        add(
            options,
            false,
            "Torcherino",
            "wirelessTorcherinoMaxBoundMachines",
            "wirelessTorcherinoMaxBoundMachines",
            false);
        add(options, false, "CutCorners", "mode", "recipeSpeedMode", false);
        add(options, false, "CutCorners", "fixedDuration", "recipeSpeedFixedDuration", false);
        add(options, false, "CutCorners", "multiplier", "recipeSpeedMultiplier", false);
        add(options, false, "CutCorners", "fullFluidOutput", "recipeSpeedFullFluidOutput", true);
        add(options, false, "CropsNH", "enableInstantGrowth", "enableCropInstantGrowth", true);
        add(options, false, "CropsNH", "enableMaxStats", "enableCropMaxStats", true);
        add(options, false, "CropsNH", "enableGuaranteedSeedDrop", "enableCropGuaranteedSeedDrop", true);
        add(options, false, "Forestry", "enableBeeAlwaysJubilant", "enableBeeAlwaysJubilant", true);
        add(options, false, "Forestry", "enableHomozygousOffspring", "enableBeeHomozygousOffspring", true);
        add(options, false, "Forestry", "enableMaxGenomeOnBreed", "enableBeeMaxGenomeOnBreed", true);
        add(options, false, "Forestry", "enableBeeIgnoreDimensionMutation", "enableBeeIgnoreDimensionMutation", true);
        add(options, false, "Forestry", "enableBeeIgnoreResourceMutation", "enableBeeIgnoreResourceMutation", true);
        add(options, false, "Thaumcraft", "disableWarpEvents", "disableWarpEvents", true);
        add(options, false, "Thaumcraft", "unlockAllResearch", "tcUnlockAllResearch", false);
        add(options, false, "Thaumcraft", "freeResearchAspects", "tcFreeResearchAspects", false);
        add(options, false, "Thaumcraft", "scanIgnoreParentAspects", "tcScanIgnoreParentAspects", false);
        add(options, false, "Thaumcraft", "infusionNoInstability", "tcInfusionNoInstability", true);
        add(options, false, "Thaumcraft", "infiniteVis", "tcInfiniteVis", true);
        add(options, true, "鸿蒙之眼", "GasInPut", "GasInPut", true);
        add(options, true, "鸿蒙之眼", "EOHSuccessRateControls", "EOHSuccessRateControls", true);
        add(options, true, "鸿蒙之眼", "RecipeChance", "RecipeChance", true);
        add(options, true, "诸神之锻炉", "FOGUpDate", "FOGUpDate", true);
        add(options, true, "黑洞压缩机", "BlackHoleCompressorStabilityLock", "BlackHoleCompressorStabilityLock", true);
        add(options, true, "净化水", "Water", "Water", false);
        add(options, true, "净化水", "Grade1WaterPurificationEnabled", "Grade1WaterPurificationEnabled", true);
        add(options, true, "净化水", "Grade2WaterPurificationEnabled", "Grade2WaterPurificationEnabled", false);
        add(options, true, "净化水", "Grade3WaterPurificationEnabled", "Grade3WaterPurificationEnabled", true);
        add(options, true, "净化水", "Grade4WaterPurificationEnabled", "Grade4WaterPurificationEnabled", true);
        add(options, true, "净化水", "Grade5WaterPurificationEnabled", "Grade5WaterPurificationEnabled", true);
        add(options, true, "净化水", "Grade6WaterPurificationEnabled", "Grade6WaterPurificationEnabled", true);
        add(options, true, "净化水", "Grade7WaterPurificationEnabled", "Grade7WaterPurificationEnabled", true);
        add(options, true, "净化水", "Grade8WaterPurificationEnabled", "Grade8WaterPurificationEnabled", true);
        return options;
    }

    private static void add(Map<String, Option> options, boolean main, String category, String key, String field,
        boolean live) {
        Option option = new Option(main, category, key, field, live);
        options.put(option.id, option);
    }

    /** Fixed binding between a persisted Forge property and its runtime field. */
    public static final class Option {

        public final String id;
        public final String category;
        public final String key;
        public final boolean live;
        private final boolean main;
        private final Field field;

        private Option(boolean main, String category, String key, String fieldName, boolean live) {
            this.main = main;
            this.category = category;
            this.key = key;
            this.live = live;
            this.id = (main ? "main/" : "config/") + category + "/" + key;
            try {
                field = (main ? MainConfig.class : Config.class).getField(fieldName);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Invalid server configuration binding", e);
            }
        }

        public Configuration configuration() {
            return main ? MainConfig.getConfiguration() : Config.getConfiguration();
        }

        public Property property() {
            return configuration().getCategory(category.toLowerCase(Locale.ENGLISH))
                .get(key);
        }

        /**
         * Parses using the server's property type and bounds before any transaction is applied.
         *
         * @param value untrusted scalar sent by the client
         * @return correctly boxed runtime field value
         */
        public Object parse(String value) {
            Property property = property();
            validate(property, value);
            if (field.getType() == boolean.class) return Boolean.valueOf(value);
            if (field.getType() == int.class) return Integer.valueOf(value);
            if (field.getType() == long.class) return Long.valueOf(value);
            if (field.getType() == float.class) return Float.valueOf(value);
            if (field.getType() == double.class) return Double.valueOf(value);
            throw new IllegalArgumentException("Unsupported field type");
        }

        public void applyRuntime(Object value) {
            if (!live) return;
            try {
                field.set(null, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Rejects malformed booleans, non-finite numbers, fractions for integers and out-of-range values.
     *
     * @param property authoritative schema
     * @param value    submitted scalar
     */
    public static void validate(Property property, String value) {
        if (property == null || value == null || value.length() > 128) throw new IllegalArgumentException();
        if (property.getType() == Property.Type.BOOLEAN) {
            if (!"true".equals(value) && !"false".equals(value)) throw new IllegalArgumentException();
            return;
        }
        double number;
        if (property.getType() == Property.Type.INTEGER) {
            number = Integer.parseInt(value);
        } else if (property.getType() == Property.Type.DOUBLE) {
            number = Double.parseDouble(value);
        } else {
            throw new IllegalArgumentException();
        }
        if (!Double.isFinite(number) || number < Double.parseDouble(property.getMinValue())
            || number > Double.parseDouble(property.getMaxValue())) throw new IllegalArgumentException();
    }
}
