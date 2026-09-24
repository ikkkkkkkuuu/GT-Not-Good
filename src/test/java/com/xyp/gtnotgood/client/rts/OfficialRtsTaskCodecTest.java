package com.xyp.gtnotgood.client.rts;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagString;

import org.junit.Test;

import com.rtsbuilding.rtsbuilding.platform.math.BlockPos;
import com.rtsbuilding.rtsbuilding.platform.math.EnumFacing;
import com.rtsbuilding.rtsbuilding.server.task.DestructionTaskPayload;
import com.rtsbuilding.rtsbuilding.server.task.MiningTaskPayload;
import com.rtsbuilding.rtsbuilding.server.task.PlacementTaskPayload;
import com.rtsbuilding.rtsbuilding.server.task.destruction.DestructionTaskCodec;
import com.rtsbuilding.rtsbuilding.server.task.destruction.DestructionTaskState;
import com.rtsbuilding.rtsbuilding.server.task.mining.MiningTaskCodec;
import com.rtsbuilding.rtsbuilding.server.task.mining.MiningTaskState;
import com.rtsbuilding.rtsbuilding.server.task.persistence.NbtCompat;
import com.rtsbuilding.rtsbuilding.server.task.placement.PlacementResumePolicy;
import com.rtsbuilding.rtsbuilding.server.task.placement.PlacementTaskCodec;
import com.rtsbuilding.rtsbuilding.server.task.placement.PlacementTaskState;

/** Reproduces task snapshot decoding failures with Minecraft 1.7.10's actual NBT types. */
public class OfficialRtsTaskCodecTest {

    @Test
    public void unavailableDestructionHistoryDoesNotThrow() throws Exception {
        java.lang.reflect.Method decode = com.rtsbuilding.rtsbuilding.server.service.destruction.RtsDestructionBatch.class
            .getDeclaredMethod(
                "decodeHistoryRecord",
                net.minecraft.entity.player.EntityPlayerMP.class,
                NBTTagCompound.class);
        decode.setAccessible(true);
        NBTTagCompound record = new NBTTagCompound();
        NBTTagCompound state = new NBTTagCompound();
        state.setString("Name", "missing_rts_test:no_such_block");
        record.setTag("state", state);
        assertNull(decode.invoke(null, null, record));
    }

    @Test
    public void placementWithoutPrototypeSurvivesRestoreAndResave() {
        NBTTagCompound definition = new NBTTagCompound();
        definition.setBoolean("forceEmptyHand", true);
        com.rtsbuilding.rtsbuilding.server.service.placement.RtsPlacementBatch.PlaceBatchJob job = com.rtsbuilding.rtsbuilding.server.service.placement.RtsPlacementBatch.PlaceBatchJob
            .fromNbt(definition);
        assertNull(job.itemPrototype());
        assertNull(
            com.rtsbuilding.rtsbuilding.server.service.placement.RtsPlacementBatch.PlaceBatchJob.fromNbt(job.toNbt())
                .itemPrototype());
    }

    @Test
    public void unreadablePlacementPrototypeRemainsEmpty() {
        NBTTagCompound definition = new NBTTagCompound();
        NBTTagCompound invalidStack = new NBTTagCompound();
        invalidStack.setShort("id", (short) -1);
        invalidStack.setByte("Count", (byte) 1);
        definition.setTag("itemPrototype", invalidStack);
        assertNull(
            com.rtsbuilding.rtsbuilding.server.service.placement.RtsPlacementBatch.PlaceBatchJob.fromNbt(definition)
                .itemPrototype());
    }

    @Test
    public void destructionRoundTripPreservesTargetsAndProgress() {
        java.util.List<BlockPos> targets = Arrays.asList(new BlockPos(-123, 64, 456), new BlockPos(27, 65, -321));
        DestructionTaskState state = new DestructionTaskState(
            targets,
            (byte) 0,
            false,
            false,
            3,
            1,
            1,
            0,
            targets.subList(0, 1),
            Collections.emptyList());
        DestructionTaskPayload original = new DestructionTaskPayload(UUID.randomUUID(), -1, 3, state);
        DestructionTaskPayload decoded = DestructionTaskCodec.decode(DestructionTaskCodec.encode(original));
        assertEquals(original.ownerId(), decoded.ownerId());
        assertEquals(
            targets,
            decoded.state()
                .targets());
        assertEquals(
            targets.subList(0, 1),
            decoded.state()
                .destroyedPositions());
        assertEquals(
            1,
            decoded.state()
                .cursorUnits());
    }

    @Test
    public void placementReadsNativeJobListAndIntPairProgress() {
        BlockPos target = new BlockPos(-456, 64, 789);
        NBTTagCompound definition = new NBTTagCompound();
        NBTTagList positions = new NBTTagList();
        positions.appendTag(new NBTTagLong(target.toLong()));
        definition.setTag("positions", positions);
        PlacementTaskState state = new PlacementTaskState(
            definition,
            4,
            1,
            1,
            1,
            0,
            Collections.singletonList(target),
            PlacementResumePolicy.DEFAULT);
        PlacementTaskPayload decoded = PlacementTaskCodec
            .decode(PlacementTaskCodec.encode(new PlacementTaskPayload(UUID.randomUUID(), 0, 4, state)));
        assertEquals(
            Collections.singletonList(target),
            decoded.state()
                .placedPositions());
        assertArrayEquals(
            new long[] { target.toLong() },
            NbtCompat.getLongArray(
                decoded.state()
                    .definition(),
                "positions"));
    }

    @Test
    public void miningRoundTripPreservesRemainingTargets() {
        java.util.List<BlockPos> targets = Collections.singletonList(new BlockPos(-500, 70, -900));
        MiningTaskState state = new MiningTaskState(
            MiningTaskState.Mode.BATCH,
            5,
            targets,
            1,
            0,
            0,
            0,
            EnumFacing.UP,
            0,
            false,
            false,
            0,
            -1,
            Collections.emptyList());
        MiningTaskPayload decoded = MiningTaskCodec
            .decode(MiningTaskCodec.encode(new MiningTaskPayload(UUID.randomUUID(), 0, 5, state)));
        assertEquals(
            targets,
            decoded.state()
                .remainingTargets());
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedIntPairsAreRejected() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("positions", new int[] { 1 });
        NbtCompat.getLongArray(tag, "positions");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonLongListIsRejected() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString("bad"));
        tag.setTag("positions", list);
        NbtCompat.getLongArray(tag, "positions");
    }
}
