package jp.simple;

import java.io.File;

public class Task {

    private String fileName;

    public Task(String fileName) {
        this.fileName = fileName;
    }

    public void execute() {
        System.out.println("Task開始: " + fileName);

        // ↓ ここでファイルに対する処理を行う START

        // 例として、4秒間スリープしてからファイルを削除する処理を行う
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }

        // ↑ ここでファイルに対する処理を行う END

        System.out.println("Task終了: " + fileName + "ファイルを削除しました。");
    }
}
