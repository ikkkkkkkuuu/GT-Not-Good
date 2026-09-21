package com.xyp.gtnotgood.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Applies the optional Edge icon on the render thread after startup icon overrides have finished.
 * Registered only by the client proxy when enabled, then removed after one attempt; no per-tick IO is retained.
 */
public final class EdgeWindowIcon {

    /**
     * Waits for a created display and replaces both Windows window icon sizes once.
     * Failure keeps the existing icon and must not prevent the game from starting.
     *
     * @param event client tick delivered on the render thread, including while in the main menu
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Display.isCreated()) {
            return;
        }
        FMLCommonHandler.instance()
            .bus()
            .unregister(this);
        try {
            Display.setIcon(new ByteBuffer[] { readIcon(16), readIcon(32) });
        } catch (IOException | RuntimeException e) {
            GTNotGood.LOG.warn("Could not apply Microsoft Edge window icon; keeping the existing icon", e);
        }
    }

    /**
     * Reads the bundled icon directly so resource packs cannot replace this optional window identity.
     * LWJGL expects tightly packed RGBA bytes, rather than Java's ARGB integer byte order.
     *
     * @param size square icon width and height in pixels
     * @return direct RGBA buffer positioned at its first byte
     * @throws IOException if the packaged image is absent, invalid, or has an unexpected size
     */
    private static ByteBuffer readIcon(int size) throws IOException {
        String path = "/assets/" + ModList.GTNotGood.getResourceLocation() + "/icons/edge_" + size + ".png";
        try (InputStream stream = EdgeWindowIcon.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing window icon: " + path);
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null || image.getWidth() != size || image.getHeight() != size) {
                throw new IOException("Invalid window icon: " + path);
            }
            ByteBuffer buffer = BufferUtils.createByteBuffer(size * size * 4);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int argb = image.getRGB(x, y);
                    buffer.put((byte) (argb >> 16));
                    buffer.put((byte) (argb >> 8));
                    buffer.put((byte) argb);
                    buffer.put((byte) (argb >> 24));
                }
            }
            buffer.flip();
            return buffer;
        }
    }
}
