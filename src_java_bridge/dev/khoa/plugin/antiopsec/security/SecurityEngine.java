package dev.khoa.plugin.antiopsec.security;

import dev.khoa.plugin.antiopsec.native.NativeBridge;
import java.util.logging.Logger;

/**
 * Central Security Sentinel Facade for AntiOpsec.
 * Initializes multi-tiered runtime defense layers on plugin start.
 */
public final class SecurityEngine {

    private static volatile boolean initialized = false;

    public static synchronized void bootstrap(Logger logger) {
        if (initialized) return;

        // 1. Enforce initial JVM Argument inspection (Anti-JavaAgent / Debugger)
        AntiAgentGuard.enforceJvmIntegrity();

        // 2. Launch background sentinel daemon (Anti-Late-Attach)
        AntiAttachDaemon.startSentinel();

        // 3. Verify Native Bridge state
        if (NativeBridge.isLoaded()) {
            if (logger != null) {
                logger.info("[AntiOpsec-Security] Native Shield Engine initialized successfully (x64).");
            }
        } else {
            if (logger != null) {
                logger.warning("[AntiOpsec-Security] Native Shield running in pure bytecode fallback mode.");
            }
        }

        initialized = true;
    }

    public static boolean isTampered() {
        return AntiAgentGuard.isJvmTampered();
    }

    private SecurityEngine() {}
}
