# Building Manazir

## Requirements

- JDK 22 or newer. The source baseline is Java 22 because the generated bindings use the finalized Foreign Function & Memory API.
- Maven 3.9 or newer.

The build is verified in CI on Temurin JDK 22 (the minimum source baseline) and JDK 25 (a current LTS runtime). This is a Java build compatibility check, not yet a supported native deployment matrix.

## Build and test

```bash
mvn --batch-mode --no-transfer-progress clean verify
```

The default build compiles the checked-in generated FFM declarations and runs unit tests without requiring FFmpeg native libraries. Native integration tests must use a separate opt-in profile after the FFmpeg header version, jextract version, generation command, and supported OS/architecture matrix are pinned.

CI runs the same Maven verification command for pull requests and pushes to `main`. It uses a JDK matrix (22 and 25), Maven dependency caching, and read-only repository permissions.

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

The FFmpeg header version, jextract version, generation command, and supported OS/architecture matrix remain to be pinned. Native integration tests remain out of the default CI path until that matrix is established.
