package tech.kayys.manazir.media.runtime;

import java.util.List;
import java.util.Set;

/** Immutable capability evidence collected from one FFmpeg executable. */
public record FfmpegCapabilityReport(
        String executable,
        String version,
        Set<String> videoDecoders,
        Set<String> audioDecoders,
        Set<String> videoEncoders,
        Set<String> audioEncoders,
        Set<String> filters,
        Set<String> muxers,
        Set<String> demuxers,
        List<String> diagnostics) {

    public FfmpegCapabilityReport {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("executable must not be blank");
        version = version == null ? "" : version;
        videoDecoders = Set.copyOf(videoDecoders);
        audioDecoders = Set.copyOf(audioDecoders);
        videoEncoders = Set.copyOf(videoEncoders);
        audioEncoders = Set.copyOf(audioEncoders);
        filters = Set.copyOf(filters);
        muxers = Set.copyOf(muxers);
        demuxers = Set.copyOf(demuxers);
        diagnostics = List.copyOf(diagnostics);
    }

    /** Capabilities are inferred only from the corresponding reported operation lists. */
    public Set<MediaCapability> verifiedCapabilities() {
        var result = new java.util.LinkedHashSet<MediaCapability>();
        if (!videoDecoders.isEmpty()) result.add(MediaCapability.VIDEO_DECODE);
        if (!audioDecoders.isEmpty()) result.add(MediaCapability.AUDIO_DECODE);
        if (!audioDecoders.isEmpty() && audioEncoders.contains("pcm_s16le") && muxers.contains("wav")) {
            result.add(MediaCapability.AUDIO_EXTRACTION);
        }
        if (!videoEncoders.isEmpty()) result.add(MediaCapability.VIDEO_ENCODE);
        if (!audioEncoders.isEmpty()) result.add(MediaCapability.AUDIO_ENCODE);
        if (!filters.isEmpty()) result.add(MediaCapability.FILTER_GRAPH);
        if (filters.contains("scale")) result.add(MediaCapability.VIDEO_SCALE);
        if (filters.contains("aresample") || filters.contains("aformat")) result.add(MediaCapability.AUDIO_RESAMPLE);
        if (!muxers.isEmpty()) result.add(MediaCapability.MUX);
        if (!demuxers.isEmpty()) result.add(MediaCapability.DEMUX);
        return Set.copyOf(result);
    }

    public MediaProviderDescriptor providerDescriptor(int priority) {
        Set<MediaCapability> capabilities = verifiedCapabilities();
        if (capabilities.isEmpty()) {
            throw new IllegalStateException("FFmpeg report contains no verified media operation capabilities");
        }
        String evidence = "FFmpeg " + (version.isBlank() ? "(version unavailable)" : version)
                + "; capabilities derived from -decoders/-encoders/-filters/-muxers/-demuxers";
        return new MediaProviderDescriptor("ffmpeg-process", capabilities,
                ProviderAvailability.AVAILABLE, priority, evidence);
    }
}
