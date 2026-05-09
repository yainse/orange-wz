package orange.wz.provider.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WzPngFormatCompatibilityTest {
    @Test
    void parsesLegacyCompatibilityFormats() {
        assertEquals(WzPngFormat.FORMAT3, WzPngFormat.getByValue(3));
        assertEquals(WzPngFormat.FORMAT517, WzPngFormat.getByValue(517));
    }

    @Test
    void keepsExistingFormatMappings() {
        assertEquals(WzPngFormat.ARGB4444, WzPngFormat.getByValue(1));
        assertEquals(WzPngFormat.ARGB8888, WzPngFormat.getByValue(2));
        assertEquals(WzPngFormat.ARGB1555, WzPngFormat.getByValue(257));
        assertEquals(WzPngFormat.RGB565, WzPngFormat.getByValue(513));
        assertEquals(WzPngFormat.DXT3, WzPngFormat.getByValue(1026));
        assertEquals(WzPngFormat.DXT5, WzPngFormat.getByValue(2050));
        assertEquals(WzPngFormat.BC7, WzPngFormat.getByValue(4098));
    }

    @Test
    void rejectsUnknownFormat() {
        RuntimeException error = assertThrows(RuntimeException.class, () -> WzPngFormat.getByValue(999999));
        assertTrue(error.getMessage().contains("未知的图片压缩格式"));
    }
}
