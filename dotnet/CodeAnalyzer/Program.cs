using System.Collections.Generic;

namespace CodeAnalyzer;

class Program
{
    static int Main(string[] args)
    {
        string path;
        string format = "html";
        string? outputPath = null;

        if (args.Length == 0)
        {
            path = Directory.GetCurrentDirectory();
        }
        else
        {
            path = args[0];
            for (int i = 1; i < args.Length; i++)
            {
                if (args[i] == "--format" || args[i] == "-f")
                {
                    if (i + 1 < args.Length) format = args[++i].ToLowerInvariant();
                }
                else if (args[i] == "--output" || args[i] == "-o")
                {
                    if (i + 1 < args.Length) outputPath = args[++i];
                }
            }
        }

        if (!Directory.Exists(path))
        {
            Console.Error.WriteLine($"Error: Directory not found: {path}");
            return 1;
        }

        Console.WriteLine($"Analyzing .NET source in: {path}");
        var files = CSharpAnalyzer.AnalyzeDirectory(path);
        if (files.Count == 0)
        {
            Console.WriteLine("No .cs files found or all failed to parse.");
            return 0;
        }

        var fileContents = new Dictionary<string, string>();
        foreach (var f in files)
        {
            try
            {
                fileContents[f.FilePath] = File.ReadAllText(f.FilePath);
            }
            catch { /* ignore */ }
        }

        var duplicates = DuplicationDetector.DetectDuplicates(files);
        var lineDuplicates = DuplicationDetector.DetectByNormalizedLines(fileContents);
        var allDuplicates = duplicates.Concat(lineDuplicates).DistinctBy(d => d.NormalizedHash).ToList();

        var result = new AnalysisResult
        {
            ProjectPath = Path.GetFileName(path.TrimEnd(Path.DirectorySeparatorChar)) ?? path,
            AnalyzedAt = DateTime.UtcNow,
            Files = files
        };
        foreach (var d in allDuplicates)
            result.Duplicates.Add(d);

        string report;
        string defaultName;
        if (format == "csv")
        {
            report = ReportGenerator.GenerateCsv(result);
            defaultName = "code-metrics-report.csv";
        }
        else
        {
            report = ReportGenerator.GenerateHtml(result);
            defaultName = "code-metrics-report.html";
        }

        var outPath = outputPath ?? Path.Combine(path, defaultName);
        File.WriteAllText(outPath, report);
        Console.WriteLine($"Report written to: {outPath}");
        Console.WriteLine($"Files analyzed: {files.Count}, Duplicate groups: {result.Duplicates.Count}");
        return 0;
    }
}
