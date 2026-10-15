package com.xyp.gtnotgood.client.radialmenu.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.json.ConfigCodec;

public class BuiltinMenuPersistenceTest {

    @Test
    public void liveInventoryIsExcludedWhileBuiltinIdentityAndLayoutSurvive() {
        Profile profile = profile();
        MenuNode belt = profile.root.childAt(0);
        MenuNode liveTool = tool("server-only-enchanted-tool");
        belt.liveChildren = Collections.singletonList(liveTool);
        assertSame(liveTool, belt.childAt(0));

        String json = ConfigCodec.writeProfile(profile);
        assertFalse(json.contains("server-only-enchanted-tool"));
        assertFalse(json.contains("server-only-item"));
        assertFalse(json.contains("liveChildren"));

        Profile loaded = ConfigCodec.readProfile(json);
        MenuNode loadedBelt = loaded.root.childAt(0);
        assertEquals("toolbelt", loadedBelt.builtIn);
        assertTrue(loadedBelt.isBuiltIn());
        assertTrue(loadedBelt.isCategory());
        assertTrue(loadedBelt.children.isEmpty());
        assertNull(loadedBelt.liveChildren);
        assertEquals(10, loadedBelt.slotCount());
        assertEquals(SlotLayout.Opening.Inline, loadedBelt.layout.opening);
        assertEquals("key.inventory", loaded.root.childAt(2).action.getString("binding", null));
    }

    @Test
    public void refreshingInventoryAndNormalizingDoesNotRewriteTheProfile() {
        Profile profile = profile();
        MenuNode belt = profile.root.childAt(0);
        String saved = ConfigCodec.writeProfile(profile);

        belt.liveChildren = new ArrayList<>();
        for (int slot = 0; slot < 10; slot++) belt.liveChildren.add(tool("server-tool-" + slot));
        profile.normalize();
        assertEquals(saved, ConfigCodec.writeProfile(profile));
        assertEquals(saved, ConfigCodec.writeProfile(copy(profile)));

        belt.liveChildren = Collections.singletonList(tool("different-server-tool"));
        profile.normalize();
        assertEquals(saved, ConfigCodec.writeProfile(profile));
    }

    @Test
    public void reloadedBuiltinKeepsItsSlotMutationProtection() {
        MenuNode belt = ConfigCodec.readProfile(ConfigCodec.writeProfile(profile())).root.childAt(0);
        MenuNode liveTool = tool("preserved-server-tool");
        belt.liveChildren = new ArrayList<>(Collections.singletonList(liveTool));

        belt.setChildAt(0, tool("editor-replacement"));
        belt.ensureSlotCapacity();
        assertFalse(belt.moveChild(0, 1));
        assertSame(liveTool, belt.childAt(0));
        assertTrue(belt.children.isEmpty());
    }

    private static Profile profile() {
        Profile profile = Profile.empty("builtin-persistence");
        MenuNode belt = MenuNode.category("Tool Belt", IconSpec.sprite("wrench", null), SlotLayout.fixed(10));
        belt.builtIn = "toolbelt";
        belt.layout.opening = SlotLayout.Opening.Inline;
        profile.root.setChildAt(0, belt);
        profile.root.setChildAt(2,
            MenuNode.leaf("Inventory", null, new ActionSpec("keybind").set("binding", "key.inventory")));
        return profile;
    }

    private static MenuNode tool(String title) {
        MenuNode tool = MenuNode.leaf(title, IconSpec.item("test:server-only-item", 23),
            new ActionSpec("toolbelt").set("slot", "0"));
        tool.builtIn = "toolbelt:0";
        return tool;
    }

    private static Profile copy(Profile source) {
        Profile copy = Profile.empty(source.name);
        copy.root = source.root.copy();
        return copy;
    }
}
