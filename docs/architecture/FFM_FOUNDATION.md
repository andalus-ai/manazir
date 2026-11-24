# Manazir FFM foundation architecture

## 1. Observed baseline

Repository inspection of `main` at commit `f75292e9fd9c46457ad9143887f065d529a7a9ae` found a single Java package:

`tech.kayys.manazir.avutil`

The package contains generated jextract output, including `AvUtil`, `AvUtil_1`, `AvUtil_2`, `AvUtil$shared`, `AVClass`, `AVRational`, and generated platform/C-library declarations and callback carrier types.

The generated code uses the Java FFM API (`java.lang.foreign`) and includes native symbol lookup for the `avutil` library. The repository tree currently has no Maven/Gradle descriptor, tests, handwritten media facade, or `avcodec`/`avformat` packages.

This is a source-tree observation, not a statement that FFmpeg itself is absent from developer machines.

## 2. Important implications

### Generated bindings are not the product API

Generated declarations expose C layouts, function handles, pointers, and `MemorySegment` values. These are useful implementation building blocks but are too low-level and ABI-sensitive to become the API consumed by Dalang or application developers.

Do not hand-edit generated classes to add domain behavior. Preserve generation inputs and produce bindings reproducibly.

### Native loading must be isolated

The current generated hierarchy initializes a library lookup for `avutil` from a static field. A higher-level API should not trigger this merely by loading a type or constructing configuration. Native initialization belongs in an explicit runtime/bootstrap boundary that can report missing libraries and versions.

### ABI and platform support need an explicit matrix

The checked-in generated output contains platform-specific C declarations. Before claiming cross-platform support, define supported OS/architecture/FFmpeg combinations and generate or validate bindings for each. A generated layout must not silently be assumed valid on another ABI.

### Time must remain exact

`AVRational` is the correct low-level reminder that FFmpeg time values are rational. The public API should use a validated immutable rational/time-base type and integer timestamp ticks. Conversion to seconds is presentation-only and must be explicit.

## 3. Target layering

```text
Manazir public Java API
  ├── MediaRuntime / capability discovery
  ├── MediaProbe / stream and format metadata
  ├── Frame and audio extraction
  ├── Transcode / filter / mux operations
  ├── Job lifecycle: progress, cancellation, diagnostics
  └── Exact time: ticks + rational time base
          |
          v
Backend SPI
  ├── Native FFM backend
  └── FFmpeg process backend (optional, isolated)
          |
          v
Generated FFmpeg bindings (internal)
  ├── avutil
  ├── avcodec
  ├── avformat
  ├── avfilter
  └── swscale / swresample as required
          |
          v
FFmpeg native libraries
```

The backend SPI is an implementation seam, not a requirement to build a large plugin framework prematurely. Start with one working backend and add another only when there is a concrete operational need.

## 4. Public API principles

- Use ordinary Java types for paths, options, metadata, timestamps, results, and errors.
- Keep native handles and `MemorySegment` out of public contracts.
- Make runtime startup explicit and return a capability/version report.
- Define ownership for buffers and extracted artifacts.
- Provide synchronous primitives for small operations and cancellable job APIs for long-running operations.
- Use argument-vector process invocation if a process backend is implemented; do not invoke a shell with interpolated user input.
- Preserve source stream time bases and timestamp provenance through extraction and transcoding.

## 5. Dalang integration boundary

Dalang should depend on Manazir for media operations such as probing, frame extraction, audio extraction, transcoding, and rendering primitives. Dalang owns assets, projects, temporal indexes, edit decision lists, clipper workflows, and review/approval. Manazir must not import Dalang domain types.

## 6. Open engineering questions to resolve during M1

- Which JDK baseline is intended? FFM is final in JDK 22, but the project's actual target must be confirmed.
- Which FFmpeg release/header set generated the current bindings?
- Which operating systems and architectures are supported initially?
- Should the first production backend be FFM or the FFmpeg CLI? Choose based on deployment and isolation needs, not assumption.
- How will jextract generation be automated and pinned?
- Which FFmpeg libraries are required for the first vertical slice (probe, video frame extraction, audio extraction)?

Resolve these questions before introducing a build that claims a supported native runtime matrix.