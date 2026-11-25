package tech.kayys.manazir.media.extract;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
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

class SelectingAudioExtractorTest {
    @Test
    void delegatesToExplicitlyAvailableAudioProvider() throws Exception {
        AudioArtifact expected = new AudioArtifact(Path.of("out.wav"), "audio/wav", 1044,
                48_000, 2, 16, 250, at(0), at(250), 1, "fake-audio");
        AudioExtractor provider = new AudioExtractor() {
            @Override public String backendId() { return "fake-audio"; }
            @Override public AudioArtifact extract(AudioExtractionRequest request) { return expected; }
        };
        var registry = MediaProviderRegistry.<AudioExtractor>builder()
                .register(new MediaProviderDescriptor("fake-audio", Set.of(MediaCapability.AUDIO_EXTRACTION),
                        ProviderAvailability.AVAILABLE, 10, "test provider"), provider)
                .build();
        var selector = new SelectingAudioExtractor(registry, Optional.empty(), ProviderSelectionPolicy.STRICT);

        assertSame(expected, selector.extract(request()));
    }

    @Test
    void mapsMissingAudioCapabilityToStructuredBackendUnavailable() {
        var registry = MediaProviderRegistry.<AudioExtractor>builder()
                .register(new MediaProviderDescriptor("probe-only", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.AVAILABLE, 10, "not an audio provider"), new AudioExtractor() {
                    @Override public String backendId() { return "probe-only"; }
                    @Override public AudioArtifact extract(AudioExtractionRequest request) {
                        throw new AssertionError("provider must not be invoked");
                    }
                }).build();
        var selector = new SelectingAudioExtractor(registry, Optional.empty(), ProviderSelectionPolicy.STRICT);

        AudioExtractionException failure = assertThrows(AudioExtractionException.class,
                () -> selector.extract(request()));

        assertEquals(AudioExtractionException.Code.BACKEND_UNAVAILABLE, failure.code());
        assertTrue(failure.getMessage().contains("does not support AUDIO_EXTRACTION"));
    }

    private static AudioExtractionRequest request() {
        return AudioExtractionRequest.wav(URI.create("file:///tmp/source.mkv"), 1,
                at(0), at(250), Path.of("out.wav"));
    }

    private static MediaTimestamp at(long millis) {
        return new MediaTimestamp(millis, RationalTimeBase.of(1, 1_000));
    }
}
