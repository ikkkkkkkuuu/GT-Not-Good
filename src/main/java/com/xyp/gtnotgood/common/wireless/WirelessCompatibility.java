package com.xyp.gtnotgood.common.wireless;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;

/**
 * Optional mods are identified without linking their classes. Custom recipe loops need an explicit adapter instead
 * of silently losing their machine-specific checks. Results are cached per controller and logic class.
 */
public final class WirelessCompatibility {

    private static final Set<String> CONTROLLERS = new HashSet<>(
        Arrays.asList(
            MTEMultiBlockBase.class.getName(),
            "com.Nxer.TwistSpaceTechnology.common.machine.multiMachineClasses.GTCM_MultiMachineBase",
            "com.science.gtnl.common.machine.multiMachineBase.MultiMachineBase",
            "com.science.gtnl.common.machine.multiMachineBase.GTMMultiMachineBase"));
    private static final Set<String> HELPERS = new HashSet<>(
        Arrays.asList(
            ProcessingLogic.class.getName(),
            "com.Nxer.TwistSpaceTechnology.common.machine.multiMachineClasses.processingLogics.GTCM_ProcessingLogic",
            "com.science.gtnl.utils.recipes.GTNLProcessingLogic"));
    private static final ClassValue<Boolean> CONTROLLER_SUPPORT = new ClassValue<Boolean>() {

        @Override
        protected Boolean computeValue(Class<?> type) {
            String running = declaring(type, "onRunningTick", ItemStack.class);
            String postCheck = declaring(type, "postCheckRecipe", CheckRecipeResult.class, ProcessingLogic.class);
            return CONTROLLERS.contains(declaring(type, "checkProcessing"))
                && CONTROLLERS.contains(declaring(type, "doCheckRecipe"))
                && CONTROLLERS.contains(declaring(type, "checkRecipeForCustomHatches", CheckRecipeResult.class))
                && CONTROLLERS.contains(declaring(type, "runMachine", IGregTechTileEntity.class, long.class))
                && (running.equals(MTEMultiBlockBase.class.getName())
                    || running.equals(MTEExtendedPowerMultiBlockBase.class.getName()))
                && (CONTROLLERS.contains(postCheck) || postCheck.equals(MTEExtendedPowerMultiBlockBase.class.getName()))
                && declaring(type, "outputAfterRecipe").equals(MTEMultiBlockBase.class.getName());
        }
    };
    private static final ClassValue<Boolean> LOGIC_SUPPORT = new ClassValue<Boolean>() {

        @Override
        protected Boolean computeValue(Class<?> type) {
            String process = declaring(type, "process");
            return HELPERS.contains(declaring(type, "createParallelHelper", GTRecipe.class))
                && HELPERS.contains(declaring(type, "onRecipeStart", GTRecipe.class))
                && HELPERS.contains(
                    declaring(
                        type,
                        "applyRecipe",
                        GTRecipe.class,
                        ParallelHelper.class,
                        OverclockCalculator.class,
                        CheckRecipeResult.class))
                && (HELPERS.contains(process) || process.equals(
                    "com.Nxer.TwistSpaceTechnology.common.machine.multiMachineClasses.GTCM_MultiMachineBase$1"));
        }
    };

    private WirelessCompatibility() {}

    public static boolean supports(MTEMultiBlockBase machine, ProcessingLogic logic) {
        return logic != null && CONTROLLER_SUPPORT.get(machine.getClass()) && LOGIC_SUPPORT.get(logic.getClass());
    }

    private static String declaring(Class<?> type, String name, Class<?>... args) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, args);
                return method.getDeclaringClass()
                    .getName();
            } catch (NoSuchMethodException ignored) {} catch (LinkageError unavailableOptionalApi) {
                return "";
            }
        }
        return "";
    }
}
