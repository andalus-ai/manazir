package tech.kayys.manazir.media.extract;

/** Backend-neutral still-image formats supported by the extraction contract. */
public enum FrameImageFormat {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg"),
    WEBP("webp", "image/webp");

    private final String extension;
    private final String mediaType;

    FrameImageFormat(String extension, String mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() { return extension; }
    public String mediaType() { return mediaType; }
}
