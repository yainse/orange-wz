package orange.wz.provider.tools;

import lombok.extern.slf4j.Slf4j;
import orange.wz.model.Pair;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
@EnableScheduling
public class FileWriteQueue {

    // 线程安全队列，存放待写入任务
    private final ConcurrentLinkedQueue<Pair<Path, byte[]>> writeQueue = new ConcurrentLinkedQueue<>();

    /**
     * 向队列添加写入任务
     */
    public void addToQueue(Path path, byte[] data) {
        if (path == null || data == null) {
            throw new IllegalArgumentException("path and data cannot be null");
        }
        writeQueue.add(new Pair<>(path, data));
    }

    /**
     * 定时任务，每秒轮询一次队列写文件
     */
    @Scheduled(fixedDelay = 1000)
    public void flushQueue() {
        // 临时列表，存放当前批次任务
        List<Pair<Path, byte[]>> batch = new ArrayList<>();

        // 批量取出队列里的任务
        Pair<Path, byte[]> task;
        while ((task = writeQueue.poll()) != null) {
            batch.add(task);
        }

        // 遍历批量任务写入文件
        if (!batch.isEmpty()) {
            System.gc();
        }

        for (Pair<Path, byte[]> pair : batch) {
            try {
                Path path = pair.getLeft();
                byte[] data = pair.getRight();

                // 创建父目录（如果不存在）
                if (Files.notExists(path.getParent())) {
                    Files.createDirectories(path.getParent());
                }

                // 写入文件
                Files.write(path, data);
                log.info("{} 已保存", path);
            } catch (IOException e) {
                // 写入失败，重新入队
                writeQueue.add(pair);
                log.error("无法保存文件 {}", e.getMessage());
            }
        }
    }
}
