package tech.kayys.manazir.media.extract;

/** Supported still-image containers for single-frame extraction. */
public enum FrameImageFormat {
    PNG("png", "image/png", "png"),
    JPEG("jpg", "image/jpeg", "mjpeg");

    private final String extension;
    private final String contentType;
    private final String ffmpegCodec;

    FrameImageFormat(String extension, String contentType, String ffmpegCodec) {
        this.extension = extension;
        this.contentType = contentType;
        this.ffmpegCodec = ffmpegCodec;
    }

    public String extension() { return extension; }
    public String contentType() { return contentType; }
    String ffmpegCodec() { return ffmpegCodec; }
}
