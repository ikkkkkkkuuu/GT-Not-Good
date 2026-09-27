package com.xyp.gtnotgood.common.machines.hatch.me;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.IOutputTransaction;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;

/** Exercises native ejection order, simulation, persistence and both real-client nine-slot GUIs. */
@Mod(
    modid = "meoutputfiltersqa",
    name = "ME Output Filters QA",
    version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public final class MEOutputFiltersClientChecks {

    private boolean started;
    private boolean finished;
    private int ticks;
    private int stage;
    private int openFluidAt;
    private volatile int screenshot;
    private int frames;
    private MaxCapacityMEOutputBus bus;
    private MaxCapacityMEOutputBus fallbackBus;
    private MaxCapacityMEOutputHatch hatch;
    private MaxCapacityMEOutputHatch fallbackHatch;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.meoutputfilters.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "me-output-filters-qa-" + System.currentTimeMillis(),
                "ME Output Filters QA",
                new WorldSettings(25L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            ticks++;
            if (ticks == 1) {
                World world = player.worldObj;
                bus = (MaxCapacityMEOutputBus) place(world, player, 0, GTNGItemList.MaxCapacityMEOutputBus.get(1))
                    .getMetaTileEntity();
                fallbackBus = (MaxCapacityMEOutputBus) place(
                    world,
                    player,
                    2,
                    GTNGItemList.MaxCapacityMEOutputBus.get(1)).getMetaTileEntity();
                hatch = (MaxCapacityMEOutputHatch) place(world, player, 4, GTNGItemList.MaxCapacityMEOutputHatch.get(1))
                    .getMetaTileEntity();
                fallbackHatch = (MaxCapacityMEOutputHatch) place(
                    world,
                    player,
                    6,
                    GTNGItemList.MaxCapacityMEOutputHatch.get(1)).getMetaTileEntity();
                player.playerNetServerHandler.setPlayerLocation(2, 10, -3, 0, 15);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
            }
            if (ticks > 1200) throw new AssertionError("QA timed out");
            if (ticks < 80 || screenshot != 0) return;
            if (stage == 0) {
                verifyDrops(player);
                verify();
                bus.onRightclick(bus.getBaseMetaTileEntity(), player);
                screenshot = 1;
                stage = 1;
            } else if (stage == 1) {
                require(
                    bus.getFilters()
                        .getItem(4) != null
                        && bus.getFilters()
                            .getItem(4)
                            .getItem() == Items.diamond,
                    "GUI item drag reaches authoritative server filter");
                player.closeScreen();
                // Let the old GUI's client close acknowledgement arrive before opening another container.
                openFluidAt = ticks + 10;
                stage = 2;
            } else if (stage == 2) {
                if (ticks < openFluidAt) return;
                hatch.onRightclick(hatch.getBaseMetaTileEntity(), player);
                screenshot = 2;
                stage = 3;
            } else {
                require(
                    hatch.getFilters()
                        .getFluid(4) != null
                        && hatch.getFilters()
                            .getFluid(4)
                            .getFluid() == FluidRegistry.WATER,
                    "GUI bucket drag reaches authoritative server filter");
                Files.write(
                    new File("me-output-filters-qa-result.txt").toPath(),
                    "PASS".getBytes(StandardCharsets.UTF_8));
                finished = true;
                Minecraft.getMinecraft()
                    .shutdown();
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finished = true;
            Minecraft.getMinecraft()
                .shutdown();
        }
    }

    /** Checks the actual GregTech drop path, including legacy empty tags and configured machine identity. */
    private static void verifyDrops(EntityPlayerMP player) {
        int x = 8;
        for (boolean fluid : new boolean[] { false, true }) {
            ItemStack fresh = (fluid ? GTNGItemList.MaxCapacityMEOutputHatch : GTNGItemList.MaxCapacityMEOutputBus)
                .get(1);
            BaseMetaTileEntity plain = place(player.worldObj, player, x++, fresh);
            require(
                ItemStack.areItemStacksEqual(
                    fresh,
                    plain.getDrops()
                        .get(0)),
                "plain drop stacks with crafted machine: fluid=" + fluid);

            BaseMetaTileEntity legacy = place(player.worldObj, player, x++, fresh);
            NBTTagCompound old = new NBTTagCompound();
            old.setLong("baseCapacity", Long.MAX_VALUE);
            net.minecraft.nbt.NBTTagList empty = new net.minecraft.nbt.NBTTagList();
            for (int i = 0; i < 9; i++) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setInteger("slot", i);
                empty.appendTag(entry);
            }
            old.setTag("gtngOutputFilters", empty);
            legacy.getMetaTileEntity()
                .loadNBTData(old);
            require(
                ItemStack.areItemStacksEqual(
                    fresh,
                    legacy.getDrops()
                        .get(0)),
                "legacy empty data normalizes on replacement: fluid=" + fluid);

            BaseMetaTileEntity configured = place(player.worldObj, player, x++, fresh);
            MaxCapacityMEOutputFilters filters = fluid
                ? ((MaxCapacityMEOutputHatch) configured.getMetaTileEntity()).getFilters()
                : ((MaxCapacityMEOutputBus) configured.getMetaTileEntity()).getFilters();
            if (fluid) filters.setFluid(8, new FluidStack(FluidRegistry.WATER, 1));
            else filters.setItem(8, new ItemStack(Items.iron_ingot));
            ItemStack marked = configured.getDrops()
                .get(0);
            require(
                ItemStack.areItemStacksEqual(fresh, marked),
                "configured drop resets and stacks with crafted machine: fluid=" + fluid);
            require(!marked.hasTagCompound(), "filters and intrinsic capacity omitted from drop: fluid=" + fluid);

            BaseMetaTileEntity cleared = place(player.worldObj, player, x++, fresh);
            cleared.getMetaTileEntity()
                .loadNBTData(marked.hasTagCompound() ? marked.getTagCompound() : new NBTTagCompound());
            if (fluid) {
                var machine = (MaxCapacityMEOutputHatch) cleared.getMetaTileEntity();
                require(
                    !machine.getFilters()
                        .hasFluids(),
                    "replaced hatch has no fluid marks");
                machine.getFilters()
                    .setFluid(8, null);
                require(
                    machine.getProvider()
                        .getCacheCapacity() == Long.MAX_VALUE,
                    "fluid capacity restored without item capacity tag");
            } else {
                var machine = (MaxCapacityMEOutputBus) cleared.getMetaTileEntity();
                require(
                    !machine.getFilters()
                        .hasItems(),
                    "replaced bus has no item marks");
                machine.getFilters()
                    .setItem(8, null);
                require(
                    machine.getProvider()
                        .getCacheCapacity() == Long.MAX_VALUE,
                    "item capacity restored without item capacity tag");
            }
            require(
                ItemStack.areItemStacksEqual(
                    fresh,
                    cleared.getDrops()
                        .get(0)),
                "cleared filters restore stackability: fluid=" + fluid);
        }
    }

    private void verify() {
        ItemStack iron = new ItemStack(Items.iron_ingot, 16);
        ItemStack gold = new ItemStack(Items.gold_ingot, 8);
        FluidStack water = new FluidStack(FluidRegistry.WATER, 1000);
        FluidStack lava = new FluidStack(FluidRegistry.LAVA, 1000);
        require(!bus.isFiltered() && !hatch.isFiltered(), "old/empty filters accept all");
        bus.getFilters()
            .setItem(8, iron);
        hatch.getFilters()
            .setFluid(8, water);
        require(iron.stackSize == 16 && water.amount == 1000, "samples do not consume originals");
        require(
            bus.getFilters()
                .getItem(8).stackSize == 1
                && hatch.getFilters()
                    .getFluid(8).amount == 1,
            "sample counts normalized");
        require(!bus.storePartial(gold, false) && gold.stackSize == 8, "direct items reject without mutation");
        require(hatch.fill(lava, true) == 0 && lava.amount == 1000, "direct fluids reject without mutation");
        require(bus.storePartial(iron.copy(), true) && hatch.fill(water, false) == 1000, "matching simulation accepts");
        require(
            bus.getProvider()
                .getCachedAmount() == 0
                && hatch.getProvider()
                    .getCachedAmount() == 0,
            "simulation leaves caches unchanged");
        for (boolean check : new boolean[] { false, true }) {
            bus.getProvider()
                .setCheckMode(check);
            hatch.getProvider()
                .setCheckMode(check);
            var itemTransaction = bus.createTransaction();
            var fluidTransaction = hatch.createTransaction();
            ((IOutputTransaction.IRecipeCheckAware) itemTransaction).setRecipeCheck(true);
            ((IOutputTransaction.IRecipeCheckAware) fluidTransaction).setRecipeCheck(true);
            require(
                !itemTransaction.storePartial(GTUtility.ItemId.create(gold), gold.copy(), 8, 8),
                "recipe item rejection check=" + check);
            require(
                !fluidTransaction.storePartial(GTUtility.FluidId.create(lava), lava.copy(), 1000, 1000),
                "recipe fluid rejection check=" + check);
        }
        bus.getProvider()
            .setCheckMode(false);
        hatch.getProvider()
            .setCheckMode(false);
        ItemEjectionHelper itemOutput = new ItemEjectionHelper(Arrays.asList(fallbackBus, bus), true);
        require(itemOutput.ejectStack(iron.copy()) == 16, "native item ejection accepts");
        itemOutput.commit();
        FluidEjectionHelper fluidOutput = new FluidEjectionHelper(Arrays.asList(fallbackHatch, hatch), true);
        require(fluidOutput.ejectStack(water.copy()) == 1000, "native fluid ejection accepts");
        fluidOutput.commit();
        require(
            bus.getProvider()
                .getCachedAmount() == 16
                && fallbackBus.getProvider()
                    .getCachedAmount() == 0,
            "filtered items win even when fallback listed first");
        require(
            hatch.getProvider()
                .getCachedAmount() == 1000
                && fallbackHatch.getProvider()
                    .getCachedAmount() == 0,
            "filtered fluids win even when fallback listed first");
        ItemEjectionHelper otherItems = new ItemEjectionHelper(Arrays.asList(bus, fallbackBus), true);
        otherItems.ejectStack(gold.copy());
        otherItems.commit();
        FluidEjectionHelper otherFluids = new FluidEjectionHelper(Arrays.asList(hatch, fallbackHatch), true);
        otherFluids.ejectStack(lava.copy());
        otherFluids.commit();
        require(
            fallbackBus.getProvider()
                .getCachedAmount() == 8
                && fallbackHatch.getProvider()
                    .getCachedAmount() == 1000,
            "unmatched outputs route to empty-filter fallback");
        NBTTagCompound itemNBT = new NBTTagCompound();
        NBTTagCompound fluidNBT = new NBTTagCompound();
        bus.setItemNBT(itemNBT);
        hatch.setItemNBT(fluidNBT);
        MaxCapacityMEOutputFilters restored = new MaxCapacityMEOutputFilters(() -> {});
        restored.load(itemNBT);
        require(!restored.hasItems(), "pickup clears item filters");
        restored.load(fluidNBT);
        require(!restored.hasFluids(), "pickup clears fluid filters");
        // Recreate real tile entities from full world NBT, including their cached outputs.
        bus = (MaxCapacityMEOutputBus) reload((BaseMetaTileEntity) bus.getBaseMetaTileEntity()).getMetaTileEntity();
        hatch = (MaxCapacityMEOutputHatch) reload((BaseMetaTileEntity) hatch.getBaseMetaTileEntity())
            .getMetaTileEntity();
        require(
            bus.getFilters()
                .hasItems()
                && hatch.getFilters()
                    .hasFluids(),
            "world reload preserves filters");
        require(
            bus.getProvider()
                .getCachedAmount() == 16
                && hatch.getProvider()
                    .getCachedAmount() == 1000,
            "world reload preserves cached outputs");
        ItemStack tagged = iron.copy();
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound()
            .setString("different", "nbt");
        require(
            !bus.getFilters()
                .accepts(tagged),
            "item NBT identities remain distinct");
        FluidStack taggedFluid = water.copy();
        taggedFluid.tag = new NBTTagCompound();
        taggedFluid.tag.setString("different", "nbt");
        require(
            !hatch.getFilters()
                .accepts(taggedFluid),
            "fluid NBT identities remain distinct");
        bus.getFilters()
            .setItem(8, null);
        hatch.getFilters()
            .setFluid(8, null);
        require(
            !bus.isFiltered() && !hatch.isFiltered()
                && bus.getFilters()
                    .accepts(gold)
                && hatch.getFilters()
                    .accepts(lava),
            "clearing final sample restores accept-all");
        bus.getFilters()
            .setItem(0, iron);
        bus.getFilters()
            .setItem(8, gold);
        hatch.getFilters()
            .setFluid(0, water);
        hatch.getFilters()
            .setFluid(8, lava);
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x, ItemStack stack) {
        world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
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
        if (event.phase != TickEvent.Phase.END || screenshot == 0 || ++frames < 20) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainerWrapper)) {
                if (frames < 600) return;
                throw new AssertionError("native MUI2 GUI did not open");
            }
            var panel = ((GuiContainerWrapper) mc.currentScreen).getScreen()
                .getMainPanel();
            java.util.List<IWidget> slots = new java.util.ArrayList<>();
            collectSlots(panel, screenshot == 1, slots);
            if (frames == 20) {
                if (screenshot == 1) {
                    require(
                        ((PhantomItemSlot) slots.get(4)).handleDragAndDrop(new ItemStack(Items.diamond, 32), 0),
                        "real phantom widget accepts NEI item drag");
                } else {
                    require(
                        ((FluidSlot) slots.get(4)).handleDragAndDrop(new ItemStack(Items.water_bucket), 0),
                        "real phantom widget accepts NEI fluid-container drag");
                }
            }
            if (frames < 80) return;
            if (mc.thePlayer.inventory.getItemStack() != null) throw new AssertionError("marking created cursor items");
            if (screenshot == 1) {
                ItemStack sample = ((PhantomItemSlot) slots.get(4)).getSlot()
                    .getStack();
                require(
                    sample != null && sample.getItem() == Items.diamond && sample.stackSize == 1,
                    "item ghost sample syncs back to client");
            } else {
                FluidStack sample = ((FluidSlot) slots.get(4)).getSyncHandler()
                    .getValue();
                if (sample == null && frames < 600) return;
                require(
                    sample != null && sample.getFluid() == FluidRegistry.WATER && sample.amount == 1,
                    "fluid ghost sample syncs back to client: client=" + sample
                        + ", server="
                        + hatch.getFilters()
                            .getFluid(4));
            }
            require(countSlots(panel, screenshot == 1) == 9, "exactly nine ghost filter slots visible");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "me-output-filters-" + screenshot + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            frames = 0;
            screenshot = 0;
        } catch (Throwable failure) {
            failure.printStackTrace();
            finished = true;
            Minecraft.getMinecraft()
                .shutdown();
        }
    }

    private static int countSlots(IWidget widget, boolean items) {
        int count = (items ? widget instanceof PhantomItemSlot : widget instanceof FluidSlot) ? 1 : 0;
        for (IWidget child : widget.getChildren()) count += countSlots(child, items);
        return count;
    }

    private static void collectSlots(IWidget widget, boolean items, java.util.List<IWidget> slots) {
        if (items ? widget instanceof PhantomItemSlot : widget instanceof FluidSlot) slots.add(widget);
        for (IWidget child : widget.getChildren()) collectSlots(child, items, slots);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        System.out.println("ME_OUTPUT_FILTER_QA: " + message);
    }
}
