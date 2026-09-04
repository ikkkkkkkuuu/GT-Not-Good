package com.xyp.gtnotgood.ae2thing.util;

import static com.glodblock.github.util.Ae2Reflect.readField;
import static com.glodblock.github.util.Ae2Reflect.reflectField;
import static com.glodblock.github.util.Ae2Reflect.reflectMethod;
import static com.glodblock.github.util.Ae2Reflect.writeField;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.glodblock.github.client.gui.GuiFluidInterface;
import com.glodblock.github.client.gui.container.ContainerFluidInterface;
import com.glodblock.github.inventory.IDualHost;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.me.ItemRepo;
import codechicken.nei.SearchField;
import codechicken.nei.util.TextHistory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Ae2ReflectClient {

    private static final Field fGuiCraftingStatus_icon;
    private static final Field fGuiCraftingStatus_originalGuiBtn;
    private static final Field fGui_drag;
    private static final Field fSearchField_history;
    private static final Field fTextHistory_history;
    private static final Field fItemRepo_view;
    private static final Field fItemRepo_list;
    private static final Field fGuiFluidInterface_cont;
    private static final Method mGui_inventorySlots;

    static {
        try {
            fGuiCraftingStatus_icon = findOptionalField(GuiCraftingStatus.class, "myIcon");
            fGuiCraftingStatus_originalGuiBtn = reflectField(GuiCraftingStatus.class, "originalGuiBtn");
            fGui_drag = reflectFirstField(AEBaseGui.class, "draggedSlots", "drag_click");
            mGui_inventorySlots = reflectMethod(AEBaseGui.class, "getInventorySlots");
            fItemRepo_view = reflectField(ItemRepo.class, "view");
            fItemRepo_list = reflectField(ItemRepo.class, "list");
            fGuiFluidInterface_cont = reflectField(GuiFluidInterface.class, "cont");
            fSearchField_history = reflectField(SearchField.class, "history");
            fTextHistory_history = reflectField(TextHistory.class, "history");
        } catch (NoSuchFieldException | NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException("Failed to initialize AE2 reflection hacks!", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Slot> getInventorySlots(AEBaseGui gui) {
        try {
            return (List<Slot>) mGui_inventorySlots.invoke(gui);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke method: " + mGui_inventorySlots, e);
        }
    }

    public static void rewriteIcon(GuiCraftingStatus gui, ItemStack icon) {
        if (fGuiCraftingStatus_icon != null) {
            writeField(gui, fGuiCraftingStatus_icon, icon);
        }
    }

    public static GuiTabButton getOriginalGuiButton(GuiCraftingStatus gui) {
        return readField(gui, fGuiCraftingStatus_originalGuiBtn);
    }

    public static Set<Slot> getDragClick(AEBaseGui gui) {
        return readField(gui, fGui_drag);
    }

    public static TextHistory getHistory(SearchField searchField) {
        return readField(searchField, fSearchField_history);
    }

    public static List<String> getHistoryList(TextHistory textHistory) {
        return readField(textHistory, fTextHistory_history);
    }

    public static ArrayList<IAEStack<?>> getView(ItemRepo repo) {
        return readField(repo, fItemRepo_view);
    }

    public static IItemList<IAEStack<?>> getList(ItemRepo repo) {
        return readField(repo, fItemRepo_list);
    }

    public static IDualHost getHost(GuiFluidInterface gui) {
        ContainerFluidInterface container = readField(gui, fGuiFluidInterface_cont);
        return container == null ? null : container.getTile();
    }

    private static Field findOptionalField(Class<?> type, String name) throws SecurityException {
        try {
            return reflectField(type, name);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static Field reflectFirstField(Class<?> type, String... names) throws NoSuchFieldException {
        NoSuchFieldException missing = null;
        for (String name : names) {
            try {
                return reflectField(type, name);
            } catch (NoSuchFieldException e) {
                if (missing == null) {
                    missing = e;
                }
            }
        }
        throw missing == null ? new NoSuchFieldException(type.getName()) : missing;
    }

}
