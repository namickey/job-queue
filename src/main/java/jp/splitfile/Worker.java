package jp.splitfile;

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
        fileWatcher = new FileWatcher("external_rcv_dir", queue);
        fileWatcher.start();

        System.out.println("ファイル監視開始。external_rcv_dir内にファイルが作成を検知し、Taskを実行。");

        while (fileWatcher.isRunning()) {
            Thread.sleep(10000);
            System.out.println("Worker is running...  end.txtが作成されたら終了します。");
        }
        w.close();

    }

    public void execute(BlockingQueue<Task> queue) {
        for (int i = 0; i < poolSize; i++) {
            add(queue);
        }
    }

    private void add(final BlockingQueue<Task> queue) {
        this.service.submit(new Callable<String>() {
            public String call() throws Exception {
                while (!isClose) {
                    Task task = queue.poll(5, TimeUnit.SECONDS);
                    if (task != null) {
                        try {
                            task.execute();
                        } catch (Exception e) {
                            System.err.println("Task execution failed: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                return "Worker is end.";
            }
        });
    }

    public void close() throws InterruptedException, ExecutionException {
        this.isClose = true;
        for (int i = 0; i < poolSize; i++) {
            System.out.println(getResult());
        }
        this.executor.shutdown();
        if (!this.executor.awaitTermination(20, TimeUnit.SECONDS)) {
            this.executor.shutdownNow();
        }
    }

    private String getResult() throws InterruptedException, ExecutionException {
        Future<String> future = this.service.take();
        return future.get();
    }
}
