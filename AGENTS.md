# AGENTS.md — Manazir

## Mission
Manazir is the reusable Java-native media execution layer for the Andalus ecosystem. It owns media-format semantics and safe FFmpeg integration, not product workflows such as Dalang projects, editing timelines, clipper UX or agent orchestration.

## Read first
README.md; docs/architecture/FFM_FOUNDATION.md; docs/architecture/ARCHITECTURE.md; docs/architecture/DIAGRAMS.md; docs/development/BUILDING.md; docs/roadmap/ROADMAP.md.

## Source baseline
The repository currently contains generated Java FFM source only under tech.kayys.manazir.avutil; that snapshot does not expose FFmpeg component version functions. The FFmpeg runtime inspector therefore uses a narrow FFM loader-symbol adapter rather than treating those generated declarations as complete FFmpeg bindings. Generated declarations are low-level ABI-specific implementation, not a stable consumer API. Before editing or regenerating generated code, establish FFmpeg headers/version, jextract version, generation command, target platform and reproducibility. Verify current Maven/JDK assumptions; do not infer supported native deployment matrix from source compiling.

## Architecture rules
1. Layer generated bindings -> explicit native runtime -> typed public media API -> backend SPI/adapters -> product integrations.
2. Keep native loading lazy and explicit. Loading API types must not load native libraries.
3. Define ownership/lifetime for every arena, allocation, callback, handle and returned buffer. Never return a segment whose arena has closed.
4. Represent time as integer ticks and rational timebase. Preserve source stream, packet and decoded-frame timestamp distinctions.
5. Keep native pointers, MemorySegment, generated structs and ABI enums out of public APIs.
6. Use typed options. Process backend uses argument vectors; never interpolate user paths/options into shell strings.
7. Bound duration, memory, temporary storage, output size and concurrency.
8. Support cancellation, progress, structured diagnostics and deterministic cleanup.
9. Backend-specific details stay behind interfaces; native FFM and isolated process backend may coexist.
10. No Dalang, Andalus-agent or AQLI product-domain dependencies in core media API.
11. OS library-load success is not ABI/symbol verification; do not advertise capabilities before checks pass.
12. Component API symbol presence is not codec/container/hardware support. Keep per-codec enumeration and execution probes separate from symbol-surface checks.
13. The Java 22 FFM adapter requires native access at runtime; keep native integration tests opt-in and enable native access only for that test JVM.

## Media probe implementation notes\nThe initial `media.probe` API is backend-neutral and keeps source stream PTS plus its rational time base. `FfprobeMediaProbe` is an optional process adapter: it invokes an argv vector without a shell, applies a timeout, parses typed JSON metadata, and deletes temporary files. It is not the native FFM implementation. Its current output-size check happens after the process exits; do not describe that as a hard disk quota or expose this adapter to untrusted multi-tenant workloads without adding bounded streaming/quota controls and cancellation. Frame/audio extraction is not implemented by this slice.\n\n## Implementation workflow
Inspect generated source/provenance first. Establish reproducible build and tests before broad API. Add typed contracts and tests before native operations. Validate ABI on supported platforms. Keep generated and handwritten changes separate. Run narrow tests then full build; report exactly what ran and environmental limits.

## Definition of done
Public contract has lifecycle, failure, cancellation, resource and compatibility semantics; tests and examples cover it; docs and roadmap are updated; no unverified native compatibility claims.