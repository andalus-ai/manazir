package tech.kayys.manazir.media.probe;

import org.junit.jupiter.api.Test;
import tech.kayys.manazir.media.time.RationalTimeBase;
import static org.junit.jupiter.api.Assertions.*;

class MediaProbeContractTest {
    @Test void streamTimestampsRetainTheOriginalTimeBase() {
        var stream = new MediaStream(2, MediaStreamType.VIDEO, "h264",
                new RationalTimeBase(1, 90_000), 9_000L, 180_000L,
                1920, 1080, "yuv420p", null, null, null, null, "25/1", "25/1");
        assertEquals(9_000L, stream.startTimestamp().orElseThrow().ticks());
        assertEquals(new RationalTimeBase(1, 90_000), stream.duration().orElseThrow().timeBase());
    }

    @Test void streamWithoutTimestampMetadataDoesNotInventAClock() {
        var stream = new MediaStream(0, MediaStreamType.AUDIO, "aac", null,
                null, null, null, null, null, "fltp", 48_000, 2, "stereo", null, null);
        assertTrue(stream.startTimestamp().isEmpty());
        assertTrue(stream.duration().isEmpty());
    }

    @Test void timestampTicksCannotExistWithoutTimeBase() {
        assertThrows(IllegalArgumentException.class, () -> new MediaStream(0, MediaStreamType.VIDEO,
                "h264", null, 1L, null, null, null, null, null, null, null, null, null, null));
    }
}
