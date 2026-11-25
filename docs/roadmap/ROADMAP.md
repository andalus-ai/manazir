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
- [ ] Add real-media fixtures for MP4, MOV, MKV, audio-only, CFR, VFR, A/V sync, sparse metadata and malformed media.
- [ ] Verify actual FFprobe behavior against installed FFmpeg/FFprobe versions.
- [ ] Connect provider selection to runtime capability assessment and explicit configuration.
- [ ] Add a native FFM provider after reproducible `avformat`/`avcodec` bindings and ABI verification.
- [ ] Preserve packet/frame timestamp provenance and verify VFR behavior with real media.
- [x] Extract one timestamped video frame to a caller-selected PNG/JPEG artifact using the FFmpeg process adapter.\n- [ ] Extract audio segments and frame sequences to caller-selected artifact destinations.
- [ ] Add cancellation tokens, OS-level CPU/memory/filesystem limits, concurrency limits and multi-tenant isolation.

**Current slice:** local-file metadata probing and single-frame PNG/JPEG extraction work through explicit FFprobe/FFmpeg process adapters. Frame extraction retains the requested rational timestamp but serializes the CLI seek to 12 decimal places and does not claim exact decoded-frame PTS provenance. Real CFR/VFR fixtures, audio extraction, native FFM support, OS-level sandboxing, cancellation and CPU/memory quotas remain open. Full M2 exit criteria are not yet met.

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
