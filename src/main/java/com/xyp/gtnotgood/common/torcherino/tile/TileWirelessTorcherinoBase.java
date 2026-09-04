package com.xyp.gtnotgood.common.torcherino.tile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.torcherino.api.ITorcherinoTile;
import com.xyp.gtnotgood.common.torcherino.util.AccelerationHelper;
import com.xyp.gtnotgood.common.torcherino.util.BoundMachineEntry;
import com.xyp.gtnotgood.config.Config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiTextures;

/**
 * Base tile entity for wireless Torcherinos that accelerate only machines bound through a GT Data Stick.
 */
public abstract class TileWirelessTorcherinoBase extends TileEntity implements IGuiHolder<PosGuiData>, ITorcherinoTile {

    private static final int VALIDATE_INTERVAL = 100;
    private static final int ACTION_PREVIOUS = 1;
    private static final int ACTION_NEXT = 2;
    private static final int ACTION_REMOVE = 3;

    protected final List<BoundMachineEntry> boundMachines = new ArrayList<>();
    protected int globalSpeedLevel = 0;
    protected boolean isStopped = false;
    protected boolean isActive = true;
    protected int selectedMachineIndex = 0;

    private long lastTickProcessed = -1L;
    private int tickCounter = 0;

    @Override
    public boolean getActive() {
        return isActive;
    }

    @Override
    public void setActive(boolean active) {
        this.isActive = active;
        markDirty();
    }

    @Override
    public boolean isStopped() {
        return isStopped;
    }

    @Override
    public int getEffectiveSpeed() {
        return Math.min(globalSpeedLevel, Config.torcherinoMaxSpeedLevel) * getSpeedMultiplier();
    }

    @Override
    public int getTorchX() {
        return xCoord;
    }

    @Override
    public int getTorchY() {
        return yCoord;
    }

    @Override
    public int getTorchZ() {
        return zCoord;
    }

    @Override
    public int getXRadius() {
        return Config.wirelessTorcherinoRadius;
    }

    @Override
    public int getYRadius() {
        return 128;
    }

    @Override
    public int getZRadius() {
        return Config.wirelessTorcherinoRadius;
    }

    /**
     * Returns the tier multiplier applied to speed levels.
     *
     * @return speed multiplier for this wireless torch tier
     */
    protected abstract int getSpeedMultiplier();

    /**
     * Returns the GUI title translation key.
     *
     * @return title translation key
     */
    protected abstract String getGuiTitleKey();

    /**
     * Returns the ModularUI panel id.
     *
     * @return stable panel id
     */
    protected abstract String getGuiPanelId();

    /**
     * Exposes the saved global speed level for render and tooltip integrations.
     *
     * @return global speed level before tier multiplication
     */
    public int getGlobalSpeedLevel() {
        return globalSpeedLevel;
    }

    /**
     * Returns the currently bound machines.
     *
     * @return immutable bound-machine list view
     */
    public List<BoundMachineEntry> getBoundMachines() {
        return Collections.unmodifiableList(boundMachines);
    }

    /**
     * Checks whether a target coordinate can be bound to this wireless torch.
     *
     * @param x target X coordinate
     * @param y target Y coordinate, intentionally ignored because wireless torches cover world height
     * @param z target Z coordinate
     * @return {@code true} when the target is in X/Z range
     */
    public boolean isInRange(int x, int y, int z) {
        return Math.abs(x - xCoord) <= Config.wirelessTorcherinoRadius
            && Math.abs(z - zCoord) <= Config.wirelessTorcherinoRadius;
    }

    /**
     * Adds a machine to the bound list.
     *
     * @param x   target X coordinate
     * @param y   target Y coordinate
     * @param z   target Z coordinate
     * @param dim target dimension id
     * @return {@code true} if the machine is now bound
     */
    public boolean addBoundMachine(int x, int y, int z, int dim) {
        if (!isInRange(x, y, z)) return false;
        BoundMachineEntry candidate = new BoundMachineEntry(x, y, z, dim);
        if (boundMachines.contains(candidate)) return true;
        if (boundMachines.size() >= Config.wirelessTorcherinoMaxBoundMachines) return false;
        boundMachines.add(candidate);
        selectedMachineIndex = boundMachines.size() - 1;
        markDirty();
        return true;
    }

