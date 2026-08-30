# Project Memory & Master Security Directives (CookieFuscator)

1. **Zero-File Invisibility (Zero Suspicious Files)**:
   - Không để lộ bất kỳ file `.png`, `.dat`, `.bin`, `.yml` hay thư mục `assets/` nào trong danh mục tệp ZIP của JAR.
   - Toàn bộ 4 Shards mã hóa và Native DLL được đóng gói vào vùng **ZIP EOCD Overlay (Polyglot Append)** nằm sau bản ghi End of Central Directory.

2. **Dynamic JNI Registration & Stripped C++**:
   - Mọi module Native C++ phải sử dụng `RegisterNatives` trong `JNI_OnLoad` thay vì export hàm tường minh (`Java_pkg_class_method`).
   - Xóa bỏ 100% exported symbol names để chống phân tích qua IDA Pro / Ghidra.

3. **Matryoshka 4-Layer Cascading Key Chaining**:
   - Khóa giải mã tầng sau $K_{i+1}$ được dẫn xuất từ HMAC-SHA256 của tầng $i$, đảm bảo tính chất Forward-Security và Anti-Bypass.

4. **Multi-Platform Support & Graceful Fallback**:
   - Hỗ trợ song song Windows x64 (`antiopsec_x64.dll`), Linux x64 (`libantiopsec.so`).
   - Luôn có cơ chế Fallback giải mã RAM Pure Java cho các môi trường server khác (Zero-Crash).
