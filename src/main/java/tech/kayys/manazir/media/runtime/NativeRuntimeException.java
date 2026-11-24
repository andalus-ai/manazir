package tech.kayys.manazir.media.runtime;

/** Raised when fail-fast native initialization cannot load a required library. */
public final class NativeRuntimeException extends RuntimeException {
    private final NativeRuntimeReport report;

    public NativeRuntimeException(NativeRuntimeReport report) {
        super("Unable to initialize FFmpeg native runtime: " + report.failures());
        this.report = report;
    }

    public NativeRuntimeReport report() {
        return report;
    }
}
