package com.xyp.gtnotgood.client.rts;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.Test;

import com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarTexture;

/** Covers malformed-image recovery and the actual bundled top-bar icon catalog. */
public class OfficialRtsTextureDecodeTest {

    @Test(expected = IOException.class)
    public void unsupportedImageIsRecoverable() throws Exception {
        TopBarTexture.decode(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
    }

    @Test(expected = IOException.class)
    public void missingImageIsRecoverable() throws Exception {
        TopBarTexture.decode(null);
    }

    @Test
    public void everyBundledTopBarPngDecodes() throws Exception {
        Path directory = Paths.get("src/vendor/rtsbuilding/resources/assets/rtsbuilding/textures/gui/topbar");
        int count = 0;
        try (Stream<Path> files = Files.list(directory)) {
            for (Path path : (Iterable<Path>) files.filter(
                p -> p.toString()
                    .endsWith(".png"))::iterator) {
                try (InputStream stream = Files.newInputStream(path)) {
                    assertTrue(
                        path.toString(),
                        TopBarTexture.decode(stream)
                            .getWidth() > 0);
                    count++;
                }
            }
        }
        assertTrue(count >= 4);
    }
}
