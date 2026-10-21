package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtnotgood.ae2thing.nei.QuickTerminalRecipeTransferHandler;
import com.xyp.gtnotgood.ae2thing.nei.object.OrderStack;
import com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.quickterminal.DualTerminalGuiObject;
import com.xyp.gtnotgood.ae2thing.quickterminal.RecipeIngredientReplacement;
import com.xyp.gtnotgood.ae2thing.quickterminal.RecipeTransferPayload;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.machine.MoldDataManager;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.slots.VirtualMEPatternSlot;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.sync.AbstractSyncHandler;
import appeng.container.sync.ActionHandler;
import appeng.container.sync.StreamCodecs;
import appeng.container.sync.SyncDirection;
import appeng.container.sync.SyncEndpoint;
import appeng.container.sync.SyncManager;
import appeng.container.sync.SyncMode;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.items.contents.WirelessPatternTerminalGuiObject;
import appeng.tile.networking.TileWireless;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
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
    private volatile List<RecipeTransferPayload> nonConsumablePayloads;
    private List<FluidDisplayCase> fluidDisplayCases;
    private volatile boolean neiChecked;
    private GTRecipe neiRecipe;
    private int startupTicks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.terminalScroll.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!started && ++startupTicks % 200 == 0) {
            System.out.println(
                "TERMINAL_QA_WAITING: " + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getName()));
        }
        if (
            !started && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen != null
                && mc.currentScreen.getClass().getName().equals("galaxyspace.core.gui.GSGuiMainMenu"))
        ) {
            started = true;
            mc.launchIntegratedServer("scroll-qa-" + System.currentTimeMillis(), "Scroll QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (!started && startupTicks > 1200) throw new AssertionError("QA did not reach the main menu");
        if (!checked || gui != null) return;
        if (failure != null) throw new AssertionError("Server checks failed", failure);
        DualTerminalGuiObject host = host(mc.thePlayer);
        gui = new GuiQuickEncodingTerminal(mc.thePlayer.inventory, host);
        ContainerQuickEncodingTerminal container = (ContainerQuickEncodingTerminal) gui.inventorySlots;
        host.setCraftingRecipe(false);
        container.craftingModeSync.setLocalValue(false);
        checkNeiImport(container);
        for (int i = 0; i < RecipeTransferPayload.SLOT_COUNT; i++) {
            container.inputsSync.get().putAEStackInSlot(i, AEItemStack.create(new ItemStack(Items.apple, i + 1)));
            container.outputsSync.get().putAEStackInSlot(i, AEItemStack.create(new ItemStack(Items.feather, i + 1)));
        }
        mc.displayGuiScreen(gui);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
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
        return new DualTerminalGuiObject(AEApi.instance().registries().wireless().getWirelessTerminalHandler(item),
            item, player, player.worldObj, 0);
    }

    private static void checkServer(EntityPlayerMP player) throws Exception {
        InterfaceNameClientChecks.checkServer(player);
        DualTerminalGuiObject host = host(player);
        player.worldObj.setBlock(0, 6, 0, AEApi.instance().definitions().blocks().wireless().maybeBlock().get());
        TileWireless accessPoint = (TileWireless) player.worldObj.getTileEntity(0, 6, 0);
        accessPoint.onReady();
        IGrid grid = accessPoint.getActionableNode().getGrid();
        for (String name : new String[] { "targetGrid", "sg", "myWap" }) {
            Field field = WirelessTerminalGuiObject.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(host,
                name.equals("targetGrid") ? grid : name.equals("sg") ? grid.getCache(IStorageGrid.class) : accessPoint);
        }
        ContainerQuickEncodingTerminal container = new ContainerQuickEncodingTerminal(player.inventory, host);
        Field mode = WirelessPatternTerminalGuiObject.class.getDeclaredField("mode");
        mode.setAccessible(true);
        require(mode.getInt(host) != 2, "custom grids must not enter GTNL's native processing-mode hooks");
        require(container.inputsSync.get().getSizeInventory() == RecipeTransferPayload.SLOT_COUNT,
            "input inventory matches the crafting matrix");
        require(container.outputsSync.get().getSizeInventory() == RecipeTransferPayload.SLOT_COUNT,
            "output inventory retains the full custom grid");
        container.setCraftingMode(true);
        container.detectAndSendChanges();
        container.setCraftingMode(false);
        container.detectAndSendChanges();
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
            Method write = RecipeTransferPayload.class.getDeclaredMethod("write", ByteBuf.class,
                RecipeTransferPayload.class);
            write.setAccessible(true);
            write.invoke(null, buffer, payload);
            Method read = RecipeTransferPayload.class.getDeclaredMethod("read", ByteBuf.class);
            read.setAccessible(true);
            payload = (RecipeTransferPayload) read.invoke(null, buffer);
        } finally {
            buffer.release();
        }
        Method transfer = ContainerQuickEncodingTerminal.class.getDeclaredMethod("applyRecipeTransfer",
            RecipeTransferPayload.class);
        transfer.setAccessible(true);
        transfer.invoke(container, payload);
        container.onContainerClosed(player);
        container = new ContainerQuickEncodingTerminal(player.inventory, host);
        require(container.inputsSync.get().getAEStackInSlot(255).getStackSize() == 1234, "last input persists");
        require(container.outputsSync.get().getAEStackInSlot(255).getStackSize() == 4321, "last output persists");
        host.getInventoryByName("pattern").setInventorySlotContents(0,
            AEApi.instance().definitions().materials().blankPattern().maybeStack(2).get());
        Method encode = ContainerQuickEncodingTerminal.class.getDeclaredMethod("quickEncode");
        encode.setAccessible(true);
        encode.invoke(container);
        ItemStack encoded = host.getInventoryByName("pattern").getStackInSlot(1);
        require(encoded != null, "encoded pattern exists");
        var details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, player.worldObj);
        checkStacks(details.getAEInputs(), 1234);
        checkStacks(details.getAEOutputs(), 4321);
        require(encoded.getTagCompound().getTagList("in", 10).tagCount() == 256, "occupied last slot retained");
        container.clear();
        container.inputsSync.get().putAEStackInSlot(0, inputs[0]);
        container.inputsSync.get().putAEStackInSlot(2, inputs[2]);
        container.outputsSync.get().putAEStackInSlot(0, outputs[0]);
        encode.invoke(container);
        ItemStack compact = host.getInventoryByName("pattern").getStackInSlot(1);
        require(compact.getTagCompound().getTagList("in", 10).tagCount() == 3, "empty input tail omitted");
        require(compact.getTagCompound().getTagList("in", 10).getCompoundTagAt(1).hasNoTags(), "interior gap retained");
        require(container.inputsSync.get().getSizeInventory() == 256, "full inventory restored after encoding");
        container.outputsSync.get().putAEStackInSlot(255, outputs[255]);
        container.clear();
        require(container.inputsSync.get().getAEStackInSlot(255) == null, "clear reaches hidden inputs");
        require(container.outputsSync.get().getAEStackInSlot(255) == null, "clear reaches hidden outputs");
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
        Method interchangeable = QuickTerminalRecipeTransferHandler.class.getDeclaredMethod("hasInterchangeableInputs",
            IRecipeHandler.class, int.class, boolean.class);
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
            neiRecipe = new GTRecipe(false, items, new ItemStack[] { new ItemStack(Items.diamond, 3) }, null, null,
                null, null, null, fluids, null, 120, 32, 0);
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
            (SyncManager) manager.get(action), action.getKey(), action.getFullKey(), SyncDirection.CLIENT_TO_SERVER,
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
        require(!captured.get(0).shouldEncode(), "plain import remains editable");
        require(captured.get(1).shouldEncode() && captured.get(2).shouldEncode(), "Shift import auto-encodes");
        for (RecipeTransferPayload payload : captured) {
            IAEStack<?>[] inputs = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
            for (int i = 0; i < inputs.length; i++) inputs[i] = payload.getInput(i);
            checkRecipeInputs(inputs);
        }
        System.out.println("TERMINAL_NEI_QA: recipe output " + neiRecipe.mOutputs[0].getDisplayName());
        List<RecipeTransferPayload> standardPayloads = new ArrayList<>(captured);
        captured.clear();
        checkNonConsumablesImport(container, captured);
        nonConsumablePayloads = new ArrayList<>(captured);
        captured.clear();
        checkFluidDisplayImport(container, captured);
        transfer.set(container, action);
        neiPayloads = standardPayloads;
    }

    private void checkFluidDisplayImport(ContainerQuickEncodingTerminal container, List<RecipeTransferPayload> captured)
        throws Exception {
        FluidStack taggedWater = new FluidStack(FluidRegistry.WATER, 71);
        taggedWater.tag = new NBTTagCompound();
        taggedWater.tag.setString("Marker", "input");
        FluidStack taggedLava = new FluidStack(FluidRegistry.LAVA, 333);
        taggedLava.tag = new NBTTagCompound();
        taggedLava.tag.setString("Marker", "output");
        ItemStack water = ItemFluidDisplay.createStack(FluidRegistry.WATER, 37);
        ItemStack lava = ItemFluidDisplay.createStack(FluidRegistry.LAVA, 129);
        long largeLavaAmount = (long) Integer.MAX_VALUE + 144;
        ItemStack largeLava = ItemFluidDisplay.createStack(FluidRegistry.LAVA, largeLavaAmount);
        ItemStack inputPacket = ItemFluidPacket.newStack(taggedWater);
        ItemStack outputPacket = ItemFluidPacket.newStack(taggedLava);
        ItemStack filled = new ItemStack(Items.water_bucket, 2);
        ItemStack empty = new ItemStack(Items.bucket, 3);
        require(water.stackSize == 1 && lava.stackSize == 1 && largeLava.stackSize == 1,
            "NEI displays store quantity outside item count");
        ItemStack[] sources = { water, inputPacket, filled, largeLava, lava, outputPacket, empty };
        NBTTagCompound[] snapshots = new NBTTagCompound[sources.length];
        for (int i = 0; i < sources.length; i++) snapshots[i] = sources[i].writeToNBT(new NBTTagCompound());
        IAEStack<?>[] inputs = { AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 37)),
            AEFluidStack.create(taggedWater), AEItemStack.create(filled),
            AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1)).setStackSize(largeLavaAmount) };
        IAEStack<?>[] outputs = { AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 129)),
            AEFluidStack.create(taggedLava), AEItemStack.create(empty) };
        var handler = new FluidDisplayRecipeHandler(
            Arrays.asList(new PositionedStack(water, 0, 0, false), new PositionedStack(inputPacket, 18, 0, false),
                new PositionedStack(filled, 36, 0, false), new PositionedStack(largeLava, 54, 0, false)),
            Arrays.asList(new PositionedStack(lava, 0, 18, false), new PositionedStack(outputPacket, 18, 18, false),
                new PositionedStack(empty, 36, 18, false)));
        container.combineSync.setLocalValue(false);
        container.prioritizeFluidsSync.setLocalValue(false);
        container.keepNonConsumablesSync.setLocalValue(false);
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(gui, handler, 0, false);
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(gui, handler, 0, true);
        require(captured.size() == 2, "synthetic fluid input/output overlays produce both transfers");
        require(!captured.get(0).shouldEncode() && captured.get(1).shouldEncode(), "synthetic fluid Shift encoding");
        fluidDisplayCases = new ArrayList<>();
        for (RecipeTransferPayload payload : captured) {
            checkFluidDisplayStacks(payloadInputs(payload), inputs, "client input payload");
            checkFluidDisplayStacks(payloadOutputs(payload), outputs, "client output payload");
            fluidDisplayCases.add(new FluidDisplayCase(payload, inputs, outputs, null));
        }
        for (int i = 0; i < sources.length; i++) {
            require(snapshots[i].equals(sources[i].writeToNBT(new NBTTagCompound())),
                "NEI fluid source stays unchanged");
        }
        PositionedStack alternatives = new PositionedStack(
            new ItemStack[] { ItemFluidDisplay.createStack(FluidRegistry.WATER, 37),
                ItemFluidDisplay.createStack(FluidRegistry.LAVA, 9) },
            0, 0, false);
        var alternateHandler = new FluidDisplayRecipeHandler(Arrays.asList(alternatives),
            Arrays.asList(new PositionedStack(lava, 0, 18, false)));
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(gui, alternateHandler, 0, true);
        require(captured.size() == 3 && !captured.get(2).shouldEncode(), "fluid alternatives remain editable");
        IAEStack<?> first = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 37));
        Method cycle = QuickTerminalRecipeTransferHandler.class.getDeclaredMethod("findAdjacentAlternative",
            PositionedStack.class, IAEStack.class, int.class, boolean.class);
        cycle.setAccessible(true);
        IAEStack<?> next = (IAEStack<?>) cycle.invoke(null, alternatives, first, 1, false);
        checkFluidDisplayStacks(new IAEStack<?>[] { next },
            new IAEStack<?>[] { AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 9)) }, "fluid alternative");
        IAEStack<?> previous = (IAEStack<?>) cycle.invoke(null, alternatives, next, -1, false);
        checkFluidDisplayStacks(new IAEStack<?>[] { previous }, new IAEStack<?>[] { first },
            "reverse fluid alternative");
        fluidDisplayCases.add(new FluidDisplayCase(captured.get(2), new IAEStack<?>[] { next },
            new IAEStack<?>[] { outputs[0] }, new RecipeIngredientReplacement(first, next)));
        System.out.println("TERMINAL_NATIVE_FLUID_QA: displays, packets, containers and alternatives PASS");
    }

    private static IAEStack<?>[] payloadInputs(RecipeTransferPayload payload) {
        IAEStack<?>[] result = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
        for (int i = 0; i < result.length; i++) result[i] = payload.getInput(i);
        return result;
    }

    private static IAEStack<?>[] payloadOutputs(RecipeTransferPayload payload) {
        IAEStack<?>[] result = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
        for (int i = 0; i < result.length; i++) result[i] = payload.getOutput(i);
        return result;
    }

    private static void checkFluidDisplayStacks(IAEStack<?>[] actual, IAEStack<?>[] expected, String location) {
        long actualCount = Arrays.stream(actual).filter(stack -> stack != null).count();
        require(actualCount == expected.length, location + " entry count");
        for (IAEStack<?> wanted : expected) {
            boolean found = false;
            for (IAEStack<?> stack : actual) {
                if (
                    stack != null && stack.getStackType() == wanted.getStackType()
                        && stack.isSameType((Object) wanted)
                        && stack.getStackSize() == wanted.getStackSize()
                ) found = true;
            }
            require(found, location + " native type, identity, quantity and fluid tag");
        }
    }

    private static final class FluidDisplayCase {

        private final RecipeTransferPayload payload;
        private final IAEStack<?>[] inputs;
        private final IAEStack<?>[] outputs;
        private final RecipeIngredientReplacement replacement;

        private FluidDisplayCase(RecipeTransferPayload payload, IAEStack<?>[] inputs, IAEStack<?>[] outputs,
            RecipeIngredientReplacement replacement) {
            this.payload = payload;
            this.inputs = inputs;
            this.outputs = outputs;
            this.replacement = replacement;
        }
    }

    private static final class FluidDisplayRecipeHandler extends TemplateRecipeHandler {

        private final List<PositionedStack> ingredients;
        private final List<PositionedStack> outputs;

        private FluidDisplayRecipeHandler(List<PositionedStack> ingredients, List<PositionedStack> outputs) {
            this.ingredients = ingredients;
            this.outputs = outputs;
        }

        @Override
        public String getRecipeName() {
            return "Native fluid QA";
        }

        @Override
        public String getGuiTexture() {
            return "textures/gui/crafting_table.png";
        }

        @Override
        public String getOverlayIdentifier() {
            return "terminal-native-fluid-qa";
        }

        @Override
        public List<PositionedStack> getIngredientStacks(int recipe) {
            return ingredients;
        }

        @Override
        public PositionedStack getResultStack(int recipe) {
            return outputs.get(0);
        }

        @Override
        public List<PositionedStack> getOtherStacks(int recipe) {
            return outputs.subList(1, outputs.size());
        }
    }

    private void checkNonConsumablesImport(ContainerQuickEncodingTerminal container,
        List<RecipeTransferPayload> captured) throws Exception {
        var handler = new GTNEIDefaultHandler(RecipeMaps.assemblerRecipes.getDefaultRecipeCategory());
        ItemStack circuit = GTUtility.getIntegratedCircuit(4);
        circuit.stackSize = 0;
        ItemStack mold = ItemList.Shape_Mold_Ingot.get(0);
        var recipe = new GTRecipe(false,
            new ItemStack[] { new ItemStack(Items.iron_ingot, 3), circuit, mold, new ItemStack(Items.blaze_rod, 0),
                ItemList.Shape_Mold_Bottle.get(2) },
            new ItemStack[] { new ItemStack(Items.apple) }, null, null, null, null, null,
            new FluidStack[] { new FluidStack(FluidRegistry.WATER, 1234) }, null, 20, 8, 0);
        handler.arecipes.clear();
        handler.arecipes.add(handler.new CachedDefaultRecipe(recipe));
        for (int variant = 0; variant < 3; variant++) {
            container.keepNonConsumablesSync.setLocalValue(variant != 0);
            container.combineSync.setLocalValue(variant == 2);
            container.prioritizeFluidsSync.setLocalValue(variant == 2);
            QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(gui, handler, 0, true);
            require(captured.size() == variant + 1, "non-consumable fixture transfers");
            List<IAEStack<?>> inputs = new ArrayList<>();
            for (int i = 0; i < RecipeTransferPayload.SLOT_COUNT; i++)
                if (captured.get(variant).getInput(i) != null) inputs.add(captured.get(variant).getInput(i));
            checkNonConsumableInputs(inputs.toArray(new IAEStack<?>[0]), variant != 0);
        }
        require(circuit.stackSize == 0 && mold.stackSize == 0, "NEI recipe source quantities are unchanged");
        Method retain = QuickTerminalRecipeTransferHandler.class.getDeclaredMethod("retainSupportedNonConsumables",
            List.class);
        retain.setAccessible(true);
        List<OrderStack<?>> virtualMolds = new ArrayList<>();
        for (ItemStack supported : MoldDataManager.getMolds()) {
            ItemStack copy = supported.copy();
            copy.stackSize = 0;
            virtualMolds.add(new OrderStack<>(copy, virtualMolds.size()));
        }
        List<OrderStack<?>> retained = (List<OrderStack<?>>) retain.invoke(null, virtualMolds);
        require(retained.size() == virtualMolds.size(), "all entries use the current virtual mold list");
        for (int i = 0; i < retained.size(); i++) {
            require(((ItemStack) retained.get(i).getStack()).stackSize == 1, "supported catalyst is retained");
            require(((ItemStack) virtualMolds.get(i).getStack()).stackSize == 0, "source catalyst is unchanged");
        }
        container.keepNonConsumablesSync.setLocalValue(false);
        System.out.println("TERMINAL_CATALYST_QA: toggle, whitelist, combine and native fluids PASS");
    }

    private static void checkNonConsumableInputs(IAEStack<?>[] inputs, boolean keep) {
        require(inputs.length == (keep ? 5 : 3), "only requested non-consumables change import size");
        boolean circuit = false, mold = false, consumedMold = false, fluid = false, ingredient = false;
        for (IAEStack<?> input : inputs) {
            if (input instanceof IAEFluidStack water) {
                require(water.getFluidStack().getFluid() == FluidRegistry.WATER && water.getStackSize() == 1234,
                    "native fluid identity and amount preserved");
                fluid = true;
            } else if (input instanceof IAEItemStack item) {
                require(item.getItem() != Items.blaze_rod, "unsupported non-consumable is omitted");
                if (item.isSameType(AEItemStack.create(GTUtility.getIntegratedCircuit(4)))) {
                    circuit = true;
                    require(item.getStackSize() == 1, "circuit imports as one with correct configuration");
                } else if (item.isSameType(AEItemStack.create(ItemList.Shape_Mold_Ingot.get(1)))) {
                    mold = true;
                    require(item.getStackSize() == 1, "virtual mold imports as one");
                } else if (item.isSameType(AEItemStack.create(ItemList.Shape_Mold_Bottle.get(1)))) {
                    consumedMold = true;
                    require(item.getStackSize() == 2, "consumed mold quantity remains unchanged");
                } else if (item.getItem() == Items.iron_ingot) {
                    ingredient = true;
                    require(item.getStackSize() == 3, "consumed input quantity remains unchanged");
                }
            }
        }
        require(circuit == keep && mold == keep && consumedMold && ingredient && fluid,
            "only circuits and current virtual mold items are retained");
    }

    private void checkNeiEncoding(EntityPlayerMP player) throws Exception {
        DualTerminalGuiObject host = host(player);
        TileWireless accessPoint = (TileWireless) player.worldObj.getTileEntity(0, 6, 0);
        IGrid grid = accessPoint.getActionableNode().getGrid();
        for (String name : new String[] { "targetGrid", "sg", "myWap" }) {
            Field field = WirelessTerminalGuiObject.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(host,
                name.equals("targetGrid") ? grid : name.equals("sg") ? grid.getCache(IStorageGrid.class) : accessPoint);
        }
        ContainerQuickEncodingTerminal container = new ContainerQuickEncodingTerminal(player.inventory, host);
        host.getInventoryByName("pattern").setInventorySlotContents(0,
            AEApi.instance().definitions().materials().blankPattern().maybeStack(3).get());
        for (RecipeTransferPayload payload : neiPayloads) {
            host.getInventoryByName("pattern").setInventorySlotContents(1, null);
            ByteBuf buffer = Unpooled.buffer();
            try {
                RecipeTransferPayload.CODEC.write(buffer, payload);
                container.transferRecipeAction.readIncoming(SyncEndpoint.CLIENT, SyncMode.FULL, buffer);
            } finally {
                buffer.release();
            }
            ItemStack encoded = host.getInventoryByName("pattern").getStackInSlot(1);
            if (!payload.shouldEncode()) {
                require(encoded == null, "plain import does not consume blank pattern");
                continue;
            }
            require(encoded != null, "Shift import automatically produces pattern");
            require(AEApi.instance().definitions().items().encodedUltimatePattern().isSameAs(encoded),
                "native Ultimate Encoded Pattern");
            var details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, player.worldObj);
            checkRecipeInputs(details.getAEInputs());
            var outputs = details.getAEOutputs();
            require(outputs.length == 1 && outputs[0] instanceof IAEItemStack, "one item output");
            require(((IAEItemStack) outputs[0]).isSameType(AEItemStack.create(neiRecipe.mOutputs[0]))
                && outputs[0].getStackSize() == neiRecipe.mOutputs[0].stackSize, "output identity and quantity");
        }
        require(!container.isKeepNonConsumablesEnabled(), "retention defaults to disabled");
        ByteBuf setting = Unpooled.buffer();
        try {
            setting.writeBoolean(true);
            container.setKeepNonConsumablesAction.readIncoming(SyncEndpoint.CLIENT, SyncMode.FULL, setting);
        } finally {
            setting.release();
        }
        require(host.shouldKeepNonConsumables() && container.isKeepNonConsumablesEnabled(),
            "retention action updates server and terminal NBT");
        container.onContainerClosed(player);
        container = new ContainerQuickEncodingTerminal(player.inventory, host);
        require(container.isKeepNonConsumablesEnabled(), "retention setting persists after reopen");
        host.getInventoryByName("pattern").setInventorySlotContents(0,
            AEApi.instance().definitions().materials().blankPattern().maybeStack(3).get());
        for (int variant = 0; variant < nonConsumablePayloads.size(); variant++) {
            host.getInventoryByName("pattern").setInventorySlotContents(1, null);
            ByteBuf buffer = Unpooled.buffer();
            try {
                RecipeTransferPayload.CODEC.write(buffer, nonConsumablePayloads.get(variant));
                container.transferRecipeAction.readIncoming(SyncEndpoint.CLIENT, SyncMode.FULL, buffer);
            } finally {
                buffer.release();
            }
            ItemStack encoded = host.getInventoryByName("pattern").getStackInSlot(1);
            require(encoded != null, "retained inputs auto-encode on server");
            var details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, player.worldObj);
            checkNonConsumableInputs(details.getCondensedAEInputs(), variant != 0);
        }
        System.out.println("TERMINAL_CATALYST_QA: server action, persistence and native pattern encoding PASS");
        checkFluidDisplayEncoding(container, host, player);
        container.onContainerClosed(player);
        Files.write(new File(System.getProperty("gtng.terminalScroll.qa.output"), "nei-result.txt").toPath(),
            ("PASS: " + neiRecipe.mOutputs[0].getDisplayName()
                + "\n16 items + 4 fluids; normal import, Shift auto-encode, fluid priority\n")
                    .getBytes(StandardCharsets.UTF_8));
        System.out
            .println("TERMINAL_NEI_QA: PASS 16 items + 4 fluids, exact quantities, native pattern, fluid priority");
    }

    private void checkFluidDisplayEncoding(ContainerQuickEncodingTerminal container, DualTerminalGuiObject host,
        EntityPlayerMP player) throws Exception {
        host.getInventoryByName("pattern").setInventorySlotContents(0,
            AEApi.instance().definitions().materials().blankPattern().maybeStack(fluidDisplayCases.size()).get());
        Method encode = ContainerQuickEncodingTerminal.class.getDeclaredMethod("quickEncode");
        encode.setAccessible(true);
        for (FluidDisplayCase displayCase : fluidDisplayCases) {
            host.getInventoryByName("pattern").setInventorySlotContents(1, null);
            ByteBuf buffer = Unpooled.buffer();
            try {
                RecipeTransferPayload.CODEC.write(buffer, displayCase.payload);
                container.transferRecipeAction.readIncoming(SyncEndpoint.CLIENT, SyncMode.FULL, buffer);
            } finally {
                buffer.release();
            }
            if (displayCase.replacement != null) {
                buffer = Unpooled.buffer();
                try {
                    RecipeIngredientReplacement.CODEC.write(buffer, displayCase.replacement);
                    container.replaceRecipeIngredientAction.readIncoming(SyncEndpoint.CLIENT, SyncMode.FULL, buffer);
                } finally {
                    buffer.release();
                }
                encode.invoke(container);
            }
            IAEStack<?>[] inputs = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
            IAEStack<?>[] outputs = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
            for (int i = 0; i < inputs.length; i++) {
                inputs[i] = container.inputsSync.get().getAEStackInSlot(i);
                outputs[i] = container.outputsSync.get().getAEStackInSlot(i);
            }
            checkFluidDisplayStacks(inputs, displayCase.inputs, "server input inventory");
            checkFluidDisplayStacks(outputs, displayCase.outputs, "server output inventory");
            ItemStack encoded = host.getInventoryByName("pattern").getStackInSlot(1);
            if (!displayCase.payload.shouldEncode() && displayCase.replacement == null) {
                require(encoded == null, "plain native fluid import stays editable");
                continue;
            }
            require(
                encoded != null && AEApi.instance().definitions().items().encodedUltimatePattern().isSameAs(encoded),
                "native fluid import produces Ultimate Encoded Pattern");
            var details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, player.worldObj);
            checkFluidDisplayStacks(details.getAEInputs(), displayCase.inputs, "encoded native fluid inputs");
            checkFluidDisplayStacks(details.getAEOutputs(), displayCase.outputs, "encoded native fluid outputs");
        }
        System.out.println("TERMINAL_NATIVE_FLUID_QA: packet round trip, server inventory and Ultimate pattern PASS");
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
            for (ItemStack other : neiRecipe.mInputs)
                if (AEItemStack.create(other).isSameType(AEItemStack.create(expected)))
                    expectedAmount += other.stackSize;
            for (IAEStack<?> input : inputs)
                if (input instanceof IAEItemStack item && item.isSameType(AEItemStack.create(expected)))
                    amount += input.getStackSize();
            require(amount == expectedAmount, "item quantity: " + expected.getDisplayName());
        }
        for (FluidStack expected : neiRecipe.mFluidInputs) {
            long amount = 0;
            for (IAEStack<?> input : inputs)
                if (input instanceof IAEFluidStack fluid && fluid.getFluidStack().isFluidEqual(expected))
                    amount += input.getStackSize();
            require(amount == expected.amount, "fluid quantity: " + expected.getFluid().getName());
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
            InterfaceSuffixButtonClientChecks.run(gui);
            checkNonConsumablesButton();
            if (Boolean.getBoolean("gtng.gtnlCompat.qa")) {
                Class<?> batcher = Class.forName("com.gtnewhorizons.angelica.client.font.BatchingFontRenderer");
                require(Class.forName("com.science.gtnl.client.text.compat.FontBatchBridge").isAssignableFrom(batcher),
                    "GTNL owns Angelica text effects");
                require(
                    !Class.forName("com.xyp.gtnotgood.client.text.compat.FontBatchBridge").isAssignableFrom(batcher),
                    "duplicate GTNG font mixin is disabled");
                String credit = "\u00a7{exotic_rainbow}" + ModList.Names.GT_NOT_GOOD + "\u00a7r";
                require(
                    mc.fontRenderer.getStringWidth(credit) == mc.fontRenderer.getStringWidth(ModList.Names.GT_NOT_GOOD),
                    "GTNL measures GTNG credit without displaying formatting markers");
                mc.fontRenderer.drawStringWithShadow(credit, 4, 4, 0xffffff);
                System.out.println("GTNL_TEXT_QA: upstream renderer ownership, width and draw PASS");
            }
            InvTweaksOrderCacheClientChecks.run();
            ItemSortNameCacheClientChecks.run();
            InterfaceViewportClientChecks.run(guiField("interfaceTerminal"));
            InterfaceRowFilterClientChecks.run(guiField("interfaceTerminal"));
            InterfaceNameClientChecks.run(guiField("interfaceTerminal"));
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
            ContainerQuickEncodingTerminal container = (ContainerQuickEncodingTerminal) gui.inventorySlots;
            FluidDisplayCase preview = fluidDisplayCases.get(0);
            var inputInventory = container.inputsSync.get();
            var outputInventory = container.outputsSync.get();
            for (int i = 0; i < RecipeTransferPayload.SLOT_COUNT; i++) {
                inputInventory.putAEStackInSlot(i, i < preview.inputs.length ? preview.inputs[i].copy() : null);
                outputInventory.putAEStackInSlot(i, i < preview.outputs.length ? preview.outputs[i].copy() : null);
            }
            Arrays.fill((int[]) guiField("patternScrollRows"), 0);
            Method layout = GuiQuickEncodingTerminal.class.getDeclaredMethod("layoutPatternExtension");
            layout.setAccessible(true);
            layout.invoke(gui);
            VirtualMEPatternSlot[] inputs = (VirtualMEPatternSlot[]) guiField("craftingSlots");
            VirtualMEPatternSlot[] outputs = (VirtualMEPatternSlot[]) guiField("outputSlots");
            require(inputs[0].getAEStack() instanceof IAEFluidStack && outputs[0].getAEStack() instanceof IAEFluidStack,
                "native virtual slots render fluids rather than packet items");
        } else if (frames == 70) {
            screenshot("nei-native-fluids.png");
            Files.write(new File(System.getProperty("gtng.terminalScroll.qa.output"), "result.txt").toPath(),
                "PASS".getBytes(StandardCharsets.UTF_8));
            mc.shutdown();
        }
    }

    private void checkVisible(String name, int first) throws Exception {
        VirtualMEPatternSlot[] slots = (VirtualMEPatternSlot[]) guiField(name);
        for (int i = 0; i < slots.length; i++)
            require(slots[i].isHidden() == (i < first || i >= first + 16), name + " visibility " + i);
    }

    private void checkNonConsumablesButton() throws Exception {
        ContainerQuickEncodingTerminal container = (ContainerQuickEncodingTerminal) gui.inventorySlots;
        GuiImgButton button = (GuiImgButton) guiField("keepNonConsumablesButton");
        require(button.visible, "retention button is visible in processing view");
        ActionHandler<Boolean> action = container.setKeepNonConsumablesAction;
        Field manager = AbstractSyncHandler.class.getDeclaredField("manager");
        manager.setAccessible(true);
        List<Boolean> requested = new ArrayList<>();
        ActionHandler<Boolean> capture = new ActionHandler<Boolean>((SyncManager) manager.get(action), action.getKey(),
            action.getFullKey(), SyncDirection.CLIENT_TO_SERVER, StreamCodecs.booleanValue()) {

            @Override
            public void send(Boolean value) {
                requested.add(value);
            }
        };
        Field setter = ContainerQuickEncodingTerminal.class.getDeclaredField("setKeepNonConsumablesAction");
        setter.setAccessible(true);
        setter.set(container, capture);
        container.keepNonConsumablesSync.setLocalValue(false);
        gui.mouseClicked(button.xPosition + 3, button.yPosition + 3, 0);
        require(gui.shouldKeepNonConsumables() && requested.equals(Arrays.asList(true)), "button enables retention");
        gui.drawScreen(button.xPosition + 3, button.yPosition + 3, 0);
        screenshot("keep-non-consumables.png");
        screenshotNonConsumablesThemes(button, true);
        gui.mouseClicked(button.xPosition + 3, button.yPosition + 3, 0);
        require(!gui.shouldKeepNonConsumables() && requested.equals(Arrays.asList(true, false)),
            "button disables retention");
        screenshotNonConsumablesThemes(button, false);
        setter.set(container, action);
        System.out.println("TERMINAL_CATALYST_QA: GUI toggle and tooltip PASS");
    }

    private void screenshotNonConsumablesThemes(GuiImgButton button, boolean enabled) {
        String originalTheme = Config.quickTerminalButtonTheme;
        try {
            for (String theme : new String[] { "light", "dark" }) {
                Config.quickTerminalButtonTheme = theme;
                gui.drawScreen(button.xPosition + 3, button.yPosition + 3, 0);
                screenshot("keep-circuits-" + (enabled ? "on" : "off") + "-" + theme + ".png");
            }
        } finally {
            Config.quickTerminalButtonTheme = originalTheme;
        }
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
        ScreenShotHelper.saveScreenshot(new File(System.getProperty("gtng.terminalScroll.qa.output")), name,
            mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
