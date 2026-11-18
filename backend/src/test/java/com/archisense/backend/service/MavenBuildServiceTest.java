
package com.archisense.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.CleanupMode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MavenBuildServiceTest {

    @TempDir(cleanup = CleanupMode.NEVER)
    Path tempDirectory;

    @Test
    void shouldBuildUploadedMavenProject() throws Exception {

        Path projectDirectory = tempDirectory.resolve("sample-project");

        Path sourceDirectory = projectDirectory.resolve(
                "src/main/java/demo"
        );

        Files.createDirectories(sourceDirectory);

        Files.writeString(
                projectDirectory.resolve("pom.xml"),
                """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                         https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>demo</groupId>
                    <artifactId>sample-project</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.source>8</maven.compiler.source>
                        <maven.compiler.target>8</maven.compiler.target>
                        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    </properties>
                </project>
                """
        );

        Files.writeString(
                sourceDirectory.resolve("Hello.java"),
                """
                package demo;

                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello ArchiSense");
                    }
                }
                """
        );

        MavenBuildService service = new MavenBuildService();

        Path logFile = service.buildProject(projectDirectory);

        assertTrue(Files.isRegularFile(logFile));

        assertTrue(
                Files.isRegularFile(
                        projectDirectory.resolve(
                                "target/classes/demo/Hello.class"
                        )
                ),
                "Expected Maven to compile Hello.java"
        );

        System.out.println("Preserved test directory: " + tempDirectory);
        System.out.println("Maven build log: " + logFile);
    }
}
