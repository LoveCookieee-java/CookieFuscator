# 🍪 Hướng Dẫn Sử Dụng CookieFuscator (Tiếng Việt)

Chào mừng bạn đến với tài liệu hướng dẫn sử dụng **CookieFuscator** — Công cụ Obfuscate & Bảo Vệ Toàn Diện Dành Riêng Cho Plugin Minecraft & Ứng Dụng JVM.

---

## 📑 Mục Lục

1. [Giới thiệu tổng quan](#1-giới-thiệu-tổng-quan)
2. [Cài đặt & Môi trường yêu cầu](#2-cài-đặt--môi-trường-yêu-cầu)
3. [Các lệnh 1-Click thông dụng](#3-các-lệnh-1-click-thông-dụng)
4. [Hai chế độ bảo vệ (Standard vs Ultra)](#4-hai-chế-độ-bảo-vệ-standard-vs-ultra)
5. [Cơ chế tự động phân tích (Zero-Config)](#5-cơ-chế-tự-động-phân-tích-zero-config)
6. [Câu hỏi thường gặp (FAQs)](#6-câu-hỏi-thường-gặp-faqs)

---

## 1. Giới thiệu tổng quan

**CookieFuscator** được thiết kế để giải quyết bài toán đau đầu nhất của lập trình viên Minecraft:
* Obfuscate thông thường dễ làm **hỏng Event `@EventHandler`**, mất lệnh `/command`, lỗi kết nối database **HikariCP**, hoặc lỗi các plugin phụ thuộc (**PlaceholderAPI, ModelEngine, PacketEvents**).
* Viết file cấu hình ProGuard thủ công mất rất nhiều thời gian và dễ sai sót.

**CookieFuscator tự động làm toàn bộ các bước này**: Tự tìm Main Class, tự nhận diện Event / Command / API, tự tải thư viện classpath, và đóng gói file JAR thành phẩm an toàn tuyệt đối.

---

## 2. Cài đặt & Môi trường yêu cầu

- **Hệ điều hành**: Windows 10/11 hoặc Windows Server.
- **Java**: JDK 21 trở lên (Khuyến nghị Eclipse Adoptium Temurin 21).
- **Python**: Python 3.8+ (đã cài thư viện `pyyaml` qua lệnh `pip install pyyaml`).
- **Maven**: Đã cài đặt Maven nếu bạn muốn build trực tiếp từ thư mục mã nguồn.

---

## 3. Các lệnh 1-Click thông dụng

Mở PowerShell tại thư mục `Obf Logic`:

### 🔹 1. Obfuscate một Plugin từ Thư mục Mã Nguồn (Ví dụ: CookieChess)
```powershell
.\CookieFuscator.ps1 -Target "E:\SERVER\plugin-pre\Unique\cookiechess\MineChess-main"
```

### 🔹 2. Obfuscate với Chế độ Ảo Hóa Siêu Bảo Mật (Ultra Decoy Container)
```powershell
.\CookieFuscator.ps1 -Target "E:\SERVER\plugin-pre\Unique\AntiOpsecMod" -Profile Ultra
```

### 🔹 3. Obfuscate trực tiếp từ một File JAR đã biên dịch sẵn
```powershell
.\CookieFuscator.ps1 -Target "C:\Users\KHOA\Desktop\MyPlugin.jar"
```

### 🔹 4. Chỉ định đổi tên Package gốc tùy ý
```powershell
.\CookieFuscator.ps1 -Target "E:\...\MyPlugin" -Repackage "com.mycompany.security.core"
```

---

## 4. Hai chế độ bảo vệ (Standard vs Ultra)

| Tính năng | Profile: `Standard` | Profile: `Ultra` |
|:---|:---:|:---:|
| **Mục đích** | Plugin thông thường, plugin PvP yêu cầu tốc độ tối đa | Plugin thương mại, plugin độc quyền chống rò rỉ source |
| **Độ trễ (Overhead)** | **0.00% (Bằng 100% bản gốc)** | **~15ms (Chỉ giải mã 1 lần duy nhất lúc bật server)** |
| **Hiển thị trong Decompiler (JD-GUI / Recaf)** | Thấy các class bị đổi tên quang học `I111l` | **Hoàn toàn KHÔNG thấy class thật (Chỉ thấy Decoy Bootstrap)** |
| **Bảo toàn File Config YAML** | 100% Plaintext (Admin chỉnh sửa bình thường) | Hỗ trợ mã hóa YAML thành `.bin` |

---

## 5. Cơ chế tự động phân tích (Zero-Config)

Khi nhận đường dẫn target, CookieFuscator sẽ:
1. Đọc `plugin.yml` / `bungee.yml` / `velocity-plugin.json` để trích xuất `main:` class.
2. Quét toàn bộ mã nguồn để phát hiện các interface/annotation quan trọng:
   - `@EventHandler` (PlayerListener, InventoryListener,...)
   - `CommandExecutor` & `TabCompleter`
   - `PlaceholderExpansion` (PlaceholderAPI)
   - `ModelEngineAPI` & `MythicMobs`
   - `PacketEvents` & `ProtocolLib`
   - Database Connection Pool `HikariCP` & `SLF4J`
3. Tự động sinh file ProGuard rules riêng biệt và tiến hành xáo trộn bytecode với từ điển **31.394 từ khóa quang học**.
4. Xóa sạch metadata nhạy cảm của Maven và xuất file JAR vào thư mục `dist/`.

---

## 6. Câu hỏi thường gặp (FAQs)

### ❓ File JAR kết quả nằm ở đâu?
> File JAR thành phẩm đã obfuscate luôn được lưu tại thư mục: `E:\SERVER\plugin-pre\Unique\Obf Logic\dist\` với đuôi `-PROT.jar`.

### ❓ Admin server có sửa được file config.yml không?
> **Có!** Toàn bộ các file `.yml` cấu hình đều được giữ nguyên vẹn để người dùng cấu hình server bình thường.

---

<p align="center">
  <sub>Tài liệu hướng dẫn tiếng Việt • Bản quyền (c) 2026 LoveCookieee / Khoa</sub>
</p>
