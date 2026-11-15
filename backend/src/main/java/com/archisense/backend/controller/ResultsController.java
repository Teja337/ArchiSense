
package com.archisense.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/projects/ant/results")
public class ResultsController {

    private final Path resultsPath = Paths.get(
        "..", "results", "ant", "intraVersion"
    ).toAbsolutePath().normalize();

    @GetMapping
    public List<Map<String, String>> getResults() throws Exception {

        List<Map<String, String>> results = new ArrayList<>();

        if (!Files.isDirectory(resultsPath)) {
            throw new IllegalStateException(
                "Results directory not found: " + resultsPath
            );
        }

        for (int i = 0; i < 3; i++) {

            Path csvFile = resultsPath
                .resolve(String.valueOf(i))
                .resolve("ProjectMetrics.csv");

            if (!Files.exists(csvFile)) {
                throw new IllegalStateException(
                    "Metrics file not found: " + csvFile
                );
            }

            List<String> lines = Files.readAllLines(csvFile);

            if (lines.size() < 2) {
                continue;
            }

            String[] headers = lines.get(0).split(",", -1);
            String[] values = lines.get(1).split(",", -1);

            Map<String, String> versionMetrics = new LinkedHashMap<>();

            for (int j = 0; j < headers.length && j < values.length; j++) {
                versionMetrics.put(headers[j], values[j]);
            }

            versionMetrics.put("versionIndex", String.valueOf(i));

            results.add(versionMetrics);
        }

        return results;
    }
}
