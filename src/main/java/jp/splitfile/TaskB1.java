package jp.splitfile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TaskB1 implements Task {

    private Path path;

    public TaskB1(Path path) {
        this.path = path;
    }

    public void execute() {
        System.out.println("Task B 開始: " + path);

        // ↓ ここでファイルに対する処理を行う START

        try (FileReader fr = new FileReader(path.toFile());
                BufferedReader br = new BufferedReader(fr);) {
            String line;
            String csvHeader = br.readLine(); // ヘッダー行を読み込む
            int i = 1;
            int count = 1;
            while ((line = br.readLine()) != null) {
                // 各行ごとに新規ファイルを作成
                Path outputPath = Path.of("pdf_report_snd_dir", path.getFileName().toString() + "_file_" + i + ".csv");
                try (FileWriter fw = new FileWriter(outputPath.toFile(), true)) {
                    if (count % 5 == 1) {
                        fw.write(csvHeader + System.lineSeparator());
                    }
                    //System.out.println(i + "ファイル目: 、" + count + "行目: " + line);
                    fw.write(line + System.lineSeparator());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.err.println("ファイル作成エラー: " + outputPath);
                }
                if (count % 5 == 0) {
                    i++;
                }
                count++;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // ↑ ここでファイルに対する処理を行う END

        try {
            Files.delete(path);
            System.out.println("Task B 終了: " + path.getFileName() + "ファイルを削除しました。");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
