package com.example.csvgenerator;

import java.nio.file.Path;
import java.util.List;

public record CsvGenerationResult(
        CsvFileType fileType,
        long rowsWritten,
        long bytesWritten,
        int parts,
        List<Path> outputFiles
) {
}
