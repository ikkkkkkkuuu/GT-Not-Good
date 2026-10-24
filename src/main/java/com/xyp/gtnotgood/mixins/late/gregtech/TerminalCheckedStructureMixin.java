package com.xyp.gtnotgood.mixins.late.gregtech;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalOperations;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.structure.error.StructureError;

@Mixin(value = MTEEnhancedMultiBlockBase.class, remap = false)
public abstract class TerminalCheckedStructureMixin {

    @Inject(method = "checkPiece(Ljava/lang/String;III)Z", at = @At("HEAD"), require = 1, allow = 1)
    private void gtnotgood$prepareCheckedPiece(String piece, int a, int b, int c, CallbackInfoReturnable<Boolean> cir) {
        if (TerminalOperations.isCapturingChecks()) gtnotgood$collectPiece(piece, a, b, c, true, false);
    }

    @Inject(method = "checkPiece(Ljava/lang/String;IIILjava/util/List;)Z", at = @At("HEAD"), require = 1, allow = 1)
    private void gtnotgood$prepareCheckedPieceWithErrors(String piece, int a, int b, int c, List<StructureError> errors,
        CallbackInfoReturnable<Boolean> cir) {
        if (TerminalOperations.isCapturingChecks()) gtnotgood$collectPiece(piece, a, b, c, true, false);
    }

    @Inject(method = "checkPiece(Ljava/lang/String;III)Z", at = @At("RETURN"), require = 1, allow = 1)
    private void gtnotgood$captureCheckedPiece(String piece, int a, int b, int c, CallbackInfoReturnable<Boolean> cir) {
        if (TerminalOperations.isCapturingChecks()) gtnotgood$collectPiece(piece, a, b, c, false, cir.getReturnValue());
    }

    @Inject(method = "checkPiece(Ljava/lang/String;IIILjava/util/List;)Z", at = @At("RETURN"), require = 1, allow = 1)
    private void gtnotgood$captureCheckedPieceWithErrors(String piece, int a, int b, int c, List<StructureError> errors,
        CallbackInfoReturnable<Boolean> cir) {
        if (TerminalOperations.isCapturingChecks()) gtnotgood$collectPiece(piece, a, b, c, false, cir.getReturnValue());
    }

    @Unique
    private void gtnotgood$collectPiece(String piece, int a, int b, int c, boolean prepare, boolean successful) {
        MTEEnhancedMultiBlockBase<?> machine = (MTEEnhancedMultiBlockBase<?>) (Object) this;
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        IStructureElement<?>[] elements = machine.getStructureDefinition().getStructureFor(piece);
        if (prepare) TerminalOperations.beginCheckedPiece(machine, elements, tile.getWorld(),
            machine.getExtendedFacing(), tile.getXCoord(), tile.getYCoord(), tile.getZCoord(), a, b, c);
        else TerminalOperations.finishCheckedPiece(machine, elements, tile.getWorld(), machine.getExtendedFacing(),
            tile.getXCoord(), tile.getYCoord(), tile.getZCoord(), a, b, c, successful);
    }
}
