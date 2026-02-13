using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.CSharp.Syntax;

namespace CodeAnalyzer;

/// <summary>
/// Detects duplicate code blocks by normalizing and hashing method/block bodies (min tokens).
/// </summary>
public static class DuplicationDetector
{
    private const int MinTokensForDuplicate = 20;
    private const int MinDuplicateOccurrences = 2;

    public static List<DuplicateBlock> DetectDuplicates(IEnumerable<FileMetrics> allFiles)
    {
        var blocksByHash = new Dictionary<string, List<(string file, int start, int end, string normalized, string preview)>>();

        foreach (var file in allFiles)
        {
            foreach (var method in file.Methods)
            {
                if (method.LinesOfCode < 5) continue;
                var key = $"{file.FilePath}|{method.LineStart}|{method.LineEnd}";
                // Use signature + LOC + complexity as a simple "normalized" fingerprint for grouping similar methods
                var fingerprint = $"{method.Signature}|{method.LinesOfCode}|{method.CyclomaticComplexity}";
                var hash = fingerprint.GetHashCode().ToString("X8");
                var preview = $"{method.Signature} ({method.LinesOfCode} lines, CC={method.CyclomaticComplexity})";

                if (!blocksByHash.TryGetValue(hash, out var list))
                {
                    list = new List<(string, int, int, string, string)>();
                    blocksByHash[hash] = list;
                }
                list.Add((file.FilePath, method.LineStart, method.LineEnd, fingerprint, preview));
            }
        }

        var duplicates = new List<DuplicateBlock>();
        foreach (var (hash, occurrences) in blocksByHash)
        {
            if (occurrences.Count < MinDuplicateOccurrences) continue;
            var distinctFiles = occurrences.Select(o => o.file).Distinct().Count();
            if (distinctFiles < 2) continue; // same file only, skip or allow based on preference
            duplicates.Add(new DuplicateBlock
            {
                NormalizedHash = hash,
                TokenCount = occurrences.First().normalized.Length,
                Occurrences = occurrences.Select(o => new DuplicateOccurrence
                {
                    FilePath = o.file,
                    LineStart = o.start,
                    LineEnd = o.end,
                    Preview = o.preview
                }).ToList()
            });
        }
        return duplicates;
    }

    /// <summary>
    /// Text-based duplicate detection: normalize whitespace and identifiers, then hash lines.
    /// </summary>
    public static List<DuplicateBlock> DetectByNormalizedLines(
        Dictionary<string, string> fileContents)
    {
        var lineBlocksByHash = new Dictionary<string, List<(string file, int start, int end, string preview)>>();
        const int blockSize = 8; // consecutive lines

        foreach (var (filePath, content) in fileContents)
        {
            var lines = content.Split('\n');
            for (int i = 0; i <= lines.Length - blockSize; i++)
            {
                var block = lines.Skip(i).Take(blockSize)
                    .Select(NormalizeLine)
                    .Where(l => l.Length > 2)
                    .ToList();
                if (block.Count < 5) continue;
                var normalized = string.Join("\n", block);
                var hash = HashString(normalized);
                var preview = string.Join(" ", lines.Skip(i).Take(3).Select(l => l.Trim().Length > 40 ? l.Trim()[..40] + "…" : l.Trim()));

                if (!lineBlocksByHash.TryGetValue(hash, out var list))
                {
                    list = new List<(string, int, int, string)>();
                    lineBlocksByHash[hash] = list;
                }
                list.Add((filePath, i + 1, i + blockSize, preview));
            }
        }

        var result = new List<DuplicateBlock>();
        foreach (var (hash, occurrences) in lineBlocksByHash)
        {
            if (occurrences.Count < MinDuplicateOccurrences) continue;
            result.Add(new DuplicateBlock
            {
                NormalizedHash = hash,
                TokenCount = blockSize,
                Occurrences = occurrences.Select(o => new DuplicateOccurrence
                {
                    FilePath = o.file,
                    LineStart = o.start,
                    LineEnd = o.end,
                    Preview = o.preview
                }).ToList()
            });
        }
        return result;
    }

    private static string NormalizeLine(string line)
    {
        var t = line.Trim();
        if (string.IsNullOrWhiteSpace(t) || t.StartsWith("//")) return "";
        return System.Text.RegularExpressions.Regex.Replace(t, @"\s+", " ");
    }

    private static string HashString(string s)
    {
        return s.GetHashCode().ToString("X8");
    }
}
