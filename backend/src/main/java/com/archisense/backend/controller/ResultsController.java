
package com.archisense.backend.controller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ResultsController {

    @Value("${archisense.project-root:..}")
    private String projectRoot;

    @Value("${archisense.output-dir:runtime/engine-output}")
    private String outputDirectory;

    private record IssueFiles(String properties, String components) {}

    @GetMapping("/{projectId}/results")
    public List<Map<String, String>> getResults(
            @PathVariable String projectId) throws IOException {

        if ("ant".equals(projectId)) {
            return getAntResults();
        }

        return getUploadedProjectResults(projectId);
    }

    /**
     * Return detailed AsTdEA issue records for a project and version.
     */
    @GetMapping("/{projectId}/issues/{issueType}/{versionIndex}")
    public List<Map<String, String>> getIssues(
            @PathVariable String projectId,
            @PathVariable String issueType,
            @PathVariable int versionIndex) throws IOException {

        IssueFiles files = getIssueFiles(issueType);

        Path runDirectory = getLatestRunDirectory(projectId);

        Path versionDirectory;

        if ("ant".equals(projectId)) {
            if (versionIndex < 0 || versionIndex >= 3) {
                throw new IllegalArgumentException(
                        "Invalid Ant version index: " + versionIndex
                );
            }

            versionDirectory = runDirectory
                    .resolve("ant")
                    .resolve("intraVersion")
                    .resolve(String.valueOf(versionIndex));
        } else {
            if (versionIndex != 0) {
                throw new IllegalArgumentException(
                        "Uploaded projects currently support version index 0."
                );
            }

            versionDirectory = runDirectory;
        }

        if (!Files.isDirectory(versionDirectory)) {
            throw new IllegalStateException(
                    "Analysis version output not found: " + versionDirectory
            );
        }

        Path propertiesFile = versionDirectory.resolve(files.properties());
        Path componentsFile = versionDirectory.resolve(files.components());

        if (!Files.isRegularFile(propertiesFile)) {
            throw new IllegalStateException(
                    "AsTdEA properties file not found: " + propertiesFile
            );
        }

        if (!Files.isRegularFile(componentsFile)) {
            throw new IllegalStateException(
                    "AsTdEA components file not found: " + componentsFile
            );
        }

        return combineIssueRecords(propertiesFile, componentsFile);
    }

    private IssueFiles getIssueFiles(String issueType) {

        return switch (issueType) {
            case "class-cycles" ->
                    new IssueFiles(
                            "ClassCDsProperties.csv",
                            "ClassCDsComponents.csv"
                    );

            case "package-cycles" ->
                    new IssueFiles(
                            "PackageCDsProperties.csv",
                            "PackageCDsComponents.csv"
                    );

            case "hub-dependencies" ->
                    new IssueFiles(
                            "HDsProperties.csv",
                            "HDsComponents.csv"
                    );

            case "unstable-dependencies" ->
                    new IssueFiles(
                            "UDsProperties.csv",
                            "UDsComponents.csv"
                    );

            default -> throw new IllegalArgumentException(
                    "Unknown issue category: " + issueType
            );
        };
    }

    /**
     * Join properties and component records using their shared issue ID.
     */
    private List<Map<String, String>> combineIssueRecords(
            Path propertiesFile,
            Path componentsFile) throws IOException {

        Map<String, Map<String, String>> issues =
                readCsvById(propertiesFile);

        Map<String, Map<String, String>> components =
                readCsvById(componentsFile);

        List<Map<String, String>> results = new ArrayList<>();

        for (Map.Entry<String, Map<String, String>> entry
                : issues.entrySet()) {

            String id = entry.getKey();

            Map<String, String> issue =
                    new LinkedHashMap<>(entry.getValue());

            Map<String, String> component =
                    components.get(id);

            if (component != null) {
                for (Map.Entry<String, String> field
                        : component.entrySet()) {

                    if (!"id".equals(field.getKey())) {
                        issue.put(field.getKey(), field.getValue());
                    }
                }
            }

            results.add(issue);
        }

        return results;
    }

    private Map<String, Map<String, String>> readCsvById(
            Path csvFile) throws IOException {

        Map<String, Map<String, String>> records =
                new LinkedHashMap<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (CSVParser parser = CSVParser.parse(
                csvFile,
                StandardCharsets.UTF_8,
                format)) {

            for (CSVRecord record : parser) {

                String id = record.get("id").trim();

                if (id.isEmpty()) {
                    continue;
                }

                Map<String, String> values =
                        new LinkedHashMap<>();

                for (String header : parser.getHeaderNames()) {
                    values.put(header, record.get(header));
                }

                records.put(id, values);
            }
        }

        return records;
    }

    // Preserve the original Ant baseline implementation.
    private List<Map<String, String>> getAntResults()
            throws IOException {

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

        Path latestRunFile = outputPath.resolve("latest-run.txt");

        if (!Files.isRegularFile(latestRunFile)) {
            throw new IllegalStateException(
                    "Latest Ant analysis run was not found."
            );
        }

        String latestRunId =
                Files.readString(latestRunFile).trim();

        Path latestRun = outputPath.resolve(latestRunId).normalize();

        if (latestRunId.isEmpty()
                || !latestRun.startsWith(outputPath)
                || !isCompleteAntRun(latestRun)) {

            throw new IllegalStateException(
                    "Latest Ant analysis run is missing or incomplete."
            );
        }

        Path resultsPath = latestRun
                .resolve("ant")
                .resolve("intraVersion");

        List<Map<String, String>> results = new ArrayList<>();

        for (int i = 0; i < 3; i++) {

            Path csvFile = resultsPath
                    .resolve(String.valueOf(i))
                    .resolve("ProjectMetrics.csv");

            results.add(readMetrics(csvFile, i, "Ant " + (i + 1)));
        }

        return results;
    }

    // Read the latest analysis of an uploaded project.
   
// Read the latest analysis of an uploaded project.
private List<Map<String, String>> getUploadedProjectResults(
        String projectId) throws IOException {

    Path root = Path.of(projectRoot)
            .toAbsolutePath()
            .normalize();

    Path outputBase = root.resolve(outputDirectory)
            .normalize();

    Path projectOutput = outputBase.resolve(projectId).normalize();

    // Validate the project output path.
    if (!projectOutput.startsWith(outputBase)) {
        throw new IllegalArgumentException(
                "Invalid project ID."
        );
    }

    // No analysis has been run yet.
    Path latestRunFile = projectOutput.resolve("latest-run.txt");

    if (!Files.isRegularFile(latestRunFile)) {
        return List.of();
    }

    Path latestRun = getLatestRunDirectory(projectId);
    Path metricsFile = latestRun.resolve("ProjectMetrics.csv");

    if (!Files.isRegularFile(metricsFile)) {
        return List.of();
    }

    return List.of(
            readMetrics(metricsFile, 0, "Uploaded project")
    );
}


    /**
     * Resolve the latest output directory for Ant or an uploaded project.
     */
    private Path getLatestRunDirectory(String projectId)
            throws IOException {

        Path root = Path.of(projectRoot)
                .toAbsolutePath()
                .normalize();

        Path outputBase = root.resolve(outputDirectory)
                .normalize();

        Path projectOutput;
        Path latestRunFile;

        if ("ant".equals(projectId)) {
            projectOutput = outputBase;
            latestRunFile = outputBase.resolve("latest-run.txt");
        } else {
            projectOutput = outputBase.resolve(projectId).normalize();

            if (!projectOutput.startsWith(outputBase)
                    || !Files.isDirectory(projectOutput)) {

                throw new IllegalArgumentException(
                        "Analysis output not found for project: " + projectId
                );
            }

            latestRunFile = projectOutput.resolve("latest-run.txt");
        }

        if (!Files.isRegularFile(latestRunFile)) {
            throw new IllegalStateException(
                    "Latest analysis run was not found for project: "
                            + projectId
            );
        }

        String runId = Files.readString(latestRunFile).trim();

        if (runId.isEmpty()) {
            throw new IllegalStateException(
                    "Latest analysis run ID is empty."
            );
        }

        Path latestRun = projectOutput.resolve(runId).normalize();

        if (!latestRun.startsWith(projectOutput)
                || !Files.isDirectory(latestRun)) {

            throw new IllegalStateException(
                    "Latest analysis directory is missing."
            );
        }

        return latestRun;
    }

    private Map<String, String> readMetrics(
            Path csvFile,
            int versionIndex,
            String versionLabel) throws IOException {

        List<String> lines = Files.readAllLines(csvFile);

        if (lines.size() < 2) {
            throw new IllegalStateException(
                    "Metrics file contains no result: " + csvFile
            );
        }

        String[] headers = lines.get(0).split(",", -1);
        String[] values = lines.get(1).split(",", -1);

        if (headers.length != values.length) {
            throw new IllegalStateException(
                    "Metrics CSV header and result do not match: " + csvFile
            );
        }

        Map<String, String> metrics = new LinkedHashMap<>();

        for (int i = 0; i < headers.length; i++) {
            metrics.put(headers[i].trim(), values[i].trim());
        }

        metrics.put("versionIndex", String.valueOf(versionIndex));
        metrics.put("versionLabel", versionLabel);

        return metrics;
    }

    private boolean isCompleteAntRun(Path runDirectory) {

        Path antDirectory = runDirectory.resolve("ant");

        if (!Files.isDirectory(antDirectory)) {
            return false;
        }

        Path intraVersion = antDirectory.resolve("intraVersion");

        Path interVersionMetrics = antDirectory
                .resolve("interVersion")
                .resolve("ProjectMetrics.csv");

        if (!Files.isRegularFile(interVersionMetrics)) {
            return false;
        }

        for (int i = 0; i < 3; i++) {

            Path metricsFile = intraVersion
                    .resolve(String.valueOf(i))
                    .resolve("ProjectMetrics.csv");

            if (!Files.isRegularFile(metricsFile)) {
                return false;
            }
        }

        return true;
    }
}
