package jp.report;

import java.util.concurrent.BlockingQueue;

public abstract class FileWatcher {

    public abstract boolean isRunning();

    public abstract void execute(BlockingQueue<Task> taskQueue);
}
