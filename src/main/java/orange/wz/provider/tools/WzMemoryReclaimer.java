package orange.wz.provider.tools;

import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.WzCanvasProperty;

import java.util.List;

/**
 * Provider-only memory reclamation helpers for headless/batch workflows.
 *
 * <p>Do not clear children, parent links, or WZ readers here. This utility only
 * discards decoded/reloadable graphic caches and is safe to call after export.</p>
 */
public final class WzMemoryReclaimer {
    private WzMemoryReclaimer() {
    }

    public static void discardDecodedImages(WzImage image) {
        if (image == null) return;
        discardDecodedImages(image.getChildren());
    }

    public static void discardDecodedImages(WzImageProperty property) {
        if (property == null) return;
        if (property instanceof WzCanvasProperty canvas) {
            canvas.discardHeavyGraphicCaches();
        }
        discardDecodedImages(property.getChildren());
    }

    public static void discardDecodedImages(List<WzImageProperty> properties) {
        if (properties == null || properties.isEmpty()) return;
        for (WzImageProperty property : properties) {
            discardDecodedImages(property);
        }
    }

    public static void discardDecodedImages(WzObject root) {
        switch (root) {
            case null -> {
            }
            case WzImage image -> discardDecodedImages(image);
            case WzImageProperty property -> discardDecodedImages(property);
            default -> {
            }
        }
    }
}
