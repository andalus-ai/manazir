# FFM and native runtime foundation

## Layers

1. Generated jextract declarations: low-level ABI-specific implementation only.
2. Native runtime: explicit loading, diagnostics, version/capability checks, and lifecycle.
3. Typed media API: stable Java records and service interfaces.
4. Backend adapters: FFM or process-based implementations.
5. Product integrations: Dalang consumes typed APIs only.

## Explicit initialization

Use `NativeRuntime.system().initialize(NativeRuntimeConfig.ffmpegDefaults())` to request loading of the configured FFmpeg libraries. Constructing the runtime, config, timestamp, or report types does not load native code.

The default order is avutil, swresample, swscale, avcodec, avformat, avfilter. This is a pragmatic dependency-oriented order, not an ABI compatibility guarantee. The actual supported FFmpeg version matrix and platform packaging still need to be pinned.

In non-fail-fast mode, initialization attempts every configured library and returns a report containing successful loads and diagnostics for failures. In fail-fast mode, the first failure stops initialization and throws `NativeRuntimeException` with the partial report.

## Important limitation

A successful `System.loadLibrary` call only confirms that the operating system loader loaded the named library. It does not prove that generated bindings match the library ABI or that required FFmpeg symbols are available. A later runtime verification layer must query library versions and required symbols before advertising media capabilities.

## Safety

- Never load native code in static initializers.
- Do not expose FFM `MemorySegment`, native pointers, or generated structs from public APIs.
- Keep native memory ownership scoped and explicit.
- Use typed options rather than arbitrary command strings.
- Treat native initialization as process-level state; do not claim that libraries can be safely unloaded.
