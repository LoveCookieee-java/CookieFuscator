# ===================================================================
# 🍪 COOKIEFUSCATOR - UNIVERSAL JVM & MINECRAFT PLUGIN OBFUSCATION ENGINE
# Multi-Platform: Paper, Spigot, Folia, Purpur, BungeeCord, Velocity
# Java 8 - Java 21 Supported
# ===================================================================
param (
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Target,

    [ValidateSet("Ultra", "Standard")]
    [string]$Profile = "Ultra",

    [string]$Repackage = "cookie.fack.please.d111",
    [string]$OutputDir = "E:\SERVER\plugin-pre\Unique\Obf Logic\dist",
    [switch]$SkipBuild = $false,
    [switch]$KeepMetadata = $false
)

$ErrorActionPreference = "Stop"
$EngineRoot = "E:\SERVER\plugin-pre\Unique\Obf Logic"
$EngineScript = "$EngineRoot\engine\universal_engine.py"
$DictFile = "$EngineRoot\dictionaries\confusing_dict.txt"
$PgJar = "$EngineRoot\tools\proguard.jar"
$JavaBin = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot\bin\java.exe"
$Jmods = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot\jmods"
$MavenBin = "E:\SERVER\plugin-pre\test\jpremium-done\.tools\apache-maven-3.9.14\bin\mvn.cmd"

Write-Host "`n=======================================================" -ForegroundColor Magenta
Write-Host "   🍪 COOKIEFUSCATOR - UNIVERSAL OBFUSCATION ENGINE 🍪" -ForegroundColor Magenta
Write-Host "=======================================================`n" -ForegroundColor Magenta

# 1. Target Inspection
Write-Host "[1/5] Auto-Inspecting Target Project / Binary..." -ForegroundColor Cyan
if (-not (Test-Path $Target)) {
    Write-Error "[-] Target path does not exist: $Target"
}

$inspectJson = & python "$EngineScript" inspect "$Target"
$info = $inspectJson | ConvertFrom-Json

Write-Host "  -> Plugin Name  : $($info.name)" -ForegroundColor White
Write-Host "  -> Main Class   : $($info.main_class)" -ForegroundColor Green
Write-Host "  -> Target Type  : $($info.type)" -ForegroundColor White
Write-Host "  -> Platform     : $($info.platform)" -ForegroundColor White
Write-Host "  -> PAPI Hook    : $($info.has_papi)" -ForegroundColor Gray
Write-Host "  -> Packet Hook  : $($info.has_packetevents)" -ForegroundColor Gray
Write-Host "  -> HikariCP DB  : $($info.has_hikaricp)" -ForegroundColor Gray
Write-Host "  -> ModelEngine  : $($info.has_modelengine)" -ForegroundColor Gray

# 2. Build Target If Source
$inputJar = ""
$targetBaseDir = ""
$classpathFile = "$EngineRoot\configs\temp_classpath.txt"

if ($info.type -eq "SOURCE") {
    $targetBaseDir = $Target
    $pomFile = Join-Path $Target "pom.xml"
    if (Test-Path $pomFile) {
        if (-not $SkipBuild) {
            Write-Host "`n[2/5] Compiling Target with Maven..." -ForegroundColor Cyan
            & $MavenBin clean package -DskipTests -f "$pomFile" | Out-Null
            Write-Host "[v] Maven Build Succeeded." -ForegroundColor Green
        }
        Write-Host "[2/5] Resolving Dependency Classpath..." -ForegroundColor Cyan
        & $MavenBin dependency:build-classpath "-Dmdep.outputFile=$classpathFile" -f "$pomFile" | Out-Null
    }

    # Locate generated jar in target folder
    $targetFolder = Join-Path $Target "target"
    $candidateJars = Get-ChildItem -Path $targetFolder -Filter "*.jar" | Where-Object { 
        $_.Name -notmatch "original-" -and $_.Name -notmatch "-PROT" -and $_.Name -notmatch "-Obfuscated" 
    }
    if ($candidateJars.Count -gt 0) {
        $inputJar = $candidateJars[0].FullName
    } else {
        Write-Error "[-] Could not find compiled jar in $targetFolder"
    }
} else {
    $inputJar = $Target
    $targetBaseDir = Split-Path -Path $Target -Parent
}

Write-Host "`n  -> Input JAR: $inputJar" -ForegroundColor Gray

# 3. Dynamic Rule Generation
Write-Host "`n[3/5] Generating Custom ProGuard Ruleset..." -ForegroundColor Cyan
$autoCfgPath = "$EngineRoot\configs\auto_$($info.name)_proguard.pro"
& python "$EngineScript" generate_config "$Target" "$DictFile" "$Repackage" "$autoCfgPath"
Write-Host "[v] Config generated at: $autoCfgPath" -ForegroundColor Green

# 4. ProGuard 31k Optical Homoglyph Obfuscation
Write-Host "`n[4/5] Executing ProGuard (31,394 Homoglyphs + Overload Aggressive)..." -ForegroundColor Cyan
$obfTempJar = "$EngineRoot\dist\temp_$($info.name)_obf.jar"

$pgArgs = @("-jar", $PgJar, "@$autoCfgPath", "-injars", $inputJar, "-outjars", $obfTempJar)
$pgArgs += "-libraryjars"
$pgArgs += "$Jmods/java.base.jmod(!module-info.class)"
$pgArgs += "-libraryjars"
$pgArgs += "$Jmods/java.logging.jmod(!module-info.class)"
$pgArgs += "-libraryjars"
$pgArgs += "$Jmods/java.desktop.jmod(!module-info.class)"
$pgArgs += "-libraryjars"
$pgArgs += "$Jmods/java.sql.jmod(!module-info.class)"
$pgArgs += "-libraryjars"
$pgArgs += "$Jmods/java.management.jmod(!module-info.class)"
$pgArgs += "-libraryjars"
$pgArgs += "$Jmods/jdk.httpserver.jmod(!module-info.class)"

