package jp.sample;

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
    private boolean isClose;

    public Worker(int poolSize) {
        this.poolSize = poolSize;
        this.executor = new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                new LinkedBlockingQueue<Runnable>());
        this.service = new ExecutorCompletionService<String>(this.executor);
    }

    public static void main(String[] args) {
        Worker w = new Worker(2);
        BlockingQueue<Task> queue = new LinkedBlockingQueue<Task>();
        w.execute(queue);
        queue.add(new Task());
        queue.add(new Task());
        queue.add(new Task());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        w.close();
    }

    public void execute(BlockingQueue<Task> queue) {
        for (int i = 0; i < poolSize; i++) {
            add(queue);
        }
    }

    public void close() {
        this.isClose = true;
        for (int i = 0; i < poolSize; i++) {
            System.out.println(getResult());
        }
        this.executor.shutdownNow();
    }

    private void add(final BlockingQueue<Task> queue) {
        this.service.submit(new Callable<String>() {
            public String call() throws Exception {
                while(!isClose) {
                    Task task = queue.poll(2, TimeUnit.SECONDS);
                    if (task != null) {
                        task.execute();
                    }
                }
                return "end.";
            }
        });
    }

    private String getResult() {
        Future<String> future = null;
        while (future == null) {
            future = this.service.poll();
        }
        String result = null;
        try {
            result = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }
}
