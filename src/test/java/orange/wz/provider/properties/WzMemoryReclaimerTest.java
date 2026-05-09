package orange.wz.provider.properties;

import orange.wz.provider.WzImageProperty;
import orange.wz.provider.tools.WzMemoryReclaimer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WzMemoryReclaimerTest {
    @Test
    void reclaimRecursivelyDropsCanvasDecodedImagesWithoutClearingTree() throws Exception {
        TestImage image = new TestImage(null);
        WzListProperty root = new WzListProperty("root", image, image);
        WzCanvasProperty canvas = new WzCanvasProperty("canvas", root, image);
        canvas.initPngProperty("canvas", canvas, image);
        setField(getField(canvas, "png"), "image", new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
        root.addChild(canvas);
        image.addChild(root);

        WzMemoryReclaimer.discardDecodedImages(image);

        assertEquals(1, image.getChildren().size());
        assertEquals(1, root.getChildren().size());
        assertSame(canvas, root.getChildren().getFirst());
        assertNull(getField(getField(canvas, "png"), "image"));
    }

    @Test
    void canvasDiscardHeavyGraphicCachesDoesNotDropPngProperty() throws Exception {
        TestImage image = new TestImage(null);
        WzCanvasProperty canvas = new WzCanvasProperty("canvas", null, image);
        canvas.initPngProperty("canvas", canvas, image);
        Object png = getField(canvas, "png");
        setField(png, "width", 2);
        setField(png, "height", 3);
        setField(png, "image", new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));

        canvas.discardHeavyGraphicCaches();

        assertSame(png, getField(canvas, "png"));
        assertEquals(2, canvas.getWidth());
        assertEquals(3, canvas.getHeight());
        assertNull(getField(png, "image"));
    }

    @SuppressWarnings("unchecked")
    private static List<WzImageProperty> children(WzImageProperty property) {
        return property.getChildren();
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
