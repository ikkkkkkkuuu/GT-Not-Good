package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.xyp.gtnotgood.common.api.gui.OreEntryInfo;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidMiner;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.util.GTUtility;

/** Server-owned inventory and synchronized model for the production LDLib miner configuration terminal. */
public final class LargeVoidMinerConfigGui implements IGuiHolder<PosGuiData> {

    private final LargeVoidMiner miner;
    public GenericListSyncHandler<OreEntryInfo> ores;
    public LargeVoidMinerConfigGui.MinerActionSyncHandler actions;
    public IntSyncValue oreMode, fortune;
    public DoubleSyncValue energy, progress, energyMult, uu, weightIncrease, dimIncrease;
    public StringSyncValue dimension;
    public BooleanSyncValue directional, enabled;

    public LargeVoidMinerConfigGui(LargeVoidMiner miner) {
        this.miner = miner;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        ores = new GenericListSyncHandler<>(
            miner::getOreEntries,
            null,
            LargeVoidMinerConfigGui::readOreInfo,
            LargeVoidMinerConfigGui::writeOreInfo,
            LargeVoidMinerConfigGui::oreInfoEqual,
            null);
        actions = new LargeVoidMinerConfigGui.MinerActionSyncHandler(miner);
        oreMode = new IntSyncValue(() -> miner.mOreMode);
        fortune = new IntSyncValue(() -> miner.mFortuneLevel);
        energy = new DoubleSyncValue(() -> miner.getEnergyCostPerTick());
        progress = new DoubleSyncValue(
            () -> miner.mMaxProgresstime <= 0 ? 0 : (double) miner.mProgresstime / miner.mMaxProgresstime);
        directional = new BooleanSyncValue(miner::getDirectionalMode);
        energyMult = new DoubleSyncValue(miner::getEnergyMultiplier);
        uu = new DoubleSyncValue(miner::getUUMultiplier);
        weightIncrease = new DoubleSyncValue(miner::getWeightIncreasePercent);
        dimIncrease = new DoubleSyncValue(miner::getDimensionIncreasePercent);
        dimension = new StringSyncValue(miner::getDimensionDisplayName);
        sync.syncValue("energyMult", energyMult);
        sync.syncValue("uu", uu);
        sync.syncValue("weightIncrease", weightIncrease);
        sync.syncValue("dimIncrease", dimIncrease);
        sync.syncValue("dimension", dimension);
        enabled = new BooleanSyncValue(
            () -> miner.getBaseMetaTileEntity()
                .isAllowedToWork(),
            value -> {
                if (value) miner.getBaseMetaTileEntity()
                    .enableWorking();
                else miner.getBaseMetaTileEntity()
                    .disableWorking();
            });
        enabled.allowC2S();
        sync.syncValue("ores", ores);
        sync.syncValue("actions", actions);
        sync.syncValue("oreMode", oreMode);
        sync.syncValue("fortune", fortune);
        sync.syncValue("energy", energy);
        sync.syncValue("progress", progress);
        sync.syncValue("directional", directional);
        sync.syncValue("enabled", enabled);
        ModularPanel panel = new ModelPanel(this).size(475, 365);
        InvWrapper inventory = new InvWrapper(miner.getPluginSlotInventory());
        for (int i = 0; i < 25; i++) {
            ItemSlot slot = new ItemSlot()
                .slot(
                    new ModularSlot(inventory, i).filter(LargeVoidMiner::isDimensionDisplayItem)
                        .singletonSlotGroup())
                .pos(12 + (i % 5) * 18, 72 + (i / 5) * 18)
                .size(18);
            if (i == 0) slot.tooltipDynamic(
                t -> t.addLine(com.cleanroommc.modularui.api.drawable.IKey.lang(VoidMinerGuiText.SLOT1_HINT)));
            panel.child(slot);
        }
        panel.child(
            SlotGroupWidget.playerInventory(true)
                .left(156)
                .bottom(7));
        return panel;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ModularScreen createScreen(PosGuiData data, ModularPanel panel) {
        return new com.xyp.ldlib.integration.modularui.LDLibModularScreen(
            ModList.GTNotGood.getID(),
            panel,
            new com.xyp.gtnotgood.client.gui.LDLibVoidMinerView(((ModelPanel) panel).model));
    }

    /** Carries the panel's model because position factories resolve a fresh holder when creating the client screen. */
    private static final class ModelPanel extends ModularPanel {

        private final LargeVoidMinerConfigGui model;

        private ModelPanel(LargeVoidMinerConfigGui model) {
            super("ldlib_void_miner");
            this.model = model;
        }
    }

    static OreEntryInfo readOreInfo(PacketBuffer buf) {
        ItemStack ore = ByteBufUtils.readItemStack(buf);
        float weight = buf.readFloat();
        int dimCount = buf.readInt();
        List<String> dimAbbrs = new ArrayList<>(dimCount);
        for (int i = 0; i < dimCount; i++) {
            dimAbbrs.add(ByteBufUtils.readUTF8String(buf));
        }
        boolean filtered = buf.readBoolean();
        boolean aimed = buf.readBoolean();
        return new OreEntryInfo(ore, weight, dimAbbrs, filtered, aimed);
    }

    static void writeOreInfo(PacketBuffer buf, OreEntryInfo info) {
        ByteBufUtils.writeItemStack(buf, info.ore);
        buf.writeFloat(info.weight);
        buf.writeInt(info.dimAbbrs.size());
        for (String abbr : info.dimAbbrs) {
            ByteBufUtils.writeUTF8String(buf, abbr);
        }
        buf.writeBoolean(info.filtered);
        buf.writeBoolean(info.aimed);
    }

    static boolean oreInfoEqual(OreEntryInfo a, OreEntryInfo b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.weight != b.weight || a.filtered != b.filtered || a.aimed != b.aimed) return false;
        if (!a.dimAbbrs.equals(b.dimAbbrs)) return false;
        if (a.ore == null || b.ore == null) return a.ore == b.ore;
        GTUtility.ItemId aid = GTUtility.ItemId.create(a.ore);
        GTUtility.ItemId bid = GTUtility.ItemId.create(b.ore);
        return aid != null && aid.equals(bid);
    }

