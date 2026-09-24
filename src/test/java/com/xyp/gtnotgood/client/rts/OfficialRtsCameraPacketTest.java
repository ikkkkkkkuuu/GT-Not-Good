package com.xyp.gtnotgood.client.rts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.rtsbuilding.rtsbuilding.network.camera.S2CRtsCameraStatePayload;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** Verifies the negative entity sentinel used by the real server's camera-close acknowledgement. */
public class OfficialRtsCameraPacketTest {

    @Test
    public void closeAcknowledgementPreservesNegativeEntityId() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            new S2CRtsCameraStatePayload(false, -1, 0, 0, 0, 128, 18, 0, 70, false, false).toBytes(buffer);
            S2CRtsCameraStatePayload decoded = new S2CRtsCameraStatePayload();
            decoded.fromBytes(buffer);
            assertFalse(decoded.enabled());
            assertEquals(-1, decoded.cameraEntityId());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
