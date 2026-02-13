namespace CodeAnalyzer;

public sealed class FileMetrics
{
    public required string FilePath { get; init; }
    public required string RelativePath { get; init; }
    public List<MethodMetrics> Methods { get; } = new();
    public int TotalLines { get; set; }
    public int CodeLines { get; set; }
    public int CommentLines { get; set; }
    public double MaintainabilityIndex { get; set; }
    public int CyclomaticComplexity { get; set; }
}

public sealed class MethodMetrics
{
    public required string Name { get; init; }
    public required string Signature { get; init; }
    public int LineStart { get; set; }
    public int LineEnd { get; set; }
    public int CyclomaticComplexity { get; set; }
    public int LinesOfCode { get; set; }
    public double MaintainabilityIndex { get; set; }
}

public sealed class DuplicateBlock
{
    public required string NormalizedHash { get; init; }
    public List<DuplicateOccurrence> Occurrences { get; } = new();
    public int TokenCount { get; set; }
}

public sealed class DuplicateOccurrence
{
    public required string FilePath { get; init; }
    public int LineStart { get; set; }
    public int LineEnd { get; set; }
    public string? Preview { get; init; }
}

public sealed class AnalysisResult
{
    public List<FileMetrics> Files { get; } = new();
    public List<DuplicateBlock> Duplicates { get; } = new();
    public string ProjectPath { get; set; } = "";
    public DateTime AnalyzedAt { get; set; }
}
