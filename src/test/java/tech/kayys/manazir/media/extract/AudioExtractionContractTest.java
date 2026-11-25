package tech.kayys.manazir.media.extract;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import tech.kayys.manazir.media.time.MediaTimestamp;
import tech.kayys.manazir.media.time.RationalTimeBase;

class AudioExtractionContractTest {
    private static MediaTimestamp at(long ticks) {
        return new MediaTimestamp(ticks, RationalTimeBase.of(1, 1_000));
    }

    @Test
    void retainsExactIntervalAndNormalizesDestination() {
        AudioExtractionRequest request = AudioExtractionRequest.wav(
                URI.create("file:///tmp/source.mkv"), 2, at(125), at(1_375), Path.of("build/audio.wav"));
        assertEquals(at(125), request.start());
        assertEquals(at(1_375), request.end());
        assertTrue(request.output().isAbsolute());
        assertEquals(48_000, request.sampleRate());
        assertEquals(2, request.channels());
    }

    @Test
    void rejectsInvalidIntervalAndAudioConstraints() {
        URI source = URI.create("file:///tmp/source.mkv");
        assertThrows(IllegalArgumentException.class, () -> new AudioExtractionRequest(
                source, 0, at(100), at(100), Path.of("out.wav"), 48_000, 2, Duration.ofSeconds(1), 1024));
        assertThrows(IllegalArgumentException.class, () -> new AudioExtractionRequest(
                source, 0, at(0), at(100), Path.of("out.wav"), 7_999, 2, Duration.ofSeconds(1), 1024));
        assertThrows(IllegalArgumentException.class, () -> new AudioExtractionRequest(
                source, 0, at(0), at(100), Path.of("out.wav"), 48_000, 9, Duration.ofSeconds(1), 1024));
    }
}
