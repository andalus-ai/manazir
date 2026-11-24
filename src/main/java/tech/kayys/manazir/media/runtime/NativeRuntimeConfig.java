package tech.kayys.manazir.media.runtime;

import java.util.List;
import java.util.Objects;

/** Explicit native runtime configuration. Constructing this value loads nothing. */
public record NativeRuntimeConfig(List<NativeLibrary> libraries, boolean failFast) {
    public NativeRuntimeConfig {
        libraries = List.copyOf(Objects.requireNonNull(libraries, "libraries"));
        if (libraries.isEmpty()) {
            throw new IllegalArgumentException("at least one native library must be configured");
        }
        if (libraries.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("libraries must not contain null");
        }
        if (libraries.stream().distinct().count() != libraries.size()) {
            throw new IllegalArgumentException("native libraries must not contain duplicates");
        }
    }

    public static NativeRuntimeConfig ffmpegDefaults() {
        return new NativeRuntimeConfig(List.of(
                NativeLibrary.AVUTIL,
                NativeLibrary.SWRESAMPLE,
                NativeLibrary.SWscale,
                NativeLibrary.AVCODEC,
                NativeLibrary.AVFORMAT,
                NativeLibrary.AVFILTER), false);
    }
}
