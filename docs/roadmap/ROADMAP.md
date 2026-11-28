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
- [x] Add real-media fixtures for MP4, MOV, MKV, audio-only, CFR, VFR, A/V sync and malformed media.
- [ ] Add sparse-metadata fixtures and broader malformed/container edge cases.
- [x] Verify actual FFprobe behavior against the installed FFmpeg/FFprobe toolchain.
- [x] Add deterministic capability-based provider selection with explicit availability, strict selection and opt-in fallback.
- [x] Wire registry-backed facades into the probe and frame-extraction ports; selection failures map to structured backend-unavailable errors.
- [ ] Connect provider registration to verified runtime capability assessment and explicit application configuration.
- [ ] Add a native FFM provider after reproducible `avformat`/`avcodec` bindings and ABI verification.
- [x] Expose optional decoded-frame PTS and its rational filter-input time base in extracted frame artifacts.
- [ ] Add packet timestamp provenance and typed multi-frame/stream timestamp enumeration.
- [x] Extract one timestamped video frame to a caller-selected PNG/JPEG artifact using the FFmpeg process adapter.
- [x] Extract a selected audio stream interval to caller-selected PCM WAV with bounded execution and output.\n- [x] Capture bounded FFmpeg stderr asynchronously and include useful diagnostics in structured failures.
- [ ] Add frame sequences, encoded audio formats, cancellation tokens, OS-level CPU/memory/filesystem limits, concurrency limits and multi-tenant isolation.

**Current slice:** local-file metadata probing, single-frame PNG/JPEG extraction, and audio-to-WAV extraction work through explicit FFprobe/FFmpeg process adapters. Frame extraction reports actual decoded-frame PTS when available. Audio extraction preserves requested rational boundaries, selects an explicit stream, emits validated PCM S16LE WAV, and reports the actual sample-frame count. Packet timestamp provenance, typed multi-frame enumeration, encoded audio formats, native FFM support, OS-level sandboxing, cancellation and CPU/memory quotas remain open. Full M2 exit criteria are not yet met.

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
