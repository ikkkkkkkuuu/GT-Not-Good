package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnotgood.client.wirelessfrequency.WirelessCardGui;
import com.xyp.gtnotgood.client.wirelessfrequency.WirelessCardRenameDialog;
import com.xyp.gtnotgood.common.items.wirelessfrequency.WirelessFrequencyCard;
import com.xyp.gtnotgood.common.wirelessfrequency.IWirelessLinkView;
import com.xyp.gtnotgood.common.wirelessfrequency.IWirelessPathingState;
import com.xyp.gtnotgood.common.wirelessfrequency.PhysicalClusterTracker;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardBinding;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardContainer;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardViewBuilder;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessChannelManager;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessChannelManager.Address;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessClusterManager;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessHatchOwner;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessInterfaceView;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessMachineGroups;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessNetworkStatistics;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessOperations;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.IPathingGrid;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrorRegistry;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.multi.MTEVacuumFreezer;

@Mod(
    modid = "wirelessfrequencyqa",
    name = "Wireless Frequency QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class WirelessFrequencyClientChecks {

    private boolean started;
    private boolean given;
    private boolean bound;
    private boolean connected;
    private boolean machinesChecked;
    private boolean finished;
    private int ticks;
    private int readyTicks;
    private int guiTicks;
    private volatile boolean deletedSourceLinksVerified;
    private Object stableCluster;
    private IGridNode performanceTarget;
    private IGridNode addedNode;
    private IWirelessPathingState performancePath;
    private long topologyRevision;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.wirelessFrequency.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        WorldServer world = player.getServerForPlayer();
        if (Boolean.getBoolean("gtng.wirelessFrequency.performance")) {
            try {
                inspectPerformance(world, player);
            } catch (Throwable failure) {
                failure.printStackTrace();
                finish("performance: " + failure);
            }
            return;
        }
        if (Boolean.getBoolean("gtng.wirelessFrequency.multiSource")) {
            try {
                inspectMultipleSources(world, player);
            } catch (Throwable failure) {
                failure.printStackTrace();
                finish("multi source: " + failure);
            }
            return;
        }
        if (!System.getProperty("gtng.wirelessFrequency.world", "").isEmpty()) {
            try {
                inspectSavedWorld(world, player);
            } catch (Throwable failure) {
                failure.printStackTrace();
                finish("saved world: " + failure);
            }
            return;
        }
        if (!given) {
            ItemStack card = GTNGItemList.WirelessFrequencyCard.get(1);
            require(card != null && card.getItem() != null, "card registration");
            require(
                world.setBlock(2, 6, 2,
                    AEApi.instance().definitions().blocks().creativeEnergyController().maybeBlock().get()),
                "source placement");
            require(world.setBlock(14, 6, 2, AEApi.instance().definitions().blocks().iface().maybeBlock().get()),
                "target placement");
            player.inventory.currentItem = 0;
            player.inventory.setInventorySlotContents(0, card);
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
            given = true;
        } else if (!bound) {
            if (++readyTicks < 60) return;
            ItemStack card = player.getCurrentEquippedItem();
            player.setSneaking(true);
            ((WirelessFrequencyCard) card.getItem()).onItemUseFirst(card, player, world, 2, 6, 2, 1, 0.5F, 0.5F, 0.5F);
            player.setSneaking(false);
            bound = WirelessCardBinding.read(card.getTagCompound()) != null;
            if (readyTicks > 300) require(bound, "controller binding");
            if (bound) readyTicks = 0;
        } else if (!connected) {
            if (++readyTicks < 20) return;
            try {
                ItemStack card = player.getCurrentEquippedItem();
                var binding = WirelessCardBinding.read(card.getTagCompound());
                var result = WirelessOperations.toggle(player, binding,
                    new Address(0, 14, 6, 2, ForgeDirection.UNKNOWN));
                connected = result == WirelessOperations.Result.CONNECTED;
                if (readyTicks > 300) require(connected, "wireless link: " + result);
                if (connected) {
                    require(!WirelessChannelManager.entrances().isEmpty(), "wireless entrance");
                    readyTicks = 0;
                }
            } catch (Exception failure) {
                throw new AssertionError("wireless link", failure);
            }
        } else if (!machinesChecked) {
            try {
                checkMachineGrouping(world, player.getUniqueID());
                machinesChecked = true;
            } catch (Throwable failure) {
                failure.printStackTrace();
                finish("machine grouping: " + failure);
            }
        } else if (++readyTicks == 20) {
            GuiFactories.playerInventory().openFromMainHand(player);
        }
    }

    private void inspectPerformance(WorldServer world, EntityPlayerMP player) throws Exception {
        int tick = ++readyTicks;
        int dimension = world.provider.dimensionId;
        Address source = new Address(dimension, 2, 6, 2, ForgeDirection.UNKNOWN);
        Address target = new Address(dimension, 514, 6, 2, ForgeDirection.UNKNOWN);
        var blocks = AEApi.instance().definitions().blocks();
        if (tick == 1) {
            player.setPositionAndUpdate(2, 8, 2);
            world.setSpawnLocation(0, 4, 0);
            require(world.setBlock(2, 6, 2, blocks.creativeEnergyController().maybeBlock().get()),
                "performance source placement");
            require(world.setBlock(514, 6, 2, blocks.iface().maybeBlock().get()), "remote target placement");
            player.inventory.currentItem = 0;
            player.inventory.setInventorySlotContents(0, GTNGItemList.WirelessFrequencyCard.get(1));
        } else if (tick == 80) {
            ItemStack card = player.getCurrentEquippedItem();
            player.setSneaking(true);
            ((WirelessFrequencyCard) card.getItem()).onItemUseFirst(card, player, world, 2, 6, 2, 1, .5F, .5F, .5F);
            player.setSneaking(false);
            var binding = WirelessCardBinding.read(card.getTagCompound());
            require(binding != null, "performance controller binding");
            require(WirelessOperations.toggle(player, binding, target) == WirelessOperations.Result.CONNECTED,
                "performance wireless link");
        } else if (tick == 160) {
            performanceTarget = target.node();
            require(performanceTarget != null, "remote target node remains loaded");
            stableCluster = clusterOf(performanceTarget);
            require(stableCluster != null && clusterMembers(performanceTarget).size() == 1,
                "wireless edge is excluded from physical membership");
            IPathingGrid path = performanceTarget.getGrid().getCache(IPathingGrid.class);
            require(path instanceof IWirelessPathingState, "PathGridCache topology mixin applied");
            performancePath = (IWirelessPathingState) path;
            require(!path.isNetworkBooting() && !performancePath.gtng$isRepathPending(),
                "wireless network settled before observing scans");
            topologyRevision = performancePath.gtng$topologyRevision();
        } else if (tick > 160 && tick <= 400) {
            require(clusterOf(performanceTarget) == stableCluster,
                "stable topology must retain the physical cluster at tick " + tick);
            require(performancePath.gtng$topologyRevision() == topologyRevision,
                "stable network must retain its topology revision");
            require(
                WirelessChannelManager.entrances().size() == 1 && WirelessChannelManager.entrances().get(0).isLive(),
                "stable wireless entrance remains live");
            if (tick > 340) {
                world.markBlockForUpdate(514, 6, 2);
                world.markBlockForUpdate(2, 6, 2);
            }
            if (tick == 340) System.out.println("WIRELESS_FREQUENCY_QA: 180 stable ticks without a physical scan PASS");
        } else if (tick == 401) {
            require(clusterOf(performanceTarget) == stableCluster, "render refresh does not schedule a scan");
            System.out.println("WIRELESS_FREQUENCY_QA: 60 render refreshes without a scan PASS");
            require(world.setBlock(515, 6, 2, blocks.iface().maybeBlock().get()), "adjacent interface placement");
        } else if (tick == 440) {
            addedNode = new Address(dimension, 515, 6, 2, ForgeDirection.UNKNOWN).node();
            require(addedNode != null && clusterMembers(performanceTarget).contains(addedNode)
                && clusterMembers(performanceTarget).size() == 2, "wired addition invalidates physical membership");
            require(clusterOf(performanceTarget) != stableCluster, "wired addition rebuilds physical cluster");
            require(performancePath.gtng$topologyRevision() > topologyRevision,
                "PathGridCache observes native node and connection additions");
            stableCluster = clusterOf(performanceTarget);
            performancePath = (IWirelessPathingState) performanceTarget.getGrid().getCache(IPathingGrid.class);
            topologyRevision = performancePath.gtng$topologyRevision();
            require(world.setBlockToAir(515, 6, 2), "adjacent interface removal");
        } else if (tick == 480) {
            require(clusterOf(performanceTarget) != stableCluster && clusterMembers(performanceTarget).size() == 1,
                "wired removal rebuilds physical membership");
            require(clusterOf(addedNode) == null, "removed native node is no longer tracked");
            require(performancePath.gtng$topologyRevision() > topologyRevision,
                "PathGridCache observes native node and connection removals");
            System.out.println("WIRELESS_FREQUENCY_QA: real wired additions, removals and topology revisions PASS");
            require(world.theChunkProviderServer.chunkExists(32, 0), "remote target chunk was loaded");
            world.theChunkProviderServer.unloadChunksIfNotNearSpawn(32, 0);
            world.theChunkProviderServer.unloadQueuedChunks();
            require(!world.theChunkProviderServer.chunkExists(32, 0), "remote target chunk actually unloaded");
        } else if (tick == 500) {
            require(!world.theChunkProviderServer.chunkExists(32, 0) && target.node() == null,
                "wireless bookkeeping does not reload the target chunk");
            require(clusterOf(performanceTarget) == null && WirelessClusterManager.links(source).isEmpty(),
                "chunk unload invalidates physical membership");
            require(WirelessChannelManager.entrances().isEmpty(), "chunk unload removes stale wireless entrances");
            System.out.println("WIRELESS_FREQUENCY_QA: native chunk unload removes stale nodes and entrances PASS");
            finish("PASS");
        }
    }

    @SuppressWarnings("unchecked")
    private static PhysicalClusterTracker<IGridNode, Address> clusterTracker() throws Exception {
        Field field = WirelessClusterManager.class.getDeclaredField("TRACKER");
        field.setAccessible(true);
        return (PhysicalClusterTracker<IGridNode, Address>) field.get(null);
    }

    private static Object clusterOf(IGridNode node) throws Exception {
        return clusterTracker().clusterOf(node);
    }

    private static Set<IGridNode> clusterMembers(IGridNode node) throws Exception {
        var cluster = clusterTracker().clusterOf(node);
        require(cluster != null, "physical cluster is available");
        return cluster.nodes();
    }

    private void inspectMultipleSources(WorldServer world, EntityPlayerMP player) throws Exception {
        int tick = ++readyTicks;
        var blocks = AEApi.instance().definitions().blocks();
        if (tick == 1) {
            player.setPositionAndUpdate(16, 8, 2);
            for (int x : new int[] { 2, 32 }) {
                require(world.setBlock(x, 6, 2, blocks.creativeEnergyController().maybeBlock().get()),
                    "source placement");
                require(world.setBlock(x + 12, 6, 2, blocks.iface().maybeBlock().get()), "target placement");
            }
            player.inventory.currentItem = 0;
            player.inventory.setInventorySlotContents(0, GTNGItemList.WirelessFrequencyCard.get(1));
        }
        if (tick == 80 || tick == 120) {
            int x = tick == 80 ? 2 : 32;
            var card = player.getCurrentEquippedItem();
            player.setSneaking(true);
            ((WirelessFrequencyCard) card.getItem()).onItemUseFirst(card, player, world, x, 6, 2, 1, .5F, .5F, .5F);
            player.setSneaking(false);
            var binding = WirelessCardBinding.read(card.getTagCompound());
            require(binding != null && binding.source().x() == x, "bind selected source");
            require(WirelessOperations.toggle(player, binding, new Address(0, x + 12, 6, 2, ForgeDirection.UNKNOWN))
                == WirelessOperations.Result.CONNECTED, "connect separate source network");
            if (tick == 80) WirelessCardBinding.toggleAutomatic(card);
            else {
                require(WirelessCardBinding.sources(card.getTagCompound()).size() == 2, "both bindings retained");
                require(!WirelessCardBinding.automatic(card.getTagCompound()), "new source starts with auto disabled");
            }
        }
        if (tick == 180) GuiFactories.playerInventory().openFromMainHand(player);
        if (tick > 180 && WirelessCardBinding.sources(player.getCurrentEquippedItem().getTagCompound()).isEmpty()) {
            require(!WirelessClusterManager.links(new Address(0, 2, 6, 2, ForgeDirection.UNKNOWN)).isEmpty(),
                "deleted first source retains wireless links");
            require(!WirelessClusterManager.links(new Address(0, 32, 6, 2, ForgeDirection.UNKNOWN)).isEmpty(),
                "deleted second source retains wireless links");
            deletedSourceLinksVerified = true;
        }
    }

    private void inspectMultipleSourcesGui(Minecraft mc) {
        var container = (WirelessCardContainer) mc.thePlayer.openContainer;
        WirelessCardGui gui = null;
        for (var panel : ModularScreen.getCurrent().getPanelManager().getOpenPanels())
            if (panel instanceof WirelessCardGui main) gui = main;
        require(gui != null, "wireless manager still open");
        if (guiTicks == 21) gui.action(WirelessCardActions.OPEN_SOURCES);
        else if (guiTicks == 40) {
            require(container.display.getBoolean("sourcesPage"), "source entry opens source page");
            require(container.display.getTagList("rows", 10).tagCount() == 2, "both sources shown");
            var selectedButton = gui.buttons.stream()
                .filter(button -> button.id == WirelessCardActions.OPEN_DETAILS + 1).findFirst().get();
            ((ButtonWidget<?>) selectedButton.widget).onMousePressed(0);
            require(container.display.getBoolean("sourcesPage"), "selected source button is inactive");
            ScreenShotHelper.saveScreenshot(output(), "wireless-multiple-sources.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            var selectButton = gui.buttons.stream().filter(button -> button.id == WirelessCardActions.OPEN_DETAILS)
                .findFirst().get();
            require(selectButton.widget.getArea().width == 60, "source has a dedicated select button");
            ((ButtonWidget<?>) selectButton.widget).onMousePressed(0);
        } else if (guiTicks == 70) {
            require(!container.display.getBoolean("sourcesPage"), "source selection returns to network");
            require(container.display.getString("source").equals("D0  2, 6, 2"), "selected first network");
            require(container.display.getBoolean("auto"), "first source auto preference restored");
            require(
                container.display.getTagList("rows", 10).tagCount() == 1 && container.display.getTagList("rows", 10)
                    .getCompoundTagAt(0).getString("position").contains("14, 6, 2"),
                "first source shows only its own target");
            ScreenShotHelper.saveScreenshot(output(), "wireless-source-first-network.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            gui.action(WirelessCardActions.TOGGLE_AUTO);
        } else if (guiTicks == 100) {
            require(!container.display.getBoolean("auto"), "auto toggle synchronized");
            gui.action(WirelessCardActions.OPEN_SOURCES);
            gui.action(WirelessCardActions.OPEN_DETAILS + 1);
        } else if (guiTicks == 130) {
            require(container.display.getString("source").equals("D0  32, 6, 2"), "selected second network");
            require(!container.display.getBoolean("auto"), "second source remains auto disabled");
            require(
                container.display.getTagList("rows", 10).tagCount() == 1 && container.display.getTagList("rows", 10)
                    .getCompoundTagAt(0).getString("position").contains("44, 6, 2"),
                "second source retains only its own target");
            gui.action(WirelessCardActions.OPEN_SOURCES);
            gui.action(WirelessCardActions.OPEN_DETAILS);
        } else if (guiTicks == 160) {
            require(
                container.display.getString("source").equals("D0  2, 6, 2") && !container.display.getBoolean("auto"),
                "switching back restores updated preference");
            container.sync.request(WirelessCardActions.SELECT_SOURCE, 999, "", 0);
        } else if (guiTicks == 190) {
            require(container.display.getString("source").equals("D0  2, 6, 2"), "invalid source ID is rejected");
            gui.action(WirelessCardActions.OPEN_SOURCES);
        } else if (guiTicks == 210 || guiTicks == 260) {
            int action = WirelessCardActions.RENAME_SOURCE + (guiTicks == 210 ? 0 : 1);
            var renameButton = gui.buttons.stream().filter(button -> button.id == action).findFirst().get();
            ((ButtonWidget<?>) renameButton.widget).onMousePressed(0);
        } else if (guiTicks == 230 || guiTicks == 280) {
            var panel = ModularScreen.getCurrent().getPanelManager().getTopMostPanel();
            require(panel instanceof WirelessCardRenameDialog, "source rename button opens name editor");
            var dialog = (WirelessCardRenameDialog) panel;
            require(dialog.name.getText().isEmpty(), "rename editor targets the requested unnamed source");
            dialog.name.setText(guiTicks == 230 ? "生产网络" : "仓储网络");
            if (guiTicks == 230) ScreenShotHelper.saveScreenshot(output(), "wireless-source-rename-dialog.png",
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            dialog.onKeyPressed('\n', 28);
        } else if (guiTicks == 250) {
            require(container.display.getTagList("sources", 10).getCompoundTagAt(0).getString("name").equals("生产网络"),
                "first source name synchronized");
            require(container.display.getTagList("sources", 10).getCompoundTagAt(1).getString("name").isEmpty(),
                "renaming one source leaves the other unchanged");
        } else if (guiTicks == 300) {
            var sources = container.display.getTagList("sources", 10);
            require(sources.getCompoundTagAt(0).getString("name").equals("生产网络")
                && sources.getCompoundTagAt(1).getString("name").equals("仓储网络"), "separate source names retained");
            ScreenShotHelper.saveScreenshot(output(), "wireless-named-sources.png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            gui.action(WirelessCardActions.BACK);
        } else if (guiTicks == 320) {
            require(container.display.getString("sourceName").equals("生产网络"), "overview shows selected source name");
            ScreenShotHelper.saveScreenshot(output(), "wireless-named-source-overview.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            gui.action(WirelessCardActions.OPEN_SOURCES);
        } else if (guiTicks == 340 || guiTicks == 380) {
            if (guiTicks == 340) ScreenShotHelper.saveScreenshot(output(), "wireless-source-delete-buttons.png",
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            var button = gui.buttons.stream().filter(control -> control.id == WirelessCardActions.DELETE_SOURCE)
                .findFirst().get();
            ((ButtonWidget<?>) button.widget).onMousePressed(0);
        } else if (guiTicks == 360) {
            require(container.display.getBoolean("sourcesPage"), "deleting current source keeps source page open");
            require(
                container.display.getTagList("rows", 10).tagCount() == 1
                    && container.display.getString("sourceName").equals("仓储网络"),
                "deleting current source selects remaining named source");
        } else if (guiTicks == 420) {
            require(
                container.display.getBoolean("sourcesPage") && !container.display.getBoolean("bound")
                    && container.display.getTagList("rows", 10).tagCount() == 0,
                "deleting final source leaves an empty card");
            require(!container.display.getBoolean("auto"), "deleting final source disables auto connect");
            require(deletedSourceLinksVerified, "source removal preserves existing network connections");
            finish("PASS");
        }
    }

    private void inspectSavedWorld(WorldServer world, EntityPlayerMP player) throws Exception {
        if (++readyTicks == 1) {
            player.setPositionAndUpdate(238, 8, 811);
            ItemStack card = GTNGItemList.WirelessFrequencyCard.get(1);
            player.inventory.currentItem = 0;
            player.inventory.setInventorySlotContents(0, card);
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }
        if (readyTicks == 100) {
            ItemStack card = player.getCurrentEquippedItem();
            player.setSneaking(true);
            ((WirelessFrequencyCard) card.getItem()).onItemUseFirst(card, player, world, 238, 4, 811, 1, .5F, .5F, .5F);
            player.setSneaking(false);
            require(WirelessCardBinding.read(card.getTagCompound()) != null, "saved world source binding");
            if (!WirelessCardBinding.automatic(card.getTagCompound())) WirelessCardBinding.toggleAutomatic(card);
        }
        if (readyTicks != 240) return;
        for (Object value : new ArrayList<>(world.loadedTileEntityList)) {
            if (
                !(value instanceof BaseMetaTileEntity base) || base.xCoord < 220
                    || base.xCoord > 250
                    || base.zCoord < 800
                    || base.zCoord > 830
            ) continue;
            var meta = base.getMetaTileEntity();
            System.out.println("WIRELESS_WORLD: tile=" + base.xCoord
                + ","
                + base.yCoord
                + ","
                + base.zCoord
                + " meta="
                + meta.getClass().getName());
            if (meta instanceof MTEMultiBlockBase controller) {
                System.out.println("WIRELESS_WORLD: formed=" + controller.mMachine
                    + " valid="
                    + controller.isValid()
                    + " in="
                    + controller.mInputHatches.size()
                    + "/"
                    + controller.mInputBusses.size()
                    + "/"
                    + controller.mDualInputHatches.size()
                    + " out="
                    + controller.mOutputHatches.size()
                    + "/"
                    + controller.mOutputBusses.size());
                System.out.println("WIRELESS_WORLD: recheck=" + controller.checkStructure(true, base));
                Field errors = MTEMultiBlockBase.class.getDeclaredField("structureErrors");
                errors.setAccessible(true);
                for (Object error : (List<?>) errors.get(controller))
                    System.out.println("WIRELESS_WORLD: structureError=" + ((StructureError) error).getId());
            }
            if (meta instanceof WirelessHatchOwner hatch) {
                System.out.println("WIRELESS_WORLD: owners=" + hatch.wirelessFrequency$getControllers().stream()
                    .map(controller -> controller.getBaseMetaTileEntity().getXCoord() + ","
                        + controller.getBaseMetaTileEntity().getYCoord()
                        + ","
                        + controller.getBaseMetaTileEntity().getZCoord())
                    .collect(Collectors.joining(";")));
            }
            IGridNode node = ((IGridHost) base).getGridNode(ForgeDirection.UNKNOWN);
            if (node != null) System.out.println("WIRELESS_WORLD: node.host=" + node.getMachine().getClass().getName());
        }
        ItemStack card = player.getCurrentEquippedItem();
        Method build = WirelessCardViewBuilder.class.getDeclaredMethod("build", WirelessCardBinding.class,
            ItemStack.class);
        build.setAccessible(true);
        Object snapshot = build.invoke(new WirelessCardViewBuilder(), WirelessCardBinding.read(card.getTagCompound()),
            card);
        Field data = snapshot.getClass().getDeclaredField("payload");
        data.setAccessible(true);
        NBTTagCompound view = (NBTTagCompound) data.get(snapshot);
        System.out.println("WIRELESS_WORLD: view=" + view);
        var rows = view.getTagList("rows", 10);
        Set<String> controllers = new HashSet<>();
        int children = 0;
        for (int i = 0; i < rows.tagCount(); i++) {
            var row = rows.getCompoundTagAt(i);
            require(row.getBoolean("group"), "saved world row uses controller: " + row.getString("position"));
            controllers.add(row.getString("position"));
            children += row.getTagList("children", 10).tagCount();
        }
        require(controllers.size() == 2 && children == 3, "two real benders own all three ME nodes");
        GuiFactories.playerInventory().openFromMainHand(player);
    }

    private static void checkMachineGrouping(WorldServer world, UUID owner) throws Exception {
        ItemStack icon = null;
        for (IMetaTileEntity entry : GregTechAPI.METATILEENTITIES) {
            if (entry instanceof MTEVacuumFreezer) {
                icon = entry.getStackForm(1);
                break;
            }
        }
        require(icon != null, "vacuum freezer registration");

        TestController first = placeController(world, icon, owner, 28);
        TestController second = placeController(world, icon, owner, 40);
        BaseMetaTileEntity firstBus = placeMachine(world, ItemList.Hatch_Output_Bus_ME.get(1), owner, 29);
        BaseMetaTileEntity firstOutput = placeMachine(world, ItemList.Hatch_Output_ME.get(1), owner, 30);
        BaseMetaTileEntity firstInput = placeMachine(world, GTNGItemList.SuperMTEHatchCraftingInputME.get(1), owner,
            31);
        BaseMetaTileEntity sharedInput = placeMachine(world, GTNGItemList.SuperMTEHatchCraftingInputME.get(1), owner,
            35);
        BaseMetaTileEntity secondBus = placeMachine(world, ItemList.Hatch_Output_Bus_ME.get(1), owner, 41);
        BaseMetaTileEntity secondOutput = placeMachine(world, ItemList.Hatch_Output_ME.get(1), owner, 42);
        BaseMetaTileEntity secondInput = placeMachine(world, GTNGItemList.SuperMTEHatchCraftingInputME.get(1), owner,
            43);

        first.attach(firstBus, firstOutput, firstInput, sharedInput);
        second.attach(secondBus, secondOutput, secondInput, sharedInput);
        require(first.checkStructure(true, first.getBaseMetaTileEntity()), "first controller structure check");
        require(second.checkStructure(true, second.getBaseMetaTileEntity()), "second controller structure check");

        for (BaseMetaTileEntity hatch : new BaseMetaTileEntity[] { firstBus, firstOutput, firstInput })
            checkOwners(hatch, first);
        for (BaseMetaTileEntity hatch : new BaseMetaTileEntity[] { secondBus, secondOutput, secondInput })
            checkOwners(hatch, second);
        checkOwners(sharedInput, first, second);
        checkRows(first, second, new BaseMetaTileEntity[] { firstBus, firstOutput, firstInput, secondBus, secondOutput,
            secondInput, sharedInput });
        first.formed = second.formed = false;
        require(!first.checkStructure(true, first.getBaseMetaTileEntity()), "first controller remains incomplete");
        require(!second.checkStructure(true, second.getBaseMetaTileEntity()), "second controller remains incomplete");
        checkOwners(firstBus, first);
        checkOwners(firstOutput, first);
        checkOwners(firstInput, first);
        checkOwners(sharedInput, first, second);
        checkRows(first, second, new BaseMetaTileEntity[] { firstBus, firstOutput, firstInput, secondBus, secondOutput,
            secondInput, sharedInput });
        second.attach(secondBus, secondOutput, secondInput, null);
        require(!second.checkStructure(true, second.getBaseMetaTileEntity()), "incomplete controller recheck");
        checkOwners(sharedInput, first);
        for (BaseMetaTileEntity hatch : new BaseMetaTileEntity[] { secondBus, secondOutput, secondInput })
            checkOwners(hatch, second);
        first.registered = false;
        require(!first.checkStructure(true, first.getBaseMetaTileEntity()), "dismantled controller recheck");
        for (BaseMetaTileEntity hatch : new BaseMetaTileEntity[] { firstBus, firstOutput, firstInput, sharedInput })
            checkOwners(hatch);
        System.out.println("WIRELESS_FREQUENCY_QA: two equal GT controllers, ME hatch ownership and grouping PASS");
    }

    private static TestController placeController(WorldServer world, ItemStack icon, UUID owner, int x) {
        BaseMetaTileEntity base = placeMachine(world, icon, owner, x);
        TestController controller = new TestController(icon);
        controller.setBaseMetaTileEntity(base);
        require(base.getMetaTileEntity() == controller, "test controller attachment at " + x);
        return controller;
    }

    private static BaseMetaTileEntity placeMachine(WorldServer world, ItemStack stack, UUID owner, int x) {
        require(stack != null && stack.getItem() != null, "GT machine stack at " + x);
        require(world.setBlock(x, 6, 2, GregTechAPI.sBlockMachines, 0, 3), "GT machine placement at " + x);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 6, 2);
        require(tile != null, "GT machine tile at " + x);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerUuid(owner);
        tile.setFrontFacing(ForgeDirection.SOUTH);
        require(tile.getMetaTileEntity() != null, "GT meta tile at " + x);
        return tile;
    }

    private static void checkOwners(BaseMetaTileEntity base, MTEMultiBlockBase... expected) {
        MTEHatch hatch = (MTEHatch) base.getMetaTileEntity();
        Set<MTEMultiBlockBase> owners = ((WirelessHatchOwner) hatch).wirelessFrequency$getControllers();
        require(owners.size() == expected.length, "owner count at " + base.xCoord + ": " + owners.size());
        for (MTEMultiBlockBase controller : expected)
            require(owners.contains(controller), "controller missing at " + base.xCoord);
        System.out.println("WIRELESS_FREQUENCY_QA: hatch=" + base.xCoord
            + " owners="
            + owners.stream().map(controller -> Integer.toString(controller.getBaseMetaTileEntity().getXCoord()))
                .collect(Collectors.joining(",")));
    }

    private static void checkRows(TestController first, TestController second, BaseMetaTileEntity[] hatches)
        throws Exception {
        List<IWirelessLinkView> links = new ArrayList<>();
        for (BaseMetaTileEntity hatch : hatches) links.add(link(hatch));
        links.add(link(hatches[0], hatches[3]));
        Method grouping = WirelessMachineGroups.class.getDeclaredMethod("group", List.class,
            WirelessInterfaceView.class);
        grouping.setAccessible(true);
        List<?> rows = (List<?>) grouping.invoke(null, links, new WirelessInterfaceView());
        int grouped = 0, separate = 0, shared = 0, crossMachine = 0;
        Set<Integer> machineRows = new HashSet<>();
        for (Object row : rows) {
            Method group = row.getClass().getDeclaredMethod("group");
            Method target = row.getClass().getDeclaredMethod("target");
            Method type = row.getClass().getDeclaredMethod("type");
            group.setAccessible(true);
            target.setAccessible(true);
            type.setAccessible(true);
            boolean isGroup = (boolean) group.invoke(row);
            Address address = (Address) target.invoke(row);
            Object key = type.invoke(row);
            System.out.println("WIRELESS_FREQUENCY_QA: row.group=" + isGroup + " typeKey=" + key + " x=" + address.x());
            if (isGroup) {
                grouped++;
                machineRows.add(address.x());
            } else {
                separate++;
                if (address.x() == hatches[6].xCoord && String.valueOf(key).contains("SHARED_HATCH")) shared++;
                if (address.x() == hatches[0].xCoord && key == null) crossMachine++;
            }
        }
        require(grouped == 2 && separate == 2 && shared == 1 && crossMachine == 1,
            "two machine rows, one shared row and one cross-machine row");
        require(
            machineRows.size() == 2 && machineRows.contains(first.getBaseMetaTileEntity().getXCoord())
                && machineRows.contains(second.getBaseMetaTileEntity().getXCoord()),
            "each grouped row targets its controller");
    }

    private static IWirelessLinkView link(BaseMetaTileEntity... hatches) {
        Set<IGridNode> members = new HashSet<>();
        for (BaseMetaTileEntity hatch : hatches) members.add(node(hatch));
        BaseMetaTileEntity first = hatches[0];
        Address address = new Address(first.getWorldObj().provider.dimensionId, first.xCoord, first.yCoord,
            first.zCoord, ForgeDirection.UNKNOWN);
        IGridNode head = members.iterator().next();
        return new IWirelessLinkView.ClusterView(address, head, Collections.emptyMap(), false, false, "active", 0,
            new WirelessNetworkStatistics(members.size(), members.size(), members.size(), 0, true), members);
    }

    private static IGridNode node(BaseMetaTileEntity hatch) {
        IGridBlock block = (IGridBlock) Proxy.newProxyInstance(IGridBlock.class.getClassLoader(),
            new Class<?>[] { IGridBlock.class }, (proxy, method, args) -> {
                if (method.getName().equals("getMachineRepresentation"))
                    return hatch.getMetaTileEntity().getStackForm(1);
                return objectMethod(proxy, method.getName(), args);
            });
        return (IGridNode) Proxy.newProxyInstance(IGridNode.class.getClassLoader(), new Class<?>[] { IGridNode.class },
            (proxy, method, args) -> {
                if (method.getName().equals("getMachine")) return hatch;
                if (method.getName().equals("getGridBlock")) return block;
                return objectMethod(proxy, method.getName(), args);
            });
    }

    private static Object objectMethod(Object proxy, String name, Object[] args) {
        if (name.equals("hashCode")) return System.identityHashCode(proxy);
        if (name.equals("equals")) return proxy == args[0];
        if (name.equals("toString")) return proxy.getClass().getName();
        throw new AssertionError("Unexpected AE node method: " + name);
    }

    private static final class TestController extends MTEVacuumFreezer {

        private final ItemStack icon;
        private IGregTechTileEntity bus, output, input, shared;
        private boolean formed = true;
        private boolean registered = true;

        TestController(ItemStack icon) {
            super("wireless_frequency_qa_freezer");
            this.icon = icon.copy();
        }

        void attach(IGregTechTileEntity bus, IGregTechTileEntity output, IGregTechTileEntity input,
            IGregTechTileEntity shared) {
            this.bus = bus;
            this.output = output;
            this.input = input;
            this.shared = shared;
        }

        @Override
        public IMetaTileEntity newMetaEntity(IGregTechTileEntity base) {
            return new TestController(icon);
        }

        @Override
        public ItemStack getMachineCraftingIcon() {
            return icon.copy();
        }

        @Override
        public void checkMachine(IGregTechTileEntity base, ItemStack stack, List<StructureError> errors) {
            if (registered) {
                mOutputBusses.add((MTEHatchOutputBus) bus.getMetaTileEntity());
                mOutputHatches.add((MTEHatchOutput) output.getMetaTileEntity());
                mDualInputHatches.add((IDualInputHatch) input.getMetaTileEntity());
                if (shared != null) mDualInputHatches.add((IDualInputHatch) shared.getMetaTileEntity());
            }
            if (!formed) errors.add(StructureErrorRegistry.UNKNOWN_STRUCTURE_ERROR);
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        try {
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                String saved = System.getProperty("gtng.wirelessFrequency.world", "");
                mc.launchIntegratedServer(saved.isEmpty() ? "wireless-frequency-" + System.currentTimeMillis() : saved,
                    "Wireless Frequency QA",
                    new WorldSettings(617L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (mc.theWorld == null || mc.thePlayer == null) return;
            if (++ticks > 1200) throw new AssertionError("card GUI timed out");
            if (ModularScreen.getCurrent() != null && mc.currentScreen != null && ++guiTicks > 20) {
                if (Boolean.getBoolean("gtng.wirelessFrequency.multiSource")) {
                    inspectMultipleSourcesGui(mc);
                    return;
                }
                if (!System.getProperty("gtng.wirelessFrequency.world", "").isEmpty()) {
                    inspectSavedGui(mc);
                    return;
                }
                ScreenShotHelper.saveScreenshot(output(), "wireless-frequency-gui.png", mc.displayWidth,
                    mc.displayHeight, mc.getFramebuffer());
                finish("PASS");
            } else if (ModularScreen.getCurrent() == null || mc.currentScreen == null) guiTicks = 0;
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish(failure.toString());
        }
    }

    private void inspectSavedGui(Minecraft mc) {
        WirelessCardContainer container = (WirelessCardContainer) mc.thePlayer.openContainer;
        var view = container.clientView();
        if (guiTicks == 21) {
            var rows = container.display.getTagList("rows", 10);
            require(rows.tagCount() == 1 && rows.getCompoundTagAt(0).getBoolean("category"),
                "main GUI groups same-type controllers");
            ScreenShotHelper.saveScreenshot(output(), "wireless-world-type-overview.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            view.navigate(WirelessCardActions.OPEN_DETAILS, false);
        } else if (guiTicks == 40) {
            require(view.groupDisplay().getBoolean("typeGroup"), "opens shared type panel");
            var rows = view.groupDisplay().getTagList("rows", 10);
            require(rows.tagCount() == 2, "type panel contains only two controller entries");
            int target = -1;
            for (int i = 0; i < rows.tagCount(); i++) {
                var row = rows.getCompoundTagAt(i);
                require(row.getBoolean("group") && row.getString("key").startsWith("machine:"),
                    "type list retains controller hierarchy");
                if (row.getInteger("clusters") == 2) target = i;
            }
            require(target >= 0, "controller with input and output");
            ScreenShotHelper.saveScreenshot(output(), "wireless-world-controller-list.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            view.navigate(WirelessCardActions.OPEN_DETAILS + target, true);
        } else if (guiTicks == 60) {
            var data = view.groupDisplay();
            var rows = data.getTagList("rows", 10);
            require(rows.tagCount() == 2 && data.getString("groupPosition").contains("814"),
                "first controller opens only its own two devices");
            Set<Integer> metadata = new HashSet<>();
            for (int i = 0; i < rows.tagCount(); i++) {
                var row = rows.getCompoundTagAt(i);
                require(row.getString("position").contains("814"), "device belongs to the first controller");
                metadata.add(row.getCompoundTag("icon").getShort("Damage") & 65535);
                require(row.getBoolean("leaf") && !row.hasKey("interfaceNames"),
                    "single-node hatch displays its own identity without another details layer");
                require(view.navigate(WirelessCardActions.OPEN_DETAILS + i, true), "device does not drill down");
            }
            require(
                metadata.contains(GTNGItemList.SuperMTEHatchCraftingInputME.get(1).getItemDamage())
                    && metadata.contains(ItemList.Hatch_Output_Bus_ME.get(1).getItemDamage()),
                "correct ME hatch variants in machine panel");
            require(view.groupDisplay().getTagList("rows", 10).tagCount() == 2,
                "input and output stay together after device clicks");
            ScreenShotHelper.saveScreenshot(output(), "wireless-world-controller-devices.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            view.navigate(WirelessCardActions.BACK, true);
        } else if (guiTicks == 80) {
            var rows = view.groupDisplay().getTagList("rows", 10);
            require(rows.tagCount() == 2 && rows.getCompoundTagAt(0).getBoolean("group"),
                "back returns to controller list");
            int target = -1;
            for (int i = 0; i < rows.tagCount(); i++)
                if (rows.getCompoundTagAt(i).getInteger("clusters") == 1) target = i;
            require(target >= 0, "second controller in list");
            view.navigate(WirelessCardActions.OPEN_DETAILS + target, true);
        } else if (guiTicks == 100) {
            var data = view.groupDisplay();
            require(data.getTagList("rows", 10).tagCount() == 1 && data.getString("groupPosition").contains("808"),
                "second controller opens only its own input assembly");
            ScreenShotHelper.saveScreenshot(output(), "wireless-world-other-controller.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            view.navigate(WirelessCardActions.BACK, true);
            view.navigate(WirelessCardActions.BACK, true);
            require(!view.groupDisplay().getBoolean("group"), "back through controller list closes panel");
            finish("PASS");
        }
    }

    private static File output() {
        return new File(System.getProperty("gtng.wirelessFrequency.output"));
    }

    private void finish(String result) {
        finished = true;
        try {
            Files.write(new File(output(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("WIRELESS_FREQUENCY_QA: " + result);
        FMLCommonHandler.instance().exitJava(result.equals("PASS") ? 0 : 1, true);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
