# Typed media-probe API

## Scope

The first Manazir operation contract is read-only container and stream inspection. It is backend-neutral and has no dependency on Dalang, FFmpeg-generated bindings, FFM types, or process execution.

## Contract

- `MediaProbeRequest` carries an absolute source URI, optional format hint, timeout, and maximum probe-byte budget.
- `MediaProbe` is the provider port. Each implementation exposes a stable `backendId`.
- `MediaProbeResult` returns source identity, backend identity, container metadata, stream metadata, and non-fatal diagnostics.
- `MediaContainerInfo` uses its own optional timestamp clock. Each `MediaStreamInfo` has its own exact rational timebase.
- Stream timestamps are integer ticks in that stream's timebase. Negative start timestamps and absent timestamps are valid.
- Frame rates use `FrameRate` (frames per second), not `RationalTimeBase` (seconds per tick). Average and nominal rates remain independently optional; neither reconstructs VFR presentation timestamps.
- Unknown codec, dimensions, audio parameters, frame rates, and durations remain absent. A provider must not guess.
- Failures use `MediaProbeException.Code`; callers must not parse FFmpeg stderr or backend-specific messages.

## Backend obligations

An implementation must treat the URI as data, not a shell fragment; honor timeout and byte limits or fail explicitly; avoid mutating source media; preserve native timestamps; and close all backend resources before returning. The public API does not expose native handles or buffers.

The current repository does not yet include an implementation. Native FFM and process-isolated FFmpeg providers are separate follow-up work, selected explicitly through runtime configuration and capability assessment.

## Dalang mapping

Dalang maps `MediaContainerInfo` and `MediaStreamInfo` into its own media-domain records. It should preserve the Manazir timebase/ticks exactly and keep its own domain types at its public boundary. No dependency from Manazir to Dalang is permitted.
