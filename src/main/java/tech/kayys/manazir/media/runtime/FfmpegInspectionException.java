package tech.kayys.manazir.media.runtime;

/** Structured failure while inspecting an FFmpeg executable's capability tables. */
public final class FfmpegInspectionException extends Exception {
    private final Code code;

    public FfmpegInspectionException(Code code, String message) {
        super(message);
        this.code = java.util.Objects.requireNonNull(code, "code");
    }

    public FfmpegInspectionException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = java.util.Objects.requireNonNull(code, "code");
    }

    public Code code() { return code; }

    public enum Code {
        EXECUTABLE_UNAVAILABLE,
        COMMAND_FAILED,
        TIMEOUT,
        OUTPUT_LIMIT,
        INTERRUPTED
    }
}
