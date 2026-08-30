package dev.khoa.plugin.antiopsec.native;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public final class NativeLoader {

    public static synchronized void loadNativeLibrary() {
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        String libName;
        if (os.contains("win")) {
            libName = "antiopsec_x64.dll";
        } else {
            libName = "libantiopsec_x64.so";
        }

        try (InputStream in = NativeLoader.class.getResourceAsStream("/native/" + libName)) {
            if (in == null) {
                System.loadLibrary("antiopsec_x64");
                return;
            }

            File tempFile = File.createTempFile("antiopsec_native_", libName.substring(libName.lastIndexOf('.')));
            tempFile.deleteOnExit();

            try (OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            System.load(tempFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load AntiOpsec native shield library: " + e.getMessage(), e);
        }
    }

    private NativeLoader() {}
}
