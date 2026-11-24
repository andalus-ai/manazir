package tech.kayys.manazir.media.runtime;

import java.util.List;
import java.util.Objects;

/**
 * Result of inspecting one loaded library. A native adapter must only list a
 * capability as VERIFIED after checking the required symbols and ABI/version
 * constraints, not merely because the shared library loaded.
 */
public record NativeLibraryInspection(
        NativeLibrary library,
        NativeLibraryVersion version,
        List<CapabilityAssessment> capabilities,
        String diagnostic) {
    public NativeLibraryInspection {
        Objects.requireNonNull(library, "library");
        if (version != null && version.library() != library) {
            throw new IllegalArgumentException("version belongs to a different library");
        }
        capabilities = List.copyOf(Objects.requireNonNull(capabilities, "capabilities"));
        if (capabilities.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("capabilities must not contain null");
        }
        diagnostic = diagnostic == null ? "" : diagnostic;
    }
}
