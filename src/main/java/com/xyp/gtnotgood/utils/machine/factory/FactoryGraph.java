package com.xyp.gtnotgood.utils.machine.factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Bounded, server-owned production graph. Recipe identities refer to the server recipe catalog. */
public final class FactoryGraph {

    public static final int MAX_NODES = 32;
    public static final int MAX_PARALLEL = Integer.MAX_VALUE;

    /** GT stack counts use signed integers; multiply in long before saturating the effective limit. */
    public static int effectiveParallel(int node, int global) {
        return (int) Math.min(MAX_PARALLEL, (long) Math.max(1, node) * Math.max(1, global));
    }

    public final List<Node> nodes = new ArrayList<>();
    private int nextId;

    /** One recipe and its incoming material routes; coordinates belong only to the editor. */
    public static final class Node {

        public int id;
        public String recipe = "";
        public int x;
        public int y;
        public int parallel = 1;
        public int overclocks;
        public boolean target;
        /** -1 uses the imported recipe EU/t; non-negative values override the pre-overclock cost. */
        public long customEUt = -1;
        public final Set<Integer> sources = new HashSet<>();

        public NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("id", id);
            tag.setString("recipe", recipe);
            tag.setInteger("x", x);
            tag.setInteger("y", y);
            tag.setInteger("parallel", parallel);
            tag.setInteger("oc", overclocks);
            tag.setBoolean("target", target);
            tag.setLong("customEUt", customEUt);
            tag.setIntArray(
                "sources",
                sources.stream()
                    .sorted()
                    .mapToInt(Integer::intValue)
                    .toArray());
            return tag;
        }
    }

    public Node find(int id) {
        for (Node node : nodes) if (node.id == id) return node;
        return null;
    }

    public void add(String recipe) {
        if (nodes.size() >= MAX_NODES || nextId == Integer.MAX_VALUE) return;
        Node node = new Node();
        node.id = nextId++;
        node.recipe = recipe;
        node.x = (nodes.size() % 3) * 112 + 8;
        node.y = (nodes.size() / 3) * 55 + 8;
        nodes.add(node);
    }

    public void remove(int id) {
        nodes.removeIf(node -> node.id == id);
        for (Node node : nodes) node.sources.remove(id);
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Node node : nodes) list.appendTag(node.write());
        tag.setTag("nodes", list);
        tag.setInteger("next", nextId);
        return tag;
    }

    /** Reads a bounded graph and drops dangling routes; runtime contents are stored separately. */
    public void read(NBTTagCompound tag) {
        nodes.clear();
        nextId = Math.max(0, tag.getInteger("next"));
        NBTTagList list = tag.getTagList("nodes", 10);
        for (int i = 0; i < Math.min(MAX_NODES, list.tagCount()); i++) {
            NBTTagCompound data = list.getCompoundTagAt(i);
            int id = data.getInteger("id");
            if (id < 0 || id == Integer.MAX_VALUE || find(id) != null) continue;
            Node node = new Node();
            node.id = id;
            node.recipe = data.getString("recipe");
            node.target = data.getBoolean("target");
            node.x = Math.max(0, Math.min(2048, data.getInteger("x")));
            node.y = Math.max(0, Math.min(2048, data.getInteger("y")));
            node.parallel = Math.max(1, data.getInteger("parallel"));
            node.customEUt = data.hasKey("customEUt") ? Math.max(-1L, data.getLong("customEUt")) : -1L;
            node.overclocks = Math.max(0, Math.min(14, data.getInteger("oc")));
            for (int source : data.getIntArray("sources")) {
                if (node.sources.size() < MAX_NODES) node.sources.add(source);
            }
            nodes.add(node);
            nextId = Math.max(nextId, id + 1);
        }
        for (Node node : nodes) node.sources.removeIf(source -> find(source) == null);
    }

    /** Every connected production component must identify at least one target recipe node. */
    public boolean hasTargetsForAllComponents() {
        if (nodes.isEmpty()) return false;
        Set<Integer> visited = new HashSet<>();
        for (Node start : nodes) {
            if (visited.contains(start.id)) continue;
            List<Node> queue = new ArrayList<>();
            queue.add(start);
            visited.add(start.id);
            boolean targetFound = false;
            for (int i = 0; i < queue.size(); i++) {
                Node current = queue.get(i);
                targetFound |= current.target;
                for (Node other : nodes) {
                    if ((current.sources.contains(other.id) || other.sources.contains(current.id))
                        && visited.add(other.id)) queue.add(other);
                }
            }
            if (!targetFound) return false;
        }
        return true;
    }

    public FactoryGraph copy() {
        FactoryGraph result = new FactoryGraph();
        result.read(write());
        return result;
    }

    /** Perfect overclocking stops at one tick; exact arithmetic rejects overflowing configurations. */
    public static long[] timing(long eut, int duration, int parallel, int overclocks) {
        long power = Math.multiplyExact(eut, parallel);
        int ticks = Math.max(1, duration);
        for (int i = 0; i < overclocks && ticks > 1; i++) {
            power = Math.multiplyExact(power, 4L);
            ticks = Math.max(1, ticks / 4);
        }
        return new long[] { power, ticks };
    }
}
