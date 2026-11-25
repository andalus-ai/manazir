package tech.kayys.manazir.media.probe;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MediaProbeResult(Path source, String formatName, String formatLongName,
        BigDecimal formatDurationSeconds, Long formatSizeBytes, List<MediaStream> streams) {
    public MediaProbeResult {
        Objects.requireNonNull(source, "source");
        streams = List.copyOf(Objects.requireNonNull(streams, "streams"));
        formatName = formatName == null ? "unknown" : formatName;
    }
    public Optional<BigDecimal> durationSeconds() { return Optional.ofNullable(formatDurationSeconds); }
    public Optional<Long> sizeBytes() { return Optional.ofNullable(formatSizeBytes); }
    public List<MediaStream> streams(MediaStreamType type) {
        return streams.stream().filter(stream -> stream.type() == type).toList();
    }
}
