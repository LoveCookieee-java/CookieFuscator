@echo off
setlocal
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"

echo [+] Using JAVA_HOME: %JAVA_HOME%

set "BIN_DIR=%~dp0bin"
if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"

where g++ >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [+] Building Windows x64 DLL with g++ MinGW...
    g++ -shared -O3 -s -fvisibility=hidden -fomit-frame-pointer -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" "%~dp0src\NativeBridge.cpp" -o "%BIN_DIR%\antiopsec_x64.dll" -lpsapi
    if %ERRORLEVEL% EQU 0 (
        echo [v] SUCCESS: Generated antiopsec_x64.dll
    ) else (
        echo [-] Compilation error with g++.
    )
) else (
    echo [!] Note: g++ not in PATH. Native C++ source and CMakeLists.txt ready.
)
echo [v] Native build script completed.
