package com.xyp.gtnotgood.common.machines.hatch.me;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.IOutputTransaction;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;

@Mod(
    modid = "meoutputassemblyqa",
    name = "ME Output Assembly QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class MEOutputAssemblyClientChecks {

    private boolean started;
    private volatile boolean finished;
    private volatile boolean visualsDone;
    private int ticks;
    private int clientTicks;
    private int networkStartedAt;
    private int stage;
    private volatile int screenshot;
    private int frames;
    private boolean filtersDragged;
    private MEOutputAssembly assembly;
    private MEOutputAssembly fallback;
    private MEOutputAssembly poweredAssembly;
    private TileDrive drive;
    private long itemAmount;
    private long fluidAmount;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.meOutputAssembly.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            if (++clientTicks > 6000) throw new AssertionError("Client QA timed out");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.guiScale = 2;
            ModularUIConfig.guiDebugMode = false;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("me-output-assembly-qa-" + System.currentTimeMillis(),
                    "ME Output Assembly QA",
                    new WorldSettings(25L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            ticks++;
            if (ticks == 1) {
                World world = player.worldObj;
                world.setWorldTime(6000);
                assembly = (MEOutputAssembly) place(world, player, 0, GTNGItemList.MEOutputAssembly.get(1))
                    .getMetaTileEntity();
                fallback = (MEOutputAssembly) place(world, player, 2, GTNGItemList.MEOutputAssembly.get(1))
                    .getMetaTileEntity();
                place(world, player, -2, ItemList.Hatch_Output_Bus_ME.get(1));
                place(world, player, -4, ItemList.Hatch_Output_ME.get(1));
                setupPoweredNetwork(player);
                player.playerNetServerHandler.setPlayerLocation(-1, 10, -6, 0, 6);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
            }
            if (ticks > 1800) throw new AssertionError("QA timed out");
            if (ticks < 80 || screenshot != 0) return;
            if (stage == 0) {
                verifyDrops(player);
                verifyNativeRegistration(player);
                verify(player);
                assembly.onRightclick(assembly.getBaseMetaTileEntity(), player);
                screenshot = 1;
                stage = 1;
            } else if (stage == 1) {
                require(
                    assembly.getFilters().getItem(4) != null
                        && assembly.getFilters().getItem(4).getItem() == Items.diamond,
                    "GUI item drag updates the authoritative item filter");
                require(
                    assembly.getFilters().getFluid(4) != null
                        && assembly.getFilters().getFluid(4).getFluid() == FluidRegistry.WATER,
                    "GUI bucket drag updates the authoritative fluid filter");
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(-1, 10, -6, 0, 6);
                screenshot = 2;
                stage = 2;
            } else if (stage == 2 && visualsDone) {
                if (!poweredAssembly.getProxy().isActive()) return;
                beginPoweredFlush();
                networkStartedAt = ticks;
                stage = 3;
            } else if (stage == 3) {
                if (
                    poweredAssembly.getProvider().getCachedAmount() != 0
                        || poweredAssembly.getFluidOutput().getProvider().getCachedAmount() != 0
                ) {
                    if (ticks - networkStartedAt > 120) throw new AssertionError("Powered ME output did not flush");
                    return;
                }
                verifyPoweredFlush();
                Files.write(new File(output(), "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                finished = true;
                Minecraft.getMinecraft().shutdown();
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setupPoweredNetwork(EntityPlayerMP player) {
        World world = player.worldObj;
        poweredAssembly = (MEOutputAssembly) place(world, player, 20, GTNGItemList.MEOutputAssembly.get(1))
            .getMetaTileEntity();
        var definitions = AEApi.instance().definitions();
        world.setBlock(20, 10, -1, definitions.blocks().multiPart().maybeBlock().get());
        require(
            ((IPartHost) world.getTileEntity(20, 10, -1)).addPart(
                definitions.parts().cableGlass().stack(AEColor.Transparent, 1), ForgeDirection.UNKNOWN, player) != null,
            "powered fixture places a native glass cable");
        world.setBlock(21, 10, -1, definitions.blocks().energyCellCreative().maybeBlock().get());
        world.setBlock(19, 10, -1, definitions.blocks().drive().maybeBlock().get());
        drive = (TileDrive) world.getTileEntity(19, 10, -1);
        drive.getInternalInventory().setInventorySlotContents(0, definitions.items().cell64k().maybeStack(1).get());
        drive.getInternalInventory().setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
    }

    private void beginPoweredFlush() throws Exception {
        var proxy = poweredAssembly.getProxy();
        require(poweredAssembly.getFluidOutput().getProvider().getProxy() == proxy,
            "both native providers share one AE proxy");
        require(drive.getActionableNode().getGrid() == proxy.getGrid(), "real ME drive shares the active output grid");
        int channels = 0;
        for (var node : proxy.getGrid().getNodes()) if (node.hasFlag(GridFlags.REQUIRE_CHANNEL)) channels++;
        require(channels == 2, "assembly consumes one channel alongside the drive");
        require(poweredAssembly.storePartial(new ItemStack(Items.iron_ingot, 13), false),
            "powered output stages thirteen items");
        require(poweredAssembly.getFluidOutput().fill(new FluidStack(FluidRegistry.WATER, 1234), true) == 1234,
            "powered output stages fluid in the shared grid");
        requireAmounts(poweredAssembly, 13, 1234, "both powered caches contain the staged outputs before ticking");
    }

    private void verifyPoweredFlush() throws Exception {
        var storage = poweredAssembly.getProxy().getStorage();
        var source = poweredAssembly.getActionSource();
        var item = storage.getItemInventory().extractItems(
            AEItemStack.create(new ItemStack(Items.iron_ingot)).setStackSize(Long.MAX_VALUE), Actionable.SIMULATE,
            source);
        var fluid = storage.getFluidInventory().extractItems(
            AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)).setStackSize(Long.MAX_VALUE),
            Actionable.SIMULATE, source);
        require(item != null && item.getStackSize() == 13, "native ticking flushes the exact item quantity into ME");
        require(fluid != null && fluid.getStackSize() == 1234,
            "native ticking flushes the exact fluid quantity into ME through the same proxy");
        requireAmounts(poweredAssembly, 0, 0, "successful ME export clears both caches");
    }

    private static void verifyDrops(EntityPlayerMP player) {
        ItemStack fresh = GTNGItemList.MEOutputAssembly.get(1);
        BaseMetaTileEntity plain = place(player.worldObj, player, 8, fresh);
        require(ItemStack.areItemStacksEqual(fresh, plain.getDrops().get(0)),
            "plain drop stacks with a fresh assembly");
        BaseMetaTileEntity legacy = place(player.worldObj, player, 9, fresh);
        NBTTagCompound old = new NBTTagCompound();
        old.setLong("baseCapacity", Long.MAX_VALUE);
        NBTTagList empty = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("slot", i);
            empty.appendTag(entry);
        }
        old.setTag("gtngOutputFilters", empty);
        legacy.getMetaTileEntity().loadNBTData(old);
        require(ItemStack.areItemStacksEqual(fresh, legacy.getDrops().get(0)),
            "legacy empty data does not split stacks");
        BaseMetaTileEntity configured = place(player.worldObj, player, 10, fresh);
        MEOutputAssembly marked = (MEOutputAssembly) configured.getMetaTileEntity();
        marked.getFilters().setItem(8, new ItemStack(Items.iron_ingot));
        marked.getFilters().setFluid(8, new FluidStack(FluidRegistry.WATER, 1));
        ItemStack dropped = configured.getDrops().get(0);
        require(ItemStack.areItemStacksEqual(fresh, dropped), "configured empty drop stacks with a fresh assembly");
        require(!dropped.hasTagCompound(), "empty pickup omits filters and intrinsic capacities");
        require(marked.getFilters().hasItems() && marked.getFilters().hasFluids(),
            "drop calculation preserves world filters");
        BaseMetaTileEntity replacement = place(player.worldObj, player, 11, dropped);
        replacement.getMetaTileEntity()
            .loadNBTData(dropped.hasTagCompound() ? dropped.getTagCompound() : new NBTTagCompound());
        MEOutputAssembly cleared = (MEOutputAssembly) replacement.getMetaTileEntity();
        require(!cleared.getFilters().hasItems() && !cleared.getFilters().hasFluids(),
            "replacement starts without filters");
        require(
            cleared.getProvider().getCachedAmount() == 0
                && cleared.getFluidOutput().getProvider().getCachedAmount() == 0,
            "replacement starts with empty caches");
        requireCapacities(cleared, "replacement restores both long capacities");
        require(ItemStack.areItemStacksEqual(fresh, replacement.getDrops().get(0)), "replacement remains stackable");
        marked.storePartial(new ItemStack(Items.iron_ingot, 16), false);
        marked.getFluidOutput().fill(new FluidStack(FluidRegistry.WATER, 1000), true);
        requireAmounts(marked, 16, 1000, "pickup fixture contains both cached output channels");
        ItemStack bufferedDrop = configured.getDrops().get(0);
        require(ItemStack.areItemStacksEqual(fresh, bufferedDrop), "buffered drop stacks with a fresh assembly");
        require(!bufferedDrop.hasTagCompound(), "buffered pickup omits both caches and filters");
        MEOutputAssembly bufferedReplacement = (MEOutputAssembly) place(player.worldObj, player, 12, bufferedDrop)
            .getMetaTileEntity();
        requireAmounts(bufferedReplacement, 0, 0, "replacing a buffered drop starts with both caches empty");
        require(!bufferedReplacement.getFilters().hasItems() && !bufferedReplacement.getFilters().hasFluids(),
            "buffered pickup resets both filters");
        requireCapacities(bufferedReplacement, "buffered replacement restores both long capacities");
    }

    private void verifyNativeRegistration(EntityPlayerMP player) {
        require(HatchElement.OutputBus.matchesHatch(assembly) && HatchElement.OutputHatch.matchesHatch(assembly),
            "native hatch elements recognize both output roles");
        require(
            GTStructureUtility.filterByMTEClass(HatchElement.OutputHatch.mteClasses())
                .test(GTNGItemList.MEOutputAssembly.get(1)),
            "survival construction accepts the assembly in output-hatch positions");
        MTEMultiBlockBase controller = (MTEMultiBlockBase) place(player.worldObj, player, 14,
            ItemList.Machine_Multi_VacuumFreezer.get(1)).getMetaTileEntity();
        BaseMetaTileEntity tile = (BaseMetaTileEntity) assembly.getBaseMetaTileEntity();
        for (int path = 0; path < 4; path++) {
            controller.clearHatches();
            boolean accepted = switch (path) {
                case 0 -> controller.addToMachineList(tile, 0);
                case 1 -> controller.addOutputToMachineList(tile, 0);
                case 2 -> controller.addOutputBusToMachineList(tile, 0);
                default -> controller.addOutputHatchToMachineList(tile, 0);
            };
            require(
                accepted && controller.mOutputBusses.size() == 1
                    && controller.mOutputBusses.get(0) == assembly
                    && controller.mOutputHatches.size() == 1
                    && controller.mOutputHatches.get(0) == assembly.getFluidOutput(),
                "native controller registers both roles exactly once path=" + path);
            require(
                controller.getOutputBusses().contains(assembly)
                    && controller.getOutputHatches().contains(assembly.getFluidOutput()),
                "native validity checks retain the fluid adapter path=" + path);
        }
        controller.clearHatches();
        controller.addOutputToMachineList(tile, 0);
        require(controller.addOutputAtomic(new ItemStack(Items.iron_ingot, 3)),
            "native controller ejects items through assembly");
        require(controller.addOutputAtomic(new FluidStack(FluidRegistry.WATER, 7)),
            "native controller ejects fluids through assembly");
        itemAmount = 3;
        fluidAmount = 7;
        controller.clearHatches();
    }

    private void verify(EntityPlayerMP player) {
        ItemStack iron = new ItemStack(Items.iron_ingot, 16);
        ItemStack gold = new ItemStack(Items.gold_ingot, 8);
        FluidStack water = new FluidStack(FluidRegistry.WATER, 1000);
        FluidStack lava = new FluidStack(FluidRegistry.LAVA, 1000);
        var fluids = assembly.getFluidOutput();
        requireCapacities(assembly, "both caches start at Long.MAX_VALUE");
        require(!assembly.isFiltered() && !fluids.isFiltered(), "empty item and fluid filters accept all");
        assembly.getFilters().setItem(8, iron);
        require(assembly.isFiltered() && !fluids.isFiltered(), "item samples do not filter the fluid output");
        assembly.getFilters().setFluid(8, water);
        require(iron.stackSize == 16 && water.amount == 1000, "filtering does not consume source samples");
        require(assembly.getFilters().getItem(8).stackSize == 1 && assembly.getFilters().getFluid(8).amount == 1,
            "both sample amounts normalize to one");
        require(!assembly.storePartial(gold, false) && gold.stackSize == 8, "direct item rejection preserves input");
        require(fluids.fill(lava, true) == 0 && lava.amount == 1000, "direct fluid rejection preserves input");
        require(assembly.storePartial(iron.copy(), true) && fluids.fill(water, false) == 1000,
            "matching direct simulations accept both channels");
        requireAmounts(assembly, itemAmount, fluidAmount, "direct simulations leave caches unchanged");
        for (boolean cache : new boolean[] { false, true }) {
            assembly.getProvider().setCacheMode(cache);
            fluids.getProvider().setCacheMode(cache);
            for (boolean check : new boolean[] { false, true }) {
                assembly.getProvider().setCheckMode(check);
                fluids.getProvider().setCheckMode(check);
                var itemTransaction = assembly.createTransaction();
                var fluidTransaction = fluids.createTransaction();
                ((IOutputTransaction.IRecipeCheckAware) itemTransaction).setRecipeCheck(true);
                ((IOutputTransaction.IRecipeCheckAware) fluidTransaction).setRecipeCheck(true);
                ItemStack excludedItem = gold.copy();
                FluidStack excludedFluid = lava.copy();
                require(!itemTransaction.storePartial(GTUtility.ItemId.create(gold), excludedItem, 8, 8)
                    && excludedItem.stackSize == 8, "recipe item rejection cache=" + cache + " check=" + check);
                require(!fluidTransaction.storePartial(GTUtility.FluidId.create(lava), excludedFluid, 1000, 1000)
                    && excludedFluid.amount == 1000, "recipe fluid rejection cache=" + cache + " check=" + check);
                ItemStack acceptedItem = iron.copy();
                FluidStack acceptedFluid = water.copy();
                require(itemTransaction.storePartial(GTUtility.ItemId.create(iron), acceptedItem, 16, 16)
                    && acceptedItem.stackSize == 0, "recipe item acceptance cache=" + cache + " check=" + check);
                require(fluidTransaction.storePartial(GTUtility.FluidId.create(water), acceptedFluid, 1000, 1000)
                    && acceptedFluid.amount == 0, "recipe fluid acceptance cache=" + cache + " check=" + check);
                requireAmounts(assembly, itemAmount, fluidAmount, "uncommitted transactions leave caches unchanged");
            }
        }
        assembly.getProvider().setCacheMode(false);
        fluids.getProvider().setCacheMode(false);
        assembly.getProvider().setCheckMode(false);
        fluids.getProvider().setCheckMode(false);
        ItemEjectionHelper itemOutput = new ItemEjectionHelper(Arrays.asList(fallback, assembly), true);
        require(itemOutput.ejectStack(iron.copy()) == 16, "native item ejection accepts a matching output");
        itemOutput.commit();
        FluidEjectionHelper fluidOutput = new FluidEjectionHelper(Arrays.asList(fallback.getFluidOutput(), fluids),
            true);
        require(fluidOutput.ejectStack(water.copy()) == 1000, "native fluid ejection accepts a matching output");
        fluidOutput.commit();
        itemAmount += 16;
        fluidAmount += 1000;
        requireAmounts(assembly, itemAmount, fluidAmount, "filtered outputs win with the fallback listed first");
        requireAmounts(fallback, 0, 0, "matching outputs do not enter the unfiltered fallback");
        ItemEjectionHelper otherItems = new ItemEjectionHelper(Arrays.asList(assembly, fallback), true);
        otherItems.ejectStack(gold.copy());
        otherItems.commit();
        FluidEjectionHelper otherFluids = new FluidEjectionHelper(Arrays.asList(fluids, fallback.getFluidOutput()),
            true);
        otherFluids.ejectStack(lava.copy());
        otherFluids.commit();
        requireAmounts(fallback, 8, 1000, "unmatched outputs route to the unfiltered fallback");
        ItemStack taggedItem = iron.copy();
        taggedItem.setTagCompound(new NBTTagCompound());
        taggedItem.getTagCompound().setString("different", "nbt");
        FluidStack taggedFluid = water.copy();
        taggedFluid.tag = new NBTTagCompound();
        taggedFluid.tag.setString("different", "nbt");
        require(!assembly.getFilters().accepts(taggedItem) && !assembly.getFilters().accepts(taggedFluid),
            "both filters preserve exact NBT identity");
        for (int i = 0; i < 2; i++) {
            require(assembly.storePartial(new ItemStack(Items.iron_ingot, Integer.MAX_VALUE), false),
                "direct items accept an int-sized batch");
            require(fluids.fill(new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE), true) == Integer.MAX_VALUE,
                "direct fluids accept an int-sized batch");
            itemAmount += Integer.MAX_VALUE;
            fluidAmount += Integer.MAX_VALUE;
        }
        requireAmounts(assembly, itemAmount, fluidAmount, "both caches hold more than Integer.MAX_VALUE");
        assembly.setInventorySlotContents(0, AEApi.instance().definitions().items().cell64k().maybeStack(1).get());
        assembly.onContentsChanged(0);
        require(assembly.getCellStack() != null, "native item storage cell is installed in the optional cell slot");
        requireCapacities(assembly, "native item cell insertion preserves both long capacities");
        assembly.setInventorySlotContents(0, null);
        assembly.onContentsChanged(0);
        requireCapacities(assembly, "native item cell removal preserves both long capacities");
        assembly.onScrewdriverRightClick(ForgeDirection.NORTH, player, 0, 0, 0, null);
        requireCapacities(assembly, "cell update and cache-mode changes preserve long capacities");
        assembly = (MEOutputAssembly) reload((BaseMetaTileEntity) assembly.getBaseMetaTileEntity()).getMetaTileEntity();
        require(assembly.getFilters().hasItems() && assembly.getFilters().hasFluids(),
            "world reload preserves both filters");
        requireAmounts(assembly, itemAmount, fluidAmount, "world reload preserves both long caches independently");
        requireCapacities(assembly, "world reload preserves both long capacities");
        assembly.getFilters().setItem(8, null);
        require(!assembly.isFiltered() && assembly.getFluidOutput().isFiltered(),
            "clearing item filters preserves fluid filtering");
        assembly.getFilters().setFluid(8, null);
        require(!assembly.isFiltered() && !assembly.getFluidOutput().isFiltered()
            && assembly.getFilters().accepts(gold)
            && assembly.getFilters().accepts(lava), "clearing the last samples restores accept-all independently");
        assembly.getFilters().setItem(0, iron);
        assembly.getFilters().setItem(8, gold);
        assembly.getFilters().setFluid(0, water);
        assembly.getFilters().setFluid(8, lava);
    }

    private static void requireCapacities(MEOutputAssembly target, String message) {
        require(target.getProvider().getCacheCapacity() == Long.MAX_VALUE
            && target.getFluidOutput().getProvider().getCacheCapacity() == Long.MAX_VALUE, message);
    }

    private static void requireAmounts(MEOutputAssembly target, long items, long fluids, String message) {
        require(
            target.getProvider().getCachedAmount() == items
                && target.getFluidOutput().getProvider().getCachedAmount() == fluids,
            message + ": item="
                + target.getProvider().getCachedAmount()
                + ", fluid="
                + target.getFluidOutput().getProvider().getCachedAmount());
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x, ItemStack stack) {
        world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
        tile.setInitialValuesAsNBT(stack.getTagCompound(), (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private static BaseMetaTileEntity reload(BaseMetaTileEntity tile) {
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        World world = tile.getWorld();
        world.removeTileEntity(tile.xCoord, tile.yCoord, tile.zCoord);
        BaseMetaTileEntity restored = new BaseMetaTileEntity();
        restored.setWorldObj(world);
        restored.readFromNBT(tag);
        world.setTileEntity(tile.xCoord, tile.yCoord, tile.zCoord, restored);
        return restored;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || screenshot == 0 || finished || ++frames < 20) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (screenshot == 1) {
                if (!(mc.currentScreen instanceof GuiContainerWrapper)) {
                    if (frames < 600) return;
                    throw new AssertionError("native MUI2 GUI did not open");
                }
                IWidget panel = ((GuiContainerWrapper) mc.currentScreen).getScreen().getMainPanel();
                List<PhantomItemSlot> items = new ArrayList<>();
                List<FluidSlot> fluids = new ArrayList<>();
                collectSlots(panel, items, fluids);
                if (!filtersDragged) {
                    require(items.size() == 9 && fluids.size() == 9,
                        "runtime GUI contains nine item and nine fluid ghost slots");
                    require(items.get(4).handleDragAndDrop(new ItemStack(Items.diamond, 32), 0),
                        "real item phantom widget accepts an NEI item drag");
                    require(fluids.get(4).handleDragAndDrop(new ItemStack(Items.water_bucket), 0),
                        "real fluid phantom widget accepts an NEI bucket drag");
                    filtersDragged = true;
                }
                if (frames < 80) return;
                ItemStack item = items.get(4).getSlot().getStack();
                FluidStack fluid = fluids.get(4).getSyncHandler().getValue();
                if ((item == null || fluid == null) && frames < 600) return;
                require(item != null && item.getItem() == Items.diamond && item.stackSize == 1,
                    "item ghost sample synchronizes back to the client");
                require(fluid != null && fluid.getFluid() == FluidRegistry.WATER && fluid.amount == 1,
                    "fluid ghost sample synchronizes back to the client");
                require(mc.thePlayer.inventory.getItemStack() == null, "marking does not create cursor items");
                saveScreenshot("assembly-filters.png");
                frames = 0;
                screenshot = 0;
            } else if (screenshot == 2) {
                if (frames < 80 || mc.currentScreen != null) return;
                saveScreenshot("assembly-world.png");
                mc.displayGuiScreen(new ModelProbeGui());
                frames = 0;
                screenshot = 3;
            } else if (frames >= 40) {
                require(mc.currentScreen instanceof ModelProbeGui, "native inventory model preview is rendered");
                saveScreenshot("assembly-inventory.png");
                frames = 0;
                visualsDone = true;
                screenshot = 0;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void collectSlots(IWidget widget, List<PhantomItemSlot> items, List<FluidSlot> fluids) {
        if (widget instanceof PhantomItemSlot item) items.add(item);
        if (widget instanceof FluidSlot fluid) fluids.add(fluid);
        for (IWidget child : widget.getChildren()) collectSlots(child, items, fluids);
    }

    private static final class ModelProbeGui extends GuiScreen {

        private final RenderItem renderItem = new RenderItem();

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawRect(0, 0, width, height, 0xFF303238);
            drawCenteredString(fontRendererObj, "ME 输出总成 · 原生物品模型", width / 2, height / 2 - 100, 0xFFFFFF);
            ItemStack[] variants = { ItemList.Hatch_Output_Bus_ME.get(1), ItemList.Hatch_Output_ME.get(1),
                GTNGItemList.MEOutputAssembly.get(1) };
            String[] names = { "原版 ME 输出总线", "原版 ME 输出仓", "ME 输出总成" };
            for (int i = 0; i < variants.length; i++) {
                int x = width / 2 - 180 + 145 * i, y = height / 2 - 40;
                drawRect(x - 3, y - 3, x + 67, y + 67, 0xFFD0D0D0);
                drawRect(x, y, x + 64, y + 64, 0xFF888888);
                GL11.glPushMatrix();
                GL11.glTranslatef(x, y, 0);
                GL11.glScalef(4, 4, 4);
                RenderHelper.enableGUIStandardItemLighting();
                renderItem.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), variants[i], 0, 0);
                RenderHelper.disableStandardItemLighting();
                GL11.glPopMatrix();
                drawCenteredString(fontRendererObj, names[i], x + 32, y + 80, 0xFFFFFF);
            }
        }
    }

    private static File output() {
        File output = new File(System.getProperty("gtng.meOutputAssembly.output"));
        output.mkdirs();
        return output;
    }

    private static void saveScreenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), trace.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        System.out.println("ME_OUTPUT_ASSEMBLY_QA: " + message);
    }
}
