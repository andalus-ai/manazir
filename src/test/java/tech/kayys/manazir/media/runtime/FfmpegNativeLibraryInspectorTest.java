package tech.kayys.manazir.media.runtime;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FfmpegNativeLibraryInspectorTest {
    @Test
    void verifiesVersionAndOnlyTheSymbolsForTheSelectedComponent() {
        var symbols = new FakeSymbols(Set.of("avcodec_version",
                "avcodec_find_decoder", "avcodec_send_packet", "avcodec_receive_frame"));
        symbols.versions.put("avcodec_version", 0x3b2a64);

        var inspection = new FfmpegNativeLibraryInspector(symbols).inspect(NativeLibrary.AVCODEC);

        assertEquals("59.42.100", inspection.version().versionString());
        assertEquals(CapabilityStatus.VERIFIED, status(inspection, MediaCapability.VIDEO_DECODE));
        assertEquals(CapabilityStatus.UNAVAILABLE, status(inspection, MediaCapability.VIDEO_ENCODE));
        assertEquals(CapabilityStatus.UNKNOWN, status(inspection, MediaCapability.MEDIA_PROBE));
    }

    @Test
    void reportsMissingVersionSymbolWithoutClaimingCapabilities() {
        var inspection = new FfmpegNativeLibraryInspector(new FakeSymbols(Set.of()))
                .inspect(NativeLibrary.AVFORMAT);

        assertNull(inspection.version());
        assertTrue(inspection.capabilities().isEmpty());
        assertTrue(inspection.diagnostic().contains("avformat_version"));
    }

    @Test
    void rejectsNonPositiveNativeVersionResult() {
        var symbols = new FakeSymbols(Set.of("swscale_version"));
        symbols.versions.put("swscale_version", 0);

        var inspection = new FfmpegNativeLibraryInspector(symbols).inspect(NativeLibrary.SWSCALE);

        assertNull(inspection.version());
        assertTrue(inspection.diagnostic().contains("non-positive"));
    }

    private static CapabilityStatus status(NativeLibraryInspection inspection, MediaCapability capability) {
        return inspection.capabilities().stream()
                .filter(assessment -> assessment.capability() == capability)
                .map(CapabilityAssessment::status)
                .findFirst()
                .orElse(CapabilityStatus.UNKNOWN);
    }

    private static final class FakeSymbols implements NativeSymbolAccess {
        private final Set<String> available;
        private final Map<String, Integer> versions = new HashMap<>();

        private FakeSymbols(Set<String> available) {
            this.available = available;
        }

        @Override
        public boolean hasSymbol(String symbolName) {
            return available.contains(symbolName);
        }

        @Override
        public int invokeInt(String symbolName) {
            if (!available.contains(symbolName)) throw new NativeSymbolAccessException("missing " + symbolName);
            return versions.getOrDefault(symbolName, 0);
        }
    }
}
