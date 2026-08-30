package cookie.fack.please.d111;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

public final class Bootstrap extends JavaPlugin {
    private JavaPlugin delegate;
    private static final Map<String, byte[]> RESOURCE_CACHE = new HashMap<>();
    private static boolean nativeActive = false;

    private static native byte[] decryptNative(byte[] enc);

    public static class SecurityClassLoader extends ClassLoader {
        private final Map<String, byte[]> resources;

        public SecurityClassLoader(ClassLoader parent, Map<String, byte[]> resources) {
            super(parent);
            this.resources = resources;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (name != null) {
                String key = name.startsWith("/") ? name.substring(1) : name;
                byte[] data = this.resources.get(key);
                if (data != null) {
                    return new ByteArrayInputStream(data);
                }
            }
            return super.getResourceAsStream(name);
        }

        public Class<?> define(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    @Override
    public InputStream getResource(String filename) {
        if (filename != null) {
            String key = filename.startsWith("/") ? filename.substring(1) : filename;
            byte[] data = RESOURCE_CACHE.get(key);
            if (data != null) {
                return new ByteArrayInputStream(data);
            }
        }
        return super.getResource(filename);
    }

    @Override
    public void onLoad() {
        try {
            loadZeroFileEngine();
            if (this.delegate != null) {
                this.delegate.onLoad();
            }
        } catch (Throwable t) {
            getLogger().severe("CookieFuscator Zero-File Shield initialization failed: " + t.getMessage());
        }
    }

    @Override
    public void onEnable() {
        if (this.delegate != null) {
            this.delegate.onEnable();
        }
    }

    @Override
    public void onDisable() {
        if (this.delegate != null) {
            this.delegate.onDisable();
        }
    }

    private void loadZeroFileEngine() throws Exception {
        byte[] payloadBytes = extractPayloadFromClass();
        if (payloadBytes == null || payloadBytes.length < 16) {
            throw new IllegalStateException("Missing security payload");
        }

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payloadBytes));
        int len1 = dis.readInt();
        int len2 = dis.readInt();
        int len3 = dis.readInt();
        int len4 = dis.readInt();

        byte[][] encShards = new byte[4][];
        encShards[0] = new byte[len1]; dis.readFully(encShards[0]);
        encShards[1] = new byte[len2]; dis.readFully(encShards[1]);
        encShards[2] = new byte[len3]; dis.readFully(encShards[2]);
        encShards[3] = new byte[len4]; dis.readFully(encShards[3]);

        SecurityClassLoader secLoader = new SecurityClassLoader(getClassLoader(), RESOURCE_CACHE);
        Map<String, byte[]> classMap = new HashMap<>();

        int K0 = (0xAB ^ 0xF6); // 0x5D
        int currentKey = (K0 * 31 + 17) & 0xFF; // K1
        byte[] nativeDllBytes = null;

