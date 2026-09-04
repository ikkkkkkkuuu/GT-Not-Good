package com.xyp.gtnotgood.ae2thing.api;

public interface Constants {

    /**
     * Slot-index offset used to distinguish a terminal held in a Baubles slot from one in the main inventory. A GUI
     * "slot" value >= this offset means {@code value - BAUBLE_SLOT_OFFSET} is an index into the player's baubles
     * inventory rather than {@code player.inventory}.
     */
    int BAUBLE_SLOT_OFFSET = 1000;

    String DISKUUID = "diskuuid";
    String DISKDATA = "diskdata";
    String FLUID_DISKLIST = "fluid_disklist";
    String DISKLIST = "disklist";
    String LINKED = "linked";
    String DISPLAY_ONLY = "DisplayOnly";
    String TC_CRAFTING = "tc_crafting";
    String IS_EMPTY = "is_empty";
    String COLOR = "color";
    String REMAINING_ITEM_COUNT = "remaining_item_count";
    String CPU_LIST = "cpu_list";
    String CPU_ELAPSED_TIME = "elapsed_time";
    String NAME = "name";
    String IS_LINKED = "is_linked";
    String LINK = "link";
    String USED_CHANNELS = "used";
    String MAGNET_MODE_KEY = "MagnetMode";
    String SIDE = "side";
    String CONFIG_INV = "ConfigInv";
    String NEI_MOUSE_WHEEL = "nei.mouse_wheel";
    String SLOT = "slot";
    String SIZE = "size";
    String VIEW_CELL = "view";
    String CONNECTIONS = "connections";
    String TILE_ENTRIES = "tile_entries";
    String TIER = "tier";
    String POWERED = "powered";
    String TARGET = "target";
    int OUTPUT_COLOR = 0x4566ccff;
    int ERROR_COLOR = 0x45DA4527;
    int INACTIVE_COLOR = 0x45FFEA05;
    int SELECTED_COLOR = 0x4545DA75;
    String PATTERN = "pattern";
    String REPLACE = "replace";
    String CRAFTING = "crafting";
    String OUTPUT = "output";
    String CRAFTING_EX = "crafting_ex";
    String OUTPUT_EX = "output_ex";
    String UPGRADES = "upgrades";
    String PLAYER = "player";
    String DEBUG_CARD_MODE = "debug_card_mode";
    String DEBUG_CARD_EXPORT_FILENAME = "history.json";
    int MODE_CRAFTING = 1;
    int MODE_PROCESSING = 0;
    String INFINITY_BOOSTER_CARD = "infinityBoosterCard";
    String INFINITY_ENERGY_CARD = "InfinityEnergyCard";
    /**
     * Stores the GuiType ordinal last chosen via the in-GUI switch button on the wireless dual interface terminal, so
     * reopening the terminal (right-click / bauble keybind) restores the crafting-terminal vs dual-interface view the
     * player last had open instead of always defaulting back to the dual interface terminal.
     */
    String LAST_GUI_MODE = "last_gui_mode";

    enum MessageType {

        UPDATE_PLAYER_CURRENT_ITEM(-1),
        UPDATE_PINNED_ITEMS(-2),
        ADD_PINNED_ITEM(-3),
        NOTIFICATION(-4);

        public final byte type;

        MessageType(int t) {
            this.type = (byte) t;
        }

        MessageType(byte t) {
            this.type = t;
        }
    }

    enum State {
        RUNNING,
        FINISHED,
        CANCELLED
    }

    enum MouseWheel {

        PREVIEW(-1),
        NEXT(1);

        public final int direction;

        MouseWheel(int direction) {
            this.direction = direction;
        }
    }

    enum Radius {

        Tier1(1),
        Tier2(3);

        public final int radius;

        Radius(int radius) {
            this.radius = radius;
        }

        @Override
        public String toString() {
            return String.format("%s x %s", this.radius, this.radius);
        }

        public int getValue() {
            return this.radius;
        }
    }
}
