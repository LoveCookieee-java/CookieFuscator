import os
import sys
import zipfile
import re
import yaml
import shutil
import json

def inspect_plugin(target_path):
    info = {
        "type": "UNKNOWN",
        "name": "UnknownPlugin",
        "version": "1.0.0",
        "main_class": None,
        "platform": "Bukkit",
        "has_papi": False,
        "has_packetevents": False,
        "has_protocollib": False,
        "has_hikaricp": False,
        "has_modelengine": False,
        "has_mythic": False,
        "has_native": False,
        "yaml_resources": []
    }
    
    if os.path.isfile(target_path) and target_path.endswith('.jar'):
        info["type"] = "JAR"
        with zipfile.ZipFile(target_path, 'r') as z:
            for item in z.namelist():
                if item.endswith(('.yml', '.yaml')) and not item.startswith('META-INF/'):
                    info["yaml_resources"].append(item)
            
            if 'plugin.yml' in z.namelist():
                info["platform"] = "Bukkit"
                p_data = yaml.safe_load(z.read('plugin.yml').decode('utf-8', errors='ignore'))
                if p_data:
                    info["main_class"] = p_data.get('main')
                    info["name"] = p_data.get('name', 'UnknownPlugin')
                    info["version"] = str(p_data.get('version', '1.0.0'))
            elif 'bungee.yml' in z.namelist():
                info["platform"] = "Bungee"
                b_data = yaml.safe_load(z.read('bungee.yml').decode('utf-8', errors='ignore'))
                if b_data:
                    info["main_class"] = b_data.get('main')
                    info["name"] = b_data.get('name', 'UnknownPlugin')
                    info["version"] = str(b_data.get('version', '1.0.0'))
            elif 'velocity-plugin.json' in z.namelist():
                info["platform"] = "Velocity"
                v_data = json.loads(z.read('velocity-plugin.json').decode('utf-8', errors='ignore'))
                info["main_class"] = v_data.get('main')
                info["name"] = v_data.get('id', 'UnknownPlugin')
                info["version"] = str(v_data.get('version', '1.0.0'))
            
            # Check for version in maven properties inside jar if unresolved
            if info["version"].startswith('$') or not info["version"]:
                for fn in z.namelist():
                    if fn.endswith('pom.properties'):
                        prop_text = z.read(fn).decode('utf-8', errors='ignore')
                        m = re.search(r'version=([^\r\n]+)', prop_text)
                        if m:
                            info["version"] = m.group(1).strip()
                            break
            
            # Check packages in jar
            names = z.namelist()
            for n in names:
                if 'placeholderapi' in n.lower(): info["has_papi"] = True
                if 'packetevents' in n.lower(): info["has_packetevents"] = True
                if 'protocollib' in n.lower(): info["has_protocollib"] = True
                if 'hikari' in n.lower(): info["has_hikaricp"] = True
                if 'modelengine' in n.lower(): info["has_modelengine"] = True
                if 'mythic' in n.lower(): info["has_mythic"] = True
                if n.endswith(('.dll', '.so', '.dylib')): info["has_native"] = True
    else:
        info["type"] = "SOURCE"
        # Check pom.xml in source directory
        pom_xml = os.path.join(target_path, 'pom.xml')
        if os.path.exists(pom_xml):
            with open(pom_xml, 'r', encoding='utf-8', errors='ignore') as fp:
                pom_text = fp.read()
                v_match = re.search(r'<version>([^<]+)</version>', pom_text)
                if v_match:
                    info["version"] = v_match.group(1).strip()

        # Check plugin.yml in src/main/resources
        res_dir = os.path.join(target_path, 'src', 'main', 'resources')
        if os.path.exists(res_dir):
            for root, _, files in os.walk(res_dir):
                for f in files:
                    if f.endswith(('.yml', '.yaml')):
                        rel = os.path.relpath(os.path.join(root, f), res_dir).replace('\\', '/')
                        info["yaml_resources"].append(rel)
            
            p_yml = os.path.join(res_dir, 'plugin.yml')
            if os.path.exists(p_yml):
                info["platform"] = "Bukkit"
                with open(p_yml, 'r', encoding='utf-8', errors='ignore') as fp:
                    p_data = yaml.safe_load(fp)
                    if p_data:
                        info["main_class"] = p_data.get('main')
                        info["name"] = p_data.get('name', 'UnknownPlugin')
                        p_ver = str(p_data.get('version', ''))
                        if p_ver and not p_ver.startswith('$'):
                            info["version"] = p_ver
        
        # Check source files for libraries & patterns
        java_src = os.path.join(target_path, 'src', 'main', 'java')
        if os.path.exists(java_src):
            for root, _, files in os.walk(java_src):
                for f in files:
                    if f.endswith('.java'):
                        p = os.path.join(root, f)
                        with open(p, 'r', encoding='utf-8', errors='ignore') as fp:
                            content = fp.read()
                            if 'PlaceholderExpansion' in content: info["has_papi"] = True
                            if 'PacketEvents' in content or 'retrooper' in content: info["has_packetevents"] = True
                            if 'ProtocolLibrary' in content or 'ProtocolLib' in content: info["has_protocollib"] = True
                            if 'HikariDataSource' in content or 'HikariConfig' in content: info["has_hikaricp"] = True
                            if 'ModelEngine' in content or 'ticxo' in content: info["has_modelengine"] = True
                            if 'MythicMobs' in content or 'io.lumine' in content: info["has_mythic"] = True
                            if 'native ' in content: info["has_native"] = True
                            
    return info

