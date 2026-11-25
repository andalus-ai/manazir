# Typed media-probe API

## Scope

The first Manazir operation is read-only container and stream inspection. The public API is backend-neutral and has no dependency on Dalang, FFmpeg-generated bindings, FFM types, or process execution.

## Contract

- `MediaProbeRequest` carries an absolute source URI, optional format hint, timeout, and maximum probe-byte budget.
- `MediaProbe` is the provider port. Each implementation exposes a stable `backendId`.
- `MediaProbeResult` returns source identity, backend identity, container metadata, stream metadata, and non-fatal diagnostics.
- `MediaContainerInfo` uses its own optional timestamp clock. Each `MediaStreamInfo` carries its own optional exact rational timebase.
- Stream timestamps are integer ticks in that stream's timebase. Negative start timestamps and absent timestamps are valid.
- Frame rates use `FrameRate` (frames per second), not `RationalTimeBase` (seconds per tick). Average and nominal rates remain independently optional; neither reconstructs VFR presentation timestamps.
- Unknown codec, dimensions, audio parameters, frame rates, and durations remain absent. A provider must not guess.
- Failures use `MediaProbeException.Code`; callers must not parse FFmpeg stderr or backend-specific messages.

## FFprobe process provider

`FfprobeMediaProbe` implements the port with backend id `ffprobe-process`.

- Accepts local `file:` URIs only; remote protocols are not enabled by this provider.
- Invokes FFprobe using `ProcessBuilder` argument vectors, never a shell command assembled from user input.
- Restricts an optional format hint to a conservative character allowlist.
- Enforces the request wall-clock timeout, terminates the child process tree on timeout or stdout overflow, and bounds captured stdout to `maxProbeBytes`.
- Retains at most 64 KiB of stderr for structured failure diagnostics.
- The byte budget applies to captured JSON stdout, not the size of the input media file.
- Container decimal timestamps are represented on a microsecond timebase; stream PTS and duration ticks retain the exact stream timebase.
- Missing stream timebases, timestamps, and metadata remain absent. Diagnostics explain timestamps that cannot be interpreted.

This is a metadata-only process adapter. It does not decode or extract media, support remote URLs, or provide OS-level CPU/memory/filesystem sandboxing. Do not expose it to hostile multi-tenant workloads without additional isolation. The native FFM provider remains separate future work.

## Dalang mapping

Dalang maps `MediaContainerInfo` and `MediaStreamInfo` into its own media-domain records. It should preserve timebases and ticks exactly and keep its own domain types at its public boundary. No dependency from Manazir to Dalang is permitted.
