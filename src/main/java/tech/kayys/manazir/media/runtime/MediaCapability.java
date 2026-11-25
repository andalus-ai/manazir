package tech.kayys.manazir.media.runtime;

/** Public media operations that a native backend may explicitly verify. */
public enum MediaCapability {
    MEDIA_PROBE,
    VIDEO_DECODE,
    VIDEO_ENCODE,
    AUDIO_DECODE,
    AUDIO_EXTRACTION,
    AUDIO_ENCODE,
    AUDIO_RESAMPLE,
    VIDEO_SCALE,
    FILTER_GRAPH,
    MUX,
    DEMUX,
    HARDWARE_ACCELERATION
}
