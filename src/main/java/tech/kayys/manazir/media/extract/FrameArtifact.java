package tech.kayys.manazir.media.extract;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import tech.kayys.manazir.media.time.MediaTimestamp;

/**
 * Legacy metadata shape retained for source compatibility with early M2
 * prototypes. New integrations should use {@link FrameExtractionResult}.
 */
@Deprecated(forRemoval = false)
public record FrameArtifact(
        Path path,
        FrameImageFormat format,
        String contentType,
        long sizeBytes,
        int width,
        int height,
        MediaTimestamp requestedPosition,
        Optional<MediaTimestamp> actualPosition,
        int streamIndex,
        String backendId) {

    public FrameArtifact {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(requestedPosition, "requestedPosition");
        actualPosition = Objects.requireNonNull(actualPosition, "actualPosition");
        Objects.requireNonNull(backendId, "backendId");
        if (sizeBytes <= 0 || width <= 0 || height <= 0 || streamIndex < 0) {
            throw new IllegalArgumentException("artifact size, dimensions and stream index must be positive");
        }
        if (!contentType.equals(format.mediaType())) throw new IllegalArgumentException("contentType does not match format");
        if (backendId.isBlank()) throw new IllegalArgumentException("backendId must not be blank");
    }
}
