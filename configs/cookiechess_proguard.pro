# ===================================================================
# ENTERPRISE HARDENED PROGUARD CONFIGURATION FOR COOKIECHESS
# Repackaged into cookie.fack.please.d111 with 31,394 optical homoglyphs
# ===================================================================

-ignorewarnings
-dontwarn **
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

# 1. MAIN BUKKIT ENTRY POINT
-keepclassmembers public class * extends org.bukkit.plugin.java.JavaPlugin {
    public <init>();
    public void onLoad();
    public void onEnable();
    public void onDisable();
}

# 2. BUKKIT EVENT LISTENERS
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

# 4. PLACEHOLDER EXPANSIONS
-keepclassmembers class * extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
    public <init>(...);
    public boolean persist();
    public boolean canRegister();
    public java.lang.String getAuthor();
    public java.lang.String getIdentifier();
    public java.lang.String getVersion();
    public java.lang.String onPlaceholderRequest(...);
    public java.lang.String onRequest(...);
}

# 5. ENUMS
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 6. HIKARICP & SLF4J
-keep class mcp.cookieee.libs.hikaricp.** { *; }
-keep class com.zaxxer.hikari.** { *; }
-keep class org.slf4j.** { *; }

# 7. SERIALIZABLE MODELS
-keepclassmembers class * implements java.io.Serializable { *; }
