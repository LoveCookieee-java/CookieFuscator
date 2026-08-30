package dev.khoa.plugin.antiopsec.security;

import dev.khoa.plugin.antiopsec.native.NativeBridge;
import java.util.Arrays;

public final class EphemeralSecrets {

    public interface SecretConsumer<T> {
        T accept(byte[] plainBytes) throws Exception;
    }

    public static <T> T useDecrypted(byte[] encryptedData, SecretConsumer<T> consumer) {
        byte[] decrypted = null;
        try {
            decrypted = NativeBridge.decryptSignature(encryptedData);
            if (decrypted == null) {
                return null;
            }
            return consumer.accept(decrypted);
        } catch (Exception e) {
            return null;
        } finally {
            if (decrypted != null) {
                Arrays.fill(decrypted, (byte) 0); // Zeroize immediately to prevent heap dumps
            }
        }
    }

    private EphemeralSecrets() {}
}
