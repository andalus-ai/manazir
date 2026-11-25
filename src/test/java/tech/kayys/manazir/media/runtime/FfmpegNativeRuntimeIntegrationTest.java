package tech.kayys.manazir.media.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FfmpegNativeRuntimeIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "manazir.native.tests", matches = "true")
    void readsVersionFromAnActuallyLoadedFfmpegLibrary() {
        var runtime = NativeRuntime.system();
        var loaded = runtime.initialize(NativeRuntimeConfig.ffmpegDefaults());
        assumeTrue(loaded.hasLibrary(NativeLibrary.AVUTIL),
                "FFmpeg libavutil is not installed: " + loaded.failures());

        var verification = new NativeRuntimeVerifier(new FfmpegNativeLibraryInspector()).verify(loaded);
        var avutil = verification.libraries().stream()
                .filter(result -> result.library() == NativeLibrary.AVUTIL)
                .findFirst()
                .orElseThrow();

        assertNotNull(avutil.version(), avutil.diagnostic());
        assertTrue(avutil.version().major() > 0);
    }
}
