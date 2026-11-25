package tech.kayys.manazir.media.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic, capability-based registry. Registration is explicit: this class
 * does not discover providers, load native libraries, or execute external tools.
 */
public final class MediaProviderRegistry<T> {
    private final List<Entry<T>> entries;

    private MediaProviderRegistry(List<Entry<T>> entries) {
        this.entries = List.copyOf(entries);
    }

    public static <T> Builder<T> builder() { return new Builder<>(); }

    public List<MediaProviderDescriptor> descriptors() {
        return entries.stream().map(Entry::descriptor).toList();
    }

    public SelectedMediaProvider<T> select(
            MediaCapability capability,
            Optional<String> preferredProviderId,
            ProviderSelectionPolicy policy) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(preferredProviderId, "preferredProviderId");
        Objects.requireNonNull(policy, "policy");

        List<String> diagnostics = new ArrayList<>();
        Entry<T> preferred = null;
        if (preferredProviderId.isPresent()) {
            String id = preferredProviderId.get();
            preferred = entries.stream().filter(entry -> entry.descriptor().providerId().equals(id))
                    .findFirst().orElse(null);
            if (preferred == null) {
                diagnostics.add("Requested provider '" + id + "' is not registered");
                if (policy == ProviderSelectionPolicy.STRICT) {
                    throw new ProviderSelectionException(capability, "Requested provider is not registered", diagnostics);
                }
            } else {
                String rejection = rejection(preferred.descriptor(), capability);
                if (rejection == null) {
                    return selected(preferred, diagnostics);
                }
                diagnostics.add("Provider '" + preferred.descriptor().providerId() + "': " + rejection);
                if (policy == ProviderSelectionPolicy.STRICT) {
                    throw new ProviderSelectionException(capability,
                            "Requested provider cannot satisfy " + capability, diagnostics);
                }
            }
        }

        List<Entry<T>> candidates = entries.stream()
                .filter(entry -> entry.descriptor().supports(capability))
                .filter(entry -> entry.descriptor().availability() == ProviderAvailability.AVAILABLE)
                .sorted(Comparator.<Entry<T>>comparingInt(entry -> entry.descriptor().priority()).reversed()
                        .thenComparing(entry -> entry.descriptor().providerId()))
                .toList();

        if (candidates.isEmpty()) {
            for (Entry<T> entry : entries) {
                if (entry.descriptor().supports(capability)) {
                    diagnostics.add("Provider '" + entry.descriptor().providerId() + "': "
                            + rejection(entry.descriptor(), capability));
                }
            }
            throw new ProviderSelectionException(capability,
                    "No available provider supports " + capability, diagnostics);
        }

        Entry<T> selected = candidates.getFirst();
        if (preferredProviderId.isPresent()) {
            diagnostics.add("Fell back to '" + selected.descriptor().providerId()
                    + "' because the preferred provider could not be used");
        } else {
            diagnostics.add("Selected highest-priority available provider; ties are resolved by provider ID");
        }
        return selected(selected, diagnostics);
    }

    private static String rejection(MediaProviderDescriptor descriptor, MediaCapability capability) {
        if (!descriptor.supports(capability)) return "does not support " + capability;
        return switch (descriptor.availability()) {
            case AVAILABLE -> null;
            case UNAVAILABLE -> "provider is unavailable: " + descriptor.diagnostic();
            case UNKNOWN -> "availability is unknown; refusing implicit activation: " + descriptor.diagnostic();
        };
    }

    private static <T> SelectedMediaProvider<T> selected(Entry<T> entry, List<String> diagnostics) {
        return new SelectedMediaProvider<>(entry.descriptor(), entry.provider(), diagnostics);
    }

    private record Entry<T>(MediaProviderDescriptor descriptor, T provider) { }

    public static final class Builder<T> {
        private final List<Entry<T>> entries = new ArrayList<>();
        private final Set<String> ids = new HashSet<>();

        public Builder<T> register(MediaProviderDescriptor descriptor, T provider) {
            Objects.requireNonNull(descriptor, "descriptor");
            Objects.requireNonNull(provider, "provider");
            if (!ids.add(descriptor.providerId())) {
                throw new IllegalArgumentException("duplicate provider ID: " + descriptor.providerId());
            }
            entries.add(new Entry<>(descriptor, provider));
            return this;
        }

        public MediaProviderRegistry<T> build() {
            return new MediaProviderRegistry<>(entries);
        }
    }
}
