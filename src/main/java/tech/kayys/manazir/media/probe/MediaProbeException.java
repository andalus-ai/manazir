package tech.kayys.manazir.media.probe;

import java.util.Objects;
import java.util.Optional;

/** Structured failure from a media probe backend. */
public final class MediaProbeException extends Exception {
    private final Code code;
    private final Optional<String> backendId;

    public MediaProbeException(Code code, Optional<String> backendId, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
        this.backendId = Objects.requireNonNull(backendId, "backendId");
        validateBackendId(backendId);
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank");
    }

    public MediaProbeException(Code code, Optional<String> backendId, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
        this.backendId = Objects.requireNonNull(backendId, "backendId");
        validateBackendId(backendId);
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank");
    }

    public Code code() { return code; }
    public Optional<String> backendId() { return backendId; }

    private static void validateBackendId(Optional<String> value) {
        value.ifPresent(id -> { if (id.isBlank()) throw new IllegalArgumentException("backendId must not be blank"); });
    }

    public enum Code {
        INVALID_SOURCE, UNSUPPORTED_FORMAT, TIMEOUT, RESOURCE_LIMIT,
        BACKEND_UNAVAILABLE, CANCELLED, PROBE_FAILED
    }
}
