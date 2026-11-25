package tech.kayys.manazir.media.extract;

/** Backend-neutral port for extracting one decoded video frame. */
public interface FrameExtractor {
    String backendId();
    FrameArtifact extract(FrameExtractionRequest request) throws FrameExtractionException;
}
