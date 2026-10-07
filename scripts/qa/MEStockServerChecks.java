package com.xyp.gtnotgood.common.parts.mestock;

import com.xyp.gtnotgood.common.blocks.mestock.TileMERequester;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Physical SERVER-side panel construction and binding, including live requester-directory synchronization. */
@Mod(
    modid = "gtngmestockserverqa",
    name = "ME Stock Dedicated Server QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class MEStockServerChecks {

    private int ticks;
    private EntityPlayerMP player;
    private WorldServer world;
    private final List<String> results = new ArrayList<>();
    private boolean failed;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.meStockServer.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || ++ticks > 50) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (!server.isDedicatedServer()) throw new AssertionError("Physical dedicated server required");
        if (ticks == 1) {
            world = server.worldServerForDimension(0);
            player = FakePlayerFactory.getMinecraft(world);
            player.setPosition(4.5, 80, 6.5);
            var d = AEApi.instance()
                .definitions();
            world.setBlock(
                4,
                80,
                4,
                d.blocks()
                    .multiPart()
                    .maybeBlock()
                    .get());
            var host = (IPartHost) world.getTileEntity(4, 80, 4);
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
                80,
                3,
                d.blocks()
                    .energyCellCreative()
                    .maybeBlock()
                    .get());
            for (int x = 4; x <= 6; x++) {
                world.setBlock(
                    x,
                    81,
                    4,
                    Block.getBlockFromItem(
                        GTNGItemList.MERequester.get(1)
                            .getItem()));
                ((TileMERequester) world.getTileEntity(x, 81, 4)).setOwnerName(player.getCommandSenderName());
            }
            ((TileMERequester) world.getTileEntity(6, 81, 4)).setName("QA 自定义名称");
            var bus = (PartThresholdExportBus) host.getPart(ForgeDirection.SOUTH);
            bus.stockConfig()
                .setKey(0, AEItemStack.create(new ItemStack(Items.iron_ingot)));
            bus.stockConfig()
                .setAmount(0, 17);
        }
        if (ticks != 50) return;
        check("unnamed requester", new StockGuiFactory.Data(player, 4, 81, 4, -1, null));
        check("named requester", new StockGuiFactory.Data(player, 6, 81, 4, -1, null));
        var terminal = new StockGuiFactory.Data(player, 4, 80, 4, ForgeDirection.DOWN.ordinal(), null);
        try {
            if (terminal.cache() == null || terminal.cache()
                .listing()
                .size() != 3) throw new AssertionError("Live terminal directory must contain three requesters");
        } catch (Throwable error) {
            recordFailure("requester directory", error);
        }
        check("requester terminal", terminal);
        check(
            "remote requester",
            new StockGuiFactory.Data(player, 4, 80, 4, ForgeDirection.DOWN.ordinal(), new int[] { 0, 4, 81, 4 }));
        check("threshold export bus", new StockGuiFactory.Data(player, 4, 80, 4, ForgeDirection.SOUTH.ordinal(), null));
        check("threshold amount", new StockGuiFactory.Data(player, 4, 80, 4, ForgeDirection.SOUTH.ordinal(), null, 0));
        check(
            "dual threshold emitter",
            new StockGuiFactory.Data(player, 4, 80, 4, ForgeDirection.EAST.ordinal(), null));
        try {
            for (StockText text : StockText.values()) text.text();
            results.add("PASS: every shared text lookup");
        } catch (Throwable error) {
            recordFailure("shared text lookup", error);
        }
        results.add(failed ? "FAIL" : "PASS");
        Path report = Paths.get(System.getProperty("gtng.meStockServer.report"));
        Files.createDirectories(report.getParent());
        Files.write(report, results, StandardCharsets.UTF_8);
        System.out.println("ME_STOCK_SERVER_QA: " + results);
        server.initiateShutdown();
    }

    private void check(String name, StockGuiFactory.Data data) {
        try {
            var factory = StockGuiFactory.instance;
            var manager = new ModularSyncManager(false);
            var sync = new PanelSyncManager(manager, true);
            var settings = new UISettings(RecipeViewerSettings.DUMMY);
            settings.defaultCanInteractWith(factory, data);
            // GuiManager.open uses this exact factory, collection and container-binding sequence.
            var panel = factory.createPanel(data, sync, settings);
            WidgetTree.collectSyncValues(sync, panel);
            var container = new ModularContainer();
            container.construct(player, manager, settings, panel.getName(), data);
            manager.onOpen();
            container.onUpdate();
            results.add("PASS: " + name);
        } catch (Throwable error) {
            recordFailure(name, error);
        }
    }

    private void recordFailure(String name, Throwable error) {
        failed = true;
        results.add("FAIL: " + name + " -> " + error);
        error.printStackTrace();
    }
}
