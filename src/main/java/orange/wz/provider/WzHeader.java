package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class WzHeader {
    private String signature;
    private String copyright;
    private long fileSize;
    private int dataStartPos;

    private short fileVersion; // 外部指定的版本号
    private short encVersion; // 从文件中读取的版本号
    private int versionHash = 0; // uint

    public WzHeader(short fileVersion) {
        this.fileVersion = fileVersion;
    }

    public static WzHeader getDefault(short fileVersion) {
        WzHeader header = new WzHeader(fileVersion);
        header.signature = "PKG1";
        header.copyright = "Package file v1.0 Copyright 2002 Wizet, ZMS";
        header.fileSize = 0;
        header.dataStartPos = 60;
        header.createVersionHash();

        return header;
    }

    public int checkAndGetVersionHash(short checkEncVersion, short fileVersion) {
        int versionHash = calcVersionHash(fileVersion);

        if (checkEncVersion == WzFile.verHeader64BitStart) return versionHash; // 永远是 59192

        int encVersion = encryptVersionHash(versionHash);

        if (checkEncVersion == encVersion) {
            return versionHash;
        } else {
            return 0;
        }
    }

    private void createVersionHash() {
        versionHash = calcVersionHash(fileVersion);
        encVersion = encryptVersionHash(versionHash);
    }

    private int calcVersionHash(short fileVersion) {
        int versionHash = 0;
        for (final byte c : String.valueOf(fileVersion).getBytes()) {
            versionHash = (versionHash * 32) + c + 1;
        }

        return versionHash;
    }

    private short encryptVersionHash(int versionHash) {
        return (short) (0xFF
                ^ ((versionHash >> 24) & 0xFF)
                ^ ((versionHash >> 16) & 0xFF)
                ^ ((versionHash >> 8) & 0xFF)
                ^ (versionHash & 0xFF));
    }
}
