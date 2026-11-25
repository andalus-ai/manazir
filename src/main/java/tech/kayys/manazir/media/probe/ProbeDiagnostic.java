package tech.kayys.manazir.media.probe;

import java.util.Objects;

/** Non-fatal, machine-readable observation made while probing. */
public record ProbeDiagnostic(Severity severity, String code, String message) {
    public ProbeDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        if (code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        if (message.isBlank()) throw new IllegalArgumentException("message must not be blank");
    }

    public enum Severity { INFO, WARNING }
}
