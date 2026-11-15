
package com.archisense.backend.service;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IssueService {

    private final CsvReaderService csvReaderService;

    public IssueService(CsvReaderService csvReaderService) {
        this.csvReaderService = csvReaderService;
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

        Path basePath = Paths.get(
            "..", "results", "ant", "intraVersion",
            String.valueOf(versionIndex)
        ).toAbsolutePath().normalize();

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
