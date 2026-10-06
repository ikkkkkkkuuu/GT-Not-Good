package com.xyp.gtnotgood.common.mestock;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
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

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.common.mebridge.TileMEBridgeBase;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.tile.storage.TileDrive;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/** Real native AE network plus shared-queue scale checks in a disposable, hidden test client. */
@Mod(modid = "mestockqa", name = "ME stock QA", version = "1", dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class MEStockClientChecks {

    private boolean started, finished, resumed;
    private int ticks, stage, frames;
    private volatile int screen;
    private volatile boolean captured;
    private String save;
    private Block targetBlock;
    private Block providerBlock;
    private PartThresholdExportBus bus;
    private PartThresholdLevelEmitter emitter;
    private PartRequesterTerminal terminal;
    private TileMERequester requester;
    private TileMERequester secondRequester;
    private TestTarget target;
    private StockGridCache cache;
    private long plansAtComplete, dormantLookups, emitterEvaluations;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        targetBlock = new TargetBlock();
        GameRegistry.registerBlock(targetBlock, "stock_target");
        GameRegistry.registerTileEntity(TestTarget.class, "mestockqa:target");
        providerBlock = new ProviderBlock();
        GameRegistry.registerBlock(providerBlock, "stock_provider");
        GameRegistry.registerTileEntity(TestProvider.class, "mestockqa:provider");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        save = System.getProperty("gtng.meStock.resume", "");
        resumed = !save.isEmpty();
        if (resumed && !save.matches("me-stock-qa-[0-9]+")) throw new IllegalArgumentException("Not a QA world");
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            if (!resumed) save = "me-stock-qa-" + System.currentTimeMillis();
            mc.launchIntegratedServer(
                save,
                "ME stock QA",
                resumed ? null : new WorldSettings(62L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
            step(player);
        } catch (Throwable error) {
            error.printStackTrace();
            finish("FAIL: " + error);
        }
    }

    private void step(EntityPlayerMP player) throws Exception {
        ticks++;
        if (ticks > 1200) throw new AssertionError(
            "Timed out at stage " + stage + ", requester status=" + (requester == null ? -1 : requester.status(0)));
        World world = player.worldObj;
        if (ticks == 1) {
            require(
                StockNumbers.format(2500)
                    .equals("2500") && StockNumbers.parse("2500") == 2500,
                "GTNH fluid quantities use native liters without bucket scaling");
            require(
                StockNumbers.parse(Long.toString(Long.MAX_VALUE)) == Long.MAX_VALUE,
                "quantity parser preserves exact long values");
            boolean rejectedFraction = false;
            try {
                StockNumbers.parse("0.5");
            } catch (ArithmeticException expected) {
                rejectedFraction = true;
            }
            require(rejectedFraction, "native quantities reject fractional liters/items");
            if (!resumed) setup(world, player);
            bus = (PartThresholdExportBus) ((IPartHost) world.getTileEntity(4, 8, 4)).getPart(ForgeDirection.SOUTH);
            emitter = (PartThresholdLevelEmitter) ((IPartHost) world.getTileEntity(4, 8, 4))
                .getPart(ForgeDirection.EAST);
            terminal = (PartRequesterTerminal) ((IPartHost) world.getTileEntity(4, 8, 4)).getPart(ForgeDirection.DOWN);
            requester = (TileMERequester) world.getTileEntity(4, 9, 4);
            secondRequester = (TileMERequester) world.getTileEntity(5, 9, 4);
            target = (TestTarget) world.getTileEntity(4, 8, 5);
            player.playerNetServerHandler.setPlayerLocation(6.5, 8, 6.5, 135, 20);
        }
        if (ticks < 120 || !bus.getProxy()
            .isActive()
            || !requester.getProxy()
                .isActive())
            return;
        if (cache == null) cache = bus.getProxy()
            .getGrid()
            .getCache(StockGridCache.class);
        if (resumed) {
            if (stage == 0) {
                require(
                    bus.stockConfig()
                        .amount(0) == 100
                        && bus.stockConfig()
                            .amount(1) == 5000,
                    "threshold settings survive restart");
                require(
                    emitter.stockConfig()
                        .amount(0) == 20 && emitter.upper() == 80,
                    "emitter thresholds survive restart");
                require(
                    requester.stockConfig()
                        .amount(0) == 8 && requester.name()
                            .equals("电路备货测试"),
                    "requester settings survive restart");
                require(
                    target.itemCount() == 156 && target.tank.getFluidAmount() == 7000,
                    "item/fluid transfers persist");
                stage = 200;
                screen = 5;
                captured = false;
                frames = 0;
                StockGuiFactory.instance.openAmount(player, bus, 1);
            } else if (stage == 200 && captured) {
                require(
                    bus.stockConfig()
                        .amount(1) == Long.MAX_VALUE,
                    "selected fluid row accepts long quantity from GUI");
                require(
                    bus.stockConfig()
                        .amount(0) == 100,
                    "selected-row editing preserves the other reserve");
                bus.stockConfig()
                    .setAmount(1, 5000);
                captured = false;
                frames = 0;
                screen = 6;
                stage = 201;
                StockGuiFactory.instance.open(player, terminal);
            } else if (stage == 201 && captured) {
                require(
                    requester.stockConfig()
                        .amount(0) == 9
                        && requester.stockConfig()
                            .batch(0) == 2,
                    "terminal submits both inline request fields together");
                require(
                    requester.stockConfig()
                        .amount(1) == 2500
                        && requester.stockConfig()
                            .batch(1) == 0,
                    "same-tick search rejects submission for the previous row identity");
                requester.stockConfig()
                    .setAmounts(0, 8, 0);
                StockGuiFactory.Data remote = new StockGuiFactory.Data(
                    player,
                    4,
                    8,
                    4,
                    ForgeDirection.DOWN.ordinal(),
                    new int[] { 0, 5, 9, 4 });
                require(StockGuiFactory.instance.canInteractWith(player, remote), "same-grid terminal access is valid");
                player.setPosition(30, 8, 30);
                require(!StockGuiFactory.instance.canInteractWith(player, remote), "terminal distance is rechecked");
                player.setPosition(6.5, 8, 6.5);
                world.setBlockToAir(5, 9, 4);
                require(
                    !StockGuiFactory.instance.canInteractWith(player, remote),
                    "removed remote requester invalidates existing access");
                world.setBlock(
                    5,
                    9,
                    4,
                    Block.getBlockFromItem(
                        GTNGItemList.MERequester.get(1)
                            .getItem()));
                require(
                    !StockGuiFactory.instance.canInteractWith(player, remote),
                    "replacement at the same coordinate cannot reuse existing access");
                finish("PASS");
            }
            return;
        }
        var storage = bus.getProxy()
            .getStorage();
        var source = new MachineSource(bus);
        var iron = AEItemStack.create(new ItemStack(Items.iron_ingot));
        var water = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1));
        var ironBlock = AEItemStack.create(new ItemStack(Blocks.gold_block));
        if (ticks % 100 == 0) System.out.println(
            "ME_STOCK_QA progress stage=" + stage
                + " status="
                + requester.status(0)
                + " fluidStatus="
                + requester.status(1)
                + " plans="
                + cache.plansStarted
                + " calculating="
                + cache.planningCount()
                + " gold="
                + StockResources.count(storage, ironBlock)
                + " iron="
                + StockResources.count(storage, iron)
                + " water="
                + StockResources.count(storage, water)
                + " tank="
                + target.tank.getFluidAmount());
        if (stage == 0) {
            require(
                PartThresholdExportBus.exportLimit(true, Long.MAX_VALUE, Long.MAX_VALUE - 5) == 5,
                "large reserve subtraction");
            require(
                PartThresholdExportBus.exportLimit(false, 100, 100) == 100,
                "below-mode boundary includes equality");
            require(
                StockResources.missing(Long.MAX_VALUE, 10, Long.MAX_VALUE) == 0,
                "in-flight addition cannot overflow");
            bus.stockConfig()
                .setKey(0, iron);
            bus.stockConfig()
                .setAmount(0, 100);
            bus.stockConfig()
                .setKey(1, water);
            bus.stockConfig()
                .setAmount(1, 5000);
            bus.getInventoryByName("upgrades")
                .setInventorySlotContents(
                    0,
                    AEApi.instance()
                        .definitions()
                        .materials()
                        .cardSuperluminalSpeed()
                        .maybeStack(1)
                        .get());
            emitter.stockConfig()
                .setKey(0, ironBlock);
            emitter.stockConfig()
                .setAmount(0, 20);
            emitter.setUpper(80);
            requester.stockConfig()
                .setKey(0, ironBlock);
            requester.stockConfig()
                .setAmount(0, 8);
            secondRequester.stockConfig()
                .setKey(0, ironBlock);
            secondRequester.stockConfig()
                .setAmount(0, 8);
            requester.setName("电路备货测试");
            storage.getItemInventory()
                .injectItems(
                    iron.copy()
                        .setStackSize(256),
                    Actionable.MODULATE,
                    source);
            storage.getFluidInventory()
                .injectItems(
                    water.copy()
                        .setStackSize(12000),
                    Actionable.MODULATE,
                    source);
            storage.getItemInventory()
                .injectItems(AEItemStack.create(new ItemStack(Items.gold_ingot, 256)), Actionable.MODULATE, source);
            stage = 1;
        } else
            if (stage == 1 && StockResources.count(storage, ironBlock) == 8 && target.tank.getFluidAmount() == 7000) {
                require(StockResources.count(storage, iron) == 100, "threshold preserves network item reserve");
                require(StockResources.count(storage, water) == 5000, "threshold preserves network fluid reserve");
                require(target.itemCount() == 156, "surplus item delivery conserves quantity");
                require(emitter.output(), "below lower threshold emits");
                require(
                    cache.plansStarted == 1,
                    "two requesters share calculation reservations and do not double-order");
                plansAtComplete = cache.plansStarted;
                storage.getItemInventory()
                    .injectItems(
                        ironBlock.copy()
                            .setStackSize(92),
                        Actionable.MODULATE,
                        source);
                stage = 2;
            } else if (stage == 2 && !emitter.output()) {
                storage.getItemInventory()
                    .extractItems(
                        ironBlock.copy()
                            .setStackSize(50),
                        Actionable.MODULATE,
                        source);
                stage = 3;
            } else if (stage == 3 && ticks % 20 == 0) {
                require(!emitter.output(), "falling inventory holds off between limits");
                storage.getItemInventory()
                    .extractItems(
                        ironBlock.copy()
                            .setStackSize(40),
                        Actionable.MODULATE,
                        source);
                stage = 4;
            } else if (stage == 4 && emitter.output()) {
                storage.getItemInventory()
                    .injectItems(
                        ironBlock.copy()
                            .setStackSize(40),
                        Actionable.MODULATE,
                        source);
                stage = 5;
            } else if (stage == 5 && ticks % 20 == 0) {
                require(emitter.output(), "rising inventory holds on between limits");
                require(cache.plansStarted == plansAtComplete, "stocked requester does not submit duplicate orders");
                NBTTagCompound tag = new NBTTagCompound();
                requester.writeSettings(tag);
                TileMERequester copy = new TileMERequester();
                copy.readSettings(tag);
                require(
                    copy.stockConfig()
                        .amount(0) == 8
                        && copy.stockConfig()
                            .batch(0) == 0,
                    "long targets and auto batches serialize");
                largeQueue(world);
                // Move iron blocks above upper then disable requester; capture a stable state for restart.
                storage.getItemInventory()
                    .injectItems(
                        ironBlock.copy()
                            .setStackSize(50),
                        Actionable.MODULATE,
                        source);
                requester.stockConfig()
                    .setEnabled(0, false);
                secondRequester.stockConfig()
                    .setEnabled(0, false);
                requester.stockConfig()
                    .setKey(1, AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1)));
                requester.stockConfig()
                    .setAmount(1, 2500);
                stage = 50;
            } else if (stage == 50
                && StockResources.count(storage, AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1))) >= 2500) {
                    TestProvider provider = (TestProvider) world.getTileEntity(5, 9, 5);
                    require(
                        provider.nativeFluidInputs == 3000,
                        "fluid requester supplies native Ultimate-pattern inputs");
                    require(
                        cache.plansStarted == plansAtComplete + 1,
                        "fluid requester submits exactly one stock order");
                    require(
                        StockResources.count(storage, water) == 2000,
                        "fluid processing consumes exactly three native batches");
                    requester.stockConfig()
                        .setEnabled(1, false);
                    dormantLookups = bus.stockLookups;
                    emitterEvaluations = emitter.evaluations;
                    screen = 1;
                    captured = false;
                    frames = 0;
                    StockGuiFactory.instance.open(player, bus);
                    stage = 6;
                } else if (stage >= 6 && stage <= 9 && captured) {
                    captured = false;
                    frames = 0;
                    if (stage == 6) {
                        screen = 2;
                        StockGuiFactory.instance.open(player, emitter);
                    }
                    if (stage == 7) {
                        screen = 3;
                        StockGuiFactory.instance.open(player, requester);
                    }
                    if (stage == 8) {
                        screen = 4;
                        StockGuiFactory.instance.open(player, terminal);
                    }
                    if (stage == 9) {
                        screen = 0;
                        require(bus.stockLookups - dormantLookups < 12, "idle threshold bus sleeps");
                        require(emitter.evaluations - emitterEvaluations < 6, "idle emitter sleeps");
                        require(
                            cache.planningCount() <= 2 && cache.cpuScans < ticks / 20 + 3,
                            "shared CPU and calculation budgets");
                        Files.write(new File("resume.txt").toPath(), save.getBytes(StandardCharsets.UTF_8));
                        System.out.println(
                            "ME_STOCK_QA METRICS ticks=" + ticks
                                + " cpuScans="
                                + cache.cpuScans
                                + " visits="
                                + cache.deviceVisits
                                + " plans="
                                + cache.plansStarted);
                        finish("SAVED");
                    }
                    stage++;
                }
    }

    private void setup(World world, EntityPlayerMP player) {
        var d = AEApi.instance()
            .definitions();
        world.setBlock(
            4,
            8,
            4,
            d.blocks()
                .multiPart()
                .maybeBlock()
                .get());
        IPartHost host = (IPartHost) world.getTileEntity(4, 8, 4);
        host.addPart(
            d.parts()
                .cableGlass()
                .stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN,
            player);
        host.addPart(GTNGItemList.ThresholdExportBus.get(1), ForgeDirection.SOUTH, player);
        host.addPart(GTNGItemList.ThresholdLevelEmitter.get(1), ForgeDirection.EAST, player);
        host.addPart(GTNGItemList.MERequesterTerminal.get(1), ForgeDirection.DOWN, player);
        world.setBlock(
            4,
            8,
            3,
            d.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        world.setBlock(
            3,
            8,
            3,
            d.blocks()
                .controller()
                .maybeBlock()
                .get());
        world.setBlock(
            3,
            8,
            4,
            d.blocks()
                .drive()
                .maybeBlock()
                .get());
        world.setBlock(
            2,
            8,
            4,
            d.blocks()
                .craftingStorage64k()
                .maybeBlock()
                .get());
        world.setBlock(
            4,
            9,
            4,
            Block.getBlockFromItem(
                GTNGItemList.MERequester.get(1)
                    .getItem()));
        ((TileMERequester) world.getTileEntity(4, 9, 4)).setOwnerName(player.getCommandSenderName());
        world.setBlock(
            5,
            9,
            4,
            Block.getBlockFromItem(
                GTNGItemList.MERequester.get(1)
                    .getItem()));
        ((TileMERequester) world.getTileEntity(5, 9, 4)).setOwnerName(player.getCommandSenderName());
        world.setBlock(4, 8, 5, targetBlock);
        world.setBlock(5, 9, 5, providerBlock);
        ((TestProvider) world.getTileEntity(5, 9, 5)).setOwnerName(player.getCommandSenderName());
        TileDrive drive = (TileDrive) world.getTileEntity(3, 8, 4);
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                d.items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
        drive.getInternalInventory()
            .setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
        for (int x = 1; x < 9; x++) for (int z = 1; z < 9; z++) world.setBlock(x, 7, z, Blocks.stone);
    }

    private static ItemStack pattern() {
        ItemStack result = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        NBTTagCompound input = new NBTTagCompound();
        Platform.writeStackNBT(AEItemStack.create(new ItemStack(Items.gold_ingot, 9)), input);
        in.appendTag(input);
        for (int i = 1; i < 16; i++) in.appendTag(new NBTTagCompound());
        NBTTagCompound output = new NBTTagCompound();
        Platform.writeStackNBT(AEItemStack.create(new ItemStack(Blocks.gold_block)), output);
        out.appendTag(output);
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        result.setTagCompound(tag);
        return result;
    }

    private static ItemStack fluidPattern() {
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList(), outputs = new NBTTagList();
        NBTTagCompound input = new NBTTagCompound(), output = new NBTTagCompound();
        Platform.writeStackNBT(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1000)), input);
        Platform.writeStackNBT(AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1000)), output);
        inputs.appendTag(input);
        for (int i = 1; i < 16; i++) inputs.appendTag(new NBTTagCompound());
        outputs.appendTag(output);
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private void largeQueue(World world) {
        StockGridCache scale = new StockGridCache(
            bus.getActionableNode()
                .getGrid());
        TileMERequester[] machines = new TileMERequester[1024];
        for (int i = 0; i < machines.length; i++) {
            machines[i] = new TileMERequester();
            machines[i].setWorldObj(world);
            scale.addNode(null, machines[i]);
            for (int n = 0; n < 20; n++) scale.schedule(machines[i], 0);
        }
        require(scale.queueSize() == machines.length, "1024 requesters coalesce 20480 notifications");
        for (int i = 0; i < 128; i++) {
            long before = scale.deviceVisits;
            scale.onUpdateTick();
            require(scale.deviceVisits - before <= StockGridCache.devicesPerTick, "large queue per-tick budget " + i);
        }
        require(scale.deviceVisits == 1024, "all large-queue devices receive service fairly");
        for (TileMERequester tile : machines) scale.removeNode(null, tile);
        require(
            scale.queueSize() == 0 && scale.listing()
                .isEmpty(),
            "network removal clears queue and directory");
        System.out
            .println("ME_STOCK_QA SCALE 1024 logical devices; 20480 notifications; visits/tick <= 8; no world scan");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || screen == 0 || captured || finished || ++frames < 40) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (frames == 40) require(mc.currentScreen instanceof GuiContainerWrapper, "GUI opens " + screen);
            if (screen == 5) {
                var sync = ((GuiContainerWrapper) mc.currentScreen).getScreen()
                    .getSyncManager()
                    .getMainPSM();
                if (frames == 40) {
                    ScreenShotHelper.saveScreenshot(
                        mc.mcDataDir,
                        "me-stock-7.png",
                        mc.displayWidth,
                        mc.displayHeight,
                        mc.getFramebuffer());
                    for (var widget : ((GuiContainerWrapper) mc.currentScreen).getScreen()
                        .getMainPanel()
                        .getChildren())
                        if (widget instanceof ButtonWidget<?>button
                            && button.getSyncHandler() == sync.findSyncHandlerNullable("step00", 0))
                            button.onMousePressed(0);
                }
                if (frames == 60) require(
                    ((StringSyncValue) sync.findSyncHandlerNullable("amount", 0)).getValue()
                        .equals("5001"),
                    "fluid plus-one button adds one native liter");
                if (frames == 80) editField(mc, "amount", StockNumbers.format(Long.MAX_VALUE), true);
                if (frames < 120) return;
            } else if (screen == 6) {
                var sync = ((GuiContainerWrapper) mc.currentScreen).getScreen()
                    .getSyncManager()
                    .getMainPSM();
                if (frames == 40) ((StringSyncValue) sync.findSyncHandlerNullable("search", 0)).setValue("电路");
                if (frames == 80) {
                    editField(mc, "batch1", "2", false);
                    editField(mc, "target1", "9", true);
                }
                if (frames == 100) {
                    String staleIdentity = ((StringSyncValue) sync.findSyncHandlerNullable("identity", 1)).getValue();
                    ((StringSyncValue) sync.findSyncHandlerNullable("search", 0)).setValue("");
                    sync.callSyncedAction("submit1Action", buffer -> {
                        NetworkUtils.writeStringSafe(buffer, staleIdentity);
                        NetworkUtils.writeStringSafe(buffer, "111");
                        NetworkUtils.writeStringSafe(buffer, "3");
                    });
                }
                if (frames == 140) ((StringSyncValue) sync.findSyncHandlerNullable("search", 0)).setValue("没有这个请求器");
                if (frames == 180) {
                    require(
                        ((IntSyncValue) sync.findSyncHandlerNullable("kind", 0)).getIntValue() == 0,
                        "terminal search hides nonmatching groups");
                    for (var widget : ((GuiContainerWrapper) mc.currentScreen).getScreen()
                        .getMainPanel()
                        .getChildren())
                        if (widget instanceof TextFieldWidget field
                            && field.getStringValue() == sync.findSyncHandlerNullable("search", 0))
                            field.onMousePressed(1);
                }
                if (frames == 220) {
                    require(
                        ((StringSyncValue) sync.findSyncHandlerNullable("search", 0)).getValue()
                            .isEmpty(),
                        "right-click clears terminal search");
                    require(
                        ((IntSyncValue) sync.findSyncHandlerNullable("rows", 0)).getIntValue() == 9,
                        "adaptive terminal rows survive initial server synchronization");
                    require(
                        ((IntSyncValue) sync.findSyncHandlerNullable("kind", 6)).getIntValue() == 1,
                        "resized list renders the second requester group");
                }
                if (frames < 240) return;
            }
            ScreenShotHelper.saveScreenshot(
                mc.mcDataDir,
                "me-stock-" + screen + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            captured = true;
        } catch (Throwable error) {
            error.printStackTrace();
            finish("FAIL GUI: " + error);
        }
    }

    private static void editField(Minecraft mc, String id, String text, boolean submit) {
        var gui = ((GuiContainerWrapper) mc.currentScreen).getScreen();
        var value = gui.getSyncManager()
            .getMainPSM()
            .findSyncHandlerNullable(id, 0);
        for (var widget : gui.getMainPanel()
            .getChildren()) {
            if (widget instanceof TextFieldWidget field && field.getStringValue() == value) {
                gui.getContext()
                    .focus(field);
                field.setText(text);
                if (submit) field.onKeyPressed('\r', Keyboard.KEY_RETURN);
                else gui.getContext()
                    .removeFocus();
                return;
            }
        }
        throw new AssertionError("Missing field " + id);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        if (!message.startsWith("large queue per-tick")) System.out.println("ME_STOCK_QA " + message);
    }

    private void finish(String value) {
        finished = true;
        try {
            Files.write(new File("result.txt").toPath(), value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            error.printStackTrace();
        }
        Minecraft.getMinecraft()
            .shutdown();
    }

    public static final class TargetBlock extends BlockContainer {

        public TargetBlock() {
            super(Material.iron);
            setBlockName("stock_target");
            setBlockTextureName("minecraft:stone");
        }

        @Override
        public TileEntity createNewTileEntity(World world, int metadata) {
            return new TestTarget();
        }
    }

    public static final class ProviderBlock extends BlockContainer {

        public ProviderBlock() {
            super(Material.iron);
            setBlockName("stock_provider");
            setBlockTextureName("minecraft:stone");
        }

        @Override
        public TileEntity createNewTileEntity(World world, int metadata) {
            return new TestProvider();
        }
    }

    public static final class TestProvider extends TileMEBridgeBase implements ICraftingProvider {

        long nativeFluidInputs;
        private int waitingTicks;
        private long outputAmount;
        private long itemOutput;

        @Override
        protected ItemStack getVisualRepresentation() {
            return AEApi.instance()
                .definitions()
                .blocks()
                .iface()
                .maybeStack(1)
                .get();
        }

        @Override
        public void provideCrafting(ICraftingProviderHelper helper) {
            ItemStack pattern = fluidPattern();
            helper.addCraftingOption(
                this,
                ((ICraftingPatternItem) pattern.getItem()).getPatternForItem(pattern, worldObj));
            ItemStack itemPattern = pattern();
            helper.addCraftingOption(
                this,
                ((ICraftingPatternItem) itemPattern.getItem()).getPatternForItem(itemPattern, worldObj));
        }

        @Override
        public boolean pushPattern(ICraftingPatternDetails details, InventoryCrafting table) {
            require(table instanceof MEInventoryCrafting, "fluid CPU supplies native inventory");
            long amount = 0;
            for (int slot = 0; slot < table.getSizeInventory(); slot++) {
                IAEStack<?> stack = ((MEInventoryCrafting) table).getAEStackInSlot(slot);
                if (stack == null) continue;
                require(
                    stack.isFluid() == details.getCondensedAEOutputs()[0].isFluid(),
                    "processing input retains its native resource type");
                amount += stack.getStackSize();
            }
            if (details.getCondensedAEOutputs()[0].isFluid()) {
                require(amount > 0 && amount % 1000 == 0, "processing amount matches native fluid pattern");
                nativeFluidInputs += amount;
                outputAmount += amount;
            } else {
                require(amount > 0 && amount % 9 == 0, "processing amount matches native item pattern");
                itemOutput += amount / 9;
            }
            waitingTicks = 5;
            return true;
        }

        @Override
        public boolean isBusy() {
            return outputAmount > 0 || itemOutput > 0;
        }

        @Override
        public void updateEntity() {
            super.updateEntity();
            if (!isServerSide() || !isBusy() || --waitingTicks > 0) return;
            try {
                if (outputAmount > 0) {
                    var remainder = getProxy().getStorage()
                        .getFluidInventory()
                        .injectItems(
                            AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, (int) outputAmount)),
                            Actionable.MODULATE,
                            new MachineSource(this));
                    outputAmount = remainder == null ? 0 : remainder.getStackSize();
                }
                if (itemOutput > 0) {
                    var remainder = getProxy().getStorage()
                        .getItemInventory()
                        .injectItems(
                            AEItemStack.create(new ItemStack(Blocks.gold_block))
                                .setStackSize(itemOutput),
                            Actionable.MODULATE,
                            new MachineSource(this));
                    itemOutput = remainder == null ? 0 : remainder.getStackSize();
                }
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }
    }

    public static final class TestTarget extends TileEntity implements IInventory, IFluidHandler {

        final InventoryBasic items = new InventoryBasic("stock target", false, 9) {

            @Override
            public int getInventoryStackLimit() {
                return 1000000;
            }
        };
        final FluidTank tank = new FluidTank(1000000);

        int itemCount() {
            int result = 0;
            for (int i = 0; i < items.getSizeInventory(); i++)
                if (items.getStackInSlot(i) != null) result += items.getStackInSlot(i).stackSize;
            return result;
        }

        public int getSizeInventory() {
            return items.getSizeInventory();
        }

        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        public ItemStack decrStackSize(int slot, int count) {
            return items.decrStackSize(slot, count);
        }

        public ItemStack getStackInSlotOnClosing(int slot) {
            return items.getStackInSlotOnClosing(slot);
        }

        public void setInventorySlotContents(int slot, ItemStack stack) {
            items.setInventorySlotContents(slot, stack);
            markDirty();
        }

        public String getInventoryName() {
            return "stock target";
        }

        public boolean hasCustomInventoryName() {
            return false;
        }

        public int getInventoryStackLimit() {
            return 1000000;
        }

        public boolean isUseableByPlayer(EntityPlayer player) {
            return true;
        }

        public void openInventory() {}

        public void closeInventory() {}

        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return true;
        }

        public int fill(ForgeDirection side, FluidStack fluid, boolean mutate) {
            int result = tank.fill(fluid, mutate);
            if (mutate) markDirty();
            return result;
        }

        public FluidStack drain(ForgeDirection side, FluidStack fluid, boolean mutate) {
            return tank.getFluid() != null && tank.getFluid()
                .isFluidEqual(fluid) ? tank.drain(fluid.amount, mutate) : null;
        }

        public FluidStack drain(ForgeDirection side, int count, boolean mutate) {
            return tank.drain(count, mutate);
        }

        public boolean canFill(ForgeDirection side, Fluid fluid) {
            return true;
        }

        public boolean canDrain(ForgeDirection side, Fluid fluid) {
            return true;
        }

        public FluidTankInfo[] getTankInfo(ForgeDirection side) {
            return new FluidTankInfo[] { tank.getInfo() };
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tank.writeToNBT(tag);
            NBTTagList rows = new NBTTagList();
            for (int i = 0; i < items.getSizeInventory(); i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (stack == null) continue;
                NBTTagCompound row = new NBTTagCompound();
                stack.writeToNBT(row);
                row.setInteger("quantity", stack.stackSize);
                row.setInteger("slot", i);
                rows.appendTag(row);
            }
            tag.setTag("items", rows);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            tank.readFromNBT(tag);
            NBTTagList rows = tag.getTagList("items", 10);
            for (int i = 0; i < rows.tagCount(); i++) {
                NBTTagCompound row = rows.getCompoundTagAt(i);
                ItemStack stack = ItemStack.loadItemStackFromNBT(row);
                if (stack != null) {
                    stack.stackSize = row.getInteger("quantity");
                    items.setInventorySlotContents(row.getInteger("slot"), stack);
                }
            }
        }
    }
}
