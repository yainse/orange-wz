package orange.wz.manager;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ServerManagerHeadlessTest {
    @Test
    void runShouldNotCreateSwingFrameInHeadlessEnvironment() throws Exception {
        assumeTrue(GraphicsEnvironment.isHeadless(), "This regression test is intended for headless CI/Docker");
        AtomicReference<Throwable> uncaught = new AtomicReference<>();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> uncaught.compareAndSet(null, throwable));
        try {
            ServerManager manager = new ServerManager();

            manager.run(null);
            SwingUtilities.invokeAndWait(() -> {
                // Flush EDT tasks scheduled by ServerManager.run().
            });

            assertNull(uncaught.get(), () -> "ServerManager.run() should not create Swing UI in headless mode: " + uncaught.get());
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous);
        }
    }
}
