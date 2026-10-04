package com.xyp.gtnotgood.mixins.late.Gregtech.wireless;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.wireless.WirelessCompatibility;
import com.xyp.gtnotgood.common.wireless.WirelessControllerAccess;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeAttempt;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeScheduler;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class CrossRecipeControllerMixin implements WirelessControllerAccess {

    @Unique
    private final WirelessRecipeScheduler gtng$wireless = new WirelessRecipeScheduler();

    @Override
    public WirelessRecipeScheduler gtng$getWirelessScheduler() {
        return gtng$wireless;
    }

    @Override
    @Accessor("processingLogic")
    public abstract ProcessingLogic gtng$getProcessingLogic();

    @Override
    @Invoker("checkRecipe")
    public abstract boolean gtng$checkRecipe();

    @Override
    @Invoker("doRandomMaintenanceDamage")
    public abstract boolean gtng$maintenance();

    @Override
    @Invoker("outputAfterRecipe")
    public abstract void gtng$outputAfterRecipe();

    @Inject(method = "runMachine", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtng$run(IGregTechTileEntity tile, long tick, CallbackInfo ci) {
        if (gtng$wireless.tick((MTEMultiBlockBase) (Object) this, tile, tick)) ci.cancel();
    }

    /** Custom loops must not consume materials through the native entry without a scheduling transaction. */
    @Inject(method = "checkRecipe()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtng$requireScheduler(CallbackInfoReturnable<Boolean> cir) {
        MTEMultiBlockBase machine = (MTEMultiBlockBase) (Object) this;
        if (WirelessRecipeAttempt.current() == null && WirelessRecipeScheduler.find(machine) != null) {
            machine.setCheckRecipeResult(CheckRecipeResultRegistry.NO_RECIPE);
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "checkRecipe()Z",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEMultiBlockBase;endRecipeProcessing()V"),
        require = 1)
    private void gtng$commitInputs(CallbackInfoReturnable<Boolean> cir) {
        WirelessRecipeAttempt attempt = WirelessRecipeAttempt.current();
        MTEMultiBlockBase machine = (MTEMultiBlockBase) (Object) this;
        if (attempt != null && attempt.belongsTo(machine)
            && machine.getCheckRecipeResult()
                .wasSuccessful()
            && !attempt.commit()) machine.setCheckRecipeResult(CheckRecipeResultRegistry.INTERNAL_ERROR);
    }

    @Inject(method = "saveNBTData", at = @At("RETURN"), require = 1)
    private void gtng$save(NBTTagCompound tag, CallbackInfo ci) {
        if (gtng$wireless.size() > 0) tag.setTag("gtngCrossRecipes", gtng$wireless.save());
        else tag.removeTag("gtngCrossRecipes");
    }

    /** Sorting a real input bus before committing its snapshot can invalidate quantities or split large stacks. */
    @Inject(method = "updateSlots", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtng$deferSlotUpdates(CallbackInfo ci) {
        WirelessRecipeAttempt attempt = WirelessRecipeAttempt.current();
        if (attempt != null && attempt.belongsTo((MTEMultiBlockBase) (Object) this) && attempt.defersSlotUpdates())
            ci.cancel();
    }

    @Inject(method = "loadNBTData", at = @At("RETURN"), require = 1)
    private void gtng$load(NBTTagCompound tag, CallbackInfo ci) {
        gtng$wireless.load(tag.getCompoundTag("gtngCrossRecipes"));
    }

    @Inject(method = "getWailaNBTData", at = @At("RETURN"), require = 1)
    private void gtng$hud(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y, int z,
        CallbackInfo ci) {
        if (gtng$wireless.size() == 0 && WirelessRecipeScheduler.find((MTEMultiBlockBase) (Object) this) == null)
            return;
        tag.setInteger("gtngCrossCount", gtng$wireless.size());
        tag.setString(
            "gtngCrossParallels",
            gtng$wireless.parallelCount()
                .toString());
        tag.setInteger(
            "gtngCrossStatus",
            WirelessCompatibility.supports((MTEMultiBlockBase) (Object) this, gtng$getProcessingLogic())
                ? gtng$wireless.status()
                : 3);
    }

    @Inject(method = "getWailaBody", at = @At("RETURN"), require = 1)
    private void gtng$tooltip(ItemStack stack, List<String> lines, IWailaDataAccessor accessor,
        IWailaConfigHandler config, CallbackInfo ci) {
        NBTTagCompound tag = accessor.getNBTData();
        if (!tag.hasKey("gtngCrossCount")) return;
        // #tr gtng.cross_wireless.running
        // # Concurrent wireless recipes: %s
        // # zh_CN 无线配方任务数：%s
        lines.add(
            StatCollector.translateToLocalFormatted("gtng.cross_wireless.running", tag.getInteger("gtngCrossCount")));
        // #tr gtng.cross_wireless.total_parallel
        // # Total queued parallels: %s
        // # zh_CN 任务总并行数：%s
        lines.add(
            StatCollector
                .translateToLocalFormatted("gtng.cross_wireless.total_parallel", tag.getString("gtngCrossParallels")));
        switch (tag.getInteger("gtngCrossStatus")) {
            case 1:
                // #tr gtng.cross_wireless.power
                // # Paused: insufficient wireless EU
                // # zh_CN 已暂停：无线电网电量不足
                lines.add(StatCollector.translateToLocal("gtng.cross_wireless.power"));
                break;
            case 2:
                // #tr gtng.cross_wireless.missing
                // # Paused: original owner's wireless hatch required
                // # zh_CN 已暂停：需要原仓主的无线能源仓
                lines.add(StatCollector.translateToLocal("gtng.cross_wireless.missing"));
                break;
            case 3:
                // #tr gtng.cross_wireless.unsupported
                // # This custom controller needs a cross-recipe adapter
                // # zh_CN 此特殊控制器尚未适配跨配方运行
                lines.add(StatCollector.translateToLocal("gtng.cross_wireless.unsupported"));
                break;
            case 4:
                // #tr gtng.cross_wireless.output
                // # Completed outputs waiting for space
                // # zh_CN 已完成的产物正在等待输出空间
                lines.add(StatCollector.translateToLocal("gtng.cross_wireless.output"));
                break;
            default:
                break;
        }
    }
}
