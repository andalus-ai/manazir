# AGENTS.md — Manazir

## Mission
Manazir is the reusable Java-native media execution layer for the Andalus ecosystem. It owns media-format semantics and safe FFmpeg integration, not product workflows such as Dalang projects, editing timelines, clipper UX or agent orchestration.

## Read first
README.md; docs/architecture/FFM_FOUNDATION.md; docs/architecture/ARCHITECTURE.md; docs/architecture/DIAGRAMS.md; docs/development/BUILDING.md; docs/roadmap/ROADMAP.md.

## Source baseline
The checked-in avutil snapshot was generated against a macOS SDK and is excluded from the portable Maven compile; it is not a supported FFmpeg ABI. The FFmpeg runtime inspector uses a narrow FFM loader-symbol adapter rather than treating generated declarations as complete FFmpeg bindings. Before regenerating generated code, establish pinned FFmpeg headers/version, jextract version, exact generation command, target platform and reproducibility.

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
14. The canonical extraction port is `FrameExtractor` with `FrameExtractionRequest` and `FrameExtractionResult`. Do not reintroduce the pre-contract `FrameArtifact` shape into new provider APIs.
15. A provider must reject a selection policy or output format it cannot honor; never silently downgrade `NEAREST` to `FIRST_AT_OR_AFTER`.

## Current process adapter limitations
`FfprobeMediaProbe` provides local-file metadata probing. `FfmpegFrameExtractor` is being aligned to the canonical typed extraction contract and supports one local-file frame as PNG/JPEG with a wall-clock timeout, bounded diagnostics, temporary output validation and non-overwriting publication. Seek serialization to the FFmpeg CLI is decimal (12 fractional places); the original requested timestamp remains intact, and decoded-frame PTS is optional `showinfo` evidence. The current process provider explicitly rejects `NEAREST` and WEBP rather than pretending to support them. Neither process adapter is an OS-level sandbox. Audio extraction, frame sequences, cancellation tokens, packet timestamp provenance, native FFM decoding and OS-level CPU/memory/filesystem isolation remain open.

## Implementation workflow
Inspect generated source/provenance first. Establish reproducible build and tests before broad API. Add typed contracts and tests before native operations. Validate ABI on supported platforms. Keep generated and handwritten changes separate. Run narrow tests then full build; report exactly what ran and environmental limits.

## Definition of done
Public contract has lifecycle, failure, cancellation, resource and compatibility semantics; tests and examples cover it; docs and roadmap are updated; no unverified native compatibility claims.
