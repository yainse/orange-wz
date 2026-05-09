package orange.wz.provider.ms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WzMsFileTest {
    @TempDir
    Path tempDir;

    @Test
    void loadShouldRejectMissingFile() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> WzMsFile.load(tempDir.resolve("missing.ms"), new byte[4], new byte[32]));

        assertTrue(ex.getMessage().contains("MS 文件不存在"));
    }

    @Test
    void loadShouldRejectTooSmallFile() throws Exception {
        Path ms = tempDir.resolve("sample.ms");
        Files.write(ms, new byte[31]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> WzMsFile.load(ms, new byte[4], new byte[32]));

        assertTrue(ex.getMessage().contains("MS 文件过小"));
    }

    @Test
    void loadShouldRejectInvalidSaltLengthBeforeAllocatingHugeArrays() throws Exception {
        String fileName = "sample.ms";
        int randByteCount = fileName.chars().sum() % WzMsConstants.RAND_BYTE_MOD + WzMsConstants.RAND_BYTE_OFFSET;
        byte[] bytes = new byte[Math.max(64, randByteCount + 4)];
        bytes[0] = 0;
        bytes[randByteCount] = (byte) 250;
        Path ms = tempDir.resolve(fileName);
        Files.write(ms, bytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> WzMsFile.load(ms, new byte[4], new byte[32]));

        assertTrue(ex.getMessage().contains("salt 长度异常"));
    }

    @Test
    void safeResolveEntryXmlPathShouldRejectPathTraversal() {
        assertThrows(IllegalArgumentException.class,
                () -> WzMsFile.resolveEntryXmlPath(tempDir, "../evil.img"));
    }

    @Test
    void safeResolveEntryXmlPathShouldRejectAbsolutePath() {
        assertThrows(IllegalArgumentException.class,
                () -> WzMsFile.resolveEntryXmlPath(tempDir, "/tmp/evil.img"));
    }

    @Test
    void safeResolveEntryXmlPathShouldPreserveRelativeEntryStructure() {
        Path resolved = WzMsFile.resolveEntryXmlPath(tempDir, "Skill/000.img");

        assertTrue(resolved.startsWith(tempDir.toAbsolutePath().normalize()));
        assertTrue(resolved.toString().endsWith("Skill/000.img.xml"));
        assertFalse(resolved.toString().contains(".."));
    }
}
