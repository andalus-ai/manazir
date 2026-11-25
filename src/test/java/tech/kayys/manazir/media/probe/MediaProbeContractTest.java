package tech.kayys.manazir.media.probe;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import tech.kayys.manazir.media.time.MediaTimestamp;
import tech.kayys.manazir.media.time.RationalTimeBase;

class MediaProbeContractTest {
    private static final RationalTimeBase STREAM_TIME_BASE = RationalTimeBase.of(1, 90_000);

    @Test
    void requestRequiresAbsoluteSourceAndPositiveBounds() {
        assertThrows(IllegalArgumentException.class, () ->
                new MediaProbeRequest(URI.create("relative.mp4"), Optional.empty(), Duration.ofSeconds(1), 1024));
        assertThrows(IllegalArgumentException.class, () ->
                new MediaProbeRequest(URI.create("file:///a.mp4"), Optional.empty(), Duration.ZERO, 1024));
        assertThrows(IllegalArgumentException.class, () ->
                new MediaProbeRequest(URI.create("file:///a.mp4"), Optional.empty(), Duration.ofSeconds(1), 0));
        assertEquals(Duration.ofSeconds(15), MediaProbeRequest.defaults(URI.create("file:///a.mp4")).timeout());
    }

    @Test
    void keepsContainerAndStreamClocksIndependentAndAllowsNegativeStart() {
        var streamStart = new MediaTimestamp(-900, STREAM_TIME_BASE);
        var stream = videoStream(0, Optional.of(streamStart), Optional.empty());
        var container = new MediaContainerInfo("mov,mp4", Optional.of("QuickTime / MOV"),
                Optional.empty(), Optional.empty());
        var result = new MediaProbeResult(URI.create("file:///vfr.mp4"), "fixture", container, List.of(stream), List.of());

        assertTrue(result.container().startTime().isEmpty());
        assertEquals(streamStart, result.streams().getFirst().startTime().orElseThrow());
        assertTrue(result.streams().getFirst().duration().isEmpty());
    }

    @Test
    void rejectsDuplicateStreamIndexesAndMismatchedTimestampClocks() {
        var stream = videoStream(0, Optional.empty(), Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new MediaProbeResult(
                URI.create("file:///a.mp4"), "fixture",
                new MediaContainerInfo("mp4", Optional.empty(), Optional.empty(), Optional.empty()),
                List.of(stream, stream), List.of()));
        var wrongClock = new MediaTimestamp(10, RationalTimeBase.of(1, 1_000));
        assertThrows(IllegalArgumentException.class, () -> videoStream(1, Optional.of(wrongClock), Optional.empty()));
    }

    @Test
    void preservesExactFrameRateAndUnknownValues() {
        var stream = videoStream(0, Optional.empty(), Optional.empty());
        assertEquals(Optional.of(new FrameRate(30_000, 1_001)), stream.averageFrameRate());
        assertTrue(stream.nominalFrameRate().isEmpty());
        assertTrue(stream.sampleRate().isEmpty());
    }

    @Test
    void allowsMissingStreamTimeBaseWithoutInventingOne() {
        var stream = new MediaStreamInfo(0, Optional.empty(), MediaStreamType.DATA, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), OptionalInt.empty(), OptionalInt.empty(),
                OptionalInt.empty(), OptionalInt.empty(), Optional.empty(), Optional.empty());
        assertTrue(stream.timeBase().isEmpty());
    }

    @Test
    void exposesStructuredProbeFailures() {
        var error = new MediaProbeException(MediaProbeException.Code.TIMEOUT,
                Optional.of("ffprobe-process"), "probe timed out");
        assertEquals(MediaProbeException.Code.TIMEOUT, error.code());
        assertEquals("ffprobe-process", error.backendId().orElseThrow());
    }

    private static MediaStreamInfo videoStream(int index, Optional<MediaTimestamp> start,
                                                Optional<MediaTimestamp> duration) {
        return new MediaStreamInfo(index, Optional.of(Integer.toString(index)), MediaStreamType.VIDEO,
                Optional.of("h264"), Optional.of(STREAM_TIME_BASE), start, duration,
                OptionalInt.of(1920), OptionalInt.of(1080), OptionalInt.empty(), OptionalInt.empty(),
                Optional.of(new FrameRate(30_000, 1_001)), Optional.empty());
    }
}