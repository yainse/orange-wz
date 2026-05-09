package orange.wz.provider.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

@Getter
@Slf4j
public class WzPngProperty extends WzImageProperty {
    public static class CompressedPngData {
        private final int width;
        private final int height;
        private final WzPngFormat format;
        private final int scale;
        private final boolean listWzUsed;
        private final byte[] compressedBytes;

        public CompressedPngData(int width, int height, WzPngFormat format, int scale,
                                 boolean listWzUsed, byte[] compressedBytes) {
            this.width = width;
            this.height = height;
            this.format = format;
            this.scale = scale;
            this.listWzUsed = listWzUsed;
            this.compressedBytes = compressedBytes == null ? null : Arrays.copyOf(compressedBytes, compressedBytes.length);
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public WzPngFormat getFormat() {
            return format;
        }

        public int getScale() {
            return scale;
        }

        public boolean isListWzUsed() {
            return listWzUsed;
        }

        public byte[] getCompressedBytes() {
            return compressedBytes == null ? null : Arrays.copyOf(compressedBytes, compressedBytes.length);
        }
    }

    private int width;
    private int height;
    private WzPngFormat format;
    private int scale;
    private int offset;
    @Getter(AccessLevel.NONE)
    private byte[] compressedBytes;
    private boolean listWzUsed;
    @Getter(AccessLevel.NONE)
    private BufferedImage image;

