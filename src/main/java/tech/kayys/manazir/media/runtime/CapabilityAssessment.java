package tech.kayys.manazir.media.runtime;

import java.util.Objects;

/** Evidence-backed assessment for one media capability. */
public record CapabilityAssessment(
        MediaCapability capability,
        CapabilityStatus status,
        String evidence) {
    public CapabilityAssessment {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(status, "status");
        evidence = evidence == null ? "" : evidence;
        if (status == CapabilityStatus.UNKNOWN && !evidence.isBlank()) {
            // Evidence may explain why verification has not yet been performed.
        }
    }
}
