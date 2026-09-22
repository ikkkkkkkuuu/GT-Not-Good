package com.xyp.gtnotgood.common.advancedio;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/** Actual cable bus, mixed item/fluid target, AE storage, UI and restart tests in a disposable world. */
@Mod(
    modid = "advancedioqa",
    name = "Advanced IO QA",
    version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public final class AdvancedIOClientChecks {

    private Block machineBlock;
    private boolean started;
    private boolean resume;
    private String save;
    private int ticks;
    private int frames;
    private int uiStage;
    private volatile boolean show;
    private PartAdvancedIOBus bus;
    private TestMachine machine;
    private TileDrive drive;
    private ItemStack savedItemCell;
    private ItemStack savedFluidCell;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        machineBlock = new MachineBlock();
        GameRegistry.registerBlock(machineBlock, "mixed_target");
        GameRegistry.registerTileEntity(TestMachine.class, "advancedioqa:target");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.advancedio.qa")) return;
        save = System.getProperty("gtng.advancedio.resume", "");
        resume = !save.isEmpty();
        if (resume && !save.matches("advancedio-qa-[0-9]+")) throw new IllegalArgumentException("Not a QA world");
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            if (!resume) save = "advancedio-qa-" + System.currentTimeMillis();
            mc.launchIntegratedServer(
                save,
                "Advanced IO QA",
                resume ? null : new WorldSettings(25L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || show) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (uiStage > 0) {
            checkUI(player);
            return;
        }
        var world = player.getServerForPlayer();
        ticks++;
        if (ticks == 1) {
            var definitions = AEApi.instance()
                .definitions();
            if (!resume) {
                world.setBlock(
                    4,
                    8,
                    4,
                    definitions.blocks()
                        .multiPart()
                        .maybeBlock()
                        .get());
                var host = (IPartHost) world.getTileEntity(4, 8, 4);
                require(
                    host.addPart(
                        definitions.parts()
                            .cableGlass()
                            .stack(AEColor.Transparent, 1),
                        ForgeDirection.UNKNOWN,
                        player) != null,
                    "cable placed");
                require(
                    host.addPart(GTNGItemList.AdvancedIOBus.get(1), ForgeDirection.SOUTH, player) != null,
                    "custom bus placed on cable");
                world.setBlock(
                    4,
                    8,
                    3,
                    definitions.blocks()
                        .energyCellCreative()
                        .maybeBlock()
                        .get());
                world.setBlock(
                    3,
                    8,
                    4,
                    definitions.blocks()
                        .drive()
                        .maybeBlock()
                        .get());
                world.setBlock(4, 8, 5, machineBlock);
                for (int x = 2; x <= 8; x++) for (int z = 2; z <= 8; z++) {
                    world.setBlock(x, 7, z, net.minecraft.init.Blocks.stone);
                }
                var newDrive = (TileDrive) world.getTileEntity(3, 8, 4);
                newDrive.getInternalInventory()
                    .setInventorySlotContents(
                        0,
                        definitions.items()
                            .cell64k()
                            .maybeStack(1)
                            .get());
                newDrive.getInternalInventory()
                    .setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
            }
            bus = (PartAdvancedIOBus) ((IPartHost) world.getTileEntity(4, 8, 4)).getPart(ForgeDirection.SOUTH);
            machine = (TestMachine) world.getTileEntity(4, 8, 5);
            drive = (TileDrive) world.getTileEntity(3, 8, 4);
            player.playerNetServerHandler.setPlayerLocation(6.5, 8, 6.5, 135, 20);
            player.capabilities.isFlying = true;
            player.inventory.addItemStackToInventory(GTNGItemList.AdvancedIOBus.get(1));
        }
        if (ticks == 80) {
            require(
                bus.getProxy()
                    .isActive(),
                "network powered with channel");
            if (resume) {
                require(
                    bus.filter(0)
                        .getStackSize() == 64
                        && bus.filter(1)
                            .getStackSize() == 16,
                    "item targets survive process restart");
                require(
                    bus.filter(2)
                        .isFluid()
                        && bus.filter(2)
                            .getStackSize() == 1500,
                    "native fluid target survives process restart");
                require(bus.regulate(), "regulation setting survived");
                checkStock(64, 16, 1500);
                new BusTarget(machine, ForgeDirection.NORTH)
                    .extract(AEItemStack.create(new ItemStack(Items.iron_ingot, 11)), false);
                machine.water.drain(777, true);
                work();
                checkStock(64, 16, 1500);
                finish(player);
            } else {
                exercise();
                savedItemCell = drive.getInternalInventory()
                    .getStackInSlot(0);
                savedFluidCell = drive.getInternalInventory()
                    .getStackInSlot(1);
                drive.getInternalInventory()
                    .setInventorySlotContents(0, null);
                drive.getInternalInventory()
                    .setInventorySlotContents(1, null);
                machine.setInventorySlotContents(4, new ItemStack(Items.diamond, 3));
                machine.lava.setFluid(new FluidStack(FluidRegistry.LAVA, 500));
            }
        }
        if (!resume && ticks == 110) {
            work();
            require(
                machine.getStackInSlot(4) != null && machine.getStackInSlot(4).stackSize == 3,
                "no ME storage: items remain in target");
            require(machine.lava.getFluidAmount() == 500, "no ME storage: fluid remains in target");
            drive.getInternalInventory()
                .setInventorySlotContents(0, savedItemCell);
            drive.getInternalInventory()
                .setInventorySlotContents(1, savedFluidCell);
        }
        if (!resume && ticks == 150) {
            work();
            require(
                machine.getStackInSlot(4) == null && machine.lava.getFluidAmount() == 0,
                "recovered after storage restored");
            world.setBlockToAir(4, 8, 3);
            bus.getProxy()
                .getEnergy()
                .extractAEPower(1e9, Actionable.MODULATE, appeng.api.config.PowerMultiplier.CONFIG);
        }
        if (!resume && ticks == 180) {
            require(
                !bus.getProxy()
                    .isActive(),
                "network offline after power removal");
            new BusTarget(machine, ForgeDirection.NORTH)
                .extract(AEItemStack.create(new ItemStack(Items.iron_ingot, 11)), false);
            machine.water.drain(777, true);
            work();
            checkStock(53, 16, 723);
            world.setBlock(
                4,
                8,
                3,
                AEApi.instance()
                    .definitions()
                    .blocks()
                    .energyCellCreative()
                    .maybeBlock()
                    .get());
        }
        if (!resume && ticks == 250) {
            work();
            checkStock(64, 16, 1500);
            finish(player);
        }
        if (ticks > 400) throw new IllegalStateException("ADVANCED_IO_QA timed out");
    }

    private void exercise() throws Exception {
        var source = new MachineSource(bus);
        var network = bus.getProxy()
            .getStorage();
        require(
            network.getItemInventory()
                .injectItems(AEItemStack.create(new ItemStack(Items.iron_ingot, 512)), Actionable.MODULATE, source)
                == null,
            "seed iron");
        require(
            network.getItemInventory()
                .injectItems(AEItemStack.create(new ItemStack(Items.gold_ingot, 512)), Actionable.MODULATE, source)
                == null,
            "seed gold");
        require(
            network.getFluidInventory()
                .injectItems(
                    AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 10000)),
                    Actionable.MODULATE,
                    source)
                == null,
            "seed water");
        bus.setFilter(0, AEItemStack.create(new ItemStack(Items.iron_ingot, 64)));
        bus.setFilter(1, AEItemStack.create(new ItemStack(Items.gold_ingot, 16)));
        bus.setFilter(2, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1500)));
        work();
        checkStock(64, 16, 1500);
        require(
            network.getItemInventory()
                .getStorageList()
                .findPrecise(AEItemStack.create(new ItemStack(Items.iron_ingot)))
                .getStackSize() == 448,
            "iron conserved");
        require(
            network.getFluidInventory()
                .getStorageList()
                .findPrecise(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)))
                .getStackSize() == 8500,
            "fluid conserved");
        work();
        checkStock(64, 16, 1500);
        bus.setRegulate(false);
        machine.setInventorySlotContents(3, new ItemStack(Items.iron_ingot, 20));
        machine.water.fill(new FluidStack(FluidRegistry.WATER, 500), true);
        machine.setInventorySlotContents(4, new ItemStack(Items.diamond, 3));
        machine.lava.setFluid(new FluidStack(FluidRegistry.LAVA, 500));
        work();
        checkStock(84, 16, 2000);
        require(
            machine.getStackInSlot(4) == null && machine.lava.getFluidAmount() == 0,
            "unlisted item/fluid products imported with regulation off");
        bus.setRegulate(true);
        work();
        checkStock(64, 16, 1500);
        machine.extractable = false;
        machine.setInventorySlotContents(4, new ItemStack(Items.diamond, 3));
        machine.lava.setFluid(new FluidStack(FluidRegistry.LAVA, 500));
        work();
        require(
            machine.getStackInSlot(4) != null && machine.lava.getFluidAmount() == 500,
            "sided extraction denial respected");
        checkStock(64, 16, 1500);
        machine.extractable = true;
        work();
        bus.getInventoryByName("upgrades")
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .materials()
                    .cardRedstone()
                    .maybeStack(1)
                    .get());
        bus.getConfigManager()
            .putSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.HIGH_SIGNAL);
        for (int i = 0; i < 9; i++) machine.setInventorySlotContents(i, null);
        machine.water.setFluid(null);
        work();
        checkStock(0, 0, 0);
        bus.getConfigManager()
            .putSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        work();
        checkStock(64, 16, 1500);
        bus.setFilter(3, AEItemStack.create(new ItemStack(Items.gold_ingot, 16)));
        require(bus.filter(1) == null, "duplicate identities have one target");
        bus.setFilter(1, AEItemStack.create(new ItemStack(Items.gold_ingot, 16)));
        var upgrades = bus.getInventoryByName("upgrades");
        upgrades.setInventorySlotContents(
            1,
            AEApi.instance()
                .definitions()
                .materials()
                .cardCapacity()
                .maybeStack(1)
                .get());
        require(bus.availableSlots() == 27, "capacity card unlocks a row");
        bus.setFilter(20, AEItemStack.create(new ItemStack(Items.emerald, 16)));
        upgrades.setInventorySlotContents(1, null);
        machine.setInventorySlotContents(5, new ItemStack(Items.emerald, 2));
        work();
        require(machine.getStackInSlot(5) != null, "disabled filter is not mistaken for a product");
        upgrades.setInventorySlotContents(
            1,
            AEApi.instance()
                .definitions()
                .materials()
                .cardCapacity()
                .maybeStack(1)
                .get());
        bus.setFilter(20, null);
        machine.setInventorySlotContents(5, null);
        upgrades.setInventorySlotContents(1, null);
        upgrades.setInventorySlotContents(
            2,
            AEApi.instance()
                .definitions()
                .materials()
                .cardSpeed()
                .maybeStack(1)
                .get());
        require(bus.calculateAmountToSend() == 64, "8x acceleration throughput");
        var materials = AEApi.instance()
            .definitions()
            .materials();
        var superCard = materials.cardSuperSpeed()
            .maybeStack(1)
            .get();
        var luminalCard = materials.cardSuperluminalSpeed()
            .maybeStack(1)
            .get();
        for (var card : new ItemStack[] { superCard, luminalCard }) {
            require(upgrades.isItemValidForSlot(3, card), "GTNH acceleration card accepted by real upgrade inventory");
            upgrades.setInventorySlotContents(3, card.copy());
            require(
                bus.calculateAmountToSend() == (card == superCard ? 192 : 1048640),
                "native additive speed tiers retain 8x bus multiplier");
            machine.water.drain(777, true);
            new BusTarget(machine, ForgeDirection.NORTH)
                .extract(AEItemStack.create(new ItemStack(Items.iron_ingot, 11)), false);
            work();
            checkStock(64, 16, 1500);
            upgrades.setInventorySlotContents(3, null);
        }
        // Max superluminal cards exercise a fluid budget above Integer.MAX_VALUE, without losing precision.
        upgrades.setInventorySlotContents(2, null);
        for (int slot = 1; slot <= 4; slot++) {
            require(upgrades.isItemValidForSlot(slot, luminalCard), "four superluminal cards accepted");
            upgrades.setInventorySlotContents(slot, luminalCard.copy());
        }
        require(!upgrades.isItemValidForSlot(5, luminalCard), "fifth superluminal card rejected");
        require(bus.calculateAmountToSend() == 536870920, "max-tier operation budget stays positive");
        machine.water.drain(777, true);
        work();
        checkStock(64, 16, 1500);
        for (int slot = 1; slot <= 4; slot++) upgrades.setInventorySlotContents(slot, null);
        upgrades.setInventorySlotContents(
            2,
            materials.cardSpeed()
                .maybeStack(1)
                .get());
        require(StockPolicy.importAmount(1501, 1500, true, true, 8000) == 1, "one mB excess is not rounded away");
        machine.water.fill(new FluidStack(FluidRegistry.WATER, 1), true);
        work();
        checkStock(64, 16, 1500);
        machine.water.drain(10, true);
        machine.rejectCommittedFill = true;
        long fluidBefore = network.getFluidInventory()
            .getStorageList()
            .findPrecise(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)))
            .getStackSize();
        work();
        require(machine.water.getFluidAmount() == 1490, "changed target rejects commit");
        require(
            network.getFluidInventory()
                .getStorageList()
                .findPrecise(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)))
                .getStackSize() == fluidBefore,
            "rejected export rolled back without fluid loss");
        machine.rejectCommittedFill = false;
        work();
        checkStock(64, 16, 1500);
        var tag = new NBTTagCompound();
        bus.writeToNBT(tag);
        var restored = new PartAdvancedIOBus(GTNGItemList.AdvancedIOBus.get(1));
        restored.readFromNBT(tag);
        require(
            restored.filter(2)
                .isFluid()
                && restored.filter(2)
                    .getStackSize() == 1500,
            "native mixed NBT roundtrip");
        restored.setFilter(4, AEItemStack.create(new ItemStack(Items.emerald, 12)));
        restored.uploadSettings(
            appeng.util.SettingsFrom.MEMORY_CARD,
            bus.downloadSettings(appeng.util.SettingsFrom.MEMORY_CARD));
        require(
            restored.filter(4) == null && restored.filter(2)
                .getStackSize() == 1500,
            "memory card replaces stale targets and retains fluid amounts");
        System.out.println("ADVANCED_IO_QA: transaction, filter, side, upgrade and redstone checks passed");
    }

    private void work() {
        for (int i = 0; i < 40; i++) bus.doBusWork();
    }

    private void checkStock(long iron, long gold, long water) {
        var stock = new BusTarget(machine, ForgeDirection.NORTH).stock();
        require(
            BusTarget.count(stock, AEItemStack.create(new ItemStack(Items.iron_ingot))) == iron,
            "iron target " + iron);
        require(
            BusTarget.count(stock, AEItemStack.create(new ItemStack(Items.gold_ingot))) == gold,
            "gold target " + gold);
        require(machine.water.getFluidAmount() == water, "water target " + water);
    }

    private void finish(EntityPlayerMP player) throws Exception {
        bus.getConfigManager()
            .putSetting(Settings.SCHEDULING_MODE, appeng.api.config.SchedulingMode.DEFAULT);
        AdvancedIOGuiFactory.INSTANCE.open(player, bus);
        uiStage = 1;
    }

    private void checkUI(EntityPlayerMP player) throws Exception {
        var container = com.cleanroommc.modularui.screen.ModularContainer.getCurrent(player);
        require(container != null, "server UI container opened");
        var sync = container.getSyncManager()
            .getMainPSM();
        if (uiStage == 1) {
            sync.setCursorItem(new ItemStack(Items.lava_bucket));
            clickSample(sync, 3, false);
            require(
                bus.filter(3) != null && bus.filter(3)
                    .isFluid()
                    && bus.filter(3)
                        .getStackSize() == 1000,
                "bucket click creates native fluid ghost");
            require(
                sync.getCursorItem()
                    .getItem() == Items.lava_bucket && sync.getCursorItem().stackSize == 1,
                "ghost click does not consume bucket");
            clickSample(sync, 62, false);
            require(bus.filter(62) == null, "server rejects disabled-row ghost edit");
            sync.setCursorItem(null);
            var middle = new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
            new com.cleanroommc.modularui.utils.MouseData(cpw.mods.fml.relauncher.Side.SERVER, 2, false, false, false)
                .writeToPacket(middle);
            ((com.cleanroommc.modularui.value.sync.PhantomItemSlotSH) sync.findSyncHandlerNullable("sample", 3))
                .readOnServer(com.cleanroommc.modularui.value.sync.PhantomItemSlotSH.SYNC_CLICK, middle);
            middle.release();
            uiStage = 2;
            return;
        }
        if (uiStage == 2) {
            var amountSync = com.cleanroommc.modularui.screen.ModularContainer.getCurrent(player)
                .getSyncManager()
                .getMainPSM();
            var quantity = (com.cleanroommc.modularui.value.sync.IntSyncValue) amountSync
                .findSyncHandlerNullable("quantity", 0);
            require(quantity != null, "middle click opens amount sub-screen");
            var display = (com.cleanroommc.modularui.value.sync.DoubleSyncValue) amountSync
                .findSyncHandlerNullable("displayQuantity", 0);
            display.setDoubleValue(0.75, true, true);
            require(
                bus.filter(3)
                    .getStackSize() == 1000,
                "draft does not change stock before Set");
            clickButton(amountSync, "confirm");
            require(
                bus.filter(3)
                    .getStackSize() == 750,
                "Set commits exact fluid quantity");
            uiStage = 3;
            return;
        }
        sync = com.cleanroommc.modularui.screen.ModularContainer.getCurrent(player)
            .getSyncManager()
            .getMainPSM();
        clickSample(sync, 3, true);
        require(bus.filter(3) == null, "shift click clears ghost");
        clickButton(sync, "schedulingButton");
        require(
            bus.getConfigManager()
                .getSetting(appeng.api.config.Settings.SCHEDULING_MODE) == appeng.api.config.SchedulingMode.ROUNDROBIN,
            "toolbar changes real scheduling mode");
        System.out
            .println("ADVANCED_IO_QA: faithful main screen, middle-click submenu, draft and Set interactions passed");
        show = true;
        Files.write(new File("advancedio-qa-resume.txt").toPath(), save.getBytes(StandardCharsets.UTF_8));
    }

    private static void clickSample(com.cleanroommc.modularui.value.sync.PanelSyncManager sync, int index,
        boolean shift) throws Exception {
        var handler = (com.cleanroommc.modularui.value.sync.PhantomItemSlotSH) sync
            .findSyncHandlerNullable("sample", index);
        var buffer = new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
        try {
            new com.cleanroommc.modularui.utils.MouseData(cpw.mods.fml.relauncher.Side.SERVER, 0, shift, false, false)
                .writeToPacket(buffer);
            handler.readOnServer(com.cleanroommc.modularui.value.sync.PhantomItemSlotSH.SYNC_CLICK, buffer);
        } finally {
            buffer.release();
        }
    }

    private static void clickButton(com.cleanroommc.modularui.value.sync.PanelSyncManager sync, String name)
        throws Exception {
        var buffer = new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
        try {
            new com.cleanroommc.modularui.utils.MouseData(cpw.mods.fml.relauncher.Side.SERVER, 0, false, false, false)
                .writeToPacket(buffer);
            ((com.cleanroommc.modularui.value.sync.InteractionSyncHandler) sync.findSyncHandlerNullable(name, 0))
                .readOnServer(1, buffer);
        } finally {
            buffer.release();
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        var mc = Minecraft.getMinecraft();
        if (!show || event.phase != TickEvent.Phase.END) return;
        if (++frames == 80) {
            require(
                mc.currentScreen instanceof com.cleanroommc.modularui.screen.GuiContainerWrapper,
                "real bus UI opened");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                resume ? "advancedio-resume.png" : "advancedio-gui.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
        }
        if (frames == 100) {
            var sync = com.cleanroommc.modularui.screen.ModularContainer.getCurrent(mc.thePlayer)
                .getSyncManager()
                .getMainPSM();
            ((com.cleanroommc.modularui.value.sync.PhantomItemSlotSH) sync.findSyncHandlerNullable("sample", 0))
                .syncToServer(
                    com.cleanroommc.modularui.value.sync.PhantomItemSlotSH.SYNC_CLICK,
                    buffer -> new com.cleanroommc.modularui.utils.MouseData(
                        cpw.mods.fml.relauncher.Side.CLIENT,
                        2,
                        false,
                        false,
                        false).writeToPacket(buffer));
        }
        if (frames == 180) {
            var sync = com.cleanroommc.modularui.screen.ModularContainer.getCurrent(mc.thePlayer)
                .getSyncManager()
                .getMainPSM();
            require(sync.findSyncHandlerNullable("quantity", 0) != null, "client receives amount sub-screen");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "advancedio-amount.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
        }
        if (frames == 200) {
            var sync = com.cleanroommc.modularui.screen.ModularContainer.getCurrent(mc.thePlayer)
                .getSyncManager()
                .getMainPSM();
            ((com.cleanroommc.modularui.value.sync.InteractionSyncHandler) sync.findSyncHandlerNullable("back", 0))
                .onMousePressed(0);
        }
        if (frames == 240) {
            var sync = com.cleanroommc.modularui.screen.ModularContainer.getCurrent(mc.thePlayer)
                .getSyncManager()
                .getMainPSM();
            ((com.cleanroommc.modularui.value.sync.PhantomItemSlotSH) sync.findSyncHandlerNullable("sample", 2))
                .syncToServer(
                    com.cleanroommc.modularui.value.sync.PhantomItemSlotSH.SYNC_CLICK,
                    buffer -> new com.cleanroommc.modularui.utils.MouseData(
                        cpw.mods.fml.relauncher.Side.CLIENT,
                        2,
                        false,
                        false,
                        false).writeToPacket(buffer));
        }
        if (frames == 320) {
            var sync = com.cleanroommc.modularui.screen.ModularContainer.getCurrent(mc.thePlayer)
                .getSyncManager()
                .getMainPSM();
            require(
                ((com.cleanroommc.modularui.value.sync.IntSyncValue) sync.findSyncHandlerNullable("amountUnit", 0))
                    .getIntValue() == 1000,
                "client receives bucket unit for fluid amount editor");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "advancedio-fluid-amount.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
        }
        if (frames == 340) mc.thePlayer.closeScreen();
        if (frames == 380) {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "advancedio-world.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            Files.write(
                new File("advancedio-qa-result.txt").toPath(),
                (resume ? "PASS" : "SAVED").getBytes(StandardCharsets.UTF_8));
            mc.shutdown();
        }
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("ADVANCED_IO_QA: " + label);
    }

    /** Disposable, ordinary sided inventory/tank fixture; only its north face accepts automation. */
    public static final class TestMachine extends TileEntity implements ISidedInventory, IFluidHandler {

        private final InventoryBasic inventory = new InventoryBasic("QA", false, 9);
        private final FluidTank water = new FluidTank(16000);
        private final FluidTank lava = new FluidTank(16000);
        private boolean extractable = true;
        private boolean rejectCommittedFill;

        public int getSizeInventory() {
            return 9;
        }

        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        public ItemStack decrStackSize(int slot, int amount) {
            markDirty();
            return inventory.decrStackSize(slot, amount);
        }

        public ItemStack getStackInSlotOnClosing(int slot) {
            return inventory.getStackInSlotOnClosing(slot);
        }

        public void setInventorySlotContents(int slot, ItemStack stack) {
            inventory.setInventorySlotContents(slot, stack);
            markDirty();
        }

        public String getInventoryName() {
            return "QA";
        }

        public boolean hasCustomInventoryName() {
            return false;
        }

        public int getInventoryStackLimit() {
            return 64;
        }

        public boolean isUseableByPlayer(EntityPlayer player) {
            return true;
        }

        public void openInventory() {}

        public void closeInventory() {}

        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return slot < 4;
        }

        public int[] getAccessibleSlotsFromSide(int side) {
            return side == 2 ? new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 } : new int[0];
        }

        public boolean canInsertItem(int slot, ItemStack stack, int side) {
            return side == 2 && slot < 4;
        }

        public boolean canExtractItem(int slot, ItemStack stack, int side) {
            return side == 2 && extractable;
        }

        public int fill(ForgeDirection side, FluidStack fluid, boolean commit) {
            if (side != ForgeDirection.NORTH || fluid == null || fluid.getFluid() != FluidRegistry.WATER) return 0;
            if (commit && rejectCommittedFill) return 0;
            if (commit) markDirty();
            return water.fill(fluid, commit);
        }

        public FluidStack drain(ForgeDirection side, FluidStack fluid, boolean commit) {
            if (side != ForgeDirection.NORTH || !extractable || fluid == null) return null;
            if (commit) markDirty();
            FluidTank tank = fluid.getFluid() == FluidRegistry.WATER ? water : lava;
            return tank.getFluid() != null && tank.getFluid()
                .isFluidEqual(fluid) ? tank.drain(fluid.amount, commit) : null;
        }

        public FluidStack drain(ForgeDirection side, int amount, boolean commit) {
            return drain(side, new FluidStack(FluidRegistry.WATER, amount), commit);
        }

        public boolean canFill(ForgeDirection side, Fluid fluid) {
            return side == ForgeDirection.NORTH && fluid == FluidRegistry.WATER;
        }

        public boolean canDrain(ForgeDirection side, Fluid fluid) {
            return side == ForgeDirection.NORTH && extractable;
        }

        public FluidTankInfo[] getTankInfo(ForgeDirection side) {
            return side == ForgeDirection.NORTH ? new FluidTankInfo[] { water.getInfo(), lava.getInfo() }
                : new FluidTankInfo[0];
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            for (int i = 0; i < 9; i++) if (getStackInSlot(i) != null) {
                NBTTagCompound entry = new NBTTagCompound();
                getStackInSlot(i).writeToNBT(entry);
                tag.setTag("item" + i, entry);
            }
            tag.setTag("water", water.writeToNBT(new NBTTagCompound()));
            tag.setTag("lava", lava.writeToNBT(new NBTTagCompound()));
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            for (int i = 0; i < 9; i++)
                inventory.setInventorySlotContents(i, ItemStack.loadItemStackFromNBT(tag.getCompoundTag("item" + i)));
            water.readFromNBT(tag.getCompoundTag("water"));
            lava.readFromNBT(tag.getCompoundTag("lava"));
        }
    }

    /** Test-only block containing the dual resource fixture. */
    private static final class MachineBlock extends BlockContainer {

        MachineBlock() {
            super(Material.iron);
            setBlockName("advancedioqa_target");
            setBlockTextureName("iron_block");
        }

        public TileEntity createNewTileEntity(World world, int meta) {
            return new TestMachine();
        }
    }
}
