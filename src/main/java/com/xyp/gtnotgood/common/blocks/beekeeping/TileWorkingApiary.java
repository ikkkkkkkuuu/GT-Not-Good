package com.xyp.gtnotgood.common.blocks.beekeeping;

import java.util.Collection;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.config.Config;

import forestry.api.apiculture.DefaultBeeModifier;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeModifier;
import forestry.apiculture.tiles.TileApiary;

/**
 * Runs the native apiary lifecycle with a bounded server-side clock. Inventory, GUI, ownership, frames,
 * environmental checks, offspring and Forestry network packets remain managed by the parent implementation.
 */
public final class TileWorkingApiary extends TileApiary {

    private double workRemainder;
    private final IBeeModifier workingModifier = new WorkingBeeModifier();

    @Override
    public String getUnlocalizedTitle() {
        // #tr tile.working_apiary.0.name
        // # Working Apiary
        // # zh_CN 工作蜂箱
        return "tile.working_apiary.0.name";
    }

    /**
     * Advances complete native ticks only, rechecking errors between steps. Failed work never accumulates a
     * catch-up backlog; fractional speed is saved across chunk unloads. At 1x, exactly one native update is run.
     */
    @Override
    public void updateServerSide() {
        workRemainder += Config.workingApiarySpeed;
        int steps = (int) workRemainder;
        workRemainder -= steps;
        if (steps == 0) {
            getBeekeepingLogic().canWork();
            return;
        }
        for (int i = 0; i < steps; i++) {
            if (!getBeekeepingLogic().canWork()) break;
            getBeekeepingLogic().doWork();
        }
    }

    @Override
    public Collection<IBeeModifier> getBeeModifiers() {
        Collection<IBeeModifier> modifiers = super.getBeeModifiers();
        modifiers.add(workingModifier);
        return modifiers;
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setDouble("WorkingApiaryRemainder", workRemainder);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        double saved = data.getDouble("WorkingApiaryRemainder");
        workRemainder = Double.isFinite(saved) && saved >= 0 && saved < 1 ? saved : 0;
    }

    /** Multiplicative modifiers compose with native apiary, frame and beekeeping-mode modifiers. */
    private static final class WorkingBeeModifier extends DefaultBeeModifier {

        @Override
        public float getLifespanModifier(IBeeGenome genome, IBeeGenome mate, float currentModifier) {
            return Config.workingApiaryLifespan;
        }

        @Override
        public float getMutationModifier(IBeeGenome genome, IBeeGenome mate, float currentModifier) {
            return Config.workingApiaryMutation;
        }
    }
}
