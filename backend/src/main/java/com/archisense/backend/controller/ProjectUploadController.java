
package com.archisense.backend.controller;

import com.archisense.backend.service.AsTdEAMetadataPreparationService;
import com.archisense.backend.service.EngineExecutionService;
import com.archisense.backend.service.MavenBuildService;
import com.archisense.backend.service.ProjectUploadService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectUploadController {

    private final ProjectUploadService uploadService;
    private final MavenBuildService buildService;
    private final AsTdEAMetadataPreparationService metadataService;
    private final EngineExecutionService engineService;

    @Value("${archisense.project-root:..}")
    private String projectRoot;

    @Value("${archisense.upload-dir:uploads}")
    private String uploadDirectory;

    public ProjectUploadController(
            ProjectUploadService uploadService,
            MavenBuildService buildService,
            AsTdEAMetadataPreparationService metadataService,
            EngineExecutionService engineService) {

        this.uploadService = uploadService;
        this.buildService = buildService;
        this.metadataService = metadataService;
        this.engineService = engineService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadProject(
            @RequestPart("file") MultipartFile file) {

        Map<String, String> uploadResult;

        try {
            uploadResult = uploadService.uploadProject(file);

        } catch (IOException | IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    failureResponse(null, "UPLOAD", exception)
            );
        }

        String projectId = uploadResult.get("projectId");
        String currentStage = "UPLOAD";

        Path projectUploadPath = null;

        try {
            if (projectId == null || projectId.isBlank()) {
                throw new IOException(
                        "Upload did not return a project ID."
                );
            }

            Path root = Path.of(projectRoot)
                    .toAbsolutePath()
                    .normalize();

            Path uploadBase = root.resolve(uploadDirectory)
                    .toAbsolutePath()
                    .normalize();

            projectUploadPath = uploadBase.resolve(projectId)
                    .normalize();

            if (!projectUploadPath.startsWith(uploadBase)
                    || !Files.isDirectory(projectUploadPath)) {
                throw new IOException(
                        "Invalid project upload directory."
                );
            }

            updateStatus(projectUploadPath, "PROCESSING");

            String uploadedProjectPath = uploadResult.get("projectPath");

            if (uploadedProjectPath == null
                    || uploadedProjectPath.isBlank()) {
                throw new IOException(
                        "Uploaded project path was not provided."
                );
            }

            Path projectDirectory = Path.of(uploadedProjectPath)
                    .toAbsolutePath()
                    .normalize();

            if (!projectDirectory.startsWith(projectUploadPath)) {
                throw new IOException(
                        "Uploaded project path is outside its upload directory."
                );
            }

            currentStage = "BUILD";

            Path buildLog = buildService.buildProject(projectDirectory);

            currentStage = "METADATA_PREPARATION";

            Path astdeaInput = metadataService.prepareInput(
                    projectDirectory,
                    projectUploadPath
            );

            currentStage = "ANALYSIS";

            Path analysisOutput = engineService.executeAnalysis(
                    astdeaInput,
                    projectId
            );

            updateStatus(projectUploadPath, "ANALYSIS_COMPLETED");

            Map<String, String> response = new HashMap<>(uploadResult);

            response.put("status", "ANALYSIS_COMPLETED");
            response.put("stage", "COMPLETED");
            response.put("buildLog", buildLog.toString());
            response.put("astdeaInput", astdeaInput.toString());
            response.put("analysisOutput", analysisOutput.toString());

            return ResponseEntity.ok(response);

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            Exception failure = new Exception(
                    "Project processing was interrupted."
            );

            saveFailureDetails(
                    projectUploadPath,
                    currentStage,
                    failure
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(failureResponse(
                            projectId,
                            currentStage,
                            failure
                    ));

        } catch (Exception exception) {

            saveFailureDetails(
                    projectUploadPath,
                    currentStage,
                    exception
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(failureResponse(
                            projectId,
                            currentStage,
                            exception
                    ));
        }
    }

    private void updateStatus(Path projectDirectory, String status)
            throws IOException {

        if (projectDirectory == null
                || !Files.isDirectory(projectDirectory)) {
            throw new IOException(
                    "Project directory is unavailable for status update."
            );
        }

        Files.writeString(
                projectDirectory.resolve("status.txt"),
                status
        );
    }

    private void saveFailureDetails(
            Path projectDirectory,
            String stage,
            Exception exception) {

        if (projectDirectory == null
                || !Files.isDirectory(projectDirectory)) {
            return;
        }

        try {
            updateStatus(projectDirectory, "FAILED");

            Files.writeString(
                    projectDirectory.resolve("failure-stage.txt"),
                    stage
            );

            Files.writeString(
                    projectDirectory.resolve("failure-message.txt"),
                    safeMessage(exception)
            );

        } catch (IOException ignored) {
            // Preserve the original processing error response.
        }
    }

    private Map<String, String> failureResponse(
            String projectId,
            String stage,
            Exception exception) {

        Map<String, String> response = new HashMap<>();

        if (projectId != null && !projectId.isBlank()) {
            response.put("projectId", projectId);
        }

        response.put("status", "FAILED");
        response.put("stage", stage);
        response.put("message", safeMessage(exception));

        return response;
    }

    private String safeMessage(Exception exception) {

        String message = exception.getMessage();

        return message == null || message.isBlank()
                ? "An unexpected error occurred."
                : message;
    }
}
