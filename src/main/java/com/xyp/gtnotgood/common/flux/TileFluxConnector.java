/*
 * Adapted from Flux Networks TileFluxConnector and ConnectionTransfer.
 * Copyright (c) 2018 Ollie Lansdell. MIT; see META-INF/licenses/Flux-Networks-MIT.txt.
 */
package com.xyp.gtnotgood.common.flux;

import java.math.BigInteger;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.tileentity.IBasicEnergyContainer;
import gregtech.api.interfaces.tileentity.IEnergyConnected;
import gregtech.common.misc.WirelessNetworkManager;

/**
 * Port of the Flux connector lifecycle to Forge 1.7.10 and GT's EU packet interface.
 * The original custom network/FE backend is replaced with the placing player's GTNH wireless
 * account. GT resolves team membership on each transaction. Chunk tickets are opt-in; no dimension scans are used.
 * All ledger mutations run on the server; GUI users cannot supply an account UUID.
 */
public abstract class TileFluxConnector extends TileEntity implements IEnergyConnected, IGuiHolder<PosGuiData> {

    public static final long MAX_VOLTAGE = GTValues.V[14];
    public static final long MAX_AMPERAGE = 1_048_576L;
    private UUID owner;
    private String ownerName = "";
    private long voltage = 32;
    private long amperage = 1;
    private boolean enabled = true;
    private boolean connected = true;
    private boolean redstoneRequired;
    private String customName = "";
    private long lastTransfer;
    private int priority;
    private boolean surgeMode;
    private boolean chunkLoading;
    private net.minecraftforge.common.ForgeChunkManager.Ticket chunkTicket;
    private boolean initialized;
    private int cursor;
    private int visualConnections;

    /** Uses server-synchronized ports even while neighboring client GT tiles are still initializing. */
    public boolean hasVisualConnection(ForgeDirection side) {
        return (visualConnections & side.flag) != 0;
    }