def generate_proguard_config(info, dict_path, repackage_pkg, output_cfg_path):
    lines = [
        "# ===================================================================",
        f"# AUTO-GENERATED ENTERPRISE PROGUARD CONFIG FOR: {info['name']}",
        f"# Generated by CookieFuscator Master Engine",
        "# ===================================================================",
        "",
        "-ignorewarnings",
        "-dontwarn **",
        "-dontshrink",
        "-dontoptimize",
        "-dontusemixedcaseclassnames",
        "-overloadaggressively",
        "-allowaccessmodification",
        "",
        f"-repackageclasses '{repackage_pkg}'",
        "-keepdirectories",
        "-adaptresourcefilecontents plugin.yml,bungee.yml,velocity-plugin.json",
        "",
        f'-obfuscationdictionary "{dict_path}"',
        f'-classobfuscationdictionary "{dict_path}"',
        f'-packageobfuscationdictionary "{dict_path}"',
        "",
        "-keepattributes Exceptions,Signature,Deprecated,*Annotation*,EnclosingMethod,InnerClasses",
        "",
        "# 1. MAIN ENTRY POINT",
        "-keepclassmembers public class * extends org.bukkit.plugin.java.JavaPlugin {",
        "    public <init>();",
        "    public void onLoad();",
        "    public void onEnable();",
        "    public void onDisable();",
        "}",
        "-keepclassmembers public class * extends net.md_5.bungee.api.plugin.Plugin {",
        "    public <init>();",
        "    public void onLoad();",
        "    public void onEnable();",
        "    public void onDisable();",
        "}",
        "-keep @com.velocitypowered.api.plugin.Plugin class * { *; }",
        "",
        "# 2. EVENT LISTENERS",
        "-keepclassmembers class * implements org.bukkit.event.Listener {",
        "    @org.bukkit.event.EventHandler *;",
        "}",
        "-keepclassmembers class * implements net.md_5.bungee.api.plugin.Listener {",
        "    @net.md_5.bungee.event.EventHandler *;",
        "}",
        "-keepclassmembers class * {",
        "    @com.velocitypowered.api.event.Subscribe *;",
        "}",
        "",
        "# 3. COMMAND EXECUTORS",
        "-keepclassmembers class * implements org.bukkit.command.CommandExecutor {",
        "    public boolean onCommand(...);",
        "}",
        "-keepclassmembers class * implements org.bukkit.command.TabCompleter {",
        "    public java.util.List onTabComplete(...);",
        "}",
        "-keepclassmembers class * extends net.md_5.bungee.api.plugin.Command {",
        "    public <init>(...);",
        "    public void execute(...);",
        "}",
        "",
        "# 4. ENUMS & SERIALIZABLES",
        "-keepclassmembers enum * {",
        "    public static **[] values();",
        "    public static ** valueOf(java.lang.String);",
        "}",
        "-keepclassmembers class * implements java.io.Serializable { *; }",
    ]
    
    if info.get("has_papi"):
        lines.extend([
            "",
            "# 5. PLACEHOLDER EXPANSIONS",
            "-keepclassmembers class * extends me.clip.placeholderapi.expansion.PlaceholderExpansion {",
            "    public <init>(...);",
            "    public boolean persist();",
            "    public boolean canRegister();",
            "    public java.lang.String getAuthor();",
            "    public java.lang.String getIdentifier();",
            "    public java.lang.String getVersion();",
            "    public java.lang.String onPlaceholderRequest(...);",
            "    public java.lang.String onRequest(...);",
            "}"
        ])
        
    if info.get("has_packetevents"):
        lines.extend([
            "",
            "# 6. PACKETEVENTS",
            "-keep class com.github.retrooper.packetevents.** { *; }",
            "-keep class io.github.retrooper.packetevents.** { *; }"
        ])

    if info.get("has_protocollib"):
        lines.extend([
            "",
            "# 7. PROTOCOLLIB",
            "-keep class com.comphenix.protocol.** { *; }"
        ])
        
    if info.get("has_hikaricp"):
        lines.extend([
            "",
            "# 8. HIKARICP & LOGGERS",
            "-keep class com.zaxxer.hikari.** { *; }",
            "-keep class org.slf4j.** { *; }",
            "-keep class *.libs.hikaricp.** { *; }",
            "-keep class *.*.libs.hikaricp.** { *; }"
        ])
        
    if info.get("has_native"):
        lines.extend([
            "",
            "# 9. JNI NATIVE METHODS",
            "-keepclasseswithmembernames class * {",
            "    native <methods>;",
            "}"
        ])
        
    os.makedirs(os.path.dirname(output_cfg_path), exist_ok=True)
    with open(output_cfg_path, 'w', encoding='utf-8') as fp:
        fp.write('\n'.join(lines) + '\n')
    return output_cfg_path

def clean_jar_metadata(in_jar, out_jar):
    with zipfile.ZipFile(in_jar, 'r') as zin:
        with zipfile.ZipFile(out_jar, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
            for item in zin.infolist():
                fn = item.filename
                if fn.startswith('META-INF/maven/') or fn.endswith('.pom') or (fn.endswith('.properties') and 'maven' in fn):
                    continue
                zout.writestr(item, zin.read(fn))

if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'inspect':
        target = sys.argv[2]
        info = inspect_plugin(target)
        print(json.dumps(info, indent=2))
    elif cmd == 'generate_config':
        target = sys.argv[2]
        dict_path = sys.argv[3].replace('\\', '/')
        repackage = sys.argv[4]
        out_cfg = sys.argv[5]
        info = inspect_plugin(target)
        generate_proguard_config(info, dict_path, repackage, out_cfg)
        print(json.dumps(info))
    elif cmd == 'clean_metadata':
        clean_jar_metadata(sys.argv[2], sys.argv[3])
