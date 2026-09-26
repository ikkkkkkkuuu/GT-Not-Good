package com.xyp.gtnotgood.common.packaged;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.xyp.gtnotgood.ae2thing.nei.recipes.extractor.ThaumcraftRecipeExtractor;
import com.xyp.gtnotgood.ae2thing.quickterminal.DualTerminalGuiObject;
import com.xyp.gtnotgood.ae2thing.quickterminal.RecipeTransferPayload;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import codechicken.nei.PositionedStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import io.netty.buffer.Unpooled;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileMagicWorkbench;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

/** Opt-in integration checks of native TC recipes, actual AE essentia storage, persistence and 1000 dispatches. */
@Mod(
    modid = "arcaneqa",
    name = "Arcane QA",
    version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public final class ArcaneWorkbenchClientChecks {

    private boolean started;
    private volatile boolean finished;
    private volatile ItemStack previewPattern;
    private DualTerminalGuiObject pendingTerminal;
    private ItemStack pendingTerminalStack;
    private int ticks;
    private int crafted;
    private int frames;
    private TilePackagedProvider provider;
    private TileArcaneWorkbench table;
    private ICraftingPatternDetails details;
    private InventoryCrafting input;
    private IMEMonitor<AEEssentiaStack> essentia;
    private final ArcaneWorkbenchAdapter adapter = new ArcaneWorkbenchAdapter();
    private final AspectList cost = new AspectList().add(Aspect.AIR, 2)
        .add(Aspect.FIRE, 3);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.arcane.qa")) return;
        ThaumcraftApi.addArcaneCraftingRecipe("", new ItemStack(Items.emerald), cost, " B", "B ", 'B', Items.brick);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (finished) {
            if (Boolean.getBoolean("gtng.arcane.terminalView")) {
                if (!(mc.currentScreen instanceof com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal)) {
                    showTerminal(mc);
                }
            } else if (!(mc.currentScreen instanceof CoreScreen)) mc.displayGuiScreen(new CoreScreen());
        } else if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "arcane-qa-" + System.currentTimeMillis(),
                "Arcane QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void server(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var world = player.getServerForPlayer();
        ticks++;
        if (ticks == 1) {
            var blocks = AEApi.instance()
                .definitions()
                .blocks();
            world.setBlock(
                4,
                8,
                4,
                net.minecraft.block.Block.getBlockFromItem(GTNGItemList.WirelessPackagedPatternProvider.getItem()));
            world.setBlock(
                3,
                8,
                4,
                blocks.energyCellCreative()
                    .maybeBlock()
                    .get());
            world.setBlock(
                3,
                8,
                5,
                blocks.drive()
                    .maybeBlock()
                    .get());
            var drive = ((TileDrive) world.getTileEntity(3, 8, 5)).getInternalInventory();
            drive.setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
            drive.setInventorySlotContents(
                1,
                ThEApi.instance()
                    .items().EssentiaCell_64k.getStack());
            world.setBlock(8, 8, 4, ConfigBlocks.blockTable, 15, 3);
            provider = (TilePackagedProvider) world.getTileEntity(4, 8, 4);
            table = (TileArcaneWorkbench) world.getTileEntity(8, 8, 4);
            provider.setOwnerName(player.getCommandSenderName());
            provider.setInventorySlotContents(TilePackagedProvider.CORE, GTNGItemList.ArcaneWorkbenchCore.get(1));
            require(provider.bind(new PackagedTarget(world.provider.dimensionId, 8, 8, 4, 1)), "binding");
            ItemStack wand = new ItemStack(ConfigItems.itemWandCasting);
            table.setInventorySlotContentsSoftly(10, wand);
            table.setInventorySlotContentsSoftly(1, new ItemStack(Items.brick, 12));
            table.setInventorySlotContentsSoftly(3, new ItemStack(Items.brick, 8));
            player.inventory.mainInventory[0] = AEApi.instance()
                .definitions()
                .materials()
                .blankPattern()
                .maybeStack(1)
                .get();
            ArcaneWorkbenchPatterns.capture(provider, player, 0);
            require(
                provider.getStackInSlot(0) != null && player.inventory.mainInventory[0] == null,
                "capture consumes blank");
            require(table.getStackInSlot(1).stackSize == 12, "capture preserves example");
            ItemStack terminalPattern = terminalPattern(player);
            require(
                terminalPattern.getTagCompound()
                    .getTagList(ArcaneWorkbenchPatterns.GRID, 10)
                    .equals(
                        provider.getStackInSlot(0)
                            .getTagCompound()
                            .getTagList(ArcaneWorkbenchPatterns.GRID, 10)),
                "NEI layout equals workbench-captured layout");
            provider.setInventorySlotContents(0, terminalPattern);
            previewPattern = terminalPattern.copy();
            table.setInventorySlotContentsSoftly(1, null);
            table.setInventorySlotContentsSoftly(3, null);
            player.playerNetServerHandler.setPlayerLocation(4.5, 10, 1.5, 0, 15);
            player.capabilities.isFlying = true;
        }
        if (ticks == 80) {
            require(
                provider.getProxy()
                    .isActive(),
                "active AE grid");
            verifyContainerEncoding(player);
            essentia = (IMEMonitor<AEEssentiaStack>) provider.getProxy()
                .getStorage()
                .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
            provider.provideCrafting(
                (ICraftingProviderHelper) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { ICraftingProviderHelper.class },
                    (proxy, method, args) -> {
                        if (method.getName()
                            .equals("addCraftingOption")) details = (ICraftingPatternDetails) args[1];
                        return null;
                    }));
            require(details != null, "advertised captured pattern");
            input = new InventoryCrafting(new appeng.container.ContainerNull(), 3, 3);
            input.setInventorySlotContents(7, new ItemStack(Items.brick, 2));
            checks(player);
            put(Aspect.AIR, 10000);
            put(Aspect.FIRE, 10000);
        }
        if (ticks >= 81 && crafted < 1000) {
            require(provider.pushPattern(details, input), "one successful dispatch per tick " + crafted);
            require(!provider.pushPattern(details, input), "second dispatch in same tick rejected");
            require(provider.queuedJobs() == 0, "no waiting receipt for synchronous output");
            crafted++;
        }
        if (ticks == 1082) {
            long count = provider.getProxy()
                .getStorage()
                .getItemInventory()
                .getStorageList()
                .findPrecise(appeng.util.item.AEItemStack.create(new ItemStack(Items.emerald)))
                .getStackSize();
            require(count == 1000, "1000 outputs reached AE storage: " + count);
            require(amount(Aspect.AIR) < 10000 && amount(Aspect.FIRE) < 10000, "actual network debited");
            NBTTagCompound saved = new NBTTagCompound();
            provider.writeToNBT(saved);
            TilePackagedProvider restored = new TilePackagedProvider();
            restored.setWorldObj(world);
            restored.readFromNBT(saved);
            require(restored.arcaneVisCredit.equals(provider.arcaneVisCredit), "fractional credit persisted");
            require(restored.getStackInSlot(0) != null, "recorded grid persisted");
            System.out.println(
                "ARCANE_QA: PASS native recipes, exact layout, shortage, wand-first, capacity, blocked returns, NBT, 1000 dispatches in 1000 ticks");
            finished = true;
        }
    }

    /** Exercises NEI positions, the wire codec, terminal persistence and native encoding before real dispatch. */
    private ItemStack terminalPattern(EntityPlayerMP player) throws Exception {
        var layout = ThaumcraftRecipeExtractor.arcaneLayout(
            java.util.Arrays.asList(
                new PositionedStack(new ItemStack(Items.brick), 75, 38, false),
                new PositionedStack(new ItemStack(Items.brick), 47, 65, false)));
        IAEStack<?>[] inputs = new IAEStack<?>[16];
        IAEStack<?>[] outputs = new IAEStack<?>[16];
        inputs[0] = AEItemStack.create(new ItemStack(Items.brick, 2));
        outputs[0] = AEItemStack.create(new ItemStack(Items.emerald));
        var buffer = Unpooled.buffer();
        RecipeTransferPayload payload;
        try {
            RecipeTransferPayload.CODEC
                .write(buffer, new RecipeTransferPayload(false, true, 4, false, inputs, outputs, layout));
            payload = RecipeTransferPayload.CODEC.read(buffer);
        } finally {
            buffer.release();
        }
        require(
            payload.shouldEncode() && payload.getArcaneLayout()
                .equals(layout),
            "layout survives transfer packet");
        ItemStack terminalStack = GTNGItemList.WirelessDualInterfaceTerminal.get(1);
        appeng.util.Platform.openNbtData(terminalStack);
        var handler = AEApi.instance()
            .registries()
            .wireless()
            .getWirelessTerminalHandler(terminalStack);
        var terminal = new DualTerminalGuiObject(handler, terminalStack, player, player.worldObj, 0);
        terminal.setInventorySize(16, 16);
        terminal.readInventory();
        terminal.setCraftingRecipe(false);
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(0, payload.getInput(0));
        terminal.getAEInventoryByName(StorageName.CRAFTING_OUTPUT)
            .putAEStackInSlot(0, payload.getOutput(0));
        terminal.setArcaneLayout(payload.getArcaneLayout());
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .materials()
                    .blankPattern()
                    .maybeStack(3)
                    .get());
        terminal.writeInventory();
        // Reopening must not lose the layout before the user presses Encode.
        terminal = new DualTerminalGuiObject(handler, terminalStack, player, player.worldObj, 0);
        terminal.setInventorySize(16, 16);
        terminal.readInventory();
        require(
            terminal.encode(null, null, null, player.getCommandSenderName(), player.worldObj),
            "terminal native encode");
        ItemStack encoded = terminal.getInventoryByName("pattern")
            .getStackInSlot(1)
            .copy();
        require(
            encoded.getTagCompound()
                .getTagList(ArcaneWorkbenchPatterns.GRID, 10)
                .equals(layout),
            "encoded arcane grid");
        require(
            !encoded.getTagCompound()
                .getBoolean("crafting"),
            "arcane remains a processing pattern");
        require(
            !encoded.getTagCompound()
                .getBoolean("substitute"),
            "exact ingredients enforced");
        ItemStack previousOutput = encoded.copy();
        previousOutput.getTagCompound()
            .removeTag(ArcaneWorkbenchPatterns.GRID);
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, previousOutput);
        terminal.setArcaneLayout(layout);
        terminal.writeInventory();
        terminal = new DualTerminalGuiObject(handler, terminalStack, player, player.worldObj, 0);
        terminal.setInventorySize(16, 16);
        terminal.readInventory();
        require(
            terminal.getArcaneLayout()
                .equals(layout),
            "previous encoded output cannot overwrite pending NEI layout");
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, null);
        require(
            terminal.encode(null, null, null, player.getCommandSenderName(), player.worldObj),
            "repeat encoding retains layout");
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.brick, 3)));
        require(
            !terminal.encode(null, null, null, player.getCommandSenderName(), player.worldObj),
            "stale layout rejects changed quantities");
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, encoded.copy());
        require(
            terminal.getArcaneLayout()
                .equals(layout),
            "loading encoded pattern restores layout");
        require(
            terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
                .getAEStackInSlot(0) == null,
            "loading encoded pattern restores shaped holes");
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(1, AEItemStack.create(new ItemStack(Items.stick)));
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(3, AEItemStack.create(new ItemStack(Items.stick)));
        terminal.replaceArcaneIngredient(inputs[0], AEItemStack.create(new ItemStack(Items.stick)));
        require(
            ArcaneWorkbenchPatterns
                .matchesInputs(terminal.getArcaneLayout(), terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)),
            "alternative updates both repeated grid cells");
        ItemStack ordinary = encoded.copy();
        ordinary.getTagCompound()
            .removeTag(ArcaneWorkbenchPatterns.GRID);
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, ordinary);
        require(
            terminal.getArcaneLayout()
                .tagCount() == 0,
            "ordinary pattern clears old layout");
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, encoded.copy());
        pendingTerminal = terminal;
        pendingTerminalStack = terminalStack;
        System.out.println(
            "ARCANE_QA: PASS terminal NEI layout, packet, reopen, native encode, repeat, replacement, stale rejection");
        return encoded;
    }

    /** Runs the actual encode action once the fixture AE node is ready. */
    private void verifyContainerEncoding(EntityPlayerMP player) throws Exception {
        var terminal = pendingTerminal;
        ItemStack terminalStack = pendingTerminalStack;
        ItemStack encoded = previewPattern.copy();
        var layout = encoded.getTagCompound()
            .getTagList(ArcaneWorkbenchPatterns.GRID, 10);
        ItemStack held = player.inventory.mainInventory[0];
        player.inventory.mainInventory[0] = terminalStack;
        try {
            // The isolated terminal fixture is not linked to a wireless access point. Supply the fixture grid's
            // node so the interface delegate can initialize; pattern encoding still uses the real host/inventories.
            var accessPoint = appeng.helpers.WirelessTerminalGuiObject.class.getDeclaredField("myWap");
            accessPoint.setAccessible(true);
            accessPoint.set(
                terminal,
                Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { appeng.api.implementations.tiles.IWirelessAccessPoint.class },
                    (proxy, method, args) -> {
                        if (method.getName()
                            .equals("getActionableNode"))
                            return provider.getProxy()
                                .getNode();
                        if (method.getName()
                            .equals("getGrid"))
                            return provider.getProxy()
                                .getNode()
                                .getGrid();
                        return null;
                    }));
            var container = new com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal(
                player.inventory,
                terminal);
            var encodeMethod = container.getClass()
                .getDeclaredMethod("quickEncode");
            encodeMethod.setAccessible(true);
            encodeMethod.invoke(container);
            encoded = terminal.getInventoryByName("pattern")
                .getStackInSlot(1)
                .copy();
            require(
                encoded.getTagCompound()
                    .getTagList(ArcaneWorkbenchPatterns.GRID, 10)
                    .equals(layout),
                "workbench view encoding action preserves exact layout");
            container.onContainerClosed(player);
        } finally {
            player.inventory.mainInventory[0] = held;
        }
        provider.setInventorySlotContents(0, encoded);
        previewPattern = encoded.copy();
    }

    private void checks(EntityPlayerMP player) {
        ItemStack original = table.getStackInSlot(10)
            .copy();
        ItemWandCasting wand = (ItemWandCasting) original.getItem();
        TileMagicWorkbench grid = ArcaneWorkbenchPatterns.grid(details.getPattern(), player.worldObj);
        require(
            grid.getStackInSlot(0) == null && grid.getStackInSlot(1) != null && grid.getStackInSlot(3) != null,
            "shaped holes preserved");
        require(ArcaneWorkbenchAdapter.matchesIngredients(grid, input), "condensed reordered ingredients");
        input.setInventorySlotContents(8, new ItemStack(Items.stick));
        require(!ArcaneWorkbenchAdapter.matchesIngredients(grid, input), "extra material rejected");
        input.setInventorySlotContents(8, null);
        require(
            dispatch() == null && ItemStack.areItemStackTagsEqual(original, table.getStackInSlot(10)),
            "empty network no wand debit");
        put(Aspect.AIR, 100);
        require(dispatch() == null && amount(Aspect.AIR) == 100, "second aspect shortage leaves first untouched");
        put(Aspect.FIRE, 100);
        for (int i = 36; i < 45; i++) provider.setInventorySlotContents(i, new ItemStack(Items.stick, 64));
        require(
            dispatch() == null && amount(Aspect.AIR) == 100 && amount(Aspect.FIRE) == 100,
            "full output buffer no debit");
        clearReturns();
        table.setInventorySlotContentsSoftly(0, new ItemStack(Items.stick));
        require(dispatch() == null && amount(Aspect.AIR) == 100, "occupied table no debit");
        table.setInventorySlotContentsSoftly(0, null);
        for (Aspect aspect : cost.getAspects()) wand.storeVis(table.getStackInSlot(10), aspect, 1000);
        require(
            dispatch() != null && amount(Aspect.AIR) == 100 && amount(Aspect.FIRE) == 100,
            "wand first without network debit");
        clearReturns();
        table.setInventorySlotContentsSoftly(10, original.copy());
        ItemStack oversized = details.getPattern()
            .copy();
        ThaumcraftApi.addArcaneCraftingRecipe(
            "",
            new ItemStack(Items.diamond),
            new AspectList().add(Aspect.AIR, 100000),
            "B",
            'B',
            Items.stick);
        TileMagicWorkbench large = new TileMagicWorkbench();
        large.setInventorySlotContentsSoftly(0, new ItemStack(Items.stick));
        oversized = ArcaneWorkbenchPatterns.encode(large, new ItemStack(Items.diamond));
        ICraftingPatternDetails excessive = ((ICraftingPatternItem) oversized.getItem())
            .getPatternForItem(oversized, player.worldObj);
        InventoryCrafting single = new InventoryCrafting(new appeng.container.ContainerNull(), 3, 3);
        single.setInventorySlotContents(0, new ItemStack(Items.stick));
        require(
            adapter.dispatch(provider, provider.targets.get(0), excessive, single) == null
                && provider.altarStatus == AltarStatus.ARCANE_CAPACITY
                && amount(Aspect.AIR) == 100,
            "capacity enforced");
        require(dispatch() != null, "network covers deficit");
        for (Aspect aspect : cost.getAspects()) {
            int needed = (int) (cost.getAmount(aspect) * 100
                * wand.getConsumptionModifier(original, player, aspect, true));
            require(amount(aspect) == 100 - (needed + 99) / 100, "exact essentia debit " + aspect.getTag());
            require(
                provider.arcaneVisCredit.getOrDefault(aspect.getTag(), 0L) == ((needed + 99) / 100) * 100L - needed,
                "fractional change retained");
        }
        clearReturns();
        NBTTagCompound fractionalSave = new NBTTagCompound();
        provider.writeToNBT(fractionalSave);
        TilePackagedProvider fractionalRestored = new TilePackagedProvider();
        fractionalRestored.setWorldObj(player.worldObj);
        fractionalRestored.readFromNBT(fractionalSave);
        require(
            fractionalRestored.arcaneVisCredit.equals(provider.arcaneVisCredit),
            "fractional balance NBT round trip");
        ThaumcraftApi.addArcaneCraftingRecipe("", new ItemStack(Items.gold_ingot), cost, "B", 'B', Items.milk_bucket);
        TileMagicWorkbench bucketGrid = new TileMagicWorkbench();
        bucketGrid.setInventorySlotContentsSoftly(0, new ItemStack(Items.milk_bucket));
        ItemStack bucketPattern = ArcaneWorkbenchPatterns.encode(bucketGrid, new ItemStack(Items.gold_ingot));
        var bucketDetails = ((ICraftingPatternItem) bucketPattern.getItem())
            .getPatternForItem(bucketPattern, player.worldObj);
        InventoryCrafting bucketInput = new InventoryCrafting(new appeng.container.ContainerNull(), 3, 3);
        bucketInput.setInventorySlotContents(0, new ItemStack(Items.milk_bucket));
        require(
            adapter.dispatch(provider, provider.targets.get(0), bucketDetails, bucketInput) != null,
            "container recipe");
        boolean returnedBucket = false;
        for (int i = 36; i < 45; i++) {
            ItemStack returned = provider.getStackInSlot(i);
            returnedBucket |= returned != null && returned.getItem() == Items.bucket && returned.stackSize == 1;
        }
        require(returnedBucket, "container returned to provider instead of player inventory");
        clearReturns();
        essentia.extractItems(new AEEssentiaStack(Aspect.AIR, 10000), Actionable.MODULATE, new MachineSource(provider));
        essentia
            .extractItems(new AEEssentiaStack(Aspect.FIRE, 10000), Actionable.MODULATE, new MachineSource(provider));
        provider.arcaneVisCredit.clear();
        table.setInventorySlotContentsSoftly(10, original);
    }

    private ItemStack dispatch() {
        return adapter.dispatch(provider, provider.targets.get(0), details, input);
    }

    private void clearReturns() {
        for (int i = 36; i < 45; i++) provider.setInventorySlotContents(i, null);
    }

    private void put(Aspect aspect, long count) {
        require(
            essentia.injectItems(new AEEssentiaStack(aspect, count), Actionable.MODULATE, new MachineSource(provider))
                == null,
            "essentia injection");
    }

    private long amount(Aspect aspect) {
        var stack = essentia.getStorageList()
            .findPrecise(new AEEssentiaStack(aspect, 1));
        return stack == null ? 0 : stack.getStackSize();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("ARCANE_QA: " + message);
    }

    /** Renders the real terminal with synchronized arcane state after server-side dispatch verification. */
    private void showTerminal(Minecraft mc) {
        ItemStack item = GTNGItemList.WirelessDualInterfaceTerminal.get(1);
        appeng.util.Platform.openNbtData(item);
        mc.thePlayer.inventory.mainInventory[0] = item;
        var handler = AEApi.instance()
            .registries()
            .wireless()
            .getWirelessTerminalHandler(item);
        var host = new DualTerminalGuiObject(handler, item, mc.thePlayer, mc.theWorld, 0);
        var gui = new com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal(
            mc.thePlayer.inventory,
            host);
        host.setCraftingRecipe(false);
        host.getInventoryByName("pattern")
            .setInventorySlotContents(1, previewPattern.copy());
        var container = (com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal) gui.inventorySlots;
        container.craftingModeSync.setLocalValue(false);
        container.arcaneModeSync.setLocalValue(true);
        container.arcaneWorkbenchViewSync.setLocalValue(true);
        // Native pattern loading is server-only. Supply the same inventory state that a real open GUI receives
        // from its server sync handlers, instead of relying on a client-side output-slot insertion to decode it.
        var layout = previewPattern.getTagCompound()
            .getTagList(ArcaneWorkbenchPatterns.GRID, 10);
        for (int i = 0; i < 16; i++) {
            container.inputsSync.get()
                .putAEStackInSlot(
                    i,
                    i < 9 ? AEItemStack.create(ItemStack.loadItemStackFromNBT(layout.getCompoundTagAt(i))) : null);
            container.outputsSync.get()
                .putAEStackInSlot(i, i == 0 ? AEItemStack.create(new ItemStack(Items.emerald)) : null);
        }
        mc.displayGuiScreen(gui);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || !(mc.currentScreen instanceof CoreScreen) && !(Boolean
            .getBoolean("gtng.arcane.terminalView")
            && mc.currentScreen instanceof com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal))
            return;
        if (++frames == 40) {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                Boolean.getBoolean("gtng.arcane.terminalView") ? "arcane-workbench-terminal-qa.png"
                    : "arcane-core-qa.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            Files.write(new File("arcane-qa-result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
            mc.shutdown();
        }
    }

    /** Uses the registered core renderer, without substituting an illustration. */
    private static final class CoreScreen extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawDefaultBackground();
            drawCenteredString(
                fontRendererObj,
                "Arcane Workbench Packaged Core - 1000 craft QA",
                width / 2,
                height / 2 - 40,
                0xffffff);
            net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
            new RenderItem().renderItemAndEffectIntoGUI(
                fontRendererObj,
                mc.getTextureManager(),
                GTNGItemList.ArcaneWorkbenchCore.get(1),
                width / 2 - 8,
                height / 2);
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        }
    }
}
