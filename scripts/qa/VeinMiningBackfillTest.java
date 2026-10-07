package com.xyp.gtnotgood.common.items.veinmining;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.oredict.OreDictionary;

import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.common.blocks.GTBlockOre;
import sun.misc.Unsafe;

/** Exercises deferred backfill through the Forge break and tick handlers without starting a server. */
public class VeinMiningBackfillTest {

    private static Unsafe unsafe;
    private static Block vanillaOre;
    private static Block redstoneOre;
    private static Block metadataOre;
    private static GTBlockOre gregTechOre;

    @BeforeClass
    public static void registerSamples() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        if (loader.get(null) == null) loader.set(null, unsafe.allocateInstance(Loader.class));
        setField(FMLRelaunchLog.class, null, "side", Side.SERVER);

        // OreDictionary also initializes vanilla crafting recipes, which need the native block/item registries.
        if (!Block.blockRegistry.containsKey("minecraft:stone")) Block.registerBlocks();
        if (!Item.itemRegistry.containsKey("minecraft:stick")) Item.registerItems();
        HeadlessBlockRegistry.bootstrap();

        vanillaOre = new BlockOre() {};
        redstoneOre = new BlockRedstoneOre(false) {};
        metadataOre = new Block(Material.rock) {

            @Override
            public int getDamageValue(World world, int x, int y, int z) {
                return world.getBlockMetadata(x, y, z) + 100;
            }
        };
        Block.blockRegistry.addObject(2000, "backfill_metadata_ore", metadataOre);
        Item.itemRegistry.addObject(2000, "backfill_metadata_ore", new ItemBlock(metadataOre));
        OreDictionary.registerOre("oreBackfillRegression", new ItemStack(metadataOre, 1, 107));

