package tech.kayys.manazir.media.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NativeRuntimeVerifierTest {
    @Test
    void inspectsOnlyLoadedLibrariesAndLeavesUnprovenCapabilitiesUnknown() {
        var runtimeReport = new NativeRuntimeReport(List.of(
                new NativeRuntimeReport.LibraryResult(NativeLibrary.AVUTIL, true, ""),
                new NativeRuntimeReport.LibraryResult(NativeLibrary.AVCODEC, false, "missing")));
        var verifier = new NativeRuntimeVerifier(library -> new NativeLibraryInspection(
                library,
                NativeLibraryVersion.fromFfmpeg(library, 0x3b2a64, "59.42.100"),
                library == NativeLibrary.AVCODEC
                        ? List.of(new CapabilityAssessment(MediaCapability.VIDEO_DECODE,
                                CapabilityStatus.VERIFIED, "required decode symbols resolved"))
                        : List.of(),
                ""));

        var report = verifier.verify(runtimeReport);

        assertEquals(List.of(NativeLibrary.AVUTIL), report.libraries().stream()
                .map(NativeLibraryInspection::library).toList());
        assertTrue(report.capabilities().isVerified(MediaCapability.VIDEO_DECODE) == false);
        assertEquals(CapabilityStatus.UNKNOWN,
                report.capabilities().assessment(MediaCapability.VIDEO_DECODE).status());
    }

    @Test
    void recordsOnlyExplicitlyVerifiedCapabilities() {
        var runtimeReport = new NativeRuntimeReport(List.of(
                new NativeRuntimeReport.LibraryResult(NativeLibrary.AVCODEC, true, "")));
        var verifier = new NativeRuntimeVerifier(library -> new NativeLibraryInspection(
                library,
                NativeLibraryVersion.fromFfmpeg(library, 0x3b2a64, "59.42.100"),
                List.of(new CapabilityAssessment(MediaCapability.VIDEO_DECODE,
                        CapabilityStatus.VERIFIED, "version and required symbols checked")),
                ""));

        var report = verifier.verify(runtimeReport);

        assertTrue(report.capabilities().isVerified(MediaCapability.VIDEO_DECODE));
        assertEquals(CapabilityStatus.UNKNOWN,
                report.capabilities().assessment(MediaCapability.VIDEO_ENCODE).status());
        assertEquals(59, report.libraries().getFirst().version().major());
        assertEquals(42, report.libraries().getFirst().version().minor());
        assertEquals(100, report.libraries().getFirst().version().micro());
    }

    @Test
    void rejectsMismatchedLibraryInspection() {
        var runtimeReport = new NativeRuntimeReport(List.of(
                new NativeRuntimeReport.LibraryResult(NativeLibrary.AVUTIL, true, "")));
        var verifier = new NativeRuntimeVerifier(library -> new NativeLibraryInspection(
                NativeLibrary.AVCODEC, null, List.of(), "wrong library"));

        assertThrows(IllegalStateException.class, () -> verifier.verify(runtimeReport));
    }

    @Test
    void decodesFfmpegVersionInteger() {
        var version = NativeLibraryVersion.fromFfmpeg(NativeLibrary.AVUTIL, 0x3b2a64, "59.42.100");
        assertEquals(59, version.major());
        assertEquals(42, version.minor());
        assertEquals(100, version.micro());
    }
}
