# ===================================================================
# COOKIECHESS 1-CLICK BUILD & ENTERPRISE OBFUSCATION SCRIPT
# ===================================================================
param (
    [string]$TargetDir = "E:\SERVER\plugin-pre\Unique\cookiechess\MineChess-main",
    [string]$ObfRoot = "E:\SERVER\plugin-pre\Unique\Obf Logic"
)

$ErrorActionPreference = "Stop"
Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "   CookieChess Master Obfuscation Pipeline (Java 21)" -ForegroundColor Cyan
Write-Host "=======================================================`n" -ForegroundColor Cyan

# Step 1: Maven Package
Write-Host "[1/4] Compiling CookieChess with Maven..." -ForegroundColor Yellow
$mvn = "E:\SERVER\plugin-pre\test\jpremium-done\.tools\apache-maven-3.9.14\bin\mvn.cmd"
& $mvn clean package -DskipTests -f "$TargetDir\pom.xml" | Out-Null
Write-Host "[v] Maven Build Succeeded." -ForegroundColor Green

# Step 2: Build Classpath
Write-Host "[2/4] Resolving Classpath Dependencies..." -ForegroundColor Yellow
& $mvn dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt" -f "$TargetDir\pom.xml" | Out-Null

# Step 3: Run ProGuard with 31k Optical Homoglyphs
Write-Host "[3/4] Executing ProGuard Obfuscation..." -ForegroundColor Yellow
$inJar = "$TargetDir\target\CookieChess-V1.jar"
$outJar = "$TargetDir\target\CookieChess-Obfuscated.jar"
$pgJar = "$ObfRoot\tools\proguard.jar"
$pgCfg = "$ObfRoot\configs\cookiechess_proguard.pro"
$javaBin = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot\bin\java.exe"
$jmods = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot\jmods"

$cpList = (Get-Content "$TargetDir\target\classpath.txt").Split(';')

$pgArgs = @("-jar", $pgJar, "@$pgCfg", "-injars", $inJar, "-outjars", $outJar)
$pgArgs += "-libraryjars"
$pgArgs += "$jmods/java.base.jmod(!module-info.class)"
$pgArgs += "-libraryjars"
$pgArgs += "$jmods/java.logging.jmod(!module-info.class)"
$pgArgs += "-libraryjars"
$pgArgs += "$jmods/java.desktop.jmod(!module-info.class)"
$pgArgs += "-libraryjars"
$pgArgs += "$jmods/java.sql.jmod(!module-info.class)"
$pgArgs += "-libraryjars"
$pgArgs += "$jmods/java.management.jmod(!module-info.class)"
$pgArgs += "-libraryjars"
$pgArgs += "$jmods/jdk.httpserver.jmod(!module-info.class)"

foreach ($cp in $cpList) {
    if ($cp -and (Test-Path $cp)) {
        $pgArgs += "-libraryjars"
        $pgArgs += "$cp(!META-INF/MANIFEST.MF)"
    }
}

& $javaBin $pgArgs | Out-Null

if (-not (Test-Path $outJar)) {
    Write-Error "[-] ProGuard execution failed to generate $outJar!"
}
Write-Host "[v] ProGuard Obfuscation Succeeded." -ForegroundColor Green

# Step 4: Metadata Stripping & Final Packaging
Write-Host "[4/4] Stripping Maven Metadata & Generating Final Artifacts..." -ForegroundColor Yellow
$finalJar = "$TargetDir\target\CookieChess-2.8.2-PROT.jar"
$distJar = "$ObfRoot\dist\CookieChess-2.8.2-PROT.jar"

& python -c "
import zipfile, os, shutil
in_jar = r'$outJar'
out_jar = r'$finalJar'
dist_jar = r'$distJar'
with zipfile.ZipFile(in_jar, 'r') as zin:
    with zipfile.ZipFile(out_jar, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            fn = item.filename
            if fn.startswith('META-INF/maven/') or fn.startswith('mc/cookieee/libs/'):
                continue
            zout.writestr(item, zin.read(fn))
os.makedirs(os.path.dirname(dist_jar), exist_ok=True)
shutil.copyfile(out_jar, dist_jar)
"

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host " [v] OBFUSCATION COMPLETE FOR COOKIECHESS!" -ForegroundColor Green
Write-Host " Output Artifacts:" -ForegroundColor White
Write-Host "   * $TargetDir\target\CookieChess-V1.jar (Original)" -ForegroundColor Gray
Write-Host "   * $finalJar (Enterprise Obfuscated)" -ForegroundColor Yellow
Write-Host "   * $distJar (Dist Distribution)" -ForegroundColor Yellow
Write-Host "=======================================================`n" -ForegroundColor Cyan
