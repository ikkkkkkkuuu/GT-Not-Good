package com.xyp.gtnotgood.client.rts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rtsbuilding.rtsbuilding.platform.block.BlockState;
import com.rtsbuilding.rtsbuilding.platform.math.BlockPos;
import com.rtsbuilding.rtsbuilding.server.history.HistoryBlockRecord;
import com.rtsbuilding.rtsbuilding.server.service.mining.RtsMiningStateMachine;
import com.rtsbuilding.rtsbuilding.server.storage.session.RtsStorageSession;
import com.rtsbuilding.rtsbuilding.server.task.MiningTaskPayload;
import com.rtsbuilding.rtsbuilding.server.task.mining.MiningTaskCodec;
import com.rtsbuilding.rtsbuilding.server.task.mining.MiningTaskState;
import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

/** Covers issue #3 with the native, case-sensitive Forge registry and saved mining history. */
public class OfficialRtsMiningHistoryTest {

    private static final String MIXED_CASE_ID = "MiningHistoryTest:MixedCaseBlock";
    private static final Block BLOCK = new Block(Material.rock) {};
    private static final BlockPos POS = new BlockPos(-123, 65, 456);

    @BeforeClass
    public static void registerBlock() throws Exception {
        HeadlessBlockRegistry.bootstrap();
        Method register = Block.blockRegistry.getClass()
            .getDeclaredMethod("addObjectRaw", int.class, String.class, Object.class);
        register.setAccessible(true);
        register.invoke(Block.blockRegistry, 31002, MIXED_CASE_ID, BLOCK);
    }

    @Test
    public void mixedCaseRegistryNameAndTileDataSurviveRoundTrip() {
        NBTTagCompound tile = new NBTTagCompound();
        tile.setInteger("machine", 42);
        HistoryBlockRecord original = new HistoryBlockRecord(POS, BlockState.of(BLOCK, 7), tile);
        NBTTagCompound encoded = MiningTaskCodec.encodeHistory(original);
        assertEquals(
            MIXED_CASE_ID,
            encoded.getCompoundTag("state")
                .getString("id"));
        HistoryBlockRecord decoded = MiningTaskCodec.decodeHistory(encoded);
        assertNotNull(decoded);
        assertSame(
            BLOCK,
            decoded.state()
                .getBlock());
        assertEquals(
            7,
            decoded.state()
                .getMetadata());
        assertEquals(POS, decoded.pos());
        assertEquals(tile, decoded.blockEntityData());
        encoded.getCompoundTag("block_entity")
            .setInteger("machine", 0);
        assertEquals(
            42,
            decoded.blockEntityData()
                .getInteger("machine"));
    }

    @Test
    public void airAndMissingBlocksAreSkippedDuringDecode() {
        assertNull(MiningTaskCodec.decodeHistory(history("minecraft:air")));
        assertNull(MiningTaskCodec.decodeHistory(history("missing_mining_test:removed_block")));
    }

    @Test
    public void malformedHistoryIsSkippedDuringDecode() {
        assertNull(MiningTaskCodec.decodeHistory(null));
        assertNull(MiningTaskCodec.decodeHistory(new NBTTagCompound()));
        NBTTagCompound missingStateId = history(MIXED_CASE_ID);
        missingStateId.getCompoundTag("state")
            .removeTag("id");
        assertNull(MiningTaskCodec.decodeHistory(missingStateId));
        NBTTagCompound wrongPositionType = history(MIXED_CASE_ID);
        wrongPositionType.setInteger("pos", 1);
        assertNull(MiningTaskCodec.decodeHistory(wrongPositionType));
        NBTTagCompound wrongMetadataType = history(MIXED_CASE_ID);
        wrongMetadataType.getCompoundTag("state")
            .setString("meta", "7");
        assertNull(MiningTaskCodec.decodeHistory(wrongMetadataType));
        NBTTagCompound invalidMetadata = history(MIXED_CASE_ID);
        invalidMetadata.getCompoundTag("state")
            .setInteger("meta", 16);
        assertNull(MiningTaskCodec.decodeHistory(invalidMetadata));
    }

