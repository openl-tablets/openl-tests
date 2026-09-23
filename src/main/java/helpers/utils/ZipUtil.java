package helpers.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    private static final Logger LOGGER = LogManager.getLogger(ZipUtil.class);

    public static List<String> listFiles(File zipFile) {
        List<String> fileNames = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String fileName = normalizePath(entry.getName());
                    fileNames.add(fileName);
                    LOGGER.debug("Found file in archive: {}", fileName);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read ZIP file: {}", zipFile.getName(), e);
            throw new RuntimeException("Failed to read ZIP archive: " + e.getMessage(), e);
        }

        LOGGER.info("Archive {} contains {} files", zipFile.getName(), fileNames.size());
        return fileNames;
    }

    public static String readFileFromZip(File zipFile, String entryName) {
        try (InputStream archive = new FileInputStream(zipFile)) {
            String content = readEntry(archive, name -> name.equals(entryName) || name.endsWith("/" + entryName))
                    .orElseThrow(() -> new RuntimeException("File '" + entryName + "' not found in ZIP archive: " + zipFile.getName()));
            LOGGER.info("Read file '{}' from archive {} ({} chars)", entryName, zipFile.getName(), content.length());
            return content;
        } catch (IOException e) {
            LOGGER.error("Failed to read file '{}' from ZIP: {}", entryName, zipFile.getName(), e);
            throw new RuntimeException("Failed to read file from ZIP archive: " + e.getMessage(), e);
        }
    }

    public static String readFileFromZip(byte[] archive, String entryName) {
        try {
            return readEntry(new ByteArrayInputStream(archive), name -> name.equals(entryName))
                    .orElseThrow(() -> new RuntimeException(
                            "File '" + entryName + "' not found in ZIP archive of " + archive.length + " bytes"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file from ZIP archive: " + e.getMessage(), e);
        }
    }

    private static Optional<String> readEntry(InputStream archive, Predicate<String> wanted) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(archive)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (wanted.test(normalizePath(entry.getName()))) {
                    return Optional.of(new String(zis.readAllBytes(), StandardCharsets.UTF_8));
                }
                zis.closeEntry();
            }
        }
        return Optional.empty();
    }

    private static String normalizePath(String path) {
        return path.replace('\\', '/');
    }
}
