package com.codeanalyzer;

import java.util.ArrayList;
import java.util.List;

public class DuplicateBlock {
    private final String normalizedHash;
    private final List<DuplicateOccurrence> occurrences = new ArrayList<>();
    private int tokenCount;

    public DuplicateBlock(String normalizedHash) {
        this.normalizedHash = normalizedHash;
    }

    public String getNormalizedHash() { return normalizedHash; }
    public List<DuplicateOccurrence> getOccurrences() { return occurrences; }
    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }
}
