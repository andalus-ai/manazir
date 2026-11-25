package tech.kayys.manazir.media.probe;

import java.util.Objects;
import java.util.Optional;
import tech.kayys.manazir.media.runtime.MediaCapability;
import tech.kayys.manazir.media.runtime.MediaProviderRegistry;
import tech.kayys.manazir.media.runtime.ProviderSelectionException;
import tech.kayys.manazir.media.runtime.ProviderSelectionPolicy;

/**
 * Routes probe requests through an explicitly configured capability registry.
 * Provider discovery and availability checks remain the caller's responsibility.
 */
public final class SelectingMediaProbe implements MediaProbe {
    private final MediaProviderRegistry<MediaProbe> providers;
    private final Optional<String> preferredProviderId;
    private final ProviderSelectionPolicy policy;

    public SelectingMediaProbe(
            MediaProviderRegistry<MediaProbe> providers,
            Optional<String> preferredProviderId,
            ProviderSelectionPolicy policy) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.preferredProviderId = Objects.requireNonNull(preferredProviderId, "preferredProviderId");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public String backendId() {
        return preferredProviderId.orElse("capability-selected");
    }

    @Override
    public MediaProbeResult probe(MediaProbeRequest request) throws MediaProbeException {
        Objects.requireNonNull(request, "request");
        final var selected;
        try {
            selected = providers.select(MediaCapability.MEDIA_PROBE, preferredProviderId, policy);
        } catch (ProviderSelectionException exception) {
            throw new MediaProbeException(MediaProbeException.Code.BACKEND_UNAVAILABLE,
                    Optional.empty(), exception.getMessage() + ": " + String.join("; ", exception.diagnostics()), exception);
        }
        return selected.provider().probe(request);
    }
}
