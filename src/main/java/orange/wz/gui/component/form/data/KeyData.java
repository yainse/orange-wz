package orange.wz.gui.component.form.data;

import lombok.Getter;

@Getter
public final class KeyData {
    private final short version;
    private final byte[] iv;
    private final byte[] key;

    public KeyData(short version, byte[] iv, byte[] key) {
        this.version = version;
        this.iv = iv;
        this.key = key;
    }
}
