package tech.kayys.manazir.media.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FfmpegCapabilityInspectorTest {
    @TempDir Path tempDir;

    @Test
    void parsesOperationTablesAndCreatesEvidenceBackedDescriptor() throws Exception {
        Path executable = script("""
                case "$*" in
                  *-version*) echo 'ffmpeg version 7.1-test';;
                  *-decoders*) printf ' V..... h264 H.264 decoder\\n A..... aac AAC decoder\\n';;
                  *-encoders*) printf ' V..... libx264 H.264 encoder\\n A..... aac AAC encoder\\n A..... pcm_s16le PCM encoder\\n';;
                  *-filters*) printf ' ... scale V->V scale video\\n ... aresample A->A resample audio\\n';;
                  *-muxers*) printf ' E mp4 MP4 format\\n E matroska Matroska format\\n E wav WAV format\\n';;
                  *-demuxers*) printf ' D mov MOV format\\n D matroska Matroska format\\n';;
                esac
                """);
        var report = new FfmpegCapabilityInspector(executable.toString(), Duration.ofSeconds(1)).inspect();

        assertEquals("ffmpeg version 7.1-test", report.version());
        assertTrue(report.videoDecoders().contains("h264"));
        assertTrue(report.audioDecoders().contains("aac"));
        assertTrue(report.videoEncoders().contains("libx264"));
        assertTrue(report.audioEncoders().contains("aac"));
        assertTrue(report.filters().contains("scale"));
        assertTrue(report.filters().contains("aresample"));
        assertTrue(report.muxers().contains("mp4"));
        assertTrue(report.demuxers().contains("mov"));
        var descriptor = report.providerDescriptor(10);
        assertEquals(ProviderAvailability.AVAILABLE, descriptor.availability());
        assertTrue(descriptor.supports(MediaCapability.VIDEO_DECODE));
        assertTrue(descriptor.supports(MediaCapability.AUDIO_RESAMPLE));
        assertTrue(descriptor.supports(MediaCapability.AUDIO_EXTRACTION));
        assertFalse(descriptor.supports(MediaCapability.HARDWARE_ACCELERATION));
    }

    @Test
    void doesNotInferCapabilitiesFromExecutablePresenceAlone() throws Exception {
        Path executable = script("""
                case "$*" in
                  *-version*) echo 'ffmpeg version minimal';;
                  *-decoders*|*-encoders*|*-filters*|*-muxers*|*-demuxers*) echo 'no entries';;
                esac
                """);
        var report = new FfmpegCapabilityInspector(executable.toString(), Duration.ofSeconds(1)).inspect();
        assertTrue(report.verifiedCapabilities().isEmpty());
        assertThrows(IllegalStateException.class, () -> report.providerDescriptor(1));
    }

    @Test
    void mapsNonZeroListingCommandToStructuredFailure() throws Exception {
        Path executable = script("""
                case "$*" in
                  *-version*) exit 0;;
                  *-decoders*) exit 9;;
                esac
                """);
        var failure = assertThrows(FfmpegInspectionException.class,
                () -> new FfmpegCapabilityInspector(executable.toString(), Duration.ofSeconds(1)).inspect());
        assertEquals(FfmpegInspectionException.Code.COMMAND_FAILED, failure.code());
    }

    @Test
    void enforcesCommandTimeout() throws Exception {
        Path executable = script("""
                case "$1" in
                  -version) echo 'ffmpeg version test';;
                  *-decoders*) sleep 2;;
                esac
                """);
        var failure = assertThrows(FfmpegInspectionException.class,
                () -> new FfmpegCapabilityInspector(executable.toString(), Duration.ofMillis(50)).inspect());
        assertEquals(FfmpegInspectionException.Code.TIMEOUT, failure.code());
    }

    @Test
    void reportsMissingExecutable() {
        var failure = assertThrows(FfmpegInspectionException.class,
                () -> new FfmpegCapabilityInspector(tempDir.resolve("missing").toString(), Duration.ofSeconds(1)).inspect());
        assertEquals(FfmpegInspectionException.Code.EXECUTABLE_UNAVAILABLE, failure.code());
    }

    private Path script(String body) throws IOException {
        Path path = tempDir.resolve("fake-ffmpeg-" + UUID.randomUUID() + ".sh");
        Files.writeString(path, "#!/bin/sh\n" + body + "\n");
        if (!path.toFile().setExecutable(true)) throw new IOException("cannot make test executable");
        return path;
    }
}
