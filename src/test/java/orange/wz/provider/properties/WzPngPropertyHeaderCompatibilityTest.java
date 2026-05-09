package orange.wz.provider.properties;

import orange.wz.provider.tools.BinaryWriter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class WzPngPropertyHeaderCompatibilityTest {
    @Test
    void oldHeaderLayoutStillReadsScaleByte() {
        WzPngProperty png = new WzPngProperty("png", null, null);

        png.setData(readerForHeader(2, 3, false));

        assertEquals(WzPngFormat.ARGB8888, png.getFormat());
        assertEquals(3, png.getScale());
        assertEquals(8, getIntField(png, "offset"));
    }

    @Test
    void ambiguousDoubleIntHeaderPrefersOldLayout() {
        WzPngProperty png = new WzPngProperty("png", null, null);

        png.setData(readerForHeader(2, 4, true));

        assertEquals(WzPngFormat.ARGB8888, png.getFormat());
        assertEquals(4, png.getScale());
        assertEquals(8, getIntField(png, "offset"));
    }

    @Test
    void directFormat3UsesCompatibilityScaleTwo() {
        WzPngProperty png = new WzPngProperty("png", null, null);

        png.setData(readerForHeader(3, 0, false));

        assertEquals(WzPngFormat.FORMAT3, png.getFormat());
        assertEquals(1, png.getScale());
    }

    @Test
    void directFormat517UsesCompatibilityScaleFour() {
        WzPngProperty png = new WzPngProperty("png", null, null);

        png.setData(readerForHeader(517, 0, false));

        assertEquals(WzPngFormat.FORMAT517, png.getFormat());
        assertEquals(2, png.getScale());
    }

    @Test
    void oldHeaderLayoutDoesNotMisreadScaleFourAsDxt3() {
        WzPngProperty png = new WzPngProperty("png", null, null);

        png.setData(readerForHeader(2, 4, false));

        assertEquals(WzPngFormat.ARGB8888, png.getFormat());
        assertEquals(4, png.getScale());
        assertEquals(8, getIntField(png, "offset"));
    }

    @Test
    void oldHeaderLayoutDoesNotMisreadScaleEightAsDxt5() {
        WzPngProperty png = new WzPngProperty("png", null, null);

        png.setData(readerForHeader(2, 8, false));

        assertEquals(WzPngFormat.ARGB8888, png.getFormat());
        assertEquals(8, png.getScale());
        assertEquals(8, getIntField(png, "offset"));
    }

    private static orange.wz.provider.tools.BinaryReader readerForHeader(int format1, int format2OrScale, boolean doubleIntFormat) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeCompressedInt(1);
        writer.writeCompressedInt(1);
        writer.writeCompressedInt(format1);
        if (doubleIntFormat) {
            writer.writeCompressedInt(format2OrScale);
        } else {
            writer.putByte((byte) format2OrScale);
        }
        writer.putInt(0);
        writer.putInt(1);
        writer.putByte((byte) 0);
        return new orange.wz.provider.tools.BinaryReader(writer.output());
    }

    private static int getIntField(WzPngProperty png, String name) {
        try {
            Field field = WzPngProperty.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(png);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
