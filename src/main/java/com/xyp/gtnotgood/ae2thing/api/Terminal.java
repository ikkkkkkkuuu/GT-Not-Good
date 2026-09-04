package com.xyp.gtnotgood.ae2thing.api;

import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;

import com.xyp.gtnotgood.ae2thing.api.adapter.crafting.ICraftingTerminalAdapter;
import com.xyp.gtnotgood.ae2thing.api.adapter.pattern.IPatternTerminalAdapter;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.ITerminal;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.ITerminalHandler;
import com.xyp.gtnotgood.ae2thing.nei.ButtonConstants;
import com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config;
import com.xyp.gtnotgood.ae2thing.util.Util;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class Terminal {

    public static Terminal API = new Terminal();
    private static final HashSet<Class<? extends AEBaseGui>> terminal = new HashSet<>();
    private static final HashSet<Class<? extends AEBaseGui>> terminalBlackList = new HashSet<>();
    private static final HashMap<Class<? extends Container>, ICraftingTerminalAdapter> craftingTerminal = new HashMap<>();
    private static final IItemList<IAEItemStack> tracking = AEApi.instance()
        .storage()
        .createPrimitiveItemList();
    private static final HashMap<Class<? extends Container>, IPatternTerminalAdapter> patternTerminal = new HashMap<>();
    private static final HashMap<Class<? extends Item>, ITerminalHandler> terminalItem = new HashMap<>();
    private static final HashSet<ITerminal> terminalSet = new HashSet<>();

    public void registerTerminal(Class<? extends AEBaseGui> clazz) {
        terminal.add(clazz);
    }

    public HashSet<ITerminal> getTerminalSet() {
        return terminalSet;
    }

    public void registerTerminalSet(ITerminal iTerminal) {
        terminalSet.add(iTerminal);
    }

    public void registerTerminalBlackList(Class<? extends AEBaseGui> clazz) {
        terminalBlackList.add(clazz);
    }

    @SideOnly(Side.CLIENT)
    public boolean isTerminal(GuiScreen gui) {
        if (gui == null) return false;
        return terminal.contains(gui.getClass());
    }

    @SideOnly(Side.CLIENT)
    public boolean isPinTerminal(GuiScreen gui) {
        if (Util.getAEVersion() >= 586) {
            return false;
        }
        if (!NEI_TH_Config.getConfigValue(ButtonConstants.PINNED_BAR)) return false;
        if (gui == null || terminalBlackList.contains(gui.getClass())) {
            return false;
        }
        return terminal.contains(gui.getClass());
    }

    public void registerCraftingTerminal(ICraftingTerminalAdapter adapter) {
        craftingTerminal.put(adapter.getContainer(), adapter);
    }

    public HashMap<Class<? extends Container>, ICraftingTerminalAdapter> getCraftingTerminal() {
        return craftingTerminal;
    }

    public boolean isCraftingTerminal(Class<? extends Container> terminal) {
        return craftingTerminal.containsKey(terminal);
    }

    @SideOnly(Side.CLIENT)
    public boolean isCraftingTerminal(GuiScreen terminal) {
        if (terminal == null) return false;
        if (terminal instanceof GuiContainer gc && gc.inventorySlots != null) {
            return craftingTerminal.containsKey(gc.inventorySlots.getClass());
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    public void addTrackingMissingItem(IAEItemStack is) {
        tracking.add(is);
    }

    @SideOnly(Side.CLIENT)
    public IItemList<IAEItemStack> getTrackingMissingItems() {
        return tracking;
    }

    @SideOnly(Side.CLIENT)
    public void clearTrackingMissingItems() {
        tracking.resetStatus();
    }

    public IPatternTerminalAdapter registerPatternTerminal(IPatternTerminalAdapter adapter) {
        patternTerminal.putIfAbsent(adapter.getContainer(), adapter);
        return adapter;
    }

    public IPatternTerminalAdapter getPatternTerminal(Container c) {
        return patternTerminal.getOrDefault(c.getClass(), null);
    }

    public void registerTerminalItem(Class<? extends Item> item, ITerminalHandler terminal) {
        terminalItem.put(item, terminal);
    }

    public ITerminalHandler getOpenTerminalHandler(Class<? extends Item> item) {
        return terminalItem.get(item);
    }
}
