package jp.fullspec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Task {

    private String fileName;

    public Task(String fileName) {
        this.fileName = fileName;
    }

    public void execute() {
        System.out.println("Task開始: " + fileName);

        // ↓ ここでファイルに対する処理を行う START
        
        try (FileReader fr = new FileReader(fileName);
                BufferedReader br = new BufferedReader(fr);) {
            String line;
            int i = 1;
            while ((line = br.readLine()) != null) {
                // 各行ごとに新規ファイルを作成
                String outputFileName = fileName.replace("rcv", "snd") + "_line_" + i + ".txt";
                try (java.io.FileWriter fw = new java.io.FileWriter(outputFileName)) {
                    fw.write(line);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.err.println("ファイル作成エラー: " + outputFileName);
                }
                i++;
            }
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
