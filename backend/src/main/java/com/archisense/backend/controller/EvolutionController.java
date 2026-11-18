
package com.archisense.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/ant/evolution")
public class EvolutionController {

    @Value("${archisense.project-root:..}")
    private String projectRoot;

    @Value("${archisense.output-dir:runtime/engine-output}")
    private String outputDirectory;

    @GetMapping
    public Map<String, String> getEvolutionMetrics() throws IOException {

        Path root = Path.of(projectRoot)
                .toAbsolutePath()
                .normalize();

        Path outputPath = root.resolve(outputDirectory)
                .normalize();

        if (!Files.isDirectory(outputPath)) {
            throw new IllegalStateException(
                    "Engine output directory not found: " + outputPath
            );
        }

        // Read the latest successful analysis run ID.
        Path latestRunFile = outputPath.resolve("latest-run.txt");

        if (!Files.isRegularFile(latestRunFile)) {
            throw new IllegalStateException(
                    "Latest analysis run ID was not found."
            );
        }

        String latestRunId = Files.readString(latestRunFile).trim();

        if (latestRunId.isEmpty()) {
            throw new IllegalStateException(
                    "Latest analysis run ID is empty."
            );
        }

        Path latestRun = outputPath.resolve(latestRunId).normalize();

        if (!latestRun.startsWith(outputPath)) {
            throw new IllegalStateException(
                    "Invalid latest analysis run ID."
            );
        }

        Path metricsFile = latestRun
                .resolve("ant")
                .resolve("interVersion")
                .resolve("ProjectMetrics.csv");

        if (!Files.isRegularFile(metricsFile)) {
            throw new IllegalStateException(
                    "Evolution metrics file not found: " + metricsFile
            );
        }

        List<String> lines = Files.readAllLines(metricsFile);

        if (lines.size() < 2) {
            throw new IllegalStateException(
                    "Evolution metrics file has no data: " + metricsFile
            );
        }

        String[] headers = lines.get(0).split(",", -1);
        String[] values = lines.get(1).split(",", -1);

        Map<String, String> metrics = new LinkedHashMap<>();

        for (int i = 0; i < headers.length && i < values.length; i++) {
            metrics.put(headers[i], values[i]);
        }

        return metrics;
    }
}