    public WzPngProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, WzType.PNG_PROPERTY, parent, wzImage);
    }

    public WzPngProperty(String name, int format, int scale, byte[] imageBytes, WzObject parent, WzImage wzImage) {
        this(name, parent, wzImage);
        this.format = WzPngFormat.getByValue(format);
        this.scale = scale;

        if (imageBytes != null && imageBytes.length > 0) {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                BufferedImage image = ImageIO.read(bis);
                if (image == null) {
                    throw new IOException("无法解码图片数据，可能不是支持的图片格式");
                }
                this.image = image;
                this.width = image.getWidth();
                this.height = image.getHeight();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Setter And Parse ------------------------------------------------------------------------------------------------
    public void setData(BinaryReader reader) {
        width = reader.readCompressedInt();
        height = reader.readCompressedInt();
        readPngFormatAndScale(reader);
        reader.skip(4); // 跳过4个字节
        offset = reader.getPosition();
    }

    private void readPngFormatAndScale(BinaryReader reader) {
        int formatValue = reader.readCompressedInt();
        int afterFormatPosition = reader.getPosition();

        WzPngFormat directFormat = tryGetPngFormat(formatValue);
        if (directFormat != null) {
            format = directFormat;
            scale = reader.getByte();
            normalizeCompatibilityScale();
            return;
        }

        try {
            int format2 = reader.readCompressedInt();
            int combinedFormat = formatValue + (format2 << 8);
            format = WzPngFormat.getByValue(combinedFormat);
            scale = 0;
            normalizeCompatibilityScale();
            return;
        } catch (RuntimeException ignored) {
            reader.setPosition(afterFormatPosition);
        }

        format = WzPngFormat.getByValue(formatValue);
        scale = reader.getByte();
        normalizeCompatibilityScale();
    }

    private WzPngFormat tryGetPngFormat(int value) {
        try {
            return WzPngFormat.getByValue(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void normalizeCompatibilityScale() {
        if (format == WzPngFormat.FORMAT3) {
            scale = 1;
        } else if (format == WzPngFormat.FORMAT517) {
            scale = 2;
        }
    }

    public void setImage(BufferedImage image, WzPngFormat format, int scale) {
        setImage(image, format, scale, Deflater.DEFAULT_COMPRESSION, WzPngZlibCompressMode.DEFAULT);
    }

    public void setImage(BufferedImage image, WzPngFormat format, int scale, int zlibCompressionLevel) {
        setImage(image, format, scale, zlibCompressionLevel, WzPngZlibCompressMode.DEFAULT);
    }

    public void setImage(BufferedImage image, WzPngFormat format, int scale, int zlibCompressionLevel,
                         WzPngZlibCompressMode zlibMode) {
        this.format = format;
        this.scale = scale;
        this.image = image;
        compressImage(zlibCompressionLevel, zlibMode);
    }

    public CompressedPngData exportCompressedData() {
        byte[] bytes = getStoredCompressedBytes(false);
        if (bytes == null) {
            throw new IllegalStateException("没有可导出的 PNG 压缩数据");
        }
        return new CompressedPngData(width, height, format, scale, listWzUsed, bytes);
    }

    public void copyCompressedFrom(CompressedPngData data, boolean keepImageInMem) {
        if (data == null || data.getCompressedBytes() == null) {
            throw new IllegalArgumentException("compressed PNG data 不能为空");
        }
        width = data.getWidth();
        height = data.getHeight();
        format = data.getFormat();
        scale = data.getScale();
        listWzUsed = data.isListWzUsed();
        compressedBytes = data.getCompressedBytes();
        clearImage();
        if (keepImageInMem) {
            parse(true);
        }
    }

    private void parse(boolean saveInMem) {
        byte[] compressedBytes = getCompressedBytes(saveInMem);
        if (compressedBytes == null) {
            log.warn("{} 没有图像数据", getPath());
            return;
        }
        byte[] rawBytes = decompress(compressedBytes);
        image = decodeRawBytes(rawBytes);
    }

    BufferedImage decodeRawBytes(byte[] rawBytes) {
        if (rawBytes.length == 0) {
            throw new RuntimeException("rawBytes 是空的");
        }
        BinaryReader rawBytesReader = new BinaryReader(rawBytes); // rawBytes 是小端序的，用Reader读更方便

        WzPngFormat rawFormat = ImgTool.effectiveRawFormat(format);
        int[] argb32 = new int[width * height];
        int imageType = ImgTool.getBufferImageType(format);
        BufferedImage img = new BufferedImage(width, height, imageType);
        int actualScale = getActualScale();

        switch (rawFormat) {
            case WzPngFormat.ARGB4444:
                if (getActualScale() == 1) {
                    for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                        argb32[i] = ImgTool.Argb32.fromArgb4444(rawBytesReader.getShort());
                    }
                    img.setRGB(0, 0, width, height, argb32, 0, width);
                } else {
                    // 原来的 Format3 (format 1 + scale 2) 实际上几乎没见过这个 // https://forum.ragezone.com/threads/new-wz-png-format-decode-code.1114978/
                    if (width % actualScale != 0 || height % actualScale != 0) {
                        throw new IllegalArgumentException("width 和 height 不能被 scale 整除");
                    }
                    int rawWidth = width / actualScale;
                    int rawHeight = height / actualScale;
                    int[] rawArgb32 = new int[rawWidth * rawHeight];
                    for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                        rawArgb32[i] = ImgTool.Argb32.fromArgb4444(rawBytesReader.getShort());
                    }
                    argb32 = ImgTool.Argb32.upscale(rawArgb32, rawWidth, rawHeight, actualScale);
                    img.setRGB(0, 0, width, height, argb32, 0, width);
                }
                break;
            case WzPngFormat.ARGB8888:
                // UI.wz/UIWindow2.img/MonsterKilling/Count/keyBackgrd/ing
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB8888 + " 不支持 scale");
                }
                for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                    argb32[i] = rawBytesReader.getInt();
                }
                img.setRGB(0, 0, width, height, argb32, 0, width);
                break;
            case WzPngFormat.ARGB1555:
                // "Npc.wz\\2570101.img\\info\\illustration2\\face\\0" // 2570107 is a decent example. Used KMS 353
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB1555 + " 不支持 scale");
                }
                for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                    argb32[i] = ImgTool.Argb32.fromArgb1555(rawBytesReader.getShort());
                }
                img.setRGB(0, 0, width, height, argb32, 0, width);
                break;
            case WzPngFormat.RGB565:
                if (getActualScale() == 1) {
                    // UI.wz/Logo.img v95
                    for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                        argb32[i] = ImgTool.Argb32.fromRgb565(rawBytesReader.getShort());
                    }
                    img.setRGB(0, 0, width, height, argb32, 0, width);
                } else {
                    // 原来的 Format 517 (format 513 + scale 4) // FullPath = "Map.wz\\Back\\midForest.img\\back\\0"
                    if (width % actualScale != 0 || height % actualScale != 0) {
                        throw new IllegalArgumentException("width 和 height 不能被 scale 整除");
                    }
                    int rawWidth = width / actualScale;
                    int rawHeight = height / actualScale;
                    int[] rawArgb32 = new int[rawWidth * rawHeight];
                    for (int i = 0; rawBytesReader.hasRemaining(); i++) {
                        rawArgb32[i] = ImgTool.Argb32.fromRgb565(rawBytesReader.getShort());
                    }
                    argb32 = ImgTool.Argb32.upscale(rawArgb32, rawWidth, rawHeight, actualScale);
                    img.setRGB(0, 0, width, height, argb32, 0, width);
                }
                break;
            case WzPngFormat.DXT3:
                // Familiar_000.wz\9960688.img\attack\info\hit\0
                // Effect_004.wz\Direction17.img\effect\ark\noise\800\0\24
                // Effect_017.wz\EliteMobEff.img\eliteMonster\0\0
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.DXT3 + " 不支持 scale");
                }
                argb32 = ImgTool.Argb32.fromDXT3(rawBytesReader, width, height);
                img.setRGB(0, 0, width, height, argb32, 0, width);
                break;
            case WzPngFormat.DXT5:
                // Skill_022.wz/40002.img/skill/400021006/effect
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.DXT5 + " 不支持 scale");
                }
                argb32 = ImgTool.Argb32.fromDXT5(rawBytesReader, width, height);
                img.setRGB(0, 0, width, height, argb32, 0, width);
                break;
            case WzPngFormat.BC7:
                // CMS220 Character/TamingMob/_Canvas/_Canvas_007.wz/01984266.img/sit/0/tamingMobRear
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.BC7 + " 不支持 scale");
                }
                argb32 = ImgTool.Argb32.fromBC7(rawBytesReader, width & ~3, height & ~3);
                img.setRGB(0, 0, width, height, argb32, 0, width);
                break;
        }

        return img;
    }

    public void clearImage() {
        BufferedImage oldImage = image;
        image = null;
        if (oldImage != null) {
            oldImage.flush();
        }
    }

    /**
     * 丢弃可从原始 WZ reader + offset 重新读取的压缩副本。
     *
     * <p>XML 导入、新建、克隆或编辑后的图片可能只有内存中的 image/compressedBytes，
     * 不能在这些场景释放 compressedBytes。</p>
     */
    public void discardReloadableCompressedCopy() {
        if (compressedBytes != null && offset != 0 && wzImage != null && wzImage.getReader() != null) {
            compressedBytes = null;
        }
    }

    // Getter ----------------------------------------------------------------------------------------------------------
    public BufferedImage getImage(boolean saveInMem) {
        if (image == null) {
            parse(saveInMem);
        }

        return image;
    }

    public byte[] getImageBytes(boolean saveInMem) {
        BufferedImage image = getImage(saveInMem);
        if (image == null) return null;
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", stream);
            return stream.toByteArray();
        } catch (IOException e) {
            log.error("加载图片二进制数据失败 节点: {} 消息: {}", getPath(), e.getMessage());
        }
        return null;
    }

    private int getActualScale() {
        return scale > 0 ? (1 << scale) : 1;
    }

    // Decompress ------------------------------------------------------------------------------------------------------
    private InflaterInputStream createZlibStream(byte[] compressedBytes, WzMutableKey wzMutableKey) {
        // C# CompressionMode.Decompress -> Java InflaterInputStream
        // C# CompressionMode.Compress -> Java DeflaterOutputStream
        InflaterInputStream zlib;

        BinaryReader reader = new BinaryReader(compressedBytes);
        int header = reader.getShort() & 0xFFFF;  // 读取无符号短整型
        // CMS079 header = 0x5E78 是ListWzUsed // acc6.img/folkvillige/moon1/19/0/0
        listWzUsed = header != 0x9C78 && header != 0xDA78 && header != 0x0178;
        if (!listWzUsed) {
            zlib = new InflaterInputStream(new ByteArrayInputStream(compressedBytes));
        } else {
            if (wzMutableKey == null) {
                throw new IllegalStateException("List.wz PNG 解压需要 WZ key");
            }
            reader.setPosition(0);
            BinaryWriter writer = new BinaryWriter();

            while (reader.hasRemaining()) {
                int blockSize = reader.getInt();
                for (int i = 0; i < blockSize; i++) {
                    writer.putByte((byte) (reader.getByte() ^ wzMutableKey.get(i)));
                }
            }
            zlib = new InflaterInputStream(new ByteArrayInputStream(writer.output()));
        }

        return zlib;
    }

    private byte[] decompress(byte[] compressedBytes) {
        WzMutableKey wzMutableKey = wzImage == null || wzImage.getReader() == null ? null : wzImage.getReader().getWzMutableKey();
        int size = ImgTool.getRawByteSize(format, getActualScale(), width, height);
        byte[] rawBytes = new byte[size]; // decompress byte
        // 使用 try-with-resources 确保资源正确关闭
        try (InflaterInputStream zlib = createZlibStream(compressedBytes, wzMutableKey)) {
            // zlib.read(decBuf, 0, uncompressedSize); 可能一次读不完全部数据，要循环确认，所以有了这个方法
            int totalRead = 0;
            int bytesRead;
            while (totalRead < size && (bytesRead = zlib.read(rawBytes, totalRead, size - totalRead)) != -1) {
                totalRead += bytesRead;
            }

            return rawBytes;
        } catch (Exception e) {
            log.error(getPath());
            throw new RuntimeException(e);
        }
    }

    // Compress --------------------------------------------------------------------------------------------------------
    private byte[] getRawBytes(BufferedImage img, WzPngFormat format) {
        int[] argb32 = ImgTool.Argb32.fromBufferedImage(img);
        BinaryWriter writer = new BinaryWriter(false); // 把数据转为小端序
        int actualScale = getActualScale();
        return switch (ImgTool.effectiveRawFormat(format)) {
            case WzPngFormat.ARGB4444 -> {
                if (actualScale > 1) {
                    argb32 = ImgTool.Argb32.downscale(argb32, width, height, actualScale, true);
                }

                for (int v : argb32) {
                    writer.putShort(ImgTool.Argb32.toArgb4444(v));
                }
                yield writer.output();
            }
            case WzPngFormat.ARGB8888 -> {
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB8888 + " 不支持 scale");
                }
                for (int v : argb32) {
                    writer.putInt(v);
                }
                yield writer.output();
            }
            case WzPngFormat.ARGB1555 -> {
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB1555 + " 不支持 scale");
                }
                for (int v : argb32) {
                    writer.putShort(ImgTool.Argb32.toArgb1555(v));
                }
                yield writer.output();
            }
            case WzPngFormat.RGB565 -> {
                if (actualScale > 1) {
                    argb32 = ImgTool.Argb32.downscale(argb32, width, height, actualScale, true);
                }

                for (int v : argb32) {
                    writer.putShort(ImgTool.Argb32.toRgb565(v));
                }
                yield writer.output();
            }
            case WzPngFormat.DXT3 -> {
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB1555 + " 不支持 scale");
                }
                ImgTool.Argb32.toDXT3(img, writer);
                yield writer.output();
            }
            case WzPngFormat.DXT5 -> {
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.ARGB1555 + " 不支持 scale");
                }
                ImgTool.Argb32.toDXT5(img, writer);
                yield writer.output();
            }
            case WzPngFormat.BC7 -> {
                if (actualScale != 1) {
                    throw new IllegalArgumentException(WzPngFormat.BC7 + " 不支持 scale");
                }
                this.format = WzPngFormat.DXT5;
                ImgTool.Argb32.toDXT5(img, writer);
                log.warn("目前还不支持BC7编码, 已降级为 DXT5, 节点: {}", getPath());
                // ImgTool.Argb32.toBC7(img, writer);
                yield writer.output();
            }
            case FORMAT3, FORMAT517 -> throw new IllegalArgumentException(format + " 必须先转换为有效原始格式");
        };
    }

    private byte[] zlibCompress(byte[] rawBytes) {
        return zlibCompress(rawBytes, Deflater.DEFAULT_COMPRESSION, Deflater.DEFAULT_STRATEGY);
    }

    private byte[] zlibCompress(byte[] rawBytes, int level, int strategy) {
        Deflater deflater = new Deflater(normalizeZlibLevel(level), false);
        try {
            deflater.setStrategy(strategy);
        } catch (IllegalArgumentException e) {
            deflater.end();
            if (strategy == Deflater.DEFAULT_STRATEGY) {
                throw e;
            }
            return zlibCompress(rawBytes, level, Deflater.DEFAULT_STRATEGY);
        }

        ByteArrayOutputStream memStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream zip = new DeflaterOutputStream(memStream, deflater)) {
            zip.write(rawBytes);
        } catch (IOException e) {
            throw new RuntimeException("压缩失败", e);
        } finally {
            deflater.end();
        }

        return memStream.toByteArray();
    }

    private int normalizeZlibLevel(int level) {
        return Math.max(Deflater.NO_COMPRESSION, Math.min(Deflater.BEST_COMPRESSION, level));
    }

    private byte[] zlibCompressSmallest(byte[] rawBytes, int level) {
        byte[] best = null;
        for (int strategy : WzPngZlibCompressMode.strategiesForBrute()) {
            byte[] candidate = zlibCompress(rawBytes, level, strategy);
            if (best == null || candidate.length < best.length) {
                best = candidate;
            }
        }
        return best;
    }

    private byte[] zlibCompressOnly(byte[] rawBytes, int level, WzPngZlibCompressMode zlibMode) {
        WzPngZlibCompressMode mode = zlibMode == null ? WzPngZlibCompressMode.DEFAULT : zlibMode;
        if (mode.brutePickSmallest()) {
            return zlibCompressSmallest(rawBytes, level);
        }
        return zlibCompress(rawBytes, level, mode.deflaterStrategy());
    }

    public void compressImage() {
        compressImage(Deflater.DEFAULT_COMPRESSION, WzPngZlibCompressMode.DEFAULT);
    }

    public void compressImage(int zlibLevel, WzPngZlibCompressMode zlibMode) {
        WzMutableKey wzMutableKey = wzImage == null || wzImage.getReader() == null ? null : wzImage.getReader().getWzMutableKey();
        width = image.getWidth();
        height = image.getHeight();

        byte[] rawBytes = getRawBytes(image, format);
        compressBytes(rawBytes, wzMutableKey, zlibLevel, zlibMode);

    }

    private void compressBytes(byte[] rawBytes, WzMutableKey wzMutableKey) {
        compressBytes(rawBytes, wzMutableKey, Deflater.DEFAULT_COMPRESSION, WzPngZlibCompressMode.DEFAULT);
    }

    private void compressBytes(byte[] rawBytes, WzMutableKey wzMutableKey, int zlibLevel, WzPngZlibCompressMode zlibMode) {
        compressedBytes = zlibCompressOnly(rawBytes, zlibLevel, zlibMode);
        if (listWzUsed) {
            BinaryWriter writer = new BinaryWriter(compressedBytes);
            if (wzMutableKey == null) {
                throw new IllegalStateException("List.wz PNG 重新压缩需要 WZ key");
            }
            writer.setWzMutableKey(wzMutableKey);
            writer.putInt(2);
            for (int i = 0; i < 2; i++) {
                writer.putByte((byte) (compressedBytes[i] ^ wzMutableKey.get(i)));
            }
            writer.putInt(compressedBytes.length - 2);
            for (int i = 2; i < compressedBytes.length; i++)
                writer.putByte((byte) (compressedBytes[i] ^ wzMutableKey.get(i - 2)));
            compressedBytes = writer.output();
        }
    }

    public byte[] getCompressedBytes(boolean saveInMem) {
        byte[] bytes = resolveCompressedBytes(saveInMem);
        return bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
    }

    private byte[] getStoredCompressedBytes(boolean saveInMem) {
        return resolveCompressedBytes(saveInMem);
    }

    private byte[] resolveCompressedBytes(boolean saveInMem) {
        if (compressedBytes == null) {
            byte[] returnBytes = null;
            if (offset != 0) {
                BinaryReader reader = wzImage.getReader();
                int curOffset = reader.getPosition();
                reader.setPosition(offset);
                int len = reader.getInt() - 1;
                reader.skip(1); // 跳过1个字节
                if (len > 0) {
                    returnBytes = reader.getBytes(len);
                }
                reader.setPosition(curOffset);

                if (saveInMem) {
                    compressedBytes = returnBytes;
                }
            } else if (image != null) {
                compressImage();
                returnBytes = compressedBytes;
                if (!saveInMem) {
                    compressedBytes = null;
                }
            }
            return returnBytes;
        }
        return compressedBytes;
    }

    public void rebuildCompressedBytesUseNewWzKey(WzMutableKey wzMutableKey) {
        // 该方法在处理CMS079的Map.wz时要额外花费123秒，只是为了List.wz的图片
        byte[] compressedBytes = getStoredCompressedBytes(false);
        byte[] rawBytes = decompress(compressedBytes);
        if (listWzUsed) {
            compressBytes(rawBytes, wzMutableKey);
        }
    }

    private static BufferedImage deepClone(BufferedImage src) {
        ColorModel cm = src.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = src.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    // Override --------------------------------------------------------------------------------------------------------
    @Override
    public void writeValue(BinaryWriter writer) {
        throw new RuntimeException("WzPngProperty writeValue不能单独调用");
    }

    @Override
    public WzPngProperty deepClone(WzObject parent) {
        WzPngProperty clone = new WzPngProperty(name, parent, null);
        clone.width = width;
        clone.height = height;
        clone.format = format;
        clone.scale = scale;
        clone.listWzUsed = false;
        // clone.compressedBytes = Arrays.copyOf(compressedBytes, compressedBytes.length); // 这个需要用新密钥重新压缩
        clone.image = deepClone(getImage(false));

        return clone;
    }
}
