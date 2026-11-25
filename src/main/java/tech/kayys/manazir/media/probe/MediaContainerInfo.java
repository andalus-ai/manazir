package tech.kayys.manazir.media.probe;

import java.util.Objects;
import java.util.Optional;
import tech.kayys.manazir.media.time.MediaTimestamp;

/** Container-level metadata. Its clock is independent of every stream clock. */
public record MediaContainerInfo(
        String formatName,
        Optional<String> longFormatName,
        Optional<MediaTimestamp> startTime,
        Optional<MediaTimestamp> duration) {

    public MediaContainerInfo {
        Objects.requireNonNull(formatName, "formatName");
        longFormatName = Objects.requireNonNull(longFormatName, "longFormatName");
        startTime = Objects.requireNonNull(startTime, "startTime");
        duration = Objects.requireNonNull(duration, "duration");
        if (formatName.isBlank()) throw new IllegalArgumentException("formatName must not be blank");
        if (longFormatName.isPresent() && longFormatName.get().isBlank()) {
            throw new IllegalArgumentException("longFormatName must not be blank when present");
        }
        if (duration.isPresent() && duration.get().ticks() < 0) {
            throw new IllegalArgumentException("container duration must not be negative");
        }
    }
}
