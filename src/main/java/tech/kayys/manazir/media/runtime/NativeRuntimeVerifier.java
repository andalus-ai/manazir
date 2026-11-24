package tech.kayys.manazir.media.runtime;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Objects;

/**
 * Runs an explicitly supplied inspection adapter only for successfully loaded
 * libraries. No FFmpeg symbol is assumed by the core runtime.
 */
public final class NativeRuntimeVerifier {
    private final NativeLibraryInspector inspector;

    public NativeRuntimeVerifier(NativeLibraryInspector inspector) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
    }

    public NativeVerificationReport verify(NativeRuntimeReport runtimeReport) {
        Objects.requireNonNull(runtimeReport, "runtimeReport");
        var inspections = new ArrayList<NativeLibraryInspection>();
        var assessments = new EnumMap<MediaCapability, CapabilityAssessment>(MediaCapability.class);

        for (var loaded : runtimeReport.loadedLibraries()) {
            var inspection = Objects.requireNonNull(inspector.inspect(loaded),
                    "inspector returned null for " + loaded);
            if (inspection.library() != loaded) {
                throw new IllegalStateException("inspector returned an inspection for a different library");
            }
            inspections.add(inspection);
            for (var assessment : inspection.capabilities()) {
                var previous = assessments.putIfAbsent(assessment.capability(), assessment);
                if (previous != null && !previous.equals(assessment)) {
                    throw new IllegalStateException("conflicting capability assessments for "
                            + assessment.capability());
                }
            }
        }

        return new NativeVerificationReport(inspections, new NativeCapabilityRegistry(assessments));
    }
}
