package com.xyp.gtnotgood.common.network;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

/**
 * Server-authoritative eight-channel scheduler. In-flight cargo is persisted and preserved in the dropped controller.
 */
public class TileNetworkController extends TileNetworkNode {

    public final Channel[] channels = new Channel[8];
    private NetworkTopology topology;
    private long topologyVersion = -1;

    public TileNetworkController() {
        for (int i = 0; i < channels.length; i++) channels[i] = new Channel();
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    public NetworkTopology topology() {
        if (worldObj == null || worldObj.isRemote) return new NetworkTopology();
        long version = NetworkTopology.version(worldObj);
        if (topology == null || topologyVersion != version) {
            topology = NetworkTopology.scan(this);
            topologyVersion = version;
        }
        return topology;
    }

    public NetworkTopology.Endpoint endpoint(String key) {
        return topology().byKey.get(key);
    }

    /** A stale GUI selection cannot create rules for a disconnected device. */
    public NetworkRule rule(int channel, String key, boolean create) {
        if (channel < 0 || channel >= channels.length || endpoint(key) == null) return null;
        Channel data = channels[channel];
        NetworkRule rule = data.rules.get(key);
        if (rule == null && create && data.rules.size() < 2048) {
            rule = new NetworkRule();
            rule.rate = data.type == 0 ? 64 : 1000;
            data.rules.put(key, rule);
        }
        return rule;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        long tick = worldObj.getTotalWorldTime();
        if (topology().status != 0) return;
        Channel[] ordered = channels.clone();
        java.util.Arrays.sort(
            ordered,
            java.util.Comparator.comparingInt((Channel c) -> c.priority)
                .reversed());
        java.util.Map<gregtech.api.interfaces.tileentity.IGregTechTileEntity, Long> energyDraw = new java.util.IdentityHashMap<>();
        for (Channel channel : ordered) {
            if (channel.enabled && (channel.type == 2 || tick % channel.interval == 0)) {
                if (channel.type == 2 ? NetworkEnergyTransfer.tick(this, channel, energyDraw)
                    : NetworkTransfer.tick(this, channel)) markDirty();
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new Channel();
            if (tag.hasKey("channel" + i)) channels[i].read(tag.getCompoundTag("channel" + i));
        }
        topology = null;
        topologyVersion = -1;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        for (int i = 0; i < channels.length; i++) tag.setTag("channel" + i, channels[i].write());
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return NetworkGui.controller(this, sync);
    }

    /** Persisted channel configuration and bounded transfer buffer, with a transient round-robin cursor. */
    public static final class Channel {

        public int type;
        public int priority;
        public long energy;
        public boolean enabled;
        public int interval = 20;
        public String name = "";
        public final Map<String, NetworkRule> rules = new LinkedHashMap<>();
        public ItemStack item;
        public FluidStack fluid;
        public String source = "";
        public int cursor;

        public boolean hasCargo() {
            return item != null || fluid != null || energy > 0;
        }

        public void read(NBTTagCompound tag) {
            type = Math.max(0, Math.min(2, tag.getInteger("type")));
            priority = Math.max(-99, Math.min(99, tag.getInteger("priority")));
            energy = type == 2 ? Math.max(0, tag.getLong("energy")) : 0;
            enabled = tag.getBoolean("enabled");
            interval = Math.max(10, Math.min(200, tag.getInteger("interval") / 10 * 10));
            name = tag.getString("name");
            if (name.length() > 24) name = name.substring(0, 24);
            item = tag.hasKey("item") ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("item")) : null;
            if (item != null && tag.hasKey("itemCount")) item.stackSize = tag.getInteger("itemCount");
            if (item != null && item.stackSize <= 0) item = null;
            fluid = tag.hasKey("fluid") ? FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("fluid")) : null;
            if (fluid != null && fluid.amount <= 0) fluid = null;
            source = tag.getString("source");
            rules.clear();
            NBTTagList entries = tag.getTagList("rules", 10);
            for (int i = 0; i < Math.min(2048, entries.tagCount()); i++) {
                NBTTagCompound entry = entries.getCompoundTagAt(i);
                String key = entry.getString("key");
                if (key.length() > 64) continue;
                NetworkRule rule = new NetworkRule();
                rule.read(entry);
                rules.put(key, rule);
            }
        }

        public NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("type", type);
            tag.setInteger("priority", priority);
            tag.setLong("energy", energy);
            tag.setBoolean("enabled", enabled);
            tag.setInteger("interval", interval);
            tag.setString("name", name);
            tag.setString("source", source);
            if (item != null) {
                ItemStack sample = item.copy();
                sample.stackSize = 1;
                tag.setTag("item", sample.writeToNBT(new NBTTagCompound()));
                tag.setInteger("itemCount", item.stackSize);
            }
            if (fluid != null) tag.setTag("fluid", fluid.writeToNBT(new NBTTagCompound()));
            NBTTagList entries = new NBTTagList();
            for (Map.Entry<String, NetworkRule> entry : rules.entrySet()) {
                NBTTagCompound data = entry.getValue()
                    .write();
                data.setString("key", entry.getKey());
                entries.appendTag(data);
            }
            tag.setTag("rules", entries);
            return tag;
        }
    }
}