        gregTechOre = (GTBlockOre) unsafe.allocateInstance(GTBlockOre.class);
        setField(Block.class, gregTechOre, "blockMaterial", Material.rock);
    }

    @Test
    public void vanillaOresWaitForHarvestAndEndOfTick() throws Exception {
        for (Block ore : new Block[] { vanillaOre, redstoneOre }) {
            Fixture fixture = fixture();
            fixture.world.place(1, ore, 0);
            fixture.pickaxe.onBlockBreak(fixture.breakEvent(1));
            assertSame(ore, fixture.world.getBlock(1, 64, 0));
            assertEquals(0, fixture.world.setCalls);

            fixture.world.place(1, Blocks.air, 0);
            fixture.tick(TickEvent.Phase.START);
            assertSame(Blocks.air, fixture.world.getBlock(1, 64, 0));
            fixture.tick(TickEvent.Phase.END);
            assertSame(Blocks.stone, fixture.world.getBlock(1, 64, 0));
            assertEquals(0, fixture.world.getBlockMetadata(1, 64, 0));
            assertEquals(3, fixture.world.lastUpdateFlags);
            assertEquals(1, fixture.world.setCalls);
        }
    }

    @Test
    public void oreDictionaryUsesActualDamageValueAndRejectsOtherMetadata() throws Exception {
        Fixture fixture = fixture();
        fixture.world.place(1, metadataOre, 7);
        fixture.world.place(2, metadataOre, 8);
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(1));
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(2));
        fixture.world.place(1, Blocks.air, 0);
        fixture.world.place(2, Blocks.air, 0);
        fixture.tick(TickEvent.Phase.END);
        assertSame(Blocks.stone, fixture.world.getBlock(1, 64, 0));
        assertSame(Blocks.air, fixture.world.getBlock(2, 64, 0));
        assertEquals(1, fixture.world.setCalls);
    }

    @Test
    public void naturalAndSmallGregTechOresDoNotNeedOreDictionaryEntries() throws Exception {
        Fixture fixture = fixture();
        fixture.world.place(1, gregTechOre, GTBlockOre.NATURAL_ORE_META_OFFSET + 35);
        fixture.world.place(2, gregTechOre, GTBlockOre.SMALL_ORE_META_OFFSET + 35);
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(1));
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(2));
        fixture.world.place(1, Blocks.air, 0);
        fixture.world.place(2, Blocks.air, 0);
        fixture.tick(TickEvent.Phase.END);
        assertSame(Blocks.stone, fixture.world.getBlock(1, 64, 0));
        assertSame(Blocks.stone, fixture.world.getBlock(2, 64, 0));
        assertEquals(2, fixture.world.setCalls);
    }

    @Test
    public void chainedHarvestOnlyBackfillsOreAndLeavesOtherMinedBlocksAsAir() throws Exception {
        Fixture fixture = fixture();
        fixture.player.sneaking = true;
        activePlayers(fixture.pickaxe).add(fixture.player.id);
        fixture.world.place(1, vanillaOre, 0);
        fixture.world.place(2, Blocks.stone, 0);
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(1));
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(2));
        fixture.world.place(1, Blocks.air, 0);
        fixture.world.place(2, Blocks.air, 0);
        fixture.tick(TickEvent.Phase.END);
        assertSame(Blocks.stone, fixture.world.getBlock(1, 64, 0));
        assertSame(Blocks.air, fixture.world.getBlock(2, 64, 0));
        assertEquals(1, fixture.world.setCalls);
    }

    @Test
    public void cancelledBreakAndWrongToolCannotScheduleBackfill() throws Exception {
        Fixture fixture = fixture();
        fixture.world.place(1, vanillaOre, 0);
        BlockEvent.BreakEvent cancelled = fixture.breakEvent(1);
        cancelled.setCanceled(true);
        fixture.pickaxe.onBlockBreak(cancelled);
        fixture.player.held = new ItemStack(new Item());
        fixture.world.place(2, vanillaOre, 0);
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(2));
        fixture.world.place(1, Blocks.air, 0);
        fixture.world.place(2, Blocks.air, 0);
        fixture.tick(TickEvent.Phase.END);
        assertEquals(0, fixture.world.setCalls);
    }

    @Test
    public void failedHarvestAndReplacementBlocksRemainUntouched() throws Exception {
        Fixture fixture = fixture();
        fixture.world.place(1, vanillaOre, 0);
        fixture.world.place(2, vanillaOre, 0);
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(1));
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(2));
        fixture.world.place(2, metadataOre, 8);
        fixture.tick(TickEvent.Phase.END);
        assertSame(vanillaOre, fixture.world.getBlock(1, 64, 0));
        assertSame(metadataOre, fixture.world.getBlock(2, 64, 0));
        assertEquals(8, fixture.world.getBlockMetadata(2, 64, 0));
        assertEquals(0, fixture.world.setCalls);

        fixture.world.place(1, Blocks.air, 0);
        fixture.world.place(2, Blocks.air, 0);
        fixture.tick(TickEvent.Phase.END);
        assertEquals(0, fixture.world.setCalls);
    }

    @Test
    public void unloadedPositionsAreNeverInspectedOrRetried() throws Exception {
        Fixture fixture = fixture();
        fixture.world.place(1, vanillaOre, 0);
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(1));
        fixture.world.place(1, Blocks.air, 0);
        fixture.world.unloaded.add(new ChunkCoordinates(1, 64, 0));
        fixture.tick(TickEvent.Phase.END);
        assertEquals(0, fixture.world.airChecks);
        assertEquals(0, fixture.world.setCalls);
        fixture.world.unloaded.clear();
        fixture.tick(TickEvent.Phase.END);
        assertEquals(0, fixture.world.setCalls);
    }

    @Test
    public void unloadingOneWorldClearsOnlyItsPendingBackfill() throws Exception {
        Fixture fixture = fixture();
        fixture.world.place(1, vanillaOre, 0);
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(1));
        fixture.world.place(1, Blocks.air, 0);

        TestWorld secondWorld = world();
        secondWorld.place(2, vanillaOre, 0);
        fixture.pickaxe.onBlockBreak(new BlockEvent.BreakEvent(2, 64, 0, secondWorld, vanillaOre, 0, fixture.player));
        secondWorld.place(2, Blocks.air, 0);
        fixture.pickaxe.onWorldUnload(new WorldEvent.Unload(fixture.world));
        fixture.tick(TickEvent.Phase.END);
        assertSame(Blocks.air, fixture.world.getBlock(1, 64, 0));
        assertSame(Blocks.stone, secondWorld.getBlock(2, 64, 0));
        assertEquals(0, fixture.world.setCalls);
        assertEquals(1, secondWorld.setCalls);
    }

    @Test
    public void clientWorldCannotScheduleOrDiscardServerBackfill() throws Exception {
        Fixture fixture = fixture();
        fixture.world.place(1, vanillaOre, 0);
        fixture.pickaxe.onBlockBreak(fixture.breakEvent(1));
        fixture.world.place(1, Blocks.air, 0);
        TestWorld clientWorld = world();
        clientWorld.isRemote = true;
        clientWorld.place(2, vanillaOre, 0);
        fixture.pickaxe.onBlockBreak(new BlockEvent.BreakEvent(2, 64, 0, clientWorld, vanillaOre, 0, fixture.player));
        clientWorld.place(2, Blocks.air, 0);
        fixture.pickaxe.onWorldUnload(new WorldEvent.Unload(clientWorld));
        fixture.tick(TickEvent.Phase.END);
        assertSame(Blocks.stone, fixture.world.getBlock(1, 64, 0));
        assertEquals(0, clientWorld.setCalls);
    }

    private static Fixture fixture() throws Exception {
        VeinMiningPickaxe pickaxe = (VeinMiningPickaxe) unsafe.allocateInstance(VeinMiningPickaxe.class);
        setField(
            Item.class,
            pickaxe,
            "delegate",
            GameData.getItemRegistry()
                .getDelegate(pickaxe, Item.class));
        setField(VeinMiningPickaxe.class, pickaxe, "pendingBackfill", new HashMap<>());
        setField(VeinMiningPickaxe.class, pickaxe, "activePlayers", new HashSet<UUID>());
        TestPlayer player = (TestPlayer) unsafe.allocateInstance(TestPlayer.class);
        TestWorld world = world();
        player.worldObj = world;
        player.inventory = new InventoryPlayer(player);
        player.held = new ItemStack(pickaxe);
        player.id = UUID.randomUUID();
        return new Fixture(pickaxe, world, player);
    }

    private static TestWorld world() throws InstantiationException {
        TestWorld world = (TestWorld) unsafe.allocateInstance(TestWorld.class);
        world.blocks = new HashMap<>();
        world.metadata = new HashMap<>();
        world.unloaded = new HashSet<>();
        return world;
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static Set<UUID> activePlayers(VeinMiningPickaxe pickaxe) throws Exception {
        Field field = VeinMiningPickaxe.class.getDeclaredField("activePlayers");
        field.setAccessible(true);
        return (Set<UUID>) field.get(pickaxe);
    }

    private static final class Fixture {

        private final VeinMiningPickaxe pickaxe;
        private final TestWorld world;
        private final TestPlayer player;

        private Fixture(VeinMiningPickaxe pickaxe, TestWorld world, TestPlayer player) {
            this.pickaxe = pickaxe;
            this.world = world;
            this.player = player;
        }

        private BlockEvent.BreakEvent breakEvent(int x) {
            return new TestBreakEvent(
                x,
                64,
                0,
                world,
                world.getBlock(x, 64, 0),
                world.getBlockMetadata(x, 64, 0),
                player);
        }

        private void tick(TickEvent.Phase phase) {
            pickaxe.onServerTick(new TickEvent.ServerTickEvent(phase));
        }
    }

    /** Forge normally adds this override when transforming @Cancelable events. */
    private static final class TestBreakEvent extends BlockEvent.BreakEvent {

        private TestBreakEvent(int x, int y, int z, World world, Block block, int metadata, TestPlayer player) {
            super(x, y, z, world, block, metadata, player);
        }

        @Override
        public boolean isCancelable() {
            return true;
        }
    }

    private static final class TestPlayer extends EntityPlayerMP {

        private ItemStack held;
        private UUID id;
        private boolean sneaking;

        private TestPlayer() {
            super(null, null, null, null);
        }

        @Override
        public ItemStack getCurrentEquippedItem() {
            return held;
        }

        @Override
        public boolean canHarvestBlock(Block block) {
            return false;
        }

        @Override
        public boolean isSneaking() {
            return sneaking;
        }

        @Override
        public UUID getUniqueID() {
            return id;
        }
    }

    /** Stores explicit block states while counting the world reads and writes made by backfill. */
    private static final class TestWorld extends World {

        private Map<ChunkCoordinates, Block> blocks;
        private Map<ChunkCoordinates, Integer> metadata;
        private Set<ChunkCoordinates> unloaded;
        private int setCalls;
        private int airChecks;
        private int lastUpdateFlags;

        private TestWorld() {
            super(null, "backfill-test", (WorldSettings) null, null, null);
        }

        private void place(int x, Block block, int meta) {
            ChunkCoordinates position = new ChunkCoordinates(x, 64, 0);
            blocks.put(position, block);
            metadata.put(position, meta);
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            return blocks.getOrDefault(new ChunkCoordinates(x, y, z), Blocks.air);
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            return metadata.getOrDefault(new ChunkCoordinates(x, y, z), 0);
        }

        @Override
        public boolean blockExists(int x, int y, int z) {
            return !unloaded.contains(new ChunkCoordinates(x, y, z));
        }

        @Override
        public boolean isAirBlock(int x, int y, int z) {
            airChecks++;
            return getBlock(x, y, z) == Blocks.air;
        }

        @Override
        public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
            ChunkCoordinates position = new ChunkCoordinates(x, y, z);
            blocks.put(position, block);
            metadata.put(position, meta);
            setCalls++;
            lastUpdateFlags = flags;
            return true;
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
}
