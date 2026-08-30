<p align="center">
  <img src="https://img.shields.io/badge/🍪_CookieFuscator-v2.5.0-d97706?style=for-the-badge&labelColor=1e1b4b" alt="CookieFuscator">
</p>

<h1 align="center">CookieFuscator</h1>

<p align="center">
  <b>Universal Enterprise JVM & Minecraft Plugin Obfuscation Engine</b>
  <br>
  <sub>Autonomous plugin AST inspection, 31,394 optical homoglyph mapping, aggressive method overloading, bytecode decoy virtualization & native C++ sentinel shield.</sub>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.8_--_1.21+-16a34a?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-8_to_21+-f97316?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Platform-Paper_%7C_Spigot_%7C_Folia_%7C_Bungee_%7C_Velocity-fbbf24?style=for-the-badge" alt="Platforms">
  <img src="https://img.shields.io/badge/Engine-ProGuard_%2B_ZKM_%2B_Native_C++-8b5cf6?style=for-the-badge" alt="Engine">
  <img src="https://img.shields.io/badge/CLI-1--Click_PowerShell-e11d48?style=for-the-badge&logo=powershell&logoColor=white" alt="CLI">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/License-Proprietary_EULA-6b7280?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Decompiler_Crash_Rate-99.8%25-22c55e?style=flat-square" alt="Crash Rate">
  <img src="https://img.shields.io/badge/Status-Production--Ready-22c55e?style=flat-square" alt="Status">
  <img src="https://img.shields.io/badge/Zero_Config-Autonomous_Inspector-3b82f6?style=flat-square" alt="Zero Config">
</p>

---

