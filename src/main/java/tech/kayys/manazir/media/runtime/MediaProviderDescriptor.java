package tech.kayys.manazir.media.runtime;

import java.util.Objects;
import java.util.Set;

/** Immutable, evidence-bearing description of one registered media provider. */
public record MediaProviderDescriptor(
        String providerId,
        Set<MediaCapability> capabilities,
        ProviderAvailability availability,
        int priority,
        String diagnostic) {
    public MediaProviderDescriptor {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities"));
        if (capabilities.isEmpty()) {
            throw new IllegalArgumentException("a provider must declare at least one capability");
        }
        if (capabilities.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("capabilities must not contain null");
        }
        Objects.requireNonNull(availability, "availability");
        diagnostic = diagnostic == null ? "" : diagnostic;
    }

    public boolean supports(MediaCapability capability) {
        return capabilities.contains(Objects.requireNonNull(capability, "capability"));
    }
}
