package com.xyp.gtnotgood.common.blocks.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.xyp.gtnotgood.common.blocks.network.NetworkTopology.Endpoint;
import com.xyp.gtnotgood.common.blocks.network.TileNetworkController.Channel;

import sun.misc.Unsafe;

/** Exercises the production tick's shared budget; only world lookup and inventories are test doubles. */
public class NetworkTransferTest {

    private static final Item ITEM = new Item();

    @Test
    public void bufferedItemReachesLastSlotWithoutRescanningTheWholeDestination() throws Exception {
        Fixture fixture = new Fixture();
        Inventory destination = new Inventory(4096, 4095);
        fixture.add(destination, 2, 64);
        fixture.channel.item = new ItemStack(ITEM);

        assertTrue(fixture.tick());
        assertNull(fixture.channel.item);
        assertEquals(1, destination.count());
        assertEquals(4097, destination.insertChecks);
    }

    @Test
    public void laterProbesCannotStealCommitBudgetAndRotationReachesBothDestinations() throws Exception {
        Fixture fixture = new Fixture();
        Inventory first = new Inventory(4096, 4095);
        Inventory second = new Inventory(4096, 4095);
        fixture.add(first, 2, 1);
        fixture.add(second, 2, 1);
        fixture.channel.distribution = 1;
        fixture.channel.item = new ItemStack(ITEM, 2);

        assertTrue(fixture.tick());
        assertEquals(1, first.count());
        assertEquals(1, fixture.channel.item.stackSize);
        assertTrue(first.insertChecks + second.insertChecks <= 8192);

        first.insertChecks = second.insertChecks = 0;
        assertTrue(fixture.tick());
        assertNull(fixture.channel.item);
        assertEquals(1, second.count());
        assertEquals(2, first.count() + second.count());
        assertTrue(first.insertChecks + second.insertChecks <= 8192);
    }

    @Test
    public void lastSourceAndDestinationSlotsMakeProgressAcrossBudgetLimitedTicks() throws Exception {
        Fixture fixture = new Fixture();
        Inventory source = new Inventory(4096, 4095);
        source.contents[4095] = new ItemStack(ITEM);
        Inventory destination = new Inventory(4096, 4095);
        fixture.add(source, 1, 1);
        fixture.add(destination, 2, 1);

        for (int tick = 0; tick < 4 && destination.count() == 0; tick++) {
            source.reads = destination.insertChecks = 0;
            fixture.tick();
            assertTrue(source.reads + destination.insertChecks <= 8192);
            int buffered = fixture.channel.item == null ? 0 : fixture.channel.item.stackSize;
            assertEquals(1, source.count() + destination.count() + buffered);
        }
        assertEquals(1, destination.count());
        assertEquals(0, source.count());
        assertNull(fixture.channel.item);
    }

    @Test
    public void collectionStartsAtTheSelectedSlotAndWrapsWithinSourceRate() throws Exception {
        Fixture fixture = new Fixture();
        Inventory source = new Inventory(4096, 0);
        source.contents[0] = new ItemStack(ITEM, 10);
        source.contents[4095] = new ItemStack(ITEM, 10);
        Inventory destination = new Inventory(1, 0);
        NetworkRule input = fixture.add(source, 1, 15);
        input.extractionCursor = 4095;
        fixture.add(destination, 2, 64);

        assertTrue(fixture.tick());
        assertNull(source.contents[4095]);
        assertEquals(5, source.contents[0].stackSize);
        assertEquals(15, destination.count());
        assertNull(fixture.channel.item);
    }

    @Test
    public void commitRechecksAcceptanceAndKeepsRejectedCargo() throws Exception {
        Fixture fixture = new Fixture();
        Inventory destination = new Inventory(1, 0);
        destination.rejectAfterSimulation = true;
        fixture.add(destination, 2, 64);
        fixture.channel.item = new ItemStack(ITEM, 7);

        assertFalse(fixture.tick());
        assertEquals(7, fixture.channel.item.stackSize);
        assertEquals(0, destination.count());
    }

