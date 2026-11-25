package tech.kayys.manazir.media.runtime;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Explicitly checks whether a process executable can run its version command.
 * This never loads native libraries and does not infer which media operations
 * the executable supports.
 */
public final class ExecutableAvailabilityCheck {
    private ExecutableAvailabilityCheck() { }

    public static ExecutableCheckResult inspect(String executable, Duration timeout) {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("executable must not be blank");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");

        final Process process;
        try {
            process = new ProcessBuilder(executable, "-version")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException exception) {
            return new ExecutableCheckResult(executable, ProviderAvailability.UNAVAILABLE,
                    "Unable to start executable: " + exception.getMessage());
        }

        try {
            long millis;
            try { millis = Math.max(1, timeout.toMillis()); }
            catch (ArithmeticException exception) { millis = Long.MAX_VALUE; }
            if (!process.waitFor(millis, TimeUnit.MILLISECONDS)) {
                terminate(process);
                return new ExecutableCheckResult(executable, ProviderAvailability.UNAVAILABLE,
                        "Version check exceeded timeout " + timeout);
            }
            if (process.exitValue() == 0) {
                return new ExecutableCheckResult(executable, ProviderAvailability.AVAILABLE,
                        "Executable completed -version successfully");
            }
            return new ExecutableCheckResult(executable, ProviderAvailability.UNAVAILABLE,
                    "Version command exited with code " + process.exitValue());
        } catch (InterruptedException exception) {
            terminate(process);
            Thread.currentThread().interrupt();
            return new ExecutableCheckResult(executable, ProviderAvailability.UNKNOWN,
                    "Version check was interrupted");
        } finally {
            if (process.isAlive()) terminate(process);
        }
    }

    private static void terminate(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }
}
