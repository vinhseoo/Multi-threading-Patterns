@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   [T45 BUILD TOOL] PTIT NETWORK PROGRAMMING - JAVA 21
echo ========================================================

set "JDK_PATH=C:\Users\maiduc.vinh\.jdks\ms-21.0.10"
if exist "%JDK_PATH%\bin\javac.exe" (
    set "JAVAC=%JDK_PATH%\bin\javac.exe"
    set "JAVA=%JDK_PATH%\bin\java.exe"
) else (
    set "JAVAC=javac"
    set "JAVA=java"
)

echo [*] Compiler: "%JAVAC%"
"%JAVAC%" -version
if errorlevel 1 (
    echo [ERROR] JDK 21 not found at %JDK_PATH%!
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
