@echo off
chcp 65001 > nul
setlocal

set "JDK_PATH=C:\Users\maiduc.vinh\.jdks\ms-21.0.10"
if exist "%JDK_PATH%\bin\java.exe" (
    set "JAVA=%JDK_PATH%\bin\java.exe"
) else (
    set "JAVA=java"
)

if not exist "target\classes\vn\ptit\network\Main.class" (
    echo [WARN] Target classes not found. Running build first...
    call build.bat
    if errorlevel 1 exit /b 1
)

echo [*] Starting T45 Network Server with Java 21 LTS (UTF-8)...
"%JAVA%" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp "target\classes;lib/*" vn.ptit.network.Main %*
