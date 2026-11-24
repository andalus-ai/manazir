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
    void rejectsInvalidTimeBases() {
        assertThrows(IllegalArgumentException.class, () -> RationalTimeBase.of(0, 1));
        assertThrows(IllegalArgumentException.class, () -> RationalTimeBase.of(1, 0));
    }
}
