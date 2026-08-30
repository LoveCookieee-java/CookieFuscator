package dev.khoa.plugin.antiopsec.security;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Continuous background sentinel that neutralizes runtime late-attaching agents,
 * dynamic debuggers, and unauthorized JVMTI threads.
 */
public final class AntiAttachDaemon {

    private static final ScheduledExecutorService SENTINEL = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Opsec-Runtime-Sentinel");
        t.setDaemon(true);
        return t;
    });

    public static void startSentinel() {
        SENTINEL.scheduleAtFixedRate(() -> {
            try {
                // 1. Verify thread names for debugger / agent signatures
                for (Thread t : Thread.getAllStackTraces().keySet()) {
                    String name = t.getName().toLowerCase();
                    if (name.contains("attach listener") || 
                        name.contains("jdwp") || 
                        name.contains("byte-buddy") || 
                        name.contains("recaf")) {
                        // Tamper detected: Halt immediately
                        Runtime.getRuntime().halt(9);
                    }
                }

                // 2. Check for newly introduced JVM arguments
                if (AntiAgentGuard.isJvmTampered()) {
                    Runtime.getRuntime().halt(9);
                }
            } catch (Throwable ignored) {}
        }, 1, 3, TimeUnit.SECONDS);
    }

    private AntiAttachDaemon() {}
}
