package orange.wz.provider.ms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WzMsEntryTest {
    @Test
    void constructorShouldDefensivelyCopyEntryKey() {
        byte[] key = new byte[WzMsConstants.SNOW_KEY_LENGTH];
        key[0] = 1;
        key[1] = 2;
        key[2] = 3;
        key[3] = 4;
        WzMsEntry entry = new WzMsEntry("Skill/000.img", 10, 20, 30, 40, 50, 60, 70, key);

        key[0] = 99;

        byte[] expected = new byte[WzMsConstants.SNOW_KEY_LENGTH];
        expected[0] = 1;
        expected[1] = 2;
        expected[2] = 3;
        expected[3] = 4;
        assertArrayEquals(expected, entry.getEntryKey());
    }

    @Test
    void getEntryKeyShouldReturnDefensiveCopy() {
        byte[] key = new byte[WzMsConstants.SNOW_KEY_LENGTH];
        key[0] = 5;
        key[1] = 6;
        key[2] = 7;
        WzMsEntry entry = new WzMsEntry("Skill/000.img", 10, 20, 30, 40, 50, 60, 70, key);

        byte[] first = entry.getEntryKey();
        first[1] = 99;
        byte[] second = entry.getEntryKey();

        byte[] expected = new byte[WzMsConstants.SNOW_KEY_LENGTH];
        expected[0] = 5;
        expected[1] = 6;
        expected[2] = 7;
        assertNotSame(first, second);
        assertArrayEquals(expected, second);
    }

    @Test
    void constructorShouldRejectNullOrBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new WzMsEntry(null, 0, 0, 0, 0, 0, 0, 0, new byte[16]));
        assertThrows(IllegalArgumentException.class, () -> new WzMsEntry(" ", 0, 0, 0, 0, 0, 0, 0, new byte[16]));
    }

    @Test
    void constructorShouldRejectInvalidEntryKeySize() {
        assertThrows(IllegalArgumentException.class, () -> new WzMsEntry("a.img", 0, 0, 0, 0, 0, 0, 0, new byte[15]));
    }

    @Test
    void gettersShouldExposeReadOnlyMetadata() {
        WzMsEntry entry = new WzMsEntry("Skill/000.img", 10, 20, 30, 40, 50, 60, 70, new byte[16]);

        assertEquals("Skill/000.img", entry.getName());
        assertEquals(10, entry.getCheckSum());
        assertEquals(20, entry.getFlags());
        assertEquals(30, entry.getStartPos());
        assertEquals(40, entry.getSize());
        assertEquals(50, entry.getSizeAligned());
        assertEquals(60, entry.getUnk1());
        assertEquals(70, entry.getUnk2());
    }
}
