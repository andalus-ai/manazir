package tech.kayys.manazir.media.probe;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tech.kayys.manazir.media.runtime.MediaCapability;
import tech.kayys.manazir.media.runtime.MediaProviderDescriptor;
import tech.kayys.manazir.media.runtime.MediaProviderRegistry;
import tech.kayys.manazir.media.runtime.ProviderAvailability;
import tech.kayys.manazir.media.runtime.ProviderSelectionPolicy;

class SelectingMediaProbeTest {
    @Test
    void delegatesToSelectedProvider() {
        MediaProbe provider = new MediaProbe() {
            @Override public String backendId() { return "probe-a"; }
            @Override public MediaProbeResult probe(MediaProbeRequest request) throws MediaProbeException {
                throw new MediaProbeException(MediaProbeException.Code.PROBE_FAILED,
                        Optional.of(backendId()), "delegated");
            }
        };
        var registry = MediaProviderRegistry.<MediaProbe>builder().register(
                new MediaProviderDescriptor("probe-a", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.AVAILABLE, 10, "verified"), provider).build();
        var facade = new SelectingMediaProbe(registry, Optional.empty(), ProviderSelectionPolicy.STRICT);

        var failure = assertThrows(MediaProbeException.class,
                () -> facade.probe(MediaProbeRequest.defaults(URI.create("file:///tmp/input.mp4"))));
        assertEquals("delegated", failure.getMessage());
        assertEquals(Optional.of("probe-a"), failure.backendId());
    }

    @Test
    void translatesSelectionFailureToBackendUnavailable() {
        var registry = MediaProviderRegistry.<MediaProbe>builder().build();
        var facade = new SelectingMediaProbe(registry, Optional.empty(), ProviderSelectionPolicy.STRICT);

        var failure = assertThrows(MediaProbeException.class,
                () -> facade.probe(MediaProbeRequest.defaults(URI.create("file:///tmp/input.mp4"))));
        assertEquals(MediaProbeException.Code.BACKEND_UNAVAILABLE, failure.code());
        assertTrue(failure.getMessage().contains("No providers are registered"));
    }

    @Test
    void strictPreferredProviderDoesNotFallback() {
        MediaProbe provider = new MediaProbe() {
            @Override public String backendId() { return "other"; }
            @Override public MediaProbeResult probe(MediaProbeRequest request) { return null; }
        };
        var registry = MediaProviderRegistry.<MediaProbe>builder().register(
                new MediaProviderDescriptor("other", Set.of(MediaCapability.MEDIA_PROBE),
                        ProviderAvailability.AVAILABLE, 10, "verified"), provider).build();
        var facade = new SelectingMediaProbe(registry, Optional.of("missing"),
                ProviderSelectionPolicy.STRICT);

        var failure = assertThrows(MediaProbeException.class,
                () -> facade.probe(MediaProbeRequest.defaults(URI.create("file:///tmp/input.mp4"))));
        assertEquals(MediaProbeException.Code.BACKEND_UNAVAILABLE, failure.code());
    }
}
