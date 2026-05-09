package orange.wz.provider.properties;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WzPngWriteOptionsTest {
    @Test
    void defaultsShouldUseDeflaterDefaultCompressionAndDefaultMode() {
        WzPngWriteOptions options = WzPngWriteOptions.defaults();

        assertEquals(Deflater.DEFAULT_COMPRESSION, options.zlibLevel());
        assertEquals(WzPngZlibCompressMode.DEFAULT, options.zlibMode());
    }

    @Test
    void constructorShouldValidateLevelAndDefaultNullMode() {
        WzPngWriteOptions options = new WzPngWriteOptions(9, null);

        assertEquals(9, options.zlibLevel());
        assertEquals(WzPngZlibCompressMode.DEFAULT, options.zlibMode());
        assertThrows(IllegalArgumentException.class, () -> new WzPngWriteOptions(-2, WzPngZlibCompressMode.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> new WzPngWriteOptions(10, WzPngZlibCompressMode.DEFAULT));
    }

    @Test
    void defaultCompressionShouldNotBeClampedToNoCompression() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, 0xFF336699);
            }
        }
        WzPngProperty defaultCompression = new WzPngProperty("default", null, null);
        WzPngProperty noCompression = new WzPngProperty("store", null, null);

        defaultCompression.setImage(image, WzPngFormat.ARGB8888, 0, Deflater.DEFAULT_COMPRESSION, WzPngZlibCompressMode.DEFAULT);
        noCompression.setImage(image, WzPngFormat.ARGB8888, 0, Deflater.NO_COMPRESSION, WzPngZlibCompressMode.DEFAULT);

        byte[] defaultBytes = defaultCompression.getCompressedBytes(false);
        byte[] storedBytes = noCompression.getCompressedBytes(false);
        assertNotNull(defaultBytes);
        assertNotNull(storedBytes);
        assertTrue(defaultBytes.length < storedBytes.length,
                "Deflater.DEFAULT_COMPRESSION (-1) should compress better than level 0 for a flat image");
    }
}
