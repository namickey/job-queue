package jp.report.splitfile;

import java.nio.file.Path;

import jp.report.FileWatcher;
import jp.report.Worker;

public class SplitWorker extends Worker {
    public static void main(String[] args) {
        new SplitWorker(2).run();
    }

    public SplitWorker(int poolSize) {
        super(poolSize);
    }

    @Override
    protected FileWatcher getFileWatcher() {
        return new SplitFileWatcher(
                Path.of("2.unziped_csv_dir"),
                Path.of("3.splited_csv_dir"));
    }
}
