package tech.kayys.manazir.media.extract;

/** Requested output dimensions in pixels. */
public record FrameSize(int width, int height) {
    public FrameSize {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("frame dimensions must be positive");
        }
    }
}
