package com.xyp.gtnotgood.mixins.late.gregtech.wireless;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.DynamicLinkedSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtnotgood.common.wireless.WirelessControllerAccess;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeDisplay;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeScheduler;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.GTUtility;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

@Mixin(value = MTEMultiBlockBaseGui.class, remap = false)
public abstract class CrossRecipeGuiMixin {

    @Shadow
    @Final
    protected MTEMultiBlockBase multiblock;

    @Inject(method = "registerSyncValues", at = @At("RETURN"), require = 1)
    private void gtng$syncOutputs(PanelSyncManager manager, CallbackInfo ci) {
        WirelessRecipeScheduler scheduler = ((WirelessControllerAccess) multiblock).gtng$getWirelessScheduler();
        StringSyncValue energy = new StringSyncValue(
            () -> scheduler.size() == 0 ? "" : WirelessRecipeDisplay.number(scheduler.displayedEU(), true),
            ignored -> {});
        manager.syncValue("gtngWirelessEU", energy);
        GenericListSyncHandler<NBTTagCompound> outputs = new GenericListSyncHandler<>(
            () -> scheduler.displayedOutputs(128),
            ignored -> {},
            PacketBuffer::readNBTTagCompoundFromBuffer,
            PacketBuffer::writeNBTTagCompoundToBuffer,
            Objects::equals,
            value -> (NBTTagCompound) value.copy());
        manager.syncValue("gtngWirelessOutputs", outputs);
        manager.syncValue(
            "gtngWirelessOutputWidget",
            new DynamicLinkedSyncHandler<>(outputs).widgetProvider((sync, value) -> {
                Flow column = Flow.column()
                    .coverChildrenHeight(0)
                    .crossAxisAlignment(Alignment.CrossAxis.START);
                for (NBTTagCompound row : value.getValue()) {
                    ItemStack icon;
                    if (row.hasKey("item")) icon = ItemStack.loadItemStackFromNBT(row.getCompoundTag("item"));
                    else icon = GTUtility
                        .getFluidDisplayStack(FluidStack.loadFluidStackFromNBT(row.getCompoundTag("fluid")), false);
                    String name = WirelessRecipeDisplay.name(row);
                    String amount = "x " + EnumChatFormatting.GOLD
                        + WirelessRecipeDisplay.amount(row, true)
                        + EnumChatFormatting.WHITE
                        + " ("
                        + WirelessRecipeDisplay.rate(row, true)
                        + ")";
                    column.child(
                        Flow.row()
                            .fullWidth()
                            .height(15)
                            .child(
                                new ItemDisplayWidget().item(icon)
                                    .displayAmount(false)
                                    .disableThemeBackground(true)
                                    .size(14)
                                    .marginRight(2))
                            .child(
                                Flow.column()
                                    .coverChildrenHeight(0)
                                    .crossAxisAlignment(Alignment.CrossAxis.START)
                                    .child(
                                        new TextWidget<>(IKey.str(EnumChatFormatting.AQUA + name)).height(8)
                                            .scale(0.75f))
                                    .child(
                                        new TextWidget<>(IKey.str(amount)).height(6)
                                            .scale(0.6f))
                                    .tooltip(
                                        t -> t.addLine(
                                            name + "\n"
                                                + WirelessRecipeDisplay.amount(row, false)
                                                + " ("
                                                + WirelessRecipeDisplay.rate(row, false)
                                                + ")"))));
                }
                return column;
            }));
    }

    @Inject(method = "createRecipeInfoWidget", at = @At("RETURN"), cancellable = true, require = 1)
    private void gtng$showOutputs(PanelSyncManager manager, CallbackInfoReturnable<IWidget> cir) {
        StringSyncValue energy = manager.findSyncHandler("gtngWirelessEU", StringSyncValue.class);
        DynamicLinkedSyncHandler<?> outputs = manager
            .findSyncHandler("gtngWirelessOutputWidget", DynamicLinkedSyncHandler.class);
        cir.setReturnValue(
            Flow.column()
                .fullWidth()
                .coverChildrenHeight(0)
                .crossAxisAlignment(Alignment.CrossAxis.START)
                .child(cir.getReturnValue())
                .child(IKey.dynamic(() -> {
                    // #tr gtng.cross_wireless.eu
                    // # Current consumption: %s EU/t (wireless)
                    // # zh_CN 当前耗电：%s EU/t（无线电网）
                    return EnumChatFormatting.WHITE + StatCollector.translateToLocalFormatted(
                        "gtng.cross_wireless.eu",
                        EnumChatFormatting.GOLD + energy.getValue() + EnumChatFormatting.WHITE);
                })
                    .asWidget()
                    .scale(0.75f)
                    .fullWidth()
                    .marginBottom(2)
                    .setEnabledIf(
                        widget -> !energy.getValue()
                            .isEmpty()))
                .child(
                    new DynamicSyncedWidget<>().syncHandler(outputs)
                        .widthRel(0.85f)
                        .coverChildrenHeight(0)));
    }
}
