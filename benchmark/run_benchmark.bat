@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0.."

echo ===========================================================================
echo   [T45 BENCHMARK RUNNER] CONG CU TU DONG BAN TAI THUC NGHIEM DA LUONG
echo ===========================================================================

REM Dynamic Java 21 detection across multiple locations
set "FOUND_JAVA="

REM 1. Check JAVA_HOME
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "FOUND_JAVA=%JAVA_HOME%\bin\java.exe"
        goto :java_selected
    )
)

REM 2. Check JDK_HOME
if defined JDK_HOME (
    if exist "%JDK_HOME%\bin\java.exe" (
        set "FOUND_JAVA=%JDK_HOME%\bin\java.exe"
        goto :java_selected
    )
)

REM 3. Check %USERPROFILE%\.jdks - Prioritize 21
for /d %%D in ("%USERPROFILE%\.jdks\*21*") do (
    if exist "%%D\bin\java.exe" (
        set "FOUND_JAVA=%%D\bin\java.exe"
        goto :java_selected
    )
)

REM 4. Check Program Files - Prioritize 21
for /d %%D in (
    "%ProgramFiles%\Java\*21*"
    "%ProgramFiles%\Eclipse Adoptium\*21*"
    "%ProgramFiles%\Microsoft\*21*"
    "%ProgramFiles%\Amazon Corretto\*21*"
    "%ProgramFiles%\BellSoft\*21*"
    "%ProgramFiles%\Zulu\*21*"
    "%LOCALAPPDATA%\Programs\Eclipse Adoptium\*21*"
) do (
    if exist "%%D\bin\java.exe" (
        set "FOUND_JAVA=%%D\bin\java.exe"
        goto :java_selected
    )
)

REM 5. Check other JDKs in %USERPROFILE%\.jdks
for /d %%D in ("%USERPROFILE%\.jdks\*") do (
    if exist "%%D\bin\java.exe" (
        set "FOUND_JAVA=%%D\bin\java.exe"
        goto :java_selected
    )
)

REM 6. Check other JDKs in Program Files
for /d %%D in ("%ProgramFiles%\Java\*") do (
    if exist "%%D\bin\java.exe" (
        set "FOUND_JAVA=%%D\bin\java.exe"
        goto :java_selected
    )
)

REM 7. Check system PATH
where java >nul 2>&1
if %errorlevel% equ 0 (
    set "JAVA=java"
    goto :java_ready
)

:java_selected
if defined FOUND_JAVA (
    set "JAVA=%FOUND_JAVA%"
    goto :java_ready
)

echo.
echo ===========================================================================
echo [ERROR] Khong tim thay Java Runtime (Java 21) tren may tinh nay!
echo.
echo Vui long cai dat JDK 21 tu: https://adoptium.net/
echo hoac thiet lap bien moi truong JAVA_HOME tro toi thu muc JDK 21.
echo ===========================================================================
exit /b 1

:java_ready
set "JAVA_FLAGS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

REM Check and build if needed
if not exist "target\classes\vn\ptit\network\benchmark\JavaLoadTester.class" (
    echo [*] Chua tim thay JavaLoadTester. Dang tien hanh bien dich du an...
    call "build.bat"
    if errorlevel 1 exit /b 1
)

chcp 65001 > nul

REM CLI parameters pass-through
if not "%~1"=="" (
    "%JAVA%" %JAVA_FLAGS% -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester %*
    goto :end
)

REM Interactive scenario menu
echo.
echo Hay chon mot kich ban ban tai de do dac va doi sanh:
echo   [1] Kich ban 1: Tai Nhe (Low Load)     - 50 clients,   500 reqs   (/api/delay?ms=100)
echo   [2] Kich ban 2: Tai Vua (Medium Load)  - 200 clients,  2,000 reqs (/api/delay?ms=100)
echo   [3] Kich ban 3: Tai Cao (C1000 Stress) - 1,000 clients, 5,000 reqs (/api/delay?ms=100)
echo   [4] Kich ban 4: Dot Bien (High Spike)  - 2,500 clients, 10,000 reqs (/api/hello)
echo   [5] Kich ban 5: CPU-Bound Stress Test  - 64 clients,   500 reqs   (/api/compute?n=30)
echo.
set /p CHOICE="-> Nhap kich ban (1-5, mac dinh [1]): "
if "%CHOICE%"=="" set "CHOICE=1"

if "%CHOICE%"=="1" (
    "%JAVA%" %JAVA_FLAGS% -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 50 -n 500 --url http://localhost:8080/api/delay?ms=100 -m "Scenario 1: Low (50 clients)"
) else if "%CHOICE%"=="2" (
    "%JAVA%" %JAVA_FLAGS% -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 200 -n 2000 --url http://localhost:8080/api/delay?ms=100 -m "Scenario 2: Med (200 clients)"
) else if "%CHOICE%"=="3" (
    "%JAVA%" %JAVA_FLAGS% -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 1000 -n 5000 --url http://localhost:8080/api/delay?ms=100 -m "Scenario 3: C1000 (1000 clients)"
) else if "%CHOICE%"=="4" (
    "%JAVA%" %JAVA_FLAGS% -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 2500 -n 10000 --url http://localhost:8080/api/hello -m "Scenario 4: High Spike (2500 clients)"
) else if "%CHOICE%"=="5" (
    "%JAVA%" %JAVA_FLAGS% -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 64 -n 500 --url http://localhost:8080/api/compute?n=30 -m "Scenario 5: CPU-bound (64 clients)"
) else (
    echo [!] Lua chon khong hop le. Chay mac dinh Kich ban 1.
    "%JAVA%" %JAVA_FLAGS% -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 50 -n 500 --url http://localhost:8080/api/delay?ms=100 -m "Scenario 1: Low (50 clients)"
)

:end
echo.
echo ===========================================================================
echo [OK] Hoan tat do dac! Du lieu da duoc ghi vao benchmark/benchmark_results.csv
echo ===========================================================================
