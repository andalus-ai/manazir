# Manazir architecture

## Mission and boundary
Manazir provides reusable Java-native media execution for Andalus products. It hides FFmpeg ABI details behind typed APIs. Dalang owns video projects, timelines, proposals and clipper workflows; Manazir must not depend on Dalang.

## Layer model
1. Generated jextract FFM bindings: low-level ABI declarations, isolated and reproducibly generated.
2. Native runtime: explicit library loading, platform/version/symbol checks, diagnostics and lifecycle.
3. Public media API: typed timestamps, media sources, probe reports, streams, extraction/transcode requests, progress, cancellation and errors.
4. Backend SPI: native FFM backend and optional process-isolated FFmpeg backend.
5. Product adapters: Dalang and other consumers depend only on stable public contracts.

## Runtime lifecycle
Constructing API values/services does not load native libraries. Explicit initialization returns a structured report. Distinguish NOT_INITIALIZED, LOAD_FAILED, LOADED_UNVERIFIED and VERIFIED states. A library load alone does not prove compatible version or required symbols. Capabilities become available only after version/symbol checks. Initialization and native resource ownership are process/runtime scoped and documented; do not promise safe unloading without evidence.

## Public API principles
- No FFM MemorySegment, native pointer, generated struct or backend enum in public API.
- Native buffers are copied or exposed through an explicit scoped resource contract; never return expired memory.
- Media time is integer ticks plus rational timebase; rescaling has explicit rounding.
- Container, stream, packet and decoded-frame timestamps remain distinct.
- Typed options and structured diagnostics; no arbitrary shell strings.
- Every long operation supports bounded resources, progress, cancellation and cleanup.

## Backend strategy
Native FFM backend offers in-process efficiency but requires strict ABI/platform validation and memory safety. Process backend offers isolation and broader deployment flexibility but incurs process/serialization overhead. Both implement the same typed contracts. Select backend by explicit configuration/capability, not hidden fallback that changes semantics.

## Dalang integration
Dalang uses Manazir public media API for probe, timestamped extraction, transforms and render execution. Manazir knows nothing about Dalang projects or timelines. Integration contract lives in Dalang docs.

## Non-goals
No agent planning, LLM prompt management, video project model, timeline editor, UI, or social publishing workflow.
 
## Initial process provider
The first working probe provider is an isolated FFprobe subprocess implementing the backend-neutral media probe port. It accepts local files, passes arguments without a shell, enforces a timeout and a bounded captured-stdout budget, and maps failures to typed errors. It is metadata-only and does not claim native FFM integration or hostile-input sandboxing. The native provider remains gated on reproducible FFmpeg-specific bindings and ABI validation.
