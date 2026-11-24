package tech.kayys.manazir.media.time;

import java.math.BigInteger;
import java.util.Objects;

/**
 * A media timestamp represented as integer ticks in an explicit time base.
 * The value is not a wall-clock timestamp and must not be interpreted without
 * its time base.
 */
public record MediaTimestamp(long ticks, RationalTimeBase timeBase)
        implements Comparable<MediaTimestamp> {

    public MediaTimestamp {
        Objects.requireNonNull(timeBase, "timeBase");
    }

    /**
     * Rescales this timestamp to another time base.
     *
     * @param target target time base
     * @param rounding how to handle a result that is not an integer tick
     */
    public MediaTimestamp rescaleTo(RationalTimeBase target, Rounding rounding) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(rounding, "rounding");

        // targetTicks = ticks * sourceSecondsPerTick / targetSecondsPerTick
        BigInteger numerator = BigInteger.valueOf(ticks)
                .multiply(BigInteger.valueOf(timeBase.numerator()))
                .multiply(BigInteger.valueOf(target.denominator()));
        BigInteger denominator = BigInteger.valueOf(timeBase.denominator())
                .multiply(BigInteger.valueOf(target.numerator()));

        BigInteger[] qr = numerator.divideAndRemainder(denominator);
        BigInteger result = qr[0];
        if (qr[1].signum() != 0) {
            result = switch (rounding) {
                case TOWARD_ZERO -> result;
                case FLOOR -> numerator.signum() < 0 ? result.subtract(BigInteger.ONE) : result;
                case CEILING -> numerator.signum() > 0 ? result.add(BigInteger.ONE) : result;
                case HALF_UP -> qr[1].abs().shiftLeft(1).compareTo(denominator) >= 0
                        ? result.add(BigInteger.valueOf(numerator.signum())) : result;
                case EXACT -> throw new ArithmeticException("timestamp cannot be represented exactly in target time base");
            };
        }
        return new MediaTimestamp(result.longValueExact(), target);
    }

    @Override
    public int compareTo(MediaTimestamp other) {
        Objects.requireNonNull(other, "other");
        BigInteger left = BigInteger.valueOf(ticks)
                .multiply(BigInteger.valueOf(timeBase.numerator()))
                .multiply(BigInteger.valueOf(other.timeBase.denominator()));
        BigInteger right = BigInteger.valueOf(other.ticks)
                .multiply(BigInteger.valueOf(other.timeBase.numerator()))
                .multiply(BigInteger.valueOf(timeBase.denominator()));
        return left.compareTo(right);
    }

    public enum Rounding {
        EXACT,
        TOWARD_ZERO,
        FLOOR,
        CEILING,
        HALF_UP
    }
}
