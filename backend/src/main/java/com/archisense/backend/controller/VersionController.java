
package com.archisense.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/projects")
public class VersionController {

    @Value("${archisense.project-root:..}")
    private String projectRoot;

    @Value("${archisense.dataset-dir:datasets}")
    private String datasetDirectory;

    @Value("${archisense.upload-dir:uploads}")
    private String uploadDirectory;

    @GetMapping("/{projectId}/versions")
    public List<String> getVersions(@PathVariable String projectId)
            throws Exception {

        Path root = Paths.get(projectRoot)
                .toAbsolutePath()
                .normalize();

        // Original Ant dataset handling remains unchanged.
        if ("ant".equals(projectId)) {

            Path versionsPath = root.resolve(datasetDirectory)
                    .resolve("ant")
                    .resolve("versions")
                    .normalize();

            if (!Files.isDirectory(versionsPath)) {
                throw new IllegalStateException(
                        "Version directory not found: " + versionsPath
                );
            }

            try (Stream<Path> paths = Files.list(versionsPath)) {
                return paths
                        .filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> name.matches(
                                "ant-\\d+\\.\\d+\\.\\d+"
                        ))
                        .sorted()
                        .toList();
            }
        }

        // Uploaded project handling.
        Path uploadBase = root.resolve(uploadDirectory)
                .toAbsolutePath()
                .normalize();

        Path projectPath = uploadBase.resolve(projectId)
                .normalize();

        if (!projectPath.startsWith(uploadBase)
                || !Files.isDirectory(projectPath)) {

            throw new IllegalArgumentException(
                    "Uploaded project not found: " + projectId
            );
        }

        Path versionsPath = projectPath
                .resolve("astdea-input")
                .resolve("uploaded-project")
                .resolve("stats")
                .resolve("versions.csv")
                .normalize();

        // Failed or incomplete projects may not have version metadata.
        if (!versionsPath.startsWith(projectPath)
                || !Files.isRegularFile(versionsPath)) {
            return List.of();
        }

        List<String> lines = Files.readAllLines(versionsPath);

        if (lines.size() < 2 || lines.get(1).isBlank()) {
            return List.of();
        }

        return List.of(lines.get(1).trim());
    }
}
