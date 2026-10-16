package com.xyp.gtnotgood.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.common.machines.hatch.me.SuperAdvancedMEInputHatch;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.GTUtility;

/** Exercises real AE storage, native GT transactions, item drops and client GUI synchronization. */
@Mod(
    modid = "superstorageqa",
    name = "Super storage input QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class SuperStorageInputChecks {

    private boolean started, finished, transactionVerified;
    private volatile int visualStage;
    private volatile boolean policyPopupOpened, policySlotsReordered;
    private int ticks, frames, autoStage, autoTick, guiReadyTick, policyWriteFrame;
    private SuperAdvancedMEInputHatch hatch;
    private MTEMultiBlockBase controller;
    private IMEMonitor<IAEFluidStack> inventory;
    private MachineSource source;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        ModularUIConfig.guiDebugMode = false;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("super-storage-" + System.currentTimeMillis(), "Super storage QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (++ticks == 1) setup(player);
            if (ticks > 1600) throw new AssertionError("QA timed out at visual stage " + visualStage);
            if (policyPopupOpened && !policySlotsReordered) {
                int lavaSlot = findMark(new FluidStack(FluidRegistry.LAVA, 1));
                if (lavaSlot >= 0) hatch.setSlotConfig(lavaSlot, null);
                hatch.setSlotConfig(899, null);
                hatch.setSlotConfig(0, null);
                hatch.setSlotConfig(899, water(1));
                hatch.setSlotConfig(0, new FluidStack(FluidRegistry.LAVA, 1));
                require(
                    GTUtility.areFluidsEqual(hatch.getSlotConfig(899), water(1))
                        && GTUtility.areFluidsEqual(hatch.getSlotConfig(0), new FluidStack(FluidRegistry.LAVA, 1)),
                    "server reorders selected fluid while its real policy popup remains open");
                policySlotsReordered = true;
            }
            if (visualStage == 3) {
                require(!hatch.isLimitedMode() && !hatch.isFixedMode(),
                    "middle-column mode buttons synchronize to server");
                require(hatch.getPolicy(899).reserve == 10000000000L && hatch.getPolicy(899).batch == 1250,
                    "real policy popup edits the selected fluid after it moves to another slot");
                require(hatch.getPolicy(0).reserve == 0 && hatch.getPolicy(0).batch == 1000,
                    "fluid replacing the selected slot keeps its default policy");
                Files.write(new File("result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                finished = true;
                Minecraft.getMinecraft().shutdown();
                return;
            }
            if (ticks < 100 || visualStage != 0 || !hatch.isActive()) return;
            if (!transactionVerified) {
                if (!verifyAutoPull()) return;
                verify(player);
                transactionVerified = true;
                return;
            }
            if (guiReadyTick == 0) {
                inventory = hatch.getProxy().getStorage().getFluidInventory();
                source = new MachineSource((IActionHost) hatch.getBaseMetaTileEntity());
                inject(20000);
                hatch.setAutoPullRefreshTime(20);
                hatch.setSlotConfig(0, water(1));
                hatch.getPolicy(0).reserve = 10000;
                hatch.getPolicy(0).batch = 1000;
                hatch.setLimitedMode(true);
                hatch.setFixedMode(true);
                int mark = 1;
                for (var fluid : FluidRegistry.getRegisteredFluids().values()) {
                    if (fluid == FluidRegistry.WATER) continue;
                    hatch.setSlotConfig(mark++, new FluidStack(fluid, 1));
                    if (mark == 900) break;
                }
                require(mark == 900, "all 900 fluid marks populated for GUI stress check");
                guiReadyTick = ticks;
                return;
            }
            if (ticks - guiReadyTick < 25) return;
            require(markedAmount(water(1)) == 1000, "server GUI fluid stock reflects the configured fixed quantity");
            hatch.onRightclick(hatch.getBaseMetaTileEntity(), player);
            visualStage = 1;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        BaseMetaTileEntity tile = place(player, 0, GTNGItemList.SuperAdvancedMEInputHatch.get(1));
        hatch = (SuperAdvancedMEInputHatch) tile.getMetaTileEntity();
        require(ItemStack.areItemStacksEqual(GTNGItemList.SuperAdvancedMEInputHatch.get(1), tile.getDrops().get(0)),
            "default empty hatch stacks with a freshly crafted hatch");
        hatch.setConnectsToAllSides(true);
        hatch.setAutoPullRefreshTime(Integer.MAX_VALUE);
        var defs = AEApi.instance().definitions();
        world.setBlock(1, 10, 0, defs.blocks().energyCellCreative().maybeBlock().get());
        world.setBlock(0, 10, 1, defs.blocks().drive().maybeBlock().get());
        ((TileDrive) world.getTileEntity(0, 10, 1)).getInternalInventory().setInventorySlotContents(0,
            new ItemStack(ItemAndBlockHolder.CELL16384KM));
        controller = (MTEMultiBlockBase) place(player, 10, GTNGItemList.LargeOreProcessor.get(1)).getMetaTileEntity();
        player.playerNetServerHandler.setPlayerLocation(0, 10, -3, 0, 10);
    }

    private boolean verifyAutoPull() throws Exception {
        if (autoStage > 0 && ticks - autoTick < 25) return false;
        switch (autoStage) {
            case 0 -> {
                inventory = hatch.getProxy().getStorage().getFluidInventory();
                source = new MachineSource((IActionHost) hatch.getBaseMetaTileEntity());
                hatch.setSlotConfig(899, new FluidStack(FluidRegistry.getFluid("steam"), 1));
                hatch.getPolicy(899).reserve = 7;
                hatch.getPolicy(899).batch = 13;
                hatch.setAutoPullFluidList(false);
                require(hatch.getSlotConfig(899) != null && hatch.getPolicy(899).reserve == 7,
                    "repeating disabled auto-pull preserves manual fluid marks and policy");
                inject(120);
                inject(new FluidStack(FluidRegistry.LAVA, 80));
                inject(new FluidStack(FluidRegistry.getFluid("hydrogen"), 20));
                hatch.setMinAutoPullAmount(50);
                hatch.setAutoPullRefreshTime(20);
                hatch.setAutoPullFluidList(true);
                require(
                    markCount() == 2 && markedAmount(water(1)) == 120
                        && markedAmount(new FluidStack(FluidRegistry.LAVA, 1)) == 80,
                    "enabling auto-pull immediately selects ME fluids and their quantities");
                require(
                    findMark(new FluidStack(FluidRegistry.getFluid("steam"), 1)) < 0 && policiesDefaultExceptMarks(),
                    "enabling auto-pull clears the previous manual fluid mark and policy");
                var policy = hatch.getPolicy(findMark(water(1)));
                policy.reserve = 7;
                policy.batch = 13;
                inject(30);
                inject(new FluidStack(FluidRegistry.LAVA, 40));
                inject(new FluidStack(FluidRegistry.getFluid("oxygen"), 60));
            }
            case 1 -> {
                require(
                    markCount() == 3 && markedAmount(water(1)) == 150
                        && markedAmount(new FluidStack(FluidRegistry.LAVA, 1)) == 120
                        && markedAmount(new FluidStack(FluidRegistry.getFluid("oxygen"), 1)) == 60
                        && findMark(new FluidStack(FluidRegistry.getFluid("hydrogen"), 1)) < 0,
                    "real refresh ticks discover new fluid types and updated quantities above the threshold");
                var policy = hatch.getPolicy(findMark(water(1)));
                require(policy.reserve == 7 && policy.batch == 13, "periodic fluid refresh preserves type policy");
                policy = hatch.getPolicy(findMark(new FluidStack(FluidRegistry.getFluid("oxygen"), 1)));
                policy.reserve = 9;
                policy.batch = 17;
                withdraw(water(120));
                withdraw(new FluidStack(FluidRegistry.LAVA, 120));
            }
            case 2 -> {
                require(
                    markCount() == 1 && findMark(water(1)) < 0
                        && findMark(new FluidStack(FluidRegistry.LAVA, 1)) < 0
                        && markedAmount(new FluidStack(FluidRegistry.getFluid("oxygen"), 1)) == 60,
                    "periodic fluid refresh removes exhausted and below-threshold types");
                var policy = hatch.getPolicy(findMark(new FluidStack(FluidRegistry.getFluid("oxygen"), 1)));
                require(policy.reserve == 9 && policy.batch == 17 && policiesDefaultExceptMarks(),
                    "surviving fluid keeps its policy after compaction while removed policies are cleared");
                hatch.setMinAutoPullAmount(100);
            }
            case 3 -> {
                require(markCount() == 0 && policiesDefaultExceptMarks(),
                    "raising the fluid threshold rescreens marks and clears excluded policies");
                hatch.setMinAutoPullAmount(50);
            }
            case 4 -> {
                require(markCount() == 1 && markedAmount(new FluidStack(FluidRegistry.getFluid("oxygen"), 1)) == 60,
                    "lowering the fluid threshold restores eligible stock on a real refresh tick");
                var policy = hatch.getPolicy(findMark(new FluidStack(FluidRegistry.getFluid("oxygen"), 1)));
                require(policy.reserve == 0 && policy.batch == 1000, "returning fluid receives a fresh default policy");
                policy.reserve = 11;
                policy.batch = 19;
                hatch.getPolicy(899).reserve = 23;
                hatch.getPolicy(899).batch = 29;
                hatch.setAutoPullFluidList(false);
                require(markCount() == 0 && policiesDefaultExceptMarks(),
                    "disabling auto-pull immediately clears every fluid mark and all 900 policies");
                withdraw(water(30));
                withdraw(new FluidStack(FluidRegistry.getFluid("hydrogen"), 20));
                withdraw(new FluidStack(FluidRegistry.getFluid("oxygen"), 60));
                hatch.setAutoPullRefreshTime(Integer.MAX_VALUE);
                return true;
            }
            default -> throw new AssertionError("Unknown auto-pull stage " + autoStage);
        }
        autoStage++;
        autoTick = ticks;
        return false;
    }

    private int findMark(FluidStack fluid) {
        for (int index = 0; index < 900; index++) {
            if (GTUtility.areFluidsEqual(hatch.getSlotConfig(index), fluid)) return index;
        }
        return -1;
    }

    private int markedAmount(FluidStack fluid) {
        int index = findMark(fluid);
        if (index < 0) return 0;
        var slot = hatch.getStorageSlots()[index];
        return slot.extracted == null ? 0 : slot.extracted.amount;
    }

    private int markCount() {
        int count = 0;
        for (int index = 0; index < 900; index++) {
            if (hatch.getSlotConfig(index) != null) count++;
        }
        return count;
    }

    private boolean policiesDefaultExceptMarks() {
        for (int index = 0; index < 900; index++) {
            if (hatch.getSlotConfig(index) != null) continue;
            var policy = hatch.getPolicy(index);
            if (policy.reserve != 0 || policy.batch != 1000) return false;
        }
        return true;
    }

    private void withdraw(FluidStack fluid) {
        IAEFluidStack result = inventory.extractItems(AEFluidStack.create(fluid), Actionable.MODULATE, source);
        require(result != null && result.getStackSize() == fluid.amount, "auto-pull fixture fluid withdrawn");
    }

    private void verify(EntityPlayerMP player) throws Exception {
        inventory = hatch.getProxy().getStorage().getFluidInventory();
        source = new MachineSource((IActionHost) hatch.getBaseMetaTileEntity());
        hatch.setSlotConfig(899, water(1));
        require(hatch.getStorageSlots().length == 900 && hatch.getSlotConfig(899) != null,
            "last of 900 slots is usable");
        hatch.setSlotConfig(0, water(1));
        require(hatch.getSlotConfig(0) == null, "duplicate fluid marks rejected");
        var policy = hatch.getPolicy(899);
        hatch.setLimitedMode(true);
        hatch.setFixedMode(true);
        policy.reserve = 10000;
        policy.batch = 1000;
        inject(10500);
        hatch.startRecipeProcessing();
        require(hatch.getStoredFluids().length == 0 && stored() == 10500,
            "insufficient excess offers nothing and leaves ME untouched");
        require(hatch.endRecipeProcessing(controller).wasSuccessful(), "empty check commits");
        inject(500);
        hatch.startRecipeProcessing();
        require(stored() == 11000 && hatch.getStoredFluids()[0].amount == 1000,
            "fixed amount exposed without pre-extraction");
        require(hatch.drain(ForgeDirection.UNKNOWN, water(600), false).amount == 600 && stored() == 11000,
            "simulation does not consume network fluid");
        require(hatch.drain(ForgeDirection.UNKNOWN, water(600), true).amount == 600 && stored() == 11000,
            "controller consumes only the check snapshot");
        require(hatch.endRecipeProcessing(controller).wasSuccessful(), "fixed check commits");
        require(stored() == 10400, "only 600 mB actually consumed, all remainder stays in ME");
        hatch.endRecipeProcessing(controller);
        require(stored() == 10400, "duplicate commit is harmless");
        hatch.startRecipeProcessing();
        require(hatch.getStoredFluids().length == 0, "fixed amount unavailable below full threshold");
        hatch.endRecipeProcessing(controller);
        hatch.setFixedMode(false);
        require(hatch.drain(ForgeDirection.NORTH, water(400), true) == null, "pipes cannot drain the hatch");
        hatch.startRecipeProcessing();
        require(hatch.getStoredFluids()[0].amount == 400, "reserve mode exposes only excess");
        hatch.getStoredFluids()[0].amount -= 200;
        require(hatch.endRecipeProcessing(controller).wasSuccessful() && stored() == 10200,
            "native shared-stack consumption commits");
        require(hatch.drain(ForgeDirection.UNKNOWN, water(1000), false).amount == 200 && stored() == 10200,
            "direct drain simulation respects reserve");
        require(hatch.drain(ForgeDirection.UNKNOWN, water(1000), true).amount == 200 && stored() == 10000,
            "direct drain cannot cross reserve");
        require(hatch.drain(ForgeDirection.UNKNOWN, water(1), true) == null, "reserve equality denies extraction");
        inject(2000);
        hatch.setFixedMode(true);
        require(hatch.drain(ForgeDirection.UNKNOWN, water(5000), false).amount == 1000 && stored() == 12000,
            "fixed direct drain capped without mutation");
        require(hatch.drain(ForgeDirection.UNKNOWN, water(5000), true).amount == 1000 && stored() == 11000,
            "fixed direct drain never exceeds configured amount");

        // Stock changes between recipe simulation and commit must never break the reserve.
        hatch.setFixedMode(false);
        hatch.startRecipeProcessing();
        hatch.getStoredFluids()[0].amount -= 1000;
        inventory.extractItems(AEFluidStack.create(water(500)), Actionable.MODULATE, source);
        require(!hatch.endRecipeProcessing(controller).wasSuccessful() && stored() == 10500,
            "commit rechecks reserve after another consumer changes ME stock");
        require(hatch.drain(ForgeDirection.UNKNOWN, water(1), false).amount == 1,
            "failed commit releases processing state");
        hatch.setFixedMode(true);

        NBTTagCompound copied = hatch.getCopiedData(player);
        SuperAdvancedMEInputHatch copy = (SuperAdvancedMEInputHatch) place(player, 4,
            GTNGItemList.SuperAdvancedMEInputHatch.get(1)).getMetaTileEntity();
        require(copy.pasteCopiedData(player, copied) && copy.getPolicy(899).reserve == 10000
            && copy.isFixedMode()
            && copy.isLimitedMode(), "data-stick copy preserves marks and mode settings");

        BaseMetaTileEntity oldTile = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
        NBTTagCompound worldSave = new NBTTagCompound();
        oldTile.writeToNBT(worldSave);
        ItemStack drop = oldTile.getDrops().get(0);
        require(ItemStack.areItemStacksEqual(drop, GTNGItemList.SuperAdvancedMEInputHatch.get(1)),
            "configured hatch drop stacks with a fresh hatch");
        BaseMetaTileEntity loaded = new BaseMetaTileEntity();
        loaded.setWorldObj(player.worldObj);
        loaded.readFromNBT(worldSave);
        SuperAdvancedMEInputHatch saved = (SuperAdvancedMEInputHatch) loaded.getMetaTileEntity();
        require(saved.getSlotConfig(899) != null && saved.getPolicy(899).reserve == 10000
            && saved.isFixedMode()
            && saved.isLimitedMode(), "world NBT retains marks and rules");
        loaded.invalidate();
        player.worldObj.setBlockToAir(0, 10, 0);
        BaseMetaTileEntity restored = place(player, 0, drop);
        hatch = (SuperAdvancedMEInputHatch) restored.getMetaTileEntity();
        require(hatch.getSlotConfig(899) == null && !hatch.isLimitedMode() && !hatch.isFixedMode(),
            "mining and replacement clears marks and mode settings");
        hatch.setConnectsToAllSides(true);
        var edge = new SuperAdvancedMEInputHatch.FluidPolicy();
        edge.reserve = Long.MAX_VALUE - 5;
        require(edge.offered(Long.MAX_VALUE, true, false) == 5 && edge.offered(0, true, false) == 0,
            "long ME quantities retain precision");
        edge.batch = 6;
        require(edge.offered(Long.MAX_VALUE, true, true) == 0, "full fixed quantity required at long boundary");
    }

    private void inject(int amount) {
        inject(water(amount));
    }

    private void inject(FluidStack fluid) {
        require(inventory.injectItems(AEFluidStack.create(fluid), Actionable.MODULATE, source) == null,
            "test fluid stored");
    }

    private long stored() {
        IAEFluidStack result = inventory.getStorageList().findPrecise(AEFluidStack.create(water(1)));
        return result == null ? 0 : result.getStackSize();
    }

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.WATER, amount);
    }

    private static BaseMetaTileEntity place(EntityPlayerMP player, int x, ItemStack stack) {
        player.worldObj.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, 10, 0);
        tile.setInitialValuesAsNBT(stack.getTagCompound(), (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || visualStage == 0 || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainerWrapper)) return;
            var panel = ((GuiContainerWrapper) mc.currentScreen).getScreen().getMainPanel();
            if (++frames == 40) {
                List<FluidSlot> slots = new ArrayList<>();
                collect(panel, slots);
                require(slots.size() == 1800, "900 filter widgets and 900 stock widgets");
                require(slots.subList(0, 900).stream().allMatch(slot -> slot.getFluidStack() != null),
                    "all 900 fluid marks synchronize to client");
                require(slots.get(900).getFluidStack() != null && slots.get(900).getFluidStack().amount == 1000,
                    "real fluid stock GUI synchronizes the configured fixed quantity");
                screenshot("super-storage-main.png");
                List<Grid> grids = new ArrayList<>();
                collectGrids(panel, grids);
                require(grids.size() == 2, "paired fluid grids");
                var left = grids.get(0).getScrollArea();
                var right = grids.get(1).getScrollArea();
                left.getScrollY().scrollTo(left, Integer.MAX_VALUE);
                require(
                    left.getScrollY().getScroll() > 0
                        && left.getScrollY().getScroll() == right.getScrollY().getScroll(),
                    "filter and stock rows scroll together through all 900 slots");
                left.getScrollY().scrollTo(left, 0);
                List<ToggleButton> buttons = new ArrayList<>();
                collectButtons(panel, buttons);
                require(buttons.size() == 3, "auto-pull plus two middle-column mode switches");
                buttons.get(1).onMousePressed(0);
                buttons.get(2).onMousePressed(0);
                slots.get(0).onMousePressed(1);
                visualStage = 2;
            }
            if (visualStage == 2 && frames >= 50 && !policyPopupOpened) {
                List<TextFieldWidget> fields = new ArrayList<>();
                for (var open : ((GuiContainerWrapper) mc.currentScreen).getScreen().getPanelManager()
                    .getOpenPanels()) {
                    if (open.getName().equals("fluidPolicy")) collectFields(open, fields);
                }
                if (fields.size() == 2) policyPopupOpened = true;
            }
            if (visualStage == 2 && frames >= 70 && policySlotsReordered && policyWriteFrame == 0) {
                screenshot("super-storage-policy.png");
                List<TextFieldWidget> fields = new ArrayList<>();
                for (var open : ((GuiContainerWrapper) mc.currentScreen).getScreen().getPanelManager()
                    .getOpenPanels()) {
                    if (open.getName().equals("fluidPolicy")) collectFields(open, fields);
                }
                require(fields.size() == 2, "per-fluid amount popup opened");
                fields.get(0).setText("10000000000");
                fields.get(0).onRemoveFocus(panel.getContext());
                fields.get(1).setText("1250");
                fields.get(1).onRemoveFocus(panel.getContext());
                policyWriteFrame = frames;
            }
            if (visualStage == 2 && policyWriteFrame > 0 && frames - policyWriteFrame >= 40) {
                visualStage = 3;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void collect(IWidget widget, List<FluidSlot> slots) {
        if (widget instanceof FluidSlot) slots.add((FluidSlot) widget);
        for (IWidget child : widget.getChildren()) collect(child, slots);
    }

    private static void collectButtons(IWidget widget, List<ToggleButton> buttons) {
        if (widget instanceof ToggleButton) buttons.add((ToggleButton) widget);
        for (IWidget child : widget.getChildren()) collectButtons(child, buttons);
    }

    private static void collectGrids(IWidget widget, List<Grid> grids) {
        if (widget instanceof Grid) grids.add((Grid) widget);
        for (IWidget child : widget.getChildren()) collectGrids(child, grids);
    }

    private static void collectFields(IWidget widget, List<TextFieldWidget> fields) {
        if (widget instanceof TextFieldWidget) fields.add((TextFieldWidget) widget);
        for (IWidget child : widget.getChildren()) collectFields(child, fields);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("SUPER_STORAGE_QA: " + message);
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }
}
