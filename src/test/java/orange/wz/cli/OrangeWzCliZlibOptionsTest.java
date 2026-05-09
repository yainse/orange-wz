package orange.wz.cli;

import orange.wz.provider.properties.WzPngZlibCompressMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class OrangeWzCliZlibOptionsTest {
    @Test
    void helpShouldMentionXmlToImgZlibOptionsAndScope() throws Exception {
        Method helpText = Class.forName("orange.wz.cli.OrangeWzCli").getDeclaredMethod("helpText");
        helpText.setAccessible(true);

        String text = (String) helpText.invoke(null);

        assertTrue(text.contains("--zlib-level"));
        assertTrue(text.contains("--zlib-mode"));
        assertTrue(text.contains("brute_smallest"));
        assertTrue(text.contains("only applies to xml-to-img"));
    }

    @Test
    void zlibLevelShouldDefaultToDeflaterDefaultCompression() throws Exception {
        Object runner = newRunner("xml-to-img", "in.xml", "-o", "out.img");
        assertEquals(-1, invokeZlibLevel(runner));
    }

    @Test
    void zlibLevelShouldAcceptMinusOneAndZeroThroughNine() throws Exception {
        assertEquals(-1, invokeZlibLevel(newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-level", "-1")));
        assertEquals(0, invokeZlibLevel(newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-level", "0")));
        assertEquals(9, invokeZlibLevel(newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-level", "9")));
    }

    @Test
    void zlibLevelShouldRejectOutOfRangeOrNonIntegerValues() throws Exception {
        assertZlibLevelRejected("-2");
        assertZlibLevelRejected("10");
        assertZlibLevelRejected("abc");
    }

    @Test
    void zlibModeShouldDefaultToDefaultAndAcceptSupportedNamesCaseInsensitively() throws Exception {
        assertEquals(WzPngZlibCompressMode.DEFAULT, invokeZlibMode(newRunner("xml-to-img", "in.xml", "-o", "out.img")));
        assertEquals(WzPngZlibCompressMode.DEFAULT, invokeZlibMode(newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-mode", "default")));
        assertEquals(WzPngZlibCompressMode.FILTERED, invokeZlibMode(newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-mode", "filtered")));
        assertEquals(WzPngZlibCompressMode.HUFFMAN_ONLY, invokeZlibMode(newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-mode", "huffman_only")));
        assertEquals(WzPngZlibCompressMode.RLE, invokeZlibMode(newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-mode", "rLe")));
        assertEquals(WzPngZlibCompressMode.BRUTE_SMALLEST, invokeZlibMode(newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-mode", "brute_smallest")));
    }

    @Test
    void zlibModeShouldRejectUnknownValues() throws Exception {
        Object runner = newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-mode", "best");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> invokeZlibMode(runner));

        assertTrue(ex.getMessage().contains("Invalid --zlib-mode value"));
        assertTrue(ex.getMessage().contains("default, filtered, huffman_only, rle, brute_smallest"));
    }

    @Test
    void parseArgsShouldAcceptZlibOptions() throws Exception {
        assertDoesNotThrow(() -> newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-level", "9", "--zlib-mode", "filtered"));
        assertDoesNotThrow(() -> newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-level", "-1", "--zlib-mode", "default"));
    }

    private static Object newRunner(String... args) throws Exception {
        Class<?> runnerClass = Class.forName("orange.wz.cli.OrangeWzCli$OrangeWzCliRunner");
        Constructor<?> constructor = runnerClass.getDeclaredConstructor(String[].class);
        constructor.setAccessible(true);
        return constructor.newInstance((Object) args);
    }

    private static int invokeZlibLevel(Object runner) throws Exception {
        Method method = runner.getClass().getDeclaredMethod("zlibLevel");
        method.setAccessible(true);
        return (int) invoke(method, runner);
    }

    private static WzPngZlibCompressMode invokeZlibMode(Object runner) throws Exception {
        Method method = runner.getClass().getDeclaredMethod("zlibMode");
        method.setAccessible(true);
        return (WzPngZlibCompressMode) invoke(method, runner);
    }

    private static Object invoke(Method method, Object target) throws Exception {
        try {
            return method.invoke(target);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private static void assertZlibLevelRejected(String value) throws Exception {
        Object runner = newRunner("xml-to-img", "in.xml", "-o", "out.img", "--zlib-level", value);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> invokeZlibLevel(runner));
        assertTrue(ex.getMessage().contains("Invalid --zlib-level value"));
        assertTrue(ex.getMessage().contains("expected -1 or 0..9"));
    }
}
