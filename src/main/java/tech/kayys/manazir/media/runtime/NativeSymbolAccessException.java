package tech.kayys.manazir.media.runtime;

/** Raised when a required native symbol cannot be resolved or invoked. */
public final class NativeSymbolAccessException extends RuntimeException {
    public NativeSymbolAccessException(String message) {
        super(message);
    }

    public NativeSymbolAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