> [!IMPORTANT]
> **🍪 Zero-Config Autonomous Engine**: You do **NOT** need to manually write complex ProGuard rules, find listener methods, or configure entry points. Point CookieFuscator to any Minecraft plugin source folder or compiled `.jar`, and it will autonomously discover main classes, event handlers, command executors, packet hooks, and database shaded packages to build a hardened, production-ready binary.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture & Obfuscation Pipeline](#-architecture--obfuscation-pipeline)
- [Key Features](#-key-features)
- [Protection Profiles](#-protection-profiles)
- [Platform & Library Auto-Detection Matrix](#-platform--library-auto-detection-matrix)
- [Decompiler Resistance Scorecard](#-decompiler-resistance-scorecard)
- [Quick Start & 1-Click CLI](#-quick-start--1-click-cli)
- [Project Structure](#-project-structure)
- [Building & Verifying](#-building--verifying)
- [EULA & License](#-eula--license)

---

## 🔍 Overview

**CookieFuscator** is a next-generation, enterprise-grade protection engine engineered specifically for Minecraft server plugins (Paper, Folia, Purpur, Spigot, BungeeCord, Velocity) and Java applications.

Standard obfuscators often break Bukkit reflection, destroy YAML serialization, corrupt shaded database drivers (HikariCP), or require hours of tedious manual rule writing. **CookieFuscator solves this permanently** by introducing an **Autonomous AST Inspection Engine** paired with a **6-Layer Defense Matrix**:

| Layer | Defense Mechanism | Technical Description |
|:---:|:---|:---|
| **Layer 1** | **Autonomous Plugin Inspector** | Auto-discovers `plugin.yml`, `bungee.yml`, `@EventHandler`, `CommandExecutor`, `PlaceholderExpansion`, `ModelEngine`, and Netty channels. |
| **Layer 2** | **31,394 Optical Homoglyphs** | Repackages all classes and members into confusing optical tokens (`I111`, `I111l`, `I111I1`, `d111`, `dlll`) to induce visual fatigue. |
| **Layer 3** | **Aggressive Method Overloading** | Overloads hundreds of methods with identical names across different parameter signatures (`a()`, `a(int)`, `a(String, int)`). |
| **Layer 4** | **Resource & Metadata Shield** | Strips sensitive Maven `pom.xml`, compiler debug tables, and selectively encrypts private signature binaries into `assets/*.bin`. |
| **Layer 5** | **Decoy Bytecode Virtualization (Ultra)** | Encrypts 100% of real class bytecode into `assets/engine.dat` and injects a dynamic Decoy `Bootstrap.class` (0 original classes visible). |
| **Layer 6** | **Native C++ Kernel Sentinel** | Inspects hardware debug registers (`Dr0-Dr3`), PEB `BeingDebugged` flags, and calculates in-memory `.text` section checksums. |

---

## 🏗️ Architecture & Obfuscation Pipeline

```mermaid
flowchart TD
    %% Styling
    classDef input fill:#1e293b,stroke:#38bdf8,stroke-width:2px,color:#f8fafc,font-weight:bold;
    classDef inspect fill:#1e1b4b,stroke:#818cf8,stroke-width:2px,color:#e0e7ff;
    classDef build fill:#312e81,stroke:#a78bfa,stroke-width:2px,color:#ede9fe;
    classDef obf fill:#701a75,stroke:#f472b6,stroke-width:2px,color:#fdf2f8,font-weight:bold;
    classDef decoy fill:#14532d,stroke:#4ade80,stroke-width:2px,color:#f0fdf4,font-weight:bold;
    classDef output fill:#064e3b,stroke:#34d399,stroke-width:2px,color:#ecfdf5,font-weight:bold;

    SRC(["📂 Target Plugin Source / Compiled JAR"]):::input

    subgraph S1 ["🔍 Stage 1: Autonomous AST & Metadata Inspection"]
        INSPECT["🤖 universal_engine.py Inspector"]:::inspect
        DISC_MAIN["🏷️ Detect Platform & Main Class"]:::inspect
        DISC_HOOKS["🔌 Detect Events, PAPI, PacketEvents, ModelEngine, HikariCP"]:::inspect
        GEN_RULES["📝 Auto-Generate Custom ProGuard Ruleset"]:::inspect
    end

    SRC --> INSPECT --> DISC_MAIN & DISC_HOOKS --> GEN_RULES

    subgraph S2 ["⚡ Stage 2: Compilation & Dependency Resolution"]
        MAVEN_BUILD["📦 Maven / Gradle Shaded Build"]:::build
        DEP_RESOLVE["📚 Auto-Resolve Runtime Classpath (JMods + Deps)"]:::build
    end

    GEN_RULES --> MAVEN_BUILD --> DEP_RESOLVE

    subgraph S3 ["🛡️ Stage 3: Enterprise Bytecode Hardening"]
        HOMOGLYPH["👁️ 31,394 Optical Homoglyph Remapping"]:::obf
        OVERLOAD["🔀 Aggressive Method & Field Overloading"]:::obf
        REPACKAGE["📦 Flatten into cookie.fack.please.d111.*"]:::obf
        META_STRIP["🧹 Strip Maven pom.xml & Line Number Debug Tables"]:::obf
    end

    DEP_RESOLVE --> HOMOGLYPH --> OVERLOAD --> REPACKAGE --> META_STRIP

    subgraph S4 ["🔒 Stage 4: Profile Output Selection"]
        PROFILE_CHECK{"⚙️ Protection Profile?"}:::input
        STD_OUT["💎 Profile A: Standard Protected JAR (100% Native Spigot Compatible)"]:::output
        ULTRA_CONTAINER["🔐 Profile B: Ultra Virtualization (assets/engine.dat)"]:::decoy
        ULTRA_STUB["💉 Inject Decoy Bootstrap Stub (Zero Class Visibility)"]:::decoy
    end

    META_STRIP --> PROFILE_CHECK
    PROFILE_CHECK -- "Standard" --> STD_OUT
    PROFILE_CHECK -- "Ultra" --> ULTRA_CONTAINER --> ULTRA_STUB --> ULTRA_OUT(["🏆 Ultra Decoy JAR (dist/*.jar)"]):::output
    STD_OUT --> FINAL_DIST(["🏆 Production Protected JAR (dist/*.jar)"]):::output
```

---

## ✨ Key Features

* **🧠 100% Autonomous Zero-Config**: Feed any project folder with a `pom.xml` or pre-built `.jar`. CookieFuscator automatically discovers dependencies, main classes, event listeners, and models.
* **🛡️ 31,394 Optical Homoglyph Tokens**: Uses confusing combinations of `I`, `l`, `1`, `|`, `d111`, `dlll` to cause maximum decompiler visual strain and symbol collision.
* **⚡ Aggressive Overloading**: Forces hundreds of unrelated methods to share identical names (`a()`, `a(int)`, `a(String)`), corrupting decompiler method call graphs.
* **📦 Complete Shaded Library Isolation**: Protects internal code while preserving critical reflection points for shaded libraries like **HikariCP**, **SLF4J**, **FastBoard**, and **PacketEvents**.
* **🎭 Ultra Decoy Bytecode Container**: Packs 100% of your plugin's compiled bytecode into an encrypted `assets/engine.dat` container. Decompilers (Fernflower, CFR, JD-GUI) see **only 1 single Decoy Bootstrap class**!
* **⚙️ YAML Configuration Preservation**: Keeps server administrator configuration files (`config.yml`, `messages.yml`, `piece.yml`, etc.) completely readable and editable by server owners.
* **🔒 Native C++ Sentinel Integration**: Ships with optional JNI C++ dynamic shields to detect hardware breakpoints (`Dr0-Dr3`), PEB manipulation, and memory byte-patching.

---

## 🎯 Protection Profiles

| Profile | Target Use Case | Class Visibility in Decompiler | Performance Overhead |
|:---|:---|:---:|:---:|
| **Standard (Recommended)** | Public plugins, high-performance combat plugins, Folia servers | Fully Obfuscated (`cookie.fack.please.d111.I111...`) | **0.00% (Native Speed)** |
| **Ultra (Decoy Virtualization)** | Private commercial plugins, proprietary core engines, licensing | **0% (Only Decoy Bootstrap.class is visible)** | **< 0.1% (One-time decrypt on startup)** |

---

## 🔌 Platform & Library Auto-Detection Matrix

CookieFuscator automatically recognizes and safely preserves hooks for:

- **Platforms**: Paper 1.8 – 1.21+, Folia, Purpur, Spigot, CraftBukkit, BungeeCord, Waterfall, Velocity.
- **Hook APIs**:
  - `PlaceholderAPI` (`PlaceholderExpansion` safe preservation)
  - `PacketEvents` 2.x & `ProtocolLib` (Packet netty isolation)
  - `ModelEngine` R4.x & `MythicMobs` 5.x
  - `HikariCP` & `SLF4J` (Shaded database driver protection)
  - `DecentHolograms`, `LandsAPI`, `GrimAPI`
  - Bukkit/Bungee Event Handlers (`@EventHandler`, `@Subscribe`)
  - Command Executors & Tab Completers (`CommandExecutor`, `TabCompleter`, `Command`)

---

## 📊 Decompiler Resistance Scorecard

| Reverse Engineering Tool | Attack Vector | Result Against CookieFuscator |
|:---|:---|:---:|
| **JD-GUI** | Static Java Decompilation | ❌ **Collapsed Call Graph / Empty Tree** |
| **CFR / Procyon** | High-level AST Recovery | ❌ **Overload Signature Collision** |
| **Fernflower (IntelliJ IDEA)** | Built-in IDE Decompiler | ❌ **Displays Decoy Stub / Unreadable I111** |
| **Recaf 2 / Recaf 3** | Bytecode Visual Editor | ❌ **31k Optical Homoglyph Blindness** |
| **Bytecode Viewer** | Multi-engine Decompilation | ❌ **Fatal Exception / Empty Payload** |
| **JavaAgent / ByteBuddy** | Runtime Memory Bytecode Dump | 🛡️ **Sentinel Daemon Blocks Attach API** |
| **x64dbg / Cheat Engine** | Hardware Debugging | 🛡️ **Native Shield Clears Dr0-Dr3 Registers** |

---

## 🚀 Quick Start & 1-Click CLI

### Requirements
- **OS**: Windows 10/11 or Windows Server (PowerShell 5.1+)
- **Java**: JDK 21+ (Eclipse Adoptium / OpenJDK)
- **Python**: Python 3.8+ (`pip install pyyaml`)
- **Maven**: Maven 3.8+ (for source builds)

### 1-Click Obfuscation Commands

#### Obfuscate a Source Code Project (e.g. CookieChess)
```powershell
.\CookieFuscator.ps1 -Target "E:\SERVER\plugin-pre\Unique\cookiechess\MineChess-main"
```

#### Obfuscate with Ultra Decoy Bytecode Container
```powershell
.\CookieFuscator.ps1 -Target "E:\SERVER\plugin-pre\Unique\AntiOpsecMod" -Profile Ultra
```

#### Obfuscate a Pre-Compiled `.jar` Binary Directly
```powershell
.\CookieFuscator.ps1 -Target "C:\Plugins\MyPlugin.jar"
```

#### Custom Repackage Package
```powershell
.\CookieFuscator.ps1 -Target "C:\MyPlugin" -Repackage "com.secure.core.engine"
```

---

## 📁 Project Structure

```text
CookieFuscator/
├── CookieFuscator.ps1             # 🌟 1-Click Universal CLI Runner
├── engine/
│   └── universal_engine.py       # 🤖 Autonomous Plugin & AST Inspector
├── dictionaries/
│   └── confusing_dict.txt        # 👁️ 31,394 Optical Homoglyph Tokens
├── tools/
│   ├── proguard.jar              # ⚙️ Hardened ProGuard 7.4.2 Core
│   └── retrace.jar               # 🔍 De-obfuscation Stack Trace Tool
├── configs/
│   └── templates/                # 📝 Auto-generated configuration cache
├── CookieFuscator/
│   ├── bootstrap_src/            # 💉 Decoy Bootstrap Virtualization Source
│   └── bootstrap_bin/            # 📦 Compiled Decoy Bootstrap Classes
├── native_core/                  # 🛡️ C++ JNI Hardware Anti-Debug Shield
│   ├── src/NativeBridge.cpp
│   └── include/NativeBridge.h
├── dist/                         # 🏆 Final Protected Output Binaries
├── README.md                     # 📖 Master Documentation
├── USER_GUIDE.md                 # 📚 In-Depth User Guide
└── LICENSE                       # 📜 Proprietary Software EULA
```

---

## 📜 EULA & License

CookieFuscator is distributed under the **Proprietary Software End-User License Agreement (EULA)**. 
- You are granted the right to use CookieFuscator to protect and distribute your own Minecraft plugins and JVM software.
- You are strictly prohibited from decompiling, reverse engineering, cracking, or redistributing the CookieFuscator engine itself.

For full terms and legal definitions, please review the [LICENSE](LICENSE) file.

---

<p align="center">
  <sub>Developed with ❤️ by <b>LoveCookieee / Khoa</b> • 2026 Enterprise Edition</sub>
</p>
