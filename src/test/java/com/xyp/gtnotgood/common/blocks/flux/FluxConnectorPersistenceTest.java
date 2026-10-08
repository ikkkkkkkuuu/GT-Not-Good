package com.xyp.gtnotgood.common.blocks.flux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.BeforeClass;
import org.junit.Test;

import cpw.mods.fml.common.registry.GameRegistry;

/** Exercises real tile NBT paths, including portable settings and invalid save data. */
public class FluxConnectorPersistenceTest {

    @BeforeClass
    public static void registerTile() {
        GameRegistry.registerTileEntity(TileFluxPlug.class, "test_flux_plug");
    }

    @Test
    public void tileReloadPreservesEnergyButPortableSettingsDoNotCopyIt() {
        TileFluxPlug plug = new TileFluxPlug();
        plug.buffer.restore(987654321);
        NBTTagCompound saved = new NBTTagCompound();
        plug.writeToNBT(saved);
        TileFluxPlug reloaded = new TileFluxPlug();
        reloaded.readFromNBT(saved);
        assertEquals(987654321, reloaded.stored());
        NBTTagCompound item = new NBTTagCompound();
        reloaded.writeSettings(item);
        assertFalse(item.hasKey("fluxBuffer"));
        assertFalse(item.hasKey("fluxOwner"));
        assertFalse(item.hasKey("fluxChunkLoading"));
    }

    @Test
    public void corruptSettingsAreClampedWithoutIntegerWrap() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("fluxVoltage", Long.MAX_VALUE);
        tag.setLong("fluxAmperage", Long.MAX_VALUE);
        tag.setLong("fluxLimit", -123);
        tag.setInteger("fluxPriority", Integer.MIN_VALUE);
        tag.setString("fluxOwner", "invalid UUID");
        TileFluxPoint point = new TileFluxPoint();
        point.readFromNBT(tag);
        assertEquals(TileFluxConnector.MAX_VOLTAGE, point.voltage());
        assertEquals(TileFluxConnector.MAX_AMPERAGE, point.amperage());
        NBTTagCompound rewritten = new NBTTagCompound();
        point.writeSettings(rewritten);
        assertFalse(rewritten.hasKey("fluxLimit"));
        assertFalse(rewritten.hasKey("fluxDisableLimit"));
        assertEquals(-9999, point.priority());
        assertFalse(point.outputsEnergyTo(ForgeDirection.NORTH));
    }

    @Test
    public void defaultsAreLowVoltageAndChunkLoadingIsOptIn() {
        TileFluxPoint point = new TileFluxPoint();
        point.readFromNBT(new NBTTagCompound());
        assertEquals(32, point.voltage());
        assertEquals(1, point.amperage());
        assertTrue(point.enabled());
        assertTrue(point.connected());
        assertFalse(point.chunkLoading());
    }

    @Test
    public void inactiveSavedConnectorsRemainDiscoverableByCableTopology() {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setBoolean("fluxEnabled", false);
        saved.setBoolean("fluxConnected", false);
        saved.setBoolean("fluxRedstone", true);
        TileFluxPlug plug = new TileFluxPlug();
        TileFluxPoint point = new TileFluxPoint();
        plug.readFromNBT(saved);
        point.readFromNBT(saved);

        // GT caches consumers using the physical query; inactive plugs must remain in that graph.
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            assertTrue(plug.inputEnergyFrom(side, false));
            assertFalse(plug.inputEnergyFrom(side, true));
            assertFalse(plug.inputEnergyFrom(side));
            assertFalse(plug.outputsEnergyTo(side, false));
            assertTrue(point.outputsEnergyTo(side, false));
            assertFalse(point.outputsEnergyTo(side, true));
            assertFalse(point.outputsEnergyTo(side));
            assertFalse(point.inputEnergyFrom(side, false));
        }
    }
}
