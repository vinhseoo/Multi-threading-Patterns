#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

echo "========================================================"
echo "  [T45 BUILD TOOL] PTIT NETWORK PROGRAMMING - JAVA 21"
echo "========================================================"

# Auto-detect javac 21
JAVAC_CMD=""

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC_CMD="$JAVA_HOME/bin/javac"
elif command -v javac >/dev/null 2>&1; then
    JAVAC_CMD="javac"
else
    # Check common Linux / macOS paths
    for candidate in \
        /usr/lib/jvm/java-21* \
        /usr/lib/jvm/jdk-21* \
        /Library/Java/JavaVirtualMachines/*21*.jdk/Contents/Home \
        "$HOME/.jdks/"*21* \
        "$HOME/.sdkman/candidates/java/21*"; do
        if [ -x "$candidate/bin/javac" ]; then
            JAVAC_CMD="$candidate/bin/javac"
            break
        fi
    done
fi

if [ -z "$JAVAC_CMD" ]; then
    echo "[ERROR] Không tìm thấy JDK 21 trên hệ thống (Linux/macOS)!"
    echo "Vui lòng cài đặt JDK 21 (ví dụ: apt install openjdk-21-jdk hoặc brew install openjdk@21)"
    echo "hoặc thiết lập biến môi trường JAVA_HOME."
    exit 1
fi

echo "[*] Compiler: $JAVAC_CMD"
"$JAVAC_CMD" -version

mkdir -p target/classes

echo "[*] Compiling Java source files with Java 21 LTS + Lombok..."
JAVA_FILES=$(find src/main/java -name "*.java")
"$JAVAC_CMD" -encoding UTF-8 -cp "lib/*" -processorpath "lib/lombok.jar" -d target/classes $JAVA_FILES

if [ -d "src/main/resources" ]; then
    echo "[*] Copying resources to target/classes..."
    cp -r src/main/resources/* target/classes/ 2>/dev/null || true
fi

echo ""
echo "[SUCCESS] Build completed successfully!"
echo "Start server with: ./run_server.sh [mode] [port]"
echo "========================================================"
