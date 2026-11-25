package tech.kayys.manazir.media.extract;

import java.util.Objects;
import java.util.Optional;
import tech.kayys.manazir.media.runtime.MediaCapability;
import tech.kayys.manazir.media.runtime.MediaProviderRegistry;
import tech.kayys.manazir.media.runtime.ProviderSelectionException;
import tech.kayys.manazir.media.runtime.ProviderSelectionPolicy;

/** Audio extraction facade that selects only explicitly registered, available providers. */
public final class SelectingAudioExtractor implements AudioExtractor {
    private final MediaProviderRegistry<AudioExtractor> registry;
    private final Optional<String> preferredProviderId;
    private final ProviderSelectionPolicy policy;

    public SelectingAudioExtractor(
            MediaProviderRegistry<AudioExtractor> registry,
            Optional<String> preferredProviderId,
            ProviderSelectionPolicy policy) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.preferredProviderId = Objects.requireNonNull(preferredProviderId, "preferredProviderId");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override public String backendId() { return "provider-registry"; }

    @Override
    public AudioArtifact extract(AudioExtractionRequest request) throws AudioExtractionException {
        Objects.requireNonNull(request, "request");
        try {
            return registry.select(MediaCapability.AUDIO_EXTRACTION, preferredProviderId, policy)
                    .provider().extract(request);
        } catch (ProviderSelectionException exception) {
            throw new AudioExtractionException(AudioExtractionException.Code.BACKEND_UNAVAILABLE,
                    Optional.empty(), exception.getMessage() + ": " + String.join("; ", exception.diagnostics()), exception);
        }
    }
}
