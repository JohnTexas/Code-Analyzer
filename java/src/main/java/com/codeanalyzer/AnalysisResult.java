package com.codeanalyzer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AnalysisResult {
    private final List<FileMetrics> files = new ArrayList<>();
    private final List<DuplicateBlock> duplicates = new ArrayList<>();
    private String projectPath = "";
    private Instant analyzedAt = Instant.now();

    public List<FileMetrics> getFiles() { return files; }
    public List<DuplicateBlock> getDuplicates() { return duplicates; }
    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }
    public Instant getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(Instant analyzedAt) { this.analyzedAt = analyzedAt; }
}
