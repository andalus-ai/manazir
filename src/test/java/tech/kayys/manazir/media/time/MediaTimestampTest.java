package tech.kayys.manazir.media.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaTimestampTest {
    @Test
    void comparesTimestampsAcrossDifferentTimeBases() {
        var frame = new MediaTimestamp(25, RationalTimeBase.of(1, 25));
        var second = new MediaTimestamp(1, RationalTimeBase.of(1, 1));
        assertEquals(0, frame.compareTo(second));
    }

    @Test
    void comparesNegativeTimestampsAcrossDifferentTimeBases() {
        var frames = new MediaTimestamp(-24, RationalTimeBase.of(1, 24));
        var milliseconds = new MediaTimestamp(-1_000, RationalTimeBase.of(1, 1_000));
        assertEquals(0, frames.compareTo(milliseconds));
    }

    @Test
    void rescalesExactly() {
        var source = new MediaTimestamp(90_000, RationalTimeBase.of(1, 90_000));
        assertEquals(new MediaTimestamp(25, RationalTimeBase.of(1, 25)),
                source.rescaleTo(RationalTimeBase.of(1, 25), MediaTimestamp.Rounding.EXACT));
    }

    @Test
    void rejectsInexactConversionWhenExactRequested() {
        var source = new MediaTimestamp(1, RationalTimeBase.of(1, 24));
        assertThrows(ArithmeticException.class, () ->
                source.rescaleTo(RationalTimeBase.of(1, 25), MediaTimestamp.Rounding.EXACT));
    }

    @Test
    void floorsNegativeValuesMathematically() {
        var source = new MediaTimestamp(-1, RationalTimeBase.of(1, 24));
        assertEquals(-2, source.rescaleTo(
                RationalTimeBase.of(1, 48), MediaTimestamp.Rounding.FLOOR).ticks());
    }

    @Test
    void roundsTiesAwayFromZeroForHalfUp() {
        var positive = new MediaTimestamp(1, RationalTimeBase.of(1, 2));
        var negative = new MediaTimestamp(-1, RationalTimeBase.of(1, 2));
        var target = RationalTimeBase.of(1, 1);
        assertEquals(1, positive.rescaleTo(target, MediaTimestamp.Rounding.HALF_UP).ticks());
        assertEquals(-1, negative.rescaleTo(target, MediaTimestamp.Rounding.HALF_UP).ticks());
    }

    @Test
    void roundsTowardZeroForNegativeValues() {
        var source = new MediaTimestamp(-3, RationalTimeBase.of(1, 4));
        assertEquals(-1, source.rescaleTo(
                RationalTimeBase.of(1, 2), MediaTimestamp.Rounding.TOWARD_ZERO).ticks());
    }

    @Test
    void detectsLongOverflowWhenRescaling() {
        var source = new MediaTimestamp(Long.MAX_VALUE, RationalTimeBase.of(2, 1));
        assertThrows(ArithmeticException.class, () ->
                source.rescaleTo(RationalTimeBase.of(1, 1), MediaTimestamp.Rounding.EXACT));
    }

    @Test
    void rejectsInvalidTimeBases() {
        assertThrows(IllegalArgumentException.class, () -> RationalTimeBase.of(0, 1));
        assertThrows(IllegalArgumentException.class, () -> RationalTimeBase.of(1, 0));
        assertThrows(IllegalArgumentException.class, () -> RationalTimeBase.of(-1, 1));
    }
}
