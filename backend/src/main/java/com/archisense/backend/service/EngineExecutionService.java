
package com.archisense.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class EngineExecutionService {

    @Value("${archisense.project-root:..}")
    private String projectRoot;

    @Value("${archisense.engine.jar}")
    private String engineJar;

    @Value("${archisense.engine.arcan-jar}")
    private String arcanJar;

    @Value("${archisense.engine.lib-dir}")
    private String libDir;

    @Value("${archisense.output-dir}")
    private String outputDir;

    public Path executeAnalysis(Path inputDirectory, String projectId)
            throws IOException, InterruptedException {

        Path root = Path.of(projectRoot)
                .toAbsolutePath()
                .normalize();

        Path input = inputDirectory
                .toAbsolutePath()
                .normalize();

        Path engine = root.resolve(engineJar).normalize();
        Path arcan = root.resolve(arcanJar).normalize();
        Path libraries = root.resolve(libDir).normalize();
        Path outputBase = root.resolve(outputDir).normalize();

        validateEngine(engine, arcan, libraries, input);

        boolean isAnt = "ant".equalsIgnoreCase(projectId);

        String runId = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        ) + "-" + UUID.randomUUID().toString().substring(0, 8);

        Path projectOutput = isAnt
                ? outputBase
                : outputBase.resolve(projectId).normalize();

        if (!projectOutput.startsWith(outputBase)) {
            throw new IOException("Invalid project ID.");
        }

        Path runOutput = projectOutput.resolve(runId);
        Files.createDirectories(runOutput);

        Path logFile = runOutput.resolve("engine.log");

        String javaExecutable = Path.of(
                System.getProperty("java.home"),
                "bin",
                System.getProperty("os.name").toLowerCase().contains("win")
                        ? "java.exe"
                        : "java"
        ).toString();

        String libraryWildcard = libraries
                + System.getProperty("file.separator") + "*";

        String classPath = String.join(
                System.getProperty("path.separator"),
                engine.toString(),
                arcan.toString(),
                libraryWildcard
        );

        List<String> command = new ArrayList<>();

        command.add(javaExecutable);
        command.add("-cp");
        command.add(classPath);

        if (isAnt) {

            // Use the original AsTdEA multi-version wrapper.
            command.add("org.astdea.TerminalExecutor");

            // The wrapper expects a parent directory containing
            // the project folder (datasets/ant).
            Path inputRoot = input.getParent();

            if (inputRoot == null || !Files.isDirectory(inputRoot)) {
                throw new IOException(
                        "Ant dataset parent directory not found: " + inputRoot
                );
            }

            if (!input.getFileName().toString().equalsIgnoreCase(projectId)) {
                throw new IOException(
                        "Ant input directory must be named '" + projectId
                                + "': " + input
                );
            }

            command.add("-inputDir");
            command.add(inputRoot.toString());

            command.add("-outputDir");
            command.add(runOutput.toString());

            // Preserve the additional AsTdEA metrics.
            command.add("-noSup");

        } else {

            // Keep the existing uploaded-project execution workflow.
            command.add("it.unimib.disco.essere.main.TerminalExecutor");

            Path projectInput = input.resolve("uploaded-project");

            Path versionsFile =
                    projectInput.resolve("stats/versions.csv");

            Path versionDirectory = projectInput
                    .resolve("versions")
                    .resolve("uploaded-project-uploaded-version");

            Path binDirectory = versionDirectory.resolve("bin");

            Path sharedClassesFile = versionDirectory
                    .resolve("stats")
                    .resolve("SharedClasses.csv");

            Path locFile = projectInput.resolve("stats/locShared.csv");

            if (!Files.isRegularFile(versionsFile)) {
                throw new IOException(
                        "AsTdEA versions.csv not found: " + versionsFile
                );
            }

            if (!Files.isDirectory(binDirectory)) {
                throw new IOException(
                        "Compiled classes directory not found: " + binDirectory
                );
            }

            if (!Files.isRegularFile(sharedClassesFile)) {
                throw new IOException(
                        "SharedClasses.csv not found: " + sharedClassesFile
                );
            }

            if (!Files.isRegularFile(locFile)) {
                throw new IOException(
                        "locShared.csv not found: " + locFile
                );
            }

            List<String> locLines = Files.readAllLines(locFile);

            if (locLines.size() < 2
                    || !"LOC".equalsIgnoreCase(locLines.get(0).trim())) {
                throw new IOException(
                        "Invalid locShared.csv format: " + locFile
                );
            }

            long totalLoc;

            try {
                totalLoc = Long.parseLong(locLines.get(1).trim());
            } catch (NumberFormatException exception) {
                throw new IOException(
                        "Invalid LOC value in locShared.csv: " + locFile,
                        exception
                );
            }

            if (totalLoc <= 0) {
                throw new IOException(
                        "Project LOC must be greater than zero."
                );
            }

            command.add("-p");
            command.add(binDirectory.toString());

            command.add("-class");

            command.add("-cf");
            command.add(sharedClassesFile.toString());

            command.add("-loc");
            command.add(String.valueOf(totalLoc));

            command.add("-out");
            command.add(runOutput.toString());

            command.add("-all");
            command.add("-asTdEvolution");
            command.add("-mute");
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(root.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(logFile.toFile());

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException(
                    "AsTdEA analysis failed with exit code "
                            + exitCode + ". Check log: " + logFile
            );
        }

        try (Stream<Path> paths = Files.walk(runOutput)) {

            boolean hasOutput = paths.anyMatch(path ->
                    !path.equals(runOutput)
                            && !path.equals(logFile)
                            && Files.isRegularFile(path)
            );

            if (!hasOutput) {
                throw new IOException(
                        "AsTdEA finished but produced no analysis files. "
                                + "Check log: " + logFile
                );
            }
        }

        Files.writeString(
                projectOutput.resolve("latest-run.txt"),
                runId
        );

        return runOutput;
    }

    private void validateEngine(
            Path engine,
            Path arcan,
            Path libraries,
            Path input) throws IOException {

        if (!Files.isDirectory(input)) {
            throw new IOException(
                    "AsTdEA input directory not found: " + input
            );
        }

        if (!Files.isRegularFile(engine)) {
            throw new IOException(
                    "AsTdEA JAR not found: " + engine
            );
        }

        if (!Files.isRegularFile(arcan)) {
            throw new IOException(
                    "Arcan JAR not found: " + arcan
            );
        }

        if (!Files.isDirectory(libraries)) {
            throw new IOException(
                    "Engine library directory not found: " + libraries
            );
        }
    }
}
