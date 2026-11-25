package tech.kayys.manazir.media.extract;

import java.util.Objects;
import java.util.Optional;

/** Structured failure from a frame extraction backend. */
public final class FrameExtractionException extends Exception {
    private final Code code;
    private final Optional<String> backendId;

    public FrameExtractionException(Code code, Optional<String> backendId, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
        this.backendId = Objects.requireNonNull(backendId, "backendId");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank");
        backendId.ifPresent(id -> { if (id.isBlank()) throw new IllegalArgumentException("backendId must not be blank"); });
    }

    public FrameExtractionException(Code code, Optional<String> backendId, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
        this.backendId = Objects.requireNonNull(backendId, "backendId");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank");
        backendId.ifPresent(id -> { if (id.isBlank()) throw new IllegalArgumentException("backendId must not be blank"); });
    }

    public Code code() { return code; }
    public Optional<String> backendId() { return backendId; }

    public enum Code {
        INVALID_SOURCE, INVALID_DESTINATION, BACKEND_UNAVAILABLE, TIMEOUT, CANCELLED, RESOURCE_LIMIT, EXTRACTION_FAILED
    }
}
