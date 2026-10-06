package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
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
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputSlave;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitMEPatternBuffer;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitPatternCodec;
import com.xyp.gtnotgood.common.utils.MoldDataManager;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPartHost;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.container.implementations.ContainerInterfaceTerminal;
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
import gregtech.api.interfaces.tileentity.IVoidable;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
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
    private int ticks, frames;
    private CircuitMEPatternBuffer hatch;
    private ItemStack firstPattern, secondPattern;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "circuit-buffer-" + System.currentTimeMillis(),
                "Circuit buffer QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
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
        var definitions = AEApi.instance()
            .definitions();
        world.setBlock(
            0,
            10,
            1,
            definitions.blocks()
                .multiPart()
                .maybeBlock()
                .get());
        ((IPartHost) world.getTileEntity(0, 10, 1)).addPart(
            definitions.parts()
                .cableGlass()
                .stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN,
            player);
        world.setBlock(
            1,
            10,
            1,
            definitions.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        world.setBlock(
            0,
            10,
            2,
            definitions.blocks()
                .wireless()
                .maybeBlock()
                .get());
        TileWireless accessPoint = (TileWireless) world.getTileEntity(0, 10, 2);
        accessPoint.setOrientation(ForgeDirection.SOUTH, ForgeDirection.UP);
        accessPoint.onReady();
        world.setBlock(
            -1,
            10,
            1,
            definitions.blocks()
                .drive()
                .maybeBlock()
                .get());
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
        hatch.inventories()
            .forEachRemaining(slots::add);
        var first = slots.get(0);
        var second = slots.get(1);
        var details = first.getPatternDetails();
        require(
            details.getCondensedAEInputs().length == 2 && !CircuitPatternCodec.isCircuit(details.getAEInputs()[0]),
            "AE requests material and native fluid without circuit");
        var cache = (CraftingGridCache) hatch.getProxy()
            .getCrafting();
        require(
            cache.getMediums(details)
                .contains(hatch),
            "runtime pattern registered in native AE cache");
        require(
            !details.equals(second.getPatternDetails()),
            "identical materials with different circuits stay distinct");
        var redecoded = CircuitPatternCodec.decode(
            details.getPattern()
                .copy(),
            world);
        require(
            details.equals(redecoded) && details.hashCode() == redecoded.hashCode(),
            "CPU native pattern identity reload");
        require(hatch.pushPattern(details, materials(3)), "CPU pushes circuit-free native item and fluid batch");
        require(hatch.pushPattern(second.getPatternDetails(), materials(3)), "second circuit accepts its own batch");
        require(
            first.getItemInputs()[1].getItemDamage() == 1 && second.getItemInputs()[1].getItemDamage() == 2,
            "per-pattern live virtual circuits");
        require(first.getPatternInputs().inputItems[1].getItemDamage() == 1, "GT recipe signature contains circuit");

        for (int config = 1; config <= 3; config++) {
            GTRecipeBuilder.builder()
                .itemInputs(new ItemStack(Items.nether_star))
                .circuit(config)
                .fluidInputs(new FluidStack(FluidRegistry.WATER, 1))
                .itemOutputs(new ItemStack(config == 1 ? Items.apple : Items.carrot))
                .duration(20)
                .eut(8)
                .addTo(RecipeMaps.mixerRecipes);
        }
        for (int i = 0; i < 2; i++) {
            var slot = slots.get(i);
            var logic = new ProcessingLogic().setRecipeMapSupplier(() -> RecipeMaps.mixerRecipes)
                .setMachine(
                    (IVoidable) GregTechAPI.METATILEENTITIES[GTNGItemList.AssemblerMatrix.get(1)
                        .getItemDamage()])
                .setVoidProtection(false, false)
                .setAvailableVoltage(32)
                .setAvailableAmperage(1)
                .setMaxParallel(1)
                .setInputItems(slot.getItemInputs())
                .setInputFluids(slot.getFluidInputs());
            require(
                logic.process()
                    .wasSuccessful(),
                "GT processing succeeds with circuit " + (i + 1));
            require(
                logic.getOutputItems()[0].getItem() == (i == 0 ? Items.apple : Items.carrot),
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
        var restoredSlot = restoredHatch.inventories()
            .next();
        require(
            restoredSlot.getItemInputs()[0].stackSize == 2 && restoredSlot.getFluidInputs()[0].amount == 2
                && restoredSlot.getItemInputs()[1].getItemDamage() == 1,
            "tile NBT reload preserves input quantities and circuit");
        var mirror = (SuperMTEHatchCraftingInputSlave) place(
            world,
            player,
            3,
            GTNGItemList.SuperMTEHatchCraftingInputSlave.get(1)).getMetaTileEntity();
        mirror.trySetMasterFromCoord(0, 10, 0);
        require(
            mirror.inventories()
                .next()
                .getItemInputs()[1].getItemDamage() == 1,
            "existing mirror exposes same independent circuit buffer");

        ItemStack large = pattern(3);
        hatch.setInventorySlotContents(2, large);
        var largeSlot = slotAt(hatch, 2);
        long aboveInt = (long) Integer.MAX_VALUE + 123;
        require(largeSlot.insertItemsAndFluids(materials(aboveInt)), "buffers accept a native AE batch above 2^31-1");
        ItemStack itemView = largeSlot.getItemInputs()[0];
        FluidStack fluidView = largeSlot.getFluidInputs()[0];
        require(
            itemView.stackSize == Integer.MAX_VALUE && fluidView.amount == Integer.MAX_VALUE,
            "GT recipe views are int-sized while the reserve remains long-sized");
        var longLogic = new ProcessingLogic().setRecipeMapSupplier(() -> RecipeMaps.mixerRecipes)
            .setMachine(
                (IVoidable) GregTechAPI.METATILEENTITIES[GTNGItemList.AssemblerMatrix.get(1)
                    .getItemDamage()])
            .setVoidProtection(false, false)
            .setAvailableVoltage(32)
            .setAvailableAmperage(1)
            .setMaxParallel(1)
            .setInputItems(largeSlot.getItemInputs())
            .setInputFluids(largeSlot.getFluidInputs());
        require(
            longLogic.process()
                .wasSuccessful(),
            "native GT processing consumes from the long-backed recipe view");
        require(
            largeSlot.getStoredItems()
                .get(0)
                .getStackSize() == aboveInt - 1
                && largeSlot.getStoredFluids()
                    .get(0)
                    .getStackSize() == aboveInt - 1
                && largeSlot.getItemInputs()[0] == itemView
                && largeSlot.getFluidInputs()[0] == fluidView
                && itemView.stackSize == Integer.MAX_VALUE
                && fluidView.amount == Integer.MAX_VALUE,
            "exact recipe debit refills the same live item and fluid views without changing the reserve identity");
        require(
            largeSlot.insertItemsAndFluids(materials(Long.MAX_VALUE - (aboveInt - 1))),
            "each item and fluid type can fill to 2^63-1 without allocating int chunks");
        require(!largeSlot.insertItemsAndFluids(materials(1)), "full buffers reject complete batch");
        require(
            largeSlot.getStoredItems()
                .get(0)
                .getStackSize() == Long.MAX_VALUE
                && largeSlot.getStoredFluids()
                    .get(0)
                    .getStackSize() == Long.MAX_VALUE,
            "rejection leaves both resource quantities intact");
        require(
            !largeSlot.insertItemsAndFluids(
                table(
                    AEItemStack.create(new ItemStack(Items.diamond)),
                    AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1))))
                && largeSlot.getItemInputs().length == 2,
            "late fluid overflow cannot partially insert item");

        hatch.setInventorySlotContents(3, pattern(4));
        var types = slotAt(hatch, 3);
        IAEStack<?>[] different = new IAEStack<?>[64];
        for (int i = 0; i < different.length; i++) {
            ItemStack item = new ItemStack(Items.paper);
            item.setTagCompound(new NBTTagCompound());
            item.getTagCompound()
                .setInteger("qaType", i);
            different[i] = AEItemStack.create(item);
        }
        require(types.insertItemsAndFluids(table(different)), "64 NBT-distinct item types");
        require(
            !types.insertItemsAndFluids(table(AEItemStack.create(new ItemStack(Items.diamond)))),
            "65th item type rejected");
        hatch.setInventorySlotContents(4, pattern(5));
        var fluidTypes = slotAt(hatch, 4);
        var registered = new ArrayList<>(
            FluidRegistry.getRegisteredFluids()
                .values());
        registered.remove(FluidRegistry.WATER);
        registered.remove(FluidRegistry.LAVA);
        require(registered.size() >= 64, "pack provides 64 distinct native fluid identities");
        for (int i = 0; i < different.length; i++)
            different[i] = AEFluidStack.create(new FluidStack(registered.get(i), 1));
        require(
            fluidTypes.insertItemsAndFluids(table(different)) && fluidTypes.getFluidInputs().length == 64,
            "64 distinct native fluid types");
        require(
            !fluidTypes.insertItemsAndFluids(table(AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1)))),
            "65th fluid type rejected");

        hatch.setInventorySlotContents(899, pattern(24));
        var last = slotAt(hatch, 899);
        require(
            last.getPatternInputs().inputItems[1].getItemDamage() == 24 && last.getPatternDetails()
                .getCondensedAEInputs().length == 2,
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
        require(
            hatch.pushPattern(CircuitPatternCodec.decode(firstPattern, world), materials(1)) == false,
            "unregistered raw pattern cannot bypass virtual circuit medium");
        require(!hatch.pushPattern(largeSlot.getPatternDetails(), materials(1)), "CPU full-buffer push rejected");
        checkLongPersistence(player, largeSlot);
        checkManualSlots();
        checkNativeRefund(player, largeSlot);
    }

    private void checkNativeRefund(EntityPlayerMP player, SuperMTEHatchCraftingInputME.PatternSlot<?> largeSlot)
        throws GridAccessException {
        TileDrive drive = (TileDrive) player.worldObj.getTileEntity(-1, 10, 1);
        require(
            drive.getActionableNode()
                .getGrid()
                == hatch.getProxy()
                    .getGrid(),
            "empty ME drive shares the active test grid");
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
        drive.getInternalInventory()
            .setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
        largeSlot.refund(hatch.getProxy(), hatch.getMEOutputActionSource(), false);
        var storage = hatch.getProxy()
            .getStorage();
        var source = hatch.getMEOutputActionSource();
        var returnedItems = storage.getItemInventory()
            .extractItems(
                AEItemStack.create(new ItemStack(Items.nether_star))
                    .setStackSize(Long.MAX_VALUE),
                Actionable.SIMULATE,
                source);
        var returnedFluids = storage.getFluidInventory()
            .extractItems(
                AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1))
                    .setStackSize(Long.MAX_VALUE),
                Actionable.SIMULATE,
                source);
        require(
            returnedItems != null && returnedItems.getStackSize() > 0
                && returnedFluids != null
                && returnedFluids.getStackSize() > 0,
            "native powered refund exports both long-backed channels into real storage cells");
        require(
            largeSlot.getStoredItems()
                .get(0)
                .getStackSize() == Long.MAX_VALUE - returnedItems.getStackSize()
                && largeSlot.getStoredFluids()
                    .get(0)
                    .getStackSize() == Long.MAX_VALUE - returnedFluids.getStackSize(),
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
            restoredSlot.getStoredItems()
                .get(0)
                .getStackSize() == Long.MAX_VALUE
                && restoredSlot.getStoredFluids()
                    .get(0)
                    .getStackSize() == Long.MAX_VALUE,
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
            legacySlot.getStoredItems()
                .get(0)
                .getStackSize() == 73
                && legacySlot.getStoredFluids()
                    .get(0)
                    .getStackSize() == 95,
            "existing int-buffer saves migrate without changing item or fluid counts");

        hatch.setInventorySlotContents(6, pattern(7));
        var secondLong = slotAt(hatch, 6);
        require(
            secondLong.insertItemsAndFluids(materials(Long.MAX_VALUE)),
            "another pattern keeps its own long maximum");
        require(
            largeSlot.getStoredItems()
                .get(0)
                .getStackSize() == Long.MAX_VALUE,
            "long inventories remain isolated");
        NBTTagCompound waila = new NBTTagCompound();
        hatch.getWailaNBTData(
            player,
            (BaseMetaTileEntity) hatch.getBaseMetaTileEntity(),
            waila,
            player.worldObj,
            0,
            10,
            0);
        NBTTagList diagnostics = waila.getTagList("inventory", 10);
        String expected = NumberFormat.getIntegerInstance(Locale.ROOT)
            .format(
                BigInteger.valueOf(Long.MAX_VALUE)
                    .multiply(BigInteger.valueOf(2))
                    .add(BigInteger.valueOf(5)));
        boolean correct = false;
        for (int i = 0; i < diagnostics.tagCount(); i++) {
            NBTTagCompound entry = diagnostics.getCompoundTagAt(i);
            if (entry.getString("name")
                .equals(new ItemStack(Items.nether_star).getDisplayName()))
                correct = expected.equals(entry.getString("amountText"));
        }
        require(correct, "Waila aggregate across independent long buffers does not overflow a signed long");

        hatch.setInventorySlotContents(7, pattern(8));
        var overflow = slotAt(hatch, 7);
        require(
            !overflow.insertItemsAndFluids(
                table(
                    AEItemStack.create(new ItemStack(Items.nether_star))
                        .setStackSize(Long.MAX_VALUE),
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
        var placed = (CircuitMEPatternBuffer) place(
            player.worldObj,
            player,
            8,
            GTNGItemList.CircuitMEPatternBuffer.get(1)).getMetaTileEntity();
        placed.loadNBTData(dropped);
        NBTTagCompound replaced = new NBTTagCompound();
        placed.saveNBTData(replaced);
        refunds = replaced.getTagList("circuitBufferRefunds", 10);
        boolean keptMaximum = false;
        for (int i = 0; i < refunds.tagCount(); i++)
            if (nativeCount(refunds.getCompoundTagAt(i), "longItems") == Long.MAX_VALUE
                && nativeCount(refunds.getCompoundTagAt(i), "longFluids") == Long.MAX_VALUE) keptMaximum = true;
        require(
            keptMaximum,
            "dropped and replaced hatch retains long input refunds without narrowing or duplicating them");
    }

    private static long nativeCount(NBTTagCompound saved, String key) {
        NBTTagList list = saved.getTagList(key, 10);
        return list.tagCount() == 0 ? 0
            : Platform.readStackNBT(list.getCompoundTagAt(0), true)
                .getStackSize();
    }

    private void checkManualSlots() {
        require(!hatch.hasVirtualMoldSlot(), "circuit hatch disables its inherited virtual mold slot");
        hatch.setMold(MoldDataManager.getMolds()[0]);
        require(
            hatch.getStackInSlot(hatch.getMoldSlot()) == null,
            "virtual mold selection cannot populate hidden legacy storage");
        ItemStack manual = new ItemStack(Items.diamond);
        hatch.setInventorySlotContents(hatch.getManualSlotStart(), manual);
        require(
            hatch.getSharedItems().length == 1 && hatch.getSharedItems()[0] == manual,
            "physical manual item slots remain shared and usable");
        hatch.setInventorySlotContents(hatch.getManualSlotStart(), null);
    }

    private void checkTerminalDiscovery(EntityPlayerMP player) throws Exception {
        require(
            AEApi.instance()
                .registries()
                .interfaceTerminal()
                .getSupportedClasses()
                .contains(CircuitMEPatternBuffer.class),
            "concrete circuit hatch class is registered for interface terminals");
        var owner = (MTEMultiBlockBase) place(player.worldObj, player, 5, GTNGItemList.LargeOreProcessor.get(1))
            .getMetaTileEntity();
        require(owner.addToMachineList(hatch.getBaseMetaTileEntity(), 0), "controller accepts circuit pattern hatch");
        String recipeName = owner.getRecipeMap()
            .getDefaultRecipeCategory().unlocalizedName;
        require(
            hatch.getRawName()
                .equals(recipeName),
            "structure attachment supplies the controller recipe-map name");
        require(
            hatch.numSlots() == hatch.rows() * hatch.rowSize() && hatch.numSlots() < 900,
            "terminal grows visible rows with stored patterns like the super hatch");
        hatch.setInventorySlotContents(898, firstPattern.copy());

        ItemStack terminal = GTNGItemList.WirelessDualInterfaceTerminal.get(1);
        player.inventory.setInventorySlotContents(0, terminal);
        var host = new DualTerminalGuiObject(
            AEApi.instance()
                .registries()
                .wireless()
                .getWirelessTerminalHandler(terminal),
            terminal,
            player,
            player.worldObj,
            0);
        TileWireless accessPoint = (TileWireless) player.worldObj.getTileEntity(0, 10, 2);
        var grid = hatch.getProxy()
            .getGrid();
        require(
            accessPoint.getActionableNode()
                .getGrid() == grid,
            "wireless terminal and hatch share the active grid");
        for (String name : new String[] { "targetGrid", "sg", "myWap" }) {
            Field field = WirelessTerminalGuiObject.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(
                host,
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
            if (click == 0) require(
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
        Field field = entry.getClass()
            .getDeclaredField(name);
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
            if (!slot.hasChanged(
                pattern,
                hatch.getBaseMetaTileEntity()
                    .getWorld()))
                return slot;
        }
        throw new AssertionError("missing slot");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !opened || finished || ++frames < 40) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainerWrapper gui)) return;
            var panel = gui.getScreen()
                .getMainPanel();
            require(count(panel, PatternSlot.class) == 900, "scrollable GUI exposes all 900 patterns");
            require(count(panel, GhostCircuitSlotWidget.class) == 0, "GUI has no shared virtual circuit button");
            require(count(panel, GhostMoldSlotWidget.class) == 0, "GUI has no virtual mold slot");
            require(
                gui.getScreen()
                    .getSyncManager()
                    .getMainPSM()
                    .findPanelHandlerNullable("manual_slots_panel") != null,
                "manual item popup remains available");
            require(
                gui.getScreen()
                    .getSyncManager()
                    .getMainPSM()
                    .findPanelHandlerNullable("pattern_circuits") == null,
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
        return table(
            AEItemStack.create(new ItemStack(Items.nether_star))
                .setStackSize(amount),
            AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1))
                .setStackSize(amount));
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
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
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
            AEItemStack.create(GTUtility.getIntegratedCircuit(circuit))
                .writeToNBTGeneric(entry);
            in.appendTag(entry);
        }
        NBTTagCompound output = new NBTTagCompound();
        AEItemStack.create(new ItemStack(Items.apple))
            .writeToNBTGeneric(output);
        out.appendTag(output);
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
        Minecraft.getMinecraft()
            .shutdown();
    }
}
