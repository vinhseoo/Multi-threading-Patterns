@echo off
setlocal enabledelayedexpansion

echo ===========================================================================
echo   🚀 [T45 BENCHMARK RUNNER] CÔNG CỤ TỰ ĐỘNG BẮN TẢI THỰC NGHIỆM ĐA LUỒNG
echo ===========================================================================

set "JDK_PATH=C:\Users\maiduc.vinh\.jdks\ms-21.0.10"
if exist "%JDK_PATH%\bin\java.exe" (
    set "JAVA=%JDK_PATH%\bin\java.exe"
) else (
    set "JAVA=java"
)

:: Kiểm tra nếu chưa build thì build trước
if not exist "target\classes\vn\ptit\network\benchmark\JavaLoadTester.class" (
    echo [*] Chưa tìm thấy JavaLoadTester. Đang tiến hành biên dịch dự án...
    call build.bat
    if errorlevel 1 exit /b 1
)

:: Nếu người dùng truyền trực tiếp tham số (ví dụ: run_benchmark.bat -c 200 -n 1000)
if not "%~1"=="" (
    "%JAVA%" -Dfile.encoding=UTF-8 -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester %*
    goto :end
)

:: Menu các kịch bản thực nghiệm tiêu chuẩn
echo.
echo Hãy chọn một kịch bản bắn tải để đo đạc và đối sánh:
echo   [1] Kịch bản 1: Tải Nhẹ (Low Load)     - 50 clients,   500 reqs   (/api/delay?ms=100)
echo   [2] Kịch bản 2: Tải Vừa (Medium Load)  - 200 clients,  2,000 reqs (/api/delay?ms=100)
echo   [3] Kịch bản 3: Tải Cao (C1000 Stress) - 1,000 clients, 5,000 reqs (/api/delay?ms=100)
echo   [4] Kịch bản 4: Đột Biến (High Spike)  - 2,500 clients, 10,000 reqs (/api/hello)
echo   [5] Kịch bản 5: CPU-Bound Stress Test  - 64 clients,   500 reqs   (/api/compute?n=30)
echo.
set /p CHOICE="👉 Nhập kịch bản (1-5, mặc định [1]): "
if "%CHOICE%"=="" set "CHOICE=1"

if "%CHOICE%"=="1" (
    "%JAVA%" -Dfile.encoding=UTF-8 -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 50 -n 500 --url http://localhost:8080/api/delay?ms=100 -m "Scenario 1: Low (50 clients)"
) else if "%CHOICE%"=="2" (
    "%JAVA%" -Dfile.encoding=UTF-8 -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 200 -n 2000 --url http://localhost:8080/api/delay?ms=100 -m "Scenario 2: Med (200 clients)"
) else if "%CHOICE%"=="3" (
    "%JAVA%" -Dfile.encoding=UTF-8 -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 1000 -n 5000 --url http://localhost:8080/api/delay?ms=100 -m "Scenario 3: C1000 (1000 clients)"
) else if "%CHOICE%"=="4" (
    "%JAVA%" -Dfile.encoding=UTF-8 -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 2500 -n 10000 --url http://localhost:8080/api/hello -m "Scenario 4: High Spike (2500 clients)"
) else if "%CHOICE%"=="5" (
    "%JAVA%" -Dfile.encoding=UTF-8 -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 64 -n 500 --url http://localhost:8080/api/compute?n=30 -m "Scenario 5: CPU-bound (64 clients)"
) else (
    echo [!] Lựa chọn không hợp lệ. Chạy mặc định Kịch bản 1.
    "%JAVA%" -Dfile.encoding=UTF-8 -cp "target\classes;lib/*" vn.ptit.network.benchmark.JavaLoadTester -c 50 -n 500 --url http://localhost:8080/api/delay?ms=100 -m "Scenario 1: Low (50 clients)"
)

:end
echo.
echo ===========================================================================
echo [✓] Hoàn tất đo đạc! Dữ liệu đã được ghi vào benchmark/benchmark_results.csv
echo ===========================================================================
