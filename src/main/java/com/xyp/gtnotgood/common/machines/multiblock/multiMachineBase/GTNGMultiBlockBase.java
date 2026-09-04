package com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * Shared base for GT Not Good electric multiblocks with the mod's modern ModularUI skin.
 * <p>
 * New electric multiblock controllers should extend this class by default. It keeps common GregTech behavior from
 * {@link MTEExtendedPowerMultiBlockBase} while swapping the GUI factory to {@link GTNGModernMultiBlockBaseGui}.
 *
 * @param <T> concrete multiblock controller type used by GregTech's generic base class
 */
public abstract class GTNGMultiBlockBase<T extends GTNGMultiBlockBase<T>> extends MTEExtendedPowerMultiBlockBase<T> {

    protected GTNGMultiBlockBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    protected GTNGMultiBlockBase(String aName) {
        super(aName);
    }

    /**
     * Accepts any stack as a valid machine part for the base class.
     * <p>
     * Individual multiblocks in this project are expected to validate their real structure through StructureLib
     * elements and hatch adders. Returning true here keeps the GregTech base class from rejecting custom parts before
     * that structure-specific validation runs.
     *
     * @param stack candidate machine-part stack
     * @return always true; structure rules decide validity elsewhere
     */
    @Override
    public boolean isCorrectMachinePart(ItemStack stack) {
        return true;
    }

    /**
     * Creates the default recipe processing logic for GT Not Good multiblocks.
     * <p>
     * GregTech's generic multiblock recipe loop only runs when a controller supplies a {@link ProcessingLogic}. The
     * shared project default enables perfect overclocking, which changes the overclock ratio from normal 2x time
     * reduction / 4x EU usage to 4x time reduction / 4x EU usage. The parallel supplier uses {@link #getTrueParallel()}
     * so each machine still respects its own {@link #getMaxParallelRecipes()} value and the GregTech power panel's
     * parallel limit.
     *
     * @return processing logic with perfect overclocking enabled by default
     */
    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {}.enablePerfectOverclock()
            .setMaxParallelSupplier(this::getTrueParallel);
    }

    /**
     * Enables the output overflow protection button for GT Not Good multiblocks by default.
     * <p>
     * GregTech disables this feature unless the controller explicitly opts in. Keeping the opt-in here makes every
     * machine inheriting this base expose the voiding/protection cycle button; a specific machine can still override
     * this method and return {@code false} when output overflow behavior must be fixed.
     *
     * @return true so the GUI button is clickable and the machine can use GregTech voiding modes
     */
    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    /**
     * Enables the input separation toggle for GT Not Good multiblocks by default.
     * <p>
     * The default GregTech multiblock base returns false, which leaves the button forbidden in ModularUI. Machines with
     * recipes that must always merge all input buses should override this method and return {@code false}.
     *
     * @return true so players can toggle separated input handling from the GUI
     */
    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    /**
     * Enables the batch mode toggle for GT Not Good multiblocks by default.
     * <p>
     * Batch mode is still controlled by the normal GregTech synced state and default config value. This method only
     * declares that the controller supports the feature, allowing the GUI button to be clicked.
     *
     * @return true so players can toggle batch processing from the GUI
     */
    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    /**
     * Enables the recipe locking toggle for GT Not Good multiblocks by default.
     * <p>
     * GregTech currently enables this on the parent class, but this explicit override documents the project policy:
     * shared multiblocks expose the lock button unless a concrete machine opts out.
     *
     * @return true so players can lock the controller to a single recipe from the GUI or screwdriver interaction
     */
    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
    }

    /**
     * Creates the default ModularUI panel used by GT Not Good multiblocks.
     *
     * @return modern skinned GregTech multiblock GUI wrapper
     */
    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new GTNGModernMultiBlockBaseGui<>(this);
    }
}
