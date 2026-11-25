package tech.kayys.manazir.media.extract;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import tech.kayys.manazir.media.time.MediaTimestamp;

/** Metadata describing the still image produced by a frame extractor. */
public record FrameExtractionResult(
        Path output,
        FrameImageFormat imageFormat,
        Optional<MediaTimestamp> actualTimestamp,
        Optional<FrameSize> outputSize,
        long outputBytes,
        String backendId) {

    public FrameExtractionResult {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(imageFormat, "imageFormat");
        actualTimestamp = Objects.requireNonNull(actualTimestamp, "actualTimestamp");
        outputSize = Objects.requireNonNull(outputSize, "outputSize");
        Objects.requireNonNull(backendId, "backendId");
        output = output.toAbsolutePath().normalize();
        if (outputBytes < 0) throw new IllegalArgumentException("outputBytes must not be negative");
        if (backendId.isBlank()) throw new IllegalArgumentException("backendId must not be blank");
    }
}
