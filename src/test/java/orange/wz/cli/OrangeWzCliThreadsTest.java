package orange.wz.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrangeWzCliThreadsTest {
    @TempDir
    Path tempDir;

    @Test
    void threadsShouldDefaultToOneAndAcceptBoundedValues() throws Exception {
        assertEquals(1, invokeThreads(newRunner("imgs-to-xml", "a.img", "-o", "out")));
        assertEquals(1, invokeThreads(newRunner("imgs-to-xml", "a.img", "-o", "out", "--threads", "1")));
        assertEquals(4, invokeThreads(newRunner("imgs-to-xml", "a.img", "-o", "out", "--threads", "4")));
    }

    @Test
    void threadsShouldRejectInvalidValues() throws Exception {
        assertThreadsError("Invalid --threads value", "0");
        assertThreadsError("Invalid --threads value", "-1");
        assertThreadsError("Invalid --threads value", "abc");
        assertThreadsError("Invalid --threads value", "5");
    }

    @Test
    void parseArgsShouldRequireThreadsValue() throws Exception {
        Exception e = assertThrows(Exception.class, () -> newRunner("imgs-to-xml", "a.img", "-o", "out", "--threads"));
        Throwable cause = e.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
        assertTrue(cause.getMessage().contains("Missing value for option: --threads"));
    }

    @Test
    void helpShouldMentionImgsToXmlAndThreadsScope() throws Exception {
        Method helpText = Class.forName("orange.wz.cli.OrangeWzCli").getDeclaredMethod("helpText");
        helpText.setAccessible(true);

        String text = (String) helpText.invoke(null);

        assertTrue(text.contains("imgs-to-xml"));
        assertTrue(text.contains("--threads"));
        assertTrue(text.contains("Default: 1"));
        assertTrue(text.contains("max: 4"));
        assertTrue(text.contains("only applies to imgs-to-xml"));
    }

    @Test
    void planImgBatchExportsShouldMapInputsIntoOutputDirectory() throws Exception {
        Path first = createFile("Skill.img");
        Path second = createFile("Item.img");
        Path out = tempDir.resolve("out");
        Object runner = newRunner("imgs-to-xml", first.toString(), second.toString(), "-o", out.toString());

        List<?> jobs = invokePlanImgBatchExports(runner);

        assertEquals(2, jobs.size());
        assertEquals(out.resolve("Skill.img.xml"), jobOutput(jobs.get(0)));
        assertEquals(out.resolve("Item.img.xml"), jobOutput(jobs.get(1)));
    }

    @Test
    void planImgBatchExportsShouldRejectOutputNameCollisions() throws Exception {
        Path a = tempDir.resolve("a");
        Path b = tempDir.resolve("b");
        Files.createDirectories(a);
        Files.createDirectories(b);
        Path first = Files.writeString(a.resolve("Skill.img"), "x");
        Path second = Files.writeString(b.resolve("Skill.img"), "x");
        Object runner = newRunner("imgs-to-xml", first.toString(), second.toString(), "-o", tempDir.resolve("out").toString());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> invokePlanImgBatchExports(runner));
        assertTrue(e.getMessage().contains("Duplicate output path"));
    }

    @Test
    void planImgBatchExportsShouldRejectNonImgInputs() throws Exception {
        Path input = createFile("Skill.wz");
        Object runner = newRunner("imgs-to-xml", input.toString(), "-o", tempDir.resolve("out").toString());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> invokePlanImgBatchExports(runner));
        assertTrue(e.getMessage().contains("Expected .img input"));
    }

    @Test
    void runImgBatchExportsShouldBoundConcurrencyAndCollectFailures() throws Exception {
        Path first = createFile("A.img");
        Path second = createFile("B.img");
        Path third = createFile("C.img");
        Object runner = newRunner("imgs-to-xml", first.toString(), second.toString(), third.toString(),
                "-o", tempDir.resolve("out").toString(), "--threads", "2");
        List<?> jobs = invokePlanImgBatchExports(runner);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        Object result = invokeRunImgBatchExports(runner, jobs, 2, (ImgExportWorkerProxy) (input, output) -> {
            int now = active.incrementAndGet();
            maxActive.accumulateAndGet(now, Math::max);
            try {
                Thread.sleep(50);
                if (input.getFileName().toString().equals("B.img")) {
                    throw new IllegalStateException("boom");
                }
            } finally {
                active.decrementAndGet();
            }
        });

        assertEquals(2, invokeResultInt(result, "successCount"));
        assertEquals(1, invokeResultInt(result, "failureCount"));
        assertTrue(maxActive.get() <= 2);
        assertFalse((Boolean) invokeNoArg(result, "isSuccess"));
    }

    private void assertThreadsError(String expected, String value) throws Exception {
        Object runner = newRunner("imgs-to-xml", "a.img", "-o", "out", "--threads", value);
        Method threads = runner.getClass().getDeclaredMethod("threads");
        threads.setAccessible(true);
        Exception e = assertThrows(Exception.class, () -> threads.invoke(runner));
        assertTrue(e.getCause() instanceof IllegalArgumentException);
        assertTrue(e.getCause().getMessage().contains(expected));
    }

    private int invokeThreads(Object runner) throws Exception {
        Method threads = runner.getClass().getDeclaredMethod("threads");
        threads.setAccessible(true);
        return (Integer) threads.invoke(runner);
    }

    @SuppressWarnings("unchecked")
    private List<?> invokePlanImgBatchExports(Object runner) throws Exception {
        Method plan = runner.getClass().getDeclaredMethod("planImgBatchExports");
        plan.setAccessible(true);
        try {
            return (List<?>) plan.invoke(runner);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private Object invokeRunImgBatchExports(Object runner, List<?> jobs, int threads, ImgExportWorkerProxy proxy) throws Exception {
        Class<?> workerClass = Class.forName("orange.wz.cli.OrangeWzCli$ImgExportWorker");
        Object worker = java.lang.reflect.Proxy.newProxyInstance(workerClass.getClassLoader(), new Class<?>[]{workerClass},
                (proxyObject, method, args) -> {
                    proxy.export((Path) args[0], (Path) args[1]);
                    return null;
                });
        Method run = runner.getClass().getDeclaredMethod("runImgBatchExports", List.class, int.class, workerClass);
        run.setAccessible(true);
        return run.invoke(runner, jobs, threads, worker);
    }

    private Path jobOutput(Object job) throws Exception {
        return (Path) invokeNoArg(job, "output");
    }

    private int invokeResultInt(Object result, String methodName) throws Exception {
        return (Integer) invokeNoArg(result, methodName);
    }

    private Object invokeNoArg(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private Path createFile(String name) throws Exception {
        return Files.writeString(tempDir.resolve(name), "x");
    }

    private Object newRunner(String... args) throws Exception {
        Class<?> runnerClass = Class.forName("orange.wz.cli.OrangeWzCli$OrangeWzCliRunner");
        Constructor<?> constructor = runnerClass.getDeclaredConstructor(String[].class);
        constructor.setAccessible(true);
        return constructor.newInstance((Object) args);
    }

    @FunctionalInterface
    private interface ImgExportWorkerProxy {
        void export(Path input, Path output) throws Exception;
    }
}
