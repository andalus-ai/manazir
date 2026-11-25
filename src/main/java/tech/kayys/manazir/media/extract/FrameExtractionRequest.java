package tech.kayys.manazir.media.extract;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import tech.kayys.manazir.media.time.MediaTimestamp;

/**
 * Request to extract one decoded video frame at a media presentation timestamp.
 *
 * <p>The timestamp is expressed in the source stream's time base. It is not a
 * wall-clock time and must not be converted through floating-point seconds by
 * an implementation when exact rational arithmetic is available.</p>
 */
public record FrameExtractionRequest(
        URI source,
        int videoStreamIndex,
        MediaTimestamp targetTimestamp,
        FrameSelectionPolicy selectionPolicy,
        Path output,
        FrameImageFormat imageFormat,
        Optional<FrameSize> outputSize,
        Duration timeout) {

    public FrameExtractionRequest {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(targetTimestamp, "targetTimestamp");
        Objects.requireNonNull(selectionPolicy, "selectionPolicy");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(imageFormat, "imageFormat");
        outputSize = Objects.requireNonNull(outputSize, "outputSize");
        Objects.requireNonNull(timeout, "timeout");
        if (!source.isAbsolute()) throw new IllegalArgumentException("source URI must be absolute");
        if (videoStreamIndex < 0) throw new IllegalArgumentException("videoStreamIndex must not be negative");
        output = output.toAbsolutePath().normalize();
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    public static FrameExtractionRequest firstAtOrAfter(
            URI source, int videoStreamIndex, MediaTimestamp timestamp, Path output) {
        return new FrameExtractionRequest(source, videoStreamIndex, timestamp,
                FrameSelectionPolicy.FIRST_AT_OR_AFTER, output, FrameImageFormat.PNG,
                Optional.empty(), Duration.ofSeconds(30));
    }
}
