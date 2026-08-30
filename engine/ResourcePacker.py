import os
import sys
import struct
import zlib
import io
import gzip
import zipfile

def calculate_crc32(data: bytes) -> int:
    return zlib.crc32(data) & 0xFFFFFFFF

def encrypt_payload(data: bytes, key_seed: int = 0x5D) -> bytes:
    out = bytearray(len(data))
    rk = key_seed
    for i in range(len(data)):
        p = data[i]
        out[i] = ((p + (i & 0x0F)) & 0xFF) ^ rk
        rk = ((rk * 37) ^ p) & 0xFF
    return bytes(out)

def decrypt_payload(data: bytes, key_seed: int = 0x5D) -> bytes:
    out = bytearray(len(data))
    rk = key_seed
    for i in range(len(data)):
        e = data[i] & 0xFF
        val = (e ^ rk) & 0xFF
        p = (val - (i & 0x0F)) & 0xFF
        out[i] = p
        rk = ((rk * 37) ^ p) & 0xFF
    return bytes(out)

def inject_png_ztxt(png_bytes: bytes, keyword: str, payload: bytes) -> bytes:
    """Injects an encrypted zTXt metadata chunk into any standard PNG image."""
    if png_bytes[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError("Invalid PNG header")

    compressed_payload = zlib.compress(payload, level=9)
    chunk_data = keyword.encode('latin-1') + b'\x00\x00' + compressed_payload
    chunk_type = b'zTXt'
    length = len(chunk_data)
    crc = calculate_crc32(chunk_type + chunk_data)

    ztxt_chunk = struct.pack(">I", length) + chunk_type + chunk_data + struct.pack(">I", crc)

    insert_pos = len(png_bytes) - 12
    return png_bytes[:insert_pos] + ztxt_chunk + png_bytes[insert_pos:]

def extract_png_ztxt(png_bytes: bytes, target_keyword: str) -> bytes:
    """Extracts and decompresses zTXt metadata chunk from a PNG image."""
    if png_bytes[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError("Invalid PNG header")

    offset = 8
    while offset < len(png_bytes):
        length = struct.unpack(">I", png_bytes[offset:offset+4])[0]
        chunk_type = png_bytes[offset+4:offset+8]
        chunk_data = png_bytes[offset+8:offset+8+length]
        offset += 12 + length

        if chunk_type == b'zTXt':
            null_idx = chunk_data.find(b'\x00')
            if null_idx != -1:
                kw = chunk_data[:null_idx].decode('latin-1', errors='ignore')
                if kw == target_keyword:
                    compressed_stream = chunk_data[null_idx+2:]
                    return zlib.decompress(compressed_stream)
        elif chunk_type == b'IEND':
            break
    return None

def generate_default_png_icon() -> bytes:
    """Generates a standard, valid 16x16 RGBA PNG icon if no custom image is supplied."""
    width, height = 16, 16
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0)
        for x in range(width):
            if (x in [0, 15] and y in [0, 15]) or (x in [1, 14] and y in [0, 15]):
                raw_data.extend([0, 0, 0, 0])
            else:
                raw_data.extend([217, 119, 6, 255])

    compressed_idat = zlib.compress(bytes(raw_data), level=9)
    png_buf = io.BytesIO()
    png_buf.write(b'\x89PNG\r\n\x1a\n')

    # IHDR
    ihdr_data = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png_buf.write(struct.pack(">I", len(ihdr_data)))
    png_buf.write(b'IHDR')
    png_buf.write(ihdr_data)
    png_buf.write(struct.pack(">I", calculate_crc32(b'IHDR' + ihdr_data)))

    # IDAT
    png_buf.write(struct.pack(">I", len(compressed_idat)))
    png_buf.write(b'IDAT')
    png_buf.write(compressed_idat)
    png_buf.write(struct.pack(">I", calculate_crc32(b'IDAT' + compressed_idat)))

    # IEND
    png_buf.write(struct.pack(">I", 0))
    png_buf.write(b'IEND')
    png_buf.write(struct.pack(">I", calculate_crc32(b'IEND')))

    return png_buf.getvalue()

def load_image_as_png(custom_icon_path: str = None) -> bytes:
    """Loads any image (.jpg, .jpeg, .png, .webp) and returns valid PNG bytes."""
    if custom_icon_path and os.path.exists(custom_icon_path):
        try:
            from PIL import Image
            im = Image.open(custom_icon_path)
            if im.mode != 'RGBA':
                im = im.convert('RGBA')
            buf = io.BytesIO()
            im.save(buf, format='PNG')
            return buf.getvalue()
        except Exception as e:
            with open(custom_icon_path, 'rb') as cf:
                raw = cf.read()
                if raw[:8] == b'\x89PNG\r\n\x1a\n':
                    return raw
    return generate_default_png_icon()

def pack_jar_with_steganography(input_obf_jar: str, output_final_jar: str, bootstrap_cls_bytes: bytes, custom_icon_path: str = None, native_dll_path: str = None):
    """Packs all classes, YAMLs, and optional Native DLL into an authentic PNG image (icon.png)."""
    with zipfile.ZipFile(input_obf_jar, 'r') as zin:
        payload_buf = io.BytesIO()
        with zipfile.ZipFile(payload_buf, 'w', compression=zipfile.ZIP_DEFLATED) as pz:
            for item in zin.infolist():
                fn = item.filename
                if fn.startswith('META-INF/') or fn in ['plugin.yml', 'bungee.yml', 'velocity-plugin.json']:
                    continue
                if not fn.endswith('/'):
                    pz.writestr(fn, zin.read(fn))

        raw_payload = payload_buf.getvalue()
        encrypted_payload = encrypt_payload(raw_payload)

        # 1. Base PNG Image
        base_png = load_image_as_png(custom_icon_path)
        
        # 2. Inject Bytecode & Resources
        stego_png = inject_png_ztxt(base_png, "CookieEnginePayload", encrypted_payload)
        
        # 3. Inject Native C++ Sentinel DLL if provided
        if native_dll_path and os.path.exists(native_dll_path):
            with open(native_dll_path, 'rb') as df:
                dll_bytes = df.read()
            stego_png = inject_png_ztxt(stego_png, "CookieNativeLibrary", dll_bytes)

        with zipfile.ZipFile(output_final_jar, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
            for item in zin.infolist():
                fn = item.filename
                if fn.endswith('.class') or fn.endswith('.yml') or fn.endswith('.yaml') or fn.endswith('.json') or fn.endswith('.bin') or fn.endswith('.txt') or fn.endswith('.png') or fn.endswith('.dll') or fn.endswith('.so'):
                    if fn not in ['plugin.yml', 'bungee.yml']:
                        continue
                if fn.startswith('META-INF/maven/') or fn.startswith('dev/') or fn.startswith('mc/') or fn.startswith('mcp/') or fn.startswith('org/') or fn.startswith('io/') or fn.startswith('com/') or fn.startswith('cookie/'):
                    continue

                if fn in ['plugin.yml', 'bungee.yml']:
                    p_text = zin.read(fn).decode('utf-8')
                    import re
                    new_p = re.sub(r'main:\s*.*', 'main: cookie.fack.please.d111.Bootstrap', p_text)
                    zout.writestr(item, new_p.encode('utf-8'))
                elif fn.startswith('META-INF/'):
                    zout.writestr(item, zin.read(fn))

            # Add decoy bootstrap class and root icon.png
            zout.writestr('cookie/fack/please/d111/Bootstrap.class', bootstrap_cls_bytes)
            zout.writestr('icon.png', stego_png)

if __name__ == '__main__':
    if len(sys.argv) >= 4:
        cmd = sys.argv[1]
        if cmd == "pack":
            in_jar = sys.argv[2]
            out_jar = sys.argv[3]
            boot_cls_path = sys.argv[4]
            custom_icon = sys.argv[5] if len(sys.argv) > 5 and sys.argv[5] != "NONE" else None
            native_dll = sys.argv[6] if len(sys.argv) > 6 and sys.argv[6] != "NONE" else None
            with open(boot_cls_path, 'rb') as bf:
                boot_bytes = bf.read()
            pack_jar_with_steganography(in_jar, out_jar, boot_bytes, custom_icon, native_dll)
            print("[v] Successfully packed JAR with Polyglot PNG Steganography & Native Sentinel.")
