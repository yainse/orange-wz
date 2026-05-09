package orange.wz.provider.properties;

import orange.wz.provider.tools.BinaryWriter;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class WzPngPropertyCompatibilityImageTest {
    @Test
    void format3DecodesAsScaledArgb4444() throws Exception {
        WzPngProperty png = new WzPngProperty("png", null, null);
        setField(png, "width", 2);
        setField(png, "height", 2);
        setField(png, "format", WzPngFormat.FORMAT3);
        setField(png, "scale", 1);

        BinaryWriter raw = new BinaryWriter();
        raw.putShort(orange.wz.provider.tools.ImgTool.Argb32.toArgb4444(0xFFFF0000));
        BufferedImage image = png.decodeRawBytes(raw.output());
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(0xFFFF0000, image.getRGB(0, 0));
        assertEquals(0xFFFF0000, image.getRGB(1, 1));
    }

    @Test
    void format517DecodesAsScaledRgb565() throws Exception {
        WzPngProperty png = new WzPngProperty("png", null, null);
        setField(png, "width", 4);
        setField(png, "height", 4);
        setField(png, "format", WzPngFormat.FORMAT517);
        setField(png, "scale", 2);

        BinaryWriter raw = new BinaryWriter();
        raw.putShort(orange.wz.provider.tools.ImgTool.Argb32.toRgb565(0xFF00FF00));
        BufferedImage image = png.decodeRawBytes(raw.output());
        assertEquals(4, image.getWidth());
        assertEquals(4, image.getHeight());
        assertEquals(0xFF00FF00, image.getRGB(0, 0));
        assertEquals(0xFF00FF00, image.getRGB(3, 3));
    }

    private static void setField(WzPngProperty png, String name, Object value) throws Exception {
        Field field = WzPngProperty.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(png, value);
    }
}