    /** Checks six loaded neighbors and sends a render update only when the physical ports change. */
    private void refreshVisualConnections() {
        if (!(getBlockType() instanceof BlockFluxConnector block)) return;
        int mask = 0;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (block.canConnectTo(worldObj, xCoord, yCoord, zCoord, side)) mask |= side.flag;
        }
        if (mask != visualConnections) {
            visualConnections = mask;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    protected final FluxTransferBuffer buffer = new FluxTransferBuffer();

    public abstract boolean isPlug();

    public long voltage() {
        return voltage;
    }

    public long amperage() {
        return amperage;
    }

    public boolean enabled() {
        return enabled;
    }

    public String ownerName() {
        return ownerName;
    }

    public long stored() {
        return buffer.stored();
    }

    public boolean connected() {
        return connected;
    }

    public boolean redstoneRequired() {
        return redstoneRequired;
    }

    public String customName() {
        return customName;
    }

    public long lastTransfer() {
        return lastTransfer;
    }

    public int priority() {
        return priority;
    }

    public boolean surgeMode() {
        return surgeMode;
    }

    public boolean chunkLoading() {
        return chunkTicket != null;
    }

    public void toggleChunkLoading() {
        if (worldObj == null || worldObj.isRemote) return;
        if (chunkTicket != null) releaseTicket();
        else chunkTicket = FluxChunkLoading.request(this);
        chunkLoading = chunkTicket != null;
        changed();
    }

    boolean restoreTicket(net.minecraftforge.common.ForgeChunkManager.Ticket ticket) {
        if (!chunkLoading || chunkTicket != null) return false;
        chunkTicket = ticket;
        return true;
    }

    private void releaseTicket() {
        if (chunkTicket != null) net.minecraftforge.common.ForgeChunkManager.releaseTicket(chunkTicket);
        chunkTicket = null;
    }

    public int effectivePriority() {
        return surgeMode ? 100_000 : priority;
    }

    public void setPriority(long value) {
        if (worldObj == null || worldObj.isRemote) return;
        priority = (int) Math.max(-9999, Math.min(9999, value));
        changed();
    }

    public void toggleSurgeMode() {
        if (worldObj == null || worldObj.isRemote) return;
        surgeMode = !surgeMode;
        changed();
    }

    private long currentLimit() {
        return voltage * amperage;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void validate() {
        super.validate();
        if (worldObj != null && !worldObj.isRemote) FluxTransferScheduler.add(this);
    }

    @Override
    public void invalidate() {
        releaseTicket();
        FluxTransferScheduler.remove(this);
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        FluxTransferScheduler.remove(this);
        super.onChunkUnload();
    }

    public void setCustomName(String value) {
        if (worldObj == null || worldObj.isRemote) return;
        customName = value.replaceAll("[\\p{Cntrl}§]", "");
        if (customName.length() > 24) customName = customName.substring(0, 24);
        changed();
    }

    public void toggleConnected() {
        if (worldObj == null || worldObj.isRemote) return;
        connected = !connected;
        changed();
    }

    public void toggleRedstone() {
        if (worldObj == null || worldObj.isRemote) return;
        redstoneRequired = !redstoneRequired;
        changed();
    }

    private boolean canTransfer() {
        return enabled && connected
            && owner != null
            && (!redstoneRequired
                || worldObj != null && worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord));
    }

    /** Queries only the six already-loaded neighbors while the GUI is open. */
    public String neighbourName(int sideIndex) {
        if (worldObj == null) return "";
        TileEntity tile = neighbour(ForgeDirection.VALID_DIRECTIONS[sideIndex]);
        if (!(tile instanceof IEnergyConnected) || tile instanceof TileFluxConnector) return "—";
        return worldObj.getBlock(tile.xCoord, tile.yCoord, tile.zCoord)
            .getLocalizedName();
    }

    public String balance() {
        return owner == null || worldObj == null || worldObj.isRemote ? "0"
            : WirelessNetworkManager.getUserEU(owner)
                .toString();
    }

    public boolean canConfigure(EntityPlayer player) {
        return owner != null && owner.equals(player.getUniqueID());
    }

    /** Placement binds to the real player, never to an owner supplied by an item or client packet. */
    public void placedBy(EntityPlayer player) {
        if (worldObj.isRemote) return;
        owner = player.getUniqueID();
        ownerName = player.getCommandSenderName();
        initializeAccount();
        lastTransfer = 0;
        changed();
    }

    private void initializeAccount() {
        if (!initialized && owner != null) {
            WirelessNetworkManager.strongCheckOrAddUser(owner);
            initialized = true;
        }
    }

    public void configure(long newVoltage, long newAmperage) {
        if (worldObj == null || worldObj.isRemote) return;
        voltage = Math.max(1, Math.min(MAX_VOLTAGE, newVoltage));
        amperage = Math.max(1, Math.min(MAX_AMPERAGE, newAmperage));
        changed();
    }

    /**
     * Initializes voltage once on placement from adjacent GT ports, with a default current of one ampere.
     * Plugs use the highest source output; points use the lowest sink rating to avoid overvolting a neighbor.
     * Physical port checks also work before GT's active-face cache updates. Without a readable rating, use 32 V.
     * This must not run from ticking, neighbor updates or NBT loading, so later manual settings remain authoritative.
     *
     * @see BlockFluxConnector#onBlockPlacedBy
     * @see #configure(long, long)
     */
    public void configureOnPlacement() {
        if (worldObj == null || worldObj.isRemote) return;
        long detected = 0;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            TileEntity adjacent = neighbour(side);
            if (!(adjacent instanceof IBasicEnergyContainer target) || adjacent instanceof TileFluxConnector) continue;
            ForgeDirection face = side.getOpposite();
            if (isPlug() ? !target.outputsEnergyTo(face, false) : !target.inputEnergyFrom(face, false)) continue;
            long rating = isPlug() ? target.getOutputVoltage() : target.getInputVoltage();
            if (rating <= 0) continue;
            detected = detected == 0 ? rating : isPlug() ? Math.max(detected, rating) : Math.min(detected, rating);
        }
        configure(detected > 0 ? detected : 32, 1);
    }

    public void toggleEnabled() {
        if (worldObj == null || worldObj.isRemote) return;
        enabled = !enabled;
        changed();
    }

