# FFM and native runtime foundation

## Layers

1. Generated jextract declarations: low-level ABI-specific implementation only.
2. Native runtime: explicit loading, diagnostics, version/symbol checks, and lifecycle.
3. Typed media API: stable Java records and service interfaces.
4. Backend adapters: FFM or process-based implementations.
5. Product integrations: Dalang consumes typed APIs only.

## Explicit initialization

Use `NativeRuntime.system().initialize(NativeRuntimeConfig.ffmpegDefaults())` to request loading of the configured FFmpeg libraries. Constructing the runtime, config, timestamp, or report types does not load native code.

The default order is avutil, swresample, swscale, avcodec, avformat, avfilter. This is a pragmatic dependency-oriented order, not an ABI compatibility guarantee. The actual supported FFmpeg version matrix and platform packaging still need to be pinned.

In non-fail-fast mode, initialization attempts every configured library and returns a report containing successful loads and diagnostics for failures. In fail-fast mode, the first failure stops initialization and throws `NativeRuntimeException` with the partial report.

## Verification and capability registry

`NativeRuntimeVerifier` accepts a `NativeLibraryInspector` adapter and invokes it only for libraries that were successfully loaded. The adapter reports the library version and capability assessments. Version integers can be decoded using FFmpeg's `AV_VERSION_INT(major, minor, micro)` representation through `NativeLibraryVersion.fromFfmpeg`.

The registry deliberately distinguishes:

- `VERIFIED`: an adapter checked the relevant ABI/version constraints and required symbols.
- `UNAVAILABLE`: an adapter has evidence that the capability cannot be used.
- `UNKNOWN`: no sufficient evidence has been supplied.

The core runtime does not infer codecs, formats, filters, or hardware acceleration from the presence of a shared library. An adapter must not mark a capability VERIFIED based only on successful `System.loadLibrary`. The current implementation supplies the typed SPI and conservative registry; a concrete FFmpeg/Jextract inspector and platform/version compatibility matrix remain follow-up work.

## Important limitation

A successful `System.loadLibrary` call only confirms that the operating system loader loaded the named library. It does not prove that generated bindings match the library ABI or that required FFmpeg symbols are available. Likewise, the new inspection SPI does not itself query native functions until a concrete inspector is connected.

## Safety

- Never load native code in static initializers.
- Do not expose FFM `MemorySegment`, native pointers, or generated structs from public APIs.
- Keep native memory ownership scoped and explicit.
- Use typed options rather than arbitrary command strings.
- Treat native initialization as process-level state; do not claim that libraries can be safely unloaded.
