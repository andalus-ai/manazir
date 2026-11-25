package tech.kayys.manazir.media.extract;

/** Provider port for extracting a bounded audio interval into a PCM artifact. */
public interface AudioExtractor {
    String backendId();

    AudioArtifact extract(AudioExtractionRequest request) throws AudioExtractionException;
}
