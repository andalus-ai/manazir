package tech.kayys.manazir.media.runtime;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NativeRuntimeTest {
    @Test
    void initializationIsExplicitAndLoadsInConfiguredOrder() {
        var loaded = new ArrayList<String>();
        var runtime = new NativeRuntime(loaded::add);

        assertTrue(loaded.isEmpty());
        var report = runtime.initialize(new NativeRuntimeConfig(
                List.of(NativeLibrary.AVUTIL, NativeLibrary.AVCODEC), true));

        assertTrue(report.successful());
        assertTrue(report.hasLibrary(NativeLibrary.AVUTIL));
        assertEquals(List.of(NativeLibrary.AVUTIL, NativeLibrary.AVCODEC), report.loadedLibraries());
        assertEquals(List.of("avutil", "avcodec"), loaded);
    }

    @Test
    void nonFailFastModeCollectsFailuresAndContinues() {
        var attempted = new ArrayList<String>();
        NativeLibraryLoader loader = name -> {
            attempted.add(name);
            if (name.equals("swresample")) {
                throw new UnsatisfiedLinkError("not installed");
            }
        };
        var runtime = new NativeRuntime(loader);

        var report = runtime.initialize(new NativeRuntimeConfig(
                List.of(NativeLibrary.AVUTIL, NativeLibrary.SWRESAMPLE, NativeLibrary.AVCODEC), false));

        assertFalse(report.successful());
        assertEquals(1, report.failures().size());
        assertEquals(List.of("avutil", "swresample", "avcodec"), attempted);
        assertTrue(report.failures().getFirst().diagnostic().contains("not installed"));
    }

    @Test
    void failFastModeReturnsActionablePartialReport() {
        NativeLibraryLoader loader = name -> {
            if (name.equals("avcodec")) throw new UnsatisfiedLinkError("missing shared library");
        };
        var runtime = new NativeRuntime(loader);

        var exception = assertThrows(NativeRuntimeException.class, () ->
                runtime.initialize(new NativeRuntimeConfig(
                        List.of(NativeLibrary.AVUTIL, NativeLibrary.AVCODEC, NativeLibrary.AVFORMAT), true)));

        assertEquals(2, exception.report().libraries().size());
        assertFalse(exception.report().successful());
    }

    @Test
    void rejectsDuplicateAndEmptyLibraryLists() {
        assertThrows(IllegalArgumentException.class,
                () -> new NativeRuntimeConfig(List.of(), false));
        assertThrows(IllegalArgumentException.class,
                () -> new NativeRuntimeConfig(List.of(NativeLibrary.AVUTIL, NativeLibrary.AVUTIL), false));
    }
}
