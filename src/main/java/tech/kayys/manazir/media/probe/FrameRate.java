package tech.kayys.manazir.media.probe;

/** Exact frame rate in frames per second, represented without floating point. */
public record FrameRate(long numerator, long denominator) {
    public FrameRate {
        if (numerator <= 0) throw new IllegalArgumentException("numerator must be positive");
        if (denominator <= 0) throw new IllegalArgumentException("denominator must be positive");
    }
}
