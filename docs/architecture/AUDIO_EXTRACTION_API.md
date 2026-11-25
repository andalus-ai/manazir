# Typed audio-extraction API

## Scope

The initial audio operation extracts one bounded interval from one selected audio stream into an uncompressed PCM WAV artifact. The public contract is independent of FFmpeg; the first implementation is a local-file FFmpeg process adapter.

## Contract

- `AudioExtractionRequest` identifies a local source URI, absolute audio stream index, exact start/end timestamps, output path, sample rate, channel count, timeout, and maximum output bytes.
- Start and end are `MediaTimestamp` values (integer ticks plus rational time bases). The request validates that the end is strictly after the start. The adapter computes the interval duration from rational values and serializes CLI timestamps to 12 decimal places; the original requested boundaries remain in the artifact.
- Output is WAV with signed 16-bit little-endian PCM. Sample rate is configurable from 8 kHz to 384 kHz; channel count is configurable from mono through 7.1 (1–8 channels).
- `AudioArtifact` reports the committed path, content type, bytes, sample rate, channels, bit depth, decoded sample-frame count, requested interval, stream index, and backend ID.
- The provider selects the exact stream with `-map 0:<index>`, performs output-side seeking after opening the input to avoid input-seek keyframe imprecision, applies a wall-clock timeout and FFmpeg output cap, validates the WAV/PCM header and payload length, and moves a sibling temporary file into place without replacement.
- Only local `file:` sources are supported. Encoded output formats, channel-layout names, packet-level timestamp provenance, and native FFM extraction are not part of this slice.

## Accuracy and safety

The interval is represented exactly at the API boundary, but FFmpeg's CLI accepts decimal seconds and audio is quantized to output sample boundaries. The artifact therefore reports requested boundaries and actual sample-frame count separately; it does not claim sample-accurate source packet PTS provenance. Output-side seeking decodes from the beginning of the source and can be expensive for long media.

The FFmpeg process currently discards stderr rather than retaining diagnostics; a follow-up should add bounded structured stderr capture. The subprocess timeout and output cap do not provide OS-level CPU, memory, filesystem, or network isolation. Do not expose this provider to hostile multi-tenant workloads without external sandboxing.

## Dalang mapping

Dalang owns timeline/edit semantics and chooses the source stream and interval. Manazir performs extraction and returns a typed artifact; it does not own projects, clips, timelines, or edit decisions.
