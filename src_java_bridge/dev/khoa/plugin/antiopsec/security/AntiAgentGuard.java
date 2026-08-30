package dev.khoa.plugin.antiopsec.security;

import java.lang.management.ManagementFactory;
import java.util.List;

public final class AntiAgentGuard {

    private static final String[] BLACKLISTED_ARGS = {
        "-javaagent:", "-agentlib:", "-Xrunjdwp:", "-Xdebug",
        "org.aspectj.weaver.loadtime.Agent", "byte-buddy-agent"
    };

    public static boolean isJvmTampered() {
        try {
            List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String arg : inputArguments) {
                for (String blacklisted : BLACKLISTED_ARGS) {
                    if (arg.toLowerCase().contains(blacklisted.toLowerCase())) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void enforceJvmIntegrity() {
        if (isJvmTampered()) {
            System.err.println("[AntiOpsec-Security] Unauthorized JVM debugging/agent detected. Terminating hook.");
            // Silent mitigation: corrupt state or shutdown
            Runtime.getRuntime().halt(1);
        }
    }

    private AntiAgentGuard() {}
}
