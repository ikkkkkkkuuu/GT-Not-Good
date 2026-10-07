package com.xyp.gtnotgood.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

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
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.common.blocks.stockio.StockIOLogic;
import com.xyp.gtnotgood.common.blocks.stockio.StockIOLogicTest;
import com.xyp.gtnotgood.common.blocks.stockio.StockIOSnapshot;
import com.xyp.gtnotgood.common.blocks.stockio.TileStockIOInterface;
import com.xyp.gtnotgood.common.parts.stockio.PartStockIOInterface;
import com.xyp.gtnotgood.common.parts.stockio.StockIOGuiFactory;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.me.helpers.AENetworkProxy;
import appeng.tile.storage.TileDrive;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.ShapelessRecipeHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/** Disposable real ME grids and native GT machines; no fixtures enter the release jar. */
@Mod(modid = "stockioqa", name = "Stock IO QA", version = "1", dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class StockIOClientChecks {

    private boolean started;
    private volatile boolean finished;
    private volatile int stage;
    private int ticks;
    private int automaticStart;
    private int blockFrames;
    private volatile NBTTagCompound conversionTag;
    private boolean neiConversionsChecked;
    private PartStockIOInterface part;
    private TileStockIOInterface block;
    private Fixture partFixture;
    private Fixture blockFixture;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.stockio.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("stockio-qa-" + System.currentTimeMillis(), "Stock IO QA",
                new WorldSettings(39L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (!finished && !neiConversionsChecked && mc.theWorld != null && conversionTag != null) {
            try {
                verifyNEIConversions(conversionTag);
                neiConversionsChecked = true;
            } catch (Throwable failure) {
                fail(failure);
            }
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            ticks++;
            if (ticks == 1) {
                verifyTransactions();
                setup(player);
                conversionTag = conversionData(partFixture);
                verifyConversions(player.worldObj, conversionTag);
            }
            if (ticks > 1600) throw new AssertionError("Stock IO QA timed out at stage " + stage);
            if (stage == 0 && ticks >= 100 && partFixture.proxy.isActive() && blockFixture.proxy.isActive()) {
                verifyNative(partFixture);
                verifyNative(blockFixture);
                prepareAutomatic(partFixture);
                prepareAutomatic(blockFixture);
                automaticStart = ticks;
                stage = 1;
            } else if (stage == 1 && ticks - automaticStart >= 100) {
                require(partFixture.items(Items.emerald) == 1 && blockFixture.items(Items.emerald) == 1,
                    "both empty GT machines automatically process and recycle exactly one product");
                verifyAutomatic(partFixture);
                verifyAutomatic(blockFixture);
                prepareMarkedAutomatic(partFixture);
                prepareMarkedAutomatic(blockFixture);
                automaticStart = ticks;
                stage = 5;
            } else if (stage == 5 && ticks - automaticStart >= 100) {
                require(partFixture.items(Items.emerald) == 2 && blockFixture.items(Items.emerald) == 2,
                    "marked products are still recovered from explicit native GT output slots");
                verifyAutomatic(partFixture, 2);
                verifyAutomatic(blockFixture, 2);
                prepareInputProtection(partFixture);
                prepareInputProtection(blockFixture);
                automaticStart = ticks;
                stage = 6;
            } else if (stage == 6 && ticks - automaticStart >= 20) {
                verifyInputProtection(partFixture);
                verifyInputProtection(blockFixture);
                prepareGui(partFixture);
                prepareGui(blockFixture);
                player.playerNetServerHandler.setPlayerLocation(1.5, 10, -2.5, 0, 15);
                StockIOGuiFactory.INSTANCE.open(player, part);
                stage = 2;
            } else if (stage == 3) {
                require(partFixture.logic.getPolicy(false, 0).reserve == 10_000_000_000L
                    && partFixture.logic.getPolicy(false, 0).batch == 7,
                    "item popup quantities synchronize to the authoritative server");
                require(partFixture.logic.getPolicy(true, 0).reserve == 1000
                    && partFixture.logic.getPolicy(true, 0).batch == 2500,
                    "fluid popup quantities synchronize to the authoritative server");
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(5.5, 10, -2.5, 0, 15);
                StockIOGuiFactory.INSTANCE.open(player, block);
                stage = 4;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void verifyTransactions() {
        Result result = JUnitCore.runClasses(StockIOLogicTest.class);
        for (var failure : result.getFailures()) System.err.println(failure.getTrace());
        require(result.getRunCount() == 11 && result.getIgnoreCount() == 0 && result.getFailureCount() == 0,
            "all eleven native ME transaction tests execute and pass in the Forge client");
    }

    private static NBTTagCompound conversionData(Fixture fixture) {
        NBTTagCompound original = new NBTTagCompound();
        fixture.logic.writeContents(original);
        try {
            configure(fixture);
            ItemStack marked = new ItemStack(Items.apple);
            marked.setTagCompound(new NBTTagCompound());
            NBTTagCompound identity = new NBTTagCompound();
            identity.setString("value", "original");
            marked.getTagCompound().setTag("qaIdentity", identity);
            fixture.logic.setItemFilter(0, marked);
            fixture.logic.getPolicy(false, 0).reserve = 10_000_000_000L;
            fixture.logic.getPolicy(false, 0).batch = 7;
            fixture.logic.setEnabled(false);
            fixture.logic.setAutoPullItems(true);
            fixture.logic.setAutoPullFluids(true);
            fixture.logic.setRefreshTime(7);
            fixture.logic.setMinItemAutoPull(13);
            fixture.logic.setMinFluidAutoPull(99);
            NBTTagCompound tag = new NBTTagCompound();
            fixture.logic.writeContents(tag);
            NBTTagCompound itemRefund = new NBTTagCompound();
            Platform.writeStackNBT(AEItemStack.create(marked).setStackSize(6_000_000_000L), itemRefund, true);
            itemRefund.setBoolean("rollback", true);
            NBTTagCompound fluidRefund = new NBTTagCompound();
            Platform.writeStackNBT(AEFluidStack.create(water(1)).setStackSize(9_000_000_000L), fluidRefund, true);
            fluidRefund.setBoolean("rollback", false);
            NBTTagList refunds = new NBTTagList();
            refunds.appendTag(itemRefund);
            refunds.appendTag(fluidRefund);
            tag.setTag("stockIOEscrow", refunds);
            require(Platform.readStackNBT(itemRefund, false).getStackSize() == 6_000_000_000L
                && Platform.readStackNBT(fluidRefund, false).getStackSize() == 9_000_000_000L,
                "conversion fixture contains native long-sized item and fluid refunds");
            return tag;
        } finally {
            fixture.logic.readContents(original);
        }
    }

    private static void verifyConversions(World world, NBTTagCompound tag) {
        verifyConversion(world, GTNGItemList.StockIOInterface, GTNGItemList.StockIOInterfacePart);
        verifyConversion(world, GTNGItemList.StockIOInterfacePart, GTNGItemList.StockIOInterface);
        InventoryCrafting inventory = craftingGrid(3);
        ItemStack original = GTNGItemList.StockIOInterface.get(1);
        original.setTagCompound((NBTTagCompound) tag.copy());
        inventory.setInventorySlotContents(8, original);
        ItemStack panel = CraftingManager.getInstance().findMatchingRecipe(inventory, world);
        require(sameType(panel, GTNGItemList.StockIOInterfacePart.get(1)) && panel.stackSize == 1
            && tag.equals(panel.getTagCompound()) && panel.getTagCompound() != original.getTagCompound(),
            "registered block-to-panel crafting preserves all configuration and refunds independently");
        inventory.setInventorySlotContents(8, null);
        inventory.setInventorySlotContents(4, panel);
        ItemStack roundTrip = CraftingManager.getInstance().findMatchingRecipe(inventory, world);
        require(sameType(roundTrip, original) && roundTrip.stackSize == 1
            && tag.equals(roundTrip.getTagCompound()) && roundTrip.getTagCompound() != panel.getTagCompound(),
            "registered block-to-panel-to-block crafting preserves all configuration and refunds");
        NBTTagCompound nested = roundTrip.getTagCompound().getTagList("stockIOItems", 10).getCompoundTagAt(0)
            .getCompoundTag("item").getCompoundTag("tag").getCompoundTag("qaIdentity");
        require("original".equals(nested.getString("value")), "converted ghost retains its nested item identity NBT");
        nested.setString("value", "changed");
        roundTrip.getTagCompound().getTagList("stockIOEscrow", 10).getCompoundTagAt(0).setBoolean("rollback", false);
        require(!tag.equals(roundTrip.getTagCompound()) && tag.equals(panel.getTagCompound())
            && tag.equals(original.getTagCompound()), "nested settings/refund edits cannot mutate either crafting ingredient");
    }

    private static void verifyConversion(World world, GTNGItemList input, GTNGItemList output) {
        ItemStack sourceType = input.get(1);
        ItemStack outputType = output.get(1);
        InventoryCrafting probe = craftingGrid(2);
        probe.setInventorySlotContents(0, sourceType);
        IRecipe registered = null;
        int matches = 0;
        for (Object entry : CraftingManager.getInstance().getRecipeList()) {
            IRecipe recipe = (IRecipe) entry;
            if (sameType(recipe.getRecipeOutput(), outputType) && recipe.matches(probe, world)) {
                registered = recipe;
                matches++;
            }
        }
        require(matches == 1 && registered.getRecipeSize() == 1 && registered.getRecipeOutput().stackSize == 1,
            input + " has exactly one registered one-to-one conversion to " + output);
        require(!registered.getRecipeOutput().hasTagCompound(), "registered conversion output is an unconfigured template");
        for (int width : new int[] {2, 3}) {
            for (int slot = 0; slot < width * width; slot++) {
                InventoryCrafting inventory = craftingGrid(width);
                ItemStack source = input.get(3);
                inventory.setInventorySlotContents(slot, source);
                ItemStack result = CraftingManager.getInstance().findMatchingRecipe(inventory, world);
                require(sameType(result, outputType) && result.stackSize == 1
                    && source.stackSize == 3 && !result.hasTagCompound(),
                    input + " converts one unit in " + width + "x" + width + " slot " + slot);
            }
            InventoryCrafting invalid = craftingGrid(width);
            invalid.setInventorySlotContents(0, sourceType.copy());
            for (ItemStack extra : new ItemStack[] {new ItemStack(Items.feather), sourceType.copy()}) {
                invalid.setInventorySlotContents(width * width - 1, extra);
                require(!registered.matches(invalid, world) && registered.getCraftingResult(invalid) == null
                    && CraftingManager.getInstance().findMatchingRecipe(invalid, world) == null,
                    input + " conversion rejects extra " + extra.getItem() + " in " + width + "x" + width);
            }
        }
    }

    private static InventoryCrafting craftingGrid(int width) {
        return new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, width, width);
    }

    private static void verifyNEIConversions(NBTTagCompound tag) {
        for (GTNGItemList input : new GTNGItemList[] {GTNGItemList.StockIOInterface, GTNGItemList.StockIOInterfacePart}) {
            GTNGItemList output = input == GTNGItemList.StockIOInterface
                ? GTNGItemList.StockIOInterfacePart : GTNGItemList.StockIOInterface;
            for (boolean configured : new boolean[] {false, true}) {
                ItemStack ingredient = input.get(1);
                ItemStack result = output.get(1);
                if (configured) {
                    ingredient.setTagCompound((NBTTagCompound) tag.copy());
                    result.setTagCompound((NBTTagCompound) tag.copy());
                }
                ShapelessRecipeHandler crafting = new ShapelessRecipeHandler();
                crafting.loadCraftingRecipes(result);
                requireNativeConversion(crafting, ingredient, result, "crafting", configured);
                ShapelessRecipeHandler usage = new ShapelessRecipeHandler();
                usage.loadUsageRecipes(ingredient);
                requireNativeConversion(usage, ingredient, result, "usage", configured);
            }
        }
    }

    private static void requireNativeConversion(ShapelessRecipeHandler handler, ItemStack input, ItemStack output,
        String query, boolean configured) {
        int matches = 0;
        for (int index = 0; index < handler.numRecipes(); index++) {
            PositionedStack result = handler.getResultStack(index);
            List<PositionedStack> ingredients = handler.getIngredientStacks(index);
            if (result == null || !sameType(result.item, output) || ingredients.size() != 1) continue;
            PositionedStack ingredient = ingredients.get(0);
            if (!Arrays.stream(ingredient.items).anyMatch(stack -> sameType(stack, input))) continue;
            require(result.item.stackSize == 1 && ingredient.item.stackSize == 1 && handler.isRecipe2x2(index),
                "native NEI conversion displays exactly one input/output and supports the player crafting grid");
            matches++;
        }
        require(matches == 1, "native NEI " + query + " query exposes " + input.getItem() + ":" + input.getItemDamage()
            + " -> " + output.getItem() + ":" + output.getItemDamage() + " (configured=" + configured + ")");
    }

    private static boolean sameType(ItemStack left, ItemStack right) {
        return left != null && right != null && left.isItemEqual(right);
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        var definitions = AEApi.instance().definitions();
        world.setBlock(0, 10, 0, definitions.blocks().multiPart().maybeBlock().get());
        IPartHost host = (IPartHost) world.getTileEntity(0, 10, 0);
        require(host.addPart(definitions.parts().cableGlass().stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN, player) != null, "native AE cable placed");
        require(host.addPart(GTNGItemList.StockIOInterfacePart.get(1), ForgeDirection.SOUTH, player) != null,
            "interface uses native cable-part placement");
        part = (PartStockIOInterface) host.getPart(ForgeDirection.SOUTH);
        world.setBlock(4, 10, 0, Block.getBlockFromItem(GTNGItemList.StockIOInterface.get(1).getItem()));
        block = (TileStockIOInterface) world.getTileEntity(4, 10, 0);
        block.setOwnerName(player.getCommandSenderName());
        block.setTargetSide(ForgeDirection.SOUTH);
        partFixture = fixture(player, 0, part.getLogic(), part.getProxy());
        blockFixture = fixture(player, 4, block.getLogic(), block.getProxy());
        GTValues.RA.stdBuilder().itemInputs(new ItemStack(Items.apple, 2), new ItemStack(Items.feather, 3))
            .fluidInputs(water(1250)).itemOutputs(new ItemStack(Items.emerald))
            .fluidOutputs(new FluidStack(FluidRegistry.LAVA, 500)).duration(40).eut(8)
            .addTo(partFixture.machine.getRecipeMap());
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(1.5, 10, -2.5, 0, 15);
    }

    private Fixture fixture(EntityPlayerMP player, int x, StockIOLogic logic, AENetworkProxy proxy) {
        World world = player.worldObj;
        var definitions = AEApi.instance().definitions();
        world.setBlock(x - 1, 10, 0, definitions.blocks().energyCellCreative().maybeBlock().get());
        world.setBlock(x + 1, 10, 0, definitions.blocks().drive().maybeBlock().get());
        TileDrive drive = (TileDrive) world.getTileEntity(x + 1, 10, 0);
        drive.getInternalInventory().setInventorySlotContents(0, definitions.items().cell64k().maybeStack(1).get());
        drive.getInternalInventory().setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
        world.setBlock(x, 10, 1, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(x, 10, 1);
        base.setInitialValuesAsNBT(null, (short) ItemList.Machine_LV_ChemicalReactor.get(1).getItemDamage());
        base.setOwnerUuid(player.getUniqueID());
        ((MTEBasicMachine) base.getMetaTileEntity()).setMainFacing(ForgeDirection.WEST);
        base.setFrontFacing(ForgeDirection.NORTH);
        base.disableWorking();
        require(((MTEBasicMachine) base.getMetaTileEntity()).getTankInfo(ForgeDirection.NORTH).length == 2,
            "native chemical reactor exposes both input and output fluid tanks");
        return new Fixture(logic, proxy, new MachineSource(drive), base);
    }

    private void verifyNative(Fixture fixture) throws Exception {
        StockIOLogic logic = fixture.logic;
        MTEBasicMachine machine = fixture.machine;
        configure(fixture);
        fixture.setItems(Items.apple, 20);
        fixture.setItems(Items.feather, 30);
        fixture.setFluid(10000);
        require(machine.checkRecipe(true) != 2, "fixed batch shortages cannot start a GT recipe");
        require(fixture.items(Items.apple) == 20 && fixture.items(Items.feather) == 30 && fixture.fluid() == 10000,
            "rejected checks leave mixed AE resources untouched");
        fixture.setItems(Items.feather, 31);
        fixture.setFluid(10250);
        require(machine.checkRecipe(true) == 2, "native GT lookup and consumption use interface item/fluid snapshots");
        require(fixture.items(Items.apple) == 18 && fixture.items(Items.feather) == 28 && fixture.fluid() == 9000,
            "accepted native GT check debits exact recipe quantities and retains configured reserves");
        emptyInputs(machine);
        clearPendingRecipe(machine);

        logic.setFixedMode(false);
        logic.setLimitedMode(false);
        fixture.setItems(Items.apple, 20);
        fixture.setItems(Items.feather, 30);
        fixture.setFluid(10000);
        machine.setInventorySlotContents(machine.getOutputSlot(), new ItemStack(Items.emerald, 64));
        require(machine.checkRecipe(true) != 2, "blocked native output prevents ME consumption");
        require(fixture.items(Items.apple) == 20 && fixture.fluid() == 10000, "blocked output retains all ME inputs");
        machine.setInventorySlotContents(machine.getOutputSlot(), null);
        machine.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Items.apple));
        machine.setInventorySlotContents(machine.getInputSlot() + 1, new ItemStack(Items.feather));
        machine.setFillableStack(water(500));
        require(machine.checkRecipe(true) == 2, "native GT aggregates local inputs before ME inputs");
        require(fixture.items(Items.apple) == 19 && fixture.items(Items.feather) == 28 && fixture.fluid() == 9250,
            "mixed local/ME recipe charges only the shortfall to the network");
        emptyInputs(machine);
        for (int i = 0; i < machine.mInputSlotCount; i++) machine.setInventorySlotContents(machine.getInputSlot() + i, null);
        machine.setFillableStack(null);
        clearPendingRecipe(machine);

        StockIOSnapshot snapshot = logic.startRecipe();
        require(snapshot != null, "recipe snapshot starts without removing network stock");
        snapshot.items[0].stackSize--;
        snapshot.fluids[0].amount -= 1000;
        long apples = fixture.items(Items.apple);
        fixture.setFluid(0);
        require(!logic.endRecipe(snapshot) && fixture.items(Items.apple) == apples,
            "another consumer removing fluid cancels the whole mixed transaction");
        NBTTagCompound settings = new NBTTagCompound();
        logic.writeContents(settings);
        logic.setItemFilter(0, null);
        logic.readContents(settings);
        require(logic.itemFilters[0] != null && logic.itemFilters[0].stackSize == 1,
            "save/reload restores ghost marks without real input stacks");
        require(fixture.items(Items.apple) == apples, "configuration round trip cannot duplicate ME stock");
        logic.setEnabled(false);
        require(machine.checkRecipe(true) != 2, "disabled interface cannot start an empty GT machine");
        logic.setEnabled(true);
    }

    private static void configure(Fixture fixture) {
        StockIOLogic logic = fixture.logic;
        logic.setRecycle(false);
        logic.setItemFilter(0, new ItemStack(Items.apple));
        logic.setItemFilter(1, new ItemStack(Items.feather));
        logic.setFluidFilter(0, water(1));
        logic.getPolicy(false, 0).reserve = 18;
        logic.getPolicy(false, 0).batch = 2;
        logic.getPolicy(false, 1).reserve = 28;
        logic.getPolicy(false, 1).batch = 3;
        logic.getPolicy(true, 0).reserve = 9000;
        logic.getPolicy(true, 0).batch = 1250;
        logic.setLimitedMode(true);
        logic.setFixedMode(true);
    }

    private static void prepareAutomatic(Fixture fixture) throws Exception {
        configure(fixture);
        fixture.setItems(Items.apple, 20);
        fixture.setItems(Items.feather, 31);
        fixture.setFluid(10250);
        fixture.setItems(Items.emerald, 0);
        fixture.setFluid(FluidRegistry.LAVA, 0);
        fixture.logic.setRecycle(true);
        fixture.base.setStoredEU(fixture.base.getEUCapacity());
        fixture.base.enableWorking();
        emptyInputs(fixture.machine);
    }

    private static void verifyAutomatic(Fixture fixture) throws Exception {
        verifyAutomatic(fixture, 1);
    }

    private static void verifyAutomatic(Fixture fixture, int completed) throws Exception {
        fixture.base.disableWorking();
        emptyInputs(fixture.machine);
        require(fixture.items(Items.apple) == 18 && fixture.items(Items.feather) == 28 && fixture.fluid() == 9000,
            "ordinary GT ticks process one fixed batch without crossing reserves");
        require(fixture.machine.getStackInSlot(fixture.machine.getOutputSlot()) == null,
            "same machine face returns completed product to ME");
        require(fixture.machine.getDrainableStack() == null && fixture.fluid(FluidRegistry.LAVA) == 500L * completed,
            "same machine face recycles completed fluid output regardless of input marks");
    }

    private static void prepareMarkedAutomatic(Fixture fixture) throws Exception {
        fixture.logic.setItemFilter(2, new ItemStack(Items.emerald));
        fixture.logic.setFluidFilter(1, new FluidStack(FluidRegistry.LAVA, 1));
        fixture.setItems(Items.apple, 20);
        fixture.setItems(Items.feather, 31);
        fixture.setFluid(10250);
        fixture.base.setStoredEU(fixture.base.getEUCapacity());
        fixture.base.enableWorking();
        emptyInputs(fixture.machine);
    }

    private static void prepareInputProtection(Fixture fixture) {
        fixture.machine.setInventorySlotContents(fixture.machine.getInputSlot(), new ItemStack(Items.emerald));
        fixture.machine.setFillableStack(new FluidStack(FluidRegistry.LAVA, 750));
    }

    private static void verifyInputProtection(Fixture fixture) throws Exception {
        ItemStack localItem = fixture.machine.getStackInSlot(fixture.machine.getInputSlot());
        FluidStack localFluid = fixture.machine.getFillableStack();
        require(localItem != null && localItem.getItem() == Items.emerald && localItem.stackSize == 1
            && localFluid != null && localFluid.getFluid() == FluidRegistry.LAVA && localFluid.amount == 750
            && fixture.items(Items.emerald) == 2 && fixture.fluid(FluidRegistry.LAVA) == 1000,
            "native recovery never touches input slots or the fillable tank even when they hold product identities");
        fixture.machine.setInventorySlotContents(fixture.machine.getInputSlot(), null);
        fixture.machine.setFillableStack(null);
    }

    private static void prepareGui(Fixture fixture) {
        fixture.logic.setAutoPullItems(false);
        fixture.logic.setAutoPullFluids(false);
        fixture.logic.setItemFilter(1, null);
        fixture.logic.setFluidFilter(1, null);
        fixture.logic.setItemFilter(899, new ItemStack(Items.diamond));
        fixture.logic.setFluidFilter(899, new FluidStack(FluidRegistry.LAVA, 1));
    }

    private static void emptyInputs(MTEBasicMachine machine) {
        for (int i = 0; i < machine.mInputSlotCount; i++) {
            ItemStack stack = machine.getStackInSlot(machine.getInputSlot() + i);
            require(stack == null || stack.stackSize == 0, "ME inputs never enter physical GT inventory slots");
        }
        require(machine.getFillableStack() == null || machine.getFillableStack().amount == 0,
            "ME fluid never enters the physical GT input tank");
    }

    private static void clearPendingRecipe(MTEBasicMachine machine) {
        Arrays.fill(machine.mOutputItems, null);
        machine.mOutputFluid = null;
        machine.mMaxProgresstime = 0;
        machine.mEUt = 0;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            if (stage == 2 && StockIOGuiClientChecks.step(outputDirectory())) stage = 3;
            if (stage == 4) {
                blockFrames++;
                if (blockFrames == 40) {
                    StockIOGuiClientChecks.captureBlock(outputDirectory());
                    StockIOGuiClientChecks.openModelProbe();
                }
                if (blockFrames == 60) {
                    StockIOGuiClientChecks.captureModelProbe(outputDirectory());
                    require(neiConversionsChecked, "native client NEI conversion queries ran before final QA success");
                    Files.write(new File(outputDirectory(), "result.txt").toPath(),
                        "PASS".getBytes(StandardCharsets.UTF_8));
                    finished = true;
                    System.out.println("STOCK_IO_QA: PASS");
                    Minecraft.getMinecraft().shutdown();
                }
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static File outputDirectory() {
        return new File(System.getProperty("gtng.stockio.qa.output", "."));
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        finished = true;
        try {
            Files.write(new File(outputDirectory(), "result.txt").toPath(), "FAIL".getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        Minecraft.getMinecraft().shutdown();
    }

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.WATER, amount);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("STOCK_IO_QA: " + message);
    }

    private static final class Fixture {

        private final StockIOLogic logic;
        private final AENetworkProxy proxy;
        private final MachineSource source;
        private final BaseMetaTileEntity base;
        private final MTEBasicMachine machine;

        private Fixture(StockIOLogic logic, AENetworkProxy proxy, MachineSource source, BaseMetaTileEntity base) {
            this.logic = logic;
            this.proxy = proxy;
            this.source = source;
            this.base = base;
            machine = (MTEBasicMachine) base.getMetaTileEntity();
        }

        private long items(Item item) throws Exception {
            var request = AEItemStack.create(new ItemStack(item)).setStackSize(Long.MAX_VALUE);
            var stack = proxy.getStorage().getItemInventory().extractItems(request, Actionable.SIMULATE, source);
            return stack == null ? 0 : stack.getStackSize();
        }

        private long fluid() throws Exception {
            return fluid(FluidRegistry.WATER);
        }

        private long fluid(Fluid type) throws Exception {
            var request = AEFluidStack.create(new FluidStack(type, 1)).setStackSize(Long.MAX_VALUE);
            var stack = proxy.getStorage().getFluidInventory().extractItems(request, Actionable.SIMULATE, source);
            return stack == null ? 0 : stack.getStackSize();
        }

        private void setItems(Item item, int amount) throws Exception {
            var inventory = proxy.getStorage().getItemInventory();
            inventory.extractItems(AEItemStack.create(new ItemStack(item)).setStackSize(Long.MAX_VALUE),
                Actionable.MODULATE, source);
            if (amount > 0) require(inventory.injectItems(AEItemStack.create(new ItemStack(item, amount)),
                Actionable.MODULATE, source) == null, "QA item seed accepted");
        }

        private void setFluid(int amount) throws Exception {
            setFluid(FluidRegistry.WATER, amount);
        }

        private void setFluid(Fluid type, int amount) throws Exception {
            var inventory = proxy.getStorage().getFluidInventory();
            inventory.extractItems(AEFluidStack.create(new FluidStack(type, 1)).setStackSize(Long.MAX_VALUE),
                Actionable.MODULATE, source);
            if (amount > 0) require(inventory.injectItems(AEFluidStack.create(new FluidStack(type, amount)), Actionable.MODULATE,
                source) == null, "QA fluid seed accepted");
        }
    }
}
