package orange.wz.provider.ms;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.WzAESConstant;
import orange.wz.provider.WzImage;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.CryptoConstants;
import orange.wz.provider.tools.FileTool;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
public final class WzMsFile {
    private static final int HEADER_CIPHER_LEN = 9;
    private static final int MAX_SALT_LENGTH = 64;
    private static final int MAX_ENTRY_COUNT = 100_000;
    private static final int MAX_ENTRY_NAME_LENGTH = 4096;

    private WzMsFile() {
    }

    @Getter
    public static final class EntryImage {
        private final String entryName;
        private final WzImage image;
        private final int flags;
        private final int unk1;
        private final int unk2;
        private final byte[] entryKey;

        public EntryImage(String entryName, WzImage image, int flags, int unk1, int unk2, byte[] entryKey) {
            if (entryName == null || entryName.isBlank()) {
                throw new IllegalArgumentException("MS entry name must not be blank");
            }
            if (image == null) {
                throw new IllegalArgumentException("MS entry image must not be null");
            }
            this.entryName = entryName;
            this.image = image;
            this.flags = flags;
            this.unk1 = unk1;
            this.unk2 = unk2;
            this.entryKey = entryKey == null ? null : Arrays.copyOf(entryKey, entryKey.length);
        }

        public byte[] getEntryKey() {
            return entryKey == null ? null : Arrays.copyOf(entryKey, entryKey.length);
        }
    }

    public static List<EntryImage> load(Path msPath, byte[] iv, byte[] userKey) {
        if (msPath == null || !Files.isRegularFile(msPath)) {
            throw new IllegalArgumentException("MS 文件不存在: " + msPath);
        }
        byte[] all = FileTool.readFile(msPath);
        if (all == null || all.length < 32) {
            throw new IllegalArgumentException("MS 文件过小或内容为空: " + msPath);
        }

        try {
            return loadBytes(msPath, all, iv, userKey);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (BufferUnderflowException | IndexOutOfBoundsException | NegativeArraySizeException e) {
            throw new IllegalArgumentException("MS 文件结构损坏或被截断: " + msPath, e);
        }
    }

    private static List<EntryImage> loadBytes(Path msPath, byte[] all, byte[] iv, byte[] userKey) {
        String originalFileName = msPath.getFileName().toString().toLowerCase(Locale.ROOT);
        ByteBuffer bb = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);

        int randByteCount = originalFileName.chars().sum() % WzMsConstants.RAND_BYTE_MOD + WzMsConstants.RAND_BYTE_OFFSET;
        requireRemaining(bb, randByteCount + Integer.BYTES, "MS salt header truncated");
        byte[] randBytes = new byte[randByteCount];
        bb.get(randBytes);

        int hashedSaltLen = bb.getInt();
        int saltLen = (hashedSaltLen & 0xFF) ^ (randBytes[0] & 0xFF);
        if (saltLen <= 0 || saltLen > MAX_SALT_LENGTH || saltLen * 2 > bb.remaining()) {
            throw new IllegalArgumentException("MS salt 长度异常: " + saltLen);
        }
        byte[] saltBytes = new byte[saltLen * 2];
        bb.get(saltBytes);

        char[] saltChars = new char[saltLen];
        for (int i = 0; i < saltLen; i++) {
            saltChars[i] = (char) ((randBytes[i] & 0xFF) ^ (saltBytes[i * 2] & 0xFF));
        }
        String salt = new String(saltChars);
        String fileNameWithSalt = originalFileName + salt;

        int headerStart = bb.position();
        int padAmount = originalFileName.chars().map(v -> v * 3).sum() % WzMsConstants.HEADER_PAD_MOD + WzMsConstants.HEADER_PAD_OFFSET;
        requireRange(all.length, headerStart, HEADER_CIPHER_LEN, "MS header truncated");
        byte[] headerCipher = Arrays.copyOfRange(all, headerStart, headerStart + HEADER_CIPHER_LEN);
        byte[] headerPlain = new Snow2CryptoTransform(deriveSnowKey(fileNameWithSalt, false), null, false).transform(headerCipher);
        ByteBuffer hb = ByteBuffer.wrap(headerPlain).order(ByteOrder.LITTLE_ENDIAN);
        int hash = hb.getInt();
        int version = hb.get() & 0xFF;
        int entryCount = hb.getInt();
        if (version != WzMsConstants.SUPPORTED_VERSION) {
            throw new IllegalArgumentException("不支持的 MS 版本: " + version);
        }
        if (entryCount < 0 || entryCount > MAX_ENTRY_COUNT) {
            throw new IllegalArgumentException("MS entry 数量异常: " + entryCount);
        }

        int checkHash = hashedSaltLen + version + entryCount;
        ByteBuffer sbuf = ByteBuffer.wrap(saltBytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < saltLen; i++) {
            checkHash += sbuf.getShort() & 0xFFFF;
        }
        if (checkHash != hash) {
            throw new IllegalArgumentException("MS 文件头 hash 校验失败，文件名参与解密，改名可能导致失败");
        }

        int entryStart = safeAdd(safeAdd(headerStart, HEADER_CIPHER_LEN, "MS entry start overflow"), padAmount, "MS entry start overflow");
        if (entryStart > all.length) {
            throw new IllegalArgumentException("MS entry table offset out of range: " + entryStart);
        }
        byte[] entryCipherTail = Arrays.copyOfRange(all, entryStart, all.length);
        byte[] entryPlain = new Snow2CryptoTransform(deriveSnowKey(fileNameWithSalt, true), null, false).transform(entryCipherTail);
        ByteBuffer eb = ByteBuffer.wrap(entryPlain).order(ByteOrder.LITTLE_ENDIAN);
        List<WzMsEntry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            requireRemaining(eb, Integer.BYTES, "MS entry name length truncated");
            int nameLen = eb.getInt();
            if (nameLen <= 0 || nameLen > MAX_ENTRY_NAME_LENGTH) {
                throw new IllegalArgumentException("MS entry name length 异常: " + nameLen);
            }
            requireRemaining(eb, nameLen * Character.BYTES + Integer.BYTES * 7 + WzMsConstants.SNOW_KEY_LENGTH,
                    "MS entry table truncated");
            char[] chars = new char[nameLen];
            for (int j = 0; j < nameLen; j++) {
                chars[j] = eb.getChar();
            }
            String entryName = new String(chars);
            int checksum = eb.getInt();
            int flags = eb.getInt();
            int startPos = eb.getInt();
            int size = eb.getInt();
            int sizeAligned = eb.getInt();
            int unk1 = eb.getInt();
            int unk2 = eb.getInt();
            validateEntryMetadata(startPos, size, sizeAligned);
            byte[] entryKey = new byte[WzMsConstants.SNOW_KEY_LENGTH];
            eb.get(entryKey);
            entries.add(new WzMsEntry(entryName, checksum, flags, startPos, size, sizeAligned, unk1, unk2, entryKey));
        }

