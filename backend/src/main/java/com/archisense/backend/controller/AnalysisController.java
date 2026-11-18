
package com.archisense.backend.controller;

import com.archisense.backend.service.EngineExecutionService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final EngineExecutionService engineExecutionService;

    @Value("${archisense.project-root:..}")
    private String projectRoot;

    @Value("${archisense.upload-dir:uploads}")
    private String uploadDirectory;

    @Value("${archisense.dataset-dir:datasets}")
    private String datasetDirectory;

    public AnalysisController(
            EngineExecutionService engineExecutionService) {
        this.engineExecutionService = engineExecutionService;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runAnalysis(
            @RequestParam("projectId") String projectId) {

        try {
            if (projectId == null || projectId.isBlank()) {
                throw new IOException("Project ID is required.");
            }

            Path root = Path.of(projectRoot)
                    .toAbsolutePath()
                    .normalize();

            Path analysisInput;

            if ("ant".equalsIgnoreCase(projectId)) {

                // Apache Ant uses the original multi-version dataset.
                Path datasetBase = root.resolve(datasetDirectory)
                        .toAbsolutePath()
                        .normalize();

                analysisInput = datasetBase.resolve("ant").normalize();

                if (!analysisInput.startsWith(datasetBase)
                        || !Files.isDirectory(
                                analysisInput.resolve("stats"))
                        || !Files.isRegularFile(
                                analysisInput.resolve("stats/versions.csv"))) {
                    throw new IOException(
                            "Apache Ant dataset is missing or incomplete.");
                }

            } else {

                // Uploaded projects use generated AsTdEA input.
                Path uploadBase = root.resolve(uploadDirectory)
                        .toAbsolutePath()
                        .normalize();

                Path projectDirectory =
                        uploadBase.resolve(projectId).normalize();

                if (!projectDirectory.startsWith(uploadBase)) {
                    throw new IOException("Invalid project ID.");
                }

                analysisInput =
                        projectDirectory.resolve("astdea-input");

                if (!Files.isDirectory(analysisInput)) {
                    throw new IOException(
                            "AsTdEA input not found. Upload and prepare the project first.");
                }
            }

            Path analysisOutput =
                    engineExecutionService.executeAnalysis(
                            analysisInput,
                            projectId
                    );

            Map<String, Object> response = new HashMap<>();

            response.put("status", "SUCCESS");
            response.put("message", "AsTdEA analysis completed.");
            response.put("projectId", projectId);
            response.put("analysisOutput", analysisOutput.toString());

            return ResponseEntity.ok(response);

        } catch (IOException exception) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", "FAILED",
                            "message", safeMessage(exception)
                    )
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "FAILED",
                            "message", "Analysis was interrupted."
                    ));
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();

        return message == null || message.isBlank()
                ? "An unexpected error occurred."
                : message;
    }
}
