# Project Memory & Master Security Directives (CookieFuscator)

1. **Zero Suspicious Files**:
   - Không bao giờ để lộ file `.dat`, `.bin` hoặc `.yml` thô ở thư mục gốc của file JAR.
   - Luôn đóng gói qua `ResourcePacker.py` giấu vào `icon.png` (metadata `Comment`/`Author` chuẩn ISO PNG).

2. **Dynamic JNI Registration & Stripped C++**:
   - Mọi module Native C++ phải sử dụng `RegisterNatives` trong `JNI_OnLoad` thay vì export hàm tường minh (`Java_pkg_class_method`).
   - Xóa bỏ 100% exported symbol names để chống phân tích qua IDA Pro / Ghidra.

3. **Dynamic Key Anchoring & Math Invariants**:
   - Khóa giải mã chuỗi luôn được tính toán động tại runtime từ hash ngữ cảnh class, không dùng hằng số tĩnh literal.
   - Chèn định lý số học bất biến x*(x+1)%2==0 để bẻ gãy phân tích tĩnh AST.

4. **Multi-Platform Support & Graceful Fallback**:
   - Hỗ trợ song song Windows x64 (`antiopsec_x64.dll`), Linux x64 (`libantiopsec.so`).
   - Luôn có cơ chế Fallback giải mã RAM Pure Java cho các môi trường server khác (Zero-Crash).
