
package com.archisense.backend.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class MavenBuildService {

    private static final long BUILD_TIMEOUT_MINUTES = 10;

    public Path buildProject(Path projectDirectory)
            throws IOException, InterruptedException {

        Path project = projectDirectory.toAbsolutePath().normalize();
        Path pomFile = project.resolve("pom.xml");

        if (!Files.isDirectory(project)) {
            throw new IOException(
                    "Project directory does not exist: " + project
            );
        }

        if (!Files.isRegularFile(pomFile)) {
            throw new IOException(
                    "pom.xml not found in: " + project
            );
        }

        Path logFile = project.getParent().resolve("build.log");

        boolean windows = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("win");

        // First attempt: normal Maven build.
        List<String> normalCommand =
                createMavenCommand(windows, false);

        Files.writeString(
                logFile,
                "===== NORMAL MAVEN BUILD ====="
                        + System.lineSeparator()
        );

        int exitCode = executeBuild(
                normalCommand,
                project,
                logFile,
                false
        );

        // Retry only when Java 7 compatibility is explicitly rejected.
        if (exitCode != 0 && requiresJava8Fallback(logFile)) {

            Files.writeString(
                    logFile,
                    System.lineSeparator()
                            + "===== JAVA 8 COMPATIBILITY RETRY ====="
                            + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            List<String> fallbackCommand =
                    createMavenCommand(windows, true);

            exitCode = executeBuild(
                    fallbackCommand,
                    project,
                    logFile,
                    true
            );
        }

        if (exitCode != 0) {
            throw new IOException(
                    "Maven build failed. Check build log: " + logFile
            );
        }

        // A successful Maven build does not require Java class files.
        // Java source validation belongs to metadata preparation.
        return logFile;
    }

    private List<String> createMavenCommand(
            boolean windows,
            boolean java8Fallback
    ) {

        List<String> command = new ArrayList<>();

        if (windows) {

            command.add("cmd.exe");
            command.add("/d");
            command.add("/c");

            StringBuilder mavenCommand = new StringBuilder();

            mavenCommand.append("mvn.cmd");
            mavenCommand.append(" -B");
            mavenCommand.append(" -DskipTests");

            if (java8Fallback) {
                mavenCommand.append(" -Dmaven.compiler.source=8");
                mavenCommand.append(" -Dmaven.compiler.target=8");
                mavenCommand.append(" -Dmaven.compiler.release=8");
            }

            mavenCommand.append(" package");

            command.add(mavenCommand.toString());

        } else {

            command.add("mvn");
            command.add("-B");
            command.add("-DskipTests");

            if (java8Fallback) {
                command.add("-Dmaven.compiler.source=8");
                command.add("-Dmaven.compiler.target=8");
                command.add("-Dmaven.compiler.release=8");
            }

            command.add("package");
        }

        return command;
    }

    private int executeBuild(
            List<String> command,
            Path project,
            Path logFile,
            boolean appendLog
    ) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        processBuilder.directory(project.toFile());
        processBuilder.redirectErrorStream(true);

        processBuilder.redirectOutput(
                appendLog
                        ? ProcessBuilder.Redirect.appendTo(logFile.toFile())
                        : ProcessBuilder.Redirect.to(logFile.toFile())
        );

        Process process = processBuilder.start();

        boolean completed = process.waitFor(
                BUILD_TIMEOUT_MINUTES,
                TimeUnit.MINUTES
        );

        if (!completed) {

            process.destroyForcibly();
            process.waitFor();

            throw new IOException(
                    "Maven build timed out after "
                            + BUILD_TIMEOUT_MINUTES
                            + " minutes. Check: " + logFile
            );
        }

        return process.exitValue();
    }

    private boolean requiresJava8Fallback(Path logFile)
            throws IOException {

        String log = Files.readString(logFile)
                .toLowerCase(Locale.ROOT);

        return log.contains("source option 7 is no longer supported")
                || log.contains("target option 7 is no longer supported")
                || log.contains("source release 7 not supported")
                || log.contains("target release 7 not supported");
    }
}
