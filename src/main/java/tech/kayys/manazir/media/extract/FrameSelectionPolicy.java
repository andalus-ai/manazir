package tech.kayys.manazir.media.extract;

/** Defines how a requested media timestamp maps to a decoded video frame. */
public enum FrameSelectionPolicy {
    /** Select the first decoded frame whose presentation timestamp is at or after the target. */
    FIRST_AT_OR_AFTER,
    /** Select the decoded frame with the smallest absolute distance from the target. */
    NEAREST
}
