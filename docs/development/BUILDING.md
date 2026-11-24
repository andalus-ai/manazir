# Building Manazir

## Requirements

- JDK 22 or newer with the Java 22 language/API baseline.
- Maven 3.9 or newer.

The current generated FFM binding snapshot targets the Java Foreign Function & Memory API. The initial Maven build declares Java 22 as its compiler release. This is a provisional source/build baseline, not yet a supported native deployment matrix.

## Build and test

```bash
mvn test
```

The unit tests for the public time value types do not require FFmpeg native libraries. The generated binding source is compiled as part of the current source tree; verify this on the target JDK before treating the build as a stable release baseline.

## Native runtime status

No native FFmpeg runtime bootstrap or capability discovery is provided yet. Do not assume that the presence of generated `avutil` declarations means the native library is installed, ABI-compatible, or loadable.

The FFmpeg header version, jextract version, generation command, and supported OS/architecture matrix remain to be pinned. Native integration tests must be opt-in until that matrix is established.
