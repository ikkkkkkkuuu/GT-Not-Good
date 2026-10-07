package com.xyp.gtnotgood.qa;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AEColor;
import appeng.container.ContainerNull;
import appeng.container.implementations.ContainerCraftingTerm;
import appeng.container.slot.SlotCraftingTerm;
import appeng.helpers.InventoryAction;
import appeng.parts.reporting.PartCraftingTerminal;
import appeng.tile.storage.TileDrive;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.items.MetaGeneratedTool;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;

/** Runs real AE2 result refreshes and Shift crafting after native GT recipe registration. */
@Mod(
    modid = "gtngterminalcraftingqa",
    name = "Terminal Crafting QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class TerminalCraftingChecks {

    private static final Item oneUseToken = new Item().setUnlocalizedName("terminal_crafting_qa_token");
    private final List<String> results = new ArrayList<>();
    private final SearchProbe probe = new SearchProbe();
    private final DynamicRecipe dynamic = new DynamicRecipe(false);
    private final DynamicRecipe fallback = new DynamicRecipe(true);
    private WorldServer world;
    private EntityPlayerMP player;
    private PartCraftingTerminal terminal;
    private ContainerCraftingTerm container;
    private SlotCraftingTerm output;
    private IInventory grid;
    private IMEMonitor<IAEItemStack> storage;
    private int ticks;
    private boolean done;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) throws Exception {
        if (!Boolean.getBoolean("gtng.terminalCrafting.qa")) return;
        GameRegistry.registerItem(oneUseToken, "terminal_crafting_qa_token");
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            // Keep the dedicated-server fixture free from client class verification.
            Class.forName("com.xyp.gtnotgood.qa.TerminalCraftingClientChecks")
                .getConstructor()
                .newInstance();
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || done) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null) return;
        if (!server.isDedicatedServer() && server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            if (++ticks == 1) setup(
                server.worldServerForDimension(0),
                server.isDedicatedServer() ? FakePlayerFactory.getMinecraft(server.worldServerForDimension(0))
                    : (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0));
            if (ticks < 100 || !terminal.getProxy()
                .isActive()) {
                if (ticks > 1200) throw new AssertionError("AE terminal did not become active");
                return;
            }
            storage = terminal.getProxy()
                .getStorage()
                .getItemInventory();
            container = new ContainerCraftingTerm(player.inventory, terminal);
            player.openContainer = container;
            grid = terminal.getInventoryByName("crafting");
            for (Object slot : container.inventorySlots) if (slot instanceof SlotCraftingTerm craft) output = craft;
            require(output != null, "native terminal output slot exists");
            recipeList().add(0, fallback);
            recipeList().add(0, dynamic);
            recipeList().add(0, probe);
            checkNativeMortar();
            checkCraftStack();
            checkOtherTools();
            checkMissingIngredients();
            checkToolBreakage();
            checkRecipeFallback();
            checkOneUseToolRecipe();
            checkVanillaRepair();
            finish(null);
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void setup(WorldServer qaWorld, EntityPlayerMP qaPlayer) {
        world = qaWorld;
        player = qaPlayer;
        player.setPosition(4.5, 80, 6.5);
        var definitions = AEApi.instance()
            .definitions();
        world.setBlock(
            4,
            80,
            4,
            definitions.blocks()
                .multiPart()
                .maybeBlock()
                .get());
        IPartHost host = (IPartHost) world.getTileEntity(4, 80, 4);
        host.addPart(
            definitions.parts()
                .cableGlass()
                .stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN,
            player);
        host.addPart(
            definitions.parts()
                .craftingTerminal()
                .maybeStack(1)
                .get(),
            ForgeDirection.SOUTH,
            player);
        terminal = (PartCraftingTerminal) host.getPart(ForgeDirection.SOUTH);
        world.setBlock(
            4,
            80,
            3,
            definitions.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        world.setBlock(
            5,
            80,
            4,
            definitions.blocks()
                .drive()
                .maybeBlock()
                .get());
        TileDrive drive = (TileDrive) world.getTileEntity(5, 80, 4);
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                definitions.items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
    }

    private void checkNativeMortar() {
        clear();
        ItemStack mortar = mortar();
        setGrid(mortar, new ItemStack(Blocks.gravel));
        ItemStack nativeResult = vanillaResult();
        require(
            nativeResult != null && nativeResult.getItem() == Items.flint && nativeResult.stackSize == 1,
            "registered GT gravel and mortar recipe yields one flint");
        require(same(output.getStack(), nativeResult), "terminal matches native gravel recipe");

        inject(new ItemStack(Blocks.gravel, 64));
        ItemStack expectedTool = mortar.copy();
        for (int i = 0; i < 64; i++) expectedTool = usedTool(expectedTool);
        int before = probe.calls;
        long started = System.nanoTime();
        output.doClick(InventoryAction.CRAFT_SHIFT, player);
        long elapsed = System.nanoTime() - started;
        require(playerCount(new ItemStack(Items.flint)) == 64, "one Shift produces exactly 64 flint");
        require(
            networkCount(new ItemStack(Blocks.gravel)) == 0 && gridCount(new ItemStack(Blocks.gravel)) == 1,
            "Shift consumes exactly 64 of 65 gravel and leaves the grid refilled");
        require(same(grid.getStackInSlot(0), expectedTool), "64 crafts preserve exact native mortar wear NBT");
        require(
            playerToolCount() == 0 && networkCount(mortar) == 0 && grid.getStackInSlot(0).stackSize == 1,
            "mortar stays in the grid without duplication");
        require(probe.calls - before <= 3, "one full Shift does not rescan all recipes after each refill");
        results.add(
            "PASS: real mortar Shift x64, exact gravel/flint quantities and NBT; unknown recipe checks="
                + (probe.calls - before)
                + ", Shift ns="
                + elapsed);
    }

    private void checkCraftStack() {
        clear();
        ItemStack mortar = mortar();
        setGrid(mortar, new ItemStack(Blocks.gravel));
        inject(new ItemStack(Blocks.gravel, 64));
        ItemStack expectedTool = mortar.copy();
        for (int i = 0; i < 64; i++) expectedTool = usedTool(expectedTool);
        int before = probe.calls;
        long started = System.nanoTime();
        output.doClick(InventoryAction.CRAFT_STACK, player);
        long elapsed = System.nanoTime() - started;
        require(
            same(player.inventory.getItemStack(), new ItemStack(Items.flint, 64)),
            "CRAFT_STACK places exactly 64 flint on the cursor");
        require(
            playerCount(new ItemStack(Items.flint)) == 0 && gridCount(new ItemStack(Blocks.gravel)) == 1
                && networkCount(new ItemStack(Blocks.gravel)) == 0,
            "CRAFT_STACK consumes exactly 64 gravel");
        require(same(grid.getStackInSlot(0), expectedTool), "CRAFT_STACK preserves exact native tool wear NBT");
        require(probe.calls - before <= 3, "CRAFT_STACK avoids per-refill recipe-list scans");
        results.add(
            "PASS: native CRAFT_STACK x64, cursor output, exact quantities and NBT; recipe checks="
                + (probe.calls - before)
                + ", ns="
                + elapsed);
    }

    private void checkMissingIngredients() {
        clear();
        ItemStack mortar = mortar();
        setGrid(mortar, new ItemStack(Blocks.gravel));
        inject(new ItemStack(Blocks.gravel, 2));
        ItemStack expectedTool = mortar.copy();
        for (int i = 0; i < 3; i++) expectedTool = usedTool(expectedTool);
        output.doClick(InventoryAction.CRAFT_SHIFT, player);
        require(
            playerCount(new ItemStack(Items.flint)) == 3 && gridCount(new ItemStack(Blocks.gravel)) == 0
                && networkCount(new ItemStack(Blocks.gravel)) == 0,
            "missing input stops Shift after three crafts");
        require(same(grid.getStackInSlot(0), expectedTool), "missing input does not charge extra mortar wear");
        require(output.getStack() == null, "incomplete grid clears the cached result");
        output.doClick(InventoryAction.CRAFT_SHIFT, player);
        require(playerCount(new ItemStack(Items.flint)) == 3, "clicking an empty result does not craft again");
        results.add("PASS: input exhaustion, partial Shift and empty result");
    }

    private void checkOtherTools() {
        ItemStack copper = GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Copper, 1);
        ItemStack plate = GTOreDictUnificator.get(OrePrefixes.plate, Materials.Copper, 1);
        ItemStack stick = GTOreDictUnificator.get(OrePrefixes.stick, Materials.Copper, 1);
        checkToolBatch("hard hammer", IDMetaTool01.HARDHAMMER.ID, copper, plate, new int[] { 3, 6 });
        checkToolBatch("file", IDMetaTool01.FILE.ID, copper, stick, new int[] { 4 });
    }

    private void checkToolBatch(String name, int toolId, ItemStack ingredient, ItemStack result,
        int[] ingredientSlots) {
        clear();
        require(ingredient != null && result != null, "native copper " + name + " recipe items exist");
        ItemStack tool = MetaGeneratedTool01.INSTANCE
            .getToolWithStats(toolId, 1, Materials.Steel, Materials.Wood, null);
        grid.setInventorySlotContents(0, tool.copy());
        for (int slot : ingredientSlots) grid.setInventorySlotContents(slot, ingredient.copy());
        container.onCraftMatrixChanged(grid);
        require(
            same(output.getStack(), vanillaResult()) && same(output.getStack(), result),
            "registered native copper " + name + " recipe matches the real shaped grid");
        ItemStack reserve = ingredient.copy();
        reserve.stackSize = 64 * ingredientSlots.length;
        inject(reserve);
        ItemStack expectedTool = tool.copy();
        for (int i = 0; i < 64; i++) expectedTool = usedTool(expectedTool);
        int before = probe.calls;
        long started = System.nanoTime();
        output.doClick(InventoryAction.CRAFT_SHIFT, player);
        long elapsed = System.nanoTime() - started;
        require(playerCount(result) == 64, "real " + name + " Shift makes exactly 64 outputs");
        require(
            networkCount(ingredient) == 0 && gridCount(ingredient) == ingredientSlots.length,
            name + " consumes exactly the native per-craft quantity and refills every material slot");
        require(
            same(grid.getStackInSlot(0), expectedTool) && grid.getStackInSlot(0).stackSize == 1
                && playerToolCount() == 0
                && networkCount(tool) == 0,
            name + " keeps one tool and its exact native 64-use wear NBT");
        require(probe.calls - before <= 3, name + " avoids per-refill recipe-list scans");
        results.add(
            "PASS: native " + name
                + " Shift x64, copper quantities and exact wear NBT; recipe checks="
                + (probe.calls - before)
                + ", Shift ns="
                + elapsed);
    }

    private void checkToolBreakage() {
        clear();
        ItemStack mortar = mortar();
        require(
            MetaGeneratedTool.setToolDamage(mortar, MetaGeneratedTool.getToolMaxDamage(mortar) - 1),
            "near-broken mortar has one durability unit left");
        require(usedTool(mortar) == null, "native next use breaks the near-broken mortar");
        setGrid(mortar, new ItemStack(Blocks.gravel));
        inject(new ItemStack(Blocks.gravel, 64));
        output.doClick(InventoryAction.CRAFT_SHIFT, player);
        require(playerCount(new ItemStack(Items.flint)) == 1, "broken mortar stops Shift after one craft");
        require(grid.getStackInSlot(0) == null && output.getStack() == null, "broken tool clears recipe output");
        require(
            networkCount(new ItemStack(Blocks.gravel)) + gridCount(new ItemStack(Blocks.gravel)) == 64,
            "tool breakage consumes exactly one gravel");
        results.add("PASS: native mortar breakage stops crafting without extra material use");
    }

    private void checkRecipeFallback() {
        clear();
        setGrid(token(1), null);
        require(
            same(output.getStack(), vanillaResult()) && output.getStack().stackSize == 1,
            "dynamic recipe produces the native first result");
        setGrid(token(2), null);
        require(
            same(output.getStack(), vanillaResult()) && output.getStack().stackSize == 2,
            "dynamic recipe recomputes output using current NBT");
        setGrid(token(3), null);
        require(
            output.getStack() != null && output.getStack()
                .getItem() == Items.diamond && same(output.getStack(), vanillaResult()),
            "failed previous match finds the next recipe");
        setGrid(new ItemStack(Blocks.clay), mortar());
        require(
            same(output.getStack(), vanillaResult()) && output.getStack() != null,
            "switching back to another registered GT mortar recipe falls back correctly");
        setGrid(null, null);
        require(output.getStack() == null, "completely empty grid has no stale result");
        ToolWearRecipe fresh = new ToolWearRecipe(false);
        ToolWearRecipe worn = new ToolWearRecipe(true);
        recipeList().add(0, fresh);
        recipeList().add(0, worn);
        try {
            checkDynamicWearBatch(false);
            checkDynamicWearBatch(true);
        } finally {
            recipeList().remove(fresh);
            recipeList().remove(worn);
        }
        results.add("PASS: dynamic output NBT, recipe fallback, native recipe switch and empty grid");
    }

    private void checkDynamicWearBatch(boolean refill) {
        clear();
        ItemStack mortar = mortar();
        setGrid(mortar, new ItemStack(Blocks.gravel, refill ? 1 : 3));
        if (refill) inject(new ItemStack(Blocks.gravel, 2));
        require(
            same(output.getStack(), new ItemStack(Items.redstone)) && same(output.getStack(), vanillaResult()),
            "fresh tool NBT selects the original dynamic recipe");
        primeDynamicRecipeSelection();
        output.doClick(InventoryAction.CRAFT_SHIFT, player);
        System.out.println(
            "TERMINAL_CRAFTING_QA: dynamic first action refill=" + refill
                + ", redstone="
                + playerCount(new ItemStack(Items.redstone))
                + ", diamond="
                + playerCount(new ItemStack(Items.diamond))
                + ", tool="
                + grid.getStackInSlot(0)
                + ", wear="
                + MetaGeneratedTool.getToolDamage(grid.getStackInSlot(0))
                + ", input="
                + grid.getStackInSlot(1)
                + ", output="
                + output.getStack());
        require(
            playerCount(new ItemStack(Items.redstone)) == 1 && playerCount(new ItemStack(Items.diamond)) == 0,
            "native batch stops when actual tool wear NBT invalidates the original recipe");
        require(
            MetaGeneratedTool.getToolDamage(grid.getStackInSlot(0)) > 0
                && same(output.getStack(), new ItemStack(Items.diamond))
                && same(output.getStack(), vanillaResult()),
            "changed tool NBT selects the current higher-priority recipe after the old match fails");
        output.doClick(InventoryAction.CRAFT_SHIFT, player);
        require(
            playerCount(new ItemStack(Items.diamond)) == 2 && playerCount(new ItemStack(Items.redstone)) == 1,
            "next native action crafts the remaining two inputs using the changed tool recipe");
        ItemStack expected = mortar.copy();
        for (int i = 0; i < 3; i++) expected = usedTool(expected);
        require(
            same(grid.getStackInSlot(0), expected) && gridCount(new ItemStack(Blocks.gravel)) == 0
                && networkCount(new ItemStack(Blocks.gravel)) == 0,
            "dynamic recipe switch preserves native wear and exact total material use");
        results.add(
            "PASS: dynamic tool-wear NBT recipe change, " + (refill ? "ME refill one input" : "three stacked inputs"));
    }

    private void primeDynamicRecipeSelection() {
        InventoryCrafting actual = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int slot = 0; slot < 9; slot++) actual.setInventorySlotContents(slot, grid.getStackInSlot(slot));
        IRecipe previous = Platform.findMatchingRecipe(actual, world);
        System.out.println(
            "TERMINAL_CRAFTING_QA: dynamic native selection before priming="
                + (previous == null ? null
                    : previous.getClass()
                        .getName())
                + ", result="
                + (previous == null ? null : previous.getCraftingResult(actual))
                + ", terminal="
                + output.getStack());
        // Temporary QA recipe insertion does not invalidate AE2's successful-recipe cache.
        // Select an unrelated real recipe first, as switching ordinary terminal inputs would do.
        InventoryCrafting unrelated = new InventoryCrafting(new ContainerNull(), 3, 3);
        unrelated.setInventorySlotContents(0, token(1));
        require(
            Platform.findMatchingRecipe(unrelated, world) != null,
            "unrelated native recipe resets the QA selection");
        IRecipe selected = Platform.findMatchingRecipe(actual, world);
        require(
            selected != null && same(selected.getCraftingResult(actual), output.getStack()),
            "native AE2 selection agrees with the newly inserted dynamic recipe before crafting");
    }

    private void checkOneUseToolRecipe() {
        OneUseToolRecipe recipe = new OneUseToolRecipe();
        recipeList().add(0, recipe);
        try {
            clear();
            ItemStack mortar = mortar();
            InventoryCrafting paperBaseline = new InventoryCrafting(new ContainerNull(), 3, 3);
            paperBaseline.setInventorySlotContents(0, usedTool(mortar));
            paperBaseline.setInventorySlotContents(1, new ItemStack(Items.paper));
            IRecipe paperRecipe = Platform.findMatchingRecipe(paperBaseline, world);
            System.out.println(
                "TERMINAL_CRAFTING_QA: worn mortar plus paper baseline recipe="
                    + (paperRecipe == null ? null
                        : paperRecipe.getClass()
                            .getName())
                    + ", result="
                    + (paperRecipe == null ? null : paperRecipe.getCraftingResult(paperBaseline)));
            setGrid(mortar, new ItemStack(oneUseToken));
            inject(new ItemStack(oneUseToken, 64));
            require(same(output.getStack(), new ItemStack(Items.redstone)), "fresh tool has the one-use recipe");
            primeDynamicRecipeSelection();
            output.doClick(InventoryAction.CRAFT_SHIFT, player);
            require(
                playerCount(new ItemStack(Items.redstone)) == 1,
                "recipe invalidation after tool wear retains the already crafted output");
            require(
                same(grid.getStackInSlot(0), usedTool(mortar))
                    && networkCount(new ItemStack(oneUseToken)) + gridCount(new ItemStack(oneUseToken)) == 64,
                "one-use recipe consumes exactly one isolated token and one native tool use");
            ItemStack vanilla = vanillaResult();
            InventoryCrafting remaining = new InventoryCrafting(new ContainerNull(), 3, 3);
            for (int slot = 0; slot < 9; slot++) remaining.setInventorySlotContents(slot, grid.getStackInSlot(slot));
            IRecipe next = Platform.findMatchingRecipe(remaining, world);
            System.out.println(
                "TERMINAL_CRAFTING_QA: one-use worn tool output=" + output.getStack()
                    + ", vanilla="
                    + vanilla
                    + ", next recipe="
                    + (next == null ? null
                        : next.getClass()
                            .getName())
                    + ", next result="
                    + (next == null ? null : next.getCraftingResult(remaining)));
            require(
                output.getStack() == null && vanilla == null && next == null,
                "refilled item grid with invalid worn-tool NBT clears the result");
            results.add(
                "PASS: one-use dynamic tool recipe, one output retained, exact wear/materials and no stale result");
        } finally {
            recipeList().remove(recipe);
        }
    }

    private void checkVanillaRepair() {
        clear();
        setGrid(new ItemStack(Items.iron_pickaxe, 1, 100), new ItemStack(Items.iron_pickaxe, 1, 150));
        ItemStack expected = vanillaResult();
        require(expected != null && same(output.getStack(), expected), "terminal preserves vanilla repair lookup");
        output.doClick(InventoryAction.CRAFT_ITEM, player);
        require(
            player.inventory.getItemStack() == null,
            "native AE2 repair alias behavior is unchanged by the batch-refill hook");
        require(
            grid.getStackInSlot(0) == null && grid.getStackInSlot(1) == null,
            "native AE2 repair consumes both damaged tools");
        ContainerWorkbench workbench = new ContainerWorkbench(player.inventory, world, 4, 80, 4);
        player.openContainer = workbench;
        workbench.getSlot(1)
            .putStack(new ItemStack(Items.iron_pickaxe, 1, 100));
        workbench.getSlot(2)
            .putStack(new ItemStack(Items.iron_pickaxe, 1, 150));
        require(
            same(
                workbench.getSlot(0)
                    .getStack(),
                expected),
            "vanilla workbench has the same repaired output");
        workbench.slotClick(0, 0, 0, player);
        require(same(player.inventory.getItemStack(), expected), "real vanilla repair returns the exact tool damage");
        require(
            workbench.getSlot(1)
                .getStack() == null
                && workbench.getSlot(2)
                    .getStack() == null,
            "real vanilla repair consumes exactly both damaged tools");
        player.openContainer = container;
        results.add(
            "PASS: native repair lookup and AE2 existing alias baseline; actual vanilla workbench repair has exact output");
    }

    private void clear() {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++)
            player.inventory.setInventorySlotContents(i, null);
        player.inventory.setItemStack(null);
        List<IAEItemStack> stored = new ArrayList<>();
        for (IAEItemStack stack : storage.getStorageList()) stored.add(stack.copy());
        for (IAEItemStack request : stored) {
            storage.extractItems(request, Actionable.MODULATE, new MachineSource(terminal));
        }
        for (int i = 0; i < 9; i++) grid.setInventorySlotContents(i, null);
        container.onCraftMatrixChanged(grid);
    }

    private void setGrid(ItemStack first, ItemStack second) {
        for (int i = 0; i < 9; i++) grid.setInventorySlotContents(i, null);
        grid.setInventorySlotContents(0, first == null ? null : first.copy());
        grid.setInventorySlotContents(1, second == null ? null : second.copy());
        container.onCraftMatrixChanged(grid);
    }

    private ItemStack vanillaResult() {
        InventoryCrafting nativeGrid = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int i = 0; i < 9; i++) nativeGrid.setInventorySlotContents(i, grid.getStackInSlot(i));
        return CraftingManager.getInstance()
            .findMatchingRecipe(nativeGrid, world);
    }

    private void inject(ItemStack stack) {
        require(
            storage.injectItems(AEItemStack.create(stack), Actionable.MODULATE, new MachineSource(terminal)) == null,
            "native drive accepts QA ingredients");
    }

    private long networkCount(ItemStack prototype) {
        IAEItemStack stored = storage.getStorageList()
            .findPrecise(AEItemStack.create(prototype));
        return stored == null ? 0 : stored.getStackSize();
    }

    private int playerCount(ItemStack prototype) {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory) if (sameType(stack, prototype)) count += stack.stackSize;
        return count;
    }

    private int gridCount(ItemStack prototype) {
        int count = 0;
        for (int i = 0; i < 9; i++)
            if (sameType(grid.getStackInSlot(i), prototype)) count += grid.getStackInSlot(i).stackSize;
        return count;
    }

    private int playerToolCount() {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory)
            if (stack != null && stack.getItem() instanceof MetaGeneratedTool) count += stack.stackSize;
        return count;
    }

    private static ItemStack mortar() {
        return MetaGeneratedTool01.INSTANCE
            .getToolWithStats(IDMetaTool01.MORTAR.ID, 1, Materials.Flint, Materials.Stone, null);
    }

    private static ItemStack usedTool(ItemStack tool) {
        return tool == null ? null
            : tool.getItem()
                .getContainerItem(tool.copy());
    }

    private static ItemStack token(int mode) {
        ItemStack token = new ItemStack(Items.paper);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("terminalCraftingQaMode", mode);
        token.setTagCompound(tag);
        return token;
    }

    private static boolean sameType(ItemStack first, ItemStack second) {
        return first != null && second != null && first.isItemEqual(second);
    }

    private static boolean same(ItemStack first, ItemStack second) {
        return ItemStack.areItemStacksEqual(first, second);
    }

    @SuppressWarnings("unchecked")
    private static List<IRecipe> recipeList() {
        return (List<IRecipe>) CraftingManager.getInstance()
            .getRecipeList();
    }

    private void finish(Throwable failure) {
        done = true;
        recipeList().remove(probe);
        recipeList().remove(dynamic);
        recipeList().remove(fallback);
        if (failure != null) {
            results.add("FAIL: " + failure);
            failure.printStackTrace();
        }
        try {
            Path report = Paths.get(System.getProperty("gtng.terminalCrafting.report"));
            Files.createDirectories(report.getParent());
            Files.write(report, results, StandardCharsets.UTF_8);
            Files.write(
                report.resolveSibling("result.txt"),
                (failure == null ? "PASS" : "FAIL").getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        System.out.println("TERMINAL_CRAFTING_QA: " + results);
        FMLCommonHandler.instance()
            .getMinecraftServerInstance()
            .initiateShutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class SearchProbe implements IRecipe {

        private int calls;

        @Override
        public boolean matches(InventoryCrafting inventory, World world) {
            calls++;
            return false;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            return null;
        }

        @Override
        public int getRecipeSize() {
            return 0;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return null;
        }
    }

    private static class ToolWearRecipe implements IRecipe {

        private final boolean worn;
        private final ItemStack ingredient;

        private ToolWearRecipe(boolean worn) {
            this(worn, new ItemStack(Blocks.gravel));
        }

        private ToolWearRecipe(boolean worn, ItemStack ingredient) {
            this.worn = worn;
            this.ingredient = ingredient;
        }

        @Override
        public boolean matches(InventoryCrafting inventory, World world) {
            ItemStack tool = inventory.getStackInSlot(0);
            if (tool == null || tool.getItem() != MetaGeneratedTool01.INSTANCE
                || tool.getItemDamage() / 2 != IDMetaTool01.MORTAR.ID / 2
                || !sameType(inventory.getStackInSlot(1), ingredient)) return false;
            for (int i = 2; i < 9; i++) if (inventory.getStackInSlot(i) != null) return false;
            long damage = MetaGeneratedTool.getToolDamage(tool);
            return worn ? damage > 0 : damage == 0;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            return matches(inventory, null) ? new ItemStack(worn ? Items.diamond : Items.redstone) : null;
        }

        @Override
        public int getRecipeSize() {
            return 2;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return null;
        }
    }

    private static final class OneUseToolRecipe extends ToolWearRecipe {

        private OneUseToolRecipe() {
            super(false, new ItemStack(oneUseToken));
        }
    }

    private static final class DynamicRecipe implements IRecipe {

        private final boolean fallback;

        private DynamicRecipe(boolean fallback) {
            this.fallback = fallback;
        }

        @Override
        public boolean matches(InventoryCrafting inventory, World world) {
            ItemStack input = inventory.getStackInSlot(0);
            if (input == null || input.getItem() != Items.paper || !input.hasTagCompound()) return false;
            for (int i = 1; i < 9; i++) if (inventory.getStackInSlot(i) != null) return false;
            int mode = input.getTagCompound()
                .getInteger("terminalCraftingQaMode");
            return fallback ? mode == 3 : mode == 1 || mode == 2;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            if (!matches(inventory, null)) return null;
            int mode = inventory.getStackInSlot(0)
                .getTagCompound()
                .getInteger("terminalCraftingQaMode");
            return fallback ? new ItemStack(Items.diamond) : new ItemStack(Items.redstone, mode);
        }

        @Override
        public int getRecipeSize() {
            return 1;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return null;
        }
    }
}
