// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.processing;

import static gregtech.api.util.GTUtility.validMTEList;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.MTEHatchInputME;

/** Native stocking hatches simulate independently; their shared inventory and AE energy need a combined check. */
public final class ModuleInputPreflight {

    private ModuleInputPreflight() {}

    public static boolean canCommit(MTEMultiBlockBase machine, ModularProcessingLogic logic) {
        Requests<IAEItemStack> items = new Requests<>();
        Requests<IAEFluidStack> fluids = new Requests<>();
        Map<IEnergyGrid, Long> energy = new IdentityHashMap<>();
        Set<ItemStack> seenItems = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<FluidStack> seenFluids = Collections.newSetFromMap(new IdentityHashMap<>());
        try {
            for (var input : validMTEList(machine.mInputBusses)) if (input instanceof MTEHatchInputBusME stocking) {
                AENetworkProxy proxy = stocking.getProxy();
                if (!proxy.isReady()) proxy.onReady();
                IMEMonitor<IAEItemStack> inventory = proxy.getStorage().getItemInventory();
                IEnergyGrid grid = proxy.getEnergy();
                for (int slot = 0; slot < MTEHatchInputBusME.SLOT_COUNT; slot++) {
                    ItemStack stack = stocking.getStackInSlot(slot);
                    if (stack == null || !seenItems.add(stack)) continue;
                    int amount = logic.getPlannedConsumption(stack);
                    if (amount <= 0) continue;
                    if (!(stocking.getBaseMetaTileEntity() instanceof IActionHost host)) return false;
                    IAEItemStack request = AEItemStack.create(stack).setStackSize(amount);
                    if (!items.add(inventory, grid, request, new MachineSource(host), energy)) return false;
                }
            }
            for (var input : validMTEList(machine.mInputHatches)) if (input instanceof MTEHatchInputME stocking) {
                AENetworkProxy proxy = stocking.getProxy();
                if (!proxy.isReady()) proxy.onReady();
                IMEMonitor<IAEFluidStack> inventory = proxy.getStorage().getFluidInventory();
                IEnergyGrid grid = proxy.getEnergy();
                for (FluidStack stack : stocking.getStoredFluids()) {
                    if (stack == null || !seenFluids.add(stack)) continue;
                    int amount = logic.getPlannedConsumption(stack);
                    if (amount <= 0) continue;
                    if (!(stocking.getBaseMetaTileEntity() instanceof IActionHost host)) return false;
                    IAEFluidStack request = AEFluidStack.create(stack).setStackSize(amount);
                    if (!fluids.add(inventory, grid, request, new MachineSource(host), energy)) return false;
                }
            }
            if (!items.available() || !fluids.available()) return false;
            for (Map.Entry<IEnergyGrid, Long> entry : energy.entrySet()) {
                if (
                    entry.getKey().extractAEPower(entry.getValue(), Actionable.SIMULATE, PowerMultiplier.CONFIG)
                        < entry.getValue()
                ) return false;
            }
            return true;
        } catch (GridAccessException | ArithmeticException failure) {
            return false;
        }
    }

    private static final class Requests<T extends IAEStack<T>> {

        private final Map<IMEMonitor<T>, Map<T, Request<T>>> inventories = new IdentityHashMap<>();

        private boolean add(IMEMonitor<T> inventory, IEnergyGrid grid, T stack, BaseActionSource source,
            Map<IEnergyGrid, Long> energy) {
            T simulated = Platform.poweredExtraction(grid, inventory, stack.copy(), source, Actionable.SIMULATE);
            if (simulated == null || simulated.getStackSize() != stack.getStackSize()) return false;
            long units = (stack.getStackSize() + stack.getAmountPerUnit() - 1) / stack.getAmountPerUnit();
            energy.put(grid, Math.addExact(energy.getOrDefault(grid, 0L), units));
            Map<T, Request<T>> requests = inventories.computeIfAbsent(inventory, ignored -> new HashMap<>());
            Request<T> request = requests.get(stack);
            if (request == null) requests.put(stack, new Request<>(stack.copy(), source));
            else request.stack.setStackSize(Math.addExact(request.stack.getStackSize(), stack.getStackSize()));
            return true;
        }

        private boolean available() {
            for (Map.Entry<IMEMonitor<T>, Map<T, Request<T>>> inventory : inventories.entrySet()) {
                for (Request<T> request : inventory.getValue().values()) {
                    T simulated = inventory.getKey().extractItems(request.stack.copy(), Actionable.SIMULATE,
                        request.source);
                    if (simulated == null || simulated.getStackSize() != request.stack.getStackSize()) return false;
                }
            }
            return true;
        }
    }

    private static final class Request<T extends IAEStack<T>> {

        private final T stack;
        private final BaseActionSource source;

        private Request(T stack, BaseActionSource source) {
            this.stack = stack;
            this.source = source;
        }
    }
}
