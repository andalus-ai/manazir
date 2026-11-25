package tech.kayys.manazir.media.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.manazir.media.extract.AudioArtifact;
import tech.kayys.manazir.media.extract.AudioExtractionException;
import tech.kayys.manazir.media.extract.AudioExtractionRequest;
import tech.kayys.manazir.media.extract.FfmpegAudioExtractor;
import tech.kayys.manazir.media.time.MediaTimestamp;
import tech.kayys.manazir.media.time.RationalTimeBase;

class AudioExtractionIntegrationTest {
    @TempDir Path tempDir;

    @BeforeEach
    void requireFfmpeg() {
        assumeTrue(commandAvailable("ffmpeg"), "Audio integration tests require ffmpeg on PATH");
    }

    @Test
    void extractsSelectedAudioStreamAndHonorsIntervalSampleRateAndChannels() throws Exception {
        Path source = tempDir.resolve("source.mkv");
        run(List.of("ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                "-f", "lavfi", "-i", "testsrc=size=96x64:rate=25:duration=2",
                "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=44100:duration=2",
                "-f", "lavfi", "-i", "sine=frequency=880:sample_rate=48000:duration=2",
                "-map", "0:v:0", "-map", "1:a:0", "-map", "2:a:0",
                "-c:v", "mpeg4", "-c:a", "pcm_s16le", source.toString()));

        Path output = tempDir.resolve("segment.wav");
        AudioExtractionRequest request = new AudioExtractionRequest(source.toUri(), 2,
                at(250), at(1_250), output, 32_000, 1, Duration.ofSeconds(15), 8 * 1024 * 1024);
        AudioArtifact artifact = new FfmpegAudioExtractor().extract(request);

        assertTrue(Files.isRegularFile(output));
        assertEquals(32_000, artifact.sampleRate());
        assertEquals(1, artifact.channels());
        assertEquals(16, artifact.bitsPerSample());
        assertEquals(2, artifact.streamIndex());
        assertEquals(at(250), artifact.requestedStart());
        assertEquals(at(1_250), artifact.requestedEnd());
        assertTrue(artifact.sampleFrames() >= 31_000 && artifact.sampleFrames() <= 33_000,
                "one-second segment should contain approximately 32000 sample frames");
        assertTrue(artifact.sizeBytes() <= request.maxOutputBytes());
    }

    @Test
    void mapsMissingAudioStreamToStructuredExtractionFailure() throws Exception {
        Path source = tempDir.resolve("video-only.mp4");
        run(List.of("ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                "-f", "lavfi", "-i", "testsrc=size=96x64:rate=25:duration=1",
                "-c:v", "mpeg4", source.toString()));
        var request = AudioExtractionRequest.wav(source.toUri(), 7, at(0), at(500), tempDir.resolve("missing.wav"));
        AudioExtractionException failure = assertThrows(AudioExtractionException.class,
                () -> new FfmpegAudioExtractor().extract(request));
        assertEquals(AudioExtractionException.Code.EXTRACTION_FAILED, failure.code());
    }

    private static MediaTimestamp at(long millis) {
        return new MediaTimestamp(millis, RationalTimeBase.of(1, 1_000));
    }

    private static boolean commandAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command, "-version").redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String run(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            throw new IOException("Command timed out: " + command.getFirst());
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
            throw new IOException("Command failed (" + process.exitValue() + "): "
                    + String.join(" ", command) + "\n" + output);
        }
        return output;
    }
}
