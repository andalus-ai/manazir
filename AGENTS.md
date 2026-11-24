# Agent instructions for Manazir

## Mission

Build a reusable Java-native media and FFmpeg integration layer. Manazir owns media execution primitives and media-format semantics; it does not own product workflows such as Dalang projects, editing timelines, clipper UX, or agent orchestration.

## Repository baseline

The initial source snapshot consists of generated jextract Java FFM bindings in `src/main/java/tech/kayys/manazir/avutil`. The generated classes are low-level ABI bindings, not a stable end-user API. Before changing generated files, inspect the generation inputs, jextract version, FFmpeg headers/version, target platform, and reproducibility strategy.

Do not claim a build or tests pass until a reproducible build and tests exist and have actually been run.

## Architecture rules

- Keep generated FFM bindings isolated from stable public Java contracts.
- Keep native loading lazy and explicit; ordinary API type loading must not require local FFmpeg native libraries.
- Define ownership rules for every native allocation, callback, arena, and returned segment.
- Represent timestamps as integer ticks with rational time bases. Preserve source time bases and distinguish stream, packet, frame, and container timestamps.
- Use typed options and argument-safe process invocation. Never concatenate user-provided media paths or filter expressions into shell command strings.
- Bound process duration, memory, temporary storage, output size, and concurrency.
- Support cancellation, progress, structured diagnostics, and deterministic cleanup.
- Keep backend-specific details behind interfaces so native FFM and process-based execution can coexist.
- Do not introduce Dalang, Andalus agent, or AQLI domain dependencies into the core media API.

## Implementation workflow

1. Inspect existing source and generated-code provenance before designing new APIs.
2. Establish a reproducible build and a minimal test baseline before broad implementation.
3. Add typed contracts and tests before exposing native operations.
4. Validate native ABI assumptions on supported platforms; never infer portability from a successful compile alone.
5. Keep generated code changes separate from handwritten API changes where possible.
6. Run the narrowest relevant tests, then the full build; report exactly what was and was not executed.

See `docs/roadmap/ROADMAP.md` for the staged implementation plan.