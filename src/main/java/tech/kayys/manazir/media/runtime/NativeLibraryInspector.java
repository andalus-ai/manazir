package tech.kayys.manazir.media.runtime;

/**
 * Adapter SPI for querying native version functions and required symbols.
 * Implementations may use generated FFM bindings or another verified backend.
 */
@FunctionalInterface
public interface NativeLibraryInspector {
    NativeLibraryInspection inspect(NativeLibrary library);
}
