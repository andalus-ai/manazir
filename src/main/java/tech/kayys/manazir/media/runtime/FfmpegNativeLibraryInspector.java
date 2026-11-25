package tech.kayys.manazir.media.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Inspects the FFmpeg component version entry point and selected stable API
 * symbol groups. Codec availability for a particular codec and hardware
 * acceleration are intentionally not inferred from API symbol presence.
 */
public final class FfmpegNativeLibraryInspector implements NativeLibraryInspector {
    private final NativeSymbolAccess symbols;

    public FfmpegNativeLibraryInspector() {
        this(new FfmNativeSymbolAccess());
    }

    public FfmpegNativeLibraryInspector(NativeSymbolAccess symbols) {
        this.symbols = Objects.requireNonNull(symbols, "symbols");
    }

    @Override
    public NativeLibraryInspection inspect(NativeLibrary library) {
        Objects.requireNonNull(library, "library");
        String versionSymbol = versionSymbol(library);
        if (!symbols.hasSymbol(versionSymbol)) {
            return new NativeLibraryInspection(library, null, List.of(),
                    "Required FFmpeg version symbol is missing: " + versionSymbol);
        }

        final int versionNumber;
        try {
            versionNumber = symbols.invokeInt(versionSymbol);
        } catch (RuntimeException failure) {
            return new NativeLibraryInspection(library, null, List.of(),
                    "Unable to invoke " + versionSymbol + ": " + failure.getMessage());
        }

        if (versionNumber <= 0) {
            return new NativeLibraryInspection(library, null, List.of(),
                    "FFmpeg version function returned a non-positive value: " + versionNumber);
        }

        var version = NativeLibraryVersion.fromFfmpeg(
                library, versionNumber, formatVersion(versionNumber));
        var assessments = new ArrayList<CapabilityAssessment>();
        for (var check : checksFor(library)) {
            var missing = check.requiredSymbols().stream().filter(symbol -> !symbols.hasSymbol(symbol)).toList();
            if (missing.isEmpty()) {
                assessments.add(new CapabilityAssessment(check.capability(), CapabilityStatus.VERIFIED,
                        "Component API symbols resolved: " + String.join(", ", check.requiredSymbols())));
            } else {
                assessments.add(new CapabilityAssessment(check.capability(), CapabilityStatus.UNAVAILABLE,
                        "Missing component API symbols: " + String.join(", ", missing)));
            }
        }
        return new NativeLibraryInspection(library, version, assessments, "");
    }

    private static String versionSymbol(NativeLibrary library) {
        return switch (library) {
            case AVUTIL -> "avutil_version";
            case SWRESAMPLE -> "swresample_version";
            case SWSCALE -> "swscale_version";
            case AVCODEC -> "avcodec_version";
            case AVFORMAT -> "avformat_version";
            case AVFILTER -> "avfilter_version";
        };
    }

    private static String formatVersion(int version) {
        int major = (version >>> 16) & 0xff;
        int minor = (version >>> 8) & 0xff;
        int micro = version & 0xff;
        return major + "." + minor + "." + micro;
    }

    private static List<SymbolCheck> checksFor(NativeLibrary library) {
        return switch (library) {
            case AVUTIL -> List.of();
            case SWRESAMPLE -> List.of(new SymbolCheck(MediaCapability.AUDIO_RESAMPLE,
                    List.of("swr_alloc", "swr_init", "swr_convert", "swr_free")));
            case SWSCALE -> List.of(new SymbolCheck(MediaCapability.VIDEO_SCALE,
                    List.of("sws_getContext", "sws_scale", "sws_freeContext")));
            case AVCODEC -> List.of(
                    new SymbolCheck(MediaCapability.VIDEO_DECODE,
                            List.of("avcodec_find_decoder", "avcodec_send_packet", "avcodec_receive_frame")),
                    new SymbolCheck(MediaCapability.AUDIO_DECODE,
                            List.of("avcodec_find_decoder", "avcodec_send_packet", "avcodec_receive_frame")),
                    new SymbolCheck(MediaCapability.VIDEO_ENCODE,
                            List.of("avcodec_find_encoder", "avcodec_send_frame", "avcodec_receive_packet")),
                    new SymbolCheck(MediaCapability.AUDIO_ENCODE,
                            List.of("avcodec_find_encoder", "avcodec_send_frame", "avcodec_receive_packet")));
            case AVFORMAT -> List.of(
                    new SymbolCheck(MediaCapability.MEDIA_PROBE,
                            List.of("avformat_open_input", "avformat_find_stream_info", "avformat_close_input")),
                    new SymbolCheck(MediaCapability.DEMUX,
                            List.of("avformat_open_input", "av_read_frame", "avformat_close_input")),
                    new SymbolCheck(MediaCapability.MUX,
                            List.of("avformat_alloc_output_context2", "avformat_write_header",
                                    "av_interleaved_write_frame", "av_write_trailer")));
            case AVFILTER -> List.of(new SymbolCheck(MediaCapability.FILTER_GRAPH,
                    List.of("avfilter_graph_alloc", "avfilter_graph_create_filter",
                            "avfilter_graph_config", "avfilter_graph_free")));
        };
    }

    private record SymbolCheck(MediaCapability capability, List<String> requiredSymbols) {
        private SymbolCheck {
            Objects.requireNonNull(capability, "capability");
            requiredSymbols = List.copyOf(requiredSymbols);
        }
    }
}
