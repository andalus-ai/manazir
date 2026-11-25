# FFM and native runtime foundation

## Layers

1. Generated jextract declarations: low-level ABI-specific implementation only.
2. Native runtime: explicit loading, diagnostics, version/symbol checks, and lifecycle.
3. Typed media API: stable Java records and service interfaces.
4. Backend adapters: FFM or process-based implementations.
5. Product integrations: Dalang consumes typed APIs only.

## Explicit initialization

Use NativeRuntime.system().initialize(NativeRuntimeConfig.ffmpegDefaults()) to request loading of the configured FFmpeg libraries. Constructing the runtime, config, timestamp, or report types does not load native code.

The default order is avutil, swresample, swscale, avcodec, avformat, avfilter. This is a pragmatic dependency-oriented order, not an ABI compatibility guarantee. The actual supported FFmpeg version matrix and platform packaging still need to be pinned.

## FFmpeg version and symbol inspection

After successful loading, pass the load report to NativeRuntimeVerifier with FfmpegNativeLibraryInspector. The inspector uses FfmNativeSymbolAccess, backed by Java 22 Linker and SymbolLookup.loaderLookup(), to resolve and invoke the component version functions:

- avutil_version
- swresample_version
- swscale_version
- avcodec_version
- avformat_version
- avfilter_version

The version integer is decoded using FFmpeg's AV_VERSION_INT(major, minor, micro) representation. The inspector also resolves selected stable component API symbol groups for probing/demux/mux, codec send/receive, scaling, resampling, and filter graph operations.

The capability registry distinguishes:

- VERIFIED: the required component API symbols resolved.
- UNAVAILABLE: one or more required component API symbols are absent.
- UNKNOWN: no sufficient evidence has been supplied.

This is an API-surface check, not proof that a particular codec, container, pixel format, hardware device, or media file is supported. Codec enumeration, hardware-device discovery, ABI compatibility policy, and real execution probes are still required before making those claims.

The current generated Java snapshot contains only the tech.kayys.manazir.avutil package and does not expose these FFmpeg component version functions. The inspector therefore uses the narrow FFM symbol-access adapter directly. Generated binding provenance and a reproducible FFmpeg-specific jextract pipeline remain open work.

## Native integration tests

Run mvn -Pnative-integration test on a machine with FFmpeg shared libraries installed. This opt-in profile enables native access for the test JVM. The regular unit-test suite uses injected symbol access and does not require native libraries.

## Safety

- Never load native code in static initializers.
- Do not expose FFM MemorySegment, native pointers, or generated structs from public APIs.
- Keep native memory ownership scoped and explicit.
- Use typed options rather than arbitrary command strings.
- Treat native initialization as process-level state; do not claim that libraries can be safely unloaded.
