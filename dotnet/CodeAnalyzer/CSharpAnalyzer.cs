using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.CSharp.Syntax;

namespace CodeAnalyzer;

public static class CSharpAnalyzer
{
    public static List<FileMetrics> AnalyzeDirectory(string rootPath)
    {
        var files = Directory.GetFiles(rootPath, "*.cs", SearchOption.AllDirectories)
            .Where(p => !p.Contains("obj") && !p.Contains("bin"))
            .ToList();
        var results = new List<FileMetrics>();
        foreach (var file in files)
        {
            try
            {
                var content = File.ReadAllText(file);
                var metrics = AnalyzeSource(content, file, rootPath);
                if (metrics != null)
                    results.Add(metrics);
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Warning: Could not analyze {file}: {ex.Message}");
            }
        }
        return results;
    }

    public static FileMetrics? AnalyzeSource(string source, string filePath, string? rootPath = null)
    {
        var tree = CSharpSyntaxTree.ParseText(source);
        var root = tree.GetRoot();
        var diagnostics = tree.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error).ToList();
        if (diagnostics.Any())
            return null;

        var relativePath = rootPath != null && filePath.StartsWith(rootPath)
            ? filePath[(rootPath.Length + 1)..]
            : filePath;

        var (totalLines, codeLines, commentLines) = RoslynMetricsCalculator.CountLines(source);
        var fileMetrics = new FileMetrics
        {
            FilePath = filePath,
            RelativePath = relativePath,
            TotalLines = totalLines,
            CodeLines = codeLines,
            CommentLines = commentLines
        };

        var methods = root.DescendantNodes()
            .OfType<BaseMethodDeclarationSyntax>()
            .ToList();

        int fileComplexity = 0;
        double fileMiSum = 0;
        int methodCount = 0;

        foreach (var method in methods)
        {
            var body = method.Body ?? (method as MethodDeclarationSyntax)?.ExpressionBody?.Expression as SyntaxNode;
            if (body == null) continue;

            var complexity = RoslynMetricsCalculator.GetCyclomaticComplexity(body);
            var loc = body.Span.Length;
            var lineCount = source.Substring(0, Math.Min(body.Span.Start, source.Length)).Split('\n').Length;
            var lineEnd = source.Substring(0, Math.Min(body.Span.End, source.Length)).Split('\n').Length;
            var linesOfCode = lineEnd - lineCount + 1;
            var mi = RoslynMetricsCalculator.GetMaintainabilityIndex(complexity, linesOfCode);

            var name = method switch
            {
                MethodDeclarationSyntax m => m.Identifier.Text,
                ConstructorDeclarationSyntax c => c.Identifier.Text,
                DestructorDeclarationSyntax d => d.Identifier.Text,
                _ => "?"
            };
            var signature = $"{name}";

            fileMetrics.Methods.Add(new MethodMetrics
            {
                Name = name,
                Signature = signature,
                LineStart = lineCount,
                LineEnd = lineEnd,
                CyclomaticComplexity = complexity,
                LinesOfCode = linesOfCode,
                MaintainabilityIndex = mi
            });
            fileComplexity += complexity;
            fileMiSum += mi;
            methodCount++;
        }

        fileMetrics.CyclomaticComplexity = fileComplexity;
        fileMetrics.MaintainabilityIndex = methodCount > 0 ? fileMiSum / methodCount : 100;
        return fileMetrics;
    }
}
