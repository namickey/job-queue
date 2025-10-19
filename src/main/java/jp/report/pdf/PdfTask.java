package jp.report.pdf;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import jp.report.Task;

public class PdfTask extends Task {

    private Path dataPath;

    private static final String PDF_INPUT_DIR = "3.splited_csv_dir";
    private static final String PDF_OUTPUT_DIR = "4.create_pdf_report_dir";

    public PdfTask(Path dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    public void execute() {

        System.out.println("タスク開始");
        System.out.println("データファイルのパス: " + dataPath.toString());

        try {

            String format = Files.readString(Path.of("pdf_format", "report-a1-jp.jrxml"));
            String data = Files.readString(dataPath);

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
                String outputFileName = dataPath.toString().replace(PDF_INPUT_DIR, PDF_OUTPUT_DIR);
                Files.write(Path.of(outputFileName.substring(0, outputFileName.length() - 4) + ".pdf"), response.body());
            } else {
                System.out.println("Failed: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        
        try {
            Files.deleteIfExists(dataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("タスク終了");
    }
}
