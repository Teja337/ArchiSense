
package com.archisense.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Stream;

@Service
public class ProjectUploadService {

    private static final long MAX_UPLOAD_SIZE = 500L * 1024 * 1024;
    private static final long MAX_EXTRACTED_SIZE = 1024L * 1024 * 1024;
    private static final int MAX_ENTRIES = 20000;

    private static final String STATUS_FILE = "status.txt";
    private static final String ORIGINAL_FILENAME_FILE =
            "original-filename.txt";

    @Value("${archisense.project-root:..}")
    private String projectRoot;

    @Value("${archisense.upload-dir:uploads}")
    private String uploadDirectory;

    public Map<String, String> uploadProject(MultipartFile file)
            throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IOException("Please select a ZIP file.");
        }

        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new IOException("Upload exceeds the 500 MB limit.");
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null ||
                !originalName.toLowerCase().endsWith(".zip")) {
            throw new IOException("Only ZIP files are supported.");
        }

        // Keep only the filename, not any supplied directory path.
        String normalizedName = originalName.replace('\\', '/');
        String safeOriginalFilename =
                normalizedName.substring(normalizedName.lastIndexOf('/') + 1);

        if (safeOriginalFilename.isBlank()) {
            throw new IOException("Invalid uploaded filename.");
        }

        Path root = Path.of(projectRoot)
                .toAbsolutePath()
                .normalize();

        Path uploadBase = root.resolve(uploadDirectory)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(uploadBase);

        String projectId = UUID.randomUUID().toString();

        Path projectDirectory = uploadBase.resolve(projectId).normalize();

        if (!projectDirectory.startsWith(uploadBase)) {
            throw new IOException("Invalid project upload path.");
        }

        Path sourceDirectory = projectDirectory.resolve("source");
        Path zipFile = projectDirectory.resolve("project.zip");

        Files.createDirectories(sourceDirectory);

        try {
            file.transferTo(zipFile);

            extractZip(zipFile, sourceDirectory);

            Path pomFile;

            try (Stream<Path> paths = Files.walk(sourceDirectory)) {
                pomFile = paths
                        .filter(Files::isRegularFile)
                        .filter(path ->
                                path.getFileName().toString()
                                        .equalsIgnoreCase("pom.xml"))
                        .min(Comparator.comparingInt(Path::getNameCount))
                        .orElseThrow(() ->
                                new IOException(
                                        "No Maven pom.xml found in the uploaded project."
                                ));
            }

            String projectPath = pomFile.getParent().toString();

            // Persist the original ZIP filename for the project list.
            Path originalFilenameFile =
                    projectDirectory.resolve(ORIGINAL_FILENAME_FILE);

            Files.writeString(
                    originalFilenameFile,
                    safeOriginalFilename
            );

            // Initialize persistent project processing status.
            Path statusFile = projectDirectory.resolve(STATUS_FILE);

            Files.writeString(statusFile, "UPLOADED");

            return Map.of(
                    "projectId", projectId,
                    "originalFilename", safeOriginalFilename,
                    "status", "UPLOADED",
                    "projectPath", projectPath,
                    "buildFile", "pom.xml"
            );

        } catch (IOException | RuntimeException exception) {
            deleteDirectory(projectDirectory);
            throw exception;
        }
    }

    /**
     * Extracts ZIP using the production extracted-size limit.
     */
    private void extractZip(Path zipFile, Path destination)
            throws IOException {

        extractZip(zipFile, destination, MAX_EXTRACTED_SIZE);
    }

    /**
     * Extraction method with a configurable size limit.
     * Package-private visibility allows focused unit testing.
     */
    void extractZip(
            Path zipFile,
            Path destination,
            long maxExtractedSize
    ) throws IOException {

        if (maxExtractedSize < 0) {
            throw new IllegalArgumentException(
                    "Maximum extracted size cannot be negative."
            );
        }

        Path safeDestination = destination.toAbsolutePath().normalize();

        Files.createDirectories(safeDestination);

        long extractedBytes = 0;
        int entryCount = 0;

        try (InputStream input = Files.newInputStream(zipFile);
             ZipInputStream zip = new ZipInputStream(input)) {

            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {

                entryCount++;

                if (entryCount > MAX_ENTRIES) {
                    throw new IOException("ZIP contains too many files.");
                }

                String entryName = entry.getName().replace('\\', '/');

                Path relativePath = Path.of(entryName);

                if (relativePath.isAbsolute()) {
                    throw new IOException(
                            "ZIP contains an absolute path."
                    );
                }

                Path output = safeDestination
                        .resolve(relativePath)
                        .normalize();

                if (!output.startsWith(safeDestination)) {
                    throw new IOException(
                            "ZIP contains an unsafe file path."
                    );
                }

                if (!entryName.endsWith("/")) {

                    Path parent = output.getParent();

                    if (parent != null) {
                        Files.createDirectories(parent);
                    }

                    try (var outputStream = Files.newOutputStream(output)) {

                        byte[] buffer = new byte[8192];
                        int bytesRead;

                        while ((bytesRead = zip.read(buffer)) != -1) {

                            extractedBytes += bytesRead;

                            if (extractedBytes > maxExtractedSize) {
                                throw new IOException(
                                        "Extracted project exceeds the 1 GB limit."
                                );
                            }

                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                } else {
                    Files.createDirectories(output);
                }

                zip.closeEntry();
            }
        }
    }

    /**
     * Best-effort cleanup of an incomplete upload.
     */
    private void deleteDirectory(Path directory) {

        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {

            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup.
                        }
                    });

        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }
}
