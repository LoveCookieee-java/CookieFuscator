# Project Memory & Master Security Directives (CookieFuscator)

1. **Zero Suspicious Files**:
   - Không bao giờ để lộ file `.dat`, `.bin` hoặc `.yml` thô ở thư mục gốc của file JAR.
   - Luôn đóng gói qua `ResourcePacker.py` giấu vào `icon.png` (metadata `Comment`/`Author`/`Description`/`Software` chuẩn ISO PNG).

2. **Dynamic JNI Registration & Stripped C++**:
   - Mọi module Native C++ phải sử dụng `RegisterNatives` trong `JNI_OnLoad` thay vì export hàm tường minh (`Java_pkg_class_method`).
   - Xóa bỏ 100% exported symbol names để chống phân tích qua IDA Pro / Ghidra.

3. **Matryoshka 4-Layer Steganography & Cascading Key Chaining**:
   - Phân tách bytecode thành 4 Shards cân bằng lồng nhau theo chuẩn ISO PNG Chunks.
   - Khóa giải mã tầng sau $K_{i+1}$ được dẫn xuất từ HMAC-SHA256 của tầng $i$, đảm bảo tính chất Forward-Security và Anti-Bypass.

4. **Multi-Platform Support & Graceful Fallback**:
   - Hỗ trợ song song Windows x64 (`antiopsec_x64.dll`), Linux x64 (`libantiopsec.so`).
   - Luôn có cơ chế Fallback giải mã RAM Pure Java cho các môi trường server khác (Zero-Crash).
