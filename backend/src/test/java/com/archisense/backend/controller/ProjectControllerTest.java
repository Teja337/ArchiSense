
package com.archisense.backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProjectControllerTest {

    @TempDir
    Path tempDirectory;

    private ProjectController controller;
    private Path uploadDirectory;

    @BeforeEach
    void setUp() throws Exception {
        controller = new ProjectController();

        uploadDirectory = tempDirectory.resolve("uploads");
        Files.createDirectories(uploadDirectory);

        ReflectionTestUtils.setField(
                controller, "projectRoot", tempDirectory.toString()
        );
        ReflectionTestUtils.setField(
                controller, "uploadDirectory", "uploads"
        );
        ReflectionTestUtils.setField(
                controller, "outputDirectory", "runtime/engine-output"
        );
    }

    @Test
    void shouldReturnSavedFailureDetailsForFailedProject() throws Exception {
        String projectId = "failed-project-1";

        Path projectDirectory = createProject(projectId, "FAILED");

        Files.writeString(
                projectDirectory.resolve("original-filename.txt"),
                "invalid-project.zip"
        );
        Files.writeString(
                projectDirectory.resolve("failure-stage.txt"),
                "BUILD"
        );
        Files.writeString(
                projectDirectory.resolve("failure-message.txt"),
                "Maven build failed."
        );

        Map<String, String> project = findProject(projectId);

        assertEquals("invalid-project.zip", project.get("name"));
        assertEquals("FAILED", project.get("status"));
        assertEquals("BUILD", project.get("failureStage"));
        assertEquals("Maven build failed.", project.get("failureMessage"));
    }

    @Test
    void shouldReturnEmptyFailureDetailsForOlderFailedProject()
            throws Exception {
        String projectId = "old-failed-project";

        createProject(projectId, "FAILED");

        Map<String, String> project = findProject(projectId);

        assertEquals("FAILED", project.get("status"));
        assertEquals("", project.get("failureStage"));
        assertEquals("", project.get("failureMessage"));
    }

    @Test
    void shouldNotIncludeFailureDetailsForNonFailedProject()
            throws Exception {
        String projectId = "uploaded-project";

        createProject(projectId, "UPLOADED");

        Map<String, String> project = findProject(projectId);

        assertEquals("UPLOADED", project.get("status"));
        assertFalse(project.containsKey("failureStage"));
        assertFalse(project.containsKey("failureMessage"));
    }

    private Path createProject(String projectId, String status)
            throws Exception {
        Path projectDirectory = uploadDirectory.resolve(projectId);
        Files.createDirectories(projectDirectory);

        Files.writeString(
                projectDirectory.resolve("status.txt"),
                status
        );

        return projectDirectory;
    }

    private Map<String, String> findProject(String projectId)
            throws Exception {
        List<Map<String, String>> projects = controller.getProjects();

        return projects.stream()
                .filter(project -> projectId.equals(project.get("id")))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError("Project not found: " + projectId)
                );
    }
}
