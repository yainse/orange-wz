package orange.wz.provider.properties;

import java.util.zip.Deflater;

/**
 * Provider-level PNG write/compression options for XML import and CLI save paths.
 *
 * <p>This value object is headless-safe and only wraps JDK zlib/provider settings.</p>
 */
public record WzPngWriteOptions(int zlibLevel, WzPngZlibCompressMode zlibMode) {
    public WzPngWriteOptions {
        if (zlibLevel != Deflater.DEFAULT_COMPRESSION
                && (zlibLevel < Deflater.NO_COMPRESSION || zlibLevel > Deflater.BEST_COMPRESSION)) {
            throw new IllegalArgumentException("zlibLevel must be -1 or 0..9");
        }
        if (zlibMode == null) {
            zlibMode = WzPngZlibCompressMode.DEFAULT;
        }
    }

    public static WzPngWriteOptions defaults() {
        return new WzPngWriteOptions(Deflater.DEFAULT_COMPRESSION, WzPngZlibCompressMode.DEFAULT);
    }
}
