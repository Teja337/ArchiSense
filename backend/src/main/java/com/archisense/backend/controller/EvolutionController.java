package com.archisense.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/projects/ant/evolution")
public class EvolutionController {

    private final Path metricsFile = Paths.get(
        "..", "results", "ant", "interVersion", "ProjectMetrics.csv"
    ).toAbsolutePath().normalize();

    @GetMapping
    public Map<String, String> getEvolutionMetrics() throws Exception {

        if (!Files.exists(metricsFile)) {
            throw new IllegalStateException(
                "Evolution metrics file not found: " + metricsFile
            );
        }

        List<String> lines = Files.readAllLines(metricsFile);

        if (lines.size() < 2) {
            throw new IllegalStateException(
                "Evolution metrics file has no data."
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