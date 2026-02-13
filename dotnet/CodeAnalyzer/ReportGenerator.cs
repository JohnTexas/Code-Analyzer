using System.Text;

namespace CodeAnalyzer;

public static class ReportGenerator
{
    public static string GenerateCsv(AnalysisResult result)
    {
        var sb = new StringBuilder();
        sb.AppendLine("Report Type,FilePath,RelativePath,TotalLines,CodeLines,CommentLines,CyclomaticComplexity,MaintainabilityIndex,MethodName,MethodCC,MethodLOC,MethodMI");
        foreach (var file in result.Files)
        {
            if (file.Methods.Count == 0)
            {
                sb.AppendLine($"File,{Escape(file.FilePath)},{Escape(file.RelativePath)},{file.TotalLines},{file.CodeLines},{file.CommentLines},{file.CyclomaticComplexity},{file.MaintainabilityIndex:F1},,,,");
                continue;
            }
            foreach (var m in file.Methods)
            {
                sb.AppendLine($"File,{Escape(file.FilePath)},{Escape(file.RelativePath)},{file.TotalLines},{file.CodeLines},{file.CommentLines},{file.CyclomaticComplexity},{file.MaintainabilityIndex:F1},{Escape(m.Name)},{m.CyclomaticComplexity},{m.LinesOfCode},{m.MaintainabilityIndex:F1}");
            }
        }
        sb.AppendLine();
        sb.AppendLine("Duplicates,Hash,FilePath,LineStart,LineEnd,Preview");
        foreach (var dup in result.Duplicates)
        {
            foreach (var occ in dup.Occurrences)
            {
                sb.AppendLine($"Duplicate,{dup.NormalizedHash},{Escape(occ.FilePath)},{occ.LineStart},{occ.LineEnd},{Escape(occ.Preview ?? "")}");
            }
        }
        return sb.ToString();
    }

