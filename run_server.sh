#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

MODE=${1:-4}
PORT=${2:-8080}

# Auto-detect java 21
JAVA_CMD=""

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_CMD="java"
else
    for candidate in \
        /usr/lib/jvm/java-21* \
        /usr/lib/jvm/jdk-21* \
        /Library/Java/JavaVirtualMachines/*21*.jdk/Contents/Home \
        "$HOME/.jdks/"*21* \
        "$HOME/.sdkman/candidates/java/21*"; do
        if [ -x "$candidate/bin/java" ]; then
            JAVA_CMD="$candidate/bin/java"
            break
        fi
    done
fi

if [ -z "$JAVA_CMD" ]; then
    echo "[ERROR] Không tìm thấy Java Runtime 21 trên hệ thống (Linux/macOS)!"
    echo "Vui lòng cài đặt JDK 21 hoặc thiết lập biến môi trường JAVA_HOME."
    exit 1
fi

if [ ! -f "target/classes/vn/ptit/network/Main.class" ]; then
    echo "[WARN] Chưa tìm thấy mã biên dịch. Đang tiến hành biên dịch dự án..."
    ./build.sh
fi

echo "[*] Khởi động T45 Network Server với Java 21 LTS (Mode: $MODE, Port: $PORT)..."
echo "[*] Java Runtime: $JAVA_CMD"

exec "$JAVA_CMD" -Dfile.encoding=UTF-8 -cp "target/classes:lib/*" vn.ptit.network.Main "$MODE" "$PORT"
