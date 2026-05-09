package orange.wz.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WzMsImageFileTest {
    @TempDir
    Path tempDir;

    @Test
    void saveShouldBeUnsupportedAndLeaveFileUntouched() throws Exception {
        Path ms = tempDir.resolve("sample.ms");
        byte[] original = new byte[64];
        original[0] = 42;
        Files.write(ms, original);
        WzMsImageFile file = new WzMsImageFile("sample.ms", ms.toString(), "gms", new byte[4], new byte[32]);

        assertFalse(file.save());

        byte[] after = Files.readAllBytes(ms);
        org.junit.jupiter.api.Assertions.assertArrayEquals(original, after);
        assertFalse(Files.exists(tempDir.resolve("sample.ms.bak")));
    }

    @Test
    void saveAsWzShouldBeUnsupported() {
        WzMsImageFile file = new WzMsImageFile("sample.ms", tempDir.resolve("sample.ms").toString(), "gms", new byte[4], new byte[32]);

        assertThrows(UnsupportedOperationException.class, () -> file.saveAsWz(tempDir.resolve("out.wz")));
    }
}
