# Manazir roadmap

The roadmap separates portable handwritten media APIs from generated native ABI declarations. Native integration is gated on reproducible generation and an explicit platform matrix.

## M0 — Foundation audit and architecture

- [x] Inspect the original generated `avutil` source snapshot.
- [x] Record the original absence of build configuration, tests, and handwritten media facade.
- [x] Define layering, timebase, native ownership, and Dalang integration boundaries.
- [x] Confirm Java 22 source baseline from FFM API usage.

## M1 — Reproducible build and runtime foundation

- [x] Add an initial Maven build targeting Java 22.
- [x] Add CI for handwritten Java compilation and tests on JDK 22 and 25.
- [x] Keep the non-portable macOS-SDK-generated `avutil` snapshot out of the supported compile unit.
- [x] Add explicit native library bootstrap, configurable load order, and structured load diagnostics.
- [x] Add typed version/inspection contracts and a conservative capability registry.
- [x] Add an FFM-backed inspector for component version functions and selected stable API symbol groups.
- [x] Add exact rational/time-base value types and conversion tests.
- [ ] Pin FFmpeg headers, jextract version, and exact generation inputs.
- [ ] Regenerate FFmpeg-specific bindings and isolate them from host C runtime declarations.
- [ ] Define and test the supported FFmpeg ABI/version matrix.
- [ ] Add codec/container enumeration, hardware-device discovery, and real execution probes before advertising those capabilities.
- [ ] Add native integration CI after the platform matrix is pinned.

**Exit criteria:** a clean checkout builds reproducibly; ordinary API types load without native libraries; supported native environments report verified component API surfaces; exact timestamp semantics are tested.

## M2 — Typed media probe and extraction

- [x] Define backend-neutral probe request/result, container, stream, frame-rate, diagnostic and structured failure contracts.
- [x] Implement an FFprobe process provider for local-file metadata probing.
- [x] Pass process arguments without shell interpolation; enforce timeout and bounded JSON stdout capture.
- [x] Preserve independent container/stream clocks, exact stream PTS ticks, negative stream starts, and optional metadata.
- [x] Test provider behavior with deterministic fake-process fixtures, including output limits and timeout handling.
- [x] Add real-media fixtures for MP4, MOV, MKV, audio-only, CFR, VFR and malformed media.
- [ ] Add sparse-metadata fixtures and broader malformed/container edge cases.
- [x] Verify actual FFprobe behavior against the installed FFmpeg/FFprobe toolchain.
- [x] Add generic capability-based provider descriptors and deterministic selection policies.
- [ ] Connect provider selection to concrete FFprobe/FFmpeg provider construction and runtime capability assessment.
- [ ] Add a native FFM provider after reproducible `avformat`/`avcodec` bindings and ABI verification.
- [x] Define backend-neutral timestamp-aware single-frame extraction contracts.
- [x] Implement a process-backed single-frame PNG/JPEG extraction slice with validated temporary output and no-overwrite publication.
- [x] Capture optional decoded-frame PTS and its rational filter-input time base from FFmpeg `showinfo`.
- [ ] Prove `FIRST_AT_OR_AFTER` behavior across non-zero stream starts and more VFR edge cases; current CLI seek serialization is limited to 12 decimal places.
- [ ] Implement `NEAREST` selection; the current process provider rejects it explicitly.
- [ ] Implement WEBP output; the current process provider rejects it explicitly.
- [ ] Extract audio segments and frame sequences to caller-selected artifact destinations.
- [ ] Add packet timestamp provenance and typed multi-frame/stream timestamp enumeration.
- [ ] Add cancellation tokens, OS-level CPU/memory/filesystem limits, concurrency limits and multi-tenant isolation.

**Current slice:** metadata probing and a first single-frame PNG/JPEG process provider exist behind typed ports. The extractor returns the canonical `FrameExtractionResult`, preserves the original rational request and reports decoded PTS only when `showinfo` yields it. It supports `FIRST_AT_OR_AFTER` via FFmpeg's accurate seek path, but CLI seek arguments are serialized to 12 decimal places; `NEAREST` and WEBP are rejected. Real-media CI covers MP4/MOV/MKV, audio-only, VFR timestamps and a decoded frame. M2 is not complete: exact seek behavior for non-zero starts and broader VFR fixtures, audio extraction, frame sequences, packet provenance, native FFM, cancellation and sandbox/resource isolation remain open.

## M3 — Transcode, filter, and mux primitives

- [ ] Add typed encoding and container options.
- [ ] Add a safe filter-graph representation; arbitrary shell fragments are not the default API.
- [ ] Support progress, cancellation, timeouts, bounded concurrency, and cleanup.
- [ ] Verify output streams and timestamps after render operations.

## M4 — Production hardening and ecosystem integration

- [ ] Add platform-specific native packaging/discovery strategy.
- [ ] Add native leak/resource lifecycle tests and concurrency tests.
- [ ] Publish versioned Java API and compatibility policy.
- [ ] Provide examples and integration tests for Dalang without introducing Dalang dependencies into Manazir.
