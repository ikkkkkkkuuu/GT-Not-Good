package com.xyp.gtnotgood.common.gui.modularui;

import java.util.function.Function;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;

/**
 * Small position-based ModularUI2 factory for extra meta-tile entity screens.
 * <p>
 * GregTech already handles the normal right-click machine GUI. This factory is for secondary controller screens opened
 * from a button or synced action, carrying only the controller block position over the network.
 *
 * @param <M> meta-tile entity type accepted by this factory
 */
public class PosMetaTileGuiFactory<M extends MetaTileEntity> extends AbstractUIFactory<PosGuiData> {

    private static final int MAX_INTERACTION_DISTANCE = 64;

    private final Class<M> machineType;
    private final Function<M, IGuiHolder<PosGuiData>> guiCtor;
    private final String machineDisplayName;

    public PosMetaTileGuiFactory(String guiId, Class<M> machineType, Function<M, IGuiHolder<PosGuiData>> guiCtor,
        String machineDisplayName) {
        super(guiId);
        this.machineType = machineType;
        this.guiCtor = guiCtor;
        this.machineDisplayName = machineDisplayName;
    }

    /**
     * Opens this factory for a real server player near the supplied controller.
     *
     * @param player  player receiving the screen
     * @param machine controller instance that owns the screen state
     */
    public void openGui(EntityPlayer player, M machine) {
        if (!(player instanceof EntityPlayerMP playerMP)) return;
        if (player instanceof FakePlayer) return;
        IGregTechTileEntity base = machine.getBaseMetaTileEntity();
        if (base == null) return;
        PosGuiData data = new PosGuiData(player, base.getXCoord(), base.getYCoord(), base.getZCoord());
        GuiManager.open(this, data, playerMP);
    }

    @Override
    public @Nonnull IGuiHolder<PosGuiData> getGuiHolder(PosGuiData data) {
        TileEntity te = data.getTileEntity();
        if (te instanceof IGregTechTileEntity gte && machineType.isInstance(gte.getMetaTileEntity())) {
            return guiCtor.apply(machineType.cast(gte.getMetaTileEntity()));
        }
        throw new IllegalStateException(
            String.format(
                "TileEntity at (%s, %s, %s) is not a %s!",
                data.getX(),
                data.getY(),
                data.getZ(),
                machineDisplayName));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player, PosGuiData guiData) {
        return super.canInteractWith(player, guiData) && guiData.getTileEntity() instanceof IGregTechTileEntity base
            && base.canAccessData()
            && guiData.getSquaredDistance(player) <= MAX_INTERACTION_DISTANCE;
    }

    @Override
    public void writeGuiData(PosGuiData guiData, PacketBuffer buffer) {
        buffer.writeVarIntToBuffer(guiData.getX());
        buffer.writeVarIntToBuffer(guiData.getY());
        buffer.writeVarIntToBuffer(guiData.getZ());
    }

    @Override
    public @Nonnull PosGuiData readGuiData(EntityPlayer player, PacketBuffer buffer) {
        return new PosGuiData(
            player,
            buffer.readVarIntFromBuffer(),
            buffer.readVarIntFromBuffer(),
            buffer.readVarIntFromBuffer());
    }
}
