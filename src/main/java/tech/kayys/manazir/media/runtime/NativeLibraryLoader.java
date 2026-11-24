package tech.kayys.manazir.media.runtime;

/**
 * Loads one native library. Implementations should delegate to the platform
 * loader and must not silently substitute a different ABI.
 */
@FunctionalInterface
public interface NativeLibraryLoader {
    void load(String libraryName);
}
