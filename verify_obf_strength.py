import zipfile
import re
import sys
import os

def analyze_jar(jar_path):
    print("=" * 60)
    print(f"[*] Analyzing Obfuscation Strength for: {os.path.basename(jar_path)}")
    print("=" * 60)

    if not os.path.exists(jar_path):
        print(f"[-] Error: File not found -> {jar_path}")
        return False

    with zipfile.ZipFile(jar_path, 'r') as z:
        entries = z.namelist()
        class_files = [e for e in entries if e.endswith('.class')]
        
        print(f"[+] Total Class Files: {len(class_files)}")
        
        # Check dictionary token usage
        d_pattern = re.compile(r'(d[l10o]{3,})')
        confusing_names = [e for e in class_files if d_pattern.search(e)]
        
        print(f"[+] Obfuscated Class Files matching [dlll/d111/d000]: {len(confusing_names)} ({len(confusing_names)/max(1, len(class_files))*100:.1f}%)")
        
        # Inspect constant pool for sensitive leaked keywords
        sensitive_keywords = [
            b"Meteor", b"Aristois", b"LiquidBounce", b"OpSec", 
            b"translation.test", b"GhostInventory"
        ]
        
        leaked_keywords = {}
        for cf in class_files:
            data = z.read(cf)
            for kw in sensitive_keywords:
                if kw in data:
                    leaked_keywords[kw.decode()] = leaked_keywords.get(kw.decode(), 0) + 1

        print("\n--- Leaked Plaintext Sensitive Keyword Report ---")
        if leaked_keywords:
            for kw, count in leaked_keywords.items():
                print(f"[!] Warning: Keyword '{kw}' found in {count} class files (Requires String Encryption)")
        else:
            print("[v] PERFECT: 0 sensitive client signatures leaked in constant pools!")

        # Check native library bundle
        native_libs = [e for e in entries if e.startswith('native/') or e.endswith('.dll') or e.endswith('.so')]
        print(f"\n--- Bundled Native Libraries: {len(native_libs)} ---")
        for lib in native_libs:
            print(f"  -> {lib}")

    print("\n[v] Verification Complete!")
    return True

if __name__ == '__main__':
    if len(sys.argv) > 1:
        analyze_jar(sys.argv[1])
    else:
        print("Usage: python verify_obf_strength.py <path_to_jar>")
