package dev.khoa.plugin.antiopsec.native;

public final class NativeBridge {

    private static volatile boolean loaded = false;

    static {
        try {
            NativeLoader.loadNativeLibrary();
            loaded = true;
        } catch (Throwable t) {
            loaded = false;
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static native boolean verifyCoreIntegrity(String jarChecksum);

    public static native byte[] decryptSignature(byte[] encryptedData);

    public static native boolean validateLicense(String key, String hwid);

    private NativeBridge() {}
}