        int dataStart = alignToPage(safeAdd(entryStart, eb.position(), "MS data start overflow"));
        List<EntryImage> result = new ArrayList<>();
        for (WzMsEntry entry : entries) {
            int entryByteOffset = safeMultiply(entry.getStartPos(), WzMsConstants.BLOCK_ALIGNMENT, "MS entry data offset overflow");
            int start = safeAdd(dataStart, entryByteOffset, "MS entry data offset overflow");
            requireRange(all.length, start, entry.getSizeAligned(), "MS entry data out of range: " + entry.getName());
            byte[] encData = Arrays.copyOfRange(all, start, start + entry.getSizeAligned());
            byte[] imageBytes = decryptData(encData, entry, salt);

            int slash = entry.getName().lastIndexOf('/');
            String imgName = slash >= 0 ? entry.getName().substring(slash + 1) : entry.getName();
            List<String> attemptLogs = new ArrayList<>();
            WzImage image = tryParseImage(imgName, imageBytes, iv, userKey, attemptLogs, "double-pass");
            String decryptMode = "double-pass";
            if (image == null) {
                byte[] singlePassBytes = decryptDataSinglePass(encData, entry, salt);
                image = tryParseImage(imgName, singlePassBytes, iv, userKey, attemptLogs, "single-pass");
                if (image != null) {
                    imageBytes = singlePassBytes;
                    decryptMode = "single-pass";
                }
            }
            if (image == null) {
                log.warn("MS 内部 Img 解析失败，已跳过: {}", imgName);
                log.warn(
                        "MS Img 失败详情 name={} checksum={} flags={} startPos={} size={} sizeAligned={} unk1={} unk2={} decryptMode={} plainHead16={} attempts={}",
                        entry.getName(), entry.getCheckSum(), entry.getFlags(), entry.getStartPos(), entry.getSize(),
                        entry.getSizeAligned(), entry.getUnk1(), entry.getUnk2(), decryptMode, toHex(imageBytes, 16),
                        String.join(" | ", attemptLogs)
                );
                continue;
            }
            result.add(new EntryImage(entry.getName(), image, entry.getFlags(), entry.getUnk1(), entry.getUnk2(), entry.getEntryKey()));
        }
        return result;
    }

    public static Path resolveEntryXmlPath(Path outputDir, String entryName) {
        if (outputDir == null) {
            throw new IllegalArgumentException("Output directory must not be null");
        }
        if (entryName == null || entryName.isBlank()) {
            throw new IllegalArgumentException("MS entry name must not be blank");
        }
        String normalizedName = entryName.replace('\\', '/');
        Path relative = Path.of(normalizedName);
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("MS entry path must be relative: " + entryName);
        }
        for (Path part : relative) {
            String segment = part.toString();
            if (segment.equals("..") || segment.isBlank()) {
                throw new IllegalArgumentException("Unsafe MS entry path: " + entryName);
            }
        }
        Path base = outputDir.toAbsolutePath().normalize();
        Path resolved = base.resolve(normalizedName + ".xml").normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Unsafe MS entry path: " + entryName);
        }
        return resolved;
    }

    private static void requireRemaining(ByteBuffer buffer, int length, String message) {
        if (length < 0 || buffer.remaining() < length) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireRange(int totalLength, int start, int length, String message) {
        if (start < 0 || length < 0 || start > totalLength || length > totalLength - start) {
            throw new IllegalArgumentException(message);
        }
    }

    private static int safeAdd(int a, int b, String message) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private static int safeMultiply(int a, int b, String message) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private static void validateEntryMetadata(int startPos, int size, int sizeAligned) {
        if (startPos < 0 || size < 0 || sizeAligned < 0) {
            throw new IllegalArgumentException("MS entry metadata contains negative values");
        }
        if (size > sizeAligned) {
            throw new IllegalArgumentException("MS entry size exceeds aligned size");
        }
    }

    private static WzImage tryParseImage(String imgName, byte[] imageBytes, byte[] uiIv, byte[] uiUserKey, List<String> attemptLogs, String modeTag) {
        List<NamedBytes> ivCandidates = new ArrayList<>();
        ivCandidates.add(new NamedBytes("WZ_CMS_IV", WzAESConstant.WZ_CMS_IV));
        ivCandidates.add(new NamedBytes("WZ_GMS_IV", WzAESConstant.WZ_GMS_IV));
        ivCandidates.add(new NamedBytes("WZ_LATEST_IV", WzAESConstant.WZ_LATEST_IV));
        ivCandidates.add(new NamedBytes("WZ_MSEA2IV", CryptoConstants.WZ_MSEA2IV));
        if (uiIv != null) {
            ivCandidates.add(new NamedBytes("UI_SELECTED_IV", uiIv));
        }

        List<NamedBytes> keyCandidates = new ArrayList<>();
        keyCandidates.add(new NamedBytes("DEFAULT_KEY", WzAESConstant.DEFAULT_KEY));
        if (uiUserKey != null) {
            keyCandidates.add(new NamedBytes("UI_SELECTED_KEY", uiUserKey));
        }

        for (NamedBytes ivCandidate : ivCandidates) {
            if (ivCandidate.bytes == null) {
                continue;
            }
            for (NamedBytes keyCandidate : keyCandidates) {
                if (keyCandidate.bytes == null) {
                    continue;
                }
                WzImage image = new WzImage(imgName, new BinaryReader(imageBytes, ivCandidate.bytes, keyCandidate.bytes), null);
                image.setDataSize(imageBytes.length);
                image.setOffset(0);
                if (image.parse()) {
                    attemptLogs.add(modeTag + ":" + ivCandidate.name + "+" + keyCandidate.name + ":ok");
                    return image;
                }
                attemptLogs.add(modeTag + ":" + ivCandidate.name + "+" + keyCandidate.name
                        + ":fail(status=" + image.getStatus() + ", msg=" + image.getStatus().getMessage()
                        + ", header=" + toHex(imageBytes, 8) + ", firstByte=" + firstByteHex(imageBytes) + ")");
            }
        }
        return null;
    }

    private record NamedBytes(String name, byte[] bytes) {
    }

    private static String toHex(byte[] data, int maxLen) {
        if (data == null || data.length == 0) {
            return "<empty>";
        }
        int len = Math.min(data.length, maxLen);
        StringBuilder sb = new StringBuilder(len * 3);
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        if (data.length > len) {
            sb.append(" ...");
        }
        return sb.toString();
    }

    private static String firstByteHex(byte[] data) {
        if (data == null || data.length == 0) {
            return "NA";
        }
        return String.format("%02X", data[0] & 0xFF);
    }

    private static int alignToPage(int pos) {
        return (pos + WzMsConstants.PAGE_ALIGNMENT_MASK) & ~WzMsConstants.PAGE_ALIGNMENT_MASK;
    }

    private static byte[] deriveSnowKey(String fileNameWithSalt, boolean isEntryKey) {
        byte[] key = new byte[WzMsConstants.SNOW_KEY_LENGTH];
        int len = fileNameWithSalt.length();
        if (!isEntryKey) {
            for (int i = 0; i < key.length; i++) {
                key[i] = (byte) (fileNameWithSalt.charAt(i % len) + i);
            }
        } else {
            for (int i = 0; i < key.length; i++) {
                key[i] = (byte) (i + (i % 3 + 2) * fileNameWithSalt.charAt(len - 1 - i % len));
            }
        }
        return key;
    }

    private static byte[] deriveImgKey(WzMsEntry entry, String salt) {
        long keyHash = WzMsConstants.INITIAL_KEY_HASH;
        for (char c : salt.toCharArray()) {
            keyHash = (keyHash ^ c) * WzMsConstants.KEY_HASH_MULTIPLIER;
            keyHash &= 0xFFFFFFFFL;
        }
        char[] digitsChars = Long.toUnsignedString(keyHash).toCharArray();
        byte[] digits = new byte[digitsChars.length];
        for (int i = 0; i < digits.length; i++) {
            digits[i] = (byte) (digitsChars[i] - '0');
        }
        byte[] imgKey = new byte[WzMsConstants.SNOW_KEY_LENGTH];
        String entryName = entry.getName();
        byte[] entryKey = entry.getEntryKey();
        for (int i = 0; i < imgKey.length; i++) {
            int digitIdx = i % digits.length;
            int entryKeyIdx = (digits[(i + 2) % digits.length] + i) % entryKey.length;
            int mixed = (digits[digitIdx] % 2) + (entryKey[entryKeyIdx] & 0xFF) + ((digits[(i + 1) % digits.length] + i) % 5);
            imgKey[i] = (byte) (i + entryName.charAt(i % entryName.length()) * mixed);
        }
        return imgKey;
    }

    private static byte[] decryptData(byte[] encryptedBlockAligned, WzMsEntry entry, String salt) {
        byte[] imgKey = deriveImgKey(entry, salt);
        int size = entry.getSize();
        byte[] firstPass = new Snow2CryptoTransform(imgKey, null, false).transform(encryptedBlockAligned);
        byte[] result = new byte[size];
        int firstLen = Math.min(size, WzMsConstants.DOUBLE_ENCRYPT_INITIAL_BYTES);
        int secondInputLen = Math.min(firstPass.length, align4(firstLen));
        byte[] firstPartAligned = new byte[secondInputLen];
        System.arraycopy(firstPass, 0, firstPartAligned, 0, secondInputLen);
        byte[] twice = new Snow2CryptoTransform(imgKey, null, false).transform(firstPartAligned);
        System.arraycopy(twice, 0, result, 0, firstLen);
        if (size > firstLen) {
            System.arraycopy(firstPass, firstLen, result, firstLen, size - firstLen);
        }
        return result;
    }

    private static byte[] decryptDataSinglePass(byte[] encryptedBlockAligned, WzMsEntry entry, String salt) {
        byte[] imgKey = deriveImgKey(entry, salt);
        int size = entry.getSize();
        byte[] firstPass = new Snow2CryptoTransform(imgKey, null, false).transform(encryptedBlockAligned);
        byte[] result = new byte[size];
        System.arraycopy(firstPass, 0, result, 0, size);
        return result;
    }

    private static int align4(int value) {
        return (value + 3) & ~3;
    }
}
