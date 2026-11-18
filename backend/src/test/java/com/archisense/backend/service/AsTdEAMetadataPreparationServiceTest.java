
package com.archisense.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AsTdEAMetadataPreparationServiceTest {

    @Test
    void shouldPrepareAsTdEAInputForUploadedProject(
            @TempDir Path tempDirectory) throws Exception {

        // Existing uploaded sample project.
        String projectId = "d7aed071-4a8f-4edf-a360-485baae20012";

        Path sourceUploadDirectory = Path.of(
                "..",
                "uploads",
                projectId
        ).toAbsolutePath().normalize();

        Path projectDirectory =
                sourceUploadDirectory.resolve("source");

        // Use a separate temporary directory for generated output.
        Path uploadDirectory = tempDirectory;

        MetadataGenerationService metadataService =
                new MetadataGenerationService();

        AsTdEAMetadataPreparationService preparationService =
                new AsTdEAMetadataPreparationService(metadataService);

        Path inputDirectory = preparationService.prepareInput(
                projectDirectory,
                uploadDirectory
        );

        assertTrue(Files.isDirectory(inputDirectory));

        assertTrue(Files.isRegularFile(
                inputDirectory.resolve(
                        "uploaded-project/stats/versions.csv"
                )
        ));

        assertTrue(Files.isRegularFile(
                inputDirectory.resolve(
                        "uploaded-project/stats/dates.csv"
                )
        ));

        assertTrue(Files.isRegularFile(
                inputDirectory.resolve(
                        "uploaded-project/stats/locShared.csv"
                )
        ));

        assertTrue(Files.isRegularFile(
                inputDirectory.resolve(
                        "uploaded-project/versions/uploaded-project-uploaded-version/bin/demo/Hello.class"
                )
        ));

        assertTrue(Files.isRegularFile(
                inputDirectory.resolve(
                        "uploaded-project/versions/uploaded-project-uploaded-version/stats/LocPerClass.csv"
                )
        ));

        assertTrue(Files.isRegularFile(
                inputDirectory.resolve(
                        "uploaded-project/versions/uploaded-project-uploaded-version/stats/SharedClasses.csv"
                )
        ));

        System.out.println(
                "AsTdEA input directory: " + inputDirectory
        );

        System.out.println(
                "Metadata preparation completed successfully."
        );
    }
}
