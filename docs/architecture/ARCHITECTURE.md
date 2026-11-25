# Manazir architecture

## Mission and boundary
Manazir provides reusable Java-native media execution for Andalus products. It hides FFmpeg ABI details behind typed APIs. Dalang owns video projects, timelines, proposals and clipper workflows; Manazir must not depend on Dalang.

## Layer model
1. Generated jextract FFM bindings: low-level ABI declarations, isolated and reproducibly generated.
2. Native runtime: explicit library loading, platform/version/symbol checks, diagnostics and lifecycle.
3. Public media API: typed timestamps, probe requests/reports, container and stream metadata, extraction/transcode requests, progress, cancellation and errors.
4. Backend SPI: native FFM backend and optional process-isolated FFmpeg backend.
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

## Current typed probe contract
The initial public contract lives under `tech.kayys.manazir.media.probe`: `MediaProbe`, `MediaProbeRequest`, `MediaProbeResult`, `MediaContainerInfo`, `MediaStreamInfo`, `FrameRate`, `ProbeDiagnostic`, and `MediaProbeException`. It defines API semantics only; no FFmpeg provider is implemented yet. See [MEDIA_PROBE_API.md](MEDIA_PROBE_API.md).

## Backend strategy
Native FFM backend offers in-process efficiency but requires strict ABI/platform validation and memory safety. Process backend offers isolation and broader deployment flexibility but incurs process/serialization overhead. Both implement the same typed contracts. Select backend by explicit configuration/capability, not hidden fallback that changes semantics.

## Dalang integration
Dalang uses Manazir public media API for probe, timestamped extraction, transforms and render execution. Manazir knows nothing about Dalang projects or timelines. Integration contract lives in Dalang docs.

## Non-goals
No agent planning, LLM prompt management, video project model, timeline editor, UI, or social publishing workflow.
