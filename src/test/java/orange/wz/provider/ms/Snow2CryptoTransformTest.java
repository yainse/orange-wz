package orange.wz.provider.ms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Snow2CryptoTransformTest {
    @Test
    void encryptThenDecryptShouldRestoreBytesWith16ByteKey() {
        byte[] key = new byte[16];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (i * 7 + 3);
        }
        byte[] plain = new byte[1025];
        for (int i = 0; i < plain.length; i++) {
            plain[i] = (byte) (i * 13 + 11);
        }

        byte[] encrypted = new Snow2CryptoTransform(key, null, true).transform(plain);
        byte[] decrypted = new Snow2CryptoTransform(key, null, false).transform(encrypted);

        assertArrayEquals(plain, decrypted);
    }

    @Test
    void encryptThenDecryptShouldRestoreBytesWith32ByteKeyAndIv() {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (255 - i * 5);
        }
        byte[] iv = new byte[]{1, 2, 3, 4};
        byte[] plain = new byte[]{9, 8, 7, 6, 5, 4, 3};

        byte[] encrypted = new Snow2CryptoTransform(key, iv, true).transform(plain);
        byte[] decrypted = new Snow2CryptoTransform(key, iv, false).transform(encrypted);

        assertArrayEquals(plain, decrypted);
    }

    @Test
    void constructorShouldRejectInvalidKeySize() {
        assertThrows(IllegalArgumentException.class, () -> new Snow2CryptoTransform(new byte[15], null, true));
    }

    @Test
    void constructorShouldRejectInvalidIvSize() {
        assertThrows(IllegalArgumentException.class, () -> new Snow2CryptoTransform(new byte[16], new byte[3], true));
    }
}
