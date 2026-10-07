package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.glodblock.github.common.parts.PartFluidInterface;
import com.glodblock.github.common.tile.TileFluidInterface;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.glodblock.github.util.DualityFluidInterface;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceGuiFactory;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceHost;
import com.xyp.gtnotgood.common.blocks.largeinterface.TileLargeInterface;
import com.xyp.gtnotgood.common.parts.largeinterface.PartLargeInterface;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartItemStack;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.DualityInterface;
import appeng.helpers.InventoryAction;
import appeng.me.helpers.AENetworkProxy;
import appeng.parts.reporting.PartInterfaceTerminal;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.storage.TileDrive;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Disposable native grids and actual synchronized screens; this fixture never enters the release jar. */
@Mod(
    modid = "largeinterfaceqa",
    name = "Large Interface QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class LargeInterfaceClientChecks {

    private boolean started;
    private volatile boolean finished;
    private volatile int stage;
    private volatile boolean clientPartReady;
    private volatile boolean clientBlockReady;
    private volatile int terminalStep;
    private volatile int terminalAcknowledged = -1;
    private volatile long terminalEntryId = -1;
    private int terminalObservedStep = -1;
    private int terminalStableFrames;
    private int ticks;
    private int seededAt;
    private int closedPartAt;
    private int terminalChangedAt;
    private int moveRegionBefore;
    private int frames;
    private int priorityEditedAt;
    private boolean priorityEdited;
    private GuiContainerWrapper partScreen;
    private ContainerInterfaceTerminal nativeTerminal;
    private TileLargeInterface block;
    private PartLargeInterface part;
    private Fixture blockFixture;
    private Fixture partFixture;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.largeInterface.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (started && stage >= 2 && mc.theWorld == null) {
            fail(new AssertionError("Large interface QA disconnected during GUI stage " + stage));
            return;
        }
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
        if (stage == 1 && mc.theWorld != null) {
            TileEntity cable = mc.theWorld.getTileEntity(0, 10, 0);
            clientPartReady = cable instanceof IPartHost host
                && host.getPart(ForgeDirection.SOUTH) instanceof PartLargeInterface;
            clientBlockReady = mc.theWorld.getTileEntity(4, 10, 0) instanceof TileLargeInterface;
        }
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "large-interface-qa-" + System.currentTimeMillis(),
                "Large Interface QA",
                new WorldSettings(37L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
                IPartHost baselineHost = verifyScopedNativeHosts(player);
                verifyPersistence(baselineHost, player);
                verifyRecipes(player.worldObj);
                setup(player);
            }
            if (ticks > 1800) throw new AssertionError(
                "Large interface QA timed out at stage " + stage
                    + ", client part loaded="
                    + clientPartReady
                    + ", client block loaded="
                    + clientBlockReady);
            if (stage == 0 && ticks >= 100 && partFixture.proxy.isActive() && blockFixture.proxy.isActive()) {
                verifyNative(partFixture, player);
                verifyNative(blockFixture, player);
                seedReturns(partFixture);
                seedReturns(blockFixture);
                seededAt = ticks;
                stage = 1;
            } else if (stage == 1 && ticks - seededAt >= 100 && clientPartReady && clientBlockReady) {
                verifyReturns(partFixture);
                verifyReturns(blockFixture);
                require(
                    clientPartReady && clientBlockReady,
                    "the real client has loaded and resolved both the cable part and full block before opening their GUI");
                LargeInterfaceGuiFactory.INSTANCE.open(player, part);
                stage = 2;
            } else if (stage == 3) {
                require(
                    part.getPriority() == 1234,
                    "part priority popup synchronizes to the authoritative native host");
                require(
                    part.getInterfaceDuality()
                        .getConfigManager()
                        .getSetting(Settings.BLOCK) != partFixture.blockingBefore,
                    "part left-side blocking control changes the native setting on the server");
                player.closeScreen();
                closedPartAt = ticks;
                stage = 6;
            } else if (stage == 6 && ticks - closedPartAt >= 20) {
                player.playerNetServerHandler.setPlayerLocation(5.5, 10, -2.5, 0, 15);
                frames = 0;
                LargeInterfaceGuiFactory.INSTANCE.open(player, block);
                stage = 4;
            } else if (stage == 5) {
                require(block.getPriority() == -13, "block priority popup preserves negative priorities on the server");
                require(
                    block.getInterfaceDuality()
                        .getConfigManager()
                        .getSetting(Settings.BLOCK) != blockFixture.blockingBefore,
                    "block left-side blocking control changes the native setting on the server");
                player.closeScreen();
                closedPartAt = ticks;
                stage = 7;
            } else if (stage == 7 && ticks - closedPartAt >= 20) {
                player.playerNetServerHandler.setPlayerLocation(1.5, 10, -2.5, 0, 15);
                part.getInterfaceDuality()
                    .getPatterns()
                    .setInventorySlotContents(0, null);
                part.getInterfaceDuality()
                    .getPatterns()
                    .setInventorySlotContents(899, null);
                require(
                    partFixture.terminal.onPartActivate(player, Vec3.createVectorHelper(0.5, 10.5, 0.5)),
                    "opens the real native interface terminal after the large interface GUI has fully closed");
                require(
                    player.openContainer instanceof ContainerInterfaceTerminal,
                    "the server opens the actual native interface terminal container");
                nativeTerminal = (ContainerInterfaceTerminal) player.openContainer;
                terminalEntryId = (Long) trackerField(terminalTracker(nativeTerminal, part), "id");
                terminalStep = 0;
                terminalChangedAt = ticks;
                stage = 8;
            } else if (stage == 8) {
                advanceTerminal(player);
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void verifyRecipes(World world) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        grid.setInventorySlotContents(0, ModsItemlist.AE2FluidCraftBlockFluidInterface.get(1));
        grid.setInventorySlotContents(8, GTNGItemList.SuperMTEHatchCraftingInputME.get(1));
        ItemStack crafted = CraftingManager.getInstance()
            .findMatchingRecipe(grid, world);
        require(
            sameType(crafted, GTNGItemList.LargeInterface.get(1)) && crafted.stackSize == 1,
            "native dual interface plus super pattern input assembly crafts one large interface");
        grid.setInventorySlotContents(0, null);
        grid.setInventorySlotContents(8, null);
        NBTTagCompound settings = new NBTTagCompound();
        settings.setInteger("priority", -37);
        settings.setTag(
            "qaNestedPattern",
            pattern(900).getTagCompound()
                .copy());
        crafted.setTagCompound(settings);
        grid.setInventorySlotContents(4, crafted);
        ItemStack panel = CraftingManager.getInstance()
            .findMatchingRecipe(grid, world);
        require(
            sameType(panel, GTNGItemList.LargeInterfacePart.get(1)) && settings.equals(panel.getTagCompound())
                && panel.getTagCompound() != settings,
            "block-to-part recipe preserves settings with independent NBT");
        grid.setInventorySlotContents(4, null);
        grid.setInventorySlotContents(8, panel);
        ItemStack roundTrip = CraftingManager.getInstance()
            .findMatchingRecipe(grid, world);
        require(
            sameType(roundTrip, crafted) && settings.equals(roundTrip.getTagCompound())
                && roundTrip.getTagCompound() != panel.getTagCompound(),
            "part-to-block recipe preserves the complete settings round trip");
        roundTrip.getTagCompound()
            .getCompoundTag("qaNestedPattern")
            .setBoolean("crafting", true);
        require(
            settings.equals(panel.getTagCompound()) && !settings.equals(roundTrip.getTagCompound()),
            "nested settings edited after conversion cannot mutate either source shape");
        for (Upgrades upgrade : Upgrades.values()) {
            int expected = upgradeMaximum(upgrade, ModsItemlist.AE2FluidCraftBlockFluidInterface.get(1));
            requireSilent(
                upgradeMaximum(upgrade, GTNGItemList.LargeInterface.get(1)) == expected
                    && upgradeMaximum(upgrade, GTNGItemList.LargeInterfacePart.get(1)) == expected,
                "large shapes differ from the native supported card count for " + upgrade);
        }
        require(true, "both shapes support exactly the native interface's registered upgrade cards and limits");
    }

    private static int upgradeMaximum(Upgrades upgrade, ItemStack machine) {
        int maximum = 0;
        for (Map.Entry<ItemStack, Integer> entry : upgrade.getSupported()
            .entrySet()) {
            if (sameType(machine, entry.getKey())) maximum = Math.max(maximum, entry.getValue());
        }
        return maximum;
    }

    private static boolean sameType(ItemStack first, ItemStack second) {
        return first != null && second != null && first.isItemEqual(second);
    }

    private static IPartHost verifyScopedNativeHosts(EntityPlayerMP player) {
        World world = player.worldObj;
        var definitions = AEApi.instance()
            .definitions();
        world.setBlock(
            10,
            10,
            0,
            definitions.blocks()
                .multiPart()
                .maybeBlock()
                .get());
        IPartHost baselineHost = (IPartHost) world.getTileEntity(10, 10, 0);
        require(
            baselineHost.addPart(
                definitions.parts()
                    .cableGlass()
                    .stack(AEColor.Transparent, 1),
                ForgeDirection.UNKNOWN,
                player) != null,
            "native baseline cable placed");
        require(
            baselineHost.addPart(new ItemStack(ItemAndBlockHolder.FLUID_INTERFACE), ForgeDirection.SOUTH, player)
                != null,
            "native baseline dual interface attached to a real part host");
        TileFluidInterface originalBlock = new TileFluidInterface();
        PartFluidInterface originalPart = (PartFluidInterface) baselineHost.getPart(ForgeDirection.SOUTH);
        for (DualityInterface duality : new DualityInterface[] { originalBlock.getInterfaceDuality(),
            originalPart.getInterfaceDuality() }) {
            require(
                duality.getPatterns()
                    .getSizeInventory() == 36,
                "ordinary native dual interfaces retain 36 pattern slots");
            duality.getConfig()
                .setInventorySlotContents(0, new ItemStack(Items.emerald));
            require(
                duality.getConfig()
                    .getStackInSlot(0) != null,
                "ordinary native item stocking configuration remains usable");
        }
        for (DualityFluidInterface duality : new DualityFluidInterface[] { originalBlock.getDualityFluid(),
            originalPart.getDualityFluid() }) {
            duality.getConfig()
                .setFluidInSlot(0, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1000)));
            require(
                duality.getConfig()
                    .getFluidInSlot(0) != null,
                "ordinary native fluid stocking configuration remains usable");
        }
        return baselineHost;
    }

    private static void verifyPersistence(IPartHost baselineHost, EntityPlayerMP player) {
        AppEngInternalInventory patterns = new AppEngInternalInventory(null, 900);
        for (int slot = 0; slot < 900; slot++) patterns.setInventorySlotContents(slot, pattern(slot + 1));
        NBTTagCompound content = new NBTTagCompound();
        patterns.writeToNBT(content, "patterns");
        content.setInteger("priority", 37);
        addForbiddenConfig(content);
        TileLargeInterface savedBlock = new TileLargeInterface();
        savedBlock.readFromNBT(content);
        PartLargeInterface savedPart = new PartLargeInterface(GTNGItemList.LargeInterfacePart.get(1));
        savedPart.setPartHostInfo(ForgeDirection.SOUTH, baselineHost, baselineHost.getTile());
        savedPart.readFromNBT(content);
        NBTTagCompound blockSave = new NBTTagCompound();
        savedBlock.writeToNBT(blockSave);
        NBTTagCompound partSave = new NBTTagCompound();
        savedPart.writeToNBT(partSave);
        TileLargeInterface loadedBlock = new TileLargeInterface();
        loadedBlock.readFromNBT(blockSave);
        PartLargeInterface loadedPart = new PartLargeInterface(GTNGItemList.LargeInterfacePart.get(1));
        loadedPart.setPartHostInfo(ForgeDirection.SOUTH, baselineHost, baselineHost.getTile());
        loadedPart.readFromNBT(partSave);
        verifyTerminalBoundaries(baselineHost);
        for (LargeInterfaceHost host : new LargeInterfaceHost[] { loadedBlock, loadedPart }) {
            DualityInterface duality = host.getInterfaceDuality();
            require(
                duality.getPatterns()
                    .getSizeInventory() == 900 && duality.getPriority() == 37,
                "block and part NBT retain 900 slots and native priority");
            for (int slot = 0; slot < 900; slot++) {
                requireSilent(
                    ItemStack.areItemStacksEqual(
                        patterns.getStackInSlot(slot),
                        duality.getPatterns()
                            .getStackInSlot(slot)),
                    "world NBT lost pattern slot " + slot);
            }
            assertNoConfig(host);
            List<ItemStack> drops = new ArrayList<>();
            duality.addDrops(drops);
            require(drops.size() == 900, "native removal drops contain every one of the 900 stored patterns");
            for (int slot = 0; slot < 900; slot++) {
                requireSilent(
                    ItemStack.areItemStacksEqual(patterns.getStackInSlot(slot), drops.get(slot)),
                    "native removal lost or changed pattern slot " + slot);
            }
        }
        verifyPartPlacement(baselineHost, player, loadedPart, content, patterns);
    }

    private static void verifyTerminalBoundaries(IPartHost baselineHost) {
        TileLargeInterface boundaryBlock = new TileLargeInterface();
        PartLargeInterface boundaryPart = new PartLargeInterface(GTNGItemList.LargeInterfacePart.get(1));
        boundaryPart.setPartHostInfo(ForgeDirection.SOUTH, baselineHost, baselineHost.getTile());
        for (LargeInterfaceHost host : new LargeInterfaceHost[] { boundaryBlock, boundaryPart }) {
            AppEngInternalInventory patterns = host.getInterfaceDuality()
                .getPatterns();
            assertTerminalShape(host, 2);
            int[] boundaries = { 0, 7, 8, 9, 16, 17, 18, 898, 899 };
            int[] rows = { 2, 2, 3, 3, 3, 4, 4, 100, 100 };
            for (int i = 0; i < boundaries.length; i++) {
                int slot = boundaries[i];
                patterns.setInventorySlotContents(slot, pattern(slot + 1));
                assertTerminalShape(host, rows[i]);
                require(
                    patterns.decrStackSize(slot, 1) != null,
                    "native slot extraction removes the occupied row boundary");
                assertTerminalShape(host, 2);
            }
            patterns.setInventorySlotContents(8, pattern(9));
            patterns.setInventorySlotContents(17, pattern(18));
            assertTerminalShape(host, 4);
            patterns.setInventorySlotContents(17, null);
            assertTerminalShape(host, 3);
            patterns.setInventorySlotContents(8, null);
            assertTerminalShape(host, 2);
            NBTTagCompound saved = new NBTTagCompound();
            AppEngInternalInventory source = new AppEngInternalInventory(null, 900);
            source.setInventorySlotContents(899, pattern(900));
            source.writeToNBT(saved, "patterns");
            patterns.readFromNBT(saved, "patterns");
            assertTerminalShape(host, 100);
            patterns.setInventorySlotContents(899, null);
            assertTerminalShape(host, 2);
            require(
                true,
                "terminal row cache follows exact super assembly boundaries, shrinkage and native high-slot NBT load");
        }
    }

    private static void assertTerminalShape(LargeInterfaceHost host, int rows) {
        requireSilent(
            host.rows() == rows && host.rowSize() == 9
                && host.numSlots() == rows * 9
                && host.getInterfaceDuality()
                    .getPatterns()
                    .getSizeInventory() == 900,
            "terminal shape expected " + rows
                + "x9 visible slots with a 900-slot backing inventory, got "
                + host.rows()
                + "x"
                + host.rowSize()
                + "/"
                + host.numSlots());
    }

    private static Object terminalTracker(ContainerInterfaceTerminal terminal, LargeInterfaceHost host)
        throws Exception {
        Field tracked = ContainerInterfaceTerminal.class.getDeclaredField("tracked");
        tracked.setAccessible(true);
        Object entry = ((Map<?, ?>) tracked.get(terminal)).get(host);
        requireSilent(entry != null, "native interface terminal must track the actual placed large host");
        return entry;
    }

    private void advanceTerminal(EntityPlayerMP player) throws Exception {
        if (ticks - terminalChangedAt > 200) throw new AssertionError(
            "Native terminal did not acknowledge step " + terminalStep
                + " after 200 server ticks; client acknowledged "
                + terminalAcknowledged);
        if (terminalAcknowledged != terminalStep || ticks - terminalChangedAt < 10) return;
        AppEngInternalInventory patterns = part.getInterfaceDuality()
            .getPatterns();
        if (terminalStep == 0) {
            player.inventory.setItemStack(pattern(9));
            nativeTerminal.doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, 8, terminalEntryId);
            require(
                ItemStack.areItemStacksEqual(patterns.getStackInSlot(8), pattern(9))
                    && player.inventory.getItemStack() == null,
                "native terminal insertion into slot eight expands the exact super assembly row boundary");
        } else if (terminalStep == 1) {
            patterns.setInventorySlotContents(17, pattern(18));
        } else if (terminalStep == 2) {
            nativeTerminal.doAction(player, InventoryAction.SHIFT_CLICK, 8, terminalEntryId);
            require(
                patterns.getStackInSlot(8) == null,
                "native terminal extraction removes slot eight without an artificial refresh request");
            patterns.setInventorySlotContents(17, null);
        } else if (terminalStep == 3) {
            patterns.setInventorySlotContents(899, pattern(900));
            assertStaleMoveRegionRejected(player, patterns);
        } else if (terminalStep == 4) {
            if (patterns.getStackInSlot(899) != null || player.inventory.getItemStack() == null) return;
            require(
                ItemStack.areItemStacksEqual(player.inventory.getItemStack(), pattern(900)),
                "a real native terminal client packet retrieves the 900th encoded pattern");
        } else if (terminalStep == 5) {
            nativeTerminal.doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, 899, terminalEntryId);
            require(
                patterns.getStackInSlot(899) == null
                    && ItemStack.areItemStacksEqual(player.inventory.getItemStack(), pattern(900)),
                "collapsed terminal capacity rejects an upload beyond its currently visible slot range");
            patterns.setInventorySlotContents(898, pattern(899));
        } else if (terminalStep == 6) {
            if (patterns.getStackInSlot(899) == null || player.inventory.getItemStack() != null) return;
            require(
                ItemStack.areItemStacksEqual(patterns.getStackInSlot(899), pattern(900)),
                "a real native terminal client packet uploads back to the expanded 900th slot");
        } else if (terminalStep == 7) {
            patterns.setInventorySlotContents(898, null);
            patterns.setInventorySlotContents(899, null);
        } else if (terminalStep == 8) {
            patterns.setInventorySlotContents(899, pattern(900));
            moveRegionBefore = patternCount(player, pattern(900));
            assertStaleMoveRegionRejected(player, patterns);
        } else if (terminalStep == 9) {
            if (patterns.getStackInSlot(899) != null) return;
            require(
                patternCount(player, pattern(900)) == moveRegionBefore + 1,
                "a native MOVE_REGION client packet after the full size update transfers the last pattern exactly once");
        } else {
            assertTerminalShape(part, 2);
            require(
                true,
                "one real native terminal session grows, shrinks, caps at 100 rows and transfers the last pattern safely");
            finish("PASS");
            return;
        }
        terminalStep++;
        terminalChangedAt = ticks;
    }

    private void assertStaleMoveRegionRejected(EntityPlayerMP player, AppEngInternalInventory patterns)
        throws Exception {
        require(
            (Integer) trackerField(terminalTracker(nativeTerminal, part), "numSlots") == 18,
            "MOVE_REGION regression starts with an actual two-row native terminal snapshot");
        int before = patternCount(player, pattern(900));
        nativeTerminal.doAction(player, InventoryAction.MOVE_REGION, 0, terminalEntryId);
        require(
            ItemStack.areItemStacksEqual(patterns.getStackInSlot(899), pattern(900))
                && patternCount(player, pattern(900)) == before
                && player.inventory.getItemStack() == null,
            "stale MOVE_REGION is rejected before hidden slot 899 can mutate inventory or emit an out-of-range update");
    }

    private static int patternCount(EntityPlayerMP player, ItemStack pattern) {
        int amount = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (Platform.isSameItemPrecise(stack, pattern)) amount += stack.stackSize;
        }
        return amount;
    }

    private static void verifyPartPlacement(IPartHost baselineHost, EntityPlayerMP player, PartLargeInterface source,
        NBTTagCompound content, AppEngInternalInventory patterns) {
        source.getConfigManager()
            .putSetting(Settings.BLOCK, YesNo.YES);
        NBTTagCompound settings = source.downloadSettings(SettingsFrom.DISMANTLE_ITEM);
        addForbiddenConfig(settings);
        ItemStack held = GTNGItemList.LargeInterfacePart.get(1);
        held.setTagCompound((NBTTagCompound) settings.copy());
        held.setStackDisplayName("Large Interface Placement QA");
        PartLargeInterface placed = new PartLargeInterface(held.copy());
        placed.setPartHostInfo(ForgeDirection.SOUTH, baselineHost, baselineHost.getTile());
        placed.onPlacement(player, held, ForgeDirection.SOUTH);
        require(
            placed.getPriority() == 37 && placed.getConfigManager()
                .getSetting(Settings.BLOCK) == YesNo.YES,
            "placing a settings-bearing part restores native priority and non-default blocking mode");
        assertNoConfig(placed);
        placed.getInterfaceDuality()
            .getPatterns()
            .readFromNBT(content, "patterns");
        placed.setPriority(1248);
        ItemStack portable = placed.getItemStack(PartItemStack.Wrench);
        require(
            portable.getTagCompound() != held.getTagCompound() && portable.getTagCompound()
                .getInteger("priority") == 1248
                && portable.getTagCompound()
                    .getString(Settings.BLOCK.name())
                    .equals(YesNo.YES.name()),
            "wrenching snapshots the current native settings instead of the original placement item NBT");
        require(
            !portable.getTagCompound()
                .hasKey("patterns")
                && !portable.getTagCompound()
                    .hasKey("upgrades")
                && !portable.getTagCompound()
                    .hasKey("config")
                && !portable.getTagCompound()
                    .hasKey("ConfigInv"),
            "portable settings contain no separate pattern, upgrade or forbidden stocking inventories");
        portable.getTagCompound()
            .setInteger("priority", -999);
        portable.getTagCompound()
            .getCompoundTag("display")
            .setString("Name", "Changed snapshot");
        ItemStack freshSnapshot = placed.getItemStack(PartItemStack.Wrench);
        require(
            placed.getPriority() == 1248 && held.getTagCompound()
                .getInteger("priority") == 37
                && freshSnapshot.getTagCompound()
                    .getInteger("priority") == 1248
                && freshSnapshot.getDisplayName()
                    .equals("Large Interface Placement QA"),
            "editing a wrench snapshot cannot mutate the live host, original placement item or later snapshot");
        List<ItemStack> drops = new ArrayList<>();
        placed.getDrops(drops, true);
        require(
            drops.size() == 900,
            "wrench removal keeps all 900 patterns in exactly one native set of separate drops");
        for (int slot = 0; slot < 900; slot++) {
            requireSilent(
                ItemStack.areItemStacksEqual(patterns.getStackInSlot(slot), drops.get(slot)),
                "wrenched part lost or duplicated pattern slot " + slot);
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        var definitions = AEApi.instance()
            .definitions();
        world.setBlock(
            0,
            10,
            0,
            definitions.blocks()
                .multiPart()
                .maybeBlock()
                .get());
        IPartHost host = (IPartHost) world.getTileEntity(0, 10, 0);
        require(
            host.addPart(
                definitions.parts()
                    .cableGlass()
                    .stack(AEColor.Transparent, 1),
                ForgeDirection.UNKNOWN,
                player) != null,
            "native AE cable placed");
        require(
            host.addPart(GTNGItemList.LargeInterfacePart.get(1), ForgeDirection.SOUTH, player) != null,
            "large dual interface uses native cable-part placement");
        part = (PartLargeInterface) host.getPart(ForgeDirection.SOUTH);
        require(
            host.addPart(
                definitions.parts()
                    .interfaceTerminal()
                    .maybeStack(1)
                    .get(),
                ForgeDirection.NORTH,
                player) != null,
            "native interface terminal attached to the part grid");
        PartInterfaceTerminal partTerminal = (PartInterfaceTerminal) host.getPart(ForgeDirection.NORTH);
        world.setBlock(
            4,
            10,
            0,
            Block.getBlockFromItem(
                GTNGItemList.LargeInterface.get(1)
                    .getItem()));
        block = (TileLargeInterface) world.getTileEntity(4, 10, 0);
        block.getProxy()
            .setOwner(player);
        partFixture = fixture(player, 0, part, partTerminal);
        blockFixture = fixture(player, 4, block, null);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(1.5, 10, -2.5, 0, 15);
    }

    private static Fixture fixture(EntityPlayerMP player, int x, LargeInterfaceHost host,
        PartInterfaceTerminal terminal) {
        World world = player.worldObj;
        var definitions = AEApi.instance()
            .definitions();
        world.setBlock(
            x - 1,
            10,
            0,
            definitions.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        world.setBlock(
            x + 1,
            10,
            0,
            definitions.blocks()
                .drive()
                .maybeBlock()
                .get());
        TileDrive drive = (TileDrive) world.getTileEntity(x + 1, 10, 0);
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                definitions.items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
        drive.getInternalInventory()
            .setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
        if (terminal == null) {
            world.setBlock(
                x,
                10,
                -1,
                definitions.blocks()
                    .multiPart()
                    .maybeBlock()
                    .get());
            IPartHost anchorHost = (IPartHost) world.getTileEntity(x, 10, -1);
            anchorHost.addPart(
                definitions.parts()
                    .cableGlass()
                    .stack(AEColor.Transparent, 1),
                ForgeDirection.UNKNOWN,
                player);
            anchorHost.addPart(
                definitions.parts()
                    .interfaceTerminal()
                    .maybeStack(1)
                    .get(),
                ForgeDirection.NORTH,
                player);
            terminal = (PartInterfaceTerminal) anchorHost.getPart(ForgeDirection.NORTH);
        }
        return new Fixture(host, terminal, new MachineSource(drive));
    }

    private static void verifyNative(Fixture fixture, EntityPlayerMP player) throws Exception {
        DualityInterface duality = fixture.host.getInterfaceDuality();
        require(
            duality.getPatterns()
                .getSizeInventory() == 900
                && duality.getUpgrades()
                    .getSizeInventory() == 4,
            "native large host exposes 900 patterns and the original four upgrade slots");
        duality.getPatterns()
            .setInventorySlotContents(0, pattern(1));
        duality.getPatterns()
            .setInventorySlotContents(899, pattern(900));
        duality.setPriority(37);
        List<ICraftingPatternDetails> offered = new ArrayList<>();
        duality.provideCrafting(
            (ICraftingProviderHelper) Proxy.newProxyInstance(
                ICraftingProviderHelper.class.getClassLoader(),
                new Class<?>[] { ICraftingProviderHelper.class },
                (proxy, method, args) -> {
                    if (method.getName()
                        .equals("addCraftingOption")) offered.add((ICraftingPatternDetails) args[1]);
                    return null;
                }));
        require(
            offered.size() == 2,
            "native crafting provider registers patterns at both the first and the 900th slot");
        ICraftingPatternDetails last = null;
        for (ICraftingPatternDetails detail : offered) {
            if (ItemStack.areItemStacksEqual(
                detail.getPattern(),
                duality.getPatterns()
                    .getStackInSlot(899)))
                last = detail;
        }
        require(
            last != null && last.getPriority() == 899 - 900 * 37,
            "native priority ordering spans all 900 slots without crossing the next interface's priority");
        boolean nativeFluid = false;
        for (IAEStack<?> input : last.getAEInputs()) {
            if (input instanceof IAEFluidStack fluid && fluid.getFluid() == FluidRegistry.WATER) nativeFluid = true;
        }
        require(nativeFluid, "last-slot Ultimate Encoded Pattern contains native AE fluid NBT");
        duality.getPatterns()
            .setInventorySlotContents(898, pattern(899));
        ContainerInterfaceTerminal terminal = new ContainerInterfaceTerminal(player.inventory, fixture.terminal);
        Field tracked = ContainerInterfaceTerminal.class.getDeclaredField("tracked");
        tracked.setAccessible(true);
        Object entry = ((Map<?, ?>) tracked.get(terminal)).get(fixture.host);
        require(
            entry != null && (Integer) trackerField(entry, "numSlots") == 900
                && (Integer) trackerField(entry, "rowSize") == 9
                && (Boolean) trackerField(entry, "shouldDisplay"),
            "native interface terminal discovers the large host and all 900 slots");
        long id = (Long) trackerField(entry, "id");
        terminal.doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, 899, id);
        require(
            duality.getPatterns()
                .getStackInSlot(899) == null
                && ItemStack.areItemStacksEqual(player.inventory.getItemStack(), pattern(900)),
            "native interface terminal retrieves the 900th encoded pattern");
        terminal.doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, 899, id);
        require(
            player.inventory.getItemStack() == null && ItemStack.areItemStacksEqual(
                duality.getPatterns()
                    .getStackInSlot(899),
                pattern(900)),
            "native interface terminal uploads back to the 900th slot");
        terminal.onContainerClosed(player);
        duality.getPatterns()
            .setInventorySlotContents(898, null);
        NBTTagCompound memoryCard = new NBTTagCompound();
        memoryCard.setInteger("priority", 37);
        addForbiddenConfig(memoryCard);
        fixture.upload(memoryCard);
        duality.getConfig()
            .setInventorySlotContents(0, new ItemStack(Items.emerald, 32));
        fixture.fluidDuality()
            .getConfig()
            .setFluidInSlot(0, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 2000)));
        fixture.setFluidConfig(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 2000)));
        duality.setHasConfig(true);
        assertNoConfig(fixture.host);
    }

    private static Object trackerField(Object entry, String name) throws Exception {
        Field field = entry.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(entry);
    }

    private static void addForbiddenConfig(NBTTagCompound target) {
        TileFluidInterface original = new TileFluidInterface();
        original.getInterfaceDuality()
            .getConfig()
            .setInventorySlotContents(0, new ItemStack(Items.emerald, 32));
        original.setConfig(0, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 2000)));
        NBTTagCompound nativeConfig = new NBTTagCompound();
        original.writeToNBT(nativeConfig);
        require(
            nativeConfig.hasKey("config") && nativeConfig.hasKey("ConfigInv"),
            "legacy stocking fixture uses native item/fluid NBT");
        target.setTag(
            "config",
            nativeConfig.getTag("config")
                .copy());
        target.setTag(
            "ConfigInv",
            nativeConfig.getTag("ConfigInv")
                .copy());
    }

    private static void assertNoConfig(LargeInterfaceHost host) {
        DualityInterface items = host.getInterfaceDuality();
        DualityFluidInterface fluids = host instanceof TileFluidInterface tile ? tile.getDualityFluid()
            : ((PartFluidInterface) host).getDualityFluid();
        for (int slot = 0; slot < items.getConfig()
            .getSizeInventory(); slot++) {
            requireSilent(
                items.getConfig()
                    .getStackInSlot(slot) == null,
                "forbidden item request survived at " + slot);
        }
        for (int slot = 0; slot < fluids.getConfig()
            .getSlots(); slot++) {
            requireSilent(
                fluids.getConfig()
                    .getFluidInSlot(slot) == null,
                "forbidden fluid request survived at " + slot);
        }
        require(
            !items.hasConfig(),
            "direct setters, loaded NBT and memory-card settings cannot enable network stocking");
    }

    private static void seedReturns(Fixture fixture) throws Exception {
        var items = fixture.proxy.getStorage()
            .getItemInventory();
        var fluids = fixture.proxy.getStorage()
            .getFluidInventory();
        require(
            items.injectItems(AEItemStack.create(new ItemStack(Items.emerald, 64)), Actionable.MODULATE, fixture.source)
                == null,
            "network item seed accepted");
        require(
            fluids.injectItems(
                AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 4000)),
                Actionable.MODULATE,
                fixture.source) == null,
            "network fluid seed accepted");
        require(
            fixture.host.getInterfaceDuality()
                .getItemInventory()
                .injectItems(AEItemStack.create(new ItemStack(Items.apple, 11)), Actionable.MODULATE, fixture.source)
                == null,
            "external item output enters the native interface monitor");
        require(
            fixture.fill(new FluidStack(FluidRegistry.WATER, 500)) == 500,
            "external fluid output enters the native IFluidHandler");
        fixture.host.getInterfaceDuality()
            .getStorage()
            .setInventorySlotContents(0, new ItemStack(Items.diamond, 7));
        fixture.fluidDuality()
            .getTanks()
            .setFluidInSlot(0, AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1000)));
    }

    private static void verifyReturns(Fixture fixture) throws Exception {
        require(
            fixture.items(Items.apple) == 11 && fixture.items(Items.diamond) == 7,
            "direct item output and real buffered item output both return completely to AE");
        require(
            fixture.fluid(FluidRegistry.WATER) == 4500 && fixture.fluid(FluidRegistry.LAVA) == 1000,
            "direct fluid output and buffered fluid output both return completely to AE");
        require(fixture.items(Items.emerald) == 64, "forbidden stocking requests never extract matching network items");
        for (int slot = 0; slot < fixture.host.getInterfaceDuality()
            .getStorage()
            .getSizeInventory(); slot++) {
            requireSilent(
                fixture.host.getInterfaceDuality()
                    .getStorage()
                    .getStackInSlot(slot) == null,
                "item storage did not empty, or extracted stock at " + slot);
        }
        for (int slot = 0; slot < fixture.fluidDuality()
            .getTanks()
            .getSlots(); slot++) {
            requireSilent(
                fixture.fluidDuality()
                    .getTanks()
                    .getFluidInSlot(slot) == null,
                "fluid storage did not empty, or extracted stock at " + slot);
        }
        require(true, "both real return buffers empty without pulling network stock");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (stage == 8) {
            try {
                renderTerminal(mc);
            } catch (Throwable failure) {
                fail(failure);
            }
            return;
        }
        if (stage != 2 && stage != 4) return;
        if (!(mc.currentScreen instanceof GuiContainerWrapper gui)) return;
        if (stage == 4 && gui == partScreen) return;
        try {
            ModularPanel panel = gui.getScreen()
                .getMainPanel();
            if (!panel.getName()
                .equals("large_interface")) return;
            frames++;
            if (frames == 1) {
                priorityEdited = false;
                if (stage == 2) partScreen = gui;
            }
            String variant = stage == 2 ? "part" : "block";
            if (frames == 40) {
                List<Grid> grids = collect(panel, Grid.class);
                require(grids.size() == 1, "GUI contains only the central pattern grid");
                Grid patterns = grids.get(0);
                List<ItemSlot> slots = collect(patterns, ItemSlot.class);
                require(
                    slots.size() == 900 && patterns.getArea()
                        .h() == 72,
                    "GUI synchronizes all 900 real pattern slots in the four-row viewport");
                require(
                    slots.get(0)
                        .getSlot()
                        .getStack() != null
                        && slots.get(899)
                            .getSlot()
                            .getStack() != null,
                    "both first and final native encoded patterns synchronize to the client");
                require(
                    collect(panel, PhantomItemSlot.class).isEmpty() && collect(panel, FluidSlot.class).isEmpty(),
                    "GUI contains no stocking marks, virtual item slots or virtual fluid slots");
                require(
                    collect(panel, ItemSlot.class).size() == 940,
                    "GUI contains exactly patterns, four upgrades and player inventory");
                for (String name : new String[] { "block", "interface_terminal", "insertion_mode",
                    "pattern_optimization" }) {
                    IWidget button = named(panel, name);
                    require(
                        button.getArea()
                            .x()
                            < panel.getArea()
                                .x(),
                        "native setting remains on the left: " + name);
                }
                for (String name : new String[] { "manual_slots_panel", "pattern_circuits", "itemPolicy",
                    "fluidPolicy" }) {
                    require(
                        gui.getScreen()
                            .getSyncManager()
                            .getMainPSM()
                            .findPanelHandlerNullable(name) == null,
                        "removed hatch controls do not create hidden popup handlers: " + name);
                }
                screenshot("large-interface-" + variant + "-first.png");
                var scroll = patterns.getScrollArea();
                scroll.getScrollY()
                    .scrollTo(scroll, Integer.MAX_VALUE);
            }
            if (frames == 60) {
                Grid patterns = collect(panel, Grid.class).get(0);
                ItemSlot last = collect(patterns, ItemSlot.class).get(899);
                int lastY = last.getArea()
                    .y() - patterns.getScrollY();
                require(
                    lastY >= patterns.getArea()
                        .y()
                        && lastY + last.getArea()
                            .h()
                            <= patterns.getArea()
                                .y()
                                + patterns.getArea()
                                    .h(),
                    "scroll reaches the fully visible 900th slot");
                screenshot("large-interface-" + variant + "-last.png");
                ((Interactable) named(panel, "block")).onMousePressed(0);
                ((Interactable) named(panel, "priority")).onMousePressed(0);
            }
            if (frames >= 100 && !priorityEdited) {
                ModularPanel priority = findPopup(gui, "large_interface_priority");
                if (priority == null) {
                    if (frames >= 400)
                        throw new AssertionError("Priority popup did not open after 340 rendered frames");
                    return;
                }
                List<TextFieldWidget> fields = collect(priority, TextFieldWidget.class);
                require(fields.size() == 1, "original interface priority button opens its synchronized value editor");
                fields.get(0)
                    .setText(stage == 2 ? "1234" : "-13");
                fields.get(0)
                    .onRemoveFocus(priority.getContext());
                priorityEdited = true;
                priorityEditedAt = frames;
            }
            if (priorityEdited && frames - priorityEditedAt >= 40) {
                ModularPanel priority = popup(gui, "large_interface_priority");
                require(
                    Integer.parseInt(
                        collect(priority, TextFieldWidget.class).get(0)
                            .getText())
                        == (stage == 2 ? 1234 : -13),
                    "priority editor retains the edited positive or negative integer");
                screenshot("large-interface-" + variant + "-priority.png");
                priority.closeIfOpen();
                stage = stage == 2 ? 3 : 5;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void renderTerminal(Minecraft mc) throws Exception {
        if (!(mc.currentScreen instanceof GuiInterfaceTerminal terminal) || terminalEntryId < 0) return;
        int step = terminalStep;
        if (terminalAcknowledged == step) return;
        if (terminalObservedStep != step) {
            terminalObservedStep = step;
            terminalStableFrames = 0;
        }
        Object master = trackerField(terminal, "masterList");
        Object entry = ((Map<?, ?>) trackerField(master, "list")).get(terminalEntryId);
        if (entry == null) return;
        int rows = new int[] { 2, 3, 4, 2, 100, 2, 100, 100, 2, 100, 2 }[step];
        AppEngInternalInventory inventory = (AppEngInternalInventory) trackerField(entry, "inv");
        if ((Integer) trackerField(entry, "rows") != rows || (Integer) trackerField(entry, "rowSize") != 9
            || (Integer) trackerField(entry, "numSlots") != rows * 9
            || inventory.getSizeInventory() != rows * 9
            || !terminalContentsReady(inventory, step, mc.thePlayer.inventory.getItemStack())) {
            terminalStableFrames = 0;
            return;
        }
        if (++terminalStableFrames < 8) return;
        require(
            ((Boolean[]) trackerField(entry, "brokenRecipes")).length == rows * 9
                && ((Boolean[]) trackerField(entry, "useSubstitute")).length == rows * 9
                && ((boolean[]) trackerField(entry, "filteredRecipes")).length == rows * 9,
            "real native ADD/OVERWRITE packets synchronize row count, inventory and all recipe metadata arrays at step "
                + step);
        require(
            (Boolean) trackerField(entry, "terminalVisible") && (Boolean) trackerField(entry, "online"),
            "the real large interface terminal entry remains visible and online while its rows change");
        Field scrollField = AEBaseGui.class.getDeclaredField("scrollBar");
        scrollField.setAccessible(true);
        GuiScrollbar scrollbar = (GuiScrollbar) scrollField.get(terminal);
        scrollbar.setCurrentScroll(rows == 100 ? Integer.MAX_VALUE : 0);
        terminal.drawScreen(-10000, -10000, 0);
        if (rows == 100) {
            int lastRowY = (Integer) trackerField(entry, "dispY") + 99 * 18;
            int viewport = (Integer) trackerField(terminal, "viewHeight");
            require(
                scrollbar.getCurrentScroll() > 0 && lastRowY >= 0 && lastRowY + 18 <= viewport,
                "the real terminal scrolls to and renders its fully visible 100th row");
        }
        screenshot("large-interface-terminal-step-" + step + "-rows-" + rows + ".png");
        require(
            true,
            "actual client terminal shape is " + rows
                + " rows and "
                + inventory.getSizeInventory()
                + " slots at step "
                + step);
        terminalAcknowledged = step;
        if (step == 4 || step == 6) {
            NetworkHandler.instance
                .sendToServer(new PacketInventoryAction(InventoryAction.PICKUP_OR_SET_DOWN, 899, terminalEntryId));
        } else if (step == 9) {
            NetworkHandler.instance
                .sendToServer(new PacketInventoryAction(InventoryAction.MOVE_REGION, 0, terminalEntryId));
        }
    }

    private static boolean terminalContentsReady(AppEngInternalInventory inventory, int step, ItemStack cursor) {
        if (step == 0 || step == 3 || step == 5 || step == 8 || step == 10) {
            for (ItemStack stack : inventory) if (stack != null) return false;
            return step == 5 ? ItemStack.areItemStacksEqual(cursor, pattern(900)) : cursor == null;
        }
        if (step == 1) return ItemStack.areItemStacksEqual(inventory.getStackInSlot(8), pattern(9));
        if (step == 2) return ItemStack.areItemStacksEqual(inventory.getStackInSlot(8), pattern(9))
            && ItemStack.areItemStacksEqual(inventory.getStackInSlot(17), pattern(18));
        if (step == 4 || step == 9)
            return ItemStack.areItemStacksEqual(inventory.getStackInSlot(899), pattern(900)) && cursor == null;
        if (step == 6) return ItemStack.areItemStacksEqual(inventory.getStackInSlot(898), pattern(899))
            && inventory.getStackInSlot(899) == null
            && ItemStack.areItemStacksEqual(cursor, pattern(900));
        return ItemStack.areItemStacksEqual(inventory.getStackInSlot(898), pattern(899))
            && ItemStack.areItemStacksEqual(inventory.getStackInSlot(899), pattern(900))
            && cursor == null;
    }

    private static ModularPanel popup(GuiContainerWrapper gui, String name) {
        ModularPanel result = findPopup(gui, name);
        if (result != null) return result;
        throw new AssertionError("Expected open popup " + name);
    }

    private static ModularPanel findPopup(GuiContainerWrapper gui, String name) {
        for (ModularPanel panel : gui.getScreen()
            .getPanelManager()
            .getOpenPanels()) {
            if (panel.getName()
                .equals(name)) return panel;
        }
        return null;
    }

    private static IWidget named(IWidget root, String name) {
        if (root.isName(name)) return root;
        for (IWidget child : root.getChildren()) {
            IWidget result = findNamed(child, name);
            if (result != null) return result;
        }
        throw new AssertionError("Missing widget " + name);
    }

    private static IWidget findNamed(IWidget root, String name) {
        if (root.isName(name)) return root;
        for (IWidget child : root.getChildren()) {
            IWidget result = findNamed(child, name);
            if (result != null) return result;
        }
        return null;
    }

    private static <T> List<T> collect(IWidget root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        collect(root, type, matches);
        return matches;
    }

    private static <T> void collect(IWidget root, Class<T> type, List<T> matches) {
        if (type.isInstance(root)) matches.add(type.cast(root));
        for (IWidget child : root.getChildren()) collect(child, type, matches);
    }

    private static ItemStack pattern(int amount) {
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagList inputs = new NBTTagList();
        NBTTagList outputs = new NBTTagList();
        for (IAEStack<?> input : new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.nether_star))
            .setStackSize(amount), AEFluidStack.create(new FluidStack(FluidRegistry.WATER, amount)) }) {
            NBTTagCompound entry = new NBTTagCompound();
            input.writeToNBTGeneric(entry);
            inputs.appendTag(entry);
        }
        NBTTagCompound output = new NBTTagCompound();
        AEItemStack.create(new ItemStack(Items.apple))
            .writeToNBTGeneric(output);
        outputs.appendTag(output);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        tag.setBoolean("beSubstitute", true);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private static File outputDirectory() {
        return new File(System.getProperty("gtng.largeInterface.qa.output", "."));
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper
            .saveScreenshot(outputDirectory(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void require(boolean condition, String message) {
        requireSilent(condition, message);
        System.out.println("LARGE_INTERFACE_QA: " + message);
    }

    private static void requireSilent(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        finish("FAIL");
    }

    private void finish(String result) {
        finished = true;
        try {
            Files.write(new File(outputDirectory(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        System.out.println("LARGE_INTERFACE_QA: " + result);
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static final class Fixture {

        private final LargeInterfaceHost host;
        private final PartInterfaceTerminal terminal;
        private final AENetworkProxy proxy;
        private final MachineSource source;
        private final Enum<?> blockingBefore;

        private Fixture(LargeInterfaceHost host, PartInterfaceTerminal terminal, MachineSource source) {
            this.host = host;
            this.terminal = terminal;
            this.proxy = host.getInterfaceDuality()
                .getProxy();
            this.source = source;
            this.blockingBefore = host.getInterfaceDuality()
                .getConfigManager()
                .getSetting(Settings.BLOCK);
        }

        private DualityFluidInterface fluidDuality() {
            return host instanceof TileFluidInterface tile ? tile.getDualityFluid()
                : ((PartFluidInterface) host).getDualityFluid();
        }

        private void upload(NBTTagCompound tag) {
            if (host instanceof TileFluidInterface tile) tile.uploadSettings(SettingsFrom.MEMORY_CARD, tag);
            else((PartFluidInterface) host).uploadSettings(SettingsFrom.MEMORY_CARD, tag);
        }

        private void setFluidConfig(IAEFluidStack fluid) {
            if (host instanceof TileFluidInterface tile) tile.setConfig(0, fluid);
            else((PartFluidInterface) host).setConfig(0, fluid);
        }

        private int fill(FluidStack stack) {
            return host instanceof TileFluidInterface tile ? tile.fill(ForgeDirection.WEST, stack, true)
                : ((PartFluidInterface) host).fill(ForgeDirection.WEST, stack, true);
        }

        private long items(Item item) throws Exception {
            var request = AEItemStack.create(new ItemStack(item))
                .setStackSize(Long.MAX_VALUE);
            var stack = proxy.getStorage()
                .getItemInventory()
                .extractItems(request, Actionable.SIMULATE, source);
            return stack == null ? 0 : stack.getStackSize();
        }

        private long fluid(Fluid type) throws Exception {
            var request = AEFluidStack.create(new FluidStack(type, 1))
                .setStackSize(Long.MAX_VALUE);
            var stack = proxy.getStorage()
                .getFluidInventory()
                .extractItems(request, Actionable.SIMULATE, source);
            return stack == null ? 0 : stack.getStackSize();
        }
    }
}
