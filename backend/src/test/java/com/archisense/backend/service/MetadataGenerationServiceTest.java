
package com.archisense.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetadataGenerationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateLocPerClassCsv() throws Exception {

        // Arrange: create a sample Java source file
        Path sourceDirectory = tempDir.resolve("src");
        Path javaFile = sourceDirectory
                .resolve("demo/Hello.java");

        Files.createDirectories(javaFile.getParent());

        String sourceCode = """
                package demo;

                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("ArchiSense upload test");
                    }
                }
                """;

        Files.writeString(javaFile, sourceCode);

        Path outputFile = tempDir.resolve(
                "stats/LocPerClass.csv");

        MetadataGenerationService service =
                new MetadataGenerationService();

        // Act: generate the metadata CSV
        Path generatedFile = service.generateLocPerClass(
                sourceDirectory, outputFile);

        // Assert: verify the generated file
        assertTrue(Files.exists(generatedFile));

        List<String> lines = Files.readAllLines(generatedFile);

        assertEquals(
                "FilePath,fullyQualifiedName,loc,UnnamedPackage",
                lines.get(0));

        assertEquals(2, lines.size());

        assertTrue(lines.get(1).contains("demo.Hello"));
        assertTrue(lines.get(1).endsWith(",5,false"));

        System.out.println("Generated CSV:");
        lines.forEach(System.out::println);
    }
}
