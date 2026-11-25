package tech.kayys.manazir.media.extract;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import tech.kayys.manazir.media.time.MediaTimestamp;

/**
 * Requests one decoded video frame at a position on the source media timeline.
 * The output path must not already exist; implementations must not overwrite user files.
 */
public record FrameExtractionRequest(
        URI source,
        int streamIndex,
        MediaTimestamp position,
        Path destination,
        FrameImageFormat format,
        Duration timeout,
        long maxOutputBytes) {

    public FrameExtractionRequest {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(timeout, "timeout");
        if (!source.isAbsolute()) throw new IllegalArgumentException("source URI must be absolute");
        if (streamIndex < 0) throw new IllegalArgumentException("streamIndex must not be negative");
        if (!destination.isAbsolute()) throw new IllegalArgumentException("destination must be absolute");
        destination = destination.normalize();
        if (destination.getFileName() == null) throw new IllegalArgumentException("destination must name a file");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        if (maxOutputBytes <= 0) throw new IllegalArgumentException("maxOutputBytes must be positive");
    }
}
