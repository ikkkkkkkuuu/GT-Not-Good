package com.xyp.gtnotgood.common.items.wildcard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardExpansion;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.SimpleFilterComponent;

import gregtech.api.enums.Materials;

/** Compares allocation-light counting with expansion for rejected, missing and empty components. */
public class WildcardExpansionTest {

    @Test
    public void countsMatchExpansionAcrossFiltersAndUnavailableForms() {
        IWildcardIOComponent valid = new TestComponent(false, false);
        IWildcardIOComponent unavailable = new TestComponent(false, true);
        IWildcardIOComponent empty = new TestComponent(true, true);
        List<IWildcardFilterComponent> ironOnly = Collections
            .singletonList(new SimpleFilterComponent(Materials.Iron, true));
        assertTrue(
            WildcardExpansion
                .countExpanded(Collections.singletonList(valid), Collections.singletonList(valid), ironOnly) > 0);
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

    private static void assertCount(List<IWildcardIOComponent> inputs, List<IWildcardIOComponent> outputs,
        List<IWildcardFilterComponent> filters, int expected) {
        assertEquals(
            expected,
            WildcardExpansion.expand(inputs, outputs, filters)
                .size());
        assertEquals(expected, WildcardExpansion.countExpanded(inputs, outputs, filters));
    }

    /** A predictable component independent of registered ore forms; missing forms return null. */
    private static final class TestComponent implements IWildcardIOComponent {

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
}