    /**
     * Removes a machine from the bound list.
     *
     * @param x   target X coordinate
     * @param y   target Y coordinate
     * @param z   target Z coordinate
     * @param dim target dimension id
     */
    public void removeBoundMachine(int x, int y, int z, int dim) {
        Iterator<BoundMachineEntry> iterator = boundMachines.iterator();
        while (iterator.hasNext()) {
            BoundMachineEntry entry = iterator.next();
            if (entry.x == x && entry.y == y && entry.z == z && entry.dim == dim) {
                iterator.remove();
            }
        }
        selectedMachineIndex = clampIndex(selectedMachineIndex);
        markDirty();
    }

    /**
     * Updates the global wireless speed level.
     *
     * @param level requested global speed level
     */
    public void setGlobalSpeedLevel(int level) {
        globalSpeedLevel = clampInt(level, 0, Config.torcherinoMaxSpeedLevel);
        markDirty();
    }

    /**
     * Updates a per-machine speed override, where zero means "use global speed".
     *
     * @param index selected bound-machine index
     * @param speed requested speed level
     */
    public void setPerMachineSpeed(int index, int speed) {
        if (boundMachines.isEmpty()) return;
        int clampedIndex = clampIndex(index);
        boundMachines.get(clampedIndex).perMachineSpeed = clampInt(speed, 0, Config.torcherinoMaxSpeedLevel);
        markDirty();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(GTNotGood.MODID, mainPanel);
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        ModularPanel panel = ModularPanel.defaultPanel(getGuiPanelId(), 220, 176)
            .background(GTGuiTextures.BACKGROUND_STANDARD)
            .child(ButtonWidget.panelCloseButton());
        int multiplier = getSpeedMultiplier();

        DoubleSyncValue globalSpeedValue = new DoubleSyncValue(
            () -> (double) clampInt(globalSpeedLevel, 0, Config.torcherinoMaxSpeedLevel),
            value -> setGlobalSpeedLevel((int) Math.round(value))).allowC2S();
        IntSyncValue selectedIndexValue = new IntSyncValue(() -> clampIndex(selectedMachineIndex), value -> {
            selectedMachineIndex = clampIndex(value);
            markDirty();
        }).allowC2S();
        DoubleSyncValue perMachineSpeed = new DoubleSyncValue(
            () -> (double) getSelectedPerMachineSpeed(),
            value -> setPerMachineSpeed(selectedMachineIndex, (int) Math.round(value))).allowC2S();
        WirelessActionSyncHandler actionSync = new WirelessActionSyncHandler();

        syncManager.syncValue("gtnotgood.torcherino.globalSpeed", globalSpeedValue);
        syncManager.syncValue("gtnotgood.torcherino.selectedIndex", selectedIndexValue);
        syncManager.syncValue("gtnotgood.torcherino.perMachineSpeed", perMachineSpeed);
        syncManager.syncValue("gtnotgood.torcherino.action", actionSync);

        Rectangle sliderBg = new Rectangle().color(0xFF3A3A3A);

        panel.child(
            IKey.lang(getGuiTitleKey())
                .asWidget()
                .pos(8, 6));
        // #tr gtnotgood.torcherino.gui.wireless.global_speed
        // # Global Speed
        // # zh_CN 全局速度
        panel.child(
            IKey.lang("gtnotgood.torcherino.gui.wireless.global_speed")
                .asWidget()
                .pos(8, 22));
        panel.child(
            buildSlider(globalSpeedValue, Config.torcherinoMaxSpeedLevel, sliderBg).pos(8, 32)
                .size(160, 10));
        panel.child(
            IKey.dynamic(
                () -> globalSpeedValue.getDoubleValue() == 0.0 ? "0%"
                    : ((int) globalSpeedValue.getDoubleValue() * multiplier * 100) + "%")
                .asWidget()
                .pos(78, 44));

        // #tr gtnotgood.torcherino.gui.wireless.bound_machines
        // # Bound Machines
        // # zh_CN 已绑定机器
        panel.child(
            IKey.lang("gtnotgood.torcherino.gui.wireless.bound_machines")
                .asWidget()
                .pos(8, 60));
        panel.child(
            new TextWidget<>(IKey.dynamic(() -> buildSelectedMachineText(selectedIndexValue.getIntValue(), multiplier)))
                .pos(8, 74)
                .size(202, 38));

        panel.child(
            new ButtonWidget<>().pos(8, 116)
                .size(46, 16)
                // #tr gtnotgood.torcherino.gui.wireless.previous
                // # Previous
                // # zh_CN 上一台
                .overlay(IKey.lang("gtnotgood.torcherino.gui.wireless.previous"))
                .onMousePressed(mouseButton -> {
                    actionSync.sendPreviousMachine();
                    return true;
                }));
        panel.child(
            new ButtonWidget<>().pos(58, 116)
                .size(46, 16)
                // #tr gtnotgood.torcherino.gui.wireless.next
                // # Next
                // # zh_CN 下一台
                .overlay(IKey.lang("gtnotgood.torcherino.gui.wireless.next"))
                .onMousePressed(mouseButton -> {
                    actionSync.sendNextMachine();
                    return true;
                }));
        panel.child(
            new ButtonWidget<>().pos(164, 116)
                .size(46, 16)
                // #tr gtnotgood.torcherino.gui.wireless.remove
                // # Remove
                // # zh_CN 移除
                .overlay(IKey.lang("gtnotgood.torcherino.gui.wireless.remove"))
                .onMousePressed(mouseButton -> {
                    actionSync.sendRemoveSelected();
                    return true;
                }));

        // #tr gtnotgood.torcherino.gui.wireless.machine_speed
        // # Machine Speed
        // # zh_CN 单机速度
        panel.child(
            IKey.lang("gtnotgood.torcherino.gui.wireless.machine_speed")
                .asWidget()
                .pos(8, 138));
        panel.child(
            buildSlider(perMachineSpeed, Config.torcherinoMaxSpeedLevel, sliderBg).pos(8, 150)
                .size(160, 10));
        panel.child(IKey.dynamic(() -> {
            int value = (int) perMachineSpeed.getDoubleValue();
            if (value == 0) {
                // #tr gtnotgood.torcherino.gui.wireless.global_label
                // # Global
                // # zh_CN 全局
                return StatCollector.translateToLocal("gtnotgood.torcherino.gui.wireless.global_label");
            }
            return value * multiplier * 100 + "%";
        })
            .asWidget()
            .pos(78, 162));

        return panel;
    }

