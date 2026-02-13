package com.codeanalyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : System.getProperty("user.dir");
        String format = "html";
        String outputPath = null;

        for (int i = 1; i < args.length; i++) {
            if ("--format".equals(args[i]) || "-f".equals(args[i])) {
                if (i + 1 < args.length) format = args[++i].toLowerCase();
            } else if ("--output".equals(args[i]) || "-o".equals(args[i])) {
                if (i + 1 < args.length) outputPath = args[++i];
            }
        }

        Path root = Paths.get(path).toAbsolutePath();
        if (!Files.isDirectory(root)) {
            System.err.println("Error: Directory not found: " + path);
            System.exit(1);
        }

        System.out.println("Analyzing Java source in: " + root);
        List<FileMetrics> files = JavaAnalyzer.analyzeDirectory(root);
        if (files.isEmpty()) {
            System.out.println("No .java files found or all failed to parse.");
            return;
        }

        List<DuplicateBlock> duplicates = DuplicationDetector.detectDuplicates(files);
        String projectName = root.getFileName() != null ? root.getFileName().toString() : path;
        AnalysisResult result = new AnalysisResult();
        result.setProjectPath(projectName);
        result.setAnalyzedAt(Instant.now());
        result.getFiles().addAll(files);
        result.getDuplicates().addAll(duplicates);

        String report = "csv".equals(format)
                ? ReportGenerator.generateCsv(result)
                : ReportGenerator.generateHtml(result);
        String defaultName = "csv".equals(format) ? "code-metrics-report.csv" : "code-metrics-report.html";
        Path outPath = outputPath != null ? Paths.get(outputPath) : root.resolve(defaultName);
        Files.writeString(outPath, report);

        System.out.println("Report written to: " + outPath);
        System.out.println("Files analyzed: " + files.size() + ", Duplicate groups: " + duplicates.size());
    }
}
