package tech.kayys.manazir.media.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MediaProviderRegistryTest {
    private static MediaProviderDescriptor descriptor(
            String id, Set<MediaCapability> capabilities, ProviderAvailability availability, int priority) {
        return new MediaProviderDescriptor(id, capabilities, availability, priority, "test evidence");
    }

    @Test
    void failsClearlyWhenNoProvidersAreRegistered() {
        var registry = MediaProviderRegistry.<String>builder().build();
        var failure = assertThrows(ProviderSelectionException.class,
                () -> registry.select(MediaCapability.MEDIA_PROBE, Optional.empty(),
                        ProviderSelectionPolicy.STRICT));
        assertEquals(MediaCapability.MEDIA_PROBE, failure.capability());
    }

    @Test
    void refusesUnknownAvailabilityWithoutImplicitActivation() {
        var registry = MediaProviderRegistry.<String>builder()
                .register(descriptor("native-ffm", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.UNKNOWN, 100), "native")
                .build();

        var failure = assertThrows(ProviderSelectionException.class,
                () -> registry.select(MediaCapability.MEDIA_PROBE, Optional.empty(),
                        ProviderSelectionPolicy.ALLOW_FALLBACK));

        assertTrue(failure.getMessage().contains("No available provider"));
        assertTrue(failure.diagnostics().getFirst().contains("availability is unknown"));
    }

    @Test
    void choosesDeterministicallyByPriorityThenProviderId() {
        var registry = MediaProviderRegistry.<String>builder()
                .register(descriptor("z-process", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.AVAILABLE, 10), "z")
                .register(descriptor("a-process", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.AVAILABLE, 10), "a")
                .register(descriptor("high-priority", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.AVAILABLE, 20), "high")
                .build();

        var selected = registry.select(MediaCapability.MEDIA_PROBE, Optional.empty(),
                ProviderSelectionPolicy.STRICT);

        assertEquals("high-priority", selected.descriptor().providerId());
        assertEquals("high", selected.provider());
    }

    @Test
    void strictExplicitSelectionNeverSilentlyFallsBack() {
        var registry = MediaProviderRegistry.<String>builder()
                .register(descriptor("ffprobe-process", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.UNAVAILABLE, 10), "ffprobe")
                .register(descriptor("native", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.AVAILABLE, 20), "native")
                .build();

        var failure = assertThrows(ProviderSelectionException.class,
                () -> registry.select(MediaCapability.MEDIA_PROBE, Optional.of("ffprobe-process"),
                        ProviderSelectionPolicy.STRICT));

        assertTrue(failure.diagnostics().getFirst().contains("unavailable"));
    }

    @Test
    void fallbackMustBeExplicitAndIsReported() {
        var registry = MediaProviderRegistry.<String>builder()
                .register(descriptor("ffprobe-process", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.UNAVAILABLE, 10), "ffprobe")
                .register(descriptor("native", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.AVAILABLE, 20), "native")
                .build();

        var selected = registry.select(MediaCapability.MEDIA_PROBE, Optional.of("ffprobe-process"),
                ProviderSelectionPolicy.ALLOW_FALLBACK);

        assertEquals("native", selected.descriptor().providerId());
        assertTrue(selected.diagnostics().stream().anyMatch(value -> value.contains("Fell back")));
    }

    @Test
    void rejectsProviderThatDoesNotSupportRequestedCapability() {
        var registry = MediaProviderRegistry.<String>builder()
                .register(descriptor("ffprobe-process", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.AVAILABLE, 10), "ffprobe")
                .build();

        var failure = assertThrows(ProviderSelectionException.class,
                () -> registry.select(MediaCapability.VIDEO_DECODE, Optional.empty(),
                        ProviderSelectionPolicy.STRICT));

        assertTrue(failure.diagnostics().stream().anyMatch(value -> value.contains("does not support VIDEO_DECODE")));
    }

    @Test
    void rejectsDuplicateProviderIds() {
        var builder = MediaProviderRegistry.<String>builder()
                .register(descriptor("same", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.AVAILABLE, 1), "one");

        assertThrows(IllegalArgumentException.class, () -> builder.register(
                descriptor("same", Set.of(MediaCapability.VIDEO_DECODE),
                        ProviderAvailability.AVAILABLE, 1), "two"));
    }
}
