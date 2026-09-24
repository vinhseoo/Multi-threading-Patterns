@echo off
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

echo [*] Starting T45 Network Server with Java 21...
"%JAVA%" -Dfile.encoding=UTF-8 -cp "target\classes" vn.ptit.network.Main %*
