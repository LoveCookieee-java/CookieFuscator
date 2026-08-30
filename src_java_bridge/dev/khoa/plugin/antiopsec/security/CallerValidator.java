package dev.khoa.plugin.antiopsec.security;

import java.lang.StackWalker.StackFrame;
import java.util.Optional;

public final class CallerValidator {

    public static boolean verifyCaller() {
        try {
            String currentPkg = CallerValidator.class.getPackageName();
            StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            Optional<StackFrame> callerFrame = walker.walk(frames -> 
                frames.skip(2).findFirst()
            );

            if (callerFrame.isPresent()) {
                String callerClassName = callerFrame.get().getClassName();
                // Allow same package or internal subpackages, reject foreign reflection
                return (callerClassName.startsWith(currentPkg) || 
                        callerClassName.startsWith("cookie.fack.please")) && 
                       !callerClassName.contains("reflect");
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void enforceAuthorizedAccess() {
        if (!verifyCaller()) {
            throw new SecurityException("Unauthorized foreign reflection access blocked by OpSec.");
        }
    }

    private CallerValidator() {}
}
