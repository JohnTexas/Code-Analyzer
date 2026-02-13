package com.codeanalyzer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.statement.*;

import java.util.Optional;

/**
 * Calculates cyclomatic complexity and maintainability index from JavaParser AST.
 */
public final class JavaMetricsCalculator {

    /**
     * Cyclomatic complexity = 1 + number of decision points.
     */
    public static int getCyclomaticComplexity(Node methodBody) {
        if (methodBody == null) return 1;
        final int[] count = { 1 };
        methodBody.walk(Node.TreeTraversal.PREORDER, node -> {
            if (node instanceof IfStmt) count[0]++;
            else if (node instanceof ForStmt) count[0]++;
            else if (node instanceof ForEachStmt) count[0]++;
            else if (node instanceof WhileStmt) count[0]++;
            else if (node instanceof DoStmt) count[0]++;
            else if (node instanceof SwitchStmt) count[0]++;
            else if (node.getClass().getSimpleName().equals("SwitchExpr")) count[0]++; // Java 12+ switch expression
            else if (node instanceof CatchClause) count[0]++;
            else if (node instanceof ConditionalExpr) count[0]++;
            else if (node instanceof BinaryExpr bin) {
                BinaryExpr.Operator op = bin.getOperator();
                if (op == BinaryExpr.Operator.AND || op == BinaryExpr.Operator.OR)
                    count[0]++;
            }
        });
        return count[0];
    }

    /**
     * Maintainability Index 0-100: higher is better.
     */
    public static double getMaintainabilityIndex(int cyclomaticComplexity, int linesOfCode) {
        if (linesOfCode <= 0) return 100;
        double log2 = Math.log(Math.max(1, linesOfCode)) / Math.log(2);
        double mi = 100 - (cyclomaticComplexity * 2.0 + log2 * 5.0);
        return Math.max(0, Math.min(100, mi));
    }

    public static int getLinesOfCode(Optional<? extends Node> body, String source) {
        if (body.isEmpty()) return 0;
        Node n = body.get();
        int start = n.getRange().map(r -> r.begin.line).orElse(0);
        int end = n.getRange().map(r -> r.end.line).orElse(0);
        return Math.max(1, end - start + 1);
    }

    public static LineCounts countLines(String source) {
        String[] lines = source.split("\n");
        int total = lines.length;
        int code = 0;
        int comment = 0;
        boolean inBlock = false;
        for (String line : lines) {
            String t = line.trim();
            if (inBlock) {
                comment++;
                if (t.contains("*/")) inBlock = false;
                continue;
            }
            if (t.startsWith("/*")) {
                comment++;
                if (!t.contains("*/")) inBlock = true;
                continue;
            }
            if (t.startsWith("//") || t.isEmpty()) {
                if (t.startsWith("//")) comment++;
                continue;
            }
            code++;
        }
        return new LineCounts(total, code, comment);
    }

    public record LineCounts(int total, int code, int comment) {}
}
