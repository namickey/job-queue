package jp.report.unzip;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Main {

    public static void main(String[] args) throws Exception {
        Path zipPath = Path.of("1.external_zip_rcv_dir/ab-1.zip");
        try (ZipFile zip = new ZipFile(zipPath.toFile());
                Stream<? extends ZipEntry> stream = zip.stream()) {
            List<Path> uncompressedFileList = stream
                    .map(zipEntry -> {
                        if (zipEntry.isDirectory()) {
                            try {
                                Path dirPath = Path.of("2.unziped_csv_dir", zipEntry.getName());
                                Files.createDirectories(dirPath);
                                return dirPath;
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        } else {
                            try (InputStream is = zip.getInputStream(zipEntry)) {
                                byte[] bytes = is.readAllBytes();
                                Path uncompressedFile = Path.of("2.unziped_csv_dir", zipEntry.getName());
                                Files.write(uncompressedFile, bytes, StandardOpenOption.CREATE,
                                        StandardOpenOption.WRITE);
                                return uncompressedFile;
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    }).toList();
        }
    }
}
