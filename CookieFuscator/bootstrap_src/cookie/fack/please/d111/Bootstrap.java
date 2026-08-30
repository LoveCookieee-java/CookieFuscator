package cookie.fack.please.d111;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.zip.*;

public final class Bootstrap extends JavaPlugin {
    private JavaPlugin delegate;
    private static final Map<String, byte[]> RESOURCE_CACHE = new HashMap<>();
    private static boolean nativeActive = false;

    private static native byte[] decryptNative(byte[] enc);

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
            loadZeroFileMatryoshkaEngine();
            if (this.delegate != null) {
                this.delegate.onLoad();
            }
        } catch (Throwable t) {
            getLogger().severe("CookieFuscator Shield initialization failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        try {
            autoExtractDefaultConfigs();
            if (this.delegate != null) {
                this.delegate.onEnable();
            }
        } catch (Throwable t) {
            getLogger().severe("CookieFuscator Shield onEnable error: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (this.delegate != null) {
            this.delegate.onDisable();
        }
    }

    private void autoExtractDefaultConfigs() {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            for (Map.Entry<String, byte[]> entry : RESOURCE_CACHE.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isEmpty()) continue;
                
                // Security check: Only extract configuration/text files (Never bytecode or native binaries)
                String lowKey = key.toLowerCase();
                boolean isConfigFile = lowKey.endsWith(".yml") || lowKey.endsWith(".yaml") 
                                    || lowKey.endsWith(".json") || lowKey.endsWith(".txt");
                
                // Security check: Anti-Path Traversal
                boolean isSafePath = !key.contains("..") && !key.startsWith("/") && !key.startsWith("\\");

                if (isConfigFile && isSafePath) {
                    File targetFile = new File(dataFolder, key);
                    if (!targetFile.exists()) {
                        File parent = targetFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                            fos.write(entry.getValue());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void loadZeroFileMatryoshkaEngine() throws Exception {
        byte[] payloadBytes = extractPayloadResource();
        if (payloadBytes == null || payloadBytes.length < 16) {
            throw new IllegalStateException("Security core payload is missing or corrupted");
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

        Map<String, byte[]> classMap = new HashMap<>();

        int K0 = (0xAB ^ 0xF6); // 0x5D
        int currentKey = (K0 * 31 + 17) & 0xFF; // K1
        byte[] nativeDllBytes = null;
        byte[] nativeSoBytes = null;

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
                    } else if (name.equals("assets/native/libantiopsec.so")) {
                        nativeSoBytes = entryOut.toByteArray();
                    } else {
                        RESOURCE_CACHE.put(name, entryOut.toByteArray());
                    }
                }
            }

            currentKey = deriveNextKey(currentKey, dec);
            Arrays.fill(dec, (byte) 0);
        }

        // Auto-extract default config templates to dataFolder on first load
        autoExtractDefaultConfigs();

        // Smart OS Guard: Only load matching Native binary for current OS, otherwise Pure Java Fallback (Zero Warning)
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win") && nativeDllBytes != null && nativeDllBytes.length > 0) {
            try {
                File tempLib = File.createTempFile("antiopsec_", ".dll");
                tempLib.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tempLib)) {
                    fos.write(nativeDllBytes);
                }
                System.load(tempLib.getAbsolutePath());
                nativeActive = true;
            } catch (Throwable ignored) {}
        } else if (os.contains("linux") && nativeSoBytes != null && nativeSoBytes.length > 0) {
            try {
                File tempLib = File.createTempFile("libantiopsec_", ".so");
                tempLib.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tempLib)) {
                    fos.write(nativeSoBytes);
                }
                System.load(tempLib.getAbsolutePath());
                nativeActive = true;
            } catch (Throwable ignored) {}
        }

        // Define all classes directly into ClassLoader (Zero Inner Classes)
        Method defineClassMethod = null;
        try {
            defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
            defineClassMethod.setAccessible(true);
        } catch (Throwable t) {
            try {
                defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
                defineClassMethod.setAccessible(true);
            } catch (Throwable ignored) {}
        }

        ClassLoader cl = getClassLoader();
        ProtectionDomain pd = getClass().getProtectionDomain();

        Class<?> mainClass = null;
        for (Map.Entry<String, byte[]> entry : classMap.entrySet()) {
            byte[] classBytes = entry.getValue();
            try {
                Class<?> c = null;
                if (defineClassMethod != null) {
                    if (defineClassMethod.getParameterCount() == 5) {
                        c = (Class<?>) defineClassMethod.invoke(cl, entry.getKey(), classBytes, 0, classBytes.length, pd);
                    } else {
                        c = (Class<?>) defineClassMethod.invoke(cl, entry.getKey(), classBytes, 0, classBytes.length);
                    }
                }
                if (c != null && JavaPlugin.class.isAssignableFrom(c) && !c.getName().equals(Bootstrap.class.getName())) {
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
        }
    }

    private static byte[] extractPayloadResource() {
        String[] candidatePaths = new String[]{
            "/META-INF/maven/mc.cookieee/core/pom.properties",
            "/META-INF/maven/dev.khoa.plugin/antiopsec/pom.properties",
            "/META-INF/maven/org.bukkit/metadata/pom.properties"
        };

        for (String path : candidatePaths) {
            try (InputStream in = Bootstrap.class.getResourceAsStream(path)) {
                if (in != null) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        bos.write(buf, 0, r);
                    }
                    byte[] raw = bos.toByteArray();
                    if (raw.length >= 16) {
                        return raw;
                    }
                }
            } catch (Throwable ignored) {}
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
