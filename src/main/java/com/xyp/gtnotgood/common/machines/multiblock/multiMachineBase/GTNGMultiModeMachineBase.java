package com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase;

import static gregtech.api.util.GTRecipeConstants.CLEANROOM;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.drawable.UITexture;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.processing.ModularProcessingLogic;

import gregtech.api.interfaces.ICleanroom;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public abstract class GTNGMultiModeMachineBase<T extends GTNGMultiModeMachineBase<T>>
    extends GTNGModularMachineBase<T> {

    protected GTNGMultiModeMachineBase(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected GTNGMultiModeMachineBase(String name) {
        super(name);
    }

    protected abstract int getMachineModeCount();

    protected abstract RecipeMap<?> getRecipeMap(int mode);

    protected abstract UITexture[] getMachineModeIcons();

    @Override
    public final RecipeMap<?> getRecipeMap() {
        return getRecipeMap(machineMode);
    }

    @Override
    public final Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        Collection<RecipeMap<?>> maps = new ArrayList<>(getMachineModeCount());
        for (int mode = 0; mode < getMachineModeCount(); mode++) maps.add(getRecipeMap(mode));
        return maps;
    }

    @Override
    public final boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public final int nextMachineMode() {
        return (machineMode + 1) % getMachineModeCount();
    }

    @Override
    public final void setMachineMode(int mode) {
        super.setMachineMode(mode >= 0 && mode < getMachineModeCount() ? mode : 0);
        for (var hatch : mInputHatches) hatch.mRecipeMap = getRecipeMap();
        for (var bus : mInputBusses) bus.mRecipeMap = getRecipeMap();
        setSingleRecipeCheck(null);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        machineMode = machineMode >= 0 && machineMode < getMachineModeCount() ? machineMode : 0;
    }

    @Override
    public final void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        if (getBaseMetaTileEntity().isClientSide()) return;
        if (player.isSneaking()) super.onScrewdriverRightClick(side, player, x, y, z, tool);
        else {
            setMachineMode(nextMachineMode());
            GTUtility.sendChatTrans(player, "gt.interact.desc.mb.mode", getMachineModeName());
        }
    }

    @Override
    @Nonnull
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new GTNGModernMultiBlockBaseGui<>(this).withMachineModeIcons(getMachineModeIcons());
    }

    @Override
    protected ProcessingLogic createMachineProcessingLogic() {
        return new ModularProcessingLogic() {

            @Override
            @Nonnull
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                if (
                    recipe.mSpecialValue == -200 || recipe.mSpecialValue == -300
                        || recipe.getMetadataOrDefault(CLEANROOM, false)
                ) {
                    ICleanroom cleanroom = cleanroomReference.getCleanroom();
                    if (cleanroom == null || !cleanroom.isValidCleanroom() || cleanroom.getCleanness() <= 0) {
                        // #tr GT5U.gui.text.recipe_result.gtng.cleanroom_required
                        // # Requires a cleanroom
                        // # zh_CN 需要洁净室
                        return SimpleCheckRecipeResult.ofFailure("gtng.cleanroom_required");
                    }
                }
                return validateRecipeForMode(recipe);
            }

        }.setMaxParallelSupplier(this::getTrueParallel).setMaxTierSkips(0);
    }

    protected CheckRecipeResult validateRecipeForMode(GTRecipe recipe) {
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    protected final void setProcessingLogicPower(ProcessingLogic logic) {
        long voltage = 0;
        for (var hatch : validMTEList(getExoticAndNormalEnergyHatchList()))
            voltage = Math.max(voltage, hatch.getBaseMetaTileEntity().getInputVoltage());
        logic.setAvailableVoltage(voltage).setAvailableAmperage(voltage == 0 ? 0 : getMaxInputEu() / voltage)
            .setAmperageOC(false);
    }

}
