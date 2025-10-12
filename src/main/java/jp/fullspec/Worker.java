package jp.fullspec;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Worker {
    private ThreadPoolExecutor executor;
    private CompletionService<String> service;
    private int poolSize;
    private volatile boolean isClose;

    public Worker(int poolSize) {
        this.poolSize = poolSize;
        this.executor = new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                new LinkedBlockingQueue<Runnable>());
        this.service = new ExecutorCompletionService<String>(this.executor);
    }

    public static void main(String[] args) throws Exception {

        Worker w = new Worker(2);
        BlockingQueue<Task> queue = new LinkedBlockingQueue<Task>();
        w.execute(queue);

        FileWatcher fileWatcher = null;
        fileWatcher = new FileWatcher("rcv_dir", queue);
        fileWatcher.start();

        System.out.println("ファイル作成の監視を開始しました。ファイルが作成されたらTaskを実行します。");

        while (fileWatcher.isRunning()) {
            Thread.sleep(20000);
            System.out.println("Worker is running...  end.txtが作成されたら終了します。");
        }
        w.close();

    }

    public void execute(BlockingQueue<Task> queue) {
        for (int i = 0; i < poolSize; i++) {
            add(queue);
        }
    }

    public void close() throws InterruptedException, ExecutionException {
        this.isClose = true;
        for (int i = 0; i < poolSize; i++) {
            System.out.println(getResult());
        }
        this.executor.shutdown();
        if(!this.executor.awaitTermination(30, TimeUnit.SECONDS)) {
            this.executor.shutdownNow();
        }
    }

    private void add(final BlockingQueue<Task> queue) {
        this.service.submit(new Callable<String>() {
            public String call() throws Exception {
                try {
                    while (!isClose) {
                        Task task = queue.poll(2, TimeUnit.SECONDS);
                        if (task != null) {
                            task.execute();
                        }
                    }
                } catch (Exception e) {
                    // 適切なログ出力やエラー処理を実装
                    System.err.println("Task execution failed: " + e.getMessage());
                    throw e;
                }
                return "Worker is end.";
            }
        });
    }

    private String getResult() throws InterruptedException, ExecutionException{
        Future<String> future = this.service.take();
        return future.get();
    }
}
