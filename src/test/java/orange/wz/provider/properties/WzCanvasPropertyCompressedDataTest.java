package orange.wz.provider.properties;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class WzCanvasPropertyCompressedDataTest {
    @Test
    void copyPngFromCompressedDataCopiesMetadataAndMarksChanged() {
        WzCanvasProperty source = new WzCanvasProperty("source", null, null);
        source.initPngProperty("source", source, null);
        source.setPng(testImage(0xFF445566), WzPngFormat.ARGB8888, 0);
        WzPngProperty.CompressedPngData data = source.exportCompressedPngData();

        TestImage image = new TestImage(null);
        WzCanvasProperty target = new WzCanvasProperty("target", null, image);
        target.initPngProperty("target", target, image);

        target.copyPngFromCompressedData(data, true);

        assertTrue(image.isChanged());
        assertTrue(target.isTempChanged());
        assertEquals(1, target.getWidth());
        assertEquals(1, target.getHeight());
        assertEquals(WzPngFormat.ARGB8888, target.getFormat());
        assertEquals(0, target.getScale());
        assertEquals(0xFF445566, target.getPngImage(false).getRGB(0, 0));
    }

    @Test
    void copyPngFromCopiesWithoutForcingDecodeWhenNotRequested() {
        WzCanvasProperty source = new WzCanvasProperty("source", null, null);
        source.initPngProperty("source", source, null);
        source.setPng(testImage(0xFF778899), WzPngFormat.ARGB8888, 0);

        TestImage image = new TestImage(null);
        WzCanvasProperty target = new WzCanvasProperty("target", null, image);
        target.initPngProperty("target", target, image);

        target.copyPngFrom(source, false);

        assertEquals(source.getCompressedPngStorageLength(), target.getCompressedPngStorageLength());
        assertEquals(0xFF778899, target.getPngImage(false).getRGB(0, 0));
    }

    @Test
    void copyPngFromRejectsNullSource() {
        WzCanvasProperty target = new WzCanvasProperty("target", null, null);
        target.initPngProperty("target", target, null);

        assertThrows(IllegalArgumentException.class, () -> target.copyPngFrom(null, false));
    }

    private static BufferedImage testImage(int argb) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, argb);
        return image;
    }
}