    private static SliderWidget buildSlider(DoubleSyncValue value, int configuredMax, Rectangle sliderBg) {
        return new SliderWidget().value(value)
            .bounds(0, Math.max(1, configuredMax))
            .stopper(1.0D)
            .background(sliderBg);
    }

    private String buildSelectedMachineText(int index, int multiplier) {
        int count = boundMachines.size();
        if (count <= 0) {
            // #tr gtnotgood.torcherino.gui.wireless.no_machines
            // # No bound machines
            // # zh_CN 暂未绑定机器
            return StatCollector.translateToLocal("gtnotgood.torcherino.gui.wireless.no_machines");
        }

        BoundMachineEntry entry = boundMachines.get(clampIndex(index));
        int speedLevel = entry.perMachineSpeed > 0 ? entry.perMachineSpeed : globalSpeedLevel;
        String speed = speedLevel <= 0 ? "0%" : speedLevel * multiplier * 100 + "%";
        String mode = entry.perMachineSpeed > 0 ? speed
            // #tr gtnotgood.torcherino.gui.wireless.uses_global
            // # %s (global)
            // # zh_CN %s (全局)
            : StatCollector.translateToLocalFormatted("gtnotgood.torcherino.gui.wireless.uses_global", speed);
        // #tr gtnotgood.torcherino.gui.wireless.machine_summary
        // # [%d/%d] %s\nPos: %d, %d, %d Dim: %d\nSpeed: %s
        // # zh_CN [%d/%d] %s\n坐标: %d, %d, %d 维度: %d\n速度: %s
        return StatCollector.translateToLocalFormatted(
            "gtnotgood.torcherino.gui.wireless.machine_summary",
            clampIndex(index) + 1,
            count,
            entry.getLocalizedName(worldObj),
            entry.x,
            entry.y,
            entry.z,
            entry.dim,
            mode);
    }

    private double getSelectedPerMachineSpeed() {
        if (boundMachines.isEmpty()) return 0.0D;
        return boundMachines.get(clampIndex(selectedMachineIndex)).perMachineSpeed;
    }

    private int getEffectiveMachineSpeed(BoundMachineEntry entry) {
        int max = Config.torcherinoMaxSpeedLevel;
        int level = entry.perMachineSpeed > 0 ? Math.min(entry.perMachineSpeed, max) : Math.min(globalSpeedLevel, max);
        if (entry.perMachineSpeed > max) {
            entry.perMachineSpeed = max;
            markDirty();
        }
        return level * getSpeedMultiplier();
    }

