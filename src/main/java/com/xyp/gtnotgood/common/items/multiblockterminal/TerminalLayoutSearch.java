package com.xyp.gtnotgood.common.items.multiblockterminal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalStructureCapture.Position;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

final class TerminalLayoutSearch {

    private TerminalLayoutSearch() {}

    static List<Position> find(IConstructable constructable, ItemStack trigger, EntityPlayerMP player, boolean mirror,
        TerminalItemSource source) {
        AutoPlaceEnvironment environment = AutoPlaceEnvironment
            .fromLegacy((predicate, simulate, count) -> source.take(predicate, true, count), player, ignored -> {});
        IGregTechTileEntity controller = constructable instanceof IMetaTileEntity machine
            ? machine.getBaseMetaTileEntity()
            : null;
        List<Position> best = List.of();
        int bestScore = 0;
        Set<Object> seen = new HashSet<>();
        try (var scope = TerminalOperations.beginLayoutSearch()) {
            for (int count = 1; count <= TerminalSettings.MAX_COUNT; count++) {
                ItemStack candidate = trigger.copy();
                candidate.stackSize = count;
                var layout = TerminalOperations.layout(constructable, candidate, player, mirror);
                if (!seen.add(layout.signature()) || layout.positions().isEmpty()) continue;
                Map<Coordinate, Position> unique = new LinkedHashMap<>();
                for (Position position : layout.positions())
                    unique.put(new Coordinate(position.world(), position.x(), position.y(), position.z()), position);
                int score = 0;
                for (Position position : unique.values()) {
                    World world = position.world();
                    if (world != player.worldObj || !world.blockExists(position.x(), position.y(), position.z()))
                        continue;
                    if (
                        controller != null && controller.getXCoord() == position.x()
                            && controller.getYCoord() == position.y()
                            && controller.getZCoord() == position.z()
                    ) continue;
                    var element = position.element();
                    if (
                        element == StructureUtility.isAir() || element == StructureUtility.notAir()
                            || element == StructureUtility.error()
                    ) continue;
                    if (world.isAirBlock(position.x(), position.y(), position.z())) continue;
                    ItemStack existing = TerminalOperations.pick(position, player);
                    if (existing != null && TerminalOperations.matches(position, existing, candidate, environment))
                        score++;
                }
                if (score > bestScore || score == bestScore && score > 0 && unique.size() < best.size()) {
                    bestScore = score;
                    best = new ArrayList<>(unique.values());
                }
            }
        }
        return best;
    }

    private record Coordinate(World world, int x, int y, int z) {}
}
