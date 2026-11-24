package tech.kayys.manazir.media.runtime;

import java.util.List;
import java.util.Objects;

/** Immutable version and capability verification result. */
public record NativeVerificationReport(
        List<NativeLibraryInspection> libraries,
        NativeCapabilityRegistry capabilities) {
    public NativeVerificationReport {
        libraries = List.copyOf(Objects.requireNonNull(libraries, "libraries"));
        Objects.requireNonNull(capabilities, "capabilities");
    }

    public List<NativeLibraryInspection> unverifiedLibraries() {
        return libraries.stream().filter(inspection -> inspection.version() == null).toList();
    }
}
