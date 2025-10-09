package jp.sample;

import java.io.File;

public class Task {

    private String fileName;

    public Task(String fileName) {
        this.fileName = fileName;
    }

    public void execute() {
        System.out.println("Task開始: " + fileName);

        // ↓ ここでファイルに対する処理を行う START
        
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ↑ ここでファイルに対する処理を行う END

        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        System.out.println("Task終了: " + fileName + "ファイルを削除しました。");
    }
}
