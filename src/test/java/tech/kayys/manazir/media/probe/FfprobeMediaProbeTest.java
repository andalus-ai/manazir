package tech.kayys.manazir.media.probe;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.manazir.media.time.RationalTimeBase;

class FfprobeMediaProbeTest {
    @TempDir Path tempDir;

    @Test
    void parsesContainerAndStreamMetadataWithoutLosingStreamTicks() throws Exception {
        Path source = Files.createFile(tempDir.resolve("fixture.mp4"));
        Path executable = fakeExecutable("""
                printf '%s' '{"format":{"format_name":"mov,mp4","format_long_name":"QuickTime / MOV","start_time":"0.125000","duration":"12.500000"},"streams":[{"index":0,"id":"0x1","codec_type":"video","codec_name":"h264","time_base":"1/90000","start_pts":"11250","duration_ts":"1125000","width":1920,"height":1080,"avg_frame_rate":"30000/1001","r_frame_rate":"25/1"},{"index":1,"codec_type":"audio","codec_name":"aac","time_base":"1/48000","start_pts":"0","duration_ts":"600000","sample_rate":"48000","channels":2}]}'
                """);
        var request = new MediaProbeRequest(source.toUri(), Optional.empty(), Duration.ofSeconds(3), 64 * 1024);

        MediaProbeResult result = new FfprobeMediaProbe(executable.toString()).probe(request);

        assertEquals("mov,mp4", result.container().formatName());
        assertEquals(125_000L, result.container().startTime().orElseThrow().ticks());
        assertEquals(RationalTimeBase.of(1, 1_000_000), result.container().duration().orElseThrow().timeBase());
        assertEquals(2, result.streams().size());
        MediaStreamInfo video = result.streams().getFirst();
        assertEquals(RationalTimeBase.of(1, 90_000), video.timeBase().orElseThrow());
        assertEquals(11_250L, video.startTime().orElseThrow().ticks());
        assertEquals(new FrameRate(30_000, 1_001), video.averageFrameRate().orElseThrow());
        assertEquals(1920, video.width().orElseThrow());
        assertEquals(MediaStreamType.AUDIO, result.streams().get(1).type());
        assertEquals(48_000, result.streams().get(1).sampleRate().orElseThrow());
    }

    @Test
    void missingTimeBaseDoesNotInventTimestampClock() throws Exception {
        Path source = Files.createFile(tempDir.resolve("missing-timebase.mp4"));
        Path executable = fakeExecutable("""
                printf '%s' '{"format":{"format_name":"unknown"},"streams":[{"index":0,"codec_type":"video","codec_name":"h264","start_pts":"5","width":640,"height":360}]}'
                """);
        var request = new MediaProbeRequest(source.toUri(), Optional.empty(), Duration.ofSeconds(3), 64 * 1024);

        MediaProbeResult result = new FfprobeMediaProbe(executable.toString()).probe(request);

        assertTrue(result.streams().getFirst().timeBase().isEmpty());
        assertTrue(result.streams().getFirst().startTime().isEmpty());
        assertEquals("MISSING_STREAM_TIME_BASE", result.diagnostics().getFirst().code());
    }

    @Test
    void incompleteDimensionsRemainAbsentWithDiagnostic() throws Exception {
        Path source = Files.createFile(tempDir.resolve("incomplete-dimensions.mp4"));
        Path executable = fakeExecutable("""
                printf '%s' '{"format":{"format_name":"mp4"},"streams":[{"index":0,"codec_type":"video","width":640}]}'
                """);
        var request = new MediaProbeRequest(source.toUri(), Optional.empty(), Duration.ofSeconds(3), 64 * 1024);

        MediaProbeResult result = new FfprobeMediaProbe(executable.toString()).probe(request);

        assertTrue(result.streams().getFirst().width().isEmpty());
        assertTrue(result.streams().getFirst().height().isEmpty());
        assertEquals("INCOMPLETE_VIDEO_DIMENSIONS", result.diagnostics().getFirst().code());
    }

    @Test
    void rejectsOutputLargerThanRequestBudgetWhileProcessIsRunning() throws Exception {
        Path source = Files.createFile(tempDir.resolve("large.mp4"));
        Path executable = fakeExecutable("head -c 200000 /dev/zero\n");
        var request = new MediaProbeRequest(source.toUri(), Optional.empty(), Duration.ofSeconds(3), 1024);

        MediaProbeException exception = assertThrows(MediaProbeException.class,
                () -> new FfprobeMediaProbe(executable.toString()).probe(request));

        assertEquals(MediaProbeException.Code.RESOURCE_LIMIT, exception.code());
    }

    @Test
    void enforcesProcessTimeout() throws Exception {
        Path source = Files.createFile(tempDir.resolve("timeout.mp4"));
        Path executable = fakeExecutable("sleep 3\n");
        var request = new MediaProbeRequest(source.toUri(), Optional.empty(), Duration.ofMillis(100), 1024);

        MediaProbeException exception = assertThrows(MediaProbeException.class,
                () -> new FfprobeMediaProbe(executable.toString()).probe(request));

        assertEquals(MediaProbeException.Code.TIMEOUT, exception.code());
    }

    @Test
    void refusesNonFileUris() {
        var request = new MediaProbeRequest(URI.create("https://example.invalid/video.mp4"),
                Optional.empty(), Duration.ofSeconds(1), 1024);
        MediaProbeException exception = assertThrows(MediaProbeException.class,
                () -> new FfprobeMediaProbe().probe(request));
        assertEquals(MediaProbeException.Code.INVALID_SOURCE, exception.code());
    }

    private Path fakeExecutable(String script) throws IOException {
        Path executable = tempDir.resolve("fake-ffprobe.sh");
        Files.writeString(executable, "#!/bin/sh\n" + script, StandardCharsets.UTF_8);
        if (!executable.toFile().setExecutable(true)) throw new IOException("Unable to mark fake ffprobe executable");
        return executable;
    }
}