package tech.kayys.manazir.media.runtime;

import java.time.Duration;
import java.util.Objects;

/** Result of an explicit executable smoke check; it says nothing about media capabilities. */
public record ExecutableCheckResult(
        String executable,
        ProviderAvailability availability,
        String diagnostic) {
    public ExecutableCheckResult {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("executable must not be blank");
        Objects.requireNonNull(availability, "availability");
        diagnostic = diagnostic == null ? "" : diagnostic;
    }
}
