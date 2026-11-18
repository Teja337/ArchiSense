
package com.archisense.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ProjectUploadServiceTest {

    @TempDir
    Path tempDirectory;

    private ProjectUploadService service;
    private Path uploadDirectory;

    @BeforeEach
    void setUp() throws IOException {

        service = new ProjectUploadService();

        uploadDirectory = tempDirectory.resolve("uploads");

        ReflectionTestUtils.setField(
                service, "projectRoot", tempDirectory.toString()
        );

        ReflectionTestUtils.setField(
                service, "uploadDirectory", "uploads"
        );

        Files.createDirectories(uploadDirectory);
    }

    @Test
    void shouldUploadValidMavenProject() throws Exception {

        byte[] zipContent = createZip(
                "sample/pom.xml",
                "<project></project>"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.zip",
                "application/zip",
                zipContent
        );

        var result = service.uploadProject(file);

        assertEquals("UPLOADED", result.get("status"));
        assertEquals("sample.zip", result.get("originalFilename"));

        Path projectDirectory = Path.of(result.get("projectPath"))
                .getParent()
                .getParent();

        // Verify that the original filename is persisted.
        Path filenameFile =
                projectDirectory.resolve("original-filename.txt");

        assertTrue(Files.isRegularFile(filenameFile));
        assertEquals("sample.zip", Files.readString(filenameFile));

        assertTrue(Files.isRegularFile(
                Path.of(result.get("projectPath")).resolve("pom.xml")
        ));
    }

    @Test
    void shouldRejectZipPathTraversal() throws Exception {

        byte[] zipContent = createZip(
                "../escaped.txt",
                "unsafe content"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "unsafe.zip",
                "application/zip",
                zipContent
        );

        assertThrows(
                IOException.class,
                () -> service.uploadProject(file)
        );

        assertFalse(
                Files.exists(tempDirectory.resolve("escaped.txt"))
        );

        assertDirectoryEmpty(uploadDirectory);
    }

    @Test
    void shouldRejectZipWithTooManyEntries() throws Exception {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {

            for (int i = 0; i < 20001; i++) {

                zip.putNextEntry(
                        new ZipEntry("file-" + i + ".txt")
                );

                zip.closeEntry();
            }
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "many-files.zip",
                "application/zip",
                buffer.toByteArray()
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> service.uploadProject(file)
        );

        assertEquals(
                "ZIP contains too many files.",
                exception.getMessage()
        );

        assertDirectoryEmpty(uploadDirectory);
    }

    @Test
    void shouldRejectNonZipFile() throws IOException {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "not a ZIP".getBytes()
        );

        assertThrows(
                IOException.class,
                () -> service.uploadProject(file)
        );

        assertDirectoryEmpty(uploadDirectory);
    }

    @Test
    void shouldRejectExtractedDataAboveConfiguredTestLimit()
            throws Exception {

        Path zipFile = tempDirectory.resolve("large-content.zip");

        Files.write(
                zipFile,
                createZip("payload.bin", "x".repeat(64))
        );

        Path extractionDirectory =
                tempDirectory.resolve("extracted");

        IOException exception = assertThrows(
                IOException.class,
                () -> service.extractZip(
                        zipFile,
                        extractionDirectory,
                        32
                )
        );

        assertEquals(
                "Extracted project exceeds the 1 GB limit.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectUploadExceeding500MBLimit() throws Exception {

        MultipartFile file = Mockito.mock(MultipartFile.class);

        Mockito.when(file.isEmpty()).thenReturn(false);

        Mockito.when(file.getSize()).thenReturn(
                500L * 1024 * 1024 + 1
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> service.uploadProject(file)
        );

        assertEquals(
                "Upload exceeds the 500 MB limit.",
                exception.getMessage()
        );

        assertDirectoryEmpty(uploadDirectory);
    }

    private byte[] createZip(String entryName, String content)
            throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {

            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content.getBytes());
            zip.closeEntry();
        }

        return buffer.toByteArray();
    }

    private void assertDirectoryEmpty(Path directory)
            throws IOException {

        try (var files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
    }
}
