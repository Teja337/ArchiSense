
package com.archisense.backend.service;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

@Service
public class AsTdEAMetadataPreparationService {

    private static final String VERSION =
            "uploaded-project-uploaded-version";

    private final MetadataGenerationService metadataGenerationService;

    public AsTdEAMetadataPreparationService(
            MetadataGenerationService metadataGenerationService) {
        this.metadataGenerationService = metadataGenerationService;
    }

    public Path prepareInput(Path projectDirectory, Path uploadDirectory)
            throws IOException {

        Path project = projectDirectory.toAbsolutePath().normalize();
        Path upload = uploadDirectory.toAbsolutePath().normalize();

        List<Path> sourceDirectories = discoverDirectories(
                project, "src", "main", "java");

        List<Path> classesDirectories = discoverDirectories(
                project, "target", "classes");

        if (sourceDirectories.isEmpty()) {
            throw new IOException(
                    "No Java source directories found. Expected "
                            + "src/main/java in the project or its Maven modules.");
        }

        if (classesDirectories.isEmpty()) {
            throw new IOException(
                    "No compiled target/classes directories found. "
                            + "Build the project first.");
        }

        boolean hasJavaSources = false;

        for (Path sourceDirectory : sourceDirectories) {
            try (Stream<Path> files = Files.walk(sourceDirectory)) {
                if (files.anyMatch(path ->
                        Files.isRegularFile(path)
                                && path.toString().endsWith(".java"))) {
                    hasJavaSources = true;
                    break;
                }
            }
        }

        if (!hasJavaSources) {
            throw new IOException("No Java source files found.");
        }

        boolean hasClassFiles = false;

        for (Path classesDirectory : classesDirectories) {
            try (Stream<Path> files = Files.walk(classesDirectory)) {
                if (files.anyMatch(path ->
                        Files.isRegularFile(path)
                                && path.toString().endsWith(".class"))) {
                    hasClassFiles = true;
                    break;
                }
            }
        }

        if (!hasClassFiles) {
            throw new IOException("No compiled .class files found.");
        }

        Path inputDirectory = upload.resolve("astdea-input");

        if (Files.exists(inputDirectory)) {
            deleteDirectory(inputDirectory);
        }

        Path projectInputDirectory =
                inputDirectory.resolve("uploaded-project");

        Path statsDirectory =
                projectInputDirectory.resolve("stats");

        Path versionDirectory = projectInputDirectory
                .resolve("versions")
                .resolve(VERSION);

        Path binDirectory = versionDirectory.resolve("bin");

        Path versionStatsDirectory =
                versionDirectory.resolve("stats");

        Files.createDirectories(statsDirectory);
        Files.createDirectories(binDirectory);
        Files.createDirectories(versionStatsDirectory);

        // Combine compiled classes from all Maven modules.
        copyModuleClasses(classesDirectories, binDirectory);

        // Generate per-class LOC metadata across all source roots.
        Path locPerClassFile =
                versionStatsDirectory.resolve("LocPerClass.csv");

        metadataGenerationService.generateLocPerClass(
                sourceDirectories,
                project,
                locPerClassFile
        );

        // Generate the unique class whitelist required by Arcan.
        Path sharedClassesFile =
                versionStatsDirectory.resolve("SharedClasses.csv");

        generateSharedClasses(binDirectory, sharedClassesFile);

        // Count physical source lines across all modules.
        long totalLoc = calculatePhysicalSourceLines(sourceDirectories);

        writeCsv(
                statsDirectory.resolve("versions.csv"),
                "version",
                VERSION
        );

        writeCsv(
                statsDirectory.resolve("dates.csv"),
                "date",
                LocalDate.now().toString()
        );

        writeCsv(
                statsDirectory.resolve("locShared.csv"),
                "LOC",
                String.valueOf(totalLoc)
        );

        return inputDirectory;
    }

