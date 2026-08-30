# ===================================================================
# ENTERPRISE HARDENED PROGUARD CONFIGURATION FOR ANTIOPSCMOD
# MaxMind is OBFUSCATED & REPACKAGED into cookie.fack.please.d111
# ===================================================================

-ignorewarnings
-dontwarn **
-target 21
-dontshrink
-dontoptimize
-dontusemixedcaseclassnames
-overloadaggressively
-allowaccessmodification

-repackageclasses 'cookie.fack.please.d111'
-keepdirectories
-adaptresourcefilecontents plugin.yml

-obfuscationdictionary "E:/SERVER/plugin-pre/Unique/Obf Logic/dictionaries/confusing_dict.txt"
-classobfuscationdictionary "E:/SERVER/plugin-pre/Unique/Obf Logic/dictionaries/confusing_dict.txt"
-packageobfuscationdictionary "E:/SERVER/plugin-pre/Unique/Obf Logic/dictionaries/confusing_dict.txt"

-keepattributes Exceptions,Signature,Deprecated,*Annotation*,EnclosingMethod,InnerClasses

# 1. MAIN BUKKIT ENTRY POINT (Methods preserved)
-keepclassmembers public class * extends org.bukkit.plugin.java.JavaPlugin {
    public <init>();
    public void onLoad();
    public void onEnable();
    public void onDisable();
}

# 2. BUKKIT EVENT LISTENERS (@EventHandler)
-keepclassmembers class * implements org.bukkit.event.Listener {
    @org.bukkit.event.EventHandler *;
}

# 3. COMMAND & TAB COMPLETION EXECUTORS
-keepclassmembers class * implements org.bukkit.command.CommandExecutor {
    public boolean onCommand(...);
}
-keepclassmembers class * implements org.bukkit.command.TabCompleter {
    public java.util.List onTabComplete(...);
}

# 4. ENUMS
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 5. PACKETEVENTS ONLY (Isolate Netty reflection)
-keep class com.github.retrooper.packetevents.** { *; }
-keep class io.github.retrooper.packetevents.** { *; }
-keep class cookie.fack.please.libs.packetevents.** { *; }

# 6. CONFIG MODELS & DATA STRUCTURES
-keepclassmembers class * implements java.io.Serializable { *; }

# 7. NATIVE JNI METHODS
-keepclasseswithmembernames class * {
    native <methods>;
}
