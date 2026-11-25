package tech.kayys.manazir.media.probe;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded, read-only request to inspect one media source.
 *
 * <p>The URI is passed as data to a backend, never interpolated into a shell
 * command. A backend must enforce the supplied limits or fail explicitly.</p>
 */
public record MediaProbeRequest(
        URI source,
        Optional<String> formatHint,
        Duration timeout,
        long maxProbeBytes) {

    public MediaProbeRequest {
        Objects.requireNonNull(source, "source");
        formatHint = Objects.requireNonNull(formatHint, "formatHint");
        Objects.requireNonNull(timeout, "timeout");
        if (!source.isAbsolute()) throw new IllegalArgumentException("source URI must be absolute");
        if (formatHint.isPresent() && formatHint.get().isBlank()) {
            throw new IllegalArgumentException("formatHint must not be blank when present");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (maxProbeBytes <= 0) throw new IllegalArgumentException("maxProbeBytes must be positive");
    }

    public static MediaProbeRequest defaults(URI source) {
        return new MediaProbeRequest(source, Optional.empty(), Duration.ofSeconds(15), 10L * 1024 * 1024);
    }
}
