package com.xyp.gtnotgood.common.wireless;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.machines.hatch.CrossRecipeWirelessEnergyHatch;
import com.xyp.gtnotgood.mixins.late.Gregtech.wireless.WirelessProcessingAccess;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SingleRecipeCheck;
import gregtech.api.util.GTRecipe;

/**
 * A server-thread recipe selection transaction. Native validators and input hatch iteration are retained; input
 * deltas are committed only after the controller accepts the result, before its ME transaction closes.
 */
public final class WirelessRecipeAttempt {

    private static final ThreadLocal<WirelessRecipeAttempt> CURRENT = new ThreadLocal<>();
    private static final Map<GTRecipe, String> KEYS = new WeakHashMap<>();
    final MTEMultiBlockBase machine;
    private final WirelessRecipeScheduler scheduler;
    private final CrossRecipeWirelessEnergyHatch hatch;
    private final UUID owner;
    private final BigInteger available;
    private final IdentityHashMap<ItemStack, ItemStack> itemCopies = new IdentityHashMap<>();
    private final IdentityHashMap<FluidStack, FluidStack> fluidCopies = new IdentityHashMap<>();
    private final IdentityHashMap<ItemStack, Integer> itemBefore = new IdentityHashMap<>();
    private final IdentityHashMap<FluidStack, Integer> fluidBefore = new IdentityHashMap<>();
    WirelessWork prepared;
    boolean committed;
    private SingleRecipeCheck recipeLock;

    WirelessRecipeAttempt(MTEMultiBlockBase machine, WirelessRecipeScheduler scheduler,
        CrossRecipeWirelessEnergyHatch hatch, BigInteger available) {
        this.machine = machine;
        this.scheduler = scheduler;
        this.hatch = hatch;
        this.owner = hatch.getBaseMetaTileEntity()
            .getOwnerUuid();
        this.available = available.max(BigInteger.ZERO);
    }

    static void enter(WirelessRecipeAttempt attempt) {
        if (CURRENT.get() != null) throw new IllegalStateException("Nested wireless recipe selection");
        CURRENT.set(attempt);
    }

    static void leave() {
        CURRENT.remove();
    }

    public static WirelessRecipeAttempt current() {
        return CURRENT.get();
    }

    public boolean belongsTo(MTEMultiBlockBase controller) {
        return machine == controller;
    }

    public boolean defersSlotUpdates() {
        return !committed;
    }

    /** Returns null outside this scheduler so every unrelated machine retains its original ProcessingLogic. */
    public static CheckRecipeResult intercept(ProcessingLogic logic) {
        WirelessRecipeAttempt attempt = current();
        if (attempt == null
            && ((WirelessProcessingAccess) logic).gtng$getMachine() instanceof MTEMultiBlockBase controller
            && WirelessRecipeScheduler.find(controller) != null) return CheckRecipeResultRegistry.NO_RECIPE;
        if (attempt == null || ((WirelessControllerAccess) attempt.machine).gtng$getProcessingLogic() != logic)
            return null;
        return attempt.process(logic);
    }

