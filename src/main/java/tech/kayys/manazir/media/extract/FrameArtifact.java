package tech.kayys.manazir.media.extract;

import java.nio.file.Path;
import java.util.Objects;
import tech.kayys.manazir.media.time.MediaTimestamp;

/** Metadata for a successfully committed extracted frame. */
public record FrameArtifact(
        Path path,
        FrameImageFormat format,
        String contentType,
        long sizeBytes,
        int width,
        int height,
        MediaTimestamp requestedPosition,
        int streamIndex,
        String backendId) {

    public FrameArtifact {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(requestedPosition, "requestedPosition");
        Objects.requireNonNull(backendId, "backendId");
        if (sizeBytes <= 0 || width <= 0 || height <= 0 || streamIndex < 0) {
            throw new IllegalArgumentException("artifact size, dimensions and stream index must be positive");
        }
        if (!contentType.equals(format.contentType())) throw new IllegalArgumentException("contentType does not match format");
        if (backendId.isBlank()) throw new IllegalArgumentException("backendId must not be blank");
    }
}
