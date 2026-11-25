package tech.kayys.manazir.media.extract;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.manazir.media.time.MediaTimestamp;
import tech.kayys.manazir.media.time.RationalTimeBase;

class FfmpegAudioExtractorTest {
    @TempDir Path tempDir;

    @Test
    void rejectsNonLocalSourceBeforeStartingProcess() {
        var request = new AudioExtractionRequest(URI.create("https://example.invalid/audio.wav"), 0,
                at(0), at(1_000), tempDir.resolve("out.wav"), 48_000, 2, Duration.ofSeconds(1), 1_000_000);
        var failure = assertThrows(AudioExtractionException.class,
                () -> new FfmpegAudioExtractor("missing-ffmpeg").extract(request));
        assertEquals(AudioExtractionException.Code.INVALID_SOURCE, failure.code());
    }

    @Test
    void refusesToOverwriteExistingDestination() throws Exception {
        Path source = Files.writeString(tempDir.resolve("source.wav"), "not media");
        Path output = Files.writeString(tempDir.resolve("output.wav"), "keep");
        var request = new AudioExtractionRequest(source.toUri(), 0, at(0), at(1_000),
                output, 48_000, 2, Duration.ofSeconds(1), 1_000_000);
        var failure = assertThrows(AudioExtractionException.class,
                () -> new FfmpegAudioExtractor("missing-ffmpeg").extract(request));
        assertEquals(AudioExtractionException.Code.INVALID_DESTINATION, failure.code());
        assertEquals("keep", Files.readString(output));
    }

    private static MediaTimestamp at(long ticks) {
        return new MediaTimestamp(ticks, RationalTimeBase.of(1, 1_000));
    }
}
