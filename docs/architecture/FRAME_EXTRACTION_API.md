# Typed frame-extraction API

## Scope

The first extraction operation decodes one video frame from a local media source into a caller-selected still-image artifact. It is backend-neutral at the port boundary; the first implementation invokes FFmpeg as a child process.

## Contract

- `FrameExtractionRequest` identifies the source URI, global source-timeline position, video stream index, destination path, image format, timeout, and maximum output bytes.
- The requested position is an exact `MediaTimestamp` (integer ticks plus rational time base). The FFmpeg CLI accepts decimal seconds, so the adapter serializes this rational value to 12 decimal places using HALF_UP rounding. The requested timestamp is retained in the returned artifact; this does not claim an exact decoded frame PTS.
- `FrameExtractor` is the provider port. `FrameArtifact` reports the committed path, content type, byte size, dimensions, requested position, stream index, and backend id.
- The initial formats are PNG and JPEG.
- The destination must be an absolute path in an existing writable directory and must not already exist. The provider renders to a sibling temporary file, validates the image, and then moves it into place without requesting replacement.
- The provider passes arguments directly (no shell), uses output-side seeking, selects exactly one video frame, applies a timeout, and asks FFmpeg to cap output size. The final artifact size is checked independently.
- Only local `file:` sources are supported in this first slice. Remote protocols, audio extraction, frame sequences, and native FFM decoding are not included.

## Safety boundary

The subprocess timeout and output-file cap do not provide OS-level CPU, memory, filesystem, or network isolation. Treat untrusted media as hostile input and do not expose this provider to multi-tenant workloads without external sandboxing.

## Dalang mapping

Dalang owns timeline semantics and decides which source position and stream to request. Manazir decodes one frame and returns an artifact descriptor; it does not own project timelines, clip identities, or editing decisions.
