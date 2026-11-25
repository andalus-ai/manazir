package tech.kayys.manazir.media.probe;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable result of a successful container and stream inspection. */
public record MediaProbeResult(
        URI source,
        String backendId,
        MediaContainerInfo container,
        List<MediaStreamInfo> streams,
        List<ProbeDiagnostic> diagnostics) {

    public MediaProbeResult {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(backendId, "backendId");
        Objects.requireNonNull(container, "container");
        streams = List.copyOf(Objects.requireNonNull(streams, "streams"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (!source.isAbsolute()) throw new IllegalArgumentException("source URI must be absolute");
        if (backendId.isBlank()) throw new IllegalArgumentException("backendId must not be blank");
        Set<Integer> indexes = new HashSet<>();
        for (MediaStreamInfo stream : streams) {
            if (!indexes.add(stream.index())) {
                throw new IllegalArgumentException("duplicate stream index: " + stream.index());
            }
        }
    }
}
