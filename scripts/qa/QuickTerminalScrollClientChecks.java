package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.ae2thing.nei.QuickTerminalRecipeTransferHandler;
import com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.quickterminal.DualTerminalGuiObject;
import com.xyp.gtnotgood.ae2thing.quickterminal.RecipeTransferPayload;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.slots.VirtualMEPatternSlot;
import appeng.container.sync.AbstractSyncHandler;
import appeng.container.sync.ActionHandler;
import appeng.container.sync.SyncDirection;
import appeng.container.sync.SyncEndpoint;
import appeng.container.sync.SyncManager;
import appeng.container.sync.SyncMode;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.networking.TileWireless;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.nei.GTNEIDefaultHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** Opt-in real-client check of high-index encoding, persistence and independent scrolling. */
@Mod(
    modid = "terminalscrollqa",
    name = "Terminal Scroll QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class QuickTerminalScrollClientChecks {

    private boolean started;
    private volatile boolean checked;
    private volatile Throwable failure;
    private GuiQuickEncodingTerminal gui;
    private int frames;
    private volatile List<RecipeTransferPayload> neiPayloads;
    private volatile boolean neiChecked;
    private GTRecipe neiRecipe;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.terminalScroll.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!started && mc.currentScreen instanceof GuiMainMenu) {
            started = true;
            mc.launchIntegratedServer(
                "scroll-qa-" + System.currentTimeMillis(),
                "Scroll QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (!checked || gui != null) return;
        if (failure != null) throw new AssertionError("Server checks failed", failure);
        DualTerminalGuiObject host = host(mc.thePlayer);
        gui = new GuiQuickEncodingTerminal(mc.thePlayer.inventory, host);
        ContainerQuickEncodingTerminal container = (ContainerQuickEncodingTerminal) gui.inventorySlots;
        host.setCraftingRecipe(false);
        container.craftingModeSync.setLocalValue(false);
        checkNeiImport(container);
        for (int i = 0; i < RecipeTransferPayload.SLOT_COUNT; i++) {
            container.inputsSync.get()
                .putAEStackInSlot(i, AEItemStack.create(new ItemStack(Items.apple, i + 1)));
            container.outputsSync.get()
                .putAEStackInSlot(i, AEItemStack.create(new ItemStack(Items.feather, i + 1)));
        }
        mc.displayGuiScreen(gui);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        if (checked) {
            if (neiPayloads != null && !neiChecked) {
                try {
                    checkNeiEncoding((EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0));
                } catch (Throwable error) {
                    failure = error;
                    error.printStackTrace();
                } finally {
                    neiChecked = true;
                }
            }
            return;
        }
        try {
            checkServer((EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0));
        } catch (Throwable error) {
            failure = error;
            error.printStackTrace();
        } finally {
            checked = true;
        }
    }

    private static DualTerminalGuiObject host(EntityPlayer player) {
        ItemStack item = GTNGItemList.WirelessDualInterfaceTerminal.get(1);
        player.inventory.setInventorySlotContents(0, item);
        return new DualTerminalGuiObject(
            AEApi.instance()
                .registries()
                .wireless()
                .getWirelessTerminalHandler(item),
            item,
            player,
            player.worldObj,
            0);
    }

    private static void checkServer(EntityPlayerMP player) throws Exception {
        DualTerminalGuiObject host = host(player);
        player.worldObj.setBlock(
            0,
            6,
            0,
            AEApi.instance()
                .definitions()
                .blocks()
                .wireless()
                .maybeBlock()
                .get());
        TileWireless accessPoint = (TileWireless) player.worldObj.getTileEntity(0, 6, 0);
        accessPoint.onReady();
        IGrid grid = accessPoint.getActionableNode()
            .getGrid();
        for (String name : new String[] { "targetGrid", "sg", "myWap" }) {
            Field field = WirelessTerminalGuiObject.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(
                host,
                name.equals("targetGrid") ? grid : name.equals("sg") ? grid.getCache(IStorageGrid.class) : accessPoint);
        }
        ContainerQuickEncodingTerminal container = new ContainerQuickEncodingTerminal(player.inventory, host);
        IAEStack<?>[] inputs = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
        IAEStack<?>[] outputs = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
        for (int i = 0; i < 20; i++) {
            ItemStack item = new ItemStack(Items.apple, i + 1);
            item.setStackDisplayName("Input " + i);
            inputs[i] = AEItemStack.create(item);
            item = new ItemStack(Items.feather, i + 1);
            item.setStackDisplayName("Output " + i);
            outputs[i] = AEItemStack.create(item);
        }
        inputs[255] = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1234));
        outputs[255] = AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 4321));
        RecipeTransferPayload payload = new RecipeTransferPayload(false, false, 4, false, inputs, outputs);
        ByteBuf buffer = Unpooled.buffer();
        try {
            Method write = RecipeTransferPayload.class
                .getDeclaredMethod("write", ByteBuf.class, RecipeTransferPayload.class);
            write.setAccessible(true);
            write.invoke(null, buffer, payload);
            Method read = RecipeTransferPayload.class.getDeclaredMethod("read", ByteBuf.class);
            read.setAccessible(true);
            payload = (RecipeTransferPayload) read.invoke(null, buffer);
        } finally {
            buffer.release();
        }
        Method transfer = ContainerQuickEncodingTerminal.class
            .getDeclaredMethod("applyRecipeTransfer", RecipeTransferPayload.class);
        transfer.setAccessible(true);
        transfer.invoke(container, payload);
        container.onContainerClosed(player);
        container = new ContainerQuickEncodingTerminal(player.inventory, host);
        require(
            container.inputsSync.get()
                .getAEStackInSlot(255)
                .getStackSize() == 1234,
            "last input persists");
        require(
            container.outputsSync.get()
                .getAEStackInSlot(255)
                .getStackSize() == 4321,
            "last output persists");
        host.getInventoryByName("pattern")
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .materials()
                    .blankPattern()
                    .maybeStack(2)
                    .get());
        Method encode = ContainerQuickEncodingTerminal.class.getDeclaredMethod("quickEncode");
        encode.setAccessible(true);
        encode.invoke(container);
        ItemStack encoded = host.getInventoryByName("pattern")
            .getStackInSlot(1);
        require(encoded != null, "encoded pattern exists");
        var details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, player.worldObj);
        checkStacks(details.getAEInputs(), 1234);
        checkStacks(details.getAEOutputs(), 4321);
        require(
            encoded.getTagCompound()
                .getTagList("in", 10)
                .tagCount() == 256,
            "occupied last slot retained");
        container.clear();
        container.inputsSync.get()
            .putAEStackInSlot(0, inputs[0]);
        container.inputsSync.get()
            .putAEStackInSlot(2, inputs[2]);
        container.outputsSync.get()
            .putAEStackInSlot(0, outputs[0]);
        encode.invoke(container);
        ItemStack compact = host.getInventoryByName("pattern")
            .getStackInSlot(1);
        require(
            compact.getTagCompound()
                .getTagList("in", 10)
                .tagCount() == 3,
            "empty input tail omitted");
        require(
            compact.getTagCompound()
                .getTagList("in", 10)
                .getCompoundTagAt(1)
                .hasNoTags(),
            "interior gap retained");
        require(
            container.inputsSync.get()
                .getSizeInventory() == 256,
            "full inventory restored after encoding");
        container.outputsSync.get()
            .putAEStackInSlot(255, outputs[255]);
        container.clear();
        require(
            container.inputsSync.get()
                .getAEStackInSlot(255) == null,
            "clear reaches hidden inputs");
        require(
            container.outputsSync.get()
                .getAEStackInSlot(255) == null,
            "clear reaches hidden outputs");
        container.onContainerClosed(player);
        System.out.println("TERMINAL_SCROLL_QA: encoding, packet, persistence and clear PASS");
    }

    private static void checkStacks(IAEStack<?>[] stacks, int amount) {
        int count = 0;
        boolean fluid = false;
        for (IAEStack<?> stack : stacks) if (stack != null) {
            count++;
            if (stack.isFluid() && stack.getStackSize() == amount) fluid = true;
        }
        require(count == 21 && fluid, "all 21 entries including final-slot native fluid encoded");
    }

    /** Calls the actual NEI overlay and captures only its transport boundary for deterministic server replay. */
    private void checkNeiImport(ContainerQuickEncodingTerminal container) throws Exception {
        GTNEIDefaultHandler handler = new GTNEIDefaultHandler(
            RecipeMaps.assemblylineVisualRecipes.getDefaultRecipeCategory());
        Method interchangeable = QuickTerminalRecipeTransferHandler.class
            .getDeclaredMethod("hasInterchangeableInputs", IRecipeHandler.class, int.class, boolean.class);
        interchangeable.setAccessible(true);
        int candidates = 0;
        for (GTRecipe recipe : RecipeMaps.assemblylineVisualRecipes.getAllRecipes()) {
            if (recipe.mInputs.length != 16 || recipe.mFluidInputs.length != 4) continue;
            candidates++;
            handler.arecipes.clear();
            handler.arecipes.add(handler.new CachedDefaultRecipe(recipe));
            if (!(Boolean) interchangeable.invoke(null, handler, 0, false)) {
                neiRecipe = recipe;
                break;
            }
        }
        if (neiRecipe == null) {
            ItemStack[] items = new ItemStack[16];
            for (int i = 0; i < items.length; i++) {
                items[i] = new ItemStack(Items.iron_ingot, i + 1);
                items[i].setStackDisplayName("NEI assembly input " + (i + 1));
            }
            FluidStack[] fluids = { new FluidStack(FluidRegistry.WATER, 1234), new FluidStack(FluidRegistry.LAVA, 2345),
                Materials.Lubricant.getFluid(3456), Materials.SolderingAlloy.getMolten(4567) };
            neiRecipe = new GTRecipe(
                false,
                items,
                new ItemStack[] { new ItemStack(Items.diamond, 3) },
                null,
                null,
                null,
                null,
                null,
                fluids,
                null,
                120,
                32,
                0);
            handler.arecipes.clear();
            handler.arecipes.add(handler.new CachedDefaultRecipe(neiRecipe));
            System.out.println(
                "TERMINAL_NEI_QA: boundary fixture in real assembly-line NEI handler; registered 16+4 candidates="
                    + candidates);
        }
        List<RecipeTransferPayload> captured = new ArrayList<>();
        ActionHandler<RecipeTransferPayload> action = container.transferRecipeAction;
        Field manager = AbstractSyncHandler.class.getDeclaredField("manager");
        manager.setAccessible(true);
        ActionHandler<RecipeTransferPayload> capture = new ActionHandler<RecipeTransferPayload>(
            (SyncManager) manager.get(action),
            action.getKey(),
            action.getFullKey(),
            SyncDirection.CLIENT_TO_SERVER,
            RecipeTransferPayload.CODEC) {

            @Override
            public void send(RecipeTransferPayload payload) {
                captured.add(payload);
            }
        };
        Field transfer = ContainerQuickEncodingTerminal.class.getDeclaredField("transferRecipeAction");
        transfer.setAccessible(true);
        transfer.set(container, capture);
        container.combineSync.setLocalValue(false);
        container.prioritizeFluidsSync.setLocalValue(false);
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(gui, handler, 0, false);
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(gui, handler, 0, true);
        container.prioritizeFluidsSync.setLocalValue(true);
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(gui, handler, 0, true);
        require(captured.size() == 3, "all NEI overlay calls produce transfers");
        require(
            !captured.get(0)
                .shouldEncode(),
            "plain import remains editable");
        require(
            captured.get(1)
                .shouldEncode()
                && captured.get(2)
                    .shouldEncode(),
            "Shift import auto-encodes");
        for (RecipeTransferPayload payload : captured) {
            IAEStack<?>[] inputs = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
            for (int i = 0; i < inputs.length; i++) inputs[i] = payload.getInput(i);
            checkRecipeInputs(inputs);
        }
        transfer.set(container, action);
        System.out.println("TERMINAL_NEI_QA: recipe output " + neiRecipe.mOutputs[0].getDisplayName());
        neiPayloads = captured;
    }

    private void checkNeiEncoding(EntityPlayerMP player) throws Exception {
        DualTerminalGuiObject host = host(player);
        TileWireless accessPoint = (TileWireless) player.worldObj.getTileEntity(0, 6, 0);
        IGrid grid = accessPoint.getActionableNode()
            .getGrid();
        for (String name : new String[] { "targetGrid", "sg", "myWap" }) {
            Field field = WirelessTerminalGuiObject.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(
                host,
                name.equals("targetGrid") ? grid : name.equals("sg") ? grid.getCache(IStorageGrid.class) : accessPoint);
        }
        ContainerQuickEncodingTerminal container = new ContainerQuickEncodingTerminal(player.inventory, host);
        host.getInventoryByName("pattern")
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .materials()
                    .blankPattern()
                    .maybeStack(3)
                    .get());
        for (RecipeTransferPayload payload : neiPayloads) {
            host.getInventoryByName("pattern")
                .setInventorySlotContents(1, null);
            ByteBuf buffer = Unpooled.buffer();
            try {
                RecipeTransferPayload.CODEC.write(buffer, payload);
                container.transferRecipeAction.readIncoming(SyncEndpoint.CLIENT, SyncMode.FULL, buffer);
            } finally {
                buffer.release();
            }
            ItemStack encoded = host.getInventoryByName("pattern")
                .getStackInSlot(1);
            if (!payload.shouldEncode()) {
                require(encoded == null, "plain import does not consume blank pattern");
                continue;
            }
            require(encoded != null, "Shift import automatically produces pattern");
            require(
                AEApi.instance()
                    .definitions()
                    .items()
                    .encodedUltimatePattern()
                    .isSameAs(encoded),
                "native Ultimate Encoded Pattern");
            var details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, player.worldObj);
            checkRecipeInputs(details.getAEInputs());
            var outputs = details.getAEOutputs();
            require(outputs.length == 1 && outputs[0] instanceof IAEItemStack, "one item output");
            require(
                ((IAEItemStack) outputs[0]).isSameType(AEItemStack.create(neiRecipe.mOutputs[0]))
                    && outputs[0].getStackSize() == neiRecipe.mOutputs[0].stackSize,
                "output identity and quantity");
        }
        container.onContainerClosed(player);
        Files.write(
            new File(System.getProperty("gtng.terminalScroll.qa.output"), "nei-result.txt").toPath(),
            ("PASS: " + neiRecipe.mOutputs[0].getDisplayName()
                + "\n16 items + 4 fluids; normal import, Shift auto-encode, fluid priority\n")
                    .getBytes(StandardCharsets.UTF_8));
        System.out
            .println("TERMINAL_NEI_QA: PASS 16 items + 4 fluids, exact quantities, native pattern, fluid priority");
    }

    private void checkRecipeInputs(IAEStack<?>[] inputs) {
        int items = 0, fluids = 0;
        for (IAEStack<?> input : inputs) {
            if (input instanceof IAEItemStack) items++;
            if (input instanceof IAEFluidStack) fluids++;
        }
        require(items == 16 && fluids == 4, "all 16 items and 4 native fluids preserved");
        for (ItemStack expected : neiRecipe.mInputs) {
            long amount = 0, expectedAmount = 0;
            for (ItemStack other : neiRecipe.mInputs) if (AEItemStack.create(other)
                .isSameType(AEItemStack.create(expected))) expectedAmount += other.stackSize;
            for (IAEStack<?> input : inputs)
                if (input instanceof IAEItemStack item && item.isSameType(AEItemStack.create(expected)))
                    amount += input.getStackSize();
            require(amount == expectedAmount, "item quantity: " + expected.getDisplayName());
        }
        for (FluidStack expected : neiRecipe.mFluidInputs) {
            long amount = 0;
            for (IAEStack<?> input : inputs) if (input instanceof IAEFluidStack fluid && fluid.getFluidStack()
                .isFluidEqual(expected)) amount += input.getStackSize();
            require(
                amount == expected.amount,
                "fluid quantity: " + expected.getFluid()
                    .getName());
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || gui == null) return;
        if (failure != null) throw new AssertionError("NEI checks failed", failure);
        if (!neiChecked) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != gui) return;
        if (++frames == 30) {
            InvTweaksOrderCacheClientChecks.run();
            ItemSortNameCacheClientChecks.run();
            InterfaceViewportClientChecks.run(guiField("interfaceTerminal"));
            screenshot("top.png");
            VirtualMEPatternSlot first = ((VirtualMEPatternSlot[]) guiField("craftingSlots"))[0];
            int x = (Integer) guiField("guiLeft") + first.getX() + 2;
            int y = (Integer) guiField("guiTop") + first.getY() + 2;
            require(gui.mouseWheelEvent(x, y, -120), "input wheel consumed");
            checkVisible("craftingSlots", 4);
            checkVisible("outputSlots", 0);
            gui.mouseClicked(x + 72, y, 0);
            gui.mouseClickMove(x + 72, y + 100, 0, 100);
            gui.mouseMovedOrUp(x + 72, y + 100, 0);
            checkVisible("craftingSlots", 240);
            checkVisible("outputSlots", 0);
            for (int i = 0; i < 70; i++) gui.mouseWheelEvent(x, y + 95, -120);
            checkVisible("outputSlots", 240);
            checkVisible("craftingSlots", 240);
        } else if (frames == 50) {
            screenshot("bottom.png");
            Files.write(
                new File(System.getProperty("gtng.terminalScroll.qa.output"), "result.txt").toPath(),
                "PASS".getBytes(StandardCharsets.UTF_8));
            mc.shutdown();
        }
    }

    private void checkVisible(String name, int first) throws Exception {
        VirtualMEPatternSlot[] slots = (VirtualMEPatternSlot[]) guiField(name);
        for (int i = 0; i < slots.length; i++)
            require(slots[i].isHidden() == (i < first || i >= first + 16), name + " visibility " + i);
    }

    private Object guiField(String name) throws Exception {
        Class<?> type = gui.getClass();
        Field field = null;
        while (field == null) {
            try {
                field = type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        field.setAccessible(true);
        return field.get(gui);
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(
            new File(System.getProperty("gtng.terminalScroll.qa.output")),
            name,
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
