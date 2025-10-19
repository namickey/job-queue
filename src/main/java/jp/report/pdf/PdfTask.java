package jp.report.pdf;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import jp.report.Task;

public class PdfTask implements Task {

    private Path inputFilePath;
    private Path outputDirPath;

    public PdfTask(Path inputFilePath, Path outputDirPath) {
        this.inputFilePath = inputFilePath;
        this.outputDirPath = outputDirPath;
    }

    @Override
    public void execute() {

        System.out.println("タスク開始");
        System.out.println("データファイルのパス: " + inputFilePath.toString());

        try {

            String format = Files.readString(Path.of("pdf_format", "report-a1-jp.jrxml"));
            String data = Files.readString(inputFilePath);

            String formatBase64 = Base64.getEncoder().encodeToString(format.getBytes("MS932"));
            String dataBase64 = Base64.getEncoder().encodeToString(data.getBytes("MS932"));

            String json = """
                    {
                      "format": "%s",
                      "data": "%s"
                    }
                    """.formatted(formatBase64, dataBase64);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/generatePdf"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                String outputFileName = outputDirPath.resolve(inputFilePath.getFileName()).toString();
                Files.write(Path.of(outputFileName.substring(0, outputFileName.length() - 4) + ".pdf"), response.body());
            } else {
                System.out.println("Failed: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        
        try {
            Files.deleteIfExists(inputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("タスク終了");
    }
}
