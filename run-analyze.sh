#!/usr/bin/env bash
# Run the appropriate code analyzer (.NET or Java) on the given project path.
# Usage: ./run-analyze.sh [PATH] [--format html|csv] [--output FILE]

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_PATH="."
[[ $# -gt 0 ]] && { PROJECT_PATH="$1"; shift; }

# Collect optional args
FORMAT=""
OUTPUT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --format|-f) FORMAT="$2"; shift 2 ;;
    --output|-o) OUTPUT="$2"; shift 2 ;;
    *) shift ;;
  esac
done

EXTRA_ARGS=()
[[ -n "$FORMAT" ]] && EXTRA_ARGS+=(--format "$FORMAT")
[[ -n "$OUTPUT" ]] && EXTRA_ARGS+=(--output "$OUTPUT")

# Detect project type: .NET (csproj/sln) vs Java (pom.xml or .java)
is_dotnet() {
  find "$1" -maxdepth 4 \( -name "*.csproj" -o -name "*.sln" \) 2>/dev/null | head -1 | grep -q .
}
is_java() {
  find "$1" -maxdepth 3 \( -name "pom.xml" -o -name "build.gradle" \) 2>/dev/null | head -1 | grep -q . || \
  find "$1" -maxdepth 4 -name "*.java" 2>/dev/null | head -1 | grep -q .
}

if is_dotnet "$PROJECT_PATH"; then
  echo "Detected .NET project. Running .NET analyzer..."
  dotnet run --project "$SCRIPT_DIR/dotnet/CodeAnalyzer" -- "$PROJECT_PATH" "${EXTRA_ARGS[@]}"
elif is_java "$PROJECT_PATH"; then
  echo "Detected Java project. Running Java analyzer..."
  JAR="$SCRIPT_DIR/java/target/code-analyzer-jar-with-dependencies.jar"
  if [[ ! -f "$JAR" ]]; then
    echo "Building Java analyzer (one-time)..."
    (cd "$SCRIPT_DIR/java" && mvn -q package -DskipTests)
  fi
  java -jar "$JAR" "$PROJECT_PATH" "${EXTRA_ARGS[@]}"
else
  echo "Unknown project type. Specify a path containing .csproj/.sln (.NET) or pom.xml/build.gradle/.java (Java)."
  exit 1
fi
