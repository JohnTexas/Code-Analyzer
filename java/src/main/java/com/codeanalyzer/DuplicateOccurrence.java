package com.codeanalyzer;

public class DuplicateOccurrence {
    private final String filePath;
    private int lineStart;
    private int lineEnd;
    private String preview;

    public DuplicateOccurrence(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() { return filePath; }
    public int getLineStart() { return lineStart; }
    public void setLineStart(int lineStart) { this.lineStart = lineStart; }
    public int getLineEnd() { return lineEnd; }
    public void setLineEnd(int lineEnd) { this.lineEnd = lineEnd; }
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
}
