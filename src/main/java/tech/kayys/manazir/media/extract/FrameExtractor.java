package tech.kayys.manazir.media.extract;

/** Backend-neutral capability for extracting a still image from a video stream. */
@FunctionalInterface
public interface FrameExtractor {
    FrameExtractionResult extract(FrameExtractionRequest request) throws FrameExtractionException;

    /** Stable identifier for diagnostics and provenance. */
    default String backendId() { return getClass().getSimpleName(); }
}
