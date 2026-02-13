package com.codeanalyzer;

import java.util.ArrayList;
import java.util.List;

public class FileMetrics {
    private final String filePath;
    private final String relativePath;
    private final List<MethodMetrics> methods = new ArrayList<>();
    private int totalLines;
    private int codeLines;
    private int commentLines;
    private double maintainabilityIndex;
    private int cyclomaticComplexity;

    public FileMetrics(String filePath, String relativePath) {
        this.filePath = filePath;
        this.relativePath = relativePath;
    }

    public String getFilePath() { return filePath; }
    public String getRelativePath() { return relativePath; }
    public List<MethodMetrics> getMethods() { return methods; }
    public int getTotalLines() { return totalLines; }
    public void setTotalLines(int totalLines) { this.totalLines = totalLines; }
    public int getCodeLines() { return codeLines; }
    public void setCodeLines(int codeLines) { this.codeLines = codeLines; }
    public int getCommentLines() { return commentLines; }
    public void setCommentLines(int commentLines) { this.commentLines = commentLines; }
    public double getMaintainabilityIndex() { return maintainabilityIndex; }
    public void setMaintainabilityIndex(double maintainabilityIndex) { this.maintainabilityIndex = maintainabilityIndex; }
    public int getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(int cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }
}
