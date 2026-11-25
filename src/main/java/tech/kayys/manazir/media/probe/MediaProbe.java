package tech.kayys.manazir.media.probe;

@FunctionalInterface
public interface MediaProbe {
    MediaProbeResult probe(MediaProbeRequest request) throws MediaProbeException;
}
