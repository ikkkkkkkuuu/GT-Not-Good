package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.ae2thing.quickterminal.InterfacePatternTarget;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEStackType;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.core.AEConfig;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate.PacketEntry;
import appeng.util.item.AEItemStack;

final class InterfaceRowFilterClientChecks {

    private static final long ENTRY_ID = 9878L;
    private static final String ENTRY_NAME = "Row Filter QA";
    private static final String[] SEARCH_FIELDS = { "searchFieldInputs", "searchFieldOutputs", "searchFieldNames" };

    static void run(Object terminal) throws Exception {
        boolean previousFilter = AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal;
        String[] previousSearch = new String[SEARCH_FIELDS.length];
        for (int i = 0; i < SEARCH_FIELDS.length; i++) {
            previousSearch[i] = ((MEGuiTextField) value(terminal, SEARCH_FIELDS[i])).getText();
        }
        try {
            call(terminal, "setNameSearchText", new Class<?>[] { String.class }, ENTRY_NAME);
            AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal = true;
            add(terminal, 27, 17);
            Object entry = entry(terminal);
            InterfacePatternRows rows = (InterfacePatternRows) entry;
            checkRows(rows, 2, 37);
            checkListHeight(terminal, 49);
            checkRenderedRows(terminal, entry, 1, 2);
            require(rows.getVisibleSlot(0) == -1 && rows.getVisibleSlot(18) == 9,
                "earlier full rows hide while the last full row remains");
            screenshot("interface-rows-partial-and-last-full.png");

            AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal = false;
            markDirty(terminal);
            checkRows(rows, 3, 55);
            checkListHeight(terminal, 67);
            require(draw(terminal, entry, -10000, -10000) == 55, "disabled filter renders all three rows");
            require(rows.getActualSlot(0) == 0 && rows.getActualSlot(26) == 26, "disabled filter retains slot order");

            AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal = true;
            markDirty(terminal);
            call(terminal, "freezeSearchResults", new Class<?>[0]);
            updateSlot(terminal, 17, stack(17));
            checkRows(rows, 1, 19);
            checkListHeight(terminal, 31);
            checkRenderedRows(terminal, entry, 2);
            require(!visibleSections(terminal).isEmpty(), "a full interface retains its last full row");
            updateSlot(terminal, 4, null);
            checkRows(rows, 2, 37);
            checkListHeight(terminal, 49);
            checkRenderedRows(terminal, entry, 0, 2);

            remove(terminal);
            add(terminal, 23, 22);
            entry = entry(terminal);
            rows = (InterfacePatternRows) entry;
            checkRows(rows, 2, 37);
            checkRenderedRows(terminal, entry, 1, 2);
            require(rows.getActualSlot(14) == -1, "partial final row has no synthetic free slots");
            require(targetAt(terminal, entry, 14) == null, "padding in the final row cannot be clicked");
            updateSlot(terminal, 22, stack(22));
            checkRows(rows, 1, 19);
            checkRenderedRows(terminal, entry, 2);
            require(!visibleSections(terminal).isEmpty(), "a filled partial final row remains visible");

            for (int capacity : new int[] { 9, 18, 27, 9 }) {
                overwriteShape(terminal, capacity);
                int count = capacity == 9 ? 1 : 2;
                checkRows(rows, count, count * 18 + 1);
                checkListHeight(terminal, count * 18 + 13);
                if (capacity == 9) checkRenderedRows(terminal, entry, 0);
                else checkRenderedRows(terminal, entry, capacity / 9 - 2, capacity / 9 - 1);
                require(rows.getActualSlot(count * 9) == -1, "expanded rows do not leave stale visible mappings");
                require(rows.getVisibleSlot(capacity) == -1, "shrunk rows reject previous inventory slots");
            }
            checkSequentialFilling(terminal);
            checkFrozenRecipeSearch(terminal);
        } finally {
            remove(terminal);
            AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal = previousFilter;
            call(terminal, "clearFrozenSearchResults", new Class<?>[0]);
            for (int i = 0; i < SEARCH_FIELDS.length; i++) {
                ((MEGuiTextField) value(terminal, SEARCH_FIELDS[i])).setText(previousSearch[i]);
            }
            markDirty(terminal);
            call(terminal, "rememberSearchText", new Class<?>[0]);
        }
        System.out.println(
            "TERMINAL_ROW_FILTER_QA: full rows, renderer, slot mapping, toggle, updates, final row and resizing PASS");
    }

    private static void checkRows(InterfacePatternRows rows, int count, int height) {
        require(rows.getVisibleRowCount() == count, "visible row count " + count);
        require(rows.getVisibleHeight() == height, "visible entry height " + height);
    }

