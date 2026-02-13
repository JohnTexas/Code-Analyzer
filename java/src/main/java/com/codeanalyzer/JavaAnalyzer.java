package com.codeanalyzer;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class JavaAnalyzer {

    static {
        StaticJavaParser.setConfiguration(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
    }

    public static List<FileMetrics> analyzeDirectory(Path rootPath) throws Exception {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("target") && !p.toString().contains("build"))
                    .forEach(files::add);
        }
        List<FileMetrics> results = new ArrayList<>();
        for (Path file : files) {
            try {
                String content = Files.readString(file);
                FileMetrics m = analyzeSource(content, file.toString(), rootPath.toString());
                if (m != null) results.add(m);
            } catch (Exception e) {
                System.err.println("Warning: Could not analyze " + file + ": " + e.getMessage());
            }
        }
        return results;
    }

    public static FileMetrics analyzeSource(String source, String filePath, String rootPath) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            String relativePath = filePath.startsWith(rootPath)
                    ? filePath.substring(rootPath.length() + (rootPath.endsWith("/") ? 0 : 1))
                    : filePath;

            JavaMetricsCalculator.LineCounts lc = JavaMetricsCalculator.countLines(source);
            FileMetrics fileMetrics = new FileMetrics(filePath, relativePath);
            fileMetrics.setTotalLines(lc.total());
            fileMetrics.setCodeLines(lc.code());
            fileMetrics.setCommentLines(lc.comment());

            int fileComplexity = 0;
            double fileMiSum = 0;
            int methodCount = 0;

            for (TypeDeclaration<?> type : cu.getTypes()) {
                for (MethodDeclaration method : type.getMethods()) {
                    Optional<Node> body = method.getBody().map(b -> (Node) b);
                    int cc = JavaMetricsCalculator.getCyclomaticComplexity(body.orElse(null));
                    int loc = JavaMetricsCalculator.getLinesOfCode(body, source);
                    double mi = JavaMetricsCalculator.getMaintainabilityIndex(cc, loc);
                    int lineStart = method.getRange().map(r -> r.begin.line).orElse(0);
                    int lineEnd = method.getRange().map(r -> r.end.line).orElse(0);

                    String sig = method.getDeclarationAsString(false, false, false);
                    if (sig.length() > 80) sig = sig.substring(0, 77) + "...";
                    MethodMetrics mm = new MethodMetrics(method.getNameAsString(), sig);
                    mm.setLineStart(lineStart);
                    mm.setLineEnd(lineEnd);
                    mm.setCyclomaticComplexity(cc);
                    mm.setLinesOfCode(loc);
                    mm.setMaintainabilityIndex(mi);
                    fileMetrics.getMethods().add(mm);
                    fileComplexity += cc;
                    fileMiSum += mi;
                    methodCount++;
                }
                for (ConstructorDeclaration cons : type.getConstructors()) {
                    Optional<Node> body = cons.getBody().map(b -> (Node) b);
                    int cc = JavaMetricsCalculator.getCyclomaticComplexity(body.orElse(null));
                    int loc = JavaMetricsCalculator.getLinesOfCode(body, source);
                    double mi = JavaMetricsCalculator.getMaintainabilityIndex(cc, loc);
                    int lineStart = cons.getRange().map(r -> r.begin.line).orElse(0);
                    int lineEnd = cons.getRange().map(r -> r.end.line).orElse(0);
                    String sig = cons.getDeclarationAsString(false, false, false);
                    if (sig.length() > 80) sig = sig.substring(0, 77) + "...";
                    MethodMetrics mm = new MethodMetrics(cons.getNameAsString(), sig);
                    mm.setLineStart(lineStart);
                    mm.setLineEnd(lineEnd);
                    mm.setCyclomaticComplexity(cc);
                    mm.setLinesOfCode(loc);
                    mm.setMaintainabilityIndex(mi);
                    fileMetrics.getMethods().add(mm);
                    fileComplexity += cc;
                    fileMiSum += mi;
                    methodCount++;
                }
            }

            fileMetrics.setCyclomaticComplexity(fileComplexity);
            fileMetrics.setMaintainabilityIndex(methodCount > 0 ? fileMiSum / methodCount : 100);
            return fileMetrics;
        } catch (Exception e) {
            return null;
        }
    }
}
