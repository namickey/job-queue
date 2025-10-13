package jp.splitfile;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
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

    public void stop() {
        isRunning = false;
        if (watcherThread != null && watcherThread.isAlive()) {
            watcherThread.interrupt();
            try {
                watcherThread.join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                            // end.txtが作成されたら監視停止
                            if (fileName.equals("end.txt")) {
                                stop();
                                break;
                            }

                            // 新しいファイルが作成されたらTaskを追加
                            Task task = createTask(fileName);
                            if (task != null) {
                                taskQueue.add(task);
                            }
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                stop();
                break;
            }
        }
    }

    private Task createTask(String fileName) throws IOException {
        if (fileName.startsWith("data-a1")) {
            if (fileName.endsWith(".trigger")) {
                Files.deleteIfExists(watchPath.resolve(fileName));
                System.out.println(fileName + "ファイルを削除しました。");
                return new TaskA1(watchPath.resolve(fileName.replace(".trigger", ".csv")));
            } else {
                System.out.println("Ignoring non-trigger file: " + fileName);
                return null;
            }
            
        } else if (fileName.startsWith("data-b1")) {
            if (fileName.endsWith(".trigger")) {
                Files.deleteIfExists(watchPath.resolve(fileName));
                System.out.println(fileName + "ファイルを削除しました。");
                return new TaskB1(watchPath.resolve(fileName.replace(".trigger", ".csv")));
            } else {
                System.out.println("Ignoring non-trigger file: " + fileName);
                return null;
            }
        } else {
            System.out.println("Unknown file type: " + fileName);
            return null;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}