package tech.kayys.manazir.media.probe;

import tech.kayys.manazir.media.time.MediaTimestamp;
import tech.kayys.manazir.media.time.RationalTimeBase;
import java.util.Objects;
import java.util.Optional;

/** Immutable stream metadata. Timestamp fields retain the source stream time base. */
public record MediaStream(int index, MediaStreamType type, String codecName, RationalTimeBase timeBase,
        Long startTimestampTicks, Long durationTicks, Integer width, Integer height, String pixelFormat,
        String sampleFormat, Integer sampleRate, Integer channels, String channelLayout,
        String averageFrameRate, String realFrameRate) {
    public MediaStream {
        if (index < 0) throw new IllegalArgumentException("index must be non-negative");
        Objects.requireNonNull(type, "type");
        codecName = codecName == null ? "unknown" : codecName;
        if ((startTimestampTicks != null || durationTicks != null) && timeBase == null)
            throw new IllegalArgumentException("timestamp ticks require a stream time base");
    }
    public Optional<MediaTimestamp> startTimestamp() {
        return startTimestampTicks == null ? Optional.empty() : Optional.of(new MediaTimestamp(startTimestampTicks, timeBase));
    }
    public Optional<MediaTimestamp> duration() {
        return durationTicks == null ? Optional.empty() : Optional.of(new MediaTimestamp(durationTicks, timeBase));
    }
}
