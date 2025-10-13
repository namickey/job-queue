package jp.report;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WorkerAndQueue {
    private ThreadPoolExecutor executor;
    private CompletionService<String> service;
    private int poolSize;
    private volatile boolean isClose;

    public WorkerAndQueue(int poolSize) {
        this.poolSize = poolSize;
        this.executor = new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                new LinkedBlockingQueue<Runnable>());
        this.service = new ExecutorCompletionService<String>(this.executor);
    }

    public static void main(String[] args) {
        WorkerAndQueue w = new WorkerAndQueue(2);
        BlockingQueue<PdfTask> queue = new LinkedBlockingQueue<>();
        w.execute(queue);

        FileWatcher fileWatcher = new FileWatcher("pdf_report_snd_dir", queue);
        fileWatcher.start();

        System.out.println("ファイル監視開始。pdf_report_snd_dir内のファイル作成を検知し、PdfTaskを実行。");

        while (fileWatcher.isRunning()) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Worker is running...  end.txtが作成されたら終了します。");
        }

        w.close();
    }

    public void execute(BlockingQueue<PdfTask> queue) {
        for (int i = 0; i < poolSize; i++) {
            add(queue);
        }
    }

    private void add(BlockingQueue<PdfTask> queue) {
        this.service.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                while (!isClose) {
                    PdfTask task = queue.poll(5, TimeUnit.SECONDS);
                    if (task != null) {
                        try {
                            task.execute();
                        } catch (Exception e) {
                            System.err.println("Task execution failed.");
                            e.printStackTrace();
                        }
                    }
                }
                return "Worker Thread is end.";
            }
        });
    }

    public void close() {
        this.isClose = true;
        for (int i = 0; i < poolSize; i++) {
            System.out.println(getResult());
        }
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(20, TimeUnit.SECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getResult() {
        try {
            Future<String> future = this.service.take();
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
