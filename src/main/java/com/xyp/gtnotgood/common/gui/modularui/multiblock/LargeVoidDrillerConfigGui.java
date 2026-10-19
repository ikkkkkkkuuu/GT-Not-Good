package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.xyp.gtnotgood.client.gui.LDLibVoidDrillerView;
import com.xyp.gtnotgood.common.api.gui.FluidEntryInfo;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidDriller;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.LDLibModularScreen;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class LargeVoidDrillerConfigGui implements IGuiHolder<PosGuiData> {

    private final LargeVoidDriller driller;
    public GenericListSyncHandler<FluidEntryInfo> fluids;
    public DrillerActionSyncHandler actions;
    public IntSyncValue overclock, cycleTicks, gasDimensions;
    public LongSyncValue energy;
    public DoubleSyncValue progress, energyMult, uu, weightIncrease, dimIncrease;
    public StringSyncValue dimension;
    public BooleanSyncValue directional, enabled;

    public LargeVoidDrillerConfigGui(LargeVoidDriller driller) {
        this.driller = driller;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        fluids = new GenericListSyncHandler<>(driller::getFluidEntries, null, LargeVoidDrillerConfigGui::readFluidInfo,
            LargeVoidDrillerConfigGui::writeFluidInfo, LargeVoidDrillerConfigGui::fluidInfoEqual, null);
        actions = new DrillerActionSyncHandler(driller);
        overclock = new IntSyncValue(driller::getOverclockLevel);
        cycleTicks = new IntSyncValue(driller::getCycleDurationTicks);
        gasDimensions = new IntSyncValue(driller::getGasDimensionMask);
        energy = new LongSyncValue(driller::getEnergyCostPerTick);
        progress = new DoubleSyncValue(
            () -> driller.mMaxProgresstime <= 0 ? 0 : (double) driller.mProgresstime / driller.mMaxProgresstime);
        directional = new BooleanSyncValue(driller::getDirectionalMode);
        energyMult = new DoubleSyncValue(driller::getEnergyMultiplier);
        uu = new DoubleSyncValue(driller::getUURatePerSecond);
        weightIncrease = new DoubleSyncValue(driller::getWeightIncreasePercent);
        dimIncrease = new DoubleSyncValue(driller::getDimensionIncreasePercent);
        dimension = new StringSyncValue(driller::getDimensionDisplayName);
        enabled = new BooleanSyncValue(() -> driller.getBaseMetaTileEntity().isAllowedToWork(), value -> {
            if (value) driller.getBaseMetaTileEntity().enableWorking();
            else driller.getBaseMetaTileEntity().disableWorking();
        });
        enabled.allowC2S();
        sync.syncValue("fluids", fluids);
        sync.syncValue("actions", actions);
        sync.syncValue("overclock", overclock);
        sync.syncValue("cycleTicks", cycleTicks);
        sync.syncValue("gasDimensions", gasDimensions);
        sync.syncValue("energy", energy);
        sync.syncValue("progress", progress);
        sync.syncValue("directional", directional);
        sync.syncValue("energyMult", energyMult);
        sync.syncValue("uu", uu);
        sync.syncValue("weightIncrease", weightIncrease);
        sync.syncValue("dimIncrease", dimIncrease);
        sync.syncValue("dimension", dimension);
        sync.syncValue("enabled", enabled);
        ModularPanel panel = new ModelPanel(this).size(475, 365);
        InvWrapper inventory = new InvWrapper(driller.getPluginSlotInventory());
        for (int i = 0; i < 25; i++) {
            ItemSlot slot = new ItemSlot()
                .slot(
                    new ModularSlot(inventory, i).filter(LargeVoidDriller::isDimensionDisplayItem).singletonSlotGroup())
                .pos(12 + (i % 5) * 18, 72 + (i / 5) * 18).size(18);
            if (i == 0) slot.tooltipDynamic(t -> t.addLine(IKey.lang(VoidDrillerGuiText.SLOT1_HINT)));
            panel.child(slot);
        }
        panel.child(SlotGroupWidget.playerInventory(true).left(156).bottom(7));
        return panel;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ModularScreen createScreen(PosGuiData data, ModularPanel panel) {
        return new LDLibModularScreen(ModList.GTNotGood.getID(), panel,
            new LDLibVoidDrillerView(((ModelPanel) panel).model));
    }

    /** Retains the synchronized holder when the position factory resolves the client screen. */
    private static final class ModelPanel extends ModularPanel {

        private final LargeVoidDrillerConfigGui model;

        private ModelPanel(LargeVoidDrillerConfigGui model) {
            super("ldlib_void_driller");
            this.model = model;
        }
    }

    static FluidEntryInfo readFluidInfo(PacketBuffer buf) {
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        FluidStack fluid = tag == null ? null : FluidStack.loadFluidStackFromNBT(tag);
        double weight = buf.readDouble();
        int minAmount = buf.readInt(), maxAmount = buf.readInt();
        int dimCount = buf.readInt();
        List<String> dimAbbrs = new ArrayList<>(dimCount);
        for (int i = 0; i < dimCount; i++) dimAbbrs.add(ByteBufUtils.readUTF8String(buf));
        return new FluidEntryInfo(fluid, weight, dimAbbrs, minAmount, maxAmount, buf.readBoolean(), buf.readBoolean());
    }

    static void writeFluidInfo(PacketBuffer buf, FluidEntryInfo info) {
        ByteBufUtils.writeTag(buf, info.fluid == null ? null : info.fluid.writeToNBT(new NBTTagCompound()));
        buf.writeDouble(info.weight);
        buf.writeInt(info.minAmount);
        buf.writeInt(info.maxAmount);
        buf.writeInt(info.dimAbbrs.size());
        for (String abbr : info.dimAbbrs) ByteBufUtils.writeUTF8String(buf, abbr);
        buf.writeBoolean(info.filtered);
        buf.writeBoolean(info.aimed);
    }

    static boolean fluidInfoEqual(FluidEntryInfo a, FluidEntryInfo b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (
            Double.compare(a.weight, b.weight) != 0 || a.minAmount != b.minAmount
                || a.maxAmount != b.maxAmount
                || a.filtered != b.filtered
                || a.aimed != b.aimed
                || !a.dimAbbrs.equals(b.dimAbbrs)
        ) return false;
        if (a.fluid == null || b.fluid == null) return a.fluid == b.fluid;
        return a.fluid.isFluidEqual(b.fluid) && a.fluid.amount == b.fluid.amount;
    }

    public static final class DrillerActionSyncHandler extends SyncHandler<DrillerActionSyncHandler> {

        private static final int ACTION_CYCLE_OVERCLOCK = 1;
        private static final int ACTION_REFRESH_POOL = 2;
        private static final int ACTION_TOGGLE_DIRECTIONAL = 3;
        private static final int ACTION_TOGGLE_FLUID = 4;
        private static final int ACTION_CLEAR_CONFIG = 5;
        private static final int ACTION_TOGGLE_GAS_DIMENSION = 6;

        private final LargeVoidDriller driller;

        private DrillerActionSyncHandler(LargeVoidDriller driller) {
            this.driller = driller;
            allowC2S();
        }

        public void sendCycleOverclock() {
            syncToServer(ACTION_CYCLE_OVERCLOCK, buf -> {});
        }

        public void sendRefreshPool() {
            syncToServer(ACTION_REFRESH_POOL, buf -> {});
        }

        public void sendToggleDirectional() {
            syncToServer(ACTION_TOGGLE_DIRECTIONAL, buf -> {});
        }

        public void sendToggleFluid(FluidEntryInfo info) {
            if (info == null || info.fluid == null || info.fluid.getFluid() == null) return;
            syncToServer(ACTION_TOGGLE_FLUID,
                buf -> ByteBufUtils.writeUTF8String(buf, info.fluid.getFluid().getName()));
        }

        public void sendClearConfig() {
            syncToServer(ACTION_CLEAR_CONFIG, buf -> {});
        }

        public void sendToggleGasDimension(int index) {
            if (index < 0 || index >= 4) return;
            syncToServer(ACTION_TOGGLE_GAS_DIMENSION, buf -> buf.writeByte(index));
        }

        @Override
        public void readOnClient(int id, PacketBuffer buf) throws IOException {}

        @Override
        public void readOnServer(int id, PacketBuffer buf) throws IOException {
            switch (id) {
                case ACTION_CYCLE_OVERCLOCK:
                    driller.cycleOverclockLevel();
                    break;
                case ACTION_REFRESH_POOL:
                    driller.forceRefreshPool();
                    break;
                case ACTION_TOGGLE_DIRECTIONAL:
                    driller.toggleDirectionalMode();
                    break;
                case ACTION_TOGGLE_FLUID:
                    driller.toggleFluidSelection(ByteBufUtils.readUTF8String(buf));
                    break;
                case ACTION_CLEAR_CONFIG:
                    driller.clearCurrentModeConfig();
                    break;
                case ACTION_TOGGLE_GAS_DIMENSION:
                    int index = buf.readUnsignedByte();
                    if (index < 4) driller.toggleGasDimension(index);
                    break;
                default:
                    break;
            }
        }
    }
}
