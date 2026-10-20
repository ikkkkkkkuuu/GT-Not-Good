package com.xyp.gtnotgood.mixins.late.gregtech.arcfurnace;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import gregtech.api.interfaces.IOreMaterial;
import kubatech.api.arcfurnace.ArcFurnaceProcessingEvent;
import kubatech.loaders.ArcFurnaceElectrode;

@Mixin(value = ArcFurnaceElectrode.class, remap = false)
public interface ArcFurnaceElectrodeAccess {

    @Invoker("<init>")
    static ArcFurnaceElectrode create(String name, int ordinal, int id, IOreMaterial material, double speed,
        int parallel, double ocSpeed, double ocPower, int durability, double euModifier, double startupPenalty,
        Consumer<ArcFurnaceProcessingEvent> specialEffect) {
        throw new AssertionError();
    }

    @Invoker("getModifierFormatted")
    static String formatModifier(int value, int low, int medium, int high) {
        throw new AssertionError();
    }

    @Invoker("getModifierFormatted")
    static String formatModifier(double value, boolean undesirable, double low, double medium, double high,
        String prefix, String suffix) {
        throw new AssertionError();
    }
}
