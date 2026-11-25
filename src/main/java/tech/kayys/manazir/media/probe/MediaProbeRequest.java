package tech.kayys.manazir.media.probe;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public record MediaProbeRequest(Path source, Duration timeout) {
    public MediaProbeRequest {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(timeout, "timeout");
        source = source.toAbsolutePath().normalize();
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
    }
    public static MediaProbeRequest of(Path source) { return new MediaProbeRequest(source, Duration.ofSeconds(30)); }
}
