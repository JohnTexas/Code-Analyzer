package com.codeanalyzer;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class ReportGenerator {

    public static String generateCsv(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Report Type,FilePath,RelativePath,TotalLines,CodeLines,CommentLines,CyclomaticComplexity,MaintainabilityIndex,MethodName,MethodCC,MethodLOC,MethodMI\n");
        for (FileMetrics file : result.getFiles()) {
            if (file.getMethods().isEmpty()) {
                sb.append("File,").append(escape(file.getFilePath())).append(",").append(escape(file.getRelativePath()))
                        .append(",").append(file.getTotalLines()).append(",").append(file.getCodeLines())
                        .append(",").append(file.getCommentLines()).append(",").append(file.getCyclomaticComplexity())
                        .append(",").append(String.format("%.1f", file.getMaintainabilityIndex())).append(",,,,\n");
                continue;
            }
            for (MethodMetrics m : file.getMethods()) {
                sb.append("File,").append(escape(file.getFilePath())).append(",").append(escape(file.getRelativePath()))
                        .append(",").append(file.getTotalLines()).append(",").append(file.getCodeLines())
                        .append(",").append(file.getCommentLines()).append(",").append(file.getCyclomaticComplexity())
                        .append(",").append(String.format("%.1f", file.getMaintainabilityIndex()))
                        .append(",").append(escape(m.getName())).append(",").append(m.getCyclomaticComplexity())
                        .append(",").append(m.getLinesOfCode()).append(",").append(String.format("%.1f", m.getMaintainabilityIndex())).append("\n");
            }
        }
        sb.append("\nDuplicates,Hash,FilePath,LineStart,LineEnd,Preview\n");
        for (DuplicateBlock dup : result.getDuplicates()) {
            for (DuplicateOccurrence occ : dup.getOccurrences()) {
                sb.append("Duplicate,").append(dup.getNormalizedHash()).append(",").append(escape(occ.getFilePath()))
                        .append(",").append(occ.getLineStart()).append(",").append(occ.getLineEnd())
                        .append(",").append(escape(occ.getPreview() != null ? occ.getPreview() : "")).append("\n");
            }
        }
        return sb.toString();
    }

    public static String generateHtml(AnalysisResult result) {
        String projectPath = escapeHtml(result.getProjectPath());
        String analyzedAt = result.getAnalyzedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        int totalFiles = result.getFiles().size();
        int totalMethods = result.getFiles().stream().mapToInt(f -> f.getMethods().size()).sum();
        int totalComplexity = result.getFiles().stream().mapToInt(FileMetrics::getCyclomaticComplexity).sum();
        double avgMi = totalMethods > 0
                ? result.getFiles().stream().flatMap(f -> f.getMethods().stream()).mapToDouble(MethodMetrics::getMaintainabilityIndex).average().orElse(0)
                : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>Code Quality Report - ").append(projectPath).append("</title>\n<style>\n");
        sb.append("body { font-family: 'Segoe UI', system-ui, sans-serif; margin: 0; padding: 24px; background: #0f0f12; color: #e4e4e7; }\n");
        sb.append("h1 { color: #fafafa; font-weight: 600; }\nh2 { color: #a1a1aa; margin-top: 32px; font-size: 1.1rem; }\n");
        sb.append("table { border-collapse: collapse; width: 100%; margin-top: 12px; }\n");
        sb.append("th, td { border: 1px solid #27272a; padding: 10px 12px; text-align: left; }\n");
        sb.append("th { background: #18181b; color: #a1a1aa; font-weight: 600; }\ntr:nth-child(even) { background: #18181b; }\n");
        sb.append(".bad { color: #f87171; }\n.warn { color: #fbbf24; }\n.good { color: #4ade80; }\n");
        sb.append(".summary { display: flex; gap: 24px; flex-wrap: wrap; margin: 20px 0; }\n");
        sb.append(".summary .card { background: #18181b; border: 1px solid #27272a; border-radius: 8px; padding: 16px 24px; min-width: 140px; }\n");
        sb.append(".summary .card .value { font-size: 1.5rem; font-weight: 700; }\n</style>\n</head>\n<body>\n");
        sb.append("<h1>Code Quality Report</h1>\n<p>Project: <code>").append(projectPath).append("</code></p>\n");
        sb.append("<p>Generated: ").append(analyzedAt).append("</p>\n");
        sb.append("<div class=\"summary\">");
        sb.append("<div class=\"card\"><span class=\"value\">").append(totalFiles).append("</span><br>Files</div>");
        sb.append("<div class=\"card\"><span class=\"value\">").append(totalMethods).append("</span><br>Methods</div>");
        sb.append("<div class=\"card\"><span class=\"value\">").append(totalComplexity).append("</span><br>Total Cyclomatic Complexity</div>");
        sb.append("<div class=\"card\"><span class=\"value ").append(miClass(avgMi)).append("\">").append(String.format("%.1f", avgMi)).append("</span><br>Avg Maintainability</div>");
        sb.append("<div class=\"card\"><span class=\"value\">").append(result.getDuplicates().size()).append("</span><br>Duplicate Groups</div>");
        sb.append("</div>\n");

        sb.append("<h2>File &amp; Method Metrics</h2>\n<table>\n<thead><tr><th>File</th><th>Lines</th><th>Code</th><th>File CC</th><th>File MI</th><th>Method</th><th>Method CC</th><th>LOC</th><th>Method MI</th></tr></thead>\n<tbody>\n");
        result.getFiles().stream().sorted(Comparator.comparing(FileMetrics::getRelativePath)).forEach(file -> {
            if (file.getMethods().isEmpty()) {
                sb.append("<tr><td>").append(escapeHtml(file.getRelativePath())).append("</td><td>").append(file.getTotalLines())
                        .append("</td><td>").append(file.getCodeLines()).append("</td><td>").append(file.getCyclomaticComplexity())
                        .append("</td><td class=\"").append(miClass(file.getMaintainabilityIndex())).append("\">").append(String.format("%.1f", file.getMaintainabilityIndex()))
                        .append("</td><td colspan=\"4\">—</td></tr>\n");
                return;
            }
            boolean[] first = { true };
            for (MethodMetrics m : file.getMethods()) {
                sb.append("<tr>");
                if (first[0]) {
                    sb.append("<td rowspan=\"").append(file.getMethods().size()).append("\">").append(escapeHtml(file.getRelativePath()))
                            .append("</td><td rowspan=\"").append(file.getMethods().size()).append("\">").append(file.getTotalLines())
                            .append("</td><td rowspan=\"").append(file.getMethods().size()).append("\">").append(file.getCodeLines())
                            .append("</td><td rowspan=\"").append(file.getMethods().size()).append("\">").append(file.getCyclomaticComplexity())
                            .append("</td><td rowspan=\"").append(file.getMethods().size()).append("\" class=\"").append(miClass(file.getMaintainabilityIndex()))
                            .append("\">").append(String.format("%.1f", file.getMaintainabilityIndex())).append("</td>");
                    first[0] = false;
                }
                sb.append("<td>").append(escapeHtml(m.getName())).append("</td><td class=\"").append(ccClass(m.getCyclomaticComplexity()))
                        .append("\">").append(m.getCyclomaticComplexity()).append("</td><td>").append(m.getLinesOfCode())
                        .append("</td><td class=\"").append(miClass(m.getMaintainabilityIndex())).append("\">").append(String.format("%.1f", m.getMaintainabilityIndex())).append("</td></tr>\n");
            }
        });
        sb.append("</tbody></table>\n");

        if (!result.getDuplicates().isEmpty()) {
            sb.append("<h2>Code Duplication</h2>\n<table>\n<thead><tr><th>Hash</th><th>Occurrences</th><th>Locations</th></tr></thead>\n<tbody>\n");
            for (DuplicateBlock dup : result.getDuplicates()) {
                String locations = dup.getOccurrences().stream()
                        .map(o -> escapeHtml(o.getFilePath()) + " L" + o.getLineStart() + "-" + o.getLineEnd())
                        .collect(Collectors.joining("; "));
                sb.append("<tr><td><code>").append(escapeHtml(dup.getNormalizedHash())).append("</code></td><td>").append(dup.getOccurrences().size())
                        .append("</td><td>").append(locations).append("</td></tr>\n");
            }
            sb.append("</tbody></table>\n");
        }
        sb.append("</body></html>\n");
        return sb.toString();
    }

    private static String ccClass(int cc) {
        if (cc > 15) return "bad";
        if (cc > 10) return "warn";
        return "good";
    }

    private static String miClass(double mi) {
        if (mi < 20) return "bad";
        if (mi < 65) return "warn";
        return "good";
    }

    private static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
