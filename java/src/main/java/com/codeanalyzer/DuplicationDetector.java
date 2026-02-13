package com.codeanalyzer;

import java.util.*;
import java.util.stream.Collectors;

public final class DuplicationDetector {

    private static final int MIN_OCCURRENCES = 2;

    public static List<DuplicateBlock> detectDuplicates(List<FileMetrics> allFiles) {
        Map<String, List<Occurrence>> byHash = new HashMap<>();
        for (FileMetrics file : allFiles) {
            for (MethodMetrics m : file.getMethods()) {
                if (m.getLinesOfCode() < 5) continue;
                String fingerprint = m.getSignature() + "|" + m.getLinesOfCode() + "|" + m.getCyclomaticComplexity();
                String hash = Integer.toHexString(fingerprint.hashCode());
                String preview = m.getSignature() + " (" + m.getLinesOfCode() + " lines, CC=" + m.getCyclomaticComplexity() + ")";
                byHash.computeIfAbsent(hash, k -> new ArrayList<>())
                        .add(new Occurrence(file.getFilePath(), m.getLineStart(), m.getLineEnd(), preview));
            }
        }
        List<DuplicateBlock> result = new ArrayList<>();
        for (Map.Entry<String, List<Occurrence>> e : byHash.entrySet()) {
            if (e.getValue().size() < MIN_OCCURRENCES) continue;
            long distinctFiles = e.getValue().stream().map(o -> o.filePath).distinct().count();
            if (distinctFiles < 2) continue;
            DuplicateBlock block = new DuplicateBlock(e.getKey());
            block.setTokenCount(e.getValue().get(0).preview.length());
            for (Occurrence o : e.getValue()) {
                DuplicateOccurrence doc = new DuplicateOccurrence(o.filePath);
                doc.setLineStart(o.lineStart);
                doc.setLineEnd(o.lineEnd);
                doc.setPreview(o.preview);
                block.getOccurrences().add(doc);
            }
            result.add(block);
        }
        return result;
    }

    private record Occurrence(String filePath, int lineStart, int lineEnd, String preview) {}
}
