package tech.kayys.manazir.media.runtime;

/** FFmpeg native library components in dependency-oriented load order. */
public enum NativeLibrary {
    AVUTIL("avutil"),
    SWRESAMPLE("swresample"),
    SWscale("swscale"),
    AVCODEC("avcodec"),
    AVFORMAT("avformat"),
    AVFILTER("avfilter");

    private final String libraryName;

    NativeLibrary(String libraryName) {
        this.libraryName = libraryName;
    }

    public String libraryName() {
        return libraryName;
    }
}
