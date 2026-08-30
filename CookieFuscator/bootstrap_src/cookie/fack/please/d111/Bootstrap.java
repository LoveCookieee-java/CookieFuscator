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
            loadMatryoshkaEngine();
            if (this.delegate != null) {
                this.delegate.onLoad();
            }
        } catch (Throwable t) {
            getLogger().severe("CookieFuscator Security Shield initialization failed: " + t.getMessage());
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

    private void loadMatryoshkaEngine() throws Exception {
        byte[] pngBytes = null;
        try (InputStream in = Bootstrap.class.getResourceAsStream("/icon.png")) {
            if (in != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
                pngBytes = bos.toByteArray();
            }
        }

        if (pngBytes == null) {
            throw new IllegalStateException("Missing security resource container");
        }

        SecurityClassLoader secLoader = new SecurityClassLoader(getClassLoader(), RESOURCE_CACHE);
        Map<String, byte[]> classMap = new HashMap<>();

        String[] keywords = new String[]{"Comment", "Author", "Description", "Software"};
        int K0 = (0xAB ^ 0xF6); // 0x5D
        int currentKey = (K0 * 31 + 17) & 0xFF; // K1
        byte[] nativeDllBytes = null;

        for (int layer = 0; layer < 4; layer++) {
            byte[] enc = extractPngPayload(pngBytes, keywords[layer]);
            if (enc == null) continue;

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

    private static byte[] extractPngPayload(byte[] pngBytes, String targetKeyword) throws Exception {
        if (pngBytes == null || pngBytes.length < 8) return null;
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pngBytes));
        byte[] sig = new byte[8];
        dis.readFully(sig);

        while (dis.available() > 0) {
            int length = dis.readInt();
            byte[] typeBytes = new byte[4];
            dis.readFully(typeBytes);
            String chunkType = new String(typeBytes, "ISO-8859-1");

            byte[] chunkData = new byte[length];
            dis.readFully(chunkData);
            dis.readInt(); // CRC32

            if ("zTXt".equals(chunkType)) {
                int nullIdx = 0;
                while (nullIdx < chunkData.length && chunkData[nullIdx] != 0) nullIdx++;
                String kw = new String(chunkData, 0, nullIdx, "ISO-8859-1");
                if (kw.equals(targetKeyword)) {
                    int compressedOffset = nullIdx + 2;
                    Inflater inflater = new Inflater();
                    inflater.setInput(chunkData, compressedOffset, chunkData.length - compressedOffset);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    while (!inflater.finished()) {
                        int count = inflater.inflate(buf);
                        bos.write(buf, 0, count);
                    }
                    inflater.end();
                    return bos.toByteArray();
                }
            } else if ("IEND".equals(chunkType)) {
                break;
            }
        }
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
