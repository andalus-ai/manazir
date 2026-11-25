package tech.kayys.manazir.media.runtime;

/** Controls whether a preferred provider may be replaced by another provider. */
public enum ProviderSelectionPolicy {
    /** Never replace a requested provider; fail if it cannot satisfy the request. */
    STRICT,
    /** Permit deterministic fallback when the preferred provider is unusable. */
    ALLOW_FALLBACK
}
