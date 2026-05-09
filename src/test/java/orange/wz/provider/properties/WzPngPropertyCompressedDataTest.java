package orange.wz.provider.properties;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.*;

class WzPngPropertyCompressedDataTest {
    @Test
    void exportCompressedDataReturnsDefensiveCopy() throws Exception {
        WzPngProperty png = pngWithCompressedBytes(new byte[]{1, 2, 3, 4});

        WzPngProperty.CompressedPngData data = png.exportCompressedData();
        byte[] exported = data.getCompressedBytes();
        exported[0] = 99;

        assertArrayEquals(new byte[]{1, 2, 3, 4}, png.exportCompressedData().getCompressedBytes());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, data.getCompressedBytes());
    }

    @Test
    void copyCompressedFromCopiesMetadataAndBytesWithoutKeepingExternalReferences() throws Exception {
        WzPngProperty source = pngWithCompressedBytes(new byte[]{5, 6, 7});
        WzPngProperty.CompressedPngData data = source.exportCompressedData();
        WzPngProperty target = new WzPngProperty("target", null, null);

        target.copyCompressedFrom(data, false);
        byte[] copied = target.getCompressedBytes(false);
        copied[0] = 99;

        assertEquals(2, target.getWidth());
        assertEquals(2, target.getHeight());
        assertEquals(WzPngFormat.ARGB8888, target.getFormat());
        assertEquals(0, target.getScale());
        assertArrayEquals(new byte[]{5, 6, 7}, data.getCompressedBytes());
        assertArrayEquals(new byte[]{5, 6, 7}, target.exportCompressedData().getCompressedBytes());
    }

    @Test
    void setImageAcceptsExplicitZlibModeAndProducesInflatableBytes() throws Exception {
        WzPngProperty png = new WzPngProperty("png", null, null);
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF112233);

        png.setImage(image, WzPngFormat.ARGB8888, 0, Deflater.BEST_COMPRESSION, WzPngZlibCompressMode.BRUTE_SMALLEST);

        assertNotNull(png.getCompressedBytes(false));
        BufferedImage decoded = png.getImage(false);
        assertEquals(0xFF112233, decoded.getRGB(0, 0));
    }

    @Test
    void copyCompressedFromRejectsNullData() {
        WzPngProperty png = new WzPngProperty("png", null, null);
        assertThrows(IllegalArgumentException.class, () -> png.copyCompressedFrom(null, false));
    }

    private static WzPngProperty pngWithCompressedBytes(byte[] compressedBytes) throws Exception {
        WzPngProperty png = new WzPngProperty("png", null, null);
        setField(png, "width", 2);
        setField(png, "height", 2);
        setField(png, "format", WzPngFormat.ARGB8888);
        setField(png, "scale", 0);
        setField(png, "listWzUsed", false);
        setField(png, "compressedBytes", Arrays.copyOf(compressedBytes, compressedBytes.length));
        return png;
    }

    private static void setField(WzPngProperty png, String name, Object value) throws Exception {
        Field field = WzPngProperty.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(png, value);
    }
}
