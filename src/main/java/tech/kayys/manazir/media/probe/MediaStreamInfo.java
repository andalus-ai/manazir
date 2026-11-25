package tech.kayys.manazir.media.probe;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import tech.kayys.manazir.media.time.MediaTimestamp;
import tech.kayys.manazir.media.time.RationalTimeBase;

/** Stream metadata; timestamp values are always expressed in the stream's own time base. */
public record MediaStreamInfo(
        int index, Optional<String> streamId, MediaStreamType type, Optional<String> codecName,
        Optional<RationalTimeBase> timeBase, Optional<MediaTimestamp> startTime,
        Optional<MediaTimestamp> duration, OptionalInt width, OptionalInt height,
        OptionalInt sampleRate, OptionalInt channels, Optional<FrameRate> averageFrameRate,
        Optional<FrameRate> nominalFrameRate) {
    public MediaStreamInfo {
        if (index < 0) throw new IllegalArgumentException("index must not be negative");
        streamId = Objects.requireNonNull(streamId, "streamId");
        type = Objects.requireNonNull(type, "type");
        codecName = Objects.requireNonNull(codecName, "codecName");
        timeBase = Objects.requireNonNull(timeBase, "timeBase");
        startTime = Objects.requireNonNull(startTime, "startTime");
        duration = Objects.requireNonNull(duration, "duration");
        width = Objects.requireNonNull(width, "width");
        height = Objects.requireNonNull(height, "height");
        sampleRate = Objects.requireNonNull(sampleRate, "sampleRate");
        channels = Objects.requireNonNull(channels, "channels");
        averageFrameRate = Objects.requireNonNull(averageFrameRate, "averageFrameRate");
        nominalFrameRate = Objects.requireNonNull(nominalFrameRate, "nominalFrameRate");
        streamId.ifPresent(v -> { if (v.isBlank()) throw new IllegalArgumentException("streamId must not be blank"); });
        codecName.ifPresent(v -> { if (v.isBlank()) throw new IllegalArgumentException("codecName must not be blank"); });
        if (duration.isPresent() && duration.get().ticks() < 0) throw new IllegalArgumentException("duration must not be negative");
        if (startTime.isPresent() && (timeBase.isEmpty() || !startTime.get().timeBase().equals(timeBase.get()))) {
            throw new IllegalArgumentException("startTime must use the stream timebase");
        }
        if (duration.isPresent() && (timeBase.isEmpty() || !duration.get().timeBase().equals(timeBase.get()))) {
            throw new IllegalArgumentException("duration must use the stream timebase");
        }
        if (width.isPresent() != height.isPresent()) throw new IllegalArgumentException("width and height must both be present or absent");
        if (width.isPresent() && (width.getAsInt() <= 0 || height.getAsInt() <= 0)) throw new IllegalArgumentException("video dimensions must be positive");
        if (sampleRate.isPresent() && sampleRate.getAsInt() <= 0) throw new IllegalArgumentException("sampleRate must be positive");
        if (channels.isPresent() && channels.getAsInt() <= 0) throw new IllegalArgumentException("channels must be positive");
    }
}