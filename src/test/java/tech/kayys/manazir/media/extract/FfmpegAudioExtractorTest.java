package tech.kayys.manazir.media.extract;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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

    @Test
    void includesBoundedStderrInStructuredFailure() throws Exception {
        Path source = Files.writeString(tempDir.resolve("source-diagnostic.wav"), "source");
        Path executable = script("printf 'fixture diagnostic: missing stream' >&2\nexit 17\n");
        var request = new AudioExtractionRequest(source.toUri(), 0, at(0), at(1_000),
                tempDir.resolve("diagnostic.wav"), 48_000, 2, Duration.ofSeconds(5), 1_000_000);

        AudioExtractionException failure = assertThrows(AudioExtractionException.class,
                () -> new FfmpegAudioExtractor(executable.toString()).extract(request));

        assertEquals(AudioExtractionException.Code.EXTRACTION_FAILED, failure.code());
        assertTrue(failure.getMessage().contains("fixture diagnostic: missing stream"));
        assertFalse(Files.exists(request.output()));
    }

    @Test
    void terminatesProcessWhenDiagnosticOutputExceedsBound() throws Exception {
        Path source = Files.writeString(tempDir.resolve("source-overflow.wav"), "source");
        Path executable = script("i=0\nwhile [ $i -lt 100 ]; do printf '%1024s' x >&2; i=$((i+1)); done\n");
        var request = new AudioExtractionRequest(source.toUri(), 0, at(0), at(1_000),
                tempDir.resolve("overflow.wav"), 48_000, 2, Duration.ofSeconds(5), 1_000_000);

        AudioExtractionException failure = assertThrows(AudioExtractionException.class,
                () -> new FfmpegAudioExtractor(executable.toString()).extract(request));

        assertEquals(AudioExtractionException.Code.RESOURCE_LIMIT, failure.code());
        assertTrue(failure.getMessage().contains("diagnostic output exceeded"));
        assertFalse(Files.exists(request.output()));
    }

    private Path script(String body) throws Exception {
        Path executable = tempDir.resolve("fake-ffmpeg-" + System.nanoTime() + ".sh");
        Files.writeString(executable, "#!/bin/sh\n" + body, StandardCharsets.UTF_8);
        assertTrue(executable.toFile().setExecutable(true));
        return executable;
    }

    private static MediaTimestamp at(long ticks) {
        return new MediaTimestamp(ticks, RationalTimeBase.of(1, 1_000));
    }
}
