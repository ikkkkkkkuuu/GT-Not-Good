package com.rtsbuilding.rtsbuilding.server.task.mining;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import com.rtsbuilding.rtsbuilding.server.history.HistoryBlockRecord;
import com.rtsbuilding.rtsbuilding.server.task.MiningTaskPayload;
import com.rtsbuilding.rtsbuilding.server.task.persistence.DimensionIdCodec;
import com.rtsbuilding.rtsbuilding.server.task.persistence.NbtCompat;
import com.rtsbuilding.rtsbuilding.platform.math.BlockPos;
import com.rtsbuilding.rtsbuilding.platform.math.EnumFacing;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.init.Blocks;
import com.rtsbuilding.rtsbuilding.platform.block.BlockState;
import net.minecraftforge.common.util.Constants;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** MiningTaskPayload 的版本化纯 NBT codec，并集中保存历史方块快照格式。 */
public final class MiningTaskCodec {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_TARGETS = 32_768;

    private MiningTaskCodec() {
    }

    public static NBTTagCompound encode(MiningTaskPayload payload) {
        MiningTaskState state = payload.state();
        if (state.totalUnits() > MAX_TARGETS) throw new IllegalArgumentException("mining target 数量越界");
        if (state.historyRecords().size() > MAX_TARGETS * 7) {
            throw new IllegalArgumentException("mining history 越界");
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("schema", SCHEMA_VERSION);
        NbtCompat.setUuid(tag, "owner", payload.ownerId());
        tag.setString("dimension", DimensionIdCodec.fromDimension(payload.dimension()));
        tag.setInteger("workflow", payload.workflowEntryId());
        tag.setString("mode", state.mode().name());
        long[] remaining = new long[state.remainingTargets().size()];
        for (int i = 0; i < remaining.length; i++) remaining[i] = state.remainingTargets().get(i).toLong();
        NbtCompat.setLongArray(tag, "remaining", remaining);
        tag.setInteger("total", state.totalUnits());
        tag.setInteger("cursor", state.cursorUnits());
        tag.setInteger("succeeded", state.succeededUnits());
        tag.setInteger("failed", state.failedUnits());
        tag.setByte("face", (byte) state.face().getIndex());
        tag.setInteger("tool_slot", state.toolSlot());
        tag.setBoolean("selected_tool", state.selectedToolRequested());
        tag.setBoolean("protect_tool", state.toolProtectionEnabled());
        tag.setFloat("progress", state.blockProgress());
        tag.setInteger("stage", state.visibleStage());
        NBTTagList history = new NBTTagList();
        for (NBTTagCompound entry : state.historyRecords()) history.appendTag(entry);
        tag.setTag("history", history);
        return tag;
    }

    public static MiningTaskPayload decode(NBTTagCompound tag) {
        requireFields(tag);
        String dimensionId = tag.getString("dimension");
        if (!DimensionIdCodec.isCanonical(dimensionId)) {
            throw new IllegalArgumentException("mining dimension 无效");
        }
        MiningTaskState.Mode mode;
        try {
            mode = MiningTaskState.Mode.valueOf(tag.getString("mode"));
        } catch (IllegalArgumentException invalidMode) {
            throw new IllegalArgumentException("mining mode 无效", invalidMode);
        }
        long[] encodedTargets = NbtCompat.getLongArray(tag, "remaining");
        int total = tag.getInteger("total");
        if (total < 0 || total > MAX_TARGETS || encodedTargets.length > total) {
            throw new IllegalArgumentException("mining target 数量越界");
        }
        List<BlockPos> targets = new ArrayList<>(encodedTargets.length);
        for (long encoded : encodedTargets) targets.add(BlockPos.fromLong(encoded));
        NBTTagList encodedHistory = tag.getTagList("history", Constants.NBT.TAG_COMPOUND);
        if (encodedHistory.tagCount() > MAX_TARGETS * 7) throw new IllegalArgumentException("mining history 越界");
        List<NBTTagCompound> history = new ArrayList<NBTTagCompound>(encodedHistory.tagCount());
        int emptyHistory = 0;
        for (int i = 0; i < encodedHistory.tagCount(); i++) {
            NBTTagCompound entry = encodedHistory.getCompoundTagAt(i);
            if (com.rtsbuilding.rtsbuilding.platform.nbt.NbtCompat.isEmpty(entry)) {
                emptyHistory++;
                continue;
            }
            history.add(com.rtsbuilding.rtsbuilding.platform.nbt.NbtCompat.copyCompound(
                    entry));
        }
        if (emptyHistory > 0) {
            RtsbuildingMod.LOGGER.warn("Skipped {} empty mining history records while restoring task for player {}",
                    emptyHistory, NbtCompat.getUuid(tag, "owner"));
        }
        int workflow = tag.getInteger("workflow");
        MiningTaskState state = new MiningTaskState(
                mode, workflow, targets, total,
                tag.getInteger("cursor"), tag.getInteger("succeeded"), tag.getInteger("failed"),
                EnumFacing.byIndex(tag.getByte("face")), tag.getInteger("tool_slot"),
                tag.getBoolean("selected_tool"), tag.getBoolean("protect_tool"),
                tag.getFloat("progress"), tag.getInteger("stage"), history);
        return new MiningTaskPayload(NbtCompat.getUuid(tag, "owner"),
                DimensionIdCodec.toDimension(dimensionId), workflow, state);
    }

    /**
     * 编码可撤回的挖掘快照；调用方只追加非空结果。
     *
     * @param record 挖掘前捕获的方块记录
     * @return 已编码记录；空气、空记录或未注册方块返回 {@code null}
     */
    @Nullable
    public static NBTTagCompound encodeHistory(@Nullable HistoryBlockRecord record) {
        if (record == null || record.state().getBlock() == Blocks.air) return null;
        NBTTagCompound state = com.rtsbuilding.rtsbuilding.platform.nbt.NbtCompat
                .writeBlockState(record.state());
        if ("minecraft:air".equals(state.getString("id"))) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("pos", record.pos().toLong());
        tag.setTag("state", state);
        if (record.blockEntityData() != null) tag.setTag("block_entity",
                com.rtsbuilding.rtsbuilding.platform.nbt.NbtCompat.copyCompound(
                        record.blockEntityData()));
        return tag;
    }

    /**
     * 读取历史快照，不依赖该坐标当前的世界状态；无效记录不应中断任务收尾。
     *
     * @param tag 持久化的历史记录
     * @return 有效方块记录；字段损坏、空气或方块已不可用时返回 {@code null}
     */
    @Nullable
    public static HistoryBlockRecord decodeHistory(@Nullable NBTTagCompound tag) {
        if (tag == null || !NbtCompat.hasType(tag, "pos", Constants.NBT.TAG_LONG)
                || !NbtCompat.hasType(tag, "state", Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        NBTTagCompound encodedState = tag.getCompoundTag("state");
        if (!NbtCompat.hasType(encodedState, "id", Constants.NBT.TAG_STRING)
                || !NbtCompat.hasType(encodedState, "meta", Constants.NBT.TAG_INT)
                || encodedState.getInteger("meta") < 0 || encodedState.getInteger("meta") > 15) return null;
        BlockState state = com.rtsbuilding.rtsbuilding.platform.nbt.NbtCompat
                .readBlockState(encodedState);
        if (state.getBlock() == Blocks.air) return null;
        NBTTagCompound blockEntity = NbtCompat.hasType(tag, "block_entity", Constants.NBT.TAG_COMPOUND)
                ? com.rtsbuilding.rtsbuilding.platform.nbt.NbtCompat.copyCompound(
                        tag.getCompoundTag("block_entity")) : null;
        return new HistoryBlockRecord(BlockPos.fromLong(tag.getLong("pos")), state, blockEntity);
    }

    private static void requireFields(NBTTagCompound tag) {
        if (tag == null || !NbtCompat.hasType(tag, "schema", Constants.NBT.TAG_INT)
                || tag.getInteger("schema") != SCHEMA_VERSION || !NbtCompat.hasUuid(tag, "owner")
                || !NbtCompat.hasType(tag, "dimension", Constants.NBT.TAG_STRING)
                || !NbtCompat.hasType(tag, "workflow", Constants.NBT.TAG_INT)
                || !NbtCompat.hasType(tag, "mode", Constants.NBT.TAG_STRING)
                || !NbtCompat.hasType(tag, "remaining", Constants.NBT.TAG_INT_ARRAY)
                || !NbtCompat.hasType(tag, "total", Constants.NBT.TAG_INT)
                || !NbtCompat.hasType(tag, "cursor", Constants.NBT.TAG_INT)
                || !NbtCompat.hasType(tag, "succeeded", Constants.NBT.TAG_INT)
                || !NbtCompat.hasType(tag, "failed", Constants.NBT.TAG_INT)
                || !NbtCompat.hasType(tag, "face", Constants.NBT.TAG_BYTE)
                || !NbtCompat.hasType(tag, "tool_slot", Constants.NBT.TAG_INT)
                || !NbtCompat.hasType(tag, "selected_tool", Constants.NBT.TAG_BYTE)
                || !NbtCompat.hasType(tag, "protect_tool", Constants.NBT.TAG_BYTE)
                || !NbtCompat.hasType(tag, "progress", Constants.NBT.TAG_FLOAT)
                || !NbtCompat.hasType(tag, "stage", Constants.NBT.TAG_INT)
                || !NbtCompat.hasType(tag, "history", Constants.NBT.TAG_LIST)) {
            throw new IllegalArgumentException("不支持或不完整的 mining task payload");
        }
    }
}
