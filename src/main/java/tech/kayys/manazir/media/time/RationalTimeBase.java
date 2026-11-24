package tech.kayys.manazir.media.time;

/**
 * Exact media time base: one tick represents numerator / denominator seconds.
 *
 * <p>For example, {@code 1/25} represents a 25 fps frame time base and
 * {@code 1/90000} represents a common MPEG clock. Values are intentionally
 * not reduced: preserving the source representation can be useful for
 * diagnostics and round-trip metadata.</p>
 */
public record RationalTimeBase(long numerator, long denominator) {
    public RationalTimeBase {
        if (numerator <= 0) {
            throw new IllegalArgumentException("numerator must be positive");
        }
        if (denominator <= 0) {
            throw new IllegalArgumentException("denominator must be positive");
        }
    }

    public static RationalTimeBase of(long numerator, long denominator) {
        return new RationalTimeBase(numerator, denominator);
    }
}
