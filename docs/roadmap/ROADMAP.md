# Manazir roadmap

This roadmap separates generated ABI bindings from the stable Java media API and distinguishes public contracts from working backend implementations.

## M0 — Foundation audit and architecture

- [x] Inspect current generated avutil source.
- [x] Record the absence of build configuration, tests, and handwritten media facade in the initial tree.
- [x] Define layering, timebase, native ownership, and Dalang integration boundaries.
- [x] Confirm Java 22 source baseline from generated FFM API usage.

## M1 — Reproducible build and runtime foundation

- [x] Add an initial Maven build targeting Java 22.
- [ ] Pin and document FFmpeg/jextract versions and generation inputs.
- [ ] Regenerate and separate FFmpeg-specific bindings from the current avutil-only generated snapshot.
- [x] Add explicit native library bootstrap, configurable load order, and structured load diagnostics.
- [x] Add typed version/inspection contracts and a conservative capability registry.
- [x] Add an FFM-backed inspector for FFmpeg component version functions and selected stable API symbol groups.
- [ ] Define and test the supported FFmpeg ABI/version matrix.
- [ ] Add codec/container enumeration, hardware-device discovery, and real execution probes before advertising those capabilities.
- [x] Add exact rational/time-base value types and conversion tests.
- [x] Add CI for compile/tests without requiring native FFmpeg (JDK 22 and 25).
- [x] Add an opt-in native integration test profile; CI coverage remains pending.
- [ ] Verify clean JDK 22 compilation and unit tests in CI.

**Exit criteria:** a clean checkout builds reproducibly; ordinary API types load without native libraries; supported native environments report verified component API surfaces; exact timestamp semantics are tested.

## M2 — Typed media probe and extraction

- [x] Define backend-neutral probe request/result, container, stream, frame-rate, diagnostic and structured failure contracts.
- [x] Preserve separate container and per-stream clocks, optional timestamps, negative stream starts, and exact frame-rate ratios in the public model.
- [x] Define bounded probe request inputs (timeout and maximum probe bytes) and backend obligations.
- [x] Implement an isolated FFprobe process provider behind the typed API; native avformat/avcodec remains future work.
- [x] Parse container and stream codec, dimensions, frame rates, stream time bases, PTS/duration ticks, sample rates and channel counts.
- [x] Enforce request timeout and stdout byte budget while the child process runs; cap retained stderr diagnostics.
- [ ] Connect native probe execution to verified runtime capability assessment.
- [ ] Verify real CFR/VFR containers and enumerate channel layouts and codec-specific properties.
- [ ] Preserve packet/frame timestamp provenance and verify VFR behavior with real media.
- [ ] Extract timestamped video frames and audio segments to caller-selected artifact destinations.
- [ ] Add fixtures for CFR, VFR, audio/video synchronization, missing metadata, and malformed media.
- [ ] Add native provider and real-media fixture coverage for CFR, VFR, A/V sync, malformed media and sparse metadata.
- [ ] Add timestamp-preserving frame/audio extraction to caller-selected artifact destinations.
- [ ] Add cancellation tokens, OS-level CPU/memory/filesystem limits, concurrency limits and multi-tenant isolation.

**Current slice:** metadata probing works through the FFprobe process adapter for local files. It does not decode/extract media or claim native FFM support. The process timeout and captured stdout budget are enforced in-process; OS-level sandboxing and CPU/memory quotas remain open. Full M2 exit criteria are not yet met.

## M3 — Transcode, filter, and mux primitives

- [ ] Add typed encoding and container options.
- [ ] Add a safe filter-graph representation; do not treat arbitrary shell fragments as the default API.
- [ ] Support progress, cancellation, timeouts, bounded concurrency, and cleanup.
- [ ] Verify output streams and timestamps after render operations.

## M4 — Production hardening and ecosystem integration

- [ ] Add platform-specific native packaging/discovery strategy.
- [ ] Add native leak/resource lifecycle tests and concurrency tests.
- [ ] Publish versioned Java API and compatibility policy.
- [ ] Provide examples and integration tests for Dalang without introducing Dalang dependencies into Manazir.

## Guiding sequence

Do not begin with a broad FFmpeg wrapper surface. Complete one vertical slice—probe plus timestamped extraction—before adding editing/render primitives. Public contracts are not evidence that a backend operation is implemented or verified.
