package orange.wz.provider.properties;

import orange.wz.provider.tools.BinaryReader;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class WzPngPropertyMemoryTest {
    @Test
    void clearImageDropsDecodedImageReference() throws Exception {
        WzPngProperty png = new WzPngProperty("png", null, null);
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        setField(png, "image", image);

        png.clearImage();

        assertNull(getField(png, "image"));
    }

    @Test
    void discardReloadableCompressedCopyKeepsBytesWhenThereIsNoReaderOffset() throws Exception {
        WzPngProperty png = new WzPngProperty("png", null, null);
        byte[] bytes = new byte[]{1, 2, 3};
        setField(png, "compressedBytes", bytes);
        setField(png, "offset", 0);

        png.discardReloadableCompressedCopy();

        assertSame(bytes, getField(png, "compressedBytes"));
    }

    @Test
    void discardReloadableCompressedCopyDropsBytesWhenReaderOffsetCanReloadThem() throws Exception {
        WzPngProperty png = new WzPngProperty("png", null, null);
        byte[] bytes = new byte[]{1, 2, 3};
        setField(png, "compressedBytes", bytes);
        setField(png, "offset", 4);
        setField(png, "wzImage", new TestImage(new BinaryReader(new byte[16])));

        png.discardReloadableCompressedCopy();

        assertNull(getField(png, "compressedBytes"));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
