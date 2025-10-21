package jp.report.pdf;

import java.nio.file.Path;

import jp.report.FileWatcher;
import jp.report.Worker;

public class PdfWorker extends Worker {
    public static void main(String[] args) {
        new PdfWorker(2).run();
    }

    public PdfWorker(int poolSize) {
        super(poolSize);
    }

    @Override
    protected FileWatcher getFileWatcher() {
        return new PdfFileWatcher(
                Path.of("3.splited_csv_dir"),
                Path.of("4.create_pdf_report_dir"));
    }
}
