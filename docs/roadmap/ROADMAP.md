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

- [ ] Add `avformat` and `avcodec` integration behind internal adapters (or a deliberately selected process backend).
- [ ] Probe containers, streams, codecs, dimensions, frame rates, durations, sample rates, and channel layouts.
- [ ] Preserve stream time bases, start times, duration ticks, and timestamp provenance.
- [x] Define backend-neutral timestamped video frame extraction request/result/service contracts.
- [ ] Implement a process-backed frame extractor with verified timestamp-selection semantics.
- [ ] Extract audio segments to caller-selected artifact destinations.
- [ ] Add fixtures for CFR, VFR, audio/video synchronization, missing metadata, and malformed media.
- [ ] Add native provider and real-media fixture coverage for CFR, VFR, A/V sync, malformed media and sparse metadata.
- [ ] Add timestamp-preserving frame/audio extraction to caller-selected artifact destinations.
- [ ] Add cancellation tokens, OS-level CPU/memory/filesystem limits, concurrency limits and multi-tenant isolation.

**Current slice:** metadata probing works through the FFprobe process adapter for local files. Backend-neutral frame-extraction contracts now model the source stream index, exact target timestamp/time base, frame-selection policy, output format/dimensions, destination, and timeout. No frame-extraction provider is implemented yet; the contracts do not imply that a frame can already be extracted. The probe adapter does not claim native FFM support. Timeout and stdout limits are enforced in-process; OS-level sandboxing and CPU/memory quotas remain open. Full M2 exit criteria are not yet met.

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
