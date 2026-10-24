package com.xyp.gtnotgood.qa;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.gtnewhorizon.structurelib.ChannelDescription;
import com.gtnewhorizon.structurelib.ConfigurationHandler;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.net.SetChannelDataMessage;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElementCheckOnly;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.mojang.authlib.GameProfile;
import com.xyp.gtnotgood.common.items.multiblockterminal.MultiblockTerminalItem;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalItemSource;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalOperations;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalSettings;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.storage.data.IAEFluidStack;
import appeng.items.misc.ItemMEStackPacket;
import appeng.util.item.AEFluidStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import forestry.api.storage.IBackpackDefinition;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicTank;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.structure.error.StructureError;
import gregtech.common.misc.GTStructureChannels;
import gregtech.common.tileentities.machines.multi.MTEElectricBlastFurnace;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "multiblockterminalserverqa",
    name = "Multiblock Terminal Server QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class MultiblockTerminalServerChecks {

    private static Block controllerBlock;
    private int checks;
    private EntityPlayerMP actor;
    private TerminalStorageServerChecks storage;
    private int storageTicks;
    private boolean storageDone;

    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.terminal.qa")) return;
        controllerBlock = new FixtureBlock();
        GameRegistry.registerBlock(controllerBlock, "terminal_fixture");
        GameRegistry.registerTileEntity(FixtureController.class, "terminal_qa_controller");
        GameRegistry.registerTileEntity(NativeFixtureController.class, "terminal_qa_native_controller");
        ChannelDescription.item("qa_tier", 1, new ItemStack(Blocks.wool, 1, 0));
        ChannelDescription.item("qa_tier", 2, new ItemStack(Blocks.wool, 1, 1));
    }

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("gtng.terminal.serverQA")) return;
        MinecraftServer server = MinecraftServer.getServer();
        try {
            require(server.isDedicatedServer(), "dedicated server runtime");
            verify(server);
            storage = new TerminalStorageServerChecks(server, actor);
            FMLCommonHandler.instance().bus().register(this);
        } catch (Throwable failure) {
            writeResult("FAIL: " + failure);
            server.initiateShutdown();
            throw new AssertionError("multiblock terminal server QA failed", failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || storage == null || storageDone) return;
        MinecraftServer server = MinecraftServer.getServer();
        try {
            if (++storageTicks > 600) throw new AssertionError("terminal AE storage QA timed out");
            if (!storage.tick()) return;
            storageDone = true;
            writeResult("PASS");
            System.out.println("MULTIBLOCK_TERMINAL_SERVER_QA: PASS (" + checks
                + " core + "
                + storage.checks()
                + " physical storage checks)");
            server.initiateShutdown();
        } catch (Throwable failure) {
            storageDone = true;
            failure.printStackTrace();
            try {
                writeResult("FAIL: " + failure);
            } catch (Exception writeFailure) {
                failure.addSuppressed(writeFailure);
            }
            server.initiateShutdown();
        }
    }

    public int verify(MinecraftServer server) throws Exception {
        WorldServer world = server.worldServerForDimension(0);
        resetFixtures(world);
        EntityPlayerMP player = new EntityPlayerMP(server, world,
            new GameProfile(UUID.fromString("23c6d89a-d6f0-4af8-b76e-71ecf7f9c5f0"), "TerminalQA"),
            new ItemInWorldManager(world));
        new NetHandlerPlayServer(server, new NetworkManager(false), player);
        actor = player;
        player.setGameType(WorldSettings.GameType.SURVIVAL);
        player.setPosition(128.5, 80, 128.5);
        player.inventory.currentItem = 0;
        ItemStack terminal = terminal();
        player.inventory.mainInventory[0] = terminal;
        verifySettings(terminal);
        verifyNativeChannels(player, terminal);
        verifyCrafting(world, terminal);
        verifyBuildAndDismantle(world, player, terminal, false);
        verifyBuildAndDismantle(world, player, terminal, true);
        verifyMissingMaterials(world, player, terminal);
        verifyBreakProtection(world, player, terminal);
        verifyReplacement(world, player, terminal);
        verifyNativeHatches(world, player, terminal);
        verifyReplaceableBlocks(world, player, terminal);
        verifyLiquids(world, player, terminal);
        verifyForestry(world, player, terminal);
        verifyNativeBlastFurnace(world, player, terminal);
        verifyNativeProjector(world, player, terminal);
        verifyCreativeBlastFurnace(world, player, terminal);
        checks += TerminalNativeStructuresServerChecks.verify(server, player);
        return checks;
    }

    private void verifyCrafting(World world, ItemStack terminal) {
        IRecipe selected = null;
        for (Object entry : CraftingManager.getInstance().getRecipeList()) {
            IRecipe recipe = (IRecipe) entry;
            ItemStack output = recipe.getRecipeOutput();
            if (output != null && output.getItem() == terminal.getItem()) {
                selected = recipe;
                break;
            }
        }
        require(selected instanceof ShapedOreRecipe, "terminal recipe is registered as a real shaped recipe");
        InventoryCrafting inventory = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        Object[] inputs = ((ShapedOreRecipe) selected).getInput();
        for (int slot = 0; slot < inputs.length; slot++) {
            Object input = inputs[slot];
            ItemStack stack = input instanceof ItemStack item ? item.copy()
                : input instanceof List<?>list && !list.isEmpty() ? ((ItemStack) list.get(0)).copy() : null;
            require(stack != null, "terminal recipe ingredient exists in slot " + slot);
            stack.stackSize = 1;
            inventory.setInventorySlotContents(slot, stack);
        }
        ItemStack projector = inventory.getStackInSlot(4);
        require(projector.getItem() instanceof ItemConstructableTrigger, "terminal recipe upgrades native projector");
        projector.stackSize = 16;
        ChannelDataAccessor.setChannelData(projector, "qa_recipe_channel", 23);
        require(selected.matches(inventory, world), "actual terminal recipe matches ore-dictionary ingredients");
        ItemStack result = selected.getCraftingResult(inventory);
        require(result.stackSize == 1 && TerminalSettings.count(result) == 16,
            "crafting converts projector quantity to terminal master channel without duplicating terminals");
        require(ChannelDataAccessor.getChannelData(result, "qa_recipe_channel") == 23,
            "crafting preserves native projector channels");
        ChannelDataAccessor.setChannelData(result, "qa_recipe_channel", 99);
        require(ChannelDataAccessor.getChannelData(projector, "qa_recipe_channel") == 23,
            "crafted terminal owns an independent NBT copy");
    }

    private void verifyReplaceableBlocks(WorldServer world, EntityPlayerMP player, ItemStack terminal) {
        int x = 320, y = 80, z = 128;
        prepare(world, x, y, z);
        player.setPosition(x + 0.5, y, z - 2.5);
        clearSupplies(player);
        int[] grass = position(x, y, z, 1, false), snow = position(x, y, z, 2, false);
        world.setBlock(grass[0], grass[1] - 1, grass[2], Blocks.grass);
        world.setBlock(snow[0], snow[1] - 1, snow[2], Blocks.stone);
        world.setBlock(grass[0], grass[1], grass[2], Blocks.tallgrass, 1, 3);
        world.setBlock(snow[0], snow[1], snow[2], Blocks.snow_layer, 0, 3);
        player.inventory.mainInventory[1] = new ItemStack(Blocks.cobblestone);
        player.inventory.mainInventory[2] = new ItemStack(Blocks.wool);
        player.inventory.mainInventory[3] = new ItemStack(Blocks.planks);
        TerminalSettings.setDismantle(terminal, false);
        TerminalSettings.setReplace(terminal, false);
        operate(terminal, player, world, x, y, z);
        require(block(world, x, y, z, 1, false) == Blocks.cobblestone && block(world, x, y, z, 2, false) == Blocks.wool,
            "normal build replaces native tall grass and snow without selected-tier replacement mode");
        require(count(player, Blocks.cobblestone, 0) == 0 && count(player, Blocks.wool, 0) == 0,
            "native replaceable-block placement consumes exact supplied materials");
        cleanup(world, x, y, z);
        world.setBlockToAir(grass[0], grass[1] - 1, grass[2]);
        world.setBlockToAir(snow[0], snow[1] - 1, snow[2]);
    }

    private void verifyLiquids(WorldServer world, EntityPlayerMP player, ItemStack terminal) {
        int x = 352, y = 80, z = 128;
        for (int variant = 1; variant <= 2; variant++) {
            prepare(world, x, y, z);
            FixtureController controller = (FixtureController) world.getTileEntity(x, y, z);
            controller.liquid = variant;
            player.setPosition(x + 0.5, y, z - 2.5);
            clearSupplies(player);
            Item bucket = variant == 1 ? Items.water_bucket : Items.lava_bucket;
            Block liquid = variant == 1 ? Blocks.water : Blocks.lava;
            player.inventory.mainInventory[1] = new ItemStack(bucket);
            TerminalSettings.setDismantle(terminal, false);
            operate(terminal, player, world, x, y, z);
            require(block(world, x, y, z, 1, false) == liquid,
                "native fluid structure builds its source block from one bucket " + variant);
            require(countRawItem(player, bucket) == 0 && countRawItem(player, Items.bucket) == 1,
                "fluid construction consumes one full bucket and returns the empty bucket " + variant);
            TerminalSettings.setDismantle(terminal, true);
            operate(terminal, player, world, x, y, z);
            require(block(world, x, y, z, 1, false) == Blocks.air,
                "native fluid structure source removed in dismantle mode " + variant);
            require(countRawItem(player, bucket) == 1 && countRawItem(player, Items.bucket) == 0,
                "fluid dismantle refills exactly the original bucket " + variant);
            cleanup(world, x, y, z);
        }
    }

    private void verifyForestry(WorldServer world, EntityPlayerMP player, ItemStack terminal) throws Exception {
        int x = 384, y = 80, z = 128;
        prepare(world, x, y, z);
        player.setPosition(x + 0.5, y, z - 2.5);
        clearSupplies(player);
        Item bagItem = GameRegistry.findItem(ModList.Forestry.getID(), "diggerBag");
        require(bagItem != null && bagItem.getClass().getName().equals("forestry.storage.items.ItemBackpack"),
            "fixed Forestry digger backpack is registered");
        ItemStack bag = new ItemStack(bagItem);
        IBackpackDefinition definition = (IBackpackDefinition) bagItem.getClass().getMethod("getDefinition")
            .invoke(bagItem);
        require(definition.isValidItem(new ItemStack(Blocks.cobblestone)),
            "native Forestry backpack definition accepts fixture casing material");
        player.inventory.mainInventory[1] = bag;
        setBlock(world, x, y, z, 1, false, Blocks.cobblestone);
        TerminalSettings.setDismantle(terminal, true);
        operate(terminal, player, world, x, y, z);
        require(backpackCount(player, bag, Blocks.cobblestone) == 1 && count(player, Blocks.cobblestone, 0) == 0,
            "dismantled casing enters the real Forestry backpack first");
        TerminalSettings.setDismantle(terminal, false);
        operate(terminal, player, world, x, y, z);
        require(
            block(world, x, y, z, 1, false) == Blocks.cobblestone
                && backpackCount(player, bag, Blocks.cobblestone) == 0,
            "terminal material source can build back from the same Forestry backpack");
        bag.setItemDamage(1);
        TerminalSettings.setDismantle(terminal, true);
        operate(terminal, player, world, x, y, z);
        require(backpackCount(player, bag, Blocks.cobblestone) == 0 && count(player, Blocks.cobblestone, 0) == 1,
            "locked native Forestry backpack is respected and recovered casing falls back to player inventory");
        for (int slot = 2; slot < player.inventory.mainInventory.length; slot++)
            player.inventory.mainInventory[slot] = null;
        bag.setItemDamage(0);
        TerminalItemSource source = new TerminalItemSource(terminal, player);
        ItemStack tagged = new ItemStack(Blocks.cobblestone);
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound().setString("TerminalQA", "backpack-material");
        ItemStack supplied = tagged.copy();
        supplied.stackSize = 6;
        source.give(supplied);
        require(backpackCount(player, bag, Blocks.cobblestone) == 6,
            "native Forestry backpack stores tagged materials");
        NBTTagCompound before = (NBTTagCompound) bag.getTagCompound().copy();
        int simulated = source
            .take(stack -> stack.isItemEqual(tagged) && ItemStack.areItemStackTagsEqual(stack, tagged), true, 4)
            .values().stream().mapToInt(Integer::intValue).sum();
        require(simulated == 4 && before.equals(bag.getTagCompound()),
            "Forestry quantity simulation leaves the real backpack NBT unchanged");
        require(source.takeOne(tagged, true) && before.equals(bag.getTagCompound()),
            "Forestry exact-item simulation leaves the real backpack NBT unchanged");
        int taken = source
            .take(stack -> stack.isItemEqual(tagged) && ItemStack.areItemStackTagsEqual(stack, tagged), false, 4)
            .values().stream().mapToInt(Integer::intValue).sum();
        require(taken == 4 && backpackCount(player, bag, Blocks.cobblestone) == 2,
            "Forestry quantity extraction persists exactly four consumed materials");
        require(source.takeOne(tagged, false) && backpackCount(player, bag, Blocks.cobblestone) == 1,
            "Forestry exact-item extraction persists exactly one consumed material");
        player.inventory.mainInventory[2] = tagged.copy();
        require(
            source.takeOne(tagged, false) && backpackCount(player, bag, Blocks.cobblestone) == 1
                && player.inventory.mainInventory[2] == null,
            "native player material source retains priority over the Forestry fallback");
        bag.setItemDamage(1);
        require(
            !source.takeOne(tagged, true) && source.take(stack -> stack.isItemEqual(tagged), false, 1).isEmpty()
                && backpackCount(player, bag, Blocks.cobblestone) == 1,
            "locked Forestry backpack does not supply terminal materials");
        bag.setItemDamage(2);
        require(!source.takeOne(tagged, false) && backpackCount(player, bag, Blocks.cobblestone) == 1,
            "receive-only Forestry backpack does not supply terminal materials");
        source.give(tagged);
        require(backpackCount(player, bag, Blocks.cobblestone) == 2,
            "receive-only Forestry backpack still accepts recovered materials");
        bag.setItemDamage(3);
        require(source.takeOne(tagged, false) && backpackCount(player, bag, Blocks.cobblestone) == 1,
            "resupply Forestry backpack supplies terminal materials");
        cleanup(world, x, y, z);
    }

    private static int backpackCount(EntityPlayerMP player, ItemStack bag, Block block) throws Exception {
        int size = (Integer) bag.getItem().getClass().getMethod("getBackpackSize").invoke(bag.getItem());
        IInventory inventory = (IInventory) Class.forName("forestry.storage.inventory.ItemInventoryBackpack")
            .getConstructor(EntityPlayer.class, int.class, ItemStack.class).newInstance(player, size, bag);
        int count = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() == Item.getItemFromBlock(block)) count += stack.stackSize;
        }
        return count;
    }

    private static int countRawItem(EntityPlayerMP player, Item item) {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory)
            if (stack != null && stack.getItem() == item) count += stack.stackSize;
        return count;
    }

    public static ItemStack terminal() {
        Item item = GameRegistry.findItem(ModList.ModIds.GT_NOT_GOOD, "multiblock_terminal");
        if (!(item instanceof MultiblockTerminalItem)) throw new AssertionError("registered terminal item missing");
        return new ItemStack(item);
    }

    public static void projectionFixture(World world, int x, int y, int z) {
        world.setBlock(x, y, z, controllerBlock, 0, 3);
    }

    private void verifySettings(ItemStack terminal) {
        require(terminal.getItem() instanceof ItemConstructableTrigger,
            "terminal inherits all native projector recognition paths");
        require(!TerminalSettings.dismantle(terminal) && TerminalSettings.noHatches(terminal)
            && !TerminalSettings.replace(terminal)
            && !TerminalSettings.mirror(terminal)
            && !TerminalSettings.ae(terminal)
            && TerminalSettings.fullBuild(terminal)
            && TerminalSettings.count(terminal) == 1, "default terminal settings");
        TerminalSettings.setCount(terminal, 648);
        ChannelDataAccessor.setChannelData(terminal, "qa_arbitrary_channel", 72);
        ItemStack trigger = TerminalSettings.trigger(terminal);
        require(trigger.stackSize == 648 && terminal.stackSize == 1,
            "virtual master channel does not duplicate the real terminal");
        require(ChannelDataAccessor.getChannelData(trigger, "qa_arbitrary_channel") == 72,
            "virtual master channel retains arbitrary subchannels");
        TerminalSettings.setDismantle(terminal, true);
        TerminalSettings.setFullBuild(terminal, false);
        TerminalSettings.setNoHatches(terminal, false);
        TerminalSettings.setReplace(terminal, true);
        TerminalSettings.setMirror(terminal, true);
        TerminalSettings.setAe(terminal, true);
        ItemStack restored = ItemStack.loadItemStackFromNBT(terminal.writeToNBT(new NBTTagCompound()));
        require(TerminalSettings.count(restored) == 648 && TerminalSettings.dismantle(restored)
            && !TerminalSettings.noHatches(restored)
            && TerminalSettings.replace(restored)
            && TerminalSettings.mirror(restored)
            && !TerminalSettings.fullBuild(restored)
            && TerminalSettings.ae(restored), "all settings survive NBT reload");
        require(ChannelDataAccessor.getChannelData(restored, "qa_arbitrary_channel") == 72,
            "native subchannel survives terminal NBT reload");
        TerminalSettings.setCount(terminal, 0);
        require(TerminalSettings.count(terminal) == 1, "master channel lower bound");
        TerminalSettings.setCount(terminal, Integer.MAX_VALUE);
        require(TerminalSettings.count(terminal) == 648, "master channel upper bound");
        TerminalSettings.setCount(terminal, 1);
        TerminalSettings.setDismantle(terminal, false);
        TerminalSettings.setFullBuild(terminal, true);
        TerminalSettings.setNoHatches(terminal, true);
        TerminalSettings.setReplace(terminal, false);
        TerminalSettings.setMirror(terminal, false);
        TerminalSettings.setAe(terminal, false);
        String hatchChannel = GTStructureChannels.HATCH.get();
        ChannelDataAccessor.setChannelData(terminal, hatchChannel, 4);
        ChannelDataAccessor.setChannelData(terminal, "hatch", 9);
        require(
            ChannelDataAccessor.getChannelData(TerminalSettings.projectorTrigger(terminal), hatchChannel) == 4
                && !ChannelDataAccessor.hasSubChannel(TerminalSettings.trigger(terminal), hatchChannel),
            "native projector trigger retains explicit hatch channel independently of terminal hatch settings");
        require(
            ChannelDataAccessor.getChannelData(TerminalSettings.projectorTrigger(terminal), "hatch") == 9
                && ChannelDataAccessor.getChannelData(TerminalSettings.trigger(terminal), "hatch") == 9,
            "native hatch filtering preserves an unrelated user channel named hatch");
        ChannelDataAccessor.unsetChannelData(terminal, hatchChannel);
        ChannelDataAccessor.unsetChannelData(terminal, "hatch");
    }

    private void verifyNativeChannels(EntityPlayerMP player, ItemStack terminal) throws Exception {
        ItemStack edited = terminal.copy();
        ChannelDataAccessor.setChannelData(edited, "qa_packet_channel", 456);
        ByteBuf bytes = Unpooled.buffer();
        try {
            new SetChannelDataMessage(edited).toBytes(bytes);
            SetChannelDataMessage inbound = new SetChannelDataMessage();
            inbound.fromBytes(bytes);
            Constructor<?> constructor = MessageContext.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            MessageContext context = (MessageContext) constructor.newInstance(player.playerNetServerHandler,
                Side.SERVER);
            new SetChannelDataMessage.Handler().onMessage(inbound, context);
            require(ChannelDataAccessor.getChannelData(terminal, "qa_packet_channel") == 456,
                "native projector channel packet accepts the terminal on the server");
            require(TerminalSettings.count(terminal) == 1,
                "native channel upload does not overwrite terminal mode or master channel");
        } finally {
            bytes.release();
        }
    }

    private void verifyBuildAndDismantle(WorldServer world, EntityPlayerMP player, ItemStack terminal, boolean mirror) {
        int x = mirror ? 160 : 128, y = 80, z = 128;
        prepare(world, x, y, z);
        player.setPosition(x + 0.5, y, z - 2.5);
        clearSupplies(player);
        player.inventory.mainInventory[1] = new ItemStack(Blocks.cobblestone, 3);
        player.inventory.mainInventory[2] = new ItemStack(Blocks.wool, 3, 0);
        player.inventory.mainInventory[3] = new ItemStack(Blocks.planks, 3, 0);
        TerminalSettings.setMirror(terminal, mirror);
        TerminalSettings.setDismantle(terminal, false);
        require(operate(terminal, player, world, x, y, z), "build handles fixture " + mirror);
        FixtureController controller = (FixtureController) world.getTileEntity(x, y, z);
        require(controller.constructCalls > 0, "real StructureLib buildOrHints path was invoked " + mirror);
        require(block(world, x, y, z, 1, mirror) == Blocks.cobblestone, "build cobblestone position " + mirror);
        require(block(world, x, y, z, 2, mirror) == Blocks.wool, "build wool position " + mirror);
        require(block(world, x, y, z, 4, mirror) == Blocks.planks, "build plank position " + mirror);
        require(
            count(player, Blocks.cobblestone, 0) == 2 && count(player, Blocks.wool, 0) == 2
                && count(player, Blocks.planks, 0) == 2,
            "survival build consumes exactly one of each material " + mirror);
        require(block(world, x, y, z, 3, mirror) == Blocks.bedrock,
            "air requirement does not erase unrelated solid block " + mirror);
        require(block(world, x, y, z, 5, mirror) == Blocks.gold_block, "any position preserved during build " + mirror);
        TerminalSettings.setDismantle(terminal, true);
        require(operate(terminal, player, world, x, y, z), "dismantle handles fixture " + mirror);
        require(
            block(world, x, y, z, 1, mirror) == Blocks.air && block(world, x, y, z, 2, mirror) == Blocks.air
                && block(world, x, y, z, 4, mirror) == Blocks.air,
            "dismantle removes matching structure blocks " + mirror);
        require(count(player, Blocks.cobblestone, 0) == 3 && count(player, Blocks.wool, 0) == 3
            && count(player, Blocks.planks, 0) == 3, "dismantle returns exact original materials " + mirror);
        require(world.getBlock(x, y, z) == controllerBlock && world.getTileEntity(x, y, z) == controller,
            "controller preserved during dismantle " + mirror);
        require(
            block(world, x, y, z, 3, mirror) == Blocks.bedrock && block(world, x, y, z, 5, mirror) == Blocks.gold_block,
            "air and any positions preserved " + mirror);
        cleanup(world, x, y, z);
    }

    private void verifyMissingMaterials(WorldServer world, EntityPlayerMP player, ItemStack terminal) {
        int x = 192, y = 80, z = 128;
        prepare(world, x, y, z);
        player.setPosition(x + 0.5, y, z - 2.5);
        clearSupplies(player);
        TerminalSettings.setMirror(terminal, false);
        TerminalSettings.setDismantle(terminal, false);
        operate(terminal, player, world, x, y, z);
        require(block(world, x, y, z, 1, false) == Blocks.air && block(world, x, y, z, 2, false) == Blocks.air
            && block(world, x, y, z, 4, false) == Blocks.air, "missing materials never generate structure blocks");
        require(count(player, Blocks.cobblestone, 0) == 0 && count(player, Blocks.wool, 0) == 0,
            "missing materials do not create inventory items");
        cleanup(world, x, y, z);
    }

    private void verifyBreakProtection(WorldServer world, EntityPlayerMP player, ItemStack terminal) {
        int x = 224, y = 80, z = 128;
        prepare(world, x, y, z);
        player.setPosition(x + 0.5, y, z - 2.5);
        clearSupplies(player);
        setBlock(world, x, y, z, 1, false, Blocks.cobblestone);
        TerminalSettings.setMirror(terminal, false);
        TerminalSettings.setDismantle(terminal, true);
        int[] protectedPosition = position(x, y, z, 1, false);
        BreakGuard guard = new BreakGuard(protectedPosition);
        MinecraftForge.EVENT_BUS.register(guard);
        try {
            operate(terminal, player, world, x, y, z);
            require(guard.observed, "normal Forge BreakEvent is emitted");
            require(block(world, x, y, z, 1, false) == Blocks.cobblestone,
                "cancelled BreakEvent preserves structure block");
            require(count(player, Blocks.cobblestone, 0) == 0, "cancelled dismantle produces no duplicate refund");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(guard);
            cleanup(world, x, y, z);
        }
    }

    private void verifyReplacement(WorldServer world, EntityPlayerMP player, ItemStack terminal) {
        int x = 256, y = 80, z = 128;
        prepare(world, x, y, z);
        FixtureController controller = (FixtureController) world.getTileEntity(x, y, z);
        controller.tiered = true;
        player.setPosition(x + 0.5, y, z - 2.5);
        clearSupplies(player);
        player.inventory.mainInventory[1] = new ItemStack(Blocks.wool, 2, 0);
        player.inventory.mainInventory[2] = new ItemStack(Blocks.wool, 2, 1);
        TerminalSettings.setDismantle(terminal, false);
        TerminalSettings.setReplace(terminal, false);
        ChannelDataAccessor.setChannelData(terminal, "qa_tier", 1);
        operate(terminal, player, world, x, y, z);
        require(block(world, x, y, z, 1, false) == Blocks.wool && metadata(world, x, y, z, 1) == 0,
            "tier channel builds selected lower tier");
        ChannelDataAccessor.setChannelData(terminal, "qa_tier", 2);
        TerminalSettings.setReplace(terminal, true);
        operate(terminal, player, world, x, y, z);
        require(metadata(world, x, y, z, 1) == 1, "replacement upgrades the selected tier block");
        require(count(player, Blocks.wool, 0) == 2 && count(player, Blocks.wool, 1) == 1,
            "replacement consumes one new tier and returns exactly one old tier");
        int[] target = position(x, y, z, 1, false);
        world.setBlock(target[0], target[1], target[2], Blocks.wool, 0, 3);
        player.inventory.mainInventory[1] = null;
        player.inventory.mainInventory[2] = new ItemStack(Blocks.wool, 2, 1);
        PlaceGuard guard = new PlaceGuard(target);
        MinecraftForge.EVENT_BUS.register(guard);
        try {
            operate(terminal, player, world, x, y, z);
            require(guard.observed, "replacement emits normal Forge PlaceEvent");
            require(metadata(world, x, y, z, 1) == 0, "cancelled replacement restores old tier block");
            require(count(player, Blocks.wool, 1) == 2 && count(player, Blocks.wool, 0) == 0,
                "cancelled replacement refunds new tier once without duplicating old block");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(guard);
        }
        controller.failPlacement = true;
        try {
            try {
                operate(terminal, player, world, x, y, z);
            } catch (IllegalStateException expected) {
                require("terminal QA placement failure".equals(expected.getMessage()),
                    "only the intentional placement exception is intercepted");
            }
            require(controller.placementFailed, "replacement actually exercised a failing placement implementation");
            require(metadata(world, x, y, z, 1) == 0, "exceptional replacement restores the old tier block");
            require(count(player, Blocks.wool, 1) == 2 && count(player, Blocks.wool, 0) == 0,
                "exceptional replacement returns reserved material exactly once");
        } finally {
            controller.failPlacement = false;
            TerminalSettings.setReplace(terminal, false);
            cleanup(world, x, y, z);
        }
    }

    private void verifyNativeHatches(WorldServer world, EntityPlayerMP player, ItemStack terminal) {
        int x = 288, y = 80, z = 128;
        prepare(world, x, y, z);
        FixtureController controller = (FixtureController) world.getTileEntity(x, y, z);
        controller.hatches = true;
        player.setPosition(x + 0.5, y, z - 2.5);
        clearSupplies(player);
        TerminalSettings.setDismantle(terminal, true);
        TerminalSettings.setNoHatches(terminal, true);
        int[] target = position(x, y, z, 1, false);
        ItemStack busItem = ItemList.Hatch_Input_Bus_LV.get(1);
        BaseMetaTileEntity busTile = machine(world, player, target, busItem);
        MTEHatchInputBus bus = (MTEHatchInputBus) busTile.getMetaTileEntity();
        ItemStack payload = new ItemStack(Items.diamond, 4);
        payload.setTagCompound(new NBTTagCompound());
        payload.getTagCompound().setString("TerminalQA", "inventory-payload");
        bus.setInventorySlotContents(0, payload.copy());
        operate(terminal, player, world, x, y, z);
        require(world.getTileEntity(target[0], target[1], target[2]) == busTile,
            "keep-hatches mode preserves a real GT input bus");
        require(ItemStack.areItemStacksEqual(bus.getStackInSlot(0), payload),
            "keep-hatches mode preserves inventory and item NBT");
        TerminalSettings.setNoHatches(terminal, false);
        operate(terminal, player, world, x, y, z);
        require(world.isAirBlock(target[0], target[1], target[2]), "dismantle removes real GT input bus");
        require(countItem(player, busItem, false) == 1, "GT input bus item is returned exactly once");
        require(countItem(player, payload, true) == 4, "GT inventory content returns with exact quantity and NBT");
        ItemList[] fluidHatches = { ItemList.Hatch_Input_LV, ItemList.Hatch_Output_LV };
        for (ItemList hatch : fluidHatches) {
            clearSupplies(player);
            ItemStack hatchItem = hatch.get(1);
            BaseMetaTileEntity fluidTile = machine(world, player, target, hatchItem);
            FluidStack fluid = new FluidStack(FluidRegistry.WATER, 2000);
            fluid.tag = new NBTTagCompound();
            fluid.tag.setString("TerminalQA", hatch.name());
            ((MTEBasicTank) fluidTile.getMetaTileEntity()).mFluid = fluid.copy();
            operate(terminal, player, world, x, y, z);
            require(world.isAirBlock(target[0], target[1], target[2]), "dismantle removes real " + hatch);
            require(countItem(player, hatchItem, false) == 1, "fluid hatch item returns exactly once " + hatch);
            require(countFluid(world, player, fluid) == 2000, "native fluid content returns with exact NBT " + hatch);
        }
        TerminalSettings.setNoHatches(terminal, true);
        cleanup(world, x, y, z);
    }

    private static BaseMetaTileEntity machine(World world, EntityPlayerMP player, int[] target, ItemStack stack) {
        world.setBlock(target[0], target[1], target[2], GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(target[0], target[1], target[2]);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerUuid(player.getUniqueID());
        return tile;
    }

    private static int countItem(EntityPlayerMP player, ItemStack expected, boolean matchNbt) {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory) if (
            stack != null && stack.isItemEqual(expected)
                && (!matchNbt || ItemStack.areItemStackTagsEqual(stack, expected))
        ) count += stack.stackSize;
        return count;
    }

    private static long countFluid(World world, EntityPlayerMP player, FluidStack expected) {
        long amount = 0;
        for (ItemStack stack : player.inventory.mainInventory) amount += fluidAmount(stack, expected);
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(player.posX - 4, player.posY - 4, player.posZ - 4,
            player.posX + 4, player.posY + 4, player.posZ + 4);
        for (Object entry : world.getEntitiesWithinAABB(EntityItem.class, area)) {
            EntityItem item = (EntityItem) entry;
            if (!item.isDead) amount += fluidAmount(item.getEntityItem(), expected);
        }
        return amount;
    }

    private static long fluidAmount(ItemStack stack, FluidStack expected) {
        if (stack == null) return 0;
        IAEFluidStack requested = AEFluidStack.create(expected);
        var packet = ItemMEStackPacket.toAEStack(stack);
        if (packet instanceof IAEFluidStack fluid) return fluid.isSameType(requested) ? fluid.getStackSize() : 0;
        ItemStack single = stack.copy();
        single.stackSize = 1;
        if (!requested.getStackType().isContainerItemForType(single)) return 0;
        IAEFluidStack actual = requested.getStackType().getStackFromContainerItem(single);
        return actual != null && actual.isSameType(requested) ? actual.getStackSize() * stack.stackSize : 0;
    }

    private static int metadata(World world, int x, int y, int z, int a) {
        int[] position = position(x, y, z, a, false);
        return world.getBlockMetadata(position[0], position[1], position[2]);
    }

    @SuppressWarnings("unchecked")
    private static boolean operate(ItemStack terminal, EntityPlayerMP player, World world, int x, int y, int z) {
        try {
            Field field = TerminalOperations.class.getDeclaredField("lastUse");
            field.setAccessible(true);
            ((Map<EntityPlayerMP, Long>) field.get(null)).remove(player);
            return TerminalOperations.execute(terminal, player, world, x, y, z, 2);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("cannot reset this QA player's operation interval", failure);
        }
    }

    private void verifyNativeBlastFurnace(WorldServer world, EntityPlayerMP player, ItemStack terminal) {
        int x = 432, y = 80, z = 128;
        clearBlastFurnace(world, x, y, z);
        player.setPosition(x + 0.5, y, z - 2.5);
        clearSupplies(player);
        TerminalSettings.setDismantle(terminal, false);
        TerminalSettings.setMirror(terminal, false);
        TerminalSettings.setReplace(terminal, false);
        TerminalSettings.setNoHatches(terminal, false);
        TerminalSettings.setCount(terminal, 1);
        ChannelDataAccessor.setChannelData(terminal, "coil", 2);
        ItemStack controllerItem = ItemList.Machine_Multi_BlastFurnace.get(1);
        BaseMetaTileEntity base = machine(world, player, new int[] { x, y, z }, controllerItem);
        base.setFrontFacing(ForgeDirection.NORTH);
        MTEElectricBlastFurnace furnace = (MTEElectricBlastFurnace) base.getMetaTileEntity();
        ItemStack casing = ItemList.Casing_HeatProof.get(1);
        ItemStack kanthal = ItemList.Casing_Coil_Kanthal.get(1);
        ItemStack cupronickel = ItemList.Casing_Coil_Cupronickel.get(1);
        player.inventory.mainInventory[1] = ItemList.Casing_HeatProof.get(64);
        player.inventory.mainInventory[2] = ItemList.Casing_Coil_Kanthal.get(32);
        player.inventory.mainInventory[3] = ItemList.Casing_Coil_Cupronickel.get(32);
        ItemList[] hatches = { ItemList.Hatch_Input_LV, ItemList.Hatch_Output_LV, ItemList.Hatch_Input_Bus_LV,
            ItemList.Hatch_Output_Bus_LV, ItemList.Hatch_Energy_LV, ItemList.Hatch_Maintenance,
            ItemList.Hatch_Muffler_LV };
        for (int index = 0; index < hatches.length; index++)
            player.inventory.mainInventory[4 + index] = hatches[index].get(1);
        require(operate(terminal, player, world, x, y, z), "terminal handles real native EBF controller");
        furnace.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        furnace.checkMachine(base, null, errors);
        require(errors.isEmpty(), "native EBF physical checkMachine passes without forced formed state: " + errors);
        require(furnace.checkStructure(true, base), "real EBF checkStructure confirms complete physical machine");
        require(furnace.getCoilLevel() == HeatingCoilLevel.MV,
            "native coil subchannel selects Kanthal independently of master channel one");
        require(countItem(player, kanthal, false) == 16 && countItem(player, cupronickel, false) == 32,
            "real EBF consumes exactly sixteen selected coils and no unselected coils");
        require(countItem(player, casing, false) == 54, "real EBF consumes exactly ten heat-proof casings");
        require(furnace.mInputHatches.size() == 1 && furnace.mOutputHatches.size() == 1
            && furnace.mInputBusses.size() == 1
            && furnace.mOutputBusses.size() == 1
            && furnace.mMaintenanceHatches.size() == 1
            && furnace.mEnergyHatches.size() == 1
            && furnace.mMufflerHatches.size() == 1, "real EBF registers exactly the seven required hatch roles");
        for (ItemList hatch : hatches)
            require(countItem(player, hatch.get(1), false) == 0, "real EBF consumes one supplied " + hatch);
        TerminalSettings.setDismantle(terminal, true);
        TerminalSettings.setNoHatches(terminal, true);
        operate(terminal, player, world, x, y, z);
        require(world.getTileEntity(x, y, z) == base, "native EBF controller remains after casing teardown");
        require(countItem(player, kanthal, false) == 32 && countItem(player, casing, false) == 64,
            "real EBF casing teardown returns exact coils and casings");
        for (ItemList hatch : hatches)
            require(countItem(player, hatch.get(1), false) == 0, "keep-hatches preserves installed " + hatch);
        TerminalSettings.setNoHatches(terminal, false);
        operate(terminal, player, world, x, y, z);
        for (ItemList hatch : hatches)
            require(countItem(player, hatch.get(1), false) == 1, "real EBF hatch teardown returns one " + hatch);
        require(world.getTileEntity(x, y, z) == base, "native EBF controller remains after full teardown");
        TerminalSettings.setDismantle(terminal, false);
        operate(terminal, player, world, x, y, z);
        require(furnace.checkStructure(true, base), "recovered materials rebuild the real EBF into formed state");
        require(
            furnace.mInputHatches.size() == 1 && furnace.mOutputHatches.size() == 1
                && furnace.mInputBusses.size() == 1
                && furnace.mOutputBusses.size() == 1
                && furnace.mMaintenanceHatches.size() == 1
                && furnace.mEnergyHatches.size() == 1
                && furnace.mMufflerHatches.size() == 1,
            "formed EBF has populated native hatch counters before one-pass dismantle");
        TerminalSettings.setDismantle(terminal, true);
        operate(terminal, player, world, x, y, z);
        require(
            countItem(player, kanthal, false) == 32 && countItem(player, cupronickel, false) == 32
                && countItem(player, casing, false) == 64,
            "one-pass formed EBF teardown returns exact selected coils and casings");
        for (ItemList hatch : hatches) require(countItem(player, hatch.get(1), false) == 1,
            "one-pass formed EBF teardown returns each hatch exactly once " + hatch);
        boolean empty = true;
        for (int dx = -2; dx <= 2; dx++) for (int dy = 0; dy <= 4; dy++) for (int dz = -2; dz <= 3; dz++)
            if (dx != 0 || dy != 0 || dz != 0) empty &= world.isAirBlock(x + dx, y + dy, z + dz);
        require(empty && world.getTileEntity(x, y, z) == base,
            "one operation fully dismantles a formed native EBF while retaining its controller");
        clearBlastFurnace(world, x, y, z);
        TerminalSettings.setNoHatches(terminal, true);
    }

    @SuppressWarnings("unchecked")
    private void verifyNativeProjector(WorldServer world, EntityPlayerMP player, ItemStack terminal) throws Exception {
        int x = 464, y = 80, z = 128;
        prepare(world, x, y, z);
        world.setBlockToAir(x, y, z);
        world.setBlock(x, y, z, controllerBlock, 1, 3);
        NativeFixtureController controller = (NativeFixtureController) world.getTileEntity(x, y, z);
        player.setPosition(x + 0.5, y, z - 2.5);
        clearSupplies(player);
        TerminalSettings.setDismantle(terminal, false);
        TerminalSettings.setFullBuild(terminal, false);
        TerminalSettings.setCount(terminal, 7);
        TerminalSettings.setNoHatches(terminal, true);
        ChannelDataAccessor.setChannelData(terminal, GTStructureChannels.HATCH.get(), 4);
        player.setSneaking(true);
        try {
            player.setGameType(WorldSettings.GameType.SURVIVAL);
            Field nativeInterval = ConstructableUtility.class.getDeclaredField("lastUse");
            nativeInterval.setAccessible(true);
            ((Map<EntityPlayerMP, Long>) nativeInterval.get(null)).remove(player);
            operate(terminal, player, world, x, y, z);
            require(
                controller.nativeSurvivalCalls == 1
                    && controller.nativeBudget == ConfigurationHandler.INSTANCE.getAutoPlaceBudget(),
                "native building delegates custom survival algorithm with the original placement budget");
            require(controller.nativeMasterCount == 7 && controller.nativeHatchChannel == 4,
                "native building passes master and hatch channels unchanged despite advanced terminal settings");
            require(block(world, x, y, z, 1, false) == Blocks.air,
                "native custom survival implementation retains control over actual placement");
            player.setGameType(WorldSettings.GameType.CREATIVE);
            ((Map<EntityPlayerMP, Long>) nativeInterval.get(null)).remove(player);
            operate(terminal, player, world, x, y, z);
            require(
                block(world, x, y, z, 1, false) == Blocks.cobblestone && block(world, x, y, z, 2, false) == Blocks.wool
                    && block(world, x, y, z, 4, false) == Blocks.planks,
                "native creative building places the original layout without survival materials");
            require(
                block(world, x, y, z, 3, false) == Blocks.air && block(world, x, y, z, 5, false) == Blocks.gold_block,
                "native creative rebuilding clears explicit air while preserving unconstrained positions");
        } finally {
            player.setSneaking(false);
            player.setGameType(WorldSettings.GameType.SURVIVAL);
            TerminalSettings.setFullBuild(terminal, true);
            TerminalSettings.setCount(terminal, 1);
            ChannelDataAccessor.unsetChannelData(terminal, GTStructureChannels.HATCH.get());
            cleanup(world, x, y, z);
        }
    }

    private void verifyCreativeBlastFurnace(WorldServer world, EntityPlayerMP player, ItemStack terminal) {
        int x = 496, y = 80, z = 128;
        NBTTagCompound previousSettings = terminal.hasTagCompound() ? (NBTTagCompound) terminal.getTagCompound().copy()
            : null;
        WorldSettings.GameType previousMode = player.theItemInWorldManager.getGameType();
        clearBlastFurnace(world, x, y, z);
        clearSupplies(player);
        player.setPosition(x + 0.5, y, z - 2.5);
        player.setGameType(WorldSettings.GameType.CREATIVE);
        TerminalSettings.setFullBuild(terminal, true);
        TerminalSettings.setDismantle(terminal, false);
        TerminalSettings.setNoHatches(terminal, false);
        TerminalSettings.setMirror(terminal, false);
        TerminalSettings.setReplace(terminal, false);
        TerminalSettings.setAe(terminal, false);
        TerminalSettings.setCount(terminal, 1);
        ChannelDataAccessor.setChannelData(terminal, "coil", 2);
        try {
            BaseMetaTileEntity base = machine(world, player, new int[] { x, y, z },
                ItemList.Machine_Multi_BlastFurnace.get(1));
            base.setFrontFacing(ForgeDirection.NORTH);
            MTEElectricBlastFurnace furnace = (MTEElectricBlastFurnace) base.getMetaTileEntity();
            require(onlyTerminal(player, terminal), "creative whole-structure fixture starts without any materials");
            require(operate(terminal, player, world, x, y, z), "creative whole-structure terminal handles native EBF");
            furnace.clearHatches();
            List<StructureError> errors = new ArrayList<>();
            furnace.checkMachine(base, null, errors);
            require(errors.isEmpty(), "creative whole-structure EBF passes real physical checkMachine: " + errors);
            require(furnace.checkStructure(true, base), "creative empty-inventory whole EBF is physically formed");
            require(furnace.getCoilLevel() == HeatingCoilLevel.MV,
                "creative whole-structure EBF honors independent Kanthal coil channel");
            require(
                furnace.mInputHatches.size() == 1 && furnace.mOutputHatches.size() == 1
                    && furnace.mInputBusses.size() == 1
                    && furnace.mOutputBusses.size() == 1
                    && furnace.mMaintenanceHatches.size() == 1
                    && furnace.mEnergyHatches.size() == 1
                    && furnace.mMufflerHatches.size() == 1,
                "creative whole-structure EBF provides exactly seven required hatch roles");
            int outward = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 3; dy++) for (int dz = 0; dz <= 2; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                if (!(world.getTileEntity(x + dx, y + dy, z + dz) instanceof BaseMetaTileEntity hatch)) continue;
                ForgeDirection facing = hatch.getFrontFacing();
                int nx = dx + facing.offsetX, ny = dy + facing.offsetY, nz = dz + facing.offsetZ;
                require(nx < -1 || nx > 1 || ny < 0 || ny > 3 || nz < 0 || nz > 2,
                    "creative native hatch faces outside the actual EBF structure");
                outward++;
            }
            require(outward == 7, "all seven creative native EBF hatches retain V2 outward orientation");
            require(onlyTerminal(player, terminal),
                "creative whole-structure building does not grant virtual materials");
            verifyCreativePlacementProtection(world, player, terminal);
        } finally {
            clearBlastFurnace(world, x, y, z);
            player.setGameType(previousMode);
            terminal.setTagCompound(previousSettings);
        }
    }

    private void verifyCreativePlacementProtection(WorldServer world, EntityPlayerMP player, ItemStack terminal) {
        int x = 528, y = 80, z = 128;
        prepare(world, x, y, z);
        player.setPosition(x + 0.5, y, z - 2.5);
        PlaceGuard guard = new PlaceGuard(position(x, y, z, 1, false));
        MinecraftForge.EVENT_BUS.register(guard);
        try {
            operate(terminal, player, world, x, y, z);
            require(guard.observed && block(world, x, y, z, 1, false) == Blocks.air,
                "creative placement respects cancelled PlaceEvent without a raw placement fallback");
            require(onlyTerminal(player, terminal), "cancelled creative placement does not refund virtual materials");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(guard);
            cleanup(world, x, y, z);
        }
    }

    private static boolean onlyTerminal(EntityPlayerMP player, ItemStack terminal) {
        if (player.getHeldItem() != terminal || terminal.stackSize != 1) return false;
        for (int slot = 1; slot < player.inventory.mainInventory.length; slot++)
            if (player.inventory.mainInventory[slot] != null) return false;
        return true;
    }

    private static void clearBlastFurnace(World world, int x, int y, int z) {
        for (int dx = -3; dx <= 3; dx++) for (int dy = -1; dy <= 5; dy++)
            for (int dz = -1; dz <= 4; dz++) world.setBlockToAir(x + dx, y + dy, z + dz);
        clearFixtureDrops(world, x - 4, y - 2, z - 4, x + 4, y + 6, z + 5);
    }

    private static void clearSupplies(EntityPlayerMP player) {
        for (int i = 1; i < player.inventory.mainInventory.length; i++) player.inventory.mainInventory[i] = null;
    }

    private static int count(EntityPlayerMP player, Block block, int meta) {
        int count = 0;
        Item item = Item.getItemFromBlock(block);
        for (ItemStack stack : player.inventory.mainInventory)
            if (stack != null && stack.getItem() == item && stack.getItemDamage() == meta) count += stack.stackSize;
        return count;
    }

    private static void prepare(World world, int x, int y, int z) {
        cleanup(world, x, y, z);
        world.setBlock(x, y, z, controllerBlock, 0, 3);
        for (boolean mirror : new boolean[] { false, true }) {
            setBlock(world, x, y, z, 3, mirror, Blocks.bedrock);
            setBlock(world, x, y, z, 5, mirror, Blocks.gold_block);
        }
    }

    private static void cleanup(World world, int x, int y, int z) {
        for (int dx = -7; dx <= 7; dx++) for (int dy = -2; dy <= 4; dy++)
            for (int dz = -3; dz <= 3; dz++) world.setBlockToAir(x + dx, y + dy, z + dz);
        clearFixtureDrops(world, x - 9, y - 4, z - 9, x + 9, y + 6, z + 9);
    }

    private static void resetFixtures(World world) {
        for (int x : new int[] { 128, 160, 192, 224, 256, 288, 320, 352, 384, 464, 528 }) cleanup(world, x, 80, 128);
        clearBlastFurnace(world, 432, 80, 128);
        clearBlastFurnace(world, 496, 80, 128);
    }

    private static void clearFixtureDrops(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (Object entry : world.getEntitiesWithinAABB(EntityItem.class,
            AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ))) ((EntityItem) entry).setDead();
    }

    private static int[] position(int x, int y, int z, int a, boolean mirror) {
        int[] offset = new int[3];
        ExtendedFacing.DEFAULT.getWorldOffset(new int[] { mirror ? -a : a, 0, 0 }, offset);
        return new int[] { x + offset[0], y + offset[1], z + offset[2] };
    }

    private static Block block(World world, int x, int y, int z, int a, boolean mirror) {
        int[] position = position(x, y, z, a, mirror);
        return world.getBlock(position[0], position[1], position[2]);
    }

    private static void setBlock(World world, int x, int y, int z, int a, boolean mirror, Block block) {
        int[] position = position(x, y, z, a, mirror);
        world.setBlock(position[0], position[1], position[2], block, 0, 3);
    }

    private static void writeResult(String value) throws Exception {
        Files.write(Path.of(System.getProperty("gtng.terminal.qa.output"), "result.txt"),
            value.getBytes(StandardCharsets.UTF_8));
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    public static class FixtureController extends TileEntity implements IConstructable {

        private static final IStructureDefinition<FixtureController> STRUCTURE = IStructureDefinition
            .<FixtureController>builder().addShape("main", new String[][] { { "~AB-CD" } })
            .addElement('A', ofBlock(Blocks.cobblestone, 0)).addElement('B', ofBlock(Blocks.wool, 0))
            .addElement('C', ofBlock(Blocks.planks, 0))
            .addElement('D', (IStructureElementCheckOnly<FixtureController>) (tile, world, x, y, z) -> true).build();
        private static final IStructureDefinition<FixtureController> TIERS = IStructureDefinition
            .<FixtureController>builder().addShape("main", new String[][] { { "~T" } })
            .addElement('T', new TierElement()).build();
        private static final IStructureDefinition<FixtureController> HATCHES = IStructureDefinition
            .<FixtureController>builder().addShape("main", new String[][] { { "~H" } })
            .addElement('H', new HatchElement()).build();
        private static final IStructureDefinition<FixtureController> WATER = IStructureDefinition
            .<FixtureController>builder().addShape("main", new String[][] { { "~F" } })
            .addElement('F', ofBlock(Blocks.water, 0)).build();
        private static final IStructureDefinition<FixtureController> LAVA = IStructureDefinition
            .<FixtureController>builder().addShape("main", new String[][] { { "~F" } })
            .addElement('F', ofBlock(Blocks.lava, 0)).build();
        private boolean tiered;
        private boolean hatches;
        private int liquid;
        private boolean failPlacement;
        private boolean placementFailed;
        private int constructCalls;
        private int lastTriggerCount;
        private int lastChannel;

        @Override
        public void construct(ItemStack trigger, boolean hintsOnly) {
            constructCalls++;
            lastTriggerCount = trigger.stackSize;
            lastChannel = ChannelDataAccessor.getChannelData(trigger, "qa_server_channel");
            structure().buildOrHints(this, trigger, "main", worldObj, ExtendedFacing.DEFAULT, xCoord, yCoord, zCoord, 0,
                0, 0, hintsOnly);
        }

        @Override
        public IStructureDefinition<?> getStructureDefinition() {
            return structure();
        }

        @Override
        public String[] getStructureDescription(ItemStack trigger) {
            return new String[] { "Terminal QA controller" };
        }

        public int constructCalls() {
            return constructCalls;
        }

        public int lastTriggerCount() {
            return lastTriggerCount;
        }

        public int lastChannel() {
            return lastChannel;
        }

        private IStructureDefinition<FixtureController> structure() {
            return liquid == 1 ? WATER : liquid == 2 ? LAVA : hatches ? HATCHES : tiered ? TIERS : STRUCTURE;
        }
    }

    public static final class NativeFixtureController extends FixtureController implements ISurvivalConstructable {

        private int nativeBudget, nativeSurvivalCalls, nativeMasterCount, nativeHatchChannel;

        @Override
        public int survivalConstruct(ItemStack trigger, int budget, ISurvivalBuildEnvironment environment) {
            nativeBudget = budget;
            nativeSurvivalCalls++;
            nativeMasterCount = trigger.stackSize;
            nativeHatchChannel = ChannelDataAccessor.getChannelData(trigger, GTStructureChannels.HATCH.get());
            return 0;
        }
    }

    private static final class TierElement implements IStructureElement<FixtureController> {

        @Override
        public boolean check(FixtureController tile, World world, int x, int y, int z) {
            return world.getBlock(x, y, z) == Blocks.wool && world.getBlockMetadata(x, y, z) <= 1;
        }

        @Override
        public boolean spawnHint(FixtureController tile, World world, int x, int y, int z, ItemStack trigger) {
            StructureLibAPI.hintParticle(world, x, y, z, Blocks.wool, meta(trigger));
            return true;
        }

        @Override
        public boolean placeBlock(FixtureController tile, World world, int x, int y, int z, ItemStack trigger) {
            return world.setBlock(x, y, z, Blocks.wool, meta(trigger), 3);
        }

        @Override
        public BlocksToPlace getBlocksToPlace(FixtureController tile, World world, int x, int y, int z,
            ItemStack trigger, AutoPlaceEnvironment environment) {
            return BlocksToPlace.create(Blocks.wool, meta(trigger));
        }

        @Override
        public PlaceResult survivalPlaceBlock(FixtureController tile, World world, int x, int y, int z,
            ItemStack trigger, AutoPlaceEnvironment environment) {
            PlaceResult result = IStructureElement.super.survivalPlaceBlock(tile, world, x, y, z, trigger, environment);
            if (tile.failPlacement) {
                tile.placementFailed = true;
                throw new IllegalStateException("terminal QA placement failure");
            }
            return result;
        }

        private static int meta(ItemStack trigger) {
            return Math.min(2, Math.max(1, ChannelDataAccessor.withChannel(trigger, "qa_tier").stackSize)) - 1;
        }
    }

    private static final class HatchElement implements IStructureElement<FixtureController> {

        @Override
        public boolean check(FixtureController tile, World world, int x, int y, int z) {
            return world.getTileEntity(x, y, z) instanceof BaseMetaTileEntity hatch
                && hatch.getMetaTileEntity() != null;
        }

        @Override
        public boolean spawnHint(FixtureController tile, World world, int x, int y, int z, ItemStack trigger) {
            return false;
        }

        @Override
        public boolean placeBlock(FixtureController tile, World world, int x, int y, int z, ItemStack trigger) {
            return false;
        }

        @Override
        public BlocksToPlace getBlocksToPlace(FixtureController tile, World world, int x, int y, int z,
            ItemStack trigger, AutoPlaceEnvironment environment) {
            return BlocksToPlace.create(ItemList.Hatch_Input_Bus_LV.get(1), ItemList.Hatch_Input_LV.get(1),
                ItemList.Hatch_Output_LV.get(1));
        }
    }

    public static final class PlaceGuard {

        private final int[] position;
        private boolean observed;

        private PlaceGuard(int[] position) {
            this.position = position;
        }

        @SubscribeEvent
        public void placing(BlockEvent.PlaceEvent event) {
            if (event.x == position[0] && event.y == position[1] && event.z == position[2]) {
                observed = true;
                event.setCanceled(true);
            }
        }
    }

    private static final class FixtureBlock extends BlockContainer {

        private FixtureBlock() {
            super(Material.iron);
            setBlockName("terminal_qa_controller");
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return meta == 1 ? new NativeFixtureController() : new FixtureController();
        }
    }

    public static final class BreakGuard {

        private final int[] position;
        private boolean observed;

        private BreakGuard(int[] position) {
            this.position = position;
        }

        @SubscribeEvent
        public void breaking(BlockEvent.BreakEvent event) {
            if (event.x == position[0] && event.y == position[1] && event.z == position[2]) {
                observed = true;
                event.setCanceled(true);
            }
        }
    }
}
