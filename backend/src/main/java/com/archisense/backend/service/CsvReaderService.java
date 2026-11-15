
package com.archisense.backend.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CsvReaderService {

    public List<Map<String, String>> readCsv(Path filePath)
            throws IOException {

        if (!Files.exists(filePath)) {
            throw new IOException(
                "CSV file not found: " + filePath
            );
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get();

        List<Map<String, String>> rows = new ArrayList<>();

        try (
            Reader reader = Files.newBufferedReader(
                filePath, StandardCharsets.UTF_8
            );
            CSVParser parser = format.parse(reader)
        ) {
            for (CSVRecord record : parser) {

                Map<String, String> row =
                        new LinkedHashMap<>();

                for (String header : parser.getHeaderNames()) {
                    row.put(header, record.get(header));
                }

                rows.add(row);
            }
        }

        return rows;
    }
}
