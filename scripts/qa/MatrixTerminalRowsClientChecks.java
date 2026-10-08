package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.machines.multiblock.AssemblerMatrix;
import com.xyp.gtnotgood.common.machines.multiblock.AssemblerMatrix.CombinationPatternsIInventory;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.parts.reporting.PartInterfaceTerminal;
import appeng.tile.inventory.AppEngInternalInventory;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/** Verifies matrix row boundaries and native terminal synchronization in one disposable client session. */
@Mod(
    modid = "matrixterminalrowsqa",
    name = "Matrix Terminal Rows QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class MatrixTerminalRowsClientChecks {

    private static final int[] EXPECTED_ROWS = { 2, 3, 4, 4, 16, 4, 3, 2 };
    private boolean started;
    private volatile boolean finished;
    private volatile boolean clientReady;
    private volatile boolean terminalOpen;
    private volatile int terminalStep;
    private volatile int terminalAcknowledged = -1;
    private volatile long terminalEntryId = -1;
    private int observedStep = -1;
    private int stableFrames;
    private int ticks;
    private int changedAt;
    private AssemblerMatrix matrix;
    private PartInterfaceTerminal terminalPart;
    private ContainerInterfaceTerminal nativeTerminal;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.matrixTerminalRows.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (mc.theWorld != null) {
            clientReady = mc.theWorld.getTileEntity(0, 10, 0) instanceof BaseMetaTileEntity
                && mc.theWorld.getTileEntity(1, 10, 1) instanceof IPartHost host
                && host.getPart(ForgeDirection.NORTH) instanceof PartInterfaceTerminal;
        }
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "matrix-terminal-rows-qa-" + System.currentTimeMillis(),
                "Matrix Terminal Rows QA",
                new WorldSettings(41L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
            if (ticks == 1) setup(player);
            if (ticks > 1800) throw new AssertionError("QA timeout at terminal step " + terminalStep);
            if (!terminalOpen) {
                if (ticks < 100 || !clientReady
                    || !matrix.getProxy()
                        .isActive())
                    return;
                require(matrix.checkStructure(true, matrix.getBaseMetaTileEntity()), "actual matrix structure forms");
                verifyBoundariesAndPersistence();
                require(
                    terminalPart.onPartActivate(player, Vec3.createVectorHelper(1.5, 10.5, 1.5)),
                    "opens the actual native interface terminal");
                require(
                    player.openContainer instanceof ContainerInterfaceTerminal,
                    "server uses the native interface terminal container");
                nativeTerminal = (ContainerInterfaceTerminal) player.openContainer;
                terminalEntryId = (Long) field(terminalTracker(), "id");
                changedAt = ticks;
                terminalOpen = true;
                return;
            }
            if (ticks - changedAt > 400) throw new AssertionError(
                "Terminal step " + terminalStep
                    + " failed to synchronize; client acknowledged "
                    + terminalAcknowledged);
            if (terminalAcknowledged != terminalStep || ticks - changedAt < 10) return;
            require(player.openContainer == nativeTerminal, "same terminal remains open at step " + terminalStep);
            if (terminalStep != 4) {
                assertShape(EXPECTED_ROWS[terminalStep]);
                require(
                    (Integer) field(terminalTracker(), "numSlots") == matrix.numSlots(),
                    "native server tracker reflects the synchronized visible capacity");
            }
            IInventory patterns = matrix.getPatterns();
            if (terminalStep == 0) {
                player.inventory.setItemStack(pattern(8));
                nativeTerminal.doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, 8, terminalEntryId);
                require(
                    ItemStack.areItemStacksEqual(patterns.getStackInSlot(8), pattern(8))
                        && player.inventory.getItemStack() == null,
                    "native terminal insertion expands from two to three rows");
            } else if (terminalStep == 1) {
                patterns.setInventorySlotContents(17, pattern(17));
            } else if (terminalStep == 2) {
                player.inventory.setItemStack(pattern(18));
                nativeTerminal.doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, 18, terminalEntryId);
                require(
                    ItemStack.areItemStacksEqual(patterns.getStackInSlot(18), pattern(18))
                        && player.inventory.getItemStack() == null,
                    "native terminal insertion synchronizes slot eighteen without changing four visible rows");
            } else if (terminalStep == 3) {
                patterns.setInventorySlotContents(143, pattern(143));
            } else if (terminalStep == 4) {
                if (patterns.getStackInSlot(143) != null || player.inventory.getItemStack() == null) return;
                assertShape(4);
                require(
                    ItemStack.areItemStacksEqual(player.inventory.getItemStack(), pattern(143)),
                    "actual native client packet retrieves the last physical pattern slot");
                player.inventory.addItemStackToInventory(player.inventory.getItemStack());
                player.inventory.setItemStack(null);
            } else if (terminalStep == 5) {
                require(patterns.decrStackSize(18, 1) != null, "native extraction removes the third-row pattern");
                patterns.setInventorySlotContents(17, null);
            } else if (terminalStep == 6) {
                nativeTerminal.doAction(player, InventoryAction.SHIFT_CLICK, 8, terminalEntryId);
                require(
                    patterns.getStackInSlot(8) == null,
                    "native terminal shift extraction removes the final pattern");
            } else {
                require(
                    true,
                    "one open native terminal automatically expands and shrinks while retaining 144 physical slots");
                finish("PASS");
                return;
            }
            terminalStep++;
            changedAt = ticks;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        ItemStack stack = GTNGItemList.AssemblerMatrix.get(1);
        world.setBlock(0, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity controller = (BaseMetaTileEntity) world.getTileEntity(0, 10, 0);
        controller.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        controller.setOwnerName(player.getCommandSenderName());
        controller.setOwnerUuid(player.getUniqueID());
        controller.setFrontFacing(ForgeDirection.NORTH);
        matrix = (AssemblerMatrix) controller.getMetaTileEntity();
        matrix.construct(new ItemStack(Items.stick), false);
        matrix.setShowPattern(true);
        var definitions = AEApi.instance()
            .definitions();
        for (int x = 0; x <= 1; x++) {
            world.setBlock(
                x,
                10,
                1,
                definitions.blocks()
                    .multiPart()
                    .maybeBlock()
                    .get());
            IPartHost host = (IPartHost) world.getTileEntity(x, 10, 1);
            require(
                host.addPart(
                    definitions.parts()
                        .cableGlass()
                        .stack(AEColor.Transparent, 1),
                    ForgeDirection.UNKNOWN,
                    player) != null,
                "native AE cable placed at " + x);
        }
        IPartHost host = (IPartHost) world.getTileEntity(1, 10, 1);
        require(
            host.addPart(
                definitions.parts()
                    .interfaceTerminal()
                    .maybeStack(1)
                    .get(),
                ForgeDirection.NORTH,
                player) != null,
            "native interface terminal attached to the matrix grid");
        terminalPart = (PartInterfaceTerminal) host.getPart(ForgeDirection.NORTH);
        world.setBlock(
            1,
            10,
            2,
            definitions.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(1.5, 10, -2.5, 0, 15);
    }

    private void verifyBoundariesAndPersistence() {
        CombinationPatternsIInventory patterns = (CombinationPatternsIInventory) matrix.getPatterns();
        assertShape(2);
        int[] slots = { 0, 7, 8, 9, 16, 17, 18, 142, 143 };
        int[] rows = { 2, 2, 3, 3, 3, 4, 4, 16, 16 };
        for (int i = 0; i < slots.length; i++) {
            patterns.setInventorySlotContents(slots[i], pattern(slots[i]));
            assertShape(rows[i]);
            require(patterns.decrStackSize(slots[i], 1) != null, "pattern boundary " + slots[i] + " extracts normally");
            assertShape(2);
        }
        patterns.setInventorySlotContents(143, pattern(143));
        assertShape(16);
        NBTTagCompound saved = new NBTTagCompound();
        patterns.saveNBTData(saved);
        patterns.setInventorySlotContents(143, null);
        assertShape(2);
        patterns.loadNBTData(saved);
        assertShape(16);
        require(
            ItemStack.areItemStacksEqual(patterns.getStackInSlot(143), pattern(143)),
            "native pattern inventory NBT restores the last pattern and recomputes visible rows");
        patterns.setInventorySlotContents(143, null);
        assertShape(2);
    }

    private void assertShape(int rows) {
        require(
            matrix.rows() == rows && matrix.rowSize() == 9
                && matrix.numSlots() == rows * 9
                && matrix.getPatterns()
                    .getSizeInventory() == 144,
            "matrix terminal shape " + rows + "x9 / 144 backing slots");
    }

    private Object terminalTracker() throws Exception {
        Object tracker = ((Map<?, ?>) field(nativeTerminal, "tracked")).get(matrix);
        if (tracker == null) throw new AssertionError("native interface terminal does not track the placed matrix");
        return tracker;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !terminalOpen) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiInterfaceTerminal terminal)) return;
            int step = terminalStep;
            if (terminalAcknowledged == step) return;
            if (observedStep != step) {
                observedStep = step;
                stableFrames = 0;
            }
            Object master = field(terminal, "masterList");
            Object entry = ((Map<?, ?>) field(master, "list")).get(terminalEntryId);
            if (entry == null) return;
            int rows = EXPECTED_ROWS[step];
            AppEngInternalInventory inventory = (AppEngInternalInventory) field(entry, "inv");
            if ((Integer) field(entry, "rows") != rows || (Integer) field(entry, "rowSize") != 9
                || (Integer) field(entry, "numSlots") != rows * 9
                || inventory.getSizeInventory() != rows * 9
                || !contentsReady(inventory, step)) {
                stableFrames = 0;
                return;
            }
            if (++stableFrames < 8) return;
            require(
                ((Boolean[]) field(entry, "brokenRecipes")).length == rows * 9
                    && ((Boolean[]) field(entry, "useSubstitute")).length == rows * 9
                    && ((boolean[]) field(entry, "filteredRecipes")).length == rows * 9,
                "native client packet resizes inventory and recipe metadata at step " + step);
            require(
                (Boolean) field(entry, "terminalVisible") && (Boolean) field(entry, "online"),
                "matrix remains visible and online while terminal rows change");
            Field scrollField = AEBaseGui.class.getDeclaredField("scrollBar");
            scrollField.setAccessible(true);
            GuiScrollbar scrollbar = (GuiScrollbar) scrollField.get(terminal);
            scrollbar.setCurrentScroll(rows == 16 ? Integer.MAX_VALUE : 0);
            terminal.drawScreen(-10000, -10000, 0);
            if (rows == 16) {
                int lastRowY = (Integer) field(entry, "dispY") + 15 * 18;
                int viewport = (Integer) field(terminal, "viewHeight");
                require(
                    scrollbar.getCurrentScroll() > 0 && lastRowY >= 0 && lastRowY + 18 <= viewport,
                    "native terminal scrolls to and renders the fully visible final row");
            }
            ScreenShotHelper.saveScreenshot(
                outputDirectory(),
                "matrix-terminal-step-" + step + "-rows-" + rows + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            require(true, "actual client terminal synchronizes " + rows + " rows at step " + step);
            terminalAcknowledged = step;
            if (step == 4) NetworkHandler.instance
                .sendToServer(new PacketInventoryAction(InventoryAction.PICKUP_OR_SET_DOWN, 143, terminalEntryId));
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static boolean contentsReady(AppEngInternalInventory inventory, int step) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            boolean occupied = slot == 8 && step >= 1 && step <= 6 || slot == 17 && step >= 2 && step <= 5
                || slot == 18 && step >= 3 && step <= 5
                || slot == 143 && step == 4;
            if (!ItemStack.areItemStacksEqual(inventory.getStackInSlot(slot), occupied ? pattern(slot) : null))
                return false;
        }
        return true;
    }

    private static ItemStack pattern(int slot) {
        ItemStack result = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .get();
        NBTTagList inputs = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            NBTTagCompound input = new NBTTagCompound();
            if (i == 0 || i == 3) new ItemStack(Blocks.planks).writeToNBT(input);
            inputs.appendTag(input);
        }
        NBTTagList outputs = new NBTTagList();
        NBTTagCompound output = new NBTTagCompound();
        new ItemStack(Items.stick, 4).writeToNBT(output);
        outputs.appendTag(output);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", true);
        tag.setInteger("qaSlot", slot);
        result.setTagCompound(tag);
        return result;
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static File outputDirectory() {
        return new File(System.getProperty("gtng.matrixTerminalRows.qa.output", "."));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("MATRIX_TERMINAL_ROWS_QA: " + message);
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
        System.out.println("MATRIX_TERMINAL_ROWS_QA: " + result);
        Minecraft.getMinecraft()
            .shutdown();
    }
}
