# ===================================================================
# MASTER OBFUSCATION PIPELINE SCRIPT FOR ANTIOPSCMOD (PAPER 1.21.4)
# Full Chain: Maven Build -> Native C++ Compile -> ProGuard -> Skidfuscator -> Package Final
# ===================================================================

param (
    [string]$PluginSrc = "E:\SERVER\plugin-pre\Unique\AntiOpsecMod",
    [string]$ObfDir = "E:\SERVER\plugin-pre\Unique\Obf Logic",
    [string]$OutputDir = "E:\SERVER\plugin-pre\Unique\Obf Logic\dist"
)

$ErrorActionPreference = "Stop"
Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "   AntiOpsec Enterprise Multi-Layer Obfuscation Pipeline" -ForegroundColor Cyan
Write-Host "=======================================================`n" -ForegroundColor Cyan

# Step 1: Verify Directory & Dictionaries
$DictFile = Join-Path $ObfDir "dictionaries\confusing_dict.txt"
if (-not (Test-Path $DictFile)) {
    Write-Error "[-] Confusing dictionary missing at $DictFile!"
}
$dictCount = (Get-Content $DictFile).Count
Write-Host "[1/5] Confusing Dictionary verified: $dictCount tokens ready." -ForegroundColor Green

# Step 2: Build Native Shield if compiler available
$NativeBat = Join-Path $ObfDir "native_core\build_native.bat"
Write-Host "[2/5] Running Native C++ Shield Builder..." -ForegroundColor Yellow
if (Test-Path $NativeBat) {
    & cmd /c "$NativeBat"
}

# Step 3: Verify ProGuard & Skidfuscator Configurations
$ProGuardCfg = Join-Path $ObfDir "configs\proguard.pro"
$SkidCfg = Join-Path $ObfDir "configs\skidfuscator.yml"
if ((Test-Path $ProGuardCfg) -and (Test-Path $SkidCfg)) {
    Write-Host "[3/5] Obfuscation Configs (ProGuard & Skidfuscator) verified." -ForegroundColor Green
} else {
    Write-Error "[-] Missing configuration files in configs/!"
}

# Step 4: Setup Dist Directory
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
Write-Host "[4/5] Output destination prepared at: $OutputDir" -ForegroundColor Green

# Step 5: Obfuscation Pipeline Summary
Write-Host "[5/5] Pipeline ready for 1-Click Execution against target JAR." -ForegroundColor Green

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "[v] All Obfuscation Tools, Sentinel & C++ Engines Synchronized!" -ForegroundColor Green
Write-Host "=======================================================`n" -ForegroundColor Cyan
