package orange.wz.provider.tools;

import orange.wz.provider.properties.WzPngFormat;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ImgToolPngFormatCompatibilityTest {
    @Test
    void format3UsesArgb4444RawSizeWithScaleTwo() {
        assertEquals(32, ImgTool.getRawByteSize(WzPngFormat.FORMAT3, 2, 8, 8));
    }

    @Test
    void format517UsesRgb565RawSizeWithScaleFour() {
        assertEquals(32, ImgTool.getRawByteSize(WzPngFormat.FORMAT517, 4, 16, 16));
    }

    @Test
    void rejectsScaledFormatsWhenDimensionsAreNotDivisible() {
        assertThrows(IllegalArgumentException.class, () -> ImgTool.getRawByteSize(WzPngFormat.FORMAT3, 2, 7, 8));
        assertThrows(IllegalArgumentException.class, () -> ImgTool.getRawByteSize(WzPngFormat.FORMAT517, 4, 15, 16));
    }

    @Test
    void imageTypeSupportsLegacyCompatibilityFormats() {
        assertEquals(BufferedImage.TYPE_INT_ARGB, ImgTool.getBufferImageType(WzPngFormat.FORMAT3));
        assertEquals(BufferedImage.TYPE_USHORT_565_RGB, ImgTool.getBufferImageType(WzPngFormat.FORMAT517));
    }
}
