param(
    [Parameter(Position=0)]
    [int]$Mode = 4,
    [Parameter(Position=1)]
    [int]$Port = 8080
)

# Thiết lập console và buffer của PowerShell sang UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 > $null

$JdkPath = "C:\Users\maiduc.vinh\.jdks\ms-21.0.10"
$Java = if (Test-Path "$JdkPath\bin\java.exe") { "$JdkPath\bin\java.exe" } else { "java" }

if (-not (Test-Path "target\classes\vn\ptit\network\Main.class")) {
    Write-Host "[*] Đang tiến hành biên dịch dự án..." -ForegroundColor Yellow
    cmd.exe /c ".\build.bat"
}

Write-Host "[*] Khởi động T45 Network Server với Java 21 LTS (Mode: $Mode, Port: $Port)..." -ForegroundColor Cyan
& $Java "-Dfile.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" "-Dsun.stderr.encoding=UTF-8" "-Dstdout.encoding=UTF-8" "-Dstderr.encoding=UTF-8" -cp "target/classes;lib/*" vn.ptit.network.Main $Mode $Port
