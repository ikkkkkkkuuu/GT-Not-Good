package com.xyp.gtnotgood.utils.machine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;
import com.xyp.gtnotgood.utils.machine.VoidDrillerFluidPool.Entry;

import cpw.mods.fml.common.Loader;
import gregtech.api.objects.GTUODimension;
import gregtech.api.objects.GTUODimensionList;
import sun.misc.Unsafe;

public class VoidDrillerFluidPoolTest {

    @BeforeClass
    public static void registerFluids() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        if (loader.get(null) == null) loader.set(null, ((Unsafe) unsafeField.get(null)).allocateInstance(Loader.class));
        HeadlessBlockRegistry.bootstrap();
        for (String name : Arrays.asList("light", "heavy", "negative", "inverted", "empty", "disabled")) {
            FluidRegistry.registerFluid(new Fluid("void_driller_test_" + name));
        }
    }

    @Test
    public void rejectsInvalidDepositsBeforeNormalizingChance() throws Exception {
        ConfigCategory category = new ConfigCategory("test");
        addFluid(category, "light", 0, 100, 10);
        addFluid(category, "heavy", 10, 50, 30);
        addFluid(category, "negative", -1, 100, 1000);
        addFluid(category, "inverted", 100, 10, 1000);
        addFluid(category, "empty", 0, 0, 1000);
        addFluid(category, "disabled", 0, 100, 0);
        addFluid(category, "missing", 0, 100, 1000);
        List<Entry> entries = underground(new GTUODimension(category), "Ow");
        assertEquals(2, entries.size());
        Entry light = entries.stream().filter(entry -> entry.fluidName.endsWith("light")).findFirst().get();
        Entry heavy = entries.stream().filter(entry -> entry.fluidName.endsWith("heavy")).findFirst().get();
        assertEquals(0.25, light.weight, 0.000001);
        assertEquals(0.75, heavy.weight, 0.000001);
        assertEquals(0, light.minAmount);
        assertEquals(100, light.maxAmount);
    }

    @Test
    public void oceansShareTheirDimensionWeightWithoutDilutingOtherDimensions() throws Exception {
        ConfigCategory category = new ConfigCategory("test");
        addFluid(category, "light", 0, 100, 10);
        addFluid(category, "heavy", 10, 50, 30);
        List<Entry> entries = underground(new GTUODimension(category), "Ow");
        Method oceans = VoidDrillerFluidPool.class.getDeclaredMethod("appendOceans", List.class, Set.class);
        oceans.setAccessible(true);
        oceans.invoke(null, entries, new HashSet<>(Arrays.asList("ow", "ne")));
        Method normalize = VoidDrillerFluidPool.class.getDeclaredMethod("normalizeEntries", List.class);
        normalize.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Entry> result = (List<Entry>) normalize.invoke(null, entries);
        assertEquals(4, result.size());
        assertEquals(1,
            result.stream().filter(entry -> entry.dimension.equals("Ow")).mapToDouble(entry -> entry.weight).sum(),
            0.000001);
        Entry ocean = result.stream().filter(entry -> entry.fluidName.equals("water")).findFirst().get();
        Entry lava = result.stream().filter(entry -> entry.fluidName.equals("lava")).findFirst().get();
        assertEquals(0.5, ocean.weight, 0.000001);
        assertEquals(1, lava.weight, 0.000001);
        assertEquals(1000, ocean.minAmount);
        assertEquals(1000, ocean.maxAmount);
        assertTrue(lava.dimension.equals("Ne"));
    }

    @Test
    public void numericMoonConfigurationMatchesItsDisplayAbbreviation() throws Exception {
        GTUODimensionList configuration = new GTUODimensionList();
        GTUODimension numeric = configuredDimension("-28");
        GTUODimension named = configuredDimension("Moon");
        configuration.getDimensionList().put("-28", numeric);
        configuration.getDimensionList().put("Moon", named);
        Map<Integer, Object> registry = moonRegistry();
        Method resolve = VoidDrillerFluidPool.class.getDeclaredMethod("resolveDimensionLabel", String.class, Map.class);
        resolve.setAccessible(true);
        assertEquals("Mo", resolve.invoke(null, "-28", registry));
        assertSame(numeric, selectedDimension(configuration, "Mo", registry));
        assertSame(numeric, selectedDimension(configuration, "mo", registry));
    }

    @Test
    public void namedMoonConfigurationCannotBypassItsRealDimensionBlacklist() throws Exception {
        GTUODimensionList configuration = new GTUODimensionList();
        GTUODimension named = configuredDimension("Moon");
        configuration.getDimensionList().put("Moon", named);
        configuration.blackList = new int[] { -28 };
        assertNull(selectedDimension(configuration, "Mo", moonRegistry()));
        configuration.blackList = new int[] { 0 };
        assertSame(named, selectedDimension(configuration, "Mo", moonRegistry()));
        assertEquals(0, configuration.blackList[0]);
    }

    @Test
    public void netherAbbreviationDoesNotMatchNeperProviderPrefix() throws Exception {
        Map<Integer, Object> registry = new LinkedHashMap<>();
        registry.put(101, registeredDimension(101, "amunra.world.neper.WorldProviderNeper", "Np"));
        registry.put(-1, registeredDimension(-1, "net.minecraft.world.WorldProviderHell", "Ne"));
        assertEquals("Ne", resolveAlias("Nether", "Ne", registry));
    }

    @Test
    public void marsAbbreviationDoesNotMatchMakemakeProviderPrefix() throws Exception {
        Map<Integer, Object> registry = new LinkedHashMap<>();
        registry.put(102, registeredDimension(102, "galaxyspace.makemake.dimension.WorldProviderMakemake", "MM"));
        registry.put(-29, registeredDimension(-29, "galacticraft.mars.dimension.WorldProviderMars", "Ma"));
        assertEquals("Ma", resolveAlias("Mars", "Ma", registry));
    }

    private static Object resolveAlias(String name, String abbr, Map<Integer, Object> registry) throws Exception {
        Method alias = VoidDrillerFluidPool.class.getDeclaredMethod("addAlias", String.class, String.class);
        alias.setAccessible(true);
        alias.invoke(null, name, abbr);
        Method resolve = VoidDrillerFluidPool.class.getDeclaredMethod("resolveDimensionLabel", String.class, Map.class);
        resolve.setAccessible(true);
        return resolve.invoke(null, abbr, registry);
    }

    private static GTUODimension configuredDimension(String name) {
        ConfigCategory category = new ConfigCategory("test");
        category.put("Dimension", new Property("Dimension", name, Property.Type.STRING));
        addFluid(category, "light", 0, 100, 10);
        return new GTUODimension(category);
    }

    private static Map<Integer, Object> moonRegistry() throws Exception {
        Map<Integer, Object> registry = new LinkedHashMap<>();
        registry.put(-28,
            registeredDimension(-28, "micdoodle8.mods.galacticraft.core.dimension.WorldProviderMoon", "Mo"));
        return registry;
    }

    private static Object registeredDimension(int id, String providerClass, String label) throws Exception {
        Class<?> type = Class.forName(VoidDrillerFluidPool.class.getName() + "$RegisteredDimension");
        Constructor<?> constructor = type.getDeclaredConstructor(int.class, String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(id, providerClass, label);
    }

    private static GTUODimension selectedDimension(GTUODimensionList configuration, String label,
        Map<Integer, Object> registry) throws Exception {
        Method selected = VoidDrillerFluidPool.class.getDeclaredMethod("getSelectedDimension", GTUODimensionList.class,
            String.class, Map.class);
        selected.setAccessible(true);
        return (GTUODimension) selected.invoke(null, configuration, label, registry);
    }

    private static List<Entry> underground(GTUODimension dimension, String label) throws Exception {
        Method append = VoidDrillerFluidPool.class.getDeclaredMethod("appendEntries", List.class, GTUODimension.class,
            String.class);
        append.setAccessible(true);
        List<Entry> entries = new ArrayList<>();
        append.invoke(null, entries, dimension, label);
        return entries;
    }

    private static void addFluid(ConfigCategory parent, String name, int minAmount, int maxAmount, int chance) {
        ConfigCategory category = new ConfigCategory(name, parent);
        category.put("Registry", new Property("Registry", "void_driller_test_" + name, Property.Type.STRING));
        category.put("MinAmount", new Property("MinAmount", Integer.toString(minAmount), Property.Type.INTEGER));
        category.put("MaxAmount", new Property("MaxAmount", Integer.toString(maxAmount), Property.Type.INTEGER));
        category.put("Chance", new Property("Chance", Integer.toString(chance), Property.Type.INTEGER));
    }
}
