package tech.kayys.manazir.media.runtime;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Performs native initialization only when {@link #initialize} is explicitly
 * called. Merely referencing Manazir API classes never loads native libraries.
 */
public final class NativeRuntime {
    private final NativeLibraryLoader loader;

    public NativeRuntime(NativeLibraryLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    public static NativeRuntime system() {
        return new NativeRuntime(System::loadLibrary);
    }

    public NativeRuntimeReport initialize(NativeRuntimeConfig config) {
        Objects.requireNonNull(config, "config");
        var results = new ArrayList<NativeRuntimeReport.LibraryResult>();

        for (NativeLibrary library : config.libraries()) {
            try {
                loader.load(library.libraryName());
                results.add(new NativeRuntimeReport.LibraryResult(library, true, ""));
            } catch (LinkageError | SecurityException failure) {
                results.add(new NativeRuntimeReport.LibraryResult(
                        library, false, failure.getClass().getSimpleName() + ": "
                        + Objects.toString(failure.getMessage(), "no diagnostic supplied")));
                if (config.failFast()) {
                    break;
                }
            }
        }

        var report = new NativeRuntimeReport(results);
        if (config.failFast() && !report.successful()) {
            throw new NativeRuntimeException(report);
        }
        return report;
    }
}
