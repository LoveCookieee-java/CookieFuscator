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

    @Override
    public void onLoad() {
        try {
            loadEngine();
            if (this.delegate != null) {
                this.delegate.onLoad();
            }
        } catch (Throwable t) {
            getLogger().severe("OpSec Security Shield initialization failed: " + t.getMessage());
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
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    ByteArrayOutputStream classOut = new ByteArrayOutputStream();
                    while ((r = zis.read(buf)) != -1) classOut.write(buf, 0, r);
                    String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                    classMap.put(className, classOut.toByteArray());
                }
            }
        }
        Arrays.fill(dec, (byte) 0);

        Method defineMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        defineMethod.setAccessible(true);
        ClassLoader loader = getClassLoader();

        Class<?> mainClass = null;
        for (Map.Entry<String, byte[]> entry : classMap.entrySet()) {
            byte[] classBytes = entry.getValue();
            try {
                Class<?> c = (Class<?>) defineMethod.invoke(loader, entry.getKey(), classBytes, 0, classBytes.length);
                if (JavaPlugin.class.isAssignableFrom(c) && !c.getName().equals(Bootstrap.class.getName())) {
                    mainClass = c;
                }
            } catch (Throwable ignored) {}
        }

        if (mainClass != null) {
            Constructor<?> ctor = mainClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            this.delegate = (JavaPlugin) ctor.newInstance();

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
}