if (Test-Path $classpathFile) {
    $cpEntries = (Get-Content $classpathFile).Split(';')
    foreach ($cp in $cpEntries) {
        if ($cp -and (Test-Path $cp)) {
            $pgArgs += "-libraryjars"
            $pgArgs += "$cp(!META-INF/MANIFEST.MF)"
        }
    }
}

& $JavaBin $pgArgs | Out-Null

if (-not (Test-Path $obfTempJar)) {
    Write-Error "[-] ProGuard failed to generate obfuscated binary."
}
Write-Host "[v] ProGuard Stage Succeeded." -ForegroundColor Green

# 5. Profile Finalization (Standard vs Ultra)
Write-Host "`n[5/5] Finalizing Output Profile [$Profile] & Encrypting Internal Resources..." -ForegroundColor Cyan
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$cleanVer = $info.version -replace '[^0-9\.]', ''
if (-not $cleanVer) { $cleanVer = "1.0.0" }
$finalDistJar = Join-Path $OutputDir "$($info.name)-$cleanVer-PROT.jar"

if ($Profile -eq "Ultra") {
    # Decoy Stub Injection (Pack all classes AND all internal YAMLs/resources into assets/engine.dat)
    Write-Host "  -> Encrypting Bytecode & All Internal YAML Resources into assets/engine.dat..." -ForegroundColor Yellow
    $bootBin = "$EngineRoot\CookieFuscator\bootstrap_bin"
    $bootSrc = "$EngineRoot\CookieFuscator\bootstrap_src\cookie\fack\please\d111\Bootstrap.java"
    $paperJar = "C:\Users\KHOA\.m2\repository\io\papermc\paper\paper-api\1.21.4-R0.1-SNAPSHOT\paper-api-1.21.4-R0.1-SNAPSHOT.jar"
    $javac = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot\bin\javac.exe"
    
    & $javac -cp $paperJar -d $bootBin $bootSrc | Out-Null
    $bootCls = "$bootBin\cookie\fack\please\d111\Bootstrap.class"

    & python -c "
import zipfile, io, os
v2 = r'$obfTempJar'
boot_cls = r'$bootCls'
final_jar = r'$finalDistJar'
def enc(b):
    out = bytearray(len(b))
    rk = 0x5D
    for i in range(len(b)):
        p = b[i]
        out[i] = ((p + (i & 0x0F)) & 0xFF) ^ rk
        rk = ((rk * 37) ^ p) & 0xFF
    return bytes(out)

with open(boot_cls, 'rb') as bf: boot_bytes = bf.read()
with zipfile.ZipFile(v2, 'r') as zin:
    names = zin.namelist()
    
    # Everything except manifest and plugin.yml goes into encrypted engine.dat
    engine_buf = io.BytesIO()
    with zipfile.ZipFile(engine_buf, 'w', compression=zipfile.ZIP_DEFLATED) as ez:
        for item in zin.infolist():
            fn = item.filename
            if fn.startswith('META-INF/') or fn in ['plugin.yml', 'bungee.yml', 'velocity-plugin.json']:
                continue
            if not fn.endswith('/'):
                ez.writestr(fn, zin.read(fn))
                
    enc_engine = enc(engine_buf.getvalue())
    
    with zipfile.ZipFile(final_jar, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            fn = item.filename
            # Exclude all classes, all internal YAMLs/resources, and shaded directories
            if fn.endswith('.class') or fn.endswith('.yml') or fn.endswith('.yaml') or fn.endswith('.json') or fn.endswith('.bin') or fn.endswith('.txt'):
                if fn not in ['plugin.yml', 'bungee.yml']:
                    continue
            if fn.startswith('META-INF/maven/') or fn.startswith('dev/') or fn.startswith('mc/') or fn.startswith('mcp/') or fn.startswith('org/') or fn.startswith('io/') or fn.startswith('com/') or fn.startswith('cookie/'):
                continue
                
            if fn in ['plugin.yml', 'bungee.yml']:
                p_text = zin.read(fn).decode('utf-8')
                import re
                new_p = re.sub(r'main:\s*.*', 'main: cookie.fack.please.d111.Bootstrap', p_text)
                zout.writestr(item, new_p.encode('utf-8'))
            elif fn.startswith('META-INF/'):
                zout.writestr(item, zin.read(fn))
                
        zout.writestr('cookie/fack/please/d111/Bootstrap.class', boot_bytes)
        zout.writestr('assets/engine.dat', enc_engine)
"
} else {
    # Standard: Clean metadata & copy to final
    & python "$EngineScript" clean_metadata "$obfTempJar" "$finalDistJar"
}

# Copy to target directory if source project
if ($info.type -eq "SOURCE" -and (Test-Path "$targetBaseDir\target")) {
    Copy-Item -Path $finalDistJar -Destination "$targetBaseDir\target\$($info.name)-$cleanVer-PROT.jar" -Force
}

# Remove temp file
if (Test-Path $obfTempJar) { Remove-Item -Path $obfTempJar -Force }
if (Test-Path $classpathFile) { Remove-Item -Path $classpathFile -Force }

Write-Host "`n=======================================================" -ForegroundColor Green
Write-Host " [v] COOKIEFUSCATOR OBFUSCATION COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host " Output Binary (Profile: $Profile):" -ForegroundColor White
Write-Host "   * $finalDistJar" -ForegroundColor Yellow
Write-Host "=======================================================`n" -ForegroundColor Green
