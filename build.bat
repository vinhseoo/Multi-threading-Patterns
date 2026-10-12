@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ========================================================
echo   [T45 BUILD TOOL] PTIT NETWORK PROGRAMMING - JAVA 21
echo ========================================================

REM Dynamic Java 21 detection across multiple locations
set "FOUND_JDK="

REM 1. Check JAVA_HOME
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "FOUND_JDK=%JAVA_HOME%"
        goto :jdk_selected
    )
)

REM 2. Check JDK_HOME
if defined JDK_HOME (
    if exist "%JDK_HOME%\bin\javac.exe" (
        set "FOUND_JDK=%JDK_HOME%"
        goto :jdk_selected
    )
)

REM 3. Check %USERPROFILE%\.jdks - Prioritize 21
for /d %%D in ("%USERPROFILE%\.jdks\*21*") do (
    if exist "%%D\bin\javac.exe" (
        set "FOUND_JDK=%%D"
        goto :jdk_selected
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
    if exist "%%D\bin\javac.exe" (
        set "FOUND_JDK=%%D"
        goto :jdk_selected
    )
)

REM 5. Check other JDKs in %USERPROFILE%\.jdks
for /d %%D in ("%USERPROFILE%\.jdks\*") do (
    if exist "%%D\bin\javac.exe" (
        set "FOUND_JDK=%%D"
        goto :jdk_selected
    )
)

REM 6. Check other JDKs in Program Files
for /d %%D in ("%ProgramFiles%\Java\*") do (
    if exist "%%D\bin\javac.exe" (
        set "FOUND_JDK=%%D"
        goto :jdk_selected
    )
)

REM 7. Check system PATH
where javac >nul 2>&1
if %errorlevel% equ 0 (
    set "JAVAC=javac"
    set "JAVA=java"
    goto :jdk_ready
)

:jdk_selected
if defined FOUND_JDK (
    set "JAVAC=%FOUND_JDK%\bin\javac.exe"
    set "JAVA=%FOUND_JDK%\bin\java.exe"
    goto :jdk_ready
)

echo.
echo ===========================================================================
echo [ERROR] Khong tim thay JDK (Java Development Kit) 21 tren may tinh nay!
echo.
echo Du an yeu cau JDK 21 LTS de su dung Virtual Threads (Project Loom).
echo Vui long cai dat JDK 21 tu: https://adoptium.net/
echo hoac thiet lap bien moi truong JAVA_HOME tro toi thu muc JDK 21.
echo ===========================================================================
exit /b 1

:jdk_ready
echo [*] Compiler: "%JAVAC%"
"%JAVAC%" -version
if errorlevel 1 (
    echo [ERROR] Trinh bien dich "%JAVAC%" khong the thuc thi!
    exit /b 1
)

if not exist "target\classes" (
    mkdir "target\classes"
)

echo [*] Compiling Java source files with Java 21 LTS + Lombok...
powershell -NoProfile -ExecutionPolicy Bypass -Command "& { $files = (Get-ChildItem -Path 'src/main/java' -Recurse -Filter *.java).FullName; if ($files.Count -eq 0) { Write-Error 'No Java files found'; exit 1 }; & '%JAVAC%' -encoding UTF-8 -cp 'lib/*' -processorpath 'lib/lombok.jar' -d 'target/classes' $files }"
if errorlevel 1 (
    echo [FAILED] Compilation failed!
    exit /b 1
)

if exist "src\main\resources" (
    echo [*] Copying resources to target\classes...
    xcopy /E /I /Y "src\main\resources" "target\classes" > nul 2>&1
)

echo.
echo [SUCCESS] Build completed successfully!
echo Start server with: run_server.bat [mode]
echo Example: run_server.bat 1
echo ========================================================
