package com.xyp.gtnotgood.ae2thing.quickterminal;

import java.io.IOException;

import appeng.api.storage.data.IAEStack;
import appeng.container.sync.StreamCodec;
import appeng.container.sync.StreamCodecs;
import io.netty.buffer.ByteBuf;

/** One NEI ingredient substitution, applied to every matching active input slot. */
public final class RecipeIngredientReplacement {

    public static final StreamCodec<RecipeIngredientReplacement> CODEC = StreamCodecs.of(
        RecipeIngredientReplacement.class.getName(),
        RecipeIngredientReplacement::write,
        RecipeIngredientReplacement::read);

    private final IAEStack<?> from;
    private final IAEStack<?> to;

    public RecipeIngredientReplacement(IAEStack<?> from, IAEStack<?> to) {
        this.from = copy(from);
        this.to = copy(to);
    }

    public IAEStack<?> getFrom() {
        return copy(from);
    }

    public IAEStack<?> getTo() {
        return copy(to);
    }

    private static IAEStack<?> copy(IAEStack<?> stack) {
        return stack == null ? null : stack.copy();
    }

    private static void write(ByteBuf buffer, RecipeIngredientReplacement replacement) throws IOException {
        IAEStack.writeToPacketGeneric(buffer, replacement.from);
        IAEStack.writeToPacketGeneric(buffer, replacement.to);
    }

    private static RecipeIngredientReplacement read(ByteBuf buffer) throws IOException {
        return new RecipeIngredientReplacement(IAEStack.fromPacketGeneric(buffer), IAEStack.fromPacketGeneric(buffer));
    }
}
