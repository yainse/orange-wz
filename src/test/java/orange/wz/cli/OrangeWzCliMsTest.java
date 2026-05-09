package orange.wz.cli;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrangeWzCliMsTest {
    @Test
    void detectTypeShouldRecognizeMsFiles() throws Exception {
        Object runner = newRunner("info", "input.ms");
        Method detectType = runner.getClass().getDeclaredMethod("detectType", Path.class);
        detectType.setAccessible(true);

        assertEquals("ms", detectType.invoke(runner, Path.of("input.ms")));
    }

    @Test
    void helpShouldMentionMsReadOnlyCommands() throws Exception {
        Object runner = newRunner("--help");
        Method helpText = Class.forName("orange.wz.cli.OrangeWzCli").getDeclaredMethod("helpText");
        helpText.setAccessible(true);

        String text = (String) helpText.invoke(null);

        assertTrue(text.contains("path.img|path.wz|path.ms"));
        assertTrue(text.contains("ms-to-xml"));
        assertTrue(text.contains("Read-only"));
    }

    private Object newRunner(String... args) throws Exception {
        Class<?> runnerClass = Class.forName("orange.wz.cli.OrangeWzCli$OrangeWzCliRunner");
        Constructor<?> constructor = runnerClass.getDeclaredConstructor(String[].class);
        constructor.setAccessible(true);
        return constructor.newInstance((Object) args);
    }
}
