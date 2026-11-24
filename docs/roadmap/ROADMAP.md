# Manazir roadmap

This roadmap separates generated ABI bindings from the stable Java media API.

## M0 — Foundation audit and architecture
- [x] Inspect generated avutil source and record ABI sensitivity.
- [x] Add architecture, build and agent guidance.
- [x] Define layering, timestamp/timebase and native ownership boundaries.
- [ ] Confirm JDK baseline, FFmpeg headers/version, jextract version, generation command and platform matrix.
- [ ] Verify generated bindings compile reproducibly on declared JDK.

## M1 — Reproducible build and runtime foundation
- [x] Add initial Maven build targeting Java 22.
- [x] Add exact rational/timebase value types and conversion tests.
- [x] Add explicit native library bootstrap and structured load diagnostics.
- [ ] Separate generated source output from handwritten source.
- [ ] Pin FFmpeg/jextract versions and generation inputs.
- [ ] Verify FFmpeg versions and required symbols before publishing capabilities.
- [ ] Add CI for compile/tests without native FFmpeg and opt-in native integration profile.

Exit: clean checkout builds reproducibly; API types load without native libraries; runtime distinguishes loaded from verified; supported platforms and capabilities are explicit; time arithmetic is tested.

## M2 — Typed media probe and extraction
- Add avformat/avcodec behind internal adapters or selected process backend.
- Probe containers/streams/codecs/dimensions/frame rates/durations/sample rates/channel layouts.
- Preserve stream timebases, start times, duration ticks and provenance.
- Extract timestamped frames/audio segments to caller-selected artifact destinations.
- Add CFR, VFR, A/V sync, missing metadata and malformed-media fixtures.
- Define resource limits, temp policy, cancellation and structured errors.

Exit: consumer can probe media and extract timestamp-preserving frames/audio through typed APIs.

## M3 — Transcode, filter and mux
Typed encoding/container options; safe filter graph representation; progress/cancellation/timeouts/bounded concurrency/cleanup; verify output streams and timestamps.

## M4 — Production hardening and ecosystem integration
Platform native packaging/discovery; leak/resource/concurrency tests; API compatibility policy; examples and Dalang contract tests without product dependencies in Manazir.

## Guiding sequence
Do not build a broad FFmpeg wrapper surface first. Establish reproducibility and runtime safety, then complete probe plus timestamped extraction. Add transform/render primitives after these contracts stabilize.
