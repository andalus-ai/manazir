# Manazir architecture

## Mission and boundary
Manazir provides reusable Java-native media execution for Andalus products. It hides FFmpeg ABI details behind typed APIs. Dalang owns video projects, timelines, proposals and clipper workflows; Manazir must not depend on Dalang.

## Layer model
1. Generated jextract FFM bindings: low-level ABI declarations, isolated and reproducibly generated.
2. Native runtime: explicit library loading, platform/version/symbol checks, diagnostics and lifecycle.
3. Public media API: typed timestamps, probe requests/reports, container and stream metadata, extraction/transcode requests, progress, cancellation and errors.
4. Backend SPI and provider registry: native FFM backend and optional process-isolated FFmpeg/FFprobe backends.
5. Product adapters: Dalang and other consumers depend only on stable public contracts.

## Runtime lifecycle
Constructing API values/services does not load native libraries. Explicit initialization returns a structured report. Distinguish NOT_INITIALIZED, LOAD_FAILED, LOADED_UNVERIFIED and VERIFIED states. A library load alone does not prove compatible version or required symbols. Capabilities become available only after version/symbol checks. Initialization and native resource ownership are process/runtime scoped and documented; do not promise safe unloading without evidence.

## Public API principles
- No FFM MemorySegment, native pointer, generated struct or backend enum in public API.
- Native buffers are copied or exposed through an explicit scoped resource contract; never return expired memory.
- Media time is integer ticks plus rational timebase; rescaling has explicit rounding.
- Container and stream timestamps use independent clocks; packet and decoded-frame timestamps remain distinct.
- Frame rates are represented as frames/second rational values, not timebases.
- Missing metadata remains absent. Average and nominal frame rates do not reconstruct VFR presentation timestamps.
- Typed options and structured diagnostics; no arbitrary shell strings.
- Every long operation supports bounded resources, progress, cancellation and cleanup.

## Typed media contracts
The public probe port lives under `tech.kayys.manazir.media.probe`; the initial process implementation is `FfprobeMediaProbe` and supports local-file metadata probing. The frame-extraction port lives under `tech.kayys.manazir.media.extract`; `FfmpegFrameExtractor` implements single-frame extraction to a caller-selected image artifact. These are process adapters, not native FFM implementations and not OS-level sandboxes.

## Capability-based provider selection
`MediaProviderRegistry<T>` is an explicit, immutable registry of provider instances and descriptors. A descriptor declares provider ID, supported `MediaCapability` values, availability evidence, priority and diagnostic. The registry itself never discovers providers, loads native libraries or executes tools.

- `ExecutableAvailabilityCheck` explicitly runs an executable's `-version` command with a timeout. It establishes only that the process can run; it does not prove codec, container, decode, encode or filter support.
- `FfmpegCapabilityInspector` separately runs bounded FFmpeg listing commands for decoders, encoders, filters, muxers and demuxers. The immutable report retains operation names and version evidence, and can create a descriptor for explicit registration.
- Decoder and encoder tables are parsed independently; a codec appearing in a table is evidence that the build advertises it, not proof that a particular input will decode or encode successfully.
- The report derives broad operation capabilities from non-empty media-specific tables and named filters. It does not infer hardware acceleration or successful operation for every codec/container combination.
- Inspection has per-command timeouts and a 2 MiB combined stdout/stderr capture limit. Failures are structured (unavailable executable, command failure, timeout, output limit, interruption).
- Native availability must come from the existing explicit native load/version/symbol verification path. Merely finding a shared library is not sufficient.
- Unknown availability is not eligible for automatic selection.
- Selection is deterministic: higher priority first, then provider ID.
- An explicitly requested provider is strict by default. Fallback requires `ALLOW_FALLBACK` and is returned in selection diagnostics; there is no hidden fallback.
- Register only capabilities actually implemented by that provider. FFprobe metadata probing is `MEDIA_PROBE`; the single-frame FFmpeg process adapter requires decode capability and must not be advertised as transcode/filter/mux support.

The selector is generic so probe, extraction and later execution ports can use the same policy without coupling the registry to product-specific APIs. Capability inspection is opt-in and does not mutate registries or execute media operations.

## Backend strategy
Native FFM backend offers in-process efficiency but requires strict ABI/platform validation and memory safety. Process backend offers process boundaries and broader deployment flexibility but incurs process/serialization overhead; it is not a security sandbox by itself. Both implement the same typed contracts. Select backend by explicit configuration/capability, not hidden fallback that changes semantics.

## Dalang integration
Dalang uses Manazir public media API for probe, timestamped extraction, transforms and render execution. Manazir knows nothing about Dalang projects or timelines. Integration contract lives in Dalang docs.

## Non-goals
No agent planning, LLM prompt management, video project model, timeline editor, UI, or social publishing workflow.
