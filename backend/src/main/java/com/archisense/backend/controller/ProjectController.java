
package com.archisense.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Value("${archisense.project-root}")
    private String projectRoot;

    @Value("${archisense.upload-dir:uploads}")
    private String uploadDirectory;

    @Value("${archisense.output-dir:runtime/engine-output}")
    private String outputDirectory;

    @GetMapping
    public List<Map<String, String>> getProjects() throws IOException {

        List<Map<String, String>> projects = new ArrayList<>();

        projects.add(Map.of(
                "id", "ant",
                "name", "Apache Ant",
                "description", "Original AsTdEA baseline dataset",
                "status", "AVAILABLE"
        ));

        Path root = Path.of(projectRoot)
                .toAbsolutePath()
                .normalize();

        Path uploadBase = root.resolve(uploadDirectory)
                .toAbsolutePath()
                .normalize();

        if (!Files.isDirectory(uploadBase)) {
            return projects;
        }

        try (Stream<Path> directories = Files.list(uploadBase)) {

            directories
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(
                            path -> path.getFileName().toString()
                    ))
                    .forEach(directory -> {

                        String projectId =
                                directory.getFileName().toString();

                        String status =
                                readProjectStatus(directory, projectId);

                        String projectName =
                                readProjectName(directory, projectId);

                        Map<String, String> project = new HashMap<>();

                        project.put("id", projectId);
                        project.put("name", projectName);
                        project.put("description", "Uploaded Java project");
                        project.put("status", status);

                        if ("FAILED".equals(status)) {
                            project.put(
                                    "failureStage",
                                    readFailureDetail(
                                            directory,
                                            "failure-stage.txt"
                                    )
                            );

                            project.put(
                                    "failureMessage",
                                    readFailureDetail(
                                            directory,
                                            "failure-message.txt"
                                    )
                            );
                        }

                        projects.add(project);
                    });
        }

        return projects;
    }

    private String readFailureDetail(
            Path projectDirectory,
            String filename) {

        Path file = projectDirectory.resolve(filename);

        if (!Files.isRegularFile(file)) {
            return "";
        }

        try {
            return Files.readString(file).trim();
        } catch (IOException ignored) {
            return "";
        }
    }

    /**
     * Reads the original uploaded ZIP filename.
     * Older projects without the filename file use their project ID.
     */
    private String readProjectName(
            Path projectDirectory,
            String projectId) {

        Path filenameFile =
                projectDirectory.resolve("original-filename.txt");

        if (!Files.isRegularFile(filenameFile)) {
            return projectId;
        }

        try {
            String filename = Files.readString(filenameFile).trim();

            if (!filename.isBlank()) {
                return filename;
            }

        } catch (IOException ignored) {
            // Fall back to the project ID.
        }

        return projectId;
    }

    private String readProjectStatus(
            Path projectDirectory,
            String projectId) {

        Path statusFile = projectDirectory.resolve("status.txt");

        if (Files.isRegularFile(statusFile)) {
            try {
                String status = Files.readString(statusFile).trim();

                if (status.equals("UPLOADED")
                        || status.equals("PROCESSING")
                        || status.equals("ANALYSIS_COMPLETED")
                        || status.equals("FAILED")) {
                    return status;
                }
            } catch (IOException ignored) {
                // Fall back to checking existing analysis records.
            }
        }

        Path outputBase = Path.of(projectRoot)
                .toAbsolutePath()
                .normalize()
                .resolve(outputDirectory)
                .normalize();

        Path projectOutput = outputBase.resolve(projectId).normalize();

        if (projectOutput.startsWith(outputBase)
                && hasCompletedAnalysis(projectOutput)) {
            return "ANALYSIS_COMPLETED";
        }

        if (Files.isRegularFile(
                projectDirectory.resolve(
                        "astdea-input/uploaded-project/stats/versions.csv"
                ))) {
            return "READY_FOR_ANALYSIS";
        }

        return "UPLOADED";
    }

    private boolean hasCompletedAnalysis(Path projectOutput) {

        Path latestRunFile = projectOutput.resolve("latest-run.txt");

        if (!Files.isRegularFile(latestRunFile)) {
            return false;
        }

        try {
            String runId = Files.readString(latestRunFile).trim();

            if (runId.isBlank()
                    || runId.contains("/")
                    || runId.contains("\\")
                    || runId.contains("..")) {
                return false;
            }

            Path runDirectory = projectOutput.resolve(runId).normalize();

            if (!runDirectory.startsWith(projectOutput)
                    || !Files.isDirectory(runDirectory)) {
                return false;
            }

            try (Stream<Path> paths = Files.walk(runDirectory)) {
                return paths.anyMatch(path ->
                        Files.isRegularFile(path)
                                && !path.getFileName()
                                        .toString()
                                        .equals("engine.log")
                );
            }

        } catch (IOException exception) {
            return false;
        }
    }
}
