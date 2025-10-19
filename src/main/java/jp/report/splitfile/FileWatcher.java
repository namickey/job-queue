package jp.report.splitfile;

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

import jp.report.Task;

public class FileWatcher {

    private WatchService watchService;
    private Path watchPath;
    private BlockingQueue<Task> taskQueue;
    private Thread watcherThread;
    private volatile boolean isRunning = false;

    public FileWatcher(String directoryPath, BlockingQueue<Task> taskQueue) {
        this.taskQueue = taskQueue;
        this.watchPath = Paths.get(directoryPath);
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.watchPath.register(this.watchService, java.nio.file.StandardWatchEventKinds.ENTRY_CREATE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize WatchService.");
        }
    }

    public boolean isRunning() {
        return isRunning;
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
            WatchKey key = null;
            try {
                System.out.println("ファイル監視中...");
                key = watchService.poll(10, TimeUnit.SECONDS);
                if (key != null) {

                    // ファイルが完全に書き込まれるまで待機
                    Thread.sleep(1000);

                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            String fileName = event.context().toString();
                            // end.txtが作成されたら監視停止
                            if ("end.txt".equals(fileName)) {
                                stop();
                                break;
                            }

                            // 新しいファイルが作成されたらTaskをキューに追加
                            Task task = createTask(fileName);
                            if (task != null) {
                                taskQueue.add(task);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                stop();
                break;
            } finally {
                if (key != null) {
                    boolean valid = key.reset();
                    if (!valid) {
                        System.out.println("WatchKey is no longer valid. Stopping watcher.");
                        stop();
                        break;
                    }
                }
            }
        }
    }

    private Task createTask(String fileName) throws IOException {

        if (!fileName.endsWith(".trigger")) {
            // トリガーファイル以外（CSVファイル）は無視
            return null;
        }
        // トリガーファイルを削除
        Files.deleteIfExists(watchPath.resolve(fileName));
        System.out.println("トリガーファイル検知：" + fileName + " また、トリガーファイルを削除しました。");

        // 対応するCSVファイルのパスを生成
        Path csvPath = watchPath.resolve(fileName.replace(".trigger", ".csv"));

        // タスク生成
        if (fileName.startsWith("data-a1")) {
            return new TaskA1(csvPath);
        } else if (fileName.startsWith("data-b1")) {
            return new TaskB1(csvPath);
        } else {
            System.out.println("Unknown file type: " + fileName);
            return null;
        }
    }
}