    @Test
    public void tickKeepsSequentialEvenAndPriorityAllocation() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            Fixture fixture = new Fixture();
            Inventory first = new Inventory(1, 0);
            Inventory second = new Inventory(1, 0);
            fixture.add(first, 2, 64);
            fixture.add(second, 2, 64).priority = 10;
            fixture.channel.distribution = mode;
            fixture.channel.item = new ItemStack(ITEM, 65);

            assertTrue(fixture.tick());
            assertEquals(mode == 0 ? 64 : mode == 1 ? 33 : 1, first.count());
            assertEquals(65, first.count() + second.count());
            assertNull(fixture.channel.item);
        }
    }

    private static final class Fixture {

        private final TestController controller = new TestController();
        private final Channel channel = controller.channels[0];
        private final TestWorld world;

        private Fixture() throws Exception {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            world = (TestWorld) ((Unsafe) field.get(null)).allocateInstance(TestWorld.class);
            world.tiles = new HashMap<>();
            controller.setWorldObj(world);
        }

        private NetworkRule add(Inventory inventory, int mode, int rate) throws Exception {
            TileNetworkNode connector = new TileNetworkNode();
            connector.xCoord = controller.network.endpoints.size() * 10;
            connector.setWorldObj(world);
            inventory.xCoord = connector.xCoord + 1;
            world.tiles.put(inventory.xCoord, inventory);
            Constructor<Endpoint> constructor = Endpoint.class.getDeclaredConstructor(TileNetworkNode.class,
                ForgeDirection.class);
            constructor.setAccessible(true);
            Endpoint endpoint = constructor.newInstance(connector, ForgeDirection.EAST);
            controller.network.endpoints.add(endpoint);
            NetworkRule rule = new NetworkRule();
            rule.mode = mode;
            rule.rate = rate;
            channel.rules.put(endpoint.key, rule);
            return rule;
        }

        private boolean tick() {
            return NetworkTransfer.tick(controller, channel);
        }
    }

    private static final class TestController extends TileNetworkController {

        private final NetworkTopology network = new NetworkTopology();

        @Override
        public NetworkTopology topology() {
            return network;
        }
    }

    /** Bypasses World construction, which otherwise boots registries, save files and chunk providers. */
    private static final class TestWorld extends World {

        private Map<Integer, TileEntity> tiles;

        private TestWorld() {
            super(null, "network-test", (WorldSettings) null, (WorldProvider) null, null);
            throw new UnsupportedOperationException();
        }

        @Override
        public long getTotalWorldTime() {
            return 0;
        }

        @Override
        public boolean blockExists(int x, int y, int z) {
            return true;
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            return tiles.get(x);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected int func_152379_p() {
            return 0;
        }

        @Override
        public Entity getEntityByID(int id) {
            return null;
        }
    }

    private static final class Inventory extends TileEntity implements IInventory {

        private final ItemStack[] contents;
        private final int writableSlot;
        private int reads;
        private int insertChecks;
        private boolean rejectAfterSimulation;

        private Inventory(int size, int writableSlot) {
            contents = new ItemStack[size];
            this.writableSlot = writableSlot;
        }

        private int count() {
            int result = 0;
            for (ItemStack stack : contents) if (stack != null) result += stack.stackSize;
            return result;
        }

        @Override
        public int getSizeInventory() {
            return contents.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            reads++;
            return contents[slot];
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            ItemStack result = contents[slot].splitStack(amount);
            if (contents[slot].stackSize == 0) contents[slot] = null;
            return result;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            return null;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            contents[slot] = stack;
        }

        @Override
        public String getInventoryName() {
            return "network-test";
        }

        @Override
        public boolean hasCustomInventoryName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 64;
        }

        @Override
        public void markDirty() {}

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return false;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            insertChecks++;
            return slot == writableSlot && (!rejectAfterSimulation || insertChecks == 1);
        }
    }
}