    public static string GenerateHtml(AnalysisResult result)
    {
        var sb = new StringBuilder();
        sb.AppendLine("<!DOCTYPE html>");
        sb.AppendLine("<html lang=\"en\">");
        sb.AppendLine("<head>");
        sb.AppendLine("<meta charset=\"UTF-8\">");
        sb.AppendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        sb.AppendLine("<title>Code Quality Report - " + EscapeHtml(result.ProjectPath) + "</title>");
        sb.AppendLine("<style>");
        sb.AppendLine(@"
body { font-family: 'Segoe UI', system-ui, sans-serif; margin: 0; padding: 24px; background: #0f0f12; color: #e4e4e7; }
h1 { color: #fafafa; font-weight: 600; }
h2 { color: #a1a1aa; margin-top: 32px; font-size: 1.1rem; }
table { border-collapse: collapse; width: 100%; margin-top: 12px; }
th, td { border: 1px solid #27272a; padding: 10px 12px; text-align: left; }
th { background: #18181b; color: #a1a1aa; font-weight: 600; }
tr:nth-child(even) { background: #18181b; }
.bad { color: #f87171; }
.warn { color: #fbbf24; }
.good { color: #4ade80; }
.summary { display: flex; gap: 24px; flex-wrap: wrap; margin: 20px 0; }
.summary .card { background: #18181b; border: 1px solid #27272a; border-radius: 8px; padding: 16px 24px; min-width: 140px; }
.summary .card .value { font-size: 1.5rem; font-weight: 700; }
pre { background: #18181b; padding: 12px; border-radius: 6px; overflow-x: auto; font-size: 0.85rem; }
");
        sb.AppendLine("</style>");
        sb.AppendLine("</head>");
        sb.AppendLine("<body>");
        sb.AppendLine("<h1>Code Quality Report</h1>");
        sb.AppendLine("<p>Project: <code>" + EscapeHtml(result.ProjectPath) + "</code></p>");
        sb.AppendLine("<p>Generated: " + result.AnalyzedAt.ToString("yyyy-MM-dd HH:mm:ss") + "</p>");

        int totalFiles = result.Files.Count;
        int totalMethods = result.Files.Sum(f => f.Methods.Count);
        int totalComplexity = result.Files.Sum(f => f.CyclomaticComplexity);
        double avgMi = totalMethods > 0 ? result.Files.SelectMany(f => f.Methods).Average(m => m.MaintainabilityIndex) : 0;
        sb.AppendLine("<div class=\"summary\">");
        sb.AppendLine("<div class=\"card\"><span class=\"value\">" + totalFiles + "</span><br>Files</div>");
        sb.AppendLine("<div class=\"card\"><span class=\"value\">" + totalMethods + "</span><br>Methods</div>");
        sb.AppendLine("<div class=\"card\"><span class=\"value\">" + totalComplexity + "</span><br>Total Cyclomatic Complexity</div>");
        sb.AppendLine("<div class=\"card\"><span class=\"value " + MiClass(avgMi) + "\">" + avgMi.ToString("F1") + "</span><br>Avg Maintainability</div>");
        sb.AppendLine("<div class=\"card\"><span class=\"value\">" + result.Duplicates.Count + "</span><br>Duplicate Groups</div>");
        sb.AppendLine("</div>");

        sb.AppendLine("<h2>File &amp; Method Metrics</h2>");
        sb.AppendLine("<table>");
        sb.AppendLine("<thead><tr><th>File</th><th>Lines</th><th>Code</th><th>File CC</th><th>File MI</th><th>Method</th><th>Method CC</th><th>LOC</th><th>Method MI</th></tr></thead>");
        sb.AppendLine("<tbody>");
        foreach (var file in result.Files.OrderBy(f => f.RelativePath))
        {
            if (file.Methods.Count == 0)
            {
                sb.AppendLine("<tr><td>" + EscapeHtml(file.RelativePath) + "</td><td>" + file.TotalLines + "</td><td>" + file.CodeLines + "</td><td>" + file.CyclomaticComplexity + "</td><td class=\"" + MiClass(file.MaintainabilityIndex) + "\">" + file.MaintainabilityIndex.ToString("F1") + "</td><td colspan=\"4\">—</td></tr>");
                continue;
            }
            bool first = true;
            foreach (var m in file.Methods)
            {
                sb.AppendLine("<tr>");
                if (first) { sb.AppendLine("<td rowspan=\"" + file.Methods.Count + "\">" + EscapeHtml(file.RelativePath) + "</td><td rowspan=\"" + file.Methods.Count + "\">" + file.TotalLines + "</td><td rowspan=\"" + file.Methods.Count + "\">" + file.CodeLines + "</td><td rowspan=\"" + file.Methods.Count + "\">" + file.CyclomaticComplexity + "</td><td rowspan=\"" + file.Methods.Count + "\" class=\"" + MiClass(file.MaintainabilityIndex) + "\">" + file.MaintainabilityIndex.ToString("F1") + "</td>"); first = false; }
                sb.AppendLine("<td>" + EscapeHtml(m.Name) + "</td><td class=\"" + CcClass(m.CyclomaticComplexity) + "\">" + m.CyclomaticComplexity + "</td><td>" + m.LinesOfCode + "</td><td class=\"" + MiClass(m.MaintainabilityIndex) + "\">" + m.MaintainabilityIndex.ToString("F1") + "</td></tr>");
            }
        }
        sb.AppendLine("</tbody></table>");

        if (result.Duplicates.Count > 0)
        {
            sb.AppendLine("<h2>Code Duplication</h2>");
            sb.AppendLine("<table>");
            sb.AppendLine("<thead><tr><th>Hash</th><th>Occurrences</th><th>Locations</th></tr></thead>");
            sb.AppendLine("<tbody>");
            foreach (var dup in result.Duplicates)
            {
                var locations = string.Join("; ", dup.Occurrences.Select(o => EscapeHtml(o.FilePath) + " L" + o.LineStart + "-" + o.LineEnd));
                sb.AppendLine("<tr><td><code>" + EscapeHtml(dup.NormalizedHash) + "</code></td><td>" + dup.Occurrences.Count + "</td><td>" + locations + "</td></tr>");
            }
            sb.AppendLine("</tbody></table>");
        }

        sb.AppendLine("</body></html>");
        return sb.ToString();
    }

    private static string CcClass(int cc)
    {
        if (cc > 15) return "bad";
        if (cc > 10) return "warn";
        return "good";
    }

    private static string MiClass(double mi)
    {
        if (mi < 20) return "bad";
        if (mi < 65) return "warn";
        return "good";
    }

    private static string Escape(string s) => s.Contains(',') || s.Contains('"') ? "\"" + s.Replace("\"", "\"\"") + "\"" : s;
    private static string EscapeHtml(string s) => System.Net.WebUtility.HtmlEncode(s);
}
