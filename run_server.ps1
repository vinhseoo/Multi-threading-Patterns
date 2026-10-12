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

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $ScriptDir) { $ScriptDir = Get-Location }

# Tự động tìm kiếm runtime Java 21 trên hệ thống (không fix cứng đường dẫn)
function Find-JavaExecutable {
    # 1. Kiểm tra JAVA_HOME
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        return "$env:JAVA_HOME\bin\java.exe"
    }
    # 2. Kiểm tra JDK_HOME
    if ($env:JDK_HOME -and (Test-Path "$env:JDK_HOME\bin\java.exe")) {
        return "$env:JDK_HOME\bin\java.exe"
    }
    # 3. Quét các vị trí chuẩn ưu tiên phiên bản 21
    $searchPatterns = @(
        "$env:USERPROFILE\.jdks\*21*",
        "$env:ProgramFiles\Java\*21*",
        "$env:ProgramFiles\Eclipse Adoptium\*21*",
        "$env:ProgramFiles\Microsoft\*21*",
        "$env:ProgramFiles\Amazon Corretto\*21*",
        "$env:ProgramFiles\BellSoft\*21*",
        "$env:ProgramFiles\Zulu\*21*",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\*21*",
        "$env:USERPROFILE\.jdks\*",
        "$env:ProgramFiles\Java\*"
    )
    foreach ($pattern in $searchPatterns) {
        $found = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | Sort-Object Name -Descending
        foreach ($dir in $found) {
            $candidate = Join-Path $dir.FullName "bin\java.exe"
            if (Test-Path $candidate) {
                return $candidate
            }
        }
    }
    # 4. Kiểm tra java trong biến môi trường PATH
    $javaInPath = Get-Command java -ErrorAction SilentlyContinue
    if ($javaInPath) {
        return "java"
    }
    return $null
}

$Java = Find-JavaExecutable

if (-not $Java) {
    Write-Host "[ERROR] Không tìm thấy Java Runtime (Java 21) trên thiết bị này!" -ForegroundColor Red
    Write-Host "Vui lòng cài đặt JDK 21 hoặc thiết lập biến môi trường JAVA_HOME." -ForegroundColor Yellow
    Write-Host "Tải JDK 21 tại: https://adoptium.net/" -ForegroundColor Gray
    exit 1
}

$targetClass = Join-Path $ScriptDir "target\classes\vn\ptit\network\Main.class"
if (-not (Test-Path $targetClass)) {
    Write-Host "[*] Chưa tìm thấy mã biên dịch. Đang tiến hành biên dịch dự án..." -ForegroundColor Yellow
    cmd.exe /c "call `"$ScriptDir\build.bat`""
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Biên dịch thất bại!" -ForegroundColor Red
        exit 1
    }
}

$classpath = "$ScriptDir/target/classes;$ScriptDir/lib/*"
Write-Host "[*] Khởi động T45 Network Server với Java 21 LTS (Mode: $Mode, Port: $Port)..." -ForegroundColor Cyan
Write-Host "[*] Java Runtime: $Java" -ForegroundColor DarkGray
& $Java "-Dfile.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" "-Dsun.stderr.encoding=UTF-8" "-Dstdout.encoding=UTF-8" "-Dstderr.encoding=UTF-8" -cp $classpath vn.ptit.network.Main $Mode $Port
