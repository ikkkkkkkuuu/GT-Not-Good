package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.quickterminal.DualTerminalGuiObject;
import com.xyp.gtnotgood.ae2thing.quickterminal.InterfacePatternTarget;
import com.xyp.gtnotgood.common.gui.modularui.widget.GhostMoldSlotWidget;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternGenerator;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardModelState;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.SimpleIOComponent;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputSlave;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitMEPatternBuffer;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitMEPatternMirror;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitPatternCodec;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.machine.MoldDataManager;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPartHost;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.implementations.ContainerOptimizePatterns;
import appeng.container.sync.SyncEndpoint;
import appeng.container.sync.SyncMode;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.me.GridAccessException;
import appeng.me.cache.CraftingGridCache;
import appeng.tile.networking.TileWireless;
import appeng.tile.storage.TileDrive;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.tileentity.IVoidable;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.common.gui.modularui.util.PatternSlot;
import gregtech.common.modularui2.widget.GhostCircuitSlotWidget;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "circuitbufferqa",
    name = "Circuit buffer QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class CircuitPatternBufferChecks {

    private boolean started, finished;
    private volatile boolean opened;
    private volatile boolean moldPlansVerified;
    private int ticks, frames;
    private CircuitMEPatternBuffer hatch;
    private CircuitMEPatternMirror circuitMirror;
    private ItemStack firstPattern, secondPattern;
    private final List<Future<ICraftingJob>> moldPlans = new ArrayList<>();
    private final List<ICraftingPatternDetails> wildcardDetails = new ArrayList<>();
    private final List<Future<ICraftingJob>> wildcardPlans = new ArrayList<>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("circuit-buffer-" + System.currentTimeMillis(), "Circuit buffer QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (++ticks > 1600) throw new AssertionError("GUI timeout");
            if (ticks == 1) setup(player);
            if (ticks == 120) {
                require(hatch.isActive(), "active ME network");
                checks(player);
                hatch.onRightclick(hatch.getBaseMetaTileEntity(), player);
                opened = true;
            }
            if (ticks == 160) startMoldPlans(player);
            if (!moldPlansVerified && !moldPlans.isEmpty()) checkMoldPlans();
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish("FAIL");
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        hatch = (CircuitMEPatternBuffer) place(world, player, 0, GTNGItemList.CircuitMEPatternBuffer.get(1))
            .getMetaTileEntity();
        hatch.setConnectsToAllSides(true);
        var definitions = AEApi.instance().definitions();
        world.setBlock(0, 10, 1, definitions.blocks().multiPart().maybeBlock().get());
        ((IPartHost) world.getTileEntity(0, 10, 1))
            .addPart(definitions.parts().cableGlass().stack(AEColor.Transparent, 1), ForgeDirection.UNKNOWN, player);
        world.setBlock(1, 10, 1, definitions.blocks().energyCellCreative().maybeBlock().get());
        world.setBlock(0, 10, 2, definitions.blocks().wireless().maybeBlock().get());
        TileWireless accessPoint = (TileWireless) world.getTileEntity(0, 10, 2);
        accessPoint.setOrientation(ForgeDirection.SOUTH, ForgeDirection.UP);
        accessPoint.onReady();
        world.setBlock(-1, 10, 1, definitions.blocks().drive().maybeBlock().get());
        firstPattern = pattern(1);
        secondPattern = pattern(2);
        hatch.setInventorySlotContents(0, firstPattern);
        hatch.setInventorySlotContents(1, secondPattern);
        player.playerNetServerHandler.setPlayerLocation(0, 10, -2, 0, 0);
    }

    private void checks(EntityPlayerMP player) throws Exception {
        World world = player.worldObj;
        checkTerminalDiscovery(player);
        require(hatch.getPatternCount() == 900 && hatch.getMoldSlot() == 910, "900 patterns and auxiliary boundaries");
        List<SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME>> slots = new ArrayList<>();
        hatch.inventories().forEachRemaining(slots::add);
        var first = slots.get(0);
        var second = slots.get(1);
        var details = first.getPatternDetails();
        require(details.getCondensedAEInputs().length == 2 && !CircuitPatternCodec.isCircuit(details.getAEInputs()[0]),
            "AE requests material and native fluid without circuit");
        var cache = (CraftingGridCache) hatch.getProxy().getCrafting();
        require(cache.getMediums(details).contains(hatch), "runtime pattern registered in native AE cache");
        require(!details.equals(second.getPatternDetails()),
            "identical materials with different circuits stay distinct");
        var redecoded = CircuitPatternCodec.decode(details.getPattern().copy(), world);
        require(details.equals(redecoded) && details.hashCode() == redecoded.hashCode(),
            "CPU native pattern identity reload");
        require(hatch.pushPattern(details, materials(3)), "CPU pushes circuit-free native item and fluid batch");
        require(hatch.pushPattern(second.getPatternDetails(), materials(3)), "second circuit accepts its own batch");
        require(first.getItemInputs()[1].getItemDamage() == 1 && second.getItemInputs()[1].getItemDamage() == 2,
            "per-pattern live virtual circuits");
        require(first.getPatternInputs().inputItems[1].getItemDamage() == 1, "GT recipe signature contains circuit");

        for (int config = 1; config <= 3; config++) {
            GTRecipeBuilder.builder().itemInputs(new ItemStack(Items.nether_star)).circuit(config)
                .fluidInputs(new FluidStack(FluidRegistry.WATER, 1))
                .itemOutputs(new ItemStack(config == 1 ? Items.apple : Items.carrot)).duration(20).eut(8)
                .addTo(RecipeMaps.mixerRecipes);
        }
        for (int i = 0; i < 2; i++) {
            var slot = slots.get(i);
            var logic = new ProcessingLogic().setRecipeMapSupplier(() -> RecipeMaps.mixerRecipes)
                .setMachine(
                    (IVoidable) GregTechAPI.METATILEENTITIES[GTNGItemList.AssemblerMatrix.get(1).getItemDamage()])
                .setVoidProtection(false, false).setAvailableVoltage(32).setAvailableAmperage(1).setMaxParallel(1)
                .setInputItems(slot.getItemInputs()).setInputFluids(slot.getFluidInputs());
            require(logic.process().wasSuccessful(), "GT processing succeeds with circuit " + (i + 1));
            require(logic.getOutputItems()[0].getItem() == (i == 0 ? Items.apple : Items.carrot),
                "GT selects correct circuit output");
        }

        require(
            ItemStack.areItemStacksEqual(firstPattern, hatch.getStackInSlot(0))
                && ItemStack.areItemStacksEqual(secondPattern, hatch.getStackInSlot(1)),
            "processing leaves encoded circuits and physical patterns unchanged");
        NBTTagCompound saved = new NBTTagCompound();
        ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity()).writeToNBT(saved);
        BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
        restoredTile.setWorldObj(world);
        restoredTile.readFromNBT(saved);
        CircuitMEPatternBuffer restoredHatch = (CircuitMEPatternBuffer) restoredTile.getMetaTileEntity();
        var restoredSlot = restoredHatch.inventories().next();
        require(
            restoredSlot.getItemInputs()[0].stackSize == 2 && restoredSlot.getFluidInputs()[0].amount == 2
                && restoredSlot.getItemInputs()[1].getItemDamage() == 1,
            "tile NBT reload preserves input quantities and circuit");
        checkMirrors(player);

        ItemStack large = pattern(3);
        hatch.setInventorySlotContents(2, large);
        var largeSlot = slotAt(hatch, 2);
        long aboveInt = (long) Integer.MAX_VALUE + 123;
        require(largeSlot.insertItemsAndFluids(materials(aboveInt)), "buffers accept a native AE batch above 2^31-1");
        ItemStack itemView = largeSlot.getItemInputs()[0];
        FluidStack fluidView = largeSlot.getFluidInputs()[0];
        require(itemView.stackSize == Integer.MAX_VALUE && fluidView.amount == Integer.MAX_VALUE,
            "GT recipe views are int-sized while the reserve remains long-sized");
        var longLogic = new ProcessingLogic().setRecipeMapSupplier(() -> RecipeMaps.mixerRecipes)
            .setMachine((IVoidable) GregTechAPI.METATILEENTITIES[GTNGItemList.AssemblerMatrix.get(1).getItemDamage()])
            .setVoidProtection(false, false).setAvailableVoltage(32).setAvailableAmperage(1).setMaxParallel(1)
            .setInputItems(largeSlot.getItemInputs()).setInputFluids(largeSlot.getFluidInputs());
        require(longLogic.process().wasSuccessful(), "native GT processing consumes from the long-backed recipe view");
        require(
            largeSlot.getStoredItems().get(0).getStackSize() == aboveInt - 1
                && largeSlot.getStoredFluids().get(0).getStackSize() == aboveInt - 1
                && largeSlot.getItemInputs()[0] == itemView
                && largeSlot.getFluidInputs()[0] == fluidView
                && itemView.stackSize == Integer.MAX_VALUE
                && fluidView.amount == Integer.MAX_VALUE,
            "exact recipe debit refills the same live item and fluid views without changing the reserve identity");
        require(largeSlot.insertItemsAndFluids(materials(Long.MAX_VALUE - (aboveInt - 1))),
            "each item and fluid type can fill to 2^63-1 without allocating int chunks");
        require(!largeSlot.insertItemsAndFluids(materials(1)), "full buffers reject complete batch");
        require(
            largeSlot.getStoredItems().get(0).getStackSize() == Long.MAX_VALUE
                && largeSlot.getStoredFluids().get(0).getStackSize() == Long.MAX_VALUE,
            "rejection leaves both resource quantities intact");
        require(
            !largeSlot.insertItemsAndFluids(table(AEItemStack.create(new ItemStack(Items.diamond)),
                AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)))) && largeSlot.getItemInputs().length == 2,
            "late fluid overflow cannot partially insert item");

        hatch.setInventorySlotContents(3, pattern(4));
        var types = slotAt(hatch, 3);
        IAEStack<?>[] different = new IAEStack<?>[64];
        for (int i = 0; i < different.length; i++) {
            ItemStack item = new ItemStack(Items.paper);
            item.setTagCompound(new NBTTagCompound());
            item.getTagCompound().setInteger("qaType", i);
            different[i] = AEItemStack.create(item);
        }
        require(types.insertItemsAndFluids(table(different)), "64 NBT-distinct item types");
        require(!types.insertItemsAndFluids(table(AEItemStack.create(new ItemStack(Items.diamond)))),
            "65th item type rejected");
        hatch.setInventorySlotContents(4, pattern(5));
        var fluidTypes = slotAt(hatch, 4);
        var registered = new ArrayList<>(FluidRegistry.getRegisteredFluids().values());
        registered.remove(FluidRegistry.WATER);
        registered.remove(FluidRegistry.LAVA);
        require(registered.size() >= 64, "pack provides 64 distinct native fluid identities");
        for (int i = 0; i < different.length; i++)
            different[i] = AEFluidStack.create(new FluidStack(registered.get(i), 1));
        require(fluidTypes.insertItemsAndFluids(table(different)) && fluidTypes.getFluidInputs().length == 64,
            "64 distinct native fluid types");
        require(!fluidTypes.insertItemsAndFluids(table(AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1)))),
            "65th fluid type rejected");

        hatch.setInventorySlotContents(899, pattern(24));
        var last = slotAt(hatch, 899);
        require(
            last.getPatternInputs().inputItems[1].getItemDamage() == 24
                && last.getPatternDetails().getCondensedAEInputs().length == 2,
            "last slot automatically reads circuit 24 and preserves native fluid input");
        hatch.setInventorySlotContents(898, pattern(-1));
        hatch.setInventorySlotContents(hatch.getCircuitSlot(), GTUtility.getIntegratedCircuit(9));
        var withoutCircuit = slotAt(hatch, 898);
        require(
            !hatch.allowSelectCircuit() && hatch.getSharedItems().length == 0
                && withoutCircuit.getPatternInputs().inputItems.length == 1,
            "legacy shared ghost circuit is disabled and cannot alter recipe signature");
        require(
            hatch.pushPattern(withoutCircuit.getPatternDetails(), materials(1))
                && withoutCircuit.getItemInputs().length == 1,
            "patterns without encoded circuit run without adding one");
        require(hatch.pushPattern(CircuitPatternCodec.decode(firstPattern, world), materials(1)) == false,
            "unregistered raw pattern cannot bypass virtual circuit medium");
        require(!hatch.pushPattern(largeSlot.getPatternDetails(), materials(1)), "CPU full-buffer push rejected");
        checkLongPersistence(player, largeSlot);
        checkManualSlots();
        checkNativeRefund(player, largeSlot);
        checkVirtualMolds(player);
        checkWildcardPatterns(player);
    }

    private void checkMirrors(EntityPlayerMP player) {
        World world = player.worldObj;
        ItemStack mirrorItem = GTNGItemList.CircuitMEPatternMirror.get(1);
        require(GregTechAPI.METATILEENTITIES[mirrorItem.getItemDamage()] instanceof CircuitMEPatternMirror,
            "dedicated circuit mirror has its own registered machine item");
        require(
            StatCollector.canTranslate("NameCircuitMEPatternMirror")
                && !mirrorItem.getDisplayName().contains("NameCircuitMEPatternMirror"),
            "the dedicated mirror item name resolves to translated text");
        var recipe = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
            .filter(candidate -> candidate.mOutputs.length == 1 && mirrorItem.isItemEqual(candidate.mOutputs[0]))
            .findFirst().orElseThrow(() -> new AssertionError("Missing circuit mirror assembler recipe"));
        require(
            recipe.mEUt == 32 && recipe.mDuration == Config.getModifiedRecipeDuration(100)
                && recipe.mInputs.length == 4
                && recipe.mFluidInputs.length == 0
                && recipe.mOutputs[0].stackSize == 1,
            "circuit mirror assembly uses LV power, five seconds, four item inputs and no fluid");
        ItemStack[] ingredients = { GTNGItemList.SuperMTEHatchCraftingInputSlave.get(1),
            GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 2),
            GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2), GTUtility.getIntegratedCircuit(23) };
        ingredients[3].stackSize = 1;
        require(RecipeMaps.assemblerRecipes.findRecipeQuery().items(ingredients).voltage(32).find() == recipe,
            "native assembler lookup selects the dedicated mirror upgrade");
        require(
            recipe.isRecipeInputEqual(true, false, new FluidStack[0], ingredients) && ingredients[0].stackSize == 0
                && ingredients[1].stackSize == 0
                && ingredients[2].stackSize == 0
                && ingredients[3].stackSize == 1,
            "mirror assembly consumes the old mirror, two circuits and two plates while retaining circuit 23");

        var legacyMirror = (SuperMTEHatchCraftingInputSlave) place(world, player, 3,
            GTNGItemList.SuperMTEHatchCraftingInputSlave.get(1)).getMetaTileEntity();
        BaseMetaTileEntity mirrorTile = place(world, player, 4, mirrorItem);
        require(mirrorTile.getMetaTileEntity() instanceof CircuitMEPatternMirror,
            "placing the dedicated mirror preserves its concrete machine class");
        circuitMirror = (CircuitMEPatternMirror) mirrorTile.getMetaTileEntity();
        require(
            StatCollector.canTranslate("tooltip.gtng.circuit_mirror.proxy") && circuitMirror.getDescription()[0]
                .equals(StatCollector.translateToLocal("tooltip.gtng.circuit_mirror.proxy")),
            "the dedicated mirror tooltip resolves to translated text");
        var ordinary = (SuperMTEHatchCraftingInputME) place(world, player, 6,
            GTNGItemList.SuperMTEHatchCraftingInputME.get(1)).getMetaTileEntity();
        ItemStack held = player.getHeldItem();
        ItemStack dataStick = ItemList.Tool_DataStick.get(1);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, dataStick);
        try {
            hatch.onLeftclick(hatch.getBaseMetaTileEntity(), player);
            require(
                "CraftingInputBuffer".equals(dataStick.getTagCompound().getString("type"))
                    && dataStick.getTagCompound().getInteger("x") == 0,
                "left-clicking the circuit master writes native data-stick link coordinates");
            require(circuitMirror.onRightclick(mirrorTile, player) && circuitMirror.getMaster() == hatch,
                "right-clicking the dedicated mirror with its data stick binds the circuit master");
            require(
                legacyMirror.onRightclick(legacyMirror.getBaseMetaTileEntity(), player)
                    && legacyMirror.getMaster() == hatch,
                "the existing super mirror still binds circuit masters through the same data stick");
            ordinary.onLeftclick(ordinary.getBaseMetaTileEntity(), player);
            require(
                circuitMirror.onRightclick(mirrorTile, player) && circuitMirror.getMaster() == hatch
                    && circuitMirror.trySetMasterFromCoord(6, 10, 0) == null
                    && circuitMirror.getMaster() == hatch,
                "a noncircuit master is rejected without replacing the dedicated mirror's valid binding");
        } finally {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, held);
        }
        var first = slotAt(hatch, 0);
        require(
            circuitMirror.inventories().next() == first && legacyMirror.inventories().next() == first
                && first.getItemInputs()[1].getItemDamage() == 1,
            "both mirrors expose the same CircuitSlot with its own virtual circuit");
        require(hatch.getProxyHatches().contains(circuitMirror) && hatch.getProxyHatches().contains(legacyMirror),
            "both mirrors subscribe to their master's ingredient and output notifications");
        for (var mirror : new SuperMTEHatchCraftingInputSlave[] { circuitMirror, legacyMirror }) {
            var shared = mirror.inventories().next();
            var logic = new ProcessingLogic().setRecipeMapSupplier(() -> RecipeMaps.mixerRecipes)
                .setMachine(
                    (IVoidable) GregTechAPI.METATILEENTITIES[GTNGItemList.AssemblerMatrix.get(1).getItemDamage()])
                .setVoidProtection(false, false).setAvailableVoltage(32).setAvailableAmperage(1).setMaxParallel(1)
                .setInputItems(shared.getItemInputs()).setInputFluids(shared.getFluidInputs());
            require(logic.process().wasSuccessful(), "a mirror consumes one recipe from the shared live buffer");
        }
        require(first.isEmpty() && first.getItemInputs().length == 0 && first.getFluidInputs().length == 0,
            "two mirrors consume one shared two-recipe reserve without duplicating inputs or virtual circuits");
        require(hatch.pushPattern(first.getPatternDetails(), materials(2)),
            "the shared circuit buffer accepts a refill");

        NBTTagCompound saved = new NBTTagCompound();
        mirrorTile.writeToNBT(saved);
        BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
        restoredTile.setWorldObj(world);
        restoredTile.readFromNBT(saved);
        require(restoredTile.getMetaTileEntity() instanceof CircuitMEPatternMirror,
            "mirror tile NBT reload preserves the dedicated machine type");
        var restored = (CircuitMEPatternMirror) restoredTile.getMetaTileEntity();
        require(restored.getMaster() == null, "a reloaded mirror resolves its saved coordinates on the server tick");
        restored.onPostTick(restoredTile, 100);
        require(restored.getMaster() == hatch && restored.inventories().next() == first,
            "a reloaded dedicated mirror reconnects to the original CircuitSlot without copying its reserve");
        hatch.removeProxyHatch(restored);
        checkMirrorOutputs(player);
    }

    private void checkMirrorOutputs(EntityPlayerMP player) {
        var controller = (MTEMultiBlockBase) place(player.worldObj, player, 7, GTNGItemList.LargeOreProcessor.get(1))
            .getMetaTileEntity();
        require(controller.addToMachineList(circuitMirror.getBaseMetaTileEntity(), 0),
            "a real multiblock accepts the dedicated circuit mirror through its hatch list");
        var output = hatch.getMEOutput();
        require(
            controller.getOutputBusses().size() == 1 && controller.getOutputBusses().get(0) == output.itemOutput
                && controller.getOutputHatches().size() == 1
                && controller.getOutputHatches().get(0) == output.fluidOutput,
            "controller output getters expose the master's exact item and fluid providers through the mirror");
        ItemEjectionHelper items = new ItemEjectionHelper(controller.getOutputBusses(), true);
        FluidEjectionHelper fluids = new FluidEjectionHelper(controller.getOutputHatches(), true);
        require(
            items.ejectStack(new ItemStack(Items.clay_ball, 7)) == 7
                && fluids.ejectStack(new FluidStack(FluidRegistry.LAVA, 250)) == 250,
            "native GT item and fluid ejection accepts products through the dedicated mirror");
        items.commit();
        fluids.commit();
        NBTTagCompound saved = new NBTTagCompound();
        output.save(saved);
        require(
            saved.getTagList("patternMEItemOutput", 10).getCompoundTagAt(0).getLong("amount") == 7
                && saved.getTagList("patternMEFluidOutput", 10).getCompoundTagAt(0).getLong("amount") == 250,
            "mirror products enter both independent output caches on the master");
        var input = slotAt(hatch, 0);
        require(input.getStoredItems().get(0).getStackSize() == 2 && input.getStoredFluids().get(0).getStackSize() == 2,
            "product ejection leaves the shared pattern ingredient reserves unchanged");
    }

    private void checkVirtualMolds(EntityPlayerMP player) throws GridAccessException {
        ItemStack blockMold = ItemList.Shape_Mold_Block.get(1);
        ItemStack plateMold = ItemList.Shape_Mold_Plate.get(1);
        ItemStack ingotMold = ItemList.Shape_Mold_Ingot.get(1);
        ItemStack consumedMold = ItemList.Shape_Extruder_Rod.get(3);
        require(
            SuperMTEHatchCraftingInputME.findMatchingMold(blockMold) != null
                && SuperMTEHatchCraftingInputME.findMatchingMold(plateMold) != null
                && SuperMTEHatchCraftingInputME.findMatchingMold(ingotMold) != null,
            "regression molds use the existing supported virtual-mold registry");
        for (ItemStack template : new ItemStack[] { plateMold, ingotMold }) {
            ItemStack mold = template.copy();
            mold.stackSize = 0;
            require(
                !GTRecipeBuilder.builder().itemInputs(mold).fluidInputs(new FluidStack(FluidRegistry.WATER, 144))
                    .itemOutputs(new ItemStack(Items.sugar)).duration(20).eut(8)
                    .addTo(RecipeMaps.fluidSolidifierRecipes).isEmpty(),
                "nonconsumed mold fixture is registered before the Ultimate encoded pattern");
        }
        ItemStack circuitMoldTemplate = plateMold.copy();
        circuitMoldTemplate.stackSize = 0;
        require(
            !GTRecipeBuilder.builder().itemInputs(circuitMoldTemplate).circuit(21)
                .fluidInputs(new FluidStack(FluidRegistry.WATER, 144)).itemOutputs(new ItemStack(Items.blaze_rod))
                .duration(20).eut(8).addTo(RecipeMaps.mixerRecipes).isEmpty(),
            "circuit-plus-nonconsumed-mold fixture is registered before encoding");
        require(
            !GTRecipeBuilder.builder().itemInputs(consumedMold.copy())
                .fluidInputs(new FluidStack(FluidRegistry.WATER, 144)).itemOutputs(new ItemStack(Items.quartz))
                .duration(20).eut(8).addTo(RecipeMaps.fluidSolidifierRecipes).isEmpty(),
            "consumed mold fixture uses a catalog item with a positive recipe quantity");
        ItemStack[] patterns = { moldPattern(plateMold, -1, new ItemStack(Items.sugar)),
            moldPattern(ingotMold, -1, new ItemStack(Items.sugar)),
            moldPattern(consumedMold, -1, new ItemStack(Items.quartz)),
            moldPattern(plateMold, 21, new ItemStack(Items.blaze_rod)),
            moldPattern(blockMold, -1, new ItemStack(Blocks.snow), 1000) };
        ItemStack[] templates = { plateMold, ingotMold, consumedMold, plateMold, blockMold };
        for (int i = 0; i < patterns.length; i++) hatch.setInventorySlotContents(10 + i, patterns[i]);
        var plate = slotAt(hatch, 10);
        var sameOutputIngot = slotAt(hatch, 11);
        var circuitMold = slotAt(hatch, 13);
        require(!plate.getPatternDetails().equals(sameOutputIngot.getPatternDetails()),
            "equal native-fluid materials and output with different molds retain separate AE runtime identities");
        for (int i = 0; i < patterns.length; i++) {
            if (i == 2) continue;
            var slot = slotAt(hatch, 10 + i);
            var details = slot.getPatternDetails();
            var raw = CircuitPatternCodec.decode(patterns[i], player.worldObj);
            require(
                raw.getAEInputs()[1] instanceof IAEFluidStack && details.getCondensedAEInputs().length == 1
                    && details.getCondensedAEInputs()[0] instanceof IAEFluidStack,
                "Ultimate encoded mold pattern " + i + " retains native water while AE omits mold and circuit");
            var redecoded = CircuitPatternCodec.decode(details.getPattern().copy(), player.worldObj);
            require(details.equals(redecoded) && details.hashCode() == redecoded.hashCode(),
                "native runtime pattern NBT preserves mold identity " + i);
            require(slot.isEmpty() && slot.getItemInputs().length == 0 && slot.getFluidInputs().length == 0,
                "virtual mold and circuit cannot make an empty buffer runnable");
            ItemStack[] signature = slot.getPatternInputs().inputItems;
            require(
                hasVirtualMold(signature, templates[i]) && signature.length == (i == 3 ? 2 : 1)
                    && slot.getPatternInputs().inputFluid[0].amount == (i == 4 ? 1000 : 144),
                "GT recipe signature restores each pattern's own mold and optional circuit");
        }
        require(countItem(circuitMold.getPatternInputs().inputItems, GTUtility.getIntegratedCircuit(21)) == 1,
            "circuit-plus-mold GT signature preserves programmed circuit 21");
        require(
            circuitMold.getPatternDetails()
                .equals(CircuitPatternCodec.runtime(moldPattern(plateMold, 21, new ItemStack(Items.blaze_rod), 144, 0),
                    player.worldObj)),
            "zero-size encoded GT circuit resolves the same reusable mold as a GUI-sized circuit");
        for (int index : new int[] { 10, 11 }) {
            var slot = slotAt(hatch, index);
            ItemStack mold = templates[index - 10];
            require(!solidifierLogic(slot, 1).process().wasSuccessful(),
                "an empty mold-only buffer cannot execute the native fluid-solidifier recipe");
            require(
                hatch.pushPattern(slot.getPatternDetails(),
                    table(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 432)))),
                "native CPU batch supplies only fluid, with no physical mold");
            var logic = solidifierLogic(slot, 2);
            require(
                logic.process().wasSuccessful() && logic.getOutputItems()[0].getItem() == Items.sugar
                    && logic.getOutputItems()[0].stackSize == 2,
                "native GT fluid-solidifier executes a two-recipe batch using the correct virtual mold");
            require(
                slot.getStoredItems().isEmpty() && slot.getStoredFluids().get(0).getStackSize() == 144
                    && hasVirtualMold(slot.getItemInputs(), mold),
                "batch processing debits only water and retains the nonconsumed virtual mold");
            ItemStack exposed = slot.getItemInputs()[0];
            exposed.stackSize = 64;
            require(hasVirtualMold(slot.getItemInputs(), mold),
                "each GT item view receives a fresh zero-size mold copy");
            require(
                solidifierLogic(slot, 1).process().wasSuccessful() && slot.isEmpty()
                    && slot.getItemInputs().length == 0,
                "consuming the final fluid batch hides virtual items from the empty live buffer");
            require(
                hatch.pushPattern(slot.getPatternDetails(),
                    table(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 144)))),
                "the same pattern can receive another fluid batch without requesting its mold");
        }
        require(
            hatch.pushPattern(circuitMold.getPatternDetails(),
                table(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 432))))
                && circuitMold.getStoredItems().isEmpty()
                && circuitMold.getItemInputs().length == 2
                && hasVirtualMold(circuitMold.getItemInputs(), plateMold)
                && countItem(circuitMold.getItemInputs(), GTUtility.getIntegratedCircuit(21)) == 1,
            "fluid-only CPU batch restores both the per-pattern circuit and mold for GT");
        var mirroredMold = find(circuitMirror, patterns[3]);
        require(
            mirroredMold == circuitMold && hasVirtualMold(mirroredMold.getItemInputs(), plateMold)
                && countItem(mirroredMold.getItemInputs(), GTUtility.getIntegratedCircuit(21)) == 1
                && mirroredMold.getStoredItems().isEmpty(),
            "the dedicated mirror shares the circuit-plus-mold slot without stocking physical virtual tools");
        var block = slotAt(hatch, 14);
        require(
            hatch.pushPattern(block.getPatternDetails(),
                table(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1000))))
                && hasVirtualMold(block.getItemInputs(), blockMold),
            "native block-mold snow pattern receives water without any physical block mold");
        var consumed = slotAt(hatch, 12);
        require(
            consumed.getPatternDetails().getCondensedAEInputs().length == 2
                && consumed.getPatternDetails().equals(CircuitPatternCodec.decode(patterns[2], player.worldObj))
                && consumed.getPatternInputs().inputItems[0].stackSize == 3,
            "a catalog mold consumed by the actual recipe remains a physical AE requirement");
        require(
            hatch.pushPattern(consumed.getPatternDetails(),
                table(AEItemStack.create(consumedMold.copy()).setStackSize(6),
                    AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 288)))),
            "CPU may push the physically consumed mold with its native fluid");
        var consumedLogic = solidifierLogic(consumed, 1);
        require(
            consumedLogic.process().wasSuccessful() && consumedLogic.getOutputItems()[0].getItem() == Items.quartz
                && consumed.getStoredItems().get(0).getStackSize() == 3
                && consumed.getStoredFluids().get(0).getStackSize() == 144,
            "native GT consumes the positive-quantity mold and water exactly once");
        NBTTagCompound saved = new NBTTagCompound();
        ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity()).writeToNBT(saved);
        BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
        restoredTile.setWorldObj(player.worldObj);
        restoredTile.readFromNBT(saved);
        var restored = (CircuitMEPatternBuffer) restoredTile.getMetaTileEntity();
        for (int i = 0; i < patterns.length; i++) {
            var original = slotAt(hatch, 10 + i);
            var reloaded = slotAt(restored, 10 + i);
            require(
                ItemStack.areItemStacksEqual(patterns[i], hatch.getStackInSlot(10 + i))
                    && original.getPatternDetails().equals(reloaded.getPatternDetails())
                    && original.getPatternDetails().hashCode() == reloaded.getPatternDetails().hashCode()
                    && (i == 2 ? reloaded.getPatternInputs().inputItems[0].stackSize == 3
                        : hasVirtualMold(reloaded.getPatternInputs().inputItems, templates[i])),
                "tile NBT reload retains original encoded mold and distinct runtime identity " + i);
        }
        var reloaded = slotAt(restored, 13);
        require(
            reloaded.getStoredItems().isEmpty() && reloaded.getStoredFluids().get(0).getStackSize() == 432
                && hasVirtualMold(reloaded.getItemInputs(), plateMold)
                && countItem(reloaded.getItemInputs(), GTUtility.getIntegratedCircuit(21)) == 1,
            "tile NBT reload retains fluid quantity and independently reconstructs mold and circuit");
    }

    private void checkWildcardPatterns(EntityPlayerMP player) throws Exception {
        ItemStack mold = ItemList.Shape_Mold_Plate.get(1);
        for (Materials material : new Materials[] { Materials.Iron, Materials.Gold }) {
            ItemStack tool = mold.copy();
            tool.stackSize = 0;
            require(
                !GTRecipeBuilder.builder().itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, material, 1), tool)
                    .circuit(22).fluidInputs(new FluidStack(FluidRegistry.WATER, 37))
                    .itemOutputs(GTOreDictUnificator.get(OrePrefixes.ingot, material, 1)).duration(20).eut(8)
                    .addTo(RecipeMaps.mixerRecipes).isEmpty(),
                "wildcard material has a matching GT recipe with a nonconsumed mold: " + material.mName);
        }
        ItemStack wildcard = GTNGItemList.WildcardPattern.get(1);
        WildcardPatternGenerator.markAsWildcard(wildcard);
        WildcardModelState.setInputs(wildcard, Arrays.asList(new PrefixIOComponent(OrePrefixes.dust, 1),
            new SimpleIOComponent(mold, 1), new SimpleIOComponent(GTUtility.getIntegratedCircuit(22), 1),
            new SimpleIOComponent(GTUtility.getFluidDisplayStack(new FluidStack(FluidRegistry.WATER, 37), true), 37)));
        WildcardModelState.setOutputs(wildcard, Collections.singletonList(new PrefixIOComponent(OrePrefixes.ingot, 1)));
        WildcardModelState.setFilters(wildcard,
            Collections.singletonList(new StringFilterComponent("^(Iron|Gold)$", true)));
        require(WildcardPatternGenerator.generateAllDetails(wildcard, player.worldObj).size() == 2,
            "real wildcard model expands exactly the iron and gold material branches");
        hatch.setInventorySlotContents(20, wildcard);
        ItemStack encoded = hatch.getStackInSlot(20).copy();
        hatch.provideCrafting(
            (ICraftingProviderHelper) Proxy.newProxyInstance(ICraftingProviderHelper.class.getClassLoader(),
                new Class<?>[] { ICraftingProviderHelper.class }, (proxy, method, args) -> {
                    if (method.getName().equals("addCraftingOption")) {
                        ICraftingPatternDetails details = (ICraftingPatternDetails) args[1];
                        for (Materials material : new Materials[] { Materials.Iron, Materials.Gold }) {
                            if (
                                GTUtility.areStacksEqual(details.getCondensedOutputs()[0].getItemStack(),
                                    GTOreDictUnificator.get(OrePrefixes.ingot, material, 1), true)
                            ) wildcardDetails.add(details);
                        }
                    }
                    return null;
                }));
        require(wildcardDetails.size() == 2, "one physical circuit-buffer wildcard offers both AE branches");
        for (ICraftingPatternDetails details : wildcardDetails) {
            require(
                details.getPattern().getItem()
                    == AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get().getItem()
                    && details.getCondensedAEInputs().length == 2,
                "expanded runtime branch is an Ultimate encoded pattern with only dust and native water");
            boolean nativeFluid = false;
            for (IAEStack<?> input : details.getCondensedAEInputs()) {
                require(
                    !CircuitPatternCodec.isCircuit(input)
                        && !CircuitPatternCodec.isMold(input, new ItemStack[] { mold }),
                    "AE wildcard branch does not request its virtual circuit or reusable mold");
                nativeFluid |= input instanceof IAEFluidStack fluid && fluid.getFluid() == FluidRegistry.WATER
                    && fluid.getStackSize() == 37;
            }
            require(nativeFluid, "wildcard fluid becomes a native AE fluid rather than a fluid-drop item");
            ICraftingPatternDetails redecoded = CircuitPatternCodec.decode(details.getPattern().copy(),
                player.worldObj);
            require(details.equals(redecoded) && details.hashCode() == redecoded.hashCode(),
                "CPU native pattern redecoding preserves each expanded wildcard branch identity");
        }
        var first = wildcardDetails.get(0);
        var second = wildcardDetails.get(1);
        require(!first.equals(second), "distinct wildcard material branches keep distinct runtime identities");
        checkWildcardOptimization(encoded, first);
        var slot = slotAt(hatch, 20);
        require(slot.isEmpty() && slot.getItemInputs().length == 0,
            "wildcard preview cannot supply virtual tools before a real CPU batch arrives");
        require(hatch.pushPattern(CircuitPatternCodec.decode(first.getPattern().copy(), player.worldObj),
            wildcardBatch(first, 2)), "CPU submits a redecoded native wildcard branch");
        require(
            slot.getPatternDetails().equals(first) && hasVirtualMold(slot.getItemInputs(), mold)
                && countItem(slot.getItemInputs(), GTUtility.getIntegratedCircuit(22)) == 1
                && slot.getStoredItems().size() == 1,
            "active wildcard GT inputs contain the selected dust plus virtual mold and circuit 22");
        var mirrored = find(circuitMirror, hatch.getStackInSlot(20));
        require(mirrored == slot && mirrored.getPatternDetails().equals(first),
            "circuit mirror shares the exact physical wildcard slot and selected branch");
        require(
            !hatch.pushPattern(second, wildcardBatch(second, 1)) && slot.getStoredItems().get(0).getStackSize() == 2
                && slot.getStoredFluids().get(0).getStackSize() == 74,
            "a different wildcard material cannot mix into an occupied long buffer");
        var logic = new ProcessingLogic().setRecipeMapSupplier(() -> RecipeMaps.mixerRecipes)
            .setMachine((IVoidable) GregTechAPI.METATILEENTITIES[GTNGItemList.AssemblerMatrix.get(1).getItemDamage()])
            .setVoidProtection(false, false).setAvailableVoltage(32).setAvailableAmperage(1).setMaxParallel(2)
            .setInputItems(mirrored.getItemInputs()).setInputFluids(mirrored.getFluidInputs());
        require(
            logic.process().wasSuccessful()
                && GTUtility.areStacksEqual(logic.getOutputItems()[0], first.getCondensedOutputs()[0].getItemStack(),
                    true)
                && logic.getOutputItems()[0].stackSize == 2
                && slot.isEmpty(),
            "native GT consumes the shared wildcard material and fluid batch through the mirror");
        require(hatch.pushPattern(second, wildcardBatch(second, 1)) && slot.getPatternDetails().equals(second),
            "an emptied physical wildcard slot switches to its other material branch");
        long aboveInt = (long) Integer.MAX_VALUE + 123;
        require(hatch.pushPattern(second, wildcardBatch(second, aboveInt - 1)),
            "active wildcard branch retains long item and native fluid reserves above the GT int view");
        NBTTagCompound saved = new NBTTagCompound();
        ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity()).writeToNBT(saved);
        BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
        restoredTile.setWorldObj(player.worldObj);
        restoredTile.readFromNBT(saved);
        var restored = (CircuitMEPatternBuffer) restoredTile.getMetaTileEntity();
        var reloaded = slotAt(restored, 20);
        require(
            reloaded.getPatternDetails().equals(second) && reloaded.getStoredItems().get(0).getStackSize() == aboveInt
                && reloaded.getStoredFluids().get(0).getStackSize() == aboveInt * 37
                && hasVirtualMold(reloaded.getItemInputs(), mold)
                && countItem(reloaded.getItemInputs(), GTUtility.getIntegratedCircuit(22)) == 1,
            "tile NBT retains active wildcard branch, virtual tools and exact long reserves");
        require(!restored.pushPattern(first, wildcardBatch(first, 1)),
            "reloaded wildcard buffer still rejects a different branch before its reserves are consumed");
        checkLegacyWildcardRecovery(player, saved, mold, second);
        Method rebuild = CircuitMEPatternBuffer.class.getDeclaredMethod("rebuildCraftingSlots");
        rebuild.setAccessible(true);
        rebuild.invoke(restored);
        Field craftingSlots = CircuitMEPatternBuffer.class.getDeclaredField("craftingSlots");
        craftingSlots.setAccessible(true);
        Map<?, ?> reloadedBranches = (Map<?, ?>) craftingSlots.get(restored);
        require(reloadedBranches.containsKey(first) && reloadedBranches.containsKey(second),
            "both original CPU runtime identities remain registered after wildcard tile recovery");
        require(
            reloaded.getPatternDetails().equals(second) && reloaded.getStoredItems().get(0).getStackSize() == aboveInt
                && reloaded.getStoredFluids().get(0).getStackSize() == aboveInt * 37,
            "rebuilding restored AE branch identities leaves the active recipe and exact long reserves unchanged");
        restored.setInventorySlotContents(20, null);
        NBTTagCompound removed = new NBTTagCompound();
        restored.saveNBTData(removed);
        NBTTagList refunds = removed.getTagList("circuitBufferRefunds", 10);
        boolean retained = false;
        for (int i = 0; i < refunds.tagCount(); i++) {
            NBTTagCompound refund = refunds.getCompoundTagAt(i);
            retained |= nativeCount(refund, "longItems") == aboveInt
                && nativeCount(refund, "longFluids") == aboveInt * 37;
        }
        require(retained, "removing an occupied wildcard preserves its selected branch's exact pending refund");
        require(ItemStack.areItemStacksEqual(encoded, hatch.getStackInSlot(20)),
            "branch execution and persistence leave the physical wildcard configuration unchanged");
    }

    private static void checkWildcardOptimization(ItemStack physical, ICraftingPatternDetails details)
        throws Exception {
        Method matcher = null;
        for (Method method : ContainerOptimizePatterns.class.getDeclaredMethods()) {
            if (
                method.getName().contains("sameTemplate") && Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() == boolean.class
                    && Arrays.equals(method.getParameterTypes(), new Class<?>[] { ItemStack.class, ItemStack.class })
            ) {
                matcher = method;
                break;
            }
        }
        require(matcher != null, "native optimization container includes the wildcard template matcher");
        matcher.setAccessible(true);
        ItemStack runtime = details.getPattern().copy();
        require((Boolean) matcher.invoke(null, physical, runtime),
            "optimization matches a native runtime branch to its physical wildcard template");
        ItemStack other = physical.copy();
        WildcardModelState.setFilters(other, Collections.singletonList(new StringFilterComponent("^Iron$", true)));
        require(!(Boolean) matcher.invoke(null, other, runtime),
            "optimization cannot apply a runtime branch to another wildcard template");
        ItemStack counted = physical.copy();
        counted.getTagCompound().setInteger(WildcardModelState.KEY_EXPANDED_COUNT, 999);
        counted.getTagCompound().setInteger("WildcardExpandedPatternCount", 777);
        require((Boolean) matcher.invoke(null, counted, runtime),
            "derived current and legacy preview counts do not change optimization template identity");
        ItemStack unrelated = runtime.copy();
        unrelated.getTagCompound().removeTag("gtngWildcardSource");
        require(!(Boolean) matcher.invoke(null, physical, unrelated),
            "a generated branch ID alone cannot make an unrelated native pattern match the template");
        ItemStack wrongSource = runtime.copy();
        wrongSource.getTagCompound().getCompoundTag("gtngWildcardSource").setString("id", "minecraft:paper");
        require(!(Boolean) matcher.invoke(null, physical, wrongSource),
            "optimization rejects a runtime pattern with a different source item");
        wrongSource = runtime.copy();
        wrongSource.getTagCompound().getCompoundTag("gtngWildcardSource").setInteger("Damage",
            physical.getItemDamage() + 1);
        require(!(Boolean) matcher.invoke(null, physical, wrongSource),
            "optimization rejects a runtime pattern with different source metadata");
    }

    private void checkLegacyWildcardRecovery(EntityPlayerMP player, NBTTagCompound saved, ItemStack mold,
        ICraftingPatternDetails active) {
        NBTTagCompound legacy = (NBTTagCompound) saved.copy();
        legacy.removeTag("circuitBufferRefunds");
        NBTTagList internal = legacy.getTagList("internalInventory", 10);
        NBTTagCompound entry = null;
        for (int i = 0; i < internal.tagCount(); i++) {
            if (internal.getCompoundTagAt(i).getInteger("patternSlot") == 20) entry = internal.getCompoundTagAt(i);
        }
        require(entry != null, "legacy wildcard fixture starts with the serialized active physical slot");
        NBTTagCompound slot = entry.getCompoundTag("patternSlotNBT");
        ItemStack generated = ItemStack.loadItemStackFromNBT(slot.getCompoundTag("WildcardActivePattern"));
        require(
            generated != null && !slot.getString("WildcardActivePatternId").isEmpty()
                && slot.getString("WildcardActivePatternId")
                    .equals(WildcardPatternGenerator.getGeneratedPatternId(generated)),
            "legacy wildcard fixture preserves its original active-pattern stack and generated ID");
        ItemStack dust = null;
        for (IAEStack<?> input : active.getCondensedAEInputs()) {
            if (input instanceof IAEItemStack item) dust = item.getItemStack().copy();
        }
        require(dust != null, "legacy wildcard fixture contains the active branch's actual dust material");
        dust.stackSize = 7;
        ItemStack physicalMold = mold.copy();
        physicalMold.stackSize = 3;
        ItemStack circuit = GTUtility.getIntegratedCircuit(22);
        circuit.stackSize = 2;
        NBTTagList items = new NBTTagList();
        for (ItemStack item : new ItemStack[] { dust, physicalMold, circuit })
            items.appendTag(GTUtility.saveItem(item));
        slot.setTag("inventory", items);
        NBTTagList fluids = new NBTTagList();
        fluids.appendTag(new FluidStack(FluidRegistry.WATER, 259).writeToNBT(new NBTTagCompound()));
        slot.setTag("fluidInventory", fluids);
        slot.removeTag("longItems");
        slot.removeTag("longFluids");
        NBTTagList onlyWildcard = new NBTTagList();
        onlyWildcard.appendTag(entry);
        legacy.setTag("internalInventory", onlyWildcard);
        for (boolean knownBranch : new boolean[] { true, false }) {
            NBTTagCompound fixture = (NBTTagCompound) legacy.copy();
            if (!knownBranch) {
                NBTTagCompound unknown = fixture.getTagList("internalInventory", 10).getCompoundTagAt(0)
                    .getCompoundTag("patternSlotNBT");
                unknown.removeTag("WildcardActivePattern");
                unknown.removeTag("WildcardActivePatternId");
            }
            BaseMetaTileEntity tile = new BaseMetaTileEntity();
            tile.setWorldObj(player.worldObj);
            tile.readFromNBT(fixture);
            var recovered = (CircuitMEPatternBuffer) tile.getMetaTileEntity();
            var live = slotAt(recovered, 20);
            require(live.isEmpty() && live.getItemInputs().length == 0 && live.getFluidInputs().length == 0,
                "legacy wildcard inputs leave the live buffer empty during recovery: " + knownBranch);
            NBTTagCompound rewritten = new NBTTagCompound();
            recovered.saveNBTData(rewritten);
            NBTTagList refunds = rewritten.getTagList("circuitBufferRefunds", 10);
            require(refunds.tagCount() == 1, "legacy wildcard recovery creates exactly one refund buffer");
            NBTTagCompound refund = refunds.getCompoundTagAt(0);
            require(
                refund.getTagList("longItems", 10).tagCount() == 3 && nativeItemCount(refund, dust) == 7
                    && nativeItemCount(refund, physicalMold) == 3
                    && nativeItemCount(refund, circuit) == 2
                    && nativeCount(refund, "longFluids") == 259,
                "legacy material, fluid and physical tools survive recovery without loss or duplication: "
                    + knownBranch);
        }
    }

    private static long nativeItemCount(NBTTagCompound saved, ItemStack item) {
        long count = 0;
        NBTTagList list = saved.getTagList("longItems", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            IAEStack<?> stack = Platform.readStackNBT(list.getCompoundTagAt(i), true);
            if (
                stack instanceof IAEItemStack candidate
                    && GTUtility.areStacksEqual(candidate.getItemStack(), item, true)
            ) count += stack.getStackSize();
        }
        return count;
    }

    private static MEInventoryCrafting wildcardBatch(ICraftingPatternDetails details, long batches) {
        List<IAEStack<?>> resources = new ArrayList<>();
        for (IAEStack<?> input : details.getCondensedAEInputs())
            resources.add(input.copy().setStackSize(Math.multiplyExact(input.getStackSize(), batches)));
        return table(resources.toArray(new IAEStack<?>[0]));
    }

    private static ProcessingLogic solidifierLogic(SuperMTEHatchCraftingInputME.PatternSlot<?> slot, int parallel) {
        return new ProcessingLogic().setRecipeMapSupplier(() -> RecipeMaps.fluidSolidifierRecipes)
            .setMachine((IVoidable) GregTechAPI.METATILEENTITIES[GTNGItemList.AssemblerMatrix.get(1).getItemDamage()])
            .setVoidProtection(false, false).setAvailableVoltage(32).setAvailableAmperage(1).setMaxParallel(parallel)
            .setInputItems(slot.getItemInputs()).setInputFluids(slot.getFluidInputs());
    }

    private void startMoldPlans(EntityPlayerMP player) throws GridAccessException {
        var cache = (CraftingGridCache) hatch.getProxy().getCrafting();
        for (int i = 10; i <= 14; i++) require(cache.getMediums(slotAt(hatch, i).getPatternDetails()).contains(hatch),
            "each independent mold runtime pattern is registered after the native AE cache refresh");
        var source = hatch.getMEOutputActionSource();
        var storage = hatch.getProxy().getStorage();
        for (ItemStack item : new ItemStack[] { ItemList.Shape_Mold_Block.get(1), ItemList.Shape_Mold_Plate.get(1),
            ItemList.Shape_Mold_Ingot.get(1), ItemList.Shape_Extruder_Rod.get(1), GTUtility.getIntegratedCircuit(21) })
            require(
                storage.getItemInventory().extractItems(AEItemStack.create(item), Actionable.SIMULATE, source) == null,
                "real crafting network has no physical " + item.getDisplayName());
        for (ItemStack output : new ItemStack[] { new ItemStack(Items.sugar), new ItemStack(Items.blaze_rod),
            new ItemStack(Blocks.snow), new ItemStack(Items.quartz) })
            moldPlans.add(hatch.getProxy().getCrafting().beginCraftingJob(player.worldObj, hatch.getProxy().getGrid(),
                source, AEItemStack.create(output).setStackSize(4), null));
        TileDrive drive = (TileDrive) player.worldObj.getTileEntity(-1, 10, 1);
        drive.getInternalInventory().setInventorySlotContents(2,
            AEApi.instance().definitions().items().cell64k().maybeStack(1).get());
        for (ICraftingPatternDetails details : wildcardDetails) {
            require(cache.getMediums(details).contains(hatch),
                "each expanded wildcard branch reaches the real AE crafting cache");
            for (IAEStack<?> input : details.getCondensedAEInputs()) {
                if (
                    input instanceof IAEItemStack item
                ) require(
                    storage.getItemInventory().injectItems(item.copy().setStackSize(4), Actionable.MODULATE, source)
                        == null,
                    "real wildcard crafting network receives only its physical dust material");
            }
            wildcardPlans.add(hatch.getProxy().getCrafting().beginCraftingJob(player.worldObj,
                hatch.getProxy().getGrid(), source, details.getCondensedOutputs()[0].copy().setStackSize(4), null));
        }
    }

    private void checkMoldPlans() throws Exception {
        for (Future<ICraftingJob> job : moldPlans) if (!job.isDone()) return;
        for (Future<ICraftingJob> job : wildcardPlans) if (!job.isDone()) return;
        for (int index = 0; index < moldPlans.size(); index++) {
            var result = moldPlans.get(index).get();
            require(result.isSimulation() == (index == 3),
                "real AE plan only reports missing material for the physically consumed mold");
            var plan = AEApi.instance().storage().createAEStackList();
            result.populatePlan(plan);
            boolean nativeWater = false;
            boolean missingConsumedMold = false;
            for (IAEStack<?> entry : plan) {
                if (entry instanceof IAEItemStack item) {
                    if (
                        index == 3
                            && GTUtility.areStacksEqual(item.getItemStack(), ItemList.Shape_Extruder_Rod.get(1), true)
                    ) missingConsumedMold |= item.getStackSize() >= 12;
                    else require(
                        SuperMTEHatchCraftingInputME.findMatchingMold(item.getItemStack()) == null
                            && !CircuitPatternCodec.isCircuit(entry),
                        "real AE crafting plan has no missing or indexed virtual item");
                } else if (entry instanceof IAEFluidStack fluid) nativeWater |= fluid.getFluid() == FluidRegistry.WATER
                    && fluid.getStackSize() >= (index == 2 ? 4000 : 576);
            }
            require(nativeWater, "real AE crafting plan indexes the native fluid needed for all four outputs");
            require(missingConsumedMold == (index == 3),
                "positive-quantity mold stays in the real missing-material plan while nonconsumed molds are absent");
        }
        require(wildcardPlans.size() == 2, "real AE crafting planner covers both wildcard materials");
        for (Future<ICraftingJob> future : wildcardPlans) {
            ICraftingJob job = future.get();
            require(!job.isSimulation(), "real AE wildcard plan succeeds without physical molds or circuits");
            var plan = AEApi.instance().storage().createAEStackList();
            job.populatePlan(plan);
            boolean nativeWater = false;
            for (IAEStack<?> input : plan) {
                if (
                    input instanceof IAEItemStack item
                ) require(
                    SuperMTEHatchCraftingInputME.findMatchingMold(item.getItemStack()) == null
                        && !CircuitPatternCodec.isCircuit(input),
                    "real wildcard crafting plan never indexes a virtual tool as a physical demand");
                else if (input instanceof IAEFluidStack fluid)
                    nativeWater |= fluid.getFluid() == FluidRegistry.WATER && fluid.getStackSize() >= 148;
            }
            require(nativeWater, "real wildcard crafting plan requests the expanded branch's native water");
        }
        moldPlansVerified = true;
    }

    private static int countItem(ItemStack[] inputs, ItemStack item) {
        int count = 0;
        for (ItemStack input : inputs) if (GTUtility.areStacksEqual(input, item, true)) count++;
        return count;
    }

    private static boolean hasVirtualMold(ItemStack[] inputs, ItemStack mold) {
        for (ItemStack input : inputs)
            if (GTUtility.areStacksEqual(input, mold, true) && input.stackSize == 0) return true;
        return false;
    }

    private void checkNativeRefund(EntityPlayerMP player, SuperMTEHatchCraftingInputME.PatternSlot<?> largeSlot)
        throws GridAccessException {
        TileDrive drive = (TileDrive) player.worldObj.getTileEntity(-1, 10, 1);
        require(drive.getActionableNode().getGrid() == hatch.getProxy().getGrid(),
            "empty ME drive shares the active test grid");
        drive.getInternalInventory().setInventorySlotContents(0,
            AEApi.instance().definitions().items().cell64k().maybeStack(1).get());
        drive.getInternalInventory().setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
        hatch.getMEOutput().flush();
        var itemProduct = hatch.getProxy().getStorage().getItemInventory().extractItems(
            AEItemStack.create(new ItemStack(Items.clay_ball, 7)), Actionable.SIMULATE,
            hatch.getMEOutputActionSource());
        var fluidProduct = hatch.getProxy().getStorage().getFluidInventory().extractItems(
            AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 250)), Actionable.SIMULATE,
            hatch.getMEOutputActionSource());
        require(
            itemProduct != null && itemProduct.getStackSize() == 7
                && fluidProduct != null
                && fluidProduct.getStackSize() == 250,
            "mirror item and fluid products export from the master into real ME storage cells");
        largeSlot.refund(hatch.getProxy(), hatch.getMEOutputActionSource(), false);
        var storage = hatch.getProxy().getStorage();
        var source = hatch.getMEOutputActionSource();
        var returnedItems = storage.getItemInventory().extractItems(
            AEItemStack.create(new ItemStack(Items.nether_star)).setStackSize(Long.MAX_VALUE), Actionable.SIMULATE,
            source);
        var returnedFluids = storage.getFluidInventory().extractItems(
            AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)).setStackSize(Long.MAX_VALUE),
            Actionable.SIMULATE, source);
        require(
            returnedItems != null && returnedItems.getStackSize() > 0
                && returnedFluids != null
                && returnedFluids.getStackSize() > 0,
            "native powered refund exports both long-backed channels into real storage cells");
        require(
            largeSlot.getStoredItems().get(0).getStackSize() == Long.MAX_VALUE - returnedItems.getStackSize()
                && largeSlot.getStoredFluids().get(0).getStackSize() == Long.MAX_VALUE - returnedFluids.getStackSize(),
            "partial native ME refunds leave the exact long remainder instead of narrowing it to int");
    }

    private void checkLongPersistence(EntityPlayerMP player, SuperMTEHatchCraftingInputME.PatternSlot<?> largeSlot) {
        NBTTagCompound saved = new NBTTagCompound();
        ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity()).writeToNBT(saved);
        BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
        restoredTile.setWorldObj(player.worldObj);
        restoredTile.readFromNBT(saved);
        var restored = (CircuitMEPatternBuffer) restoredTile.getMetaTileEntity();
        var restoredSlot = slotAt(restored, 2);
        require(
            restoredSlot.getStoredItems().get(0).getStackSize() == Long.MAX_VALUE
                && restoredSlot.getStoredFluids().get(0).getStackSize() == Long.MAX_VALUE,
            "long item and native fluid counts survive complete tile NBT reload");

        NBTTagList internal = saved.getTagList("internalInventory", 10);
        for (int i = 0; i < internal.tagCount(); i++) {
            NBTTagCompound entry = internal.getCompoundTagAt(i);
            if (entry.getInteger("patternSlot") != 2) continue;
            NBTTagCompound slot = entry.getCompoundTag("patternSlotNBT");
            slot.removeTag("longItems");
            slot.removeTag("longFluids");
            NBTTagList items = new NBTTagList();
            items.appendTag(GTUtility.saveItem(new ItemStack(Items.nether_star, 73)));
            slot.setTag("inventory", items);
            NBTTagList fluids = new NBTTagList();
            fluids.appendTag(new FluidStack(FluidRegistry.WATER, 95).writeToNBT(new NBTTagCompound()));
            slot.setTag("fluidInventory", fluids);
        }
        BaseMetaTileEntity legacyTile = new BaseMetaTileEntity();
        legacyTile.setWorldObj(player.worldObj);
        legacyTile.readFromNBT(saved);
        var legacySlot = slotAt((CircuitMEPatternBuffer) legacyTile.getMetaTileEntity(), 2);
        require(
            legacySlot.getStoredItems().get(0).getStackSize() == 73
                && legacySlot.getStoredFluids().get(0).getStackSize() == 95,
            "existing int-buffer saves migrate without changing item or fluid counts");

        hatch.setInventorySlotContents(6, pattern(7));
        var secondLong = slotAt(hatch, 6);
        require(secondLong.insertItemsAndFluids(materials(Long.MAX_VALUE)),
            "another pattern keeps its own long maximum");
        require(largeSlot.getStoredItems().get(0).getStackSize() == Long.MAX_VALUE, "long inventories remain isolated");
        NBTTagCompound waila = new NBTTagCompound();
        hatch.getWailaNBTData(player, (BaseMetaTileEntity) hatch.getBaseMetaTileEntity(), waila, player.worldObj, 0, 10,
            0);
        NBTTagList diagnostics = waila.getTagList("inventory", 10);
        String expected = NumberFormat.getIntegerInstance(Locale.ROOT)
            .format(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(2)).add(BigInteger.valueOf(5)));
        boolean correct = false;
        for (int i = 0; i < diagnostics.tagCount(); i++) {
            NBTTagCompound entry = diagnostics.getCompoundTagAt(i);
            if (entry.getString("name").equals(new ItemStack(Items.nether_star).getDisplayName()))
                correct = expected.equals(entry.getString("amountText"));
        }
        require(correct, "Waila aggregate across independent long buffers does not overflow a signed long");

        hatch.setInventorySlotContents(7, pattern(8));
        var overflow = slotAt(hatch, 7);
        require(
            !overflow.insertItemsAndFluids(
                table(AEItemStack.create(new ItemStack(Items.nether_star)).setStackSize(Long.MAX_VALUE),
                    AEItemStack.create(new ItemStack(Items.nether_star))))
                && overflow.isEmpty(),
            "overflow from repeated types within one CPU batch is rejected atomically");
        hatch.setInventorySlotContents(6, null);
        NBTTagCompound pending = new NBTTagCompound();
        hatch.saveNBTData(pending);
        NBTTagList refunds = pending.getTagList("circuitBufferRefunds", 10);
        require(
            refunds.tagCount() == 1 && nativeCount(refunds.getCompoundTagAt(0), "longItems") == Long.MAX_VALUE
                && nativeCount(refunds.getCompoundTagAt(0), "longFluids") == Long.MAX_VALUE,
            "removing a full long pattern retains rejected ME refunds in a persisted queue");

        NBTTagCompound dropped = new NBTTagCompound();
        restored.setItemNBT(dropped);
        restored.refundAll(true);
        require(restoredSlot.isEmpty(), "serialized dropped-hatch reserves are not refunded a second time");
        var placed = (CircuitMEPatternBuffer) place(player.worldObj, player, 8,
            GTNGItemList.CircuitMEPatternBuffer.get(1)).getMetaTileEntity();
        placed.loadNBTData(dropped);
        NBTTagCompound replaced = new NBTTagCompound();
        placed.saveNBTData(replaced);
        refunds = replaced.getTagList("circuitBufferRefunds", 10);
        boolean keptMaximum = false;
        for (int i = 0; i < refunds.tagCount(); i++) if (
            nativeCount(refunds.getCompoundTagAt(i), "longItems") == Long.MAX_VALUE
                && nativeCount(refunds.getCompoundTagAt(i), "longFluids") == Long.MAX_VALUE
        ) keptMaximum = true;
        require(keptMaximum,
            "dropped and replaced hatch retains long input refunds without narrowing or duplicating them");
    }

    private static long nativeCount(NBTTagCompound saved, String key) {
        NBTTagList list = saved.getTagList(key, 10);
        return list.tagCount() == 0 ? 0 : Platform.readStackNBT(list.getCompoundTagAt(0), true).getStackSize();
    }

    private void checkManualSlots() {
        require(!hatch.hasVirtualMoldSlot(), "circuit hatch disables its inherited virtual mold slot");
        hatch.setMold(MoldDataManager.getMolds()[0]);
        require(hatch.getStackInSlot(hatch.getMoldSlot()) == null,
            "virtual mold selection cannot populate hidden legacy storage");
        ItemStack manual = new ItemStack(Items.diamond);
        hatch.setInventorySlotContents(hatch.getManualSlotStart(), manual);
        require(hatch.getSharedItems().length == 1 && hatch.getSharedItems()[0] == manual,
            "physical manual item slots remain shared and usable");
        hatch.setInventorySlotContents(hatch.getManualSlotStart(), null);
    }

    private void checkTerminalDiscovery(EntityPlayerMP player) throws Exception {
        require(AEApi.instance().registries().interfaceTerminal().getSupportedClasses().contains(
            CircuitMEPatternBuffer.class), "concrete circuit hatch class is registered for interface terminals");
        var owner = (MTEMultiBlockBase) place(player.worldObj, player, 5, GTNGItemList.LargeOreProcessor.get(1))
            .getMetaTileEntity();
        require(owner.addToMachineList(hatch.getBaseMetaTileEntity(), 0), "controller accepts circuit pattern hatch");
        String recipeName = owner.getRecipeMap().getDefaultRecipeCategory().unlocalizedName;
        require(hatch.getRawName().equals(recipeName), "structure attachment supplies the controller recipe-map name");
        require(hatch.numSlots() == hatch.rows() * hatch.rowSize() && hatch.numSlots() < 900,
            "terminal grows visible rows with stored patterns like the super hatch");
        hatch.setInventorySlotContents(898, firstPattern.copy());

        ItemStack terminal = GTNGItemList.WirelessDualInterfaceTerminal.get(1);
        player.inventory.setInventorySlotContents(0, terminal);
        var host = new DualTerminalGuiObject(
            AEApi.instance().registries().wireless().getWirelessTerminalHandler(terminal), terminal, player,
            player.worldObj, 0);
        TileWireless accessPoint = (TileWireless) player.worldObj.getTileEntity(0, 10, 2);
        var grid = hatch.getProxy().getGrid();
        require(accessPoint.getActionableNode().getGrid() == grid, "wireless terminal and hatch share the active grid");
        for (String name : new String[] { "targetGrid", "sg", "myWap" }) {
            Field field = WirelessTerminalGuiObject.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(host,
                name.equals("targetGrid") ? grid : name.equals("sg") ? grid.getCache(IStorageGrid.class) : accessPoint);
        }
        var container = new ContainerQuickEncodingTerminal(player.inventory, host);
        Field delegateField = ContainerQuickEncodingTerminal.class.getDeclaredField("interfaceDelegate");
        delegateField.setAccessible(true);
        var delegate = (ContainerInterfaceTerminal) delegateField.get(container);
        Field trackedField = ContainerInterfaceTerminal.class.getDeclaredField("tracked");
        trackedField.setAccessible(true);
        Object entry = ((Map<?, ?>) trackedField.get(delegate)).get(hatch);
        require(entry != null, "combined terminal discovers circuit pattern hatch through native interface registry");
        require((Boolean) trackerField(entry, "shouldDisplay"), "circuit pattern hatch is visible in interface list");
        require((Integer) trackerField(entry, "numSlots") == 900, "terminal entry exposes all 900 pattern slots");
        require(trackerField(entry, "name").equals(recipeName), "terminal entry uses the controller recipe-map name");
        long id = (Long) trackerField(entry, "id");
        ItemStack uploaded = pattern(24);
        player.inventory.setItemStack(uploaded.copy());
        for (int click = 0; click < 2; click++) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                InterfacePatternTarget.CODEC.write(buffer, new InterfacePatternTarget(id, 899));
                container.clickInterfacePatternAction.readIncoming(SyncEndpoint.CLIENT, SyncMode.FULL, buffer);
            } finally {
                buffer.release();
            }
            if (
                click == 0
            ) require(
                ItemStack.areItemStacksEqual(hatch.getStackInSlot(899), uploaded)
                    && player.inventory.getItemStack() == null,
                "combined terminal uploads encoded circuit to last slot");
            else require(
                hatch.getStackInSlot(899) == null
                    && ItemStack.areItemStacksEqual(player.inventory.getItemStack(), uploaded),
                "combined terminal retrieves the encoded circuit pattern");
        }
        player.inventory.setItemStack(null);
        hatch.setInventorySlotContents(898, null);
        container.onContainerClosed(player);
    }

    private static Object trackerField(Object entry, String name) throws Exception {
        Field field = entry.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(entry);
    }

    private static SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME> slotAt(
        CircuitMEPatternBuffer hatch, int index) {
        return find(hatch, hatch.getStackInSlot(index));
    }

    private static SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME> find(
        CircuitMEPatternBuffer hatch, ItemStack pattern) {
        var iterator = hatch.inventories();
        while (iterator.hasNext()) {
            var slot = iterator.next();
            if (!slot.hasChanged(pattern, hatch.getBaseMetaTileEntity().getWorld())) return slot;
        }
        throw new AssertionError("missing slot");
    }

    private static SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME> find(
        SuperMTEHatchCraftingInputSlave mirror, ItemStack pattern) {
        var iterator = mirror.inventories();
        while (iterator.hasNext()) {
            var slot = iterator.next();
            if (!slot.hasChanged(pattern, mirror.getBaseMetaTileEntity().getWorld())) return slot;
        }
        throw new AssertionError("missing mirrored slot");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !opened || !moldPlansVerified || finished || ++frames < 40) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainerWrapper gui)) return;
            var panel = gui.getScreen().getMainPanel();
            require(count(panel, PatternSlot.class) == 900, "scrollable GUI exposes all 900 patterns");
            require(count(panel, GhostCircuitSlotWidget.class) == 0, "GUI has no shared virtual circuit button");
            require(count(panel, GhostMoldSlotWidget.class) == 0, "GUI has no virtual mold slot");
            require(
                gui.getScreen().getSyncManager().getMainPSM().findPanelHandlerNullable("manual_slots_panel") != null,
                "manual item popup remains available");
            require(gui.getScreen().getSyncManager().getMainPSM().findPanelHandlerNullable("pattern_circuits") == null,
                "GUI has no pattern circuit configuration panel");
            screenshot("circuit-buffer.png");
            finish("PASS");
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish("FAIL");
        }
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static int count(IWidget widget, Class<?> type) {
        int count = type.isInstance(widget) ? 1 : 0;
        for (IWidget child : widget.getChildren()) count += count(child, type);
        return count;
    }

    private static MEInventoryCrafting materials(long amount) {
        return table(AEItemStack.create(new ItemStack(Items.nether_star)).setStackSize(amount),
            AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)).setStackSize(amount));
    }

    private static MEInventoryCrafting table(IAEStack<?>... inputs) {
        MEInventoryCrafting table = new MEInventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, inputs.length, 1);
        for (int i = 0; i < inputs.length; i++) table.setInventorySlotContents(i, inputs[i]);
        return table;
    }

    private static ItemStack pattern(int circuit) {
        ItemStack pattern = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        NBTTagList in = new NBTTagList();
        NBTTagList out = new NBTTagList();
        for (IAEStack<?> input : new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.nether_star)),
            AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)) }) {
            NBTTagCompound entry = new NBTTagCompound();
            input.writeToNBTGeneric(entry);
            in.appendTag(entry);
        }
        if (circuit >= 0) {
            NBTTagCompound entry = new NBTTagCompound();
            AEItemStack.create(GTUtility.getIntegratedCircuit(circuit)).writeToNBTGeneric(entry);
            in.appendTag(entry);
        }
        NBTTagCompound output = new NBTTagCompound();
        AEItemStack.create(new ItemStack(Items.apple)).writeToNBTGeneric(output);
        out.appendTag(output);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        tag.setBoolean("beSubstitute", true);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private static ItemStack moldPattern(ItemStack mold, int circuit, ItemStack output) {
        return moldPattern(mold, circuit, output, 144);
    }

    private static ItemStack moldPattern(ItemStack mold, int circuit, ItemStack output, int fluidAmount) {
        return moldPattern(mold, circuit, output, fluidAmount, 1);
    }

    private static ItemStack moldPattern(ItemStack mold, int circuit, ItemStack output, int fluidAmount,
        int circuitCount) {
        ItemStack pattern = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        List<IAEStack<?>> inputs = new ArrayList<>();
        inputs.add(AEItemStack.create(mold.copy()));
        inputs.add(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, fluidAmount)));
        if (circuit >= 0)
            inputs.add(AEItemStack.create(GTUtility.getIntegratedCircuit(circuit)).setStackSize(circuitCount));
        NBTTagList in = new NBTTagList();
        for (IAEStack<?> input : inputs) {
            NBTTagCompound entry = new NBTTagCompound();
            input.writeToNBTGeneric(entry);
            in.appendTag(entry);
        }
        NBTTagList out = new NBTTagList();
        NBTTagCompound entry = new NBTTagCompound();
        AEItemStack.create(output).writeToNBTGeneric(entry);
        out.appendTag(entry);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        tag.setBoolean("beSubstitute", true);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x, ItemStack stack) {
        world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        return tile;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("CIRCUIT_BUFFER_QA: " + message);
    }

    private void finish(String result) {
        try {
            Files.write(new File("result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }
}
