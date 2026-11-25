package tech.kayys.manazir.media.extract;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tech.kayys.manazir.media.runtime.MediaCapability;
import tech.kayys.manazir.media.runtime.MediaProviderDescriptor;
import tech.kayys.manazir.media.runtime.MediaProviderRegistry;
import tech.kayys.manazir.media.runtime.ProviderAvailability;
import tech.kayys.manazir.media.runtime.ProviderSelectionPolicy;
import tech.kayys.manazir.media.time.MediaTimestamp;
import tech.kayys.manazir.media.time.RationalTimeBase;

class SelectingFrameExtractorTest {
    @Test
    void delegatesToVideoDecodeProvider() {
        FrameExtractor provider = request -> {
            throw new FrameExtractionException(FrameExtractionException.Code.EXTRACTION_FAILED,
                    Optional.of("extractor-a"), "delegated");
        };
        var registry = MediaProviderRegistry.<FrameExtractor>builder().register(
                new MediaProviderDescriptor("extractor-a", Set.of(MediaCapability.VIDEO_DECODE),
                        ProviderAvailability.AVAILABLE, 10, "verified"), provider).build();
        var facade = new SelectingFrameExtractor(registry, Optional.empty(), ProviderSelectionPolicy.STRICT);

        var failure = assertThrows(FrameExtractionException.class, () -> facade.extract(request()));
        assertEquals("delegated", failure.getMessage());
        assertEquals(Optional.of("extractor-a"), failure.backendId());
    }

    @Test
    void translatesMissingDecodeProviderToBackendUnavailable() {
        var facade = new SelectingFrameExtractor(
                MediaProviderRegistry.<FrameExtractor>builder().build(),
                Optional.empty(), ProviderSelectionPolicy.STRICT);
        var failure = assertThrows(FrameExtractionException.class, () -> facade.extract(request()));
        assertEquals(FrameExtractionException.Code.BACKEND_UNAVAILABLE, failure.code());
        assertTrue(failure.getMessage().contains("No providers are registered"));
    }

    private static FrameExtractionRequest request() {
        return FrameExtractionRequest.firstAtOrAfter(URI.create("file:///tmp/input.mp4"), 0,
                new MediaTimestamp(0, RationalTimeBase.of(1, 1_000)), Path.of("frame.png"));
    }
}
