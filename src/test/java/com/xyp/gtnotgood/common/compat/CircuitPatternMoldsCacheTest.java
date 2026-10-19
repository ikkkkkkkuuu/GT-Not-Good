package com.xyp.gtnotgood.common.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.recipe.RecipeCategory;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.util.GTRecipe;
import sun.misc.Unsafe;

public class CircuitPatternMoldsCacheTest {

    private static Unsafe unsafe;
    private static Method invalidateChangedRecipes;
    private static Field recipeCount;
    private static Field mapCount;
    private static Map<ICraftingPatternDetails, ItemStack[]> cache;
    private Map<String, RecipeMap<?>> savedMaps;
    private Map<ICraftingPatternDetails, ItemStack[]> savedCache;
    private int savedRecipeCount;
    private int savedMapCount;

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void accessCache() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);
        invalidateChangedRecipes = CircuitPatternMolds.class.getDeclaredMethod("invalidateChangedRecipes");
        invalidateChangedRecipes.setAccessible(true);
        recipeCount = field(CircuitPatternMolds.class, "cachedRecipeCount");
        mapCount = field(CircuitPatternMolds.class, "cachedMapCount");
        cache = (Map<ICraftingPatternDetails, ItemStack[]>) field(CircuitPatternMolds.class, "cache").get(null);
    }

    @Before
    public void isolateRegistries() throws Exception {
        savedMaps = new HashMap<>(RecipeMap.ALL_RECIPE_MAPS);
        savedCache = new HashMap<>(cache);
        savedRecipeCount = recipeCount.getInt(null);
        savedMapCount = mapCount.getInt(null);
        RecipeMap.ALL_RECIPE_MAPS.clear();
        cache.clear();
        recipeCount.setInt(null, -1);
        mapCount.setInt(null, -1);
    }

    @After
    public void restoreRegistries() throws Exception {
        RecipeMap.ALL_RECIPE_MAPS.clear();
        RecipeMap.ALL_RECIPE_MAPS.putAll(savedMaps);
        cache.clear();
        cache.putAll(savedCache);
        recipeCount.setInt(null, savedRecipeCount);
        mapCount.setInt(null, savedMapCount);
    }

    @Test
    public void fluidOnlyPatternsNeverInspectRecipeRegistries() throws Exception {
        CountingBackend backend = backend("unused");
        backend.forbidCategories = true;
        IAEFluidStack fluid = (IAEFluidStack) Proxy.newProxyInstance(IAEFluidStack.class.getClassLoader(),
            new Class<?>[] { IAEFluidStack.class },
            (proxy, method, args) -> {
                throw new AssertionError("Mold-free input should not need decoding: " + method.getName());
            });
        ICraftingPatternDetails pattern = pattern(fluid);
        for (int i = 0; i < 900; i++) assertEquals(0, CircuitPatternMolds.resolve(pattern).length);
        assertEquals(-1, recipeCount.getInt(null));
    }

    @Test
    public void cacheValidationReadsCategorySizesWithoutEnumeratingRecipes() throws Exception {
        CountingBackend backend = backend("static");
        SizeOnlyRecipes first = recipes(backend, 50_000);
        SizeOnlyRecipes second = recipes(backend, 25_000);
        invalidateChangedRecipes.invoke(null);
        assertEquals(75_000, recipeCount.getInt(null));
        assertEquals(1, first.sizeReads);
        assertEquals(1, second.sizeReads);
        invalidateChangedRecipes.invoke(null);
        assertEquals(2, first.sizeReads);
        assertEquals(2, second.sizeReads);
    }

    @Test
    public void recipeAdditionsRemovalsAndMapChangesInvalidateCachedResults() throws Exception {
        CountingBackend backend = backend("static");
        SizeOnlyRecipes recipes = recipes(backend, 1);
        invalidateChangedRecipes.invoke(null);
        ICraftingPatternDetails pattern = pattern();
        cache.put(pattern, new ItemStack[0]);
        invalidateChangedRecipes.invoke(null);
        assertTrue(cache.containsKey(pattern));

        recipes.count++;
        invalidateChangedRecipes.invoke(null);
        assertFalse(cache.containsKey(pattern));
        cache.put(pattern, new ItemStack[0]);
        recipes.count--;
        invalidateChangedRecipes.invoke(null);
        assertFalse(cache.containsKey(pattern));

        cache.put(pattern, new ItemStack[0]);
        CountingBackend dynamic = backend("dynamic");
        dynamic.dynamic = true;
        dynamic.forbidCategories = true;
        invalidateChangedRecipes.invoke(null);
        assertFalse(cache.containsKey(pattern));
        assertEquals(1, recipeCount.getInt(null));
        cache.put(pattern, new ItemStack[0]);
        RecipeMap.ALL_RECIPE_MAPS.remove("dynamic");
        invalidateChangedRecipes.invoke(null);
        assertFalse(cache.containsKey(pattern));
    }

    private static CountingBackend backend(String name) throws Exception {
        // RecipeMap's GUI constructor requires a live Forge loader; only native category storage is needed here.
        CountingBackend backend = (CountingBackend) unsafe.allocateInstance(CountingBackend.class);
        field(RecipeMapBackend.class, "recipesByCategory").set(backend, new HashMap<>());
        RecipeMap<?> map = (RecipeMap<?>) unsafe.allocateInstance(RecipeMap.class);
        field(RecipeMap.class, "backend").set(map, backend);
        RecipeMap.ALL_RECIPE_MAPS.put(name, map);
        return backend;
    }

    private static SizeOnlyRecipes recipes(CountingBackend backend, int count) throws Exception {
        RecipeCategory category = (RecipeCategory) unsafe.allocateInstance(RecipeCategory.class);
        SizeOnlyRecipes recipes = new SizeOnlyRecipes(count);
        @SuppressWarnings("unchecked")
        Map<RecipeCategory, Collection<GTRecipe>> categories = (Map<RecipeCategory, Collection<GTRecipe>>) field(
            RecipeMapBackend.class, "recipesByCategory").get(backend);
        categories.put(category, recipes);
        return recipes;
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static ICraftingPatternDetails pattern(IAEStack<?>... inputs) {
        return (ICraftingPatternDetails) Proxy.newProxyInstance(ICraftingPatternDetails.class.getClassLoader(),
            new Class<?>[] { ICraftingPatternDetails.class }, (proxy, method, args) -> switch (method.getName()) {
            case "getAEInputs" -> inputs;
            case "isCraftable", "isInputOnly" -> false;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError(method.getName());
            });
    }

    private static final class CountingBackend extends RecipeMapBackend {

        private boolean dynamic;
        private boolean forbidCategories;

        private CountingBackend() {
            super(null);
        }

        @Override
        public boolean doesOverwriteFindRecipe() {
            return dynamic;
        }

        @Override
        public Collection<GTRecipe> getAllRecipes() {
            throw new AssertionError("Cache validation must not flatten recipe lists");
        }

        @Override
        public Map<RecipeCategory, Collection<GTRecipe>> getRecipeCategoryMap() {
            if (forbidCategories) throw new AssertionError("Unexpected recipe registry access");
            return super.getRecipeCategoryMap();
        }
    }

    private static final class SizeOnlyRecipes extends AbstractCollection<GTRecipe> {

        private int count;
        private int sizeReads;

        private SizeOnlyRecipes(int count) {
            this.count = count;
        }

        @Override
        public int size() {
            sizeReads++;
            return count;
        }

        @Override
        public Iterator<GTRecipe> iterator() {
            throw new AssertionError("Cache validation must not enumerate recipes");
        }
    }
}
