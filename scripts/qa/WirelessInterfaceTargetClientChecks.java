package com.xyp.gtnotgood.common.wirelessfrequency;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.xyp.gtnotgood.client.wirelessfrequency.WirelessCardGui;
import com.xyp.gtnotgood.common.blocks.stockio.TileStockIOInterface;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessChannelManager.Address;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessMachineGroups.Row;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.helpers.ICustomNameObject;
import appeng.parts.AEBasePart;
import appeng.tile.misc.TileInterface;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;

@Mod(
    modid = "wirelessinterfacetargetqa",
    name = "Wireless Interface Target QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class WirelessInterfaceTargetClientChecks {

    private final List<Fixture> fixtures = new ArrayList<>();
    private boolean started, serverChecked;
    private volatile boolean finished;
    private volatile NBTTagCompound packet;
    private int serverTicks, clientTicks, guiTicks, checks;
    private ItemStack reactor, furnace;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.wirelessInterfaceTarget.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || serverChecked || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (++serverTicks == 1) setup(player);
            if (serverTicks != 100) return;
            verifyTargets();
            packet = buildPacket(player.worldObj);
            serverChecked = true;
            GuiFactories.playerInventory().openFromMainHand(player);
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        reactor = ItemList.Machine_LV_ChemicalReactor.get(1);
        furnace = ItemList.Machine_LV_E_Furnace.get(1);
        var definitions = AEApi.instance().definitions();
        addBlock(world, 4, definitions.blocks().iface().maybeStack(1).get(), reactor, true, "native block");
        addPart(player, 8, definitions.parts().iface().maybeStack(1).get(), reactor, "native part");
        addBlock(world, 12, GTNGItemList.LargeInterface.get(1), furnace, true, "large block");
        addPart(player, 16, GTNGItemList.LargeInterfacePart.get(1), reactor, "large part");
        addBlock(world, 20, GTNGItemList.StockIOInterface.get(1), reactor, true, "stock IO block");
        addPart(player, 24, GTNGItemList.StockIOInterfacePart.get(1), reactor, "stock IO part");
        addPart(player, 28, GTNGItemList.AdvancedIOBus.get(1), reactor, "advanced IO bus");
        addBlock(world, 32, definitions.blocks().iface().maybeStack(1).get(), null, false, "no target");
        addBlock(world, 36, definitions.blocks().iface().maybeStack(1).get(), reactor, false, "ambiguous targets");
        placeMachine(world, 36, 10, -1, furnace);
        ((ICustomNameObject) fixtures.get(0).host).setCustomName("QA reactor note");
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, GTNGItemList.WirelessFrequencyCard.get(1));
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(15, 14, -5, 0, 25);
    }

    private void addBlock(World world, int x, ItemStack stack, ItemStack target, boolean directed, String label) {
        require(world.setBlock(x, 10, 0, Block.getBlockFromItem(stack.getItem()), stack.getItemDamage(), 3),
            label + " placed");
        TileEntity tile = world.getTileEntity(x, 10, 0);
        if (directed && tile instanceof TileInterface iface)
            iface.setOrientation(ForgeDirection.UP, ForgeDirection.NORTH);
        if (tile instanceof TileStockIOInterface iface) iface.setTargetSide(ForgeDirection.SOUTH);
        if (target != null) placeMachine(world, x, 10, 1, target);
        fixtures.add(new Fixture(tile, tile, target, label));
    }

    private void addPart(EntityPlayerMP player, int x, ItemStack stack, ItemStack target, String label) {
        World world = player.worldObj;
        var definitions = AEApi.instance().definitions();
        require(world.setBlock(x, 10, 0, definitions.blocks().multiPart().maybeBlock().get()), label + " host placed");
        TileEntity tile = world.getTileEntity(x, 10, 0);
        IPartHost host = (IPartHost) tile;
        require(
            host.addPart(definitions.parts().cableGlass().stack(AEColor.Transparent, 1), ForgeDirection.UNKNOWN, player)
                != null,
            label + " cable placed");
        require(host.addPart(stack, ForgeDirection.SOUTH, player) != null, label + " part placed");
        Object part = host.getPart(ForgeDirection.SOUTH);
        placeMachine(world, x, 10, 1, target);
        fixtures.add(new Fixture(part, tile, target, label));
    }

    private static void placeMachine(World world, int x, int y, int z, ItemStack stack) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.disableWorking();
    }

    private void verifyTargets() {
        WirelessInterfaceView interfaces = new WirelessInterfaceView();
        for (Fixture fixture : fixtures) {
            ItemStack icon = interfaces.displayIcon(fixture.host);
            boolean fallback = fixture.label.equals("no target") || fixture.label.equals("ambiguous targets");
            require(fallback ? icon == null : sameType(icon, fixture.target), fixture.label + " target icon");
            IWirelessLinkView link = fixture.link();
            List<WirelessInterfaceView.Name> names = interfaces.names(link);
            require(fallback ? names.isEmpty() : names.size() == 1 && sameType(names.get(0).icon(), fixture.target),
                fixture.label + " target name");
            var classification = WirelessMachineGroups.classify(link, new IdentityHashMap<>(), interfaces);
            ItemStack expected = fallback ? link.node().getGridBlock().getMachineRepresentation() : fixture.target;
            require(classification.type() != null && sameType(classification.type().icon(), expected),
                fixture.label + " uses the target machine type or its own fallback type");
            require(link.target().x() == fixture.tile.xCoord && link.target().z() == fixture.tile.zCoord,
                fixture.label + " retains its device action coordinates");
        }
        var first = WirelessMachineGroups.classify(fixtures.get(0).link(), new IdentityHashMap<>(), interfaces);
        var same = WirelessMachineGroups.classify(fixtures.get(1).link(), new IdentityHashMap<>(), interfaces);
        var different = WirelessMachineGroups.classify(fixtures.get(2).link(), new IdentityHashMap<>(), interfaces);
        require(first.type().equals(same.type()), "different interface forms share the same target machine type");
        require(!first.type().equals(different.type()), "different target machines have distinct type groups");
        Set<IGridNode> mixed = Collections.newSetFromMap(new IdentityHashMap<>());
        mixed.add(fixtures.get(0).node());
        mixed.add(fixtures.get(2).node());
        Fixture fixture = fixtures.get(0);
        IWirelessLinkView mixedLink = new IWirelessLinkView.ClusterView(fixture.link().target(), fixture.node(),
            Collections.emptyMap(), false, false, "active", 1, new WirelessNetworkStatistics(2, 2, 2, 0, true), mixed);
        require(WirelessMachineGroups.classify(mixedLink, new IdentityHashMap<>(), interfaces).type() == null,
            "mixed-target physical clusters stay separate");
        require(((ICustomNameObject) fixture.host).getCustomName().equals("QA reactor note"),
            "reading target names preserves interface custom names");
    }

    private NBTTagCompound buildPacket(World world) throws Exception {
        List<IWirelessLinkView> links = new ArrayList<>();
        for (Fixture fixture : fixtures) links.add(fixture.link());
        WirelessInterfaceView interfaces = new WirelessInterfaceView();
        List<Row> rows = WirelessMachineGroups.group(links, interfaces);
        Method write = WirelessCardViewBuilder.class.getDeclaredMethod("writeRow", Row.class,
            WirelessInterfaceView.class, NBTTagCompound.class, Map.class, Map.class, boolean.class);
        write.setAccessible(true);
        NBTTagCompound dimensions = new NBTTagCompound();
        NBTTagList entries = new NBTTagList();
        WirelessCardViewBuilder builder = new WirelessCardViewBuilder();
        for (Row row : rows) entries.appendTag((NBTTagCompound) write.invoke(builder, row, interfaces, dimensions,
            new HashMap<>(), new HashMap<>(), false));
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("rows", entries);
        data.setTag("dimensions", dimensions);
        data.setLong("session", 1);
        data.setString("selectedSource", "qa");
        data.setString("source", "D" + world.provider.dimensionId + " 0, 10, 0");
        data.setBoolean("bound", true);
        data.setBoolean("sourceLoaded", true);
        data.setBoolean("channels", true);
        data.setBoolean("stable", true);
        data.setBoolean("collapseTypes", true);
        data.setInteger("capacity", 32);
        data.setInteger("used", links.size());
        data.setInteger("devices", links.size());
        data.setInteger("online", links.size());
        data.setInteger("entrances", links.size());
        data.setInteger("entranceLimit", 4096);
        return data;
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            mc.gameSettings.pauseOnLostFocus = false;
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("wireless-interface-target-" + System.currentTimeMillis(),
                    "Wireless Interface Target QA",
                    new WorldSettings(617L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (++clientTicks > 2400) throw new AssertionError("client timed out");
            if (packet == null || ModularScreen.getCurrent() == null) return;
            WirelessCardGui gui = null;
            for (var panel : ModularScreen.getCurrent().getPanelManager().getOpenPanels())
                if (panel instanceof WirelessCardGui main) gui = main;
            if (gui == null) return;
            Method accept = WirelessCardClientView.class.getDeclaredMethod("accept", NBTTagCompound.class);
            accept.setAccessible(true);
            accept.invoke(gui.container.clientView(), packet.copy());
            if (++guiTicks < 25) return;
            NBTTagList rows = gui.container.display.getTagList("rows", 10);
            int reactorCategories = 0, furnaceRows = 0;
            for (int i = 0; i < rows.tagCount(); i++) {
                NBTTagCompound row = rows.getCompoundTagAt(i);
                ItemStack icon = ItemStack.loadItemStackFromNBT(row.getCompoundTag("icon"));
                if (sameType(icon, reactor)) {
                    require(row.getBoolean("category") && row.getInteger("items") == 6,
                        "real GUI collapses six interface forms under the reactor icon");
                    reactorCategories++;
                }
                if (sameType(icon, furnace)) furnaceRows++;
            }
            require(reactorCategories == 1 && furnaceRows == 1, "real GUI shows machine identities for target groups");
            gui.container.clientView().filter("QA reactor note", "", 0);
            rows = gui.container.display.getTagList("rows", 10);
            require(rows.tagCount() == 1 && rows.getCompoundTagAt(0).getString("name").equals("QA reactor note"),
                "custom interface notes remain searchable and visible");
            gui.container.clientView().filter("", "", 0);
            ScreenShotHelper.saveScreenshot(output(), "wireless-interface-targets.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            finish("PASS");
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static boolean sameType(ItemStack first, ItemStack second) {
        return first != null && second != null && first.isItemEqual(second);
    }

    private synchronized void require(boolean valid, String description) {
        if (!valid) throw new AssertionError(description);
        checks++;
        System.out.println("WIRELESS_INTERFACE_TARGET_QA: " + description);
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        finish("FAIL: " + failure);
    }

    private static File output() {
        return new File(System.getProperty("gtng.wirelessInterfaceTarget.output"));
    }

    private synchronized void finish(String result) {
        if (finished) return;
        try {
            output().mkdirs();
            Files.write(new File(output(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        System.out.println("WIRELESS_INTERFACE_TARGET_QA: " + result + "; " + checks + " checks");
        finished = true;
    }

    private static final class Fixture {

        private final Object host;
        private final TileEntity tile;
        private final ItemStack target;
        private final String label;

        private Fixture(Object host, TileEntity tile, ItemStack target, String label) {
            this.host = host;
            this.tile = tile;
            this.target = target;
            this.label = label;
        }

        IGridNode node() {
            return ((IGridHost) host).getGridNode(ForgeDirection.UNKNOWN);
        }

        IWirelessLinkView link() {
            IGridNode node = node();
            if (node == null) throw new AssertionError(label + " has no real AE node");
            String note = host instanceof ICustomNameObject named && named.hasCustomName() ? named.getCustomName() : "";
            Address position = new Address(tile.getWorldObj().provider.dimensionId, tile.xCoord, tile.yCoord,
                tile.zCoord, host instanceof AEBasePart part ? part.getSide() : ForgeDirection.UNKNOWN);
            return new IWirelessLinkView.ClusterView(position, node, Collections.singletonMap(node, note), false, false,
                "active", 1, new WirelessNetworkStatistics(1, 1, 1, 0, true), Collections.singleton(node));
        }
    }
}
