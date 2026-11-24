package tech.kayys.manazir.media.runtime;

import java.util.Objects;

/** Version information reported by a native library's own version function. */
public record NativeLibraryVersion(
        NativeLibrary library,
        int versionNumber,
        int major,
        int minor,
        int micro,
        String versionString) {
    public NativeLibraryVersion {
        Objects.requireNonNull(library, "library");
        if (versionNumber < 0 || major < 0 || minor < 0 || micro < 0) {
            throw new IllegalArgumentException("version components must be non-negative");
        }
        versionString = Objects.requireNonNull(versionString, "versionString");
        if (versionString.isBlank()) throw new IllegalArgumentException("versionString must not be blank");
    }

    /** Decodes FFmpeg's AV_VERSION_INT(major, minor, micro) representation. */
    public static NativeLibraryVersion fromFfmpeg(
            NativeLibrary library, int versionNumber, String versionString) {
        return new NativeLibraryVersion(
                library,
                versionNumber,
                (versionNumber >>> 16) & 0xff,
                (versionNumber >>> 8) & 0xff,
                versionNumber & 0xff,
                versionString);
    }
}
