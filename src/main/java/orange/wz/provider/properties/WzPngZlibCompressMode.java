package orange.wz.provider.properties;

import java.util.zip.Deflater;

/**
 * WZ PNG 像素块 zlib 压缩策略。
 *
 * <p>只影响 PNG 原始像素块的 zlib payload；不会引入 GUI 或 native 压缩依赖。</p>
 */
public enum WzPngZlibCompressMode {
    /** 默认策略，兼容旧行为。 */
    DEFAULT(Deflater.DEFAULT_STRATEGY, false),
    /** 对渐变、平滑区域可能更小。 */
    FILTERED(Deflater.FILTERED, false),
    /** 仅哈夫曼编码。 */
    HUFFMAN_ONLY(Deflater.HUFFMAN_ONLY, false),
    /** JDK 常量未在所有版本暴露，zlib strategy value 为 3。 */
    RLE(3, false),
    /** 逐一尝试 DEFAULT/FILTERED/HUFFMAN_ONLY/RLE，取最小 zlib 输出。 */
    BRUTE_SMALLEST(Deflater.DEFAULT_STRATEGY, true);

    private final int deflaterStrategy;
    private final boolean brutePickSmallest;

    WzPngZlibCompressMode(int deflaterStrategy, boolean brutePickSmallest) {
        this.deflaterStrategy = deflaterStrategy;
        this.brutePickSmallest = brutePickSmallest;
    }

    public int deflaterStrategy() {
        return deflaterStrategy;
    }

    public boolean brutePickSmallest() {
        return brutePickSmallest;
    }

    static int[] strategiesForBrute() {
        return new int[]{
                Deflater.DEFAULT_STRATEGY,
                Deflater.FILTERED,
                Deflater.HUFFMAN_ONLY,
                3,
        };
    }
}
