package tech.kayys.manazir.media.runtime;

import java.util.List;
import java.util.Objects;

/** Immutable result of an explicit native-library initialization attempt. */
public record NativeRuntimeReport(List<LibraryResult> libraries) {
    public NativeRuntimeReport {
        libraries = List.copyOf(Objects.requireNonNull(libraries, "libraries"));
    }

    public boolean successful() {
        return libraries.stream().allMatch(LibraryResult::loaded);
    }

    public List<NativeLibrary> loadedLibraries() {
        return libraries.stream().filter(LibraryResult::loaded).map(LibraryResult::library).toList();
    }

    public boolean hasLibrary(NativeLibrary library) {
        return libraries.stream().anyMatch(result -> result.library() == library && result.loaded());
    }

    public List<LibraryResult> failures() {
        return libraries.stream().filter(result -> !result.loaded()).toList();
    }

    public record LibraryResult(NativeLibrary library, boolean loaded, String diagnostic) {
        public LibraryResult {
            Objects.requireNonNull(library, "library");
            diagnostic = diagnostic == null ? "" : diagnostic;
        }
    }
}
