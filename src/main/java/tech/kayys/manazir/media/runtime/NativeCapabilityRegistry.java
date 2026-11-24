package tech.kayys.manazir.media.runtime;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable, conservative view of verified media capabilities. */
public final class NativeCapabilityRegistry {
    private final Map<MediaCapability, CapabilityAssessment> assessments;

    public NativeCapabilityRegistry(Map<MediaCapability, CapabilityAssessment> assessments) {
        Objects.requireNonNull(assessments, "assessments");
        var copy = new EnumMap<MediaCapability, CapabilityAssessment>(MediaCapability.class);
        for (MediaCapability capability : MediaCapability.values()) {
            var assessment = assessments.get(capability);
            if (assessment == null) {
                assessment = new CapabilityAssessment(
                        capability, CapabilityStatus.UNKNOWN, "No verifier evidence supplied");
            }
            if (assessment.capability() != capability) {
                throw new IllegalArgumentException("assessment key does not match capability");
            }
            copy.put(capability, assessment);
        }
        this.assessments = Collections.unmodifiableMap(copy);
    }

    public static NativeCapabilityRegistry unknown() {
        return new NativeCapabilityRegistry(Map.of());
    }

    public CapabilityAssessment assessment(MediaCapability capability) {
        return assessments.get(Objects.requireNonNull(capability, "capability"));
    }

    public boolean isVerified(MediaCapability capability) {
        return assessment(capability).status() == CapabilityStatus.VERIFIED;
    }

    public Map<MediaCapability, CapabilityAssessment> assessments() {
        return assessments;
    }
}
