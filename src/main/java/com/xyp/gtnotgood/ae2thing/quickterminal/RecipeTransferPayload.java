package com.xyp.gtnotgood.ae2thing.quickterminal;

import java.io.IOException;

import appeng.api.storage.data.IAEStack;
import appeng.container.sync.StreamCodec;
import appeng.container.sync.StreamCodecs;
import io.netty.buffer.ByteBuf;

/**
 * Complete state needed to atomically transfer an NEI recipe into the pattern
 * terminal. Keeping the optional encode request in the same action guarantees
 * that the server sees the new ingredients before encoding the pattern.
 */
public final class RecipeTransferPayload {

    public static final int SLOT_COUNT = 16;
    public static final StreamCodec<RecipeTransferPayload> CODEC = StreamCodecs
        .of(RecipeTransferPayload.class.getName(), RecipeTransferPayload::write, RecipeTransferPayload::read);

    private final boolean crafting;
    private final boolean encode;
    private final int processingGridSize;
    private final boolean inverted;
    private final IAEStack<?>[] inputs;
    private final IAEStack<?>[] outputs;

    public RecipeTransferPayload(boolean crafting, boolean encode, int processingGridSize, boolean inverted,
        IAEStack<?>[] inputs, IAEStack<?>[] outputs) {
        this.crafting = crafting;
        this.encode = encode;
        this.processingGridSize = 4;
        this.inverted = !crafting && this.processingGridSize == 4 && inverted;
        this.inputs = copyToFixedSize(inputs);
        this.outputs = copyToFixedSize(outputs);
    }

    public boolean isCrafting() {
        return crafting;
    }

    public boolean shouldEncode() {
        return encode;
    }

    public int getProcessingGridSize() {
        return processingGridSize;
    }

    public boolean isInverted() {
        return inverted;
    }

    public IAEStack<?> getInput(int slot) {
        return copy(inputs[slot]);
    }

    public IAEStack<?> getOutput(int slot) {
        return copy(outputs[slot]);
    }

    private static IAEStack<?>[] copyToFixedSize(IAEStack<?>[] source) {
        IAEStack<?>[] result = new IAEStack<?>[SLOT_COUNT];
        if (source == null) return result;
        int count = Math.min(source.length, SLOT_COUNT);
        for (int slot = 0; slot < count; slot++) {
            result[slot] = copy(source[slot]);
        }
        return result;
    }

    private static IAEStack<?> copy(IAEStack<?> stack) {
        return stack == null ? null : stack.copy();
    }

    private static void write(ByteBuf buffer, RecipeTransferPayload payload) throws IOException {
        buffer.writeBoolean(payload.crafting);
        buffer.writeBoolean(payload.encode);
        buffer.writeByte(payload.processingGridSize);
        buffer.writeBoolean(payload.inverted);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            IAEStack.writeToPacketGeneric(buffer, payload.inputs[slot]);
        }
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            IAEStack.writeToPacketGeneric(buffer, payload.outputs[slot]);
        }
    }

    private static RecipeTransferPayload read(ByteBuf buffer) throws IOException {
        boolean crafting = buffer.readBoolean();
        boolean encode = buffer.readBoolean();
        int processingGridSize = buffer.readUnsignedByte();
        boolean inverted = buffer.readBoolean();
        IAEStack<?>[] inputs = new IAEStack<?>[SLOT_COUNT];
        IAEStack<?>[] outputs = new IAEStack<?>[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            inputs[slot] = IAEStack.fromPacketGeneric(buffer);
        }
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            outputs[slot] = IAEStack.fromPacketGeneric(buffer);
        }
        return new RecipeTransferPayload(crafting, encode, processingGridSize, inverted, inputs, outputs);
    }
}
