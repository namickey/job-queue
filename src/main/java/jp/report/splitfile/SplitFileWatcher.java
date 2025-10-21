package jp.report.splitfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import jp.report.FileWatcher;
import jp.report.Task;

public class SplitFileWatcher extends FileWatcher {

    private BlockingQueue<Task> taskQueue;
    private Path watchPath;
    private Path outputDirPath;
    private WatchService watchService;
    private Thread watcherThread;
    private volatile boolean isRunning = false;

    public SplitFileWatcher(Path watchPath, Path outputDirPath) {
        this.watchPath = watchPath;
        this.outputDirPath = outputDirPath;

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

    public void execute(BlockingQueue<Task> taskQueue) {
        this.taskQueue = taskQueue;

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
                            String fileOrDirName = event.context().toString();
                            System.out.println(fileOrDirName + " が作成されました。1");
                            // end.txtが作成されたら監視停止
                            if ("end.txt".equals(fileOrDirName)) {
                                stop();
                                break;
                            }

                            if (Files.isDirectory(watchPath.resolve(fileOrDirName))) {
                                // ディレクトリの場合

                                for (File file : new File(watchPath.resolve(fileOrDirName).toString()).listFiles()) {
                                    Path filePath = file.toPath();
                                    System.out.println(filePath + " が作成されました。2");
                                    // 新しいファイルが作成されたらTaskをキューに追加
                                    Task task = createTask(filePath);
                                    if (task != null) {
                                        taskQueue.add(task);
                                    }
                                }

                                while (new File(watchPath.resolve(fileOrDirName).toString()).listFiles().length > 0) {
                                    Thread.sleep(2000);
                                }

                                Files.deleteIfExists(watchPath.resolve(fileOrDirName));

                            // } else {
                            //     // ファイルの場合

                            //     // 新しいファイルが作成されたらTaskをキューに追加
                            //     Task task = createTask(watchPath.resolve(fileOrDirName));
                            //     if (task != null) {
                            //         taskQueue.add(task);
                            //     }
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

    private Task createTask(Path filePath) throws IOException {

        // if (!fileName.endsWith(".trigger")) {
        //     // トリガーファイル以外（CSVファイル）は無視
        //     return null;
        // }
        // // トリガーファイルを削除
        // Files.deleteIfExists(watchPath.resolve(fileName));
        // System.out.println("トリガーファイル検知：" + fileName + " また、トリガーファイルを削除しました。");

        // // 対応するCSVファイルのパスを生成
        // Path csvPath = watchPath.resolve(fileName.replace(".trigger", ".csv"));

        //Path csvPath = watchPath.resolve(fileName);

        // タスク生成
        if (filePath.getFileName().toString().startsWith("data-a1")) {
            return new TaskA1(filePath, outputDirPath.resolve(filePath.getParent().getFileName()));
        } else if (filePath.getFileName().toString().startsWith("data-b1")) {
            return new TaskB1(filePath, outputDirPath.resolve(filePath.getParent().getFileName()));
        } else {
            System.out.println("Unknown file type: " + filePath);
            Files.deleteIfExists(filePath);
            return null;
        }
    }
}