using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.CSharp.Syntax;

namespace CodeAnalyzer;

/// <summary>
/// Calculates cyclomatic complexity and maintainability index using Roslyn syntax tree.
/// </summary>
public static class RoslynMetricsCalculator
{
    /// <summary>
    /// Cyclomatic complexity = 1 + number of decision points (if, for, while, catch, conditional, etc.).
    /// </summary>
    public static int GetCyclomaticComplexity(SyntaxNode methodBody)
    {
        if (methodBody == null) return 1;

        int complexity = 1;
        foreach (var node in methodBody.DescendantNodes())
        {
            complexity += node switch
            {
                IfStatementSyntax => 1,
                ElseClauseSyntax => 1,
                ForStatementSyntax => 1,
                ForEachStatementSyntax => 1,
                WhileStatementSyntax => 1,
                DoStatementSyntax => 1,
                SwitchStatementSyntax => 1,
                SwitchExpressionSyntax => 1,
                CatchClauseSyntax => 1,
                ConditionalExpressionSyntax => 1,  // ? :
                BinaryExpressionSyntax b when IsLogicalBranch(b) => 1,
                CaseSwitchLabelSyntax => 1,
                _ => 0
            };
        }
        return complexity;
    }

    private static bool IsLogicalBranch(BinaryExpressionSyntax b)
    {
        return b.Kind() is SyntaxKind.LogicalAndExpression or SyntaxKind.LogicalOrExpression;
    }

    /// <summary>
    /// Maintainability Index: 0-100 scale. Formula based on Halstead Volume, cyclomatic complexity, and lines of code.
    /// Simplified: MI = 100 - (complexity * 2 + log2(loc) * 5) capped and scaled.
    /// </summary>
    public static double GetMaintainabilityIndex(int cyclomaticComplexity, int linesOfCode)
    {
        if (linesOfCode <= 0) return 100;
        double mi = 100 - (cyclomaticComplexity * 2.0 + Math.Log2(Math.Max(1, linesOfCode)) * 5.0);
        return Math.Max(0, Math.Min(100, mi));
    }

    public static (int total, int code, int comment) CountLines(string source)
    {
        var lines = source.Split('\n');
        int total = lines.Length;
        int code = 0;
        int comment = 0;
        bool inBlockComment = false;

        foreach (var line in lines)
        {
            var trimmed = line.Trim();
            if (inBlockComment)
            {
                comment++;
                if (trimmed.Contains("*/"))
                    inBlockComment = false;
                continue;
            }
            if (trimmed.StartsWith("/*"))
            {
                comment++;
                inBlockComment = !trimmed.Contains("*/");
                continue;
            }
            if (trimmed.StartsWith("//") || string.IsNullOrWhiteSpace(trimmed))
            {
                if (trimmed.StartsWith("//")) comment++;
                continue;
            }
            code++;
        }
        return (total, code, comment);
    }
}
