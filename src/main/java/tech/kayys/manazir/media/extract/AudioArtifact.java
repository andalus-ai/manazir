package tech.kayys.manazir.media.extract;

import java.nio.file.Path;
import java.util.Objects;
import tech.kayys.manazir.media.time.MediaTimestamp;

/** Metadata for a committed 16-bit PCM WAV segment. */
public record AudioArtifact(
        Path path,
        String contentType,
        long sizeBytes,
        int sampleRate,
        int channels,
        int bitsPerSample,
        long sampleFrames,
        MediaTimestamp requestedStart,
        MediaTimestamp requestedEnd,
        int streamIndex,
        String backendId) {

    public AudioArtifact {
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(requestedStart, "requestedStart");
        Objects.requireNonNull(requestedEnd, "requestedEnd");
        Objects.requireNonNull(backendId, "backendId");
        if (sizeBytes <= 44 || sampleRate <= 0 || channels <= 0 || bitsPerSample != 16
                || sampleFrames <= 0 || streamIndex < 0) {
            throw new IllegalArgumentException("invalid audio artifact metadata");
        }
        if (requestedStart.compareTo(requestedEnd) >= 0) {
            throw new IllegalArgumentException("requestedEnd must be after requestedStart");
        }
        if (!"audio/wav".equals(contentType)) throw new IllegalArgumentException("contentType must be audio/wav");
    }
}