    private List<Path> discoverDirectories(
            Path project,
            String... directoryParts) throws IOException {

        List<Path> discovered = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(project)) {

            paths.filter(Files::isDirectory)
                    .filter(path -> {
                        Path relative = project.relativize(path);

                        if (relative.getNameCount() < directoryParts.length) {
                            return false;
                        }

                        int offset =
                                relative.getNameCount() - directoryParts.length;

                        for (int i = 0; i < directoryParts.length; i++) {
                            if (!relative.getName(offset + i)
                                    .toString()
                                    .equals(directoryParts[i])) {
                                return false;
                            }
                        }

                        return true;
                    })
                    .sorted()
                    .forEach(discovered::add);
        }

        return discovered;
    }

    private void copyModuleClasses(
            List<Path> classesDirectories,
            Path binDirectory) throws IOException {

        Map<Path, Path> copiedClasses = new LinkedHashMap<>();

        for (Path classesDirectory : classesDirectories) {

            try (Stream<Path> paths = Files.walk(classesDirectory)) {

                List<Path> classFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path ->
                                path.toString().endsWith(".class"))
                        .sorted()
                        .toList();

                for (Path classFile : classFiles) {

                    Path relativeClass =
                            classesDirectory.relativize(classFile);

                    Path destination =
                            binDirectory.resolve(relativeClass);

                    if (Files.exists(destination)) {

                        if (!MessageDigest.isEqual(
                                sha256(destination),
                                sha256(classFile))) {

                            throw new IOException(
                                    "Conflicting compiled classes found for "
                                            + relativeClass
                                            + ". Multiple Maven modules contain "
                                            + "different bytecode for the same class.");
                        }

                        // Identical compiled class already copied.
                        continue;
                    }

                    Files.createDirectories(destination.getParent());

                    Files.copy(
                            classFile,
                            destination,
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    copiedClasses.put(relativeClass, classFile);
                }
            }
        }
    }

    private byte[] sha256(Path file)
            throws IOException {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (var input = Files.newInputStream(file)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return digest.digest();

        } catch (NoSuchAlgorithmException exception) {
            throw new IOException(
                    "SHA-256 algorithm is not available.", exception);
        }
    }

    private void generateSharedClasses(
            Path binDirectory,
            Path outputFile) throws IOException {

        Set<String> classNames = new TreeSet<>();

        try (Stream<Path> paths = Files.walk(binDirectory)) {

            List<Path> classFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".class"))
                    .sorted()
                    .toList();

            for (Path classFile : classFiles) {

                try {
                    JavaClass javaClass = new ClassParser(
                            classFile.toString()
                    ).parse();

                    classNames.add(javaClass.getClassName());

                } catch (Exception exception) {
                    throw new IOException(
                            "Unable to parse compiled class: " + classFile,
                            exception
                    );
                }
            }
        }

        StringBuilder csv =
                new StringBuilder("fullyQualifiedName");

        for (String className : classNames) {
            csv.append(System.lineSeparator()).append(className);
        }

        Files.writeString(
                outputFile,
                csv.append(System.lineSeparator()).toString()
        );
    }

    private long calculatePhysicalSourceLines(
            List<Path> sourceDirectories) throws IOException {

        long total = 0;

        for (Path sourceDirectory : sourceDirectories) {

            try (Stream<Path> paths = Files.walk(sourceDirectory)) {

                List<Path> javaFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path ->
                                path.toString().endsWith(".java"))
                        .toList();

                for (Path javaFile : javaFiles) {

                    try (Stream<String> lines = Files.lines(javaFile)) {
                        total += lines.count();
                    }
                }
            }
        }

        if (total == 0) {
            throw new IOException("No Java source lines found.");
        }

        return total;
    }

    private void writeCsv(Path file, String header, String value)
            throws IOException {

        Files.writeString(
                file,
                header + System.lineSeparator()
                        + value + System.lineSeparator()
        );
    }

    private void deleteDirectory(Path directory) throws IOException {

        try (Stream<Path> paths = Files.walk(directory)) {

            List<Path> allPaths = paths
                    .sorted(Comparator.reverseOrder())
                    .toList();

            for (Path path : allPaths) {

                try {
                    Files.deleteIfExists(path);

                } catch (AccessDeniedException exception) {

                    if (Files.isDirectory(path)) {

                        try (Stream<Path> contents = Files.list(path)) {
                            if (contents.findAny().isEmpty()) {
                                continue;
                            }
                        }
                    }

                    throw exception;
                }
            }
        }
    }
}