    private static void checkListHeight(Object terminal, int expected) throws Exception {
        Object master = value(terminal, "masterList");
        require((int) call(master, "getHeight", new Class<?>[0]) == expected, "scroll height " + expected);
    }

    private static void checkRenderedRows(Object terminal, Object entry, int... actualRows) throws Exception {
        InterfacePatternRows rows = (InterfacePatternRows) entry;
        int numSlots = (int) value(entry, "numSlots");
        for (int row = 0; row < actualRows.length; row++) {
            for (int column = 0; column < 9 && actualRows[row] * 9 + column < numSlots; column++) {
                int actualSlot = actualRows[row] * 9 + column;
                int visibleSlot = row * 9 + column;
                field(entry, "hoveredSlotIdx").setInt(entry, -1);
                require(draw(terminal, entry, 16 + column * 18, row * 18 + 5) == actualRows.length * 18 + 1,
                    "compressed renderer height");
                require((int) value(entry, "hoveredSlotIdx") == actualSlot, "rendered original slot " + actualSlot);
                InterfacePatternTarget target = targetAt(terminal, entry, visibleSlot);
                require(target != null && target.getEntryId() == ENTRY_ID && target.getSlot() == actualSlot,
                    "clicked original slot " + actualSlot);
                require(rows.getActualSlot(visibleSlot) == actualSlot, "visible-to-actual slot " + actualSlot);
                require(rows.getVisibleSlot(actualSlot) == visibleSlot, "actual-to-visible slot " + actualSlot);
            }
        }
    }

    private static InterfacePatternTarget targetAt(Object terminal, Object entry, int visibleSlot) throws Exception {
        int x = (int) value(terminal, "guiLeft") + 10 + 16 + visibleSlot % 9 * 18;
        int y = (int) value(terminal, "guiTop") + 52 + (int) value(entry, "dispY") + visibleSlot / 9 * 18 + 5;
        return (InterfacePatternTarget) call(terminal, "patternSlotAt", new Class<?>[] { int.class, int.class }, x, y);
    }

