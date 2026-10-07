package com.xyp.gtnotgood.common.items.patternsorter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.ModernThemeAdapter;

import cpw.mods.fml.common.network.ByteBufUtils;
import gregtech.api.recipe.RecipeMap;

/** Server-owned classifier model; client actions carry only recipe-map/group identifiers, never item stacks. */
public final class PatternSorterGui {

    private static final String MAP_TAG = "PatternSorterRecipeMap";
    public final GenericListSyncHandler<PatternSorter.Entry> entries;
    public final StringSyncValue selectedMap;
    public final Actions actions;
    private final PlayerInventoryGuiData data;
    private final PanelSyncManager sync;
    private String mapName;
    private ItemStack[] snapshot;
    private List<PatternSorter.Entry> cached = Collections.emptyList();

    public PatternSorterGui(PlayerInventoryGuiData data, PanelSyncManager sync) {
        this.data = data;
        this.sync = sync;
        ItemStack tool = data.getUsedItemStack();
        mapName = tool.hasTagCompound() ? tool.getTagCompound()
            .getString(MAP_TAG) : "";
        selectedMap = new StringSyncValue(() -> mapName);
        entries = new GenericListSyncHandler<>(
            this::scan,
            null,
            PatternSorterGui::readEntry,
            PatternSorterGui::writeEntry,
            PatternSorterGui::sameEntry,
            null);
        actions = new Actions();
        sync.syncValue("map", selectedMap);
        sync.syncValue("entries", entries);
        sync.syncValue("actions", actions);
    }

    public ModularPanel build() {
        ModernThemeAdapter theme = new ModernThemeAdapter(
            path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));
        return new Panel(this).size(420, 308)
            .background(theme.panel)
            .child(SlotGroupWidget.playerInventory((index, slot) -> {
                if (index == data.getSlotIndex()) slot = new LockedToolSlot();
                slot.background(theme.slot);
                return slot;
            })
                .pos(129, 220));
    }

    /** Keeps the tool visible while preventing extraction or replacement on both sides after slot binding. */
    private static final class LockedToolSlot extends ItemSlot {

        @Override
        public void onInit() {
            super.onInit();
            getSlot().canTake(false)
                .canPut(false);
        }
    }

    /** Retains the per-open model when the item factory constructs the client screen. */
    public static final class Panel extends ModularPanel {

        public final PatternSorterGui model;

        private Panel(PatternSorterGui model) {
            super("pattern_sorter");
            this.model = model;
        }
    }

    public static List<String> maps() {
        List<String> result = new ArrayList<>();
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            if (!map.getAllRecipes()
                .isEmpty()) result.add(map.unlocalizedName);
        }
        Collections.sort(result);
        return result;
    }

    private RecipeMap<?> map() {
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            if (map.unlocalizedName.equals(mapName)) return map;
        }
        return null;
    }

    /** Uses factory player data even during eager sync construction; unchanged inventories skip recipe scans. */
    private List<PatternSorter.Entry> scan() {
        EntityPlayer player = data.getPlayer();
        if (player.worldObj.isRemote) return cached;
        ItemStack[] inventory = player.inventory.mainInventory;
        boolean changed = snapshot == null;
        if (!changed) for (int i = 0; i < 36; i++) {
            if (!ItemStack.areItemStacksEqual(snapshot[i], inventory[i])) {
                changed = true;
                break;
            }
        }
        if (changed) {
            cached = PatternSorter.classify(inventory, player.worldObj, map());
            snapshot = new ItemStack[36];
            for (int i = 0; i < 36; i++) snapshot[i] = ItemStack.copyItemStack(inventory[i]);
        }
        return cached;
    }

    private static PatternSorter.Entry readEntry(PacketBuffer buf) {
        return new PatternSorter.Entry(
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            ByteBufUtils.readItemStack(buf));
    }

    private static void writeEntry(PacketBuffer buf, PatternSorter.Entry entry) {
        buf.writeInt(entry.slot);
        buf.writeInt(entry.status);
        buf.writeInt(entry.circuit);
        buf.writeInt(entry.mold);
        ByteBufUtils.writeItemStack(buf, entry.stack);
    }

    private static boolean sameEntry(PatternSorter.Entry a, PatternSorter.Entry b) {
        return a.slot == b.slot && a.group()
            .equals(b.group()) && ItemStack.areItemStacksEqual(a.stack, b.stack);
    }

    /** All inventory writes execute on the server against a freshly checked snapshot and an empty cursor. */
    public final class Actions extends SyncHandler {

        private Actions() {
            allowC2S();
        }

        public void chooseMap(String name) {
            syncToServer(0, buf -> ByteBufUtils.writeUTF8String(buf, name));
        }

        public void sort(String group) {
            syncToServer(1, buf -> ByteBufUtils.writeUTF8String(buf, group));
        }

        @Override
        public void readOnClient(int id, PacketBuffer buf) throws IOException {}

        @Override
        public void readOnServer(int id, PacketBuffer buf) throws IOException {
            EntityPlayer player = sync.getPlayer();
            ItemStack tool = player.inventory.getStackInSlot(data.getSlotIndex());
            if (tool == null || !(tool.getItem() instanceof PatternSorterItem)) return;
            String value = ByteBufUtils.readUTF8String(buf);
            if (id == 0 && maps().contains(value)) {
                mapName = value;
                if (!tool.hasTagCompound()) tool.setTagCompound(new NBTTagCompound());
                tool.getTagCompound()
                    .setString(MAP_TAG, mapName);
                snapshot = null;
                player.inventory.markDirty();
            } else if (id == 1 && map() != null && player.inventory.getItemStack() == null) {
                List<PatternSorter.Entry> current = scan();
                if (!value.isEmpty() && current.stream()
                    .noneMatch(
                        entry -> entry.group()
                            .equals(value)))
                    return;
                if (PatternSorter.reorder(player.inventory.mainInventory, current, value)) {
                    snapshot = null;
                    player.inventory.markDirty();
                    player.openContainer.detectAndSendChanges();
                }
            }
        }
    }
}
