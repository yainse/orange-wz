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

    public void verifiedFileVersion() {
        int versionHash = calcVersionHash(fileVersion);
        int encVersion = encryptVersionHash(versionHash);

        if (this.encVersion == encVersion) {
            this.versionHash = versionHash;
        } else {
            throw new RuntimeException("文件版本错误");
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
