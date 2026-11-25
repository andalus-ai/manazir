package tech.kayys.manazir.media.probe;

public class MediaProbeException extends Exception {
    public enum Reason { INVALID_INPUT, BACKEND_UNAVAILABLE, TIMEOUT, PROCESS_FAILED, OUTPUT_TOO_LARGE, INVALID_OUTPUT }
    private final Reason reason;
    public MediaProbeException(Reason reason, String message) { super(message); this.reason = reason; }
    public MediaProbeException(Reason reason, String message, Throwable cause) { super(message, cause); this.reason = reason; }
    public Reason reason() { return reason; }
}
