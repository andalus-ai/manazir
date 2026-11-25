package tech.kayys.manazir.media.runtime;

import java.util.List;
import java.util.Objects;

/** The selected implementation and the reasons that led to the selection. */
public record SelectedMediaProvider<T>(
        MediaProviderDescriptor descriptor,
        T provider,
        List<String> diagnostics) {
    public SelectedMediaProvider {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(provider, "provider");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
