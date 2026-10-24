package com.xyp.gtnotgood.common.items.multiblockterminal;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalStructureCapture.Position;

public final class TerminalHintProbe {

    private static final ThreadLocal<Probe> activeProbe = new ThreadLocal<>();

    private TerminalHintProbe() {}

    public static Hint probe(Position position, ItemStack trigger) {
        Probe previous = activeProbe.get();
        Probe probe = new Probe(position);
        activeProbe.set(probe);
        try {
            TerminalOperations.element(position).spawnHint(position.context(), position.world(), position.x(),
                position.y(), position.z(), trigger);
            return probe.ambiguous ? null : probe.hint;
        } catch (RuntimeException | LinkageError unsupportedHint) {
            return null;
        } finally {
            if (previous == null) activeProbe.remove();
            else activeProbe.set(previous);
        }
    }

    public static boolean capture(World world, int x, int y, int z, Block block, int meta) {
        Probe probe = activeProbe.get();
        if (probe == null) return false;
        Position position = probe.position;
        if (position.world() != world || position.x() != x || position.y() != y || position.z() != z) return false;
        if (block == null || block == StructureLibAPI.getBlockHint()) return true;
        Hint hint = new Hint(block, meta);
        if (probe.hint == null) probe.hint = hint;
        else if (!probe.hint.equals(hint)) probe.ambiguous = true;
        return true;
    }

    public record Hint(Block block, int meta) {}

    private static final class Probe {

        private final Position position;
        private Hint hint;
        private boolean ambiguous;

        private Probe(Position position) {
            this.position = position;
        }
    }
}
