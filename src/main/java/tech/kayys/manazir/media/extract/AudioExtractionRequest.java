package tech.kayys.manazir.media.extract;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import tech.kayys.manazir.media.time.MediaTimestamp;

/** Exact source interval and PCM output constraints for audio segment extraction. */
public record AudioExtractionRequest(
        URI source,
        int audioStreamIndex,
        MediaTimestamp start,
        MediaTimestamp end,
        Path output,
        int sampleRate,
        int channels,
        Duration timeout,
        long maxOutputBytes) {

    public AudioExtractionRequest {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(timeout, "timeout");
        if (!source.isAbsolute()) throw new IllegalArgumentException("source URI must be absolute");
        if (audioStreamIndex < 0) throw new IllegalArgumentException("audioStreamIndex must not be negative");
        if (start.compareTo(end) >= 0) throw new IllegalArgumentException("end must be after start");
        output = output.toAbsolutePath().normalize();
        if (sampleRate < 8_000 || sampleRate > 384_000) throw new IllegalArgumentException("sampleRate must be between 8000 and 384000 Hz");
        if (channels < 1 || channels > 8) throw new IllegalArgumentException("channels must be between 1 and 8");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        if (maxOutputBytes <= 44) throw new IllegalArgumentException("maxOutputBytes must exceed the WAV header size");
    }

    public static AudioExtractionRequest wav(
            URI source, int audioStreamIndex, MediaTimestamp start, MediaTimestamp end, Path output) {
        return new AudioExtractionRequest(source, audioStreamIndex, start, end, output,
                48_000, 2, Duration.ofSeconds(60), 128L * 1024 * 1024);
    }
}
