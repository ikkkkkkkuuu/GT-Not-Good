package com.xyp.gtnotgood.common.items.wildcard.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials.Property;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.PropertyFilterComponent;

public class PropertyFilterCompatibilityTest {

    @Test
    public void preservesLegacyPropertyNamesInSavedFilters() {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setString("Property", "TOOL_HEAD");
        saved.setBoolean("Whitelist", false);

        PropertyFilterComponent filter = PropertyFilterComponent.readData(saved);

        assertEquals("-TOOL_HEAD", filter.describe());
        assertFalse(filter.isWhitelist());
        assertEquals(saved, filter.writeData());
        assertEquals("FLUID_PIPE",
            new PropertyFilterComponent(Property.FluidPipe, null, true).writeData().getString("Property"));
    }

    @Test
    public void preservesPropertyParsingAndDisplayAliases() {
        assertEquals(Property.ToolHead, WildcardMaterials.findProperty(" tool_head "));
        assertEquals(Property.FluidPipe, WildcardMaterials.findProperty("fluid_pipe"));
        assertEquals("tool", Property.ToolHead.displayName());
        assertEquals("ingot", Property.Metal.displayName());
        assertEquals("fluid_pipe", Property.FluidPipe.displayName());
        assertNull(WildcardMaterials.findProperty(null));
        assertNull(WildcardMaterials.findProperty("unknown"));
    }
}
