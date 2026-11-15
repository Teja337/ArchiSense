package com.archisense.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/projects/ant/versions")
public class VersionController {

    private final Path versionsPath = Paths.get(
        "..", "datasets", "ant", "versions"
    ).toAbsolutePath().normalize();

    @GetMapping
    public List<String> getVersions() throws Exception {

        if (!Files.isDirectory(versionsPath)) {
            throw new IllegalStateException(
                "Apache Ant dataset directory not found: " + versionsPath
            );
        }

        try (Stream<Path> paths = Files.list(versionsPath)) {
            return paths
                .filter(Files::isDirectory)
                .map(path -> path.getFileName().toString())
                .filter(name -> name.matches("ant-\\d+\\.\\d+\\.\\d+"))
                .sorted()
                .toList();
        }
    }
}