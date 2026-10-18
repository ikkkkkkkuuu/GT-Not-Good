package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.compat.ApiaryStorageRevision;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialApiary;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import forestry.api.apiculture.EnumBeeType;
import forestry.apiculture.genetics.BeeDefinition;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.GTUtility.ItemId;
import kubatech.api.helpers.GTHelper.StackableItemSlot;
import kubatech.gui.modularui2.MTEMegaIndustrialApiaryGui;
import kubatech.tileentity.gregtech.multiblock.MTEMegaIndustrialApiary;
import kubatech.tileentity.gregtech.multiblock.MTEMegaIndustrialApiary.BeeSimulator;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.overlay.tooltiprenderers.TTRenderStack;

@Mod(
    modid = "apiaryperformanceqa",
    name = "Apiary Performance QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class ApiaryPerformanceClientChecks {

    private static final int QUEEN_COUNT = 4096;
    private static final int SAMPLE_COUNT = 200;
    private static final int PRODUCTION_CYCLES = 3;

    private final StringBuilder report = new StringBuilder();
    private final List<NBTTagCompound> prototypes = new ArrayList<>();
    private BaseMetaTileEntity base;
    private MTEMegaIndustrialApiary machine;
    private MTEMegaIndustrialApiaryGui gui;
    private Method aggregate, full, paged, invalidate, click, merge;
    private Field page;
    private boolean started;
    private volatile boolean checked, finished, openRequested, opened;
    private volatile int hoverRequested, hoverPositioned;
    private int clientTicks, serverTicks, frames, stage;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.apiaryPerformance.qa")) return;
        ModularUIConfig.guiDebugMode = false;
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks < 6000, "client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F);
            mc.gameSettings.limitFramerate = 60;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("apiary-performance-qa-" + System.currentTimeMillis(),
                    "Apiary Performance QA",
                    new WorldSettings(1703L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (checked && stage < 2 && mc.thePlayer != null) {
                mc.displayGuiScreen(null);
                mc.gameSettings.thirdPersonView = 0;
                mc.gameSettings.hideGUI = false;
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.setPositionAndRotation(stage == 0 ? 0.5 : 4.5, 13.62, -3, 0, 18);
                mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
            }
            if (
                checked && stage == 2
                    && mc.theWorld != null
                    && mc.theWorld.getTileEntity(0, 12, 0) instanceof BaseMetaTileEntity clientBase
                    && clientBase.getMetaTileEntity() instanceof MTEMegaIndustrialApiary
            ) openRequested = true;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            require(++serverTicks < 3600, "server timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (checked) {
                if (hoverRequested > hoverPositioned) {
                    player.playerNetServerHandler.setPlayerLocation(4.5, 12, -3, 0, 18);
                    hoverPositioned = hoverRequested;
                }
                if (openRequested && !opened) {
                    player.playerNetServerHandler.setPlayerLocation(0.5, 12, -3, 0, 18);
                    machine.onRightclick(base, player);
                    opened = true;
                }
                return;
            }
            if (serverTicks != 40) return;
            setup(player);
            verifyAggregationAndTiming();
            verifyMutationAndPersistence(player);
            verifyProduction();
            verifyWaila(player);
            machine.mPrimaryMode = 0;
            machine.mMaxProgresstime = 0;
            machine.mMaxSlots = QUEEN_COUNT * 2;
            player.inventory.setItemStack(null);
            player.playerNetServerHandler.setPlayerLocation(0.5, 12, -3, 0, 18);
            checked = true;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) throws Exception {
        int controllerId = -1;
        for (int i = 0; i < GregTechAPI.METATILEENTITIES.length; i++) {
            if (
                GregTechAPI.METATILEENTITIES[i] != null
                    && GregTechAPI.METATILEENTITIES[i].getClass() == MTEMegaIndustrialApiary.class
            ) {
                controllerId = i;
                break;
            }
        }
        require(controllerId >= 0, "native KubaTech controller is registered");
        base = place(player.worldObj, 0, controllerId);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        machine = (MTEMegaIndustrialApiary) base.getMetaTileEntity();
        machine.mEnergyHatches
            .add((MTEHatchEnergy) place(player.worldObj, 1, ItemList.Hatch_Energy_UMV.get(1).getItemDamage())
                .getMetaTileEntity());
        machine.mMaxSlots = QUEEN_COUNT * 2;
        Map<String, NBTTagCompound> unique = new LinkedHashMap<>();
        for (BeeDefinition definition : BeeDefinition.values()) {
            ItemStack queen = definition.getMemberStack(EnumBeeType.QUEEN);
            if (queen == null) continue;
            BeeSimulator bee = new BeeSimulator(queen, player.worldObj, (float) machine.getVoltageTierExact());
            if (bee.isValid) unique.putIfAbsent(bee.speciesKey, bee.toNBTTagCompound());
        }
        prototypes.addAll(unique.values());
        require(prototypes.size() > 36, "real registered queens span multiple GUI pages");
        for (int i = 0; i < QUEEN_COUNT; i++)
            machine.mStorage.add(new BeeSimulator(prototypes.get(i % prototypes.size())));
        machine.onStorageContentChanged(true);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        gui = new MTEMegaIndustrialApiaryGui(machine);
        field(MTEMegaIndustrialApiaryGui.class, "mainSyncManager").set(gui,
            new PanelSyncManager(new ModularSyncManager(false), true) {

                @Override
                public EntityPlayer getPlayer() {
                    return player;
                }
            });
        aggregate = method(MTEMegaIndustrialApiaryGui.class, "buildAggregatedBeeList");
        full = method(MTEMegaIndustrialApiaryGui.class, "getFullAggregatedBeeList");
        paged = method(MTEMegaIndustrialApiaryGui.class, "buildPagedAggregatedBeeList");
        invalidate = method(MTEMegaIndustrialApiaryGui.class, "invalidateBeeListCache");
        click = method(MTEMegaIndustrialApiaryGui.class, "handleBeeClick", int.class);
        merge = method(MTEMegaIndustrialApiary.class, "mergeOutputStacks", List.class);
        page = field(MTEMegaIndustrialApiaryGui.class, "beePage");
        report.append("Native controller ").append(controllerId).append(": ").append(QUEEN_COUNT).append(" queens, ")
            .append(prototypes.size()).append(" species/speed groups\n");
    }

    private void verifyAggregationAndTiming() throws Exception {
        List<StackableItemSlot> expected = slots(aggregate);
        verifySlots(expected, slots(full));
        Map<String, Integer> counts = new HashMap<>();
        for (BeeSimulator bee : machine.mStorage) counts.merge(bee.speciesKey, 1, Integer::sum);
        require(expected.size() == counts.size(), "aggregated group count matches storage");
        for (StackableItemSlot slot : expected) {
            int index = slot.realSlots.get(0);
            String key = machine.mStorage.get(index).speciesKey;
            require(slot.count == counts.get(key), "group preserves every queen count");
            for (int i = 0; i < index; i++)
                require(!key.equals(machine.mStorage.get(i).speciesKey), "group points to its first real queen");
        }
        int pageSize = field(MTEMegaIndustrialApiaryGui.class, "BEE_ENTRIES_PER_PAGE").getInt(null);
        int pages = (expected.size() + pageSize - 1) / pageSize;
        List<StackableItemSlot> flattened = new ArrayList<>();
        for (int i = 0; i < pages; i++) {
            page.setInt(gui, i);
            flattened.addAll(slots(paged));
        }
        verifySlots(expected, flattened);
        page.setInt(gui, Integer.MAX_VALUE);
        slots(paged);
        require(page.getInt(gui) == pages - 1, "high page clamps to last page");
        page.setInt(gui, -1);
        slots(paged);
        require(page.getInt(gui) == 0, "negative page clamps to first page");
        for (int i = 0; i < 20; i++) slots(aggregate);
        long before = System.nanoTime();
        for (int i = 0; i < SAMPLE_COUNT; i++) slots(aggregate);
        long baselineNanos = System.nanoTime() - before;
        long coldNanos = 0;
        for (int i = 0; i < 20; i++) {
            invalidate.invoke(gui);
            before = System.nanoTime();
            slots(full);
            coldNanos += System.nanoTime() - before;
        }
        List<StackableItemSlot> cached = slots(full);
        World world = base.getWorld();
        long oldTick = world.getTotalWorldTime();
        long hotNanos = 0;
        try {
            for (int i = 0; i < SAMPLE_COUNT; i++) {
                world.func_82738_a(oldTick + i + 1);
                before = System.nanoTime();
                List<StackableItemSlot> current = slots(full);
                hotNanos += System.nanoTime() - before;
                require(current == cached, "unchanged storage reuses one list across 200 simulated world-time values");
            }
        } finally {
            world.func_82738_a(oldTick);
        }
        report.append("GUI aggregation ns/call: original=").append(baselineNanos / SAMPLE_COUNT).append(", cold=")
            .append(coldNanos / 20).append(", cached=").append(hotNanos / SAMPLE_COUNT)
            .append("; 200/200 simulated world-time values retained cache identity; ").append(pages)
            .append(" pages verified\n");
    }

    private void verifyMutationAndPersistence(EntityPlayerMP player) throws Exception {
        ApiaryStorageRevision revision = (ApiaryStorageRevision) machine;
        List<StackableItemSlot> oldList = slots(full);
        long oldRevision = revision.getBeeStorageRevision();
        machine.mStorage.set(0, new BeeSimulator(prototypes.get(prototypes.size() - 1)));
        machine.onStorageContentChanged(true);
        require(revision.getBeeStorageRevision() != oldRevision, "same-count replacement advances revision");
        require(slots(full) != oldList, "same-count replacement invalidates aggregation");
        verifySlots(slots(aggregate), slots(full));
        oldList = slots(full);
        machine.mStorage.add(new BeeSimulator(prototypes.get(0)));
        require(slots(full) != oldList, "size guard detects an unannounced addition");
        machine.mStorage.remove(machine.mStorage.size() - 1);
        machine.onStorageContentChanged(true);
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        oldList = slots(full);
        oldRevision = revision.getBeeStorageRevision();
        machine.mStorage.clear();
        machine.loadNBTData(saved);
        require(revision.getBeeStorageRevision() != oldRevision, "NBT load advances revision at identical size");
        require(machine.mStorage.size() == QUEEN_COUNT && slots(full) != oldList, "NBT load invalidates existing GUI");
        for (int i = 0; i < QUEEN_COUNT; i++)
            require(machine.mStorage.get(i).toNBTTagCompound().equals(saved.getCompoundTag("mStorage." + i)),
                "complete native queen and simulator NBT survives reload");
        verifySlots(slots(aggregate), slots(full));
        page.setInt(gui, 1);
        int pageSize = field(MTEMegaIndustrialApiaryGui.class, "BEE_ENTRIES_PER_PAGE").getInt(null);
        StackableItemSlot target = slots(full).get(pageSize);
        ItemStack original = machine.mStorage.get(target.realSlots.get(0)).queenStack.copy();
        player.inventory.setItemStack(null);
        click.invoke(gui, 1 << 3);
        require(machine.mStorage.size() == QUEEN_COUNT - 1, "second-page click removes exactly one queen");
        require(ItemStack.areItemStacksEqual(original, player.inventory.getItemStack()),
            "GUI extraction returns complete real genetics instead of trimmed display NBT");
        verifySlots(slots(aggregate), slots(full));
        ItemStack replacement = new BeeSimulator(prototypes.get(0)).queenStack.copy();
        replacement.setStackDisplayName("Apiary QA queen");
        player.inventory.setItemStack(replacement);
        oldRevision = revision.getBeeStorageRevision();
        click.invoke(gui, 1 << 3);
        require(machine.mStorage.size() == QUEEN_COUNT - 1 && revision.getBeeStorageRevision() != oldRevision,
            "GUI replacement updates the same-count inventory");
        require(Arrays.toString(machine.getInfoData()).contains("Apiary QA queen"),
            "status names follow a replaced queen identity");
        String[] normalInfo = machine.getInfoData();
        machine.mPrimaryMode = 1;
        require(!Arrays.equals(normalInfo, machine.getInfoData()), "status mode remains dynamic");
        machine.mPrimaryMode = 2;
        machine.mMaxProgresstime = 100;
        int size = machine.mStorage.size();
        oldRevision = revision.getBeeStorageRevision();
        player.inventory.setItemStack(null);
        click.invoke(gui, 1 << 3);
        require(machine.mStorage.size() == size && player.inventory.getItemStack() == null
            && revision.getBeeStorageRevision() == oldRevision, "running machine rejects extraction");
        machine.mPrimaryMode = 0;
        machine.mMaxProgresstime = 0;
        machine.mStorage.add(new BeeSimulator(prototypes.get(0)));
        machine.onStorageContentChanged(true);
        report.append(
            "Same-count/size/NBT invalidation, pagination, real-NBT extraction, replacement and running lock passed\n");
    }

    private void verifyProduction() throws Exception {
        machine.mPrimaryMode = 2;
        machine.mSecondaryMode = 0;
        field(MTEMegaIndustrialApiary.class, "missingFlowers").setBoolean(machine, false);
        require(machine.checkProcessing().wasSuccessful(),
            "production fixture initializes native voltage and simulator state");
        Map<ItemId, Double> seed = productionSeed();
        List<Map<ItemId, Long>> expectedOutputs = new ArrayList<>();
        List<Map<ItemId, Double>> expectedProgress = new ArrayList<>();
        machine.dropProgress.clear();
        machine.dropProgress.putAll(seed);
        long before = System.nanoTime();
        for (int cycle = 0; cycle < PRODUCTION_CYCLES; cycle++) {
            List<ItemStack> drops = new ArrayList<>();
            for (BeeSimulator bee : machine.mStorage) drops.addAll(bee.getDrops(machine, 6400D));
            expectedOutputs.add(outputCounts((ItemStack[]) merge.invoke(null, drops)));
            expectedProgress.add(new HashMap<>(machine.dropProgress));
        }
        long baselineNanos = System.nanoTime() - before;
        machine.dropProgress.clear();
        machine.dropProgress.putAll(seed);
        before = System.nanoTime();
        for (int cycle = 0; cycle < PRODUCTION_CYCLES; cycle++) {
            machine.mMaxProgresstime = 0;
            require(machine.checkProcessing().wasSuccessful(), "native production cycle succeeds");
            require(outputCounts(machine.mOutputItems).equals(expectedOutputs.get(cycle)),
                "production merged outputs match original per-bee processing exactly");
            require(machine.dropProgress.equals(expectedProgress.get(cycle)),
                "every fractional progress value matches original processing exactly");
            require(machine.mMaxProgresstime == 100, "native production duration remains 100 ticks");
        }
        long optimizedNanos = System.nanoTime() - before;
        report.append("Production ns/cycle: original=").append(baselineNanos / PRODUCTION_CYCLES).append(", optimized=")
            .append(optimizedNanos / PRODUCTION_CYCLES).append("; ").append(PRODUCTION_CYCLES)
            .append(" cycles matched exact outputs and ").append(machine.dropProgress.size())
            .append(" progress keys, including unrelated existing progress\n");
    }

    @SuppressWarnings("unchecked")
    private Map<ItemId, Double> productionSeed() throws Exception {
        Map<ItemId, ItemStack> stacks = (Map<ItemId, ItemStack>) field(BeeSimulator.class, "dropstacks").get(null);
        Map<ItemId, Double> result = new HashMap<>();
        double[] amounts = { 0D, 1D, 1.25D, 64D, 128.75D };
        for (int i = 0; i < amounts.length; i++) {
            ItemStack stack = new ItemStack(Items.stick);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("apiaryQaProgress", i);
            stack.setTagCompound(tag);
            ItemId id = ItemId.createNoCopy(stack);
            stacks.put(id, stack);
            result.put(id, amounts[i]);
        }
        return result;
    }

    private void verifyWaila(EntityPlayerMP player) throws Exception {
        ItemStack[] production = machine.mOutputItems;
        require(production != null && production.length > 3, "production supplies multiple Waila output previews");
        ItemStack[] sparse = new ItemStack[production.length * 2 + 2];
        for (int i = 0; i < production.length; i++) sparse[i * 2 + 1] = production[i].copy();
        BaseMetaTileEntity largeBase = place(player.worldObj, 4, GTNGMachineID.LargeIndustrialApiary.id);
        largeBase.setOwnerName(player.getCommandSenderName());
        largeBase.setOwnerUuid(player.getUniqueID());
        largeBase.disableWorking();
        LargeIndustrialApiary large = (LargeIndustrialApiary) largeBase.getMetaTileEntity();
        verifyWaila(machine, player, sparse, "Native");
        verifyWaila(large, player, sparse, "Large");
        machine.mOutputItems = production;
        large.mOutputItems = sparse;
        large.mMaxProgresstime = 0;
    }

    private void verifyWaila(MTEMultiBlockBase target, EntityPlayerMP player, ItemStack[] outputs, String label)
        throws Exception {
        target.mOutputItems = outputs;
        target.mOutputFluids = new FluidStack[] { new FluidStack(FluidRegistry.WATER, 250), null,
            new FluidStack(FluidRegistry.LAVA, 500) };
        target.mMaxProgresstime = 100;
        target.mProgresstime = 25;
        target.mEfficiency = 10000;
        target.setErrorDisplayID(0);
        target.getBaseMetaTileEntity().setActive(true);
        List<NBTTagCompound> originalStacks = new ArrayList<>();
        int count = 0;
        for (ItemStack stack : outputs) {
            originalStacks.add(stack == null ? null : stack.writeToNBT(new NBTTagCompound()));
            if (stack != null) count++;
        }
        NBTTagCompound original = wailaData(target, player, true);
        NBTTagCompound optimized = wailaData(target, player, false);
        NBTTagCompound expected = (NBTTagCompound) original.copy();
        for (int i = 3; i < count; i++) {
            expected.removeTag("outputItemIcon" + i);
            expected.removeTag("outputItemStack" + i);
            expected.removeTag("outputItemCount" + i);
        }
        require(expected.equals(optimized), label + " Waila differs only by omitted item previews after the third");
        require(optimized.getInteger("outputItemLength") == count,
            label + " Waila preserves the complete nonnull count");
        require(!optimized.hasKey("outputItemStack3"), label + " Waila sends exactly three item previews");
        require(target.mOutputItems == outputs, label + " Waila preserves the actual pending output array");
        for (int i = 0; i < outputs.length; i++) require(
            outputs[i] == null ? originalStacks.get(i) == null
                : originalStacks.get(i).equals(outputs[i].writeToNBT(new NBTTagCompound())),
            label + " Waila preserves every pending stack and its NBT");
        List<String> originalBody = wailaBody(target, player, original);
        List<String> optimizedBody = wailaBody(target, player, optimized);
        require(originalBody.equals(optimizedBody), label + " Waila hover body remains identical line by line");
        String more = StatCollector.translateToLocalFormatted("GT5U.waila.producing.andmore",
            NumberFormatUtil.formatNumber(count + 2 - 3));
        require(optimizedBody.contains(more), label + " Waila and-more includes all omitted item and fluid outputs");
        for (int i = 0; i < 10; i++) {
            wailaData(target, player, true);
            wailaData(target, player, false);
        }
        long before = System.nanoTime();
        for (int i = 0; i < 100; i++) wailaData(target, player, true);
        long originalNanos = System.nanoTime() - before;
        before = System.nanoTime();
        for (int i = 0; i < 100; i++) wailaData(target, player, false);
        long optimizedNanos = System.nanoTime() - before;
        int originalBytes = CompressedStreamTools.compress(original).length;
        int optimizedBytes = CompressedStreamTools.compress(optimized).length;
        report.append(label).append(" Waila: pending array=").append(outputs.length).append(", nonnull=").append(count)
            .append(", previews=3; compressed bytes=").append(originalBytes).append(" -> ").append(optimizedBytes)
            .append("; ns/request=").append(originalNanos / 100).append(" -> ").append(optimizedNanos / 100)
            .append(" (100 requests); ").append(optimizedBody.size())
            .append(" identical body lines, exact and-more count\n");
        for (int size : new int[] { 0, 2, 3 }) {
            target.mOutputItems = Arrays.copyOf(outputs, size * 2 + 1);
            require(wailaData(target, player, true).equals(wailaData(target, player, false)),
                label + " Waila preserves short or empty sparse output payloads");
        }
        target.mOutputItems = null;
        require(wailaData(target, player, true).equals(wailaData(target, player, false)),
            label + " Waila preserves null output payloads");
        target.mOutputItems = outputs;
        target.mOutputFluids = null;
        target.mMaxProgresstime = 0;
        target.getBaseMetaTileEntity().setActive(false);
    }

    private static NBTTagCompound wailaData(MTEMultiBlockBase target, EntityPlayerMP player, boolean original) {
        ItemStack[] outputs = target.mOutputItems;
        NBTTagCompound tag = new NBTTagCompound();
        var tile = target.getBaseMetaTileEntity();
        try {
            if (original) target.mOutputItems = null;
            target.getWailaNBTData(player, (BaseMetaTileEntity) tile, tag, tile.getWorld(), tile.getXCoord(),
                tile.getYCoord(), tile.getZCoord());
        } finally {
            target.mOutputItems = outputs;
        }
        if (original && outputs != null) {
            int index = 0;
            for (ItemStack stack : outputs) {
                if (stack == null) continue;
                tag.setString("outputItemIcon" + index, TTRenderStack.create(stack, true));
                tag.setTag("outputItemStack" + index, stack.writeToNBT(new NBTTagCompound()));
                tag.setInteger("outputItemCount" + index, stack.stackSize);
                index++;
            }
            if (index != 0) tag.setInteger("outputItemLength", index);
        }
        return tag;
    }

    private static List<String> wailaBody(MTEMultiBlockBase target, EntityPlayerMP player, NBTTagCompound tag) {
        IWailaDataAccessor accessor = (IWailaDataAccessor) Proxy.newProxyInstance(
            IWailaDataAccessor.class.getClassLoader(), new Class<?>[] { IWailaDataAccessor.class },
            (proxy, method, arguments) -> switch (method.getName()) {
            case "getNBTData" -> tag;
            case "getPlayer" -> player;
            case "getWorld" -> player.worldObj;
            case "getTileEntity" -> target.getBaseMetaTileEntity();
            default -> null;
            });
        IWailaConfigHandler config = (IWailaConfigHandler) Proxy.newProxyInstance(
            IWailaConfigHandler.class.getClassLoader(), new Class<?>[] { IWailaConfigHandler.class },
            (proxy, method, arguments) -> switch (method.getName()) {
            case "getConfig" -> arguments.length > 1 ? arguments[1] : true;
            case "getModuleNames" -> Collections.emptySet();
            case "getConfigKeys" -> new HashMap<>();
            default -> null;
            });
        List<String> body = new ArrayList<>();
        target.getWailaBody(null, body, accessor, config);
        return body;
    }

    private static Map<ItemId, Long> outputCounts(ItemStack[] stacks) {
        Map<ItemId, Long> result = new HashMap<>();
        for (ItemStack stack : stacks) result.merge(ItemId.createNoCopy(stack), (long) stack.stackSize, Long::sum);
        return result;
    }

    private static void verifySlots(List<StackableItemSlot> expected, List<StackableItemSlot> actual) {
        require(expected.size() == actual.size(), "group list length matches original");
        for (int i = 0; i < expected.size(); i++) {
            StackableItemSlot a = expected.get(i), b = actual.get(i);
            require(
                a.count == b.count && a.realSlots.equals(b.realSlots) && ItemStack.areItemStacksEqual(a.stack, b.stack),
                "group order, count, display stack and real index match original");
        }
    }

    @SuppressWarnings("unchecked")
    private List<StackableItemSlot> slots(Method method) throws Exception {
        return (List<StackableItemSlot>) method.invoke(gui);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static BaseMetaTileEntity place(World world, int x, int id) {
        world.setBlock(x, 12, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 12, 0);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            require(++frames < 1800, "hover or GUI timeout at stage " + stage);
            Minecraft mc = Minecraft.getMinecraft();
            if (stage < 2) {
                int targetX = stage == 0 ? 0 : 4;
                if (
                    mc.currentScreen != null || mc.objectMouseOver == null
                        || mc.objectMouseOver.blockX != targetX
                        || mc.objectMouseOver.blockY != 12
                        || mc.objectMouseOver.blockZ != 0
                        || frames < 90
                ) return;
                ScreenShotHelper.saveScreenshot(output(), "apiary-waila-hover-" + stage + ".png", mc.displayWidth,
                    mc.displayHeight, mc.getFramebuffer());
                report.append(stage == 0 ? "Native" : "Large")
                    .append(" controller targeted in world with no screen open\n");
                stage++;
                frames = 0;
                if (stage == 1) hoverRequested = 1;
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper) || frames < 90) return;
            GuiContainerWrapper screen = (GuiContainerWrapper) mc.currentScreen;
            ScreenShotHelper.saveScreenshot(output(), "apiary-performance-" + stage + ".png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            if (stage++ == 2) {
                require(openInventory(screen.getScreen().getMainPanel()), "native inventory toggle exists");
                frames = 0;
                return;
            }
            report.append("Native MUI2 status and inventory panels rendered with 4096 queens\n");
            Files.write(new File(output(), "result.txt").toPath(),
                ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
            System.out.println("APIARY_PERFORMANCE_QA: PASS\n" + report);
            finished = true;
            mc.shutdown();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static boolean openInventory(IWidget parent) {
        List<IWidget> children = parent.getChildren();
        for (int i = 0; i < children.size(); i++) {
            IWidget child = children.get(i);
            if (child instanceof ButtonWidget<?>button && button.getOverlay() == GuiTextures.GEAR) {
                require(i > 0 && children.get(i - 1) instanceof ButtonWidget<?>, "inventory toggle precedes settings");
                ((ButtonWidget<?>) children.get(i - 1)).onMousePressed(0);
                return true;
            }
            if (openInventory(child)) return true;
        }
        return false;
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.apiaryPerformance.output", "build/apiary-performance-qa"));
        folder.mkdirs();
        return folder;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                report.append("Client state: screen=").append(mc.currentScreen).append(", position=")
                    .append(mc.thePlayer.posX).append(',').append(mc.thePlayer.posY).append(',')
                    .append(mc.thePlayer.posZ).append(", yaw/pitch=").append(mc.thePlayer.rotationYaw).append('/')
                    .append(mc.thePlayer.rotationPitch).append(", hit=").append(mc.objectMouseOver).append('\n');
            }
            ScreenShotHelper.saveScreenshot(output(), "apiary-performance-failure.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(),
                ("FAIL\n" + report + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("APIARY_PERFORMANCE_QA: " + message);
    }
}