    @Test
    public void airAndUnregisteredBlocksAreSkippedDuringEncode() {
        assertNull(MiningTaskCodec.encodeHistory(null));
        assertNull(MiningTaskCodec.encodeHistory(new HistoryBlockRecord(POS, BlockState.defaultState(Blocks.air))));
        Block unregistered = new Block(Material.rock) {};
        assertNull(MiningTaskCodec.encodeHistory(new HistoryBlockRecord(POS, BlockState.of(unregistered, 0))));
    }

    @Test
    public void detachedFinalizationKeepsValidHistoryAroundInvalidEntries() throws Exception {
        NBTTagCompound valid = history(MIXED_CASE_ID);
        List<NBTTagCompound> saved = Arrays.asList(
            history("minecraft:air"),
            valid,
            history("missing_mining_test:removed_block"),
            new NBTTagCompound(),
            valid);
        Method decode = RtsMiningStateMachine.class
            .getDeclaredMethod("decodeDetachedHistory", EntityPlayerMP.class, List.class);
        decode.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<HistoryBlockRecord> decoded = (List<HistoryBlockRecord>) decode.invoke(null, null, saved);
        assertEquals(2, decoded.size());
        assertSame(
            BLOCK,
            decoded.get(0)
                .state()
                .getBlock());
        assertSame(
            BLOCK,
            decoded.get(1)
                .state()
                .getBlock());
    }

    @Test
    public void activeSnapshotFiltersAirBeforePersistence() {
        RtsStorageSession session = new RtsStorageSession();
        session.mining.miningPos = POS;
        session.mining.ultimineProcessedPositions.add(new HistoryBlockRecord(POS, BlockState.defaultState(Blocks.air)));
        session.mining.ultimineProcessedPositions.add(new HistoryBlockRecord(POS, BlockState.of(BLOCK, 7)));
        MiningTaskState state = RtsMiningStateMachine.snapshotDetachedActive(session);
        MiningTaskPayload restored = MiningTaskCodec
            .decode(MiningTaskCodec.encode(new MiningTaskPayload(UUID.randomUUID(), 0, -1, state)));
        assertEquals(
            1,
            restored.state()
                .historyRecords()
                .size());
        assertSame(
            BLOCK,
            MiningTaskCodec.decodeHistory(
                restored.state()
                    .historyRecords()
                    .get(0))
                .state()
                .getBlock());
    }

    @Test
    public void persistedTaskWithInvalidHistoryCanStillFinalize() throws Exception {
        RtsStorageSession session = new RtsStorageSession();
        session.mining.miningPos = POS;
        MiningTaskState state = RtsMiningStateMachine.snapshotDetachedActive(session);
        state = state.next(
            state.mode(),
            state.remainingTargets(),
            0,
            0,
            0,
            0,
            -1,
            Arrays.asList(
                history("minecraft:air"),
                history(MIXED_CASE_ID),
                history("missing_mining_test:removed_block"),
                new NBTTagCompound()));
        MiningTaskPayload restored = MiningTaskCodec
            .decode(MiningTaskCodec.encode(new MiningTaskPayload(UUID.randomUUID(), 0, -1, state)));
        Method decode = RtsMiningStateMachine.class
            .getDeclaredMethod("decodeDetachedHistory", EntityPlayerMP.class, List.class);
        decode.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<HistoryBlockRecord> decoded = (List<HistoryBlockRecord>) decode.invoke(
            null,
            null,
            restored.state()
                .historyRecords());
        assertEquals(1, decoded.size());
        assertSame(
            BLOCK,
            decoded.get(0)
                .state()
                .getBlock());
    }

    private static NBTTagCompound history(String id) {
        NBTTagCompound state = new NBTTagCompound();
        state.setString("id", id);
        state.setInteger("meta", 7);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("pos", POS.toLong());
        tag.setTag("state", state);
        return tag;
    }
}
