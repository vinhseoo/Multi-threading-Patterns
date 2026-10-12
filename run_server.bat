@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

REM ===========================================================================
REM T45 Server Launcher - Java 21 Auto-Detection
REM ===========================================================================
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

REM 3. Check %USERPROFILE%\.jdks (IntelliJ IDEA / VS Code) - Prioritize 21
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

echo ===========================================================================
echo [ERROR] Khong tim thay Java Runtime (Java 21) tren may tinh nay!
echo.
echo Du an yeu cau Java 21 LTS de su dung Virtual Threads (Project Loom).
echo Vui long cai dat JDK 21 tu: https://adoptium.net/
echo hoac thiet lap bien moi truong JAVA_HOME tro toi thu muc JDK 21.
echo ===========================================================================
exit /b 1

:java_ready
if not exist "%~dp0target\classes\vn\ptit\network\Main.class" (
    echo [*] Chua tim thay target classes. Dang tu dong bien dich du an...
    call "%~dp0build.bat"
    if errorlevel 1 exit /b 1
)

chcp 65001 > nul
echo [*] Starting T45 Network Server with Java 21 LTS (UTF-8)...
echo [*] Java Runtime: "%JAVA%"
"%JAVA%" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp "%~dp0target\classes;%~dp0lib/*" vn.ptit.network.Main %*
