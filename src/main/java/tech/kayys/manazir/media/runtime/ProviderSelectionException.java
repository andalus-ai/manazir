package tech.kayys.manazir.media.runtime;

import java.util.List;

/** Raised when no registered provider can safely satisfy a capability request. */
public final class ProviderSelectionException extends RuntimeException {
    private final MediaCapability capability;
    private final List<String> diagnostics;

    public ProviderSelectionException(MediaCapability capability, String message, List<String> diagnostics) {
        super(message);
        this.capability = capability;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public MediaCapability capability() { return capability; }
    public List<String> diagnostics() { return diagnostics; }
}
