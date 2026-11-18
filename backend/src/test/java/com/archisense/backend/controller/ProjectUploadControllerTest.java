
package com.archisense.backend.controller;

import com.archisense.backend.service.AsTdEAMetadataPreparationService;
import com.archisense.backend.service.EngineExecutionService;
import com.archisense.backend.service.MavenBuildService;
import com.archisense.backend.service.ProjectUploadService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectUploadControllerTest {

    @TempDir
    Path tempDirectory;

    private ProjectUploadService uploadService;
    private MavenBuildService buildService;
    private AsTdEAMetadataPreparationService metadataService;
    private EngineExecutionService engineService;

    private ProjectUploadController controller;
    private Path projectUploadDirectory;
    private Path projectSourceDirectory;

    @BeforeEach
    void setUp() throws Exception {
        uploadService = mock(ProjectUploadService.class);
        buildService = mock(MavenBuildService.class);
        metadataService = mock(AsTdEAMetadataPreparationService.class);
        engineService = mock(EngineExecutionService.class);

        controller = new ProjectUploadController(
                uploadService,
                buildService,
                metadataService,
                engineService
        );

        ReflectionTestUtils.setField(
                controller, "projectRoot", tempDirectory.toString()
        );
        ReflectionTestUtils.setField(
                controller, "uploadDirectory", "uploads"
        );

        projectUploadDirectory =
                tempDirectory.resolve("uploads/test-project");

        projectSourceDirectory =
                projectUploadDirectory.resolve("source");

        Files.createDirectories(projectSourceDirectory);
    }

    @Test
    void shouldPersistBuildFailureDetails() throws Exception {
        String projectId = "test-project";

        Map<String, String> uploadResult = new HashMap<>();
        uploadResult.put("projectId", projectId);
        uploadResult.put(
                "projectPath",
                projectSourceDirectory.toString()
        );

        when(uploadService.uploadProject(any()))
                .thenReturn(uploadResult);

        when(buildService.buildProject(any(Path.class)))
                .thenThrow(new RuntimeException("Maven build failed."));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid-project.zip",
                "application/zip",
                new byte[]{1, 2, 3}
        );

        ResponseEntity<Map<String, String>> response =
                controller.uploadProject(file);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals("FAILED", response.getBody().get("status"));
        assertEquals("BUILD", response.getBody().get("stage"));
        assertEquals(
                "Maven build failed.",
                response.getBody().get("message")
        );

        assertEquals(
                "FAILED",
                Files.readString(
                        projectUploadDirectory.resolve("status.txt")
                )
        );

        assertEquals(
                "BUILD",
                Files.readString(
                        projectUploadDirectory.resolve("failure-stage.txt")
                )
        );

        assertEquals(
                "Maven build failed.",
                Files.readString(
                        projectUploadDirectory.resolve("failure-message.txt")
                )
        );

        verify(buildService).buildProject(any(Path.class));
        verifyNoInteractions(metadataService, engineService);
    }

    @Test
    void shouldPersistMetadataPreparationFailureDetails()
            throws Exception {
        String projectId = "test-project";

        Map<String, String> uploadResult = new HashMap<>();
        uploadResult.put("projectId", projectId);
        uploadResult.put(
                "projectPath",
                projectSourceDirectory.toString()
        );

        when(uploadService.uploadProject(any()))
                .thenReturn(uploadResult);

        when(buildService.buildProject(any(Path.class)))
                .thenReturn(projectSourceDirectory.resolve("build.log"));

        when(metadataService.prepareInput(
                any(Path.class),
                any(Path.class)
        )).thenThrow(new RuntimeException("Metadata preparation failed."));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "project.zip",
                "application/zip",
                new byte[]{1, 2, 3}
        );

        ResponseEntity<Map<String, String>> response =
                controller.uploadProject(file);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals("FAILED", response.getBody().get("status"));
        assertEquals(
                "METADATA_PREPARATION",
                response.getBody().get("stage")
        );
        assertEquals(
                "Metadata preparation failed.",
                response.getBody().get("message")
        );

        assertEquals(
                "FAILED",
                Files.readString(
                        projectUploadDirectory.resolve("status.txt")
                )
        );

        assertEquals(
                "METADATA_PREPARATION",
                Files.readString(
                        projectUploadDirectory.resolve("failure-stage.txt")
                )
        );

        assertEquals(
                "Metadata preparation failed.",
                Files.readString(
                        projectUploadDirectory.resolve("failure-message.txt")
                )
        );

        verifyNoInteractions(engineService);
    }
}