    private void changed() {
        FluxTransferScheduler.changed();
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote || owner == null) return;
        if (worldObj.getTotalWorldTime() % 10 == 0) refreshVisualConnections();
        initializeAccount();
        lastTransfer = 0;
        buffer.beginTick(worldObj.getTotalWorldTime());
        if (isPlug()) {
            lastTransfer = buffer.stored();
            // Previously accepted EU must reach its account even when the connector is disabled.
            returnBufferToNetwork();
        } else if (canTransfer()) {
            transferToNeighbours();
        }
    }

    /**
     * Debit before delivery, then refund every rejected EU in the same tick. Partial balances can
     * supply whole packets without waiting for a full transfer limit. No point can duplicate energy
     * when several points use the same account. The buffer also protects a failed delivery call.
     */
    private void transferToNeighbours() {
        returnBufferToNetwork();
        if (buffer.stored() != 0) return;
        long available = WirelessNetworkManager.getUserEU(owner)
            .min(BigInteger.valueOf(currentLimit()))
            .longValue();
        long packets = available / voltage;
        if (packets <= 0) return;
        long reservation = packets * voltage;
        if (!WirelessNetworkManager.addEUToGlobalEnergyMap(owner, -reservation)) return;
        buffer.restore(reservation);
        markDirty();
        try {
            for (int i = 0; i < 6 && buffer.stored() >= voltage; i++) {
                ForgeDirection side = ForgeDirection.VALID_DIRECTIONS[(cursor + i) % 6];
                TileEntity tile = neighbour(side);
                if (!(tile instanceof IEnergyConnected target) || tile instanceof TileFluxConnector) continue;
                ForgeDirection opposite = side.getOpposite();
                if (!target.inputEnergyFrom(opposite)) continue;
                long offered = buffer.stored() / voltage;
                long accepted = target.injectEnergyUnits(opposite, voltage, offered);
                buffer.remove(Math.max(0, Math.min(offered, accepted)) * voltage);
                lastTransfer += Math.max(0, Math.min(offered, accepted)) * voltage;
                tile.markDirty();
            }
        } finally {
            cursor = (cursor + 1) % 6;
            returnBufferToNetwork();
        }
    }

    /** Refund on block removal, but never on chunk unload (where the buffer remains in tile NBT). */
    public void returnBufferToNetwork() {
        if (worldObj == null || worldObj.isRemote || owner == null || buffer.stored() == 0) return;
        initializeAccount();
        long amount = buffer.stored();
        if (WirelessNetworkManager.addEUToGlobalEnergyMap(owner, amount)) {
            buffer.remove(amount);
            markDirty();
        }
    }

    private TileEntity neighbour(ForgeDirection side) {
        int x = xCoord + side.offsetX, y = yCoord + side.offsetY, z = zCoord + side.offsetZ;
        return worldObj.blockExists(x, y, z) ? worldObj.getTileEntity(x, y, z) : null;
    }

    @Override
    public long injectEnergyUnits(ForgeDirection side, long inputVoltage, long inputAmperage) {
        if (worldObj == null || worldObj.isRemote || owner == null || !inputEnergyFrom(side)) return 0;
        buffer.beginTick(worldObj.getTotalWorldTime());
        long accepted = buffer.receive(inputVoltage, inputAmperage, voltage, amperage);
        if (accepted > 0) markDirty();
        return accepted;
    }

    @Override
    public boolean inputEnergyFrom(ForgeDirection side) {
        return isPlug() && canTransfer();
    }

    @Override
    public boolean outputsEnergyTo(ForgeDirection side) {
        return !isPlug() && canTransfer();
    }

    @Override
    public byte getColorization() {
        return -1;
    }

    @Override
    public byte setColorization(byte color) {
        return -1;
    }

    public void writeSettings(NBTTagCompound tag) {
        tag.setLong("fluxVoltage", voltage);
        tag.setLong("fluxAmperage", amperage);
        tag.setBoolean("fluxEnabled", enabled);
        tag.setBoolean("fluxConnected", connected);
        tag.setBoolean("fluxRedstone", redstoneRequired);
        tag.setString("fluxName", customName);
        tag.setInteger("fluxPriority", priority);
        tag.setBoolean("fluxSurge", surgeMode);
    }

    public void readSettings(NBTTagCompound tag) {
        voltage = tag.hasKey("fluxVoltage") ? Math.max(1, Math.min(MAX_VOLTAGE, tag.getLong("fluxVoltage"))) : 32;
        amperage = tag.hasKey("fluxAmperage") ? Math.max(1, Math.min(MAX_AMPERAGE, tag.getLong("fluxAmperage"))) : 1;
        enabled = !tag.hasKey("fluxEnabled") || tag.getBoolean("fluxEnabled");
        connected = !tag.hasKey("fluxConnected") || tag.getBoolean("fluxConnected");
        redstoneRequired = tag.getBoolean("fluxRedstone");
        customName = tag.getString("fluxName");
        if (customName.length() > 24) customName = customName.substring(0, 24);
        priority = Math.max(-9999, Math.min(9999, tag.getInteger("fluxPriority")));
        surgeMode = tag.getBoolean("fluxSurge");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeSettings(tag);
        if (owner != null) tag.setString("fluxOwner", owner.toString());
        tag.setString("fluxOwnerName", ownerName);
        tag.setLong("fluxBuffer", buffer.stored());
        tag.setBoolean("fluxChunkLoading", chunkLoading);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readSettings(tag);
        owner = null;
        try {
            owner = UUID.fromString(tag.getString("fluxOwner"));
        } catch (IllegalArgumentException ignored) {}
        ownerName = tag.getString("fluxOwnerName");
        buffer.restore(tag.getLong("fluxBuffer"));
        chunkLoading = tag.getBoolean("fluxChunkLoading");
        initialized = false;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        tag.setInteger("fluxConnections", visualConnections);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager manager, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
        visualConnections = packet.func_148857_g()
            .getInteger("fluxConnections");
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public ModularScreen createScreen(PosGuiData data, ModularPanel panel) {
        return new com.xyp.gtnotgood.client.flux.FluxConnectorScreen(ModList.GTNotGood.getID(), panel);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return FluxConnectorGui.build(this, sync);
    }
}
