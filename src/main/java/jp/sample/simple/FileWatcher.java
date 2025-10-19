package jp.sample.simple;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class FileWatcher {
    
    private WatchService watchService;
    private Path watchPath;
    private BlockingQueue<Task> taskQueue;
    private Thread watcherThread;
    private volatile boolean isRunning = false;

    public FileWatcher(String directoryPath, BlockingQueue<Task> taskQueue) throws Exception {
        this.taskQueue = taskQueue;
        this.watchPath = Paths.get(directoryPath);
        this.watchService = FileSystems.getDefault().newWatchService();
        this.watchPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
    }

    public void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        watcherThread = new Thread(this::watchLoop);
        watcherThread.setName("FileWatcher-Thread");
        watcherThread.start();
    }

    public void stop() throws IOException, InterruptedException {
        isRunning = false;
        if (watcherThread != null && watcherThread.isAlive()) {
            watcherThread.interrupt();
            watcherThread.join(5000);
        }

        if (watchService != null) {
            watchService.close();
        }
    }

    private void watchLoop() {
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.poll(2, TimeUnit.SECONDS);
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            String fileName = event.context().toString();
                            System.out.println("File created: " + fileName);
                            // end.txtが作成されたらファイル監視を停止
                            if (fileName.equals("end.txt")) {
                                stop();
                                break;
                            }

                            // 新しいファイルが作成されたらキューに、Taskを追加
                            taskQueue.add(new Task(watchPath.resolve(fileName).toString()));
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}