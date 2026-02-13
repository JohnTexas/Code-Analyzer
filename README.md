# Code Analyzer

A **DevOps-friendly code quality tool** that scans Java and .NET source directories to compute **cyclomatic complexity**, **code duplication**, and **maintainability scores**, then outputs **HTML** or **CSV** reports. All analysis runs **locally** using [JavaParser](https://javaparser.org/) for Java and [Roslyn](https://github.com/dotnet/roslyn) for .NET.

Use it after training or refactors to assess and track code quality in ongoing projects.

---

## Metrics

| Metric | Description |
|--------|-------------|
| **Cyclomatic complexity** | Number of linearly independent paths through code (if/else, loops, switch, `&&`/`\|\|`, etc.). Lower is better; high values suggest hard-to-test, brittle code. |
| **Code duplication** | Similar or identical method signatures and blocks across files. Reported as duplicate groups with file and line ranges. |
| **Maintainability index** | 0–100 score derived from complexity and size. Higher is better; &lt; 20 = low, 20–65 = moderate, &gt; 65 = good. |

---

## Quick start

### .NET projects (C#)

**Requirements:** .NET 8 SDK.

```bash
# Build (from repo root)
dotnet build dotnet/CodeAnalyzer.sln

# Run (analyze current directory)
dotnet run --project dotnet/CodeAnalyzer -- .

# Output directory and format
dotnet run --project dotnet/CodeAnalyzer -- /path/to/YourProject --format html --output ./report.html
dotnet run --project dotnet/CodeAnalyzer -- /path/to/YourProject -f csv -o report.csv
```

Reports are written to the project directory by default: `code-metrics-report.html` or `code-metrics-report.csv`.

### Java projects

**Requirements:** Java 17+, Maven 3.6+.

```bash
# Build (fat JAR with dependencies)
cd java && mvn package -DskipTests

# Run
java -jar target/code-analyzer-jar-with-dependencies.jar /path/to/YourProject

# Options
java -jar target/code-analyzer-jar-with-dependencies.jar /path/to/YourProject --format csv --output report.csv
```

Default report: `code-metrics-report.html` in the project directory.

### Unified runner (optional)

From the repo root you can use the script to **auto-detect** project type and run the right analyzer:

```bash
./run-analyze.sh /path/to/your/project
./run-analyze.sh /path/to/your/project --format csv -o ./metrics.csv
```

Requires either:

- **.NET:** `dotnet` on `PATH` and the solution built once.
- **Java:** `mvn` on `PATH` and the JAR built once.

The script picks **.NET** if it finds `*.csproj` (or `*.sln`) under the path, otherwise **Java** if it finds `pom.xml` or `*.java`.

---

## Project layout

```
code_analyzer/
├── README.md
├── run-analyze.sh          # Optional: auto-detect and run .NET or Java analyzer
├── dotnet/
│   ├── CodeAnalyzer.sln
│   └── CodeAnalyzer/
│       ├── CodeAnalyzer.csproj
│       ├── Program.cs
│       ├── CSharpAnalyzer.cs
│       ├── RoslynMetricsCalculator.cs
│       ├── DuplicationDetector.cs
│       ├── ReportGenerator.cs
│       └── Models.cs
└── java/
    ├── pom.xml
    └── src/main/java/com/codeanalyzer/
        ├── Main.java
        ├── JavaAnalyzer.java
        ├── JavaMetricsCalculator.java
        ├── DuplicationDetector.java
        ├── ReportGenerator.java
        └── (model classes)
```

---

## Output formats

- **HTML:** Summary cards (file/method counts, total complexity, average maintainability, duplicate groups) and tables (per-file and per-method metrics, duplicate locations). Styled for readability (dark theme).
- **CSV:** Same data in columns for import into spreadsheets or CI (e.g. `Report Type`, `FilePath`, `RelativePath`, `TotalLines`, `CodeLines`, `CommentLines`, `CyclomaticComplexity`, `MaintainabilityIndex`, method-level columns, and duplicate rows).

---

## CI / pipelines

- **.NET:** Add a step that runs `dotnet run --project dotnet/CodeAnalyzer -- $(pwd) -f csv -o report.csv` (or your repo path), then archive or publish `report.csv` / `report.html`.
- **Java:** Run the JAR with the repo path and `--format csv -o report.csv`; archive the report artifact.

You can then fail or warn the build when e.g. average maintainability drops below a threshold or total cyclomatic complexity exceeds a limit by parsing the CSV.

---

## License

Use and modify as needed for your organization.
