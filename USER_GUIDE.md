# 🍪 CookieFuscator User Guide & Integration Manual

Welcome to the **CookieFuscator** comprehensive user guide. This manual covers installation, CLI command flags, integration into development workflows, and custom protection rule configuration.

---

## 📑 Table of Contents

1. [System Prerequisites](#1-system-prerequisites)
2. [CLI Syntax & Arguments](#2-cli-syntax--arguments)
3. [Protection Profiles Explained](#3-protection-profiles-explained)
4. [Step-by-Step Usage Examples](#4-step-by-step-usage-examples)
5. [How Autonomous Inspection Works](#5-how-autonomous-inspection-works)
6. [Preserving Custom Reflection / API Hooks](#6-preserving-custom-reflection--api-hooks)
7. [Troubleshooting & FAQs](#7-troubleshooting--faqs)

---

## 1. System Prerequisites

Before running CookieFuscator, ensure your workstation or build server has:

- **Operating System**: Windows 10/11 / Windows Server (PowerShell 5.1 or PowerShell 7+)
- **Java Development Kit (JDK)**: JDK 21+ installed and configured in `JAVA_HOME` or default path:
  `C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot`
- **Python 3**: Python 3.8+ with PyYAML:
  ```powershell
  pip install pyyaml
  ```
- **Apache Maven**: (Required only if obfuscating directly from source code):
  ```powershell
  mvn -version
  ```

---

## 2. CLI Syntax & Arguments

The master CLI command is invoked via `CookieFuscator.ps1`:

```powershell
.\CookieFuscator.ps1 -Target <Path> [-Profile <Standard|Ultra>] [-Repackage <PackageName>] [-OutputDir <Path>] [-SkipBuild]
```

### Argument Reference Table

| Parameter | Type | Required | Default Value | Description |
|:---|:---:|:---:|:---:|:---|
| `-Target` | String | **Yes** | *None* | Path to plugin source directory (with `pom.xml`) OR path to a compiled `.jar` file. |
| `-Profile` | String | No | `Standard` | Obfuscation profile: `Standard` (Native Speed & Full Compat) or `Ultra` (Decoy Bytecode Container). |
| `-Repackage` | String | No | `cookie.fack.please.d111` | Root package name to flatten all obfuscated classes into. |
| `-OutputDir` | String | No | `E:\...\Obf Logic\dist` | Output folder for finalized protected binaries. |
| `-SkipBuild` | Switch | No | `False` | Skips running `mvn clean package` if the target `target/*.jar` was already compiled. |
| `-KeepMetadata` | Switch | No | `False` | If set, preserves Maven `pom.xml` properties inside the JAR. Default strips all metadata. |

---

## 3. Protection Profiles Explained

### 🟢 Profile: `Standard` (Recommended for High-Performance Plugins)
- **Mechanism**: Repackages all classes into `cookie.fack.please.d111.*` using the 31,394 Optical Homoglyph dictionary with Aggressive Method Overloading.
- **Compatibility**: 100% Spigot / Paper / Folia native compatibility.
- **Preservation**: All YAML config files (`config.yml`, `messages.yml`, `piece.yml`, etc.) remain accessible to server owners.
- **Overhead**: **0.00%** (Identical execution speed to vanilla bytecode).

### 🔴 Profile: `Ultra` (Decoy Bytecode Virtualization)
- **Mechanism**: Encrypts 100% of real class files into `assets/engine.dat` with dynamic S-box + rolling key cipher. Injects a Decoy `Bootstrap.class` that decrypts bytecode directly in RAM upon server startup.
- **Decompiler View**: Opening the JAR in JD-GUI / CFR / Recaf reveals **0 original classes**. Decompilers only see 1 small decoy loader.
- **Overhead**: Tiny one-time ~15ms memory decryption during `onLoad()`, zero runtime overhead thereafter.

---

## 4. Step-by-Step Usage Examples

### Example 1: Obfuscating CookieChess from Source
```powershell
cd "E:\SERVER\plugin-pre\Unique\Obf Logic"
.\CookieFuscator.ps1 -Target "E:\SERVER\plugin-pre\Unique\cookiechess\MineChess-main"
```
**Output**: `dist/CookieChess-2.8.2-PROT.jar`

### Example 2: Obfuscating AntiSpoofing with Ultra Decoy Virtualization
```powershell
cd "E:\SERVER\plugin-pre\Unique\Obf Logic"
.\CookieFuscator.ps1 -Target "E:\SERVER\plugin-pre\Unique\AntiOpsecMod" -Profile Ultra
```
**Output**: `dist/AntiSpoofing-1.2-PROT.jar`

### Example 3: Obfuscating a Pre-Compiled Third-Party JAR
```powershell
.\CookieFuscator.ps1 -Target "C:\Users\KHOA\Desktop\MyCustomPlugin.jar"
```
**Output**: `dist/MyCustomPlugin-1.0.0-PROT.jar`

---

## 5. How Autonomous Inspection Works

When you run CookieFuscator on a project:
1. `universal_engine.py` scans `plugin.yml` / `bungee.yml` / `velocity-plugin.json` to find the Main Class.
2. The engine scans the abstract syntax tree (AST) and import statements of your source code to detect:
   - Bukkit/Bungee Events (`@EventHandler`, `@Subscribe`)
   - Commands & Tab Completers
   - PlaceholderAPI Expansion classes
   - PacketEvents & ProtocolLib packet listeners
   - Shaded HikariCP & SLF4J database drivers
   - ModelEngine & MythicMobs blueprints
3. CookieFuscator automatically generates a tailored configuration file at `configs/auto_<PluginName>_proguard.pro`.
4. It resolves all Paper API and plugin dependencies from your `.m2` repository and applies the 31k homoglyph mapping.

---

## 6. Preserving Custom Reflection / API Hooks

If your plugin uses custom reflection to call internal methods by exact string name, you can add keep rules to the generated config or a custom template in `configs/`:

```proguard
# Keep specific internal API class methods from being renamed
-keepclassmembers class com.myplugin.api.InternalBridge {
    public static void customReflectionMethod(...);
}
```

---

## 7. Troubleshooting & FAQs

### Q: Why do decompilers fail to open classes or show confusing symbols?
> **A**: This is intentional! CookieFuscator uses 31,394 optical tokens (`I111`, `I111l`, `I111I1`) combined with Aggressive Method Overloading (`a()`, `a(int)`). Decompilers like Recaf, JD-GUI, and CFR collapse or display empty method bodies due to signature collision.

### Q: Can server admins still edit `config.yml`?
> **A**: **Yes!** All YAML files (`config.yml`, `messages.yml`, `signs.yml`, etc.) are preserved in plaintext within the JAR unless explicitly configured for binary encryption in `Ultra` mode.

### Q: Does this support Java 21 features like Pattern Matching and Records?
> **A**: **Yes!** CookieFuscator is powered by hardened ProGuard 7.4.2 + Java 21 JMods bytecode compatibility layer.

---

<p align="center">
  <sub>CookieFuscator Enterprise Documentation • (c) 2026 LoveCookieee / Khoa</sub>
</p>
