package cookie.fack.please.d111;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class Bootstrap extends JavaPlugin {
    private JavaPlugin delegate;
    private static final Map<String, byte[]> RESOURCE_CACHE = new HashMap<>();

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
            loadEngine();
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

    private void loadEngine() throws Exception {
        byte[] enc;
        try (InputStream in = Bootstrap.class.getResourceAsStream("/assets/engine.dat")) {
            if (in == null) {
                throw new IllegalStateException("Missing security payload");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) baos.write(buf, 0, r);
            enc = baos.toByteArray();
        }

        byte[] dec = new byte[enc.length];
        int roundKey = 0x5D;
        for (int i = 0; i < enc.length; i++) {
            int e = enc[i] & 0xFF;
            int val = (e ^ roundKey) & 0xFF;
            int p = (val - (i & 0x0F)) & 0xFF;
            dec[i] = (byte) p;
            roundKey = ((roundKey * 37) ^ p) & 0xFF;
        }

        Map<String, byte[]> classMap = new HashMap<>();
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
                } else {
                    RESOURCE_CACHE.put(name, entryOut.toByteArray());
                }
            }
        }
        Arrays.fill(dec, (byte) 0);

        SecurityClassLoader secLoader = new SecurityClassLoader(getClassLoader(), RESOURCE_CACHE);

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
}
