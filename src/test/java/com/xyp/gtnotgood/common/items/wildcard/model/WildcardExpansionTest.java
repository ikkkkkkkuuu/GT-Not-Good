package com.xyp.gtnotgood.common.items.wildcard.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import com.xyp.gtnotgood.common.items.wildcard.model.filter.SimpleFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.StringFilterComponent;

import gregtech.api.enums.Materials;

/** Checks expansion with a fixed material list so JUnit does not need a running Forge registry. */
public class WildcardExpansionTest {

    private static final String ADDON_NAME = "WildcardAddonTestium";
    private static final List<String> MATERIAL_NAMES = Arrays.asList(Materials.Iron.mName, ADDON_NAME);

    @Test
    public void countsMatchExpansionAcrossFiltersAndUnavailableForms() {
        IWildcardIOComponent valid = new TestComponent(false, false);
        IWildcardIOComponent unavailable = new TestComponent(false, true);
        IWildcardIOComponent empty = new TestComponent(true, true);
        List<IWildcardFilterComponent> ironOnly = Collections
            .singletonList(new SimpleFilterComponent(Materials.Iron, true));
        assertTrue(
            WildcardExpansion.countExpanded(
                Collections.singletonList(valid),
                Collections.singletonList(valid),
                ironOnly,
                MATERIAL_NAMES,
                false) > 0);
        assertCount(Collections.singletonList(valid), Collections.singletonList(valid), ironOnly, 1);
        assertCount(Arrays.asList(null, empty, valid), Collections.singletonList(valid), ironOnly, 1);
        assertCount(Collections.singletonList(unavailable), Collections.singletonList(valid), ironOnly, 0);
        assertCount(Collections.singletonList(valid), Collections.singletonList(unavailable), ironOnly, 0);
        assertCount(Collections.singletonList(empty), Collections.emptyList(), ironOnly, 0);
        assertCount(Collections.emptyList(), Collections.singletonList(valid), ironOnly, 1);
        assertCount(Collections.singletonList(valid), Collections.emptyList(), ironOnly, 1);
        assertCount(
            Collections.singletonList(valid),
            Collections.singletonList(valid),
            Arrays.asList(
                new SimpleFilterComponent(Materials.Iron, true),
                new SimpleFilterComponent(Materials.Iron, false)),
            0);
        assertCount(null, Collections.singletonList(valid), ironOnly, 0);
    }

    @Test
    public void expandsAddonMaterialNameWithoutGtEntry() {
        Item ingot = new Item();
        Item dust = new Item();
        List<IWildcardIOComponent> inputs = Collections.singletonList(new AddonFormComponent(ingot, 2));
        List<IWildcardIOComponent> outputs = Collections.singletonList(new AddonFormComponent(dust, 1));
        List<IWildcardFilterComponent> filters = Collections
            .singletonList(new StringFilterComponent(ADDON_NAME, true, true));

        assertCount(inputs, outputs, filters, true, 1);
        WildcardExpansion.Expanded expanded = WildcardExpansion.expand(inputs, outputs, filters, MATERIAL_NAMES, true)
            .get(0);
        assertEquals(ADDON_NAME, expanded.materialName);
        assertEquals(
            ingot,
            expanded.inputs.get(0)
                .getItem());
        assertEquals(2, expanded.inputs.get(0).stackSize);
        assertEquals(
            dust,
            expanded.outputs.get(0)
                .getItem());
    }

    private static void assertCount(List<IWildcardIOComponent> inputs, List<IWildcardIOComponent> outputs,
        List<IWildcardFilterComponent> filters, int expected) {
        assertCount(inputs, outputs, filters, false, expected);
    }

    private static void assertCount(List<IWildcardIOComponent> inputs, List<IWildcardIOComponent> outputs,
        List<IWildcardFilterComponent> filters, boolean addonForms, int expected) {
        assertEquals(
            expected,
            WildcardExpansion.expand(inputs, outputs, filters, MATERIAL_NAMES, addonForms)
                .size());
        assertEquals(expected, WildcardExpansion.countExpanded(inputs, outputs, filters, MATERIAL_NAMES, addonForms));
    }

    private static class TestComponent implements IWildcardIOComponent {

        private final boolean empty;
        private final boolean unavailable;
        private final Item item = new Item();

        private TestComponent(boolean empty, boolean unavailable) {
            this.empty = empty;
            this.unavailable = unavailable;
        }

        @Override
        public ItemStack apply(Materials material) {
            return unavailable ? null : new ItemStack(item);
        }

        @Override
        public ItemStack getDisplayStack() {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }

        @Override
        public String typeKey() {
            return "test";
        }

        @Override
        public NBTTagCompound writeData() {
            return new NBTTagCompound();
        }
    }

    private static final class AddonFormComponent extends TestComponent {

        private final Item item;
        private final int amount;

        private AddonFormComponent(Item item, int amount) {
            super(false, false);
            this.item = item;
            this.amount = amount;
        }

        @Override
        public ItemStack apply(String materialName) {
            return ADDON_NAME.equals(materialName) ? new ItemStack(item, amount) : null;
        }
    }
}
