package orange.wz.provider.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryModeTest {
    @Test
    void blankOrNullDefaultsToNormal() {
        assertEquals(MemoryMode.NORMAL, MemoryMode.fromCliValue(null));
        assertEquals(MemoryMode.NORMAL, MemoryMode.fromCliValue(""));
        assertEquals(MemoryMode.NORMAL, MemoryMode.fromCliValue("  "));
    }

    @Test
    void parsesNormalAndLowCaseInsensitively() {
        assertEquals(MemoryMode.NORMAL, MemoryMode.fromCliValue("normal"));
        assertEquals(MemoryMode.LOW, MemoryMode.fromCliValue("LOW"));
        assertEquals(MemoryMode.LOW, MemoryMode.fromCliValue(" low "));
    }

    @Test
    void rejectsUnknownValues() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> MemoryMode.fromCliValue("tiny"));
        assertTrue(error.getMessage().contains("Invalid --memory-mode value"));
    }
}
