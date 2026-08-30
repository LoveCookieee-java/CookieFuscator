# ===================================================================
# 🍪 COOKIEFUSCATOR - NATIVE C++ SENTINEL COMPILER (MinGW-w64 x64)
# ===================================================================
param (
    [string]$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
)

$ErrorActionPreference = "Stop"
$Root = "E:\SERVER\plugin-pre\Unique\Obf Logic"
$CompilerBin = "$Root\tools\compiler\bin"
$Gxx = "$CompilerBin\g++.exe"
$NativeSrc = "$Root\native_core\src\NativeBridge.cpp"
$OutDir = "$Root\tools\native"
$OutDll = "$OutDir\antiopsec_x64.dll"

if (-not (Test-Path $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
}

$env:PATH = "$CompilerBin;$env:PATH"

Write-Host "[+] Building Native C++ JNI Sentinel DLL (x64 Windows)..." -ForegroundColor Cyan
Write-Host "  -> Compiler: $Gxx" -ForegroundColor Gray
Write-Host "  -> Source  : $NativeSrc" -ForegroundColor Gray
Write-Host "  -> Output  : $OutDll" -ForegroundColor Gray

$args = @(
    "-shared",
    "-O3",
    "-s",
    "-fvisibility=hidden",
    "-fomit-frame-pointer",
    "-I$JavaHome\include",
    "-I$JavaHome\include\win32",
    "$NativeSrc",
    "-o", "$OutDll",
    "-lpsapi"
)

& $Gxx $args

if (Test-Path $OutDll) {
    $dllSize = (Get-Item $OutDll).Length
    Write-Host "[v] SUCCESS: Generated antiopsec_x64.dll ($dllSize bytes, fully stripped)." -ForegroundColor Green
} else {
    Write-Error "[-] Failed to compile native DLL."
}