    /**
     * Server action sync handler shared by all configuration controls.
     */
    public static class MinerActionSyncHandler extends SyncHandler<MinerActionSyncHandler> {

        private static final int ACTION_CYCLE_ORE_MODE = 1;
        private static final int ACTION_CYCLE_FORTUNE = 2;
        private static final int ACTION_TOGGLE_FILTER = 3;
        private static final int ACTION_REFRESH_POOL = 4;
        private static final int ACTION_TOGGLE_DIRECTIONAL = 5;
        private static final int ACTION_TOGGLE_DIRECTIONAL_ORE = 6;
        private static final int ACTION_CLEAR_CONFIG = 7;

        private final LargeVoidMiner miner;

        public MinerActionSyncHandler(LargeVoidMiner miner) {
            this.miner = miner;
            allowC2S();
        }

        public void sendCycleOreMode() {
            syncToServer(ACTION_CYCLE_ORE_MODE, buf -> {});
        }

        public void sendCycleFortune() {
            syncToServer(ACTION_CYCLE_FORTUNE, buf -> {});
        }

        public void sendToggleFilter(OreEntryInfo info) {
            writeOreAction(ACTION_TOGGLE_FILTER, info);
        }

        public void sendRefreshPool() {
            syncToServer(ACTION_REFRESH_POOL, buf -> {});
        }

        public void sendToggleDirectional() {
            syncToServer(ACTION_TOGGLE_DIRECTIONAL, buf -> {});
        }

        public void sendToggleDirectionalOre(OreEntryInfo info) {
            writeOreAction(ACTION_TOGGLE_DIRECTIONAL_ORE, info);
        }

        public void sendClearConfig() {
            syncToServer(ACTION_CLEAR_CONFIG, buf -> {});
        }

        private void writeOreAction(int action, OreEntryInfo info) {
            syncToServer(action, buf -> {
                GameRegistry.UniqueIdentifier uid = info == null || info.ore == null ? null
                    : GameRegistry.findUniqueIdentifierFor(info.ore.getItem());
                ByteBufUtils.writeUTF8String(buf, uid == null ? "" : uid.modId + ":" + uid.name);
                buf.writeInt(info == null || info.ore == null ? 0 : info.ore.getItemDamage());
            });
        }

        @Override
        public void readOnClient(int id, PacketBuffer buf) throws IOException {}

        @Override
        public void readOnServer(int id, PacketBuffer buf) throws IOException {
            switch (id) {
                case ACTION_CYCLE_ORE_MODE:
                    miner.cycleOreMode();
                    break;
                case ACTION_CYCLE_FORTUNE:
                    miner.cycleFortuneLevel();
                    break;
                case ACTION_TOGGLE_FILTER:
                    toggleOre(buf, false);
                    break;
                case ACTION_REFRESH_POOL:
                    miner.forceRefreshPool();
                    break;
                case ACTION_TOGGLE_DIRECTIONAL:
                    miner.toggleDirectionalMode(null);
                    break;
                case ACTION_TOGGLE_DIRECTIONAL_ORE:
                    toggleOre(buf, true);
                    break;
                case ACTION_CLEAR_CONFIG:
                    miner.clearCurrentModeConfig();
                    break;
                default:
                    break;
            }
        }

        private void toggleOre(PacketBuffer buf, boolean directional) {
            String name = ByteBufUtils.readUTF8String(buf);
            int meta = buf.readInt();
            if (name == null || name.isEmpty()) return;
            String[] parts = name.split(":", 2);
            if (parts.length != 2) return;
            Item item = GameRegistry.findItem(parts[0], parts[1]);
            if (item == null) return;
            GTUtility.ItemId id = GTUtility.ItemId.createNoCopy(new ItemStack(item, 1, meta));
            if (directional) {
                miner.setOreAimed(id, !miner.isOreAimed(id));
            } else {
                miner.setOreFiltered(id, !miner.isOreFiltered(id));
            }
        }
    }

}