    private CheckRecipeResult process(ProcessingLogic logic) {
        if (prepared != null) return CheckRecipeResultRegistry.NO_RECIPE;
        WirelessProcessingAccess access = (WirelessProcessingAccess) logic;
        logic.setAvailableVoltage(Long.MAX_VALUE)
            .setAvailableAmperage(1);
        ItemStack[] originalItems = access.gtng$getItems();
        FluidStack[] originalFluids = access.gtng$getFluids();
        ItemStack[] inputs = access.gtng$prepareCatalyst(originalItems == null ? new ItemStack[0] : originalItems);
        logic.setInputItems(inputs);
        CheckRecipeResult last = CheckRecipeResultRegistry.NO_RECIPE;
        try (Stream<GTRecipe> matches = machine.isRecipeLockingEnabled() && machine.getSingleRecipeCheck() != null
            ? Stream.of(
                machine.getSingleRecipeCheck()
                    .getRecipe())
            : access.gtng$matches(access.gtng$recipeMap())) {
            for (GTRecipe recipe : (Iterable<GTRecipe>) matches::iterator) {
                String key = key(recipe);
                if (scheduler.contains(key) || recipe.mEUt < 0 || recipe.mDuration < 1) continue;
                CheckRecipeResult validation = access.gtng$validate(recipe);
                if (!validation.wasSuccessful()) {
                    last = validation;
                    continue;
                }
                BigInteger cost = BigInteger.valueOf(recipe.mEUt)
                    .multiply(BigInteger.valueOf(recipe.mDuration));
                BigInteger cap = hatch.getParallelLimit()
                    .signum() == 0 ? null : hatch.getParallelLimit();
                if (cost.signum() > 0) {
                    BigInteger energyLimit = available.divide(cost);
                    cap = cap == null ? energyLimit : cap.min(energyLimit);
                }
                if (cap != null && cap.signum() <= 0) {
                    last = CheckRecipeResultRegistry.insufficientStartupPower(cost);
                    continue;
                }
                itemCopies.clear();
                fluidCopies.clear();
                itemBefore.clear();
                fluidBefore.clear();
                List<ItemStack> items = new ArrayList<>();
                for (ItemStack stack : inputs) {
                    if (stack == null || itemCopies.containsKey(stack)) continue;
                    ItemStack copy = stack.copy();
                    itemCopies.put(stack, copy);
                    itemBefore.put(stack, stack.stackSize);
                    items.add(copy);
                }
                List<FluidStack> fluids = new ArrayList<>();
                if (originalFluids != null) for (FluidStack fluid : originalFluids) {
                    if (fluid == null || fluidCopies.containsKey(fluid)) continue;
                    FluidStack copy = fluid.copy();
                    fluidCopies.put(fluid, copy);
                    fluidBefore.put(fluid, fluid.amount);
                    fluids.add(copy);
                }
                WirelessOutputs outputs = WirelessOutputs
                    .forRecipe(recipe, machine.getItemOutputLimit(), machine.getFluidOutputLimit());
                BigInteger parallels = WirelessInputBatch
                    .consume(recipe, items.toArray(new ItemStack[0]), fluids.toArray(new FluidStack[0]), cap, outputs);
                if (parallels.signum() <= 0) continue;
                // Native fields serve legacy hooks; the task owns the exact count.
                access.gtng$setParallels(
                    parallels.min(BigInteger.valueOf(Integer.MAX_VALUE))
                        .intValueExact());
                logic.overwriteCalculatedEut(0)
                    .overwriteCalculatedDuration(hatch.getDuration());
                CheckRecipeResult started = access.gtng$start(recipe);
                if (!started.wasSuccessful()) {
                    last = started;
                    continue;
                }
                prepared = new WirelessWork(
                    owner,
                    key,
                    cost.multiply(parallels),
                    hatch.getDuration(),
                    parallels,
                    outputs);
                if (machine.isRecipeLockingEnabled() && machine.getSingleRecipeCheck() == null) {
                    // A one-craft probe builds the native lock without charging the actual batch twice.
                    ItemStack[] lockItems = itemBefore.keySet()
                        .stream()
                        .map(ItemStack::copy)
                        .toArray(ItemStack[]::new);
                    FluidStack[] lockFluids = fluidBefore.keySet()
                        .stream()
                        .map(FluidStack::copy)
                        .toArray(FluidStack[]::new);
                    SingleRecipeCheck.Builder builder = SingleRecipeCheck.builder(machine.getRecipeMap())
                        .setBefore(lockItems, lockFluids);
                    recipe.consumeInput(1, lockFluids, lockItems);
                    recipeLock = builder.setAfter(lockItems, lockFluids)
                        .setRecipe(recipe)
                        .build();
                }
                logic.overwriteOutputItems(new ItemStack[0])
                    .overwriteOutputFluids(new FluidStack[0]);
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }
        } finally {
            logic.setInputItems(originalItems);
        }
        return last;
    }

    /**
     * Must run inside startRecipeProcessing/endRecipeProcessing, never after an ME hatch has closed its transaction.
     */
    public boolean commit() {
        if (prepared == null || committed) return committed;
        for (Map.Entry<ItemStack, ItemStack> entry : itemCopies.entrySet()) {
            if (entry.getKey().stackSize != itemBefore.get(entry.getKey()) || entry.getValue().stackSize < 0)
                return false;
        }
        for (Map.Entry<FluidStack, FluidStack> entry : fluidCopies.entrySet()) {
            if (entry.getKey().amount != fluidBefore.get(entry.getKey()) || entry.getValue().amount < 0) return false;
        }
        itemCopies.forEach((original, copy) -> original.stackSize = copy.stackSize);
        fluidCopies.forEach((original, copy) -> original.amount = copy.amount);
        committed = true;
        if (recipeLock != null) machine.setSingleRecipeCheck(recipeLock);
        machine.updateSlots();
        return true;
    }

    private static String key(GTRecipe recipe) {
        return KEYS.computeIfAbsent(recipe, value -> {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("eu", value.mEUt);
            tag.setInteger("ticks", value.mDuration);
            tag.setInteger("special", value.mSpecialValue);
            addItems(tag, "in", value.mInputs);
            addItems(tag, "out", value.mOutputs);
            addFluids(tag, "fin", value.mFluidInputs);
            addFluids(tag, "fout", value.mFluidOutputs);
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(
                        tag.toString()
                            .getBytes(StandardCharsets.UTF_8));
                StringBuilder text = new StringBuilder(64);
                for (byte b : digest) text.append(String.format("%02x", b & 255));
                return text.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static void addItems(NBTTagCompound tag, String prefix, ItemStack[] values) {
        if (values == null) return;
        for (int i = 0; i < values.length; i++)
            if (values[i] != null) tag.setTag(prefix + i, values[i].writeToNBT(new NBTTagCompound()));
    }

    private static void addFluids(NBTTagCompound tag, String prefix, FluidStack[] values) {
        if (values == null) return;
        for (int i = 0; i < values.length; i++)
            if (values[i] != null) tag.setTag(prefix + i, values[i].writeToNBT(new NBTTagCompound()));
    }
}
