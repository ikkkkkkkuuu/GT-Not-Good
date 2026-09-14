package com.xyp.gtnotgood.ae2thing.quickterminal;

import java.io.IOException;

import appeng.api.storage.data.IAEStack;
import appeng.container.sync.StreamCodec;
import appeng.container.sync.StreamCodecs;
import io.netty.buffer.ByteBuf;

/** A fluid selected in the quick terminal's ME storage panel. */
public final class StorageFluidRequest {

    public static final StreamCodec<StorageFluidRequest> CODEC = StreamCodecs
        .of(StorageFluidRequest.class.getName(), StorageFluidRequest::write, StorageFluidRequest::read);

    private final IAEStack<?> fluid;

    private final boolean fullStack;

    public StorageFluidRequest(IAEStack<?> fluid) {
        this(fluid, false);
    }

    /**
     * Requests one container or one stack, with the actual limit determined by the server.
     *
     * @param fluid     selected fluid, copied so later client changes do not modify this request
     * @param fullStack true for Ctrl-click batch interaction; false for a single container
     */
    public StorageFluidRequest(IAEStack<?> fluid, boolean fullStack) {
        this.fullStack = fullStack;
        this.fluid = fluid == null ? null : fluid.copy();
    }

    public IAEStack<?> getFluid() {
        return fluid == null ? null : fluid.copy();
    }

    public boolean isFullStack() {
        return fullStack;
    }

    private static void write(ByteBuf buffer, StorageFluidRequest request) throws IOException {
        IAEStack.writeToPacketGeneric(buffer, request.fluid);
        buffer.writeBoolean(request.fullStack);
    }

    private static StorageFluidRequest read(ByteBuf buffer) throws IOException {
        return new StorageFluidRequest(IAEStack.fromPacketGeneric(buffer), buffer.readBoolean());
    }
}