    private static int draw(Object terminal, Object entry, int mouseX, int mouseY) throws Exception {
        Method draw = GuiInterfaceTerminal.class.getDeclaredMethod("drawEntry", entry.getClass(), int.class, int.class,
            int.class, int.class);
        draw.setAccessible(true);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            return (int) draw.invoke(terminal, entry, 0, 0, mouseX, mouseY);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void add(Object terminal, int numSlots, int emptySlot) throws Exception {
        NBTTagList items = new NBTTagList();
        for (int slot = 0; slot < 27; slot++) {
            items.appendTag(slot >= numSlots || slot == emptySlot ? new NBTTagCompound()
                : stack(slot).writeToNBT(new NBTTagCompound()));
        }
        PacketInterfaceTerminalUpdate update = new PacketInterfaceTerminalUpdate();
        update.addNewEntry(ENTRY_ID, ENTRY_NAME, true).setTerminalVisible(true).setItems(3, 9, numSlots, items)
            .setSupportedStackTypes(new IAEStackType<?>[0]).setReps(new ItemStack(Items.apple), null);
        post(terminal, update);
    }

    private static ItemStack stack(int slot) {
        ItemStack stack = new ItemStack(Items.apple);
        stack.setStackDisplayName("Row QA " + slot);
        return stack;
    }

    private static void updateSlot(Object terminal, int slot, ItemStack stack) throws Exception {
        NBTTagList items = new NBTTagList();
        items.appendTag(stack == null ? new NBTTagCompound() : stack.writeToNBT(new NBTTagCompound()));
        PacketInterfaceTerminalUpdate update = new PacketInterfaceTerminalUpdate();
        update.addOverwriteEntry(ENTRY_ID).setItems(new int[] { slot }, items);
        post(terminal, update);
    }

    private static void overwriteShape(Object terminal, int capacity) throws Exception {
        NBTTagList items = new NBTTagList();
        for (int slot = 0; slot < capacity; slot++) {
            items.appendTag(slot == capacity - 1 ? new NBTTagCompound() : stack(slot).writeToNBT(new NBTTagCompound()));
        }
        PacketInterfaceTerminalUpdate update = new PacketInterfaceTerminalUpdate();
        update.addOverwriteEntry(ENTRY_ID).setItems(null, items).setSize(capacity / 9, 9, capacity);
        post(terminal, update);
    }

    private static void checkSequentialFilling(Object terminal) throws Exception {
        NBTTagList items = new NBTTagList();
        for (int slot = 0; slot < 27; slot++) items.appendTag(new NBTTagCompound());
        PacketInterfaceTerminalUpdate update = new PacketInterfaceTerminalUpdate();
        update.addOverwriteEntry(ENTRY_ID).setItems(null, items).setSize(3, 9, 27);
        post(terminal, update);
        Object entry = entry(terminal);
        InterfacePatternRows rows = (InterfacePatternRows) entry;
        checkRows(rows, 3, 55);
        for (int row = 0; row < 3; row++) {
            for (int slot = row * 9; slot < (row + 1) * 9; slot++) updateSlot(terminal, slot, stack(slot));
            int count = 3 - row;
            checkRows(rows, count, count * 18 + 1);
            if (row == 0) checkRenderedRows(terminal, entry, 0, 1, 2);
            else if (row == 1) checkRenderedRows(terminal, entry, 1, 2);
            else checkRenderedRows(terminal, entry, 2);
            require(!visibleSections(terminal).isEmpty(), "sequential filling retains the latest full row");
        }
        screenshot("interface-rows-last-full.png");
    }

    private static void checkFrozenRecipeSearch(Object terminal) throws Exception {
        ItemStack pattern = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        NBTTagCompound patternTag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList(), outputs = new NBTTagList();
        NBTTagCompound input = new NBTTagCompound(), output = new NBTTagCompound();
        AEItemStack.create(new ItemStack(Items.apple)).writeToNBTGeneric(input);
        AEItemStack.create(new ItemStack(Items.feather)).writeToNBTGeneric(output);
        inputs.appendTag(input);
        outputs.appendTag(output);
        patternTag.setTag("in", inputs);
        patternTag.setTag("out", outputs);
        patternTag.setBoolean("crafting", false);
        pattern.setTagCompound(patternTag);
        NBTTagList items = new NBTTagList();
        items.appendTag(pattern.writeToNBT(new NBTTagCompound()));
        for (int slot = 1; slot < 9; slot++) items.appendTag(new NBTTagCompound());
        PacketInterfaceTerminalUpdate update = new PacketInterfaceTerminalUpdate();
        update.addOverwriteEntry(ENTRY_ID).setItems(null, items).setSize(1, 9, 9);
        post(terminal, update);
        call(terminal, "setNameSearchText", new Class<?>[] { String.class }, ENTRY_NAME);
        ((MEGuiTextField) value(terminal, "searchFieldInputs")).setText(new ItemStack(Items.apple).getDisplayName());
        require(!visibleSections(terminal).isEmpty(), "localized input search finds the Ultimate Encoded Pattern");
        call(terminal, "freezeSearchResults", new Class<?>[0]);
        require(value(terminal, "frozenSearchResults") != null,
            "recipe search snapshot is retained while folding rows");
        updateSlot(terminal, 0, null);
        require(value(terminal, "frozenSearchResults") != null,
            "removing the last matching pattern retains the snapshot");
        require(!visibleSections(terminal).isEmpty(), "removing the last match preserves the searched interface");
        checkListHeight(terminal, 31);
        Object entry = entry(terminal);
        checkRenderedRows(terminal, entry, 0);
        InterfacePatternTarget target = targetAt(terminal, entry, 0);
        require(target != null && target.getSlot() == 0, "the emptied matching slot remains available for replacement");
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        } finally {
            GL11.glPopAttrib();
        }
        mc.currentScreen.drawScreen(-10000, -10000, 0);
        ScreenShotHelper.saveScreenshot(new File(System.getProperty("gtng.terminalScroll.qa.output")), name,
            mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void remove(Object terminal) throws Exception {
        PacketInterfaceTerminalUpdate update = new PacketInterfaceTerminalUpdate();
        update.addRemovalEntry(ENTRY_ID);
        post(terminal, update);
    }

    @SuppressWarnings("unchecked")
    private static void post(Object terminal, PacketInterfaceTerminalUpdate update) throws Exception {
        ((GuiInterfaceTerminal) terminal).postUpdate((List<PacketEntry>) value(update, "commands"), 0);
    }

    private static Object entry(Object terminal) throws Exception {
        Object entries = value(value(terminal, "masterList"), "list");
        Object entry = ((Map<?, ?>) entries).get(ENTRY_ID);
        require(entry != null, "row filter fixture is registered");
        return entry;
    }

    private static List<?> visibleSections(Object terminal) throws Exception {
        return (List<?>) call(value(terminal, "masterList"), "getVisibleSections", new Class<?>[0]);
    }

    private static void markDirty(Object terminal) throws Exception {
        call(value(terminal, "masterList"), "markDirty", new Class<?>[0]);
    }

    private static Object value(Object owner, String name) throws Exception {
        return field(owner, name).get(owner);
    }

    private static Field field(Object owner, String name) throws Exception {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static Object call(Object owner, String name, Class<?>[] parameters, Object... arguments) throws Exception {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Method method = type.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method.invoke(owner, arguments);
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
