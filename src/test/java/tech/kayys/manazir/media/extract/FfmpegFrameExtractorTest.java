package tech.kayys.manazir.media.extract;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.manazir.media.time.MediaTimestamp;
import tech.kayys.manazir.media.time.RationalTimeBase;

class FfmpegFrameExtractorTest {
    @TempDir Path tempDir;

    @Test
    void extractsAndPublishesValidatedFrame() throws Exception {
        Path source = Files.createFile(tempDir.resolve("source.mp4"));
        Path fixture = pngFixture("fixture.png", 64, 36);
        Path destination = tempDir.resolve("frame.png");
        Path executable = fakeExecutable("for last do :; done\ncp '" + fixture + "' \"$last\"\n");
        var request = request(source, destination, executable, Duration.ofSeconds(3), 64 * 1024);

        FrameArtifact artifact = new FfmpegFrameExtractor(executable.toString()).extract(request);

        assertEquals(destination, artifact.path());
        assertEquals("image/png", artifact.contentType());
        assertEquals(64, artifact.width());
        assertEquals(36, artifact.height());
        assertTrue(artifact.sizeBytes() > 0);
        assertEquals(new MediaTimestamp(90_000, RationalTimeBase.of(1, 90_000)), artifact.requestedPosition());
        assertTrue(Files.isRegularFile(destination));
    }

    @Test
    void refusesToOverwriteExistingDestination() throws Exception {
        Path source = Files.createFile(tempDir.resolve("source-existing.mp4"));
        Path executable = fakeExecutable("exit 0\n");
        Path destination = Files.writeString(tempDir.resolve("existing.png"), "keep");
        FrameExtractionException exception = assertThrows(FrameExtractionException.class,
                () -> new FfmpegFrameExtractor(executable.toString()).extract(request(source, destination, executable, Duration.ofSeconds(2), 1024)));
        assertEquals(FrameExtractionException.Code.INVALID_DESTINATION, exception.code());
        assertEquals("keep", Files.readString(destination));
    }

    @Test
    void enforcesOutputSizeAfterFfmpegFinishes() throws Exception {
        Path source = Files.createFile(tempDir.resolve("source-size.mp4"));
        Path fixture = pngFixture("large.png", 64, 36);
        Path executable = fakeExecutable("for last do :; done\ncp '" + fixture + "' \"$last\"\n");
        FrameExtractionException exception = assertThrows(FrameExtractionException.class,
                () -> new FfmpegFrameExtractor(executable.toString()).extract(request(source,
                        tempDir.resolve("too-large.png"), executable, Duration.ofSeconds(3), 1)));
        assertEquals(FrameExtractionException.Code.RESOURCE_LIMIT, exception.code());
    }

    @Test
    void enforcesProcessTimeoutAndCleansTemporaryOutput() throws Exception {
        Path source = Files.createFile(tempDir.resolve("source-timeout.mp4"));
        Path executable = fakeExecutable("sleep 3\n");
        Path destination = tempDir.resolve("timeout.png");
        FrameExtractionException exception = assertThrows(FrameExtractionException.class,
                () -> new FfmpegFrameExtractor(executable.toString()).extract(request(source,
                        destination, executable, Duration.ofMillis(100), 1024)));
        assertEquals(FrameExtractionException.Code.TIMEOUT, exception.code());
        assertFalse(Files.exists(destination));
        try (var files = Files.list(tempDir)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".manazir-frame-")));
        }
    }

    @Test
    void rejectsRemoteSourceUris() throws Exception {
        Path executable = fakeExecutable("exit 0\n");
        var request = new FrameExtractionRequest(URI.create("https://example.invalid/video.mp4"), 0,
                new MediaTimestamp(0, RationalTimeBase.of(1, 1)), tempDir.resolve("remote.png"),
                FrameImageFormat.PNG, Duration.ofSeconds(1), 1024);
        FrameExtractionException exception = assertThrows(FrameExtractionException.class,
                () -> new FfmpegFrameExtractor(executable.toString()).extract(request));
        assertEquals(FrameExtractionException.Code.INVALID_SOURCE, exception.code());
    }

    private FrameExtractionRequest request(Path source, Path destination, Path executable, Duration timeout, long maxBytes) {
        return new FrameExtractionRequest(source.toUri(), 0,
                new MediaTimestamp(90_000, RationalTimeBase.of(1, 90_000)), destination.toAbsolutePath(),
                FrameImageFormat.PNG, timeout, maxBytes);
    }

    private Path pngFixture(String name, int width, int height) throws IOException {
        Path path = tempDir.resolve(name);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", path.toFile());
        return path;
    }

    private Path fakeExecutable(String script) throws IOException {
        Path executable = tempDir.resolve("fake-ffmpeg-" + System.nanoTime() + ".sh");
        Files.writeString(executable, "#!/bin/sh\n" + script);
        if (!executable.toFile().setExecutable(true)) throw new IOException("Unable to mark fake FFmpeg executable");
        return executable;
    }
}
