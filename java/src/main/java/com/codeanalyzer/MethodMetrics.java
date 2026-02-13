package com.codeanalyzer;

public class MethodMetrics {
    private final String name;
    private final String signature;
    private int lineStart;
    private int lineEnd;
    private int cyclomaticComplexity;
    private int linesOfCode;
    private double maintainabilityIndex;

    public MethodMetrics(String name, String signature) {
        this.name = name;
        this.signature = signature;
    }

    public String getName() { return name; }
    public String getSignature() { return signature; }
    public int getLineStart() { return lineStart; }
    public void setLineStart(int lineStart) { this.lineStart = lineStart; }
    public int getLineEnd() { return lineEnd; }
    public void setLineEnd(int lineEnd) { this.lineEnd = lineEnd; }
    public int getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(int cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }
    public int getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(int linesOfCode) { this.linesOfCode = linesOfCode; }
    public double getMaintainabilityIndex() { return maintainabilityIndex; }
    public void setMaintainabilityIndex(double maintainabilityIndex) { this.maintainabilityIndex = maintainabilityIndex; }
}
