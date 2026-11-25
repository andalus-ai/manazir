package tech.kayys.manazir.media.extract;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tech.kayys.manazir.media.time.MediaTimestamp;
import tech.kayys.manazir.media.time.RationalTimeBase;

class FrameExtractionContractTest {
    @Test
    void requestRetainsExactSourceTimestampAndNormalizesOutputPath() {
        MediaTimestamp target = new MediaTimestamp(90_090, RationalTimeBase.of(1, 90_000));
        Path output = Path.of("build", "frames", "shot.png");

        FrameExtractionRequest request = FrameExtractionRequest.firstAtOrAfter(
                URI.create("file:///media/source.mp4"), 2, target, output);

        assertEquals(target, request.targetTimestamp());
        assertEquals(FrameSelectionPolicy.FIRST_AT_OR_AFTER, request.selectionPolicy());
        assertTrue(request.output().isAbsolute());
        assertEquals(FrameImageFormat.PNG, request.imageFormat());
    }

    @Test
    void rejectsNegativeStreamIndexAndNonPositiveTimeout() {
        URI source = URI.create("file:///media/source.mp4");
        MediaTimestamp target = new MediaTimestamp(0, RationalTimeBase.of(1, 1_000));

        assertThrows(IllegalArgumentException.class, () -> new FrameExtractionRequest(
                source, -1, target, FrameSelectionPolicy.NEAREST, Path.of("frame.png"),
                FrameImageFormat.PNG, Optional.empty(), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new FrameExtractionRequest(
                source, 0, target, FrameSelectionPolicy.NEAREST, Path.of("frame.png"),
                FrameImageFormat.PNG, Optional.empty(), Duration.ZERO));
    }

    @Test
    void rejectsInvalidOutputDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new FrameSize(0, 720));
        assertThrows(IllegalArgumentException.class, () -> new FrameSize(1280, -1));
    }

    @Test
    void resultNormalizesOutputAndRequiresBackendIdentity() {
        FrameExtractionResult result = new FrameExtractionResult(
                Path.of("build/frame.webp"), FrameImageFormat.WEBP, Optional.empty(),
                Optional.of(new FrameSize(640, 360)), 42, "test-backend");

        assertTrue(result.output().isAbsolute());
        assertEquals("test-backend", result.backendId());
        assertEquals(42, result.outputBytes());
    }
}
