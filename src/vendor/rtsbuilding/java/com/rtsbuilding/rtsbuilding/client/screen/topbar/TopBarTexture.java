package com.rtsbuilding.rtsbuilding.client.screen.topbar;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;

/**
 * Loads top-bar PNGs without passing an undecodable image to the legacy texture uploader.
 * Resource packs retain priority; invalid overrides fall back to the bundled icon. If both fail,
 * IOException lets TextureManager use its standard missing texture instead of crashing the screen.
 */
public final class TopBarTexture extends SimpleTexture {
    private final ResourceLocation location;

    public TopBarTexture(ResourceLocation location) {
        super(location);
        this.location = location;
    }

    @Override
    public void loadTexture(IResourceManager manager) throws IOException {
        BufferedImage image;
        try (InputStream stream = manager.getResource(location).getInputStream()) {
            image = decode(stream);
        } catch (IOException failure) {
            RtsbuildingMod.LOGGER.warn("RTS top-bar texture {} cannot be decoded; using bundled PNG", location, failure);
            String bundled = "/assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
            try (InputStream stream = TopBarTexture.class.getResourceAsStream(bundled)) {
                image = decode(stream);
            }
        }
        deleteGlTexture();
        TextureUtil.uploadTextureImageAllocate(getGlTextureId(), image, false, false);
    }

    /**
     * Converts ImageIO's null result into the recoverable error expected by TextureManager.
     *
     * @param stream PNG input owned and closed by the caller
     * @return decoded image, never null
     * @throws IOException when input is missing, unsupported or corrupt
     */
    public static BufferedImage decode(InputStream stream) throws IOException {
        if (stream == null) throw new IOException("Missing RTS icon stream");
        BufferedImage image = ImageIO.read(stream);
        if (image == null) throw new IOException("Unsupported or invalid RTS icon image");
        return image;
    }
}
