package tech.kayys.manazir.media.runtime;

/** UNKNOWN is deliberately distinct from UNAVAILABLE: absence of evidence is not failure. */
public enum CapabilityStatus {
    VERIFIED,
    UNAVAILABLE,
    UNKNOWN
}