        for (int layer = 0; layer < 4; layer++) {
            byte[] enc = encShards[layer];
            byte[] dec = decryptBuffer(enc, currentKey);

            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(dec))) {
                ZipEntry entry;
                byte[] buf = new byte[8192];
                int r;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
                    while ((r = zis.read(buf)) != -1) entryOut.write(buf, 0, r);
                    String name = entry.getName();
                    if (name.endsWith(".class")) {
                        String className = name.replace('/', '.').substring(0, name.length() - 6);
                        classMap.put(className, entryOut.toByteArray());
                    } else if (name.equals("assets/native/antiopsec_x64.dll")) {
                        nativeDllBytes = entryOut.toByteArray();
                    } else {
                        RESOURCE_CACHE.put(name, entryOut.toByteArray());
                    }
                }
            }

            currentKey = deriveNextKey(currentKey, dec);
            Arrays.fill(dec, (byte) 0);
        }

        // Try Loading Native C++ Sentinel DLL if present in Shard 4
        if (nativeDllBytes != null && nativeDllBytes.length > 0) {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                String ext = os.contains("win") ? ".dll" : (os.contains("mac") ? ".dylib" : ".so");
                File tempLib = File.createTempFile("libantiopsec_", ext);
                tempLib.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tempLib)) {
                    fos.write(nativeDllBytes);
                }
                System.load(tempLib.getAbsolutePath());
                nativeActive = true;
            } catch (Throwable ignored) {}
        }

        Class<?> mainClass = null;
        for (Map.Entry<String, byte[]> entry : classMap.entrySet()) {
            byte[] classBytes = entry.getValue();
            try {
                Class<?> c = secLoader.define(entry.getKey(), classBytes);
                if (JavaPlugin.class.isAssignableFrom(c) && !c.getName().equals(Bootstrap.class.getName())) {
                    mainClass = c;
                }
            } catch (Throwable ignored) {}
        }

        if (mainClass != null) {
            try {
                Field theUnsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                Object unsafeObj = theUnsafe.get(null);
                Method allocateInstance = unsafeObj.getClass().getMethod("allocateInstance", Class.class);
                this.delegate = (JavaPlugin) allocateInstance.invoke(unsafeObj, mainClass);
            } catch (Throwable t) {
                Constructor<?> ctor = mainClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                this.delegate = (JavaPlugin) ctor.newInstance();
            }

            Class<?> jpClass = JavaPlugin.class;
            while (jpClass != null && jpClass != Object.class) {
                for (Field f : jpClass.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    f.set(this.delegate, f.get(this));
                }
                jpClass = jpClass.getSuperclass();
            }

            try {
                Field clField = JavaPlugin.class.getDeclaredField("classLoader");
                clField.setAccessible(true);
                clField.set(this.delegate, secLoader);
            } catch (Throwable ignored) {}
        }
    }

    private static byte[] extractPayloadFromClass() {
        try (InputStream in = Bootstrap.class.getResourceAsStream("/cookie/fack/please/d111/Bootstrap.class")) {
            if (in == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
            byte[] b = bos.toByteArray();

            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
            int magic = dis.readInt();
            int minor = dis.readUnsignedShort();
            int major = dis.readUnsignedShort();
            int cpCount = dis.readUnsignedShort();

            String[] utf8Strings = new String[cpCount];
            for (int i = 1; i < cpCount; i++) {
                int tag = dis.readUnsignedByte();
                if (tag == 1) {
                    utf8Strings[i] = dis.readUTF();
                } else if (tag == 3 || tag == 4) {
                    dis.skipBytes(4);
                } else if (tag == 5 || tag == 6) {
                    dis.skipBytes(8);
                    i++;
                } else if (tag == 7 || tag == 8 || tag == 16 || tag == 19 || tag == 20) {
                    dis.skipBytes(2);
                } else if (tag == 9 || tag == 10 || tag == 11 || tag == 12 || tag == 18) {
                    dis.skipBytes(4);
                } else if (tag == 15) {
                    dis.skipBytes(3);
                }
            }

            int accessFlags = dis.readUnsignedShort();
            int thisClass = dis.readUnsignedShort();
            int superClass = dis.readUnsignedShort();
            int interfacesCount = dis.readUnsignedShort();
            dis.skipBytes(interfacesCount * 2);

            int fieldsCount = dis.readUnsignedShort();
            for (int i = 0; i < fieldsCount; i++) {
                dis.skipBytes(6);
                int fAttrs = dis.readUnsignedShort();
                for (int j = 0; j < fAttrs; j++) {
                    dis.skipBytes(2);
                    int aLen = dis.readInt();
                    dis.skipBytes(aLen);
                }
            }

            int methodsCount = dis.readUnsignedShort();
            for (int i = 0; i < methodsCount; i++) {
                dis.skipBytes(6);
                int mAttrs = dis.readUnsignedShort();
                for (int j = 0; j < mAttrs; j++) {
                    dis.skipBytes(2);
                    int aLen = dis.readInt();
                    dis.skipBytes(aLen);
                }
            }

            int attrsCount = dis.readUnsignedShort();
            for (int i = 0; i < attrsCount; i++) {
                int attrNameIdx = dis.readUnsignedShort();
                int attrLen = dis.readInt();
                String attrName = utf8Strings[attrNameIdx];
                if ("SourceDebugExtension".equals(attrName)) {
                    byte[] payload = new byte[attrLen];
                    dis.readFully(payload);
                    return payload;
                } else {
                    dis.skipBytes(attrLen);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static byte[] decryptBuffer(byte[] enc, int keySeed) {
        byte[] dec = new byte[enc.length];
        int rk = keySeed & 0xFF;
        for (int i = 0; i < enc.length; i++) {
            int e = enc[i] & 0xFF;
            int val = (e ^ rk) & 0xFF;
            int p = (val - (i & 0x0F)) & 0xFF;
            dec[i] = (byte) p;
            rk = ((rk * 37) ^ p) & 0xFF;
        }
        return dec;
    }

    private static int deriveNextKey(int prevKey, byte[] payload) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(payload);
        md.update((byte) (prevKey >> 24));
        md.update((byte) (prevKey >> 16));
        md.update((byte) (prevKey >> 8));
        md.update((byte) prevKey);
        byte[] h = md.digest();
        int key = 0;
        for (int i = 0; i < 4; i++) {
            key = (key << 8) | (h[i] & 0xFF);
        }
        return (key ^ 0x5D) & 0xFF;
    }
}
