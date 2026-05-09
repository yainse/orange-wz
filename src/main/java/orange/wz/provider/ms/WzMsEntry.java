package orange.wz.provider.ms;

import lombok.Getter;

import java.util.Arrays;

@Getter
public class WzMsEntry {
    private final String name;
    private final int checkSum;
    private final int flags;
    private final int startPos;
    private final int size;
    private final int sizeAligned;
    private final int unk1;
    private final int unk2;
    private final byte[] entryKey;

    public WzMsEntry(String name, int checkSum, int flags, int startPos, int size, int sizeAligned, int unk1, int unk2, byte[] entryKey) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("MS entry name must not be blank");
        }
        if (entryKey == null || entryKey.length != WzMsConstants.SNOW_KEY_LENGTH) {
            throw new IllegalArgumentException("MS entry key size must be " + WzMsConstants.SNOW_KEY_LENGTH + " bytes");
        }
        this.name = name;
        this.checkSum = checkSum;
        this.flags = flags;
        this.startPos = startPos;
        this.size = size;
        this.sizeAligned = sizeAligned;
        this.unk1 = unk1;
        this.unk2 = unk2;
        this.entryKey = Arrays.copyOf(entryKey, entryKey.length);
    }

    public byte[] getEntryKey() {
        return Arrays.copyOf(entryKey, entryKey.length);
    }
}
