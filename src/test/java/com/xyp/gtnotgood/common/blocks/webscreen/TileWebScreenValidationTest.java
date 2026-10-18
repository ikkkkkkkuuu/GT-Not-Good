package com.xyp.gtnotgood.common.blocks.webscreen;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;

import org.junit.Test;

import sun.misc.Unsafe;

public class TileWebScreenValidationTest {

    @Test
    public void serverValidationCreatesStableIdentityWithoutAccessingTheUnregisteredTile() throws Exception {
        TileWebScreen tile = new TileWebScreen();
        tile.setWorldObj(world(false));

        tile.validate();
        UUID identity = tile.getScreenId();
        assertNotNull(identity);

        tile.invalidate();
        tile.validate();
        assertFalse(tile.isInvalid());
        assertSame(identity, tile.getScreenId());
    }

    @Test
    public void clientValidationWaitsForTheServerIdentity() throws Exception {
        TileWebScreen tile = new TileWebScreen();
        tile.setWorldObj(world(true));

        tile.validate();

        assertNull(tile.getScreenId());
    }

    private static World world(boolean remote) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        TestWorld world = (TestWorld) ((Unsafe) field.get(null)).allocateInstance(TestWorld.class);
        world.isRemote = remote;
        return world;
    }

    private static final class TestWorld extends World {

        private TestWorld() {
            super(null, "web-screen-validation-test", (WorldSettings) null, (WorldProvider) null, null);
            throw new UnsupportedOperationException();
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            throw new AssertionError("The tile is not registered during validate");
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            throw new AssertionError("Validation must not mark the tile dirty");
        }

        @Override
        public void markTileEntityChunkModified(int x, int y, int z, TileEntity tile) {
            throw new AssertionError("Validation must not modify the world");
        }

        @Override
        public void markBlockForUpdate(int x, int y, int z) {
            throw new AssertionError("Block notifications can re-enter tile creation");
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
