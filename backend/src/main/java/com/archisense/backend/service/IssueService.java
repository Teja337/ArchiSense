
package com.archisense.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IssueService {

    private final CsvReaderService csvReaderService;

    @Value("${archisense.project-root:..}")
    private String projectRoot;

    @Value("${archisense.output-dir:runtime/engine-output}")
    private String outputDirectory;

    public IssueService(CsvReaderService csvReaderService) {
        this.csvReaderService = csvReaderService;
    }

    private Path findLatestCompleteRun() throws Exception {

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

        Path antDirectory = latestRun.resolve("ant");

        if (!Files.isDirectory(antDirectory)) {
            throw new IllegalStateException(
                    "Latest analysis run is incomplete: " + latestRunId
            );
        }

        return latestRun;
    }

    private List<Map<String, Object>> getIssues(
            int versionIndex,
            String componentFileName,
            String propertyFileName) throws Exception {

        if (versionIndex < 0 || versionIndex > 2) {
            throw new IllegalArgumentException(
                    "Invalid version index: " + versionIndex
            );
        }

        Path latestRun = findLatestCompleteRun();

        Path basePath = latestRun
                .resolve("ant")
                .resolve("intraVersion")
                .resolve(String.valueOf(versionIndex));

        List<Map<String, String>> components =
                csvReaderService.readCsv(
                        basePath.resolve(componentFileName)
                );

        List<Map<String, String>> properties =
                csvReaderService.readCsv(
                        basePath.resolve(propertyFileName)
                );

        Map<String, Map<String, String>> propertiesById =
                new LinkedHashMap<>();

        for (Map<String, String> property : properties) {
            propertiesById.put(property.get("id"), property);
        }

        List<Map<String, Object>> issues = new ArrayList<>();

        for (Map<String, String> component : components) {

            String id = component.get("id");
            Map<String, String> property = propertiesById.get(id);

            if (property == null) {
                continue;
            }

            Map<String, Object> issue = new LinkedHashMap<>();

            issue.putAll(component);
            issue.putAll(property);

            issues.add(issue);
        }

        return issues;
    }

    public List<Map<String, Object>> getClassCycles(
            int versionIndex) throws Exception {

        return getIssues(
                versionIndex,
                "ClassCDsComponents.csv",
                "ClassCDsProperties.csv"
        );
    }

    public List<Map<String, Object>> getPackageCycles(
            int versionIndex) throws Exception {

        return getIssues(
                versionIndex,
                "PackageCDsComponents.csv",
                "PackageCDsProperties.csv"
        );
    }

    public List<Map<String, Object>> getHubDependencies(
            int versionIndex) throws Exception {

        return getIssues(
                versionIndex,
                "HDsComponents.csv",
                "HDsProperties.csv"
        );
    }

    public List<Map<String, Object>> getUnstableDependencies(
            int versionIndex) throws Exception {

        return getIssues(
                versionIndex,
                "UDsComponents.csv",
                "UDsProperties.csv"
        );
    }
}
