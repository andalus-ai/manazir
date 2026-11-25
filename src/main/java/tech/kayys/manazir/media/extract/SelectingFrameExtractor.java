package tech.kayys.manazir.media.extract;

import java.util.Objects;
import java.util.Optional;
import tech.kayys.manazir.media.runtime.MediaCapability;
import tech.kayys.manazir.media.runtime.MediaProviderRegistry;
import tech.kayys.manazir.media.runtime.ProviderSelectionException;
import tech.kayys.manazir.media.runtime.ProviderSelectionPolicy;
import tech.kayys.manazir.media.runtime.SelectedMediaProvider;

/** Routes frame extraction through an explicitly configured capability registry. */
public final class SelectingFrameExtractor implements FrameExtractor {
    private final MediaProviderRegistry<FrameExtractor> providers;
    private final Optional<String> preferredProviderId;
    private final ProviderSelectionPolicy policy;

    public SelectingFrameExtractor(MediaProviderRegistry<FrameExtractor> providers,
            Optional<String> preferredProviderId, ProviderSelectionPolicy policy) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.preferredProviderId = Objects.requireNonNull(preferredProviderId, "preferredProviderId");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override public String backendId() { return preferredProviderId.orElse("capability-selected"); }

    @Override
    public FrameExtractionResult extract(FrameExtractionRequest request) throws FrameExtractionException {
        Objects.requireNonNull(request, "request");
        final SelectedMediaProvider<FrameExtractor> selected;
        try {
            selected = providers.select(MediaCapability.VIDEO_DECODE, preferredProviderId, policy);
        } catch (ProviderSelectionException exception) {
            throw new FrameExtractionException(FrameExtractionException.Code.BACKEND_UNAVAILABLE,
                    Optional.empty(), exception.getMessage() + ": " + String.join("; ", exception.diagnostics()), exception);
        }
        return selected.provider().extract(request);
    }
}
