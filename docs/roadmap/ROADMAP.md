# Manazir roadmap

This roadmap is based on the inspected initial repository snapshot. It separates generated ABI bindings from the stable Java media API.

## M0 — Foundation audit and architecture

- [x] Inspect current generated avutil source.
- [x] Record the absence of build configuration, tests, and handwritten media facade in the current tree.
- [x] Define layering, timebase, native ownership, and Dalang integration boundaries.
- [ ] Confirm JDK baseline, FFmpeg headers/version, jextract version, and supported platform matrix.

## M1 — Reproducible build and runtime foundation

- [x] Add an initial Maven build targeting Java 22 (baseline is provisional pending a clean JDK 22 build).
- [ ] Pin and document FFmpeg/jextract versions and generation inputs.
- [ ] Regenerate and separate FFmpeg-specific bindings from the current avutil-only generated snapshot.
- [x] Add explicit native library bootstrap, configurable load order, and structured load diagnostics.
- [x] Add typed version/inspection contracts and a conservative capability registry.
- [x] Add an FFM-backed inspector for FFmpeg component version functions and selected stable API symbol groups.
- [ ] Define and test the supported FFmpeg ABI/version matrix.
- [ ] Add codec/container enumeration, hardware-device discovery, and real execution probes before advertising those capabilities.
- [x] Add exact rational/time-base value types and conversion tests.
- [x] Add an opt-in native integration test profile; CI coverage remains pending.
- [ ] Verify clean JDK 22 compilation and unit tests in CI.

**Exit criteria:** a clean checkout builds reproducibly; ordinary API types load without native libraries; supported native environments report verified component API surfaces; exact timestamp semantics are tested.

## M2 — Typed media probe and extraction

- [ ] Add avformat and avcodec integration behind internal adapters (or a deliberately selected process backend).
- [ ] Probe containers, streams, codecs, dimensions, frame rates, durations, sample rates, and channel layouts.
- [ ] Preserve stream time bases, start times, duration ticks, and timestamp provenance.
- [ ] Extract timestamped video frames and audio segments to caller-selected artifact destinations.
- [ ] Add fixtures for CFR, VFR, audio/video synchronization, missing metadata, and malformed media.
- [ ] Define resource limits, temp-file policy, cancellation, and structured error mapping.

**Exit criteria:** Dalang can import an asset, obtain reliable stream metadata, and request timestamp-preserving frame/audio extraction through typed contracts.

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

Do not begin with a broad FFmpeg wrapper surface. First establish reproducibility and runtime safety (M1), then complete one vertical slice—probe plus timestamped extraction (M2). Add editing/render primitives only after those contracts are stable.
