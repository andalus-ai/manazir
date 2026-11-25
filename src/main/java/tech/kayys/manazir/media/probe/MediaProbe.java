package tech.kayys.manazir.media.probe;

/**
 * Backend-neutral media inspection port.
 *
 * <p>Implementations must honor request limits, preserve native timestamps and
 * report unknown metadata as absent. Implementations must not mutate the source.</p>
 */
public interface MediaProbe {
    /** Stable identifier for the selected implementation, e.g. "ffmpeg-ffm". */
    String backendId();

    MediaProbeResult probe(MediaProbeRequest request) throws MediaProbeException;
}
