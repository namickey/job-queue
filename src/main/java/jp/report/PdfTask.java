package jp.report;

import java.nio.file.Path;

public class PdfTask {
    
    private Path path;

    public PdfTask(Path path) {
        this.path = path;
    }

    public void execute() {
        System.out.println("タスク開始");
        System.out.println("タスク終了");
    }
}