    private void validateBoundMachines() {
        if (worldObj == null) return;
        Iterator<BoundMachineEntry> iterator = boundMachines.iterator();
        while (iterator.hasNext()) {
            BoundMachineEntry entry = iterator.next();
            if (entry.dim != worldObj.provider.dimensionId) {
                iterator.remove();
                markDirty();
                continue;
            }
            TileEntity target = worldObj.getTileEntity(entry.x, entry.y, entry.z);
            if (target == null || target.isInvalid()) {
                iterator.remove();
                markDirty();
            }
        }
        selectedMachineIndex = clampIndex(selectedMachineIndex);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        globalSpeedLevel = clampInt(compound.getInteger("GlobalSpeedLevel"), 0, Config.torcherinoMaxSpeedLevel);
        isStopped = compound.getBoolean("IsStopped");
        isActive = !compound.hasKey("IsActive") || compound.getBoolean("IsActive");
        selectedMachineIndex = Math.max(0, compound.getInteger("SelectedMachineIndex"));

        boundMachines.clear();
        if (compound.hasKey("BoundMachines")) {
            NBTTagList list = compound.getTagList("BoundMachines", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                boundMachines.add(BoundMachineEntry.fromNBT(list.getCompoundTagAt(i)));
            }
        }
        selectedMachineIndex = clampIndex(selectedMachineIndex);

        if (compound.hasKey("TimeRate") && !compound.hasKey("GlobalSpeedLevel")) {
            globalSpeedLevel = clampInt(
                compound.getInteger("TimeRate") / getSpeedMultiplier(),
                0,
                Config.torcherinoMaxSpeedLevel);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("GlobalSpeedLevel", globalSpeedLevel);
        compound.setBoolean("IsStopped", isStopped);
        compound.setBoolean("IsActive", isActive);
        compound.setInteger("SelectedMachineIndex", selectedMachineIndex);

        NBTTagList list = new NBTTagList();
        for (BoundMachineEntry entry : boundMachines) {
            list.appendTag(entry.toNBT());
        }
        compound.setTag("BoundMachines", list);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, blockMetadata, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;

        long currentTick = worldObj.getTotalWorldTime();
        if (lastTickProcessed == currentTick) return;
        lastTickProcessed = currentTick;

        if (!isActive || isStopped) return;

        tickCounter++;
        if (tickCounter >= VALIDATE_INTERVAL) {
            tickCounter = 0;
            validateBoundMachines();
        }

        for (BoundMachineEntry entry : boundMachines) {
            if (entry.dim != worldObj.provider.dimensionId) continue;
            int machineSpeed = getEffectiveMachineSpeed(entry);
            if (machineSpeed <= 0) continue;
            AccelerationHelper
                .accelerateAtPosition(worldObj, xCoord, yCoord, zCoord, machineSpeed, entry.x, entry.y, entry.z);
        }
    }

    private int clampIndex(int index) {
        int max = boundMachines.size() - 1;
        return max < 0 ? 0 : clampInt(index, 0, max);
    }

    private static int clampInt(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * Server-side action sync for compact wireless Torcherino list controls.
     */
    private final class WirelessActionSyncHandler extends SyncHandler<WirelessActionSyncHandler> {

        private WirelessActionSyncHandler() {
            allowC2S();
        }

        private void sendPreviousMachine() {
            syncToServer(ACTION_PREVIOUS);
        }

        private void sendNextMachine() {
            syncToServer(ACTION_NEXT);
        }

        private void sendRemoveSelected() {
            syncToServer(ACTION_REMOVE);
        }

        @Override
        public void readOnClient(int id, PacketBuffer buf) throws IOException {}

        @Override
        public void readOnServer(int id, PacketBuffer buf) throws IOException {
            if (boundMachines.isEmpty()) return;
            switch (id) {
                case ACTION_PREVIOUS:
                    selectedMachineIndex = clampIndex(selectedMachineIndex - 1);
                    markDirty();
                    break;
                case ACTION_NEXT:
                    selectedMachineIndex = clampIndex(selectedMachineIndex + 1);
                    markDirty();
                    break;
                case ACTION_REMOVE:
                    boundMachines.remove(clampIndex(selectedMachineIndex));
                    selectedMachineIndex = clampIndex(selectedMachineIndex);
                    markDirty();
                    break;
                default:
                    break;
            }
        }
    }
}
