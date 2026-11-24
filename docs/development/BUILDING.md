# Building Manazir

## Requirements

- JDK 22 or newer with the Java 22 language/API baseline.
- Maven 3.9 or newer.

The current generated FFM binding snapshot targets the Java Foreign Function & Memory API. The initial Maven build declares Java 22 as its compiler release. This is a provisional source/build baseline, not yet a supported native deployment matrix.

## Build and test

```bash
mvn test
```

The public timestamp and native-runtime contract tests use injected loaders and do not require FFmpeg native libraries. The generated binding source is compiled as part of the current source tree; verify this on the target JDK before treating the build as a stable release baseline.

## Native runtime initialization

Native loading is explicit:

```java
var report = NativeRuntime.system()
    .initialize(NativeRuntimeConfig.ffmpegDefaults());

if (!report.successful()) {
    report.failures().forEach(System.err::println);
}
```

Use fail-fast configuration when startup must stop if a required library is unavailable. Merely referencing Manazir API classes does not load native libraries.

A successful OS-level load does not yet verify FFmpeg ABI compatibility or required symbol availability. Do not advertise probe/decode/encode capabilities until version and symbol checks are implemented.

## Native runtime status

The FFmpeg header version, jextract version, generation command, and supported OS/architecture matrix remain to be pinned. Native integration tests must be opt-in until that matrix is established.
