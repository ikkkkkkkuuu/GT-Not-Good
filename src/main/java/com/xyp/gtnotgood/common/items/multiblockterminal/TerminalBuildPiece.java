// SPDX-License-Identifier: GPL-3.0-only
// StructureLib 1.4.45 navigation and survival-build API adaptation.
package com.xyp.gtnotgood.common.items.multiblockterminal;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElementNoPlacement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalStructureCapture.Position;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.common.misc.GTStructureChannels;

public final class TerminalBuildPiece implements IStructureDefinition<Object> {

    private static final String PIECE = "terminal";
    private final Object context;
    private final IStructureElement<Object>[] elements;
    private final World world;
    private final ExtendedFacing facing;
    private final int x, y, z, a, b, c;
    private final Set<Coordinate> occupied = new HashSet<>();
    private TerminalItemSource refunds;

    @SuppressWarnings("unchecked")
    public TerminalBuildPiece(Object context, IStructureElement<?>[] original, World world, ExtendedFacing facing,
        int x, int y, int z, int a, int b, int c, boolean mirror) {
        this.context = context;
        this.world = world;
        this.facing = mirror ? facing.with(mirrored(facing.getFlip())) : facing;
        this.x = x;
        this.y = y;
        this.z = z;
        this.a = a;
        this.b = b;
        this.c = c;
        elements = (IStructureElement<Object>[]) new IStructureElement<?>[original.length];
        int[] abc = { -a, -b, -c };
        for (int index = 0; index < original.length; index++) {
            IStructureElement<Object> element = (IStructureElement<Object>) original[index];
            if (element.isNavigating()) {
                elements[index] = element;
                abc[0] = (element.resetA() ? -a : abc[0]) + element.getStepA();
                abc[1] = (element.resetB() ? -b : abc[1]) + element.getStepB();
                abc[2] = (element.resetC() ? -c : abc[2]) + element.getStepC();
            } else {
                elements[index] = new Element(element);
                occupied.add(new Coordinate(abc[0] + a, abc[1] + b, abc[2] + c));
                abc[0]++;
            }
        }
        occupied.add(new Coordinate(a, b, c));
    }

    public int build(ItemStack terminal, ItemStack trigger, EntityPlayerMP player, IItemSource materials,
        TerminalItemSource refunds) {
        ItemStack buildTrigger = trigger.copy();
        String hatchChannel = GTStructureChannels.HATCH.get();
        if (TerminalSettings.noHatches(terminal)) ChannelDataAccessor.unsetChannelData(buildTrigger, hatchChannel);
        else if (!ChannelDataAccessor.hasSubChannel(buildTrigger, hatchChannel))
            ChannelDataAccessor.setChannelData(buildTrigger, hatchChannel, 1);
        this.refunds = refunds;
        try {
            return survivalBuild(context, buildTrigger, PIECE, world, facing, x, y, z, a, b, c, Integer.MAX_VALUE,
                ISurvivalBuildEnvironment.create(materials, player), true);
        } finally {
            this.refunds = null;
        }
    }

    @Override
    public IStructureElement<Object>[] getStructureFor(String name) {
        return elements;
    }

    @Override
    public boolean isContainedInStructure(String name, int offsetA, int offsetB, int offsetC) {
        return occupied.contains(new Coordinate(offsetA, offsetB, offsetC));
    }

    private static Flip mirrored(Flip original) {
        if (original.isVerticallyFliped()) return original.isHorizontallyFlipped() ? Flip.VERTICAL : Flip.BOTH;
        return original.isHorizontallyFlipped() ? Flip.NONE : Flip.HORIZONTAL;
    }

    private final class Element implements IStructureElement<Object> {

        private final IStructureElement<Object> original;

        private Element(IStructureElement<Object> original) {
            this.original = original;
        }

        @Override
        public boolean check(Object object, World world, int x, int y, int z) {
            return original.check(object, world, x, y, z);
        }

        @Override
        public boolean couldBeValid(Object object, World world, int x, int y, int z, ItemStack trigger) {
            return original.couldBeValid(object, world, x, y, z, trigger);
        }

        @Override
        public boolean spawnHint(Object object, World world, int x, int y, int z, ItemStack trigger) {
            return original.spawnHint(object, world, x, y, z, trigger);
        }

        @Override
        public boolean placeBlock(Object object, World world, int x, int y, int z, ItemStack trigger) {
            return original.placeBlock(object, world, x, y, z, trigger);
        }

        @Override
        public BlocksToPlace getBlocksToPlace(Object object, World world, int x, int y, int z, ItemStack trigger,
            AutoPlaceEnvironment environment) {
            return original.getBlocksToPlace(object, world, x, y, z, trigger, environment);
        }

        @Override
        public PlaceResult survivalPlaceBlock(Object object, World world, int x, int y, int z, ItemStack trigger,
            AutoPlaceEnvironment environment) {
            if (!TerminalOperations.shouldBuild(object, original, world, x, y, z)) return PlaceResult.SKIP;
            if (
                x == TerminalBuildPiece.this.x && y == TerminalBuildPiece.this.y && z == TerminalBuildPiece.this.z
                    || original == StructureUtility.isAir()
                    || original == StructureUtility.error()
                    || original instanceof IStructureElementNoPlacement<?>
            ) return PlaceResult.SKIP;
            TileEntity tile = world.getTileEntity(x, y, z);
            if (
                TerminalSettings.noHatches(trigger) && tile instanceof IGregTechTileEntity gtTile
                    && gtTile.getMetaTileEntity() instanceof MTEHatch
            ) return PlaceResult.SKIP;
            if (!StructureLibAPI.isBlockTriviallyReplaceable(world, x, y, z, environment.getActor())) {
                original.check(object, world, x, y, z);
                return PlaceResult.SKIP;
            }
            Position position = new Position(object, original, world, x, y, z);
            var hint = TerminalHintProbe.probe(position, trigger);
            EntityPlayerMP player = (EntityPlayerMP) environment.getActor();
            boolean placed = TerminalFluidOperations.handles(hint)
                ? TerminalFluidOperations.place(position, hint, trigger, player, refunds)
                : TerminalPlacement.place(position, trigger, player, environment.getSource(), refunds, environment);
            return placed ? PlaceResult.ACCEPT : PlaceResult.REJECT_CONTINUE;
        }
    }

    private record Coordinate(int a, int b, int c) {}
}
