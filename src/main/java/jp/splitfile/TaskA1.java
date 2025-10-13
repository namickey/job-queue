package jp.splitfile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TaskA1 implements Task {

    private Path path;

    public TaskA1(Path path) {
        this.path = path;
    }

    public void execute() {
        System.out.println("Task開始: " + path);

        // ↓ ここでファイルに対する処理を行う START
        
        try (FileReader fr = new FileReader(path.toFile());
                BufferedReader br = new BufferedReader(fr);) {
            String line;
            String csvHeader = br.readLine(); // ヘッダー行を読み込む
            int i = 1;
            while ((line = br.readLine()) != null) {
                // 各行ごとに新規ファイルを作成
                Path outputPath = Path.of("pdf_report_snd_dir", path.getFileName().toString() + "_file_" + i + ".csv");
                try (java.io.FileWriter fw = new java.io.FileWriter(outputPath.toFile())) {
                    fw.write(csvHeader + System.lineSeparator());
                    fw.write(line);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.err.println("ファイル作成エラー: " + outputPath);
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

        try {
            Files.delete(path);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("Task終了: " + path.getFileName() + "ファイルを削除しました。");
    }
}
